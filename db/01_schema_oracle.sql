-- ============================================================================
--  SAPI · Módulo Catálogo · Oracle PL/SQL
--  Ejecutar como el usuario dueño del esquema (SAPI):
--      sqlplus SAPI/sapi@//localhost:1521/XEPDB1 @01_schema_oracle.sql
-- ============================================================================

-- ---------------------------------------------------------------- SECUENCIAS
CREATE SEQUENCE CAT_CATEGORIA_SEQ
    START WITH 100
    INCREMENT BY 1
    NOCACHE
    NOCYCLE;

CREATE SEQUENCE CAT_PRODUCTO_SEQ
    START WITH 100
    INCREMENT BY 1
    NOCACHE
    NOCYCLE;

-- --------------------------------------------------------------- CATEGORÍAS
CREATE TABLE CAT_CATEGORIA (
    ID_CATEGORIA  NUMBER(19)     NOT NULL,
    NOMBRE        VARCHAR2(80)   NOT NULL,
    DESCRIPCION   VARCHAR2(200),
    ACTIVO        NUMBER(1)      DEFAULT 1 NOT NULL,
    CONSTRAINT PK_CAT_CATEGORIA        PRIMARY KEY (ID_CATEGORIA),
    CONSTRAINT UK_CAT_CATEGORIA_NOMBRE UNIQUE (NOMBRE),
    CONSTRAINT CK_CAT_CATEGORIA_ACTIVO CHECK (ACTIVO IN (0, 1))
);

-- ---------------------------------------------------------------- PRODUCTOS
CREATE TABLE CAT_PRODUCTO (
    ID_PRODUCTO         NUMBER(19)     NOT NULL,
    CODIGO              VARCHAR2(20)   NOT NULL,
    NOMBRE              VARCHAR2(120)  NOT NULL,
    DESCRIPCION         VARCHAR2(250),
    ID_CATEGORIA        NUMBER(19)     NOT NULL,
    UNIDAD_MEDIDA       VARCHAR2(12)   NOT NULL,
    PRECIO              NUMBER(12,2)   NOT NULL,
    STOCK_MINIMO        NUMBER(12,2)   DEFAULT 0 NOT NULL,
    ACTIVO              NUMBER(1)      DEFAULT 1 NOT NULL,
    FECHA_REGISTRO      TIMESTAMP      DEFAULT SYSTIMESTAMP NOT NULL,
    FECHA_ACTUALIZACION TIMESTAMP,
    CONSTRAINT PK_CAT_PRODUCTO          PRIMARY KEY (ID_PRODUCTO),
    CONSTRAINT UK_CAT_PRODUCTO_CODIGO   UNIQUE (CODIGO),
    CONSTRAINT FK_CAT_PRODUCTO_CATEG    FOREIGN KEY (ID_CATEGORIA)
        REFERENCES CAT_CATEGORIA (ID_CATEGORIA),
    CONSTRAINT CK_CAT_PRODUCTO_PRECIO   CHECK (PRECIO > 0),
    CONSTRAINT CK_CAT_PRODUCTO_STOCK    CHECK (STOCK_MINIMO >= 0),
    CONSTRAINT CK_CAT_PRODUCTO_ACTIVO   CHECK (ACTIVO IN (0, 1)),
    CONSTRAINT CK_CAT_PRODUCTO_UNIDAD   CHECK (UNIDAD_MEDIDA IN
        ('UNIDAD', 'KILOGRAMO', 'GRAMO', 'LITRO', 'CAJA'))
);

CREATE INDEX IX_CAT_PRODUCTO_CATEGORIA ON CAT_PRODUCTO (ID_CATEGORIA);

-- Búsqueda insensible a mayúsculas por nombre (usada por el filtro del listado)
CREATE INDEX IX_CAT_PRODUCTO_NOMBRE ON CAT_PRODUCTO (UPPER(NOMBRE));

COMMENT ON TABLE  CAT_PRODUCTO             IS 'Catálogo maestro de productos de la panificadora';
COMMENT ON COLUMN CAT_PRODUCTO.CODIGO      IS 'SKU interno en mayúsculas';
COMMENT ON COLUMN CAT_PRODUCTO.STOCK_MINIMO IS 'Política de reposición: punto de reorden para el módulo de Alertas';
COMMENT ON COLUMN CAT_PRODUCTO.ACTIVO      IS 'Baja lógica: 1 activo / 0 inactivo';

COMMIT;
