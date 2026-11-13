-- ############################################################################
-- Contactos: como máximo uno por persona y publicación al día
-- ############################################################################
-- La app y la web cuentan un contacto al tocar Llamar o WhatsApp (ya no al abrir la
-- ventana con los datos del vendedor). Para que una misma persona no infle el número
-- entrando varias veces, se guarda como máximo un 'contactar' por cuenta y publicación
-- por día (hora de México). Los demás se descartan en silencio, igual que
-- fn_interacciones_ignorar_propietario descarta los del dueño. Las vistas no cambian.


CREATE FUNCTION public.fn_interacciones_un_contacto_al_dia()
RETURNS TRIGGER
LANGUAGE plpgsql
SECURITY DEFINER
SET search_path = ''
AS $$
BEGIN
    -- La fecha la pone la base de datos (no se acepta una enviada por el cliente).
    NEW.fecha_hora := NOW();
    -- Dos toques casi simultáneos de la misma persona y auto se esperan uno al otro, así
    -- el segundo ya ve al primero y no se guardan dos.
    PERFORM pg_advisory_xact_lock(hashtextextended(NEW.id_cuenta::TEXT || ':' || NEW.id_publicacion::TEXT, 0));

    IF EXISTS (
        SELECT 1
        FROM public.interacciones_publicacion i
        WHERE i.id_cuenta = NEW.id_cuenta
          AND i.id_publicacion = NEW.id_publicacion
          AND i.tipo_interaccion = 'contactar'
          AND (i.fecha_hora AT TIME ZONE 'America/Mexico_City')::DATE
              = (NOW() AT TIME ZONE 'America/Mexico_City')::DATE
    ) THEN
        RETURN NULL; -- ya contó hoy: no se guarda otro
    END IF;
    RETURN NEW;
END;
$$;

REVOKE ALL ON FUNCTION public.fn_interacciones_un_contacto_al_dia() FROM PUBLIC, anon, authenticated;

CREATE TRIGGER trg_interacciones_un_contacto_al_dia
    BEFORE INSERT ON public.interacciones_publicacion
    FOR EACH ROW
    WHEN (NEW.tipo_interaccion = 'contactar' AND NEW.id_cuenta IS NOT NULL)
    EXECUTE FUNCTION public.fn_interacciones_un_contacto_al_dia();
