-- Verificación de lotes con la Constancia de Situación Fiscal (CSF) del SAT.
--
-- · El lote sube (opcional) su constancia al bucket privado "constancias-fiscales"
--   en <id_cuenta>/constancia_<timestamp>.<ext> y la envía con lote_enviar_constancia().
-- · El admin la revisa (URL firmada) y la aprueba o rechaza con admin_resolver_verificacion().
-- · Con estado 'verificado' las apps muestran la insignia "Lote verificado".
--
-- Seguridad: el lote ya no puede actualizar cualquier columna de perfiles_lote (antes sí),
-- solo su dirección y horario; el estado de verificación solo cambia por las funciones.

-- ── Columnas en perfiles_lote ──────────────────────────────────────────────

ALTER TABLE public.perfiles_lote
    ADD COLUMN constancia_url               TEXT,
    ADD COLUMN estado_verificacion          TEXT NOT NULL DEFAULT 'sin_documento',
    ADD COLUMN motivo_rechazo_verificacion  TEXT,
    ADD COLUMN fecha_verificacion           TIMESTAMPTZ,
    ADD CONSTRAINT chk_perfiles_lote_estado_verificacion
        CHECK (estado_verificacion IN ('sin_documento', 'pendiente', 'verificado', 'rechazado')),
    ADD CONSTRAINT chk_perfiles_lote_motivo_verificacion_largo
        CHECK (CHAR_LENGTH(motivo_rechazo_verificacion) <= 1000);

COMMENT ON COLUMN public.perfiles_lote.constancia_url IS
    'Ruta de la constancia en el bucket privado constancias-fiscales (no es una URL pública).';
COMMENT ON COLUMN public.perfiles_lote.estado_verificacion IS
    'sin_documento, pendiente (en revisión), verificado o rechazado.';

-- El lote solo edita su dirección y horario; lo demás (verificación) lo cambian las funciones.
REVOKE UPDATE ON public.perfiles_lote FROM anon, authenticated;
GRANT UPDATE (calle, numero, colonia, codigo_postal, horarios_atencion) ON public.perfiles_lote TO authenticated;

-- ── Bucket privado ─────────────────────────────────────────────────────────

INSERT INTO storage.buckets (id, name, public, file_size_limit, allowed_mime_types)
VALUES ('constancias-fiscales', 'constancias-fiscales', FALSE, 5242880,
        ARRAY['application/pdf', 'image/jpeg', 'image/png'])
ON CONFLICT (id) DO NOTHING;

-- Cada lote sube y lee solo lo de su carpeta (<id_cuenta>/...); el admin lee todo.
-- No hay UPDATE ni DELETE: cada envío es un archivo nuevo.
CREATE POLICY constancias_subir_propia ON storage.objects FOR INSERT TO authenticated
    WITH CHECK (bucket_id = 'constancias-fiscales'
        AND (storage.foldername(name))[1] = auth.uid()::TEXT
        AND EXISTS (SELECT 1 FROM public.perfiles_lote pl WHERE pl.id_cuenta = auth.uid()));
CREATE POLICY constancias_leer_propia ON storage.objects FOR SELECT TO authenticated
    USING (bucket_id = 'constancias-fiscales' AND (storage.foldername(name))[1] = auth.uid()::TEXT);
CREATE POLICY constancias_leer_admin ON storage.objects FOR SELECT TO authenticated
    USING (bucket_id = 'constancias-fiscales' AND public.es_admin_actual());

-- ── Historial de acciones del admin: dos tipos nuevos ──────────────────────

ALTER TABLE public.acciones_administrativas
    DROP CONSTRAINT chk_acciones_administrativas_tipo_valido,
    ADD CONSTRAINT chk_acciones_administrativas_tipo_valido CHECK (tipo_accion IN (
        'aprobar_propuesta', 'rechazar_propuesta',
        'habilitar_publicacion', 'deshabilitar_publicacion',
        'suspender_cuenta', 'reactivar_cuenta',
        'resolver_reporte', 'aprobar_destacado', 'rechazar_destacado',
        'aprobar_verificacion', 'rechazar_verificacion'));

-- ── El lote envía su constancia ────────────────────────────────────────────

-- [p_ruta] es la ruta ya subida al bucket (<id_cuenta>/constancia_...). Solo se puede enviar
-- sin documento o después de un rechazo; queda 'pendiente' hasta que el admin la revise.
CREATE FUNCTION public.lote_enviar_constancia(p_ruta TEXT)
RETURNS VOID
LANGUAGE plpgsql
SECURITY DEFINER
SET search_path = ''
AS $$
DECLARE
    v_uid UUID := auth.uid();
BEGIN
    IF v_uid IS NULL THEN
        RAISE EXCEPTION 'Inicia sesión para enviar tu constancia.';
    END IF;
    IF p_ruta IS NULL OR LEFT(p_ruta, CHAR_LENGTH(v_uid::TEXT) + 12) <> v_uid::TEXT || '/constancia_'
       OR NOT EXISTS (SELECT 1 FROM storage.objects o WHERE o.bucket_id = 'constancias-fiscales' AND o.name = p_ruta) THEN
        RAISE EXCEPTION 'No encontramos el archivo de tu constancia. Vuelve a subirlo.';
    END IF;

    UPDATE public.perfiles_lote
    SET constancia_url = p_ruta, estado_verificacion = 'pendiente',
        motivo_rechazo_verificacion = NULL, fecha_verificacion = NULL
    WHERE id_cuenta = v_uid AND estado_verificacion IN ('sin_documento', 'rechazado');

    IF NOT FOUND THEN
        RAISE EXCEPTION 'Tu constancia ya está en revisión o tu lote ya está verificado.';
    END IF;
END;
$$;

REVOKE ALL ON FUNCTION public.lote_enviar_constancia(TEXT) FROM PUBLIC, anon;
GRANT EXECUTE ON FUNCTION public.lote_enviar_constancia(TEXT) TO authenticated;

-- ── Panel de administración ────────────────────────────────────────────────

-- Lotes que enviaron constancia (pendientes primero). fecha_envio = cuándo se subió el archivo.
CREATE FUNCTION public.admin_listar_verificaciones()
RETURNS TABLE (
    id_cuenta            UUID,
    nombre_mostrar       TEXT,
    correo               TEXT,
    responsable          TEXT,
    municipio            TEXT,
    estado               TEXT,
    constancia_url       TEXT,
    estado_verificacion  TEXT,
    motivo_rechazo       TEXT,
    fecha_envio          TIMESTAMPTZ,
    fecha_verificacion   TIMESTAMPTZ
)
LANGUAGE plpgsql
STABLE
SECURITY DEFINER
SET search_path = ''
AS $$
BEGIN
    PERFORM public.exigir_admin();

    RETURN QUERY
    SELECT
        c.id_cuenta,
        c.nombre_mostrar::TEXT,
        u.email::TEXT,
        COALESCE(u.raw_user_meta_data ->> 'responsable', c.nombre_mostrar)::TEXT,
        c.municipio_perfil_ubi::TEXT,
        c.estado_perfil_ubi::TEXT,
        pl.constancia_url,
        pl.estado_verificacion,
        pl.motivo_rechazo_verificacion,
        o.created_at,
        pl.fecha_verificacion
    FROM public.perfiles_lote pl
    JOIN public.cuentas c ON c.id_cuenta = pl.id_cuenta
    JOIN auth.users u ON u.id = pl.id_cuenta
    LEFT JOIN storage.objects o ON o.bucket_id = 'constancias-fiscales' AND o.name = pl.constancia_url
    WHERE pl.estado_verificacion <> 'sin_documento'
    ORDER BY (pl.estado_verificacion = 'pendiente') DESC, o.created_at DESC NULLS LAST;
END;
$$;

REVOKE ALL ON FUNCTION public.admin_listar_verificaciones() FROM PUBLIC, anon;
GRANT EXECUTE ON FUNCTION public.admin_listar_verificaciones() TO authenticated;

-- Aprueba o rechaza la constancia pendiente de un lote. Rechazar pide motivo (mín. 10 caracteres).
CREATE FUNCTION public.admin_resolver_verificacion(p_id_cuenta UUID, p_aprobar BOOLEAN, p_motivo TEXT DEFAULT NULL)
RETURNS VOID
LANGUAGE plpgsql
SECURITY DEFINER
SET search_path = ''
AS $$
DECLARE
    v_motivo TEXT := NULLIF(TRIM(p_motivo), '');
BEGIN
    PERFORM public.exigir_admin();

    IF p_aprobar THEN
        UPDATE public.perfiles_lote
        SET estado_verificacion = 'verificado', fecha_verificacion = NOW(), motivo_rechazo_verificacion = NULL
        WHERE id_cuenta = p_id_cuenta AND estado_verificacion = 'pendiente';
    ELSE
        v_motivo := public.exigir_motivo(p_motivo, 'rechaza');

        UPDATE public.perfiles_lote
        SET estado_verificacion = 'rechazado', motivo_rechazo_verificacion = v_motivo, fecha_verificacion = NULL
        WHERE id_cuenta = p_id_cuenta AND estado_verificacion = 'pendiente';
    END IF;

    IF NOT FOUND THEN
        RAISE EXCEPTION 'Esta constancia no existe o ya fue revisada.';
    END IF;

    INSERT INTO public.acciones_administrativas (id_admin, tipo_accion, motivo, id_cuenta_objetivo)
    VALUES (auth.uid(),
            CASE WHEN p_aprobar THEN 'aprobar_verificacion' ELSE 'rechazar_verificacion' END,
            COALESCE(v_motivo, 'Constancia fiscal verificada.'),
            p_id_cuenta);
END;
$$;

REVOKE ALL ON FUNCTION public.admin_resolver_verificacion(UUID, BOOLEAN, TEXT) FROM PUBLIC, anon;
GRANT EXECUTE ON FUNCTION public.admin_resolver_verificacion(UUID, BOOLEAN, TEXT) TO authenticated;
