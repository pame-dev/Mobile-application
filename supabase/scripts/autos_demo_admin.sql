-- ############################################################################
-- 6 autos de demostración para la cuenta del administrador
-- ############################################################################
-- NO es una migración: no se aplica con `supabase db push`. Se corre UNA vez a mano:
-- Supabase → SQL Editor → pegar este archivo → Run.
--
-- ANTES de correrlo, sube las 6 fotos de supabase/scripts/fotos_demo/ (demo-corolla.jpg,
-- demo-cx5.jpg, demo-np300.jpg, demo-jetta.jpg, demo-hrv.jpg, demo-rio.jpg) a:
--   Storage → bucket "karsy" → carpeta publicaciones/<id del admin>/
-- El id del admin está en Authentication → Users (columna UID). Si falta alguna foto, el
-- script te dice cuál y en qué carpeta, y no crea nada.
--
-- Qué hace:
--   * Busca la cuenta del administrador (si hay más de una, escribe su correo en v_correo).
--   * Crea cada auto con el mismo camino que la app y la web (crear_publicacion) y lo
--     aprueba como ese admin (admin_moderar_propuesta), así pasa por todas las validaciones.
--   * Mientras tanto apaga los avisos de "búsqueda guardada" y "auto parecido" para que
--     ningún usuario reciba notificaciones de autos de demostración; al terminar los vuelve
--     a encender. Si algo falla, no se guarda nada (todo va en una sola transacción).
--   * Si ya existe un auto de demostración con el mismo título, no lo duplica.
--
--   * Borra los avisos que el admin se mandaría a sí mismo ("propuesta enviada" y
--     "publicación aprobada"), para que no le lleguen 12 notificaciones.
--
-- Fotos: de Wikimedia Commons (licencias libres); el crédito va en la descripción de cada
-- auto. Se pueden editar desde la app o la web como cualquier publicación. Para quitarlos:
-- márcalos como vendidos o pausados desde Mi panel.

DO $$
DECLARE
    -- Correo del admin; vacío = el único administrador que exista.
    v_correo    TEXT := '';
    v_admin     UUID;
    v_id        BIGINT;
    v_propuesta BIGINT;
    v_creados   INT := 0;
    v_ids       BIGINT[] := ARRAY[]::BIGINT[];
    v_admins    INT;
    v_marca     BIGINT;
    v_modelo    BIGINT;
    v_color     BIGINT;
    v_trans     BIGINT;
    v_carroc    BIGINT;
    a           JSONB;
    v_autos     JSONB := '[
      {"marca":"Toyota","modelo":"Corolla","anio":2020,"precio":329000,"km":42000,"color":"Plata","transmision":"CVT","carroceria":"Sedán",
       "cilindros":4,"motor":"2.0L","combustible":"Gasolina","duenos":1,"estado":"Ciudad de México","municipio":"Coyoacán",
       "descripcion":"Corolla SE en excelente estado, servicios de agencia al día, llantas nuevas y factura original. Ideal para ciudad y carretera.",
       "foto":"demo-corolla.jpg",
       "credito":"Elise240SX, Wikimedia Commons, CC BY-SA 4.0"},
      {"marca":"Mazda","modelo":"CX-5","anio":2019,"precio":389000,"km":58000,"color":"Rojo","transmision":"Automática","carroceria":"SUV",
       "cilindros":4,"motor":"2.5L","combustible":"Gasolina","duenos":1,"estado":"Jalisco","municipio":"Zapopan",
       "descripcion":"CX-5 Grand Touring, color rojo cristal, interiores de piel, cámara de reversa y pantalla con Apple CarPlay. Muy cuidada.",
       "foto":"demo-cx5.jpg",
       "credito":"Vauxford, Wikimedia Commons, CC BY-SA 4.0"},
      {"marca":"Nissan","modelo":"NP300","anio":2018,"precio":319000,"km":96000,"color":"Gris","transmision":"Manual","carroceria":"Pickup",
       "cilindros":4,"motor":"2.5L","combustible":"Diésel","duenos":2,"estado":"Nuevo León","municipio":"Monterrey",
       "descripcion":"NP300 Frontier doble cabina diésel, lista para trabajar. Mantenimiento al corriente, batea con protector.",
       "foto":"demo-np300.jpg",
       "credito":"Bull-Doser, Wikimedia Commons, dominio público"},
      {"marca":"Volkswagen","modelo":"Jetta","anio":2019,"precio":299000,"km":61000,"color":"Gris","transmision":"Automática","carroceria":"Sedán",
       "cilindros":4,"motor":"1.4L turbo","combustible":"Gasolina","duenos":1,"estado":"Puebla","municipio":"Puebla",
       "descripcion":"Jetta Comfortline 1.4 turbo, muy rendidor de gasolina. Un solo dueño, todos los servicios en agencia.",
       "foto":"demo-jetta.jpg",
       "credito":"Kevauto, Wikimedia Commons, CC BY-SA 4.0"},
      {"marca":"Honda","modelo":"HR-V","anio":2019,"precio":339000,"km":47000,"color":"Negro","transmision":"CVT","carroceria":"SUV",
       "cilindros":4,"motor":"1.8L","combustible":"Gasolina","duenos":1,"estado":"Querétaro","municipio":"Querétaro",
       "descripcion":"HR-V Touring negra, quemacocos, asientos traseros abatibles tipo Magic Seat. Lista para viajar con la familia.",
       "foto":"demo-hrv.jpg",
       "credito":"Harvey Bold, Wikimedia Commons, CC BY 4.0"},
      {"marca":"Kia","modelo":"Rio","anio":2018,"precio":209000,"km":73000,"color":"Blanco","transmision":"Manual","carroceria":"Hatchback",
       "cilindros":4,"motor":"1.6L","combustible":"Gasolina","duenos":2,"estado":"Estado de México","municipio":"Naucalpan de Juárez",
       "descripcion":"Rio hatchback EX, económico y fácil de estacionar. Aire acondicionado, pantalla táctil y control de crucero.",
       "foto":"demo-rio.jpg",
       "credito":"Areaseven, Wikimedia Commons, CC BY-SA 4.0"}
    ]';
BEGIN
    -- 1. La cuenta del administrador.
    SELECT COUNT(*) INTO v_admins FROM public.cuentas c WHERE public.es_administrador(c.id_cuenta);
    IF TRIM(v_correo) <> '' THEN
        SELECT c.id_cuenta INTO v_admin
        FROM public.cuentas c JOIN auth.users u ON u.id = c.id_cuenta
        WHERE LOWER(u.email) = LOWER(TRIM(v_correo)) AND public.es_administrador(c.id_cuenta);
    ELSIF v_admins = 0 THEN
        RAISE EXCEPTION 'No hay ninguna cuenta de administrador.';
    ELSIF v_admins > 1 THEN
        RAISE EXCEPTION 'Hay % administradores: escribe en v_correo el correo del que tendrá los autos.', v_admins;
    ELSE
        SELECT c.id_cuenta INTO v_admin FROM public.cuentas c WHERE public.es_administrador(c.id_cuenta);
    END IF;
    IF v_admin IS NULL THEN
        RAISE EXCEPTION 'No encontré una cuenta de administrador con ese correo.';
    END IF;

    -- Las fotos ya deben estar subidas en publicaciones/<id del admin>/.
    FOR a IN SELECT * FROM jsonb_array_elements(v_autos) LOOP
        IF NOT EXISTS (
            SELECT 1 FROM storage.objects o
            WHERE o.bucket_id = 'karsy' AND o.name = 'publicaciones/' || v_admin || '/' || (a ->> 'foto')
        ) THEN
            RAISE EXCEPTION 'Falta subir la foto % a Storage → karsy → publicaciones/%/', a ->> 'foto', v_admin;
        END IF;
    END LOOP;

    -- 2. Actuar como ese admin (lo mismo que hace la app con su sesión), solo en esta transacción.
    PERFORM set_config('request.jwt.claim.sub', v_admin::TEXT, TRUE);
    PERFORM set_config('request.jwt.claims', json_build_object('sub', v_admin, 'role', 'authenticated')::TEXT, TRUE);

    -- 3. Sin avisos a otros usuarios por autos de demostración. Si la tabla está ocupada por
    --    otra operación larga, se rinde a los 5 s (vuelve a correr el script) en vez de frenar la app.
    PERFORM set_config('lock_timeout', '5s', TRUE);
    ALTER TABLE public.publicaciones DISABLE TRIGGER trg_publicaciones_notificar_busquedas;
    ALTER TABLE public.publicaciones DISABLE TRIGGER trg_publicaciones_notificar_parecidos;

    -- 4. Crear y aprobar cada auto.
    FOR a IN SELECT * FROM jsonb_array_elements(v_autos) LOOP
        CONTINUE WHEN EXISTS (
            SELECT 1 FROM public.publicaciones p
            WHERE p.id_propietario = v_admin
              AND p.titulo = (a ->> 'marca') || ' ' || (a ->> 'modelo') || ' ' || (a ->> 'anio')
              AND p.descripcion LIKE '%Publicación de demostración%'
        );

        -- Catálogos por nombre; si alguno no existe se avisa cuál (y no se guarda nada).
        SELECT m.id_marca INTO v_marca FROM public.marcas m WHERE m.nombre_marca = a ->> 'marca';
        SELECT mo.id_modelo INTO v_modelo FROM public.modelos mo WHERE mo.id_marca = v_marca AND mo.nombre_modelo = a ->> 'modelo';
        SELECT c.id_color INTO v_color FROM public.colores c WHERE c.nombre_color = a ->> 'color';
        SELECT t.id_transmision INTO v_trans FROM public.transmisiones t WHERE t.nombre_transmision = a ->> 'transmision';
        SELECT ca.id_carroceria INTO v_carroc FROM public.carrocerias ca WHERE ca.nombre_carroceria = a ->> 'carroceria';
        IF v_marca IS NULL OR v_modelo IS NULL THEN
            RAISE EXCEPTION 'No existe en el catálogo: % %', a ->> 'marca', a ->> 'modelo';
        ELSIF v_color IS NULL THEN
            RAISE EXCEPTION 'No existe el color: %', a ->> 'color';
        ELSIF v_trans IS NULL THEN
            RAISE EXCEPTION 'No existe la transmisión: %', a ->> 'transmision';
        ELSIF v_carroc IS NULL THEN
            RAISE EXCEPTION 'No existe la carrocería: %', a ->> 'carroceria';
        END IF;

        v_id := public.crear_publicacion(
            jsonb_build_object(
                'titulo', (a ->> 'marca') || ' ' || (a ->> 'modelo') || ' ' || (a ->> 'anio'),
                'descripcion', (a ->> 'descripcion')
                    || E'\n\nPublicación de demostración de Karsy: no es un auto real a la venta.'
                    || E'\nFoto: ' || (a ->> 'credito') || '.',
                'precio', (a ->> 'precio')::NUMERIC,
                'moneda', 'MXN',
                'estado_ubicacion', a ->> 'estado',
                'municipio_ubicacion', a ->> 'municipio',
                'id_marca', v_marca,
                'id_modelo', v_modelo,
                'anio', (a ->> 'anio')::INT,
                'kilometraje', (a ->> 'km')::INT,
                'id_color', v_color,
                'id_transmision', v_trans,
                'id_carroceria', v_carroc,
                'numero_propietarios_anteriores', (a ->> 'duenos')::INT,
                'cilindros', (a ->> 'cilindros')::INT,
                'cilindrada', a ->> 'motor',
                'tipo_combustible', a ->> 'combustible',
                'recuperado_por_seguro', FALSE,
                'descripcion_problemas', ''
            ),
            ARRAY['publicaciones/' || v_admin || '/' || (a ->> 'foto')]
        );

        SELECT pr.id_propuesta INTO v_propuesta
        FROM public.propuestas_publicacion pr
        WHERE pr.id_publicacion = v_id AND pr.estado_propuesta = 'pendiente';

        PERFORM public.admin_moderar_propuesta(v_propuesta, TRUE, 'Publicación de demostración.');
        v_creados := v_creados + 1;
        v_ids := v_ids || v_id;
    END LOOP;

    -- Sin avisos para el propio admin por sus autos de demostración (tampoco sale el push:
    -- enviar-push omite los avisos que ya no existen).
    DELETE FROM public.notificaciones WHERE id_cuenta = v_admin AND id_publicacion = ANY (v_ids);

    -- 5. Avisos encendidos otra vez.
    ALTER TABLE public.publicaciones ENABLE TRIGGER trg_publicaciones_notificar_busquedas;
    ALTER TABLE public.publicaciones ENABLE TRIGGER trg_publicaciones_notificar_parecidos;

    RAISE NOTICE 'Autos de demostración creados: %', v_creados;
END;
$$;
