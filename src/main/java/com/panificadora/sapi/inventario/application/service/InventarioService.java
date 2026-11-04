package com.panificadora.sapi.inventario.application.service;

import com.panificadora.sapi.catalogo.domain.model.Insumo;
import com.panificadora.sapi.catalogo.infrastructure.persistence.InsumoRepository;
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
import java.util.Map;
import java.util.stream.Collectors;
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

    private final MovimientoRepository movimientoRepository;
    private final LoteRepository loteRepository;
    private final InsumoRepository insumoRepository;

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
     * Stock utilizable por insumo: suma de {@code CANTIDAD_DISPONIBLE} de los
     * lotes con saldo y no vencidos.
     *
     * <p>Es el mismo criterio que usa el módulo de Alertas, de modo que la
     * pantalla de despacho muestra exactamente el stock que se puede retirar.
     * Los insumos sin lotes vigentes no aparecen en el mapa; la vista los
     * interpreta como cero.</p>
     */
    public Map<Long, BigDecimal> stockVigentePorInsumo() {
        LocalDate hoy = LocalDate.now();
        return loteRepository.findDisponibles().stream()
                .filter(lote -> lote.estaVigente(hoy))
                .collect(Collectors.groupingBy(
                        lote -> lote.getInsumo().getId(),
                        Collectors.reducing(BigDecimal.ZERO,
                                Lote::getCantidadDisponible,
                                BigDecimal::add)));
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

        Insumo insumo = insumoRepository.findByIdAndActivoTrue(form.getIdInsumo())
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

    /**
     * Descuenta stock consumiendo los lotes en orden FIFO (RN-01).
     *
     * <p>Los lotes vencidos se saltan porque no son stock utilizable. Si al
     * recorrer todos los lotes todavía queda cantidad pendiente, se lanza
     * {@link ReglaNegocioException} y la transacción completa se revierte.</p>
     */
    @Transactional
    public void consumirFifo(Long idInsumo, BigDecimal cantidad) {
        if (idInsumo == null) {
            throw new ReglaNegocioException("Debe indicar el insumo a consumir.");
        }
        if (cantidad == null || cantidad.compareTo(BigDecimal.ZERO) <= 0) {
            throw new ReglaNegocioException("La cantidad a consumir debe ser mayor a cero.");
        }

        LocalDate hoy = LocalDate.now();
        List<Lote> lotes = loteRepository.findDisponiblesPorInsumo(idInsumo).stream()
                .filter(lote -> !lote.getFechaVencimiento().isBefore(hoy))
                .sorted(ORDEN_FIFO)
                .toList();

        // Primera pasada: se valida el total antes de tocar cualquier lote.
        // Así un retiro inválido no deja lotes decrementados en el contexto de
        // persistencia, aunque la transacción luego se revierta.
        BigDecimal disponibleTotal = lotes.stream()
                .map(Lote::getCantidadDisponible)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        if (disponibleTotal.compareTo(cantidad) < 0) {
            throw new ReglaNegocioException(
                    "Stock insuficiente: faltan "
                            + cantidad.subtract(disponibleTotal).stripTrailingZeros().toPlainString()
                            + " unidades para completar el retiro.");
        }

        // Segunda pasada: consumo FIFO real.
        BigDecimal pendiente = cantidad;
        for (Lote lote : lotes) {
            if (pendiente.signum() <= 0) {
                break;
            }
            BigDecimal disponible = lote.getCantidadDisponible();
            BigDecimal consumido = disponible.min(pendiente);
            lote.setCantidadDisponible(disponible.subtract(consumido));
            pendiente = pendiente.subtract(consumido);
        }
    }

    // ------------------------------------------------------------------
    // Apoyo
    // ------------------------------------------------------------------

    /**
     * Insumos activos del catálogo, ordenados por nombre. Son la única fuente
     * de verdad para el ingreso de lotes y las alertas de stock.
     */
    public List<Insumo> listarInsumos() {
        return insumoRepository.findByActivoTrueOrderByNombreAsc();
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
