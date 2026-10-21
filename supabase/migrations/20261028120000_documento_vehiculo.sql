-- ############################################################################
-- Documento del vehículo al publicar y editar
-- ############################################################################
-- En el paso "Datos" el vendedor elige UNO de estos documentos:
--   · vin                  → número de serie escrito (17 caracteres).
--   · placa                → placa escrita.
--   · tarjeta_circulacion  → foto de la tarjeta de circulación.
--   · baja                 → foto del documento de baja del vehículo.
--
-- Es un dato privado: solo lo ven el dueño y los administradores.
-- · Se guarda solo en propuestas_publicacion (RLS propuestas_leer: dueño o admin).
--   admin_moderar_propuesta NO lo copia a publicaciones y v_anuncios NO lo
--   muestra, porque ambas las lee el público.
-- · Las fotos van al bucket privado "documentos-vehiculo" (<id_cuenta>/<uuid>.jpg).
--   No se reutiliza "constancias-fiscales": admin_documentos_verificacion_sin_uso
--   borra de ahí todo archivo que no esté en verificaciones_usuario.
--
-- crear_publicacion y editar_publicacion lo piden siempre. Las propuestas
-- anteriores a esta migración quedan sin documento (NULL).
-- crear_publicacion parte de 20261003120000_motor_y_combustible.sql y
-- editar_publicacion de 20261008120000_recuperado_por_seguro_al_editar.sql;
-- solo se agrega el documento.


-- ── Columnas ───────────────────────────────────────────────────────────────

ALTER TABLE public.propuestas_publicacion
    ADD COLUMN tipo_documento_vehiculo VARCHAR(20) NULL,
    ADD COLUMN documento_vehiculo      TEXT        NULL,
    ADD CONSTRAINT chk_propuestas_publicacion_documento_vehiculo CHECK (
        (tipo_documento_vehiculo IS NULL AND documento_vehiculo IS NULL)
        OR (tipo_documento_vehiculo = 'vin'   AND documento_vehiculo ~ '^[A-HJ-NPR-Z0-9]{17}$')
        OR (tipo_documento_vehiculo = 'placa' AND documento_vehiculo ~ '^[A-Z0-9-]{4,10}$')
        OR (tipo_documento_vehiculo IN ('tarjeta_circulacion', 'baja')
            AND documento_vehiculo ~ '^[0-9a-f-]{36}/[0-9a-f-]{36}\.jpg$')
    );

COMMENT ON COLUMN public.propuestas_publicacion.tipo_documento_vehiculo IS
    'Documento que dio el vendedor: vin, placa, tarjeta_circulacion o baja. Privado (dueño y admin).';
COMMENT ON COLUMN public.propuestas_publicacion.documento_vehiculo IS
    'VIN o placa en mayúsculas, o ruta de la foto en el bucket privado documentos-vehiculo.';


-- ── Bucket privado ─────────────────────────────────────────────────────────

INSERT INTO storage.buckets (id, name, public, file_size_limit, allowed_mime_types)
VALUES ('documentos-vehiculo', 'documentos-vehiculo', FALSE, 5242880, ARRAY['image/jpeg'])
ON CONFLICT (id) DO UPDATE
SET public = FALSE, file_size_limit = 5242880, allowed_mime_types = ARRAY['image/jpeg'];

-- Cada cuenta sube y lee solo su carpeta; el admin lee todo. Sin UPDATE ni DELETE:
-- cambiar la foto sube un archivo nuevo.
CREATE POLICY documentos_vehiculo_subir_propio ON storage.objects FOR INSERT TO authenticated
    WITH CHECK (bucket_id = 'documentos-vehiculo'
        AND name ~ ('^' || auth.uid()::TEXT || '/[0-9a-f-]{36}\.jpg$'));
CREATE POLICY documentos_vehiculo_leer_propio ON storage.objects FOR SELECT TO authenticated
    USING (bucket_id = 'documentos-vehiculo' AND (storage.foldername(name))[1] = auth.uid()::TEXT);
CREATE POLICY documentos_vehiculo_leer_admin ON storage.objects FOR SELECT TO authenticated
    USING (bucket_id = 'documentos-vehiculo' AND public.es_admin_actual());


-- ── Validación ─────────────────────────────────────────────────────────────

-- Devuelve el valor a guardar (VIN/placa sin espacios y en mayúsculas, o la ruta de la
-- foto) o lanza un error claro. La foto debe ser de la carpeta de quien publica y
-- existir en el bucket.
CREATE FUNCTION public.documento_vehiculo_valido(p_tipo TEXT, p_valor TEXT)
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

    IF v_valor IS NULL
       OR v_valor !~ ('^' || auth.uid()::TEXT || '/[0-9a-f-]{36}\.jpg$')
       OR NOT EXISTS (SELECT 1 FROM storage.objects o
                      WHERE o.bucket_id = 'documentos-vehiculo' AND o.name = v_valor) THEN
        RAISE EXCEPTION 'Agrega la foto del documento del vehículo.';
    END IF;
    RETURN v_valor;
END;
$$;
REVOKE ALL ON FUNCTION public.documento_vehiculo_valido(TEXT, TEXT) FROM PUBLIC, anon;
GRANT EXECUTE ON FUNCTION public.documento_vehiculo_valido(TEXT, TEXT) TO authenticated;


-- ── crear_publicacion ──────────────────────────────────────────────────────

CREATE OR REPLACE FUNCTION public.crear_publicacion(p_datos JSONB, p_fotos TEXT[])
RETURNS BIGINT
LANGUAGE plpgsql
SET search_path = ''
AS $$
DECLARE
    v_cuenta       public.cuentas%ROWTYPE;
    v_id           BIGINT;
    v_id_propuesta BIGINT;
    v_problemas    TEXT := NULLIF(TRIM(p_datos ->> 'descripcion_problemas'), '');
    v_documento    TEXT;
BEGIN
    SELECT * INTO v_cuenta FROM public.cuentas WHERE id_cuenta = auth.uid();
    IF NOT FOUND THEN
        RAISE EXCEPTION 'Inicia sesión para publicar.';
    END IF;
    IF v_cuenta.estado_cuenta = 'suspendida' THEN
        RAISE EXCEPTION 'Tu cuenta está suspendida.';
    END IF;

    v_documento := public.documento_vehiculo_valido(
        p_datos ->> 'tipo_documento_vehiculo', p_datos ->> 'documento_vehiculo');

    INSERT INTO public.publicaciones (id_propietario)
    VALUES (auth.uid())
    RETURNING id_publicacion INTO v_id;

    INSERT INTO public.propuestas_publicacion (
        id_publicacion, titulo, descripcion, precio, precio_negociable, moneda,
        estado_ubicacion, municipio_ubicacion,
        id_marca, marca_otra, id_modelo, modelo_otro, anio, kilometraje,
        id_color, id_transmision, id_carroceria,
        numero_propietarios_anteriores, tiene_adeudos, tiene_problemas,
        descripcion_problemas, recuperado_por_seguro, cilindros,
        cilindrada, tipo_combustible,
        tipo_documento_vehiculo, documento_vehiculo
    ) VALUES (
        v_id,
        TRIM(p_datos ->> 'titulo'),
        TRIM(p_datos ->> 'descripcion'),
        (p_datos ->> 'precio')::NUMERIC,
        COALESCE((p_datos ->> 'precio_negociable')::BOOLEAN, FALSE),
        COALESCE(p_datos ->> 'moneda', 'MXN'),
        COALESCE(NULLIF(TRIM(p_datos ->> 'estado_ubicacion'), ''), v_cuenta.estado_perfil_ubi),
        COALESCE(NULLIF(TRIM(p_datos ->> 'municipio_ubicacion'), ''), v_cuenta.municipio_perfil_ubi),
        (p_datos ->> 'id_marca')::BIGINT,
        NULLIF(TRIM(p_datos ->> 'marca_otra'), ''),
        (p_datos ->> 'id_modelo')::BIGINT,
        NULLIF(TRIM(p_datos ->> 'modelo_otro'), ''),
        (p_datos ->> 'anio')::SMALLINT,
        (p_datos ->> 'kilometraje')::INTEGER,
        (p_datos ->> 'id_color')::BIGINT,
        (p_datos ->> 'id_transmision')::BIGINT,
        (p_datos ->> 'id_carroceria')::BIGINT,
        COALESCE((p_datos ->> 'numero_propietarios_anteriores')::SMALLINT, 0),
        COALESCE((p_datos ->> 'tiene_adeudos')::BOOLEAN, FALSE),
        v_problemas IS NOT NULL,
        v_problemas,
        COALESCE((p_datos ->> 'recuperado_por_seguro')::BOOLEAN, FALSE),
        (p_datos ->> 'cilindros')::SMALLINT,
        NULLIF(TRIM(p_datos ->> 'cilindrada'), ''),
        NULLIF(TRIM(p_datos ->> 'tipo_combustible'), ''),
        p_datos ->> 'tipo_documento_vehiculo',
        v_documento
    )
    RETURNING id_propuesta INTO v_id_propuesta;

    INSERT INTO public.fotos_propuesta_publicacion (id_propuesta, ruta_storage, orden_foto)
    SELECT v_id_propuesta, f.ruta, f.orden::SMALLINT
    FROM unnest(COALESCE(p_fotos, ARRAY[]::TEXT[])) WITH ORDINALITY AS f(ruta, orden);

    RETURN v_id;
END;
$$;


-- ── editar_publicacion ─────────────────────────────────────────────────────

CREATE OR REPLACE FUNCTION public.editar_publicacion(p_id_publicacion BIGINT, p_datos JSONB, p_fotos TEXT[])
RETURNS VOID
LANGUAGE plpgsql
SECURITY DEFINER
SET search_path = ''
AS $$
DECLARE
    v_cuenta       public.cuentas%ROWTYPE;
    v_id_propuesta BIGINT;
    v_problemas    TEXT := NULLIF(TRIM(p_datos ->> 'descripcion_problemas'), '');
    v_documento    TEXT;
BEGIN
    SELECT * INTO v_cuenta FROM public.cuentas WHERE id_cuenta = auth.uid();
    IF NOT FOUND THEN
        RAISE EXCEPTION 'Inicia sesión para publicar.';
    END IF;
    IF v_cuenta.estado_cuenta = 'suspendida' THEN
        RAISE EXCEPTION 'Tu cuenta está suspendida.';
    END IF;
    IF NOT public.es_propietario_publicacion(p_id_publicacion) THEN
        RAISE EXCEPTION 'La publicación no existe.';
    END IF;
    IF COALESCE(array_length(p_fotos, 1), 0) = 0 THEN
        RAISE EXCEPTION 'Agrega al menos una foto.';
    END IF;
    IF array_length(p_fotos, 1) > 15 THEN
        RAISE EXCEPTION 'Puedes subir máximo 15 fotos.';
    END IF;
    -- Solo fotos subidas por el dueño (CarRepository.uploadPhoto usa esta carpeta).
    IF EXISTS (SELECT 1 FROM unnest(p_fotos) AS r
               WHERE r NOT LIKE 'publicaciones/' || auth.uid()::TEXT || '/%') THEN
        RAISE EXCEPTION 'Alguna foto no es válida.';
    END IF;

    v_documento := public.documento_vehiculo_valido(
        p_datos ->> 'tipo_documento_vehiculo', p_datos ->> 'documento_vehiculo');

    SELECT id_propuesta INTO v_id_propuesta
    FROM public.propuestas_publicacion
    WHERE id_publicacion = p_id_publicacion AND estado_propuesta IN ('pendiente', 'rechazada')
    FOR UPDATE;

    IF NOT FOUND THEN
        -- Copia completa: los campos que el formulario no edita (ubicación,
        -- número de puertas, etc.) se conservan al aprobar.
        INSERT INTO public.propuestas_publicacion (
            id_publicacion, titulo, descripcion, precio, precio_negociable, moneda,
            estado_ubicacion, municipio_ubicacion, horario_preferido_contacto, ruta_video,
            id_marca, marca_otra, id_modelo, modelo_otro, anio, kilometraje,
            id_color, color_otro, id_transmision, transmision_otra, id_carroceria, carroceria_otra,
            numero_propietarios_anteriores, tiene_adeudos, tiene_problemas, descripcion_problemas,
            recuperado_por_seguro, tipo_combustible, numero_puertas, numero_pasajeros,
            cilindros, cilindrada, tipo_traccion, modificaciones
        )
        SELECT
            p.id_publicacion, p.titulo, p.descripcion, p.precio, p.precio_negociable, p.moneda,
            p.estado_ubicacion, p.municipio_ubicacion, p.horario_preferido_contacto, p.ruta_video,
            p.id_marca, p.marca_otra, p.id_modelo, p.modelo_otro, p.anio, p.kilometraje,
            p.id_color, p.color_otro, p.id_transmision, p.transmision_otra, p.id_carroceria, p.carroceria_otra,
            p.numero_propietarios_anteriores, p.tiene_adeudos, p.tiene_problemas, p.descripcion_problemas,
            p.recuperado_por_seguro, p.tipo_combustible, p.numero_puertas, p.numero_pasajeros,
            p.cilindros, p.cilindrada, p.tipo_traccion, p.modificaciones
        FROM public.publicaciones p
        WHERE p.id_publicacion = p_id_publicacion AND p.fecha_primera_publicacion IS NOT NULL
        RETURNING id_propuesta INTO v_id_propuesta;

        IF v_id_propuesta IS NULL THEN
            RAISE EXCEPTION 'La publicación no se puede editar.';
        END IF;
    END IF;

    UPDATE public.propuestas_publicacion
    SET titulo                         = TRIM(p_datos ->> 'titulo'),
        descripcion                    = TRIM(p_datos ->> 'descripcion'),
        precio                         = (p_datos ->> 'precio')::NUMERIC,
        precio_negociable              = COALESCE((p_datos ->> 'precio_negociable')::BOOLEAN, precio_negociable),
        moneda                         = COALESCE(p_datos ->> 'moneda', moneda),
        id_marca                       = (p_datos ->> 'id_marca')::BIGINT,
        marca_otra                     = NULLIF(TRIM(p_datos ->> 'marca_otra'), ''),
        id_modelo                      = (p_datos ->> 'id_modelo')::BIGINT,
        modelo_otro                    = NULLIF(TRIM(p_datos ->> 'modelo_otro'), ''),
        anio                           = (p_datos ->> 'anio')::SMALLINT,
        kilometraje                    = (p_datos ->> 'kilometraje')::INTEGER,
        id_color                       = (p_datos ->> 'id_color')::BIGINT,
        id_transmision                 = (p_datos ->> 'id_transmision')::BIGINT,
        id_carroceria                  = (p_datos ->> 'id_carroceria')::BIGINT,
        numero_propietarios_anteriores = COALESCE((p_datos ->> 'numero_propietarios_anteriores')::SMALLINT, 0),
        tiene_problemas                = v_problemas IS NOT NULL,
        descripcion_problemas          = v_problemas,
        cilindros                      = (p_datos ->> 'cilindros')::SMALLINT,
        cilindrada                     = NULLIF(TRIM(p_datos ->> 'cilindrada'), ''),
        tipo_combustible               = NULLIF(TRIM(p_datos ->> 'tipo_combustible'), ''),
        recuperado_por_seguro          = COALESCE((p_datos ->> 'recuperado_por_seguro')::BOOLEAN, recuperado_por_seguro),
        tipo_documento_vehiculo        = p_datos ->> 'tipo_documento_vehiculo',
        documento_vehiculo             = v_documento,
        -- A moderación.
        estado_propuesta                 = 'pendiente',
        motivo_rechazo                   = NULL,
        id_admin_revisor                 = NULL,
        fecha_revision                   = NULL,
        fecha_envio_moderacion           = NOW(),
        fecha_ultima_edicion_propietario = NOW()
    WHERE id_propuesta = v_id_propuesta;

    -- La galería se reemplaza completa (puede conservar rutas que ya tenía).
    DELETE FROM public.fotos_propuesta_publicacion WHERE id_propuesta = v_id_propuesta;

    INSERT INTO public.fotos_propuesta_publicacion (id_propuesta, ruta_storage, orden_foto)
    SELECT v_id_propuesta, f.ruta, f.orden::SMALLINT
    FROM unnest(p_fotos) WITH ORDINALITY AS f(ruta, orden);
END;
$$;
