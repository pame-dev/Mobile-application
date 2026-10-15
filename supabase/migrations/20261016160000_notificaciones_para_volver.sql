-- ############################################################################
-- Notificaciones para que compradores y vendedores vuelvan a la app y a la web
-- ############################################################################
-- Compradores
--   precio_bajo            Bajó de precio un auto que guardaste.
--   favorito_no_disponible Un auto que guardaste se vendió o se dio de baja.
--   resumen_semanal        Autos nuevos de la semana en tu estado (y cuántos coinciden
--                          con tus búsquedas guardadas).
--   auto_parecido          Se publicó un auto parecido a los que has visto.
-- Vendedores
--   resumen_vendedor       Vistas y favoritos de tus autos en la semana.
--   hito_vistas            Tu auto llegó a 50 / 100 / 250 / 500 / 1000 vistas.
--   poca_actividad         Tu auto lleva 30 días con pocas vistas.
--   confirmar_venta        ¿Ya vendiste tu auto? (a los 60 días).
--
-- Reglas para no saturar (todas pasan por crear_aviso):
--   * Preferencias por categoría (preferencias_notificacion; sin fila = todo activo).
--   * Máximo un aviso "promocional" al día por cuenta (resúmenes, parecidos,
--     poca actividad, confirmar venta).
--   * Horario de silencio 22:00–8:00 (hora del centro de México): el aviso llega a la
--     bandeja pero sin push (se marca push_enviado y enviar-push lo omite).
-- Los números (precio, vistas…) van en notificaciones.datos; la app y la web arman el
-- texto en el idioma de cada quien.


-- ----------------------------------------------------------------------------
-- Datos del aviso y tipos nuevos
-- ----------------------------------------------------------------------------
ALTER TABLE public.notificaciones ADD COLUMN datos JSONB;
COMMENT ON COLUMN public.notificaciones.datos IS
    'Números del aviso (precio, precio_anterior, vistas, favoritos, nuevos, coincidencias, estado) para armar el texto.';

ALTER TABLE public.notificaciones DROP CONSTRAINT chk_notificaciones_tipo;
ALTER TABLE public.notificaciones ADD CONSTRAINT chk_notificaciones_tipo
    CHECK (tipo IN ('favorito', 'propuesta_enviada', 'publicacion_aprobada', 'destacado_aprobado',
                    'publicacion_rechazada', 'edicion_aprobada', 'edicion_rechazada',
                    'publicacion_deshabilitada', 'publicacion_rehabilitada',
                    'destacado_rechazado', 'reporte_atendido', 'reporte_descartado',
                    'destacado_por_vencer', 'cuenta_suspendida', 'cuenta_reactivada',
                    'busqueda_coincidencia',
                    'precio_bajo', 'favorito_no_disponible', 'resumen_semanal', 'auto_parecido',
                    'resumen_vendedor', 'hito_vistas', 'poca_actividad', 'confirmar_venta'));


-- ----------------------------------------------------------------------------
-- Preferencias (Notificaciones → Elegir qué avisos recibir)
-- ----------------------------------------------------------------------------
-- Los avisos de la cuenta (aprobaciones, rechazos, reportes, suspensión) y las
-- búsquedas guardadas no se pueden apagar: son de su publicación o los pidió.
CREATE TABLE public.preferencias_notificacion (
    id_cuenta            UUID         PRIMARY KEY,
    precios              BOOLEAN      NOT NULL DEFAULT TRUE,
    resumenes            BOOLEAN      NOT NULL DEFAULT TRUE,
    recomendaciones      BOOLEAN      NOT NULL DEFAULT TRUE,
    vendedor             BOOLEAN      NOT NULL DEFAULT TRUE,
    fecha_actualizacion  TIMESTAMPTZ  NOT NULL DEFAULT NOW(),

    CONSTRAINT fk_preferencias_notificacion_cuentas
        FOREIGN KEY (id_cuenta) REFERENCES public.cuentas (id_cuenta) ON DELETE CASCADE
);

COMMENT ON TABLE public.preferencias_notificacion IS
    'Qué avisos opcionales recibe cada cuenta. Sin fila = todos activos.';

ALTER TABLE public.preferencias_notificacion ENABLE ROW LEVEL SECURITY;
CREATE POLICY preferencias_notificacion_leer_propias ON public.preferencias_notificacion
    FOR SELECT TO authenticated USING (id_cuenta = auth.uid());
CREATE POLICY preferencias_notificacion_crear_propias ON public.preferencias_notificacion
    FOR INSERT TO authenticated WITH CHECK (id_cuenta = auth.uid());
CREATE POLICY preferencias_notificacion_editar_propias ON public.preferencias_notificacion
    FOR UPDATE TO authenticated USING (id_cuenta = auth.uid()) WITH CHECK (id_cuenta = auth.uid());
GRANT SELECT, INSERT, UPDATE ON public.preferencias_notificacion TO authenticated;


-- ----------------------------------------------------------------------------
-- Crear un aviso respetando preferencias, tope diario y horario de silencio
-- ----------------------------------------------------------------------------
CREATE FUNCTION public.crear_aviso(
    p_id_cuenta UUID, p_tipo TEXT, p_id_publicacion BIGINT, p_titulo TEXT, p_datos JSONB DEFAULT NULL)
RETURNS BOOLEAN
LANGUAGE plpgsql
SECURITY DEFINER
SET search_path = ''
AS $$
DECLARE
    v_pref     public.preferencias_notificacion%ROWTYPE;
    v_promo    TEXT[] := ARRAY['resumen_semanal', 'resumen_vendedor', 'auto_parecido', 'poca_actividad', 'confirmar_venta'];
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
            -- De noche se guarda en la bandeja sin push (enviar-push omite los ya "enviados").
            v_hora >= 22 OR v_hora < 8);
    RETURN TRUE;
END;
$$;

REVOKE EXECUTE ON FUNCTION public.crear_aviso(UUID, TEXT, BIGINT, TEXT, JSONB) FROM PUBLIC, anon, authenticated;


-- ----------------------------------------------------------------------------
-- Compradores: bajó de precio / ya no está disponible un favorito
-- ----------------------------------------------------------------------------
CREATE FUNCTION public.fn_avisar_cambios_favoritos()
RETURNS TRIGGER
LANGUAGE plpgsql
SECURITY DEFINER
SET search_path = ''
AS $$
DECLARE
    v_antes_visible BOOLEAN := OLD.fecha_primera_publicacion IS NOT NULL
        AND OLD.estado_publicacion = 'activa' AND OLD.estado_administrativo = 'habilitada';
    v_ahora_visible BOOLEAN := NEW.fecha_primera_publicacion IS NOT NULL
        AND NEW.estado_publicacion = 'activa' AND NEW.estado_administrativo = 'habilitada';
    r RECORD;
BEGIN
    IF v_antes_visible AND NOT v_ahora_visible THEN
        FOR r IN SELECT f.id_cuenta FROM public.favoritos f
                 WHERE f.id_publicacion = NEW.id_publicacion AND f.id_cuenta <> NEW.id_propietario LOOP
            PERFORM public.crear_aviso(r.id_cuenta, 'favorito_no_disponible', NEW.id_publicacion, NEW.titulo);
        END LOOP;
    ELSIF v_antes_visible AND v_ahora_visible AND NEW.precio < OLD.precio THEN
        FOR r IN SELECT f.id_cuenta FROM public.favoritos f
                 WHERE f.id_publicacion = NEW.id_publicacion AND f.id_cuenta <> NEW.id_propietario LOOP
            PERFORM public.crear_aviso(r.id_cuenta, 'precio_bajo', NEW.id_publicacion, NEW.titulo,
                jsonb_build_object('precio', NEW.precio, 'precio_anterior', OLD.precio));
        END LOOP;
    END IF;
    RETURN NULL;
END;
$$;

CREATE TRIGGER trg_publicaciones_avisar_favoritos
    AFTER UPDATE OF precio, estado_publicacion, estado_administrativo ON public.publicaciones
    FOR EACH ROW EXECUTE FUNCTION public.fn_avisar_cambios_favoritos();


-- ----------------------------------------------------------------------------
-- Compradores: se publicó un auto parecido a los que viste
-- ----------------------------------------------------------------------------
-- Al publicarse por primera vez: a quien vio en los últimos 30 días al menos 2 autos
-- distintos del mismo tipo (carrocería) o de la misma marca. No se repite si ya le
-- llegó el aviso de búsqueda guardada de este auto (ese trigger corre antes: los
-- triggers se ejecutan en orden alfabético).
CREATE FUNCTION public.fn_avisar_autos_parecidos()
RETURNS TRIGGER
LANGUAGE plpgsql
SECURITY DEFINER
SET search_path = ''
AS $$
DECLARE
    r RECORD;
BEGIN
    IF NEW.estado_publicacion <> 'activa' OR NEW.estado_administrativo <> 'habilitada' THEN
        RETURN NULL;
    END IF;

    FOR r IN
        SELECT i.id_cuenta
        FROM public.interacciones_publicacion i
        JOIN public.publicaciones p ON p.id_publicacion = i.id_publicacion
        WHERE i.tipo_interaccion = 'ver_detalle'
          AND i.fecha_hora > NOW() - INTERVAL '30 days'
          AND i.id_cuenta IS NOT NULL
          AND i.id_cuenta <> NEW.id_propietario
          AND i.id_publicacion <> NEW.id_publicacion
          AND (p.id_carroceria = NEW.id_carroceria OR p.id_marca = NEW.id_marca)
          AND NOT EXISTS (
              SELECT 1 FROM public.notificaciones n
              WHERE n.id_cuenta = i.id_cuenta AND n.id_publicacion = NEW.id_publicacion)
        GROUP BY i.id_cuenta
        HAVING COUNT(DISTINCT i.id_publicacion) >= 2
        LIMIT 500
    LOOP
        PERFORM public.crear_aviso(r.id_cuenta, 'auto_parecido', NEW.id_publicacion, NEW.titulo);
    END LOOP;
    RETURN NULL;
END;
$$;

CREATE TRIGGER trg_publicaciones_notificar_parecidos
    AFTER UPDATE OF fecha_primera_publicacion ON public.publicaciones
    FOR EACH ROW
    WHEN (OLD.fecha_primera_publicacion IS NULL AND NEW.fecha_primera_publicacion IS NOT NULL)
    EXECUTE FUNCTION public.fn_avisar_autos_parecidos();


-- ----------------------------------------------------------------------------
-- Vendedores: hito de vistas
-- ----------------------------------------------------------------------------
CREATE FUNCTION public.fn_avisar_hito_vistas()
RETURNS TRIGGER
LANGUAGE plpgsql
SECURITY DEFINER
SET search_path = ''
AS $$
DECLARE
    v_pub   public.publicaciones%ROWTYPE;
    v_total INT;
BEGIN
    SELECT * INTO v_pub FROM public.publicaciones WHERE id_publicacion = NEW.id_publicacion;
    -- Las vistas del propio dueño no cuentan.
    IF NOT FOUND OR NEW.id_cuenta IS NOT DISTINCT FROM v_pub.id_propietario THEN
        RETURN NULL;
    END IF;

    SELECT COUNT(*) INTO v_total
    FROM public.interacciones_publicacion
    WHERE id_publicacion = NEW.id_publicacion AND tipo_interaccion = 'ver_detalle'
      AND id_cuenta IS DISTINCT FROM v_pub.id_propietario;

    -- Cada hito una sola vez por auto (aunque lleguen varias vistas al mismo tiempo).
    IF v_total IN (50, 100, 250, 500, 1000) AND NOT EXISTS (
        SELECT 1 FROM public.notificaciones n
        WHERE n.id_publicacion = v_pub.id_publicacion AND n.tipo = 'hito_vistas'
          AND (n.datos ->> 'vistas')::INT = v_total
    ) THEN
        PERFORM public.crear_aviso(v_pub.id_propietario, 'hito_vistas', v_pub.id_publicacion, v_pub.titulo,
            jsonb_build_object('vistas', v_total));
    END IF;
    RETURN NULL;
END;
$$;

CREATE TRIGGER trg_interacciones_avisar_hito
    AFTER INSERT ON public.interacciones_publicacion
    FOR EACH ROW
    WHEN (NEW.tipo_interaccion = 'ver_detalle')
    EXECUTE FUNCTION public.fn_avisar_hito_vistas();


-- ----------------------------------------------------------------------------
-- Tareas programadas
-- ----------------------------------------------------------------------------
-- Compradores, domingo: autos nuevos de la semana en su estado.
CREATE FUNCTION public.notificar_resumen_semanal()
RETURNS INT
LANGUAGE plpgsql
SECURITY DEFINER
SET search_path = ''
AS $$
DECLARE
    v_total INT := 0;
    r RECORD;
BEGIN
    FOR r IN
        SELECT c.id_cuenta, c.estado_perfil_ubi AS estado,
               COUNT(p.id_publicacion) AS nuevos,
               COUNT(p.id_publicacion) FILTER (WHERE EXISTS (
                   SELECT 1 FROM public.busquedas_guardadas b
                   WHERE b.id_cuenta = c.id_cuenta
                     AND (b.id_marca IS NULL OR b.id_marca = p.id_marca)
                     AND (b.id_modelo IS NULL OR b.id_modelo = p.id_modelo)
                     AND (b.anio IS NULL OR b.anio = p.anio)
                     AND (b.id_carroceria IS NULL OR b.id_carroceria = p.id_carroceria)
                     AND (b.precio_min IS NULL OR p.precio >= b.precio_min)
                     AND (b.precio_max IS NULL OR p.precio <= b.precio_max)
                     AND (b.estado_ubicacion IS NULL OR b.estado_ubicacion = p.estado_ubicacion)
               )) AS coincidencias
        FROM public.cuentas c
        JOIN public.publicaciones p
          ON p.estado_ubicacion = c.estado_perfil_ubi
         AND p.id_propietario <> c.id_cuenta
         AND p.fecha_primera_publicacion > NOW() - INTERVAL '7 days'
         AND p.estado_publicacion = 'activa' AND p.estado_administrativo = 'habilitada'
        WHERE c.estado_cuenta = 'activa'
        GROUP BY c.id_cuenta, c.estado_perfil_ubi
    LOOP
        IF public.crear_aviso(r.id_cuenta, 'resumen_semanal', NULL, NULL,
               jsonb_build_object('nuevos', r.nuevos, 'coincidencias', r.coincidencias, 'estado', r.estado)) THEN
            v_total := v_total + 1;
        END IF;
    END LOOP;
    RETURN v_total;
END;
$$;

-- Vendedores, lunes: vistas y favoritos de sus autos en la semana.
CREATE FUNCTION public.notificar_resumen_vendedor()
RETURNS INT
LANGUAGE plpgsql
SECURITY DEFINER
SET search_path = ''
AS $$
DECLARE
    v_total INT := 0;
    r RECORD;
BEGIN
    FOR r IN
        SELECT p.id_propietario,
               (SELECT COUNT(*) FROM public.interacciones_publicacion i
                JOIN public.publicaciones x ON x.id_publicacion = i.id_publicacion
                WHERE x.id_propietario = p.id_propietario AND i.tipo_interaccion = 'ver_detalle'
                  AND i.fecha_hora > NOW() - INTERVAL '7 days'
                  AND i.id_cuenta IS DISTINCT FROM p.id_propietario) AS vistas,
               (SELECT COUNT(*) FROM public.favoritos f
                JOIN public.publicaciones x ON x.id_publicacion = f.id_publicacion
                WHERE x.id_propietario = p.id_propietario
                  AND f.fecha_guardado > NOW() - INTERVAL '7 days') AS favoritos
        FROM public.publicaciones p
        WHERE p.fecha_primera_publicacion IS NOT NULL
          AND p.estado_publicacion = 'activa' AND p.estado_administrativo = 'habilitada'
        GROUP BY p.id_propietario
    LOOP
        IF r.vistas > 0 AND public.crear_aviso(r.id_propietario, 'resumen_vendedor', NULL, NULL,
               jsonb_build_object('vistas', r.vistas, 'favoritos', r.favoritos)) THEN
            v_total := v_total + 1;
        END IF;
    END LOOP;
    RETURN v_total;
END;
$$;

-- Vendedores, diario: auto con poca actividad a los 30 días y "¿ya lo vendiste?" a los 60.
-- Cada aviso una vez por auto (si el tope diario lo deja fuera, se intenta otro día).
CREATE FUNCTION public.notificar_actividad_vendedores()
RETURNS INT
LANGUAGE plpgsql
SECURITY DEFINER
SET search_path = ''
AS $$
DECLARE
    v_total INT := 0;
    r RECORD;
BEGIN
    FOR r IN
        SELECT p.id_publicacion, p.id_propietario, p.titulo, p.fecha_primera_publicacion
        FROM public.publicaciones p
        WHERE p.estado_publicacion = 'activa' AND p.estado_administrativo = 'habilitada'
          AND p.fecha_primera_publicacion <= NOW() - INTERVAL '30 days'
    LOOP
        IF r.fecha_primera_publicacion <= NOW() - INTERVAL '60 days'
           AND NOT EXISTS (SELECT 1 FROM public.notificaciones n
                           WHERE n.id_publicacion = r.id_publicacion AND n.tipo = 'confirmar_venta'
                             AND n.fecha_creacion > NOW() - INTERVAL '60 days') THEN
            IF public.crear_aviso(r.id_propietario, 'confirmar_venta', r.id_publicacion, r.titulo) THEN
                v_total := v_total + 1;
            END IF;
        ELSIF NOT EXISTS (SELECT 1 FROM public.notificaciones n
                          WHERE n.id_publicacion = r.id_publicacion AND n.tipo = 'poca_actividad')
           AND (SELECT COUNT(*) FROM public.interacciones_publicacion i
                WHERE i.id_publicacion = r.id_publicacion AND i.tipo_interaccion = 'ver_detalle'
                  AND i.fecha_hora > NOW() - INTERVAL '30 days'
                  AND i.id_cuenta IS DISTINCT FROM r.id_propietario) < 20 THEN
            IF public.crear_aviso(r.id_propietario, 'poca_actividad', r.id_publicacion, r.titulo) THEN
                v_total := v_total + 1;
            END IF;
        END IF;
    END LOOP;
    RETURN v_total;
END;
$$;

REVOKE EXECUTE ON FUNCTION public.notificar_resumen_semanal(), public.notificar_resumen_vendedor(),
    public.notificar_actividad_vendedores() FROM PUBLIC, anon, authenticated;

-- Horas en UTC (centro de México = UTC-6): domingo y lunes 10:00, diario 11:00.
CREATE EXTENSION IF NOT EXISTS pg_cron;

SELECT cron.schedule('resumen-semanal-compradores', '0 16 * * 0', $$SELECT public.notificar_resumen_semanal()$$);
SELECT cron.schedule('resumen-semanal-vendedores',  '0 16 * * 1', $$SELECT public.notificar_resumen_vendedor()$$);
SELECT cron.schedule('actividad-vendedores',        '0 17 * * *', $$SELECT public.notificar_actividad_vendedores()$$);
