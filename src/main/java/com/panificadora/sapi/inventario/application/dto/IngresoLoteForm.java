package com.panificadora.sapi.inventario.application.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.FutureOrPresent;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.time.LocalDate;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.format.annotation.DateTimeFormat;

/**
 * Formulario de registro de una entrada de lote.
 * Vive en {@code application} para que el controlador no filtre tipos web
 * hacia los casos de uso.
 */
@Getter
@Setter
@NoArgsConstructor
public class IngresoLoteForm {

    @NotNull(message = "Seleccione el insumo que ingresa.")
    private Long idInsumo;

    @Size(max = 40, message = "El código de lote admite como máximo 40 caracteres.")
    private String codigoLote;

    @NotNull(message = "La cantidad es obligatoria.")
    @DecimalMin(value = "0.01", message = "La cantidad debe ser mayor a cero.")
    @Digits(integer = 10, fraction = 2, message = "La cantidad admite 2 decimales como máximo.")
    private BigDecimal cantidad;

    @DecimalMin(value = "0.00", message = "El costo unitario no puede ser negativo.")
    @Digits(integer = 10, fraction = 2, message = "El costo unitario admite 2 decimales como máximo.")
    private BigDecimal costoUnitario;

    @NotNull(message = "La fecha de vencimiento es obligatoria.")
    @FutureOrPresent(message = "La fecha de vencimiento no puede ser anterior a hoy.")
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate fechaVencimiento;

    @Size(max = 250, message = "La observación admite como máximo 250 caracteres.")
    private String observacion;

    /** Trazabilidad del operario; en producción vendrá de la sesión autenticada. */
    private Long idPersonal = 101L;
}
