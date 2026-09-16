-- =============================================================================
-- ÉXITO STOCK AI · Migración V3 (aditiva: NO destruye tablas existentes)
-- Autenticación y roles, auditoría obligatoria, compras simuladas, y campos
-- extendidos de producto/movimientos. Preparación de estados de modelos.
-- =============================================================================

-- ---------------------------------------------------------------------------
-- Roles: se agrega EMPLEADO (CLIENTE/PROVEEDOR/ADMINISTRADOR ya existían)
-- ---------------------------------------------------------------------------
ALTER TABLE roles DROP CONSTRAINT IF EXISTS roles_nombre_check;
ALTER TABLE roles ADD CONSTRAINT roles_nombre_check
    CHECK (nombre IN ('CLIENTE', 'PROVEEDOR', 'ADMINISTRADOR', 'EMPLEADO'));

INSERT INTO roles (nombre) VALUES ('CLIENTE'), ('PROVEEDOR'), ('ADMINISTRADOR'), ('EMPLEADO')
    ON CONFLICT (nombre) DO NOTHING;

-- ---------------------------------------------------------------------------
-- Estados de modelos matemáticos: se preparan estados de ciclo de vida futuros
-- ---------------------------------------------------------------------------
ALTER TABLE modelos_matematicos DROP CONSTRAINT IF EXISTS modelos_matematicos_estado_check;
ALTER TABLE modelos_matematicos ADD CONSTRAINT modelos_matematicos_estado_check
    CHECK (estado IN ('PROPUESTO', 'PILOTO', 'EN_DESARROLLO', 'IMPLEMENTADO', 'VALIDADO'));

-- ---------------------------------------------------------------------------
-- Productos: descripción e imagen
-- ---------------------------------------------------------------------------
ALTER TABLE productos ADD COLUMN IF NOT EXISTS descripcion TEXT;
ALTER TABLE productos ADD COLUMN IF NOT EXISTS imagen VARCHAR(255);

-- ---------------------------------------------------------------------------
-- Movimientos de inventario: usuario que realizó la operación
-- ---------------------------------------------------------------------------
ALTER TABLE movimientos_inventario ADD COLUMN IF NOT EXISTS usuario_id BIGINT REFERENCES usuarios (id);
CREATE INDEX IF NOT EXISTS idx_movimientos_usuario ON movimientos_inventario (usuario_id);

-- ===========================================================================
-- AUDITORÍA OBLIGATORIA (registros inmutables de modificación de inventario)
-- ===========================================================================
CREATE TABLE IF NOT EXISTS auditoria (
    id              BIGSERIAL PRIMARY KEY,
    usuario_id      BIGINT REFERENCES usuarios (id),
    rol             VARCHAR(40)  NOT NULL,
    producto_id     BIGINT REFERENCES productos (id),
    operacion       VARCHAR(40)  NOT NULL,
    stock_anterior  INTEGER,
    cantidad        INTEGER      NOT NULL,
    stock_posterior INTEGER,
    fecha           DATE         NOT NULL,
    hora            TIME         NOT NULL,
    fecha_hora      TIMESTAMPTZ  NOT NULL,
    observacion     TEXT
);

CREATE INDEX IF NOT EXISTS idx_auditoria_usuario  ON auditoria (usuario_id);
CREATE INDEX IF NOT EXISTS idx_auditoria_producto ON auditoria (producto_id);
CREATE INDEX IF NOT EXISTS idx_auditoria_fecha    ON auditoria (fecha_hora);
CREATE INDEX IF NOT EXISTS idx_auditoria_operacion ON auditoria (operacion);

-- Inmutabilidad a nivel de base de datos: no se permite modificar/borrar auditoría
CREATE OR REPLACE FUNCTION auditar_no_borrar() RETURNS trigger AS $$
BEGIN
    RAISE EXCEPTION 'Los registros de auditoría son inmutables (operación no permitida)';
END;
$$ LANGUAGE plpgsql;

DROP TRIGGER IF EXISTS trg_auditoria_inmutable ON auditoria;
CREATE TRIGGER trg_auditoria_inmutable
    BEFORE UPDATE OR DELETE ON auditoria
    FOR EACH ROW EXECUTE FUNCTION auditar_no_borrar();

-- ===========================================================================
-- COMPRAS SIMULADAS
-- ===========================================================================
CREATE TABLE IF NOT EXISTS compras (
    id         BIGSERIAL PRIMARY KEY,
    usuario_id BIGINT        NOT NULL REFERENCES usuarios (id),
    estado     VARCHAR(20)   NOT NULL DEFAULT 'REALIZADA',
    total      NUMERIC(12, 2) NOT NULL,
    fecha      TIMESTAMPTZ   NOT NULL DEFAULT now()
);

CREATE INDEX IF NOT EXISTS idx_compras_usuario ON compras (usuario_id);

CREATE TABLE IF NOT EXISTS detalle_compra (
    id              BIGSERIAL PRIMARY KEY,
    compra_id       BIGINT         NOT NULL REFERENCES compras (id),
    producto_id     BIGINT         NOT NULL REFERENCES productos (id),
    cantidad        INTEGER        NOT NULL CHECK (cantidad > 0),
    precio_unitario NUMERIC(12, 2) NOT NULL,
    subtotal        NUMERIC(12, 2) NOT NULL
);

CREATE INDEX IF NOT EXISTS idx_detalle_compra_compra ON detalle_compra (compra_id);
CREATE INDEX IF NOT EXISTS idx_detalle_compra_producto ON detalle_compra (producto_id);

-- Modelo físico de soporte (documentación consultable en BD):
-- Las tres cadenas de relación son verificables vía FK:
--   usuario -> usuario_id | movimiento -> usuario_id | auditoria -> usuario_id/producto_id
--   admin -> usuario | ejecucion -> modelo | compra -> usuario -> detalle -> producto