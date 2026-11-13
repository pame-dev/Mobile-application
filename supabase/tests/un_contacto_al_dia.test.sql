-- pgTAP: un_contacto_al_dia. Correr SOLO contra BD local: supabase test db
BEGIN;
SELECT plan(4);

INSERT INTO auth.users (id, email) VALUES
  ('00000000-0000-0000-0000-0000000000d1', 'dueno@test.local'),
  ('00000000-0000-0000-0000-0000000000a1', 'a@test.local'),
  ('00000000-0000-0000-0000-0000000000b2', 'b@test.local');
INSERT INTO public.cuentas (id_cuenta, tipo_cuenta, nombre_mostrar, estado_perfil_ubi, municipio_perfil_ubi) VALUES
  ('00000000-0000-0000-0000-0000000000d1', 'lote', 'Dueno', 'Jalisco', 'Guadalajara'),
  ('00000000-0000-0000-0000-0000000000a1', 'lote', 'Cuenta A', 'Jalisco', 'Guadalajara'),
  ('00000000-0000-0000-0000-0000000000b2', 'lote', 'Cuenta B', 'Jalisco', 'Zapopan');
INSERT INTO public.perfiles_lote (id_cuenta, calle, numero, colonia, codigo_postal) VALUES
  ('00000000-0000-0000-0000-0000000000d1', 'Calle', '1', 'Centro', '44100'),
  ('00000000-0000-0000-0000-0000000000a1', 'Calle', '2', 'Centro', '44100'),
  ('00000000-0000-0000-0000-0000000000b2', 'Calle', '3', 'Centro', '45100');

CREATE TEMP TABLE pub AS
  WITH i AS (
    INSERT INTO public.publicaciones (id_propietario, titulo)
    VALUES ('00000000-0000-0000-0000-0000000000d1', 'Auto de prueba')
    RETURNING id_publicacion
  ) SELECT id_publicacion FROM i;

-- (1) Primer 'contactar' de A se guarda.
INSERT INTO public.interacciones_publicacion (id_publicacion, id_cuenta, tipo_interaccion)
SELECT id_publicacion, '00000000-0000-0000-0000-0000000000a1', 'contactar' FROM pub;
SELECT is((SELECT count(*)::int FROM public.interacciones_publicacion
           WHERE tipo_interaccion = 'contactar'),
          1, 'el primer contactar de una cuenta se guarda');

-- (2) Segundo el mismo dia se descarta.
INSERT INTO public.interacciones_publicacion (id_publicacion, id_cuenta, tipo_interaccion)
SELECT id_publicacion, '00000000-0000-0000-0000-0000000000a1', 'contactar' FROM pub;
SELECT is((SELECT count(*)::int FROM public.interacciones_publicacion
           WHERE tipo_interaccion = 'contactar'),
          1, 'un segundo contactar el mismo dia no se guarda');

-- (3) Otra cuenta si se guarda.
INSERT INTO public.interacciones_publicacion (id_publicacion, id_cuenta, tipo_interaccion)
SELECT id_publicacion, '00000000-0000-0000-0000-0000000000b2', 'contactar' FROM pub;
SELECT is((SELECT count(*)::int FROM public.interacciones_publicacion
           WHERE tipo_interaccion = 'contactar'),
          2, 'otra cuenta si cuenta su propio contactar');

-- (4) ver_detalle repetido no se ve afectado.
INSERT INTO public.interacciones_publicacion (id_publicacion, id_cuenta, tipo_interaccion)
SELECT id_publicacion, '00000000-0000-0000-0000-0000000000a1', 'ver_detalle' FROM pub;
INSERT INTO public.interacciones_publicacion (id_publicacion, id_cuenta, tipo_interaccion)
SELECT id_publicacion, '00000000-0000-0000-0000-0000000000a1', 'ver_detalle' FROM pub;
SELECT is((SELECT count(*)::int FROM public.interacciones_publicacion
           WHERE tipo_interaccion = 'ver_detalle'
             AND id_cuenta = '00000000-0000-0000-0000-0000000000a1'),
          2, 'ver_detalle repetido no se descarta');

SELECT * FROM finish();
ROLLBACK;
