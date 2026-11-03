package com.panificadora.sapi.inventario.application.service;

import com.panificadora.sapi.catalogo.domain.model.Producto;
import com.panificadora.sapi.catalogo.infrastructure.persistence.ProductoRepository;
import com.panificadora.sapi.inventario.application.dto.IngresoLoteForm;
import com.panificadora.sapi.inventario.application.dto.LoteCaducidadView;
import com.panificadora.sapi.inventario.domain.exception.ReglaNegocioException;
import com.panificadora.sapi.inventario.domain.model.Lote;
import com.panificadora.sapi.inventario.domain.model.Movimiento;
import com.panificadora.sapi.inventario.domain.model.TipoMovimiento;
import com.panificadora.sapi.inventario.infrastructure.persistence.LoteRepository;
import com.panificadora.sapi.inventario.infrastructure.persistence.MovimientoRepository;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Casos de uso del módulo Inventario: ingreso de lotes y consultas FIFO.
 *
 * <p>RN-01: los lotes vigentes de un insumo se listan siempre por
 * {@code fecha_vencimiento} ascendente. El repositorio ya ordena usando
 * {@code IDX_LOTE_FIFO}; el servicio reordena de forma defensiva para que la
 * regla no dependa de la implementación del repositorio.</p>
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class InventarioService {

    private static final String CATEGORIA_INSUMOS = "Insumos";

    private final MovimientoRepository movimientoRepository;
    private final LoteRepository loteRepository;
    private final ProductoRepository productoRepository;

    private static final Comparator<Lote> ORDEN_FIFO = Comparator
            .comparing(Lote::getFechaVencimiento)
            .thenComparing(Lote::getId, Comparator.nullsLast(Comparator.naturalOrder()));

    // ------------------------------------------------------------------
    // Consultas FIFO
    // ------------------------------------------------------------------

    /**
     * Lotes vigentes de un insumo ordenados estrictamente por vencimiento
     * ascendente (RN-01). Se excluyen lotes agotados y ya vencidos.
     */
    public List<Lote> listarLotesVigentes(Long idInsumo) {
        if (idInsumo == null) {
            throw new ReglaNegocioException("Debe indicar el insumo para consultar sus lotes.");
        }

        LocalDate hoy = LocalDate.now();
        return loteRepository.findDisponiblesPorInsumo(idInsumo).stream()
                .filter(lote -> lote.estaVigente(hoy))
                .sorted(ORDEN_FIFO)
                .toList();
    }

    /** Vista consolidada de todos los lotes vigentes, también en orden FIFO. */
    public List<Lote> listarTodosLosLotesVigentes() {
        LocalDate hoy = LocalDate.now();
        return loteRepository.findDisponibles().stream()
                .filter(lote -> lote.estaVigente(hoy))
                .sorted(ORDEN_FIFO)
                .toList();
    }

    /**
     * Reporte de caducidad: lotes con saldo cuyo vencimiento ocurre dentro de
     * los próximos {@code diasUmbral} días, incluidos los ya vencidos.
     */
    public List<LoteCaducidadView> generarReporteCaducidad(int diasUmbral) {
        if (diasUmbral < 0) {
            throw new ReglaNegocioException("El umbral de días no puede ser negativo.");
        }

        LocalDate hoy = LocalDate.now();
        LocalDate limite = hoy.plusDays(diasUmbral);

        return loteRepository.findDisponiblesHasta(limite).stream()
                .sorted(ORDEN_FIFO)
                .map(lote -> {
                    long dias = lote.diasHastaVencimiento(hoy);
                    return new LoteCaducidadView(
                            lote.getId(),
                            lote.getCodigoLote(),
                            lote.getInsumo().getCodigo(),
                            lote.getInsumo().getNombre(),
                            lote.getCantidadDisponible(),
                            lote.getFechaVencimiento(),
                            dias,
                            estadoCaducidad(dias)
                    );
                })
                .toList();
    }

    // ------------------------------------------------------------------
    // Comandos
    // ------------------------------------------------------------------

    /**
     * Registra un ingreso completo (cabecera + lote) en una sola transacción.
     * Si falla el detalle, la cabecera también se revierte.
     */
    @Transactional
    public Movimiento registrarIngreso(IngresoLoteForm form) {
        validarIngreso(form);

        Producto insumo = productoRepository.findById(form.getIdInsumo())
                .filter(Producto::estaActivo)
                .orElseThrow(() -> new ReglaNegocioException(
                        "El insumo seleccionado no existe o está inactivo."));

        if (form.getFechaVencimiento().isBefore(LocalDate.now())) {
            throw new ReglaNegocioException("La fecha de vencimiento no puede ser anterior a hoy.");
        }

        Movimiento movimiento = Movimiento.builder()
                .tipoMovimiento(TipoMovimiento.INGRESO)
                .fechaMovimiento(LocalDateTime.now())
                .idPersonal(form.getIdPersonal())
                .observacion(limpiar(form.getObservacion()))
                .build();
        movimientoRepository.save(movimiento);

        Lote lote = Lote.builder()
                .movimiento(movimiento)
                .insumo(insumo)
                .codigoLote(limpiar(form.getCodigoLote()))
                .cantidad(form.getCantidad())
                .cantidadDisponible(form.getCantidad())
                .costoUnitario(form.getCostoUnitario())
                .fechaIngreso(movimiento.getFechaMovimiento())
                .fechaVencimiento(form.getFechaVencimiento())
                .build();
        loteRepository.save(lote);

        return movimiento;
    }

    // ------------------------------------------------------------------
    // Apoyo
    // ------------------------------------------------------------------

    /**
     * Insumos activos: los de la categoría "Insumos" si existe; si no, todos
     * los productos activos para no bloquear la operación en bases nuevas.
     */
    public List<Producto> listarInsumos() {
        List<Producto> activos = productoRepository.findByActivoTrueOrderByNombreAsc();
        List<Producto> insumos = activos.stream()
                .filter(producto -> producto.getCategoria() != null
                        && producto.getCategoria().getNombre() != null
                        && producto.getCategoria().getNombre().equalsIgnoreCase(CATEGORIA_INSUMOS))
                .toList();
        return insumos.isEmpty() ? activos : insumos;
    }

    private void validarIngreso(IngresoLoteForm form) {
        if (form == null) {
            throw new ReglaNegocioException("Los datos del ingreso son obligatorios.");
        }
        if (form.getIdInsumo() == null) {
            throw new ReglaNegocioException("Debe seleccionar el insumo que ingresa.");
        }
        if (form.getCantidad() == null || form.getCantidad().compareTo(BigDecimal.ZERO) <= 0) {
            throw new ReglaNegocioException("La cantidad del lote debe ser mayor a cero.");
        }
        if (form.getFechaVencimiento() == null) {
            throw new ReglaNegocioException("La fecha de vencimiento es obligatoria.");
        }
        if (form.getCostoUnitario() != null
                && form.getCostoUnitario().compareTo(BigDecimal.ZERO) < 0) {
            throw new ReglaNegocioException("El costo unitario no puede ser negativo.");
        }
    }

    private String estadoCaducidad(long diasRestantes) {
        if (diasRestantes < 0) {
            return "VENCIDO";
        }
        if (diasRestantes == 0) {
            return "VENCE HOY";
        }
        if (diasRestantes <= 7) {
            return "CRÍTICO";
        }
        if (diasRestantes <= 30) {
            return "POR VENCER";
        }
        return "VIGENTE";
    }

    private String limpiar(String valor) {
        return (valor == null || valor.isBlank()) ? null : valor.trim();
    }
}
