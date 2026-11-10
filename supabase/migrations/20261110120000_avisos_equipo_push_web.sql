-- ############################################################################
-- Avisos del panel para el equipo (push en el teléfono y en el navegador)
-- ############################################################################
-- 1. Avisos nuevos para el administrador y el moderador de publicaciones:
--      publicacion_por_revisar   alguien mandó una publicación nueva a revisión
--      edicion_por_revisar       alguien editó una publicación ya aprobada
--      reporte_nuevo             alguien reportó una publicación
--    (verificacion_pendiente y pago_destacado ya les llegan al administrador y
--    al moderador de verificaciones.)
-- 2. Los avisos del panel no se silencian de noche: siempre van por push.
-- 3. Push en el navegador (solo el equipo): dispositivos_push también guarda la
--    suscripción del navegador (dirección + llaves). La Edge Function enviar-push
--    manda por Firebase a los teléfonos y por Web Push (VAPID) a los navegadores.


-- ── Tipos de aviso ─────────────────────────────────────────────────────────

ALTER TABLE public.notificaciones DROP CONSTRAINT chk_notificaciones_tipo;
ALTER TABLE public.notificaciones ADD CONSTRAINT chk_notificaciones_tipo
    CHECK (tipo IN ('favorito', 'propuesta_enviada', 'publicacion_aprobada', 'destacado_aprobado',
                    'publicacion_rechazada', 'edicion_aprobada', 'edicion_rechazada',
                    'publicacion_deshabilitada', 'publicacion_rehabilitada',
                    'destacado_rechazado', 'reporte_atendido', 'reporte_descartado',
                    'destacado_por_vencer', 'cuenta_suspendida', 'cuenta_reactivada',
                    'busqueda_coincidencia',
                    'precio_bajo', 'favorito_no_disponible', 'resumen_semanal', 'auto_parecido',
                    'resumen_vendedor', 'hito_vistas', 'poca_actividad', 'confirmar_venta',
                    'verificacion_pendiente', 'verificacion_aprobada', 'verificacion_rechazada',
                    'pago_destacado',
                    'publicacion_por_revisar', 'edicion_por_revisar', 'reporte_nuevo'));


-- ── crear_aviso: los avisos del panel llegan también de noche ──────────────
-- Mismo cuerpo que en 20261016160000; solo cambia el cálculo de push_enviado.

CREATE OR REPLACE FUNCTION public.crear_aviso(
    p_id_cuenta UUID, p_tipo TEXT, p_id_publicacion BIGINT, p_titulo TEXT, p_datos JSONB DEFAULT NULL)
RETURNS BOOLEAN
LANGUAGE plpgsql
SECURITY DEFINER
SET search_path = ''
AS $$
DECLARE
    v_pref     public.preferencias_notificacion%ROWTYPE;
    v_promo    TEXT[] := ARRAY['resumen_semanal', 'resumen_vendedor', 'auto_parecido', 'poca_actividad', 'confirmar_venta'];
    -- Avisos del panel (administrador y moderadores): sin horario de silencio.
    v_equipo   TEXT[] := ARRAY['publicacion_por_revisar', 'edicion_por_revisar', 'reporte_nuevo',
                               'verificacion_pendiente', 'pago_destacado'];
    v_hora     INT := EXTRACT(HOUR FROM NOW() AT TIME ZONE 'America/Mexico_City');
BEGIN
    IF NOT EXISTS (SELECT 1 FROM public.cuentas WHERE id_cuenta = p_id_cuenta AND estado_cuenta = 'activa') THEN
        RETURN FALSE;
    END IF;

    SELECT * INTO v_pref FROM public.preferencias_notificacion WHERE id_cuenta = p_id_cuenta;
    IF FOUND AND (
        (p_tipo IN ('precio_bajo', 'favorito_no_disponible') AND NOT v_pref.precios)
        OR (p_tipo IN ('resumen_semanal', 'resumen_vendedor') AND NOT v_pref.resumenes)
        OR (p_tipo = 'auto_parecido' AND NOT v_pref.recomendaciones)
        OR (p_tipo IN ('hito_vistas', 'poca_actividad', 'confirmar_venta') AND NOT v_pref.vendedor)
    ) THEN
        RETURN FALSE;
    END IF;

    -- Máximo un aviso promocional por día.
    IF p_tipo = ANY (v_promo) AND EXISTS (
        SELECT 1 FROM public.notificaciones
        WHERE id_cuenta = p_id_cuenta AND tipo = ANY (v_promo)
          AND fecha_creacion > NOW() - INTERVAL '20 hours'
    ) THEN
        RETURN FALSE;
    END IF;

    INSERT INTO public.notificaciones (id_cuenta, tipo, id_publicacion, titulo_vehiculo, datos, push_enviado)
    VALUES (p_id_cuenta, p_tipo, p_id_publicacion, NULLIF(TRIM(p_titulo), ''), p_datos,
            -- De noche se guarda en la bandeja sin push (enviar-push omite los ya "enviados"),
            -- salvo los avisos del panel.
            NOT (p_tipo = ANY (v_equipo)) AND (v_hora >= 22 OR v_hora < 8));
    RETURN TRUE;
END;
$$;

REVOKE EXECUTE ON FUNCTION public.crear_aviso(UUID, TEXT, BIGINT, TEXT, JSONB) FROM PUBLIC, anon, authenticated;


-- ── Publicación o edición por revisar ──────────────────────────────────────
-- Cuando una propuesta queda pendiente (nueva o reenviada), a cada administrador y
-- moderador de publicaciones, menos al dueño de la publicación.

CREATE FUNCTION public.fn_avisar_propuesta_equipo()
RETURNS TRIGGER
LANGUAGE plpgsql
SECURITY DEFINER
SET search_path = ''
AS $$
DECLARE
    v_dueno UUID;
    v_tipo  TEXT;
    r RECORD;
BEGIN
    IF NEW.estado_propuesta IS DISTINCT FROM 'pendiente'
       OR (TG_OP = 'UPDATE' AND OLD.estado_propuesta = 'pendiente') THEN
        RETURN NULL;
    END IF;

    -- Igual que fn_notificar_propuesta: si ya hubo otra versión aprobada, es una edición.
    v_tipo := CASE WHEN EXISTS (
                  SELECT 1 FROM public.propuestas_publicacion x
                  WHERE x.id_publicacion = NEW.id_publicacion
                    AND x.id_propuesta <> NEW.id_propuesta
                    AND x.estado_propuesta = 'aprobada')
              THEN 'edicion_por_revisar' ELSE 'publicacion_por_revisar' END;

    SELECT id_propietario INTO v_dueno
    FROM public.publicaciones WHERE id_publicacion = NEW.id_publicacion;

    FOR r IN SELECT DISTINCT cr.id_cuenta
             FROM public.cuentas_roles cr
             JOIN public.roles ro ON ro.id_rol = cr.id_rol
             WHERE ro.nombre_rol IN ('administrador', 'moderador_publicaciones')
               AND cr.id_cuenta IS DISTINCT FROM v_dueno LOOP
        PERFORM public.crear_aviso(r.id_cuenta, v_tipo, NEW.id_publicacion, NEW.titulo, NULL);
    END LOOP;
    RETURN NULL;
END;
$$;

REVOKE ALL ON FUNCTION public.fn_avisar_propuesta_equipo() FROM PUBLIC, anon, authenticated;

CREATE TRIGGER trg_propuestas_publicacion_avisar_equipo
    AFTER INSERT OR UPDATE OF estado_propuesta ON public.propuestas_publicacion
    FOR EACH ROW EXECUTE FUNCTION public.fn_avisar_propuesta_equipo();


-- ── Reporte nuevo ──────────────────────────────────────────────────────────
-- A cada administrador y moderador de publicaciones, menos a quien reportó.

CREATE FUNCTION public.fn_avisar_reporte_equipo()
RETURNS TRIGGER
LANGUAGE plpgsql
SECURITY DEFINER
SET search_path = ''
AS $$
DECLARE
    v_titulo TEXT;
    r RECORD;
BEGIN
    SELECT titulo INTO v_titulo
    FROM public.publicaciones WHERE id_publicacion = NEW.id_publicacion;

    FOR r IN SELECT DISTINCT cr.id_cuenta
             FROM public.cuentas_roles cr
             JOIN public.roles ro ON ro.id_rol = cr.id_rol
             WHERE ro.nombre_rol IN ('administrador', 'moderador_publicaciones')
               AND cr.id_cuenta <> NEW.id_reportante LOOP
        PERFORM public.crear_aviso(r.id_cuenta, 'reporte_nuevo', NEW.id_publicacion, v_titulo, NULL);
    END LOOP;
    RETURN NULL;
END;
$$;

REVOKE ALL ON FUNCTION public.fn_avisar_reporte_equipo() FROM PUBLIC, anon, authenticated;

CREATE TRIGGER trg_reportes_avisar_equipo
    AFTER INSERT ON public.reportes
    FOR EACH ROW WHEN (NEW.estado_reporte = 'pendiente')
    EXECUTE FUNCTION public.fn_avisar_reporte_equipo();


-- ── Navegadores del equipo (Web Push) ──────────────────────────────────────
-- En un navegador, token es la dirección (endpoint) de la suscripción y p256dh /
-- auth sus llaves. En un teléfono (Firebase) las dos quedan en NULL.

ALTER TABLE public.dispositivos_push
    ADD COLUMN p256dh TEXT NULL,
    ADD COLUMN auth   TEXT NULL,
    ADD CONSTRAINT chk_dispositivos_push_navegador CHECK (
        (p256dh IS NULL AND auth IS NULL)
        OR (token LIKE 'https://%'
            AND p256dh IS NOT NULL AND auth IS NOT NULL
            AND CHAR_LENGTH(p256dh) BETWEEN 60 AND 120
            AND CHAR_LENGTH(auth) BETWEEN 16 AND 40)
    );

COMMENT ON COLUMN public.dispositivos_push.p256dh IS 'Navegador: llave pública de la suscripción Web Push (NULL en teléfonos).';
COMMENT ON COLUMN public.dispositivos_push.auth IS 'Navegador: secreto de la suscripción Web Push (NULL en teléfonos).';

-- Solo el equipo registra navegadores. Si el navegador ya estaba con otra cuenta,
-- pasa a la que está en sesión. Se quita con quitar_dispositivo(endpoint).
CREATE FUNCTION public.registrar_navegador(p_endpoint TEXT, p_p256dh TEXT, p_auth TEXT)
RETURNS VOID
LANGUAGE plpgsql
SECURITY DEFINER
SET search_path = ''
AS $$
BEGIN
    IF auth.uid() IS NULL OR NOT public.es_personal(auth.uid()) THEN
        RAISE EXCEPTION 'Solo el equipo de Karsy recibe notificaciones en el navegador.';
    END IF;
    IF p_endpoint IS NULL OR p_p256dh IS NULL OR p_auth IS NULL THEN
        RAISE EXCEPTION 'Suscripción del navegador incompleta.';
    END IF;

    INSERT INTO public.dispositivos_push (token, id_cuenta, p256dh, auth)
    VALUES (TRIM(p_endpoint), auth.uid(), TRIM(p_p256dh), TRIM(p_auth))
    ON CONFLICT (token) DO UPDATE
        SET id_cuenta = EXCLUDED.id_cuenta,
            p256dh = EXCLUDED.p256dh,
            auth = EXCLUDED.auth,
            fecha_actualizacion = NOW();
END;
$$;

REVOKE EXECUTE ON FUNCTION public.registrar_navegador(TEXT, TEXT, TEXT) FROM PUBLIC, anon;
GRANT EXECUTE ON FUNCTION public.registrar_navegador(TEXT, TEXT, TEXT) TO authenticated;
