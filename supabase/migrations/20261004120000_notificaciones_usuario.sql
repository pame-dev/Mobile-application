-- ############################################################################
-- Notificaciones para usuarios particulares y lotes
-- ############################################################################
-- Bandeja que se abre desde Configuración → Notificaciones. Los avisos los
-- crean triggers en la base, así que la app solo los lee y los marca como
-- leídos:
--   favorito             alguien agregó tu publicación a favoritos
--   propuesta_enviada    tu publicación se envió a revisión del administrador
--   publicacion_aprobada el administrador aprobó tu publicación
--   destacado_aprobado   tu publicación ahora es destacada
--   nuevo_destacado      hay una nueva publicación destacada (para todos)
-- El texto se arma en la app (español / inglés) a partir del tipo.


-- ----------------------------------------------------------------------------
-- Tabla
-- ----------------------------------------------------------------------------
CREATE TABLE public.notificaciones (
    id_notificacion  BIGINT        GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    id_cuenta        UUID          NOT NULL,
    tipo             VARCHAR(24)   NOT NULL,
    id_publicacion   BIGINT,
    -- Título del vehículo al momento del aviso, para no depender de que la
    -- publicación siga visible.
    titulo_vehiculo  VARCHAR(120),
    leida            BOOLEAN       NOT NULL DEFAULT FALSE,
    fecha_creacion   TIMESTAMPTZ   NOT NULL DEFAULT NOW(),

    CONSTRAINT fk_notificaciones_cuentas
        FOREIGN KEY (id_cuenta) REFERENCES public.cuentas (id_cuenta) ON DELETE CASCADE,
    CONSTRAINT fk_notificaciones_publicaciones
        FOREIGN KEY (id_publicacion) REFERENCES public.publicaciones (id_publicacion) ON DELETE CASCADE,

    CONSTRAINT chk_notificaciones_tipo
        CHECK (tipo IN ('favorito', 'propuesta_enviada', 'publicacion_aprobada',
                        'destacado_aprobado', 'nuevo_destacado'))
);

CREATE INDEX ix_notificaciones_cuenta_fecha
    ON public.notificaciones (id_cuenta, fecha_creacion DESC);


-- ----------------------------------------------------------------------------
-- Seguridad: cada cuenta ve solo sus avisos y solo puede marcarlos leídos.
-- Nadie inserta ni borra desde la app; solo los triggers de abajo.
-- ----------------------------------------------------------------------------
ALTER TABLE public.notificaciones ENABLE ROW LEVEL SECURITY;

CREATE POLICY notificaciones_leer_propias ON public.notificaciones
    FOR SELECT TO authenticated
    USING (id_cuenta = auth.uid());
CREATE POLICY notificaciones_marcar_propias ON public.notificaciones
    FOR UPDATE TO authenticated
    USING (id_cuenta = auth.uid()) WITH CHECK (id_cuenta = auth.uid());

REVOKE ALL ON public.notificaciones FROM anon, authenticated;
GRANT SELECT ON public.notificaciones TO authenticated;
GRANT UPDATE (leida) ON public.notificaciones TO authenticated;


-- ----------------------------------------------------------------------------
-- Favoritos: avisa al dueño (no cuando él mismo guarda su publicación).
-- ----------------------------------------------------------------------------
CREATE FUNCTION public.fn_notificar_favorito()
RETURNS TRIGGER
LANGUAGE plpgsql
SECURITY DEFINER
SET search_path = ''
AS $$
DECLARE
    v_pub public.publicaciones%ROWTYPE;
BEGIN
    SELECT * INTO v_pub FROM public.publicaciones WHERE id_publicacion = NEW.id_publicacion;
    IF FOUND AND v_pub.id_propietario <> NEW.id_cuenta THEN
        INSERT INTO public.notificaciones (id_cuenta, tipo, id_publicacion, titulo_vehiculo)
        VALUES (v_pub.id_propietario, 'favorito', v_pub.id_publicacion, v_pub.titulo);
    END IF;
    RETURN NULL;
END;
$$;

CREATE TRIGGER trg_favoritos_notificar
    AFTER INSERT ON public.favoritos
    FOR EACH ROW EXECUTE FUNCTION public.fn_notificar_favorito();


-- ----------------------------------------------------------------------------
-- Propuestas: avisa al dueño cuando entra a revisión (nueva, editada o
-- reenviada) y cuando el administrador la aprueba.
-- ----------------------------------------------------------------------------
CREATE FUNCTION public.fn_notificar_propuesta()
RETURNS TRIGGER
LANGUAGE plpgsql
SECURITY DEFINER
SET search_path = ''
AS $$
DECLARE
    v_dueno UUID;
    v_tipo  TEXT;
BEGIN
    IF NEW.estado_propuesta = 'pendiente'
       AND (TG_OP = 'INSERT' OR OLD.estado_propuesta IS DISTINCT FROM 'pendiente') THEN
        v_tipo := 'propuesta_enviada';
    ELSIF NEW.estado_propuesta = 'aprobada'
       AND TG_OP = 'UPDATE' AND OLD.estado_propuesta IS DISTINCT FROM 'aprobada' THEN
        v_tipo := 'publicacion_aprobada';
    ELSE
        RETURN NULL;
    END IF;

    SELECT id_propietario INTO v_dueno
    FROM public.publicaciones WHERE id_publicacion = NEW.id_publicacion;
    IF v_dueno IS NOT NULL THEN
        INSERT INTO public.notificaciones (id_cuenta, tipo, id_publicacion, titulo_vehiculo)
        VALUES (v_dueno, v_tipo, NEW.id_publicacion, NEW.titulo);
    END IF;
    RETURN NULL;
END;
$$;

CREATE TRIGGER trg_propuestas_notificar
    AFTER INSERT OR UPDATE OF estado_propuesta ON public.propuestas_publicacion
    FOR EACH ROW EXECUTE FUNCTION public.fn_notificar_propuesta();


-- ----------------------------------------------------------------------------
-- Destacados: al aprobarse, avisa al dueño y a todas las demás cuentas
-- activas que hay una nueva publicación destacada.
-- ----------------------------------------------------------------------------
CREATE FUNCTION public.fn_notificar_destacado()
RETURNS TRIGGER
LANGUAGE plpgsql
SECURITY DEFINER
SET search_path = ''
AS $$
DECLARE
    v_pub public.publicaciones%ROWTYPE;
BEGIN
    IF NEW.estado_solicitud <> 'aprobada' OR OLD.estado_solicitud = 'aprobada' THEN
        RETURN NULL;
    END IF;

    SELECT * INTO v_pub FROM public.publicaciones WHERE id_publicacion = NEW.id_publicacion;
    IF NOT FOUND THEN
        RETURN NULL;
    END IF;

    INSERT INTO public.notificaciones (id_cuenta, tipo, id_publicacion, titulo_vehiculo)
    VALUES (v_pub.id_propietario, 'destacado_aprobado', v_pub.id_publicacion, v_pub.titulo);

    INSERT INTO public.notificaciones (id_cuenta, tipo, id_publicacion, titulo_vehiculo)
    SELECT c.id_cuenta, 'nuevo_destacado', v_pub.id_publicacion, v_pub.titulo
    FROM public.cuentas c
    WHERE c.id_cuenta <> v_pub.id_propietario
      AND c.estado_cuenta = 'activa';

    RETURN NULL;
END;
$$;

CREATE TRIGGER trg_solicitudes_destacado_notificar
    AFTER UPDATE OF estado_solicitud ON public.solicitudes_destacado
    FOR EACH ROW EXECUTE FUNCTION public.fn_notificar_destacado();
