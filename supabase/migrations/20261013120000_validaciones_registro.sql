-- ############################################################################
-- Validaciones del registro también en la BD
-- ############################################################################
-- La app ya valida estos campos, pero alguien podría saltársela usando la API
-- con la clave pública. Estas reglas se revisan solo cuando se escribe o cambia
-- el campo (triggers BEFORE INSERT OR UPDATE OF …): los datos que ya existían no
-- estorban al actualizar otras columnas, pero nada nuevo entra mal.
--
-- También: telefono_en_uso(), para avisar en el registro si el número ya está
-- en otra cuenta (la app solo avisa; no bloquea).


-- ----------------------------------------------------------------------------
-- Teléfonos: exactamente 10 dígitos
-- ----------------------------------------------------------------------------
CREATE FUNCTION public.fn_validar_telefono()
RETURNS TRIGGER
LANGUAGE plpgsql
SET search_path = ''
AS $$
BEGIN
    IF NEW.numero_telefono !~ '^[0-9]{10}$' THEN
        RAISE EXCEPTION 'El teléfono debe tener 10 dígitos.';
    END IF;
    RETURN NEW;
END;
$$;

CREATE TRIGGER trg_telefonos_validar
    BEFORE INSERT OR UPDATE OF numero_telefono ON public.telefonos_contacto
    FOR EACH ROW EXECUTE FUNCTION public.fn_validar_telefono();


-- ----------------------------------------------------------------------------
-- Lote: código postal de 5 dígitos
-- ----------------------------------------------------------------------------
CREATE FUNCTION public.fn_validar_codigo_postal()
RETURNS TRIGGER
LANGUAGE plpgsql
SET search_path = ''
AS $$
BEGIN
    IF NEW.codigo_postal !~ '^[0-9]{5}$' THEN
        RAISE EXCEPTION 'El código postal debe tener 5 dígitos.';
    END IF;
    RETURN NEW;
END;
$$;

CREATE TRIGGER trg_perfiles_lote_validar_cp
    BEFORE INSERT OR UPDATE OF codigo_postal ON public.perfiles_lote
    FOR EACH ROW EXECUTE FUNCTION public.fn_validar_codigo_postal();


-- ----------------------------------------------------------------------------
-- Nombre de la cuenta: al menos 2 caracteres (sin contar espacios)
-- ----------------------------------------------------------------------------
CREATE FUNCTION public.fn_validar_nombre_cuenta()
RETURNS TRIGGER
LANGUAGE plpgsql
SET search_path = ''
AS $$
BEGIN
    IF CHAR_LENGTH(REPLACE(NEW.nombre_mostrar, ' ', '')) < 2 THEN
        RAISE EXCEPTION 'El nombre debe tener al menos 2 caracteres.';
    END IF;
    RETURN NEW;
END;
$$;

CREATE TRIGGER trg_cuentas_validar_nombre
    BEFORE INSERT OR UPDATE OF nombre_mostrar ON public.cuentas
    FOR EACH ROW EXECUTE FUNCTION public.fn_validar_nombre_cuenta();


-- ----------------------------------------------------------------------------
-- ¿El teléfono ya está en otra cuenta? (aviso del registro / perfil)
-- ----------------------------------------------------------------------------
-- Solo responde sí/no; no dice de quién es. Compara solo dígitos (los números
-- viejos pueden tener espacios) y quita la lada 52 de números de 12 dígitos.
CREATE FUNCTION public.telefono_en_uso(p_numero TEXT)
RETURNS BOOLEAN
LANGUAGE plpgsql
STABLE
SECURITY DEFINER
SET search_path = ''
AS $$
DECLARE
    v_numero TEXT := regexp_replace(COALESCE(p_numero, ''), '[^0-9]', '', 'g');
BEGIN
    IF CHAR_LENGTH(v_numero) = 12 AND v_numero LIKE '52%' THEN
        v_numero := SUBSTRING(v_numero FROM 3);
    END IF;
    IF CHAR_LENGTH(v_numero) <> 10 THEN
        RETURN FALSE;
    END IF;

    RETURN EXISTS (
        SELECT 1 FROM public.telefonos_contacto t
        WHERE t.activo
          -- La cuenta en sesión (al editar el perfil) no cuenta como "otra".
          AND t.id_cuenta IS DISTINCT FROM auth.uid()
          AND RIGHT(regexp_replace(t.numero_telefono, '[^0-9]', '', 'g'), 10) = v_numero
    );
END;
$$;

-- El registro ocurre sin sesión, así que también lo pueden llamar visitantes.
GRANT EXECUTE ON FUNCTION public.telefono_en_uso(TEXT) TO anon, authenticated;
