-- ############################################################################
-- Avisar al usuario cuando el admin suspende o reactiva su cuenta
-- ############################################################################
--   cuenta_suspendida  "Tu cuenta fue suspendida" + motivo del admin
--   cuenta_reactivada  "Tu cuenta fue reactivada, ya puedes volver a entrar"
-- La cuenta suspendida no puede abrir la bandeja, pero el aviso le llega por push
-- (su teléfono sigue registrado en dispositivos_push).
--
-- Se dispara con la bitácora (acciones_administrativas) y no con el cambio en
-- cuentas: admin_cambiar_estado_cuenta actualiza la cuenta antes de guardar el
-- motivo, así que solo en la bitácora están juntos la cuenta y el motivo.


ALTER TABLE public.notificaciones DROP CONSTRAINT chk_notificaciones_tipo;
ALTER TABLE public.notificaciones ADD CONSTRAINT chk_notificaciones_tipo
    CHECK (tipo IN ('favorito', 'propuesta_enviada', 'publicacion_aprobada', 'destacado_aprobado',
                    'publicacion_rechazada', 'edicion_aprobada', 'edicion_rechazada',
                    'publicacion_deshabilitada', 'publicacion_rehabilitada',
                    'destacado_rechazado', 'reporte_atendido', 'reporte_descartado',
                    'destacado_por_vencer', 'cuenta_suspendida', 'cuenta_reactivada'));


CREATE FUNCTION public.fn_notificar_estado_cuenta()
RETURNS TRIGGER
LANGUAGE plpgsql
SECURITY DEFINER
SET search_path = ''
AS $$
BEGIN
    IF NEW.id_cuenta_objetivo IS NULL
       OR NEW.tipo_accion NOT IN ('suspender_cuenta', 'reactivar_cuenta') THEN
        RETURN NULL;
    END IF;

    INSERT INTO public.notificaciones (id_cuenta, tipo, motivo)
    VALUES (NEW.id_cuenta_objetivo,
            CASE NEW.tipo_accion WHEN 'suspender_cuenta' THEN 'cuenta_suspendida' ELSE 'cuenta_reactivada' END,
            -- El motivo solo se comparte al suspender (al reactivar es un texto interno).
            CASE WHEN NEW.tipo_accion = 'suspender_cuenta' THEN NEW.motivo END);
    RETURN NULL;
END;
$$;

CREATE TRIGGER trg_acciones_notificar_estado_cuenta
    AFTER INSERT ON public.acciones_administrativas
    FOR EACH ROW EXECUTE FUNCTION public.fn_notificar_estado_cuenta();
