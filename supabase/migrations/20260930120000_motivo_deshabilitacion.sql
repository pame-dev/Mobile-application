-- ############################################################################
-- Motivo de deshabilitación visible para el dueño de la publicación
-- ############################################################################
-- Antes el comentario del admin solo quedaba en reportes.motivo_resolucion y en
-- acciones_administrativas, que el dueño no puede leer. Ahora se guarda también
-- en la publicación y se expone en v_anuncios (que respeta el RLS: una
-- publicación deshabilitada solo la ven su dueño y los administradores).

ALTER TABLE public.publicaciones
    ADD COLUMN motivo_deshabilitacion TEXT,
    ADD COLUMN fecha_deshabilitacion  TIMESTAMPTZ;

COMMENT ON COLUMN public.publicaciones.motivo_deshabilitacion IS 'Justificación del administrador al deshabilitar; se muestra al dueño. NULL si está habilitada.';
COMMENT ON COLUMN public.publicaciones.fecha_deshabilitacion IS 'Cuándo la deshabilitó administración. NULL si está habilitada.';

-- Las publicaciones que ya estaban deshabilitadas toman el motivo de la bitácora.
UPDATE public.publicaciones p
SET motivo_deshabilitacion = a.motivo,
    fecha_deshabilitacion  = a.fecha_hora
FROM (
    SELECT DISTINCT ON (id_publicacion_objetivo) id_publicacion_objetivo, motivo, fecha_hora
    FROM public.acciones_administrativas
    WHERE tipo_accion = 'deshabilitar_publicacion'
    ORDER BY id_publicacion_objetivo, fecha_hora DESC
) a
WHERE p.id_publicacion = a.id_publicacion_objetivo
  AND p.estado_administrativo = 'deshabilitada_administrador';


-- Deshabilitar exige un comentario (mín. 10 caracteres) y lo guarda en la
-- publicación; habilitar lo limpia.
CREATE OR REPLACE FUNCTION public.admin_cambiar_estado_publicacion(p_id_publicacion BIGINT, p_habilitar BOOLEAN, p_motivo TEXT DEFAULT NULL)
RETURNS VOID
LANGUAGE plpgsql
SECURITY DEFINER
SET search_path = ''
AS $$
DECLARE
    v_motivo TEXT := NULLIF(TRIM(p_motivo), '');
BEGIN
    PERFORM public.exigir_admin();

    IF NOT p_habilitar AND COALESCE(CHAR_LENGTH(v_motivo), 0) < 10 THEN
        RAISE EXCEPTION 'Escribe un comentario de al menos 10 caracteres explicando por qué se deshabilita.';
    END IF;

    UPDATE public.publicaciones
    SET estado_administrativo  = CASE WHEN p_habilitar THEN 'habilitada' ELSE 'deshabilitada_administrador' END,
        motivo_deshabilitacion = CASE WHEN p_habilitar THEN NULL ELSE v_motivo END,
        fecha_deshabilitacion  = CASE WHEN p_habilitar THEN NULL ELSE NOW() END
    WHERE id_publicacion = p_id_publicacion;

    IF NOT FOUND THEN
        RAISE EXCEPTION 'La publicación no existe.';
    END IF;

    INSERT INTO public.acciones_administrativas (id_admin, tipo_accion, motivo, id_publicacion_objetivo)
    VALUES (auth.uid(),
            CASE WHEN p_habilitar THEN 'habilitar_publicacion' ELSE 'deshabilitar_publicacion' END,
            COALESCE(v_motivo, 'Sin motivo especificado.'),
            p_id_publicacion);
END;
$$;


-- Al deshabilitar desde un reporte, los demás reportes pendientes de la misma
-- publicación se cierran con el mismo motivo.
CREATE OR REPLACE FUNCTION public.admin_resolver_reporte(p_id_reporte BIGINT, p_estado TEXT, p_motivo TEXT, p_deshabilitar BOOLEAN DEFAULT FALSE)
RETURNS VOID
LANGUAGE plpgsql
SECURITY DEFINER
SET search_path = ''
AS $$
DECLARE
    v_id_publicacion BIGINT;
    v_motivo TEXT := COALESCE(NULLIF(TRIM(p_motivo), ''), 'Sin motivo especificado.');
BEGIN
    PERFORM public.exigir_admin();

    IF p_deshabilitar AND CHAR_LENGTH(COALESCE(TRIM(p_motivo), '')) < 10 THEN
        RAISE EXCEPTION 'Escribe un comentario de al menos 10 caracteres explicando por qué se deshabilita.';
    END IF;

    UPDATE public.reportes
    SET estado_reporte = p_estado, id_admin_resolutor = auth.uid(),
        motivo_resolucion = v_motivo, fecha_resolucion = NOW()
    WHERE id_reporte = p_id_reporte AND estado_reporte = 'pendiente'
    RETURNING id_publicacion INTO v_id_publicacion;

    IF v_id_publicacion IS NULL THEN
        RAISE EXCEPTION 'El reporte no existe o ya fue resuelto.';
    END IF;

    INSERT INTO public.acciones_administrativas (id_admin, tipo_accion, motivo, id_reporte_objetivo)
    VALUES (auth.uid(), 'resolver_reporte', v_motivo, p_id_reporte);

    IF p_deshabilitar THEN
        PERFORM public.admin_cambiar_estado_publicacion(v_id_publicacion, FALSE, v_motivo);

        UPDATE public.reportes
        SET estado_reporte = 'atendido', id_admin_resolutor = auth.uid(),
            motivo_resolucion = v_motivo, fecha_resolucion = NOW()
        WHERE id_publicacion = v_id_publicacion AND estado_reporte = 'pendiente';
    END IF;
END;
$$;


CREATE OR REPLACE VIEW public.v_anuncios
WITH (security_invoker = TRUE)
AS
SELECT
    p.id_publicacion,
    p.id_propietario,
    c.nombre_mostrar                       AS propietario_nombre,
    c.tipo_cuenta                          AS propietario_tipo,
    c.foto_perfil                          AS propietario_foto,
    d.titulo,
    d.descripcion,
    d.precio,
    d.precio_negociable,
    d.moneda,
    d.estado_ubicacion,
    d.municipio_ubicacion,
    d.horario_preferido_contacto,
    d.id_marca,
    COALESCE(d.marca_otra, ma.nombre_marca)             AS marca,
    d.id_modelo,
    COALESCE(d.modelo_otro, mo.nombre_modelo)           AS modelo,
    d.anio,
    d.kilometraje,
    d.id_color,
    COALESCE(d.color_otro, co.nombre_color)             AS color,
    d.id_transmision,
    COALESCE(d.transmision_otra, tr.nombre_transmision) AS transmision,
    d.id_carroceria,
    COALESCE(d.carroceria_otra, ca.nombre_carroceria)   AS carroceria,
    d.numero_propietarios_anteriores,
    d.tiene_adeudos,
    d.tiene_problemas,
    d.descripcion_problemas,
    d.recuperado_por_seguro,
    d.tipo_combustible,
    d.numero_puertas,
    d.numero_pasajeros,
    d.cilindros,
    d.cilindrada,
    d.tipo_traccion,
    d.modificaciones,
    p.estado_publicacion,
    p.estado_administrativo,
    p.fecha_creacion,
    p.fecha_primera_publicacion,
    (p.fecha_primera_publicacion IS NOT NULL)            AS aprobada,
    (p.fecha_primera_publicacion IS NOT NULL
        AND p.estado_publicacion = 'activa'
        AND p.estado_administrativo = 'habilitada')      AS visible,
    pr.id_propuesta                                      AS id_ultima_propuesta,
    pr.estado_propuesta                                  AS estado_ultima_propuesta,
    pr.motivo_rechazo                                    AS motivo_rechazo_propuesta,
    EXISTS (
        SELECT 1 FROM public.solicitudes_destacado s
        WHERE s.id_publicacion = p.id_publicacion
          AND s.estado_solicitud = 'aprobada'
          AND NOW() BETWEEN s.fecha_inicio_destacado AND s.fecha_fin_destacado
    )                                                    AS destacado,
    EXISTS (
        SELECT 1 FROM public.solicitudes_destacado s
        WHERE s.id_publicacion = p.id_publicacion
          AND s.estado_solicitud = 'pendiente'
    )                                                    AS destacado_pendiente,
    CASE WHEN p.fecha_primera_publicacion IS NOT NULL THEN
        ARRAY(SELECT f.ruta_storage FROM public.fotos_publicacion f
              WHERE f.id_publicacion = p.id_publicacion AND f.activa
              ORDER BY f.orden_foto)
    ELSE
        ARRAY(SELECT f.ruta_storage FROM public.fotos_propuesta_publicacion f
              WHERE f.id_propuesta = pr.id_propuesta
              ORDER BY f.orden_foto)
    END                                                  AS fotos,
    -- Al final: CREATE OR REPLACE VIEW solo permite agregar columnas al final.
    p.motivo_deshabilitacion,
    p.fecha_deshabilitacion
FROM public.publicaciones p
JOIN public.cuentas c ON c.id_cuenta = p.id_propietario
LEFT JOIN LATERAL (
    SELECT x.*
    FROM public.propuestas_publicacion x
    WHERE x.id_publicacion = p.id_publicacion
    ORDER BY x.fecha_envio_moderacion DESC, x.id_propuesta DESC
    LIMIT 1
) pr ON TRUE
LEFT JOIN LATERAL (
    SELECT p.titulo, p.descripcion, p.precio, p.precio_negociable, p.moneda,
           p.estado_ubicacion, p.municipio_ubicacion, p.horario_preferido_contacto,
           p.id_marca, p.marca_otra, p.id_modelo, p.modelo_otro, p.anio, p.kilometraje,
           p.id_color, p.color_otro, p.id_transmision, p.transmision_otra,
           p.id_carroceria, p.carroceria_otra, p.numero_propietarios_anteriores,
           p.tiene_adeudos, p.tiene_problemas, p.descripcion_problemas,
           p.recuperado_por_seguro, p.tipo_combustible, p.numero_puertas,
           p.numero_pasajeros, p.cilindros, p.cilindrada, p.tipo_traccion, p.modificaciones
    WHERE p.fecha_primera_publicacion IS NOT NULL
    UNION ALL
    SELECT pr.titulo, pr.descripcion, pr.precio, pr.precio_negociable, pr.moneda,
           pr.estado_ubicacion, pr.municipio_ubicacion, pr.horario_preferido_contacto,
           pr.id_marca, pr.marca_otra, pr.id_modelo, pr.modelo_otro, pr.anio, pr.kilometraje,
           pr.id_color, pr.color_otro, pr.id_transmision, pr.transmision_otra,
           pr.id_carroceria, pr.carroceria_otra, pr.numero_propietarios_anteriores,
           pr.tiene_adeudos, pr.tiene_problemas, pr.descripcion_problemas,
           pr.recuperado_por_seguro, pr.tipo_combustible, pr.numero_puertas,
           pr.numero_pasajeros, pr.cilindros, pr.cilindrada, pr.tipo_traccion, pr.modificaciones
    WHERE p.fecha_primera_publicacion IS NULL AND pr.id_propuesta IS NOT NULL
) d ON TRUE
LEFT JOIN public.marcas        ma ON ma.id_marca       = d.id_marca
LEFT JOIN public.modelos       mo ON mo.id_modelo      = d.id_modelo
LEFT JOIN public.colores       co ON co.id_color       = d.id_color
LEFT JOIN public.transmisiones tr ON tr.id_transmision = d.id_transmision
LEFT JOIN public.carrocerias   ca ON ca.id_carroceria  = d.id_carroceria;
