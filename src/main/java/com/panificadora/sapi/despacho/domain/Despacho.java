package com.panificadora.sapi.despacho.domain;

import jakarta.persistence.*;
import lombok.Data;

import java.time.LocalDateTime;

@Data 
@Entity
@Table(name = "detalle_movimiento") // Nombre de tabla según tu asignación
public class Despacho {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long idMovimiento;

    @Column(nullable = false)
    private String codigoProducto;

    @Column(nullable = false)
    private Double cantidad;

    @Column(nullable = false)
    private LocalDateTime fechaHora;

    @Column(nullable = false)
    private Long idPersonal;

    // Puedes generar los Getters y Setters manualmente o usar @Data si tienen Lombok
}