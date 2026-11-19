-- CURP cifrada en la base de datos.
--
-- Hasta ahora la CURP de los particulares vivía en texto plano en verificaciones_usuario.curp y
-- cualquiera con permiso de lectura sobre la fila (el dueño, un moderador, un respaldo del esquema)
-- la veía tal cual. Desde esta migración:
-- · curp_cifrada: la CURP cifrada con pgcrypto (pgp_sym_encrypt) y una llave que vive en el Vault
--   de Supabase (secreto "curp_llave"). La llave se genera aquí mismo si no existe y nunca sale de
--   la base: ni la app ni la web la conocen.
-- · curp_hash: HMAC-SHA256 de la CURP con esa misma llave. Sirve solo para que una CURP no esté en
--   revisión o verificada en dos cuentas (índice único). Es HMAC y no SHA simple porque una CURP se
--   adivina a partir de nombre y fecha de nacimiento; sin la llave no se puede probar candidatos.
-- · La columna curp queda siempre NULL (la restricción lo exige) y se conserva un tiempo para que
--   las versiones viejas de la app, que todavía la leen, no fallen. Borrarla en una migración futura.
-- · Nadie lee curp_cifrada ni curp_hash por la API: el SELECT de authenticated es por columnas y
--   las excluye. La CURP en claro solo sale de funciones SECURITY DEFINER que validan permisos:
--     - mi_ultima_verificacion(): al dueño, su última verificación con su CURP descifrada.
--     - admin_listar_verificaciones_usuario(): al equipo de verificaciones (ya existía).
-- · Al aprobar una constancia u otro documento, admin_resolver_verificacion_usuario deja
--   archivo_path en NULL: el archivo ya no se necesita y así admin_documentos_verificacion_sin_uso()
--   lo devuelve para borrarlo del bucket (la app y la web ya llaman esa limpieza al resolver).
--   Borrar en el bucket sigue siendo solo de administradores (20261102130000): si aprueba un
--   moderador, el archivo queda hasta que un administrador resuelva o abra su panel.
--
-- Lo que esto protege: lecturas directas a la tabla, políticas RLS mal hechas, respaldos o volcados
-- del esquema public. Lo que no: a quien tenga acceso de administrador al proyecto (la llave está en
-- el mismo proyecto).
--
-- Respaldos: un pg_dump de public/auth guarda curp_cifrada pero NO la llave (vive en vault y está
-- cifrada con la llave raíz del proyecto). Si se restaura en otro proyecto, las CURP guardadas no se
-- pueden descifrar y los usuarios tendrían que reenviarla. Es intencional: una llave junto a los
-- datos anularía el cifrado.

-- ── Extensiones y llave ────────────────────────────────────────────────────

CREATE EXTENSION IF NOT EXISTS pgcrypto WITH SCHEMA extensions;
CREATE EXTENSION IF NOT EXISTS supabase_vault;

-- La llave se crea una sola vez, aleatoria, dentro de la base. Si ya existe (p. ej. se volvió a
-- correr la migración en local) se respeta: con otra llave las CURP guardadas quedarían ilegibles.
DO $$
BEGIN
    IF NOT EXISTS (SELECT 1 FROM vault.secrets s WHERE s.name = 'curp_llave') THEN
        PERFORM vault.create_secret(
            encode(extensions.gen_random_bytes(32), 'hex'),
            'curp_llave',
            'Llave con la que se cifra la CURP en verificaciones_usuario. No borrar: sin ella las CURP guardadas no se pueden leer.');
    END IF;
END;
$$;

-- Solo la usan las funciones de abajo (todas SECURITY DEFINER, dueño postgres). Ningún rol de la API
-- puede llamarla.
CREATE FUNCTION public.curp_llave()
RETURNS TEXT
LANGUAGE plpgsql
STABLE
SECURITY DEFINER
SET search_path = ''
AS $$
DECLARE
    v_llave TEXT;
BEGIN
    SELECT s.decrypted_secret INTO v_llave FROM vault.decrypted_secrets s WHERE s.name = 'curp_llave';
    IF v_llave IS NULL THEN
        RAISE EXCEPTION 'Falta el secreto curp_llave en el Vault de Supabase.';
    END IF;
    RETURN v_llave;
END;
$$;
REVOKE ALL ON FUNCTION public.curp_llave() FROM PUBLIC, anon, authenticated, service_role;

-- Cifra la CURP (ya normalizada). Cada llamada da bytes distintos (pgp usa sal), por eso los
-- duplicados se buscan con curp_hash y no comparando curp_cifrada.
CREATE FUNCTION public.curp_cifrar(p_curp TEXT)
RETURNS BYTEA
LANGUAGE plpgsql
SECURITY DEFINER
SET search_path = ''
AS $$
BEGIN
    IF p_curp IS NULL THEN
        RETURN NULL;
    END IF;
    RETURN extensions.pgp_sym_encrypt(p_curp, public.curp_llave());
END;
$$;
REVOKE ALL ON FUNCTION public.curp_cifrar(TEXT) FROM PUBLIC, anon, authenticated, service_role;

CREATE FUNCTION public.curp_descifrar(p_cifrada BYTEA)
RETURNS TEXT
LANGUAGE plpgsql
STABLE
SECURITY DEFINER
SET search_path = ''
AS $$
BEGIN
    IF p_cifrada IS NULL THEN
        RETURN NULL;
    END IF;
    RETURN extensions.pgp_sym_decrypt(p_cifrada, public.curp_llave());
END;
$$;
REVOKE ALL ON FUNCTION public.curp_descifrar(BYTEA) FROM PUBLIC, anon, authenticated, service_role;

-- HMAC-SHA256 en hexadecimal (64 caracteres). Determinista: la misma CURP siempre da el mismo hash.
CREATE FUNCTION public.curp_hash(p_curp TEXT)
RETURNS TEXT
LANGUAGE plpgsql
STABLE
SECURITY DEFINER
SET search_path = ''
AS $$
BEGIN
    IF p_curp IS NULL THEN
        RETURN NULL;
    END IF;
    RETURN encode(extensions.hmac(p_curp, public.curp_llave(), 'sha256'), 'hex');
END;
$$;
REVOKE ALL ON FUNCTION public.curp_hash(TEXT) FROM PUBLIC, anon, authenticated, service_role;

-- ── Columnas, datos existentes y reglas ────────────────────────────────────

ALTER TABLE public.verificaciones_usuario
    ADD COLUMN curp_cifrada BYTEA,
    ADD COLUMN curp_hash    TEXT;
COMMENT ON COLUMN public.verificaciones_usuario.curp_cifrada IS
    'CURP cifrada con pgcrypto y la llave curp_llave del Vault. Solo la descifran las funciones de la BD.';
COMMENT ON COLUMN public.verificaciones_usuario.curp_hash IS
    'HMAC-SHA256 (hex) de la CURP con la llave curp_llave. Solo para el índice único de CURP repetida.';
COMMENT ON COLUMN public.verificaciones_usuario.curp IS
    'Ya no se usa: siempre NULL. Se conserva para versiones viejas de la app; borrar en una migración futura.';

-- La restricción vieja exigía la CURP en claro; se quita antes de mover los datos.
ALTER TABLE public.verificaciones_usuario DROP CONSTRAINT chk_verificaciones_usuario_curp;

UPDATE public.verificaciones_usuario
SET curp_cifrada = public.curp_cifrar(curp),
    curp_hash    = public.curp_hash(curp),
    curp         = NULL
WHERE tipo_documento = 'curp' AND curp IS NOT NULL;

-- CURP: cifrada y con hash, sin archivo ni texto plano. Documento: archivo y nada de CURP; el
-- archivo puede faltar solo cuando ya está verificado (se borra al aprobar).
-- El formato oficial de la CURP lo valida enviar_curp_verificacion() antes de cifrar.
ALTER TABLE public.verificaciones_usuario
    ADD CONSTRAINT chk_verificaciones_usuario_curp CHECK (
        curp IS NULL
        AND CASE WHEN tipo_documento = 'curp'
                 THEN archivo_path IS NULL AND curp_cifrada IS NOT NULL
                      AND curp_hash IS NOT NULL AND CHAR_LENGTH(curp_hash) = 64
                 ELSE curp_cifrada IS NULL AND curp_hash IS NULL
                      AND (archivo_path IS NOT NULL OR estado = 'verificado')
            END);

-- Una CURP no puede estar en revisión o verificada en dos cuentas (ahora por hash).
DROP INDEX public.ux_verificaciones_usuario_curp;
CREATE UNIQUE INDEX ux_verificaciones_usuario_curp_hash
    ON public.verificaciones_usuario (curp_hash)
    WHERE tipo_documento = 'curp' AND estado IN ('pendiente', 'verificado');

-- ── Permisos: la API nunca ve el cifrado ni el hash ────────────────────────

-- SELECT por columnas. "curp" sigue en la lista (siempre NULL) para las apps viejas que la piden.
-- Las políticas RLS (leer_propias / leer_admin) siguen igual.
REVOKE SELECT ON public.verificaciones_usuario FROM authenticated;
GRANT SELECT (id, user_id, tipo_usuario, tipo_documento, archivo_path, curp, estado,
              motivo_rechazo, revisado_por, fecha_envio, fecha_revision)
    ON public.verificaciones_usuario TO authenticated;

-- ── Enviar o reenviar la CURP (ahora cifrada) ──────────────────────────────

CREATE OR REPLACE FUNCTION public.enviar_curp_verificacion(p_curp TEXT)
RETURNS VOID
LANGUAGE plpgsql
SECURITY DEFINER
SET search_path = ''
AS $$
DECLARE
    v_uid      UUID := auth.uid();
    v_tipo     TEXT;
    v_curp     TEXT := UPPER(REGEXP_REPLACE(COALESCE(p_curp, ''), '\s', '', 'g'));
    v_hash     TEXT;
    v_anterior public.verificaciones_usuario%ROWTYPE;
BEGIN
    IF v_uid IS NULL THEN
        RAISE EXCEPTION 'Inicia sesión para enviar tu CURP.';
    END IF;
    SELECT c.tipo_cuenta INTO v_tipo FROM public.cuentas c WHERE c.id_cuenta = v_uid;
    IF v_tipo IS NULL THEN
        RAISE EXCEPTION 'No encontramos tu cuenta.';
    END IF;
    IF v_tipo <> 'particular' THEN
        RAISE EXCEPTION 'La verificación con CURP es solo para particulares.';
    END IF;
    IF v_curp !~ '^[A-Z][AEIOUX][A-Z]{2}[0-9]{2}(0[1-9]|1[0-2])(0[1-9]|[12][0-9]|3[01])[HMX](AS|BC|BS|CC|CL|CM|CS|CH|DF|DG|GT|GR|HG|JC|MC|MN|MS|NT|NL|OC|PL|QT|QR|SP|SL|SR|TC|TS|TL|VZ|YN|ZS|NE)[B-DF-HJ-NP-TV-Z]{3}[A-Z0-9][0-9]$' THEN
        RAISE EXCEPTION 'La CURP no es válida. Revisa que tenga 18 caracteres, como aparece en tu documento.';
    END IF;

    v_hash := public.curp_hash(v_curp);

    IF EXISTS (SELECT 1 FROM public.verificaciones_usuario v
               WHERE v.tipo_documento = 'curp' AND v.curp_hash = v_hash AND v.user_id <> v_uid
                 AND v.estado IN ('pendiente', 'verificado')) THEN
        RAISE EXCEPTION 'Esta CURP ya está registrada en otra cuenta.';
    END IF;

    SELECT * INTO v_anterior FROM public.verificaciones_usuario
    WHERE user_id = v_uid AND tipo_documento = 'curp'
    FOR UPDATE;

    IF NOT FOUND THEN
        INSERT INTO public.verificaciones_usuario (user_id, tipo_usuario, tipo_documento, curp_cifrada, curp_hash)
        VALUES (v_uid, v_tipo, 'curp', public.curp_cifrar(v_curp), v_hash);
        RETURN;
    END IF;

    IF v_anterior.estado <> 'rechazado' THEN
        RAISE EXCEPTION 'Ya tienes tu CURP en revisión o tu cuenta ya está verificada.';
    END IF;

    UPDATE public.verificaciones_usuario
    SET curp_cifrada = public.curp_cifrar(v_curp), curp_hash = v_hash,
        estado = 'pendiente', fecha_envio = NOW(),
        motivo_rechazo = NULL, revisado_por = NULL, fecha_revision = NULL
    WHERE id = v_anterior.id;
EXCEPTION
    -- Dos cuentas enviaron la misma CURP al mismo tiempo: el índice único la detiene; mismo mensaje
    -- que la comprobación de arriba.
    WHEN unique_violation THEN
        RAISE EXCEPTION 'Esta CURP ya está registrada en otra cuenta.';
END;
$$;

-- ── El dueño lee su última verificación (con su CURP descifrada) ───────────

-- Reemplaza el SELECT directo que hacían la app y la web sobre verificaciones_usuario. Devuelve
-- cero filas si nunca envió nada. Solo descifra la CURP del usuario en sesión.
CREATE FUNCTION public.mi_ultima_verificacion()
RETURNS TABLE (
    estado          TEXT,
    motivo_rechazo  TEXT,
    fecha_envio     TIMESTAMPTZ,
    fecha_revision  TIMESTAMPTZ,
    tipo_documento  TEXT,
    archivo_path    TEXT,
    curp            TEXT
)
LANGUAGE plpgsql
STABLE
SECURITY DEFINER
SET search_path = ''
AS $$
BEGIN
    IF auth.uid() IS NULL THEN
        RETURN;
    END IF;

    RETURN QUERY
    SELECT v.estado, v.motivo_rechazo, v.fecha_envio, v.fecha_revision, v.tipo_documento,
           v.archivo_path, public.curp_descifrar(v.curp_cifrada)
    FROM public.verificaciones_usuario v
    WHERE v.user_id = auth.uid()
    ORDER BY v.fecha_envio DESC
    LIMIT 1;
END;
$$;

REVOKE ALL ON FUNCTION public.mi_ultima_verificacion() FROM PUBLIC, anon;
GRANT EXECUTE ON FUNCTION public.mi_ultima_verificacion() TO authenticated;

-- ── Lista del equipo de verificaciones: descifra la CURP ───────────────────

-- Misma firma y mismo permiso (moderador_verificaciones) que en 20261102120000.
CREATE OR REPLACE FUNCTION public.admin_listar_verificaciones_usuario()
RETURNS TABLE (
    id              UUID,
    user_id         UUID,
    nombre_mostrar  TEXT,
    correo          TEXT,
    tipo_usuario    TEXT,
    tipo_documento  TEXT,
    archivo_path    TEXT,
    curp            TEXT,
    estado          TEXT,
    motivo_rechazo  TEXT,
    revisado_por    TEXT,
    fecha_envio     TIMESTAMPTZ,
    fecha_revision  TIMESTAMPTZ
)
LANGUAGE plpgsql
STABLE
SECURITY DEFINER
SET search_path = ''
AS $$
BEGIN
    PERFORM public.exigir_moderador('moderador_verificaciones');

    RETURN QUERY
    SELECT v.id, v.user_id, c.nombre_mostrar::TEXT, u.email::TEXT, v.tipo_usuario, v.tipo_documento,
           v.archivo_path, public.curp_descifrar(v.curp_cifrada), v.estado, v.motivo_rechazo,
           adm.nombre_mostrar::TEXT, v.fecha_envio, v.fecha_revision
    FROM public.verificaciones_usuario v
    JOIN public.cuentas c ON c.id_cuenta = v.user_id
    JOIN auth.users u ON u.id = v.user_id
    LEFT JOIN public.cuentas adm ON adm.id_cuenta = v.revisado_por
    ORDER BY (v.estado = 'pendiente') DESC, v.fecha_envio DESC;
END;
$$;

-- ── Al aprobar un documento, el archivo ya no se necesita ──────────────────

-- Igual que en 20261102130000, más: si se aprueba un documento (no una CURP), archivo_path pasa a
-- NULL. Así admin_documentos_verificacion_sin_uso() lo lista y la app/web lo borran del bucket al
-- terminar de resolver (esa limpieza solo toma archivos con más de una hora y el DELETE en Storage
-- es solo de administradores; lo demás se borra en la siguiente limpieza de un administrador).
-- El perfil y los paneles ya ocultan "ver documento" sin ruta.
CREATE OR REPLACE FUNCTION public.admin_resolver_verificacion_usuario(p_id UUID, p_aprobar BOOLEAN, p_motivo TEXT DEFAULT NULL)
RETURNS VOID
LANGUAGE plpgsql
SECURITY DEFINER
SET search_path = ''
AS $$
DECLARE
    v_motivo  TEXT := NULLIF(TRIM(p_motivo), '');
    v_usuario UUID;
BEGIN
    PERFORM public.exigir_moderador('moderador_verificaciones');

    IF NOT public.es_admin_actual() AND EXISTS (
        SELECT 1 FROM public.verificaciones_usuario v WHERE v.id = p_id AND v.user_id = auth.uid()
    ) THEN
        RAISE EXCEPTION 'No puedes revisar tu propia verificación.';
    END IF;

    IF NOT p_aprobar THEN
        v_motivo := public.exigir_motivo(p_motivo, 'rechaza');
        IF CHAR_LENGTH(v_motivo) > 1000 THEN
            RAISE EXCEPTION 'El motivo puede tener como máximo 1000 caracteres.';
        END IF;
    END IF;

    UPDATE public.verificaciones_usuario
    SET estado = CASE WHEN p_aprobar THEN 'verificado' ELSE 'rechazado' END,
        motivo_rechazo = CASE WHEN p_aprobar THEN NULL ELSE v_motivo END,
        archivo_path = CASE WHEN p_aprobar AND tipo_documento <> 'curp' THEN NULL ELSE archivo_path END,
        revisado_por = auth.uid(), fecha_revision = NOW()
    WHERE id = p_id AND estado = 'pendiente'
    RETURNING user_id INTO v_usuario;

    IF v_usuario IS NULL THEN
        RAISE EXCEPTION 'Este documento no existe o ya fue revisado.';
    END IF;

    -- Aprobado: insignia. Rechazado: la pierde, salvo que tenga otro documento verificado
    -- (la fila rechazada ya cambió de estado arriba, así que no se cuenta a sí misma).
    UPDATE public.cuentas
    SET verificado = p_aprobar OR EXISTS (
            SELECT 1 FROM public.verificaciones_usuario v WHERE v.user_id = v_usuario AND v.estado = 'verificado')
    WHERE id_cuenta = v_usuario;

    INSERT INTO public.acciones_administrativas (id_admin, tipo_accion, motivo, id_cuenta_objetivo)
    VALUES (auth.uid(),
            CASE WHEN p_aprobar THEN 'aprobar_verificacion' ELSE 'rechazar_verificacion' END,
            COALESCE(v_motivo, 'Documento de verificación aprobado.'),
            v_usuario);
END;
$$;
