-- ############################################################################
-- "Requiere atención": las cuentas suspendidas también se quitan al verlas
-- ############################################################################
-- Igual que las publicaciones deshabilitadas y pausadas (20261016190000): la alerta
-- "X cuentas suspendidas" cuenta solo las que este admin todavía no ha visto en
-- Usuarios con el filtro "Suspendido" (web o app). El contador 'suspendidas' no cambia.

-- Qué cuentas suspendidas ya vio cada admin.
CREATE TABLE public.admin_cuentas_vistas (
    id_admin   UUID         NOT NULL,
    id_cuenta  UUID         NOT NULL,
    fecha      TIMESTAMPTZ  NOT NULL DEFAULT NOW(),

    CONSTRAINT pk_admin_cuentas_vistas PRIMARY KEY (id_admin, id_cuenta),
    CONSTRAINT fk_admin_cuentas_vistas_admin
        FOREIGN KEY (id_admin) REFERENCES public.cuentas (id_cuenta) ON DELETE CASCADE,
    CONSTRAINT fk_admin_cuentas_vistas_cuenta
        FOREIGN KEY (id_cuenta) REFERENCES public.cuentas (id_cuenta) ON DELETE CASCADE
);

CREATE INDEX ix_admin_cuentas_vistas_id_cuenta ON public.admin_cuentas_vistas (id_cuenta);

COMMENT ON TABLE public.admin_cuentas_vistas IS
    'Cuentas suspendidas que cada admin ya revisó (quita la alerta de "Requiere atención").';

-- Nadie la lee ni escribe directo: solo a través de las funciones admin_* (SECURITY DEFINER).
ALTER TABLE public.admin_cuentas_vistas ENABLE ROW LEVEL SECURITY;
REVOKE ALL ON public.admin_cuentas_vistas FROM anon, authenticated;

-- El admin abrió el filtro "Suspendido": las suspendidas de ahora quedan vistas.
CREATE FUNCTION public.admin_marcar_suspendidas_vistas()
RETURNS VOID
LANGUAGE plpgsql
SECURITY DEFINER
SET search_path = ''
AS $$
BEGIN
    PERFORM public.exigir_admin();

    INSERT INTO public.admin_cuentas_vistas (id_admin, id_cuenta)
    SELECT auth.uid(), c.id_cuenta FROM public.cuentas c WHERE c.estado_cuenta = 'suspendida'
    ON CONFLICT DO NOTHING;
END;
$$;

REVOKE ALL ON FUNCTION public.admin_marcar_suspendidas_vistas() FROM PUBLIC, anon;
GRANT EXECUTE ON FUNCTION public.admin_marcar_suspendidas_vistas() TO authenticated;

-- Si la cuenta se reactiva, deja de contar como vista: si la vuelven a suspender, avisa otra vez.
CREATE FUNCTION public.fn_olvidar_cuenta_vista()
RETURNS TRIGGER
LANGUAGE plpgsql
SECURITY DEFINER
SET search_path = ''
AS $$
BEGIN
    DELETE FROM public.admin_cuentas_vistas WHERE id_cuenta = NEW.id_cuenta;
    RETURN NULL;
END;
$$;

REVOKE ALL ON FUNCTION public.fn_olvidar_cuenta_vista() FROM PUBLIC, anon, authenticated;

CREATE TRIGGER trg_cuentas_olvidar_vista
    AFTER UPDATE OF estado_cuenta ON public.cuentas
    FOR EACH ROW
    WHEN (OLD.estado_cuenta IS DISTINCT FROM NEW.estado_cuenta)
    EXECUTE FUNCTION public.fn_olvidar_cuenta_vista();

-- Misma función que en 20261016190000_alertas_vistas.sql, con el contador nuevo.
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
        'suspendidas_sin_ver',    (SELECT COUNT(*) FROM public.cuentas c
                                   WHERE c.estado_cuenta = 'suspendida'
                                     AND NOT EXISTS (SELECT 1 FROM public.admin_cuentas_vistas v
                                                     WHERE v.id_admin = auth.uid() AND v.id_cuenta = c.id_cuenta)),
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
