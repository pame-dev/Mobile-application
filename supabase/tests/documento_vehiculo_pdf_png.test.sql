-- pgTAP: documento del vehículo en JPG, PNG o PDF (migración 20261029120000_documento_vehiculo_pdf_png.sql).
-- Correr SOLO contra BD local: supabase test db
-- ESTADO: ESCRITO, NO EJECUTADO (sin Docker en el entorno donde se creó). Revisar el primer resultado
-- real antes de fiarse. Las pruebas de INSERT en storage.objects dependen de los permisos de la
-- versión local de Storage; si alguna falla por permisos y no por la política, ajustar ahí.
-- Cubre: configuración del bucket, documento_vehiculo_valido (png/pdf aceptados; .webp, .jpeg y .PDF
-- en mayúsculas rechazados; mensaje nuevo; sin sesión rechaza), la política de subida, crear/editar
-- publicación con png y pdf, y el CHECK chk_propuestas_publicacion_documento_vehiculo.
-- El archivo documento_vehiculo.test.sql (migración 20261028) sigue cubriendo VIN, placa, RLS y el
-- resto; aquí solo lo que cambió con la migración 20261029.
BEGIN;
SELECT plan(43);

-- ── Fixtures (como superusuario) ────────────────────────────────────────────
-- A y B: particulares.
INSERT INTO auth.users (id, email) VALUES
  ('00000000-0000-0000-0000-0000000000a1', 'a@test.local'),
  ('00000000-0000-0000-0000-0000000000b2', 'b@test.local');
INSERT INTO public.cuentas (id_cuenta, tipo_cuenta, nombre_mostrar, estado_perfil_ubi, municipio_perfil_ubi) VALUES
  ('00000000-0000-0000-0000-0000000000a1', 'particular', 'Vendedor A', 'Jalisco', 'Guadalajara'),
  ('00000000-0000-0000-0000-0000000000b2', 'particular', 'Vendedor B', 'Jalisco', 'Zapopan');

-- Catálogos reales (no "Otros": crear_publicacion no guarda color_otro, etc.).
INSERT INTO public.marcas        (nombre_marca, es_otro)            VALUES ('MarcaTest', FALSE);
INSERT INTO public.colores       (nombre_color, es_otro)            VALUES ('ColorTest', FALSE);
INSERT INTO public.transmisiones (nombre_transmision, es_otro)      VALUES ('TransmisionTest', FALSE);
INSERT INTO public.carrocerias   (nombre_carroceria, es_otro)       VALUES ('CarroceriaTest', FALSE);
INSERT INTO public.modelos (id_marca, nombre_modelo, es_otro)
SELECT id_marca, 'ModeloTest', FALSE FROM public.marcas WHERE nombre_marca = 'MarcaTest';

-- Archivos ya subidos. De A: png, pdf y jpg válidos, más tres con extensión no permitida que SÍ
-- existen (para que el rechazo sea por la extensión y no por ausencia). De B: un pdf.
INSERT INTO storage.objects (bucket_id, name, owner) VALUES
  ('documentos-vehiculo', '00000000-0000-0000-0000-0000000000a1/aaaaaaaa-0000-0000-0000-00000000000a.png',  '00000000-0000-0000-0000-0000000000a1'),
  ('documentos-vehiculo', '00000000-0000-0000-0000-0000000000a1/aaaaaaaa-0000-0000-0000-00000000000b.pdf',  '00000000-0000-0000-0000-0000000000a1'),
  ('documentos-vehiculo', '00000000-0000-0000-0000-0000000000a1/aaaaaaaa-0000-0000-0000-00000000000c.jpg',  '00000000-0000-0000-0000-0000000000a1'),
  ('documentos-vehiculo', '00000000-0000-0000-0000-0000000000a1/aaaaaaaa-0000-0000-0000-00000000000d.webp', '00000000-0000-0000-0000-0000000000a1'),
  ('documentos-vehiculo', '00000000-0000-0000-0000-0000000000a1/aaaaaaaa-0000-0000-0000-00000000000e.jpeg', '00000000-0000-0000-0000-0000000000a1'),
  ('documentos-vehiculo', '00000000-0000-0000-0000-0000000000a1/aaaaaaaa-0000-0000-0000-00000000000f.PDF',  '00000000-0000-0000-0000-0000000000a1'),
  ('documentos-vehiculo', '00000000-0000-0000-0000-0000000000b2/bbbbbbbb-0000-0000-0000-00000000000b.pdf',  '00000000-0000-0000-0000-0000000000b2');

-- Datos base de una publicación; cada prueba le suma el documento y el título con ||.
CREATE TEMP TABLE base_datos AS
SELECT jsonb_build_object(
  'titulo', 'Base', 'descripcion', 'Auto de prueba', 'precio', 150000, 'moneda', 'MXN',
  'id_marca',  (SELECT id_marca FROM public.marcas WHERE nombre_marca = 'MarcaTest'),
  'id_modelo', (SELECT id_modelo FROM public.modelos WHERE nombre_modelo = 'ModeloTest'),
  'id_color',  (SELECT id_color FROM public.colores WHERE nombre_color = 'ColorTest'),
  'id_transmision', (SELECT id_transmision FROM public.transmisiones WHERE nombre_transmision = 'TransmisionTest'),
  'id_carroceria',  (SELECT id_carroceria FROM public.carrocerias WHERE nombre_carroceria = 'CarroceriaTest'),
  'anio', 2020, 'kilometraje', 30000
) AS j;
GRANT SELECT ON base_datos TO authenticated;

-- ── Bucket (como superusuario) ──────────────────────────────────────────────

-- 1 a 3. Tipos permitidos, tamaño máximo (5 MB) y bucket privado.
SELECT is(
  (SELECT allowed_mime_types FROM storage.buckets WHERE id = 'documentos-vehiculo'),
  ARRAY['application/pdf', 'image/jpeg', 'image/png'],
  'el bucket acepta solo pdf, jpeg y png');
SELECT is(
  (SELECT file_size_limit FROM storage.buckets WHERE id = 'documentos-vehiculo'),
  5242880::BIGINT, 'el bucket limita cada archivo a 5 MB');
SELECT is(
  (SELECT public FROM storage.buckets WHERE id = 'documentos-vehiculo'),
  FALSE, 'el bucket sigue siendo privado');

SET LOCAL ROLE authenticated;
SET LOCAL request.jwt.claims = '{"sub":"00000000-0000-0000-0000-0000000000a1","role":"authenticated"}';

-- ── documento_vehiculo_valido ───────────────────────────────────────────────

-- 4 a 6. Propios y existentes: png, pdf (también como baja) y jpg se aceptan tal cual.
SELECT is(
  public.documento_vehiculo_valido('tarjeta_circulacion',
    '00000000-0000-0000-0000-0000000000a1/aaaaaaaa-0000-0000-0000-00000000000a.png'),
  '00000000-0000-0000-0000-0000000000a1/aaaaaaaa-0000-0000-0000-00000000000a.png',
  'un png propio que existe se acepta');
SELECT is(
  public.documento_vehiculo_valido('baja',
    '00000000-0000-0000-0000-0000000000a1/aaaaaaaa-0000-0000-0000-00000000000b.pdf'),
  '00000000-0000-0000-0000-0000000000a1/aaaaaaaa-0000-0000-0000-00000000000b.pdf',
  'un pdf propio que existe se acepta');
SELECT is(
  public.documento_vehiculo_valido('tarjeta_circulacion',
    '00000000-0000-0000-0000-0000000000a1/aaaaaaaa-0000-0000-0000-00000000000c.jpg'),
  '00000000-0000-0000-0000-0000000000a1/aaaaaaaa-0000-0000-0000-00000000000c.jpg',
  'un jpg propio sigue aceptándose');

-- 7 a 9. Extensiones no permitidas (existen en el bucket, pero se rechazan).
SELECT throws_ok(
  $$SELECT public.documento_vehiculo_valido('baja',
      '00000000-0000-0000-0000-0000000000a1/aaaaaaaa-0000-0000-0000-00000000000d.webp')$$,
  'P0001', 'Agrega la foto o el PDF del documento del vehículo.', '.webp se rechaza');
SELECT throws_ok(
  $$SELECT public.documento_vehiculo_valido('baja',
      '00000000-0000-0000-0000-0000000000a1/aaaaaaaa-0000-0000-0000-00000000000e.jpeg')$$,
  'P0001', 'Agrega la foto o el PDF del documento del vehículo.', '.jpeg se rechaza (la extensión es .jpg)');
SELECT throws_ok(
  $$SELECT public.documento_vehiculo_valido('baja',
      '00000000-0000-0000-0000-0000000000a1/aaaaaaaa-0000-0000-0000-00000000000f.PDF')$$,
  'P0001', 'Agrega la foto o el PDF del documento del vehículo.', '.PDF en mayúsculas se rechaza');

-- 10 y 11. Un pdf propio que no existe en el bucket, y el pdf de otro usuario.
SELECT throws_ok(
  $$SELECT public.documento_vehiculo_valido('baja',
      '00000000-0000-0000-0000-0000000000a1/aaaaaaaa-0000-0000-0000-000000000099.pdf')$$,
  'P0001', 'Agrega la foto o el PDF del documento del vehículo.', 'un pdf que no existe en el bucket se rechaza');
SELECT throws_ok(
  $$SELECT public.documento_vehiculo_valido('baja',
      '00000000-0000-0000-0000-0000000000b2/bbbbbbbb-0000-0000-0000-00000000000b.pdf')$$,
  'P0001', 'Agrega la foto o el PDF del documento del vehículo.', 'el pdf de otro usuario se rechaza');

-- 12 y 13. Sin archivo: el mensaje nuevo (nulo y solo espacios).
SELECT throws_ok(
  $$SELECT public.documento_vehiculo_valido('tarjeta_circulacion', NULL)$$,
  'P0001', 'Agrega la foto o el PDF del documento del vehículo.', 'sin archivo usa el mensaje nuevo (nulo)');
SELECT throws_ok(
  $$SELECT public.documento_vehiculo_valido('baja', '   ')$$,
  'P0001', 'Agrega la foto o el PDF del documento del vehículo.', 'sin archivo usa el mensaje nuevo (solo espacios)');

-- 14 y 15. Sin sesión (auth.uid() NULL): rechaza aunque la ruta sea válida y el archivo exista.
-- Antes de esta migración '^' || NULL daba NULL y la validación no lanzaba el error.
SET LOCAL request.jwt.claims = '{"role":"authenticated"}';
SELECT throws_ok(
  $$SELECT public.documento_vehiculo_valido('baja',
      '00000000-0000-0000-0000-0000000000a1/aaaaaaaa-0000-0000-0000-00000000000b.pdf')$$,
  'P0001', 'Agrega la foto o el PDF del documento del vehículo.', 'sin sesión se rechaza una ruta válida que existe');
SELECT throws_ok(
  $$SELECT public.documento_vehiculo_valido('tarjeta_circulacion', NULL)$$,
  'P0001', 'Agrega la foto o el PDF del documento del vehículo.', 'sin sesión y sin archivo se rechaza');

-- ── Política de subida documentos_vehiculo_subir_propio ────────────────────

SET LOCAL request.jwt.claims = '{"sub":"00000000-0000-0000-0000-0000000000a1","role":"authenticated"}';

-- 16 a 18. A sube png, pdf y jpg a su carpeta.
SELECT lives_ok(
  $$INSERT INTO storage.objects (bucket_id, name, owner)
    VALUES ('documentos-vehiculo', '00000000-0000-0000-0000-0000000000a1/cccccccc-0000-0000-0000-00000000000a.png',
            '00000000-0000-0000-0000-0000000000a1')$$,
  'A puede subir un png a su carpeta');
SELECT lives_ok(
  $$INSERT INTO storage.objects (bucket_id, name, owner)
    VALUES ('documentos-vehiculo', '00000000-0000-0000-0000-0000000000a1/cccccccc-0000-0000-0000-00000000000b.pdf',
            '00000000-0000-0000-0000-0000000000a1')$$,
  'A puede subir un pdf a su carpeta');
SELECT lives_ok(
  $$INSERT INTO storage.objects (bucket_id, name, owner)
    VALUES ('documentos-vehiculo', '00000000-0000-0000-0000-0000000000a1/cccccccc-0000-0000-0000-00000000000c.jpg',
            '00000000-0000-0000-0000-0000000000a1')$$,
  'A puede subir un jpg a su carpeta');

-- 19 a 23. Extensiones o nombres no permitidos.
SELECT throws_ok(
  $$INSERT INTO storage.objects (bucket_id, name, owner)
    VALUES ('documentos-vehiculo', '00000000-0000-0000-0000-0000000000a1/dddddddd-0000-0000-0000-00000000000a.webp',
            '00000000-0000-0000-0000-0000000000a1')$$,
  '42501', NULL, 'la política rechaza .webp');
SELECT throws_ok(
  $$INSERT INTO storage.objects (bucket_id, name, owner)
    VALUES ('documentos-vehiculo', '00000000-0000-0000-0000-0000000000a1/dddddddd-0000-0000-0000-00000000000b.jpeg',
            '00000000-0000-0000-0000-0000000000a1')$$,
  '42501', NULL, 'la política rechaza .jpeg');
SELECT throws_ok(
  $$INSERT INTO storage.objects (bucket_id, name, owner)
    VALUES ('documentos-vehiculo', '00000000-0000-0000-0000-0000000000a1/dddddddd-0000-0000-0000-00000000000c.PDF',
            '00000000-0000-0000-0000-0000000000a1')$$,
  '42501', NULL, 'la política rechaza .PDF en mayúsculas');
SELECT throws_ok(
  $$INSERT INTO storage.objects (bucket_id, name, owner)
    VALUES ('documentos-vehiculo', '00000000-0000-0000-0000-0000000000a1/dddddddd-0000-0000-0000-00000000000d.gif',
            '00000000-0000-0000-0000-0000000000a1')$$,
  '42501', NULL, 'la política rechaza .gif');
SELECT throws_ok(
  $$INSERT INTO storage.objects (bucket_id, name, owner)
    VALUES ('documentos-vehiculo', '00000000-0000-0000-0000-0000000000a1/documento.pdf',
            '00000000-0000-0000-0000-0000000000a1')$$,
  '42501', NULL, 'la política rechaza un nombre que no es <uuid>.pdf');

-- 24. A no puede subir un pdf a la carpeta de B.
SELECT throws_ok(
  $$INSERT INTO storage.objects (bucket_id, name, owner)
    VALUES ('documentos-vehiculo', '00000000-0000-0000-0000-0000000000b2/eeeeeeee-0000-0000-0000-00000000000a.pdf',
            '00000000-0000-0000-0000-0000000000a1')$$,
  '42501', NULL, 'A no puede subir un pdf a la carpeta de B');

-- 25. Sin sesión no se sube nada.
SET LOCAL request.jwt.claims = '{"role":"authenticated"}';
SELECT throws_ok(
  $$INSERT INTO storage.objects (bucket_id, name, owner)
    VALUES ('documentos-vehiculo', '00000000-0000-0000-0000-0000000000a1/ffffffff-0000-0000-0000-00000000000a.pdf',
            '00000000-0000-0000-0000-0000000000a1')$$,
  '42501', NULL, 'sin sesión no se puede subir un pdf');

-- ── crear_publicacion y editar_publicacion ─────────────────────────────────

SET LOCAL request.jwt.claims = '{"sub":"00000000-0000-0000-0000-0000000000a1","role":"authenticated"}';

-- 26 y 27. Crear con png (tarjeta) y con pdf (baja).
SELECT lives_ok(
  $$SELECT public.crear_publicacion(
      (SELECT j FROM base_datos) || '{"titulo":"t-png","tipo_documento_vehiculo":"tarjeta_circulacion","documento_vehiculo":"00000000-0000-0000-0000-0000000000a1/aaaaaaaa-0000-0000-0000-00000000000a.png"}',
      ARRAY[]::TEXT[])$$,
  'crear con tarjeta en png funciona');
SELECT lives_ok(
  $$SELECT public.crear_publicacion(
      (SELECT j FROM base_datos) || '{"titulo":"t-pdf","tipo_documento_vehiculo":"baja","documento_vehiculo":"00000000-0000-0000-0000-0000000000a1/aaaaaaaa-0000-0000-0000-00000000000b.pdf"}',
      ARRAY[]::TEXT[])$$,
  'crear con baja en pdf funciona');

-- 28 y 29. Se guardó la ruta tal cual.
SELECT results_eq(
  $$SELECT tipo_documento_vehiculo, documento_vehiculo FROM public.propuestas_publicacion WHERE titulo = 't-png'$$,
  $$VALUES ('tarjeta_circulacion'::VARCHAR,
            '00000000-0000-0000-0000-0000000000a1/aaaaaaaa-0000-0000-0000-00000000000a.png'::TEXT)$$,
  'la propuesta guarda la ruta del png');
SELECT results_eq(
  $$SELECT tipo_documento_vehiculo, documento_vehiculo FROM public.propuestas_publicacion WHERE titulo = 't-pdf'$$,
  $$VALUES ('baja'::VARCHAR,
            '00000000-0000-0000-0000-0000000000a1/aaaaaaaa-0000-0000-0000-00000000000b.pdf'::TEXT)$$,
  'la propuesta guarda la ruta del pdf');

-- 30. Crear con .webp falla y no deja publicación huérfana (31).
SELECT throws_ok(
  $$SELECT public.crear_publicacion(
      (SELECT j FROM base_datos) || '{"titulo":"t-webp","tipo_documento_vehiculo":"baja","documento_vehiculo":"00000000-0000-0000-0000-0000000000a1/aaaaaaaa-0000-0000-0000-00000000000d.webp"}',
      ARRAY[]::TEXT[])$$,
  'P0001', 'Agrega la foto o el PDF del documento del vehículo.', 'crear con .webp falla');
SELECT is(
  (SELECT COUNT(*)::INT FROM public.propuestas_publicacion WHERE titulo = 't-webp'),
  0, 'crear con .webp no deja una propuesta a medias');

-- Ids de las publicaciones creadas, para usarlos al editar.
RESET ROLE;
CREATE TEMP TABLE pubs AS
SELECT p.titulo AS nombre, p.id_publicacion AS id FROM public.propuestas_publicacion p;
GRANT SELECT ON pubs TO authenticated;
SET LOCAL ROLE authenticated;
SET LOCAL request.jwt.claims = '{"sub":"00000000-0000-0000-0000-0000000000a1","role":"authenticated"}';

-- 32 y 33. Editar la publicación del png para cambiarla al pdf de baja.
SELECT lives_ok(
  $$SELECT public.editar_publicacion((SELECT id FROM pubs WHERE nombre = 't-png'),
      (SELECT j FROM base_datos) || '{"titulo":"t-png","tipo_documento_vehiculo":"baja","documento_vehiculo":"00000000-0000-0000-0000-0000000000a1/aaaaaaaa-0000-0000-0000-00000000000b.pdf"}',
      ARRAY['publicaciones/00000000-0000-0000-0000-0000000000a1/f1.jpg'])$$,
  'editar cambiando a pdf funciona');
SELECT results_eq(
  $$SELECT tipo_documento_vehiculo, documento_vehiculo
    FROM public.propuestas_publicacion WHERE id_publicacion = (SELECT id FROM pubs WHERE nombre = 't-png')$$,
  $$VALUES ('baja'::VARCHAR,
            '00000000-0000-0000-0000-0000000000a1/aaaaaaaa-0000-0000-0000-00000000000b.pdf'::TEXT)$$,
  'editar guarda la ruta del pdf');

-- 34. Editar con .PDF en mayúsculas falla.
SELECT throws_ok(
  $$SELECT public.editar_publicacion((SELECT id FROM pubs WHERE nombre = 't-png'),
      (SELECT j FROM base_datos) || '{"titulo":"t-png","tipo_documento_vehiculo":"baja","documento_vehiculo":"00000000-0000-0000-0000-0000000000a1/aaaaaaaa-0000-0000-0000-00000000000f.PDF"}',
      ARRAY['publicaciones/00000000-0000-0000-0000-0000000000a1/f1.jpg'])$$,
  'P0001', 'Agrega la foto o el PDF del documento del vehículo.', 'editar con .PDF en mayúsculas falla');

-- ── CHECK chk_propuestas_publicacion_documento_vehiculo (como superusuario) ─
-- Se prueba con UPDATE sobre la propuesta de 't-pdf' (siempre se cambian tipo y valor juntos).
RESET ROLE;

-- 35 a 37. Aceptadas: png, pdf y jpg (para tarjeta y baja).
SELECT lives_ok(
  $$UPDATE public.propuestas_publicacion
    SET tipo_documento_vehiculo = 'tarjeta_circulacion',
        documento_vehiculo = '00000000-0000-0000-0000-0000000000a1/aaaaaaaa-0000-0000-0000-00000000000a.png'
    WHERE titulo = 't-pdf'$$,
  'el CHECK acepta png');
SELECT lives_ok(
  $$UPDATE public.propuestas_publicacion
    SET tipo_documento_vehiculo = 'baja',
        documento_vehiculo = '00000000-0000-0000-0000-0000000000a1/aaaaaaaa-0000-0000-0000-00000000000b.pdf'
    WHERE titulo = 't-pdf'$$,
  'el CHECK acepta pdf');
SELECT lives_ok(
  $$UPDATE public.propuestas_publicacion
    SET tipo_documento_vehiculo = 'baja',
        documento_vehiculo = '00000000-0000-0000-0000-0000000000a1/aaaaaaaa-0000-0000-0000-00000000000c.jpg'
    WHERE titulo = 't-pdf'$$,
  'el CHECK sigue aceptando jpg');

-- 38 a 43. Rechazadas.
SELECT throws_ok(
  $$UPDATE public.propuestas_publicacion
    SET tipo_documento_vehiculo = 'baja',
        documento_vehiculo = '00000000-0000-0000-0000-0000000000a1/aaaaaaaa-0000-0000-0000-00000000000d.webp'
    WHERE titulo = 't-pdf'$$,
  '23514', NULL, 'el CHECK rechaza .webp');
SELECT throws_ok(
  $$UPDATE public.propuestas_publicacion
    SET tipo_documento_vehiculo = 'baja',
        documento_vehiculo = '00000000-0000-0000-0000-0000000000a1/aaaaaaaa-0000-0000-0000-00000000000e.jpeg'
    WHERE titulo = 't-pdf'$$,
  '23514', NULL, 'el CHECK rechaza .jpeg');
SELECT throws_ok(
  $$UPDATE public.propuestas_publicacion
    SET tipo_documento_vehiculo = 'baja',
        documento_vehiculo = '00000000-0000-0000-0000-0000000000a1/aaaaaaaa-0000-0000-0000-00000000000f.PDF'
    WHERE titulo = 't-pdf'$$,
  '23514', NULL, 'el CHECK rechaza .PDF en mayúsculas');
SELECT throws_ok(
  $$UPDATE public.propuestas_publicacion
    SET tipo_documento_vehiculo = 'tarjeta_circulacion',
        documento_vehiculo = '00000000-0000-0000-0000-0000000000a1/aaaaaaaa-0000-0000-0000-00000000000d.gif'
    WHERE titulo = 't-pdf'$$,
  '23514', NULL, 'el CHECK rechaza .gif');
SELECT throws_ok(
  $$UPDATE public.propuestas_publicacion
    SET tipo_documento_vehiculo = 'baja', documento_vehiculo = 'documento.pdf'
    WHERE titulo = 't-pdf'$$,
  '23514', NULL, 'el CHECK rechaza una ruta que no es <uuid>/<uuid>.pdf');
SELECT throws_ok(
  $$UPDATE public.propuestas_publicacion
    SET tipo_documento_vehiculo = 'vin',
        documento_vehiculo = '00000000-0000-0000-0000-0000000000a1/aaaaaaaa-0000-0000-0000-00000000000b.pdf'
    WHERE titulo = 't-pdf'$$,
  '23514', NULL, 'un pdf no sirve como valor de un VIN');

SELECT * FROM finish();
ROLLBACK;
