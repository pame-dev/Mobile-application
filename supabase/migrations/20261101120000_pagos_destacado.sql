-- ############################################################################
-- Pago de destacados por transferencia (fuera de la app) y destacado solo para lotes
-- ############################################################################
-- · El destacado cuesta [precio] por [dias] días (config_pagos, editable por el admin).
-- · Solo los lotes pueden pedirlo. Al pedirlo, el lote transfiere a la CLABE que muestra Karsy
--   (banco y CLABE, sin el nombre del titular) con la referencia KAR-<id_publicacion> y sube
--   el comprobante (foto o PDF) en ese momento: no hay solicitudes sin comprobante.
-- · El admin revisa que el dinero haya llegado y aprueba o rechaza con motivo (como antes).
--   Rechazada, el lote puede volver a pedirlo con otro comprobante. Sin reembolsos.
-- · Los datos del pago van en una tabla aparte (pagos_destacado): solicitudes_destacado
--   aprobadas las puede leer cualquiera (para saber qué autos están destacados).
-- · Comprobantes en el bucket privado "comprobantes-pago". La BD no puede borrar archivos:
--   admin_comprobantes_sin_uso() lista los que ya no se necesitan (destacado terminado,
--   rechazado y reemplazado o de más de 30 días) y el panel del admin los borra.
-- · Las solicitudes que ya existían (sin pago, etapa de prueba) se siguen aprobando gratis.
-- · Los destacados vigentes de particulares siguen hasta que terminen.


-- ── Configuración del pago ─────────────────────────────────────────────────

-- Una sola fila. La CLABE y el banco los escribe el admin desde el panel; mientras falten,
-- no se puede pedir destacado (nadie paga a una cuenta vacía).
CREATE TABLE public.config_pagos (
    id                   SMALLINT       PRIMARY KEY DEFAULT 1,
    precio               NUMERIC(10, 2) NOT NULL DEFAULT 300,
    dias                 SMALLINT       NOT NULL DEFAULT 30,
    banco                VARCHAR(60),
    clabe                CHAR(18),
    fecha_actualizacion  TIMESTAMPTZ    NOT NULL DEFAULT NOW(),
    -- Quién la cambió por última vez (cambiar la CLABE redirige los pagos: debe quedar rastro).
    id_admin_actualiza   UUID,

    CONSTRAINT chk_config_pagos_una_fila CHECK (id = 1),
    CONSTRAINT chk_config_pagos_precio CHECK (precio > 0 AND precio <= 100000),
    CONSTRAINT chk_config_pagos_dias CHECK (dias BETWEEN 1 AND 365),
    CONSTRAINT chk_config_pagos_clabe CHECK (clabe IS NULL OR clabe ~ '^[0-9]{18}$'),
    CONSTRAINT fk_config_pagos_admin
        FOREIGN KEY (id_admin_actualiza) REFERENCES public.cuentas (id_cuenta) ON DELETE SET NULL
);

COMMENT ON TABLE public.config_pagos IS
    'Precio, duración y cuenta (banco y CLABE) para pagar destacados. Una fila; la edita el admin.';

INSERT INTO public.config_pagos (id) VALUES (1);

-- Nadie la lee directo: datos_pago_destacado() y admin_guardar_config_pagos().
ALTER TABLE public.config_pagos ENABLE ROW LEVEL SECURITY;
REVOKE ALL ON public.config_pagos FROM anon, authenticated;

-- Lo que ve quien va a pagar (y el admin): precio, días, banco y CLABE (sin titular).
CREATE FUNCTION public.datos_pago_destacado()
RETURNS TABLE (precio NUMERIC, dias SMALLINT, banco TEXT, clabe TEXT)
LANGUAGE sql
STABLE
SECURITY DEFINER
SET search_path = ''
AS $$
    SELECT c.precio, c.dias, c.banco::TEXT, c.clabe::TEXT FROM public.config_pagos c WHERE c.id = 1;
$$;

REVOKE ALL ON FUNCTION public.datos_pago_destacado() FROM PUBLIC, anon;
GRANT EXECUTE ON FUNCTION public.datos_pago_destacado() TO authenticated;

CREATE FUNCTION public.admin_guardar_config_pagos(p_precio NUMERIC, p_dias INT, p_banco TEXT, p_clabe TEXT)
RETURNS VOID
LANGUAGE plpgsql
SECURITY DEFINER
SET search_path = ''
AS $$
DECLARE
    v_banco TEXT := NULLIF(TRIM(p_banco), '');
    v_clabe TEXT := NULLIF(REGEXP_REPLACE(COALESCE(p_clabe, ''), '\s', '', 'g'), '');
BEGIN
    PERFORM public.exigir_admin();
    IF p_precio IS NULL OR p_precio <= 0 OR p_precio > 100000 THEN
        RAISE EXCEPTION 'El precio debe ser mayor a 0.';
    END IF;
    IF p_dias IS NULL OR p_dias < 1 OR p_dias > 365 THEN
        RAISE EXCEPTION 'Los días deben estar entre 1 y 365.';
    END IF;
    IF v_banco IS NOT NULL AND CHAR_LENGTH(v_banco) > 60 THEN
        RAISE EXCEPTION 'El nombre del banco puede tener como máximo 60 caracteres.';
    END IF;
    IF v_clabe IS NOT NULL AND v_clabe !~ '^[0-9]{18}$' THEN
        RAISE EXCEPTION 'La CLABE debe tener 18 dígitos.';
    END IF;

    UPDATE public.config_pagos
    SET precio = ROUND(p_precio, 2), dias = p_dias, banco = v_banco, clabe = v_clabe,
        fecha_actualizacion = NOW(), id_admin_actualiza = auth.uid()
    WHERE id = 1;
END;
$$;

REVOKE ALL ON FUNCTION public.admin_guardar_config_pagos(NUMERIC, INT, TEXT, TEXT) FROM PUBLIC, anon;
GRANT EXECUTE ON FUNCTION public.admin_guardar_config_pagos(NUMERIC, INT, TEXT, TEXT) TO authenticated;


-- ── Datos del pago de cada solicitud ───────────────────────────────────────

CREATE TABLE public.pagos_destacado (
    id_solicitud_destacado  BIGINT         PRIMARY KEY,
    monto                   NUMERIC(10, 2) NOT NULL,
    -- Días pagados: se respetan aunque el admin cambie la configuración antes de aprobar.
    dias                    SMALLINT       NOT NULL,
    referencia              VARCHAR(20)    NOT NULL,
    comprobante_path        TEXT           NOT NULL,
    fecha_registro          TIMESTAMPTZ    NOT NULL DEFAULT NOW(),

    CONSTRAINT fk_pagos_destacado_solicitud
        FOREIGN KEY (id_solicitud_destacado) REFERENCES public.solicitudes_destacado (id_solicitud_destacado) ON DELETE CASCADE,
    CONSTRAINT chk_pagos_destacado_monto CHECK (monto > 0),
    CONSTRAINT chk_pagos_destacado_dias CHECK (dias BETWEEN 1 AND 365),
    CONSTRAINT chk_pagos_destacado_comprobante CHECK (CHAR_LENGTH(comprobante_path) <= 300)
);

COMMENT ON TABLE public.pagos_destacado IS
    'Pago de una solicitud de destacado: monto, referencia y comprobante (privado: dueño y admin).';

-- Un mismo comprobante no sirve para dos solicitudes.
CREATE UNIQUE INDEX ux_pagos_destacado_comprobante ON public.pagos_destacado (comprobante_path);

-- Solo lectura para el dueño de la publicación y el admin; se escribe con solicitar_destacado().
ALTER TABLE public.pagos_destacado ENABLE ROW LEVEL SECURITY;
REVOKE ALL ON public.pagos_destacado FROM anon, authenticated;
GRANT SELECT ON public.pagos_destacado TO authenticated;

CREATE POLICY pagos_destacado_leer ON public.pagos_destacado FOR SELECT TO authenticated
    USING (
        public.es_admin_actual()
        OR EXISTS (SELECT 1 FROM public.solicitudes_destacado s
                   WHERE s.id_solicitud_destacado = pagos_destacado.id_solicitud_destacado
                     AND public.es_propietario_publicacion(s.id_publicacion))
    );


-- ── Bucket privado de comprobantes ─────────────────────────────────────────

INSERT INTO storage.buckets (id, name, public, file_size_limit, allowed_mime_types)
VALUES ('comprobantes-pago', 'comprobantes-pago', FALSE, 5242880, ARRAY['image/jpeg', 'image/png', 'application/pdf'])
ON CONFLICT (id) DO UPDATE
SET public = FALSE, file_size_limit = 5242880, allowed_mime_types = ARRAY['image/jpeg', 'image/png', 'application/pdf'];

-- Cada cuenta sube y lee solo en su carpeta (<id_cuenta>/<uuid>.<jpg|png|pdf>); el admin lee y borra.
CREATE POLICY comprobantes_pago_subir_propio ON storage.objects FOR INSERT TO authenticated
    WITH CHECK (bucket_id = 'comprobantes-pago'
        AND name ~ ('^' || (SELECT auth.uid())::TEXT || '/[0-9a-f-]{36}\.(jpg|png|pdf)$')
        -- Solo los lotes destacan: nadie más necesita subir comprobantes.
        AND EXISTS (SELECT 1 FROM public.cuentas c WHERE c.id_cuenta = (SELECT auth.uid()) AND c.tipo_cuenta = 'lote'));
CREATE POLICY comprobantes_pago_leer_propio ON storage.objects FOR SELECT TO authenticated
    USING (bucket_id = 'comprobantes-pago' AND (storage.foldername(name))[1] = auth.uid()::TEXT);
CREATE POLICY comprobantes_pago_leer_admin ON storage.objects FOR SELECT TO authenticated
    USING (bucket_id = 'comprobantes-pago' AND public.es_admin_actual());
CREATE POLICY comprobantes_pago_borrar_admin ON storage.objects FOR DELETE TO authenticated
    USING (bucket_id = 'comprobantes-pago' AND public.es_admin_actual());


-- ── Pedir destacado (solo lotes, con comprobante) ──────────────────────────

-- Antes el dueño insertaba la solicitud directo; ahora solo con esta función.
DROP POLICY solicitudes_crear ON public.solicitudes_destacado;

-- Devuelve el id de la solicitud. Una solicitud pendiente por publicación (uq_solicitudes_pendiente:
-- si ya hay una, la BD responde 23505 y la app y la web dicen "ya hay una solicitud pendiente").
-- Un comprobante ya usado se avisa antes con su propio mensaje, así el 23505 solo significa eso.
CREATE FUNCTION public.solicitar_destacado(p_id_publicacion BIGINT, p_comprobante TEXT)
RETURNS BIGINT
LANGUAGE plpgsql
SECURITY DEFINER
SET search_path = ''
AS $$
DECLARE
    v_uid    UUID := auth.uid();
    v_config public.config_pagos%ROWTYPE;
    v_id     BIGINT;
BEGIN
    IF v_uid IS NULL THEN
        RAISE EXCEPTION 'Inicia sesión para destacar tu auto.';
    END IF;
    IF NOT EXISTS (SELECT 1 FROM public.cuentas c WHERE c.id_cuenta = v_uid AND c.tipo_cuenta = 'lote') THEN
        RAISE EXCEPTION 'Destacar autos es solo para lotes.';
    END IF;
    IF NOT EXISTS (SELECT 1 FROM public.cuentas c WHERE c.id_cuenta = v_uid AND c.estado_cuenta = 'activa') THEN
        RAISE EXCEPTION 'Tu cuenta no está activa.';
    END IF;
    IF NOT EXISTS (
        SELECT 1 FROM public.publicaciones p
        WHERE p.id_publicacion = p_id_publicacion AND p.id_propietario = v_uid
          AND p.fecha_primera_publicacion IS NOT NULL
          AND p.estado_publicacion = 'activa' AND p.estado_administrativo = 'habilitada'
    ) THEN
        RAISE EXCEPTION 'Solo puedes destacar tus publicaciones visibles.';
    END IF;

    SELECT * INTO v_config FROM public.config_pagos WHERE id = 1;
    IF v_config.banco IS NULL OR v_config.clabe IS NULL THEN
        RAISE EXCEPTION 'Los pagos de destacados aún no están disponibles.';
    END IF;

    IF p_comprobante IS NULL OR LEFT(p_comprobante, CHAR_LENGTH(v_uid::TEXT) + 1) <> v_uid::TEXT || '/'
       OR NOT EXISTS (SELECT 1 FROM storage.objects o WHERE o.bucket_id = 'comprobantes-pago' AND o.name = p_comprobante) THEN
        RAISE EXCEPTION 'No encontramos tu comprobante. Vuelve a subirlo.';
    END IF;
    IF EXISTS (SELECT 1 FROM public.pagos_destacado pg WHERE pg.comprobante_path = p_comprobante) THEN
        RAISE EXCEPTION 'Ese comprobante ya se usó. Sube uno nuevo.';
    END IF;

    INSERT INTO public.solicitudes_destacado (id_publicacion) VALUES (p_id_publicacion)
    RETURNING id_solicitud_destacado INTO v_id;

    INSERT INTO public.pagos_destacado (id_solicitud_destacado, monto, dias, referencia, comprobante_path)
    VALUES (v_id, v_config.precio, v_config.dias, 'KAR-' || p_id_publicacion, p_comprobante);

    RETURN v_id;
END;
$$;

REVOKE ALL ON FUNCTION public.solicitar_destacado(BIGINT, TEXT) FROM PUBLIC, anon;
GRANT EXECUTE ON FUNCTION public.solicitar_destacado(BIGINT, TEXT) TO authenticated;


-- ── Aprobar o rechazar (duración desde la configuración) ───────────────────

-- La regla de campos exigía que un destacado durara exactamente 1 mes; ahora dura los días
-- pagados. Solo cambia la rama 'aprobada' (fin posterior al inicio); las demás quedan igual
-- que en 20260924120000.
ALTER TABLE public.solicitudes_destacado DROP CONSTRAINT chk_solicitudes_destacado_campos;
ALTER TABLE public.solicitudes_destacado ADD CONSTRAINT chk_solicitudes_destacado_campos
    CHECK (
        (estado_solicitud = 'pendiente'
            AND id_admin_resuelve IS NULL AND motivo_rechazo IS NULL
            AND fecha_resolucion IS NULL AND fecha_inicio_destacado IS NULL
            AND fecha_fin_destacado IS NULL AND fecha_cancelacion IS NULL)
        OR
        (estado_solicitud = 'aprobada'
            AND id_admin_resuelve IS NOT NULL AND motivo_rechazo IS NULL
            AND fecha_resolucion IS NOT NULL
            AND fecha_inicio_destacado IS NOT NULL
            AND fecha_fin_destacado > fecha_inicio_destacado
            AND fecha_cancelacion IS NULL)
        OR
        (estado_solicitud = 'rechazada'
            AND id_admin_resuelve IS NOT NULL
            AND COALESCE(CHAR_LENGTH(TRIM(motivo_rechazo)), 0) > 0
            AND fecha_resolucion IS NOT NULL
            AND fecha_inicio_destacado IS NULL AND fecha_fin_destacado IS NULL
            AND fecha_cancelacion IS NULL)
        OR
        (estado_solicitud = 'cancelada'
            AND id_admin_resuelve IS NULL AND motivo_rechazo IS NULL
            AND fecha_resolucion IS NULL AND fecha_inicio_destacado IS NULL
            AND fecha_fin_destacado IS NULL AND fecha_cancelacion IS NOT NULL)
    );

-- Igual que en 20261011120000; ahora dura los días que se pagaron (pagos_destacado; las
-- solicitudes de prueba sin pago, los de config_pagos) y solo se resuelven pendientes.
CREATE OR REPLACE FUNCTION public.admin_resolver_destacado(p_id_solicitud BIGINT, p_aprobar BOOLEAN, p_motivo TEXT DEFAULT NULL)
RETURNS VOID
LANGUAGE plpgsql
SECURITY DEFINER
SET search_path = ''
AS $$
DECLARE
    v_ahora  TIMESTAMPTZ := NOW();
    v_inicio TIMESTAMPTZ;
    v_dias   INT;
    v_motivo TEXT := NULLIF(TRIM(p_motivo), '');
BEGIN
    PERFORM public.exigir_admin();

    IF p_aprobar THEN
        SELECT COALESCE(pg.dias, c.dias) INTO v_dias
        FROM public.config_pagos c
        LEFT JOIN public.pagos_destacado pg ON pg.id_solicitud_destacado = p_id_solicitud
        WHERE c.id = 1;

        SELECT GREATEST(v_ahora, COALESCE(MAX(vigente.fecha_fin_destacado), v_ahora))
        INTO v_inicio
        FROM public.solicitudes_destacado s
        LEFT JOIN public.solicitudes_destacado vigente
               ON vigente.id_publicacion = s.id_publicacion
              AND vigente.estado_solicitud = 'aprobada'
              AND vigente.fecha_fin_destacado > v_ahora
        WHERE s.id_solicitud_destacado = p_id_solicitud;

        UPDATE public.solicitudes_destacado
        SET estado_solicitud = 'aprobada', id_admin_resuelve = auth.uid(),
            fecha_resolucion = v_ahora, fecha_inicio_destacado = v_inicio,
            fecha_fin_destacado = v_inicio + make_interval(days => COALESCE(v_dias, 30))
        WHERE id_solicitud_destacado = p_id_solicitud AND estado_solicitud = 'pendiente';
    ELSE
        v_motivo := public.exigir_motivo(p_motivo, 'rechaza', 1);

        UPDATE public.solicitudes_destacado
        SET estado_solicitud = 'rechazada', id_admin_resuelve = auth.uid(),
            fecha_resolucion = v_ahora, motivo_rechazo = v_motivo
        WHERE id_solicitud_destacado = p_id_solicitud AND estado_solicitud = 'pendiente';
    END IF;

    IF NOT FOUND THEN
        RAISE EXCEPTION 'La solicitud no existe o ya fue revisada.';
    END IF;

    INSERT INTO public.acciones_administrativas (id_admin, tipo_accion, motivo, id_solicitud_destacado_objetivo)
    VALUES (auth.uid(),
            CASE WHEN p_aprobar THEN 'aprobar_destacado' ELSE 'rechazar_destacado' END,
            COALESCE(v_motivo, 'Solicitud aprobada.'),
            p_id_solicitud);
END;
$$;


-- ── Lista del admin: ahora con el pago ─────────────────────────────────────

DROP FUNCTION public.admin_listar_destacados();

-- monto, referencia y comprobante son NULL en las solicitudes de prueba (sin pago).
CREATE FUNCTION public.admin_listar_destacados()
RETURNS TABLE (
    id_solicitud_destacado BIGINT,
    id_publicacion         BIGINT,
    estado_solicitud       TEXT,
    fecha_solicitud        TIMESTAMPTZ,
    motivo_rechazo         TEXT,
    solicitante            TEXT,
    solicitante_foto       TEXT,
    monto                  NUMERIC,
    referencia             TEXT,
    comprobante_path       TEXT
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
           s.fecha_solicitud, s.motivo_rechazo, c.nombre_mostrar::TEXT, c.foto_perfil,
           pg.monto, pg.referencia::TEXT, pg.comprobante_path
    FROM public.solicitudes_destacado s
    JOIN public.publicaciones p ON p.id_publicacion = s.id_publicacion
    JOIN public.cuentas c       ON c.id_cuenta = p.id_propietario
    LEFT JOIN public.pagos_destacado pg ON pg.id_solicitud_destacado = s.id_solicitud_destacado
    WHERE s.estado_solicitud <> 'cancelada'
    ORDER BY s.fecha_solicitud DESC;
END;
$$;

REVOKE ALL ON FUNCTION public.admin_listar_destacados() FROM PUBLIC, anon;
GRANT EXECUTE ON FUNCTION public.admin_listar_destacados() TO authenticated;


-- ── Aviso a los admins cuando llega un pago ────────────────────────────────

ALTER TABLE public.notificaciones DROP CONSTRAINT chk_notificaciones_tipo;
ALTER TABLE public.notificaciones ADD CONSTRAINT chk_notificaciones_tipo
    CHECK (tipo IN ('favorito', 'propuesta_enviada', 'publicacion_aprobada', 'destacado_aprobado',
                    'publicacion_rechazada', 'edicion_aprobada', 'edicion_rechazada',
                    'publicacion_deshabilitada', 'publicacion_rehabilitada',
                    'destacado_rechazado', 'reporte_atendido', 'reporte_descartado',
                    'destacado_por_vencer', 'cuenta_suspendida', 'cuenta_reactivada',
                    'busqueda_coincidencia',
                    'precio_bajo', 'favorito_no_disponible', 'resumen_semanal', 'auto_parecido',
                    'resumen_vendedor', 'hito_vistas', 'poca_actividad', 'confirmar_venta',
                    'verificacion_pendiente', 'verificacion_aprobada', 'verificacion_rechazada',
                    'pago_destacado'));

-- A cada admin (menos el que pidió, si es admin): llegó un pago de destacado por revisar.
CREATE FUNCTION public.fn_avisar_pago_destacado()
RETURNS TRIGGER
LANGUAGE plpgsql
SECURITY DEFINER
SET search_path = ''
AS $$
DECLARE
    v_pub    BIGINT;
    v_titulo TEXT;
    v_dueno  UUID;
    r RECORD;
BEGIN
    SELECT p.id_publicacion, p.titulo, p.id_propietario INTO v_pub, v_titulo, v_dueno
    FROM public.solicitudes_destacado s
    JOIN public.publicaciones p ON p.id_publicacion = s.id_publicacion
    WHERE s.id_solicitud_destacado = NEW.id_solicitud_destacado;

    FOR r IN SELECT DISTINCT cr.id_cuenta
             FROM public.cuentas_roles cr
             JOIN public.roles ro ON ro.id_rol = cr.id_rol
             WHERE ro.nombre_rol = 'administrador' AND cr.id_cuenta <> v_dueno LOOP
        PERFORM public.crear_aviso(r.id_cuenta, 'pago_destacado', v_pub, v_titulo, NULL);
    END LOOP;
    RETURN NULL;
END;
$$;

REVOKE EXECUTE ON FUNCTION public.fn_avisar_pago_destacado() FROM PUBLIC, anon, authenticated;

CREATE TRIGGER trg_pagos_destacado_avisar
    AFTER INSERT ON public.pagos_destacado
    FOR EACH ROW EXECUTE FUNCTION public.fn_avisar_pago_destacado();


-- ── Comprobantes que ya se pueden borrar ───────────────────────────────────

-- Se guardan mientras la solicitud está pendiente, mientras dura el destacado aprobado y,
-- si se rechazó, hasta que el lote suba otro para esa publicación o pasen 30 días.
-- También los subidos hace más de un día que ninguna solicitud usa (se subió y no se envió).
-- El margen de 24 h evita borrar uno que se está enviando justo mientras el admin limpia.
CREATE FUNCTION public.admin_comprobantes_sin_uso()
RETURNS TEXT[]
LANGUAGE plpgsql
STABLE
SECURITY DEFINER
SET search_path = ''
AS $$
BEGIN
    PERFORM public.exigir_admin();

    RETURN ARRAY(
        SELECT o.name FROM storage.objects o
        WHERE o.bucket_id = 'comprobantes-pago'
          AND o.created_at < NOW() - INTERVAL '24 hours'
          AND NOT EXISTS (
              SELECT 1 FROM public.pagos_destacado pg
              JOIN public.solicitudes_destacado s ON s.id_solicitud_destacado = pg.id_solicitud_destacado
              WHERE pg.comprobante_path = o.name
                AND (s.estado_solicitud = 'pendiente'
                     OR (s.estado_solicitud = 'aprobada' AND s.fecha_fin_destacado > NOW())
                     OR (s.estado_solicitud = 'rechazada'
                         AND s.fecha_resolucion > NOW() - INTERVAL '30 days'
                         AND NOT EXISTS (SELECT 1 FROM public.solicitudes_destacado nueva
                                         WHERE nueva.id_publicacion = s.id_publicacion
                                           AND nueva.fecha_solicitud > s.fecha_solicitud)))
          )
        LIMIT 500
    );
END;
$$;

REVOKE ALL ON FUNCTION public.admin_comprobantes_sin_uso() FROM PUBLIC, anon;
GRANT EXECUTE ON FUNCTION public.admin_comprobantes_sin_uso() TO authenticated;


-- ── "Tu destacado termina pronto, renuévalo": solo a lotes ─────────────────

-- Igual que en 20261011120000, pero sin avisar a particulares: ya no pueden renovar.
CREATE OR REPLACE FUNCTION public.notificar_destacados_por_vencer()
RETURNS INT
LANGUAGE plpgsql
SECURITY DEFINER
SET search_path = ''
AS $$
DECLARE
    v_total INT := 0;
    r RECORD;
BEGIN
    FOR r IN
        SELECT s.id_solicitud_destacado, p.id_publicacion, p.id_propietario, p.titulo
        FROM public.solicitudes_destacado s
        JOIN public.publicaciones p ON p.id_publicacion = s.id_publicacion
        JOIN public.cuentas c ON c.id_cuenta = p.id_propietario
        WHERE s.estado_solicitud = 'aprobada'
          AND NOT s.aviso_vencimiento_enviado
          AND c.tipo_cuenta = 'lote'
          -- Un auto vendido o pausado no necesita renovar su destacado.
          AND p.estado_publicacion = 'activa'
          AND p.estado_administrativo = 'habilitada'
          AND s.fecha_fin_destacado > NOW()
          AND s.fecha_fin_destacado <= NOW() + INTERVAL '3 days'
          AND NOT EXISTS (
              SELECT 1 FROM public.solicitudes_destacado o
              WHERE o.id_publicacion = s.id_publicacion
                AND o.id_solicitud_destacado <> s.id_solicitud_destacado
                AND (o.estado_solicitud = 'pendiente'
                     OR (o.estado_solicitud = 'aprobada' AND o.fecha_fin_destacado > s.fecha_fin_destacado))
          )
    LOOP
        INSERT INTO public.notificaciones (id_cuenta, tipo, id_publicacion, titulo_vehiculo)
        VALUES (r.id_propietario, 'destacado_por_vencer', r.id_publicacion, r.titulo);

        UPDATE public.solicitudes_destacado
        SET aviso_vencimiento_enviado = TRUE
        WHERE id_solicitud_destacado = r.id_solicitud_destacado;

        v_total := v_total + 1;
    END LOOP;
    RETURN v_total;
END;
$$;

-- ── Solicitudes pendientes de particulares ────────────────────────────────
-- Destacar ahora es solo para lotes: las solicitudes pendientes de particulares se borran
-- (aún no tienen acciones del admin ni pago). Los destacados ya aprobados siguen hasta su fin.
DELETE FROM public.solicitudes_destacado s
USING public.publicaciones p, public.cuentas c
WHERE s.id_publicacion = p.id_publicacion
  AND c.id_cuenta = p.id_propietario
  AND c.tipo_cuenta <> 'lote'
  AND s.estado_solicitud = 'pendiente';
