package com.example.demo.repository;

import java.time.LocalDate;
import com.example.demo.model.Corte;
import com.example.demo.model.Usuario;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface CorteRepository extends JpaRepository<Corte, String> {

    List<Corte> findByUsuario(Usuario usuario);

    void deleteByUsuarioTelefono(String telefono);

    List<Corte> findByUsuario_Telefono(String telefono);

    @Query("SELECT COALESCE(SUM(CAST(c.juegos AS int)), 0) FROM Corte c WHERE c.medidas = :medidas AND c.fecha >= :fechaInicio")
    int contarJuegosPorMedidaDesde(@Param("medidas") String medidas, @Param("fechaInicio") LocalDate fechaInicio);

    @Query("SELECT COALESCE(SUM(CAST(c.juegos AS int)), 0) FROM Corte c WHERE c.medidas = :medidas AND c.fecha >= :fechaInicio")
    int contarJuegosCortadosPorMedida(@Param("medidas") String medidas, @Param("fechaInicio") LocalDate fechaInicio);

    @Query("SELECT COALESCE(SUM(c.juegos), 0) FROM Corte c WHERE c.pedido.id = :pedidoId")
    int contarJuegosCortadosPorPedido(@Param("pedidoId") Long pedidoId);

}
