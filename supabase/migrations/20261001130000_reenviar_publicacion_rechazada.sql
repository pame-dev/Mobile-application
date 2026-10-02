-- ############################################################################
-- Corregir y reenviar una publicación rechazada
-- ############################################################################
-- uq_propuestas_editable permite una sola propuesta editable (pendiente o
-- rechazada) por publicación, así que reenviar no crea otra: la propuesta
-- rechazada se actualiza con los datos corregidos y vuelve a 'pendiente'.
-- El motivo del rechazo anterior queda en acciones_administrativas.
--
-- SECURITY DEFINER porque RLS no deja al dueño actualizar propuestas ni borrar
-- sus fotos; la función valida que la publicación sea suya.

CREATE FUNCTION public.reenviar_publicacion(p_id_publicacion BIGINT, p_datos JSONB, p_fotos TEXT[])
RETURNS VOID
LANGUAGE plpgsql
SECURITY DEFINER
SET search_path = ''
AS $$
DECLARE
    v_cuenta       public.cuentas%ROWTYPE;
    v_id_propuesta BIGINT;
    v_problemas    TEXT := NULLIF(TRIM(p_datos ->> 'descripcion_problemas'), '');
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
    -- Solo fotos subidas por el dueño (CarRepository.uploadPhoto usa esta carpeta).
    IF EXISTS (SELECT 1 FROM unnest(p_fotos) AS r
               WHERE r NOT LIKE 'publicaciones/' || auth.uid()::TEXT || '/%') THEN
        RAISE EXCEPTION 'Alguna foto no es válida.';
    END IF;

    SELECT id_propuesta INTO v_id_propuesta
    FROM public.propuestas_publicacion
    WHERE id_publicacion = p_id_publicacion AND estado_propuesta = 'rechazada'
    FOR UPDATE;
    IF NOT FOUND THEN
        RAISE EXCEPTION 'Esta publicación no tiene un rechazo que corregir.';
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
        -- De vuelta a moderación.
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

REVOKE EXECUTE ON FUNCTION public.reenviar_publicacion(BIGINT, JSONB, TEXT[]) FROM PUBLIC, anon;
GRANT EXECUTE ON FUNCTION public.reenviar_publicacion(BIGINT, JSONB, TEXT[]) TO authenticated;
