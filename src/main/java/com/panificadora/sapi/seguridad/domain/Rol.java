package com.panificadora.sapi.seguridad.domain;

import jakarta.persistence.*;

@Entity
@Table(name = "SEG_ROL")
public class Rol {

    @Id
    @Column(name = "ID_ROL")
    private Long idRol;

    @Column(name = "NOMBRE", nullable = false, unique = true)
    private String nombre;

    // Getters y Setters explícitos
    public Long getIdRol() {
        return idRol;
    }

    public void setIdRol(Long idRol) {
        this.idRol = idRol;
    }

    public String getNombre() {
        return nombre;
    }

    public void setNombre(String nombre) {
        this.nombre = nombre;
    }
}