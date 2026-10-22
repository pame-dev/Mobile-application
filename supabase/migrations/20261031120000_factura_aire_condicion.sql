-- ############################################################################
-- Factura original, aire acondicionado y condición (seminuevo / usado)
-- ############################################################################
-- Tres datos nuevos del paso "Datos" al publicar y editar:
--   · factura_original    → ¿cuenta con factura original? (sí / no)
--   · aire_acondicionado  → ¿tiene aire acondicionado? (sí / no)
--   · condicion_vehiculo  → 'seminuevo' o 'usado' (obligatorio al publicar y editar)
--
-- Van en publicaciones y en propuestas_publicacion, como el resto de los datos
-- del vehículo: admin_moderar_propuesta los copia al aprobar y v_anuncios los
-- muestra (al final, para no mover las columnas que ya tenía). Las publicaciones
-- anteriores quedan con factura y aire en FALSE y la condición en NULL.
--
-- crear_publicacion y editar_publicacion parten de
-- 20261028120000_documento_vehiculo.sql, admin_moderar_propuesta de
-- 20261001120000_motivo_obligatorio_acciones_admin.sql y v_anuncios de
-- 20261015120000_normalizacion_integridad.sql; solo se agregan estas columnas.


-- ── Columnas ───────────────────────────────────────────────────────────────

ALTER TABLE public.publicaciones
    ADD COLUMN factura_original   BOOLEAN     NOT NULL DEFAULT FALSE,
    ADD COLUMN aire_acondicionado BOOLEAN     NOT NULL DEFAULT FALSE,
    ADD COLUMN condicion_vehiculo VARCHAR(10) NULL,
    ADD CONSTRAINT chk_publicaciones_condicion_vehiculo
        CHECK (condicion_vehiculo IN ('seminuevo', 'usado'));

ALTER TABLE public.propuestas_publicacion
    ADD COLUMN factura_original   BOOLEAN     NOT NULL DEFAULT FALSE,
    ADD COLUMN aire_acondicionado BOOLEAN     NOT NULL DEFAULT FALSE,
    ADD COLUMN condicion_vehiculo VARCHAR(10) NULL,
    ADD CONSTRAINT chk_propuestas_publicacion_condicion_vehiculo
        CHECK (condicion_vehiculo IN ('seminuevo', 'usado'));

COMMENT ON COLUMN public.publicaciones.condicion_vehiculo IS
    'seminuevo o usado, según el vendedor. NULL en publicaciones anteriores a este dato.';
COMMENT ON COLUMN public.propuestas_publicacion.condicion_vehiculo IS
    'seminuevo o usado, según el vendedor. NULL en propuestas anteriores a este dato.';


-- ── Validación ─────────────────────────────────────────────────────────────

-- Devuelve la condición a guardar o lanza un error claro.
CREATE FUNCTION public.condicion_vehiculo_valida(p_condicion TEXT)
RETURNS TEXT
LANGUAGE plpgsql
IMMUTABLE
SET search_path = ''
AS $$
BEGIN
    IF p_condicion IS NULL OR p_condicion NOT IN ('seminuevo', 'usado') THEN
        RAISE EXCEPTION 'Elige si el vehículo es seminuevo o usado.';
    END IF;
    RETURN p_condicion;
END;
$$;
REVOKE ALL ON FUNCTION public.condicion_vehiculo_valida(TEXT) FROM PUBLIC, anon;
GRANT EXECUTE ON FUNCTION public.condicion_vehiculo_valida(TEXT) TO authenticated;


-- ── crear_publicacion ──────────────────────────────────────────────────────

CREATE OR REPLACE FUNCTION public.crear_publicacion(p_datos JSONB, p_fotos TEXT[])
RETURNS BIGINT
LANGUAGE plpgsql
SET search_path = ''
AS $$
DECLARE
    v_cuenta       public.cuentas%ROWTYPE;
    v_id           BIGINT;
    v_id_propuesta BIGINT;
    v_problemas    TEXT := NULLIF(TRIM(p_datos ->> 'descripcion_problemas'), '');
    v_documento    TEXT;
    v_condicion    TEXT;
BEGIN
    SELECT * INTO v_cuenta FROM public.cuentas WHERE id_cuenta = auth.uid();
    IF NOT FOUND THEN
        RAISE EXCEPTION 'Inicia sesión para publicar.';
    END IF;
    IF v_cuenta.estado_cuenta = 'suspendida' THEN
        RAISE EXCEPTION 'Tu cuenta está suspendida.';
    END IF;

    v_documento := public.documento_vehiculo_valido(
        p_datos ->> 'tipo_documento_vehiculo', p_datos ->> 'documento_vehiculo');
    v_condicion := public.condicion_vehiculo_valida(p_datos ->> 'condicion_vehiculo');

    INSERT INTO public.publicaciones (id_propietario)
    VALUES (auth.uid())
    RETURNING id_publicacion INTO v_id;

    INSERT INTO public.propuestas_publicacion (
        id_publicacion, titulo, descripcion, precio, precio_negociable, moneda,
        estado_ubicacion, municipio_ubicacion,
        id_marca, marca_otra, id_modelo, modelo_otro, anio, kilometraje,
        id_color, id_transmision, id_carroceria,
        numero_propietarios_anteriores, tiene_adeudos, tiene_problemas,
        descripcion_problemas, recuperado_por_seguro, cilindros,
        cilindrada, tipo_combustible,
        tipo_documento_vehiculo, documento_vehiculo,
        factura_original, aire_acondicionado, condicion_vehiculo
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
        (p_datos ->> 'cilindros')::SMALLINT,
        NULLIF(TRIM(p_datos ->> 'cilindrada'), ''),
        NULLIF(TRIM(p_datos ->> 'tipo_combustible'), ''),
        p_datos ->> 'tipo_documento_vehiculo',
        v_documento,
        COALESCE((p_datos ->> 'factura_original')::BOOLEAN, FALSE),
        COALESCE((p_datos ->> 'aire_acondicionado')::BOOLEAN, FALSE),
        v_condicion
    )
    RETURNING id_propuesta INTO v_id_propuesta;

    INSERT INTO public.fotos_propuesta_publicacion (id_propuesta, ruta_storage, orden_foto)
    SELECT v_id_propuesta, f.ruta, f.orden::SMALLINT
    FROM unnest(COALESCE(p_fotos, ARRAY[]::TEXT[])) WITH ORDINALITY AS f(ruta, orden);

    RETURN v_id;
END;
$$;


-- ── editar_publicacion ─────────────────────────────────────────────────────

CREATE OR REPLACE FUNCTION public.editar_publicacion(p_id_publicacion BIGINT, p_datos JSONB, p_fotos TEXT[])
RETURNS VOID
LANGUAGE plpgsql
SECURITY DEFINER
SET search_path = ''
AS $$
DECLARE
    v_cuenta       public.cuentas%ROWTYPE;
    v_id_propuesta BIGINT;
    v_problemas    TEXT := NULLIF(TRIM(p_datos ->> 'descripcion_problemas'), '');
    v_documento    TEXT;
    v_condicion    TEXT;
BEGIN
    SELECT * INTO v_cuenta FROM public.cuentas WHERE id_cuenta = auth.uid();
    IF NOT FOUND THEN
        RAISE EXCEPTION 'Inicia sesión para publicar.';
    END IF;
    IF v_cuenta.estado_cuenta = 'suspendida' THEN
        RAISE EXCEPTION 'Tu cuenta está suspendida.';
    END IF;
    IF NOT public.es_propietario_publicacion(p_id_publicacion) THEN
        RAISE EXCEPTION 'La publicación no existe.';
    END IF;
    IF COALESCE(array_length(p_fotos, 1), 0) = 0 THEN
        RAISE EXCEPTION 'Agrega al menos una foto.';
    END IF;
    IF array_length(p_fotos, 1) > 15 THEN
        RAISE EXCEPTION 'Puedes subir máximo 15 fotos.';
    END IF;
    -- Solo fotos subidas por el dueño (CarRepository.uploadPhoto usa esta carpeta).
    IF EXISTS (SELECT 1 FROM unnest(p_fotos) AS r
               WHERE r NOT LIKE 'publicaciones/' || auth.uid()::TEXT || '/%') THEN
        RAISE EXCEPTION 'Alguna foto no es válida.';
    END IF;

    v_documento := public.documento_vehiculo_valido(
        p_datos ->> 'tipo_documento_vehiculo', p_datos ->> 'documento_vehiculo');
    v_condicion := public.condicion_vehiculo_valida(p_datos ->> 'condicion_vehiculo');

    SELECT id_propuesta INTO v_id_propuesta
    FROM public.propuestas_publicacion
    WHERE id_publicacion = p_id_publicacion AND estado_propuesta IN ('pendiente', 'rechazada')
    FOR UPDATE;

    IF NOT FOUND THEN
        -- Copia completa: los campos que el formulario no edita (ubicación,
        -- número de puertas, etc.) se conservan al aprobar.
        INSERT INTO public.propuestas_publicacion (
            id_publicacion, titulo, descripcion, precio, precio_negociable, moneda,
            estado_ubicacion, municipio_ubicacion, horario_preferido_contacto, ruta_video,
            id_marca, marca_otra, id_modelo, modelo_otro, anio, kilometraje,
            id_color, color_otro, id_transmision, transmision_otra, id_carroceria, carroceria_otra,
            numero_propietarios_anteriores, tiene_adeudos, tiene_problemas, descripcion_problemas,
            recuperado_por_seguro, tipo_combustible, numero_puertas, numero_pasajeros,
            cilindros, cilindrada, tipo_traccion, modificaciones,
            factura_original, aire_acondicionado, condicion_vehiculo
        )
        SELECT
            p.id_publicacion, p.titulo, p.descripcion, p.precio, p.precio_negociable, p.moneda,
            p.estado_ubicacion, p.municipio_ubicacion, p.horario_preferido_contacto, p.ruta_video,
            p.id_marca, p.marca_otra, p.id_modelo, p.modelo_otro, p.anio, p.kilometraje,
            p.id_color, p.color_otro, p.id_transmision, p.transmision_otra, p.id_carroceria, p.carroceria_otra,
            p.numero_propietarios_anteriores, p.tiene_adeudos, p.tiene_problemas, p.descripcion_problemas,
            p.recuperado_por_seguro, p.tipo_combustible, p.numero_puertas, p.numero_pasajeros,
            p.cilindros, p.cilindrada, p.tipo_traccion, p.modificaciones,
            p.factura_original, p.aire_acondicionado, p.condicion_vehiculo
        FROM public.publicaciones p
        WHERE p.id_publicacion = p_id_publicacion AND p.fecha_primera_publicacion IS NOT NULL
        RETURNING id_propuesta INTO v_id_propuesta;

        IF v_id_propuesta IS NULL THEN
            RAISE EXCEPTION 'La publicación no se puede editar.';
        END IF;
    END IF;

    UPDATE public.propuestas_publicacion
    SET titulo                         = TRIM(p_datos ->> 'titulo'),
        descripcion                    = TRIM(p_datos ->> 'descripcion'),
        precio                         = (p_datos ->> 'precio')::NUMERIC,
        precio_negociable              = COALESCE((p_datos ->> 'precio_negociable')::BOOLEAN, precio_negociable),
        moneda                         = COALESCE(p_datos ->> 'moneda', moneda),
        id_marca                       = (p_datos ->> 'id_marca')::BIGINT,
        marca_otra                     = NULLIF(TRIM(p_datos ->> 'marca_otra'), ''),
        id_modelo                      = (p_datos ->> 'id_modelo')::BIGINT,
        modelo_otro                    = NULLIF(TRIM(p_datos ->> 'modelo_otro'), ''),
        anio                           = (p_datos ->> 'anio')::SMALLINT,
        kilometraje                    = (p_datos ->> 'kilometraje')::INTEGER,
        id_color                       = (p_datos ->> 'id_color')::BIGINT,
        id_transmision                 = (p_datos ->> 'id_transmision')::BIGINT,
        id_carroceria                  = (p_datos ->> 'id_carroceria')::BIGINT,
        numero_propietarios_anteriores = COALESCE((p_datos ->> 'numero_propietarios_anteriores')::SMALLINT, 0),
        tiene_problemas                = v_problemas IS NOT NULL,
        descripcion_problemas          = v_problemas,
        cilindros                      = (p_datos ->> 'cilindros')::SMALLINT,
        cilindrada                     = NULLIF(TRIM(p_datos ->> 'cilindrada'), ''),
        tipo_combustible               = NULLIF(TRIM(p_datos ->> 'tipo_combustible'), ''),
        recuperado_por_seguro          = COALESCE((p_datos ->> 'recuperado_por_seguro')::BOOLEAN, recuperado_por_seguro),
        tipo_documento_vehiculo        = p_datos ->> 'tipo_documento_vehiculo',
        documento_vehiculo             = v_documento,
        factura_original               = COALESCE((p_datos ->> 'factura_original')::BOOLEAN, factura_original),
        aire_acondicionado             = COALESCE((p_datos ->> 'aire_acondicionado')::BOOLEAN, aire_acondicionado),
        condicion_vehiculo             = v_condicion,
        -- A moderación.
        estado_propuesta                 = 'pendiente',
        motivo_rechazo                   = NULL,
        id_admin_revisor                 = NULL,
        fecha_revision                   = NULL,
        fecha_envio_moderacion           = NOW(),
        fecha_ultima_edicion_propietario = NOW()
    WHERE id_propuesta = v_id_propuesta;

    -- La galería se reemplaza completa (puede conservar rutas que ya tenía).
    DELETE FROM public.fotos_propuesta_publicacion WHERE id_propuesta = v_id_propuesta;

    INSERT INTO public.fotos_propuesta_publicacion (id_propuesta, ruta_storage, orden_foto)
    SELECT v_id_propuesta, f.ruta, f.orden::SMALLINT
    FROM unnest(p_fotos) WITH ORDINALITY AS f(ruta, orden);
END;
$$;


-- ── admin_moderar_propuesta ────────────────────────────────────────────────

CREATE OR REPLACE FUNCTION public.admin_moderar_propuesta(p_id_propuesta BIGINT, p_aprobar BOOLEAN, p_motivo TEXT DEFAULT NULL)
RETURNS VOID
LANGUAGE plpgsql
SECURITY DEFINER
SET search_path = ''
AS $$
DECLARE
    v_pr public.propuestas_publicacion%ROWTYPE;
    v_motivo TEXT;
BEGIN
    PERFORM public.exigir_admin();

    SELECT * INTO v_pr FROM public.propuestas_publicacion
    WHERE id_propuesta = p_id_propuesta FOR UPDATE;
    IF NOT FOUND OR v_pr.estado_propuesta <> 'pendiente' THEN
        RAISE EXCEPTION 'La propuesta no existe o ya fue revisada.';
    END IF;

    IF NOT p_aprobar THEN
        v_motivo := public.exigir_motivo(p_motivo, 'rechaza');

        UPDATE public.propuestas_publicacion
        SET estado_propuesta = 'rechazada',
            motivo_rechazo   = v_motivo,
            id_admin_revisor = auth.uid(),
            fecha_revision   = NOW()
        WHERE id_propuesta = p_id_propuesta;

        INSERT INTO public.acciones_administrativas (id_admin, tipo_accion, motivo, id_propuesta_objetivo)
        VALUES (auth.uid(), 'rechazar_propuesta', v_motivo, p_id_propuesta);
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
        factura_original = v_pr.factura_original, aire_acondicionado = v_pr.aire_acondicionado,
        condicion_vehiculo = v_pr.condicion_vehiculo,
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


-- ── v_anuncios ─────────────────────────────────────────────────────────────

CREATE OR REPLACE VIEW public.v_anuncios WITH (security_invoker='true') AS
 SELECT p.id_publicacion,
    p.id_propietario,
    c.nombre_mostrar AS propietario_nombre,
    c.tipo_cuenta AS propietario_tipo,
    c.foto_perfil AS propietario_foto,
    d.titulo,
    d.descripcion,
    d.precio,
    d.precio_negociable,
    d.moneda,
    d.estado_ubicacion,
    d.municipio_ubicacion,
    d.horario_preferido_contacto,
    d.id_marca,
    COALESCE(d.marca_otra, ma.nombre_marca) AS marca,
    d.id_modelo,
    COALESCE(d.modelo_otro, mo.nombre_modelo) AS modelo,
    d.anio,
    d.kilometraje,
    d.id_color,
    COALESCE(d.color_otro, co.nombre_color) AS color,
    d.id_transmision,
    COALESCE(d.transmision_otra, tr.nombre_transmision) AS transmision,
    d.id_carroceria,
    COALESCE(d.carroceria_otra, ca.nombre_carroceria) AS carroceria,
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
    (p.fecha_primera_publicacion IS NOT NULL) AS aprobada,
    ((p.fecha_primera_publicacion IS NOT NULL) AND ((p.estado_publicacion)::text = 'activa'::text) AND ((p.estado_administrativo)::text = 'habilitada'::text)) AS visible,
    pr.id_propuesta AS id_ultima_propuesta,
    pr.estado_propuesta AS estado_ultima_propuesta,
    pr.motivo_rechazo AS motivo_rechazo_propuesta,
    (EXISTS ( SELECT 1
           FROM public.solicitudes_destacado s
          WHERE ((s.id_publicacion = p.id_publicacion) AND ((s.estado_solicitud)::text = 'aprobada'::text) AND ((now() >= s.fecha_inicio_destacado) AND (now() <= s.fecha_fin_destacado))))) AS destacado,
    (EXISTS ( SELECT 1
           FROM public.solicitudes_destacado s
          WHERE ((s.id_publicacion = p.id_publicacion) AND ((s.estado_solicitud)::text = 'pendiente'::text)))) AS destacado_pendiente,
        CASE
            WHEN (NOT u.usar_propuesta) THEN ARRAY( SELECT f.ruta_storage
               FROM public.fotos_publicacion f
              WHERE ((f.id_publicacion = p.id_publicacion) AND f.activa)
              ORDER BY f.orden_foto)
            ELSE ARRAY( SELECT f.ruta_storage
               FROM public.fotos_propuesta_publicacion f
              WHERE (f.id_propuesta = pr.id_propuesta)
              ORDER BY f.orden_foto)
        END AS fotos,
    p.motivo_deshabilitacion,
    p.fecha_deshabilitacion,
    ((p.estado_publicacion)::text = 'vendida'::text) AS vendido,
    p.medio_venta,
    d.factura_original,
    d.aire_acondicionado,
    d.condicion_vehiculo
   FROM (((((((((public.publicaciones p
     JOIN public.cuentas c ON ((c.id_cuenta = p.id_propietario)))
     LEFT JOIN LATERAL ( SELECT x.id_propuesta,
            x.id_publicacion,
            x.titulo,
            x.descripcion,
            x.precio,
            x.precio_negociable,
            x.moneda,
            x.estado_ubicacion,
            x.municipio_ubicacion,
            x.horario_preferido_contacto,
            x.ruta_video,
            x.id_marca,
            x.marca_otra,
            x.id_modelo,
            x.modelo_otro,
            x.anio,
            x.kilometraje,
            x.id_color,
            x.color_otro,
            x.id_transmision,
            x.transmision_otra,
            x.id_carroceria,
            x.carroceria_otra,
            x.numero_propietarios_anteriores,
            x.tiene_adeudos,
            x.tiene_problemas,
            x.descripcion_problemas,
            x.recuperado_por_seguro,
            x.tipo_combustible,
            x.numero_puertas,
            x.numero_pasajeros,
            x.cilindros,
            x.cilindrada,
            x.tipo_traccion,
            x.modificaciones,
            x.factura_original,
            x.aire_acondicionado,
            x.condicion_vehiculo,
            x.estado_propuesta,
            x.motivo_rechazo,
            x.id_admin_revisor,
            x.fecha_envio_moderacion,
            x.fecha_revision,
            x.fecha_ultima_edicion_propietario
           FROM public.propuestas_publicacion x
          WHERE (x.id_publicacion = p.id_publicacion)
          ORDER BY x.fecha_envio_moderacion DESC, x.id_propuesta DESC
         LIMIT 1) pr ON (true))
     CROSS JOIN LATERAL ( SELECT ((pr.id_propuesta IS NOT NULL) AND ((p.fecha_primera_publicacion IS NULL) OR (((p.estado_administrativo)::text = 'deshabilitada_administrador'::text) AND ((pr.estado_propuesta)::text = ANY ((ARRAY['pendiente'::character varying, 'rechazada'::character varying])::text[]))))) AS usar_propuesta) u)
     LEFT JOIN LATERAL ( SELECT p.titulo,
            p.descripcion,
            p.precio,
            p.precio_negociable,
            p.moneda,
            p.estado_ubicacion,
            p.municipio_ubicacion,
            p.horario_preferido_contacto,
            p.id_marca,
            p.marca_otra,
            p.id_modelo,
            p.modelo_otro,
            p.anio,
            p.kilometraje,
            p.id_color,
            p.color_otro,
            p.id_transmision,
            p.transmision_otra,
            p.id_carroceria,
            p.carroceria_otra,
            p.numero_propietarios_anteriores,
            p.tiene_adeudos,
            p.tiene_problemas,
            p.descripcion_problemas,
            p.recuperado_por_seguro,
            p.tipo_combustible,
            p.numero_puertas,
            p.numero_pasajeros,
            p.cilindros,
            p.cilindrada,
            p.tipo_traccion,
            p.modificaciones,
            p.factura_original,
            p.aire_acondicionado,
            p.condicion_vehiculo
          WHERE ((p.fecha_primera_publicacion IS NOT NULL) AND (NOT u.usar_propuesta))
        UNION ALL
         SELECT pr.titulo,
            pr.descripcion,
            pr.precio,
            pr.precio_negociable,
            pr.moneda,
            pr.estado_ubicacion,
            pr.municipio_ubicacion,
            pr.horario_preferido_contacto,
            pr.id_marca,
            pr.marca_otra,
            pr.id_modelo,
            pr.modelo_otro,
            pr.anio,
            pr.kilometraje,
            pr.id_color,
            pr.color_otro,
            pr.id_transmision,
            pr.transmision_otra,
            pr.id_carroceria,
            pr.carroceria_otra,
            pr.numero_propietarios_anteriores,
            pr.tiene_adeudos,
            pr.tiene_problemas,
            pr.descripcion_problemas,
            pr.recuperado_por_seguro,
            pr.tipo_combustible,
            pr.numero_puertas,
            pr.numero_pasajeros,
            pr.cilindros,
            pr.cilindrada,
            pr.tipo_traccion,
            pr.modificaciones,
            pr.factura_original,
            pr.aire_acondicionado,
            pr.condicion_vehiculo
          WHERE u.usar_propuesta) d ON (true))
     LEFT JOIN public.marcas ma ON ((ma.id_marca = d.id_marca)))
     LEFT JOIN public.modelos mo ON ((mo.id_modelo = d.id_modelo)))
     LEFT JOIN public.colores co ON ((co.id_color = d.id_color)))
     LEFT JOIN public.transmisiones tr ON ((tr.id_transmision = d.id_transmision)))
     LEFT JOIN public.carrocerias ca ON ((ca.id_carroceria = d.id_carroceria)));
