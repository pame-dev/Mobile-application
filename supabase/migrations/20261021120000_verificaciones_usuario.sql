-- Verificación de usuarios con documento: una sola tabla para lotes y (más adelante) particulares.
--
-- Reemplaza las columnas de verificación de perfiles_lote (20261020120000/130000):
-- · verificaciones_usuario guarda cada documento enviado y su revisión.
-- · cuentas.verificado = TRUE muestra la insignia (hoy "Lote verificado").
-- · El usuario inserta su propia fila (estado 'pendiente'); el admin la aprueba o rechaza con
--   admin_resolver_verificacion_usuario(), que también marca cuentas.verificado.
-- · Bucket "constancias-fiscales" ahora es general: <id_cuenta>/<tipo_documento>_<timestamp>.<ext>.
-- Las columnas viejas de perfiles_lote se borran en 20261021130000 (aplicar cuando esto funcione).
--
-- Para agregar un documento nuevo (p. ej. INE de particulares): sumarlo a chk_verificaciones_usuario_tipo_documento.

-- ── Insignia en el perfil ──────────────────────────────────────────────────

-- Solo la cambia admin_resolver_verificacion_usuario (cuentas no da UPDATE de esta columna).
ALTER TABLE public.cuentas ADD COLUMN verificado BOOLEAN NOT NULL DEFAULT FALSE;
COMMENT ON COLUMN public.cuentas.verificado IS 'Cuenta verificada con un documento revisado por un admin (insignia).';

-- ── Tabla ──────────────────────────────────────────────────────────────────

CREATE TABLE public.verificaciones_usuario (
    id              UUID         PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id         UUID         NOT NULL,
    tipo_usuario    TEXT         NOT NULL,
    tipo_documento  TEXT         NOT NULL,
    archivo_path    TEXT         NOT NULL,
    estado          TEXT         NOT NULL DEFAULT 'pendiente',
    motivo_rechazo  TEXT,
    revisado_por    UUID,
    fecha_envio     TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    fecha_revision  TIMESTAMPTZ,

    CONSTRAINT fk_verificaciones_usuario_cuenta
        FOREIGN KEY (user_id) REFERENCES public.cuentas (id_cuenta) ON DELETE RESTRICT,
    CONSTRAINT fk_verificaciones_usuario_admin
        FOREIGN KEY (revisado_por) REFERENCES public.cuentas (id_cuenta) ON DELETE RESTRICT,
    CONSTRAINT chk_verificaciones_usuario_tipo_usuario CHECK (tipo_usuario IN ('lote', 'particular')),
    CONSTRAINT chk_verificaciones_usuario_tipo_documento CHECK (tipo_documento IN ('constancia_situacion_fiscal')),
    CONSTRAINT chk_verificaciones_usuario_estado CHECK (estado IN ('pendiente', 'verificado', 'rechazado')),
    CONSTRAINT chk_verificaciones_usuario_motivo_largo CHECK (CHAR_LENGTH(motivo_rechazo) <= 1000),
    CONSTRAINT chk_verificaciones_usuario_archivo_largo CHECK (CHAR_LENGTH(archivo_path) <= 300)
);

COMMENT ON TABLE public.verificaciones_usuario IS
    'Documentos de verificación enviados por los usuarios y su revisión por un administrador.';

-- Un solo envío vivo por usuario y documento: no se manda otro mientras está en revisión ni ya verificado.
CREATE UNIQUE INDEX ux_verificaciones_usuario_vigente
    ON public.verificaciones_usuario (user_id, tipo_documento) WHERE estado IN ('pendiente', 'verificado');
CREATE INDEX ix_verificaciones_usuario_usuario ON public.verificaciones_usuario (user_id, fecha_envio DESC);
CREATE INDEX ix_verificaciones_usuario_estado ON public.verificaciones_usuario (estado, fecha_envio DESC);

-- ── Permisos y RLS ─────────────────────────────────────────────────────────

ALTER TABLE public.verificaciones_usuario ENABLE ROW LEVEL SECURITY;

-- El usuario solo pone sus datos y el archivo; estado, motivo y revisión toman su default
-- y solo los cambia el admin con admin_resolver_verificacion_usuario().
REVOKE ALL ON public.verificaciones_usuario FROM anon, authenticated;
GRANT SELECT ON public.verificaciones_usuario TO authenticated;
GRANT INSERT (user_id, tipo_usuario, tipo_documento, archivo_path) ON public.verificaciones_usuario TO authenticated;

CREATE POLICY verificaciones_usuario_leer_propias ON public.verificaciones_usuario FOR SELECT TO authenticated
    USING (user_id = auth.uid());
CREATE POLICY verificaciones_usuario_leer_admin ON public.verificaciones_usuario FOR SELECT TO authenticated
    USING (public.es_admin_actual());
-- Solo su propia fila, del tipo de su cuenta y con un archivo ya subido a su carpeta.
CREATE POLICY verificaciones_usuario_enviar_propia ON public.verificaciones_usuario FOR INSERT TO authenticated
    WITH CHECK (user_id = auth.uid()
        AND tipo_usuario = (SELECT c.tipo_cuenta FROM public.cuentas c WHERE c.id_cuenta = auth.uid())
        AND LEFT(archivo_path, CHAR_LENGTH(auth.uid()::TEXT) + 1) = auth.uid()::TEXT || '/'
        AND EXISTS (SELECT 1 FROM storage.objects o
                    WHERE o.bucket_id = 'constancias-fiscales' AND o.name = archivo_path));

-- ── Datos existentes (constancias de lotes) ────────────────────────────────

INSERT INTO public.verificaciones_usuario
    (user_id, tipo_usuario, tipo_documento, archivo_path, estado, motivo_rechazo, revisado_por, fecha_envio, fecha_revision)
SELECT pl.id_cuenta, 'lote', 'constancia_situacion_fiscal', pl.constancia_url,
       pl.estado_verificacion, pl.motivo_rechazo_verificacion,
       CASE WHEN pl.estado_verificacion IN ('verificado', 'rechazado') THEN ult.id_admin END,
       COALESCE(o.created_at, NOW()),
       CASE WHEN pl.estado_verificacion IN ('verificado', 'rechazado') THEN COALESCE(pl.fecha_verificacion, ult.fecha_hora) END
FROM public.perfiles_lote pl
LEFT JOIN storage.objects o ON o.bucket_id = 'constancias-fiscales' AND o.name = pl.constancia_url
LEFT JOIN LATERAL (
    SELECT a.id_admin, a.fecha_hora FROM public.acciones_administrativas a
    WHERE a.id_cuenta_objetivo = pl.id_cuenta AND a.tipo_accion IN ('aprobar_verificacion', 'rechazar_verificacion')
    ORDER BY a.fecha_hora DESC LIMIT 1
) ult ON TRUE
WHERE pl.constancia_url IS NOT NULL AND pl.estado_verificacion IN ('pendiente', 'verificado', 'rechazado');

UPDATE public.cuentas c SET verificado = TRUE
FROM public.perfiles_lote pl
WHERE pl.id_cuenta = c.id_cuenta AND pl.estado_verificacion = 'verificado';

-- ── Bucket general: cualquier usuario sube en su carpeta ───────────────────

DROP POLICY constancias_subir_propia ON storage.objects;
CREATE POLICY constancias_subir_propia ON storage.objects FOR INSERT TO authenticated
    WITH CHECK (bucket_id = 'constancias-fiscales'
        AND (storage.foldername(name))[1] = auth.uid()::TEXT
        AND name ~ ('^' || auth.uid()::TEXT || '/[a-z_]+_[0-9]+\.(pdf|jpg|png)$'));
-- Lectura propia y del admin (constancias_leer_propia / constancias_leer_admin) se quedan igual.

-- ── Funciones: se quitan las de perfiles_lote ──────────────────────────────

DROP FUNCTION public.lote_enviar_constancia(TEXT);
DROP FUNCTION public.admin_listar_verificaciones();
DROP FUNCTION public.admin_resolver_verificacion(UUID, BOOLEAN, TEXT);

-- Documentos enviados por todos los usuarios (pendientes primero).
CREATE FUNCTION public.admin_listar_verificaciones_usuario()
RETURNS TABLE (
    id              UUID,
    user_id         UUID,
    nombre_mostrar  TEXT,
    correo          TEXT,
    tipo_usuario    TEXT,
    tipo_documento  TEXT,
    archivo_path    TEXT,
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
           v.archivo_path, v.estado, v.motivo_rechazo, adm.nombre_mostrar::TEXT, v.fecha_envio, v.fecha_revision
    FROM public.verificaciones_usuario v
    JOIN public.cuentas c ON c.id_cuenta = v.user_id
    JOIN auth.users u ON u.id = v.user_id
    LEFT JOIN public.cuentas adm ON adm.id_cuenta = v.revisado_por
    ORDER BY (v.estado = 'pendiente') DESC, v.fecha_envio DESC;
END;
$$;

REVOKE ALL ON FUNCTION public.admin_listar_verificaciones_usuario() FROM PUBLIC, anon;
GRANT EXECUTE ON FUNCTION public.admin_listar_verificaciones_usuario() TO authenticated;

-- Aprueba (verificado + insignia) o rechaza con motivo (mín. 10, máx. 1000) un documento pendiente.
CREATE FUNCTION public.admin_resolver_verificacion_usuario(p_id UUID, p_aprobar BOOLEAN, p_motivo TEXT DEFAULT NULL)
RETURNS VOID
LANGUAGE plpgsql
SECURITY DEFINER
SET search_path = ''
AS $$
DECLARE
    v_motivo  TEXT := NULLIF(TRIM(p_motivo), '');
    v_usuario UUID;
BEGIN
    PERFORM public.exigir_admin();

    IF NOT p_aprobar THEN
        v_motivo := public.exigir_motivo(p_motivo, 'rechaza');
        IF CHAR_LENGTH(v_motivo) > 1000 THEN
            RAISE EXCEPTION 'El motivo puede tener como máximo 1000 caracteres.';
        END IF;
    END IF;

    UPDATE public.verificaciones_usuario
    SET estado = CASE WHEN p_aprobar THEN 'verificado' ELSE 'rechazado' END,
        motivo_rechazo = CASE WHEN p_aprobar THEN NULL ELSE v_motivo END,
        revisado_por = auth.uid(), fecha_revision = NOW()
    WHERE id = p_id AND estado = 'pendiente'
    RETURNING user_id INTO v_usuario;

    IF v_usuario IS NULL THEN
        RAISE EXCEPTION 'Este documento no existe o ya fue revisado.';
    END IF;

    IF p_aprobar THEN
        UPDATE public.cuentas SET verificado = TRUE WHERE id_cuenta = v_usuario;
    END IF;

    INSERT INTO public.acciones_administrativas (id_admin, tipo_accion, motivo, id_cuenta_objetivo)
    VALUES (auth.uid(),
            CASE WHEN p_aprobar THEN 'aprobar_verificacion' ELSE 'rechazar_verificacion' END,
            COALESCE(v_motivo, 'Documento de verificación aprobado.'),
            v_usuario);
END;
$$;

REVOKE ALL ON FUNCTION public.admin_resolver_verificacion_usuario(UUID, BOOLEAN, TEXT) FROM PUBLIC, anon;
GRANT EXECUTE ON FUNCTION public.admin_resolver_verificacion_usuario(UUID, BOOLEAN, TEXT) TO authenticated;
