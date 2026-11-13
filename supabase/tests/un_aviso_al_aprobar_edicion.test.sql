-- pgTAP: un_aviso_al_aprobar_edicion. Correr SOLO contra BD local: supabase test db
-- ESTADO: ESCRITO, NO EJECUTADO (sin Docker en el entorno donde se creó).
-- Cubre: aprobar la edición de una publicación deshabilitada deja un solo aviso al dueño
-- (edicion_aprobada) y ninguno 'publicacion_rehabilitada'; rehabilitar a mano sí lo genera.
-- Se corre como superusuario fijando auth.uid() con request.jwt.claims (las funciones son
-- SECURITY DEFINER y leen auth.uid()).
BEGIN;
SELECT plan(8);

-- Fixtures: A (dueño, particular) y E (administrador; solo una cuenta lote puede serlo).
INSERT INTO auth.users (id, email) VALUES
  ('00000000-0000-0000-0000-0000000000a1', 'a@test.local'),
  ('00000000-0000-0000-0000-0000000000e5', 'admin@test.local');
INSERT INTO public.cuentas (id_cuenta, tipo_cuenta, nombre_mostrar, estado_perfil_ubi, municipio_perfil_ubi) VALUES
  ('00000000-0000-0000-0000-0000000000a1', 'particular', 'Vendedor A', 'Jalisco', 'Guadalajara'),
  ('00000000-0000-0000-0000-0000000000e5', 'lote',       'Admin',      'Jalisco', 'Guadalajara');
INSERT INTO public.perfiles_lote (id_cuenta, calle, numero, colonia, codigo_postal) VALUES
  ('00000000-0000-0000-0000-0000000000e5', 'Calle', '5', 'Centro', '44100');
INSERT INTO public.cuentas_roles (id_cuenta, id_rol)
SELECT '00000000-0000-0000-0000-0000000000e5', r.id_rol FROM public.roles r WHERE r.nombre_rol = 'administrador'
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

-- A publica y el admin aprueba la primera propuesta (la publicación queda habilitada).
SELECT set_config('request.jwt.claims', '{"sub":"00000000-0000-0000-0000-0000000000a1","role":"authenticated"}', TRUE);
SELECT public.crear_publicacion((SELECT j FROM base_datos), ARRAY[]::TEXT[]);
CREATE TEMP TABLE pub AS SELECT id_publicacion AS id FROM public.publicaciones
  WHERE id_propietario = '00000000-0000-0000-0000-0000000000a1';

SELECT set_config('request.jwt.claims', '{"sub":"00000000-0000-0000-0000-0000000000e5","role":"authenticated"}', TRUE);
SELECT public.admin_moderar_propuesta(
  (SELECT id_propuesta FROM public.propuestas_publicacion
   WHERE id_publicacion = (SELECT id FROM pub) AND estado_propuesta = 'pendiente'), TRUE);

-- El admin la deshabilita; el dueño la edita (propuesta nueva pendiente).
SELECT public.admin_cambiar_estado_publicacion(
  (SELECT id FROM pub), FALSE, 'Fotos borrosas, corrige y vuelve a enviar.');
SELECT set_config('request.jwt.claims', '{"sub":"00000000-0000-0000-0000-0000000000a1","role":"authenticated"}', TRUE);
SELECT public.editar_publicacion((SELECT id FROM pub),
  (SELECT j FROM base_datos) || '{"titulo":"Auto de prueba editado"}',
  ARRAY['publicaciones/00000000-0000-0000-0000-0000000000a1/f1.jpg']);

-- Marca de los avisos que ya existen: solo cuentan los que lleguen después.
CREATE TEMP TABLE previas AS SELECT id_notificacion FROM public.notificaciones;

-- (1) El admin aprueba la edición.
SELECT set_config('request.jwt.claims', '{"sub":"00000000-0000-0000-0000-0000000000e5","role":"authenticated"}', TRUE);
SELECT public.admin_moderar_propuesta(
  (SELECT id_propuesta FROM public.propuestas_publicacion
   WHERE id_publicacion = (SELECT id FROM pub) AND estado_propuesta = 'pendiente'), TRUE);

SELECT is((SELECT estado_administrativo FROM public.publicaciones WHERE id_publicacion = (SELECT id FROM pub)),
          'habilitada', 'aprobar la edición rehabilita la publicación');
SELECT is((SELECT count(*)::int FROM public.notificaciones
           WHERE id_cuenta = '00000000-0000-0000-0000-0000000000a1'
             AND id_notificacion NOT IN (SELECT id_notificacion FROM previas)),
          1, 'aprobar la edición deja una sola notificación nueva al dueño');
SELECT is((SELECT tipo FROM public.notificaciones
           WHERE id_cuenta = '00000000-0000-0000-0000-0000000000a1'
             AND id_notificacion NOT IN (SELECT id_notificacion FROM previas)),
          'edicion_aprobada', 'la única notificación nueva es la de edición aprobada');
SELECT is((SELECT count(*)::int FROM public.notificaciones
           WHERE id_cuenta = '00000000-0000-0000-0000-0000000000a1'
             AND tipo = 'publicacion_rehabilitada'
             AND id_notificacion NOT IN (SELECT id_notificacion FROM previas)),
          0, 'aprobar la edición no crea aviso publicacion_rehabilitada');

-- (3) Deshabilitar después de la aprobación sí avisa (la marca quedó limpia).
SELECT public.admin_cambiar_estado_publicacion(
  (SELECT id FROM pub), FALSE, 'Deshabilitada de nuevo para la prueba.');
SELECT is((SELECT count(*)::int FROM public.notificaciones
           WHERE id_cuenta = '00000000-0000-0000-0000-0000000000a1'
             AND tipo = 'publicacion_deshabilitada'
             AND id_notificacion NOT IN (SELECT id_notificacion FROM previas)),
          1, 'deshabilitar tras aprobar la edición genera publicacion_deshabilitada');
SELECT isnt(current_setting('karsy.rehabilitar_por_aprobacion', TRUE), 'on',
            'la marca de rehabilitación por aprobación no queda en on');

-- (2) Rehabilitar a mano (publicación ya deshabilitada) sí avisa.
SELECT public.admin_cambiar_estado_publicacion((SELECT id FROM pub), TRUE, NULL);
SELECT is((SELECT count(*)::int FROM public.notificaciones
           WHERE id_cuenta = '00000000-0000-0000-0000-0000000000a1'
             AND tipo = 'publicacion_rehabilitada'),
          1, 'rehabilitar a mano genera el aviso publicacion_rehabilitada');
SELECT is(coalesce(current_setting('karsy.rehabilitar_por_aprobacion', TRUE), ''),
          '', 'la marca de rehabilitación por aprobación queda apagada');

SELECT * FROM finish();
ROLLBACK;
