-- pgTAP: favorito_avisa_una_vez. Correr SOLO contra BD local: supabase test db
-- ESTADO: ESCRITO, NO EJECUTADO (sin supabase CLI/Docker en el entorno donde se creó).
BEGIN;
SELECT plan(8);

-- Fixtures (superusuario): dueño (D) y dos cuentas (A, B).
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
GRANT SELECT ON pub TO authenticated;

-- (1) Primer favorito de A -> 1 aviso al dueño.
INSERT INTO public.favoritos (id_cuenta, id_publicacion)
SELECT '00000000-0000-0000-0000-0000000000a1', id_publicacion FROM pub;
SELECT is((SELECT count(*)::int FROM public.notificaciones
           WHERE id_cuenta = '00000000-0000-0000-0000-0000000000d1' AND tipo = 'favorito'),
          1, 'primer favorito de una cuenta avisa una vez al dueño');

-- (2) Quitar y volver a poner -> sigue 1.
DELETE FROM public.favoritos WHERE id_cuenta = '00000000-0000-0000-0000-0000000000a1';
INSERT INTO public.favoritos (id_cuenta, id_publicacion)
SELECT '00000000-0000-0000-0000-0000000000a1', id_publicacion FROM pub;
SELECT is((SELECT count(*)::int FROM public.notificaciones
           WHERE id_cuenta = '00000000-0000-0000-0000-0000000000d1' AND tipo = 'favorito'),
          1, 'quitar y volver a poner el favorito no avisa de nuevo');

-- (3) Otra cuenta -> 2.
INSERT INTO public.favoritos (id_cuenta, id_publicacion)
SELECT '00000000-0000-0000-0000-0000000000b2', id_publicacion FROM pub;
SELECT is((SELECT count(*)::int FROM public.notificaciones
           WHERE id_cuenta = '00000000-0000-0000-0000-0000000000d1' AND tipo = 'favorito'),
          2, 'otra cuenta que guarda la misma publicación genera su propio aviso');

-- (4) El dueño guarda la suya -> sin aviso ni registro.
INSERT INTO public.favoritos (id_cuenta, id_publicacion)
SELECT '00000000-0000-0000-0000-0000000000d1', id_publicacion FROM pub;
SELECT is((SELECT count(*)::int FROM public.notificaciones
           WHERE id_cuenta = '00000000-0000-0000-0000-0000000000d1' AND tipo = 'favorito'),
          2, 'el dueño que guarda su propia publicación no genera aviso');
SELECT is((SELECT count(*)::int FROM public.favoritos_avisados
           WHERE id_cuenta = '00000000-0000-0000-0000-0000000000d1'),
          0, 'el dueño no queda en favoritos_avisados');

-- (5) anon / authenticated no leen favoritos_avisados.
SET LOCAL ROLE authenticated;
SET LOCAL request.jwt.claims = '{"sub":"00000000-0000-0000-0000-0000000000a1","role":"authenticated"}';
SELECT throws_ok($$SELECT * FROM public.favoritos_avisados$$, '42501', NULL,
                 'authenticated no puede leer favoritos_avisados');
RESET ROLE;
SET LOCAL ROLE anon;
SELECT throws_ok($$SELECT * FROM public.favoritos_avisados$$, '42501', NULL,
                 'anon no puede leer favoritos_avisados');
RESET ROLE;
SELECT ok(
  (SELECT relrowsecurity FROM pg_class WHERE oid = 'public.favoritos_avisados'::regclass),
  'favoritos_avisados tiene RLS activa');

SELECT * FROM finish();
ROLLBACK;
