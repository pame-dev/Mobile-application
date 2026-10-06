-- ############################################################################
-- Notificaciones push (Firebase Cloud Messaging)
-- ############################################################################
-- Cada teléfono con sesión iniciada guarda aquí su token de Firebase. Cuando un
-- trigger crea una fila en `notificaciones`, un Database Webhook llama a la Edge
-- Function `enviar-push`, que lee los tokens de esa cuenta y envía el aviso.
-- La app no lee ni escribe la tabla directamente: usa las dos funciones de abajo.


-- ----------------------------------------------------------------------------
-- Tabla de teléfonos
-- ----------------------------------------------------------------------------
CREATE TABLE public.dispositivos_push (
    -- Un token identifica un teléfono (una instalación de la app).
    token                TEXT          PRIMARY KEY,
    id_cuenta            UUID          NOT NULL,
    fecha_actualizacion  TIMESTAMPTZ   NOT NULL DEFAULT NOW(),

    CONSTRAINT fk_dispositivos_push_cuentas
        FOREIGN KEY (id_cuenta) REFERENCES public.cuentas (id_cuenta) ON DELETE CASCADE,
    CONSTRAINT chk_dispositivos_push_token
        CHECK (CHAR_LENGTH(token) BETWEEN 20 AND 4096)
);

CREATE INDEX ix_dispositivos_push_cuenta ON public.dispositivos_push (id_cuenta);

COMMENT ON TABLE public.dispositivos_push IS 'Tokens de Firebase de los teléfonos con sesión; los usa la Edge Function enviar-push.';

-- Sin políticas: nadie la lee desde la app. Solo las funciones de abajo
-- (SECURITY DEFINER) y la Edge Function (service_role).
ALTER TABLE public.dispositivos_push ENABLE ROW LEVEL SECURITY;
REVOKE ALL ON public.dispositivos_push FROM anon, authenticated;


-- ----------------------------------------------------------------------------
-- Registrar / quitar el teléfono de la cuenta en sesión
-- ----------------------------------------------------------------------------
-- Si el mismo teléfono inicia sesión con otra cuenta, el token pasa a la nueva.
CREATE FUNCTION public.registrar_dispositivo(p_token TEXT)
RETURNS VOID
LANGUAGE plpgsql
SECURITY DEFINER
SET search_path = ''
AS $$
BEGIN
    IF auth.uid() IS NULL THEN
        RAISE EXCEPTION 'Inicia sesión para recibir notificaciones.';
    END IF;

    INSERT INTO public.dispositivos_push (token, id_cuenta)
    VALUES (TRIM(p_token), auth.uid())
    ON CONFLICT (token) DO UPDATE
        SET id_cuenta = EXCLUDED.id_cuenta, fecha_actualizacion = NOW();
END;
$$;

-- Al cerrar sesión: solo puede quitar un teléfono de su propia cuenta.
CREATE FUNCTION public.quitar_dispositivo(p_token TEXT)
RETURNS VOID
LANGUAGE sql
SECURITY DEFINER
SET search_path = ''
AS $$
    DELETE FROM public.dispositivos_push
    WHERE token = TRIM(p_token) AND id_cuenta = auth.uid();
$$;

REVOKE EXECUTE ON FUNCTION public.registrar_dispositivo(TEXT), public.quitar_dispositivo(TEXT) FROM PUBLIC, anon;
GRANT EXECUTE ON FUNCTION public.registrar_dispositivo(TEXT), public.quitar_dispositivo(TEXT) TO authenticated;


-- ----------------------------------------------------------------------------
-- Cada aviso se envía por push una sola vez (la Edge Function lo marca).
-- ----------------------------------------------------------------------------
ALTER TABLE public.notificaciones
    ADD COLUMN push_enviado BOOLEAN NOT NULL DEFAULT FALSE;

COMMENT ON COLUMN public.notificaciones.push_enviado IS 'TRUE cuando enviar-push ya lo entregó a Firebase (evita reenvíos).';

-- Los avisos que ya existían no se envían por push.
UPDATE public.notificaciones SET push_enviado = TRUE;
