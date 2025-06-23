package com.example.demo.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor; // Añadido para el constructor con todos los argumentos
import lombok.Data;
import lombok.NoArgsConstructor; // Añadido para el constructor sin argumentos

@Entity
@Table(name = "usuario") // Mapea a la tabla 'usuario'
@Data // Genera getters, setters, toString, equals y hashCode
@NoArgsConstructor // Genera un constructor sin argumentos (necesario para JPA)
@AllArgsConstructor // Genera un constructor con todos los argumentos (útil para DataLoader)
public class Empleado {

    @Id // Marca 'telefono' como la clave primaria
    @Column(name = "telefono") // Especifica el nombre de la columna en la BD
    private String telefono;

    @Column(name = "nombre")
    private String nombre;

    @Column(name = "apellido")
    private String apellido;

    @Column(name = "correo", unique = true) // 'unique = true' para asegurar correos únicos
    private String correo;

    @Column(name = "actividad")
    private String actividad; // Podría ser 'corte', 'maquina', 'empaque'

    @Column(name = "password")
    private String password;

    @Column(name = "rol") // e.g., "Maquinista", "Cortador", "Empacador"
    private String rol;
}