-- ############################################################################
-- Marcar una publicación como vendida (dentro o fuera de la app)
-- ############################################################################
-- El dueño marca su publicación como vendida e indica si la venta fue dentro o
-- fuera de Karsy; también puede quitarle la marca para que vuelva a estar
-- disponible.
--
-- vendido y medio_venta se mantienen sincronizados con
-- estado_publicacion = 'vendida', que ya usan las estadísticas, el RLS y la
-- vista: una publicación vendida deja de ser "visible" (no sale en el inicio)
-- pero el público la sigue pudiendo leer, así quien la tenga en favoritos ve
-- que ya se vendió.

ALTER TABLE public.publicaciones
    ADD COLUMN vendido     BOOLEAN     NOT NULL DEFAULT FALSE,
    ADD COLUMN medio_venta VARCHAR(10);

COMMENT ON COLUMN public.publicaciones.vendido IS 'El dueño la marcó como vendida (equivale a estado_publicacion = ''vendida'').';
COMMENT ON COLUMN public.publicaciones.medio_venta IS 'Dónde se vendió: dentro_app o fuera_app. NULL si no está vendida o se marcó antes de existir este campo.';

-- Las que ya estaban vendidas no tienen medio de venta registrado.
UPDATE public.publicaciones SET vendido = TRUE WHERE estado_publicacion = 'vendida';

ALTER TABLE public.publicaciones
    ADD CONSTRAINT chk_publicaciones_medio_venta
        CHECK (medio_venta IN ('dentro_app', 'fuera_app')),
    ADD CONSTRAINT chk_publicaciones_vendido
        CHECK (vendido = (estado_publicacion = 'vendida') AND (vendido OR medio_venta IS NULL));


-- ----------------------------------------------------------------------------
-- El dueño marca (p_vendida = TRUE, con p_medio_venta) o desmarca su
-- publicación como vendida. Al desmarcarla vuelve a estar activa.
--
-- SECURITY DEFINER porque el dueño solo tiene permiso de UPDATE sobre
-- estado_publicacion; la función valida que la publicación sea suya.
-- ----------------------------------------------------------------------------
CREATE FUNCTION public.marcar_publicacion_vendida(p_id_publicacion BIGINT, p_vendida BOOLEAN, p_medio_venta TEXT DEFAULT NULL)
RETURNS VOID
LANGUAGE plpgsql
SECURITY DEFINER
SET search_path = ''
AS $$
DECLARE
    v_pub public.publicaciones%ROWTYPE;
BEGIN
    IF NOT public.es_propietario_publicacion(p_id_publicacion) THEN
        RAISE EXCEPTION 'La publicación no existe.';
    END IF;

    SELECT * INTO v_pub FROM public.publicaciones WHERE id_publicacion = p_id_publicacion FOR UPDATE;

    IF v_pub.fecha_primera_publicacion IS NULL THEN
        RAISE EXCEPTION 'La publicación todavía no ha sido aprobada.';
    END IF;
    IF v_pub.estado_administrativo = 'deshabilitada_administrador' THEN
        RAISE EXCEPTION 'La publicación está deshabilitada por administración.';
    END IF;

    IF p_vendida THEN
        IF p_medio_venta IS NULL OR p_medio_venta NOT IN ('dentro_app', 'fuera_app') THEN
            RAISE EXCEPTION 'Indica si la vendiste dentro o fuera de la app.';
        END IF;
        IF v_pub.vendido THEN
            RAISE EXCEPTION 'La publicación ya está marcada como vendida.';
        END IF;

        UPDATE public.publicaciones
        SET vendido = TRUE, medio_venta = p_medio_venta, estado_publicacion = 'vendida'
        WHERE id_publicacion = p_id_publicacion;

        -- Ya no tiene caso destacar un auto vendido.
        UPDATE public.solicitudes_destacado
        SET estado_solicitud = 'cancelada', fecha_cancelacion = NOW()
        WHERE id_publicacion = p_id_publicacion AND estado_solicitud = 'pendiente';
    ELSE
        IF NOT v_pub.vendido THEN
            RAISE EXCEPTION 'La publicación no está marcada como vendida.';
        END IF;

        UPDATE public.publicaciones
        SET vendido = FALSE, medio_venta = NULL, estado_publicacion = 'activa'
        WHERE id_publicacion = p_id_publicacion;
    END IF;
END;
$$;

REVOKE EXECUTE ON FUNCTION public.marcar_publicacion_vendida(BIGINT, BOOLEAN, TEXT) FROM PUBLIC, anon;
GRANT EXECUTE ON FUNCTION public.marcar_publicacion_vendida(BIGINT, BOOLEAN, TEXT) TO authenticated;


-- ----------------------------------------------------------------------------
-- v_anuncios expone vendido y medio_venta.
-- Igual que en 20261009120000_ver_edicion_publicacion_deshabilitada.sql, con
-- las dos columnas nuevas al final.
-- ----------------------------------------------------------------------------
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
    CASE WHEN NOT u.usar_propuesta THEN
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
    p.fecha_deshabilitacion,
    p.vendido,
    p.medio_venta
FROM public.publicaciones p
JOIN public.cuentas c ON c.id_cuenta = p.id_propietario
LEFT JOIN LATERAL (
    SELECT x.*
    FROM public.propuestas_publicacion x
    WHERE x.id_publicacion = p.id_publicacion
    ORDER BY x.fecha_envio_moderacion DESC, x.id_propuesta DESC
    LIMIT 1
) pr ON TRUE
-- Contenido a mostrar: el de la propuesta si la publicación nunca se aprobó, o
-- si está deshabilitada por administración y el dueño ya la corrigió.
CROSS JOIN LATERAL (
    SELECT pr.id_propuesta IS NOT NULL AND (
               p.fecha_primera_publicacion IS NULL
               OR (p.estado_administrativo = 'deshabilitada_administrador'
                   AND pr.estado_propuesta IN ('pendiente', 'rechazada'))
           ) AS usar_propuesta
) u
LEFT JOIN LATERAL (
    SELECT p.titulo, p.descripcion, p.precio, p.precio_negociable, p.moneda,
           p.estado_ubicacion, p.municipio_ubicacion, p.horario_preferido_contacto,
           p.id_marca, p.marca_otra, p.id_modelo, p.modelo_otro, p.anio, p.kilometraje,
           p.id_color, p.color_otro, p.id_transmision, p.transmision_otra,
           p.id_carroceria, p.carroceria_otra, p.numero_propietarios_anteriores,
           p.tiene_adeudos, p.tiene_problemas, p.descripcion_problemas,
           p.recuperado_por_seguro, p.tipo_combustible, p.numero_puertas,
           p.numero_pasajeros, p.cilindros, p.cilindrada, p.tipo_traccion, p.modificaciones
    WHERE p.fecha_primera_publicacion IS NOT NULL AND NOT u.usar_propuesta
    UNION ALL
    SELECT pr.titulo, pr.descripcion, pr.precio, pr.precio_negociable, pr.moneda,
           pr.estado_ubicacion, pr.municipio_ubicacion, pr.horario_preferido_contacto,
           pr.id_marca, pr.marca_otra, pr.id_modelo, pr.modelo_otro, pr.anio, pr.kilometraje,
           pr.id_color, pr.color_otro, pr.id_transmision, pr.transmision_otra,
           pr.id_carroceria, pr.carroceria_otra, pr.numero_propietarios_anteriores,
           pr.tiene_adeudos, pr.tiene_problemas, pr.descripcion_problemas,
           pr.recuperado_por_seguro, pr.tipo_combustible, pr.numero_puertas,
           pr.numero_pasajeros, pr.cilindros, pr.cilindrada, pr.tipo_traccion, pr.modificaciones
    WHERE u.usar_propuesta
) d ON TRUE
LEFT JOIN public.marcas        ma ON ma.id_marca       = d.id_marca
LEFT JOIN public.modelos       mo ON mo.id_modelo      = d.id_modelo
LEFT JOIN public.colores       co ON co.id_color       = d.id_color
LEFT JOIN public.transmisiones tr ON tr.id_transmision = d.id_transmision
LEFT JOIN public.carrocerias   ca ON ca.id_carroceria  = d.id_carroceria;
