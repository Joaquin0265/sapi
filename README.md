# SAPI · Sistema de Alertas Inteligentes para Control Preventivo

Sistema para una panificadora cuyo objetivo es **evitar quiebres de stock** mediante alertas
tempranas y control preventivo de inventario.

> Estado actual: **Asignación 5 · Módulo Analítico de Alertas y Sugerencias** implementado
> de punta a punta, junto con los módulos de Catálogo, Seguridad, Inventario y Despacho.

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
| `DESPACHO` | `ID_DESPACHO`, `ID_MOVIMIENTO`, `ID_INSUMO`, `CANTIDAD`, `FECHA_HORA` |

- `LOTE.ID_INSUMO` referencia `CAT_INSUMO(ID_INSUMO)`; el stock se calcula sumando
  `CANTIDAD_DISPONIBLE` de los lotes vigentes (no vencidos) de cada insumo.
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
- **Datos demo**: cinco insumos con escenarios de semáforo (rojo, ámbar, verde, rojo sin
  retiros y sin umbral), lotes vigentes y retiros de los últimos 7 días para ejercitar la
  proyección histórica.

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

## Módulo Despacho · Salidas con FIFO real

El despacho dejó de recibir el stock desde la vista: ahora lee el stock real, valida que
alcance y descuenta los lotes en orden FIFO dentro de la misma transacción que registra la
cabecera `MOVIMIENTO(EGRESO)` y el detalle `DESPACHO`. Si falta stock, no se descuenta ningún
lote ni se persiste el movimiento.

| Método | Ruta | Descripción |
|---|---|---|
| GET | `/despacho` | Formulario de salida (insumos activos y stock real) |
| POST | `/despacho/registrar` | Valida, consume FIFO y registra la trazabilidad |

## Módulo Alertas · Motor Analítico y Sugerencias (Asignación 5)

Motor de alertas inteligentes que compara el **stock real** de cada insumo contra su
política de reposición y proyecta el consumo con los retiros históricos. Las alertas se
calculan en cada petición (no se persisten), de modo que un quiebre aparece **de inmediato**
después de registrar un despacho.

### Reglas implementadas

| Regla | Descripción |
|---|---|
| **RN-03 · Semáforo** | `stock_actual − umbral_minimo <= 0` → **ROJO (quiebre)**. `stock <= mínimo × 1.20` → **AMARILLO (por reponer)**. Si supera ese margen → **VERDE**. Sin umbral configurado → **SIN_UMBRAL**. |
| **RN-04 · Histórico** | Con retiros en los últimos **7 días** se calcula `consumo_diario = salidas / 7` y se proyecta la fecha de quiebre. **Sin retiros no se inventa un promedio**: se devuelve la *reposición manual estándar* (`mínimo − stock`). |
| **RF-05 · Sugerencia** | La cantidad sugerida cubre un horizonte de **14 días**: `consumo_diario × 14 − stock_actual`, acotada a cero. |

Además, un insumo cuyo saldo está en cero o por debajo de su `stock_critico` se marca con el
subdistintivo **“Quiebre inminente”**.

### Artefactos

| Artefacto | Ruta |
|---|---|
| Modelo de dominio | `alertas/domain/model/{NivelAlerta,OrigenSugerencia,EvaluacionStock,SugerenciaCompra}.java` |
| Motor | `alertas/application/service/AlertaService.java` |
| DTOs de vista | `alertas/application/dto/{AlertaStockView,ResumenAlertas}.java` |
| Repositorios de consulta | `alertas/infrastructure/persistence/{StockInsumoRepository,SalidaInsumoRepository}.java` |
| Controlador | `alertas/infrastructure/web/AlertaController.java` |
| Vistas | `templates/alertas/panel.html`, `templates/shared/dashboard.html` |
| DDL Oracle | `db/05_alertas_oracle.sql` |

### Consultas clave

- **Stock real**: `SUM(LOTE.CANTIDAD_DISPONIBLE)` por insumo, excluyendo lotes vencidos y agotados.
- **Retiros históricos**: `SUM(DESPACHO.CANTIDAD)` por insumo desde el inicio de la ventana de 7 días.
- El índice `IX_DESPACHO_INSUMO_FECHA (ID_INSUMO, FECHA_HORA)` soporta la ventana histórica.

### Rutas

| Método | Ruta | Descripción |
|---|---|---|
| GET | `/` | Tablero del administrador con aviso rojo si hay quiebres |
| GET | `/alertas` | Panel completo de semáforos, proyección y compra sugerida |
| GET | `/alertas?nivel=ROJO\|AMARILLO\|VERDE\|SIN_UMBRAL` | Panel filtrado por nivel |

El aviso rojo del inicio y el panel `/alertas` están protegidos con la autoridad
`ADMINISTRADOR`.

### Pruebas

```bash
mvn test
```

| Prueba | Qué verifica |
|---|---|
| `EvaluacionStockTest` | **Caso exigido RN-03**: con `stock = 10` y `umbral = 15` el quiebre se dispara (`10 − 15 = −5 <= 0`); también el límite exacto (`15 − 15 = 0`). |
| `SugerenciaCompraTest` | **Caso exigido RN-04**: sin salidas en 7 días omite el cálculo histórico y devuelve `REPOSICION_MANUAL`; con salidas calcula consumo, cobertura y compra a 14 días. |
| `AlertaServiceTest` | Combinación de stock, umbrales y retiros, orden por severidad y ausencia de umbral. |
| `AlertaWebIntegrationTest` | **Prueba funcional RF-04**: un retiro artificial que deja `LEV-001` bajo su mínimo enciende el rojo en `/alertas` y el aviso del inicio; el operario recibe 403. |
| `HomeDashboardTest` | El administrador ve el tablero y el operario es redirigido a `/despacho`. |
| `DespachoServiceTest` / `DespachoFifoTest` / `DespachoIntegrationTest` | Validación de stock, consumo FIFO real y no persistencia cuando falta stock. |

## Cómo ejecutarlo

### Sin Oracle (perfil `demo`, H2 en memoria con datos de ejemplo)

```bash
mvn spring-boot:run -Dspring-boot.run.profiles=demo
```

Luego abrir <http://localhost:8090/login> e ingresar con:

| Usuario | Clave | Rol | Qué ve |
|---|---|---|---|
| `admin` | `admin123` | `ADMINISTRADOR` | Tablero con aviso rojo, `/alertas`, catálogo e inventario |
| `operario` | `operario123` | `OPERARIO_ALMACEN` | Formulario de despacho |

El perfil `demo` siembra cinco insumos: uno en rojo (`HAR-001`), uno ámbar (`AZU-001`),
uno verde (`LEV-001`), uno rojo sin retiros (`MAN-001`) y uno sin umbral (`SAL-001`).

### Con Oracle

```bash
sqlplus SAPI/sapi@//localhost:1521/XEPDB1 @db/01_schema_oracle.sql
sqlplus SAPI/sapi@//localhost:1521/XEPDB1 @db/02_data_oracle.sql
sqlplus SAPI/sapi@//localhost:1521/XEPDB1 @db/03_schema_insumos_oracle.sql
sqlplus SAPI/sapi@//localhost:1521/XEPDB1 @db/04_data_insumos_oracle.sql
sqlplus SAPI/sapi@//localhost:1521/XEPDB1 @db/03_schema_seguridad_oracle.sql
sqlplus SAPI/sapi@//localhost:1521/XEPDB1 @db/05_alertas_oracle.sql
sqlplus SAPI/sapi@//localhost:1521/XEPDB1 @db/06_fix_claves_seguridad_oracle.sql
mvn spring-boot:run
```

> `05_alertas_oracle.sql` migra `LOTE.ID_INSUMO` de `CAT_PRODUCTO` a `CAT_INSUMO`, crea la
> tabla `DESPACHO` y su secuencia. **Haga respaldo antes de ejecutarlo** y revise la consulta
> de control de lotes sin insumo equivalente que incluye el script.

> `06_fix_claves_seguridad_oracle.sql` corrige los hashes BCrypt de `admin` y `operario`.
> Solo es imprescindible si la base ya existía con una versión anterior del script de
> seguridad; en una instalación nueva los hashes ya vienen bien.

Credenciales configurables por variables de entorno: `DB_URL`, `DB_USERNAME`, `DB_PASSWORD`.

## Roadmap

- [x] Catálogo de productos (CRUD con baja lógica)
- [ ] CRUD de categorías
- [x] Inventario: ingreso de lotes, movimientos y listado/reporte FIFO (Asignación 3)
- [x] Alertas: comparación stock real vs `stock_minimo`, semáforo y sugerencias de compra (Asignación 5)
- [x] Despacho: salidas con descuento FIFO y trazabilidad
- [x] Seguridad: usuarios, roles y auditoría
