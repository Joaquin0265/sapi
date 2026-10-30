package com.panificadora.sapi.catalogo.application.service;

import com.panificadora.sapi.catalogo.application.dto.ProductoForm;
import com.panificadora.sapi.catalogo.domain.exception.ReglaNegocioException;
import com.panificadora.sapi.catalogo.domain.model.Categoria;
import com.panificadora.sapi.catalogo.domain.model.Producto;
import com.panificadora.sapi.catalogo.domain.model.UnidadMedida;
import com.panificadora.sapi.catalogo.infrastructure.persistence.CategoriaRepository;
import com.panificadora.sapi.catalogo.infrastructure.persistence.ProductoRepository;
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
