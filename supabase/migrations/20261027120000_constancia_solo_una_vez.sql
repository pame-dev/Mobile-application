-- La constancia se sube solo una vez: el usuario puede enviar su documento si no tiene ninguno o
-- si se lo rechazaron. Verificado o en revisión ya no se puede cambiar (revierte el "actualizar
-- constancia" de 20261026120000).
--
-- La regla vive en la BD, no solo en la interfaz: el usuario no tiene INSERT ni UPDATE directo en
-- verificaciones_usuario (ver 20261022120000) y esta función SECURITY DEFINER es la única forma de
-- escribir. Crea la fila si no existe; si estaba 'rechazado' actualiza la misma fila (archivo nuevo,
-- 'pendiente', fecha de hoy, sin revisión) y devuelve el archivo anterior para borrarlo. No toca
-- cuentas.verificado. Al rechazar, el admin pone verificado = FALSE (20261026130000, sin cambios).

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
        RAISE EXCEPTION 'Tu documento ya está en revisión.';
    END IF;
    IF v_anterior.estado = 'verificado' THEN
        RAISE EXCEPTION 'Tu documento ya está verificado y no se puede cambiar.';
    END IF;

    -- 'rechazado': misma fila, otra vez en revisión.
    UPDATE public.verificaciones_usuario
    SET archivo_path = p_archivo_path, estado = 'pendiente', fecha_envio = NOW(),
        motivo_rechazo = NULL, revisado_por = NULL, fecha_revision = NULL
    WHERE id = v_anterior.id;

    RETURN NULLIF(v_anterior.archivo_path, p_archivo_path);
END;
$$;
