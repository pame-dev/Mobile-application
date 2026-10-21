-- Verificación de particulares con su CURP (opcional en el registro o desde su perfil).
--
-- Usa la misma tabla y revisión que la constancia de los lotes (20261021120000 / 20261022120000):
-- · tipo_documento 'curp': la CURP va en la columna nueva "curp" y no hay archivo (archivo_path NULL).
-- · El particular la envía con enviar_curp_verificacion(): crea la fila 'pendiente' o, si se la
--   rechazaron, la reemplaza. El admin la aprueba o rechaza con admin_resolver_verificacion_usuario()
--   (sin cambios): al aprobarla, cuentas.verificado = TRUE y sale la insignia "Verificado".
-- · Una CURP solo puede estar en revisión o verificada en una cuenta.
-- · admin_listar_verificaciones_usuario() ahora también devuelve la CURP.

-- ── Columna y reglas ───────────────────────────────────────────────────────

ALTER TABLE public.verificaciones_usuario ADD COLUMN curp TEXT;
COMMENT ON COLUMN public.verificaciones_usuario.curp IS 'CURP enviada por un particular (tipo_documento = curp).';

-- La CURP no tiene archivo.
ALTER TABLE public.verificaciones_usuario ALTER COLUMN archivo_path DROP NOT NULL;

ALTER TABLE public.verificaciones_usuario
    DROP CONSTRAINT chk_verificaciones_usuario_tipo_documento,
    ADD CONSTRAINT chk_verificaciones_usuario_tipo_documento
        CHECK (tipo_documento IN ('constancia_situacion_fiscal', 'curp')),
    -- CURP: 18 caracteres con el formato oficial y sin archivo. Los demás: archivo y sin CURP.
    ADD CONSTRAINT chk_verificaciones_usuario_curp CHECK (
        CASE WHEN tipo_documento = 'curp'
             THEN archivo_path IS NULL AND curp IS NOT NULL
                  AND curp ~ '^[A-Z][AEIOUX][A-Z]{2}[0-9]{2}(0[1-9]|1[0-2])(0[1-9]|[12][0-9]|3[01])[HMX](AS|BC|BS|CC|CL|CM|CS|CH|DF|DG|GT|GR|HG|JC|MC|MN|MS|NT|NL|OC|PL|QT|QR|SP|SL|SR|TC|TS|TL|VZ|YN|ZS|NE)[B-DF-HJ-NP-TV-Z]{3}[A-Z0-9][0-9]$'
             ELSE archivo_path IS NOT NULL AND curp IS NULL
        END);

-- Una CURP no puede estar en revisión o verificada en dos cuentas.
CREATE UNIQUE INDEX ux_verificaciones_usuario_curp
    ON public.verificaciones_usuario (curp) WHERE tipo_documento = 'curp' AND estado IN ('pendiente', 'verificado');

-- ── Enviar o reenviar la CURP ──────────────────────────────────────────────

-- Solo particulares. Si no tenía registro, lo crea 'pendiente'; si estaba 'rechazado', lo
-- reemplaza (CURP nueva, 'pendiente', fecha de hoy, sin revisión). Pendiente o verificado no se reenvían.
CREATE FUNCTION public.enviar_curp_verificacion(p_curp TEXT)
RETURNS VOID
LANGUAGE plpgsql
SECURITY DEFINER
SET search_path = ''
AS $$
DECLARE
    v_uid      UUID := auth.uid();
    v_tipo     TEXT;
    v_curp     TEXT := UPPER(REGEXP_REPLACE(COALESCE(p_curp, ''), '\s', '', 'g'));
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
    IF EXISTS (SELECT 1 FROM public.verificaciones_usuario v
               WHERE v.tipo_documento = 'curp' AND v.curp = v_curp AND v.user_id <> v_uid
                 AND v.estado IN ('pendiente', 'verificado')) THEN
        RAISE EXCEPTION 'Esta CURP ya está registrada en otra cuenta.';
    END IF;

    SELECT * INTO v_anterior FROM public.verificaciones_usuario
    WHERE user_id = v_uid AND tipo_documento = 'curp'
    FOR UPDATE;

    IF NOT FOUND THEN
        INSERT INTO public.verificaciones_usuario (user_id, tipo_usuario, tipo_documento, curp)
        VALUES (v_uid, v_tipo, 'curp', v_curp);
        RETURN;
    END IF;

    IF v_anterior.estado <> 'rechazado' THEN
        RAISE EXCEPTION 'Ya tienes tu CURP en revisión o tu cuenta ya está verificada.';
    END IF;

    UPDATE public.verificaciones_usuario
    SET curp = v_curp, estado = 'pendiente', fecha_envio = NOW(),
        motivo_rechazo = NULL, revisado_por = NULL, fecha_revision = NULL
    WHERE id = v_anterior.id;
END;
$$;

REVOKE ALL ON FUNCTION public.enviar_curp_verificacion(TEXT) FROM PUBLIC, anon;
GRANT EXECUTE ON FUNCTION public.enviar_curp_verificacion(TEXT) TO authenticated;

-- ── Lista del admin: ahora con la CURP ─────────────────────────────────────

DROP FUNCTION public.admin_listar_verificaciones_usuario();

CREATE FUNCTION public.admin_listar_verificaciones_usuario()
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
    PERFORM public.exigir_admin();

    RETURN QUERY
    SELECT v.id, v.user_id, c.nombre_mostrar::TEXT, u.email::TEXT, v.tipo_usuario, v.tipo_documento,
           v.archivo_path, v.curp, v.estado, v.motivo_rechazo, adm.nombre_mostrar::TEXT, v.fecha_envio, v.fecha_revision
    FROM public.verificaciones_usuario v
    JOIN public.cuentas c ON c.id_cuenta = v.user_id
    JOIN auth.users u ON u.id = v.user_id
    LEFT JOIN public.cuentas adm ON adm.id_cuenta = v.revisado_por
    ORDER BY (v.estado = 'pendiente') DESC, v.fecha_envio DESC;
END;
$$;

REVOKE ALL ON FUNCTION public.admin_listar_verificaciones_usuario() FROM PUBLIC, anon;
GRANT EXECUTE ON FUNCTION public.admin_listar_verificaciones_usuario() TO authenticated;
