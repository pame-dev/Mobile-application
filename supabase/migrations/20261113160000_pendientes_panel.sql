-- ############################################################################
-- Pendientes del panel en un solo número (para el menú de la cuenta)
-- ############################################################################
-- La app y la web muestran junto a "Panel de administración" (y un puntito en el
-- avatar) cuántas cosas esperan revisión. Fuera del panel no conviene cargar las
-- listas completas, así que esta función solo cuenta lo que el rol de quien llama
-- puede atender:
--   moderador_publicaciones   propuestas de publicación y reportes pendientes
--   moderador_verificaciones  solicitudes de destacado y verificaciones pendientes
--   administrador             todo lo anterior
-- Para cualquier otra cuenta devuelve 0. No crea tablas ni cambia permisos existentes.


CREATE FUNCTION public.panel_pendientes()
RETURNS INTEGER
LANGUAGE sql
STABLE
SECURITY DEFINER
SET search_path = ''
AS $$
    SELECT (
        CASE WHEN public.es_moderador_actual('moderador_publicaciones') THEN
            (SELECT COUNT(*) FROM public.propuestas_publicacion WHERE estado_propuesta = 'pendiente')
          + (SELECT COUNT(*) FROM public.reportes WHERE estado_reporte = 'pendiente')
        ELSE 0 END
      + CASE WHEN public.es_moderador_actual('moderador_verificaciones') THEN
            (SELECT COUNT(*) FROM public.solicitudes_destacado WHERE estado_solicitud = 'pendiente')
          + (SELECT COUNT(*) FROM public.verificaciones_usuario WHERE estado = 'pendiente')
        ELSE 0 END
    )::INTEGER;
$$;

REVOKE ALL ON FUNCTION public.panel_pendientes() FROM PUBLIC, anon;
GRANT EXECUTE ON FUNCTION public.panel_pendientes() TO authenticated;
