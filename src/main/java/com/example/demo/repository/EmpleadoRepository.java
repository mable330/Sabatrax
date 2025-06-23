package com.example.demo.repository;

import com.example.demo.model.Empleado;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

public interface EmpleadoRepository extends JpaRepository<Empleado, String> {
    Empleado findByCorreo(String correo);

}