-- ############################################################################
-- Corrige el registro de cuentas: "permission denied for table cuentas"
-- ############################################################################
-- fn_lote_requiere_perfil() (20261015120000_normalizacion_integridad) es de dos
-- triggers DEFERRABLE INITIALLY DEFERRED: se revisa al hacer COMMIT, ya fuera de
-- fn_auth_crear_cuenta. En el registro esa transacción es del servicio de Auth
-- (rol supabase_auth_admin), que no puede leer public.cuentas, así que el COMMIT
-- fallaba y el registro se deshacía (en la app y en la web), aunque el correo con
-- el código ya se hubiera enviado.
--
-- Con SECURITY DEFINER la revisión corre con los permisos del dueño de la función,
-- como fn_auth_crear_cuenta. La regla (un lote necesita su perfil) no cambia.

ALTER FUNCTION public.fn_lote_requiere_perfil() SECURITY DEFINER;
