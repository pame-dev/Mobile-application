-- ############################################################################
-- Ajustes a 20261111120000 (suspender una cuenta deshabilita sus publicaciones)
-- ############################################################################
-- 1. "Cuenta suspendida: <motivo>" se recorta a 1000 caracteres: es el límite de
--    motivo_deshabilitacion y el motivo de la suspensión puede medir hasta 1000.
-- 2. Aprobar una propuesta ya no rehabilita la publicación si la cuenta del dueño
--    está suspendida o si la deshabilitó la suspensión: mientras la cuenta esté
--    suspendida sus autos no se ven; al reactivarla regresan.


-- ── 1. Suspender / reactivar cuenta ────────────────────────────────────────
-- Cuerpo de 20261111120000; solo cambia el LEFT(…, 1000) del motivo.

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

    IF p_suspender THEN
        -- Sus publicaciones habilitadas (publicadas o en revisión) dejan de verse.
        UPDATE public.publicaciones
        SET estado_administrativo        = 'deshabilitada_administrador',
            motivo_deshabilitacion       = LEFT('Cuenta suspendida: ' || v_motivo, 1000),
            fecha_deshabilitacion        = NOW(),
            deshabilitada_por_suspension = TRUE
        WHERE id_propietario = p_id_cuenta
          AND estado_administrativo = 'habilitada';
    ELSE
        -- Regresan solo las que deshabilitó la suspensión.
        UPDATE public.publicaciones
        SET estado_administrativo        = 'habilitada',
            motivo_deshabilitacion       = NULL,
            fecha_deshabilitacion        = NULL,
            deshabilitada_por_suspension = FALSE
        WHERE id_propietario = p_id_cuenta
          AND deshabilitada_por_suspension;
    END IF;

    INSERT INTO public.acciones_administrativas (id_admin, tipo_accion, motivo, id_cuenta_objetivo)
    VALUES (auth.uid(),
            CASE WHEN p_suspender THEN 'suspender_cuenta' ELSE 'reactivar_cuenta' END,
            COALESCE(v_motivo, 'Sin motivo especificado.'),
            p_id_cuenta);
END;
$$;


-- ── 2. Aprobar una propuesta no rehabilita autos de cuentas suspendidas ────
-- Cuerpo de 20261007120000_aprobar_edicion_rehabilita.sql más las dos condiciones.

CREATE OR REPLACE FUNCTION public.fn_rehabilitar_al_aprobar_propuesta()
RETURNS TRIGGER
LANGUAGE plpgsql
SECURITY DEFINER
SET search_path = ''
AS $$
BEGIN
    IF NEW.estado_propuesta <> 'aprobada' OR OLD.estado_propuesta = 'aprobada' THEN
        RETURN NULL;
    END IF;

    IF EXISTS (
        SELECT 1
        FROM public.publicaciones p
        JOIN public.cuentas c ON c.id_cuenta = p.id_propietario
        WHERE p.id_publicacion = NEW.id_publicacion
          AND p.estado_administrativo = 'deshabilitada_administrador'
          -- La deshabilitó la suspensión: la regresa la reactivación de la cuenta.
          AND NOT p.deshabilitada_por_suspension
          -- Con la cuenta suspendida, sus autos siguen sin verse.
          AND c.estado_cuenta = 'activa'
    ) THEN
        -- Solo el equipo aprueba propuestas (admin_moderar_propuesta), así que esta
        -- llamada corre con su sesión y pasa exigir_moderador().
        PERFORM public.admin_cambiar_estado_publicacion(
            NEW.id_publicacion, TRUE, 'Rehabilitada al aprobar la edición corregida.'
        );
    END IF;
    RETURN NULL;
END;
$$;
