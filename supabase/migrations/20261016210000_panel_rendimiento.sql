-- ############################################################################
-- Mi panel: "Cómo van tus publicaciones" en lugar de "Interés en la semana"
-- ############################################################################
-- vendedor_estadisticas agrega dos datos de los últimos 30 días:
--   * embudo: vistas -> favoritos -> contactos de todas las publicaciones del vendedor;
--   * rendimiento: vistas, favoritos y contactos de cada publicación.
-- El resto de la función queda igual (la app y la web siguen leyendo lo de antes).

CREATE OR REPLACE FUNCTION public.vendedor_estadisticas()
RETURNS JSONB
LANGUAGE plpgsql
STABLE
SECURITY DEFINER
SET search_path = ''
AS $$
DECLARE
    v_uid   UUID := auth.uid();
    v_dias  TEXT[] := ARRAY['D', 'L', 'M', 'X', 'J', 'V', 'S'];
    v_desde TIMESTAMPTZ := NOW() - INTERVAL '30 days';
BEGIN
    IF v_uid IS NULL THEN
        RAISE EXCEPTION 'Inicia sesión para ver tu panel.';
    END IF;

    RETURN jsonb_build_object(
        'publicadas', (SELECT COUNT(*) FROM public.publicaciones
                       WHERE id_propietario = v_uid AND fecha_primera_publicacion IS NOT NULL),
        'activas',    (SELECT COUNT(*) FROM public.publicaciones
                       WHERE id_propietario = v_uid AND fecha_primera_publicacion IS NOT NULL
                         AND estado_publicacion = 'activa' AND estado_administrativo = 'habilitada'),
        'vendidas',   (SELECT COUNT(*) FROM public.publicaciones
                       WHERE id_propietario = v_uid AND estado_publicacion = 'vendida'),
        'pendientes', (SELECT COUNT(*) FROM public.propuestas_publicacion pr
                       JOIN public.publicaciones p ON p.id_publicacion = pr.id_publicacion
                       WHERE p.id_propietario = v_uid AND pr.estado_propuesta = 'pendiente'),
        'vistas',     (SELECT COUNT(*) FROM public.interacciones_publicacion i
                       JOIN public.publicaciones p ON p.id_publicacion = i.id_publicacion
                       WHERE p.id_propietario = v_uid AND i.tipo_interaccion = 'ver_detalle'),
        'contactos',  (SELECT COUNT(*) FROM public.interacciones_publicacion i
                       JOIN public.publicaciones p ON p.id_publicacion = i.id_publicacion
                       WHERE p.id_propietario = v_uid AND i.tipo_interaccion = 'contactar'),
        'vistas_mes', (SELECT COUNT(*) FROM public.interacciones_publicacion i
                       JOIN public.publicaciones p ON p.id_publicacion = i.id_publicacion
                       WHERE p.id_propietario = v_uid AND i.tipo_interaccion = 'ver_detalle'
                         AND i.fecha_hora >= date_trunc('month', NOW())),
        'vistas_mes_anterior', (SELECT COUNT(*) FROM public.interacciones_publicacion i
                       JOIN public.publicaciones p ON p.id_publicacion = i.id_publicacion
                       WHERE p.id_propietario = v_uid AND i.tipo_interaccion = 'ver_detalle'
                         AND i.fecha_hora >= date_trunc('month', NOW()) - INTERVAL '1 month'
                         AND i.fecha_hora <  date_trunc('month', NOW())),
        -- 12 valores, del mes más antiguo al actual.
        'vistas_por_mes', (
            SELECT jsonb_agg(COALESCE(x.total, 0) ORDER BY m.mes)
            FROM generate_series(date_trunc('month', NOW()) - INTERVAL '11 months',
                                 date_trunc('month', NOW()), INTERVAL '1 month') AS m(mes)
            LEFT JOIN (
                SELECT date_trunc('month', i.fecha_hora) AS mes, COUNT(*) AS total
                FROM public.interacciones_publicacion i
                JOIN public.publicaciones p ON p.id_publicacion = i.id_publicacion
                WHERE p.id_propietario = v_uid AND i.tipo_interaccion = 'ver_detalle'
                GROUP BY 1
            ) x ON x.mes = m.mes
        ),
        -- Últimos 7 días, del más antiguo a hoy.
        'favoritos_semana', (
            SELECT jsonb_agg(COALESCE(x.total, 0) ORDER BY d.dia)
            FROM generate_series(CURRENT_DATE - 6, CURRENT_DATE, INTERVAL '1 day') AS d(dia)
            LEFT JOIN (
                SELECT f.fecha_guardado::DATE AS dia, COUNT(*) AS total
                FROM public.favoritos f
                JOIN public.publicaciones p ON p.id_publicacion = f.id_publicacion
                WHERE p.id_propietario = v_uid
                GROUP BY 1
            ) x ON x.dia = d.dia
        ),
        'dias_semana', (
            SELECT jsonb_agg(v_dias[EXTRACT(DOW FROM d.dia)::INT + 1] ORDER BY d.dia)
            FROM generate_series(CURRENT_DATE - 6, CURRENT_DATE, INTERVAL '1 day') AS d(dia)
        ),
        -- Embudo de los últimos 30 días: cuántas vistas terminan en favorito o en contacto
        -- (mismas publicaciones que 'rendimiento': las que ya se publicaron).
        'embudo', jsonb_build_object(
            'vistas',    (SELECT COUNT(*) FROM public.interacciones_publicacion i
                          JOIN public.publicaciones p ON p.id_publicacion = i.id_publicacion
                          WHERE p.id_propietario = v_uid AND p.fecha_primera_publicacion IS NOT NULL AND i.tipo_interaccion = 'ver_detalle'
                            AND i.fecha_hora >= v_desde),
            'favoritos', (SELECT COUNT(*) FROM public.favoritos f
                          JOIN public.publicaciones p ON p.id_publicacion = f.id_publicacion
                          WHERE p.id_propietario = v_uid AND p.fecha_primera_publicacion IS NOT NULL AND f.fecha_guardado >= v_desde),
            'contactos', (SELECT COUNT(*) FROM public.interacciones_publicacion i
                          JOIN public.publicaciones p ON p.id_publicacion = i.id_publicacion
                          WHERE p.id_propietario = v_uid AND p.fecha_primera_publicacion IS NOT NULL AND i.tipo_interaccion = 'contactar'
                            AND i.fecha_hora >= v_desde)
        ),
        -- Cómo va cada publicación en los últimos 30 días (la más vista primero). Solo el
        -- id: el nombre y la foto los toma la app o la web de la lista que ya cargó.
        'rendimiento', (
            SELECT COALESCE(jsonb_agg(jsonb_build_object(
                       'id', r.id_publicacion, 'vistas', r.vistas, 'favoritos', r.favoritos, 'contactos', r.contactos)
                       ORDER BY r.vistas DESC, r.contactos DESC, r.id_publicacion DESC), '[]'::JSONB)
            FROM (
                SELECT p.id_publicacion,
                       (SELECT COUNT(*) FROM public.interacciones_publicacion i
                        WHERE i.id_publicacion = p.id_publicacion AND i.tipo_interaccion = 'ver_detalle'
                          AND i.fecha_hora >= v_desde) AS vistas,
                       (SELECT COUNT(*) FROM public.favoritos f
                        WHERE f.id_publicacion = p.id_publicacion AND f.fecha_guardado >= v_desde) AS favoritos,
                       (SELECT COUNT(*) FROM public.interacciones_publicacion i
                        WHERE i.id_publicacion = p.id_publicacion AND i.tipo_interaccion = 'contactar'
                          AND i.fecha_hora >= v_desde) AS contactos
                FROM public.publicaciones p
                WHERE p.id_propietario = v_uid AND p.fecha_primera_publicacion IS NOT NULL
            ) r
        )
    );
END;
$$;
