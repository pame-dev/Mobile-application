-- ############################################################################
-- Aviso de destacado por vencer y renovación sin perder días
-- ############################################################################
-- 1. Una vez al día (pg_cron) se avisa al dueño de cada destacado que termina en
--    los próximos 3 días: "Tu destacado termina pronto, renuévalo". El aviso llega
--    a la bandeja y por push (webhook de notificaciones → enviar-push).
-- 2. Al aprobar una renovación, el mes nuevo empieza cuando termina el destacado
--    vigente (antes empezaba al aprobar y se perdían los días que quedaban).


-- ----------------------------------------------------------------------------
-- Tipo de aviso nuevo y marca para avisar una sola vez por destacado
-- ----------------------------------------------------------------------------
ALTER TABLE public.notificaciones DROP CONSTRAINT chk_notificaciones_tipo;
ALTER TABLE public.notificaciones ADD CONSTRAINT chk_notificaciones_tipo
    CHECK (tipo IN ('favorito', 'propuesta_enviada', 'publicacion_aprobada', 'destacado_aprobado',
                    'publicacion_rechazada', 'edicion_aprobada', 'edicion_rechazada',
                    'publicacion_deshabilitada', 'publicacion_rehabilitada',
                    'destacado_rechazado', 'reporte_atendido', 'reporte_descartado',
                    'destacado_por_vencer'));

ALTER TABLE public.solicitudes_destacado
    ADD COLUMN aviso_vencimiento_enviado BOOLEAN NOT NULL DEFAULT FALSE;

COMMENT ON COLUMN public.solicitudes_destacado.aviso_vencimiento_enviado IS 'TRUE cuando ya se avisó al dueño que este destacado está por terminar.';

-- El trigger que impide modificar una solicitud ya resuelta se disparaba con
-- cualquier columna; ahora solo con las de la decisión, para poder marcar el aviso.
DROP TRIGGER trg_solicitudes_destacado_transicion ON public.solicitudes_destacado;
CREATE TRIGGER trg_solicitudes_destacado_transicion
    BEFORE UPDATE OF estado_solicitud, id_admin_resuelve, fecha_resolucion,
                     fecha_inicio_destacado, fecha_fin_destacado, fecha_cancelacion, motivo_rechazo
    ON public.solicitudes_destacado
    FOR EACH ROW EXECUTE FUNCTION public.fn_solicitudes_destacado_transicion();


-- ----------------------------------------------------------------------------
-- Avisar los destacados que terminan en los próximos 3 días
-- ----------------------------------------------------------------------------
-- No se avisa si ya hay una renovación aprobada (otro destacado que termina
-- después) o una solicitud pendiente: el dueño ya la pidió.
CREATE FUNCTION public.notificar_destacados_por_vencer()
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
        WHERE s.estado_solicitud = 'aprobada'
          AND NOT s.aviso_vencimiento_enviado
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

-- Solo la corre la tarea programada (no se expone a la app).
REVOKE EXECUTE ON FUNCTION public.notificar_destacados_por_vencer() FROM PUBLIC, anon, authenticated;


-- ----------------------------------------------------------------------------
-- Aprobar: si la publicación ya está destacada, el mes nuevo empieza al terminar
-- el vigente (renovación sin perder días).
-- ----------------------------------------------------------------------------
CREATE OR REPLACE FUNCTION public.admin_resolver_destacado(p_id_solicitud BIGINT, p_aprobar BOOLEAN, p_motivo TEXT DEFAULT NULL)
RETURNS VOID
LANGUAGE plpgsql
SECURITY DEFINER
SET search_path = ''
AS $$
DECLARE
    v_ahora  TIMESTAMPTZ := NOW();
    v_inicio TIMESTAMPTZ;
    v_motivo TEXT := NULLIF(TRIM(p_motivo), '');
BEGIN
    PERFORM public.exigir_admin();

    IF p_aprobar THEN
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
            fecha_fin_destacado = v_inicio + INTERVAL '1 month'
        WHERE id_solicitud_destacado = p_id_solicitud;
    ELSE
        v_motivo := public.exigir_motivo(p_motivo, 'rechaza', 1);

        UPDATE public.solicitudes_destacado
        SET estado_solicitud = 'rechazada', id_admin_resuelve = auth.uid(),
            fecha_resolucion = v_ahora, motivo_rechazo = v_motivo
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


-- ----------------------------------------------------------------------------
-- Tarea diaria: 15:00 UTC = 9:00 en el centro de México.
-- ----------------------------------------------------------------------------
CREATE EXTENSION IF NOT EXISTS pg_cron;

SELECT cron.schedule(
    'destacados-por-vencer',
    '0 15 * * *',
    $$SELECT public.notificar_destacados_por_vencer()$$
);
