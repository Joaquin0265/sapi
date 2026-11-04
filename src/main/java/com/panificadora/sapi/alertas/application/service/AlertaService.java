package com.panificadora.sapi.alertas.application.service;

import com.panificadora.sapi.alertas.application.dto.AlertaStockView;
import com.panificadora.sapi.alertas.application.dto.ResumenAlertas;
import com.panificadora.sapi.alertas.domain.model.EvaluacionStock;
import com.panificadora.sapi.alertas.domain.model.NivelAlerta;
import com.panificadora.sapi.alertas.domain.model.SugerenciaCompra;
import com.panificadora.sapi.alertas.infrastructure.persistence.SalidaInsumoRepository;
import com.panificadora.sapi.alertas.infrastructure.persistence.StockInsumoRepository;
import com.panificadora.sapi.catalogo.domain.model.Insumo;
import com.panificadora.sapi.catalogo.domain.model.UmbralSeguridad;
import com.panificadora.sapi.catalogo.infrastructure.persistence.InsumoRepository;
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
 * Motor de Alertas Inteligentes (Asignación 5).
 *
 * <p>Compara el stock real de cada insumo activo contra su umbral y calcula la
 * proyección de consumo con los retiros de los últimos
 * {@value SugerenciaCompra#DIAS_VENTANA} días.</p>
 *
 * <p>El servicio no persiste alertas: el panel siempre refleja el estado
 * actual, que es lo que exige la prueba funcional (aviso inmediato).</p>
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AlertaService {

    private final InsumoRepository insumoRepository;
    private final StockInsumoRepository stockInsumoRepository;
    private final SalidaInsumoRepository salidaInsumoRepository;

    /**
     * Evalúa todos los insumos activos y los ordena por severidad: primero los
     * quiebres, y dentro de cada nivel los que tienen mayor faltante.
     */
    public List<AlertaStockView> evaluarTodas() {
        LocalDate hoy = LocalDate.now();
        LocalDateTime desde = hoy.minusDays(SugerenciaCompra.DIAS_VENTANA - 1L).atStartOfDay();

        Map<Long, BigDecimal> stockPorInsumo = stockInsumoRepository
                .stockVigentePorInsumo(hoy).stream()
                .collect(Collectors.toMap(
                        StockInsumoRepository.StockPorInsumo::getIdInsumo,
                        proyeccion -> valorSeguro(proyeccion.getStock())));

        Map<Long, BigDecimal> salidasPorInsumo = salidaInsumoRepository
                .salidasDesde(desde).stream()
                .collect(Collectors.toMap(
                        SalidaInsumoRepository.SalidaPorInsumo::getIdInsumo,
                        proyeccion -> valorSeguro(proyeccion.getSalidas())));

        return insumoRepository.findByActivoTrueOrderByNombreAsc().stream()
                .map(insumo -> evaluar(insumo,
                        stockPorInsumo.getOrDefault(insumo.getId(), BigDecimal.ZERO),
                        salidasPorInsumo.getOrDefault(insumo.getId(), BigDecimal.ZERO),
                        hoy))
                .sorted(Comparator
                        .comparing((AlertaStockView alerta) -> alerta.getNivel().ordinal())
                        .thenComparing(AlertaStockView::getFaltante,
                                Comparator.reverseOrder()))
                .toList();
    }

    /** Contadores por nivel para las tarjetas del panel. */
    public ResumenAlertas resumir() {
        return new ResumenAlertas(evaluarTodas());
    }

    /** Aviso del inicio: solo interesa saber si hay algo en rojo. */
    public boolean hayAlertasRojas() {
        return evaluarTodas().stream()
                .anyMatch(alerta -> alerta.getNivel() == NivelAlerta.ROJO);
    }

    public List<AlertaStockView> evaluarPorNivel(NivelAlerta nivel) {
        if (nivel == null) {
            return evaluarTodas();
        }
        return evaluarTodas().stream()
                .filter(alerta -> alerta.getNivel() == nivel)
                .toList();
    }

    private AlertaStockView evaluar(Insumo insumo,
                                    BigDecimal stockActual,
                                    BigDecimal salidasVentana,
                                    LocalDate hoy) {
        UmbralSeguridad umbral = insumo.getUmbral();
        BigDecimal stockMinimo = umbral == null ? null : umbral.getStockMinimo();
        BigDecimal stockCritico = umbral == null ? null : umbral.getStockCritico();

        EvaluacionStock evaluacion = new EvaluacionStock(stockActual, stockMinimo, stockCritico);
        SugerenciaCompra sugerencia = SugerenciaCompra.calcular(
                stockActual,
                stockMinimo,
                salidasVentana,
                SugerenciaCompra.DIAS_VENTANA,
                SugerenciaCompra.HORIZONTE_DIAS,
                hoy);

        return AlertaStockView.builder()
                .idInsumo(insumo.getId())
                .codigo(insumo.getCodigo())
                .nombre(insumo.getNombre())
                .unidad(insumo.getUnidadMedida() == null ? "—" : insumo.getUnidadMedida().getEtiqueta())
                .stockActual(evaluacion.getStockActual())
                .stockMinimo(evaluacion.getStockMinimo())
                .stockCritico(evaluacion.getStockCritico())
                .nivel(evaluacion.getNivel())
                .quiebreInminente(evaluacion.isQuiebreInminente())
                .faltante(evaluacion.getFaltante())
                .consumoDiario(sugerencia.getConsumoDiario())
                .diasCobertura(sugerencia.getDiasCobertura())
                .fechaEstimadaQuiebre(sugerencia.getFechaEstimadaQuiebre())
                .cantidadSugerida(sugerencia.getCantidadSugerida())
                .origenSugerencia(sugerencia.getOrigen())
                .build();
    }

    private BigDecimal valorSeguro(BigDecimal valor) {
        return valor == null ? BigDecimal.ZERO : valor;
    }
}
