-- ############################################################################
-- Corrige admin_reporte: "Exportar" del panel de administración daba error
-- ############################################################################
-- En "mas_vistas" se agrupaba por el alias "titulo", pero publicaciones también tiene
-- una columna "titulo" y Postgres agrupaba por esa columna; la consulta fallaba con
-- "ma.nombre_marca must appear in the GROUP BY clause". Ahora se agrupa por las
-- columnas reales. Además, la marca escrita a mano se muestra igual que en "marcas"
-- (sin espacios de más y con mayúscula inicial). El resto queda igual que en 20261016240000.

CREATE OR REPLACE FUNCTION public.admin_reporte(p_dias INT DEFAULT 30)
RETURNS JSONB
LANGUAGE plpgsql
STABLE
SECURITY DEFINER
SET search_path = ''
AS $$
DECLARE
    v_desde TIMESTAMPTZ := NOW() - make_interval(days => LEAST(GREATEST(p_dias, 1), 366));
BEGIN
    PERFORM public.exigir_admin();

    RETURN jsonb_build_object(
        'usuarios', (
            SELECT jsonb_build_object(
                'total',       COUNT(*),
                'nuevos',      COUNT(*) FILTER (WHERE c.fecha_creacion >= v_desde),
                'particulares', COUNT(*) FILTER (WHERE c.tipo_cuenta = 'particular'),
                'lotes',       COUNT(*) FILTER (WHERE c.tipo_cuenta = 'lote'),
                'suspendidos', COUNT(*) FILTER (WHERE c.estado_cuenta = 'suspendida'))
            FROM public.cuentas c
        ),
        'publicaciones', (
            SELECT jsonb_build_object(
                'publicadas',     COUNT(*) FILTER (WHERE p.fecha_primera_publicacion IS NOT NULL),
                'nuevas',         COUNT(*) FILTER (WHERE p.fecha_primera_publicacion >= v_desde),
                'activas',        COUNT(*) FILTER (WHERE p.fecha_primera_publicacion IS NOT NULL
                                                     AND p.estado_publicacion = 'activa' AND p.estado_administrativo = 'habilitada'),
                'pausadas',       COUNT(*) FILTER (WHERE p.fecha_primera_publicacion IS NOT NULL
                                                     AND p.estado_publicacion = 'deshabilitada' AND p.estado_administrativo = 'habilitada'),
                -- Mismas categorías que la app (no se traslapan): deshabilitada por admin gana.
                'deshabilitadas', COUNT(*) FILTER (WHERE p.fecha_primera_publicacion IS NOT NULL
                                                     AND p.estado_administrativo = 'deshabilitada_administrador'),
                'vendidas',       COUNT(*) FILTER (WHERE p.estado_publicacion = 'vendida' AND p.estado_administrativo = 'habilitada'),
                'vendidas_dentro', COUNT(*) FILTER (WHERE p.estado_publicacion = 'vendida' AND p.estado_administrativo = 'habilitada'
                                                      AND p.medio_venta = 'dentro_app'),
                'vendidas_fuera', COUNT(*) FILTER (WHERE p.estado_publicacion = 'vendida' AND p.estado_administrativo = 'habilitada'
                                                     AND p.medio_venta = 'fuera_app'))
            FROM public.publicaciones p
        ),
        'interes', (
            SELECT jsonb_build_object(
                'vistas',    COUNT(*) FILTER (WHERE i.tipo_interaccion = 'ver_detalle'),
                'contactos', COUNT(*) FILTER (WHERE i.tipo_interaccion = 'contactar'))
            FROM public.interacciones_publicacion i
            WHERE i.fecha_hora >= v_desde
        ),
        'favoritos', (SELECT COUNT(*) FROM public.favoritos f WHERE f.fecha_guardado >= v_desde),
        'destacados', (
            SELECT jsonb_build_object(
                'pendientes', COUNT(*) FILTER (WHERE s.estado_solicitud = 'pendiente'),
                -- Vigentes = los que se ven en portada (la publicación sigue activa y habilitada).
                'vigentes',   COUNT(*) FILTER (WHERE s.estado_solicitud = 'aprobada'
                                                 AND NOW() BETWEEN s.fecha_inicio_destacado AND s.fecha_fin_destacado
                                                 AND EXISTS (SELECT 1 FROM public.publicaciones p
                                                             WHERE p.id_publicacion = s.id_publicacion
                                                               AND p.estado_publicacion = 'activa'
                                                               AND p.estado_administrativo = 'habilitada')),
                'aprobados',  COUNT(*) FILTER (WHERE s.estado_solicitud = 'aprobada' AND s.fecha_resolucion >= v_desde),
                'rechazados', COUNT(*) FILTER (WHERE s.estado_solicitud = 'rechazada' AND s.fecha_resolucion >= v_desde))
            FROM public.solicitudes_destacado s
        ),
        'reportes', (
            SELECT jsonb_build_object(
                'pendientes', COUNT(*) FILTER (WHERE r.estado_reporte = 'pendiente'),
                'recibidos',  COUNT(*) FILTER (WHERE r.fecha_reporte >= v_desde),
                'resueltos',  COUNT(*) FILTER (WHERE r.estado_reporte <> 'pendiente' AND r.fecha_resolucion >= v_desde))
            FROM public.reportes r
        ),
        -- Revisión de publicaciones nuevas y ediciones en el rango.
        'revision', (
            SELECT jsonb_build_object(
                'pendientes', (SELECT COUNT(*) FROM public.propuestas_publicacion x WHERE x.estado_propuesta = 'pendiente'),
                'aprobadas',  COUNT(*) FILTER (WHERE pr.estado_propuesta = 'aprobada'),
                'rechazadas', COUNT(*) FILTER (WHERE pr.estado_propuesta = 'rechazada'),
                'horas_promedio', ROUND((AVG(EXTRACT(EPOCH FROM (pr.fecha_revision - pr.fecha_envio_moderacion))) / 3600)::NUMERIC, 1))
            FROM public.propuestas_publicacion pr
            WHERE pr.fecha_revision >= v_desde AND pr.estado_propuesta IN ('aprobada', 'rechazada')
        ),
        'marcas', (
            SELECT COALESCE(jsonb_agg(jsonb_build_object('nombre', m.nombre, 'publicaciones', m.n) ORDER BY m.n DESC, m.nombre), '[]'::JSONB)
            FROM (
                -- Las marcas escritas a mano se unifican ("toyota" y "Toyota " cuentan juntas).
                SELECT COALESCE(INITCAP(NULLIF(TRIM(p.marca_otra), '')), ma.nombre_marca) AS nombre, COUNT(*) AS n
                FROM public.publicaciones p
                LEFT JOIN public.marcas ma ON ma.id_marca = p.id_marca
                WHERE p.fecha_primera_publicacion IS NOT NULL
                  AND p.estado_publicacion = 'activa' AND p.estado_administrativo = 'habilitada'
                GROUP BY 1
                ORDER BY 2 DESC, 1
                LIMIT 10
            ) m
        ),
        'estados', (
            SELECT COALESCE(jsonb_agg(jsonb_build_object('nombre', e.nombre, 'publicaciones', e.n) ORDER BY e.n DESC, e.nombre), '[]'::JSONB)
            FROM (
                SELECT p.estado_ubicacion AS nombre, COUNT(*) AS n
                FROM public.publicaciones p
                WHERE p.fecha_primera_publicacion IS NOT NULL AND p.estado_ubicacion IS NOT NULL
                  AND p.estado_publicacion = 'activa' AND p.estado_administrativo = 'habilitada'
                GROUP BY 1
                ORDER BY 2 DESC, 1
                LIMIT 10
            ) e
        ),
        'mas_vistas', (
            SELECT COALESCE(jsonb_agg(jsonb_build_object(
                       'id', v.id_publicacion, 'titulo', v.titulo, 'vendedor', v.vendedor,
                       'vistas', v.vistas, 'contactos', v.contactos) ORDER BY v.vistas DESC, v.contactos DESC, v.id_publicacion), '[]'::JSONB)
            FROM (
                SELECT p.id_publicacion,
                       TRIM(CONCAT_WS(' ', COALESCE(INITCAP(NULLIF(TRIM(p.marca_otra), '')), ma.nombre_marca),
                                     COALESCE(NULLIF(TRIM(p.modelo_otro), ''), mo.nombre_modelo), p.anio::TEXT)) AS titulo,
                       c.nombre_mostrar AS vendedor,
                       COUNT(*) FILTER (WHERE i.tipo_interaccion = 'ver_detalle') AS vistas,
                       COUNT(*) FILTER (WHERE i.tipo_interaccion = 'contactar') AS contactos
                FROM public.interacciones_publicacion i
                JOIN public.publicaciones p ON p.id_publicacion = i.id_publicacion
                JOIN public.cuentas c ON c.id_cuenta = p.id_propietario
                LEFT JOIN public.marcas ma ON ma.id_marca = p.id_marca
                LEFT JOIN public.modelos mo ON mo.id_modelo = p.id_modelo
                WHERE i.fecha_hora >= v_desde
                -- Se agrupa por las columnas (no por el alias "titulo": publicaciones tiene una
                -- columna con ese nombre y Postgres usaría esa).
                GROUP BY p.id_publicacion, ma.nombre_marca, mo.nombre_modelo, c.nombre_mostrar
                ORDER BY vistas DESC, contactos DESC, p.id_publicacion
                LIMIT 10
            ) v
        ),
        'por_dia', (
            SELECT jsonb_agg(COALESCE(x.total, 0) ORDER BY d.dia)
            FROM generate_series(0, 6) AS d(dia)
            LEFT JOIN (
                SELECT EXTRACT(DOW FROM i.fecha_hora AT TIME ZONE 'America/Mexico_City')::INT AS dia, COUNT(*) AS total
                FROM public.interacciones_publicacion i
                WHERE i.tipo_interaccion = 'ver_detalle' AND i.fecha_hora >= v_desde
                GROUP BY 1
            ) x ON x.dia = d.dia
        ),
        'por_hora', (
            SELECT jsonb_agg(COALESCE(x.total, 0) ORDER BY h.hora)
            FROM generate_series(0, 23) AS h(hora)
            LEFT JOIN (
                SELECT EXTRACT(HOUR FROM i.fecha_hora AT TIME ZONE 'America/Mexico_City')::INT AS hora, COUNT(*) AS total
                FROM public.interacciones_publicacion i
                WHERE i.tipo_interaccion = 'ver_detalle' AND i.fecha_hora >= v_desde
                GROUP BY 1
            ) x ON x.hora = h.hora
        ),
        'motivos', (
            SELECT COALESCE(jsonb_agg(jsonb_build_object('motivo', m.motivo, 'reportes', m.n) ORDER BY m.n DESC, m.motivo), '[]'::JSONB)
            FROM (
                SELECT r.motivo_reporte AS motivo, COUNT(*) AS n
                FROM public.reportes r
                WHERE r.fecha_reporte >= v_desde
                GROUP BY 1
                ORDER BY 2 DESC, 1
                LIMIT 8
            ) m
        )
    );
END;
$$;
