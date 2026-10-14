-- ############################################################################
-- Cambios en vivo (Supabase Realtime) para la web
-- ############################################################################
-- Lo que se hace en la app (publicar, aprobar, favoritos, notificaciones…) llega a la
-- web al momento y la pantalla se recarga sola (web/src/lib/tiempoReal.ts).
-- Realtime respeta la seguridad por filas: cada quien solo recibe cambios de las filas
-- que puede leer con sus políticas SELECT.
--
-- Solo se agregan las tablas que aún no estén (por si alguna se activó desde el panel).

DO $$
DECLARE
    v_tabla TEXT;
BEGIN
    FOREACH v_tabla IN ARRAY ARRAY[
        'publicaciones', 'propuestas_publicacion', 'fotos_publicacion', 'notificaciones',
        'favoritos', 'reportes', 'solicitudes_destacado', 'cuentas'
    ] LOOP
        IF NOT EXISTS (
            SELECT 1 FROM pg_publication_tables
            WHERE pubname = 'supabase_realtime' AND schemaname = 'public' AND tablename = v_tabla
        ) THEN
            EXECUTE format('ALTER PUBLICATION supabase_realtime ADD TABLE public.%I', v_tabla);
        END IF;
    END LOOP;
END;
$$;
