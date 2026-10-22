-- pgTAP: documento del vehículo (migración 20261028120000_documento_vehiculo.sql).
-- Correr SOLO contra BD local: supabase test db
-- ESTADO: ESCRITO, NO EJECUTADO (sin Docker en el entorno donde se creó; el CLI de supabase está
-- instalado pero `supabase test db` necesita Docker). Revisar el primer resultado real antes de fiarse.
-- Cubre: función documento_vehiculo_valido, crear_publicacion y editar_publicacion con documento,
-- RLS de propuestas_publicacion y del bucket documentos-vehiculo, que el documento no llegue a
-- publicaciones ni a v_anuncios al aprobar, y el CHECK chk_propuestas_publicacion_documento_vehiculo.
BEGIN;
SELECT plan(48);

-- ── Fixtures (como superusuario) ────────────────────────────────────────────
-- A y B: particulares. E: administrador (cuenta lote, único tipo que puede ser admin).
INSERT INTO auth.users (id, email) VALUES
  ('00000000-0000-0000-0000-0000000000a1', 'a@test.local'),
  ('00000000-0000-0000-0000-0000000000b2', 'b@test.local'),
  ('00000000-0000-0000-0000-0000000000e5', 'admin@test.local');
INSERT INTO public.cuentas (id_cuenta, tipo_cuenta, nombre_mostrar, estado_perfil_ubi, municipio_perfil_ubi) VALUES
  ('00000000-0000-0000-0000-0000000000a1', 'particular', 'Vendedor A', 'Jalisco', 'Guadalajara'),
  ('00000000-0000-0000-0000-0000000000b2', 'particular', 'Vendedor B', 'Jalisco', 'Zapopan'),
  ('00000000-0000-0000-0000-0000000000e5', 'lote',       'Admin',      'Jalisco', 'Guadalajara');
INSERT INTO public.cuentas_roles (id_cuenta, id_rol)
SELECT '00000000-0000-0000-0000-0000000000e5', r.id_rol FROM public.roles r WHERE r.nombre_rol = 'administrador'
ON CONFLICT (id_cuenta, id_rol) DO NOTHING;

-- Catálogos reales (no "Otros": crear_publicacion no guarda color_otro, etc.).
INSERT INTO public.marcas        (nombre_marca, es_otro)            VALUES ('MarcaTest', FALSE);
INSERT INTO public.colores       (nombre_color, es_otro)            VALUES ('ColorTest', FALSE);
INSERT INTO public.transmisiones (nombre_transmision, es_otro)      VALUES ('TransmisionTest', FALSE);
INSERT INTO public.carrocerias   (nombre_carroceria, es_otro)       VALUES ('CarroceriaTest', FALSE);
INSERT INTO public.modelos (id_marca, nombre_modelo, es_otro)
SELECT id_marca, 'ModeloTest', FALSE FROM public.marcas WHERE nombre_marca = 'MarcaTest';

-- Fotos de documento ya subidas: dos de A y una de B.
INSERT INTO storage.objects (bucket_id, name, owner) VALUES
  ('documentos-vehiculo', '00000000-0000-0000-0000-0000000000a1/11111111-1111-1111-1111-111111111111.jpg', '00000000-0000-0000-0000-0000000000a1'),
  ('documentos-vehiculo', '00000000-0000-0000-0000-0000000000a1/22222222-2222-2222-2222-222222222222.jpg', '00000000-0000-0000-0000-0000000000a1'),
  ('documentos-vehiculo', '00000000-0000-0000-0000-0000000000b2/33333333-3333-3333-3333-333333333333.jpg', '00000000-0000-0000-0000-0000000000b2');

-- Datos base de una publicación; cada prueba le suma el documento y el título con ||.
CREATE TEMP TABLE base_datos AS
SELECT jsonb_build_object(
  'titulo', 'Base', 'descripcion', 'Auto de prueba', 'precio', 150000, 'moneda', 'MXN',
  'id_marca',  (SELECT id_marca FROM public.marcas WHERE nombre_marca = 'MarcaTest'),
  'id_modelo', (SELECT id_modelo FROM public.modelos WHERE nombre_modelo = 'ModeloTest'),
  'id_color',  (SELECT id_color FROM public.colores WHERE nombre_color = 'ColorTest'),
  'id_transmision', (SELECT id_transmision FROM public.transmisiones WHERE nombre_transmision = 'TransmisionTest'),
  'id_carroceria',  (SELECT id_carroceria FROM public.carrocerias WHERE nombre_carroceria = 'CarroceriaTest'),
  'anio', 2020, 'kilometraje', 30000, 'condicion_vehiculo', 'usado'
) AS j;
GRANT SELECT ON base_datos TO authenticated;

SET LOCAL ROLE authenticated;
SET LOCAL request.jwt.claims = '{"sub":"00000000-0000-0000-0000-0000000000a1","role":"authenticated"}';

-- ── documento_vehiculo_valido ───────────────────────────────────────────────

-- 1 y 2. El VIN se normaliza: sin espacios y en mayúsculas.
SELECT is(
  public.documento_vehiculo_valido('vin', ' abcd efgh jklm npr12 '),
  'ABCDEFGHJKLMNPR12', 'el VIN se guarda sin espacios y en mayúsculas');
-- 2. La placa también (el guion se conserva).
SELECT is(
  public.documento_vehiculo_valido('placa', ' ab-12 3 '),
  'AB-123', 'la placa se guarda sin espacios y en mayúsculas');

-- 3 a 5. Tipo vacío, nulo o inválido.
SELECT throws_ok(
  $$SELECT public.documento_vehiculo_valido(NULL, 'ABC1234')$$,
  'P0001', 'Elige qué documento del vehículo vas a agregar.', 'tipo nulo se rechaza');
SELECT throws_ok(
  $$SELECT public.documento_vehiculo_valido('', 'ABC1234')$$,
  'P0001', 'Elige qué documento del vehículo vas a agregar.', 'tipo vacío se rechaza');
SELECT throws_ok(
  $$SELECT public.documento_vehiculo_valido('licencia', 'ABC1234')$$,
  'P0001', 'Elige qué documento del vehículo vas a agregar.', 'tipo inválido se rechaza');

-- 6 a 9. Formatos de VIN y placa.
SELECT throws_ok(
  $$SELECT public.documento_vehiculo_valido('vin', '1HGCM82633A00435')$$,
  'P0001', 'El VIN debe tener 17 letras o números (sin I, O ni Q).', 'VIN de 16 caracteres se rechaza');
SELECT throws_ok(
  $$SELECT public.documento_vehiculo_valido('vin', '1HGCM82633A0043521')$$,
  'P0001', 'El VIN debe tener 17 letras o números (sin I, O ni Q).', 'VIN de 18 caracteres se rechaza');
SELECT throws_ok(
  $$SELECT public.documento_vehiculo_valido('vin', '1HGCM82633I004352')$$,
  'P0001', 'El VIN debe tener 17 letras o números (sin I, O ni Q).', 'VIN con I se rechaza (igual con O y Q)');
SELECT throws_ok(
  $$SELECT public.documento_vehiculo_valido('placa', 'AB1')$$,
  'P0001', 'La placa debe tener de 4 a 10 letras o números.', 'placa de 3 caracteres se rechaza');

-- 10 y 11. Foto: sin valor y de la carpeta de otro usuario.
SELECT throws_ok(
  $$SELECT public.documento_vehiculo_valido('tarjeta_circulacion', NULL)$$,
  'P0001', 'Agrega la foto o el PDF del documento del vehículo.', 'tarjeta sin foto se rechaza');
SELECT throws_ok(
  $$SELECT public.documento_vehiculo_valido('baja',
      '00000000-0000-0000-0000-0000000000b2/33333333-3333-3333-3333-333333333333.jpg')$$,
  'P0001', 'Agrega la foto o el PDF del documento del vehículo.', 'foto de la carpeta de otro usuario se rechaza');
-- 12. Foto propia pero que no existe en storage.objects.
SELECT throws_ok(
  $$SELECT public.documento_vehiculo_valido('baja',
      '00000000-0000-0000-0000-0000000000a1/44444444-4444-4444-4444-444444444444.jpg')$$,
  'P0001', 'Agrega la foto o el PDF del documento del vehículo.', 'foto que no existe en el bucket se rechaza');
-- 13. Foto propia que existe.
SELECT is(
  public.documento_vehiculo_valido('baja',
    '00000000-0000-0000-0000-0000000000a1/11111111-1111-1111-1111-111111111111.jpg'),
  '00000000-0000-0000-0000-0000000000a1/11111111-1111-1111-1111-111111111111.jpg',
  'una foto propia que existe se acepta tal cual');

-- ── crear_publicacion ──────────────────────────────────────────────────────

-- 14 a 16. Sin documento falla (no hay campo, tipo vacío o tipo inválido).
SELECT throws_ok(
  $$SELECT public.crear_publicacion((SELECT j FROM base_datos), ARRAY[]::TEXT[])$$,
  'P0001', 'Elige qué documento del vehículo vas a agregar.', 'crear sin documento falla');
SELECT throws_ok(
  $$SELECT public.crear_publicacion((SELECT j FROM base_datos) || '{"tipo_documento_vehiculo":""}', ARRAY[]::TEXT[])$$,
  'P0001', 'Elige qué documento del vehículo vas a agregar.', 'crear con tipo vacío falla');
SELECT throws_ok(
  $$SELECT public.crear_publicacion(
      (SELECT j FROM base_datos) || '{"tipo_documento_vehiculo":"vin","documento_vehiculo":"123"}', ARRAY[]::TEXT[])$$,
  'P0001', 'El VIN debe tener 17 letras o números (sin I, O ni Q).', 'crear con VIN inválido falla');

-- 17. Las fallas no dejan una publicación a medias (la validación va antes del INSERT).
SELECT is(
  (SELECT COUNT(*)::INT FROM public.publicaciones WHERE id_propietario = '00000000-0000-0000-0000-0000000000a1'),
  0, 'crear con documento inválido no deja publicaciones huérfanas');

-- 18. Con VIN (minúsculas y espacios) crea la publicación.
SELECT lives_ok(
  $$SELECT public.crear_publicacion(
      (SELECT j FROM base_datos) || '{"titulo":"t-vin","tipo_documento_vehiculo":"vin","documento_vehiculo":" 1hgcm 82633a 004352 "}',
      ARRAY[]::TEXT[])$$,
  'crear con VIN válido funciona');
-- 19. Con foto de tarjeta de circulación propia.
SELECT lives_ok(
  $$SELECT public.crear_publicacion(
      (SELECT j FROM base_datos) || '{"titulo":"t-tarjeta","tipo_documento_vehiculo":"tarjeta_circulacion","documento_vehiculo":"00000000-0000-0000-0000-0000000000a1/22222222-2222-2222-2222-222222222222.jpg"}',
      ARRAY[]::TEXT[])$$,
  'crear con foto de tarjeta de circulación funciona');

-- 20 y 21. Se guardó normalizado.
SELECT results_eq(
  $$SELECT tipo_documento_vehiculo, documento_vehiculo FROM public.propuestas_publicacion WHERE titulo = 't-vin'$$,
  $$VALUES ('vin'::VARCHAR, '1HGCM82633A004352'::TEXT)$$,
  'el VIN queda guardado en mayúsculas y sin espacios');
SELECT results_eq(
  $$SELECT tipo_documento_vehiculo, documento_vehiculo FROM public.propuestas_publicacion WHERE titulo = 't-tarjeta'$$,
  $$VALUES ('tarjeta_circulacion'::VARCHAR,
            '00000000-0000-0000-0000-0000000000a1/22222222-2222-2222-2222-222222222222.jpg'::TEXT)$$,
  'la tarjeta queda guardada con la ruta de la foto');

-- Ids de las publicaciones creadas, para usarlos al editar.
RESET ROLE;
CREATE TEMP TABLE pubs AS
SELECT p.titulo AS nombre, p.id_publicacion AS id FROM public.propuestas_publicacion p;
GRANT SELECT ON pubs TO authenticated;

-- ── RLS ────────────────────────────────────────────────────────────────────

-- 22. El dueño lee su documento.
SET LOCAL ROLE authenticated;
SET LOCAL request.jwt.claims = '{"sub":"00000000-0000-0000-0000-0000000000a1","role":"authenticated"}';
SELECT is(
  (SELECT COUNT(*)::INT FROM public.propuestas_publicacion WHERE documento_vehiculo IS NOT NULL),
  2, 'el dueño ve el documento de sus propuestas');
-- 23. Su foto en el bucket también.
SELECT is(
  (SELECT COUNT(*)::INT FROM storage.objects WHERE bucket_id = 'documentos-vehiculo'),
  2, 'el dueño ve solo sus fotos de documento');

-- 24 y 25. Otro usuario no lee ni el documento ni la foto.
SET LOCAL request.jwt.claims = '{"sub":"00000000-0000-0000-0000-0000000000b2","role":"authenticated"}';
SELECT is(
  (SELECT COUNT(*)::INT FROM public.propuestas_publicacion WHERE documento_vehiculo IS NOT NULL),
  0, 'otro usuario no lee el documento de propuestas_publicacion');
SELECT is(
  (SELECT COUNT(*)::INT FROM storage.objects
   WHERE bucket_id = 'documentos-vehiculo' AND name LIKE '00000000-0000-0000-0000-0000000000a1/%'),
  0, 'otro usuario no ve las fotos de documento de A');
-- 26. Tampoco puede subir a la carpeta de A.
SELECT throws_ok(
  $$INSERT INTO storage.objects (bucket_id, name, owner)
    VALUES ('documentos-vehiculo', '00000000-0000-0000-0000-0000000000a1/55555555-5555-5555-5555-555555555555.jpg',
            '00000000-0000-0000-0000-0000000000b2')$$,
  '42501', NULL, 'otro usuario no puede subir a la carpeta de A');

-- 27 y 28. El admin lee los documentos y las fotos de todos.
SET LOCAL request.jwt.claims = '{"sub":"00000000-0000-0000-0000-0000000000e5","role":"authenticated"}';
SELECT is(
  (SELECT COUNT(*)::INT FROM public.propuestas_publicacion WHERE documento_vehiculo IS NOT NULL),
  2, 'el admin ve el documento de las propuestas');
SELECT is(
  (SELECT COUNT(*)::INT FROM storage.objects WHERE bucket_id = 'documentos-vehiculo'),
  3, 'el admin ve las fotos de documento de todos');

-- ── editar_publicacion ─────────────────────────────────────────────────────

SET LOCAL request.jwt.claims = '{"sub":"00000000-0000-0000-0000-0000000000a1","role":"authenticated"}';

-- 29. Sin documento falla.
SELECT throws_ok(
  $$SELECT public.editar_publicacion((SELECT id FROM pubs WHERE nombre = 't-vin'),
      (SELECT j FROM base_datos) || '{"titulo":"t-vin"}',
      ARRAY['publicaciones/00000000-0000-0000-0000-0000000000a1/f1.jpg'])$$,
  'P0001', 'Elige qué documento del vehículo vas a agregar.', 'editar sin documento falla');
-- 30. Con la foto de otro usuario falla.
SELECT throws_ok(
  $$SELECT public.editar_publicacion((SELECT id FROM pubs WHERE nombre = 't-vin'),
      (SELECT j FROM base_datos) || '{"titulo":"t-vin","tipo_documento_vehiculo":"baja","documento_vehiculo":"00000000-0000-0000-0000-0000000000b2/33333333-3333-3333-3333-333333333333.jpg"}',
      ARRAY['publicaciones/00000000-0000-0000-0000-0000000000a1/f1.jpg'])$$,
  'P0001', 'Agrega la foto o el PDF del documento del vehículo.', 'editar con la foto de otro usuario falla');
-- 31. Cambiar de VIN a placa guarda la placa normalizada.
SELECT lives_ok(
  $$SELECT public.editar_publicacion((SELECT id FROM pubs WHERE nombre = 't-vin'),
      (SELECT j FROM base_datos) || '{"titulo":"t-vin-ed","tipo_documento_vehiculo":"placa","documento_vehiculo":" ab-123 "}',
      ARRAY['publicaciones/00000000-0000-0000-0000-0000000000a1/f1.jpg'])$$,
  'editar con placa funciona');
SELECT results_eq(
  $$SELECT tipo_documento_vehiculo, documento_vehiculo, estado_propuesta
    FROM public.propuestas_publicacion WHERE id_publicacion = (SELECT id FROM pubs WHERE nombre = 't-vin')$$,
  $$VALUES ('placa'::VARCHAR, 'AB-123'::TEXT, 'pendiente'::VARCHAR)$$,
  'editar reemplaza el documento y deja la propuesta pendiente');
-- 33. Cambiar a foto de baja.
SELECT lives_ok(
  $$SELECT public.editar_publicacion((SELECT id FROM pubs WHERE nombre = 't-vin'),
      (SELECT j FROM base_datos) || '{"titulo":"t-vin-ed","tipo_documento_vehiculo":"baja","documento_vehiculo":"00000000-0000-0000-0000-0000000000a1/11111111-1111-1111-1111-111111111111.jpg"}',
      ARRAY['publicaciones/00000000-0000-0000-0000-0000000000a1/f1.jpg'])$$,
  'editar con foto de baja funciona');
SELECT results_eq(
  $$SELECT tipo_documento_vehiculo, documento_vehiculo
    FROM public.propuestas_publicacion WHERE id_publicacion = (SELECT id FROM pubs WHERE nombre = 't-vin')$$,
  $$VALUES ('baja'::VARCHAR, '00000000-0000-0000-0000-0000000000a1/11111111-1111-1111-1111-111111111111.jpg'::TEXT)$$,
  'editar guarda la ruta de la foto de baja');

-- ── admin_moderar_propuesta: aprobar no copia el documento ─────────────────

-- 35. Estructura: ni publicaciones ni v_anuncios tienen las columnas (las lee el público).
SELECT hasnt_column('public', 'publicaciones', 'documento_vehiculo', 'publicaciones no tiene documento_vehiculo');
SELECT hasnt_column('public', 'publicaciones', 'tipo_documento_vehiculo', 'publicaciones no tiene tipo_documento_vehiculo');
SELECT hasnt_column('public', 'v_anuncios', 'documento_vehiculo', 'v_anuncios no expone documento_vehiculo');
SELECT hasnt_column('public', 'v_anuncios', 'tipo_documento_vehiculo', 'v_anuncios no expone tipo_documento_vehiculo');

-- 39 y 40. El admin aprueba; la propuesta conserva su documento.
SET LOCAL request.jwt.claims = '{"sub":"00000000-0000-0000-0000-0000000000e5","role":"authenticated"}';
SELECT lives_ok(
  $$SELECT public.admin_moderar_propuesta(
      (SELECT id_propuesta FROM public.propuestas_publicacion
       WHERE id_publicacion = (SELECT id FROM pubs WHERE nombre = 't-vin') AND estado_propuesta = 'pendiente'),
      TRUE)$$,
  'el admin aprueba la propuesta con documento');
SELECT results_eq(
  $$SELECT estado_propuesta, tipo_documento_vehiculo, documento_vehiculo
    FROM public.propuestas_publicacion WHERE id_publicacion = (SELECT id FROM pubs WHERE nombre = 't-vin')$$,
  $$VALUES ('aprobada'::VARCHAR, 'baja'::VARCHAR,
            '00000000-0000-0000-0000-0000000000a1/11111111-1111-1111-1111-111111111111.jpg'::TEXT)$$,
  'la propuesta aprobada conserva su documento (solo vive en propuestas_publicacion)');

-- 41 y 42. Editar una publicación ya aprobada crea una propuesta nueva con el documento nuevo
-- (la copia completa no trae el documento: lo pone el UPDATE de editar_publicacion).
SET LOCAL request.jwt.claims = '{"sub":"00000000-0000-0000-0000-0000000000a1","role":"authenticated"}';
SELECT lives_ok(
  $$SELECT public.editar_publicacion((SELECT id FROM pubs WHERE nombre = 't-vin'),
      (SELECT j FROM base_datos) || '{"titulo":"t-vin-ed2","tipo_documento_vehiculo":"vin","documento_vehiculo":"1hgcm82633a004352"}',
      ARRAY['publicaciones/00000000-0000-0000-0000-0000000000a1/f1.jpg'])$$,
  'editar una publicación aprobada con documento funciona');
SELECT results_eq(
  $$SELECT estado_propuesta, tipo_documento_vehiculo, documento_vehiculo
    FROM public.propuestas_publicacion WHERE id_publicacion = (SELECT id FROM pubs WHERE nombre = 't-vin')
    ORDER BY id_propuesta$$,
  $$VALUES ('aprobada'::VARCHAR, 'baja'::VARCHAR,
            '00000000-0000-0000-0000-0000000000a1/11111111-1111-1111-1111-111111111111.jpg'::TEXT),
           ('pendiente'::VARCHAR, 'vin'::VARCHAR, '1HGCM82633A004352'::TEXT)$$,
  'la propuesta nueva tiene el documento nuevo y la aprobada no cambia');

-- ── CHECK chk_propuestas_publicacion_documento_vehiculo (como superusuario) ─
-- Se prueba con UPDATE sobre la propuesta de 't-tarjeta' (siempre se cambian tipo y valor juntos).
RESET ROLE;

-- 43 a 48. Combinaciones inválidas: la BD las rechaza aunque la app falle.
SELECT throws_ok(
  $$UPDATE public.propuestas_publicacion SET tipo_documento_vehiculo = 'vin', documento_vehiculo = NULL
    WHERE titulo = 't-tarjeta'$$,
  '23514', NULL, 'tipo sin valor se rechaza');
SELECT throws_ok(
  $$UPDATE public.propuestas_publicacion SET tipo_documento_vehiculo = NULL, documento_vehiculo = 'ABC1234'
    WHERE titulo = 't-tarjeta'$$,
  '23514', NULL, 'valor sin tipo se rechaza');
SELECT throws_ok(
  $$UPDATE public.propuestas_publicacion SET tipo_documento_vehiculo = 'licencia', documento_vehiculo = 'ABC1234'
    WHERE titulo = 't-tarjeta'$$,
  '23514', NULL, 'tipo desconocido se rechaza');
SELECT throws_ok(
  $$UPDATE public.propuestas_publicacion SET tipo_documento_vehiculo = 'vin', documento_vehiculo = '1hgcm82633a004352'
    WHERE titulo = 't-tarjeta'$$,
  '23514', NULL, 'VIN en minúsculas se rechaza (se guarda normalizado)');
SELECT throws_ok(
  $$UPDATE public.propuestas_publicacion SET tipo_documento_vehiculo = 'placa', documento_vehiculo = 'AB'
    WHERE titulo = 't-tarjeta'$$,
  '23514', NULL, 'placa demasiado corta se rechaza');
SELECT throws_ok(
  $$UPDATE public.propuestas_publicacion SET tipo_documento_vehiculo = 'baja', documento_vehiculo = 'foto.jpg'
    WHERE titulo = 't-tarjeta'$$,
  '23514', NULL, 'foto con ruta que no es <uuid>/<uuid>.jpg se rechaza');

SELECT * FROM finish();
ROLLBACK;
