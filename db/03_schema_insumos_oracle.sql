-- ============================================================================
--  SAPI · Módulo Catálogo · Insumos y umbrales de seguridad
--  Requiere haber ejecutado 01_schema_oracle.sql
--  Ejecutar como el usuario dueño del esquema (SAPI):
--      sqlplus SAPI/sapi@//localhost:1521/XEPDB1 @03_schema_insumos_oracle.sql
-- ============================================================================

-- ---------------------------------------------------------------- SECUENCIAS
CREATE SEQUENCE CAT_INSUMO_SEQ
    START WITH 100
    INCREMENT BY 1
    NOCACHE
    NOCYCLE;

CREATE SEQUENCE UMBRAL_SEGURIDAD_SEQ
    START WITH 1
    INCREMENT BY 1
    NOCACHE
    NOCYCLE;

-- ------------------------------------------------------------------- INSUMOS
CREATE TABLE CAT_INSUMO (
    ID_INSUMO           NUMBER(19)     NOT NULL,
    CODIGO              VARCHAR2(20)   NOT NULL,
    NOMBRE              VARCHAR2(120)  NOT NULL,
    DESCRIPCION         VARCHAR2(250),
    ID_CATEGORIA        NUMBER(19)     NOT NULL,
    UNIDAD_MEDIDA       VARCHAR2(12)   NOT NULL,
    ACTIVO              NUMBER(1)      DEFAULT 1 NOT NULL,
    FECHA_REGISTRO      TIMESTAMP      DEFAULT SYSTIMESTAMP NOT NULL,
    FECHA_ACTUALIZACION TIMESTAMP,
    CONSTRAINT PK_CAT_INSUMO          PRIMARY KEY (ID_INSUMO),
    CONSTRAINT UK_CAT_INSUMO_CODIGO   UNIQUE (CODIGO),
    CONSTRAINT FK_CAT_INSUMO_CATEG    FOREIGN KEY (ID_CATEGORIA)
        REFERENCES CAT_CATEGORIA (ID_CATEGORIA),
    CONSTRAINT CK_CAT_INSUMO_ACTIVO   CHECK (ACTIVO IN (0, 1)),
    CONSTRAINT CK_CAT_INSUMO_UNIDAD   CHECK (UNIDAD_MEDIDA IN
        ('UNIDAD', 'KILOGRAMO', 'GRAMO', 'LITRO', 'CAJA'))
);

CREATE INDEX IX_CAT_INSUMO_CATEGORIA ON CAT_INSUMO (ID_CATEGORIA);
CREATE INDEX IX_CAT_INSUMO_NOMBRE    ON CAT_INSUMO (UPPER(NOMBRE));

-- ------------------------------------------------------- UMBRAL DE SEGURIDAD
-- Un solo umbral por insumo (relación 1:1) y la regla invariable:
-- el stock crítico debe ser estrictamente menor que el stock mínimo.
CREATE TABLE UMBRAL_SEGURIDAD (
    ID_UMBRAL           NUMBER(19)     NOT NULL,
    ID_INSUMO           NUMBER(19)     NOT NULL,
    STOCK_MINIMO        NUMBER(12,2)   NOT NULL,
    STOCK_CRITICO       NUMBER(12,2)   NOT NULL,
    FECHA_ACTUALIZACION TIMESTAMP,
    CONSTRAINT PK_UMBRAL_SEGURIDAD      PRIMARY KEY (ID_UMBRAL),
    CONSTRAINT UK_UMBRAL_INSUMO         UNIQUE (ID_INSUMO),
    CONSTRAINT FK_UMBRAL_INSUMO         FOREIGN KEY (ID_INSUMO)
        REFERENCES CAT_INSUMO (ID_INSUMO),
    CONSTRAINT CK_UMBRALES              CHECK (STOCK_CRITICO < STOCK_MINIMO),
    CONSTRAINT CK_UMBRAL_MIN_NO_NEG     CHECK (STOCK_MINIMO >= 0),
    CONSTRAINT CK_UMBRAL_CRIT_NO_NEG    CHECK (STOCK_CRITICO >= 0)
);

COMMENT ON TABLE  CAT_INSUMO                  IS 'Materias primas que consume producción';
COMMENT ON COLUMN CAT_INSUMO.CODIGO           IS 'SKU interno en mayúsculas';
COMMENT ON TABLE  UMBRAL_SEGURIDAD            IS 'Política de reposición por insumo (RF-01 / RF-02)';
COMMENT ON COLUMN UMBRAL_SEGURIDAD.STOCK_MINIMO  IS 'Punto de reorden: si el stock real baja de aquí, reponer';
COMMENT ON COLUMN UMBRAL_SEGURIDAD.STOCK_CRITICO IS 'Quiebre inminente: debe ser MENOR que STOCK_MINIMO';

COMMIT;
