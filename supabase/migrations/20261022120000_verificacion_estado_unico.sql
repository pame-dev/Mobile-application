-- Verificación: un solo registro por usuario y tipo de documento, con su estado actual (sin historial).
--
-- · Se borran los registros viejos repetidos (se queda el envío más reciente).
-- · UNIQUE (user_id, tipo_documento): reenviar después de un rechazo actualiza la misma fila.
-- · El envío (primero o reenvío) pasa por enviar_documento_verificacion(): el usuario ya no
--   inserta ni actualiza la tabla directo, así no puede ponerse 'verificado' ni cambiar la revisión.
-- · Archivos: la BD no puede borrarlos del bucket (eso lo hace la API de Storage). El usuario borra
--   su archivo anterior al reenviar, y el admin borra los que ya nadie usa
--   (admin_documentos_verificacion_sin_uso), igual que admin_fotos_retiradas_sin_uso.

-- ── Limpieza de repetidos ──────────────────────────────────────────────────

DELETE FROM public.verificaciones_usuario v
USING (
    SELECT id, ROW_NUMBER() OVER (PARTITION BY user_id, tipo_documento ORDER BY fecha_envio DESC, id) AS n
    FROM public.verificaciones_usuario
) r
WHERE r.id = v.id AND r.n > 1;

-- ── Un registro por usuario y documento ────────────────────────────────────

DROP INDEX public.ux_verificaciones_usuario_vigente;
ALTER TABLE public.verificaciones_usuario
    ADD CONSTRAINT ux_verificaciones_usuario_documento UNIQUE (user_id, tipo_documento);

-- El usuario ya no escribe en la tabla directo: solo con la función de abajo.
DROP POLICY verificaciones_usuario_enviar_propia ON public.verificaciones_usuario;
REVOKE INSERT ON public.verificaciones_usuario FROM authenticated;

-- ── Enviar o reenviar ──────────────────────────────────────────────────────

-- [p_archivo_path] ya subido a la carpeta del usuario. Si no tenía registro, lo crea 'pendiente';
-- si estaba 'rechazado', lo reemplaza (archivo nuevo, 'pendiente', fecha de hoy, sin revisión).
-- Pendiente o verificado no se pueden reenviar. Devuelve el archivo anterior (para borrarlo) o NULL.
CREATE FUNCTION public.enviar_documento_verificacion(p_tipo_documento TEXT, p_archivo_path TEXT)
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

    IF v_anterior.estado <> 'rechazado' THEN
        RAISE EXCEPTION 'Ya tienes un documento en revisión o tu cuenta ya está verificada.';
    END IF;

    UPDATE public.verificaciones_usuario
    SET archivo_path = p_archivo_path, estado = 'pendiente', fecha_envio = NOW(),
        motivo_rechazo = NULL, revisado_por = NULL, fecha_revision = NULL
    WHERE id = v_anterior.id;

    RETURN NULLIF(v_anterior.archivo_path, p_archivo_path);
END;
$$;

REVOKE ALL ON FUNCTION public.enviar_documento_verificacion(TEXT, TEXT) FROM PUBLIC, anon;
GRANT EXECUTE ON FUNCTION public.enviar_documento_verificacion(TEXT, TEXT) TO authenticated;

-- ── Borrar archivos que ya no se usan ──────────────────────────────────────

-- El usuario borra su archivo anterior al reenviar; el admin, cualquiera que ya no se use.
CREATE POLICY constancias_borrar_propia ON storage.objects FOR DELETE TO authenticated
    USING (bucket_id = 'constancias-fiscales' AND (storage.foldername(name))[1] = auth.uid()::TEXT
        AND NOT EXISTS (SELECT 1 FROM public.verificaciones_usuario v WHERE v.archivo_path = name));
CREATE POLICY constancias_borrar_admin ON storage.objects FOR DELETE TO authenticated
    USING (bucket_id = 'constancias-fiscales' AND public.es_admin_actual());

-- Archivos del bucket que ningún registro usa (los repetidos de arriba y reenvíos viejos).
-- Solo los de más de una hora, para no tocar uno que se acaba de subir y aún no se envía.
CREATE FUNCTION public.admin_documentos_verificacion_sin_uso()
RETURNS TEXT[]
LANGUAGE plpgsql
STABLE
SECURITY DEFINER
SET search_path = ''
AS $$
BEGIN
    PERFORM public.exigir_admin();

    RETURN ARRAY(
        SELECT o.name FROM storage.objects o
        WHERE o.bucket_id = 'constancias-fiscales'
          AND o.created_at < NOW() - INTERVAL '1 hour'
          AND NOT EXISTS (SELECT 1 FROM public.verificaciones_usuario v WHERE v.archivo_path = o.name)
        LIMIT 500
    );
END;
$$;

REVOKE ALL ON FUNCTION public.admin_documentos_verificacion_sin_uso() FROM PUBLIC, anon;
GRANT EXECUTE ON FUNCTION public.admin_documentos_verificacion_sin_uso() TO authenticated;
