-- ############################################################################
-- Publicaciones ocultas del perfil, sincronizadas entre la app y la web
-- ############################################################################
-- El dueño puede ocultar de su perfil una publicación rechazada, deshabilitada o
-- pausada (y volver a mostrarla desde Mi panel). Antes se guardaba solo en el
-- teléfono o en el navegador, así que lo que se ocultaba en la web no se veía oculto
-- en la app. Ahora se guarda aquí. La publicación no cambia: solo cómo la ve su dueño.

CREATE TABLE public.publicaciones_ocultas (
    id_cuenta       UUID         NOT NULL,
    id_publicacion  BIGINT       NOT NULL,
    fecha           TIMESTAMPTZ  NOT NULL DEFAULT NOW(),

    CONSTRAINT pk_publicaciones_ocultas PRIMARY KEY (id_cuenta, id_publicacion),
    CONSTRAINT fk_publicaciones_ocultas_cuentas
        FOREIGN KEY (id_cuenta) REFERENCES public.cuentas (id_cuenta) ON DELETE CASCADE,
    CONSTRAINT fk_publicaciones_ocultas_publicaciones
        FOREIGN KEY (id_publicacion) REFERENCES public.publicaciones (id_publicacion) ON DELETE CASCADE
);

CREATE INDEX ix_publicaciones_ocultas_id_publicacion ON public.publicaciones_ocultas (id_publicacion);

COMMENT ON TABLE public.publicaciones_ocultas IS
    'Publicaciones propias que el dueño ocultó de su perfil (app y web leen la misma lista).';

-- Cada quien ve, oculta y vuelve a mostrar solo publicaciones suyas.
ALTER TABLE public.publicaciones_ocultas ENABLE ROW LEVEL SECURITY;
CREATE POLICY publicaciones_ocultas_leer_propias ON public.publicaciones_ocultas
    FOR SELECT TO authenticated USING (id_cuenta = auth.uid());
CREATE POLICY publicaciones_ocultas_crear_propias ON public.publicaciones_ocultas
    FOR INSERT TO authenticated
    WITH CHECK (id_cuenta = auth.uid() AND EXISTS (
        SELECT 1 FROM public.publicaciones p
        WHERE p.id_publicacion = publicaciones_ocultas.id_publicacion AND p.id_propietario = auth.uid()));
CREATE POLICY publicaciones_ocultas_borrar_propias ON public.publicaciones_ocultas
    FOR DELETE TO authenticated USING (id_cuenta = auth.uid());

REVOKE ALL ON public.publicaciones_ocultas FROM anon, authenticated;
GRANT SELECT, INSERT, DELETE ON public.publicaciones_ocultas TO authenticated;

-- Cambios en vivo: si se oculta en la app, la web abierta se actualiza sola.
DO $$
BEGIN
    IF EXISTS (SELECT 1 FROM pg_publication WHERE pubname = 'supabase_realtime')
       AND NOT EXISTS (SELECT 1 FROM pg_publication_tables
                       WHERE pubname = 'supabase_realtime' AND schemaname = 'public' AND tablename = 'publicaciones_ocultas') THEN
        ALTER PUBLICATION supabase_realtime ADD TABLE public.publicaciones_ocultas;
    END IF;
END;
$$;
