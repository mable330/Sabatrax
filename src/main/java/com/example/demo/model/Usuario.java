package com.example.demo.model;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.*;

@Entity
@Table(name = "usuario")
public class Usuario {
    @Id
    @Column(name = "telefono", nullable = false, unique = true)
    private String telefono;

    @Column(name = "Nombre")
    private String nombre;

    @Column(name = "Apellido")
    private String apellido;

    @Column(name = "Rol")
    private String rol;

    @Column(name = "Actividad")
    private String actividad;

    @Column(name = "password")
    private String clave;

    @Column(name = "correo")
    private String correo;

    @Override
    public boolean equals(Object o) {
        if (this == o)
            return true;
        if (!(o instanceof Usuario))
            return false;
        Usuario usuario = (Usuario) o;
        return telefono != null && telefono.equals(usuario.telefono);
    }

    @Override
    public int hashCode() {
        return telefono != null ? telefono.hashCode() : 0;
    }

    // Getters y Setters
    public String getCorreo() {
        return correo;
    }

    public void setCorreo(String correo) {
        this.correo = correo;
    }

    public String getTelefono() {
        return telefono;
    }

    public void setTelefono(String telefono) {
        this.telefono = telefono;
    }

    public String getNombre() {
        return nombre;
    }

    public void setNombre(String nombre) {
        this.nombre = nombre;
    }

    public String getApellido() {
        return apellido;
    }

    public void setApellido(String apellido) {
        this.apellido = apellido;
    }

    public String getRol() {
        return rol;
    }

    public void setRol(String rol) {
        this.rol = rol;
    }

    public String getActividad() {
        return actividad;
    }

    public void setActividad(String actividad) {
        this.actividad = actividad;
    }

    public String getClave() {
        return clave;
    }

    public void setClave(String clave) {
        this.clave = clave;
    }

    // Métodos equals y hashCode basados en el teléfono

}