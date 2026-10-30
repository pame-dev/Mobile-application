-- ############################################################################
-- Moderadores: ajustes a 20261102120000_moderadores.sql
-- ############################################################################
-- 1. Un moderador no resuelve lo suyo: su propia propuesta, reportes contra sus
--    publicaciones, destacados de sus publicaciones ni su propia verificación.
--    El administrador sí puede (como hasta ahora). Cada función es la de
--    20261102120000_moderadores.sql con un solo IF nuevo después de exigir_moderador.
-- 2. Borrar archivos directo en Storage vuelve a ser solo del administrador: un
--    moderador podía borrar fotos activas, comprobantes o constancias sin dejar
--    rastro en la bitácora. La limpieza de archivos sin uso la hace el admin al
--    abrir el panel (en la app y la web ya no truena si falla).


-- ── 1. Sin autoaprobación ──────────────────────────────────────────────────

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

    IF NOT public.es_admin_actual() AND public.es_propietario_publicacion(v_pr.id_publicacion) THEN
        RAISE EXCEPTION 'No puedes revisar tus propias publicaciones.';
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

    IF NOT public.es_admin_actual() AND EXISTS (
        SELECT 1 FROM public.reportes r
        WHERE r.id_reporte = p_id_reporte AND public.es_propietario_publicacion(r.id_publicacion)
    ) THEN
        RAISE EXCEPTION 'No puedes resolver reportes de tus propias publicaciones.';
    END IF;

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

    IF NOT public.es_admin_actual() AND EXISTS (
        SELECT 1 FROM public.solicitudes_destacado s
        WHERE s.id_solicitud_destacado = p_id_solicitud AND public.es_propietario_publicacion(s.id_publicacion)
    ) THEN
        RAISE EXCEPTION 'No puedes revisar el destacado de tus propias publicaciones.';
    END IF;

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

    IF NOT public.es_admin_actual() AND EXISTS (
        SELECT 1 FROM public.verificaciones_usuario v WHERE v.id = p_id AND v.user_id = auth.uid()
    ) THEN
        RAISE EXCEPTION 'No puedes revisar tu propia verificación.';
    END IF;

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


-- ── 2. Borrar en Storage: solo el administrador ────────────────────────────

DROP POLICY karsy_ver_admin ON storage.objects;
CREATE POLICY karsy_ver_admin ON storage.objects
    FOR SELECT TO authenticated
    USING (bucket_id = 'karsy' AND public.es_admin_actual());
DROP POLICY karsy_borrar_admin ON storage.objects;
CREATE POLICY karsy_borrar_admin ON storage.objects
    FOR DELETE TO authenticated
    USING (bucket_id = 'karsy' AND public.es_admin_actual());

DROP POLICY comprobantes_pago_borrar_admin ON storage.objects;
CREATE POLICY comprobantes_pago_borrar_admin ON storage.objects FOR DELETE TO authenticated
    USING (bucket_id = 'comprobantes-pago' AND public.es_admin_actual());

DROP POLICY constancias_borrar_admin ON storage.objects;
CREATE POLICY constancias_borrar_admin ON storage.objects FOR DELETE TO authenticated
    USING (bucket_id = 'constancias-fiscales' AND public.es_admin_actual());
