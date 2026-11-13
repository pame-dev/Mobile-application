-- ############################################################################
-- Aviso de favorito: una sola vez por persona y publicación
-- ############################################################################
-- Antes, cada vez que alguien guardaba una publicación en favoritos el dueño recibía un
-- aviso; quitar y volver a poner el corazón varias veces le mandaba un aviso por cada vez.
-- Ahora se anota quién ya generó el aviso de cada publicación y solo se avisa la primera
-- vez. El dueño sigue sin saber quién fue (la tabla no la lee nadie más que este trigger).


-- ── Quién ya avisó de qué publicación ──────────────────────────────────────

CREATE TABLE public.favoritos_avisados (
    id_cuenta       UUID    NOT NULL,
    id_publicacion  BIGINT  NOT NULL,

    CONSTRAINT pk_favoritos_avisados PRIMARY KEY (id_cuenta, id_publicacion),
    CONSTRAINT fk_favoritos_avisados_cuentas
        FOREIGN KEY (id_cuenta) REFERENCES public.cuentas (id_cuenta) ON DELETE CASCADE,
    CONSTRAINT fk_favoritos_avisados_publicaciones
        FOREIGN KEY (id_publicacion) REFERENCES public.publicaciones (id_publicacion) ON DELETE CASCADE
);

COMMENT ON TABLE public.favoritos_avisados IS
    'Cuenta que ya generó el aviso de "favorito" de una publicación: no se vuelve a avisar al dueño.';

-- Solo la escribe el trigger (SECURITY DEFINER); la app y la web no la ven.
ALTER TABLE public.favoritos_avisados ENABLE ROW LEVEL SECURITY;
REVOKE ALL ON public.favoritos_avisados FROM anon, authenticated;

-- Los favoritos que ya existen ya avisaron al guardarse.
INSERT INTO public.favoritos_avisados (id_cuenta, id_publicacion)
SELECT f.id_cuenta, f.id_publicacion
FROM public.favoritos f
JOIN public.publicaciones p ON p.id_publicacion = f.id_publicacion
WHERE p.id_propietario <> f.id_cuenta
ON CONFLICT DO NOTHING;


-- ── Trigger: avisa solo la primera vez ─────────────────────────────────────
-- Cuerpo de 20261004120000_notificaciones_usuario.sql más el registro en favoritos_avisados.

CREATE OR REPLACE FUNCTION public.fn_notificar_favorito()
RETURNS TRIGGER
LANGUAGE plpgsql
SECURITY DEFINER
SET search_path = ''
AS $$
DECLARE
    v_pub public.publicaciones%ROWTYPE;
BEGIN
    SELECT * INTO v_pub FROM public.publicaciones WHERE id_publicacion = NEW.id_publicacion;
    IF FOUND AND v_pub.id_propietario <> NEW.id_cuenta THEN
        INSERT INTO public.favoritos_avisados (id_cuenta, id_publicacion)
        VALUES (NEW.id_cuenta, NEW.id_publicacion)
        ON CONFLICT DO NOTHING;
        -- FOUND: se insertó (primera vez que esta cuenta guarda esta publicación).
        IF FOUND THEN
            INSERT INTO public.notificaciones (id_cuenta, tipo, id_publicacion, titulo_vehiculo)
            VALUES (v_pub.id_propietario, 'favorito', v_pub.id_publicacion, v_pub.titulo);
        END IF;
    END IF;
    RETURN NULL;
END;
$$;
