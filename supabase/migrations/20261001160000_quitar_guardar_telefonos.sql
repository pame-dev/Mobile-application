-- 20261001150000 se aplicó en remoto cuando todavía creaba guardar_telefonos();
-- después el archivo local se cambió por el GRANT, que por eso nunca corrió.
-- Esta migración deja la BD como se quería: sin esa función y con el permiso.
DROP FUNCTION IF EXISTS public.guardar_telefonos(JSONB);

-- El perfil deja elegir si el medio de contacto principal es llamada o WhatsApp.
GRANT UPDATE (medio_contacto_principal) ON public.cuentas TO authenticated;
