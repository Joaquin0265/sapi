package com.panificadora.sapi.catalogo.application.dto;

import com.panificadora.sapi.catalogo.domain.model.UnidadMedida;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Formulario de alta/edición de producto.
 *
 * <p>Vive en {@code application} (no en {@code infrastructure}) para que el
 * controlador MVC no filtre tipos web hacia los casos de uso.</p>
 */
@Getter
@Setter
@NoArgsConstructor
public class ProductoForm {

    /** null en alta, con valor en edición. */
    private Long id;

    @NotBlank(message = "El código es obligatorio.")
    @Size(max = 20, message = "El código admite como máximo 20 caracteres.")
    private String codigo;

    @NotBlank(message = "El nombre del producto es obligatorio.")
    @Size(max = 120, message = "El nombre admite como máximo 120 caracteres.")
    private String nombre;

    @Size(max = 250, message = "La descripción admite como máximo 250 caracteres.")
    private String descripcion;

    @NotNull(message = "Seleccione una categoría.")
    private Long categoriaId;

    @NotNull(message = "Seleccione la unidad de medida.")
    private UnidadMedida unidadMedida;

    @NotNull(message = "El precio es obligatorio.")
    @Positive(message = "El precio debe ser mayor a cero.")
    @Digits(integer = 10, fraction = 2, message = "El precio admite 2 decimales como máximo.")
    private BigDecimal precio;

    @NotNull(message = "El stock mínimo es obligatorio.")
    @DecimalMin(value = "0.00", message = "El stock mínimo no puede ser negativo.")
    @Digits(integer = 10, fraction = 2, message = "El stock mínimo admite 2 decimales como máximo.")
    private BigDecimal stockMinimo;

    private Boolean activo = Boolean.TRUE;

    public boolean esAlta() {
        return id == null;
    }
}
