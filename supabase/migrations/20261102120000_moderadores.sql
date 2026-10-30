-- ############################################################################
-- Moderadores: dos roles con acceso a una parte del panel de administración
-- ############################################################################
--   administrador             Súper admin: todo, como hasta ahora (Inicio,
--                             estadísticas, reporte y datos de pago solo él).
--   moderador_publicaciones   Vehículos (aprobar propuestas y revisar el documento
--                             del vehículo), Reportes y Usuarios.
--   moderador_verificaciones  Destacados (pagos) y Verificaciones (CURP del
--                             particular y constancia del lote).
--
-- Un moderador es una cuenta normal (particular o lote) con el rol extra en
-- cuentas_roles. Se asigna a mano en el SQL Editor (no hay pantalla), p. ej.:
--   INSERT INTO public.cuentas_roles (id_cuenta, id_rol)
--   SELECT u.id, r.id_rol FROM auth.users u, public.roles r
--   WHERE u.email = 'persona@correo.com' AND r.nombre_rol = 'moderador_publicaciones';
--
-- Cada función admin_* de esas áreas pasa de exigir_admin() a
-- exigir_moderador('<rol>'), que también deja pasar al administrador. El cuerpo
-- de cada una es el de su última versión; solo cambia esa línea (y en
-- admin_cambiar_estado_cuenta, que un moderador no suspende a otro miembro del
-- equipo). Las políticas RLS de esas tablas y buckets se abren igual.


-- ── Roles ──────────────────────────────────────────────────────────────────

INSERT INTO public.roles (nombre_rol, descripcion_rol) VALUES
    ('moderador_publicaciones',  'Revisa publicaciones (y el documento del vehículo), reportes y usuarios.'),
    ('moderador_verificaciones', 'Revisa pagos de destacados y verificaciones de identidad y de lote.');


-- ── Funciones de permisos ──────────────────────────────────────────────────

-- ¿La cuenta en sesión es administrador o tiene el rol de moderador p_rol?
CREATE FUNCTION public.es_moderador_actual(p_rol TEXT)
RETURNS BOOLEAN
LANGUAGE sql
STABLE
SECURITY DEFINER
SET search_path = ''
AS $$
    SELECT EXISTS (
        SELECT 1
        FROM public.cuentas_roles cr
        JOIN public.roles r ON r.id_rol = cr.id_rol
        WHERE cr.id_cuenta = auth.uid()
          AND r.nombre_rol IN ('administrador', p_rol)
    );
$$;

-- Lanza error si quien llama no es administrador ni moderador p_rol.
CREATE FUNCTION public.exigir_moderador(p_rol TEXT)
RETURNS VOID
LANGUAGE plpgsql
STABLE
SECURITY DEFINER
SET search_path = ''
AS $$
BEGIN
    IF NOT public.es_moderador_actual(p_rol) THEN
        RAISE EXCEPTION 'No tienes permiso para realizar esta acción.';
    END IF;
END;
$$;

-- ¿La cuenta es del equipo (administrador o cualquier moderador)?
CREATE FUNCTION public.es_personal(p_id_cuenta UUID)
RETURNS BOOLEAN
LANGUAGE sql
STABLE
SECURITY DEFINER
SET search_path = ''
AS $$
    SELECT EXISTS (
        SELECT 1
        FROM public.cuentas_roles cr
        JOIN public.roles r ON r.id_rol = cr.id_rol
        WHERE cr.id_cuenta = p_id_cuenta
          AND r.nombre_rol IN ('administrador', 'moderador_publicaciones', 'moderador_verificaciones')
    );
$$;

-- Rol de panel de la cuenta en sesión (la app y la web deciden qué secciones mostrar):
-- 'administrador', 'moderador_publicaciones', 'moderador_verificaciones' o NULL.
-- Si tuviera varios, gana administrador.
CREATE FUNCTION public.rol_panel()
RETURNS TEXT
LANGUAGE sql
STABLE
SECURITY DEFINER
SET search_path = ''
AS $$
    SELECT r.nombre_rol::TEXT
    FROM public.cuentas_roles cr
    JOIN public.roles r ON r.id_rol = cr.id_rol
    WHERE cr.id_cuenta = auth.uid()
      AND r.nombre_rol IN ('administrador', 'moderador_publicaciones', 'moderador_verificaciones')
    ORDER BY r.nombre_rol <> 'administrador', r.nombre_rol
    LIMIT 1;
$$;

REVOKE ALL ON FUNCTION public.exigir_moderador(TEXT) FROM PUBLIC, anon;
GRANT EXECUTE ON FUNCTION public.exigir_moderador(TEXT) TO authenticated;
REVOKE ALL ON FUNCTION public.rol_panel() FROM PUBLIC, anon;
GRANT EXECUTE ON FUNCTION public.rol_panel() TO authenticated;
-- es_moderador_actual y es_personal quedan como es_admin_actual / es_administrador:
-- las usan políticas que también evalúa anon.


-- ── Revisor / resolutor: basta con ser del equipo ─────────────────────────
-- (id_admin_revisor, id_admin_resolutor, id_admin_resuelve, id_admin de la bitácora)

CREATE OR REPLACE FUNCTION public.fn_validar_admin_columna()
RETURNS TRIGGER
LANGUAGE plpgsql
AS $$
DECLARE
    v_id_admin UUID;
BEGIN
    -- TG_ARGV[0] = nombre de la columna que contiene al administrador.
    v_id_admin := (to_jsonb(NEW) ->> TG_ARGV[0])::UUID;

    IF v_id_admin IS NOT NULL AND NOT public.es_personal(v_id_admin) THEN
        RAISE EXCEPTION 'La cuenta indicada en % no es administrador ni moderador.', TG_ARGV[0];
    END IF;
    RETURN NEW;
END;
$$;


-- ── Avisos de verificación y de pagos: también al moderador de verificaciones ──

CREATE OR REPLACE FUNCTION public.fn_avisar_verificacion()
RETURNS TRIGGER
LANGUAGE plpgsql
SECURITY DEFINER
SET search_path = ''
AS $$
DECLARE
    v_datos JSONB := jsonb_build_object('documento', NEW.tipo_documento);
    r RECORD;
BEGIN
    -- Llegó una solicitud (primera vez, o reenviada después de un rechazo): a los admins.
    -- crear_aviso respeta el horario de silencio (de noche llega a la bandeja sin push).
    IF NEW.estado = 'pendiente' AND (TG_OP = 'INSERT' OR OLD.estado IS DISTINCT FROM 'pendiente') THEN
        FOR r IN SELECT DISTINCT cr.id_cuenta
                 FROM public.cuentas_roles cr
                 JOIN public.roles ro ON ro.id_rol = cr.id_rol
                 WHERE ro.nombre_rol IN ('administrador', 'moderador_verificaciones') AND cr.id_cuenta <> NEW.user_id LOOP
            PERFORM public.crear_aviso(r.id_cuenta, 'verificacion_pendiente', NULL, NULL, v_datos);
        END LOOP;

    -- El admin la resolvió: al usuario, con el motivo si se rechazó.
    ELSIF TG_OP = 'UPDATE' AND OLD.estado = 'pendiente' AND NEW.estado IN ('verificado', 'rechazado') THEN
        INSERT INTO public.notificaciones (id_cuenta, tipo, motivo, datos)
        VALUES (NEW.user_id,
                CASE NEW.estado WHEN 'verificado' THEN 'verificacion_aprobada' ELSE 'verificacion_rechazada' END,
                CASE WHEN NEW.estado = 'rechazado' THEN NEW.motivo_rechazo END,
                v_datos);
    END IF;
    RETURN NULL;
END;
$$;

CREATE OR REPLACE FUNCTION public.fn_avisar_pago_destacado()
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
             WHERE ro.nombre_rol IN ('administrador', 'moderador_verificaciones') AND cr.id_cuenta <> v_dueno LOOP
        PERFORM public.crear_aviso(r.id_cuenta, 'pago_destacado', v_pub, v_titulo, NULL);
    END LOOP;
    RETURN NULL;
END;
$$;


-- ############################################################################
-- moderador_publicaciones: Vehículos, Reportes y Usuarios
-- ############################################################################

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
    PERFORM public.exigir_moderador('moderador_publicaciones');

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

CREATE OR REPLACE FUNCTION public.admin_cambiar_estado_publicacion(p_id_publicacion BIGINT, p_habilitar BOOLEAN, p_motivo TEXT DEFAULT NULL)
RETURNS VOID
LANGUAGE plpgsql
SECURITY DEFINER
SET search_path = ''
AS $$
DECLARE
    v_motivo TEXT := NULLIF(TRIM(p_motivo), '');
BEGIN
    PERFORM public.exigir_moderador('moderador_publicaciones');

    IF NOT p_habilitar AND COALESCE(CHAR_LENGTH(v_motivo), 0) < 10 THEN
        RAISE EXCEPTION 'Escribe un comentario de al menos 10 caracteres explicando por qué se deshabilita.';
    END IF;

    UPDATE public.publicaciones
    SET estado_administrativo  = CASE WHEN p_habilitar THEN 'habilitada' ELSE 'deshabilitada_administrador' END,
        motivo_deshabilitacion = CASE WHEN p_habilitar THEN NULL ELSE v_motivo END,
        fecha_deshabilitacion  = CASE WHEN p_habilitar THEN NULL ELSE NOW() END
    WHERE id_publicacion = p_id_publicacion;

    IF NOT FOUND THEN
        RAISE EXCEPTION 'La publicación no existe.';
    END IF;

    INSERT INTO public.acciones_administrativas (id_admin, tipo_accion, motivo, id_publicacion_objetivo)
    VALUES (auth.uid(),
            CASE WHEN p_habilitar THEN 'habilitar_publicacion' ELSE 'deshabilitar_publicacion' END,
            COALESCE(v_motivo, 'Sin motivo especificado.'),
            p_id_publicacion);
END;
$$;

CREATE OR REPLACE FUNCTION public.admin_marcar_vistas(p_estado TEXT)
RETURNS VOID
LANGUAGE plpgsql
SECURITY DEFINER
SET search_path = ''
AS $$
BEGIN
    PERFORM public.exigir_moderador('moderador_publicaciones');
    IF p_estado IS NULL OR p_estado NOT IN ('deshabilitada', 'pausada') THEN
        RAISE EXCEPTION 'Estado no válido.';
    END IF;

    DELETE FROM public.admin_publicaciones_vistas v
    WHERE v.id_admin = auth.uid() AND v.estado = p_estado
      AND v.id_publicacion NOT IN (SELECT e.id FROM public.admin_publicaciones_en_estado(p_estado) e(id));

    INSERT INTO public.admin_publicaciones_vistas (id_admin, estado, id_publicacion)
    SELECT auth.uid(), p_estado, e.id FROM public.admin_publicaciones_en_estado(p_estado) e(id)
    ON CONFLICT DO NOTHING;
END;
$$;

CREATE OR REPLACE FUNCTION public.admin_fotos_retiradas_sin_uso()
RETURNS TEXT[]
LANGUAGE plpgsql
STABLE
SECURITY DEFINER
SET search_path = ''
AS $$
BEGIN
    PERFORM public.exigir_moderador('moderador_publicaciones');

    RETURN ARRAY(
        SELECT DISTINCT f.ruta_storage
        FROM public.fotos_publicacion f
        WHERE NOT f.activa
          -- Solo archivos del bucket (las fotos de prueba con link externo no).
          AND f.ruta_storage LIKE 'publicaciones/%'
          AND NOT EXISTS (
              SELECT 1 FROM public.fotos_publicacion a
              WHERE a.activa AND a.ruta_storage = f.ruta_storage)
          AND NOT EXISTS (
              SELECT 1
              FROM public.fotos_propuesta_publicacion fp
              JOIN public.propuestas_publicacion pr ON pr.id_propuesta = fp.id_propuesta
              WHERE fp.ruta_storage = f.ruta_storage
                AND pr.estado_propuesta IN ('pendiente', 'rechazada'))
          -- Las que ya se borraron no vuelven a salir.
          AND EXISTS (
              SELECT 1 FROM storage.objects o
              WHERE o.bucket_id = 'karsy' AND o.name = f.ruta_storage)
        LIMIT 100
    );
END;
$$;

CREATE OR REPLACE FUNCTION public.admin_publicaciones_sin_huellas()
RETURNS TABLE (id_publicacion BIGINT, fotos TEXT[])
LANGUAGE plpgsql
STABLE
SECURITY DEFINER
SET search_path = ''
AS $$
BEGIN
    PERFORM public.exigir_moderador('moderador_publicaciones');
    RETURN QUERY
    SELECT f.id_publicacion, array_agg(f.ruta_storage ORDER BY f.orden_foto)
    FROM public.fotos_publicacion f
    WHERE f.activa
      AND NOT EXISTS (SELECT 1 FROM public.huellas_fotos h WHERE h.id_publicacion = f.id_publicacion)
    GROUP BY f.id_publicacion
    ORDER BY f.id_publicacion;
END;
$$;

CREATE OR REPLACE FUNCTION public.admin_registrar_huellas(p_id_publicacion BIGINT, p_huellas TEXT[])
RETURNS VOID
LANGUAGE plpgsql
SECURITY DEFINER
SET search_path = ''
AS $$
BEGIN
    PERFORM public.exigir_moderador('moderador_publicaciones');
    IF NOT EXISTS (SELECT 1 FROM public.publicaciones p WHERE p.id_publicacion = p_id_publicacion) THEN
        RAISE EXCEPTION 'La publicación no existe.';
    END IF;
    IF COALESCE(cardinality(p_huellas), 0) > 15
       OR EXISTS (SELECT 1 FROM unnest(p_huellas) AS h WHERE h IS NULL OR h !~ '^[0-9a-f]{16}$') THEN
        RAISE EXCEPTION 'Huellas de fotos no válidas.';
    END IF;

    DELETE FROM public.huellas_fotos WHERE id_publicacion = p_id_publicacion;
    INSERT INTO public.huellas_fotos (id_publicacion, huella)
    SELECT DISTINCT p_id_publicacion, public.huella_de_texto(h)
    FROM unnest(p_huellas) AS h;
END;
$$;

CREATE OR REPLACE FUNCTION public.admin_listar_reportes()
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
    PERFORM public.exigir_moderador('moderador_publicaciones');

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

CREATE OR REPLACE FUNCTION public.admin_resolver_reporte(p_id_reporte BIGINT, p_estado TEXT, p_motivo TEXT, p_deshabilitar BOOLEAN DEFAULT FALSE)
RETURNS VOID
LANGUAGE plpgsql
SECURITY DEFINER
SET search_path = ''
AS $$
DECLARE
    v_id_publicacion BIGINT;
    v_motivo TEXT;
BEGIN
    PERFORM public.exigir_moderador('moderador_publicaciones');

    v_motivo := public.exigir_motivo(
        p_motivo,
        CASE WHEN p_deshabilitar THEN 'deshabilita'
             WHEN p_estado = 'descartado' THEN 'descarta el reporte'
             ELSE 'atiende el reporte' END);

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

        UPDATE public.reportes
        SET estado_reporte = 'atendido', id_admin_resolutor = auth.uid(),
            motivo_resolucion = v_motivo, fecha_resolucion = NOW()
        WHERE id_publicacion = v_id_publicacion AND estado_reporte = 'pendiente';
    END IF;
END;
$$;

CREATE OR REPLACE FUNCTION public.admin_listar_cuentas()
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
    activas           BIGINT,
    foto_perfil       TEXT,
    verificado        BOOLEAN
)
LANGUAGE plpgsql
STABLE
SECURITY DEFINER
SET search_path = ''
AS $$
BEGIN
    PERFORM public.exigir_moderador('moderador_publicaciones');

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
           AND p.estado_publicacion = 'activa' AND p.estado_administrativo = 'habilitada'),
        c.foto_perfil::TEXT,
        c.verificado
    FROM public.cuentas c
    JOIN auth.users u ON u.id = c.id_cuenta
    ORDER BY c.fecha_creacion DESC;
END;
$$;

CREATE OR REPLACE FUNCTION public.admin_marcar_suspendidas_vistas()
RETURNS VOID
LANGUAGE plpgsql
SECURITY DEFINER
SET search_path = ''
AS $$
BEGIN
    PERFORM public.exigir_moderador('moderador_publicaciones');

    INSERT INTO public.admin_cuentas_vistas (id_admin, id_cuenta)
    SELECT auth.uid(), c.id_cuenta FROM public.cuentas c WHERE c.estado_cuenta = 'suspendida'
    ON CONFLICT DO NOTHING;
END;
$$;

CREATE OR REPLACE FUNCTION public.admin_cambiar_estado_cuenta(p_id_cuenta UUID, p_suspender BOOLEAN, p_motivo TEXT DEFAULT NULL)
RETURNS VOID
LANGUAGE plpgsql
SECURITY DEFINER
SET search_path = ''
AS $$
DECLARE
    v_motivo TEXT := NULLIF(TRIM(p_motivo), '');
BEGIN
    PERFORM public.exigir_moderador('moderador_publicaciones');

    IF p_id_cuenta = auth.uid() THEN
        RAISE EXCEPTION 'No puedes suspender tu propia cuenta.';
    END IF;

    -- Un moderador no suspende ni reactiva al administrador ni a otro moderador.
    IF NOT public.es_admin_actual() AND public.es_personal(p_id_cuenta) THEN
        RAISE EXCEPTION 'Solo el administrador puede cambiar el estado de una cuenta del equipo.';
    END IF;

    IF p_suspender THEN
        v_motivo := public.exigir_motivo(p_motivo, 'suspende');
    END IF;

    UPDATE public.cuentas
    SET estado_cuenta = CASE WHEN p_suspender THEN 'suspendida' ELSE 'activa' END
    WHERE id_cuenta = p_id_cuenta;

    IF NOT FOUND THEN
        RAISE EXCEPTION 'La cuenta no existe.';
    END IF;

    INSERT INTO public.acciones_administrativas (id_admin, tipo_accion, motivo, id_cuenta_objetivo)
    VALUES (auth.uid(),
            CASE WHEN p_suspender THEN 'suspender_cuenta' ELSE 'reactivar_cuenta' END,
            COALESCE(v_motivo, 'Sin motivo especificado.'),
            p_id_cuenta);
END;
$$;


-- ############################################################################
-- moderador_verificaciones: Destacados y Verificaciones
-- ############################################################################

CREATE OR REPLACE FUNCTION public.admin_listar_destacados()
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
    PERFORM public.exigir_moderador('moderador_verificaciones');

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
    PERFORM public.exigir_moderador('moderador_verificaciones');

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

CREATE OR REPLACE FUNCTION public.admin_comprobantes_sin_uso()
RETURNS TEXT[]
LANGUAGE plpgsql
STABLE
SECURITY DEFINER
SET search_path = ''
AS $$
BEGIN
    PERFORM public.exigir_moderador('moderador_verificaciones');

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

CREATE OR REPLACE FUNCTION public.admin_listar_verificaciones_usuario()
RETURNS TABLE (
    id              UUID,
    user_id         UUID,
    nombre_mostrar  TEXT,
    correo          TEXT,
    tipo_usuario    TEXT,
    tipo_documento  TEXT,
    archivo_path    TEXT,
    curp            TEXT,
    estado          TEXT,
    motivo_rechazo  TEXT,
    revisado_por    TEXT,
    fecha_envio     TIMESTAMPTZ,
    fecha_revision  TIMESTAMPTZ
)
LANGUAGE plpgsql
STABLE
SECURITY DEFINER
SET search_path = ''
AS $$
BEGIN
    PERFORM public.exigir_moderador('moderador_verificaciones');

    RETURN QUERY
    SELECT v.id, v.user_id, c.nombre_mostrar::TEXT, u.email::TEXT, v.tipo_usuario, v.tipo_documento,
           v.archivo_path, v.curp, v.estado, v.motivo_rechazo, adm.nombre_mostrar::TEXT, v.fecha_envio, v.fecha_revision
    FROM public.verificaciones_usuario v
    JOIN public.cuentas c ON c.id_cuenta = v.user_id
    JOIN auth.users u ON u.id = v.user_id
    LEFT JOIN public.cuentas adm ON adm.id_cuenta = v.revisado_por
    ORDER BY (v.estado = 'pendiente') DESC, v.fecha_envio DESC;
END;
$$;

CREATE OR REPLACE FUNCTION public.admin_resolver_verificacion_usuario(p_id UUID, p_aprobar BOOLEAN, p_motivo TEXT DEFAULT NULL)
RETURNS VOID
LANGUAGE plpgsql
SECURITY DEFINER
SET search_path = ''
AS $$
DECLARE
    v_motivo  TEXT := NULLIF(TRIM(p_motivo), '');
    v_usuario UUID;
BEGIN
    PERFORM public.exigir_moderador('moderador_verificaciones');

    IF NOT p_aprobar THEN
        v_motivo := public.exigir_motivo(p_motivo, 'rechaza');
        IF CHAR_LENGTH(v_motivo) > 1000 THEN
            RAISE EXCEPTION 'El motivo puede tener como máximo 1000 caracteres.';
        END IF;
    END IF;

    UPDATE public.verificaciones_usuario
    SET estado = CASE WHEN p_aprobar THEN 'verificado' ELSE 'rechazado' END,
        motivo_rechazo = CASE WHEN p_aprobar THEN NULL ELSE v_motivo END,
        revisado_por = auth.uid(), fecha_revision = NOW()
    WHERE id = p_id AND estado = 'pendiente'
    RETURNING user_id INTO v_usuario;

    IF v_usuario IS NULL THEN
        RAISE EXCEPTION 'Este documento no existe o ya fue revisado.';
    END IF;

    -- Aprobado: insignia. Rechazado: la pierde, salvo que tenga otro documento verificado
    -- (la fila rechazada ya cambió de estado arriba, así que no se cuenta a sí misma).
    UPDATE public.cuentas
    SET verificado = p_aprobar OR EXISTS (
            SELECT 1 FROM public.verificaciones_usuario v WHERE v.user_id = v_usuario AND v.estado = 'verificado')
    WHERE id_cuenta = v_usuario;

    INSERT INTO public.acciones_administrativas (id_admin, tipo_accion, motivo, id_cuenta_objetivo)
    VALUES (auth.uid(),
            CASE WHEN p_aprobar THEN 'aprobar_verificacion' ELSE 'rechazar_verificacion' END,
            COALESCE(v_motivo, 'Documento de verificación aprobado.'),
            v_usuario);
END;
$$;

CREATE OR REPLACE FUNCTION public.admin_documentos_verificacion_sin_uso()
RETURNS TEXT[]
LANGUAGE plpgsql
STABLE
SECURITY DEFINER
SET search_path = ''
AS $$
BEGIN
    PERFORM public.exigir_moderador('moderador_verificaciones');

    RETURN ARRAY(
        SELECT o.name FROM storage.objects o
        WHERE o.bucket_id = 'constancias-fiscales'
          AND o.created_at < NOW() - INTERVAL '1 hour'
          AND NOT EXISTS (SELECT 1 FROM public.verificaciones_usuario v WHERE v.archivo_path = o.name)
        LIMIT 500
    );
END;
$$;


-- ############################################################################
-- Políticas RLS: lo que el panel lee directo de tablas y buckets
-- ############################################################################
-- Las demás (interacciones, bitácora, cuentas_roles, teléfonos) siguen solo para el admin.

-- Todo el equipo ve todas las publicaciones (los destacados también las muestran).
DROP POLICY publicaciones_leer ON public.publicaciones;
CREATE POLICY publicaciones_leer ON public.publicaciones
    FOR SELECT TO anon, authenticated
    USING (
        (fecha_primera_publicacion IS NOT NULL
            AND estado_administrativo = 'habilitada'
            AND estado_publicacion IN ('activa', 'vendida'))
        OR id_propietario = auth.uid()
        OR public.es_personal(auth.uid())
    );

DROP POLICY propuestas_leer ON public.propuestas_publicacion;
CREATE POLICY propuestas_leer ON public.propuestas_publicacion
    FOR SELECT TO authenticated
    USING (public.es_propietario_publicacion(id_publicacion)
        OR public.es_moderador_actual('moderador_publicaciones'));

DROP POLICY fotos_propuesta_leer ON public.fotos_propuesta_publicacion;
CREATE POLICY fotos_propuesta_leer ON public.fotos_propuesta_publicacion
    FOR SELECT TO authenticated
    USING (public.es_propietario_propuesta(id_propuesta)
        OR public.es_moderador_actual('moderador_publicaciones'));

DROP POLICY reportes_leer ON public.reportes;
CREATE POLICY reportes_leer ON public.reportes
    FOR SELECT TO authenticated
    USING (id_reportante = auth.uid() OR public.es_moderador_actual('moderador_publicaciones'));

DROP POLICY karsy_ver_admin ON storage.objects;
CREATE POLICY karsy_ver_admin ON storage.objects
    FOR SELECT TO authenticated
    USING (bucket_id = 'karsy' AND public.es_moderador_actual('moderador_publicaciones'));
DROP POLICY karsy_borrar_admin ON storage.objects;
CREATE POLICY karsy_borrar_admin ON storage.objects
    FOR DELETE TO authenticated
    USING (bucket_id = 'karsy' AND public.es_moderador_actual('moderador_publicaciones'));

DROP POLICY documentos_vehiculo_leer_admin ON storage.objects;
CREATE POLICY documentos_vehiculo_leer_admin ON storage.objects FOR SELECT TO authenticated
    USING (bucket_id = 'documentos-vehiculo' AND public.es_moderador_actual('moderador_publicaciones'));

DROP POLICY solicitudes_leer ON public.solicitudes_destacado;
CREATE POLICY solicitudes_leer ON public.solicitudes_destacado
    FOR SELECT TO anon, authenticated
    USING (
        estado_solicitud = 'aprobada'
        OR public.es_propietario_publicacion(id_publicacion)
        OR public.es_moderador_actual('moderador_verificaciones')
    );

DROP POLICY pagos_destacado_leer ON public.pagos_destacado;
CREATE POLICY pagos_destacado_leer ON public.pagos_destacado FOR SELECT TO authenticated
    USING (
        public.es_moderador_actual('moderador_verificaciones')
        OR EXISTS (SELECT 1 FROM public.solicitudes_destacado s
                   WHERE s.id_solicitud_destacado = pagos_destacado.id_solicitud_destacado
                     AND public.es_propietario_publicacion(s.id_publicacion))
    );

DROP POLICY comprobantes_pago_leer_admin ON storage.objects;
CREATE POLICY comprobantes_pago_leer_admin ON storage.objects FOR SELECT TO authenticated
    USING (bucket_id = 'comprobantes-pago' AND public.es_moderador_actual('moderador_verificaciones'));
DROP POLICY comprobantes_pago_borrar_admin ON storage.objects;
CREATE POLICY comprobantes_pago_borrar_admin ON storage.objects FOR DELETE TO authenticated
    USING (bucket_id = 'comprobantes-pago' AND public.es_moderador_actual('moderador_verificaciones'));

DROP POLICY verificaciones_usuario_leer_admin ON public.verificaciones_usuario;
CREATE POLICY verificaciones_usuario_leer_admin ON public.verificaciones_usuario FOR SELECT TO authenticated
    USING (public.es_moderador_actual('moderador_verificaciones'));

DROP POLICY constancias_leer_admin ON storage.objects;
CREATE POLICY constancias_leer_admin ON storage.objects FOR SELECT TO authenticated
    USING (bucket_id = 'constancias-fiscales' AND public.es_moderador_actual('moderador_verificaciones'));
DROP POLICY constancias_borrar_admin ON storage.objects;
CREATE POLICY constancias_borrar_admin ON storage.objects FOR DELETE TO authenticated
    USING (bucket_id = 'constancias-fiscales' AND public.es_moderador_actual('moderador_verificaciones'));
