-- ============================================================================
-- KARSY — Migración: cuentas del equipo
-- ============================================================================
--   01  Pame   pame@karsy.com   administrador (tipo lote + rol administrador)
--   02  Nancy  nancy@karsy.com  lote
--   03  Jahir  jahir@karsy.com  particular
--
-- Las tres usan la contraseña 6767. Se insertan directo en auth.users con
-- bcrypt (igual que lo hace Supabase Auth), así que el hash no es reversible.
-- El administrador debe ser tipo lote por la regla de
-- trg_cuentas_roles_validar_admin.
-- ============================================================================

CREATE TEMP TABLE t_equipo (
    id              UUID PRIMARY KEY,
    correo          TEXT NOT NULL,
    nombre          TEXT NOT NULL,
    tipo_cuenta     TEXT NOT NULL,
    descripcion     TEXT,
    es_admin        BOOLEAN NOT NULL
);

INSERT INTO t_equipo VALUES
    ('00000000-0000-0000-0000-000000000001', 'pame@karsy.com',  'Pame',  'lote',       'Administración de Karsy.', TRUE),
    ('00000000-0000-0000-0000-000000000002', 'nancy@karsy.com', 'Nancy', 'lote',       'Lote de autos seminuevos.', FALSE),
    ('00000000-0000-0000-0000-000000000003', 'jahir@karsy.com', 'Jahir', 'particular', NULL, FALSE);

INSERT INTO auth.users (
    instance_id, id, aud, role, email, encrypted_password, email_confirmed_at,
    confirmation_token, recovery_token, email_change_token_new, email_change,
    email_change_token_current, phone_change, phone_change_token, reauthentication_token,
    raw_app_meta_data, raw_user_meta_data, created_at, updated_at
)
SELECT
    '00000000-0000-0000-0000-000000000000', e.id, 'authenticated', 'authenticated',
    e.correo, extensions.crypt('6767', extensions.gen_salt('bf')), NOW(),
    '', '', '', '', '', '', '', '',
    '{"provider": "email", "providers": ["email"]}'::JSONB,
    jsonb_build_object('nombre_mostrar', e.nombre),
    NOW(), NOW()
FROM t_equipo e;

INSERT INTO auth.identities (
    id, provider_id, user_id, identity_data, provider, last_sign_in_at, created_at, updated_at
)
SELECT
    gen_random_uuid(), e.id::TEXT, e.id,
    jsonb_build_object('sub', e.id::TEXT, 'email', e.correo,
                       'email_verified', TRUE, 'phone_verified', FALSE),
    'email', NOW(), NOW(), NOW()
FROM t_equipo e;

INSERT INTO public.cuentas (
    id_cuenta, tipo_cuenta, nombre_mostrar, descripcion_corta,
    estado_perfil_ubi, municipio_perfil_ubi
)
SELECT e.id, e.tipo_cuenta, e.nombre, e.descripcion, 'Coahuila', 'Torreón'
FROM t_equipo e;

INSERT INTO public.perfiles_lote (
    id_cuenta, nombre_comercial, descripcion_lote, calle, numero, colonia,
    codigo_postal, municipio, estado, horarios_atencion
) VALUES
    ('00000000-0000-0000-0000-000000000001', 'Karsy', 'Cuenta administrativa de la plataforma.',
     'Blvd. Revolución', '1500', 'Centro', '27000', 'Torreón', 'Coahuila', NULL),
    ('00000000-0000-0000-0000-000000000002', 'Nancy', 'Lote de autos seminuevos.',
     'Av. Juárez', '100', 'Centro', '27000', 'Torreón', 'Coahuila', 'Lun–Sáb 9:00–18:00');

INSERT INTO public.cuentas_roles (id_cuenta, id_rol)
SELECT e.id, r.id_rol
FROM t_equipo e
JOIN public.roles r ON r.nombre_rol = 'administrador'
WHERE e.es_admin;

DROP TABLE t_equipo;
