package com.example.demo.model;

import jakarta.persistence.*;
import java.util.Base64;
import java.util.Date;

@Entity
@Table(name = "maquina")
public class Maquina {

    @Id
    private String id;

    @Column(name = "precio_unitario")
    private Integer precioUnitario;

    // 💵 Precio total (precioUnitario * cantidad)
    @Column(name = "precio_total")
    private Integer precioTotal;
    @ManyToOne(optional = false)
    @JoinColumn(name = "usuario_telefono", referencedColumnName = "telefono", nullable = false)
    private Usuario usuario;

    @ManyToOne(optional = false)
    @JoinColumn(name = "pedido_id", referencedColumnName = "id", nullable = false)
    private Pedido pedido;

    @Column(nullable = false)
    private Date fecha;

    @Column(nullable = false, length = 50)
    private String medidas;

    @Column(name = "tipo_sabanas", nullable = false, length = 50)
    private String tipoSabanas;

    @Column(nullable = false, length = 50)
    private String proveedor;

    @Column(length = 255)
    private String novedades;

    @Column(nullable = false)
    private Integer cantidad;

    @Lob
    @Column(columnDefinition = "LONGBLOB")
    private byte[] imagen;

    // Getters y Setters
    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public Date getFecha() {
        return fecha;
    }

    public void setFecha(Date fecha) {
        this.fecha = fecha;
    }

    public String getMedidas() {
        return medidas;
    }

    public void setMedidas(String medidas) {
        this.medidas = medidas;
    }

    // ✅ CORREGIDO: Método getter y setter consistente
    public String getTipoSabanas() {
        return tipoSabanas;
    }

    public void setTipoSabanas(String tipoSabanas) {
        this.tipoSabanas = tipoSabanas;
    }

    // ✅ Mantener compatibilidad con nombre de base de datos
    public String getTipo_sabanas() {
        return this.tipoSabanas;
    }

    public void setTipo_sabanas(String tipo_sabanas) {
        this.tipoSabanas = tipo_sabanas;
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

    public Usuario getUsuario() {
        return usuario;
    }

    public void setUsuario(Usuario usuario) {
        this.usuario = usuario;
    }

    public Pedido getPedido() {
        return pedido;
    }

    public void setPedido(Pedido pedido) {
        this.pedido = pedido;
    }

    public Integer getCantidad() {
        return cantidad;
    }

    public void setCantidad(Integer cantidad) {
        this.cantidad = cantidad;
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

    public String getImagenBase64() {
        return (this.imagen != null && this.imagen.length > 0) ? Base64.getEncoder().encodeToString(this.imagen) : "";
    }
}