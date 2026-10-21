-- Ajustes a 20261020120000_verificacion_lotes (recomendaciones de la revisión):
-- · El bucket queda privado y con sus límites aunque ya existiera.
-- · Solo se suben archivos con el nombre esperado: <id_cuenta>/constancia_<timestamp>.<pdf|jpg|png>.
-- · Mensajes claros para quien no es lote y para motivos de más de 1000 caracteres.

UPDATE storage.buckets
SET public = FALSE, file_size_limit = 5242880, allowed_mime_types = ARRAY['application/pdf', 'image/jpeg', 'image/png']
WHERE id = 'constancias-fiscales';

DROP POLICY constancias_subir_propia ON storage.objects;
CREATE POLICY constancias_subir_propia ON storage.objects FOR INSERT TO authenticated
    WITH CHECK (bucket_id = 'constancias-fiscales'
        AND (storage.foldername(name))[1] = auth.uid()::TEXT
        AND name ~ ('^' || auth.uid()::TEXT || '/constancia_[0-9]+\.(pdf|jpg|png)$')
        AND EXISTS (SELECT 1 FROM public.perfiles_lote pl WHERE pl.id_cuenta = auth.uid()));

CREATE OR REPLACE FUNCTION public.lote_enviar_constancia(p_ruta TEXT)
RETURNS VOID
LANGUAGE plpgsql
SECURITY DEFINER
SET search_path = ''
AS $$
DECLARE
    v_uid UUID := auth.uid();
BEGIN
    IF v_uid IS NULL THEN
        RAISE EXCEPTION 'Inicia sesión para enviar tu constancia.';
    END IF;
    IF NOT EXISTS (SELECT 1 FROM public.perfiles_lote pl WHERE pl.id_cuenta = v_uid) THEN
        RAISE EXCEPTION 'Solo los lotes pueden enviar una constancia fiscal.';
    END IF;
    IF p_ruta IS NULL OR LEFT(p_ruta, CHAR_LENGTH(v_uid::TEXT) + 12) <> v_uid::TEXT || '/constancia_'
       OR NOT EXISTS (SELECT 1 FROM storage.objects o WHERE o.bucket_id = 'constancias-fiscales' AND o.name = p_ruta) THEN
        RAISE EXCEPTION 'No encontramos el archivo de tu constancia. Vuelve a subirlo.';
    END IF;

    UPDATE public.perfiles_lote
    SET constancia_url = p_ruta, estado_verificacion = 'pendiente',
        motivo_rechazo_verificacion = NULL, fecha_verificacion = NULL
    WHERE id_cuenta = v_uid AND estado_verificacion IN ('sin_documento', 'rechazado');

    IF NOT FOUND THEN
        RAISE EXCEPTION 'Tu constancia ya está en revisión o tu lote ya está verificado.';
    END IF;
END;
$$;

CREATE OR REPLACE FUNCTION public.admin_resolver_verificacion(p_id_cuenta UUID, p_aprobar BOOLEAN, p_motivo TEXT DEFAULT NULL)
RETURNS VOID
LANGUAGE plpgsql
SECURITY DEFINER
SET search_path = ''
AS $$
DECLARE
    v_motivo TEXT := NULLIF(TRIM(p_motivo), '');
BEGIN
    PERFORM public.exigir_admin();

    IF p_aprobar THEN
        UPDATE public.perfiles_lote
        SET estado_verificacion = 'verificado', fecha_verificacion = NOW(), motivo_rechazo_verificacion = NULL
        WHERE id_cuenta = p_id_cuenta AND estado_verificacion = 'pendiente';
    ELSE
        v_motivo := public.exigir_motivo(p_motivo, 'rechaza');
        IF CHAR_LENGTH(v_motivo) > 1000 THEN
            RAISE EXCEPTION 'El motivo puede tener como máximo 1000 caracteres.';
        END IF;

        UPDATE public.perfiles_lote
        SET estado_verificacion = 'rechazado', motivo_rechazo_verificacion = v_motivo, fecha_verificacion = NULL
        WHERE id_cuenta = p_id_cuenta AND estado_verificacion = 'pendiente';
    END IF;

    IF NOT FOUND THEN
        RAISE EXCEPTION 'Esta constancia no existe o ya fue revisada.';
    END IF;

    INSERT INTO public.acciones_administrativas (id_admin, tipo_accion, motivo, id_cuenta_objetivo)
    VALUES (auth.uid(),
            CASE WHEN p_aprobar THEN 'aprobar_verificacion' ELSE 'rechazar_verificacion' END,
            COALESCE(v_motivo, 'Constancia fiscal verificada.'),
            p_id_cuenta);
END;
$$;
