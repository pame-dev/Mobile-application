-- ############################################################################
-- Aprobar la edición de una publicación deshabilitada la rehabilita
-- ############################################################################
-- Caso: el admin deshabilita una publicación, el dueño la corrige (queda como
-- "Edición sin revisar") y el admin aprueba los cambios. Antes seguía
-- deshabilitada y había que rehabilitarla aparte; ahora aprobar la edición
-- corregida la vuelve a habilitar.
--
-- Solo aplica a publicaciones deshabilitadas por administración. Si el dueño la
-- pausó (estado_publicacion = 'deshabilitada'), sigue pausada: eso lo decide él.
--
-- Se usa admin_cambiar_estado_publicacion, así queda en la bitácora, se borra el
-- motivo de deshabilitación y el dueño recibe el aviso "publicación rehabilitada"
-- (después de "edición aprobada", por el orden alfabético de los triggers).


CREATE FUNCTION public.fn_rehabilitar_al_aprobar_propuesta()
RETURNS TRIGGER
LANGUAGE plpgsql
SECURITY DEFINER
SET search_path = ''
AS $$
BEGIN
    IF NEW.estado_propuesta <> 'aprobada' OR OLD.estado_propuesta = 'aprobada' THEN
        RETURN NULL;
    END IF;

    IF EXISTS (
        SELECT 1 FROM public.publicaciones
        WHERE id_publicacion = NEW.id_publicacion
          AND estado_administrativo = 'deshabilitada_administrador'
    ) THEN
        -- Solo un admin aprueba propuestas (admin_moderar_propuesta), así que esta
        -- llamada corre con su sesión y pasa exigir_admin().
        PERFORM public.admin_cambiar_estado_publicacion(
            NEW.id_publicacion, TRUE, 'Rehabilitada al aprobar la edición corregida.'
        );
    END IF;
    RETURN NULL;
END;
$$;

CREATE TRIGGER trg_propuestas_rehabilitar
    AFTER UPDATE OF estado_propuesta ON public.propuestas_publicacion
    FOR EACH ROW EXECUTE FUNCTION public.fn_rehabilitar_al_aprobar_propuesta();
