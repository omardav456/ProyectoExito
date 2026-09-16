-- =============================================================================
-- ÉXITO STOCK AI · Esquema base (PostgreSQL)
-- Migración V1: inventario, alertas, modelos matemáticos, seguridad (esqueleto)
-- =============================================================================

-- ---------------------------------------------------------------------------
-- DOMINIO INVENTARIO
-- ---------------------------------------------------------------------------

CREATE TABLE categorias (
    id          BIGSERIAL PRIMARY KEY,
    nombre      VARCHAR(80)  NOT NULL UNIQUE,
    descripcion VARCHAR(255),
    color       VARCHAR(20),
    activa      BOOLEAN      NOT NULL DEFAULT TRUE
);

CREATE TABLE productos (
    id              BIGSERIAL PRIMARY KEY,
    nombre          VARCHAR(120)   NOT NULL UNIQUE,
    categoria_id    BIGINT         NOT NULL REFERENCES categorias (id),
    stock_actual    INTEGER        NOT NULL DEFAULT 0,
    stock_minimo    INTEGER        NOT NULL DEFAULT 0,
    precio          NUMERIC(12, 2) NOT NULL DEFAULT 0,
    tasa_reposicion INTEGER        NOT NULL DEFAULT 0,
    unidad          VARCHAR(20)    NOT NULL DEFAULT 'uds',
    activo          BOOLEAN        NOT NULL DEFAULT TRUE,
    created_at      TIMESTAMPTZ    NOT NULL DEFAULT now(),
    updated_at      TIMESTAMPTZ    NOT NULL DEFAULT now()
);

CREATE INDEX idx_productos_categoria ON productos (categoria_id);

CREATE TABLE movimientos_inventario (
    id         BIGSERIAL PRIMARY KEY,
    producto_id BIGINT       NOT NULL REFERENCES productos (id),
    tipo       VARCHAR(20)   NOT NULL CHECK (tipo IN ('ENTRADA', 'SALIDA', 'AJUSTE')),
    cantidad   INTEGER       NOT NULL CHECK (cantidad > 0),
    fecha      DATE          NOT NULL DEFAULT CURRENT_DATE,
    descripcion VARCHAR(255),
    created_at TIMESTAMPTZ   NOT NULL DEFAULT now()
);

CREATE INDEX idx_movimientos_producto_fecha ON movimientos_inventario (producto_id, fecha);

CREATE TABLE demanda_diaria (
    id          BIGSERIAL PRIMARY KEY,
    producto_id BIGINT      NOT NULL REFERENCES productos (id),
    fecha       DATE        NOT NULL,
    unidades    INTEGER     NOT NULL CHECK (unidades >= 0),
    tipo        VARCHAR(20) NOT NULL CHECK (tipo IN ('REAL', 'PREVISTA')),
    created_at  TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT uq_demanda_producto_fecha_tipo UNIQUE (producto_id, fecha, tipo)
);

CREATE INDEX idx_demanda_producto_fecha ON demanda_diaria (producto_id, fecha);

CREATE TABLE alertas (
    id         BIGSERIAL PRIMARY KEY,
    producto_id BIGINT REFERENCES productos (id),
    tipo       VARCHAR(20)  NOT NULL CHECK (tipo IN ('CRITICO', 'ADVERTENCIA', 'INFO')),
    prioridad  VARCHAR(20)  NOT NULL DEFAULT 'MEDIA' CHECK (prioridad IN ('ALTA', 'MEDIA', 'BAJA')),
    mensaje    TEXT         NOT NULL,
    hora       TIMESTAMPTZ  NOT NULL DEFAULT now(),
    estado     VARCHAR(20)  NOT NULL DEFAULT 'ACTIVA' CHECK (estado IN ('ACTIVA', 'RESUELTA'))
);

CREATE INDEX idx_alertas_estado_tipo ON alertas (estado, tipo);

CREATE TABLE recomendaciones (
    id             BIGSERIAL PRIMARY KEY,
    producto_id    BIGINT REFERENCES productos (id),
    tipo           VARCHAR(30)  NOT NULL DEFAULT 'OPERATIVA',
    titulo         VARCHAR(160) NOT NULL,
    descripcion    TEXT         NOT NULL,
    accion_sugerida VARCHAR(255),
    prioridad      VARCHAR(20)  NOT NULL DEFAULT 'MEDIA' CHECK (prioridad IN ('ALTA', 'MEDIA', 'BAJA')),
    fecha          DATE         NOT NULL DEFAULT CURRENT_DATE,
    activa         BOOLEAN      NOT NULL DEFAULT TRUE
);

CREATE INDEX idx_recomendaciones_activa ON recomendaciones (activa);

-- Historial de ejecuciones del motor (parámetros y resultados en formato JSON texto)
CREATE TABLE simulaciones (
    id           BIGSERIAL PRIMARY KEY,
    modelo_codigo VARCHAR(40),
    descripcion  VARCHAR(255),
    parametros   TEXT NOT NULL,
    resultado    TEXT NOT NULL,
    creada_en    TIMESTAMPTZ NOT NULL DEFAULT now()
);

-- ---------------------------------------------------------------------------
-- DOMINIO MODELOS MATEMÁTICOS (catálogo de la propuesta de 1.000 modelos)
-- ---------------------------------------------------------------------------

CREATE TABLE areas_modelo (
    id          BIGSERIAL PRIMARY KEY,
    nombre      VARCHAR(80) NOT NULL UNIQUE,
    descripcion TEXT
);

CREATE TABLE categorias_modelo (
    id          BIGSERIAL PRIMARY KEY,
    area_id     BIGINT      NOT NULL REFERENCES areas_modelo (id),
    nombre      VARCHAR(120) NOT NULL,
    descripcion TEXT,
    CONSTRAINT uq_categoria_modelo_area_nombre UNIQUE (area_id, nombre)
);

CREATE TABLE subcategorias_modelo (
    id          BIGSERIAL PRIMARY KEY,
    categoria_id BIGINT      NOT NULL REFERENCES categorias_modelo (id),
    nombre      VARCHAR(120) NOT NULL,
    descripcion TEXT,
    CONSTRAINT uq_subcategoria_modelo_cat_nombre UNIQUE (categoria_id, nombre)
);

CREATE TABLE modelos_matematicos (
    id             BIGSERIAL PRIMARY KEY,
    subcategoria_id BIGINT       NOT NULL REFERENCES subcategorias_modelo (id),
    codigo         VARCHAR(20)   NOT NULL UNIQUE,
    nombre         VARCHAR(180)  NOT NULL,
    descripcion    TEXT,
    problema       TEXT,
    tipo_modelo    VARCHAR(40),
    metodo_sugerido VARCHAR(120),
    complejidad    VARCHAR(20)   NOT NULL DEFAULT 'MEDIA' CHECK (complejidad IN ('BAJA', 'MEDIA', 'ALTA', 'MUY_ALTA')),
    entrada_esperada TEXT,
    salida_esperada  TEXT,
    estado         VARCHAR(20)   NOT NULL DEFAULT 'PROPUESTO' CHECK (estado IN ('PROPUESTO', 'PILOTO', 'IMPLEMENTADO')),
    motor_impl     VARCHAR(120),
    documentacion  TEXT
);

CREATE INDEX idx_modelos_subcategoria ON modelos_matematicos (subcategoria_id);
CREATE INDEX idx_modelos_estado_tipo  ON modelos_matematicos (estado, tipo_modelo);
CREATE INDEX idx_modelos_complejidad  ON modelos_matematicos (complejidad);

CREATE TABLE variables_modelo (
    id          BIGSERIAL PRIMARY KEY,
    modelo_id   BIGINT      NOT NULL REFERENCES modelos_matematicos (id) ON DELETE CASCADE,
    nombre      VARCHAR(120) NOT NULL,
    simbolo     VARCHAR(30),
    rol         VARCHAR(20) NOT NULL DEFAULT 'ENTRADA' CHECK (rol IN ('ENTRADA', 'SALIDA', 'ESTADO')),
    tipo_dato   VARCHAR(30),
    unidad      VARCHAR(40),
    descripcion TEXT
);

CREATE INDEX idx_variables_modelo ON variables_modelo (modelo_id);

CREATE TABLE parametros_modelo (
    id               BIGSERIAL PRIMARY KEY,
    modelo_id        BIGINT      NOT NULL REFERENCES modelos_matematicos (id) ON DELETE CASCADE,
    nombre           VARCHAR(120) NOT NULL,
    simbolo          VARCHAR(30),
    valor_por_defecto VARCHAR(60),
    unidad           VARCHAR(40),
    descripcion      TEXT
);

CREATE INDEX idx_parametros_modelo ON parametros_modelo (modelo_id);

CREATE TABLE metodos_numericos (
    id          BIGSERIAL PRIMARY KEY,
    nombre      VARCHAR(120) NOT NULL UNIQUE,
    descripcion TEXT
);

CREATE TABLE modelo_metodo (
    modelo_id  BIGINT NOT NULL REFERENCES modelos_matematicos (id) ON DELETE CASCADE,
    metodo_id  BIGINT NOT NULL REFERENCES metodos_numericos (id) ON DELETE CASCADE,
    PRIMARY KEY (modelo_id, metodo_id)
);

CREATE TABLE aplicaciones_modelo (
    id          BIGSERIAL PRIMARY KEY,
    modelo_id   BIGINT NOT NULL REFERENCES modelos_matematicos (id) ON DELETE CASCADE,
    descripcion VARCHAR(255) NOT NULL
);

CREATE INDEX idx_aplicaciones_modelo ON aplicaciones_modelo (modelo_id);

-- Ejecuciones de modelos (fase futura de implementación del motor)
CREATE TABLE ejecuciones_modelo (
    id          BIGSERIAL PRIMARY KEY,
    modelo_id   BIGINT REFERENCES modelos_matematicos (id),
    modelo_codigo VARCHAR(20),
    parametros  TEXT NOT NULL,
    resultado   TEXT,
    estado      VARCHAR(20) NOT NULL DEFAULT 'OK' CHECK (estado IN ('OK', 'ERROR')),
    mensaje     TEXT,
    creada_en   TIMESTAMPTZ NOT NULL DEFAULT now()
);

-- ---------------------------------------------------------------------------
-- SEGURIDAD (esqueleto preparado para fases futuras: e-commerce multirol/chat)
-- ---------------------------------------------------------------------------

CREATE TABLE roles (
    id     BIGSERIAL PRIMARY KEY,
    nombre VARCHAR(30) NOT NULL UNIQUE CHECK (nombre IN ('CLIENTE', 'PROVEEDOR', 'ADMINISTRADOR'))
);

CREATE TABLE usuarios (
    id            BIGSERIAL PRIMARY KEY,
    email         VARCHAR(120) NOT NULL UNIQUE,
    password_hash VARCHAR(255) NOT NULL,
    nombre        VARCHAR(120),
    rol_id        BIGINT       NOT NULL REFERENCES roles (id),
    activo        BOOLEAN      NOT NULL DEFAULT TRUE,
    created_at    TIMESTAMPTZ  NOT NULL DEFAULT now()
);

CREATE INDEX idx_usuarios_rol ON usuarios (rol_id);