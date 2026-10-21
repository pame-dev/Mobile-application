-- El lote verificado puede actualizar (renovar) su constancia.
--
-- · enviar_documento_verificacion() ahora también acepta el reenvío cuando el documento está
--   'verificado' (antes solo sin documento o 'rechazado'). La misma fila pasa a 'pendiente' con el
--   archivo nuevo; cuentas.verificado NO cambia, así el lote conserva su insignia mientras se revisa.
--   Mientras está 'pendiente' sigue sin poder mandar otro.
-- · admin_resolver_verificacion_usuario(): al rechazar, cuentas.verificado = FALSE (pierde la insignia
--   hasta que suba un documento válido). Aprobar sigue igual (verificado = TRUE).
-- El usuario sigue sin escribir la tabla directo: todo pasa por estas funciones (ver 20261022120000),
-- así no puede ponerse 'verificado' ni tocar cuentas.verificado. El aviso al admin de una solicitud
-- nueva sale solo con el trigger de 20261024120000 (verificado → pendiente).

CREATE OR REPLACE FUNCTION public.enviar_documento_verificacion(p_tipo_documento TEXT, p_archivo_path TEXT)
RETURNS TEXT
LANGUAGE plpgsql
SECURITY DEFINER
SET search_path = ''
AS $$
DECLARE
    v_uid      UUID := auth.uid();
    v_tipo     TEXT;
    v_anterior public.verificaciones_usuario%ROWTYPE;
BEGIN
    IF v_uid IS NULL THEN
        RAISE EXCEPTION 'Inicia sesión para enviar tu documento.';
    END IF;
    SELECT c.tipo_cuenta INTO v_tipo FROM public.cuentas c WHERE c.id_cuenta = v_uid;
    IF v_tipo IS NULL THEN
        RAISE EXCEPTION 'No encontramos tu cuenta.';
    END IF;
    IF p_archivo_path IS NULL OR LEFT(p_archivo_path, CHAR_LENGTH(v_uid::TEXT) + 1) <> v_uid::TEXT || '/'
       OR NOT EXISTS (SELECT 1 FROM storage.objects o WHERE o.bucket_id = 'constancias-fiscales' AND o.name = p_archivo_path) THEN
        RAISE EXCEPTION 'No encontramos el archivo de tu documento. Vuelve a subirlo.';
    END IF;

    SELECT * INTO v_anterior FROM public.verificaciones_usuario
    WHERE user_id = v_uid AND tipo_documento = p_tipo_documento
    FOR UPDATE;

    IF NOT FOUND THEN
        INSERT INTO public.verificaciones_usuario (user_id, tipo_usuario, tipo_documento, archivo_path)
        VALUES (v_uid, v_tipo, p_tipo_documento, p_archivo_path);
        RETURN NULL;
    END IF;

    IF v_anterior.estado = 'pendiente' THEN
        RAISE EXCEPTION 'Tu documento ya está en revisión. Podrás subir otro cuando el administrador lo revise.';
    END IF;

    -- 'rechazado' (volver a subir) o 'verificado' (actualizar): misma fila, otra vez en revisión.
    UPDATE public.verificaciones_usuario
    SET archivo_path = p_archivo_path, estado = 'pendiente', fecha_envio = NOW(),
        motivo_rechazo = NULL, revisado_por = NULL, fecha_revision = NULL
    WHERE id = v_anterior.id;

    RETURN NULLIF(v_anterior.archivo_path, p_archivo_path);
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

    -- Aprobado: insignia. Rechazado (también una actualización de un lote ya verificado): la pierde.
    UPDATE public.cuentas SET verificado = p_aprobar WHERE id_cuenta = v_usuario;

    INSERT INTO public.acciones_administrativas (id_admin, tipo_accion, motivo, id_cuenta_objetivo)
    VALUES (auth.uid(),
            CASE WHEN p_aprobar THEN 'aprobar_verificacion' ELSE 'rechazar_verificacion' END,
            COALESCE(v_motivo, 'Documento de verificación aprobado.'),
            v_usuario);
END;
$$;
