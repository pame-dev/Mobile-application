-- ############################################################################
-- Avisos de verificación de cuentas (CURP de particulares y constancia de lotes)
-- ############################################################################
--   verificacion_pendiente  A cada admin: llegó una solicitud por revisar (nueva o
--                           reenviada después de un rechazo). datos.documento dice cuál.
--   verificacion_aprobada   Al usuario: su cuenta quedó verificada.
--   verificacion_rechazada  Al usuario: se rechazó, con el motivo del admin.
-- Se disparan con el cambio de estado en verificaciones_usuario, así cubren
-- enviar_curp_verificacion, enviar_documento_verificacion y
-- admin_resolver_verificacion_usuario sin tocarlas. El push sale solo (webhook de
-- notificaciones → enviar-push); la app y la web arman el texto.
-- Los avisos a los admins no dicen el nombre de quién la envió (el push pasa por Google).


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
                    'verificacion_pendiente', 'verificacion_aprobada', 'verificacion_rechazada'));


CREATE FUNCTION public.fn_avisar_verificacion()
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
                 WHERE ro.nombre_rol = 'administrador' AND cr.id_cuenta <> NEW.user_id LOOP
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

REVOKE EXECUTE ON FUNCTION public.fn_avisar_verificacion() FROM PUBLIC, anon, authenticated;

CREATE TRIGGER trg_verificaciones_usuario_avisar
    AFTER INSERT OR UPDATE OF estado ON public.verificaciones_usuario
    FOR EACH ROW EXECUTE FUNCTION public.fn_avisar_verificacion();
