-- ############################################################################
-- Tema claro / oscuro guardado en la cuenta
-- ############################################################################
-- Para que el tema elegido en Configuración sea el mismo en todos los dispositivos
-- (app y web). NULL = nunca lo eligió: cada dispositivo usa lo suyo. 'sistema' = seguir
-- el modo del teléfono o la computadora.
-- La tabla ya tiene RLS: cada quien edita solo su fila (cuentas_editar_propia), y solo
-- las columnas que se le dan con GRANT UPDATE.

ALTER TABLE public.cuentas ADD COLUMN tema VARCHAR(7);
ALTER TABLE public.cuentas ADD CONSTRAINT chk_cuentas_tema
    CHECK (tema IS NULL OR tema IN ('claro', 'oscuro', 'sistema'));
COMMENT ON COLUMN public.cuentas.tema IS
    'Tema elegido en Configuración: claro, oscuro o sistema. NULL = nunca lo eligió.';

GRANT UPDATE (tema) ON public.cuentas TO authenticated;
