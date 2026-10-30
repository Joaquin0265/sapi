# SAPI · Sistema de Alertas Inteligentes para Control Preventivo

Sistema para una panificadora cuyo objetivo es **evitar quiebres de stock** mediante alertas
tempranas y control preventivo de inventario.

> Estado actual: **Módulo de Catálogo** implementado de punta a punta (dominio, casos de uso,
> persistencia, controlador MVC y vistas).

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
| Entidades | `catalogo/domain/model/{Producto,Categoria,UnidadMedida}.java` |
| Servicio | `catalogo/application/service/CatalogoService.java` |
| DTO de formulario | `catalogo/application/dto/ProductoForm.java` |
| Repositorios | `catalogo/infrastructure/persistence/` |
| Controlador | `catalogo/infrastructure/web/CatalogoController.java` |
| Vistas | `src/main/resources/templates/catalogo/{lista,formulario}.html` |
| Scripts Oracle | `db/01_schema_oracle.sql`, `db/02_data_oracle.sql` |

Funcionalidad:

- Listado de productos activos con búsqueda por nombre.
- Alta y edición con validación en cliente y servidor (Bean Validation + reglas de negocio).
- SKU normalizado a mayúsculas y validado como único.
- Política de reposición (`stock_minimo`) que consumirá el módulo de Alertas.
- Baja lógica y reactivación.

### Rutas

| Método | Ruta | Descripción |
|---|---|---|
| GET | `/catalogo?q=` | Listado + filtro |
| GET | `/catalogo/nuevo` | Formulario de alta |
| GET | `/catalogo/{id}/editar` | Formulario de edición |
| POST | `/catalogo/guardar` | Alta/edición (POST-Redirect-GET) |
| POST | `/catalogo/{id}/desactivar` | Baja lógica |
| POST | `/catalogo/{id}/reactivar` | Reactivación |

## Cómo ejecutarlo

### Sin Oracle (perfil `demo`, H2 en memoria con datos de ejemplo)

```bash
mvn spring-boot:run -Dspring-boot.run.profiles=demo
```

Luego abrir <http://localhost:8080/catalogo>.

### Con Oracle

```bash
sqlplus SAPI/sapi@//localhost:1521/XEPDB1 @db/01_schema_oracle.sql
sqlplus SAPI/sapi@//localhost:1521/XEPDB1 @db/02_data_oracle.sql
mvn spring-boot:run
```

Credenciales configurables por variables de entorno: `DB_URL`, `DB_USERNAME`, `DB_PASSWORD`.

## Roadmap

- [x] Catálogo de productos (CRUD con baja lógica)
- [ ] CRUD de categorías
- [ ] Inventario: stock real, movimientos y kardex
- [ ] Alertas: comparación stock real vs `stock_minimo` y notificaciones
- [ ] Despacho: salidas y trazabilidad
- [ ] Seguridad: usuarios, roles y auditoría
