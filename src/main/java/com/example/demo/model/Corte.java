package com.example.demo.model;

import jakarta.persistence.*;
import java.util.Base64;
import java.util.Date;
import java.time.LocalDate;

@Entity
@Table(name = "corte")
public class Corte {

    @Id
    private String id;

    @ManyToOne(optional = false)
    @JoinColumn(name = "usuario_telefono", referencedColumnName = "telefono", nullable = false)
    private Usuario usuario;

    @ManyToOne(optional = false)
    @JoinColumn(name = "pedido_id", referencedColumnName = "id", nullable = false)
    private Pedido pedido;

    @Column(nullable = false)
    private LocalDate fecha;

    @Column(nullable = false, length = 50)
    private String medidas;

    @Column(nullable = false, length = 50)
    private String juegos;

    @Column(nullable = false, length = 50)
    private String proveedor;

    @Column(length = 255)
    private String novedades;

    @Lob
    @Column(name = "image", columnDefinition = "LONGBLOB")
    private byte[] imagen;
    // ✅ Getters y Setters completos

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public Usuario getUsuario() {
        return usuario;
    }

    public void setUsuario(Usuario usuario) {
        this.usuario = usuario;
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

    public String getJuegos() {
        return juegos;
    }

    public void setJuegos(String juegos) {
        this.juegos = juegos;
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

    public Pedido getPedido() {
        return pedido;
    }

    public void setPedido(Pedido pedido) {
        this.pedido = pedido;
    }

    public byte[] getImagen() {
        return imagen;
    }

    public void setImagen(byte[] imagen) {
        this.imagen = imagen;
    }

    // ✅ Para mostrar la imagen en Base64 en el HTML
    public String getImagenBase64() {
        return (this.imagen != null && this.imagen.length > 0) ? Base64.getEncoder().encodeToString(this.imagen) : "";
    }
}
