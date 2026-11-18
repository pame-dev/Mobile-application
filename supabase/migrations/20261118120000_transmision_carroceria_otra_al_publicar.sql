-- Transmisión y tipo de carro "Otros" con texto libre al publicar y al editar (web y app).
--
-- Igual que el color (20261117120000_color_otro_al_publicar): la web y la app ya ofrecen "Otros"
-- en Transmisión y Tipo de carro con un campo para escribir el valor, y lo mandan en
-- p_datos.transmision_otra / p_datos.carroceria_otra (null con cualquier otra opción). Las
-- versiones vigentes de crear_publicacion y editar_publicacion (la de color) no leían esas dos
-- columnas, así que fn_validar_catalogo_otros rechazaba "Otros" y, al editar, pasar de "Otros"
-- a otra opción dejaba el texto viejo.
--
-- Solo se redefinen esas dos funciones, idénticas a 20261117120000 más transmision_otra y
-- carroceria_otra (con NULLIF/TRIM, igual que color_otro). Sin cambios de tablas, vistas ni
-- permisos: v_anuncios ya muestra COALESCE(transmision_otra, …) y COALESCE(carroceria_otra, …), y
-- la aprobación ya copia ambas columnas de la propuesta a la publicación. CREATE OR REPLACE
-- conserva los GRANT.

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
    v_condicion    TEXT;
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
    v_condicion := public.condicion_vehiculo_valida(p_datos ->> 'condicion_vehiculo');

    INSERT INTO public.publicaciones (id_propietario)
    VALUES (auth.uid())
    RETURNING id_publicacion INTO v_id;

    INSERT INTO public.propuestas_publicacion (
        id_publicacion, titulo, descripcion, precio, precio_negociable, moneda,
        estado_ubicacion, municipio_ubicacion,
        id_marca, marca_otra, id_modelo, modelo_otro, anio, kilometraje,
        id_color, color_otro, id_transmision, transmision_otra, id_carroceria, carroceria_otra,
        numero_propietarios_anteriores, tiene_adeudos, tiene_problemas,
        descripcion_problemas, recuperado_por_seguro, cilindros,
        cilindrada, tipo_combustible,
        tipo_documento_vehiculo, documento_vehiculo,
        factura_original, aire_acondicionado, condicion_vehiculo
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
        NULLIF(TRIM(p_datos ->> 'color_otro'), ''),
        (p_datos ->> 'id_transmision')::BIGINT,
        NULLIF(TRIM(p_datos ->> 'transmision_otra'), ''),
        (p_datos ->> 'id_carroceria')::BIGINT,
        NULLIF(TRIM(p_datos ->> 'carroceria_otra'), ''),
        COALESCE((p_datos ->> 'numero_propietarios_anteriores')::SMALLINT, 0),
        COALESCE((p_datos ->> 'tiene_adeudos')::BOOLEAN, FALSE),
        v_problemas IS NOT NULL,
        v_problemas,
        COALESCE((p_datos ->> 'recuperado_por_seguro')::BOOLEAN, FALSE),
        (p_datos ->> 'cilindros')::SMALLINT,
        NULLIF(TRIM(p_datos ->> 'cilindrada'), ''),
        NULLIF(TRIM(p_datos ->> 'tipo_combustible'), ''),
        p_datos ->> 'tipo_documento_vehiculo',
        v_documento,
        COALESCE((p_datos ->> 'factura_original')::BOOLEAN, FALSE),
        COALESCE((p_datos ->> 'aire_acondicionado')::BOOLEAN, FALSE),
        v_condicion
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
    v_condicion    TEXT;
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
    v_condicion := public.condicion_vehiculo_valida(p_datos ->> 'condicion_vehiculo');

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
            cilindros, cilindrada, tipo_traccion, modificaciones,
            factura_original, aire_acondicionado, condicion_vehiculo
        )
        SELECT
            p.id_publicacion, p.titulo, p.descripcion, p.precio, p.precio_negociable, p.moneda,
            p.estado_ubicacion, p.municipio_ubicacion, p.horario_preferido_contacto, p.ruta_video,
            p.id_marca, p.marca_otra, p.id_modelo, p.modelo_otro, p.anio, p.kilometraje,
            p.id_color, p.color_otro, p.id_transmision, p.transmision_otra, p.id_carroceria, p.carroceria_otra,
            p.numero_propietarios_anteriores, p.tiene_adeudos, p.tiene_problemas, p.descripcion_problemas,
            p.recuperado_por_seguro, p.tipo_combustible, p.numero_puertas, p.numero_pasajeros,
            p.cilindros, p.cilindrada, p.tipo_traccion, p.modificaciones,
            p.factura_original, p.aire_acondicionado, p.condicion_vehiculo
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
        color_otro                     = NULLIF(TRIM(p_datos ->> 'color_otro'), ''),
        id_transmision                 = (p_datos ->> 'id_transmision')::BIGINT,
        transmision_otra               = NULLIF(TRIM(p_datos ->> 'transmision_otra'), ''),
        id_carroceria                  = (p_datos ->> 'id_carroceria')::BIGINT,
        carroceria_otra                = NULLIF(TRIM(p_datos ->> 'carroceria_otra'), ''),
        numero_propietarios_anteriores = COALESCE((p_datos ->> 'numero_propietarios_anteriores')::SMALLINT, 0),
        tiene_problemas                = v_problemas IS NOT NULL,
        descripcion_problemas          = v_problemas,
        cilindros                      = (p_datos ->> 'cilindros')::SMALLINT,
        cilindrada                     = NULLIF(TRIM(p_datos ->> 'cilindrada'), ''),
        tipo_combustible               = NULLIF(TRIM(p_datos ->> 'tipo_combustible'), ''),
        recuperado_por_seguro          = COALESCE((p_datos ->> 'recuperado_por_seguro')::BOOLEAN, recuperado_por_seguro),
        tipo_documento_vehiculo        = p_datos ->> 'tipo_documento_vehiculo',
        documento_vehiculo             = v_documento,
        factura_original               = COALESCE((p_datos ->> 'factura_original')::BOOLEAN, factura_original),
        aire_acondicionado             = COALESCE((p_datos ->> 'aire_acondicionado')::BOOLEAN, aire_acondicionado),
        condicion_vehiculo             = v_condicion,
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
