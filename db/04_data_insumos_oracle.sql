-- ============================================================================
--  SAPI · Módulo Catálogo · Datos iniciales de insumos y umbrales
--  Requiere: 01_schema_oracle.sql, 02_data_oracle.sql y 03_schema_insumos_oracle.sql
-- ============================================================================

-- Categoría 4 = 'Insumos' (creada en 02_data_oracle.sql)
INSERT INTO CAT_INSUMO (ID_INSUMO, CODIGO, NOMBRE, DESCRIPCION, ID_CATEGORIA, UNIDAD_MEDIDA, ACTIVO)
VALUES (1, 'HAR-001', 'Harina Especial', 'Saco de 50 kg para producción diaria', 4, 'KILOGRAMO', 1);

INSERT INTO CAT_INSUMO (ID_INSUMO, CODIGO, NOMBRE, DESCRIPCION, ID_CATEGORIA, UNIDAD_MEDIDA, ACTIVO)
VALUES (2, 'AZU-001', 'Azúcar rubia', 'Saco de 50 kg', 4, 'KILOGRAMO', 1);

INSERT INTO CAT_INSUMO (ID_INSUMO, CODIGO, NOMBRE, DESCRIPCION, ID_CATEGORIA, UNIDAD_MEDIDA, ACTIVO)
VALUES (3, 'LEV-001', 'Levadura fresca', 'Bloque de 1 kg, conservar en frío', 4, 'KILOGRAMO', 1);

INSERT INTO CAT_INSUMO (ID_INSUMO, CODIGO, NOMBRE, DESCRIPCION, ID_CATEGORIA, UNIDAD_MEDIDA, ACTIVO)
VALUES (4, 'MAN-001', 'Manteca vegetal', 'Caja de 20 kg', 4, 'KILOGRAMO', 1);

-- Este insumo queda sin umbral a propósito: la pantalla debe mostrar
-- "Sin configurar" y permitir crearlo desde el modal.
INSERT INTO CAT_INSUMO (ID_INSUMO, CODIGO, NOMBRE, DESCRIPCION, ID_CATEGORIA, UNIDAD_MEDIDA, ACTIVO)
VALUES (5, 'SAL-001', 'Sal industrial', 'Saco de 25 kg', 4, 'KILOGRAMO', 1);

-- Umbrales: el crítico siempre es MENOR que el mínimo.
INSERT INTO UMBRAL_SEGURIDAD (ID_UMBRAL, ID_INSUMO, STOCK_MINIMO, STOCK_CRITICO)
VALUES (1, 1, 50.00, 20.00);

INSERT INTO UMBRAL_SEGURIDAD (ID_UMBRAL, ID_INSUMO, STOCK_MINIMO, STOCK_CRITICO)
VALUES (2, 2, 30.00, 10.00);

INSERT INTO UMBRAL_SEGURIDAD (ID_UMBRAL, ID_INSUMO, STOCK_MINIMO, STOCK_CRITICO)
VALUES (3, 3, 10.00, 4.00);

INSERT INTO UMBRAL_SEGURIDAD (ID_UMBRAL, ID_INSUMO, STOCK_MINIMO, STOCK_CRITICO)
VALUES (4, 4, 15.00, 5.00);

COMMIT;
