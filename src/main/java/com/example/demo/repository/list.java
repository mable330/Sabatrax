package com.example.demo.repository;

import com.example.demo.model.Maquina;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface list extends JpaRepository<Maquina, String> {
    // Puedes agregar métodos personalizados aquí si los necesitas
}
