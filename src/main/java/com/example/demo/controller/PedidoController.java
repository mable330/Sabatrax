package com.example.demo.controller;

import com.example.demo.model.Pedido;
import com.example.demo.repository.MaquinaRepository;
import com.example.demo.repository.PedidoRepository;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.time.temporal.ChronoUnit;
import com.example.demo.repository.CorteRepository; // Si ya lo tienes
import com.example.demo.repository.MaquinaRepository;
import com.example.demo.repository.PedidoRepository;

@Controller
@RequestMapping("/pedidos")
public class PedidoController {

    @Autowired
    private PedidoRepository pedidoRepository;

    @Autowired
    private MaquinaRepository maquinaRepository;

    @Autowired
    private CorteRepository corteRepository;

    // 🔥 Asegúrate de tener este repositorio

    // ✅ Mostrar pedidos
    @GetMapping
    public String mostrarPedidos(Model model) {
        List<Pedido> pedidos = pedidoRepository.findAll();

        // 💡 Recorremos para calcular porcentaje por actividad al vuelo
        for (Pedido pedido : pedidos) {
            String medida = pedido.getMedidasSabanas();
            LocalDate fechaInicio = pedido.getFechaEnvio().minusDays(60);

            // 🛠️ Obtener cantidad de juegos cortados (desde la tabla de corte)
            int juegosCorte = corteRepository.contarJuegosCortadosPorMedida(medida, fechaInicio);

            // 🛠️ Obtener cantidad de piezas de máquina (plana + caucho + fundas)
            int planas = maquinaRepository.contarPorMedidaYTipoDesde(medida, "Plana", fechaInicio);
            int cauchos = maquinaRepository.contarPorMedidaYTipoDesde(medida, "Caucho", fechaInicio);
            int fundas = maquinaRepository.contarPorMedidaYTipoDesde(medida, "Fundas", fechaInicio);

            // 💡 Calcular juegos completos en máquina
            int juegosMaquina = Math.min(planas, Math.min(cauchos, fundas / 2));

            // 📊 Progreso por actividad
            double progresoCorte = (double) juegosCorte / pedido.getJuegos() * 30;
            double progresoMaquina = (double) juegosMaquina / pedido.getJuegos() * 40;
            double progresoEmpaque = (double) pedido.getCantidadEntregada() / pedido.getJuegos() * 30;

            double porcentajeTotal = progresoCorte + progresoMaquina + progresoEmpaque;

            // 👇 Guardamos en atributos temporales para Thymeleaf (NO en la base de datos)
            pedido.setProgresoCorte(progresoCorte);
            pedido.setProgresoMaquina(progresoMaquina);
            pedido.setProgresoEmpaque(progresoEmpaque);
            pedido.setPorcentajeTotal(Math.min(porcentajeTotal, 100.0));
        }

        model.addAttribute("pedidos", pedidos);

        // 📈 Preparar datos para la gráfica
        List<Pedido> pedidosCompletados = pedidos.stream()
                .filter(p -> p.getCantidadEntregada() >= p.getJuegos())
                .sorted((p1, p2) -> p1.getId().compareTo(p2.getId()))
                .toList();

        List<String> etiquetas = pedidosCompletados.stream()
                .map(p -> "Pedido " + p.getId())
                .toList();

        List<Integer> cantidades = pedidosCompletados.stream()
                .map(Pedido::getCantidadEntregada)
                .toList();

        model.addAttribute("etiquetas", etiquetas);
        model.addAttribute("cantidades", cantidades);

        return "pedido";
    }

    // ✅ Registrar nuevo pedido
    @PostMapping("/registrar")
    public String registrarPedido(
            @RequestParam String medidasSabanas,
            @RequestParam int juegos,
            @RequestParam String fechaEnvio,
            Model model) {

        try {
            // Validar y parsear fecha
            LocalDate fechaEntrega = LocalDate.parse(fechaEnvio);

            // Validar que la fecha no sea pasada
            if (fechaEntrega.isBefore(LocalDate.now())) {
                model.addAttribute("error", "❌ La fecha de entrega no puede ser anterior a hoy");
                return mostrarPedidos(model);
            }

            // Validar cantidad mínima
            if (juegos <= 0) {
                model.addAttribute("error", "❌ La cantidad de juegos debe ser mayor a 0");
                return mostrarPedidos(model);
            }

            // Verificar si ya existe un pedido con la misma medida y fecha
            Optional<Pedido> pedidoExistente = pedidoRepository.findByMedidasSabanasAndFechaEnvio(
                    medidasSabanas, fechaEntrega);

            if (pedidoExistente.isPresent()) {
                model.addAttribute("error",
                        "❌ Ya existe un pedido para la medida '" + medidasSabanas +
                                "' con fecha de entrega " + fechaEntrega);
                return mostrarPedidos(model);
            }

            // Crear y guardar el pedido
            Pedido pedido = new Pedido();
            pedido.setMedidasSabanas(medidasSabanas);
            pedido.setJuegos(juegos);
            pedido.setFechaEnvio(fechaEntrega);
            pedido.setCantidadEntregada(0);

            pedidoRepository.save(pedido);

            model.addAttribute("mensaje",
                    "✅ Pedido registrado exitosamente. ID: " + pedido.getId() +
                            " | Juegos: " + juegos + " | Medida: " + medidasSabanas);

        } catch (Exception e) {
            model.addAttribute("error", "❌ Error al registrar el pedido: " + e.getMessage());
        }

        return mostrarPedidos(model);
    }

    // ✅ Validar producción en máquina (trabaja por piezas)
    @PostMapping("/validar-produccion-maquina")
    @ResponseBody
    public ValidationResult validarProduccionMaquina(
            @RequestParam String medidasSabanas,
            @RequestParam String tipoSabana,
            @RequestParam int cantidadNueva) {

        try {
            // Buscar pedidos activos para esta medida
            List<Pedido> pedidosActivos = pedidoRepository.findByMedidasSabanasAndFechaEnvioAfter(
                    medidasSabanas, LocalDate.now().minusDays(1));

            if (pedidosActivos.isEmpty()) {
                return new ValidationResult(false,
                        "❌ No hay pedidos activos para la medida: " + medidasSabanas);
            }

            // Calcular total de juegos solicitados
            int totalJuegosSolicitados = pedidosActivos.stream()
                    .mapToInt(Pedido::getJuegos)
                    .sum();

            // Obtener producción actual (últimos 60 días)
            LocalDate fechaInicio = LocalDate.now().minusDays(60);
            int planasActuales = maquinaRepository.contarPorMedidaYTipoDesde(
                    medidasSabanas, "Plana", fechaInicio);
            int cauchosActuales = maquinaRepository.contarPorMedidaYTipoDesde(
                    medidasSabanas, "Caucho", fechaInicio);
            int fundasActuales = maquinaRepository.contarPorMedidaYTipoDesde(
                    medidasSabanas, "Fundas", fechaInicio);

            // Calcular límites según los juegos solicitados
            // 1 juego = 1 plana + 1 caucho + 2 fundas
            int limitePlanas = totalJuegosSolicitados;
            int limiteCauchos = totalJuegosSolicitados;
            int limiteFundas = totalJuegosSolicitados * 2;

            // Calcular nueva producción
            int nuevasPlanas = planasActuales + (tipoSabana.equals("Plana") ? cantidadNueva : 0);
            int nuevosCauchos = cauchosActuales + (tipoSabana.equals("Caucho") ? cantidadNueva : 0);
            int nuevasFundas = fundasActuales + (tipoSabana.equals("Fundas") ? cantidadNueva : 0);

            // Validar límites por tipo de pieza
            if (nuevasPlanas > limitePlanas) {
                int maxPermitido = limitePlanas - planasActuales;
                return new ValidationResult(false,
                        "🚨 Exceso de planas para " + medidasSabanas +
                                ". Máximo permitido: " + Math.max(0, maxPermitido) +
                                " (Tienes: " + planasActuales + "/" + limitePlanas + ")");
            }

            if (nuevosCauchos > limiteCauchos) {
                int maxPermitido = limiteCauchos - cauchosActuales;
                return new ValidationResult(false,
                        "🚨 Exceso de cauchos para " + medidasSabanas +
                                ". Máximo permitido: " + Math.max(0, maxPermitido) +
                                " (Tienes: " + cauchosActuales + "/" + limiteCauchos + ")");
            }

            if (nuevasFundas > limiteFundas) {
                int maxPermitido = limiteFundas - fundasActuales;
                return new ValidationResult(false,
                        "🚨 Exceso de fundas para " + medidasSabanas +
                                ". Máximo permitido: " + Math.max(0, maxPermitido) +
                                " (Tienes: " + fundasActuales + "/" + limiteFundas + ")");
            }

            // Retornar validación exitosa con información detallada
            return new ValidationResult(true,
                    "✅ Producción válida para " + medidasSabanas +
                            ". Puedes producir " + cantidadNueva + " " + tipoSabana.toLowerCase() +
                            "(s). Estado actual: Planas(" + planasActuales + "/" + limitePlanas +
                            "), Cauchos(" + cauchosActuales + "/" + limiteCauchos +
                            "), Fundas(" + fundasActuales + "/" + limiteFundas + ")");

        } catch (Exception e) {
            return new ValidationResult(false, "❌ Error en validación: " + e.getMessage());
        }
    }

    // ✅ Validar producción en empaque
    @PostMapping("/validar-produccion-empaque")
    @ResponseBody
    public ValidationResult validarProduccionEmpaque(
            @RequestParam String medidasSabanas,
            @RequestParam int cantidadNueva) {

        try {
            List<Pedido> pedidosActivos = pedidoRepository.findByMedidasSabanasAndFechaEnvioAfter(
                    medidasSabanas, LocalDate.now().minusDays(1));

            if (pedidosActivos.isEmpty()) {
                return new ValidationResult(false,
                        "❌ No hay pedidos activos para la medida: " + medidasSabanas);
            }

            int totalJuegosSolicitados = pedidosActivos.stream()
                    .mapToInt(Pedido::getJuegos)
                    .sum();

            int juegosYaEntregados = pedidosActivos.stream()
                    .mapToInt(Pedido::getCantidadEntregada)
                    .sum();

            if (juegosYaEntregados + cantidadNueva > totalJuegosSolicitados) {
                int maxPermitido = totalJuegosSolicitados - juegosYaEntregados;
                return new ValidationResult(false,
                        "🚨 Exceso de juegos. Solo puedes registrar " + Math.max(0, maxPermitido) + " juegos más.");
            }

            return new ValidationResult(true, "✅ Producción válida para empaque.");

        } catch (Exception e) {
            return new ValidationResult(false, "❌ Error en validación: " + e.getMessage());
        }
    }

    // ✅ Validar producción en corte (trabaja por juegos completos)
    // ✅ Validar y registrar producción en corte
    @PostMapping("/validar-produccion-corte")
    @ResponseBody
    public ValidationResult validarProduccionCorte(
            @RequestParam String medidasSabanas,
            @RequestParam int juegosCompletados) {

        try {
            if (juegosCompletados != 1) {
                return new ValidationResult(false, "❌ Solo puedes registrar un juego a la vez.");
            }

            List<Pedido> pedidosActivos = pedidoRepository.findByMedidasSabanasAndFechaEnvioAfter(
                    medidasSabanas, LocalDate.now().minusDays(1));

            if (pedidosActivos.isEmpty()) {
                return new ValidationResult(false, "❌ No hay pedidos activos para la medida: " + medidasSabanas);
            }

            int totalJuegosSolicitados = pedidosActivos.stream()
                    .mapToInt(Pedido::getJuegos)
                    .sum();

            int juegosYaEntregados = pedidosActivos.stream()
                    .mapToInt(Pedido::getCantidadEntregada)
                    .sum();

            if (juegosYaEntregados + juegosCompletados > totalJuegosSolicitados) {
                int maxPermitido = totalJuegosSolicitados - juegosYaEntregados;
                return new ValidationResult(false,
                        "🚨 Exceso de juegos completos. Solo puedes registrar " + Math.max(0, maxPermitido)
                                + " juego(s) más.");
            }

            // 🔥 Actualizamos directamente la entrega en la base de datos
            for (Pedido pedido : pedidosActivos) {
                int juegosFaltantes = pedido.getJuegos() - pedido.getCantidadEntregada();

                if (juegosFaltantes <= 0)
                    continue;

                pedido.setCantidadEntregada(pedido.getCantidadEntregada() + 1);
                pedidoRepository.save(pedido);
                break; // Solo registramos 1 juego a la vez
            }

            int juegosRestantes = totalJuegosSolicitados - (juegosYaEntregados + 1);

            return new ValidationResult(true,
                    "✅ Registro exitoso. Has entregado 1 juego. " +
                            "Quedan " + juegosRestantes + " juego(s) pendientes.");

        } catch (Exception e) {
            return new ValidationResult(false, "❌ Error en validación: " + e.getMessage());
        }
    }

    // ✅ Actualizar cantidad entregada (usado por corte)
    @PostMapping("/actualizar-entrega")
    public String actualizarEntrega(
            @RequestParam Long pedidoId,
            @RequestParam int cantidadNueva,
            Model model) {

        try {
            Optional<Pedido> pedidoOpt = pedidoRepository.findById(pedidoId);

            if (pedidoOpt.isEmpty()) {
                model.addAttribute("error", "❌ Pedido no encontrado");
                return mostrarPedidos(model);
            }

            Pedido pedido = pedidoOpt.get();

            // Validar que no exceda el total solicitado
            if (pedido.getCantidadEntregada() + cantidadNueva > pedido.getJuegos()) {
                int maxPermitido = pedido.getJuegos() - pedido.getCantidadEntregada();
                model.addAttribute("error",
                        "❌ No puedes entregar más de lo solicitado. Máximo: " + maxPermitido + " juegos");
                return mostrarPedidos(model);
            }

            // Actualizar cantidad entregada
            pedido.setCantidadEntregada(pedido.getCantidadEntregada() + cantidadNueva);
            pedidoRepository.save(pedido);

            String estadoPedido = pedido.getCantidadEntregada() >= pedido.getJuegos() ? "COMPLETADO" : "PENDIENTE";

            model.addAttribute("mensaje",
                    "✅ Entrega actualizada. Pedido ID: " + pedidoId +
                            " | Entregados: " + pedido.getCantidadEntregada() +
                            "/" + pedido.getJuegos() + " | Estado: " + estadoPedido);

        } catch (Exception e) {
            model.addAttribute("error", "❌ Error al actualizar entrega: " + e.getMessage());
        }

        return mostrarPedidos(model);
    }

    // ✅ Obtener información de un pedido específico
    @GetMapping("/info/{id}")
    @ResponseBody
    public PedidoInfo obtenerInfoPedido(@PathVariable Long id) {
        try {
            Optional<Pedido> pedidoOpt = pedidoRepository.findById(id);

            if (pedidoOpt.isEmpty()) {
                return new PedidoInfo(false, "Pedido no encontrado", null);
            }

            Pedido pedido = pedidoOpt.get();

            // Calcular información adicional
            int juegosRestantes = pedido.getJuegos() - pedido.getCantidadEntregada();
            double porcentajeCompletado = (double) pedido.getCantidadEntregada() / pedido.getJuegos() * 100;

            String info = String.format(
                    "📋 Pedido ID: %d\n" +
                            "📏 Medida: %s\n" +
                            "📦 Juegos solicitados: %d\n" +
                            "✅ Juegos completados: %d\n" +
                            "⏳ Juegos restantes: %d\n" +
                            "📊 Progreso: %.1f%%\n" +
                            "📅 Fecha entrega: %s",
                    pedido.getId(),
                    pedido.getMedidasSabanas(),
                    pedido.getJuegos(),
                    pedido.getCantidadEntregada(),
                    juegosRestantes,
                    porcentajeCompletado,
                    pedido.getFechaEnvio());

            return new PedidoInfo(true, info, pedido);

        } catch (Exception e) {
            return new PedidoInfo(false, "Error: " + e.getMessage(), null);
        }
    }

    // ✅ Clase para validaciones
    public static class ValidationResult {
        private boolean valid;
        private String message;

        public ValidationResult(boolean valid, String message) {
            this.valid = valid;
            this.message = message;
        }

        public boolean isValid() {
            return valid;
        }

        public String getMessage() {
            return message;
        }
    }

    // ✅ Clase para información de pedidos
    public static class PedidoInfo {
        private boolean success;
        private String message;
        private Pedido pedido;

        public PedidoInfo(boolean success, String message, Pedido pedido) {
            this.success = success;
            this.message = message;
            this.pedido = pedido;
        }

        public boolean isSuccess() {
            return success;
        }

        public String getMessage() {
            return message;
        }

        public Pedido getPedido() {
            return pedido;
        }
    }
}