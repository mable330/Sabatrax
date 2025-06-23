package com.example.demo.controller;

import com.example.demo.model.Empleado;
import com.example.demo.repository.EmpleadoRepository;
import com.example.demo.service.CorreoService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;
import org.springframework.ui.Model;

import java.util.List;
import java.util.stream.Collectors;

@Controller
@RequestMapping("/correo/masivo")
public class CorreoMasivoController {

    @Autowired
    private EmpleadoRepository repo;

    @Autowired
    private CorreoService correoService;

    @GetMapping
    public String mostrarFormularioMasivo() {
        return "correo-masivo";
    }

    @PostMapping("/enviar")
    public String enviarMasivo(
            @RequestParam String asunto,
            @RequestParam String mensaje,
            @RequestParam String tipoEnvio,
            Model model) {

        List<String> correos;
        String mensajeExito;

        // Filtrar empleados según el tipo de envío seleccionado
        switch (tipoEnvio.toLowerCase()) {
            case "maquina":
                correos = obtenerCorreosPorDepartamento("maquina");
                mensajeExito = "El mensaje fue enviado exitosamente a todos los empleados del departamento de Máquina.";
                break;

            case "corte":
                correos = obtenerCorreosPorDepartamento("corte");
                mensajeExito = "El mensaje fue enviado exitosamente a todos los empleados del departamento de Corte.";
                break;

            case "empaque":
                correos = obtenerCorreosPorDepartamento("empaque");
                mensajeExito = "El mensaje fue enviado exitosamente a todos los empleados del departamento de Empaque.";
                break;

            case "todos":
            default:
                correos = obtenerTodosLosCorreos();
                mensajeExito = "El mensaje fue enviado exitosamente a todos los empleados.";
                break;
        }

        // Validar que hay correos para enviar
        if (correos.isEmpty()) {
            String errorMsg = obtenerMensajeError(tipoEnvio);
            model.addAttribute("error", errorMsg);
            return "correo-masivo";
        }

        try {
            // Enviar correos de forma asíncrona
            correoService.enviarCorreoMasivoConPlantillaAsincrono(
                    correos,
                    asunto,
                    "correo-template",
                    mensaje);

            // Agregar información adicional al mensaje de éxito
            mensajeExito += " Total de correos enviados: " + correos.size();
            model.addAttribute("mensajeExito", mensajeExito);

        } catch (Exception e) {
            model.addAttribute("error", "Error al enviar los correos: " + e.getMessage());
            return "correo-masivo";
        }

        return "correo-masivo";
    }

    /**
     * Obtiene todos los correos válidos de la base de datos
     */
    private List<String> obtenerTodosLosCorreos() {
        return repo.findAll().stream()
                .map(Empleado::getCorreo)
                .filter(correo -> correo != null && correo.contains("@"))
                .collect(Collectors.toList());
    }

    /**
     * Obtiene correos de empleados filtrados por departamento
     * Asume que la entidad Empleado tiene un campo 'departamento'
     */
    private List<String> obtenerCorreosPorDepartamento(String departamento) {
        return repo.findAll().stream()
                .filter(empleado -> empleado.getActividad() != null &&
                        empleado.getActividad().toLowerCase().equals(departamento.toLowerCase()))
                .map(Empleado::getCorreo)
                .filter(correo -> correo != null && correo.contains("@"))
                .collect(Collectors.toList());
    }

    /**
     * Genera mensaje de error personalizado según el tipo de envío
     */
    private String obtenerMensajeError(String tipoEnvio) {
        switch (tipoEnvio.toLowerCase()) {
            case "maquina":
                return "No se encontraron empleados con correos válidos en el departamento de Máquina.";
            case "corte":
                return "No se encontraron empleados con correos válidos en el departamento de Corte.";
            case "empaque":
                return "No se encontraron empleados con correos válidos en el departamento de Empaque.";
            default:
                return "No hay correos válidos para enviar.";
        }
    }

    /**
     * Método adicional: Obtener estadísticas de correos por departamento
     * Útil para mostrar información en la interfaz
     */
    @GetMapping("/estadisticas")
    @ResponseBody
    public String obtenerEstadisticas() {
        long totalTodos = obtenerTodosLosCorreos().size();
        long totalMaquina = obtenerCorreosPorDepartamento("maquina").size();
        long totalCorte = obtenerCorreosPorDepartamento("corte").size();
        long totalEmpaque = obtenerCorreosPorDepartamento("empaque").size();

        return String.format(
                "Estadísticas de correos: Todos=%d, Máquina=%d, Corte=%d, Empaque=%d",
                totalTodos, totalMaquina, totalCorte, totalEmpaque);
    }
}