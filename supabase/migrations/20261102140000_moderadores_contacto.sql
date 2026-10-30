-- ############################################################################
-- Moderadores: también ven el contacto del vendedor
-- ############################################################################
-- contacto_vendedor devolvía NULL a un moderador en publicaciones que el público
-- no ve (pendientes de revisión o deshabilitadas); ahora basta con ser del equipo
-- (es_personal, de 20261102120000_moderadores.sql). Cuerpo igual al de
-- 20261015120000_normalizacion_integridad.sql; solo cambia es_administrador → es_personal.

CREATE OR REPLACE FUNCTION public.contacto_vendedor(p_id_publicacion bigint) RETURNS jsonb
    LANGUAGE plpgsql STABLE SECURITY DEFINER
    SET search_path TO ''
    AS $$
DECLARE
    v_propietario UUID;
    v_resultado   JSONB;
BEGIN
    SELECT id_propietario INTO v_propietario
    FROM public.publicaciones
    WHERE id_publicacion = p_id_publicacion
      AND (
          (fecha_primera_publicacion IS NOT NULL AND estado_administrativo = 'habilitada')
          OR id_propietario = auth.uid()
          OR public.es_personal(auth.uid())
      );

    IF v_propietario IS NULL THEN
        RETURN NULL;
    END IF;

    SELECT jsonb_build_object(
        'id_cuenta',       c.id_cuenta,
        'nombre',          c.nombre_mostrar,
        'tipo_cuenta',     c.tipo_cuenta,
        'descripcion',     c.descripcion_corta,
        'ubicacion',       c.municipio_perfil_ubi || ', ' || c.estado_perfil_ubi,
        'foto_perfil',     c.foto_perfil,
        'miembro_desde',   c.fecha_creacion,
        'telefono',        CASE WHEN auth.uid() IS NOT NULL THEN tl.numero_telefono END,
        'whatsapp',        CASE WHEN auth.uid() IS NOT NULL THEN tl.es_whatsapp END,
        'numero_whatsapp', CASE WHEN auth.uid() IS NOT NULL THEN wa.numero_telefono END,
        'medio_principal', CASE WHEN auth.uid() IS NOT NULL THEN c.medio_contacto_principal END,
        'correo',          CASE WHEN auth.uid() IS NOT NULL THEN u.email END
    )
    INTO v_resultado
    FROM public.cuentas c
    JOIN auth.users u ON u.id = c.id_cuenta
    LEFT JOIN LATERAL (
        SELECT numero_telefono, es_whatsapp
        FROM public.telefonos_contacto
        WHERE id_cuenta = c.id_cuenta AND activo AND permite_llamadas
        ORDER BY es_principal DESC, id_telefono
        LIMIT 1
    ) tl ON TRUE
    LEFT JOIN LATERAL (
        SELECT numero_telefono
        FROM public.telefonos_contacto
        WHERE id_cuenta = c.id_cuenta AND activo AND es_whatsapp
        ORDER BY es_principal DESC, id_telefono
        LIMIT 1
    ) wa ON TRUE
    WHERE c.id_cuenta = v_propietario;

    RETURN v_resultado;
END;
$$;
