-- ############################################################################
-- Borrar de Storage las fotos que una edición aprobada dejó sin usar
-- ############################################################################
-- Al aprobar una edición, admin_moderar_propuesta solo marca la galería anterior
-- como activa = FALSE; los archivos se quedaban en el bucket (públicos para quien
-- tuviera el enlace). Eso importa sobre todo con las placas: la app y la web suben
-- la versión tapada y la original, con placa, seguía guardada.
--
-- La BD no puede borrar archivos del bucket (eso lo hace la API de Storage), así que:
--   1. admin_fotos_retiradas_sin_uso() da las rutas que ya nadie usa.
--   2. La app/web del admin las borra con la API justo después de aprobar.
--      (AdminRepository.moderateProposal / web/src/data/admin.ts)
--
-- Una foto retirada puede seguir en uso: las ediciones reutilizan la ruta de las
-- fotos que no cambiaron, y una propuesta pendiente o rechazada (que se puede
-- reenviar) puede tenerla. Esas no se tocan.


-- El admin puede borrar archivos del bucket de cualquier cuenta. Para borrar, Storage
-- primero busca el archivo con el rol del usuario, así que también necesita verlo
-- (las fotos se siguen sirviendo por su URL pública; esto solo es para la API).
CREATE POLICY karsy_ver_admin ON storage.objects
    FOR SELECT TO authenticated
    USING (bucket_id = 'karsy' AND public.es_admin_actual());

CREATE POLICY karsy_borrar_admin ON storage.objects
    FOR DELETE TO authenticated
    USING (bucket_id = 'karsy' AND public.es_admin_actual());


CREATE FUNCTION public.admin_fotos_retiradas_sin_uso()
RETURNS TEXT[]
LANGUAGE plpgsql
STABLE
SECURITY DEFINER
SET search_path = ''
AS $$
BEGIN
    PERFORM public.exigir_admin();

    RETURN ARRAY(
        SELECT DISTINCT f.ruta_storage
        FROM public.fotos_publicacion f
        WHERE NOT f.activa
          -- Solo archivos del bucket (las fotos de prueba con link externo no).
          AND f.ruta_storage LIKE 'publicaciones/%'
          AND NOT EXISTS (
              SELECT 1 FROM public.fotos_publicacion a
              WHERE a.activa AND a.ruta_storage = f.ruta_storage)
          AND NOT EXISTS (
              SELECT 1
              FROM public.fotos_propuesta_publicacion fp
              JOIN public.propuestas_publicacion pr ON pr.id_propuesta = fp.id_propuesta
              WHERE fp.ruta_storage = f.ruta_storage
                AND pr.estado_propuesta IN ('pendiente', 'rechazada'))
          -- Las que ya se borraron no vuelven a salir.
          AND EXISTS (
              SELECT 1 FROM storage.objects o
              WHERE o.bucket_id = 'karsy' AND o.name = f.ruta_storage)
        LIMIT 100
    );
END;
$$;

REVOKE EXECUTE ON FUNCTION public.admin_fotos_retiradas_sin_uso() FROM PUBLIC, anon;
GRANT EXECUTE ON FUNCTION public.admin_fotos_retiradas_sin_uso() TO authenticated;
