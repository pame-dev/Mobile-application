-- Quita las columnas de verificación de perfiles_lote: ya viven en verificaciones_usuario
-- (y la insignia en cuentas.verificado), ver 20261021120000_verificaciones_usuario.
-- Aplicar DESPUÉS de comprobar que la web y la app ya usan la tabla nueva.

ALTER TABLE public.perfiles_lote
    DROP COLUMN constancia_url,
    DROP COLUMN estado_verificacion,
    DROP COLUMN motivo_rechazo_verificacion,
    DROP COLUMN fecha_verificacion;
