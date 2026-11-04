-- ============================================================================
--  SAPI - Correccion de las claves BCrypt del modulo de Seguridad
--
--  Los hashes originales de 03_schema_seguridad_oracle.sql no correspondian
--  a las contrasenas documentadas (admin123 / operario123), por lo que el
--  login fallaba incluso con Oracle.
--
--  USO: ejecutar UNA sola vez sobre una base creada con la version anterior
--  del script de seguridad. Si va a recrear el esquema desde cero NO hace
--  falta: 03_schema_seguridad_oracle.sql ya trae los hashes corregidos.
--
--  Requiere: 03_schema_seguridad_oracle.sql
--  Ejecutar como el usuario dueno del esquema (SAPI):
--      sqlplus SAPI/sapi@//localhost:1521/XEPDB1 @06_fix_claves_seguridad_oracle.sql
-- ============================================================================
SET DEFINE OFF;

-- admin / admin123  (rol ADMINISTRADOR)
UPDATE SEG_USUARIO
   SET CLAVE_ENCRIPTADA = '$2a$10$QO6iePcBXoBSUKHMce5DjuQeKvoPoFNb0mVUKo1WAta4CrA.tZeBK'
 WHERE NOMBRE_USUARIO = 'admin';

-- operario / operario123  (rol OPERARIO_ALMACEN)
UPDATE SEG_USUARIO
   SET CLAVE_ENCRIPTADA = '$2a$10$g3PBjGx6BGSc5W6xSUVEruyQ/Q/oXgi27PuZeTNGsAB6YFfiXB0Qu'
 WHERE NOMBRE_USUARIO = 'operario';

COMMIT;

-- ----------------------------------------------------------------------------
-- Control: debe devolver 2 filas con prefijos distintos.
-- ----------------------------------------------------------------------------
SELECT NOMBRE_USUARIO,
       SUBSTR(CLAVE_ENCRIPTADA, 1, 7) AS PREFIJO
  FROM SEG_USUARIO
 WHERE NOMBRE_USUARIO IN ('admin', 'operario');
