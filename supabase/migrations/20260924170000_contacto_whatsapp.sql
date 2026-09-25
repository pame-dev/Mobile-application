-- ============================================================================
-- KARSY — Migración: teléfono, WhatsApp y método de contacto principal
-- ============================================================================
-- El registro pide:
--   telefono        número para llamadas
--   whatsapp        número de WhatsApp (igual al teléfono si usa el mismo;
--                   vacío si no tiene)
--   medio_contacto  'llamada' | 'whatsapp'
--
-- En telefonos_contacto:
--   · Mismo número  → una fila con permite_llamadas y es_whatsapp.
--   · Otro número   → dos filas; es_principal marca la del método elegido.
-- Con un solo número es_principal no distingue si prefiere llamada o WhatsApp,
-- por eso el método queda en cuentas.medio_contacto_principal.
-- ============================================================================

ALTER TABLE public.cuentas
    ADD COLUMN medio_contacto_principal VARCHAR(10),
    ADD CONSTRAINT chk_cuentas_medio_contacto_principal
        CHECK (medio_contacto_principal IN ('llamada', 'whatsapp'));

COMMENT ON COLUMN public.cuentas.medio_contacto_principal IS
    'Cómo prefiere la cuenta que la contacten: llamada o whatsapp. Nulo si no lo indicó.';


-- ----------------------------------------------------------------------------
-- Registro: misma función que en 20260924120200, con los teléfonos nuevos.
-- ----------------------------------------------------------------------------
CREATE OR REPLACE FUNCTION public.fn_auth_crear_cuenta()
RETURNS TRIGGER
LANGUAGE plpgsql
SECURITY DEFINER
SET search_path = ''
AS $$
DECLARE
    v_meta     JSONB := COALESCE(NEW.raw_user_meta_data, '{}'::JSONB);
    v_tipo     TEXT  := v_meta ->> 'tipo_cuenta';
    v_telefono TEXT  := NULLIF(TRIM(v_meta ->> 'telefono'), '');
    v_whatsapp TEXT  := NULLIF(TRIM(v_meta ->> 'whatsapp'), '');
    v_medio    TEXT  := v_meta ->> 'medio_contacto';
BEGIN
    IF v_tipo IS NULL THEN
        RETURN NEW;
    END IF;

    -- Versiones anteriores de la app mandaban un solo número para todo.
    IF NOT v_meta ? 'whatsapp' THEN
        v_whatsapp := v_telefono;
    END IF;

    -- El método principal tiene que tener un número detrás.
    v_medio := CASE
        WHEN v_telefono IS NULL AND v_whatsapp IS NULL THEN NULL
        WHEN v_whatsapp IS NULL THEN 'llamada'
        WHEN v_telefono IS NULL THEN 'whatsapp'
        WHEN v_medio IN ('llamada', 'whatsapp') THEN v_medio
        ELSE NULL
    END;

    INSERT INTO public.cuentas (
        id_cuenta, tipo_cuenta, nombre_mostrar, descripcion_corta,
        estado_perfil_ubi, municipio_perfil_ubi, medio_contacto_principal
    ) VALUES (
        NEW.id,
        v_tipo,
        TRIM(v_meta ->> 'nombre_mostrar'),
        LEFT(NULLIF(TRIM(v_meta ->> 'descripcion'), ''), 300),
        TRIM(v_meta ->> 'estado'),
        TRIM(v_meta ->> 'municipio'),
        v_medio
    );

    IF v_telefono IS NOT NULL AND v_telefono = v_whatsapp THEN
        INSERT INTO public.telefonos_contacto (
            id_cuenta, numero_telefono, permite_llamadas, es_whatsapp, es_principal
        ) VALUES (NEW.id, v_telefono, TRUE, TRUE, TRUE);
    ELSE
        IF v_telefono IS NOT NULL THEN
            INSERT INTO public.telefonos_contacto (
                id_cuenta, numero_telefono, permite_llamadas, es_whatsapp, es_principal
            ) VALUES (NEW.id, v_telefono, TRUE, FALSE, v_medio IS DISTINCT FROM 'whatsapp');
        END IF;
        IF v_whatsapp IS NOT NULL THEN
            INSERT INTO public.telefonos_contacto (
                id_cuenta, numero_telefono, permite_llamadas, es_whatsapp, es_principal
            ) VALUES (NEW.id, v_whatsapp, FALSE, TRUE, v_medio IS NOT DISTINCT FROM 'whatsapp');
        END IF;
    END IF;

    IF v_tipo = 'lote' THEN
        INSERT INTO public.perfiles_lote (
            id_cuenta, nombre_comercial, descripcion_lote, calle, numero, colonia,
            codigo_postal, municipio, estado, horarios_atencion
        ) VALUES (
            NEW.id,
            TRIM(COALESCE(v_meta ->> 'nombre_comercial', v_meta ->> 'nombre_mostrar')),
            NULLIF(TRIM(v_meta ->> 'descripcion'), ''),
            TRIM(v_meta ->> 'calle'),
            TRIM(v_meta ->> 'numero'),
            TRIM(v_meta ->> 'colonia'),
            TRIM(v_meta ->> 'codigo_postal'),
            TRIM(v_meta ->> 'municipio'),
            TRIM(v_meta ->> 'estado'),
            NULLIF(TRIM(v_meta ->> 'horarios'), '')
        );
    END IF;

    RETURN NEW;
END;
$$;


-- ----------------------------------------------------------------------------
-- Contacto del vendedor: "telefono" ahora es el número para llamadas (antes era
-- el principal, que puede ser solo de WhatsApp). Se agregan numero_whatsapp y
-- medio_principal. "whatsapp" se conserva para versiones anteriores de la app.
-- ----------------------------------------------------------------------------
CREATE OR REPLACE FUNCTION public.contacto_vendedor(p_id_publicacion BIGINT)
RETURNS JSONB
LANGUAGE plpgsql
STABLE
SECURITY DEFINER
SET search_path = ''
AS $$
DECLARE
    v_propietario UUID;
    v_resultado   JSONB;
BEGIN
    SELECT id_propietario INTO v_propietario
    FROM public.publicaciones
    WHERE id_publicacion = p_id_publicacion
      AND (
          (fecha_primera_publicacion IS NOT NULL AND estado_administrativo = 'habilitada')
          OR id_propietario = auth.uid()
          OR public.es_administrador(auth.uid())
      );

    IF v_propietario IS NULL THEN
        RETURN NULL;
    END IF;

    SELECT jsonb_build_object(
        'id_cuenta',       c.id_cuenta,
        'nombre',          c.nombre_mostrar,
        'tipo_cuenta',     c.tipo_cuenta,
        'descripcion',     COALESCE(pl.descripcion_lote, c.descripcion_corta),
        'ubicacion',       c.municipio_perfil_ubi || ', ' || c.estado_perfil_ubi,
        'foto_perfil',     c.foto_perfil,
        'miembro_desde',   c.fecha_creacion,
        'telefono',        CASE WHEN auth.uid() IS NOT NULL THEN tl.numero_telefono END,
        'whatsapp',        CASE WHEN auth.uid() IS NOT NULL THEN tl.es_whatsapp END,
        'numero_whatsapp', CASE WHEN auth.uid() IS NOT NULL THEN wa.numero_telefono END,
        'medio_principal', CASE WHEN auth.uid() IS NOT NULL THEN c.medio_contacto_principal END,
        'correo',          CASE WHEN auth.uid() IS NOT NULL THEN u.email END
    )
    INTO v_resultado
    FROM public.cuentas c
    JOIN auth.users u ON u.id = c.id_cuenta
    LEFT JOIN public.perfiles_lote pl ON pl.id_cuenta = c.id_cuenta
    LEFT JOIN LATERAL (
        SELECT numero_telefono, es_whatsapp
        FROM public.telefonos_contacto
        WHERE id_cuenta = c.id_cuenta AND activo AND permite_llamadas
        ORDER BY es_principal DESC, id_telefono
        LIMIT 1
    ) tl ON TRUE
    LEFT JOIN LATERAL (
        SELECT numero_telefono
        FROM public.telefonos_contacto
        WHERE id_cuenta = c.id_cuenta AND activo AND es_whatsapp
        ORDER BY es_principal DESC, id_telefono
        LIMIT 1
    ) wa ON TRUE
    WHERE c.id_cuenta = v_propietario;

    RETURN v_resultado;
END;
$$;
