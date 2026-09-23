-- ============================================================================
-- KARSY — Migración: estructura inicial completa (modelo v4.3, 19 tablas)
-- ============================================================================
-- Fuentes:
--   · Diccionario de datos (catálogo PostgreSQL, 2026-09-23)
--   · DER lógico v4.3 y Diagrama de clases v4.3 (Lucidchart)
--
-- Sustituye a la migración 001 anterior. Cambios respecto a ella:
--   · Se eliminan las tablas permisos y roles_permisos (decisión del equipo:
--     la app solo consulta si la cuenta tiene el rol administrador).
--   · cuentas: se elimina fecha_aceptacion_condiciones; estado_perfil y
--     municipio_perfil pasan a estado_perfil_ubi y municipio_perfil_ubi;
--     se agrega fecha_reactivacion.
--   · perfiles_lote: se eliminan nombre_representante, rfc y sitio_web;
--     horarios_atencion pasa a ser opcional.
--   · telefonos_contacto: se agrega permite_llamadas.
--   · roles: se elimina activo; descripcion pasa a descripcion_rol.
--   · Se agregan catálogos, publicaciones, propuestas, fotos, favoritos,
--     reportes, solicitudes de destacado, interacciones y bitácora.
--
-- Contraseñas, sesiones, tokens y correo permanecen en Supabase Auth.
-- Las políticas RLS se definen en una migración posterior.
-- ============================================================================


-- ############################################################################
-- 1. ACCESO Y PERFIL
-- ############################################################################

-- ----------------------------------------------------------------------------
-- cuentas
-- ----------------------------------------------------------------------------
CREATE TABLE public.cuentas (
    id_cuenta                UUID          PRIMARY KEY,
    tipo_cuenta              VARCHAR(12)   NOT NULL,
    nombre_mostrar           VARCHAR(100)  NOT NULL,
    foto_perfil              TEXT,
    descripcion_corta        VARCHAR(300),
    estado_perfil_ubi        VARCHAR(60)   NOT NULL,
    municipio_perfil_ubi     VARCHAR(100)  NOT NULL,
    estado_cuenta            VARCHAR(12)   NOT NULL DEFAULT 'activa',
    fecha_creacion           TIMESTAMPTZ   NOT NULL DEFAULT NOW(),
    fecha_suspension_inicio  TIMESTAMPTZ,
    fecha_reactivacion       TIMESTAMPTZ,

    -- Mismo UUID que Supabase Auth. RESTRICT evita borrar una identidad
    -- que todavía tiene información de dominio en Karsy.
    CONSTRAINT fk_cuentas_auth_users
        FOREIGN KEY (id_cuenta) REFERENCES auth.users (id) ON DELETE RESTRICT,

    CONSTRAINT chk_cuentas_tipo_cuenta
        CHECK (tipo_cuenta IN ('particular', 'lote')),
    CONSTRAINT chk_cuentas_estado_cuenta
        CHECK (estado_cuenta IN ('activa', 'suspendida')),
    CONSTRAINT chk_cuentas_nombre_mostrar
        CHECK (CHAR_LENGTH(TRIM(nombre_mostrar)) > 0),
    CONSTRAINT chk_cuentas_estado_perfil_ubi
        CHECK (CHAR_LENGTH(TRIM(estado_perfil_ubi)) > 0),
    CONSTRAINT chk_cuentas_municipio_perfil_ubi
        CHECK (CHAR_LENGTH(TRIM(municipio_perfil_ubi)) > 0),

    -- Suspendida ⇔ existe fecha de inicio de la suspensión vigente.
    CONSTRAINT chk_cuentas_suspension
        CHECK (
            (estado_cuenta = 'activa'     AND fecha_suspension_inicio IS NULL) OR
            (estado_cuenta = 'suspendida' AND fecha_suspension_inicio IS NOT NULL)
        )
);


-- ----------------------------------------------------------------------------
-- perfiles_lote  (solo existe si cuentas.tipo_cuenta = 'lote'; ver trigger)
-- ----------------------------------------------------------------------------
CREATE TABLE public.perfiles_lote (
    id_cuenta          UUID          PRIMARY KEY,
    nombre_comercial   VARCHAR(120)  NOT NULL,
    logo_lote          TEXT,
    descripcion_lote   TEXT,
    calle              VARCHAR(120)  NOT NULL,
    numero             VARCHAR(20)   NOT NULL,
    colonia            VARCHAR(120)  NOT NULL,
    codigo_postal      VARCHAR(10)   NOT NULL,
    municipio          VARCHAR(100)  NOT NULL,
    estado             VARCHAR(60)   NOT NULL,
    horarios_atencion  VARCHAR(250),

    CONSTRAINT fk_perfiles_lote_cuentas
        FOREIGN KEY (id_cuenta) REFERENCES public.cuentas (id_cuenta) ON DELETE RESTRICT,

    CONSTRAINT chk_perfiles_lote_nombre_comercial CHECK (CHAR_LENGTH(TRIM(nombre_comercial)) > 0),
    CONSTRAINT chk_perfiles_lote_calle            CHECK (CHAR_LENGTH(TRIM(calle)) > 0),
    CONSTRAINT chk_perfiles_lote_numero           CHECK (CHAR_LENGTH(TRIM(numero)) > 0),
    CONSTRAINT chk_perfiles_lote_colonia          CHECK (CHAR_LENGTH(TRIM(colonia)) > 0),
    CONSTRAINT chk_perfiles_lote_codigo_postal    CHECK (CHAR_LENGTH(TRIM(codigo_postal)) > 0),
    CONSTRAINT chk_perfiles_lote_municipio        CHECK (CHAR_LENGTH(TRIM(municipio)) > 0),
    CONSTRAINT chk_perfiles_lote_estado           CHECK (CHAR_LENGTH(TRIM(estado)) > 0)
);


-- ----------------------------------------------------------------------------
-- telefonos_contacto
-- ----------------------------------------------------------------------------
CREATE TABLE public.telefonos_contacto (
    id_telefono       UUID         PRIMARY KEY DEFAULT gen_random_uuid(),
    id_cuenta         UUID         NOT NULL,
    numero_telefono   VARCHAR(20)  NOT NULL,
    permite_llamadas  BOOLEAN      NOT NULL DEFAULT TRUE,
    es_whatsapp       BOOLEAN      NOT NULL DEFAULT FALSE,
    es_principal      BOOLEAN      NOT NULL DEFAULT FALSE,
    activo            BOOLEAN      NOT NULL DEFAULT TRUE,

    CONSTRAINT fk_telefonos_contacto_cuentas
        FOREIGN KEY (id_cuenta) REFERENCES public.cuentas (id_cuenta) ON DELETE RESTRICT,

    CONSTRAINT uq_telefonos_contacto_id_cuenta_numero_telefono
        UNIQUE (id_cuenta, numero_telefono),

    CONSTRAINT chk_telefonos_contacto_numero
        CHECK (CHAR_LENGTH(TRIM(numero_telefono)) > 0),

    -- Un número activo debe servir al menos para llamadas o WhatsApp.
    CONSTRAINT chk_telefonos_contacto_medio
        CHECK (NOT activo OR permite_llamadas OR es_whatsapp)
);

-- Máximo un número principal activo por cuenta.
CREATE UNIQUE INDEX uq_telefonos_contacto_principal
    ON public.telefonos_contacto (id_cuenta)
    WHERE es_principal AND activo;


-- ############################################################################
-- 2. ROLES  (sin tabla de permisos: la app consulta el rol por nombre)
-- ############################################################################

CREATE TABLE public.roles (
    id_rol           BIGINT        GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    nombre_rol       VARCHAR(40)   NOT NULL,
    descripcion_rol  VARCHAR(250),

    CONSTRAINT uq_roles_nombre_rol  UNIQUE (nombre_rol),
    CONSTRAINT chk_roles_nombre_rol CHECK (CHAR_LENGTH(TRIM(nombre_rol)) > 0)
);


CREATE TABLE public.cuentas_roles (
    id_cuenta         UUID         NOT NULL,
    id_rol            BIGINT       NOT NULL,
    fecha_asignacion  TIMESTAMPTZ  NOT NULL DEFAULT NOW(),

    CONSTRAINT pk_cuentas_roles PRIMARY KEY (id_cuenta, id_rol),

    CONSTRAINT fk_cuentas_roles_cuentas
        FOREIGN KEY (id_cuenta) REFERENCES public.cuentas (id_cuenta) ON DELETE RESTRICT,
    CONSTRAINT fk_cuentas_roles_roles
        FOREIGN KEY (id_rol) REFERENCES public.roles (id_rol) ON DELETE RESTRICT
);

CREATE INDEX ix_cuentas_roles_id_rol ON public.cuentas_roles (id_rol);


-- ############################################################################
-- 3. CATÁLOGOS  (cada uno con una sola fila "Otros"; se administran por scripts)
-- ############################################################################

CREATE TABLE public.marcas (
    id_marca      BIGINT       GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    nombre_marca  VARCHAR(80)  NOT NULL,
    es_otro       BOOLEAN      NOT NULL DEFAULT FALSE,

    CONSTRAINT uq_marcas_nombre_marca  UNIQUE (nombre_marca),
    CONSTRAINT chk_marcas_nombre_marca CHECK (CHAR_LENGTH(TRIM(nombre_marca)) > 0)
);
CREATE UNIQUE INDEX uq_marcas_otros ON public.marcas (es_otro) WHERE es_otro;


CREATE TABLE public.modelos (
    id_modelo      BIGINT       GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    id_marca       BIGINT       NOT NULL,
    nombre_modelo  VARCHAR(80)  NOT NULL,
    es_otro        BOOLEAN      NOT NULL DEFAULT FALSE,

    CONSTRAINT fk_modelos_marcas
        FOREIGN KEY (id_marca) REFERENCES public.marcas (id_marca) ON DELETE RESTRICT,

    CONSTRAINT uq_modelos_id_marca_nombre_modelo UNIQUE (id_marca, nombre_modelo),

    -- Permite la FK compuesta (id_modelo, id_marca) desde publicaciones y
    -- propuestas: garantiza que el modelo pertenezca a la marca elegida.
    CONSTRAINT uq_modelos_id_modelo_id_marca UNIQUE (id_modelo, id_marca),

    CONSTRAINT chk_modelos_nombre_modelo CHECK (CHAR_LENGTH(TRIM(nombre_modelo)) > 0)
);
-- Máximo una fila "Otros" por marca.
CREATE UNIQUE INDEX uq_modelos_otros ON public.modelos (id_marca) WHERE es_otro;


CREATE TABLE public.carrocerias (
    id_carroceria      BIGINT       GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    nombre_carroceria  VARCHAR(80)  NOT NULL,
    es_otro            BOOLEAN      NOT NULL DEFAULT FALSE,

    CONSTRAINT uq_carrocerias_nombre_carroceria  UNIQUE (nombre_carroceria),
    CONSTRAINT chk_carrocerias_nombre_carroceria CHECK (CHAR_LENGTH(TRIM(nombre_carroceria)) > 0)
);
CREATE UNIQUE INDEX uq_carrocerias_otros ON public.carrocerias (es_otro) WHERE es_otro;


CREATE TABLE public.transmisiones (
    id_transmision      BIGINT       GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    nombre_transmision  VARCHAR(80)  NOT NULL,
    es_otro             BOOLEAN      NOT NULL DEFAULT FALSE,

    CONSTRAINT uq_transmisiones_nombre_transmision  UNIQUE (nombre_transmision),
    CONSTRAINT chk_transmisiones_nombre_transmision CHECK (CHAR_LENGTH(TRIM(nombre_transmision)) > 0)
);
CREATE UNIQUE INDEX uq_transmisiones_otros ON public.transmisiones (es_otro) WHERE es_otro;


CREATE TABLE public.colores (
    id_color      BIGINT       GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    nombre_color  VARCHAR(80)  NOT NULL,
    es_otro       BOOLEAN      NOT NULL DEFAULT FALSE,

    CONSTRAINT uq_colores_nombre_color  UNIQUE (nombre_color),
    CONSTRAINT chk_colores_nombre_color CHECK (CHAR_LENGTH(TRIM(nombre_color)) > 0)
);
CREATE UNIQUE INDEX uq_colores_otros ON public.colores (es_otro) WHERE es_otro;


-- ############################################################################
-- 4. ANUNCIOS
-- ############################################################################

-- ----------------------------------------------------------------------------
-- publicaciones — versión pública aprobada del anuncio.
-- Antes de la 1.ª aprobación (fecha_primera_publicacion NULL) los campos
-- públicos pueden ser nulos; después son obligatorios.
-- ----------------------------------------------------------------------------
CREATE TABLE public.publicaciones (
    id_publicacion                       UUID           PRIMARY KEY DEFAULT gen_random_uuid(),
    id_propietario                       UUID           NOT NULL,

    titulo                               VARCHAR(120),
    descripcion                          TEXT,
    precio                               NUMERIC(12,2),
    precio_negociable                    BOOLEAN,
    moneda                               CHAR(3),
    estado_ubicacion                     VARCHAR(60),
    municipio_ubicacion                  VARCHAR(100),
    horario_preferido_contacto           VARCHAR(120),
    ruta_video                           TEXT,

    id_marca                             BIGINT,
    marca_otra                           VARCHAR(60),
    id_modelo                            BIGINT,
    modelo_otro                          VARCHAR(60),
    anio                                 SMALLINT,
    kilometraje                          INTEGER,
    id_color                             BIGINT,
    color_otro                           VARCHAR(60),
    id_transmision                       BIGINT,
    transmision_otra                     VARCHAR(60),
    id_carroceria                        BIGINT,
    carroceria_otra                      VARCHAR(60),

    numero_propietarios_anteriores       SMALLINT,
    tiene_adeudos                        BOOLEAN,
    tiene_problemas                      BOOLEAN,
    descripcion_problemas                TEXT,
    recuperado_por_seguro                BOOLEAN,

    tipo_combustible                     VARCHAR(40),
    numero_puertas                       SMALLINT,
    numero_pasajeros                     SMALLINT,
    cilindros                            SMALLINT,
    cilindrada                           VARCHAR(30),
    tipo_traccion                        VARCHAR(40),
    modificaciones                       TEXT,

    estado_publicacion                   VARCHAR(16)    NOT NULL DEFAULT 'activa',
    estado_administrativo                VARCHAR(32)    NOT NULL DEFAULT 'habilitada',
    fecha_creacion                       TIMESTAMPTZ    NOT NULL DEFAULT NOW(),
    fecha_primera_publicacion            TIMESTAMPTZ,
    fecha_ultima_actualizacion_aprobada  TIMESTAMPTZ,

    -- Relaciones
    CONSTRAINT fk_publicaciones_cuentas
        FOREIGN KEY (id_propietario) REFERENCES public.cuentas (id_cuenta) ON DELETE RESTRICT,
    CONSTRAINT fk_publicaciones_marcas
        FOREIGN KEY (id_marca) REFERENCES public.marcas (id_marca) ON DELETE RESTRICT,
    CONSTRAINT fk_publicaciones_modelos
        FOREIGN KEY (id_modelo, id_marca) REFERENCES public.modelos (id_modelo, id_marca) ON DELETE RESTRICT,
    CONSTRAINT fk_publicaciones_colores
        FOREIGN KEY (id_color) REFERENCES public.colores (id_color) ON DELETE RESTRICT,
    CONSTRAINT fk_publicaciones_transmisiones
        FOREIGN KEY (id_transmision) REFERENCES public.transmisiones (id_transmision) ON DELETE RESTRICT,
    CONSTRAINT fk_publicaciones_carrocerias
        FOREIGN KEY (id_carroceria) REFERENCES public.carrocerias (id_carroceria) ON DELETE RESTRICT,

    -- Estados
    CONSTRAINT chk_publicaciones_estado_publicacion
        CHECK (estado_publicacion IN ('activa', 'vendida', 'deshabilitada')),
    CONSTRAINT chk_publicaciones_estado_administrativo
        CHECK (estado_administrativo IN ('habilitada', 'deshabilitada_administrador')),

    -- Dominios de valores
    CONSTRAINT chk_publicaciones_moneda      CHECK (moneda IN ('MXN', 'USD')),
    CONSTRAINT chk_publicaciones_precio      CHECK (precio > 0),
    CONSTRAINT chk_publicaciones_kilometraje CHECK (kilometraje >= 0),
    CONSTRAINT chk_publicaciones_propietarios_anteriores CHECK (numero_propietarios_anteriores >= 0),
    CONSTRAINT chk_publicaciones_numero_puertas   CHECK (numero_puertas >= 0),
    CONSTRAINT chk_publicaciones_numero_pasajeros CHECK (numero_pasajeros >= 0),
    CONSTRAINT chk_publicaciones_cilindros        CHECK (cilindros >= 0),
    CONSTRAINT chk_publicaciones_titulo           CHECK (CHAR_LENGTH(TRIM(titulo)) > 0),

    -- Texto "Otros": 1..60 caracteres cuando existe
    -- (la obligatoriedad según es_otro se valida con trigger).
    CONSTRAINT chk_publicaciones_marca_otra       CHECK (CHAR_LENGTH(TRIM(marca_otra))       BETWEEN 1 AND 60),
    CONSTRAINT chk_publicaciones_modelo_otro      CHECK (CHAR_LENGTH(TRIM(modelo_otro))      BETWEEN 1 AND 60),
    CONSTRAINT chk_publicaciones_color_otro       CHECK (CHAR_LENGTH(TRIM(color_otro))       BETWEEN 1 AND 60),
    CONSTRAINT chk_publicaciones_transmision_otra CHECK (CHAR_LENGTH(TRIM(transmision_otra)) BETWEEN 1 AND 60),
    CONSTRAINT chk_publicaciones_carroceria_otra  CHECK (CHAR_LENGTH(TRIM(carroceria_otra))  BETWEEN 1 AND 60),

    -- descripcion_problemas solo cuando tiene_problemas = verdadero.
    CONSTRAINT chk_publicaciones_problemas
        CHECK (
            (tiene_problemas IS NOT TRUE AND descripcion_problemas IS NULL) OR
            (tiene_problemas IS TRUE AND COALESCE(CHAR_LENGTH(TRIM(descripcion_problemas)), 0) > 0)
        ),

    -- Campos "req. al aprobar": obligatorios desde la primera aprobación.
    CONSTRAINT chk_publicaciones_campos_aprobada
        CHECK (
            fecha_primera_publicacion IS NULL OR (
                titulo IS NOT NULL AND descripcion IS NOT NULL AND
                precio IS NOT NULL AND precio_negociable IS NOT NULL AND
                moneda IS NOT NULL AND estado_ubicacion IS NOT NULL AND
                municipio_ubicacion IS NOT NULL AND
                id_marca IS NOT NULL AND id_modelo IS NOT NULL AND
                anio IS NOT NULL AND kilometraje IS NOT NULL AND
                id_color IS NOT NULL AND id_transmision IS NOT NULL AND
                id_carroceria IS NOT NULL AND
                numero_propietarios_anteriores IS NOT NULL AND
                tiene_adeudos IS NOT NULL AND tiene_problemas IS NOT NULL AND
                recuperado_por_seguro IS NOT NULL
            )
        ),

    -- La última actualización aprobada solo existe después de la primera.
    CONSTRAINT chk_publicaciones_fechas_aprobacion
        CHECK (fecha_ultima_actualizacion_aprobada IS NULL OR fecha_primera_publicacion IS NOT NULL)
);

CREATE INDEX ix_publicaciones_id_propietario     ON public.publicaciones (id_propietario);
CREATE INDEX ix_publicaciones_id_marca           ON public.publicaciones (id_marca);
CREATE INDEX ix_publicaciones_id_modelo_id_marca ON public.publicaciones (id_modelo, id_marca);
CREATE INDEX ix_publicaciones_id_color           ON public.publicaciones (id_color);
CREATE INDEX ix_publicaciones_id_transmision     ON public.publicaciones (id_transmision);
CREATE INDEX ix_publicaciones_id_carroceria      ON public.publicaciones (id_carroceria);


-- ----------------------------------------------------------------------------
-- propuestas_publicacion — contenido enviado a moderación.
-- Máximo una editable (pendiente | rechazada) por publicación; las aprobadas
-- se conservan como evidencia.
-- ----------------------------------------------------------------------------
CREATE TABLE public.propuestas_publicacion (
    id_propuesta                      UUID           PRIMARY KEY DEFAULT gen_random_uuid(),
    id_publicacion                    UUID           NOT NULL,

    titulo                            VARCHAR(120)   NOT NULL,
    descripcion                       TEXT           NOT NULL,
    precio                            NUMERIC(12,2)  NOT NULL,
    precio_negociable                 BOOLEAN        NOT NULL,
    moneda                            CHAR(3)        NOT NULL,
    estado_ubicacion                  VARCHAR(60)    NOT NULL,
    municipio_ubicacion               VARCHAR(100)   NOT NULL,
    horario_preferido_contacto        VARCHAR(120),
    ruta_video                        TEXT,

    id_marca                          BIGINT         NOT NULL,
    marca_otra                        VARCHAR(60),
    id_modelo                         BIGINT         NOT NULL,
    modelo_otro                       VARCHAR(60),
    anio                              SMALLINT       NOT NULL,
    kilometraje                       INTEGER        NOT NULL,
    id_color                          BIGINT         NOT NULL,
    color_otro                        VARCHAR(60),
    id_transmision                    BIGINT         NOT NULL,
    transmision_otra                  VARCHAR(60),
    id_carroceria                     BIGINT         NOT NULL,
    carroceria_otra                   VARCHAR(60),

    numero_propietarios_anteriores    SMALLINT       NOT NULL,
    tiene_adeudos                     BOOLEAN        NOT NULL,
    tiene_problemas                   BOOLEAN        NOT NULL,
    descripcion_problemas             TEXT,
    recuperado_por_seguro             BOOLEAN        NOT NULL,

    tipo_combustible                  VARCHAR(40),
    numero_puertas                    SMALLINT,
    numero_pasajeros                  SMALLINT,
    cilindros                         SMALLINT,
    cilindrada                        VARCHAR(30),
    tipo_traccion                     VARCHAR(40),
    modificaciones                    TEXT,

    estado_propuesta                  VARCHAR(16)    NOT NULL DEFAULT 'pendiente',
    motivo_rechazo                    TEXT,
    id_admin_revisor                  UUID,
    fecha_envio_moderacion            TIMESTAMPTZ    NOT NULL DEFAULT NOW(),
    fecha_revision                    TIMESTAMPTZ,
    fecha_ultima_edicion_propietario  TIMESTAMPTZ    NOT NULL DEFAULT NOW(),

    -- Relaciones
    CONSTRAINT fk_propuestas_publicacion_publicaciones
        FOREIGN KEY (id_publicacion) REFERENCES public.publicaciones (id_publicacion) ON DELETE RESTRICT,
    CONSTRAINT fk_propuestas_publicacion_marcas
        FOREIGN KEY (id_marca) REFERENCES public.marcas (id_marca) ON DELETE RESTRICT,
    CONSTRAINT fk_propuestas_publicacion_modelos
        FOREIGN KEY (id_modelo, id_marca) REFERENCES public.modelos (id_modelo, id_marca) ON DELETE RESTRICT,
    CONSTRAINT fk_propuestas_publicacion_colores
        FOREIGN KEY (id_color) REFERENCES public.colores (id_color) ON DELETE RESTRICT,
    CONSTRAINT fk_propuestas_publicacion_transmisiones
        FOREIGN KEY (id_transmision) REFERENCES public.transmisiones (id_transmision) ON DELETE RESTRICT,
    CONSTRAINT fk_propuestas_publicacion_carrocerias
        FOREIGN KEY (id_carroceria) REFERENCES public.carrocerias (id_carroceria) ON DELETE RESTRICT,
    CONSTRAINT fk_propuestas_publicacion_admin_revisor
        FOREIGN KEY (id_admin_revisor) REFERENCES public.cuentas (id_cuenta) ON DELETE RESTRICT,

    -- Estado de moderación
    CONSTRAINT chk_propuestas_publicacion_estado
        CHECK (estado_propuesta IN ('pendiente', 'rechazada', 'aprobada')),
    CONSTRAINT chk_propuestas_publicacion_rechazo
        CHECK (estado_propuesta <> 'rechazada' OR COALESCE(CHAR_LENGTH(TRIM(motivo_rechazo)), 0) > 0),
    CONSTRAINT chk_propuestas_publicacion_revision
        CHECK (
            estado_propuesta = 'pendiente' OR
            (id_admin_revisor IS NOT NULL AND fecha_revision IS NOT NULL)
        ),

    -- Dominios de valores
    CONSTRAINT chk_propuestas_publicacion_moneda      CHECK (moneda IN ('MXN', 'USD')),
    CONSTRAINT chk_propuestas_publicacion_precio      CHECK (precio > 0),
    CONSTRAINT chk_propuestas_publicacion_kilometraje CHECK (kilometraje >= 0),
    CONSTRAINT chk_propuestas_publicacion_propietarios_anteriores CHECK (numero_propietarios_anteriores >= 0),
    CONSTRAINT chk_propuestas_publicacion_numero_puertas   CHECK (numero_puertas >= 0),
    CONSTRAINT chk_propuestas_publicacion_numero_pasajeros CHECK (numero_pasajeros >= 0),
    CONSTRAINT chk_propuestas_publicacion_cilindros        CHECK (cilindros >= 0),
    CONSTRAINT chk_propuestas_publicacion_titulo           CHECK (CHAR_LENGTH(TRIM(titulo)) > 0),
    CONSTRAINT chk_propuestas_publicacion_descripcion      CHECK (CHAR_LENGTH(TRIM(descripcion)) > 0),
    CONSTRAINT chk_propuestas_publicacion_estado_ubicacion CHECK (CHAR_LENGTH(TRIM(estado_ubicacion)) > 0),
    CONSTRAINT chk_propuestas_publicacion_municipio_ubicacion CHECK (CHAR_LENGTH(TRIM(municipio_ubicacion)) > 0),

    CONSTRAINT chk_propuestas_publicacion_marca_otra       CHECK (CHAR_LENGTH(TRIM(marca_otra))       BETWEEN 1 AND 60),
    CONSTRAINT chk_propuestas_publicacion_modelo_otro      CHECK (CHAR_LENGTH(TRIM(modelo_otro))      BETWEEN 1 AND 60),
    CONSTRAINT chk_propuestas_publicacion_color_otro       CHECK (CHAR_LENGTH(TRIM(color_otro))       BETWEEN 1 AND 60),
    CONSTRAINT chk_propuestas_publicacion_transmision_otra CHECK (CHAR_LENGTH(TRIM(transmision_otra)) BETWEEN 1 AND 60),
    CONSTRAINT chk_propuestas_publicacion_carroceria_otra  CHECK (CHAR_LENGTH(TRIM(carroceria_otra))  BETWEEN 1 AND 60),

    CONSTRAINT chk_propuestas_publicacion_problemas
        CHECK (
            (NOT tiene_problemas AND descripcion_problemas IS NULL) OR
            (tiene_problemas AND COALESCE(CHAR_LENGTH(TRIM(descripcion_problemas)), 0) > 0)
        )
);

CREATE INDEX ix_propuestas_publicacion_id_admin_revisor     ON public.propuestas_publicacion (id_admin_revisor);
CREATE INDEX ix_propuestas_publicacion_id_marca             ON public.propuestas_publicacion (id_marca);
CREATE INDEX ix_propuestas_publicacion_id_modelo_id_marca   ON public.propuestas_publicacion (id_modelo, id_marca);
CREATE INDEX ix_propuestas_publicacion_id_color             ON public.propuestas_publicacion (id_color);
CREATE INDEX ix_propuestas_publicacion_id_transmision       ON public.propuestas_publicacion (id_transmision);
CREATE INDEX ix_propuestas_publicacion_id_carroceria        ON public.propuestas_publicacion (id_carroceria);

-- Máximo una propuesta editable (pendiente o rechazada) por publicación.
CREATE UNIQUE INDEX uq_propuestas_editable
    ON public.propuestas_publicacion (id_publicacion)
    WHERE estado_propuesta IN ('pendiente', 'rechazada');


-- ############################################################################
-- 5. FOTOGRAFÍAS
-- ############################################################################

-- Galería aprobada. Las fotos retiradas se conservan (activa = false).
-- La principal es la activa con menor orden_foto.
CREATE TABLE public.fotos_publicacion (
    id_foto         UUID         PRIMARY KEY DEFAULT gen_random_uuid(),
    id_publicacion  UUID         NOT NULL,
    ruta_storage    TEXT         NOT NULL,
    orden_foto      SMALLINT     NOT NULL,
    activa          BOOLEAN      NOT NULL DEFAULT TRUE,
    fecha_retiro    TIMESTAMPTZ,

    CONSTRAINT fk_fotos_publicacion_publicaciones
        FOREIGN KEY (id_publicacion) REFERENCES public.publicaciones (id_publicacion) ON DELETE RESTRICT,

    CONSTRAINT chk_fotos_publicacion_ruta  CHECK (CHAR_LENGTH(TRIM(ruta_storage)) > 0),
    CONSTRAINT chk_fotos_publicacion_orden CHECK (orden_foto > 0),

    -- Activa ⇔ sin fecha de retiro.
    CONSTRAINT chk_fotos_publicacion_retiro
        CHECK ((activa AND fecha_retiro IS NULL) OR (NOT activa AND fecha_retiro IS NOT NULL))
);

-- El orden no se repite entre las fotos activas de una publicación.
CREATE UNIQUE INDEX uq_fotos_publicacion_orden_activa
    ON public.fotos_publicacion (id_publicacion, orden_foto)
    WHERE activa;


-- Galería de una propuesta.
CREATE TABLE public.fotos_propuesta_publicacion (
    id_foto_propuesta  UUID      PRIMARY KEY DEFAULT gen_random_uuid(),
    id_propuesta       UUID      NOT NULL,
    ruta_storage       TEXT      NOT NULL,
    orden_foto         SMALLINT  NOT NULL,

    CONSTRAINT fk_fotos_propuesta_publicacion_propuestas
        FOREIGN KEY (id_propuesta) REFERENCES public.propuestas_publicacion (id_propuesta) ON DELETE RESTRICT,

    CONSTRAINT uq_fotos_propuesta_publicacion_id_propuesta_orden_foto
        UNIQUE (id_propuesta, orden_foto),

    CONSTRAINT chk_fotos_propuesta_publicacion_ruta  CHECK (CHAR_LENGTH(TRIM(ruta_storage)) > 0),
    CONSTRAINT chk_fotos_propuesta_publicacion_orden CHECK (orden_foto > 0)
);


-- ############################################################################
-- 6. INTERACCIÓN
-- ############################################################################

-- Borrado físico permitido (quitar de favoritos).
CREATE TABLE public.favoritos (
    id_cuenta       UUID         NOT NULL,
    id_publicacion  UUID         NOT NULL,
    fecha_guardado  TIMESTAMPTZ  NOT NULL DEFAULT NOW(),

    CONSTRAINT pk_favoritos PRIMARY KEY (id_cuenta, id_publicacion),

    CONSTRAINT fk_favoritos_cuentas
        FOREIGN KEY (id_cuenta) REFERENCES public.cuentas (id_cuenta) ON DELETE RESTRICT,
    CONSTRAINT fk_favoritos_publicaciones
        FOREIGN KEY (id_publicacion) REFERENCES public.publicaciones (id_publicacion) ON DELETE RESTRICT
);

CREATE INDEX ix_favoritos_id_publicacion ON public.favoritos (id_publicacion);


-- Un reporte por usuario y publicación.
CREATE TABLE public.reportes (
    id_reporte          UUID         PRIMARY KEY DEFAULT gen_random_uuid(),
    id_publicacion      UUID         NOT NULL,
    id_reportante       UUID         NOT NULL,
    motivo_reporte      TEXT         NOT NULL,
    estado_reporte      VARCHAR(12)  NOT NULL DEFAULT 'pendiente',
    id_admin_resolutor  UUID,
    motivo_resolucion   TEXT,
    fecha_reporte       TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    fecha_resolucion    TIMESTAMPTZ,

    CONSTRAINT fk_reportes_publicaciones
        FOREIGN KEY (id_publicacion) REFERENCES public.publicaciones (id_publicacion) ON DELETE RESTRICT,
    CONSTRAINT fk_reportes_reportante
        FOREIGN KEY (id_reportante) REFERENCES public.cuentas (id_cuenta) ON DELETE RESTRICT,
    CONSTRAINT fk_reportes_admin_resolutor
        FOREIGN KEY (id_admin_resolutor) REFERENCES public.cuentas (id_cuenta) ON DELETE RESTRICT,

    CONSTRAINT uq_reportes_id_reportante_id_publicacion UNIQUE (id_reportante, id_publicacion),

    CONSTRAINT chk_reportes_estado
        CHECK (estado_reporte IN ('pendiente', 'atendido', 'descartado')),
    CONSTRAINT chk_reportes_motivo
        CHECK (CHAR_LENGTH(TRIM(motivo_reporte)) > 0),

    -- Pendiente: sin resolución. Atendido/descartado: resolución completa.
    CONSTRAINT chk_reportes_resolucion
        CHECK (
            (estado_reporte = 'pendiente'
                AND id_admin_resolutor IS NULL AND motivo_resolucion IS NULL AND fecha_resolucion IS NULL)
            OR
            (estado_reporte IN ('atendido', 'descartado')
                AND id_admin_resolutor IS NOT NULL AND fecha_resolucion IS NOT NULL
                AND COALESCE(CHAR_LENGTH(TRIM(motivo_resolucion)), 0) > 0)
        )
);

CREATE INDEX ix_reportes_id_publicacion     ON public.reportes (id_publicacion);
CREATE INDEX ix_reportes_id_admin_resolutor ON public.reportes (id_admin_resolutor);


-- Solicitudes para destacar una publicación durante 1 mes.
CREATE TABLE public.solicitudes_destacado (
    id_solicitud_destacado  UUID         PRIMARY KEY DEFAULT gen_random_uuid(),
    id_publicacion          UUID         NOT NULL,
    fecha_solicitud         TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    estado_solicitud        VARCHAR(12)  NOT NULL DEFAULT 'pendiente',
    id_admin_resuelve       UUID,
    motivo_rechazo          TEXT,
    fecha_resolucion        TIMESTAMPTZ,
    fecha_inicio_destacado  TIMESTAMPTZ,
    fecha_fin_destacado     TIMESTAMPTZ,
    fecha_cancelacion       TIMESTAMPTZ,

    CONSTRAINT fk_solicitudes_destacado_publicaciones
        FOREIGN KEY (id_publicacion) REFERENCES public.publicaciones (id_publicacion) ON DELETE RESTRICT,
    CONSTRAINT fk_solicitudes_destacado_admin_resuelve
        FOREIGN KEY (id_admin_resuelve) REFERENCES public.cuentas (id_cuenta) ON DELETE RESTRICT,

    CONSTRAINT chk_solicitudes_destacado_estado
        CHECK (estado_solicitud IN ('pendiente', 'aprobada', 'rechazada', 'cancelada')),

    -- Campos que corresponden a cada estado.
    CONSTRAINT chk_solicitudes_destacado_campos
        CHECK (
            (estado_solicitud = 'pendiente'
                AND id_admin_resuelve IS NULL AND motivo_rechazo IS NULL
                AND fecha_resolucion IS NULL AND fecha_inicio_destacado IS NULL
                AND fecha_fin_destacado IS NULL AND fecha_cancelacion IS NULL)
            OR
            (estado_solicitud = 'aprobada'
                AND id_admin_resuelve IS NOT NULL AND motivo_rechazo IS NULL
                AND fecha_resolucion IS NOT NULL
                AND fecha_inicio_destacado IS NOT NULL
                AND fecha_fin_destacado = fecha_inicio_destacado + INTERVAL '1 month'
                AND fecha_cancelacion IS NULL)
            OR
            (estado_solicitud = 'rechazada'
                AND id_admin_resuelve IS NOT NULL
                AND COALESCE(CHAR_LENGTH(TRIM(motivo_rechazo)), 0) > 0
                AND fecha_resolucion IS NOT NULL
                AND fecha_inicio_destacado IS NULL AND fecha_fin_destacado IS NULL
                AND fecha_cancelacion IS NULL)
            OR
            (estado_solicitud = 'cancelada'
                AND id_admin_resuelve IS NULL AND motivo_rechazo IS NULL
                AND fecha_resolucion IS NULL AND fecha_inicio_destacado IS NULL
                AND fecha_fin_destacado IS NULL AND fecha_cancelacion IS NOT NULL)
        )
);

CREATE INDEX ix_solicitudes_destacado_id_admin_resuelve ON public.solicitudes_destacado (id_admin_resuelve);

-- Máximo una solicitud pendiente por publicación.
CREATE UNIQUE INDEX uq_solicitudes_pendiente
    ON public.solicitudes_destacado (id_publicacion)
    WHERE estado_solicitud = 'pendiente';


-- Eventos para el dashboard. No se registran interacciones del propietario.
CREATE TABLE public.interacciones_publicacion (
    id_interaccion    UUID         PRIMARY KEY DEFAULT gen_random_uuid(),
    id_publicacion    UUID         NOT NULL,
    id_cuenta         UUID,
    tipo_interaccion  VARCHAR(16)  NOT NULL,
    fecha_hora        TIMESTAMPTZ  NOT NULL DEFAULT NOW(),

    CONSTRAINT fk_interacciones_publicacion_publicaciones
        FOREIGN KEY (id_publicacion) REFERENCES public.publicaciones (id_publicacion) ON DELETE RESTRICT,
    CONSTRAINT fk_interacciones_publicacion_cuentas
        FOREIGN KEY (id_cuenta) REFERENCES public.cuentas (id_cuenta) ON DELETE RESTRICT,

    CONSTRAINT chk_interacciones_publicacion_tipo
        CHECK (tipo_interaccion IN ('ver_detalle', 'contactar')),

    -- id_cuenta nulo solo en ver_detalle de visitante; contactar requiere sesión.
    CONSTRAINT chk_interacciones_publicacion_contactar
        CHECK (tipo_interaccion <> 'contactar' OR id_cuenta IS NOT NULL)
);

CREATE INDEX ix_interacciones_publicacion_id_publicacion ON public.interacciones_publicacion (id_publicacion);
CREATE INDEX ix_interacciones_publicacion_id_cuenta      ON public.interacciones_publicacion (id_cuenta);


-- ############################################################################
-- 7. AUDITORÍA
-- ############################################################################

-- Bitácora inmutable. Exactamente un objetivo por acción.
CREATE TABLE public.acciones_administrativas (
    id_accion                        UUID         PRIMARY KEY DEFAULT gen_random_uuid(),
    id_admin                         UUID         NOT NULL,
    tipo_accion                      VARCHAR(60)  NOT NULL,
    motivo                           TEXT         NOT NULL,
    fecha_hora                       TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    id_cuenta_objetivo               UUID,
    id_publicacion_objetivo          UUID,
    id_propuesta_objetivo            UUID,
    id_reporte_objetivo              UUID,
    id_solicitud_destacado_objetivo  UUID,

    CONSTRAINT fk_acciones_administrativas_admin
        FOREIGN KEY (id_admin) REFERENCES public.cuentas (id_cuenta) ON DELETE RESTRICT,
    CONSTRAINT fk_acciones_administrativas_cuenta_objetivo
        FOREIGN KEY (id_cuenta_objetivo) REFERENCES public.cuentas (id_cuenta) ON DELETE RESTRICT,
    CONSTRAINT fk_acciones_administrativas_publicacion_objetivo
        FOREIGN KEY (id_publicacion_objetivo) REFERENCES public.publicaciones (id_publicacion) ON DELETE RESTRICT,
    CONSTRAINT fk_acciones_administrativas_propuesta_objetivo
        FOREIGN KEY (id_propuesta_objetivo) REFERENCES public.propuestas_publicacion (id_propuesta) ON DELETE RESTRICT,
    CONSTRAINT fk_acciones_administrativas_reporte_objetivo
        FOREIGN KEY (id_reporte_objetivo) REFERENCES public.reportes (id_reporte) ON DELETE RESTRICT,
    CONSTRAINT fk_acciones_administrativas_solicitud_objetivo
        FOREIGN KEY (id_solicitud_destacado_objetivo) REFERENCES public.solicitudes_destacado (id_solicitud_destacado) ON DELETE RESTRICT,

    CONSTRAINT chk_acciones_administrativas_tipo   CHECK (CHAR_LENGTH(TRIM(tipo_accion)) > 0),
    CONSTRAINT chk_acciones_administrativas_motivo CHECK (CHAR_LENGTH(TRIM(motivo)) > 0),

    CONSTRAINT chk_acciones_administrativas_un_objetivo
        CHECK (
            num_nonnulls(
                id_cuenta_objetivo, id_publicacion_objetivo, id_propuesta_objetivo,
                id_reporte_objetivo, id_solicitud_destacado_objetivo
            ) = 1
        )
);

CREATE INDEX ix_acciones_administrativas_id_admin                        ON public.acciones_administrativas (id_admin);
CREATE INDEX ix_acciones_administrativas_id_cuenta_objetivo              ON public.acciones_administrativas (id_cuenta_objetivo);
CREATE INDEX ix_acciones_administrativas_id_publicacion_objetivo         ON public.acciones_administrativas (id_publicacion_objetivo);
CREATE INDEX ix_acciones_administrativas_id_propuesta_objetivo           ON public.acciones_administrativas (id_propuesta_objetivo);
CREATE INDEX ix_acciones_administrativas_id_reporte_objetivo             ON public.acciones_administrativas (id_reporte_objetivo);
CREATE INDEX ix_acciones_administrativas_id_solicitud_destacado_objetivo ON public.acciones_administrativas (id_solicitud_destacado_objetivo);


-- ############################################################################
-- 8. FUNCIONES Y TRIGGERS DE REGLAS DE NEGOCIO
-- ############################################################################
-- Reglas que un CHECK no puede expresar porque consultan otra tabla o
-- dependen del valor anterior de la fila.

-- ----------------------------------------------------------------------------
-- Función auxiliar: ¿la cuenta tiene el rol administrador?
-- Resuelve el rol por nombre, no por ID. Servirá también en las políticas RLS.
-- ----------------------------------------------------------------------------
CREATE FUNCTION public.es_administrador(p_id_cuenta UUID)
RETURNS BOOLEAN
LANGUAGE sql
STABLE
SECURITY DEFINER
SET search_path = ''
AS $$
    SELECT EXISTS (
        SELECT 1
        FROM public.cuentas_roles cr
        JOIN public.roles r ON r.id_rol = cr.id_rol
        WHERE cr.id_cuenta = p_id_cuenta
          AND r.nombre_rol = 'administrador'
    );
$$;


-- ----------------------------------------------------------------------------
-- cuentas: fechas de suspensión y reactivación automáticas.
-- ----------------------------------------------------------------------------
CREATE FUNCTION public.fn_cuentas_fechas_estado()
RETURNS TRIGGER
LANGUAGE plpgsql
AS $$
BEGIN
    IF TG_OP = 'INSERT' THEN
        IF NEW.estado_cuenta = 'suspendida' AND NEW.fecha_suspension_inicio IS NULL THEN
            NEW.fecha_suspension_inicio := NOW();
        END IF;
        RETURN NEW;
    END IF;

    IF NEW.estado_cuenta IS DISTINCT FROM OLD.estado_cuenta THEN
        IF NEW.estado_cuenta = 'suspendida' THEN
            NEW.fecha_suspension_inicio := NOW();
        ELSIF NEW.estado_cuenta = 'activa' THEN
            NEW.fecha_suspension_inicio := NULL;
            NEW.fecha_reactivacion := NOW();
        END IF;
    END IF;

    -- El tipo de cuenta no cambia después del registro.
    IF NEW.tipo_cuenta IS DISTINCT FROM OLD.tipo_cuenta THEN
        RAISE EXCEPTION 'El tipo de cuenta no puede modificarse.';
    END IF;

    RETURN NEW;
END;
$$;

CREATE TRIGGER trg_cuentas_fechas_estado
    BEFORE INSERT OR UPDATE ON public.cuentas
    FOR EACH ROW EXECUTE FUNCTION public.fn_cuentas_fechas_estado();


-- ----------------------------------------------------------------------------
-- cuentas: toda cuenta nueva recibe el rol 'usuario'.
-- ----------------------------------------------------------------------------
CREATE FUNCTION public.fn_cuentas_rol_inicial()
RETURNS TRIGGER
LANGUAGE plpgsql
SECURITY DEFINER
SET search_path = ''
AS $$
BEGIN
    INSERT INTO public.cuentas_roles (id_cuenta, id_rol)
    SELECT NEW.id_cuenta, r.id_rol
    FROM public.roles r
    WHERE r.nombre_rol = 'usuario';

    IF NOT FOUND THEN
        RAISE EXCEPTION 'No existe el rol "usuario".';
    END IF;

    RETURN NEW;
END;
$$;

CREATE TRIGGER trg_cuentas_rol_inicial
    AFTER INSERT ON public.cuentas
    FOR EACH ROW EXECUTE FUNCTION public.fn_cuentas_rol_inicial();


-- ----------------------------------------------------------------------------
-- perfiles_lote: solo para cuentas de tipo lote.
-- ----------------------------------------------------------------------------
CREATE FUNCTION public.fn_perfiles_lote_validar_tipo()
RETURNS TRIGGER
LANGUAGE plpgsql
AS $$
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM public.cuentas
        WHERE id_cuenta = NEW.id_cuenta AND tipo_cuenta = 'lote'
    ) THEN
        RAISE EXCEPTION 'Solo una cuenta de tipo lote puede tener perfil de lote.';
    END IF;
    RETURN NEW;
END;
$$;

CREATE TRIGGER trg_perfiles_lote_validar_tipo
    BEFORE INSERT OR UPDATE OF id_cuenta ON public.perfiles_lote
    FOR EACH ROW EXECUTE FUNCTION public.fn_perfiles_lote_validar_tipo();


-- ----------------------------------------------------------------------------
-- cuentas_roles: el rol administrador solo se asigna a cuentas lote.
-- ----------------------------------------------------------------------------
CREATE FUNCTION public.fn_cuentas_roles_validar_admin()
RETURNS TRIGGER
LANGUAGE plpgsql
AS $$
BEGIN
    IF EXISTS (SELECT 1 FROM public.roles WHERE id_rol = NEW.id_rol AND nombre_rol = 'administrador')
       AND NOT EXISTS (SELECT 1 FROM public.cuentas WHERE id_cuenta = NEW.id_cuenta AND tipo_cuenta = 'lote')
    THEN
        RAISE EXCEPTION 'El rol administrador solo puede asignarse a cuentas de tipo lote.';
    END IF;
    RETURN NEW;
END;
$$;

CREATE TRIGGER trg_cuentas_roles_validar_admin
    BEFORE INSERT OR UPDATE ON public.cuentas_roles
    FOR EACH ROW EXECUTE FUNCTION public.fn_cuentas_roles_validar_admin();


-- ----------------------------------------------------------------------------
-- publicaciones y propuestas: texto "Otros" obligatorio solo si el valor de
-- catálogo elegido tiene es_otro = true; en otro caso debe quedar nulo.
-- ----------------------------------------------------------------------------
CREATE FUNCTION public.fn_validar_catalogo_otros()
RETURNS TRIGGER
LANGUAGE plpgsql
AS $$
DECLARE
    v_es_otro BOOLEAN;
BEGIN
    -- Marca
    SELECT es_otro INTO v_es_otro FROM public.marcas WHERE id_marca = NEW.id_marca;
    IF COALESCE(v_es_otro, FALSE) <> (NEW.marca_otra IS NOT NULL) THEN
        RAISE EXCEPTION 'marca_otra es obligatoria solo cuando la marca elegida es "Otros".';
    END IF;

    -- Modelo
    SELECT es_otro INTO v_es_otro FROM public.modelos WHERE id_modelo = NEW.id_modelo;
    IF COALESCE(v_es_otro, FALSE) <> (NEW.modelo_otro IS NOT NULL) THEN
        RAISE EXCEPTION 'modelo_otro es obligatorio solo cuando el modelo elegido es "Otros".';
    END IF;

    -- Color
    SELECT es_otro INTO v_es_otro FROM public.colores WHERE id_color = NEW.id_color;
    IF COALESCE(v_es_otro, FALSE) <> (NEW.color_otro IS NOT NULL) THEN
        RAISE EXCEPTION 'color_otro es obligatorio solo cuando el color elegido es "Otros".';
    END IF;

    -- Transmisión
    SELECT es_otro INTO v_es_otro FROM public.transmisiones WHERE id_transmision = NEW.id_transmision;
    IF COALESCE(v_es_otro, FALSE) <> (NEW.transmision_otra IS NOT NULL) THEN
        RAISE EXCEPTION 'transmision_otra es obligatoria solo cuando la transmisión elegida es "Otros".';
    END IF;

    -- Carrocería
    SELECT es_otro INTO v_es_otro FROM public.carrocerias WHERE id_carroceria = NEW.id_carroceria;
    IF COALESCE(v_es_otro, FALSE) <> (NEW.carroceria_otra IS NOT NULL) THEN
        RAISE EXCEPTION 'carroceria_otra es obligatoria solo cuando la carrocería elegida es "Otros".';
    END IF;

    RETURN NEW;
END;
$$;

CREATE TRIGGER trg_publicaciones_catalogo_otros
    BEFORE INSERT OR UPDATE ON public.publicaciones
    FOR EACH ROW EXECUTE FUNCTION public.fn_validar_catalogo_otros();

CREATE TRIGGER trg_propuestas_publicacion_catalogo_otros
    BEFORE INSERT OR UPDATE ON public.propuestas_publicacion
    FOR EACH ROW EXECUTE FUNCTION public.fn_validar_catalogo_otros();


-- ----------------------------------------------------------------------------
-- Revisores y resolutores deben tener el rol administrador.
-- ----------------------------------------------------------------------------
CREATE FUNCTION public.fn_validar_admin_columna()
RETURNS TRIGGER
LANGUAGE plpgsql
AS $$
DECLARE
    v_id_admin UUID;
BEGIN
    -- TG_ARGV[0] = nombre de la columna que contiene al administrador.
    v_id_admin := (to_jsonb(NEW) ->> TG_ARGV[0])::UUID;

    IF v_id_admin IS NOT NULL AND NOT public.es_administrador(v_id_admin) THEN
        RAISE EXCEPTION 'La cuenta indicada en % no tiene el rol administrador.', TG_ARGV[0];
    END IF;
    RETURN NEW;
END;
$$;

CREATE TRIGGER trg_propuestas_publicacion_admin
    BEFORE INSERT OR UPDATE OF id_admin_revisor ON public.propuestas_publicacion
    FOR EACH ROW EXECUTE FUNCTION public.fn_validar_admin_columna('id_admin_revisor');

CREATE TRIGGER trg_reportes_admin
    BEFORE INSERT OR UPDATE OF id_admin_resolutor ON public.reportes
    FOR EACH ROW EXECUTE FUNCTION public.fn_validar_admin_columna('id_admin_resolutor');

CREATE TRIGGER trg_solicitudes_destacado_admin
    BEFORE INSERT OR UPDATE OF id_admin_resuelve ON public.solicitudes_destacado
    FOR EACH ROW EXECUTE FUNCTION public.fn_validar_admin_columna('id_admin_resuelve');

CREATE TRIGGER trg_acciones_administrativas_admin
    BEFORE INSERT ON public.acciones_administrativas
    FOR EACH ROW EXECUTE FUNCTION public.fn_validar_admin_columna('id_admin');


-- ----------------------------------------------------------------------------
-- solicitudes_destacado: solo se cancela o resuelve desde pendiente.
-- ----------------------------------------------------------------------------
CREATE FUNCTION public.fn_solicitudes_destacado_transicion()
RETURNS TRIGGER
LANGUAGE plpgsql
AS $$
BEGIN
    IF OLD.estado_solicitud <> 'pendiente' THEN
        RAISE EXCEPTION 'Solo una solicitud pendiente puede cancelarse o resolverse.';
    END IF;
    RETURN NEW;
END;
$$;

CREATE TRIGGER trg_solicitudes_destacado_transicion
    BEFORE UPDATE ON public.solicitudes_destacado
    FOR EACH ROW EXECUTE FUNCTION public.fn_solicitudes_destacado_transicion();


-- ----------------------------------------------------------------------------
-- fotos_publicacion: fecha de retiro automática y sin reactivación.
-- ----------------------------------------------------------------------------
CREATE FUNCTION public.fn_fotos_publicacion_retiro()
RETURNS TRIGGER
LANGUAGE plpgsql
AS $$
BEGIN
    IF OLD.activa AND NOT NEW.activa THEN
        NEW.fecha_retiro := NOW();
    ELSIF NOT OLD.activa AND NEW.activa THEN
        RAISE EXCEPTION 'Una foto retirada no se reactiva; registre una foto nueva.';
    END IF;
    RETURN NEW;
END;
$$;

CREATE TRIGGER trg_fotos_publicacion_retiro
    BEFORE UPDATE OF activa ON public.fotos_publicacion
    FOR EACH ROW EXECUTE FUNCTION public.fn_fotos_publicacion_retiro();


-- ----------------------------------------------------------------------------
-- Máximo 15 fotos (activas en publicaciones; todas en propuestas).
-- El mínimo de 5 se valida al enviar a moderación y al aprobar, porque
-- durante la captura la galería se arma foto por foto.
-- ----------------------------------------------------------------------------
CREATE FUNCTION public.fn_fotos_publicacion_maximo()
RETURNS TRIGGER
LANGUAGE plpgsql
AS $$
BEGIN
    IF NEW.activa AND (
        SELECT COUNT(*) FROM public.fotos_publicacion
        WHERE id_publicacion = NEW.id_publicacion AND activa
          AND id_foto <> NEW.id_foto
    ) >= 15 THEN
        RAISE EXCEPTION 'Una publicación no puede tener más de 15 fotos activas.';
    END IF;
    RETURN NEW;
END;
$$;

CREATE TRIGGER trg_fotos_publicacion_maximo
    BEFORE INSERT OR UPDATE OF activa, id_publicacion ON public.fotos_publicacion
    FOR EACH ROW EXECUTE FUNCTION public.fn_fotos_publicacion_maximo();


CREATE FUNCTION public.fn_fotos_propuesta_maximo()
RETURNS TRIGGER
LANGUAGE plpgsql
AS $$
BEGIN
    IF (
        SELECT COUNT(*) FROM public.fotos_propuesta_publicacion
        WHERE id_propuesta = NEW.id_propuesta
          AND id_foto_propuesta <> NEW.id_foto_propuesta
    ) >= 15 THEN
        RAISE EXCEPTION 'Una propuesta no puede tener más de 15 fotos.';
    END IF;
    RETURN NEW;
END;
$$;

CREATE TRIGGER trg_fotos_propuesta_maximo
    BEFORE INSERT OR UPDATE OF id_propuesta ON public.fotos_propuesta_publicacion
    FOR EACH ROW EXECUTE FUNCTION public.fn_fotos_propuesta_maximo();


-- ----------------------------------------------------------------------------
-- interacciones_publicacion: no se registran las del propietario.
-- Se descarta el evento en silencio para no romper el flujo de la app.
-- ----------------------------------------------------------------------------
CREATE FUNCTION public.fn_interacciones_ignorar_propietario()
RETURNS TRIGGER
LANGUAGE plpgsql
AS $$
BEGIN
    IF NEW.id_cuenta IS NOT NULL AND EXISTS (
        SELECT 1 FROM public.publicaciones
        WHERE id_publicacion = NEW.id_publicacion
          AND id_propietario = NEW.id_cuenta
    ) THEN
        RETURN NULL;
    END IF;
    RETURN NEW;
END;
$$;

CREATE TRIGGER trg_interacciones_ignorar_propietario
    BEFORE INSERT ON public.interacciones_publicacion
    FOR EACH ROW EXECUTE FUNCTION public.fn_interacciones_ignorar_propietario();


-- ----------------------------------------------------------------------------
-- acciones_administrativas: bitácora inmutable (sin UPDATE ni DELETE).
-- ----------------------------------------------------------------------------
CREATE FUNCTION public.fn_acciones_administrativas_inmutable()
RETURNS TRIGGER
LANGUAGE plpgsql
AS $$
BEGIN
    RAISE EXCEPTION 'La bitácora administrativa es inmutable.';
END;
$$;

CREATE TRIGGER trg_acciones_administrativas_inmutable
    BEFORE UPDATE OR DELETE ON public.acciones_administrativas
    FOR EACH ROW EXECUTE FUNCTION public.fn_acciones_administrativas_inmutable();


-- ############################################################################
-- 9. RLS
-- ############################################################################
-- Se habilita en todas las tablas para no depender de la opción del
-- Dashboard. Sin políticas, nadie (excepto service_role) puede leer ni
-- escribir: las políticas se agregan en la siguiente migración.

ALTER TABLE public.cuentas                     ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.perfiles_lote               ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.telefonos_contacto          ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.roles                       ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.cuentas_roles               ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.marcas                      ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.modelos                     ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.carrocerias                 ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.transmisiones               ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.colores                     ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.publicaciones               ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.propuestas_publicacion      ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.fotos_publicacion           ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.fotos_propuesta_publicacion ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.favoritos                   ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.reportes                    ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.solicitudes_destacado       ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.interacciones_publicacion   ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.acciones_administrativas    ENABLE ROW LEVEL SECURITY;


-- ############################################################################
-- 10. COMENTARIOS (alimentan el diccionario de datos generado del catálogo)
-- ############################################################################

COMMENT ON TABLE public.cuentas IS 'Representa la cuenta de dominio de Karsy vinculada 1:1 con una identidad de Supabase Auth. Las credenciales y el correo de autenticación permanecen en Auth.';
COMMENT ON COLUMN public.cuentas.id_cuenta IS 'Identificador único de la cuenta Karsy; coincide con la identidad de Supabase Auth.';
COMMENT ON COLUMN public.cuentas.tipo_cuenta IS 'Clasifica la cuenta según el tipo de anunciante.';
COMMENT ON COLUMN public.cuentas.nombre_mostrar IS 'Nombre visible del usuario o cuenta.';
COMMENT ON COLUMN public.cuentas.foto_perfil IS 'Ruta estable de la imagen de perfil en Supabase Storage.';
COMMENT ON COLUMN public.cuentas.descripcion_corta IS 'Descripción breve del perfil público.';
COMMENT ON COLUMN public.cuentas.estado_perfil_ubi IS 'Estado de residencia o referencia del perfil general.';
COMMENT ON COLUMN public.cuentas.municipio_perfil_ubi IS 'Municipio del perfil general.';
COMMENT ON COLUMN public.cuentas.estado_cuenta IS 'Estado operativo actual de la cuenta.';
COMMENT ON COLUMN public.cuentas.fecha_creacion IS 'Momento en que se creó la cuenta de dominio en Karsy.';
COMMENT ON COLUMN public.cuentas.fecha_suspension_inicio IS 'Inicio de la suspensión vigente.';
COMMENT ON COLUMN public.cuentas.fecha_reactivacion IS 'Momento de la reactivación más reciente.';

COMMENT ON TABLE public.perfiles_lote IS 'Almacena la información adicional exclusiva de una cuenta de tipo lote y su dirección comercial pública.';
COMMENT ON COLUMN public.perfiles_lote.id_cuenta IS 'Cuenta propietaria del perfil comercial.';
COMMENT ON COLUMN public.perfiles_lote.nombre_comercial IS 'Nombre comercial mostrado para el lote.';
COMMENT ON COLUMN public.perfiles_lote.logo_lote IS 'Ruta estable del logotipo del lote.';
COMMENT ON COLUMN public.perfiles_lote.descripcion_lote IS 'Descripción pública del negocio.';
COMMENT ON COLUMN public.perfiles_lote.calle IS 'Calle del establecimiento.';
COMMENT ON COLUMN public.perfiles_lote.numero IS 'Número exterior o identificador equivalente.';
COMMENT ON COLUMN public.perfiles_lote.colonia IS 'Colonia del establecimiento.';
COMMENT ON COLUMN public.perfiles_lote.codigo_postal IS 'Código postal del establecimiento.';
COMMENT ON COLUMN public.perfiles_lote.municipio IS 'Municipio del establecimiento.';
COMMENT ON COLUMN public.perfiles_lote.estado IS 'Estado donde se ubica el establecimiento.';
COMMENT ON COLUMN public.perfiles_lote.horarios_atencion IS 'Horario comercial visible cuando el lote decide proporcionarlo.';

COMMENT ON TABLE public.telefonos_contacto IS 'Registra uno o varios números por cuenta e indica si cada número admite llamadas, WhatsApp o ambas formas de contacto.';
COMMENT ON COLUMN public.telefonos_contacto.id_telefono IS 'Identificador del contacto telefónico.';
COMMENT ON COLUMN public.telefonos_contacto.id_cuenta IS 'Cuenta propietaria del número.';
COMMENT ON COLUMN public.telefonos_contacto.numero_telefono IS 'Número de contacto, con prefijo cuando aplique.';
COMMENT ON COLUMN public.telefonos_contacto.permite_llamadas IS 'Indica si el número admite llamadas.';
COMMENT ON COLUMN public.telefonos_contacto.es_whatsapp IS 'Indica si el mismo número admite WhatsApp.';
COMMENT ON COLUMN public.telefonos_contacto.es_principal IS 'Señala el número principal de la cuenta.';
COMMENT ON COLUMN public.telefonos_contacto.activo IS 'Indica si el contacto está habilitado para mostrarse y usarse.';
COMMENT ON INDEX public.uq_telefonos_contacto_principal IS 'Máximo un número principal activo por cuenta.';

COMMENT ON TABLE public.roles IS 'Catálogo de roles de la aplicación. La app consulta si la cuenta tiene el rol administrador para mostrar u ocultar las funciones administrativas. El tipo particular/lote se mantiene separado del rol.';
COMMENT ON COLUMN public.roles.id_rol IS 'Identificador interno del rol.';
COMMENT ON COLUMN public.roles.nombre_rol IS 'Nombre estable y único del rol.';
COMMENT ON COLUMN public.roles.descripcion_rol IS 'Explica el alcance general del rol.';

COMMENT ON TABLE public.cuentas_roles IS 'Asocia cuentas con los roles que tienen asignados.';
COMMENT ON COLUMN public.cuentas_roles.id_cuenta IS 'Cuenta que recibe el rol.';
COMMENT ON COLUMN public.cuentas_roles.id_rol IS 'Rol asignado.';
COMMENT ON COLUMN public.cuentas_roles.fecha_asignacion IS 'Momento en que se asignó el rol.';

COMMENT ON TABLE public.marcas IS 'Catálogo controlado de marcas de vehículos utilizado en captura, búsqueda y filtros.';
COMMENT ON COLUMN public.marcas.id_marca IS 'Identificador del valor de catálogo.';
COMMENT ON COLUMN public.marcas.nombre_marca IS 'Nombre mostrado en el selector.';
COMMENT ON COLUMN public.marcas.es_otro IS 'Indica si la fila representa la opción Otros.';
COMMENT ON INDEX public.uq_marcas_otros IS 'Solo una fila ''Otros''.';

COMMENT ON TABLE public.modelos IS 'Catálogo controlado de modelos dependientes de una marca.';
COMMENT ON COLUMN public.modelos.id_modelo IS 'Identificador del modelo.';
COMMENT ON COLUMN public.modelos.id_marca IS 'Marca a la que pertenece el modelo.';
COMMENT ON COLUMN public.modelos.nombre_modelo IS 'Nombre mostrado en el selector de modelos.';
COMMENT ON COLUMN public.modelos.es_otro IS 'Indica si representa Otros para esa marca.';
COMMENT ON INDEX public.uq_modelos_otros IS 'Máximo una fila ''Otros'' por marca.';

COMMENT ON TABLE public.carrocerias IS 'Catálogo controlado de tipos de carrocería.';
COMMENT ON COLUMN public.carrocerias.id_carroceria IS 'Identificador del valor de catálogo.';
COMMENT ON COLUMN public.carrocerias.nombre_carroceria IS 'Nombre mostrado en el selector.';
COMMENT ON COLUMN public.carrocerias.es_otro IS 'Indica si la fila representa la opción Otros.';
COMMENT ON INDEX public.uq_carrocerias_otros IS 'Solo una fila ''Otros''.';

COMMENT ON TABLE public.transmisiones IS 'Catálogo controlado de tipos de transmisión.';
COMMENT ON COLUMN public.transmisiones.id_transmision IS 'Identificador del valor de catálogo.';
COMMENT ON COLUMN public.transmisiones.nombre_transmision IS 'Nombre mostrado en el selector.';
COMMENT ON COLUMN public.transmisiones.es_otro IS 'Indica si la fila representa la opción Otros.';
COMMENT ON INDEX public.uq_transmisiones_otros IS 'Solo una fila ''Otros''.';

COMMENT ON TABLE public.colores IS 'Catálogo controlado de colores de vehículos.';
COMMENT ON COLUMN public.colores.id_color IS 'Identificador del valor de catálogo.';
COMMENT ON COLUMN public.colores.nombre_color IS 'Nombre mostrado en el selector.';
COMMENT ON COLUMN public.colores.es_otro IS 'Indica si la fila representa la opción Otros.';
COMMENT ON INDEX public.uq_colores_otros IS 'Solo una fila ''Otros''.';

COMMENT ON TABLE public.publicaciones IS 'Guarda el contenido público actualmente aprobado del anuncio y sus estados actuales. Antes de la primera aprobación puede existir una fila base con los campos públicos todavía nulos.';
COMMENT ON COLUMN public.publicaciones.id_publicacion IS 'Identificador estable del anuncio.';
COMMENT ON COLUMN public.publicaciones.id_propietario IS 'Cuenta propietaria del anuncio.';
COMMENT ON COLUMN public.publicaciones.titulo IS 'Título público del anuncio.';
COMMENT ON COLUMN public.publicaciones.descripcion IS 'Descripción pública del vehículo/anuncio.';
COMMENT ON COLUMN public.publicaciones.precio IS 'Precio actual del vehículo.';
COMMENT ON COLUMN public.publicaciones.precio_negociable IS 'Indica si se acepta negociación del precio.';
COMMENT ON COLUMN public.publicaciones.moneda IS 'Código de moneda del precio.';
COMMENT ON COLUMN public.publicaciones.estado_ubicacion IS 'Estado donde se encuentra u ofrece el vehículo.';
COMMENT ON COLUMN public.publicaciones.municipio_ubicacion IS 'Municipio donde se encuentra u ofrece el vehículo.';
COMMENT ON COLUMN public.publicaciones.horario_preferido_contacto IS 'Horario orientativo preferido para contacto.';
COMMENT ON COLUMN public.publicaciones.ruta_video IS 'Ruta estable del único video opcional.';
COMMENT ON COLUMN public.publicaciones.id_marca IS 'Marca del vehículo.';
COMMENT ON COLUMN public.publicaciones.marca_otra IS 'Texto libre para una marca fuera del catálogo.';
COMMENT ON COLUMN public.publicaciones.id_modelo IS 'Modelo del vehículo.';
COMMENT ON COLUMN public.publicaciones.modelo_otro IS 'Texto libre para un modelo fuera del catálogo.';
COMMENT ON COLUMN public.publicaciones.anio IS 'Año del vehículo.';
COMMENT ON COLUMN public.publicaciones.kilometraje IS 'Kilometraje actual.';
COMMENT ON COLUMN public.publicaciones.id_color IS 'Color del vehículo.';
COMMENT ON COLUMN public.publicaciones.color_otro IS 'Texto libre para color fuera del catálogo.';
COMMENT ON COLUMN public.publicaciones.id_transmision IS 'Transmisión del vehículo.';
COMMENT ON COLUMN public.publicaciones.transmision_otra IS 'Texto libre para transmisión fuera del catálogo.';
COMMENT ON COLUMN public.publicaciones.id_carroceria IS 'Carrocería del vehículo.';
COMMENT ON COLUMN public.publicaciones.carroceria_otra IS 'Texto libre para carrocería fuera del catálogo.';
COMMENT ON COLUMN public.publicaciones.numero_propietarios_anteriores IS 'Cantidad declarada de propietarios anteriores.';
COMMENT ON COLUMN public.publicaciones.tiene_adeudos IS 'Indica si el vehículo tiene adeudos.';
COMMENT ON COLUMN public.publicaciones.tiene_problemas IS 'Indica si el vehículo tiene problemas conocidos.';
COMMENT ON COLUMN public.publicaciones.descripcion_problemas IS 'Describe los problemas conocidos.';
COMMENT ON COLUMN public.publicaciones.recuperado_por_seguro IS 'Indica si fue recuperado por aseguradora.';
COMMENT ON COLUMN public.publicaciones.tipo_combustible IS 'Tipo de combustible.';
COMMENT ON COLUMN public.publicaciones.numero_puertas IS 'Número de puertas.';
COMMENT ON COLUMN public.publicaciones.numero_pasajeros IS 'Capacidad de pasajeros.';
COMMENT ON COLUMN public.publicaciones.cilindros IS 'Número de cilindros.';
COMMENT ON COLUMN public.publicaciones.cilindrada IS 'Cilindrada declarada.';
COMMENT ON COLUMN public.publicaciones.tipo_traccion IS 'Tipo de tracción declarado.';
COMMENT ON COLUMN public.publicaciones.modificaciones IS 'Describe modificaciones relevantes del vehículo.';
COMMENT ON COLUMN public.publicaciones.estado_publicacion IS 'Estado controlado por el propietario.';
COMMENT ON COLUMN public.publicaciones.estado_administrativo IS 'Estado controlado por administración.';
COMMENT ON COLUMN public.publicaciones.fecha_creacion IS 'Momento de alta de la identidad del anuncio.';
COMMENT ON COLUMN public.publicaciones.fecha_primera_publicacion IS 'Momento de la primera aprobación pública.';
COMMENT ON COLUMN public.publicaciones.fecha_ultima_actualizacion_aprobada IS 'Momento de la aprobación más reciente posterior a la primera.';

COMMENT ON TABLE public.propuestas_publicacion IS 'Guarda el contenido enviado a moderación para una publicación inicial o modificación sin sustituir la información pública vigente mientras está pendiente o rechazada.';
COMMENT ON COLUMN public.propuestas_publicacion.id_propuesta IS 'Identificador de la propuesta.';
COMMENT ON COLUMN public.propuestas_publicacion.id_publicacion IS 'Publicación estable a la que pertenece.';
COMMENT ON COLUMN public.propuestas_publicacion.titulo IS 'Título propuesto para el anuncio.';
COMMENT ON COLUMN public.propuestas_publicacion.descripcion IS 'Descripción propuesta del vehículo/anuncio.';
COMMENT ON COLUMN public.propuestas_publicacion.precio IS 'Precio propuesto del vehículo.';
COMMENT ON COLUMN public.propuestas_publicacion.precio_negociable IS 'Indica si se acepta negociación del precio.';
COMMENT ON COLUMN public.propuestas_publicacion.moneda IS 'Código de moneda del precio.';
COMMENT ON COLUMN public.propuestas_publicacion.estado_ubicacion IS 'Estado donde se encuentra u ofrece el vehículo.';
COMMENT ON COLUMN public.propuestas_publicacion.municipio_ubicacion IS 'Municipio donde se encuentra u ofrece el vehículo.';
COMMENT ON COLUMN public.propuestas_publicacion.horario_preferido_contacto IS 'Horario orientativo preferido para contacto.';
COMMENT ON COLUMN public.propuestas_publicacion.ruta_video IS 'Ruta estable del único video opcional.';
COMMENT ON COLUMN public.propuestas_publicacion.id_marca IS 'Marca del vehículo.';
COMMENT ON COLUMN public.propuestas_publicacion.marca_otra IS 'Texto libre para una marca fuera del catálogo.';
COMMENT ON COLUMN public.propuestas_publicacion.id_modelo IS 'Modelo del vehículo.';
COMMENT ON COLUMN public.propuestas_publicacion.modelo_otro IS 'Texto libre para un modelo fuera del catálogo.';
COMMENT ON COLUMN public.propuestas_publicacion.anio IS 'Año del vehículo.';
COMMENT ON COLUMN public.propuestas_publicacion.kilometraje IS 'Kilometraje actual.';
COMMENT ON COLUMN public.propuestas_publicacion.id_color IS 'Color del vehículo.';
COMMENT ON COLUMN public.propuestas_publicacion.color_otro IS 'Texto libre para color fuera del catálogo.';
COMMENT ON COLUMN public.propuestas_publicacion.id_transmision IS 'Transmisión del vehículo.';
COMMENT ON COLUMN public.propuestas_publicacion.transmision_otra IS 'Texto libre para transmisión fuera del catálogo.';
COMMENT ON COLUMN public.propuestas_publicacion.id_carroceria IS 'Carrocería del vehículo.';
COMMENT ON COLUMN public.propuestas_publicacion.carroceria_otra IS 'Texto libre para carrocería fuera del catálogo.';
COMMENT ON COLUMN public.propuestas_publicacion.numero_propietarios_anteriores IS 'Cantidad declarada de propietarios anteriores.';
COMMENT ON COLUMN public.propuestas_publicacion.tiene_adeudos IS 'Indica si el vehículo tiene adeudos.';
COMMENT ON COLUMN public.propuestas_publicacion.tiene_problemas IS 'Indica si el vehículo tiene problemas conocidos.';
COMMENT ON COLUMN public.propuestas_publicacion.descripcion_problemas IS 'Describe los problemas conocidos.';
COMMENT ON COLUMN public.propuestas_publicacion.recuperado_por_seguro IS 'Indica si fue recuperado por aseguradora.';
COMMENT ON COLUMN public.propuestas_publicacion.tipo_combustible IS 'Tipo de combustible.';
COMMENT ON COLUMN public.propuestas_publicacion.numero_puertas IS 'Número de puertas.';
COMMENT ON COLUMN public.propuestas_publicacion.numero_pasajeros IS 'Capacidad de pasajeros.';
COMMENT ON COLUMN public.propuestas_publicacion.cilindros IS 'Número de cilindros.';
COMMENT ON COLUMN public.propuestas_publicacion.cilindrada IS 'Cilindrada declarada.';
COMMENT ON COLUMN public.propuestas_publicacion.tipo_traccion IS 'Tipo de tracción declarado.';
COMMENT ON COLUMN public.propuestas_publicacion.modificaciones IS 'Describe modificaciones relevantes del vehículo.';
COMMENT ON COLUMN public.propuestas_publicacion.estado_propuesta IS 'Estado actual de moderación de la propuesta.';
COMMENT ON COLUMN public.propuestas_publicacion.motivo_rechazo IS 'Motivo de la última decisión de rechazo.';
COMMENT ON COLUMN public.propuestas_publicacion.id_admin_revisor IS 'Administrador que realizó la última revisión.';
COMMENT ON COLUMN public.propuestas_publicacion.fecha_envio_moderacion IS 'Momento en que el propietario envió la propuesta a moderación.';
COMMENT ON COLUMN public.propuestas_publicacion.fecha_revision IS 'Momento de la última revisión administrativa.';
COMMENT ON COLUMN public.propuestas_publicacion.fecha_ultima_edicion_propietario IS 'Momento de la última modificación hecha por el propietario.';
COMMENT ON INDEX public.uq_propuestas_editable IS 'Máximo una propuesta editable (pendiente o rechazada) por publicación.';

COMMENT ON TABLE public.fotos_publicacion IS 'Conserva las referencias de fotografías que han formado parte de la galería aprobada, incluidas las retiradas históricamente.';
COMMENT ON COLUMN public.fotos_publicacion.id_foto IS 'Identificador de la fotografía.';
COMMENT ON COLUMN public.fotos_publicacion.id_publicacion IS 'Publicación a la que pertenece.';
COMMENT ON COLUMN public.fotos_publicacion.ruta_storage IS 'Ruta estable del archivo original en Storage.';
COMMENT ON COLUMN public.fotos_publicacion.orden_foto IS 'Posición de la imagen en la galería vigente.';
COMMENT ON COLUMN public.fotos_publicacion.activa IS 'Indica si forma parte de la galería vigente.';
COMMENT ON COLUMN public.fotos_publicacion.fecha_retiro IS 'Momento en que dejó de formar parte de la galería vigente.';
COMMENT ON INDEX public.uq_fotos_publicacion_orden_activa IS 'El orden no se repite entre las fotos activas de una publicación.';

COMMENT ON TABLE public.fotos_propuesta_publicacion IS 'Registra las fotografías de una propuesta pendiente, rechazada editable o aprobada conservada como evidencia simple.';
COMMENT ON COLUMN public.fotos_propuesta_publicacion.id_foto_propuesta IS 'Identificador de la fotografía propuesta.';
COMMENT ON COLUMN public.fotos_propuesta_publicacion.id_propuesta IS 'Propuesta a la que pertenece.';
COMMENT ON COLUMN public.fotos_propuesta_publicacion.ruta_storage IS 'Ruta estable del archivo en Storage.';
COMMENT ON COLUMN public.fotos_propuesta_publicacion.orden_foto IS 'Posición de la imagen dentro de la galería propuesta.';

COMMENT ON TABLE public.favoritos IS 'Relaciona una cuenta con las publicaciones que ha guardado como favoritas.';
COMMENT ON COLUMN public.favoritos.id_cuenta IS 'Cuenta que guardó la publicación.';
COMMENT ON COLUMN public.favoritos.id_publicacion IS 'Publicación guardada como favorita.';
COMMENT ON COLUMN public.favoritos.fecha_guardado IS 'Momento en que se agregó el favorito.';

COMMENT ON TABLE public.reportes IS 'Registra reportes realizados por usuarios autenticados sobre publicaciones y su resolución administrativa.';
COMMENT ON COLUMN public.reportes.id_reporte IS 'Identificador del reporte.';
COMMENT ON COLUMN public.reportes.id_publicacion IS 'Publicación reportada.';
COMMENT ON COLUMN public.reportes.id_reportante IS 'Usuario autenticado que realizó el reporte.';
COMMENT ON COLUMN public.reportes.motivo_reporte IS 'Motivo proporcionado por el usuario.';
COMMENT ON COLUMN public.reportes.estado_reporte IS 'Estado actual del reporte.';
COMMENT ON COLUMN public.reportes.id_admin_resolutor IS 'Administrador que resolvió el reporte.';
COMMENT ON COLUMN public.reportes.motivo_resolucion IS 'Justificación administrativa de atención/descarte.';
COMMENT ON COLUMN public.reportes.fecha_reporte IS 'Momento de creación del reporte.';
COMMENT ON COLUMN public.reportes.fecha_resolucion IS 'Momento de la resolución administrativa.';

COMMENT ON TABLE public.solicitudes_destacado IS 'Registra las solicitudes para destacar publicaciones, su cancelación previa a revisión y la resolución administrativa.';
COMMENT ON COLUMN public.solicitudes_destacado.id_solicitud_destacado IS 'Identificador de la solicitud.';
COMMENT ON COLUMN public.solicitudes_destacado.id_publicacion IS 'Publicación que se solicita destacar.';
COMMENT ON COLUMN public.solicitudes_destacado.fecha_solicitud IS 'Momento en que se creó la solicitud.';
COMMENT ON COLUMN public.solicitudes_destacado.estado_solicitud IS 'Estado actual de la solicitud.';
COMMENT ON COLUMN public.solicitudes_destacado.id_admin_resuelve IS 'Administrador que aprueba o rechaza.';
COMMENT ON COLUMN public.solicitudes_destacado.motivo_rechazo IS 'Motivo administrativo del rechazo.';
COMMENT ON COLUMN public.solicitudes_destacado.fecha_resolucion IS 'Momento de aprobación o rechazo.';
COMMENT ON COLUMN public.solicitudes_destacado.fecha_inicio_destacado IS 'Inicio del periodo aprobado.';
COMMENT ON COLUMN public.solicitudes_destacado.fecha_fin_destacado IS 'Fin del periodo aprobado.';
COMMENT ON COLUMN public.solicitudes_destacado.fecha_cancelacion IS 'Momento en que el propietario canceló una solicitud pendiente.';
COMMENT ON INDEX public.uq_solicitudes_pendiente IS 'Máximo una solicitud pendiente por publicación.';

COMMENT ON TABLE public.interacciones_publicacion IS 'Registra eventos que alimentan el dashboard, principalmente vistas de detalle y acciones de contacto.';
COMMENT ON COLUMN public.interacciones_publicacion.id_interaccion IS 'Identificador del evento.';
COMMENT ON COLUMN public.interacciones_publicacion.id_publicacion IS 'Publicación involucrada.';
COMMENT ON COLUMN public.interacciones_publicacion.id_cuenta IS 'Cuenta que generó el evento cuando está autenticada.';
COMMENT ON COLUMN public.interacciones_publicacion.tipo_interaccion IS 'Tipo de interacción registrada.';
COMMENT ON COLUMN public.interacciones_publicacion.fecha_hora IS 'Momento del evento.';

COMMENT ON TABLE public.acciones_administrativas IS 'Bitácora histórica e inmutable de acciones administrativas importantes.';
COMMENT ON COLUMN public.acciones_administrativas.id_accion IS 'Identificador de la acción administrativa.';
COMMENT ON COLUMN public.acciones_administrativas.id_admin IS 'Administrador que ejecutó la acción.';
COMMENT ON COLUMN public.acciones_administrativas.tipo_accion IS 'Tipo de acción administrativa.';
COMMENT ON COLUMN public.acciones_administrativas.motivo IS 'Justificación de la acción.';
COMMENT ON COLUMN public.acciones_administrativas.fecha_hora IS 'Momento exacto de la acción.';
COMMENT ON COLUMN public.acciones_administrativas.id_cuenta_objetivo IS 'Cuenta objetivo cuando aplica.';
COMMENT ON COLUMN public.acciones_administrativas.id_publicacion_objetivo IS 'Publicación objetivo cuando aplica.';
COMMENT ON COLUMN public.acciones_administrativas.id_propuesta_objetivo IS 'Propuesta concreta aprobada/rechazada.';
COMMENT ON COLUMN public.acciones_administrativas.id_reporte_objetivo IS 'Reporte objetivo cuando aplica.';
COMMENT ON COLUMN public.acciones_administrativas.id_solicitud_destacado_objetivo IS 'Solicitud de destacado objetivo cuando aplica.';


-- ############################################################################
-- 11. DATOS INICIALES
-- ############################################################################
-- Solo lo estructural: roles y la fila "Otros" de cada catálogo.
-- Las marcas, modelos y demás valores reales se cargan con su propio script.

INSERT INTO public.roles (nombre_rol, descripcion_rol) VALUES
    ('usuario',       'Usuario autenticado de Karsy.'),
    ('administrador', 'Cuenta lote con acceso a las funciones administrativas.');

INSERT INTO public.marcas        (nombre_marca, es_otro)       VALUES ('Otros', TRUE);
INSERT INTO public.carrocerias   (nombre_carroceria, es_otro)  VALUES ('Otros', TRUE);
INSERT INTO public.transmisiones (nombre_transmision, es_otro) VALUES ('Otros', TRUE);
INSERT INTO public.colores       (nombre_color, es_otro)       VALUES ('Otros', TRUE);

-- La marca "Otros" necesita su modelo "Otros" para poder capturarse.
INSERT INTO public.modelos (id_marca, nombre_modelo, es_otro)
SELECT id_marca, 'Otros', TRUE FROM public.marcas WHERE es_otro;