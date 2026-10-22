-- pgTAP: factura original, aire acondicionado y condición (migración 20261031120000_factura_aire_condicion.sql).
-- Correr SOLO contra BD local: supabase test db
-- ESTADO: ESCRITO, NO EJECUTADO (sin Docker ni supabase CLI en el entorno donde se creó).
BEGIN;
SELECT plan(23);

-- ── Fixtures (como superusuario) ────────────────────────────────────────────
INSERT INTO auth.users (id, email) VALUES
  ('00000000-0000-0000-0000-0000000000a1', 'a@test.local'),
  ('00000000-0000-0000-0000-0000000000e5', 'admin@test.local');
INSERT INTO public.cuentas (id_cuenta, tipo_cuenta, nombre_mostrar, estado_perfil_ubi, municipio_perfil_ubi) VALUES
  ('00000000-0000-0000-0000-0000000000a1', 'particular', 'Vendedor A', 'Jalisco', 'Guadalajara'),
  ('00000000-0000-0000-0000-0000000000e5', 'lote',       'Admin',      'Jalisco', 'Guadalajara');
INSERT INTO public.cuentas_roles (id_cuenta, id_rol)
SELECT '00000000-0000-0000-0000-0000000000e5', r.id_rol FROM public.roles r WHERE r.nombre_rol = 'administrador'
ON CONFLICT (id_cuenta, id_rol) DO NOTHING;

INSERT INTO public.marcas        (nombre_marca, es_otro)       VALUES ('MarcaTest', FALSE);
INSERT INTO public.colores       (nombre_color, es_otro)       VALUES ('ColorTest', FALSE);
INSERT INTO public.transmisiones (nombre_transmision, es_otro) VALUES ('TransmisionTest', FALSE);
INSERT INTO public.carrocerias   (nombre_carroceria, es_otro)  VALUES ('CarroceriaTest', FALSE);
INSERT INTO public.modelos (id_marca, nombre_modelo, es_otro)
SELECT id_marca, 'ModeloTest', FALSE FROM public.marcas WHERE nombre_marca = 'MarcaTest';

-- Datos base con documento (placa) pero SIN condición, factura ni aire.
CREATE TEMP TABLE base_datos AS
SELECT jsonb_build_object(
  'titulo', 'Base', 'descripcion', 'Auto de prueba', 'precio', 150000, 'moneda', 'MXN',
  'id_marca',  (SELECT id_marca FROM public.marcas WHERE nombre_marca = 'MarcaTest'),
  'id_modelo', (SELECT id_modelo FROM public.modelos WHERE nombre_modelo = 'ModeloTest'),
  'id_color',  (SELECT id_color FROM public.colores WHERE nombre_color = 'ColorTest'),
  'id_transmision', (SELECT id_transmision FROM public.transmisiones WHERE nombre_transmision = 'TransmisionTest'),
  'id_carroceria',  (SELECT id_carroceria FROM public.carrocerias WHERE nombre_carroceria = 'CarroceriaTest'),
  'anio', 2020, 'kilometraje', 30000,
  'tipo_documento_vehiculo', 'placa', 'documento_vehiculo', 'ABC1234'
) AS j;
GRANT SELECT ON base_datos TO authenticated;

SET LOCAL ROLE authenticated;
SET LOCAL request.jwt.claims = '{"sub":"00000000-0000-0000-0000-0000000000a1","role":"authenticated"}';

-- ── Estructura ──────────────────────────────────────────────────────────────
SELECT has_column('public', 'publicaciones', 'factura_original', 'publicaciones tiene factura_original');
SELECT has_column('public', 'publicaciones', 'aire_acondicionado', 'publicaciones tiene aire_acondicionado');
SELECT has_column('public', 'publicaciones', 'condicion_vehiculo', 'publicaciones tiene condicion_vehiculo');
SELECT has_column('public', 'propuestas_publicacion', 'condicion_vehiculo', 'propuestas_publicacion tiene condicion_vehiculo');
SELECT has_column('public', 'v_anuncios', 'factura_original', 'v_anuncios expone factura_original');
SELECT has_column('public', 'v_anuncios', 'aire_acondicionado', 'v_anuncios expone aire_acondicionado');
SELECT has_column('public', 'v_anuncios', 'condicion_vehiculo', 'v_anuncios expone condicion_vehiculo');

-- ── condicion_vehiculo_valida ───────────────────────────────────────────────
SELECT is(public.condicion_vehiculo_valida('usado'), 'usado', 'usado es válida');
SELECT is(public.condicion_vehiculo_valida('seminuevo'), 'seminuevo', 'seminuevo es válida');
SELECT throws_ok($$SELECT public.condicion_vehiculo_valida(NULL)$$,
  'P0001', 'Elige si el vehículo es seminuevo o usado.', 'condición nula se rechaza');
SELECT throws_ok($$SELECT public.condicion_vehiculo_valida('nuevo')$$,
  'P0001', 'Elige si el vehículo es seminuevo o usado.', 'condición desconocida se rechaza');

-- ── crear_publicacion ──────────────────────────────────────────────────────
SELECT throws_ok(
  $$SELECT public.crear_publicacion((SELECT j FROM base_datos), ARRAY[]::TEXT[])$$,
  'P0001', 'Elige si el vehículo es seminuevo o usado.', 'crear sin condición falla');
SELECT throws_ok(
  $$SELECT public.crear_publicacion((SELECT j FROM base_datos) || '{"condicion_vehiculo":""}', ARRAY[]::TEXT[])$$,
  'P0001', 'Elige si el vehículo es seminuevo o usado.', 'crear con condición vacía falla');

SELECT lives_ok(
  $$SELECT public.crear_publicacion(
      (SELECT j FROM base_datos) ||
      '{"titulo":"t-cond","condicion_vehiculo":"usado","factura_original":true,"aire_acondicionado":true}',
      ARRAY[]::TEXT[])$$,
  'crear con condición, factura y aire funciona');
SELECT results_eq(
  $$SELECT factura_original, aire_acondicionado, condicion_vehiculo
    FROM public.propuestas_publicacion WHERE titulo = 't-cond'$$,
  $$VALUES (TRUE, TRUE, 'usado'::VARCHAR)$$,
  'la propuesta guarda los tres datos');

-- Factura y aire son opcionales: si no vienen quedan en FALSE.
SELECT lives_ok(
  $$SELECT public.crear_publicacion(
      (SELECT j FROM base_datos) || '{"titulo":"t-solo-cond","condicion_vehiculo":"seminuevo"}', ARRAY[]::TEXT[])$$,
  'crear solo con condición funciona');
SELECT results_eq(
  $$SELECT factura_original, aire_acondicionado, condicion_vehiculo
    FROM public.propuestas_publicacion WHERE titulo = 't-solo-cond'$$,
  $$VALUES (FALSE, FALSE, 'seminuevo'::VARCHAR)$$,
  'factura y aire por defecto en FALSE');

-- ── editar_publicacion ─────────────────────────────────────────────────────
CREATE TEMP TABLE pubs AS
SELECT pp.id_publicacion AS id FROM public.propuestas_publicacion pp WHERE pp.titulo = 't-cond';
GRANT SELECT ON pubs TO authenticated;

SELECT throws_ok(
  $$SELECT public.editar_publicacion((SELECT id FROM pubs),
      (SELECT j FROM base_datos) || '{"titulo":"t-cond"}',
      ARRAY['publicaciones/00000000-0000-0000-0000-0000000000a1/f1.jpg'])$$,
  'P0001', 'Elige si el vehículo es seminuevo o usado.', 'editar sin condición falla');
SELECT lives_ok(
  $$SELECT public.editar_publicacion((SELECT id FROM pubs),
      (SELECT j FROM base_datos) ||
      '{"titulo":"t-cond-ed","condicion_vehiculo":"seminuevo","factura_original":false,"aire_acondicionado":true}',
      ARRAY['publicaciones/00000000-0000-0000-0000-0000000000a1/f1.jpg'])$$,
  'editar con nueva condición funciona');
SELECT results_eq(
  $$SELECT factura_original, aire_acondicionado, condicion_vehiculo
    FROM public.propuestas_publicacion WHERE id_publicacion = (SELECT id FROM pubs)
    ORDER BY id_propuesta DESC LIMIT 1$$,
  $$VALUES (FALSE, TRUE, 'seminuevo'::VARCHAR)$$,
  'editar actualiza los tres datos');

-- ── Aprobar copia a publicaciones y v_anuncios los expone ──────────────────
SET LOCAL request.jwt.claims = '{"sub":"00000000-0000-0000-0000-0000000000e5","role":"authenticated"}';
SELECT lives_ok(
  $$SELECT public.admin_moderar_propuesta(
      (SELECT id_propuesta FROM public.propuestas_publicacion
       WHERE id_publicacion = (SELECT id FROM pubs) AND estado_propuesta = 'pendiente'),
      TRUE)$$,
  'el admin aprueba la propuesta');
RESET ROLE;
SELECT results_eq(
  $$SELECT factura_original, aire_acondicionado, condicion_vehiculo
    FROM public.publicaciones WHERE id_publicacion = (SELECT id FROM pubs)$$,
  $$VALUES (FALSE, TRUE, 'seminuevo'::VARCHAR)$$,
  'aprobar copia los tres datos a publicaciones');
SELECT results_eq(
  $$SELECT factura_original, aire_acondicionado, condicion_vehiculo
    FROM public.v_anuncios WHERE id_publicacion = (SELECT id FROM pubs)$$,
  $$VALUES (FALSE, TRUE, 'seminuevo'::VARCHAR)$$,
  'v_anuncios muestra los tres datos del anuncio aprobado');

SELECT * FROM finish();
ROLLBACK;
