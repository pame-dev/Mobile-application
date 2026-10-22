-- ############################################################################
-- Panel de administración: escudo de verificado en la lista de usuarios
-- ############################################################################
-- admin_listar_cuentas (20261025120000) ahora también devuelve verificado (cuentas.verificado),
-- para que Usuarios muestre el escudo azul junto al nombre de las cuentas verificadas.
-- El resto de la función queda igual. Cambia el tipo que devuelve, por eso se borra y se
-- vuelve a crear (nadie más depende de ella).

DROP FUNCTION public.admin_listar_cuentas();

CREATE FUNCTION public.admin_listar_cuentas()
RETURNS TABLE (
    id_cuenta         UUID,
    nombre_mostrar    TEXT,
    correo            TEXT,
    tipo_cuenta       TEXT,
    estado_cuenta     TEXT,
    fecha_creacion    TIMESTAMPTZ,
    es_admin          BOOLEAN,
    municipio         TEXT,
    estado            TEXT,
    responsable       TEXT,
    publicaciones     BIGINT,
    activas           BIGINT,
    foto_perfil       TEXT,
    verificado        BOOLEAN
)
LANGUAGE plpgsql
STABLE
SECURITY DEFINER
SET search_path = ''
AS $$
BEGIN
    PERFORM public.exigir_admin();

    RETURN QUERY
    SELECT
        c.id_cuenta,
        c.nombre_mostrar::TEXT,
        u.email::TEXT,
        c.tipo_cuenta::TEXT,
        c.estado_cuenta::TEXT,
        c.fecha_creacion,
        public.es_administrador(c.id_cuenta),
        c.municipio_perfil_ubi::TEXT,
        c.estado_perfil_ubi::TEXT,
        COALESCE(u.raw_user_meta_data ->> 'responsable', c.nombre_mostrar)::TEXT,
        (SELECT COUNT(*) FROM public.publicaciones p WHERE p.id_propietario = c.id_cuenta),
        (SELECT COUNT(*) FROM public.publicaciones p
         WHERE p.id_propietario = c.id_cuenta AND p.fecha_primera_publicacion IS NOT NULL
           AND p.estado_publicacion = 'activa' AND p.estado_administrativo = 'habilitada'),
        c.foto_perfil::TEXT,
        c.verificado
    FROM public.cuentas c
    JOIN auth.users u ON u.id = c.id_cuenta
    ORDER BY c.fecha_creacion DESC;
END;
$$;

REVOKE ALL ON FUNCTION public.admin_listar_cuentas() FROM PUBLIC, anon;
GRANT EXECUTE ON FUNCTION public.admin_listar_cuentas() TO authenticated;
