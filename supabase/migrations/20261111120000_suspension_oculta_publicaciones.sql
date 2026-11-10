-- ############################################################################
-- Suspender una cuenta deshabilita sus publicaciones; reactivarla las regresa
-- ############################################################################
-- Antes, al suspender una cuenta sus autos seguían a la vista. Ahora:
--   · Al suspender (admin o moderador de publicaciones), cada publicación habilitada
--     de la cuenta pasa a "deshabilitada_administrador" con el motivo
--     "Cuenta suspendida: <motivo>" y queda marcada con deshabilitada_por_suspension.
--   · Al reactivar, vuelven a habilitarse solo las marcadas. Las que el equipo ya
--     había deshabilitado por otra razón siguen deshabilitadas.
--   · Si el equipo habilita o deshabilita a mano una de ellas, deja de estar marcada
--     (esa decisión ya no la deshace la reactivación).
--   · El dueño no recibe un aviso por cada publicación: ya le llega "cuenta suspendida"
--     o "cuenta reactivada". Quien la tenía en favoritos sí recibe "ya no disponible".
-- La pausa del dueño (estado_publicacion) no se toca.


ALTER TABLE public.publicaciones
    ADD COLUMN deshabilitada_por_suspension BOOLEAN NOT NULL DEFAULT FALSE;

COMMENT ON COLUMN public.publicaciones.deshabilitada_por_suspension IS
    'TRUE si la deshabilitó la suspensión de la cuenta; al reactivarla se habilita de nuevo.';


-- ── Suspender / reactivar cuenta ───────────────────────────────────────────
-- Cuerpo de 20261102120000_moderadores.sql más los dos UPDATE de publicaciones.

CREATE OR REPLACE FUNCTION public.admin_cambiar_estado_cuenta(p_id_cuenta UUID, p_suspender BOOLEAN, p_motivo TEXT DEFAULT NULL)
RETURNS VOID
LANGUAGE plpgsql
SECURITY DEFINER
SET search_path = ''
AS $$
DECLARE
    v_motivo TEXT := NULLIF(TRIM(p_motivo), '');
BEGIN
    PERFORM public.exigir_moderador('moderador_publicaciones');

    IF p_id_cuenta = auth.uid() THEN
        RAISE EXCEPTION 'No puedes suspender tu propia cuenta.';
    END IF;

    -- Un moderador no suspende ni reactiva al administrador ni a otro moderador.
    IF NOT public.es_admin_actual() AND public.es_personal(p_id_cuenta) THEN
        RAISE EXCEPTION 'Solo el administrador puede cambiar el estado de una cuenta del equipo.';
    END IF;

    IF p_suspender THEN
        v_motivo := public.exigir_motivo(p_motivo, 'suspende');
    END IF;

    UPDATE public.cuentas
    SET estado_cuenta = CASE WHEN p_suspender THEN 'suspendida' ELSE 'activa' END
    WHERE id_cuenta = p_id_cuenta;

    IF NOT FOUND THEN
        RAISE EXCEPTION 'La cuenta no existe.';
    END IF;

    IF p_suspender THEN
        -- Sus publicaciones habilitadas (publicadas o en revisión) dejan de verse.
        UPDATE public.publicaciones
        SET estado_administrativo        = 'deshabilitada_administrador',
            motivo_deshabilitacion       = 'Cuenta suspendida: ' || v_motivo,
            fecha_deshabilitacion        = NOW(),
            deshabilitada_por_suspension = TRUE
        WHERE id_propietario = p_id_cuenta
          AND estado_administrativo = 'habilitada';
    ELSE
        -- Regresan solo las que deshabilitó la suspensión.
        UPDATE public.publicaciones
        SET estado_administrativo        = 'habilitada',
            motivo_deshabilitacion       = NULL,
            fecha_deshabilitacion        = NULL,
            deshabilitada_por_suspension = FALSE
        WHERE id_propietario = p_id_cuenta
          AND deshabilitada_por_suspension;
    END IF;

    INSERT INTO public.acciones_administrativas (id_admin, tipo_accion, motivo, id_cuenta_objetivo)
    VALUES (auth.uid(),
            CASE WHEN p_suspender THEN 'suspender_cuenta' ELSE 'reactivar_cuenta' END,
            COALESCE(v_motivo, 'Sin motivo especificado.'),
            p_id_cuenta);
END;
$$;


-- ── Habilitar / deshabilitar una publicación a mano ────────────────────────
-- Cuerpo de 20261102120000_moderadores.sql; además quita la marca de suspensión.

CREATE OR REPLACE FUNCTION public.admin_cambiar_estado_publicacion(p_id_publicacion BIGINT, p_habilitar BOOLEAN, p_motivo TEXT DEFAULT NULL)
RETURNS VOID
LANGUAGE plpgsql
SECURITY DEFINER
SET search_path = ''
AS $$
DECLARE
    v_motivo TEXT := NULLIF(TRIM(p_motivo), '');
BEGIN
    PERFORM public.exigir_moderador('moderador_publicaciones');

    IF NOT p_habilitar AND COALESCE(CHAR_LENGTH(v_motivo), 0) < 10 THEN
        RAISE EXCEPTION 'Escribe un comentario de al menos 10 caracteres explicando por qué se deshabilita.';
    END IF;

    UPDATE public.publicaciones
    SET estado_administrativo        = CASE WHEN p_habilitar THEN 'habilitada' ELSE 'deshabilitada_administrador' END,
        motivo_deshabilitacion       = CASE WHEN p_habilitar THEN NULL ELSE v_motivo END,
        fecha_deshabilitacion        = CASE WHEN p_habilitar THEN NULL ELSE NOW() END,
        deshabilitada_por_suspension = FALSE
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


-- ── Aviso al dueño: no por cada publicación de una suspensión ──────────────
-- Cuerpo de 20261006120000_notificaciones_decisiones_admin.sql más la salida temprana.

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


-- ── Cuentas que ya estaban suspendidas ─────────────────────────────────────
-- Sus publicaciones habilitadas se deshabilitan igual, con el último motivo de la bitácora.

UPDATE public.publicaciones p
SET estado_administrativo        = 'deshabilitada_administrador',
    motivo_deshabilitacion       = 'Cuenta suspendida: ' || COALESCE((
        SELECT a.motivo FROM public.acciones_administrativas a
        WHERE a.id_cuenta_objetivo = p.id_propietario AND a.tipo_accion = 'suspender_cuenta'
        ORDER BY a.fecha_hora DESC LIMIT 1), 'sin motivo registrado.'),
    fecha_deshabilitacion        = NOW(),
    deshabilitada_por_suspension = TRUE
FROM public.cuentas c
WHERE c.id_cuenta = p.id_propietario
  AND c.estado_cuenta = 'suspendida'
  AND p.estado_administrativo = 'habilitada';
