-- ############################################################################
-- Foto de portada del perfil (opcional)
-- ############################################################################
-- Ruta en Storage (bucket "karsy", carpeta perfiles/<id_cuenta>/, igual que
-- foto_perfil). Si es NULL, la app muestra el degradado de la marca.
-- cuentas ya es de lectura pública (cuentas_leer), así que la portada se ve
-- también en el perfil público del vendedor.

ALTER TABLE public.cuentas ADD COLUMN foto_portada TEXT;

COMMENT ON COLUMN public.cuentas.foto_portada IS 'Ruta en Storage de la foto de portada del perfil (opcional).';

-- Cada quien cambia o quita su propia portada (cuentas_editar_propia limita la fila).
GRANT UPDATE (foto_portada) ON public.cuentas TO authenticated;
