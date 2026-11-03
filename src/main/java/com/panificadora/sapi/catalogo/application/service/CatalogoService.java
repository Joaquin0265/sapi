package com.panificadora.sapi.catalogo.application.service;

import com.panificadora.sapi.catalogo.application.dto.ProductoForm;
import com.panificadora.sapi.catalogo.domain.exception.ReglaNegocioException;
import com.panificadora.sapi.catalogo.domain.model.Categoria;
import com.panificadora.sapi.catalogo.domain.model.Insumo;
import com.panificadora.sapi.catalogo.domain.model.Producto;
import com.panificadora.sapi.catalogo.domain.model.UmbralSeguridad;
import com.panificadora.sapi.catalogo.domain.model.UnidadMedida;
import com.panificadora.sapi.catalogo.infrastructure.persistence.CategoriaRepository;
import com.panificadora.sapi.catalogo.infrastructure.persistence.InsumoRepository;
import com.panificadora.sapi.catalogo.infrastructure.persistence.ProductoRepository;
import com.panificadora.sapi.catalogo.infrastructure.persistence.UmbralSeguridadRepository;
import java.math.BigDecimal;
import java.util.List;
import java.util.Locale;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Casos de uso del módulo Catálogo.
 *
 * <p>Toda la lógica de negocio vive aquí: normalización de SKU, unicidad,
 * validación de la política de reposición y baja lógica. El controlador solo
 * traduce HTTP &lt;-&gt; casos de uso.</p>
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CatalogoService {

    private final ProductoRepository productoRepository;
    private final CategoriaRepository categoriaRepository;
    private final InsumoRepository insumoRepository;
    private final UmbralSeguridadRepository umbralSeguridadRepository;

    // ------------------------------------------------------------------
    // Consultas
    // ------------------------------------------------------------------

    public List<Producto> listarProductos(String filtro) {
        if (filtro == null || filtro.isBlank()) {
            return productoRepository.findByActivoTrueOrderByNombreAsc();
        }
        return productoRepository.findByActivoTrueAndNombreContainingIgnoreCaseOrderByNombreAsc(filtro.trim());
    }

    public List<Producto> listarTodos() {
        return productoRepository.findAllByOrderByNombreAsc();
    }

    public List<Categoria> listarCategorias() {
        return categoriaRepository.findByActivoTrueOrderByNombreAsc();
    }

    public Producto obtenerProducto(Long id) {
        return productoRepository.findByIdAndActivoTrue(id)
                .orElseThrow(() -> new ReglaNegocioException("El producto solicitado no existe o está inactivo."));
    }

    public List<Insumo> listarInsumos(String filtro) {
        if (filtro == null || filtro.isBlank()) {
            return insumoRepository.findByActivoTrueOrderByNombreAsc();
        }
        return insumoRepository.findByActivoTrueAndNombreContainingIgnoreCaseOrderByNombreAsc(filtro.trim());
    }

    public Insumo obtenerInsumo(Long id) {
        return insumoRepository.findByIdAndActivoTrue(id)
                .orElseThrow(() -> new ReglaNegocioException("El insumo solicitado no existe o está inactivo."));
    }

    // ------------------------------------------------------------------
    // Formularios (armado del DTO para la vista)
    // ------------------------------------------------------------------

    public ProductoForm prepararAlta() {
        ProductoForm form = new ProductoForm();
        form.setUnidadMedida(UnidadMedida.UNIDAD);
        form.setStockMinimo(BigDecimal.ZERO);
        form.setActivo(Boolean.TRUE);
        return form;
    }

    public ProductoForm prepararEdicion(Long id) {
        Producto producto = obtenerProducto(id);
        ProductoForm form = new ProductoForm();
        form.setId(producto.getId());
        form.setCodigo(producto.getCodigo());
        form.setNombre(producto.getNombre());
        form.setDescripcion(producto.getDescripcion());
        form.setCategoriaId(producto.getCategoria().getId());
        form.setUnidadMedida(producto.getUnidadMedida());
        form.setPrecio(producto.getPrecio());
        form.setStockMinimo(producto.getStockMinimo());
        form.setActivo(producto.getActivo());
        return form;
    }

    // ------------------------------------------------------------------
    // Comandos
    // ------------------------------------------------------------------

    @Transactional
    public Producto registrar(ProductoForm form) {
        String codigo = normalizarCodigo(form.getCodigo());

        if (productoRepository.existsByCodigoIgnoreCase(codigo)) {
            throw new ReglaNegocioException("Ya existe un producto con el código " + codigo + ".");
        }

        validarPoliticaReposicion(form);

        Producto producto = new Producto();
        producto.setCodigo(codigo);
        aplicarDatosEditables(producto, form);
        return productoRepository.save(producto);
    }

    @Transactional
    public Producto actualizar(ProductoForm form) {
        if (form.getId() == null) {
            throw new ReglaNegocioException("No se puede actualizar un producto sin identificador.");
        }

        Producto producto = obtenerProducto(form.getId());
        String codigo = normalizarCodigo(form.getCodigo());

        if (productoRepository.existsByCodigoIgnoreCaseAndIdNot(codigo, producto.getId())) {
            throw new ReglaNegocioException("El código " + codigo + " ya está asignado a otro producto.");
        }

        validarPoliticaReposicion(form);

        producto.setCodigo(codigo);
        aplicarDatosEditables(producto, form);

        if (!Boolean.TRUE.equals(form.getActivo())) {
            producto.desactivar();
        }

        return producto;
    }

    /** Baja lógica: nunca se borra un producto con historial de despachos. */
    @Transactional
    public void desactivar(Long id) {
        Producto producto = productoRepository.findById(id)
                .orElseThrow(() -> new ReglaNegocioException("El producto a desactivar no existe."));
        producto.desactivar();
    }

    @Transactional
    public void reactivar(Long id) {
        Producto producto = productoRepository.findById(id)
                .orElseThrow(() -> new ReglaNegocioException("El producto a reactivar no existe."));
        producto.reactivar();
    }

    /**
     * Registra o actualiza el umbral de seguridad de un insumo (RF-01 / RF-02).
     *
     * <p>Regla de negocio: {@code stockCritico} debe ser estrictamente menor que
     * {@code stockMinimo}. Si se viola, no se toca la base de datos: se lanza
     * {@link ReglaNegocioException} para que el adaptador web muestre el mensaje
     * al propietario.</p>
     *
     * <p>Es idempotente: si el insumo ya tenía umbral, lo actualiza; si no,
     * lo crea. Así el botón "Guardar" del modal sirve tanto para configurar por
     * primera vez como para corregir valores.</p>
     */
    @Transactional
    public UmbralSeguridad registrarUmbral(Long idInsumo, BigDecimal stockMinimo, BigDecimal stockCritico) {
        if (idInsumo == null) {
            throw new ReglaNegocioException("Debe indicar el insumo al que pertenece el umbral.");
        }
        validarRangoUmbral(stockMinimo, stockCritico);

        Insumo insumo = insumoRepository.findByIdAndActivoTrue(idInsumo)
                .orElseThrow(() -> new ReglaNegocioException(
                        "El insumo solicitado no existe o está inactivo."));

        UmbralSeguridad umbral = umbralSeguridadRepository.findByInsumoId(idInsumo)
                .orElseGet(() -> UmbralSeguridad.builder().insumo(insumo).build());

        umbral.setStockMinimo(stockMinimo);
        umbral.setStockCritico(stockCritico);

        UmbralSeguridad guardado = umbralSeguridadRepository.save(umbral);

        // Mantiene coherentes ambos lados de la relación 1:1 en memoria.
        if (insumo.getUmbral() != guardado) {
            insumo.setUmbral(guardado);
        }
        return guardado;
    }

    // ------------------------------------------------------------------
    // Reglas internas
    // ------------------------------------------------------------------

    private void aplicarDatosEditables(Producto producto, ProductoForm form) {
        producto.setNombre(form.getNombre().trim());
        producto.setDescripcion(limpiar(form.getDescripcion()));
        producto.setCategoria(obtenerCategoriaActiva(form.getCategoriaId()));
        producto.setUnidadMedida(form.getUnidadMedida());
        producto.setPrecio(form.getPrecio());
        producto.setStockMinimo(form.getStockMinimo());
    }

    private Categoria obtenerCategoriaActiva(Long categoriaId) {
        if (categoriaId == null) {
            throw new ReglaNegocioException("Debe seleccionar una categoría.");
        }
        return categoriaRepository.findById(categoriaId)
                .filter(Categoria::estaActiva)
                .orElseThrow(() -> new ReglaNegocioException(
                        "La categoría seleccionada no existe o está inactiva."));
    }

    private void validarPoliticaReposicion(ProductoForm form) {
        if (form.getPrecio() == null || form.getPrecio().compareTo(BigDecimal.ZERO) <= 0) {
            throw new ReglaNegocioException("El precio de venta debe ser mayor a cero.");
        }
        if (form.getStockMinimo() == null || form.getStockMinimo().compareTo(BigDecimal.ZERO) < 0) {
            throw new ReglaNegocioException("El stock mínimo no puede ser negativo.");
        }
    }

    /**
     * Validación de código de la restricción {@code CHK_UMBRALES}.
     * El mensaje es explícito porque lo lee directamente el propietario en la vista.
     */
    private void validarRangoUmbral(BigDecimal stockMinimo, BigDecimal stockCritico) {
        if (stockMinimo == null || stockCritico == null) {
            throw new ReglaNegocioException("Debe indicar el stock mínimo y el stock crítico.");
        }
        if (stockMinimo.compareTo(BigDecimal.ZERO) < 0) {
            throw new ReglaNegocioException("El stock mínimo no puede ser negativo.");
        }
        if (stockCritico.compareTo(BigDecimal.ZERO) < 0) {
            throw new ReglaNegocioException("El stock crítico no puede ser negativo.");
        }
        if (stockCritico.compareTo(stockMinimo) >= 0) {
            throw new ReglaNegocioException(
                    "El stock crítico (" + stockCritico.toPlainString() + ") debe ser menor que el stock mínimo ("
                            + stockMinimo.toPlainString() + ").");
        }
    }

    /** SKU en mayúsculas y sin espacios: evita duplicados tipo "pan-01" vs "PAN-01". */
    private String normalizarCodigo(String codigo) {
        if (codigo == null || codigo.isBlank()) {
            throw new ReglaNegocioException("El código del producto es obligatorio.");
        }
        return codigo.trim().toUpperCase(Locale.ROOT);
    }

    private String limpiar(String valor) {
        return (valor == null || valor.isBlank()) ? null : valor.trim();
    }
}
