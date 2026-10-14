-- ############################################################################
-- Búsquedas guardadas con alerta
-- ############################################################################
-- El usuario guarda los filtros del catálogo (marca, modelo, año, tipo, precio y,
-- desde "Cerca de mí", el estado). Cuando se publica por primera vez un auto que
-- coincide, le llega la notificación "busqueda_coincidencia" (también por push,
-- con el mismo webhook de notificaciones). Un aviso por cuenta aunque coincidan
-- varias de sus búsquedas; nunca por sus propios autos.


CREATE TABLE public.busquedas_guardadas (
    id_busqueda       BIGINT         GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    id_cuenta         UUID           NOT NULL,
    id_marca          BIGINT,
    id_modelo         BIGINT,
    anio              SMALLINT,
    id_carroceria     BIGINT,
    precio_min        NUMERIC(12,2),
    precio_max        NUMERIC(12,2),
    estado_ubicacion  VARCHAR(60),
    fecha_creacion    TIMESTAMPTZ    NOT NULL DEFAULT NOW(),

    CONSTRAINT fk_busquedas_guardadas_cuentas
        FOREIGN KEY (id_cuenta) REFERENCES public.cuentas (id_cuenta) ON DELETE CASCADE,
    CONSTRAINT fk_busquedas_guardadas_marcas
        FOREIGN KEY (id_marca) REFERENCES public.marcas (id_marca) ON DELETE CASCADE,
    CONSTRAINT fk_busquedas_guardadas_modelos
        FOREIGN KEY (id_modelo) REFERENCES public.modelos (id_modelo) ON DELETE CASCADE,
    CONSTRAINT fk_busquedas_guardadas_carrocerias
        FOREIGN KEY (id_carroceria) REFERENCES public.carrocerias (id_carroceria) ON DELETE CASCADE,
    CONSTRAINT fk_busquedas_guardadas_estados
        FOREIGN KEY (estado_ubicacion) REFERENCES public.estados (nombre_estado) ON UPDATE CASCADE ON DELETE CASCADE,

    -- Sin ningún filtro avisaría de todos los autos.
    CONSTRAINT chk_busquedas_guardadas_algun_filtro CHECK (
        id_marca IS NOT NULL OR id_modelo IS NOT NULL OR anio IS NOT NULL OR id_carroceria IS NOT NULL
        OR precio_min IS NOT NULL OR precio_max IS NOT NULL OR estado_ubicacion IS NOT NULL),
    CONSTRAINT chk_busquedas_guardadas_modelo_con_marca CHECK (id_modelo IS NULL OR id_marca IS NOT NULL),
    CONSTRAINT chk_busquedas_guardadas_anio CHECK (anio IS NULL OR anio BETWEEN 1900 AND 2100),
    CONSTRAINT chk_busquedas_guardadas_precios CHECK (
        (precio_min IS NULL OR precio_min >= 0) AND (precio_max IS NULL OR precio_max >= 0)
        AND (precio_min IS NULL OR precio_max IS NULL OR precio_min <= precio_max)),
    -- La misma búsqueda no se guarda dos veces.
    CONSTRAINT uq_busquedas_guardadas UNIQUE NULLS NOT DISTINCT
        (id_cuenta, id_marca, id_modelo, anio, id_carroceria, precio_min, precio_max, estado_ubicacion)
);

CREATE INDEX ix_busquedas_guardadas_id_cuenta ON public.busquedas_guardadas (id_cuenta);

COMMENT ON TABLE public.busquedas_guardadas IS
    'Filtros guardados del catálogo; avisan (notificaciones) cuando se publica un auto que coincide.';


-- Máximo 10 por cuenta.
CREATE FUNCTION public.fn_busquedas_guardadas_maximo()
RETURNS TRIGGER
LANGUAGE plpgsql
SET search_path = ''
AS $$
BEGIN
    IF (SELECT COUNT(*) FROM public.busquedas_guardadas WHERE id_cuenta = NEW.id_cuenta) >= 10 THEN
        RAISE EXCEPTION 'Puedes guardar máximo 10 búsquedas. Borra alguna para guardar otra.';
    END IF;
    RETURN NEW;
END;
$$;

CREATE TRIGGER trg_busquedas_guardadas_maximo
    BEFORE INSERT ON public.busquedas_guardadas
    FOR EACH ROW EXECUTE FUNCTION public.fn_busquedas_guardadas_maximo();


-- Cada quien ve, guarda y borra solo las suyas.
ALTER TABLE public.busquedas_guardadas ENABLE ROW LEVEL SECURITY;

CREATE POLICY busquedas_guardadas_leer_propias ON public.busquedas_guardadas
    FOR SELECT TO authenticated USING (id_cuenta = auth.uid());
CREATE POLICY busquedas_guardadas_crear_propias ON public.busquedas_guardadas
    FOR INSERT TO authenticated WITH CHECK (id_cuenta = auth.uid());
CREATE POLICY busquedas_guardadas_borrar_propias ON public.busquedas_guardadas
    FOR DELETE TO authenticated USING (id_cuenta = auth.uid());

GRANT SELECT, INSERT, DELETE ON public.busquedas_guardadas TO authenticated;


-- Nuevo tipo de notificación.
ALTER TABLE public.notificaciones DROP CONSTRAINT chk_notificaciones_tipo;
ALTER TABLE public.notificaciones ADD CONSTRAINT chk_notificaciones_tipo
    CHECK (tipo IN ('favorito', 'propuesta_enviada', 'publicacion_aprobada', 'destacado_aprobado',
                    'publicacion_rechazada', 'edicion_aprobada', 'edicion_rechazada',
                    'publicacion_deshabilitada', 'publicacion_rehabilitada',
                    'destacado_rechazado', 'reporte_atendido', 'reporte_descartado',
                    'destacado_por_vencer', 'cuenta_suspendida', 'cuenta_reactivada',
                    'busqueda_coincidencia'));


-- Aviso cuando un auto se publica por primera vez (admin aprueba la propuesta nueva).
-- Las ediciones de autos ya publicados no avisan, para no repetir.
CREATE FUNCTION public.fn_notificar_busquedas_guardadas()
RETURNS TRIGGER
LANGUAGE plpgsql
SECURITY DEFINER
SET search_path = ''
AS $$
BEGIN
    IF NEW.estado_publicacion <> 'activa' OR NEW.estado_administrativo <> 'habilitada' THEN
        RETURN NULL;
    END IF;

    INSERT INTO public.notificaciones (id_cuenta, tipo, id_publicacion, titulo_vehiculo)
    SELECT DISTINCT b.id_cuenta, 'busqueda_coincidencia', NEW.id_publicacion, NEW.titulo
    FROM public.busquedas_guardadas b
    JOIN public.cuentas c ON c.id_cuenta = b.id_cuenta AND c.estado_cuenta = 'activa'
    WHERE b.id_cuenta <> NEW.id_propietario
      AND (b.id_marca IS NULL OR b.id_marca = NEW.id_marca)
      AND (b.id_modelo IS NULL OR b.id_modelo = NEW.id_modelo)
      AND (b.anio IS NULL OR b.anio = NEW.anio)
      AND (b.id_carroceria IS NULL OR b.id_carroceria = NEW.id_carroceria)
      AND (b.precio_min IS NULL OR NEW.precio >= b.precio_min)
      AND (b.precio_max IS NULL OR NEW.precio <= b.precio_max)
      AND (b.estado_ubicacion IS NULL OR b.estado_ubicacion = NEW.estado_ubicacion);
    RETURN NULL;
END;
$$;

CREATE TRIGGER trg_publicaciones_notificar_busquedas
    AFTER UPDATE OF fecha_primera_publicacion ON public.publicaciones
    FOR EACH ROW
    WHEN (OLD.fecha_primera_publicacion IS NULL AND NEW.fecha_primera_publicacion IS NOT NULL)
    EXECUTE FUNCTION public.fn_notificar_busquedas_guardadas();
