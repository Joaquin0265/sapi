-- ============================================================================
--  SAPI · Módulo Catálogo · Datos iniciales
--  Requiere haber ejecutado 01_schema_oracle.sql
-- ============================================================================

INSERT INTO CAT_CATEGORIA (ID_CATEGORIA, NOMBRE, DESCRIPCION, ACTIVO) VALUES (1, 'Panadería',  'Panes de sal y de dulce del día', 1);
INSERT INTO CAT_CATEGORIA (ID_CATEGORIA, NOMBRE, DESCRIPCION, ACTIVO) VALUES (2, 'Pastelería', 'Tortas, bocaditos y postres',     1);
INSERT INTO CAT_CATEGORIA (ID_CATEGORIA, NOMBRE, DESCRIPCION, ACTIVO) VALUES (3, 'Bebidas',    'Café, jugos y refrescos',         1);
INSERT INTO CAT_CATEGORIA (ID_CATEGORIA, NOMBRE, DESCRIPCION, ACTIVO) VALUES (4, 'Insumos',    'Materia prima de producción',     1);

INSERT INTO CAT_PRODUCTO (ID_PRODUCTO, CODIGO, NOMBRE, DESCRIPCION, ID_CATEGORIA, UNIDAD_MEDIDA, PRECIO, STOCK_MINIMO, ACTIVO)
VALUES (1, 'PAN-001', 'Pan francés', 'Unidad de 60 g, horneado dos veces al día', 1, 'UNIDAD', 0.30, 200.00, 1);

INSERT INTO CAT_PRODUCTO (ID_PRODUCTO, CODIGO, NOMBRE, DESCRIPCION, ID_CATEGORIA, UNIDAD_MEDIDA, PRECIO, STOCK_MINIMO, ACTIVO)
VALUES (2, 'PAN-014', 'Pan de molde integral', 'Bolsa de 500 g', 1, 'UNIDAD', 7.50, 30.00, 1);

INSERT INTO CAT_PRODUCTO (ID_PRODUCTO, CODIGO, NOMBRE, DESCRIPCION, ID_CATEGORIA, UNIDAD_MEDIDA, PRECIO, STOCK_MINIMO, ACTIVO)
VALUES (3, 'POS-003', 'Torta de chocolate', 'Torta entera de 1 kg, por encargo', 2, 'UNIDAD', 45.00, 5.00, 1);

INSERT INTO CAT_PRODUCTO (ID_PRODUCTO, CODIGO, NOMBRE, DESCRIPCION, ID_CATEGORIA, UNIDAD_MEDIDA, PRECIO, STOCK_MINIMO, ACTIVO)
VALUES (4, 'BEB-002', 'Café pasado', 'Vaso de 8 oz', 3, 'LITRO', 3.00, 10.00, 1);

INSERT INTO CAT_PRODUCTO (ID_PRODUCTO, CODIGO, NOMBRE, DESCRIPCION, ID_CATEGORIA, UNIDAD_MEDIDA, PRECIO, STOCK_MINIMO, ACTIVO)
VALUES (5, 'INS-001', 'Harina de trigo', 'Saco de 50 kg para producción', 4, 'KILOGRAMO', 3.20, 500.00, 1);

COMMIT;
