package com.panificadora.sapi.seguridad.domain;

import jakarta.persistence.*;

@Entity
@Table(name = "SEG_USUARIO")
public class UsuarioCredencial {

    @Id
    @Column(name = "ID_USUARIO")
    private Long idUsuario;

    @OneToOne
    @JoinColumn(name = "ID_PERSONAL", nullable = false)
    private Personal personal;

    @ManyToOne
    @JoinColumn(name = "ID_ROL", nullable = false)
    private Rol rol;

    @Column(name = "NOMBRE_USUARIO", nullable = false, unique = true)
    private String nombreUsuario;

    @Column(name = "CLAVE_ENCRIPTADA", nullable = false)
    private String claveEncriptada;

    @Column(name = "ACTIVO", nullable = false)
    private Integer activo;

    // Getters y Setters explícitos
    public Long getIdUsuario() {
        return idUsuario;
    }

    public void setIdUsuario(Long idUsuario) {
        this.idUsuario = idUsuario;
    }

    public Personal getPersonal() {
        return personal;
    }

    public void setPersonal(Personal personal) {
        this.personal = personal;
    }

    public Rol getRol() {
        return rol;
    }

    public void setRol(Rol rol) {
        this.rol = rol;
    }

    public String getNombreUsuario() {
        return nombreUsuario;
    }

    public void setNombreUsuario(String nombreUsuario) {
        this.nombreUsuario = nombreUsuario;
    }

    public String getClaveEncriptada() {
        return claveEncriptada;
    }

    public void setClaveEncriptada(String claveEncriptada) {
        this.claveEncriptada = claveEncriptada;
    }

    public Integer getActivo() {
        return activo;
    }

    public void setActivo(Integer activo) {
        this.activo = activo;
    }
}