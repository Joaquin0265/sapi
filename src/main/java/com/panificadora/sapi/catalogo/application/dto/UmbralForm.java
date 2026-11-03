package com.panificadora.sapi.catalogo.application.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Datos del formulario "Configurar umbral" de un insumo.
 *
 * <p>Solo transporta los dos niveles. La regla cruzada
 * ({@code stockCritico < stockMinimo}) se valida en el servicio, no aquí, para
 * que también se cumpla cuando el umbral se registre desde otro adaptador
 * (por ejemplo, una carga masiva o una API futura).</p>
 */
@Getter
@Setter
@NoArgsConstructor
public class UmbralForm {

    @NotNull(message = "El stock mínimo es obligatorio.")
    @DecimalMin(value = "0.00", message = "El stock mínimo no puede ser negativo.")
    @Digits(integer = 10, fraction = 2, message = "El stock mínimo admite 2 decimales como máximo.")
    private BigDecimal stockMinimo;

    @NotNull(message = "El stock crítico es obligatorio.")
    @DecimalMin(value = "0.00", message = "El stock crítico no puede ser negativo.")
    @Digits(integer = 10, fraction = 2, message = "El stock crítico admite 2 decimales como máximo.")
    private BigDecimal stockCritico;
}
