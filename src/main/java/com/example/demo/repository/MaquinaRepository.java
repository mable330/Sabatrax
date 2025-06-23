package com.example.demo.repository;

// Correct import for LocalDate from java.time
import java.time.LocalDate;
import com.example.demo.model.Maquina;
import com.example.demo.model.Usuario;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Date; // Keep java.util.Date for the 'fecha' property in Maquina entity

@Repository
public interface MaquinaRepository extends JpaRepository<Maquina, String> {

        void deleteByUsuarioTelefono(String telefono);

        List<Maquina> findByUsuario(Usuario usuario);

        List<Maquina> findByUsuario_Telefono(String telefono);

        @Query("SELECT COALESCE(SUM(m.cantidad), 0) FROM Maquina m WHERE " +
                        "m.medidas = :medidas AND m.tipoSabanas  = :tipo AND FUNCTION('DATE', m.fecha) = :fecha")
        int contarPorMedidaYTipo(@Param("medidas") String medidas,
                        @Param("tipo") String tipo,
                        @Param("fecha") java.time.LocalDate fechaActual);

        /**
         * Cuenta la cantidad total de sábanas por medida y tipo desde una fecha hasta
         * hoy
         */
        @Query("SELECT COALESCE(SUM(m.cantidad), 0) FROM Maquina m WHERE " +
                        "m.medidas = :medidas AND m.tipoSabanas = :tipo AND FUNCTION('DATE', m.fecha) >= :fechaInicio")
        int contarPorMedidaYTipoDesde(@Param("medidas") String medidas,
                        @Param("tipo") String tipo,
                        @Param("fechaInicio") java.time.LocalDate fechaInicio);

        /**
         * Obtiene todas las actividades por medida de sábanas
         */
        @Query("SELECT m FROM Maquina m WHERE m.medidas = :medidas ORDER BY m.fecha DESC")
        List<Maquina> findByMedidas(@Param("medidas") String medidas);

        /**
         * Cuenta actividades por usuario y medida
         */
        @Query("SELECT COUNT(m) FROM Maquina m WHERE m.usuario = :usuario AND m.medidas = :medidas")
        long contarPorUsuarioYMedida(@Param("usuario") Usuario usuario, @Param("medidas") String medidas);

        @Query("SELECT COALESCE(SUM(m.cantidad), 0) FROM Maquina m WHERE m.pedido.id = :pedidoId AND m.tipoSabanas = :tipoSabanas")
        int contarPorPedidoYTipo(@Param("pedidoId") Long pedidoId, @Param("tipoSabanas") String tipoSabanas);

}