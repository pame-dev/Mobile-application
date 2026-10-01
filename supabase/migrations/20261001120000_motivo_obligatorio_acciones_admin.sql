-- ############################################################################
-- Motivo obligatorio en las acciones administrativas
-- ############################################################################
-- Antes solo deshabilitar una publicación exigía comentario; las demás acciones
-- guardaban 'Sin motivo especificado.' si el admin no escribía nada. Ahora:
--   * rechazar una propuesta, suspender una cuenta y resolver un reporte
--     (atenderlo o descartarlo) exigen un motivo de al menos 10 caracteres;
--   * rechazar una solicitud de destacado exige un motivo (la app lo elige de
--     una lista, así que basta con que no venga vacío).
-- Aprobar, habilitar y reactivar siguen aceptando el motivo opcional.

CREATE OR REPLACE FUNCTION public.exigir_motivo(p_motivo TEXT, p_accion TEXT, p_minimo INT DEFAULT 10)
RETURNS TEXT
LANGUAGE plpgsql
IMMUTABLE
SET search_path = ''
AS $$
DECLARE
    v_motivo TEXT := NULLIF(TRIM(p_motivo), '');
BEGIN
    IF COALESCE(CHAR_LENGTH(v_motivo), 0) < p_minimo THEN
        RAISE EXCEPTION 'Escribe un comentario de al menos % caracteres explicando por qué se %.', p_minimo, p_accion;
    END IF;
    RETURN v_motivo;
END;
$$;

REVOKE ALL ON FUNCTION public.exigir_motivo(TEXT, TEXT, INT) FROM PUBLIC, anon, authenticated;


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


CREATE OR REPLACE FUNCTION public.admin_cambiar_estado_cuenta(p_id_cuenta UUID, p_suspender BOOLEAN, p_motivo TEXT DEFAULT NULL)
RETURNS VOID
LANGUAGE plpgsql
SECURITY DEFINER
SET search_path = ''
AS $$
DECLARE
    v_motivo TEXT := NULLIF(TRIM(p_motivo), '');
BEGIN
    PERFORM public.exigir_admin();

    IF p_id_cuenta = auth.uid() THEN
        RAISE EXCEPTION 'No puedes suspender tu propia cuenta.';
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


-- Atender o descartar un reporte siempre exige motivo.
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
    PERFORM public.exigir_admin();

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
    v_inicio TIMESTAMPTZ := NOW();
    v_motivo TEXT := NULLIF(TRIM(p_motivo), '');
BEGIN
    PERFORM public.exigir_admin();

    IF p_aprobar THEN
        UPDATE public.solicitudes_destacado
        SET estado_solicitud = 'aprobada', id_admin_resuelve = auth.uid(),
            fecha_resolucion = v_inicio, fecha_inicio_destacado = v_inicio,
            fecha_fin_destacado = v_inicio + INTERVAL '1 month'
        WHERE id_solicitud_destacado = p_id_solicitud;
    ELSE
        v_motivo := public.exigir_motivo(p_motivo, 'rechaza', 1);

        UPDATE public.solicitudes_destacado
        SET estado_solicitud = 'rechazada', id_admin_resuelve = auth.uid(),
            fecha_resolucion = v_inicio, motivo_rechazo = v_motivo
        WHERE id_solicitud_destacado = p_id_solicitud;
    END IF;

    IF NOT FOUND THEN
        RAISE EXCEPTION 'La solicitud no existe.';
    END IF;

    INSERT INTO public.acciones_administrativas (id_admin, tipo_accion, motivo, id_solicitud_destacado_objetivo)
    VALUES (auth.uid(),
            CASE WHEN p_aprobar THEN 'aprobar_destacado' ELSE 'rechazar_destacado' END,
            COALESCE(v_motivo, 'Solicitud aprobada.'),
            p_id_solicitud);
END;
$$;
