-- pgTAP: verificaciones_usuario (documento de verificación). Correr SOLO contra BD local: supabase test db
-- ESTADO: ESCRITO, NO EJECUTADO (sin supabase CLI/Docker en el entorno donde se creó).
-- Casos 15 a 27: la constancia se sube una sola vez (solo sin fila o 'rechazado'; pendiente y
-- verificado se bloquean). NO EJECUTADOS.
BEGIN;
SELECT plan(27);

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

-- Admin (E) y un lote (D) que ya tiene la CURP verificada y una constancia en revisión.
INSERT INTO auth.users (id, email) VALUES
  ('00000000-0000-0000-0000-0000000000e5', 'admin@test.local'),
  ('00000000-0000-0000-0000-0000000000d4', 'lote_d@test.local');
INSERT INTO public.cuentas (id_cuenta, tipo_cuenta, nombre_mostrar, estado_perfil_ubi, municipio_perfil_ubi) VALUES
  ('00000000-0000-0000-0000-0000000000e5', 'lote', 'Admin', 'Jalisco', 'Guadalajara'),
  ('00000000-0000-0000-0000-0000000000d4', 'lote', 'Lote D', 'Jalisco', 'Zapopan');
INSERT INTO public.perfiles_lote (id_cuenta, calle, numero, colonia, codigo_postal) VALUES
  ('00000000-0000-0000-0000-0000000000d4', 'Calle', '4', 'Centro', '45100');
INSERT INTO public.cuentas_roles (id_cuenta, id_rol)
SELECT '00000000-0000-0000-0000-0000000000e5', r.id_rol FROM public.roles r WHERE r.nombre_rol = 'administrador'
ON CONFLICT (id_cuenta, id_rol) DO NOTHING;

-- Archivos ya subidos: tres de A, uno de B y uno de D.
INSERT INTO storage.objects (bucket_id, name, owner) VALUES
  ('constancias-fiscales', '00000000-0000-0000-0000-0000000000a1/constancia_situacion_fiscal_3.pdf', '00000000-0000-0000-0000-0000000000a1'),
  ('constancias-fiscales', '00000000-0000-0000-0000-0000000000d4/constancia_situacion_fiscal_1.pdf', '00000000-0000-0000-0000-0000000000d4'),
  ('constancias-fiscales', '00000000-0000-0000-0000-0000000000a1/constancia_situacion_fiscal_1.pdf', '00000000-0000-0000-0000-0000000000a1'),
  ('constancias-fiscales', '00000000-0000-0000-0000-0000000000a1/constancia_situacion_fiscal_2.pdf', '00000000-0000-0000-0000-0000000000a1'),
  ('constancias-fiscales', '00000000-0000-0000-0000-0000000000b2/constancia_situacion_fiscal_1.pdf', '00000000-0000-0000-0000-0000000000b2');

-- Documento ya enviado por B (para comprobar que A no lo ve).
INSERT INTO public.verificaciones_usuario (user_id, tipo_usuario, tipo_documento, archivo_path) VALUES
  ('00000000-0000-0000-0000-0000000000b2', 'lote', 'constancia_situacion_fiscal',
   '00000000-0000-0000-0000-0000000000b2/constancia_situacion_fiscal_1.pdf');
-- D: constancia en revisión + CURP verificada (otro documento verificado) y la insignia puesta.
INSERT INTO public.verificaciones_usuario (user_id, tipo_usuario, tipo_documento, archivo_path) VALUES
  ('00000000-0000-0000-0000-0000000000d4', 'lote', 'constancia_situacion_fiscal',
   '00000000-0000-0000-0000-0000000000d4/constancia_situacion_fiscal_1.pdf');
INSERT INTO public.verificaciones_usuario (user_id, tipo_usuario, tipo_documento, curp, estado) VALUES
  ('00000000-0000-0000-0000-0000000000d4', 'lote', 'curp', 'GOMC800101HJCRRL09', 'verificado');
UPDATE public.cuentas SET verificado = TRUE WHERE id_cuenta = '00000000-0000-0000-0000-0000000000d4';
CREATE TEMP TABLE fila_d AS
  SELECT id FROM public.verificaciones_usuario
  WHERE user_id = '00000000-0000-0000-0000-0000000000d4' AND tipo_documento = 'constancia_situacion_fiscal';
GRANT SELECT ON fila_d TO authenticated;

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
  'P0001', 'Tu documento ya está en revisión.',
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

-- ── Constancia de una sola vez (casos 15 a 27, NO EJECUTADOS) ───────────────
-- 15 y 16. A tenía la insignia (por otro motivo); el admin rechaza su constancia pendiente: la pierde.
RESET ROLE;
UPDATE public.cuentas SET verificado = TRUE WHERE id_cuenta = '00000000-0000-0000-0000-0000000000a1';
SET LOCAL ROLE authenticated;
SET LOCAL request.jwt.claims = '{"sub":"00000000-0000-0000-0000-0000000000e5","role":"authenticated"}';
SELECT lives_ok(
  $$SELECT public.admin_resolver_verificacion_usuario((SELECT id FROM fila_a), FALSE, 'Documento ilegible, sube otro por favor')$$,
  'el admin rechaza la constancia de A');
RESET ROLE;
SELECT is(
  (SELECT verificado FROM public.cuentas WHERE id_cuenta = '00000000-0000-0000-0000-0000000000a1'),
  FALSE, 'rechazar sin otro documento verificado pone cuentas.verificado = false');

-- 17. Tras el rechazo, A puede reenviar: devuelve la ruta anterior.
SET LOCAL ROLE authenticated;
SET LOCAL request.jwt.claims = '{"sub":"00000000-0000-0000-0000-0000000000a1","role":"authenticated"}';
SELECT is(
  public.enviar_documento_verificacion('constancia_situacion_fiscal',
    '00000000-0000-0000-0000-0000000000a1/constancia_situacion_fiscal_3.pdf'),
  '00000000-0000-0000-0000-0000000000a1/constancia_situacion_fiscal_2.pdf',
  'el reenvío tras rechazo del admin devuelve el archivo anterior');

-- 18. Misma fila, pendiente, archivo nuevo y revisión limpia.
SELECT results_eq(
  $$SELECT (v.id = f.id), v.estado, v.archivo_path, v.motivo_rechazo IS NULL, v.revisado_por IS NULL,
           v.fecha_revision IS NULL
    FROM public.verificaciones_usuario v, fila_a f WHERE v.user_id = '00000000-0000-0000-0000-0000000000a1'$$,
  $$VALUES (TRUE, 'pendiente'::TEXT, '00000000-0000-0000-0000-0000000000a1/constancia_situacion_fiscal_3.pdf'::TEXT,
            TRUE, TRUE, TRUE)$$,
  'el reenvío reutiliza la fila, queda pendiente y sin datos de revisión');

-- 19. La función no toca cuentas.verificado.
RESET ROLE;
SELECT is(
  (SELECT verificado FROM public.cuentas WHERE id_cuenta = '00000000-0000-0000-0000-0000000000a1'),
  FALSE, 'enviar_documento_verificacion no cambia cuentas.verificado');

-- 20 y 21. El admin aprueba: A queda verificado y con insignia.
SET LOCAL ROLE authenticated;
SET LOCAL request.jwt.claims = '{"sub":"00000000-0000-0000-0000-0000000000e5","role":"authenticated"}';
SELECT lives_ok(
  $$SELECT public.admin_resolver_verificacion_usuario((SELECT id FROM fila_a), TRUE)$$,
  'el admin aprueba el documento de A');
RESET ROLE;
SELECT is(
  (SELECT verificado FROM public.cuentas WHERE id_cuenta = '00000000-0000-0000-0000-0000000000a1'),
  TRUE, 'aprobar pone cuentas.verificado = true');

-- 22. Estando 'verificado' ya no se puede reenviar.
SET LOCAL ROLE authenticated;
SET LOCAL request.jwt.claims = '{"sub":"00000000-0000-0000-0000-0000000000a1","role":"authenticated"}';
SELECT throws_ok(
  $$SELECT public.enviar_documento_verificacion('constancia_situacion_fiscal',
      '00000000-0000-0000-0000-0000000000a1/constancia_situacion_fiscal_1.pdf')$$,
  'P0001', 'Tu documento ya está verificado y no se puede cambiar.',
  'no se reenvía estando verificado');

-- 23. La fila queda igual: mismo id, verificado y con el archivo de antes.
SELECT results_eq(
  $$SELECT (v.id = f.id), v.estado, v.archivo_path
    FROM public.verificaciones_usuario v, fila_a f WHERE v.user_id = '00000000-0000-0000-0000-0000000000a1'$$,
  $$VALUES (TRUE, 'verificado'::TEXT, '00000000-0000-0000-0000-0000000000a1/constancia_situacion_fiscal_3.pdf'::TEXT)$$,
  'el intento rechazado no cambia la fila verificada');

-- 24. Ni la insignia.
RESET ROLE;
SELECT is(
  (SELECT verificado FROM public.cuentas WHERE id_cuenta = '00000000-0000-0000-0000-0000000000a1'),
  TRUE, 'el intento rechazado no cambia cuentas.verificado');

-- 25 y 26. D: se rechaza su constancia pero conserva otro documento verificado (CURP) y la insignia.
SET LOCAL ROLE authenticated;
SET LOCAL request.jwt.claims = '{"sub":"00000000-0000-0000-0000-0000000000e5","role":"authenticated"}';
SELECT lives_ok(
  $$SELECT public.admin_resolver_verificacion_usuario((SELECT id FROM fila_d), FALSE, 'Documento ilegible, sube otro por favor')$$,
  'el admin rechaza la constancia de D');
RESET ROLE;
SELECT is(
  (SELECT verificado FROM public.cuentas WHERE id_cuenta = '00000000-0000-0000-0000-0000000000d4'),
  TRUE, 'rechazar con otro documento verificado conserva cuentas.verificado');

SELECT * FROM finish();
ROLLBACK;
