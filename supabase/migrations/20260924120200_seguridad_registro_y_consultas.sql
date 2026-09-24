-- ============================================================================
-- KARSY — Migración: seguridad (RLS), registro, vistas y funciones de la app
-- ============================================================================
--   1. Funciones auxiliares de permisos.
--   2. Registro: al crear un usuario en Supabase Auth se crea su cuenta Karsy
--      con los datos que la app manda en raw_user_meta_data.
--   3. Políticas RLS y permisos por columna.
--   4. Vista v_anuncios (lo que muestran listas, detalle y paneles).
--   5. Funciones para publicar, contactar, estadísticas y administración.
--   6. Catálogo: fila "Otros" de modelo para cada marca.
--   7. Storage: bucket público "karsy" para fotos.
-- ============================================================================


-- ############################################################################
-- 1. FUNCIONES AUXILIARES
-- ############################################################################

CREATE FUNCTION public.es_admin_actual()
RETURNS BOOLEAN
LANGUAGE sql
STABLE
SECURITY DEFINER
SET search_path = ''
AS $$
    SELECT public.es_administrador(auth.uid());
$$;

CREATE FUNCTION public.es_propietario_publicacion(p_id_publicacion BIGINT)
RETURNS BOOLEAN
LANGUAGE sql
STABLE
SECURITY DEFINER
SET search_path = ''
AS $$
    SELECT EXISTS (
        SELECT 1 FROM public.publicaciones
        WHERE id_publicacion = p_id_publicacion
          AND id_propietario = auth.uid()
    );
$$;

CREATE FUNCTION public.es_propietario_propuesta(p_id_propuesta BIGINT)
RETURNS BOOLEAN
LANGUAGE sql
STABLE
SECURITY DEFINER
SET search_path = ''
AS $$
    SELECT EXISTS (
        SELECT 1
        FROM public.propuestas_publicacion pr
        JOIN public.publicaciones p ON p.id_publicacion = pr.id_publicacion
        WHERE pr.id_propuesta = p_id_propuesta
          AND p.id_propietario = auth.uid()
    );
$$;

-- Lanza error si quien llama no es administrador (para las funciones admin_*).
CREATE FUNCTION public.exigir_admin()
RETURNS VOID
LANGUAGE plpgsql
STABLE
SECURITY DEFINER
SET search_path = ''
AS $$
BEGIN
    IF NOT public.es_administrador(auth.uid()) THEN
        RAISE EXCEPTION 'Solo un administrador puede realizar esta acción.';
    END IF;
END;
$$;


-- ############################################################################
-- 2. REGISTRO
-- ############################################################################
-- La app llama a supabase.auth.signUp con estos datos en "data":
--   tipo_cuenta ('particular' | 'lote'), nombre_mostrar, estado, municipio,
--   telefono, descripcion
--   y para lote: nombre_comercial, calle, numero, colonia, codigo_postal,
--   horarios, responsable.
-- Los usuarios creados sin tipo_cuenta (p. ej. desde el Dashboard) no
-- generan cuenta Karsy.

CREATE FUNCTION public.fn_auth_crear_cuenta()
RETURNS TRIGGER
LANGUAGE plpgsql
SECURITY DEFINER
SET search_path = ''
AS $$
DECLARE
    v_meta     JSONB := COALESCE(NEW.raw_user_meta_data, '{}'::JSONB);
    v_tipo     TEXT  := v_meta ->> 'tipo_cuenta';
    v_telefono TEXT  := NULLIF(TRIM(v_meta ->> 'telefono'), '');
BEGIN
    IF v_tipo IS NULL THEN
        RETURN NEW;
    END IF;

    INSERT INTO public.cuentas (
        id_cuenta, tipo_cuenta, nombre_mostrar, descripcion_corta,
        estado_perfil_ubi, municipio_perfil_ubi
    ) VALUES (
        NEW.id,
        v_tipo,
        TRIM(v_meta ->> 'nombre_mostrar'),
        LEFT(NULLIF(TRIM(v_meta ->> 'descripcion'), ''), 300),
        TRIM(v_meta ->> 'estado'),
        TRIM(v_meta ->> 'municipio')
    );

    IF v_telefono IS NOT NULL THEN
        INSERT INTO public.telefonos_contacto (
            id_cuenta, numero_telefono, permite_llamadas, es_whatsapp, es_principal
        ) VALUES (NEW.id, v_telefono, TRUE, TRUE, TRUE);
    END IF;

    IF v_tipo = 'lote' THEN
        INSERT INTO public.perfiles_lote (
            id_cuenta, nombre_comercial, descripcion_lote, calle, numero, colonia,
            codigo_postal, municipio, estado, horarios_atencion
        ) VALUES (
            NEW.id,
            TRIM(COALESCE(v_meta ->> 'nombre_comercial', v_meta ->> 'nombre_mostrar')),
            NULLIF(TRIM(v_meta ->> 'descripcion'), ''),
            TRIM(v_meta ->> 'calle'),
            TRIM(v_meta ->> 'numero'),
            TRIM(v_meta ->> 'colonia'),
            TRIM(v_meta ->> 'codigo_postal'),
            TRIM(v_meta ->> 'municipio'),
            TRIM(v_meta ->> 'estado'),
            NULLIF(TRIM(v_meta ->> 'horarios'), '')
        );
    END IF;

    RETURN NEW;
END;
$$;

CREATE TRIGGER trg_auth_crear_cuenta
    AFTER INSERT ON auth.users
    FOR EACH ROW EXECUTE FUNCTION public.fn_auth_crear_cuenta();


-- ############################################################################
-- 3. RLS Y PERMISOS
-- ############################################################################

-- Catálogos y roles: lectura pública.
CREATE POLICY marcas_leer        ON public.marcas        FOR SELECT TO anon, authenticated USING (TRUE);
CREATE POLICY modelos_leer       ON public.modelos       FOR SELECT TO anon, authenticated USING (TRUE);
CREATE POLICY carrocerias_leer   ON public.carrocerias   FOR SELECT TO anon, authenticated USING (TRUE);
CREATE POLICY transmisiones_leer ON public.transmisiones FOR SELECT TO anon, authenticated USING (TRUE);
CREATE POLICY colores_leer       ON public.colores       FOR SELECT TO anon, authenticated USING (TRUE);
CREATE POLICY roles_leer         ON public.roles         FOR SELECT TO anon, authenticated USING (TRUE);

-- cuentas: perfil público; cada quien edita solo sus datos de perfil.
-- La suspensión la hace un administrador con admin_cambiar_estado_cuenta().
CREATE POLICY cuentas_leer ON public.cuentas
    FOR SELECT TO anon, authenticated USING (TRUE);
CREATE POLICY cuentas_editar_propia ON public.cuentas
    FOR UPDATE TO authenticated
    USING (id_cuenta = auth.uid()) WITH CHECK (id_cuenta = auth.uid());

REVOKE INSERT, UPDATE, DELETE ON public.cuentas FROM anon, authenticated;
GRANT UPDATE (nombre_mostrar, descripcion_corta, foto_perfil, estado_perfil_ubi, municipio_perfil_ubi)
    ON public.cuentas TO authenticated;

-- perfiles_lote
CREATE POLICY perfiles_lote_leer ON public.perfiles_lote
    FOR SELECT TO anon, authenticated USING (TRUE);
CREATE POLICY perfiles_lote_editar_propio ON public.perfiles_lote
    FOR UPDATE TO authenticated
    USING (id_cuenta = auth.uid()) WITH CHECK (id_cuenta = auth.uid());

-- telefonos_contacto
CREATE POLICY telefonos_leer ON public.telefonos_contacto
    FOR SELECT TO anon, authenticated
    USING (activo OR id_cuenta = auth.uid() OR public.es_admin_actual());
CREATE POLICY telefonos_insertar_propio ON public.telefonos_contacto
    FOR INSERT TO authenticated WITH CHECK (id_cuenta = auth.uid());
CREATE POLICY telefonos_editar_propio ON public.telefonos_contacto
    FOR UPDATE TO authenticated
    USING (id_cuenta = auth.uid()) WITH CHECK (id_cuenta = auth.uid());
CREATE POLICY telefonos_borrar_propio ON public.telefonos_contacto
    FOR DELETE TO authenticated USING (id_cuenta = auth.uid());

-- cuentas_roles: cada quien ve sus roles; el administrador ve todos.
CREATE POLICY cuentas_roles_leer ON public.cuentas_roles
    FOR SELECT TO authenticated
    USING (id_cuenta = auth.uid() OR public.es_admin_actual());

-- publicaciones: el público ve las aprobadas y habilitadas; el dueño y el
-- administrador ven todas. El dueño solo cambia estado_publicacion; el
-- contenido público cambia al aprobar una propuesta.
CREATE POLICY publicaciones_leer ON public.publicaciones
    FOR SELECT TO anon, authenticated
    USING (
        (fecha_primera_publicacion IS NOT NULL
            AND estado_administrativo = 'habilitada'
            AND estado_publicacion IN ('activa', 'vendida'))
        OR id_propietario = auth.uid()
        OR public.es_admin_actual()
    );
CREATE POLICY publicaciones_crear ON public.publicaciones
    FOR INSERT TO authenticated
    WITH CHECK (
        id_propietario = auth.uid()
        AND fecha_primera_publicacion IS NULL
        AND estado_administrativo = 'habilitada'
    );
CREATE POLICY publicaciones_editar_propia ON public.publicaciones
    FOR UPDATE TO authenticated
    USING (id_propietario = auth.uid()) WITH CHECK (id_propietario = auth.uid());

REVOKE UPDATE, DELETE ON public.publicaciones FROM anon, authenticated;
GRANT UPDATE (estado_publicacion) ON public.publicaciones TO authenticated;

-- propuestas_publicacion
CREATE POLICY propuestas_leer ON public.propuestas_publicacion
    FOR SELECT TO authenticated
    USING (public.es_propietario_publicacion(id_publicacion) OR public.es_admin_actual());
CREATE POLICY propuestas_crear ON public.propuestas_publicacion
    FOR INSERT TO authenticated
    WITH CHECK (
        public.es_propietario_publicacion(id_publicacion)
        AND estado_propuesta = 'pendiente'
        AND id_admin_revisor IS NULL
        AND fecha_revision IS NULL
    );

-- fotos
CREATE POLICY fotos_publicacion_leer ON public.fotos_publicacion
    FOR SELECT TO anon, authenticated
    USING (EXISTS (
        SELECT 1 FROM public.publicaciones p
        WHERE p.id_publicacion = fotos_publicacion.id_publicacion
    ));
CREATE POLICY fotos_propuesta_leer ON public.fotos_propuesta_publicacion
    FOR SELECT TO authenticated
    USING (public.es_propietario_propuesta(id_propuesta) OR public.es_admin_actual());
CREATE POLICY fotos_propuesta_crear ON public.fotos_propuesta_publicacion
    FOR INSERT TO authenticated
    WITH CHECK (public.es_propietario_propuesta(id_propuesta));

-- favoritos
CREATE POLICY favoritos_leer_propios ON public.favoritos
    FOR SELECT TO authenticated USING (id_cuenta = auth.uid());
CREATE POLICY favoritos_crear_propios ON public.favoritos
    FOR INSERT TO authenticated WITH CHECK (id_cuenta = auth.uid());
CREATE POLICY favoritos_borrar_propios ON public.favoritos
    FOR DELETE TO authenticated USING (id_cuenta = auth.uid());

-- reportes
CREATE POLICY reportes_leer ON public.reportes
    FOR SELECT TO authenticated
    USING (id_reportante = auth.uid() OR public.es_admin_actual());
CREATE POLICY reportes_crear ON public.reportes
    FOR INSERT TO authenticated
    WITH CHECK (id_reportante = auth.uid() AND estado_reporte = 'pendiente');

-- solicitudes_destacado
CREATE POLICY solicitudes_leer ON public.solicitudes_destacado
    FOR SELECT TO anon, authenticated
    USING (
        estado_solicitud = 'aprobada'
        OR public.es_propietario_publicacion(id_publicacion)
        OR public.es_admin_actual()
    );
CREATE POLICY solicitudes_crear ON public.solicitudes_destacado
    FOR INSERT TO authenticated
    WITH CHECK (
        public.es_propietario_publicacion(id_publicacion)
        AND estado_solicitud = 'pendiente'
    );

-- interacciones: cualquiera registra una vista (anónima o propia);
-- contactar requiere sesión (lo exige también un CHECK de la tabla).
CREATE POLICY interacciones_crear ON public.interacciones_publicacion
    FOR INSERT TO anon, authenticated
    WITH CHECK (
        (id_cuenta IS NULL AND tipo_interaccion = 'ver_detalle')
        OR id_cuenta = auth.uid()
    );
CREATE POLICY interacciones_leer ON public.interacciones_publicacion
    FOR SELECT TO authenticated
    USING (
        id_cuenta = auth.uid()
        OR public.es_propietario_publicacion(id_publicacion)
        OR public.es_admin_actual()
    );

-- acciones_administrativas: solo lectura para administradores; se escriben
-- desde las funciones admin_*.
CREATE POLICY acciones_leer ON public.acciones_administrativas
    FOR SELECT TO authenticated USING (public.es_admin_actual());


-- ############################################################################
-- 4. VISTA v_anuncios
-- ############################################################################
-- Una fila por publicación. Si ya fue aprobada muestra el contenido público;
-- si no, el de su propuesta más reciente (solo lo ven el dueño y el admin,
-- porque la vista respeta el RLS de quien consulta).

CREATE VIEW public.v_anuncios
WITH (security_invoker = TRUE)
AS
SELECT
    p.id_publicacion,
    p.id_propietario,
    c.nombre_mostrar                       AS propietario_nombre,
    c.tipo_cuenta                          AS propietario_tipo,
    c.foto_perfil                          AS propietario_foto,
    d.titulo,
    d.descripcion,
    d.precio,
    d.precio_negociable,
    d.moneda,
    d.estado_ubicacion,
    d.municipio_ubicacion,
    d.horario_preferido_contacto,
    d.id_marca,
    COALESCE(d.marca_otra, ma.nombre_marca)             AS marca,
    d.id_modelo,
    COALESCE(d.modelo_otro, mo.nombre_modelo)           AS modelo,
    d.anio,
    d.kilometraje,
    d.id_color,
    COALESCE(d.color_otro, co.nombre_color)             AS color,
    d.id_transmision,
    COALESCE(d.transmision_otra, tr.nombre_transmision) AS transmision,
    d.id_carroceria,
    COALESCE(d.carroceria_otra, ca.nombre_carroceria)   AS carroceria,
    d.numero_propietarios_anteriores,
    d.tiene_adeudos,
    d.tiene_problemas,
    d.descripcion_problemas,
    d.recuperado_por_seguro,
    d.tipo_combustible,
    d.numero_puertas,
    d.numero_pasajeros,
    d.cilindros,
    d.cilindrada,
    d.tipo_traccion,
    d.modificaciones,
    p.estado_publicacion,
    p.estado_administrativo,
    p.fecha_creacion,
    p.fecha_primera_publicacion,
    (p.fecha_primera_publicacion IS NOT NULL)            AS aprobada,
    (p.fecha_primera_publicacion IS NOT NULL
        AND p.estado_publicacion = 'activa'
        AND p.estado_administrativo = 'habilitada')      AS visible,
    pr.id_propuesta                                      AS id_ultima_propuesta,
    pr.estado_propuesta                                  AS estado_ultima_propuesta,
    pr.motivo_rechazo                                    AS motivo_rechazo_propuesta,
    EXISTS (
        SELECT 1 FROM public.solicitudes_destacado s
        WHERE s.id_publicacion = p.id_publicacion
          AND s.estado_solicitud = 'aprobada'
          AND NOW() BETWEEN s.fecha_inicio_destacado AND s.fecha_fin_destacado
    )                                                    AS destacado,
    EXISTS (
        SELECT 1 FROM public.solicitudes_destacado s
        WHERE s.id_publicacion = p.id_publicacion
          AND s.estado_solicitud = 'pendiente'
    )                                                    AS destacado_pendiente,
    CASE WHEN p.fecha_primera_publicacion IS NOT NULL THEN
        ARRAY(SELECT f.ruta_storage FROM public.fotos_publicacion f
              WHERE f.id_publicacion = p.id_publicacion AND f.activa
              ORDER BY f.orden_foto)
    ELSE
        ARRAY(SELECT f.ruta_storage FROM public.fotos_propuesta_publicacion f
              WHERE f.id_propuesta = pr.id_propuesta
              ORDER BY f.orden_foto)
    END                                                  AS fotos
FROM public.publicaciones p
JOIN public.cuentas c ON c.id_cuenta = p.id_propietario
LEFT JOIN LATERAL (
    SELECT x.*
    FROM public.propuestas_publicacion x
    WHERE x.id_publicacion = p.id_publicacion
    ORDER BY x.fecha_envio_moderacion DESC, x.id_propuesta DESC
    LIMIT 1
) pr ON TRUE
LEFT JOIN LATERAL (
    SELECT p.titulo, p.descripcion, p.precio, p.precio_negociable, p.moneda,
           p.estado_ubicacion, p.municipio_ubicacion, p.horario_preferido_contacto,
           p.id_marca, p.marca_otra, p.id_modelo, p.modelo_otro, p.anio, p.kilometraje,
           p.id_color, p.color_otro, p.id_transmision, p.transmision_otra,
           p.id_carroceria, p.carroceria_otra, p.numero_propietarios_anteriores,
           p.tiene_adeudos, p.tiene_problemas, p.descripcion_problemas,
           p.recuperado_por_seguro, p.tipo_combustible, p.numero_puertas,
           p.numero_pasajeros, p.cilindros, p.cilindrada, p.tipo_traccion, p.modificaciones
    WHERE p.fecha_primera_publicacion IS NOT NULL
    UNION ALL
    SELECT pr.titulo, pr.descripcion, pr.precio, pr.precio_negociable, pr.moneda,
           pr.estado_ubicacion, pr.municipio_ubicacion, pr.horario_preferido_contacto,
           pr.id_marca, pr.marca_otra, pr.id_modelo, pr.modelo_otro, pr.anio, pr.kilometraje,
           pr.id_color, pr.color_otro, pr.id_transmision, pr.transmision_otra,
           pr.id_carroceria, pr.carroceria_otra, pr.numero_propietarios_anteriores,
           pr.tiene_adeudos, pr.tiene_problemas, pr.descripcion_problemas,
           pr.recuperado_por_seguro, pr.tipo_combustible, pr.numero_puertas,
           pr.numero_pasajeros, pr.cilindros, pr.cilindrada, pr.tipo_traccion, pr.modificaciones
    WHERE p.fecha_primera_publicacion IS NULL AND pr.id_propuesta IS NOT NULL
) d ON TRUE
LEFT JOIN public.marcas        ma ON ma.id_marca       = d.id_marca
LEFT JOIN public.modelos       mo ON mo.id_modelo      = d.id_modelo
LEFT JOIN public.colores       co ON co.id_color       = d.id_color
LEFT JOIN public.transmisiones tr ON tr.id_transmision = d.id_transmision
LEFT JOIN public.carrocerias   ca ON ca.id_carroceria  = d.id_carroceria;

COMMENT ON VIEW public.v_anuncios IS 'Anuncio listo para mostrar: contenido aprobado (o la última propuesta si aún no se aprueba), nombres de catálogo, fotos, dueño y si está destacado. Respeta el RLS de quien consulta.';

GRANT SELECT ON public.v_anuncios TO anon, authenticated;


-- ############################################################################
-- 5. FUNCIONES DE LA APP
-- ############################################################################

-- ----------------------------------------------------------------------------
-- Publicar: crea la publicación, su propuesta pendiente y las fotos.
-- Corre con los permisos de quien llama (RLS).
-- ----------------------------------------------------------------------------
CREATE FUNCTION public.crear_publicacion(p_datos JSONB, p_fotos TEXT[])
RETURNS BIGINT
LANGUAGE plpgsql
SET search_path = ''
AS $$
DECLARE
    v_cuenta       public.cuentas%ROWTYPE;
    v_id           BIGINT;
    v_id_propuesta BIGINT;
    v_problemas    TEXT := NULLIF(TRIM(p_datos ->> 'descripcion_problemas'), '');
BEGIN
    SELECT * INTO v_cuenta FROM public.cuentas WHERE id_cuenta = auth.uid();
    IF NOT FOUND THEN
        RAISE EXCEPTION 'Inicia sesión para publicar.';
    END IF;
    IF v_cuenta.estado_cuenta = 'suspendida' THEN
        RAISE EXCEPTION 'Tu cuenta está suspendida.';
    END IF;

    INSERT INTO public.publicaciones (id_propietario)
    VALUES (auth.uid())
    RETURNING id_publicacion INTO v_id;

    INSERT INTO public.propuestas_publicacion (
        id_publicacion, titulo, descripcion, precio, precio_negociable, moneda,
        estado_ubicacion, municipio_ubicacion,
        id_marca, marca_otra, id_modelo, modelo_otro, anio, kilometraje,
        id_color, id_transmision, id_carroceria,
        numero_propietarios_anteriores, tiene_adeudos, tiene_problemas,
        descripcion_problemas, recuperado_por_seguro, cilindros
    ) VALUES (
        v_id,
        TRIM(p_datos ->> 'titulo'),
        TRIM(p_datos ->> 'descripcion'),
        (p_datos ->> 'precio')::NUMERIC,
        COALESCE((p_datos ->> 'precio_negociable')::BOOLEAN, FALSE),
        COALESCE(p_datos ->> 'moneda', 'MXN'),
        COALESCE(NULLIF(TRIM(p_datos ->> 'estado_ubicacion'), ''), v_cuenta.estado_perfil_ubi),
        COALESCE(NULLIF(TRIM(p_datos ->> 'municipio_ubicacion'), ''), v_cuenta.municipio_perfil_ubi),
        (p_datos ->> 'id_marca')::BIGINT,
        NULLIF(TRIM(p_datos ->> 'marca_otra'), ''),
        (p_datos ->> 'id_modelo')::BIGINT,
        NULLIF(TRIM(p_datos ->> 'modelo_otro'), ''),
        (p_datos ->> 'anio')::SMALLINT,
        (p_datos ->> 'kilometraje')::INTEGER,
        (p_datos ->> 'id_color')::BIGINT,
        (p_datos ->> 'id_transmision')::BIGINT,
        (p_datos ->> 'id_carroceria')::BIGINT,
        COALESCE((p_datos ->> 'numero_propietarios_anteriores')::SMALLINT, 0),
        COALESCE((p_datos ->> 'tiene_adeudos')::BOOLEAN, FALSE),
        v_problemas IS NOT NULL,
        v_problemas,
        COALESCE((p_datos ->> 'recuperado_por_seguro')::BOOLEAN, FALSE),
        (p_datos ->> 'cilindros')::SMALLINT
    )
    RETURNING id_propuesta INTO v_id_propuesta;

    INSERT INTO public.fotos_propuesta_publicacion (id_propuesta, ruta_storage, orden_foto)
    SELECT v_id_propuesta, f.ruta, f.orden::SMALLINT
    FROM unnest(COALESCE(p_fotos, ARRAY[]::TEXT[])) WITH ORDINALITY AS f(ruta, orden);

    RETURN v_id;
END;
$$;

-- ----------------------------------------------------------------------------
-- Datos de contacto del vendedor de una publicación visible.
-- Teléfono y correo solo se entregan a usuarios con sesión.
-- ----------------------------------------------------------------------------
CREATE FUNCTION public.contacto_vendedor(p_id_publicacion BIGINT)
RETURNS JSONB
LANGUAGE plpgsql
STABLE
SECURITY DEFINER
SET search_path = ''
AS $$
DECLARE
    v_propietario UUID;
    v_resultado   JSONB;
BEGIN
    SELECT id_propietario INTO v_propietario
    FROM public.publicaciones
    WHERE id_publicacion = p_id_publicacion
      AND (
          (fecha_primera_publicacion IS NOT NULL AND estado_administrativo = 'habilitada')
          OR id_propietario = auth.uid()
          OR public.es_administrador(auth.uid())
      );

    IF v_propietario IS NULL THEN
        RETURN NULL;
    END IF;

    SELECT jsonb_build_object(
        'id_cuenta',   c.id_cuenta,
        'nombre',      c.nombre_mostrar,
        'tipo_cuenta', c.tipo_cuenta,
        'descripcion', COALESCE(pl.descripcion_lote, c.descripcion_corta),
        'ubicacion',   c.municipio_perfil_ubi || ', ' || c.estado_perfil_ubi,
        'foto_perfil', c.foto_perfil,
        'miembro_desde', c.fecha_creacion,
        'telefono',    CASE WHEN auth.uid() IS NOT NULL THEN t.numero_telefono END,
        'whatsapp',    CASE WHEN auth.uid() IS NOT NULL THEN t.es_whatsapp END,
        'correo',      CASE WHEN auth.uid() IS NOT NULL THEN u.email END
    )
    INTO v_resultado
    FROM public.cuentas c
    JOIN auth.users u ON u.id = c.id_cuenta
    LEFT JOIN public.perfiles_lote pl ON pl.id_cuenta = c.id_cuenta
    LEFT JOIN LATERAL (
        SELECT numero_telefono, es_whatsapp
        FROM public.telefonos_contacto
        WHERE id_cuenta = c.id_cuenta AND activo
        ORDER BY es_principal DESC, id_telefono
        LIMIT 1
    ) t ON TRUE
    WHERE c.id_cuenta = v_propietario;

    RETURN v_resultado;
END;
$$;

-- ----------------------------------------------------------------------------
-- Correo de la cuenta en sesión (el perfil lo muestra y edita).
-- ----------------------------------------------------------------------------
CREATE FUNCTION public.mi_correo()
RETURNS TEXT
LANGUAGE sql
STABLE
SECURITY DEFINER
SET search_path = ''
AS $$
    SELECT email FROM auth.users WHERE id = auth.uid();
$$;

-- ----------------------------------------------------------------------------
-- Estadísticas del panel del vendedor (cuenta en sesión).
-- ----------------------------------------------------------------------------
CREATE FUNCTION public.vendedor_estadisticas()
RETURNS JSONB
LANGUAGE plpgsql
STABLE
SECURITY DEFINER
SET search_path = ''
AS $$
DECLARE
    v_uid   UUID := auth.uid();
    v_dias  TEXT[] := ARRAY['D', 'L', 'M', 'X', 'J', 'V', 'S'];
BEGIN
    IF v_uid IS NULL THEN
        RAISE EXCEPTION 'Inicia sesión para ver tu panel.';
    END IF;

    RETURN jsonb_build_object(
        'publicadas', (SELECT COUNT(*) FROM public.publicaciones
                       WHERE id_propietario = v_uid AND fecha_primera_publicacion IS NOT NULL),
        'activas',    (SELECT COUNT(*) FROM public.publicaciones
                       WHERE id_propietario = v_uid AND fecha_primera_publicacion IS NOT NULL
                         AND estado_publicacion = 'activa' AND estado_administrativo = 'habilitada'),
        'vendidas',   (SELECT COUNT(*) FROM public.publicaciones
                       WHERE id_propietario = v_uid AND estado_publicacion = 'vendida'),
        'pendientes', (SELECT COUNT(*) FROM public.propuestas_publicacion pr
                       JOIN public.publicaciones p ON p.id_publicacion = pr.id_publicacion
                       WHERE p.id_propietario = v_uid AND pr.estado_propuesta = 'pendiente'),
        'vistas',     (SELECT COUNT(*) FROM public.interacciones_publicacion i
                       JOIN public.publicaciones p ON p.id_publicacion = i.id_publicacion
                       WHERE p.id_propietario = v_uid AND i.tipo_interaccion = 'ver_detalle'),
        'contactos',  (SELECT COUNT(*) FROM public.interacciones_publicacion i
                       JOIN public.publicaciones p ON p.id_publicacion = i.id_publicacion
                       WHERE p.id_propietario = v_uid AND i.tipo_interaccion = 'contactar'),
        'vistas_mes', (SELECT COUNT(*) FROM public.interacciones_publicacion i
                       JOIN public.publicaciones p ON p.id_publicacion = i.id_publicacion
                       WHERE p.id_propietario = v_uid AND i.tipo_interaccion = 'ver_detalle'
                         AND i.fecha_hora >= date_trunc('month', NOW())),
        'vistas_mes_anterior', (SELECT COUNT(*) FROM public.interacciones_publicacion i
                       JOIN public.publicaciones p ON p.id_publicacion = i.id_publicacion
                       WHERE p.id_propietario = v_uid AND i.tipo_interaccion = 'ver_detalle'
                         AND i.fecha_hora >= date_trunc('month', NOW()) - INTERVAL '1 month'
                         AND i.fecha_hora <  date_trunc('month', NOW())),
        -- 12 valores, del mes más antiguo al actual.
        'vistas_por_mes', (
            SELECT jsonb_agg(COALESCE(x.total, 0) ORDER BY m.mes)
            FROM generate_series(date_trunc('month', NOW()) - INTERVAL '11 months',
                                 date_trunc('month', NOW()), INTERVAL '1 month') AS m(mes)
            LEFT JOIN (
                SELECT date_trunc('month', i.fecha_hora) AS mes, COUNT(*) AS total
                FROM public.interacciones_publicacion i
                JOIN public.publicaciones p ON p.id_publicacion = i.id_publicacion
                WHERE p.id_propietario = v_uid AND i.tipo_interaccion = 'ver_detalle'
                GROUP BY 1
            ) x ON x.mes = m.mes
        ),
        -- Últimos 7 días, del más antiguo a hoy.
        'favoritos_semana', (
            SELECT jsonb_agg(COALESCE(x.total, 0) ORDER BY d.dia)
            FROM generate_series(CURRENT_DATE - 6, CURRENT_DATE, INTERVAL '1 day') AS d(dia)
            LEFT JOIN (
                SELECT f.fecha_guardado::DATE AS dia, COUNT(*) AS total
                FROM public.favoritos f
                JOIN public.publicaciones p ON p.id_publicacion = f.id_publicacion
                WHERE p.id_propietario = v_uid
                GROUP BY 1
            ) x ON x.dia = d.dia
        ),
        'dias_semana', (
            SELECT jsonb_agg(v_dias[EXTRACT(DOW FROM d.dia)::INT + 1] ORDER BY d.dia)
            FROM generate_series(CURRENT_DATE - 6, CURRENT_DATE, INTERVAL '1 day') AS d(dia)
        )
    );
END;
$$;


-- ############################################################################
-- 5b. FUNCIONES DE ADMINISTRACIÓN
-- ############################################################################

-- Lista de cuentas con correo (auth.users) y conteos.
CREATE FUNCTION public.admin_listar_cuentas()
RETURNS TABLE (
    id_cuenta         UUID,
    nombre_mostrar    TEXT,
    correo            TEXT,
    tipo_cuenta       TEXT,
    estado_cuenta     TEXT,
    fecha_creacion    TIMESTAMPTZ,
    es_admin          BOOLEAN,
    municipio         TEXT,
    estado            TEXT,
    responsable       TEXT,
    publicaciones     BIGINT,
    activas           BIGINT
)
LANGUAGE plpgsql
STABLE
SECURITY DEFINER
SET search_path = ''
AS $$
BEGIN
    PERFORM public.exigir_admin();

    RETURN QUERY
    SELECT
        c.id_cuenta,
        c.nombre_mostrar::TEXT,
        u.email::TEXT,
        c.tipo_cuenta::TEXT,
        c.estado_cuenta::TEXT,
        c.fecha_creacion,
        public.es_administrador(c.id_cuenta),
        c.municipio_perfil_ubi::TEXT,
        c.estado_perfil_ubi::TEXT,
        COALESCE(u.raw_user_meta_data ->> 'responsable', c.nombre_mostrar)::TEXT,
        (SELECT COUNT(*) FROM public.publicaciones p WHERE p.id_propietario = c.id_cuenta),
        (SELECT COUNT(*) FROM public.publicaciones p
         WHERE p.id_propietario = c.id_cuenta AND p.fecha_primera_publicacion IS NOT NULL
           AND p.estado_publicacion = 'activa' AND p.estado_administrativo = 'habilitada')
    FROM public.cuentas c
    JOIN auth.users u ON u.id = c.id_cuenta
    ORDER BY c.fecha_creacion DESC;
END;
$$;

-- Indicadores del dashboard de administración. p_dias = 7, 30 o 90.
CREATE FUNCTION public.admin_estadisticas(p_dias INT DEFAULT 7)
RETURNS JSONB
LANGUAGE plpgsql
STABLE
SECURITY DEFINER
SET search_path = ''
AS $$
DECLARE
    v_desde  TIMESTAMPTZ := NOW() - make_interval(days => p_dias);
    v_meses  TEXT[] := ARRAY['ene','feb','mar','abr','may','jun','jul','ago','sep','oct','nov','dic'];
BEGIN
    PERFORM public.exigir_admin();

    RETURN jsonb_build_object(
        'usuarios',        (SELECT COUNT(*) FROM public.cuentas),
        'usuarios_nuevos', (SELECT COUNT(*) FROM public.cuentas WHERE fecha_creacion >= v_desde),
        'lotes',           (SELECT COUNT(*) FROM public.cuentas WHERE tipo_cuenta = 'lote'),
        'lotes_nuevos',    (SELECT COUNT(*) FROM public.cuentas WHERE tipo_cuenta = 'lote' AND fecha_creacion >= v_desde),
        'publicadas',      (SELECT COUNT(*) FROM public.publicaciones WHERE fecha_primera_publicacion IS NOT NULL),
        'publicadas_nuevas', (SELECT COUNT(*) FROM public.publicaciones WHERE fecha_primera_publicacion >= v_desde),
        'activas',         (SELECT COUNT(*) FROM public.publicaciones
                            WHERE fecha_primera_publicacion IS NOT NULL
                              AND estado_publicacion = 'activa' AND estado_administrativo = 'habilitada'),
        'deshabilitadas',  (SELECT COUNT(*) FROM public.publicaciones
                            WHERE estado_administrativo = 'deshabilitada_administrador'
                               OR estado_publicacion = 'deshabilitada'),
        'suspendidas',     (SELECT COUNT(*) FROM public.cuentas WHERE estado_cuenta = 'suspendida'),
        'propuestas_pendientes', (SELECT COUNT(*) FROM public.propuestas_publicacion WHERE estado_propuesta = 'pendiente'),
        'reportes_pendientes',   (SELECT COUNT(*) FROM public.reportes WHERE estado_reporte = 'pendiente'),
        'destacados_pendientes', (SELECT COUNT(*) FROM public.solicitudes_destacado WHERE estado_solicitud = 'pendiente'),
        -- 7 puntos repartidos en el rango: totales acumulados a cada fecha.
        'crecimiento', (
            SELECT jsonb_build_object(
                'etiquetas',     jsonb_agg(EXTRACT(DAY FROM t.corte)::INT || ' ' || v_meses[EXTRACT(MONTH FROM t.corte)::INT] ORDER BY t.k),
                'usuarios',      jsonb_agg((SELECT COUNT(*) FROM public.cuentas c WHERE c.fecha_creacion <= t.corte) ORDER BY t.k),
                'lotes',         jsonb_agg((SELECT COUNT(*) FROM public.cuentas c WHERE c.tipo_cuenta = 'lote' AND c.fecha_creacion <= t.corte) ORDER BY t.k),
                'publicaciones', jsonb_agg((SELECT COUNT(*) FROM public.publicaciones p WHERE p.fecha_primera_publicacion <= t.corte) ORDER BY t.k)
            )
            FROM (
                SELECT k, NOW() - make_interval(secs => (p_dias * 86400.0) * (6 - k) / 6) AS corte
                FROM generate_series(0, 6) AS k
            ) t
        ),
        -- Últimos 6 meses: vistas de detalle y contactos.
        'interacciones', (
            SELECT jsonb_build_object(
                'etiquetas', jsonb_agg(INITCAP(v_meses[EXTRACT(MONTH FROM m.mes)::INT]) ORDER BY m.mes),
                'vistas',    jsonb_agg((SELECT COUNT(*) FROM public.interacciones_publicacion i
                                        WHERE i.tipo_interaccion = 'ver_detalle'
                                          AND date_trunc('month', i.fecha_hora) = m.mes) ORDER BY m.mes),
                'contactos', jsonb_agg((SELECT COUNT(*) FROM public.interacciones_publicacion i
                                        WHERE i.tipo_interaccion = 'contactar'
                                          AND date_trunc('month', i.fecha_hora) = m.mes) ORDER BY m.mes)
            )
            FROM generate_series(date_trunc('month', NOW()) - INTERVAL '5 months',
                                 date_trunc('month', NOW()), INTERVAL '1 month') AS m(mes)
        )
    );
END;
$$;

-- Aprobar o rechazar una propuesta. Al aprobar, su contenido y fotos pasan a
-- la publicación.
CREATE FUNCTION public.admin_moderar_propuesta(p_id_propuesta BIGINT, p_aprobar BOOLEAN, p_motivo TEXT DEFAULT NULL)
RETURNS VOID
LANGUAGE plpgsql
SECURITY DEFINER
SET search_path = ''
AS $$
DECLARE
    v_pr public.propuestas_publicacion%ROWTYPE;
BEGIN
    PERFORM public.exigir_admin();

    SELECT * INTO v_pr FROM public.propuestas_publicacion
    WHERE id_propuesta = p_id_propuesta FOR UPDATE;
    IF NOT FOUND OR v_pr.estado_propuesta <> 'pendiente' THEN
        RAISE EXCEPTION 'La propuesta no existe o ya fue revisada.';
    END IF;

    IF NOT p_aprobar THEN
        UPDATE public.propuestas_publicacion
        SET estado_propuesta = 'rechazada',
            motivo_rechazo   = COALESCE(NULLIF(TRIM(p_motivo), ''), 'Sin motivo especificado.'),
            id_admin_revisor = auth.uid(),
            fecha_revision   = NOW()
        WHERE id_propuesta = p_id_propuesta;

        INSERT INTO public.acciones_administrativas (id_admin, tipo_accion, motivo, id_propuesta_objetivo)
        VALUES (auth.uid(), 'rechazar_propuesta',
                COALESCE(NULLIF(TRIM(p_motivo), ''), 'Sin motivo especificado.'), p_id_propuesta);
        RETURN;
    END IF;

    UPDATE public.publicaciones p
    SET titulo = v_pr.titulo, descripcion = v_pr.descripcion, precio = v_pr.precio,
        precio_negociable = v_pr.precio_negociable, moneda = v_pr.moneda,
        estado_ubicacion = v_pr.estado_ubicacion, municipio_ubicacion = v_pr.municipio_ubicacion,
        horario_preferido_contacto = v_pr.horario_preferido_contacto, ruta_video = v_pr.ruta_video,
        id_marca = v_pr.id_marca, marca_otra = v_pr.marca_otra,
        id_modelo = v_pr.id_modelo, modelo_otro = v_pr.modelo_otro,
        anio = v_pr.anio, kilometraje = v_pr.kilometraje,
        id_color = v_pr.id_color, color_otro = v_pr.color_otro,
        id_transmision = v_pr.id_transmision, transmision_otra = v_pr.transmision_otra,
        id_carroceria = v_pr.id_carroceria, carroceria_otra = v_pr.carroceria_otra,
        numero_propietarios_anteriores = v_pr.numero_propietarios_anteriores,
        tiene_adeudos = v_pr.tiene_adeudos, tiene_problemas = v_pr.tiene_problemas,
        descripcion_problemas = v_pr.descripcion_problemas,
        recuperado_por_seguro = v_pr.recuperado_por_seguro,
        tipo_combustible = v_pr.tipo_combustible, numero_puertas = v_pr.numero_puertas,
        numero_pasajeros = v_pr.numero_pasajeros, cilindros = v_pr.cilindros,
        cilindrada = v_pr.cilindrada, tipo_traccion = v_pr.tipo_traccion,
        modificaciones = v_pr.modificaciones,
        fecha_ultima_actualizacion_aprobada =
            CASE WHEN p.fecha_primera_publicacion IS NULL THEN NULL ELSE NOW() END,
        fecha_primera_publicacion = COALESCE(p.fecha_primera_publicacion, NOW())
    WHERE p.id_publicacion = v_pr.id_publicacion;

    -- La galería aprobada se reemplaza por la de la propuesta.
    UPDATE public.fotos_publicacion SET activa = FALSE
    WHERE id_publicacion = v_pr.id_publicacion AND activa;

    INSERT INTO public.fotos_publicacion (id_publicacion, ruta_storage, orden_foto)
    SELECT v_pr.id_publicacion, f.ruta_storage, f.orden_foto
    FROM public.fotos_propuesta_publicacion f
    WHERE f.id_propuesta = p_id_propuesta
    ORDER BY f.orden_foto;

    UPDATE public.propuestas_publicacion
    SET estado_propuesta = 'aprobada', id_admin_revisor = auth.uid(), fecha_revision = NOW()
    WHERE id_propuesta = p_id_propuesta;

    INSERT INTO public.acciones_administrativas (id_admin, tipo_accion, motivo, id_propuesta_objetivo)
    VALUES (auth.uid(), 'aprobar_propuesta',
            COALESCE(NULLIF(TRIM(p_motivo), ''), 'Contenido conforme a las reglas de publicación.'),
            p_id_propuesta);
END;
$$;

-- Habilitar o deshabilitar una publicación.
CREATE FUNCTION public.admin_cambiar_estado_publicacion(p_id_publicacion BIGINT, p_habilitar BOOLEAN, p_motivo TEXT DEFAULT NULL)
RETURNS VOID
LANGUAGE plpgsql
SECURITY DEFINER
SET search_path = ''
AS $$
BEGIN
    PERFORM public.exigir_admin();

    UPDATE public.publicaciones
    SET estado_administrativo = CASE WHEN p_habilitar THEN 'habilitada' ELSE 'deshabilitada_administrador' END
    WHERE id_publicacion = p_id_publicacion;

    INSERT INTO public.acciones_administrativas (id_admin, tipo_accion, motivo, id_publicacion_objetivo)
    VALUES (auth.uid(),
            CASE WHEN p_habilitar THEN 'habilitar_publicacion' ELSE 'deshabilitar_publicacion' END,
            COALESCE(NULLIF(TRIM(p_motivo), ''), 'Sin motivo especificado.'),
            p_id_publicacion);
END;
$$;

-- Suspender o reactivar una cuenta.
CREATE FUNCTION public.admin_cambiar_estado_cuenta(p_id_cuenta UUID, p_suspender BOOLEAN, p_motivo TEXT DEFAULT NULL)
RETURNS VOID
LANGUAGE plpgsql
SECURITY DEFINER
SET search_path = ''
AS $$
BEGIN
    PERFORM public.exigir_admin();

    IF p_id_cuenta = auth.uid() THEN
        RAISE EXCEPTION 'No puedes suspender tu propia cuenta.';
    END IF;

    UPDATE public.cuentas
    SET estado_cuenta = CASE WHEN p_suspender THEN 'suspendida' ELSE 'activa' END
    WHERE id_cuenta = p_id_cuenta;

    INSERT INTO public.acciones_administrativas (id_admin, tipo_accion, motivo, id_cuenta_objetivo)
    VALUES (auth.uid(),
            CASE WHEN p_suspender THEN 'suspender_cuenta' ELSE 'reactivar_cuenta' END,
            COALESCE(NULLIF(TRIM(p_motivo), ''), 'Sin motivo especificado.'),
            p_id_cuenta);
END;
$$;

-- Resolver un reporte ('atendido' | 'descartado'); opcionalmente deshabilita
-- la publicación reportada.
CREATE FUNCTION public.admin_resolver_reporte(p_id_reporte BIGINT, p_estado TEXT, p_motivo TEXT, p_deshabilitar BOOLEAN DEFAULT FALSE)
RETURNS VOID
LANGUAGE plpgsql
SECURITY DEFINER
SET search_path = ''
AS $$
DECLARE
    v_id_publicacion BIGINT;
    v_motivo TEXT := COALESCE(NULLIF(TRIM(p_motivo), ''), 'Sin motivo especificado.');
BEGIN
    PERFORM public.exigir_admin();

    UPDATE public.reportes
    SET estado_reporte = p_estado, id_admin_resolutor = auth.uid(),
        motivo_resolucion = v_motivo, fecha_resolucion = NOW()
    WHERE id_reporte = p_id_reporte AND estado_reporte = 'pendiente'
    RETURNING id_publicacion INTO v_id_publicacion;

    IF v_id_publicacion IS NULL THEN
        RAISE EXCEPTION 'El reporte no existe o ya fue resuelto.';
    END IF;

    INSERT INTO public.acciones_administrativas (id_admin, tipo_accion, motivo, id_reporte_objetivo)
    VALUES (auth.uid(), 'resolver_reporte', v_motivo, p_id_reporte);

    IF p_deshabilitar THEN
        PERFORM public.admin_cambiar_estado_publicacion(v_id_publicacion, FALSE, v_motivo);
    END IF;
END;
$$;

-- Aprobar (1 mes desde hoy) o rechazar una solicitud de destacado.
CREATE FUNCTION public.admin_resolver_destacado(p_id_solicitud BIGINT, p_aprobar BOOLEAN, p_motivo TEXT DEFAULT NULL)
RETURNS VOID
LANGUAGE plpgsql
SECURITY DEFINER
SET search_path = ''
AS $$
DECLARE
    v_inicio TIMESTAMPTZ := NOW();
    v_motivo TEXT := COALESCE(NULLIF(TRIM(p_motivo), ''), 'Sin motivo especificado.');
BEGIN
    PERFORM public.exigir_admin();

    IF p_aprobar THEN
        UPDATE public.solicitudes_destacado
        SET estado_solicitud = 'aprobada', id_admin_resuelve = auth.uid(),
            fecha_resolucion = v_inicio, fecha_inicio_destacado = v_inicio,
            fecha_fin_destacado = v_inicio + INTERVAL '1 month'
        WHERE id_solicitud_destacado = p_id_solicitud;
    ELSE
        UPDATE public.solicitudes_destacado
        SET estado_solicitud = 'rechazada', id_admin_resuelve = auth.uid(),
            fecha_resolucion = v_inicio, motivo_rechazo = v_motivo
        WHERE id_solicitud_destacado = p_id_solicitud;
    END IF;

    INSERT INTO public.acciones_administrativas (id_admin, tipo_accion, motivo, id_solicitud_destacado_objetivo)
    VALUES (auth.uid(),
            CASE WHEN p_aprobar THEN 'aprobar_destacado' ELSE 'rechazar_destacado' END,
            CASE WHEN p_aprobar THEN COALESCE(NULLIF(TRIM(p_motivo), ''), 'Solicitud aprobada.') ELSE v_motivo END,
            p_id_solicitud);
END;
$$;

-- Listados para el panel admin (con nombres del reportante / solicitante).
CREATE FUNCTION public.admin_listar_reportes()
RETURNS TABLE (
    id_reporte        BIGINT,
    id_publicacion    BIGINT,
    publicacion       TEXT,
    reportante        TEXT,
    motivo_reporte    TEXT,
    estado_reporte    TEXT,
    motivo_resolucion TEXT,
    fecha_reporte     TIMESTAMPTZ
)
LANGUAGE plpgsql
STABLE
SECURITY DEFINER
SET search_path = ''
AS $$
BEGIN
    PERFORM public.exigir_admin();

    RETURN QUERY
    SELECT r.id_reporte, r.id_publicacion,
           COALESCE(p.titulo, 'Publicación #' || r.id_publicacion)::TEXT,
           c.nombre_mostrar::TEXT, r.motivo_reporte, r.estado_reporte::TEXT,
           r.motivo_resolucion, r.fecha_reporte
    FROM public.reportes r
    JOIN public.publicaciones p ON p.id_publicacion = r.id_publicacion
    JOIN public.cuentas c       ON c.id_cuenta = r.id_reportante
    ORDER BY r.fecha_reporte DESC;
END;
$$;

CREATE FUNCTION public.admin_listar_destacados()
RETURNS TABLE (
    id_solicitud_destacado BIGINT,
    id_publicacion         BIGINT,
    estado_solicitud       TEXT,
    fecha_solicitud        TIMESTAMPTZ,
    motivo_rechazo         TEXT,
    solicitante            TEXT,
    solicitante_foto       TEXT
)
LANGUAGE plpgsql
STABLE
SECURITY DEFINER
SET search_path = ''
AS $$
BEGIN
    PERFORM public.exigir_admin();

    RETURN QUERY
    SELECT s.id_solicitud_destacado, s.id_publicacion, s.estado_solicitud::TEXT,
           s.fecha_solicitud, s.motivo_rechazo, c.nombre_mostrar::TEXT, c.foto_perfil
    FROM public.solicitudes_destacado s
    JOIN public.publicaciones p ON p.id_publicacion = s.id_publicacion
    JOIN public.cuentas c       ON c.id_cuenta = p.id_propietario
    WHERE s.estado_solicitud <> 'cancelada'
    ORDER BY s.fecha_solicitud DESC;
END;
$$;

-- Las funciones admin_* validan el rol adentro; no se exponen a anónimos.
REVOKE EXECUTE ON FUNCTION
    public.admin_listar_cuentas(),
    public.admin_estadisticas(INT),
    public.admin_moderar_propuesta(BIGINT, BOOLEAN, TEXT),
    public.admin_cambiar_estado_publicacion(BIGINT, BOOLEAN, TEXT),
    public.admin_cambiar_estado_cuenta(UUID, BOOLEAN, TEXT),
    public.admin_resolver_reporte(BIGINT, TEXT, TEXT, BOOLEAN),
    public.admin_resolver_destacado(BIGINT, BOOLEAN, TEXT),
    public.admin_listar_reportes(),
    public.admin_listar_destacados(),
    public.crear_publicacion(JSONB, TEXT[]),
    public.vendedor_estadisticas(),
    public.mi_correo()
FROM PUBLIC, anon;


-- ############################################################################
-- 6. CATÁLOGO: MODELO "OTROS" PARA CADA MARCA
-- ############################################################################
-- Permite publicar un modelo que no está en el catálogo de su marca.

INSERT INTO public.modelos (id_marca, nombre_modelo, es_otro)
SELECT m.id_marca, 'Otros', TRUE
FROM public.marcas m
WHERE NOT EXISTS (
    SELECT 1 FROM public.modelos x WHERE x.id_marca = m.id_marca AND x.es_otro
);


-- ############################################################################
-- 7. STORAGE
-- ############################################################################
-- Bucket público "karsy". Rutas: publicaciones/<id_cuenta>/<archivo> y
-- perfiles/<id_cuenta>/<archivo>. Cada usuario escribe solo en su carpeta.

INSERT INTO storage.buckets (id, name, public)
VALUES ('karsy', 'karsy', TRUE)
ON CONFLICT (id) DO NOTHING;

CREATE POLICY karsy_subir_propio ON storage.objects
    FOR INSERT TO authenticated
    WITH CHECK (
        bucket_id = 'karsy'
        AND (storage.foldername(name))[1] IN ('publicaciones', 'perfiles')
        AND (storage.foldername(name))[2] = auth.uid()::TEXT
    );

CREATE POLICY karsy_editar_propio ON storage.objects
    FOR UPDATE TO authenticated
    USING (bucket_id = 'karsy' AND (storage.foldername(name))[2] = auth.uid()::TEXT);

CREATE POLICY karsy_borrar_propio ON storage.objects
    FOR DELETE TO authenticated
    USING (bucket_id = 'karsy' AND (storage.foldername(name))[2] = auth.uid()::TEXT);
