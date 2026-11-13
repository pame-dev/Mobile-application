-- pgTAP: panel_pendientes(). Correr SOLO contra BD local: supabase test db
-- ESTADO: ESCRITO, NO EJECUTADO (sin Docker en el entorno donde se creó).
-- Cubre: el administrador cuenta todo; moderador_publicaciones solo propuestas y reportes
-- pendientes; moderador_verificaciones solo destacados y verificaciones pendientes; una
-- cuenta normal recibe 0; anon no puede ejecutarla.
-- Se corre como superusuario fijando auth.uid() con request.jwt.claims. Los esperados se
-- calculan con conteos directos, así la prueba no depende de que la BD local esté vacía.
BEGIN;
SELECT plan(7);

-- Fixtures: E admin, P mod. publicaciones, V mod. verificaciones, A dueño (particular), B normal.
INSERT INTO auth.users (id, email) VALUES
  ('00000000-0000-0000-0000-0000000000e5', 'admin@test.local'),
  ('00000000-0000-0000-0000-0000000000f1', 'modpub@test.local'),
  ('00000000-0000-0000-0000-0000000000f2', 'modver@test.local'),
  ('00000000-0000-0000-0000-0000000000a1', 'a@test.local'),
  ('00000000-0000-0000-0000-0000000000b2', 'b@test.local');
INSERT INTO public.cuentas (id_cuenta, tipo_cuenta, nombre_mostrar, estado_perfil_ubi, municipio_perfil_ubi) VALUES
  ('00000000-0000-0000-0000-0000000000e5', 'lote',       'Admin',   'Jalisco', 'Guadalajara'),
  ('00000000-0000-0000-0000-0000000000f1', 'particular', 'Mod Pub', 'Jalisco', 'Guadalajara'),
  ('00000000-0000-0000-0000-0000000000f2', 'particular', 'Mod Ver', 'Jalisco', 'Guadalajara'),
  ('00000000-0000-0000-0000-0000000000a1', 'particular', 'Dueno A', 'Jalisco', 'Guadalajara'),
  ('00000000-0000-0000-0000-0000000000b2', 'particular', 'Normal B', 'Jalisco', 'Zapopan');
INSERT INTO public.perfiles_lote (id_cuenta, calle, numero, colonia, codigo_postal) VALUES
  ('00000000-0000-0000-0000-0000000000e5', 'Calle', '5', 'Centro', '44100');
INSERT INTO public.cuentas_roles (id_cuenta, id_rol)
SELECT x.id, r.id_rol
FROM (VALUES ('00000000-0000-0000-0000-0000000000e5'::uuid, 'administrador'),
             ('00000000-0000-0000-0000-0000000000f1'::uuid, 'moderador_publicaciones'),
             ('00000000-0000-0000-0000-0000000000f2'::uuid, 'moderador_verificaciones')) AS x(id, rol)
JOIN public.roles r ON r.nombre_rol = x.rol
ON CONFLICT (id_cuenta, id_rol) DO NOTHING;

-- Catálogos reales (no "Otros").
INSERT INTO public.marcas        (nombre_marca, es_otro)       VALUES ('MarcaTest', FALSE);
INSERT INTO public.colores       (nombre_color, es_otro)       VALUES ('ColorTest', FALSE);
INSERT INTO public.transmisiones (nombre_transmision, es_otro) VALUES ('TransmisionTest', FALSE);
INSERT INTO public.carrocerias   (nombre_carroceria, es_otro)  VALUES ('CarroceriaTest', FALSE);
INSERT INTO public.modelos (id_marca, nombre_modelo, es_otro)
SELECT id_marca, 'ModeloTest', FALSE FROM public.marcas WHERE nombre_marca = 'MarcaTest';

CREATE TEMP TABLE base_datos AS
SELECT jsonb_build_object(
  'titulo', 'Auto de prueba', 'descripcion', 'Auto de prueba', 'precio', 150000, 'moneda', 'MXN',
  'id_marca',  (SELECT id_marca FROM public.marcas WHERE nombre_marca = 'MarcaTest'),
  'id_modelo', (SELECT id_modelo FROM public.modelos WHERE nombre_modelo = 'ModeloTest'),
  'id_color',  (SELECT id_color FROM public.colores WHERE nombre_color = 'ColorTest'),
  'id_transmision', (SELECT id_transmision FROM public.transmisiones WHERE nombre_transmision = 'TransmisionTest'),
  'id_carroceria',  (SELECT id_carroceria FROM public.carrocerias WHERE nombre_carroceria = 'CarroceriaTest'),
  'anio', 2020, 'kilometraje', 30000, 'condicion_vehiculo', 'usado',
  'tipo_documento_vehiculo', 'vin', 'documento_vehiculo', '1HGCM82633A004352'
) AS j;

-- A publica: queda una propuesta pendiente.
SELECT set_config('request.jwt.claims', '{"sub":"00000000-0000-0000-0000-0000000000a1","role":"authenticated"}', TRUE);
SELECT public.crear_publicacion((SELECT j FROM base_datos), ARRAY[]::TEXT[]);
CREATE TEMP TABLE pub AS SELECT id_publicacion AS id FROM public.publicaciones
  WHERE id_propietario = '00000000-0000-0000-0000-0000000000a1';

-- Un reporte, una solicitud de destacado y una verificación, todos pendientes (superusuario).
INSERT INTO public.reportes (id_publicacion, id_reportante, motivo_reporte)
SELECT id, '00000000-0000-0000-0000-0000000000b2', 'Motivo de prueba' FROM pub;
INSERT INTO public.solicitudes_destacado (id_publicacion) SELECT id FROM pub;
INSERT INTO storage.objects (bucket_id, name, owner) VALUES
  ('constancias-fiscales', '00000000-0000-0000-0000-0000000000b2/constancia_situacion_fiscal_1.pdf',
   '00000000-0000-0000-0000-0000000000b2');
INSERT INTO public.verificaciones_usuario (user_id, tipo_usuario, tipo_documento, archivo_path) VALUES
  ('00000000-0000-0000-0000-0000000000b2', 'particular', 'constancia_situacion_fiscal',
   '00000000-0000-0000-0000-0000000000b2/constancia_situacion_fiscal_1.pdf');

-- Esperados con conteos directos.
CREATE TEMP TABLE esperado AS SELECT
  (SELECT count(*) FROM public.propuestas_publicacion WHERE estado_propuesta = 'pendiente')::int AS propuestas,
  (SELECT count(*) FROM public.reportes WHERE estado_reporte = 'pendiente')::int AS reportes,
  (SELECT count(*) FROM public.solicitudes_destacado WHERE estado_solicitud = 'pendiente')::int AS destacados,
  (SELECT count(*) FROM public.verificaciones_usuario WHERE estado = 'pendiente')::int AS verificaciones;
GRANT SELECT ON esperado TO authenticated, anon;

-- (1) Los fixtures dejan al menos uno de cada tipo (la prueba no pasa en vacío).
SELECT ok((SELECT propuestas >= 1 AND reportes >= 1 AND destacados >= 1 AND verificaciones >= 1 FROM esperado),
          'hay al menos un pendiente de cada tipo');

-- (2) Administrador: todo.
SET LOCAL ROLE authenticated;
SELECT set_config('request.jwt.claims', '{"sub":"00000000-0000-0000-0000-0000000000e5","role":"authenticated"}', true);
SELECT is(public.panel_pendientes(),
          (SELECT propuestas + reportes + destacados + verificaciones FROM esperado),
          'administrador cuenta propuestas, reportes, destacados y verificaciones');
RESET ROLE;

-- (3) moderador_publicaciones: solo propuestas y reportes.
SET LOCAL ROLE authenticated;
SELECT set_config('request.jwt.claims', '{"sub":"00000000-0000-0000-0000-0000000000f1","role":"authenticated"}', true);
SELECT is(public.panel_pendientes(),
          (SELECT propuestas + reportes FROM esperado),
          'moderador_publicaciones solo cuenta propuestas y reportes');
RESET ROLE;

-- (4) moderador_verificaciones: solo destacados y verificaciones.
SET LOCAL ROLE authenticated;
SELECT set_config('request.jwt.claims', '{"sub":"00000000-0000-0000-0000-0000000000f2","role":"authenticated"}', true);
SELECT is(public.panel_pendientes(),
          (SELECT destacados + verificaciones FROM esperado),
          'moderador_verificaciones solo cuenta destacados y verificaciones');
RESET ROLE;

-- (5) Cuenta normal: 0.
SET LOCAL ROLE authenticated;
SELECT set_config('request.jwt.claims', '{"sub":"00000000-0000-0000-0000-0000000000a1","role":"authenticated"}', true);
SELECT is(public.panel_pendientes(), 0, 'una cuenta normal recibe 0');
RESET ROLE;

-- (6) Otra cuenta normal (la que reportó y envió el documento) también recibe 0.
SET LOCAL ROLE authenticated;
SELECT set_config('request.jwt.claims', '{"sub":"00000000-0000-0000-0000-0000000000b2","role":"authenticated"}', true);
SELECT is(public.panel_pendientes(), 0, 'quien envió un reporte o documento sin ser del equipo recibe 0');
RESET ROLE;

-- (7) anon no puede ejecutarla.
SET LOCAL ROLE anon;
SELECT set_config('request.jwt.claims', '{"role":"anon"}', true);
SELECT throws_ok($$SELECT public.panel_pendientes()$$, '42501', NULL,
                 'anon no puede ejecutar panel_pendientes');
RESET ROLE;

SELECT * FROM finish();
ROLLBACK;
