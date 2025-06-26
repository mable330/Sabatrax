package com.example.demo.repository;

import com.example.demo.model.PrecioActividad;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.time.LocalDate;

public interface PrecioActividadRepository extends JpaRepository<PrecioActividad, Long> {

    // ✅ Buscar por actividad exacta
    List<PrecioActividad> findByActividad(String actividad);

    // ✅ Buscar por actividad y descripción
    Optional<PrecioActividad> findByActividadAndDescripcion(String actividad, String descripcion);

    // 🔥 NUEVO: Contar precios por actividad (para estadísticas)
    long countByActividad(String actividad);

    // 🔥 NUEVO: Buscar precios por actividad ignorando mayúsculas/minúsculas
    @Query("SELECT p FROM PrecioActividad p WHERE LOWER(p.actividad) = LOWER(:actividad)")
    List<PrecioActividad> findByActividadIgnoreCase(@Param("actividad") String actividad);

    // 🔥 NUEVO: Buscar precio específico ignorando mayúsculas/minúsculas
    @Query("SELECT p FROM PrecioActividad p WHERE LOWER(p.actividad) = LOWER(:actividad) AND LOWER(p.descripcion) = LOWER(:descripcion)")
    Optional<PrecioActividad> findByActividadAndDescripcionIgnoreCase(@Param("actividad") String actividad,
            @Param("descripcion") String descripcion);

    // 🔥 NUEVO: Obtener todos los precios ordenados por actividad y descripción
    @Query("SELECT p FROM PrecioActividad p ORDER BY p.actividad ASC, p.descripcion ASC")
    List<PrecioActividad> findAllOrderedByActividadAndDescripcion();

    // 🔥 NUEVO: Verificar si existe un precio para una actividad específica
    boolean existsByActividad(String actividad);

    // 🔥 NUEVO: Verificar si existe un precio para una combinación
    // actividad-descripción
    boolean existsByActividadAndDescripcion(String actividad, String descripcion);

    Optional<PrecioActividad> findByActividadAndDescripcionAndFechaInicio(String actividad, String descripcion,
            LocalDate fechaInicio);

    @Query("SELECT p FROM PrecioActividad p WHERE p.actividad = :actividad AND p.descripcion = :descripcion AND p.fechaInicio <= :fecha ORDER BY p.fechaInicio DESC")
    List<PrecioActividad> findVigenteByFecha(
            @Param("actividad") String actividad,
            @Param("descripcion") String descripcion,
            @Param("fecha") LocalDate fecha);

}