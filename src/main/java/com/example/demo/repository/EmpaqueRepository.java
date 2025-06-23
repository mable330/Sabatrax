package com.example.demo.repository;

import com.example.demo.model.Empaque;
import com.example.demo.model.Usuario;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

@Repository
public interface EmpaqueRepository extends JpaRepository<Empaque, String> {

    void deleteByUsuarioTelefono(String telefono);

    List<Empaque> findByUsuario(Usuario usuario);

    List<Empaque> findByMedidas(String medidas);

    List<Empaque> findByUsuario_Telefono(String telefono);
}
