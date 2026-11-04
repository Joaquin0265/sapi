-- ============================================================================
--  SAPI - Asignacion 5: Motor de Alertas y Sugerencias de Compra
--  Integra LOTE con CAT_INSUMO y crea la tabla DESPACHO con FK al insumo.
--
--  Requiere: 01_schema_oracle.sql, 02_data_oracle.sql,
--            03_schema_insumos_oracle.sql, 04_data_insumos_oracle.sql
--  Ejecutar como el usuario dueno del esquema (SAPI):
--      sqlplus SAPI/sapi@//localhost:1521/XEPDB1 @05_alertas_oracle.sql
-- ============================================================================
SET DEFINE OFF;

-- ----------------------------------------------------------------------------
-- 1) Unificar LOTE.ID_INSUMO -> CAT_INSUMO.ID_INSUMO
-- ----------------------------------------------------------------------------
ALTER TABLE LOTE DROP CONSTRAINT FK_LOTE_INSUMO;

-- Mapa temporal producto -> insumo. Primero por codigo identico...
CREATE TABLE TMP_MAP_INSUMO AS
SELECT p.ID_PRODUCTO,
       i.ID_INSUMO
  FROM CAT_PRODUCTO p
  JOIN CAT_INSUMO i ON i.CODIGO = p.CODIGO;

-- ...y luego el mapeo explicito de los datos demo (INS-001 -> HAR-001).
INSERT INTO TMP_MAP_INSUMO (ID_PRODUCTO, ID_INSUMO)
SELECT p.ID_PRODUCTO,
       i.ID_INSUMO
  FROM CAT_PRODUCTO p
 CROSS JOIN CAT_INSUMO i
 WHERE p.CODIGO = 'INS-001'
   AND i.CODIGO = 'HAR-001'
   AND NOT EXISTS (SELECT 1 FROM TMP_MAP_INSUMO m WHERE m.ID_PRODUCTO = p.ID_PRODUCTO);

-- Una sola sentencia: Oracle evalua el mapa antes de actualizar, sin
-- ambiguedades entre IDs de producto e insumo.
UPDATE LOTE l
   SET l.ID_INSUMO = (SELECT m.ID_INSUMO
                        FROM TMP_MAP_INSUMO m
                       WHERE m.ID_PRODUCTO = l.ID_INSUMO)
 WHERE l.ID_INSUMO IN (SELECT m.ID_PRODUCTO FROM TMP_MAP_INSUMO m);

DROP TABLE TMP_MAP_INSUMO;

-- Control previo: si esta consulta devuelve filas, hay lotes apuntando a un
-- producto que no tiene insumo equivalente. Mapeelos antes de continuar.
SELECT l.ID_LOTE, l.ID_INSUMO AS ID_INSUMO_SIN_EQUIVALENTE
  FROM LOTE l
 WHERE NOT EXISTS (SELECT 1 FROM CAT_INSUMO i WHERE i.ID_INSUMO = l.ID_INSUMO);

ALTER TABLE LOTE
    ADD CONSTRAINT FK_LOTE_INSUMO FOREIGN KEY (ID_INSUMO)
        REFERENCES CAT_INSUMO (ID_INSUMO);

COMMENT ON COLUMN LOTE.ID_INSUMO IS 'Insumo vigilado por el modulo de Alertas (CAT_INSUMO)';

-- ----------------------------------------------------------------------------
-- 2) Tabla DESPACHO: detalle de salidas con referencia real al insumo
-- ----------------------------------------------------------------------------
CREATE SEQUENCE DSP_DESPACHO_SEQ
    START WITH 100
    INCREMENT BY 1
    NOCACHE
    NOCYCLE;

CREATE TABLE DESPACHO (
    ID_DESPACHO   NUMBER(19)    NOT NULL,
    ID_MOVIMIENTO NUMBER(19)    NOT NULL,
    ID_INSUMO     NUMBER(19)    NOT NULL,
    CANTIDAD      NUMBER(12,2)  NOT NULL,
    FECHA_HORA    TIMESTAMP     DEFAULT SYSTIMESTAMP NOT NULL,
    CONSTRAINT PK_DESPACHO            PRIMARY KEY (ID_DESPACHO),
    CONSTRAINT FK_DESPACHO_MOVIMIENTO FOREIGN KEY (ID_MOVIMIENTO)
        REFERENCES MOVIMIENTO (ID_MOVIMIENTO),
    CONSTRAINT FK_DESPACHO_INSUMO     FOREIGN KEY (ID_INSUMO)
        REFERENCES CAT_INSUMO (ID_INSUMO),
    CONSTRAINT CK_DESPACHO_CANTIDAD   CHECK (CANTIDAD > 0)
);

-- Indice que soporta el calculo de retiros de los ultimos 7 dias (RN-04).
CREATE INDEX IX_DESPACHO_INSUMO_FECHA ON DESPACHO (ID_INSUMO, FECHA_HORA);

COMMENT ON TABLE  DESPACHO                IS 'Detalle de salidas de insumos a produccion (RF-03)';
COMMENT ON COLUMN DESPACHO.ID_INSUMO      IS 'Insumo retirado; alimenta el promedio historico de Alertas';
COMMENT ON COLUMN DESPACHO.FECHA_HORA     IS 'Fecha del retiro usada por la ventana de 7 dias (RN-04)';

COMMIT;
