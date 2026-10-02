-- Esta versión se aplicó en remoto creando la función guardar_telefonos();
-- el archivo se cambió después y la corrección está en 20261001160000
-- (quita la función y da el permiso sobre medio_contacto_principal).
-- Se deja el GRANT aquí para que una BD local nueva quede igual.
GRANT UPDATE (medio_contacto_principal) ON public.cuentas TO authenticated;
