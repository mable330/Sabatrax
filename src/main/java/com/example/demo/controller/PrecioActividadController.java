package com.example.demo.controller;

import com.example.demo.model.PrecioActividad;
import com.example.demo.repository.PrecioActividadRepository;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.Optional;
import java.util.List;
import java.util.Map;
import java.util.HashMap;

@Controller
@RequestMapping("/precios")
public class PrecioActividadController {

    @Autowired
    private PrecioActividadRepository precioActividadRepository;

    // ✅ Mostrar precios con estadísticas mejoradas
    @GetMapping
    public String listarPrecios(Model model) {
        List<PrecioActividad> precios = precioActividadRepository.findAll();
        model.addAttribute("precios", precios);

        // 🔥 ESTADÍSTICAS COMPLETAS para el administrador
        long totalPrecios = precios.size();
        long preciosMaquina = precioActividadRepository.countByActividad("maquina");
        long preciosCorte = precioActividadRepository.countByActividad("corte");
        long preciosEmpaque = precioActividadRepository.countByActividad("empaque");

        // Calcular promedios de precios por actividad
        double promedioMaquina = precioActividadRepository.findByActividad("maquina")
                .stream().mapToInt(PrecioActividad::getPrecio).average().orElse(0.0);
        double promedioCorte = precioActividadRepository.findByActividad("corte")
                .stream().mapToInt(PrecioActividad::getPrecio).average().orElse(0.0);
        double promedioEmpaque = precioActividadRepository.findByActividad("empaque")
                .stream().mapToInt(PrecioActividad::getPrecio).average().orElse(0.0);

        model.addAttribute("totalPrecios", totalPrecios);
        model.addAttribute("preciosMaquina", preciosMaquina);
        model.addAttribute("preciosCorte", preciosCorte);
        model.addAttribute("preciosEmpaque", preciosEmpaque);
        model.addAttribute("promedioMaquina", Math.round(promedioMaquina));
        model.addAttribute("promedioCorte", Math.round(promedioCorte));
        model.addAttribute("promedioEmpaque", Math.round(promedioEmpaque));

        return "precios";
    }

    // 🔥 MEJORADO: Guardar precio con validaciones avanzadas y normalización
    @PostMapping("/guardar")
    public String guardarPrecio(@RequestParam String actividad,
            @RequestParam String descripcion,
            @RequestParam Integer precio,
            RedirectAttributes redirectAttributes) {

        try {
            // ✅ VALIDACIONES MEJORADAS
            if (actividad == null || actividad.trim().isEmpty()) {
                redirectAttributes.addFlashAttribute("error", "🚨 La actividad es obligatoria");
                return "redirect:/precios";
            }

            if (descripcion == null || descripcion.trim().isEmpty()) {
                redirectAttributes.addFlashAttribute("error", "🚨 La descripción es obligatoria");
                return "redirect:/precios";
            }

            if (precio == null || precio <= 0) {
                redirectAttributes.addFlashAttribute("error", "🚨 El precio debe ser mayor a 0");
                return "redirect:/precios";
            }

            if (precio > 50000) {
                redirectAttributes.addFlashAttribute("error", "🚨 El precio no puede exceder $50,000");
                return "redirect:/precios";
            }

            // ✅ NORMALIZACIÓN AVANZADA
            String actividadNormalizada = normalizarActividad(actividad);
            String descripcionNormalizada = normalizarDescripcion(descripcion);

            // Validar que la actividad sea válida
            if (!esActividadValida(actividadNormalizada)) {
                redirectAttributes.addFlashAttribute("error",
                        "🚨 Actividad no válida. Use: maquina, corte o empaque");
                return "redirect:/precios";
            }

            // ✅ VERIFICAR DUPLICADOS Y ACTUALIZAR O CREAR
            Optional<PrecioActividad> precioExistente = precioActividadRepository
                    .findByActividadAndDescripcion(actividadNormalizada, descripcionNormalizada);

            if (precioExistente.isPresent()) {
                // 🔥 ACTUALIZAR PRECIO EXISTENTE
                PrecioActividad precioActualizar = precioExistente.get();
                Integer precioAnterior = precioActualizar.getPrecio();
                precioActualizar.setPrecio(precio);
                precioActividadRepository.save(precioActualizar);

                // Calcular diferencia porcentual
                double diferenciaPorcentual = ((double) (precio - precioAnterior) / precioAnterior) * 100;
                String tendencia = diferenciaPorcentual > 0 ? "📈 +" : "📉 ";

                redirectAttributes.addFlashAttribute("exito",
                        "✅ Precio actualizado: " + descripcionNormalizada +
                                " en " + actividadNormalizada +
                                " cambió de $" + precioAnterior + " a $" + precio +
                                " (" + tendencia + String.format("%.1f", Math.abs(diferenciaPorcentual)) + "%)");
            } else {
                // 🔥 CREAR NUEVO PRECIO
                PrecioActividad nuevoPrecio = new PrecioActividad();
                nuevoPrecio.setActividad(actividadNormalizada);
                nuevoPrecio.setDescripcion(descripcionNormalizada);
                nuevoPrecio.setPrecio(precio);
                precioActividadRepository.save(nuevoPrecio);

                redirectAttributes.addFlashAttribute("exito",
                        "✅ Nuevo precio registrado: " + descripcionNormalizada +
                                " en " + actividadNormalizada + " = $" + precio);
            }

            // 🔥 LOGGING para auditoria (opcional)
            logCambioPrecio(actividadNormalizada, descripcionNormalizada, precio,
                    precioExistente.isPresent() ? "ACTUALIZADO" : "CREADO");

        } catch (NumberFormatException e) {
            redirectAttributes.addFlashAttribute("error", "🚨 El precio debe ser un número válido");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error",
                    "🚨 Error al guardar el precio: " + e.getMessage());
        }

        return "redirect:/precios";
    }

    // ✅ NUEVO: API REST para obtener precios por actividad (para el frontend)
    @GetMapping("/api/{actividad}")
    @ResponseBody
    public ResponseEntity<Map<String, Integer>> obtenerPreciosPorActividad(@PathVariable String actividad) {
        try {
            String actividadNormalizada = normalizarActividad(actividad);
            if (!esActividadValida(actividadNormalizada)) {
                return ResponseEntity.badRequest().build();
            }

            List<PrecioActividad> precios = precioActividadRepository.findByActividad(actividadNormalizada);
            Map<String, Integer> preciosMap = new HashMap<>();

            for (PrecioActividad precio : precios) {
                preciosMap.put(precio.getDescripcion(), precio.getPrecio());
            }

            return ResponseEntity.ok(preciosMap);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    // ✅ NUEVO: Obtener precio específico para cálculos automáticos
    @GetMapping("/api/{actividad}/{descripcion}")
    @ResponseBody
    public ResponseEntity<Integer> obtenerPrecioEspecifico(@PathVariable String actividad,
            @PathVariable String descripcion) {
        try {
            String actividadNormalizada = normalizarActividad(actividad);
            String descripcionNormalizada = normalizarDescripcion(descripcion);

            Optional<PrecioActividad> precio = precioActividadRepository
                    .findByActividadAndDescripcion(actividadNormalizada, descripcionNormalizada);

            if (precio.isPresent()) {
                return ResponseEntity.ok(precio.get().getPrecio());
            } else {
                // Devolver precio por defecto si no existe
                Integer precioDefault = obtenerPrecioDefault(descripcionNormalizada);
                return ResponseEntity.ok(precioDefault);
            }
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    // ✅ Mostrar formulario de edición mejorado
    @GetMapping("/editar/{id}")
    public String mostrarFormularioEditar(@PathVariable Long id, Model model, RedirectAttributes redirectAttributes) {
        Optional<PrecioActividad> precioOptional = precioActividadRepository.findById(id);

        if (precioOptional.isPresent()) {
            PrecioActividad precio = precioOptional.get();
            model.addAttribute("precio", precio);

            // Agregar información adicional para el formulario
            model.addAttribute("esEdicion", true);
            model.addAttribute("tituloFormulario", "Editar Precio - " +
                    precio.getDescripcion() + " (" + precio.getActividad() + ")");

            return "editar-precio";
        } else {
            redirectAttributes.addFlashAttribute("error", "🚨 Precio no encontrado");
            return "redirect:/precios";
        }
    }

    // 🔥 MEJORADO: Actualizar precio con validaciones completas
    @PostMapping("/actualizar")
    public String actualizarPrecio(@ModelAttribute PrecioActividad precio,
            RedirectAttributes redirectAttributes) {
        try {
            // ✅ VALIDACIONES COMPLETAS
            if (precio.getId() == null) {
                redirectAttributes.addFlashAttribute("error", "🚨 ID de precio inválido");
                return "redirect:/precios";
            }

            if (precio.getPrecio() == null || precio.getPrecio() <= 0) {
                redirectAttributes.addFlashAttribute("error", "🚨 El precio debe ser mayor a 0");
                return "redirect:/precios";
            }

            if (precio.getPrecio() > 50000) {
                redirectAttributes.addFlashAttribute("error", "🚨 El precio no puede exceder $50,000");
                return "redirect:/precios";
            }

            if (precio.getDescripcion() == null || precio.getDescripcion().trim().isEmpty()) {
                redirectAttributes.addFlashAttribute("error", "🚨 La descripción es obligatoria");
                return "redirect:/precios";
            }

            // ✅ VERIFICAR EXISTENCIA
            Optional<PrecioActividad> precioExistente = precioActividadRepository.findById(precio.getId());
            if (precioExistente.isEmpty()) {
                redirectAttributes.addFlashAttribute("error", "🚨 Precio no encontrado");
                return "redirect:/precios";
            }

            // ✅ ACTUALIZAR CON NORMALIZACIÓN
            PrecioActividad precioActualizar = precioExistente.get();
            Integer precioAnterior = precioActualizar.getPrecio();
            String descripcionAnterior = precioActualizar.getDescripcion();

            precioActualizar.setActividad(normalizarActividad(precio.getActividad()));
            precioActualizar.setDescripcion(normalizarDescripcion(precio.getDescripcion()));
            precioActualizar.setPrecio(precio.getPrecio());

            precioActividadRepository.save(precioActualizar);

            // 🔥 MENSAJE DE ÉXITO DETALLADO
            boolean cambioDescripcion = !descripcionAnterior.equals(precioActualizar.getDescripcion());
            boolean cambioPrecio = !precioAnterior.equals(precio.getPrecio());

            String mensaje = "✅ Precio actualizado correctamente";
            if (cambioDescripcion && cambioPrecio) {
                mensaje += ": '" + descripcionAnterior + "' → '" + precioActualizar.getDescripcion() +
                        "' y precio $" + precioAnterior + " → $" + precio.getPrecio();
            } else if (cambioPrecio) {
                mensaje += ": " + precioActualizar.getDescripcion() +
                        " cambió de $" + precioAnterior + " a $" + precio.getPrecio();
            } else if (cambioDescripcion) {
                mensaje += ": '" + descripcionAnterior + "' → '" + precioActualizar.getDescripcion() + "'";
            }

            redirectAttributes.addFlashAttribute("exito", mensaje);

        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error",
                    "🚨 Error al actualizar el precio: " + e.getMessage());
        }

        return "redirect:/precios";
    }

    // ✅ NUEVO: Eliminar precio con confirmación
    @PostMapping("/eliminar/{id}")
    public String eliminarPrecio(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        try {
            Optional<PrecioActividad> precioOptional = precioActividadRepository.findById(id);

            if (precioOptional.isPresent()) {
                PrecioActividad precio = precioOptional.get();
                String descripcion = precio.getDescripcion();
                String actividad = precio.getActividad();

                precioActividadRepository.deleteById(id);

                redirectAttributes.addFlashAttribute("exito",
                        "✅ Precio eliminado: " + descripcion + " (" + actividad + ")");
            } else {
                redirectAttributes.addFlashAttribute("error", "🚨 Precio no encontrado");
            }
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error",
                    "🚨 Error al eliminar: " + e.getMessage());
        }

        return "redirect:/precios";
    }

    // ================================
    // MÉTODOS AUXILIARES MEJORADOS
    // ================================

    /**
     * Normaliza el nombre de la actividad
     */
    private String normalizarActividad(String actividad) {
        if (actividad == null)
            return "";

        String normalizada = actividad.toLowerCase().trim();

        // Mapear variaciones comunes
        switch (normalizada) {
            case "maquinas", "máquina", "máquinas" -> normalizada = "maquina";
            case "cortes" -> normalizada = "corte";
            case "empaques", "empacado" -> normalizada = "empaque";
        }

        return normalizada;
    }

    /**
     * Normaliza la descripción
     */
    private String normalizarDescripcion(String descripcion) {
        if (descripcion == null)
            return "";

        String normalizada = descripcion.trim();

        // Capitalizar primera letra
        if (!normalizada.isEmpty()) {
            normalizada = normalizada.substring(0, 1).toUpperCase() +
                    normalizada.substring(1).toLowerCase();
        }

        // Mapear variaciones comunes
        switch (normalizada.toLowerCase()) {
            case "planas" -> normalizada = "Plana";
            case "cauchos" -> normalizada = "Caucho";
            case "funda", "fundas" -> normalizada = "Fundas";
            case "cortina", "cortinas" -> normalizada = "Cortinas";
        }

        return normalizada;
    }

    /**
     * Verifica si la actividad es válida
     */
    private boolean esActividadValida(String actividad) {
        return actividad != null &&
                (actividad.equals("maquina") || actividad.equals("corte") || actividad.equals("empaque"));
    }

    /**
     * Obtiene el precio por defecto si no existe en la base de datos
     */
    private Integer obtenerPrecioDefault(String descripcion) {
        return switch (descripcion.toLowerCase()) {
            case "plana", "caucho" -> 400;
            case "fundas" -> 50;
            case "cortinas" -> 1200;
            default -> 0;
        };
    }

    /**
     * Log de cambios para auditoría (implementar según necesidades)
     */
    private void logCambioPrecio(String actividad, String descripcion, Integer precio, String accion) {
        // Aquí puedes implementar logging a archivo, base de datos, etc.
        System.out.println(String.format("[PRECIO_%s] %s - %s: $%d",
                accion, actividad.toUpperCase(), descripcion, precio));
    }
}