package com.example.demo.model;

import jakarta.persistence.*;
import java.time.LocalDate;
import java.util.UUID;

import jakarta.persistence.*;
import java.time.LocalDate;
import java.util.UUID;

@Entity
@Table(name = "maquina")
public class RegistroActividad {

    @Id
    @Column(length = 36) // Longitud suficiente para UUID
    private String id;

    private LocalDate fecha;
    private String medidas;

    @Column(name = "tipo_sabanas")
    private String tipoSabanas;

    private String proveedor;
    private String novedades;
    private Integer cantidad;

    @Lob
    private byte[] imagen;

    // Método para generar ID automáticamente
    @PrePersist
    public void generarId() {
        if (this.id == null) {
            this.id = UUID.randomUUID().toString();
        }
    }

    // ————— Getters y Setters —————
    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public LocalDate getFecha() {
        return fecha;
    }

    public void setFecha(LocalDate fecha) {
        this.fecha = fecha;
    }

    public String getMedidas() {
        return medidas;
    }

    public void setMedidas(String medidas) {
        this.medidas = medidas;
    }

    public String getTipoSabanas() {
        return tipoSabanas;
    }

    public void setTipoSabanas(String tipoSabanas) {
        this.tipoSabanas = tipoSabanas;
    }

    public String getProveedor() {
        return proveedor;
    }

    public void setProveedor(String proveedor) {
        this.proveedor = proveedor;
    }

    public String getNovedades() {
        return novedades;
    }

    public void setNovedades(String novedades) {
        this.novedades = novedades;
    }

    public Integer getCantidad() {
        return cantidad;
    }

    public void setCantidad(Integer cantidad) {
        this.cantidad = cantidad;
    }

    public byte[] getImagen() {
        return imagen;
    }

    public void setImagen(byte[] imagen) {
        this.imagen = imagen;
    }
}
