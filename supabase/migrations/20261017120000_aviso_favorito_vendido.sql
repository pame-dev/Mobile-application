-- ############################################################################
-- Aviso de favorito vendido
-- ############################################################################
-- Cuando un auto guardado en favoritos deja de estar disponible, el aviso
-- favorito_no_disponible ahora lleva en datos el estado: 'vendida' si se vendió, o
-- 'deshabilitada' si su dueño lo pausó o el admin lo deshabilitó. Con eso la app y la web
-- dicen "se vendió" en vez de "ya no está disponible". El resto de la función queda
-- igual que en 20261016160000; el trigger trg_publicaciones_avisar_favoritos no cambia.

CREATE OR REPLACE FUNCTION public.fn_avisar_cambios_favoritos()
RETURNS TRIGGER
LANGUAGE plpgsql
SECURITY DEFINER
SET search_path = ''
AS $$
DECLARE
    v_antes_visible BOOLEAN := OLD.fecha_primera_publicacion IS NOT NULL
        AND OLD.estado_publicacion = 'activa' AND OLD.estado_administrativo = 'habilitada';
    v_ahora_visible BOOLEAN := NEW.fecha_primera_publicacion IS NOT NULL
        AND NEW.estado_publicacion = 'activa' AND NEW.estado_administrativo = 'habilitada';
    r RECORD;
BEGIN
    IF v_antes_visible AND NOT v_ahora_visible THEN
        FOR r IN SELECT f.id_cuenta FROM public.favoritos f
                 WHERE f.id_publicacion = NEW.id_publicacion AND f.id_cuenta <> NEW.id_propietario LOOP
            -- El estado ('vendida' o 'deshabilitada') deja que la app y la web digan "se vendió".
            PERFORM public.crear_aviso(r.id_cuenta, 'favorito_no_disponible', NEW.id_publicacion, NEW.titulo,
                jsonb_build_object('estado', CASE WHEN NEW.estado_administrativo <> 'habilitada'
                                                  THEN 'deshabilitada' ELSE NEW.estado_publicacion END));
        END LOOP;
    ELSIF v_antes_visible AND v_ahora_visible AND NEW.precio < OLD.precio THEN
        FOR r IN SELECT f.id_cuenta FROM public.favoritos f
                 WHERE f.id_publicacion = NEW.id_publicacion AND f.id_cuenta <> NEW.id_propietario LOOP
            PERFORM public.crear_aviso(r.id_cuenta, 'precio_bajo', NEW.id_publicacion, NEW.titulo,
                jsonb_build_object('precio', NEW.precio, 'precio_anterior', OLD.precio));
        END LOOP;
    END IF;
    RETURN NULL;
END;
$$;
