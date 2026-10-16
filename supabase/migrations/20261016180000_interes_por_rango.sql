-- ############################################################################
-- La gráfica "Interés en publicaciones" sigue el rango elegido (7, 30 o 90 días)
-- ############################################################################
-- Antes, admin_estadisticas siempre devolvía las interacciones de los últimos 6
-- meses, así que al cambiar el rango en el panel solo cambiaba "Crecimiento".
-- Ahora vistas y contactos se cuentan dentro del rango. El resto de la función
-- queda igual. Lo usan la web y la app (mismo formato de respuesta).

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
