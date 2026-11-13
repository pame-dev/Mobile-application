-- pgTAP: favorito_ve_auto_pausado. Correr SOLO contra BD local: supabase test db
-- ESTADO: ESCRITO, NO EJECUTADO (sin Docker en el entorno donde se creó).
BEGIN;
SELECT plan(5);

-- Fixtures (superusuario): dueño (D), cuenta con favorito (A) y cuenta sin favorito (B).
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

-- Dos autos ya publicados alguna vez y luego pausados por el dueño; A guarda ambos.
CREATE TEMP TABLE pub AS
  WITH i AS (
    INSERT INTO public.publicaciones (id_propietario, titulo, fecha_primera_publicacion,
                                      estado_publicacion, estado_administrativo)
    VALUES ('00000000-0000-0000-0000-0000000000d1', 'Auto pausado', now(), 'deshabilitada', 'habilitada'),
           ('00000000-0000-0000-0000-0000000000d1', 'Auto del equipo', now(), 'deshabilitada', 'habilitada')
    RETURNING id_publicacion, titulo
  ) SELECT id_publicacion, titulo FROM i;
GRANT SELECT ON pub TO authenticated, anon;

INSERT INTO public.favoritos (id_cuenta, id_publicacion)
SELECT '00000000-0000-0000-0000-0000000000a1', id_publicacion FROM pub;

-- El segundo lo deshabilita el equipo (queda con motivo).
UPDATE public.publicaciones
   SET estado_administrativo = 'deshabilitada_administrador',
       -- chk_publicaciones_deshabilitacion exige motivo y fecha al deshabilitar.
       motivo_deshabilitacion = 'Motivo de prueba del equipo',
       fecha_deshabilitacion = NOW()
 WHERE id_publicacion = (SELECT id_publicacion FROM pub WHERE titulo = 'Auto del equipo');

-- (1) A (con favorito) lee el auto pausado.
SET LOCAL ROLE authenticated;
SELECT set_config('request.jwt.claims',
  '{"sub":"00000000-0000-0000-0000-0000000000a1","role":"authenticated"}', true);
SELECT is((SELECT count(*)::int FROM public.publicaciones
           WHERE id_publicacion = (SELECT id_publicacion FROM pub WHERE titulo = 'Auto pausado')),
          1, 'quien lo tiene en favoritos puede leer el auto pausado por su dueño');

-- (3) A con favorito NO lee el deshabilitado por el equipo.
SELECT is((SELECT count(*)::int FROM public.publicaciones
           WHERE id_publicacion = (SELECT id_publicacion FROM pub WHERE titulo = 'Auto del equipo')),
          0, 'con favorito no se puede leer un auto deshabilitado por el equipo');

-- (5) v_anuncios con visible=true no incluye el pausado, aunque A lo tenga en favoritos.
SELECT is((SELECT count(*)::int FROM public.v_anuncios
           WHERE visible = true
             AND id_publicacion = (SELECT id_publicacion FROM pub WHERE titulo = 'Auto pausado')),
          0, 'v_anuncios con visible=true no incluye el pausado para quien lo tiene en favoritos');
RESET ROLE;

-- (2) B (sin favorito) no lee el pausado.
SET LOCAL ROLE authenticated;
SELECT set_config('request.jwt.claims',
  '{"sub":"00000000-0000-0000-0000-0000000000b2","role":"authenticated"}', true);
SELECT is((SELECT count(*)::int FROM public.publicaciones
           WHERE id_publicacion = (SELECT id_publicacion FROM pub WHERE titulo = 'Auto pausado')),
          0, 'otra cuenta sin favorito no puede leer el auto pausado');
RESET ROLE;

-- (4) anon no lee el pausado.
SET LOCAL ROLE anon;
SELECT set_config('request.jwt.claims', '{"role":"anon"}', true);
SELECT is((SELECT count(*)::int FROM public.publicaciones
           WHERE id_publicacion = (SELECT id_publicacion FROM pub WHERE titulo = 'Auto pausado')),
          0, 'anon no puede leer el auto pausado');
RESET ROLE;

SELECT * FROM finish();
ROLLBACK;
