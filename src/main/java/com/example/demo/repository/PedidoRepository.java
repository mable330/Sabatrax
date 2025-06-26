package com.example.demo.repository;

import com.example.demo.model.Corte;
import com.example.demo.model.Pedido;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.Date;

public interface PedidoRepository extends JpaRepository<Pedido, Long> {

        List<Pedido> findByMedidasSabanasAndFechaEnvioAfter(String medidasSabanas, Date fechaEnvio);

        /**
         * Busca un pedido por medidas de sábanas y fecha de envío exacta
         */
        Optional<Pedido> findByMedidasSabanasAndFechaEnvio(String medidasSabanas, LocalDate fechaEnvio);

        List<Pedido> findByMedidasSabanasAndFechaEnvioAfterAndCantidadEntregadaLessThan(
                        String medidasSabanas,
                        LocalDate fechaEnvio,
                        int cantidadEntregada);

        /**
         * Busca pedidos por medidas con fecha de envío posterior a la fecha dada
         * (pedidos activos)
         */
        @Query("SELECT p FROM Pedido p WHERE p.medidasSabanas = :medidas AND p.fechaEnvio >= :fecha")
        List<Pedido> findByMedidasSabanasAndFechaEnvioAfter(@Param("medidas") String medidas,
                        @Param("fecha") LocalDate fechaActual);

        /**
         * Busca todos los pedidos activos (no vencidos)
         */
        @Query("SELECT p FROM Pedido p WHERE p.fechaEnvio >= :fechaActual ORDER BY p.fechaEnvio ASC")
        List<Pedido> findPedidosActivos(@Param("fechaActual") LocalDate fechaActual);

        /**
         * Busca pedidos incompletos por medida
         */
        @Query("SELECT p FROM Pedido p WHERE p.medidasSabanas = :medidas AND p.cantidadEntregada < p.juegos")
        List<Pedido> findPedidosIncompletosPorMedida(@Param("medidas") String medidas);

        /**
         * Busca pedido específico por medida y fecha
         */
        @Query("SELECT p FROM Pedido p WHERE p.medidasSabanas = :medidas AND p.fechaEnvio = :fecha")
        Optional<Pedido> buscarPedidoPorMedidaYFecha(@Param("medidas") String medidas,
                        @Param("fecha") LocalDate fecha);

        List<Pedido> findByFechaEnvioAfter(LocalDate minusDays);

        @Query("SELECT COALESCE(SUM(m.cantidad), 0) FROM Maquina m WHERE m.pedido.id = :pedidoId AND m.tipoSabanas = :tipoSabanas")
        int contarPorPedidoYTipo(@Param("pedidoId") Long pedidoId, @Param("tipoSabanas") String tipoSabanas);

        List<Pedido> findByMedidasSabanas(String medidas);

}
