-- ############################################################################
-- Documento del vehículo: foto (JPG o PNG) o PDF
-- ############################################################################
-- En 20261028120000_documento_vehiculo.sql la tarjeta de circulación y la baja
-- solo se aceptaban como JPG. Ahora pueden ser JPG, PNG o PDF de hasta 5 MB
-- (mismas reglas que los documentos de verificación). Cambia:
-- · Tipos permitidos del bucket privado "documentos-vehiculo".
-- · La política de subida y el CHECK aceptan <id_cuenta>/<uuid>.(jpg|png|pdf).
-- · documento_vehiculo_valido acepta esas extensiones y rechaza sin sesión.


-- ── Bucket ─────────────────────────────────────────────────────────────────

UPDATE storage.buckets
SET public = FALSE, file_size_limit = 5242880,
    allowed_mime_types = ARRAY['application/pdf', 'image/jpeg', 'image/png']
WHERE id = 'documentos-vehiculo';

DROP POLICY documentos_vehiculo_subir_propio ON storage.objects;
CREATE POLICY documentos_vehiculo_subir_propio ON storage.objects FOR INSERT TO authenticated
    WITH CHECK (bucket_id = 'documentos-vehiculo'
        AND name ~ ('^' || auth.uid()::TEXT || '/[0-9a-f-]{36}\.(jpg|png|pdf)$'));


-- ── Columnas ───────────────────────────────────────────────────────────────

ALTER TABLE public.propuestas_publicacion
    DROP CONSTRAINT chk_propuestas_publicacion_documento_vehiculo,
    ADD CONSTRAINT chk_propuestas_publicacion_documento_vehiculo CHECK (
        (tipo_documento_vehiculo IS NULL AND documento_vehiculo IS NULL)
        OR (tipo_documento_vehiculo = 'vin'   AND documento_vehiculo ~ '^[A-HJ-NPR-Z0-9]{17}$')
        OR (tipo_documento_vehiculo = 'placa' AND documento_vehiculo ~ '^[A-Z0-9-]{4,10}$')
        OR (tipo_documento_vehiculo IN ('tarjeta_circulacion', 'baja')
            AND documento_vehiculo ~ '^[0-9a-f-]{36}/[0-9a-f-]{36}\.(jpg|png|pdf)$')
    );

COMMENT ON COLUMN public.propuestas_publicacion.documento_vehiculo IS
    'VIN o placa en mayúsculas, o ruta de la foto (JPG/PNG) o PDF en el bucket privado documentos-vehiculo.';


-- ── Validación ─────────────────────────────────────────────────────────────

-- Igual que en 20261028120000_documento_vehiculo.sql salvo las extensiones del archivo,
-- el rechazo explícito sin sesión y el mensaje del archivo faltante.
CREATE OR REPLACE FUNCTION public.documento_vehiculo_valido(p_tipo TEXT, p_valor TEXT)
RETURNS TEXT
LANGUAGE plpgsql
STABLE
SECURITY DEFINER
SET search_path = ''
AS $$
DECLARE
    v_valor TEXT := NULLIF(TRIM(p_valor), '');
BEGIN
    IF p_tipo IS NULL OR p_tipo NOT IN ('vin', 'placa', 'tarjeta_circulacion', 'baja') THEN
        RAISE EXCEPTION 'Elige qué documento del vehículo vas a agregar.';
    END IF;

    IF p_tipo IN ('vin', 'placa') THEN
        v_valor := UPPER(regexp_replace(COALESCE(v_valor, ''), '\s', '', 'g'));
        IF p_tipo = 'vin' AND v_valor !~ '^[A-HJ-NPR-Z0-9]{17}$' THEN
            RAISE EXCEPTION 'El VIN debe tener 17 letras o números (sin I, O ni Q).';
        END IF;
        IF p_tipo = 'placa' AND v_valor !~ '^[A-Z0-9-]{4,10}$' THEN
            RAISE EXCEPTION 'La placa debe tener de 4 a 10 letras o números.';
        END IF;
        RETURN v_valor;
    END IF;

    IF v_valor IS NULL OR auth.uid() IS NULL
       OR v_valor !~ ('^' || auth.uid()::TEXT || '/[0-9a-f-]{36}\.(jpg|png|pdf)$')
       OR NOT EXISTS (SELECT 1 FROM storage.objects o
                      WHERE o.bucket_id = 'documentos-vehiculo' AND o.name = v_valor) THEN
        RAISE EXCEPTION 'Agrega la foto o el PDF del documento del vehículo.';
    END IF;
    RETURN v_valor;
END;
$$;
