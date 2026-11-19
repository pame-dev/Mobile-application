-- pgTAP: CURP cifrada (migración 20261119120000_curp_cifrada). Correr SOLO contra BD local: supabase test db
-- ESTADO: ESCRITO, NO EJECUTADO (sin supabase CLI/Docker en el entorno donde se creó).
BEGIN;
SELECT plan(26);

-- Fixtures (como superusuario): P1 y P2 particulares, C un lote con constancia, E administrador
-- (solo una cuenta lote puede serlo), P3 y P4 particulares para probar la restricción.
INSERT INTO auth.users (id, email) VALUES
  ('00000000-0000-0000-0000-0000000000a1', 'p1@test.local'),
  ('00000000-0000-0000-0000-0000000000b2', 'p2@test.local'),
  ('00000000-0000-0000-0000-0000000000c3', 'lote_c@test.local'),
  ('00000000-0000-0000-0000-0000000000e5', 'admin@test.local'),
  ('00000000-0000-0000-0000-0000000000f6', 'p3@test.local'),
  ('00000000-0000-0000-0000-0000000000f7', 'p4@test.local');
INSERT INTO public.cuentas (id_cuenta, tipo_cuenta, nombre_mostrar, estado_perfil_ubi, municipio_perfil_ubi) VALUES
  ('00000000-0000-0000-0000-0000000000a1', 'particular', 'Particular 1', 'Jalisco', 'Guadalajara'),
  ('00000000-0000-0000-0000-0000000000b2', 'particular', 'Particular 2', 'Jalisco', 'Zapopan'),
  ('00000000-0000-0000-0000-0000000000c3', 'lote', 'Lote C', 'Jalisco', 'Zapopan'),
  ('00000000-0000-0000-0000-0000000000e5', 'lote', 'Admin', 'Jalisco', 'Guadalajara'),
  ('00000000-0000-0000-0000-0000000000f6', 'particular', 'Particular 3', 'Jalisco', 'Guadalajara'),
  ('00000000-0000-0000-0000-0000000000f7', 'particular', 'Particular 4', 'Jalisco', 'Zapopan');
INSERT INTO public.perfiles_lote (id_cuenta, calle, numero, colonia, codigo_postal) VALUES
  ('00000000-0000-0000-0000-0000000000c3', 'Calle', '3', 'Centro', '45100');
INSERT INTO public.cuentas_roles (id_cuenta, id_rol)
SELECT '00000000-0000-0000-0000-0000000000e5', r.id_rol FROM public.roles r WHERE r.nombre_rol = 'administrador'
ON CONFLICT (id_cuenta, id_rol) DO NOTHING;
-- Constancia ya enviada por C (pendiente, con archivo).
INSERT INTO public.verificaciones_usuario (user_id, tipo_usuario, tipo_documento, archivo_path) VALUES
  ('00000000-0000-0000-0000-0000000000c3', 'lote', 'constancia_situacion_fiscal',
   '00000000-0000-0000-0000-0000000000c3/constancia_situacion_fiscal_1.pdf');
CREATE TEMP TABLE fila_c AS
  SELECT id FROM public.verificaciones_usuario
  WHERE user_id = '00000000-0000-0000-0000-0000000000c3' AND tipo_documento = 'constancia_situacion_fiscal';
GRANT SELECT ON fila_c TO authenticated;

-- ── 1 a 4. P1 envía su CURP: queda cifrada, con hash y sin texto plano ──────
SET LOCAL ROLE authenticated;
SET LOCAL request.jwt.claims = '{"sub":"00000000-0000-0000-0000-0000000000a1","role":"authenticated"}';
SELECT lives_ok(
  $$SELECT public.enviar_curp_verificacion('gomc 800101 hjcrrl09')$$,
  'P1 envía su CURP (se normaliza a mayúsculas y sin espacios)');

RESET ROLE;
SELECT ok(
  (SELECT curp IS NULL FROM public.verificaciones_usuario
   WHERE user_id = '00000000-0000-0000-0000-0000000000a1' AND tipo_documento = 'curp'),
  'la columna curp queda en NULL');
SELECT ok(
  (SELECT curp_cifrada IS NOT NULL FROM public.verificaciones_usuario
   WHERE user_id = '00000000-0000-0000-0000-0000000000a1' AND tipo_documento = 'curp'),
  'curp_cifrada queda con datos');
SELECT is(
  (SELECT CHAR_LENGTH(curp_hash) FROM public.verificaciones_usuario
   WHERE user_id = '00000000-0000-0000-0000-0000000000a1' AND tipo_documento = 'curp'),
  64, 'curp_hash tiene 64 caracteres (HMAC-SHA256 hex)');
CREATE TEMP TABLE fila_p1 AS
  SELECT id FROM public.verificaciones_usuario
  WHERE user_id = '00000000-0000-0000-0000-0000000000a1' AND tipo_documento = 'curp';
GRANT SELECT ON fila_p1 TO authenticated;

-- ── 5 y 6. mi_ultima_verificacion: el dueño ve su CURP, otro usuario no ─────
SET LOCAL ROLE authenticated;
SET LOCAL request.jwt.claims = '{"sub":"00000000-0000-0000-0000-0000000000a1","role":"authenticated"}';
SELECT is(
  (SELECT curp FROM public.mi_ultima_verificacion()),
  'GOMC800101HJCRRL09', 'mi_ultima_verificacion devuelve la CURP en claro al dueño');

SET LOCAL request.jwt.claims = '{"sub":"00000000-0000-0000-0000-0000000000b2","role":"authenticated"}';
SELECT is(
  (SELECT COUNT(*)::INT FROM public.mi_ultima_verificacion()),
  0, 'otro usuario no recibe la fila de P1 (ni ninguna: no ha enviado nada)');

-- ── 7 a 9. Permisos por columna ─────────────────────────────────────────────
SET LOCAL request.jwt.claims = '{"sub":"00000000-0000-0000-0000-0000000000a1","role":"authenticated"}';
SELECT throws_ok(
  $$SELECT curp_cifrada FROM public.verificaciones_usuario$$,
  '42501', NULL, 'authenticated no puede leer curp_cifrada');
SELECT throws_ok(
  $$SELECT curp_hash FROM public.verificaciones_usuario$$,
  '42501', NULL, 'authenticated no puede leer curp_hash');
SELECT lives_ok(
  $$SELECT estado, curp FROM public.verificaciones_usuario$$,
  'authenticated sí puede leer estado y curp (siempre NULL)');

-- ── 10 a 13. Funciones internas sin EXECUTE ─────────────────────────────────
SELECT throws_ok(
  $$SELECT public.curp_descifrar('\x00'::BYTEA)$$,
  '42501', NULL, 'authenticated no puede llamar curp_descifrar');
SELECT throws_ok(
  $$SELECT public.curp_llave()$$,
  '42501', NULL, 'authenticated no puede llamar curp_llave');
SELECT throws_ok(
  $$SELECT public.curp_hash('GOMC800101HJCRRL09')$$,
  '42501', NULL, 'authenticated no puede llamar curp_hash');
SELECT throws_ok(
  $$SELECT public.curp_cifrar('GOMC800101HJCRRL09')$$,
  '42501', NULL, 'authenticated no puede llamar curp_cifrar');

-- ── 14. P2 intenta registrar la misma CURP ──────────────────────────────────
SET LOCAL request.jwt.claims = '{"sub":"00000000-0000-0000-0000-0000000000b2","role":"authenticated"}';
SELECT throws_ok(
  $$SELECT public.enviar_curp_verificacion('GOMC800101HJCRRL09')$$,
  'P0001', 'Esta CURP ya está registrada en otra cuenta.',
  'la misma CURP en otra cuenta se rechaza (por hash)');

-- ── 15. El admin ve la CURP descifrada ──────────────────────────────────────
SET LOCAL request.jwt.claims = '{"sub":"00000000-0000-0000-0000-0000000000e5","role":"authenticated"}';
SELECT is(
  (SELECT curp FROM public.admin_listar_verificaciones_usuario()
   WHERE user_id = '00000000-0000-0000-0000-0000000000a1' AND tipo_documento = 'curp'),
  'GOMC800101HJCRRL09', 'admin_listar_verificaciones_usuario devuelve la CURP descifrada');

-- ── 16 a 18. Aprobar la constancia: archivo_path NULL y lote verificado ─────
SELECT lives_ok(
  $$SELECT public.admin_resolver_verificacion_usuario((SELECT id FROM fila_c), TRUE)$$,
  'el admin aprueba la constancia de C');
RESET ROLE;
SELECT is(
  (SELECT archivo_path FROM public.verificaciones_usuario WHERE id = (SELECT id FROM fila_c)),
  NULL::TEXT, 'al aprobar la constancia archivo_path queda NULL');
SELECT is(
  (SELECT verificado FROM public.cuentas WHERE id_cuenta = '00000000-0000-0000-0000-0000000000c3'),
  TRUE, 'al aprobar la constancia cuentas.verificado = true');

-- ── 19 a 22. Aprobar la CURP: la fila sigue válida y legible ────────────────
SET LOCAL ROLE authenticated;
SET LOCAL request.jwt.claims = '{"sub":"00000000-0000-0000-0000-0000000000e5","role":"authenticated"}';
SELECT lives_ok(
  $$SELECT public.admin_resolver_verificacion_usuario((SELECT id FROM fila_p1), TRUE)$$,
  'el admin aprueba la CURP de P1');
RESET ROLE;
SELECT results_eq(
  $$SELECT v.estado, v.archivo_path IS NULL, v.curp IS NULL, v.curp_cifrada IS NOT NULL, CHAR_LENGTH(v.curp_hash)
    FROM public.verificaciones_usuario v WHERE v.id = (SELECT id FROM fila_p1)$$,
  $$VALUES ('verificado'::TEXT, TRUE, TRUE, TRUE, 64)$$,
  'tras aprobar la CURP la fila sigue cumpliendo la restricción');
SELECT is(
  (SELECT verificado FROM public.cuentas WHERE id_cuenta = '00000000-0000-0000-0000-0000000000a1'),
  TRUE, 'al aprobar la CURP cuentas.verificado = true');
SET LOCAL ROLE authenticated;
SET LOCAL request.jwt.claims = '{"sub":"00000000-0000-0000-0000-0000000000a1","role":"authenticated"}';
SELECT is(
  (SELECT curp FROM public.mi_ultima_verificacion()),
  'GOMC800101HJCRRL09', 'P1 sigue viendo su CURP después de la aprobación');

-- ── 23 a 26. Filas insertadas a mano (como el UPDATE de datos existentes) ───
RESET ROLE;
SELECT lives_ok(
  $$INSERT INTO public.verificaciones_usuario (user_id, tipo_usuario, tipo_documento, curp_cifrada, curp_hash)
    VALUES ('00000000-0000-0000-0000-0000000000f6', 'particular', 'curp',
            public.curp_cifrar('LOPM900202MDFPRR05'), public.curp_hash('LOPM900202MDFPRR05'))$$,
  'se puede insertar una fila curp con curp_cifrar y curp_hash');
SELECT is(
  (SELECT public.curp_descifrar(curp_cifrada) FROM public.verificaciones_usuario
   WHERE user_id = '00000000-0000-0000-0000-0000000000f6'),
  'LOPM900202MDFPRR05', 'curp_descifrar recupera la CURP cifrada');
SELECT throws_ok(
  $$INSERT INTO public.verificaciones_usuario (user_id, tipo_usuario, tipo_documento, curp)
    VALUES ('00000000-0000-0000-0000-0000000000f7', 'particular', 'curp', 'GOMC800101HJCRRL09')$$,
  '23514', NULL, 'la restricción rechaza una fila con curp en claro');
SELECT throws_ok(
  $$INSERT INTO public.verificaciones_usuario (user_id, tipo_usuario, tipo_documento, curp_cifrada, curp_hash)
    VALUES ('00000000-0000-0000-0000-0000000000f7', 'particular', 'curp',
            public.curp_cifrar('LOPM900202MDFPRR05'), public.curp_hash('LOPM900202MDFPRR05'))$$,
  '23505', NULL, 'el índice único por curp_hash impide la misma CURP en dos cuentas activas');

SELECT * FROM finish();
ROLLBACK;
