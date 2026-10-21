-- pgTAP: verificaciones_usuario (documento de verificación). Correr SOLO contra BD local: supabase test db
-- ESTADO: ESCRITO, NO EJECUTADO (sin supabase CLI/Docker en el entorno donde se creó).
BEGIN;
SELECT plan(15);

-- Fixtures (como superusuario): dos lotes y un usuario normal sin rol admin (A).
INSERT INTO auth.users (id, email) VALUES
  ('00000000-0000-0000-0000-0000000000a1', 'lote_a@test.local'),
  ('00000000-0000-0000-0000-0000000000b2', 'lote_b@test.local');
INSERT INTO public.cuentas (id_cuenta, tipo_cuenta, nombre_mostrar, estado_perfil_ubi, municipio_perfil_ubi) VALUES
  ('00000000-0000-0000-0000-0000000000a1', 'lote', 'Lote A', 'Jalisco', 'Guadalajara'),
  ('00000000-0000-0000-0000-0000000000b2', 'lote', 'Lote B', 'Jalisco', 'Zapopan');
-- Esquema actual: nombre y ubicación viven en cuentas; perfiles_lote solo guarda dirección.
-- (Jalisco/Guadalajara y Jalisco/Zapopan existen en municipios; el CP solo debe tener 5 dígitos.)
INSERT INTO public.perfiles_lote (id_cuenta, calle, numero, colonia, codigo_postal) VALUES
  ('00000000-0000-0000-0000-0000000000a1', 'Calle', '1', 'Centro', '44100'),
  ('00000000-0000-0000-0000-0000000000b2', 'Calle', '2', 'Centro', '45100');

-- Archivos ya subidos: dos de A y uno de B.
INSERT INTO storage.objects (bucket_id, name, owner) VALUES
  ('constancias-fiscales', '00000000-0000-0000-0000-0000000000a1/constancia_situacion_fiscal_1.pdf', '00000000-0000-0000-0000-0000000000a1'),
  ('constancias-fiscales', '00000000-0000-0000-0000-0000000000a1/constancia_situacion_fiscal_2.pdf', '00000000-0000-0000-0000-0000000000a1'),
  ('constancias-fiscales', '00000000-0000-0000-0000-0000000000b2/constancia_situacion_fiscal_1.pdf', '00000000-0000-0000-0000-0000000000b2');

-- Documento ya enviado por B (para comprobar que A no lo ve).
INSERT INTO public.verificaciones_usuario (user_id, tipo_usuario, tipo_documento, archivo_path) VALUES
  ('00000000-0000-0000-0000-0000000000b2', 'lote', 'constancia_situacion_fiscal',
   '00000000-0000-0000-0000-0000000000b2/constancia_situacion_fiscal_1.pdf');

SET LOCAL ROLE authenticated;
SET LOCAL request.jwt.claims = '{"sub":"00000000-0000-0000-0000-0000000000a1","role":"authenticated"}';

-- 1. INSERT directo: ya no hay permiso (se envía solo con enviar_documento_verificacion).
SELECT throws_ok(
  $$INSERT INTO public.verificaciones_usuario (user_id, tipo_usuario, tipo_documento, archivo_path)
    VALUES ('00000000-0000-0000-0000-0000000000a1', 'lote', 'constancia_situacion_fiscal',
            '00000000-0000-0000-0000-0000000000a1/constancia_situacion_fiscal_1.pdf')$$,
  '42501', NULL, 'el usuario no puede insertar directo en la tabla');

-- 2. UPDATE directo: tampoco (no puede ponerse verificado ni tocar la revisión).
SELECT throws_ok(
  $$UPDATE public.verificaciones_usuario SET estado = 'verificado' WHERE user_id = '00000000-0000-0000-0000-0000000000a1'$$,
  '42501', NULL, 'el usuario no puede actualizar directo la tabla');

-- 3. La función rechaza un archivo en la carpeta de otro usuario (aunque exista).
SELECT throws_ok(
  $$SELECT public.enviar_documento_verificacion('constancia_situacion_fiscal',
      '00000000-0000-0000-0000-0000000000b2/constancia_situacion_fiscal_1.pdf')$$,
  'P0001', 'No encontramos el archivo de tu documento. Vuelve a subirlo.',
  'no se puede enviar un archivo de la carpeta de otro usuario');

-- 4. Primer envío: crea la fila pendiente y no hay archivo anterior que borrar.
SELECT is(
  public.enviar_documento_verificacion('constancia_situacion_fiscal',
    '00000000-0000-0000-0000-0000000000a1/constancia_situacion_fiscal_1.pdf'),
  NULL::TEXT, 'el primer envío no devuelve archivo anterior');
SELECT is(
  (SELECT estado FROM public.verificaciones_usuario WHERE user_id = '00000000-0000-0000-0000-0000000000a1'),
  'pendiente', 'el primer envío queda pendiente');

-- 5. Reenviar mientras está pendiente se rechaza.
SELECT throws_ok(
  $$SELECT public.enviar_documento_verificacion('constancia_situacion_fiscal',
      '00000000-0000-0000-0000-0000000000a1/constancia_situacion_fiscal_2.pdf')$$,
  'P0001', 'Ya tienes un documento en revisión o tu cuenta ya está verificada.',
  'no se reenvía mientras está pendiente');

-- 6 y 7. Un no-admin solo ve sus filas.
SELECT is(
  (SELECT COUNT(*)::INT FROM public.verificaciones_usuario WHERE user_id = '00000000-0000-0000-0000-0000000000a1'),
  1, 'el usuario ve su propia fila');
SELECT is(
  (SELECT COUNT(*)::INT FROM public.verificaciones_usuario WHERE user_id <> '00000000-0000-0000-0000-0000000000a1'),
  0, 'el usuario no ve filas de otros');

-- 8 a 10. Funciones de admin.
SELECT throws_ok(
  $$SELECT public.admin_resolver_verificacion_usuario(gen_random_uuid(), TRUE)$$,
  'P0001', 'Solo un administrador puede realizar esta acción.',
  'un no-admin no puede resolver verificaciones');
SELECT throws_ok(
  $$SELECT * FROM public.admin_listar_verificaciones_usuario()$$,
  'P0001', 'Solo un administrador puede realizar esta acción.',
  'un no-admin no puede listar verificaciones');
SELECT throws_ok(
  $$SELECT public.admin_documentos_verificacion_sin_uso()$$,
  'P0001', NULL, 'un no-admin no puede listar documentos sin uso');

-- 11. Nadie se autoverifica.
SELECT throws_ok(
  $$UPDATE public.cuentas SET verificado = TRUE WHERE id_cuenta = '00000000-0000-0000-0000-0000000000a1'$$,
  '42501', NULL, 'un usuario no puede poner cuentas.verificado = true');

-- Un admin rechaza el documento (se simula como dueño de la tabla; B hace de revisor).
RESET ROLE;
UPDATE public.verificaciones_usuario
SET estado = 'rechazado', motivo_rechazo = 'Ilegible', revisado_por = '00000000-0000-0000-0000-0000000000b2', fecha_revision = NOW()
WHERE user_id = '00000000-0000-0000-0000-0000000000a1';
CREATE TEMP TABLE fila_a AS SELECT id FROM public.verificaciones_usuario WHERE user_id = '00000000-0000-0000-0000-0000000000a1';
GRANT SELECT ON fila_a TO authenticated;

-- 12. Duplicado (user_id, tipo_documento) como dueño: la restricción UNIQUE lo impide.
SELECT throws_ok(
  $$INSERT INTO public.verificaciones_usuario (user_id, tipo_usuario, tipo_documento, archivo_path)
    VALUES ('00000000-0000-0000-0000-0000000000a1', 'lote', 'constancia_situacion_fiscal',
            '00000000-0000-0000-0000-0000000000a1/constancia_situacion_fiscal_2.pdf')$$,
  '23505', NULL, 'no hay dos filas del mismo user_id y tipo_documento');

SET LOCAL ROLE authenticated;
SET LOCAL request.jwt.claims = '{"sub":"00000000-0000-0000-0000-0000000000a1","role":"authenticated"}';

-- 13. Reenvío tras rechazo: devuelve la ruta vieja.
SELECT is(
  public.enviar_documento_verificacion('constancia_situacion_fiscal',
    '00000000-0000-0000-0000-0000000000a1/constancia_situacion_fiscal_2.pdf'),
  '00000000-0000-0000-0000-0000000000a1/constancia_situacion_fiscal_1.pdf',
  'el reenvío devuelve el archivo anterior');

-- 14. Misma fila (mismo id), de nuevo pendiente, con la revisión limpia y el archivo nuevo.
SELECT results_eq(
  $$SELECT (v.id = f.id), v.estado, v.motivo_rechazo IS NULL, v.revisado_por IS NULL,
           v.fecha_revision IS NULL, v.archivo_path
    FROM public.verificaciones_usuario v, fila_a f WHERE v.user_id = '00000000-0000-0000-0000-0000000000a1'$$,
  $$VALUES (TRUE, 'pendiente'::TEXT, TRUE, TRUE, TRUE,
            '00000000-0000-0000-0000-0000000000a1/constancia_situacion_fiscal_2.pdf'::TEXT)$$,
  'el reenvío reutiliza la fila y limpia la revisión');

SELECT * FROM finish();
ROLLBACK;
