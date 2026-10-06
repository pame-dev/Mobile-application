-- ############################################################################
-- Notificaciones de las decisiones del administrador
-- ############################################################################
-- Hasta ahora solo se avisaba lo positivo (aprobada, destacada). Se agregan:
--   publicacion_rechazada     una publicación nueva fue rechazada (con motivo)
--   edicion_aprobada          se aprobaron los cambios a una publicación activa
--   edicion_rechazada         se rechazaron los cambios (sigue la versión anterior)
--   publicacion_deshabilitada administración la retiró (con motivo)
--   publicacion_rehabilitada  vuelve a estar visible
--   destacado_rechazado       la solicitud de destacado se rechazó (con motivo)
--   reporte_atendido          al que reportó: se tomaron medidas
--   reporte_descartado        al que reportó: se revisó y no procede
-- Se quita nuevo_destacado (se enviaba a todas las cuentas por cada destacado).
-- El texto se sigue armando en la app; [motivo] lleva el comentario del admin.


-- ----------------------------------------------------------------------------
-- Tabla: tipos nuevos y motivo
-- ----------------------------------------------------------------------------
ALTER TABLE public.notificaciones
    ALTER COLUMN tipo TYPE VARCHAR(32),
    ADD COLUMN motivo TEXT;

COMMENT ON COLUMN public.notificaciones.motivo IS 'Comentario del administrador (rechazo / deshabilitación); NULL si no aplica.';

ALTER TABLE public.notificaciones DROP CONSTRAINT chk_notificaciones_tipo;

-- Los avisos "nueva publicación destacada" ya enviados se retiran de las bandejas.
DELETE FROM public.notificaciones WHERE tipo = 'nuevo_destacado';

ALTER TABLE public.notificaciones ADD CONSTRAINT chk_notificaciones_tipo
    CHECK (tipo IN ('favorito', 'propuesta_enviada', 'publicacion_aprobada', 'destacado_aprobado',
                    'publicacion_rechazada', 'edicion_aprobada', 'edicion_rechazada',
                    'publicacion_deshabilitada', 'publicacion_rehabilitada',
                    'destacado_rechazado', 'reporte_atendido', 'reporte_descartado'));


-- ----------------------------------------------------------------------------
-- Propuestas: enviada, aprobada o rechazada, distinguiendo publicación nueva
-- de edición de una ya publicada.
-- ----------------------------------------------------------------------------
CREATE OR REPLACE FUNCTION public.fn_notificar_propuesta()
RETURNS TRIGGER
LANGUAGE plpgsql
SECURITY DEFINER
SET search_path = ''
AS $$
DECLARE
    v_dueno   UUID;
    v_tipo    TEXT;
    v_motivo  TEXT;
    v_edicion BOOLEAN;
BEGIN
    -- Si la publicación ya tenía otra versión aprobada, esta propuesta es una edición.
    -- (Al aprobar, la publicación se actualiza antes que la propuesta, así que no
    -- sirve mirar fecha_primera_publicacion.)
    v_edicion := EXISTS (
        SELECT 1 FROM public.propuestas_publicacion x
        WHERE x.id_publicacion = NEW.id_publicacion
          AND x.id_propuesta <> NEW.id_propuesta
          AND x.estado_propuesta = 'aprobada'
    );

    IF NEW.estado_propuesta = 'pendiente'
       AND (TG_OP = 'INSERT' OR OLD.estado_propuesta IS DISTINCT FROM 'pendiente') THEN
        v_tipo := 'propuesta_enviada';
    ELSIF NEW.estado_propuesta = 'aprobada'
       AND TG_OP = 'UPDATE' AND OLD.estado_propuesta IS DISTINCT FROM 'aprobada' THEN
        v_tipo := CASE WHEN v_edicion THEN 'edicion_aprobada' ELSE 'publicacion_aprobada' END;
    ELSIF NEW.estado_propuesta = 'rechazada'
       AND TG_OP = 'UPDATE' AND OLD.estado_propuesta IS DISTINCT FROM 'rechazada' THEN
        v_tipo := CASE WHEN v_edicion THEN 'edicion_rechazada' ELSE 'publicacion_rechazada' END;
        v_motivo := NEW.motivo_rechazo;
    ELSE
        RETURN NULL;
    END IF;

    SELECT id_propietario INTO v_dueno
    FROM public.publicaciones WHERE id_publicacion = NEW.id_publicacion;
    IF v_dueno IS NOT NULL THEN
        INSERT INTO public.notificaciones (id_cuenta, tipo, id_publicacion, titulo_vehiculo, motivo)
        VALUES (v_dueno, v_tipo, NEW.id_publicacion, NEW.titulo, v_motivo);
    END IF;
    RETURN NULL;
END;
$$;


-- ----------------------------------------------------------------------------
-- Destacados: aprobada o rechazada, solo al dueño.
-- ----------------------------------------------------------------------------
CREATE OR REPLACE FUNCTION public.fn_notificar_destacado()
RETURNS TRIGGER
LANGUAGE plpgsql
SECURITY DEFINER
SET search_path = ''
AS $$
DECLARE
    v_pub  public.publicaciones%ROWTYPE;
    v_tipo TEXT;
BEGIN
    IF NEW.estado_solicitud IS NOT DISTINCT FROM OLD.estado_solicitud THEN
        RETURN NULL;
    END IF;
    v_tipo := CASE NEW.estado_solicitud
                  WHEN 'aprobada'  THEN 'destacado_aprobado'
                  WHEN 'rechazada' THEN 'destacado_rechazado'
              END;
    IF v_tipo IS NULL THEN
        RETURN NULL;
    END IF;

    SELECT * INTO v_pub FROM public.publicaciones WHERE id_publicacion = NEW.id_publicacion;
    IF NOT FOUND THEN
        RETURN NULL;
    END IF;

    INSERT INTO public.notificaciones (id_cuenta, tipo, id_publicacion, titulo_vehiculo, motivo)
    VALUES (v_pub.id_propietario, v_tipo, v_pub.id_publicacion, v_pub.titulo,
            CASE WHEN v_tipo = 'destacado_rechazado' THEN NEW.motivo_rechazo END);
    RETURN NULL;
END;
$$;


-- ----------------------------------------------------------------------------
-- Publicaciones: deshabilitada / rehabilitada por administración.
-- ----------------------------------------------------------------------------
CREATE FUNCTION public.fn_notificar_estado_administrativo()
RETURNS TRIGGER
LANGUAGE plpgsql
SECURITY DEFINER
SET search_path = ''
AS $$
BEGIN
    IF NEW.estado_administrativo IS NOT DISTINCT FROM OLD.estado_administrativo THEN
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

CREATE TRIGGER trg_publicaciones_estado_admin_notificar
    AFTER UPDATE OF estado_administrativo ON public.publicaciones
    FOR EACH ROW EXECUTE FUNCTION public.fn_notificar_estado_administrativo();


-- ----------------------------------------------------------------------------
-- Reportes: al que reportó, cuando se resuelve. No se comparte el comentario
-- interno del admin. Si se atendió, la publicación pudo quedar oculta, así que
-- el aviso no enlaza a ella.
-- ----------------------------------------------------------------------------
CREATE FUNCTION public.fn_notificar_reporte()
RETURNS TRIGGER
LANGUAGE plpgsql
SECURITY DEFINER
SET search_path = ''
AS $$
DECLARE
    v_titulo TEXT;
BEGIN
    IF OLD.estado_reporte <> 'pendiente' OR NEW.estado_reporte NOT IN ('atendido', 'descartado') THEN
        RETURN NULL;
    END IF;

    SELECT titulo INTO v_titulo FROM public.publicaciones WHERE id_publicacion = NEW.id_publicacion;

    INSERT INTO public.notificaciones (id_cuenta, tipo, id_publicacion, titulo_vehiculo)
    VALUES (NEW.id_reportante,
            CASE WHEN NEW.estado_reporte = 'atendido' THEN 'reporte_atendido' ELSE 'reporte_descartado' END,
            CASE WHEN NEW.estado_reporte = 'descartado' THEN NEW.id_publicacion END,
            v_titulo);
    RETURN NULL;
END;
$$;

CREATE TRIGGER trg_reportes_notificar
    AFTER UPDATE OF estado_reporte ON public.reportes
    FOR EACH ROW EXECUTE FUNCTION public.fn_notificar_reporte();
