package com.panificadora.sapi.seguridad.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.validation.constraints.Pattern;

@Entity
@Table(name = "SEG_PERSONAL")
public class Personal {

    @Id
    @Column(name = "ID_PERSONAL")
    private Long idPersonal;

    @Column(name = "DNI", length = 8, nullable = false, unique = true)
    @Pattern(regexp = "^\\d{8}$", message = "El DNI debe tener exactamente 8 dígitos numéricos.")
    private String dni;

    @Column(name = "NOMBRES", nullable = false)
    private String nombres;

    @Column(name = "APELLIDOS", nullable = false)
    private String apellidos;

    @Column(name = "CORREO")
    private String correo;

    @Column(name = "ACTIVO", nullable = false)
    private Integer activo;

    // Getters y Setters explícitos
    public Long getIdPersonal() {
        return idPersonal;
    }

    public void setIdPersonal(Long idPersonal) {
        this.idPersonal = idPersonal;
    }

    public String getDni() {
        return dni;
    }

    public void setDni(String dni) {
        this.dni = dni;
    }

    public String getNombres() {
        return nombres;
    }

    public void setNombres(String nombres) {
        this.nombres = nombres;
    }

    public String getApellidos() {
        return apellidos;
    }

    public void setApellidos(String apellidos) {
        this.apellidos = apellidos;
    }

    public String getCorreo() {
        return correo;
    }

    public void setCorreo(String correo) {
        this.correo = correo;
    }

    public Integer getActivo() {
        return activo;
    }

    public void setActivo(Integer activo) {
        this.activo = activo;
    }
}