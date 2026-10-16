-- ############################################################################
-- "Requiere atención": deshabilitadas y pausadas por separado, y que se quiten al verlas
-- ############################################################################
-- Antes la alerta "X publicaciones deshabilitadas" sumaba las que deshabilitó un
-- admin y las que pausó su dueño, pero en Vehículos se ven en filtros distintos
-- ("Deshabilitado" y "Pausado"). Ahora son dos alertas, y cada una cuenta solo las
-- publicaciones que el admin todavía no ha visto en ese filtro (web o app).
-- El contador 'deshabilitadas' (tarjeta "Estado de publicaciones") no cambia.

-- Publicaciones que se ven como "Deshabilitado" o "Pausado" en Vehículos (mismas
-- reglas que la web y la app: aprobada, sin cambios pendientes de revisar).
CREATE FUNCTION public.admin_publicaciones_en_estado(p_estado TEXT)
RETURNS SETOF BIGINT
LANGUAGE sql
STABLE
SET search_path = ''
AS $$
    SELECT p.id_publicacion
    FROM public.publicaciones p
    WHERE p.fecha_primera_publicacion IS NOT NULL
      AND COALESCE((SELECT x.estado_propuesta FROM public.propuestas_publicacion x
                    WHERE x.id_publicacion = p.id_publicacion
                    ORDER BY x.fecha_envio_moderacion DESC, x.id_propuesta DESC
                    LIMIT 1), '') <> 'pendiente'
      AND CASE p_estado
              WHEN 'deshabilitada' THEN p.estado_administrativo = 'deshabilitada_administrador'
              WHEN 'pausada' THEN p.estado_administrativo <> 'deshabilitada_administrador'
                              AND p.estado_publicacion = 'deshabilitada'
              ELSE FALSE
          END;
$$;

-- Solo la usan las funciones admin_* de abajo (no se llama desde la app ni la web).
REVOKE ALL ON FUNCTION public.admin_publicaciones_en_estado(TEXT) FROM PUBLIC, anon, authenticated;

-- Qué publicaciones deshabilitadas o pausadas ya vio cada admin.
CREATE TABLE public.admin_publicaciones_vistas (
    id_admin        UUID         NOT NULL,
    estado          VARCHAR(16)  NOT NULL,
    id_publicacion  BIGINT       NOT NULL,
    fecha           TIMESTAMPTZ  NOT NULL DEFAULT NOW(),

    CONSTRAINT pk_admin_publicaciones_vistas PRIMARY KEY (id_admin, estado, id_publicacion),
    CONSTRAINT chk_admin_publicaciones_vistas_estado CHECK (estado IN ('deshabilitada', 'pausada')),
    CONSTRAINT fk_admin_publicaciones_vistas_cuentas
        FOREIGN KEY (id_admin) REFERENCES public.cuentas (id_cuenta) ON DELETE CASCADE,
    CONSTRAINT fk_admin_publicaciones_vistas_publicaciones
        FOREIGN KEY (id_publicacion) REFERENCES public.publicaciones (id_publicacion) ON DELETE CASCADE
);

CREATE INDEX ix_admin_publicaciones_vistas_id_publicacion ON public.admin_publicaciones_vistas (id_publicacion);

COMMENT ON TABLE public.admin_publicaciones_vistas IS
    'Publicaciones deshabilitadas o pausadas que cada admin ya revisó (quita la alerta de "Requiere atención").';

-- Nadie la lee ni escribe directo: solo a través de las funciones admin_* (SECURITY DEFINER).
ALTER TABLE public.admin_publicaciones_vistas ENABLE ROW LEVEL SECURITY;
REVOKE ALL ON public.admin_publicaciones_vistas FROM anon, authenticated;

-- El admin abrió el filtro "Deshabilitado" o "Pausado": las que hay ahora quedan vistas.
-- Las que ya salieron de ese estado se olvidan (el trigger de abajo lo hace al momento).
CREATE FUNCTION public.admin_marcar_vistas(p_estado TEXT)
RETURNS VOID
LANGUAGE plpgsql
SECURITY DEFINER
SET search_path = ''
AS $$
BEGIN
    PERFORM public.exigir_admin();
    IF p_estado IS NULL OR p_estado NOT IN ('deshabilitada', 'pausada') THEN
        RAISE EXCEPTION 'Estado no válido.';
    END IF;

    DELETE FROM public.admin_publicaciones_vistas v
    WHERE v.id_admin = auth.uid() AND v.estado = p_estado
      AND v.id_publicacion NOT IN (SELECT e.id FROM public.admin_publicaciones_en_estado(p_estado) e(id));

    INSERT INTO public.admin_publicaciones_vistas (id_admin, estado, id_publicacion)
    SELECT auth.uid(), p_estado, e.id FROM public.admin_publicaciones_en_estado(p_estado) e(id)
    ON CONFLICT DO NOTHING;
END;
$$;

REVOKE ALL ON FUNCTION public.admin_marcar_vistas(TEXT) FROM PUBLIC, anon;
GRANT EXECUTE ON FUNCTION public.admin_marcar_vistas(TEXT) TO authenticated;

-- Si una publicación cambia de estado (p. ej. el dueño la reactiva y la vuelve a pausar),
-- deja de contar como vista: al regresar a "Deshabilitado" o "Pausado" avisa otra vez.
CREATE FUNCTION public.fn_olvidar_publicacion_vista()
RETURNS TRIGGER
LANGUAGE plpgsql
SECURITY DEFINER
SET search_path = ''
AS $$
BEGIN
    DELETE FROM public.admin_publicaciones_vistas WHERE id_publicacion = NEW.id_publicacion;
    RETURN NULL;
END;
$$;

REVOKE ALL ON FUNCTION public.fn_olvidar_publicacion_vista() FROM PUBLIC, anon, authenticated;

CREATE TRIGGER trg_publicaciones_olvidar_vista
    AFTER UPDATE OF estado_publicacion, estado_administrativo ON public.publicaciones
    FOR EACH ROW
    WHEN (OLD.estado_publicacion IS DISTINCT FROM NEW.estado_publicacion
          OR OLD.estado_administrativo IS DISTINCT FROM NEW.estado_administrativo)
    EXECUTE FUNCTION public.fn_olvidar_publicacion_vista();

-- Misma función que en 20261016180000_interes_por_rango.sql, con los dos contadores nuevos.
CREATE OR REPLACE FUNCTION public.admin_estadisticas(p_dias INT DEFAULT 7)
RETURNS JSONB
LANGUAGE plpgsql
STABLE
SECURITY DEFINER
SET search_path = ''
AS $$
DECLARE
    v_desde  TIMESTAMPTZ := NOW() - make_interval(days => p_dias);
    v_meses  TEXT[] := ARRAY['ene','feb','mar','abr','may','jun','jul','ago','sep','oct','nov','dic'];
    -- Interés en publicaciones: días de México; barras de 1 día (hasta 14 días),
    -- 1 semana (hasta 31) o 2 semanas (más), para no pasar de unas 7 barras (rango máximo: 366 días).
    v_hoy    DATE := (NOW() AT TIME ZONE 'America/Mexico_City')::DATE;
    v_paso   INT  := CASE WHEN p_dias <= 14 THEN 1 WHEN p_dias <= 31 THEN 7 ELSE 14 END;
BEGIN
    PERFORM public.exigir_admin();

    RETURN jsonb_build_object(
        'usuarios',        (SELECT COUNT(*) FROM public.cuentas),
        'usuarios_nuevos', (SELECT COUNT(*) FROM public.cuentas WHERE fecha_creacion >= v_desde),
        'lotes',           (SELECT COUNT(*) FROM public.cuentas WHERE tipo_cuenta = 'lote'),
        'lotes_nuevos',    (SELECT COUNT(*) FROM public.cuentas WHERE tipo_cuenta = 'lote' AND fecha_creacion >= v_desde),
        'publicadas',      (SELECT COUNT(*) FROM public.publicaciones WHERE fecha_primera_publicacion IS NOT NULL),
        'publicadas_nuevas', (SELECT COUNT(*) FROM public.publicaciones WHERE fecha_primera_publicacion >= v_desde),
        'activas',         (SELECT COUNT(*) FROM public.publicaciones
                            WHERE fecha_primera_publicacion IS NOT NULL
                              AND estado_publicacion = 'activa' AND estado_administrativo = 'habilitada'),
        'deshabilitadas',  (SELECT COUNT(*) FROM public.publicaciones
                            WHERE estado_administrativo = 'deshabilitada_administrador'
                               OR estado_publicacion = 'deshabilitada'),
        'suspendidas',     (SELECT COUNT(*) FROM public.cuentas WHERE estado_cuenta = 'suspendida'),
        -- "Requiere atención": solo las que este admin todavía no ha visto.
        'deshabilitadas_sin_ver', (SELECT COUNT(*) FROM public.admin_publicaciones_en_estado('deshabilitada') e(id)
                                   WHERE NOT EXISTS (SELECT 1 FROM public.admin_publicaciones_vistas v
                                                     WHERE v.id_admin = auth.uid() AND v.estado = 'deshabilitada' AND v.id_publicacion = e.id)),
        'pausadas_sin_ver',       (SELECT COUNT(*) FROM public.admin_publicaciones_en_estado('pausada') e(id)
                                   WHERE NOT EXISTS (SELECT 1 FROM public.admin_publicaciones_vistas v
                                                     WHERE v.id_admin = auth.uid() AND v.estado = 'pausada' AND v.id_publicacion = e.id)),
        'propuestas_pendientes', (SELECT COUNT(*) FROM public.propuestas_publicacion WHERE estado_propuesta = 'pendiente'),
        'reportes_pendientes',   (SELECT COUNT(*) FROM public.reportes WHERE estado_reporte = 'pendiente'),
        'destacados_pendientes', (SELECT COUNT(*) FROM public.solicitudes_destacado WHERE estado_solicitud = 'pendiente'),
        -- 7 puntos repartidos en el rango: totales acumulados a cada fecha.
        'crecimiento', (
            SELECT jsonb_build_object(
                'etiquetas',     jsonb_agg(EXTRACT(DAY FROM t.corte)::INT || ' ' || v_meses[EXTRACT(MONTH FROM t.corte)::INT] ORDER BY t.k),
                'usuarios',      jsonb_agg((SELECT COUNT(*) FROM public.cuentas c WHERE c.fecha_creacion <= t.corte) ORDER BY t.k),
                'lotes',         jsonb_agg((SELECT COUNT(*) FROM public.cuentas c WHERE c.tipo_cuenta = 'lote' AND c.fecha_creacion <= t.corte) ORDER BY t.k),
                'publicaciones', jsonb_agg((SELECT COUNT(*) FROM public.publicaciones p WHERE p.fecha_primera_publicacion <= t.corte) ORDER BY t.k)
            )
            FROM (
                SELECT k, NOW() - make_interval(secs => (p_dias * 86400.0) * (6 - k) / 6) AS corte
                FROM generate_series(0, 6) AS k
            ) t
        ),
        -- Vistas de detalle y contactos dentro del mismo rango (7, 30 o 90 días).
        -- Cada barra se etiqueta con su primer día; la más antigua puede ser más corta.
        'interacciones', (
            SELECT jsonb_build_object(
                'etiquetas', jsonb_agg(EXTRACT(DAY FROM b.inicio)::INT || ' ' || v_meses[EXTRACT(MONTH FROM b.inicio)::INT] ORDER BY b.inicio),
                'vistas',    jsonb_agg((SELECT COUNT(*) FROM public.interacciones_publicacion i
                                        WHERE i.tipo_interaccion = 'ver_detalle'
                                          AND (i.fecha_hora AT TIME ZONE 'America/Mexico_City')::DATE BETWEEN b.inicio AND b.fin) ORDER BY b.inicio),
                'contactos', jsonb_agg((SELECT COUNT(*) FROM public.interacciones_publicacion i
                                        WHERE i.tipo_interaccion = 'contactar'
                                          AND (i.fecha_hora AT TIME ZONE 'America/Mexico_City')::DATE BETWEEN b.inicio AND b.fin) ORDER BY b.inicio)
            )
            FROM (
                SELECT GREATEST(v_hoy - (k * v_paso + v_paso - 1), v_hoy - (LEAST(GREATEST(p_dias, 1), 366) - 1)) AS inicio,
                       v_hoy - k * v_paso AS fin
                FROM generate_series(0, CEIL(LEAST(GREATEST(p_dias, 1), 366)::NUMERIC / v_paso)::INT - 1) AS k
            ) b
        )
    );
END;
$$;
