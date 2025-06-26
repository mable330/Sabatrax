package com.example.demo.model;

import jakarta.persistence.*;
import java.util.Base64;
import java.util.Date;

@Entity
@Table(name = "empaque")
public class Empaque {

    @Id
    private String id;

    @Column(name = "precio_total")
    private Integer precioTotal;

    @Column(name = "precio_unitario")
    private Integer precioUnitario;

    @ManyToOne(optional = false)
    @JoinColumn(name = "usuario_telefono", referencedColumnName = "telefono", nullable = false)
    private Usuario usuario;

    @Column(nullable = false)
    private Date fecha;

    @Column()
    private String tipoEmpaque;

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

    public Date getFecha() {
        return fecha;
    }

    public void setFecha(Date fecha) {
        this.fecha = fecha;
    }

    public String getTipoEmpaque() {
        return tipoEmpaque;
    }

    public void setTipoEmpaque(String tipoEmpaque) {
        this.tipoEmpaque = tipoEmpaque;
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

    public Integer getPrecioUnitario() {
        return precioUnitario;
    }

    public void setPrecioUnitario(Integer precioUnitario) {
        this.precioUnitario = precioUnitario;
    }

    // 🔧 Getter y Setter para precioTotal
    public Integer getPrecioTotal() {
        return precioTotal;
    }

    public void setPrecioTotal(Integer precioTotal) {
        this.precioTotal = precioTotal;
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
