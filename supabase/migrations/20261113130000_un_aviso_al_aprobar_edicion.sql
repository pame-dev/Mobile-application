-- ############################################################################
-- Un solo aviso al aprobar la edición de una publicación deshabilitada
-- ############################################################################
-- Si el equipo deshabilita una publicación y el dueño la corrige, al aprobar la edición
-- llegaban dos avisos: "Se aprobaron los cambios… Ya se ven publicados" y "…fue
-- rehabilitada y vuelve a estar visible". El primero ya dice que se ve, así que el de
-- rehabilitada se omite cuando la rehabilitación viene de esa aprobación. Si el equipo
-- rehabilita a mano (sin edición), el aviso de rehabilitada sigue llegando.
--
-- Cómo: fn_rehabilitar_al_aprobar_propuesta pone una marca que solo dura la transacción
-- (set_config local) mientras rehabilita, y fn_notificar_estado_administrativo la lee.


-- ── Aprobar una edición rehabilita (con la marca) ──────────────────────────
-- Cuerpo de 20261111130000_suspension_ajustes.sql más la marca alrededor de la llamada.

CREATE OR REPLACE FUNCTION public.fn_rehabilitar_al_aprobar_propuesta()
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
        SELECT 1
        FROM public.publicaciones p
        JOIN public.cuentas c ON c.id_cuenta = p.id_propietario
        WHERE p.id_publicacion = NEW.id_publicacion
          AND p.estado_administrativo = 'deshabilitada_administrador'
          -- La deshabilitó la suspensión: la regresa la reactivación de la cuenta.
          AND NOT p.deshabilitada_por_suspension
          -- Con la cuenta suspendida, sus autos siguen sin verse.
          AND c.estado_cuenta = 'activa'
    ) THEN
        -- El aviso de "edición aprobada" ya dice que se ve: sin aviso de rehabilitada.
        PERFORM set_config('karsy.rehabilitar_por_aprobacion', 'on', true);
        -- Solo el equipo aprueba propuestas (admin_moderar_propuesta), así que esta
        -- llamada corre con su sesión y pasa exigir_moderador().
        PERFORM public.admin_cambiar_estado_publicacion(
            NEW.id_publicacion, TRUE, 'Rehabilitada al aprobar la edición corregida.'
        );
        PERFORM set_config('karsy.rehabilitar_por_aprobacion', '', true);
    END IF;
    RETURN NULL;
END;
$$;


-- ── Aviso de deshabilitada / rehabilitada (respeta la marca) ───────────────
-- Cuerpo de 20261111120000_suspension_oculta_publicaciones.sql más la salida por la marca.

CREATE OR REPLACE FUNCTION public.fn_notificar_estado_administrativo()
RETURNS TRIGGER
LANGUAGE plpgsql
SECURITY DEFINER
SET search_path = ''
AS $$
BEGIN
    IF NEW.estado_administrativo IS NOT DISTINCT FROM OLD.estado_administrativo THEN
        RETURN NULL;
    END IF;

    -- La suspensión o reactivación de la cuenta ya avisa una vez.
    IF NEW.deshabilitada_por_suspension IS DISTINCT FROM OLD.deshabilitada_por_suspension
       AND (NEW.deshabilitada_por_suspension OR OLD.deshabilitada_por_suspension AND NEW.estado_administrativo = 'habilitada'
            AND EXISTS (SELECT 1 FROM public.cuentas c
                        WHERE c.id_cuenta = NEW.id_propietario AND c.estado_cuenta = 'activa')) THEN
        RETURN NULL;
    END IF;

    -- Rehabilitada al aprobar su edición: ya le llega "edición aprobada" (ver arriba).
    IF NEW.estado_administrativo = 'habilitada'
       AND current_setting('karsy.rehabilitar_por_aprobacion', true) = 'on' THEN
        RETURN NULL;
    END IF;

    INSERT INTO public.notificaciones (id_cuenta, tipo, id_publicacion, titulo_vehiculo, motivo)
    VALUES (NEW.id_propietario,
            CASE WHEN NEW.estado_administrativo = 'deshabilitada_administrador'
                 THEN 'publicacion_deshabilitada' ELSE 'publicacion_rehabilitada' END,
            NEW.id_publicacion,
            NEW.titulo,
            CASE WHEN NEW.estado_administrativo = 'deshabilitada_administrador'
                 THEN NEW.motivo_deshabilitacion END);
    RETURN NULL;
END;
$$;
