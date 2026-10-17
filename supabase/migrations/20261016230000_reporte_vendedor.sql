-- ############################################################################
-- Reporte de "Mi panel" (Excel y PDF): datos extra que solo se piden al exportar
-- ############################################################################
-- vendedor_reporte() calcula, para las publicaciones del vendedor en sesión:
--   * mercado: precio promedio de autos similares de OTROS vendedores (misma marca y
--     modelo del catálogo, año ±2, misma moneda, activos y habilitados);
--   * por_dia / por_hora: vistas de detalle por día de la semana (domingo = 0) y por
--     hora, en hora de México;
--   * destacados: vistas antes y durante el último destacado aprobado de cada auto.
--     "Durante" va del inicio a hoy (o al fin); "antes" mide el mismo tiempo exacto hacia
--     atrás, pero sin empezar antes de que el auto se publicara. Se devuelven los días de
--     cada ventana para comparar vistas por día.
-- No crea tablas ni columnas: solo lee lo que ya existe.

CREATE FUNCTION public.vendedor_reporte()
RETURNS JSONB
LANGUAGE plpgsql
STABLE
SECURITY DEFINER
SET search_path = ''
AS $$
DECLARE
    v_uid UUID := auth.uid();
BEGIN
    IF v_uid IS NULL THEN
        RAISE EXCEPTION 'Inicia sesión para ver tu panel.';
    END IF;

    RETURN jsonb_build_object(
        'mercado', (
            SELECT COALESCE(jsonb_agg(jsonb_build_object(
                       'id', p.id_publicacion, 'promedio', m.promedio, 'similares', m.n)), '[]'::JSONB)
            FROM public.publicaciones p
            CROSS JOIN LATERAL (
                SELECT ROUND(AVG(q.precio)) AS promedio, COUNT(*) AS n
                FROM public.publicaciones q
                WHERE q.id_propietario <> v_uid
                  AND q.fecha_primera_publicacion IS NOT NULL
                  AND q.estado_publicacion = 'activa'
                  AND q.estado_administrativo = 'habilitada'
                  AND q.id_marca = p.id_marca
                  AND q.id_modelo = p.id_modelo
                  AND q.moneda = p.moneda
                  AND ABS(q.anio - p.anio) <= 2
                  AND q.precio IS NOT NULL
            ) m
            WHERE p.id_propietario = v_uid
              AND p.fecha_primera_publicacion IS NOT NULL
              AND m.n > 0
        ),
        'por_dia', (
            SELECT jsonb_agg(COALESCE(x.total, 0) ORDER BY d.dia)
            FROM generate_series(0, 6) AS d(dia)
            LEFT JOIN (
                SELECT EXTRACT(DOW FROM i.fecha_hora AT TIME ZONE 'America/Mexico_City')::INT AS dia, COUNT(*) AS total
                FROM public.interacciones_publicacion i
                JOIN public.publicaciones p ON p.id_publicacion = i.id_publicacion
                WHERE p.id_propietario = v_uid AND i.tipo_interaccion = 'ver_detalle'
                GROUP BY 1
            ) x ON x.dia = d.dia
        ),
        'por_hora', (
            SELECT jsonb_agg(COALESCE(x.total, 0) ORDER BY h.hora)
            FROM generate_series(0, 23) AS h(hora)
            LEFT JOIN (
                SELECT EXTRACT(HOUR FROM i.fecha_hora AT TIME ZONE 'America/Mexico_City')::INT AS hora, COUNT(*) AS total
                FROM public.interacciones_publicacion i
                JOIN public.publicaciones p ON p.id_publicacion = i.id_publicacion
                WHERE p.id_propietario = v_uid AND i.tipo_interaccion = 'ver_detalle'
                GROUP BY 1
            ) x ON x.hora = h.hora
        ),
        'destacados', (
            SELECT COALESCE(jsonb_agg(jsonb_build_object(
                       'id', s.id_publicacion,
                       'inicio', s.fecha_inicio_destacado,
                       'fin', s.fecha_fin_destacado,
                       'dias', GREATEST(1, ROUND(EXTRACT(EPOCH FROM s.duracion) / 86400))::INT,
                       'dias_antes', ROUND((EXTRACT(EPOCH FROM (s.fecha_inicio_destacado - s.desde_antes)) / 86400)::NUMERIC, 2),
                       'dias_durante', ROUND((EXTRACT(EPOCH FROM s.duracion) / 86400)::NUMERIC, 2),
                       'vistas_antes', (SELECT COUNT(*) FROM public.interacciones_publicacion i
                                        WHERE i.id_publicacion = s.id_publicacion AND i.tipo_interaccion = 'ver_detalle'
                                          AND i.fecha_hora >= s.desde_antes
                                          AND i.fecha_hora <  s.fecha_inicio_destacado),
                       'vistas_durante', (SELECT COUNT(*) FROM public.interacciones_publicacion i
                                          WHERE i.id_publicacion = s.id_publicacion AND i.tipo_interaccion = 'ver_detalle'
                                            AND i.fecha_hora >= s.fecha_inicio_destacado
                                            AND i.fecha_hora <  LEAST(s.fecha_fin_destacado, NOW())))), '[]'::JSONB)
            FROM (
                -- El último destacado aprobado que ya empezó, por publicación.
                SELECT DISTINCT ON (sd.id_publicacion)
                       sd.id_publicacion, sd.fecha_inicio_destacado, sd.fecha_fin_destacado,
                       LEAST(sd.fecha_fin_destacado, NOW()) - sd.fecha_inicio_destacado AS duracion,
                       GREATEST(
                           sd.fecha_inicio_destacado - (LEAST(sd.fecha_fin_destacado, NOW()) - sd.fecha_inicio_destacado),
                           p.fecha_primera_publicacion
                       ) AS desde_antes
                FROM public.solicitudes_destacado sd
                JOIN public.publicaciones p ON p.id_publicacion = sd.id_publicacion
                WHERE p.id_propietario = v_uid
                  AND sd.estado_solicitud = 'aprobada'
                  AND sd.fecha_inicio_destacado IS NOT NULL
                  AND sd.fecha_fin_destacado IS NOT NULL
                  AND sd.fecha_inicio_destacado <= NOW()
                ORDER BY sd.id_publicacion, sd.fecha_inicio_destacado DESC
            ) s
        )
    );
END;
$$;

COMMENT ON FUNCTION public.vendedor_reporte() IS
    'Datos extra del reporte de Mi panel: precio vs mercado, vistas por día/hora y antes/durante el destacado.';

REVOKE EXECUTE ON FUNCTION public.vendedor_reporte() FROM PUBLIC, anon;
GRANT EXECUTE ON FUNCTION public.vendedor_reporte() TO authenticated;
