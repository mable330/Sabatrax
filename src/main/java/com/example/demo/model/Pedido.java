package com.example.demo.model;

import jakarta.persistence.*;
import java.time.LocalDate;

@Entity
@Table(name = "pedido")
public class Pedido {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String medidasSabanas; // Ejemplo: "Sencillo", "Doble", "Queen", "King"

    private int juegos;

    private LocalDate fechaEnvio;

    private int cantidadEntregada = 0;

    // 🔥 Campos transitorios (NO se guardan en la base de datos)
    @Transient
    private double progresoCorte;

    @Transient
    private double progresoMaquina;

    @Transient
    private double progresoEmpaque;

    @Transient
    private double porcentajeTotal;

    // ✅ Getters y Setters

    public Long getId() {
        return id;
    }

    public String getMedidasSabanas() {
        return medidasSabanas;
    }

    public void setMedidasSabanas(String medidasSabanas) {
        this.medidasSabanas = medidasSabanas;
    }

    public int getJuegos() {
        return juegos;
    }

    public void setJuegos(int juegos) {
        this.juegos = juegos;
    }

    public LocalDate getFechaEnvio() {
        return fechaEnvio;
    }

    public void setFechaEnvio(LocalDate fechaEnvio) {
        this.fechaEnvio = fechaEnvio;
    }

    public int getCantidadEntregada() {
        return cantidadEntregada;
    }

    public void setCantidadEntregada(int cantidadEntregada) {
        this.cantidadEntregada = cantidadEntregada;
    }

    public double getProgresoCorte() {
        return progresoCorte;
    }

    public void setProgresoCorte(double progresoCorte) {
        this.progresoCorte = progresoCorte;
    }

    public double getProgresoMaquina() {
        return progresoMaquina;
    }

    public void setProgresoMaquina(double progresoMaquina) {
        this.progresoMaquina = progresoMaquina;
    }

    public double getProgresoEmpaque() {
        return progresoEmpaque;
    }

    public void setProgresoEmpaque(double progresoEmpaque) {
        this.progresoEmpaque = progresoEmpaque;
    }

    public double getPorcentajeTotal() {
        return porcentajeTotal;
    }

    public void setPorcentajeTotal(double porcentajeTotal) {
        this.porcentajeTotal = porcentajeTotal;
    }

    // ✅ Verifica si el pedido está completo
    public boolean estaCompleto() {
        return cantidadEntregada >= juegos;
    }

    // ✅ Verifica si la fecha está vencida
    public boolean fechaExpirada() {
        return LocalDate.now().isAfter(fechaEnvio);
    }
}
