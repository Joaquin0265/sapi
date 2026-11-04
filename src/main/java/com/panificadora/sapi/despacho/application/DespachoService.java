package com.panificadora.sapi.despacho.application;

import com.panificadora.sapi.catalogo.domain.model.Insumo;
import com.panificadora.sapi.catalogo.infrastructure.persistence.InsumoRepository;
import com.panificadora.sapi.despacho.domain.Despacho;
import com.panificadora.sapi.despacho.infrastructure.DespachoRepository;
import com.panificadora.sapi.inventario.application.service.InventarioService;
import com.panificadora.sapi.inventario.domain.exception.ReglaNegocioException;
import com.panificadora.sapi.inventario.domain.model.Movimiento;
import com.panificadora.sapi.inventario.domain.model.TipoMovimiento;
import com.panificadora.sapi.inventario.infrastructure.persistence.MovimientoRepository;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Caso de uso de salida de insumos a producción.
 *
 * <p>Trabaja en una sola transacción: valida y descuenta los lotes en orden
 * FIFO, registra la cabecera {@code MOVIMIENTO(EGRESO)} y el detalle
 * {@code DESPACHO}. Las tres operaciones se confirman o se revierten juntas.</p>
 */
@Service
@RequiredArgsConstructor
public class DespachoService {

    private final DespachoRepository despachoRepository;
    private final MovimientoRepository movimientoRepository;
    private final InsumoRepository insumoRepository;
    private final InventarioService inventarioService;

    @Transactional
    public Despacho registrarSalida(Long idInsumo, BigDecimal cantidad, Long idPersonal) {
        if (idInsumo == null) {
            throw new ReglaNegocioException("Debe seleccionar el insumo que se retira.");
        }
        if (cantidad == null || cantidad.compareTo(BigDecimal.ZERO) <= 0) {
            throw new ReglaNegocioException("La cantidad a retirar debe ser mayor a cero.");
        }

        Insumo insumo = insumoRepository.findByIdAndActivoTrue(idInsumo)
                .orElseThrow(() -> new ReglaNegocioException(
                        "El insumo seleccionado no existe o está inactivo."));

        // Valida y descuenta lotes (RN-01). Si no alcanza, lanza excepción y no
        // se persiste ni la cabecera ni el detalle.
        inventarioService.consumirFifo(idInsumo, cantidad);

        Movimiento movimiento = Movimiento.builder()
                .tipoMovimiento(TipoMovimiento.EGRESO)
                .fechaMovimiento(LocalDateTime.now())
                .idPersonal(idPersonal)
                .observacion("Salida de insumo a producción")
                .build();
        movimientoRepository.save(movimiento);

        Despacho despacho = Despacho.builder()
                .movimiento(movimiento)
                .insumo(insumo)
                .cantidad(cantidad)
                .fechaHora(movimiento.getFechaMovimiento())
                .build();

        return despachoRepository.save(despacho);
    }
}
