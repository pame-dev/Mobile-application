-- ############################################################################
-- Huellas visuales de las fotos: no aceptar fotos que ya están en otra publicación
-- ############################################################################
-- Al publicar, la app y la web calculan una huella visual de cada foto (dHash de 64 bits:
-- reconoce la misma foto aunque se haya vuelto a guardar, cambiado de tamaño o tapado las
-- placas). Aquí se guardan por publicación y se comparan al publicar: si una foto se parece
-- casi por completo (4 bits o menos de 64) a una de la publicación de OTRA cuenta, se avisa.
-- Solo cuentan publicaciones ya aprobadas y no deshabilitadas por el admin (una rechazada no
-- "aparta" fotos ajenas), y nunca las propias (un lote puede repetir fondo o auto).
-- Es una revisión de la app y la web (la huella la calcula el cliente), no una garantía: el
-- admin sigue revisando cada publicación.
--
-- Nadie lee la tabla directo: solo las funciones de abajo (SECURITY DEFINER), que no dicen
-- en qué publicación está la foto.


CREATE TABLE public.huellas_fotos (
    id_publicacion  BIGINT  NOT NULL,
    huella          BIGINT  NOT NULL,

    CONSTRAINT pk_huellas_fotos PRIMARY KEY (id_publicacion, huella),
    CONSTRAINT fk_huellas_fotos_publicaciones
        FOREIGN KEY (id_publicacion) REFERENCES public.publicaciones (id_publicacion) ON DELETE CASCADE
);

COMMENT ON TABLE public.huellas_fotos IS
    'Huella visual (dHash de 64 bits) de cada foto de una publicación, para no aceptar fotos de otras publicaciones.';

ALTER TABLE public.huellas_fotos ENABLE ROW LEVEL SECURITY;
REVOKE ALL ON public.huellas_fotos FROM anon, authenticated;


-- Huella en texto (16 caracteres hex, como la calculan la app y la web) → BIGINT.
-- Se manda como texto porque un número de 64 bits no cabe exacto en JSON.
CREATE FUNCTION public.huella_de_texto(p_huella TEXT)
RETURNS BIGINT
LANGUAGE sql
IMMUTABLE
SET search_path = ''
AS $$
    SELECT ('x' || p_huella)::BIT(64)::BIGINT;
$$;

REVOKE EXECUTE ON FUNCTION public.huella_de_texto(TEXT) FROM PUBLIC, anon, authenticated;


-- Guarda (reemplaza) las huellas de las fotos de una publicación propia, al publicar o editar.
CREATE FUNCTION public.registrar_huellas_fotos(p_id_publicacion BIGINT, p_huellas TEXT[])
RETURNS VOID
LANGUAGE plpgsql
SECURITY DEFINER
SET search_path = ''
AS $$
DECLARE
    v_dueno UUID;
BEGIN
    SELECT p.id_propietario INTO v_dueno FROM public.publicaciones p WHERE p.id_publicacion = p_id_publicacion;
    IF v_dueno IS NULL OR v_dueno IS DISTINCT FROM auth.uid() THEN
        RAISE EXCEPTION 'No puedes registrar fotos de esta publicación.';
    END IF;
    IF COALESCE(cardinality(p_huellas), 0) > 15
       OR EXISTS (SELECT 1 FROM unnest(p_huellas) AS h WHERE h IS NULL OR h !~ '^[0-9a-f]{16}$') THEN
        RAISE EXCEPTION 'Huellas de fotos no válidas.';
    END IF;

    DELETE FROM public.huellas_fotos WHERE id_publicacion = p_id_publicacion;
    INSERT INTO public.huellas_fotos (id_publicacion, huella)
    SELECT DISTINCT p_id_publicacion, public.huella_de_texto(h)
    FROM unnest(p_huellas) AS h;
END;
$$;

REVOKE EXECUTE ON FUNCTION public.registrar_huellas_fotos(BIGINT, TEXT[]) FROM PUBLIC, anon;
GRANT EXECUTE ON FUNCTION public.registrar_huellas_fotos(BIGINT, TEXT[]) TO authenticated;


-- Posiciones (desde 1) de las huellas que ya están en una publicación aprobada y habilitada de
-- OTRA cuenta. No dice cuál publicación ni de quién.
CREATE FUNCTION public.fotos_ya_usadas(p_huellas TEXT[])
RETURNS INT[]
LANGUAGE plpgsql
STABLE
SECURITY DEFINER
SET search_path = ''
AS $$
DECLARE
    v_cuenta UUID := auth.uid();
BEGIN
    IF v_cuenta IS NULL THEN
        RAISE EXCEPTION 'Inicia sesión para publicar.';
    END IF;
    IF COALESCE(cardinality(p_huellas), 0) > 15 THEN
        RAISE EXCEPTION 'Demasiadas fotos.';
    END IF;

    RETURN COALESCE((
        SELECT array_agg(t.i::INT ORDER BY t.i)
        FROM unnest(p_huellas) WITH ORDINALITY AS t(h, i)
        WHERE t.h ~ '^[0-9a-f]{16}$'
          AND EXISTS (
              SELECT 1
              FROM public.huellas_fotos f
              JOIN public.publicaciones p ON p.id_publicacion = f.id_publicacion
              WHERE p.id_propietario <> v_cuenta
                AND p.fecha_primera_publicacion IS NOT NULL
                AND p.estado_administrativo = 'habilitada'
                AND bit_count((f.huella # public.huella_de_texto(t.h))::BIT(64)) <= 4
          )
    ), ARRAY[]::INT[]);
END;
$$;

REVOKE EXECUTE ON FUNCTION public.fotos_ya_usadas(TEXT[]) FROM PUBLIC, anon;
GRANT EXECUTE ON FUNCTION public.fotos_ya_usadas(TEXT[]) TO authenticated;


-- Admin: publicaciones que todavía no tienen huellas (las que ya existían antes de esto),
-- con sus fotos vigentes, para registrarlas una vez desde el panel.
CREATE FUNCTION public.admin_publicaciones_sin_huellas()
RETURNS TABLE (id_publicacion BIGINT, fotos TEXT[])
LANGUAGE plpgsql
STABLE
SECURITY DEFINER
SET search_path = ''
AS $$
BEGIN
    PERFORM public.exigir_admin();
    RETURN QUERY
    SELECT f.id_publicacion, array_agg(f.ruta_storage ORDER BY f.orden_foto)
    FROM public.fotos_publicacion f
    WHERE f.activa
      AND NOT EXISTS (SELECT 1 FROM public.huellas_fotos h WHERE h.id_publicacion = f.id_publicacion)
    GROUP BY f.id_publicacion
    ORDER BY f.id_publicacion;
END;
$$;

REVOKE EXECUTE ON FUNCTION public.admin_publicaciones_sin_huellas() FROM PUBLIC, anon;
GRANT EXECUTE ON FUNCTION public.admin_publicaciones_sin_huellas() TO authenticated;


-- Admin: guarda las huellas de una publicación ya existente.
CREATE FUNCTION public.admin_registrar_huellas(p_id_publicacion BIGINT, p_huellas TEXT[])
RETURNS VOID
LANGUAGE plpgsql
SECURITY DEFINER
SET search_path = ''
AS $$
BEGIN
    PERFORM public.exigir_admin();
    IF NOT EXISTS (SELECT 1 FROM public.publicaciones p WHERE p.id_publicacion = p_id_publicacion) THEN
        RAISE EXCEPTION 'La publicación no existe.';
    END IF;
    IF COALESCE(cardinality(p_huellas), 0) > 15
       OR EXISTS (SELECT 1 FROM unnest(p_huellas) AS h WHERE h IS NULL OR h !~ '^[0-9a-f]{16}$') THEN
        RAISE EXCEPTION 'Huellas de fotos no válidas.';
    END IF;

    DELETE FROM public.huellas_fotos WHERE id_publicacion = p_id_publicacion;
    INSERT INTO public.huellas_fotos (id_publicacion, huella)
    SELECT DISTINCT p_id_publicacion, public.huella_de_texto(h)
    FROM unnest(p_huellas) AS h;
END;
$$;

REVOKE EXECUTE ON FUNCTION public.admin_registrar_huellas(BIGINT, TEXT[]) FROM PUBLIC, anon;
GRANT EXECUTE ON FUNCTION public.admin_registrar_huellas(BIGINT, TEXT[]) TO authenticated;
