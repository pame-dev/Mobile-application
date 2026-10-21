-- Ajuste a 20261026120000: al rechazar un documento, la cuenta solo pierde la insignia si no le
-- queda OTRO documento 'verificado' (p. ej. un particular verificado con CURP al que le rechazan
-- una constancia). Aprobar sigue poniendo verificado = TRUE.

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
    PERFORM public.exigir_admin();

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
