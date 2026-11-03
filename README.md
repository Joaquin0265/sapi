# SAPI · Sistema de Alertas Inteligentes para Control Preventivo

Sistema para una panificadora cuyo objetivo es **evitar quiebres de stock** mediante alertas
tempranas y control preventivo de inventario.

> Estado actual: **Asignación 3 · Módulo de Inventario (gestión FIFO e ingreso de lotes)**
> implementado de punta a punta, junto con los módulos de Catálogo, Seguridad y Despacho.

## Stack

| Capa | Tecnología |
|---|---|
| Lenguaje | Java 21 |
| Framework | Spring Boot 3.5 (Web MVC, Data JPA, Validation) |
| Vistas | Thymeleaf + Bootstrap 5 (CDN) |
| Base de datos | Oracle (PL/SQL) · H2 en memoria para pruebas |
| Build | Maven |

## Arquitectura: monolito modular + hexagonal

Cinco módulos independientes, cada uno con la misma división interna:

```
Seguridad · Catálogo · Inventario · Despacho · Alertas
```

```
<modulo>/
├── domain/           Entidades JPA planas + reglas y excepciones de negocio
├── application/      Servicios (casos de uso) y DTOs de entrada
└── infrastructure/   Adaptadores: repositorios Spring Data y controladores MVC
```

Reglas del proyecto:

- Inyección de dependencias **por constructor** (`@RequiredArgsConstructor`). Nunca `@Autowired` en atributos.
- Los controladores son `@Controller` (devuelven vistas), no `@RestController`.
- La lógica de negocio vive en `application`; el controlador solo traduce HTTP.
- Bajas **lógicas**: nunca se borra un producto con historial de movimientos.
- `open-in-view: false` + `@EntityGraph` para evitar el problema N+1 sin sesión abierta en la vista.

## Módulo Catálogo

| Artefacto | Ruta |
|---|---|
| Entidades | `catalogo/domain/model/{Producto,Categoria,Insumo,UmbralSeguridad,UnidadMedida}.java` |
| Servicio | `catalogo/application/service/CatalogoService.java` |
| DTO de formulario | `catalogo/application/dto/{ProductoForm,UmbralForm}.java` |
| Repositorios | `catalogo/infrastructure/persistence/` |
| Controlador | `catalogo/infrastructure/web/CatalogoController.java` |
| Vistas | `src/main/resources/templates/catalogo/{lista,formulario,insumos}.html` |
| Scripts Oracle | `db/01_schema_oracle.sql`, `db/02_data_oracle.sql`, `db/03_schema_insumos_oracle.sql`, `db/04_data_insumos_oracle.sql` |

Funcionalidad:

- Listado de productos activos con búsqueda por nombre.
- Alta y edición con validación en cliente y servidor (Bean Validation + reglas de negocio).
- SKU normalizado a mayúsculas y validado como único.
- Política de reposición (`stock_minimo`) que consumirá el módulo de Alertas.
- Baja lógica y reactivación.
- **Insumos y umbrales (RF-01 / RF-02):** pantalla de búsqueda de insumos y modal para
  configurar `stock_minimo` y `stock_critico`. La regla **crítico < mínimo** se valida en
  `CatalogoService#registrarUmbral` (lanza `ReglaNegocioException`) y también en la base de
  datos con `CHK_UMBRALES`.

### Rutas

| Método | Ruta | Descripción |
|---|---|---|
| GET | `/catalogo?q=` | Listado + filtro |
| GET | `/catalogo/nuevo` | Formulario de alta |
| GET | `/catalogo/{id}/editar` | Formulario de edición |
| POST | `/catalogo/guardar` | Alta/edición (POST-Redirect-GET) |
| POST | `/catalogo/{id}/desactivar` | Baja lógica |
| POST | `/catalogo/{id}/reactivar` | Reactivación |
| GET | `/catalogo/insumos?q=` | Insumos + umbral vigente (buscador) |
| POST | `/catalogo/insumos/{id}/umbral` | Configurar umbral (solo Administrador, PRG) |

## Módulo Inventario · Gestión FIFO e Ingreso de Lotes (Asignación 3)

Implementa la regla **RN-01: primero en vencer, primero en salir**. Los lotes de un insumo se
consultan siempre por `fecha_vencimiento` ascendente y un ingreso se registra como cabecera
`MOVIMIENTO` + detalle `LOTE` dentro de una **única transacción**: si falla el detalle, la
cabecera también se revierte.

### Artefactos

| Artefacto | Ruta |
|---|---|
| Entidades | `inventario/domain/model/{Movimiento,Lote,TipoMovimiento}.java` |
| Excepción de negocio | `inventario/domain/exception/ReglaNegocioException.java` |
| DTOs | `inventario/application/dto/{IngresoLoteForm,LoteCaducidadView}.java` |
| Servicio | `inventario/application/service/InventarioService.java` |
| Repositorios | `inventario/infrastructure/persistence/` |
| Controlador | `inventario/infrastructure/web/InventarioController.java` |
| Vistas | `src/main/resources/templates/inventario/{ingreso,lotes,reporte-caducidad}.html` |
| DDL Oracle | `db/01_schema_oracle.sql` |
| Datos demo | `catalogo/infrastructure/bootstrap/DatosDemoInitializer.java` |

### Modelo de datos

| Tabla | Columnas principales |
|---|---|
| `MOVIMIENTO` | `ID_MOVIMIENTO`, `TIPO_MOVIMIENTO`, `FECHA_MOVIMIENTO`, `ID_PERSONAL`, `OBSERVACION` |
| `LOTE` | `ID_LOTE`, `ID_MOVIMIENTO`, `ID_INSUMO`, `CODIGO_LOTE`, `CANTIDAD`, `CANTIDAD_DISPONIBLE`, `COSTO_UNITARIO`, `FECHA_INGRESO`, `FECHA_VENCIMIENTO` |

- `LOTE.ID_INSUMO` referencia `CAT_PRODUCTO(ID_PRODUCTO)`; los insumos se toman de la categoría
  `Insumos`, con fallback a todos los productos activos.
- Secuencias `INV_MOVIMIENTO_SEQ` e `INV_LOTE_SEQ`.
- Índice `IDX_LOTE_FIFO (ID_INSUMO, FECHA_VENCIMIENTO)` para soportar el orden FIFO.
- Restricciones de integridad: cantidad positiva, disponible entre `0` y la cantidad ingresada,
  fechas obligatorias y tipo de movimiento válido (`INGRESO`, `EGRESO`, `AJUSTE`).

### Funcionalidad

- **Ingreso de lote**: valida insumo activo, cantidad mayor a cero, costo no negativo y
  vencimiento igual o posterior a hoy; persiste cabecera y detalle de forma transaccional.
- **Listado FIFO**: lotes vigentes de un insumo (o de todos), excluyendo agotados y vencidos,
  ordenados estrictamente por vencimiento ascendente con desempate por ID.
- **Reporte de caducidad**: lotes con saldo que vencen dentro del umbral de días indicado,
  incluidos los vencidos, con estado `VENCIDO`, `VENCE HOY`, `CRÍTICO`, `POR VENCER` o `VIGENTE`.
- **Datos demo**: insumo `INS-001` (Harina de trigo) y tres lotes con vencimiento a -2, +10 y
  +40 días para probar el filtrado, el orden FIFO y el reporte.

### Rutas

| Método | Ruta | Descripción |
|---|---|---|
| GET | `/inventario/ingreso` | Formulario de registro de ingreso |
| POST | `/inventario/ingreso` | Registra cabecera + lote y redirige al listado |
| GET | `/inventario/lotes?idInsumo=` | Lotes vigentes en orden FIFO |
| GET | `/inventario/reporte-caducidad?dias=30` | Reporte de caducidad por umbral |

### Pruebas

```bash
mvn test
```

La suite cubre el ordenamiento FIFO y la exclusión de lotes vencidos/agotados
(`InventarioServiceTest`), la persistencia transaccional de cabecera y detalle
(`IngresoLoteIntegrationTest`) y el renderizado de las tres vistas
(`InventarioWebIntegrationTest`).

## Cómo ejecutarlo

### Sin Oracle (perfil `demo`, H2 en memoria con datos de ejemplo)

```bash
mvn spring-boot:run -Dspring-boot.run.profiles=demo
```

Luego abrir <http://localhost:8090/catalogo> o <http://localhost:8090/inventario/ingreso>.

### Con Oracle

```bash
sqlplus SAPI/sapi@//localhost:1521/XEPDB1 @db/01_schema_oracle.sql
sqlplus SAPI/sapi@//localhost:1521/XEPDB1 @db/02_data_oracle.sql
sqlplus SAPI/sapi@//localhost:1521/XEPDB1 @db/03_schema_insumos_oracle.sql
sqlplus SAPI/sapi@//localhost:1521/XEPDB1 @db/04_data_insumos_oracle.sql
mvn spring-boot:run
```

Credenciales configurables por variables de entorno: `DB_URL`, `DB_USERNAME`, `DB_PASSWORD`.

## Roadmap

- [x] Catálogo de productos (CRUD con baja lógica)
- [ ] CRUD de categorías
- [x] Inventario: ingreso de lotes, movimientos y listado/reporte FIFO (Asignación 3)
- [ ] Alertas: comparación stock real vs `stock_minimo` y notificaciones
- [x] Despacho: salidas y trazabilidad
- [x] Seguridad: usuarios, roles y auditoría
