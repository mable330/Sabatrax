package com.example.demo.controller;

import com.example.demo.model.Pedido;
import com.example.demo.repository.MaquinaRepository;
import com.example.demo.repository.PedidoRepository;
import com.example.demo.repository.CorteRepository;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Controller
@RequestMapping("/pedidos")
public class PedidoController {

    @Autowired
    private PedidoRepository pedidoRepository;

    @Autowired
    private MaquinaRepository maquinaRepository;

    @Autowired
    private CorteRepository corteRepository;

    // ✅ Mostrar pedidos
    @GetMapping
    public String mostrarPedidos(Model model) {
        List<Pedido> pedidos = pedidoRepository.findAll();

        for (Pedido pedido : pedidos) {
            String medida = pedido.getMedidasSabanas();

            int juegosCorte = corteRepository.contarJuegosCortadosPorPedido(pedido.getId());

            int planas = maquinaRepository.contarPorPedidoYTipo(pedido.getId(), "Plana");
            int cauchos = maquinaRepository.contarPorPedidoYTipo(pedido.getId(), "Caucho");
            int fundas = maquinaRepository.contarPorPedidoYTipo(pedido.getId(), "Fundas");

            int juegosMaquina = Math.min(planas, Math.min(cauchos, fundas / 2));

            double progresoCorte = (double) juegosCorte / pedido.getJuegos() * 30;
            double progresoMaquina = (double) juegosMaquina / pedido.getJuegos() * 40;
            double progresoEmpaque = (double) pedido.getCantidadEntregada() / pedido.getJuegos() * 30;

            double porcentajeTotal = progresoCorte + progresoMaquina + progresoEmpaque;

            pedido.setProgresoCorte(progresoCorte);
            pedido.setProgresoMaquina(progresoMaquina);
            pedido.setProgresoEmpaque(progresoEmpaque);
            pedido.setPorcentajeTotal(Math.min(porcentajeTotal, 100.0));
        }

        model.addAttribute("pedidos", pedidos);

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
            RedirectAttributes redirectAttributes) {

        try {
            if (medidasSabanas == null || medidasSabanas.trim().isEmpty()) {
                redirectAttributes.addFlashAttribute("error", "❌ La medida de sábanas es obligatoria");
                return "redirect:/pedidos";
            }

            LocalDate fechaEntrega;
            try {
                fechaEntrega = LocalDate.parse(fechaEnvio);
            } catch (Exception e) {
                redirectAttributes.addFlashAttribute("error", "❌ Formato de fecha inválido. Use YYYY-MM-DD");
                return "redirect:/pedidos";
            }

            if (fechaEntrega.isBefore(LocalDate.now())) {
                redirectAttributes.addFlashAttribute("error", "❌ La fecha de entrega no puede ser anterior a hoy");
                return "redirect:/pedidos";
            }

            if (juegos <= 0) {
                redirectAttributes.addFlashAttribute("error", "❌ La cantidad de juegos debe ser mayor a 0");
                return "redirect:/pedidos";
            }

            List<Pedido> pedidosIncompletos = pedidoRepository.findPedidosIncompletosPorMedida(medidasSabanas);

            if (!pedidosIncompletos.isEmpty()) {
                Pedido pedidoIncompleto = pedidosIncompletos.get(0);
                int faltantes = pedidoIncompleto.getJuegos() - pedidoIncompleto.getCantidadEntregada();

                redirectAttributes.addFlashAttribute("error",
                        String.format(
                                "❌ Existe un pedido incompleto para '%s'. Pedido ID: %d - Faltan %d juegos de %d total. Complete este pedido antes de crear uno nuevo.",
                                medidasSabanas, pedidoIncompleto.getId(), faltantes, pedidoIncompleto.getJuegos()));
                return "redirect:/pedidos";
            }

            Pedido pedido = new Pedido();
            pedido.setMedidasSabanas(medidasSabanas.trim());
            pedido.setJuegos(juegos);
            pedido.setFechaEnvio(fechaEntrega);
            pedido.setCantidadEntregada(0);

            Pedido pedidoGuardado = pedidoRepository.save(pedido);

            redirectAttributes.addFlashAttribute("mensaje",
                    String.format(
                            "✅ Pedido registrado exitosamente. ID: %d | Juegos: %d | Medida: %s | Fecha entrega: %s",
                            pedidoGuardado.getId(), juegos, medidasSabanas, fechaEntrega));

        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "❌ Error al registrar el pedido: " + e.getMessage());
            e.printStackTrace();
        }

        return "redirect:/pedidos";
    }

    // ✅ Validar producción en máquina
    @PostMapping("/validar-produccion-maquina")
    @ResponseBody
    public ValidationResult validarProduccionMaquina(
            @RequestParam String medidasSabanas,
            @RequestParam String tipoSabana,
            @RequestParam int cantidadNueva) {

        try {
            List<Pedido> pedidosActivos = pedidoRepository.findPedidosIncompletosPorMedida(medidasSabanas);

            if (pedidosActivos.isEmpty()) {
                return new ValidationResult(false, "❌ No hay pedidos activos para la medida: " + medidasSabanas);
            }

            int totalJuegosSolicitados = pedidosActivos.stream()
                    .mapToInt(Pedido::getJuegos)
                    .sum();

            LocalDate fechaInicio = LocalDate.now().minusDays(60);
            int planasActuales = maquinaRepository.contarPorMedidaYTipoDesde(medidasSabanas, "Plana", fechaInicio);
            int cauchosActuales = maquinaRepository.contarPorMedidaYTipoDesde(medidasSabanas, "Caucho", fechaInicio);
            int fundasActuales = maquinaRepository.contarPorMedidaYTipoDesde(medidasSabanas, "Fundas", fechaInicio);

            int limitePlanas = totalJuegosSolicitados;
            int limiteCauchos = totalJuegosSolicitados;
            int limiteFundas = totalJuegosSolicitados * 2;

            int nuevasPlanas = planasActuales + (tipoSabana.equals("Plana") ? cantidadNueva : 0);
            int nuevosCauchos = cauchosActuales + (tipoSabana.equals("Caucho") ? cantidadNueva : 0);
            int nuevasFundas = fundasActuales + (tipoSabana.equals("Fundas") ? cantidadNueva : 0);

            if (nuevasPlanas > limitePlanas) {
                int maxPermitido = limitePlanas - planasActuales;
                return new ValidationResult(false,
                        "🚨 Exceso de planas para " + medidasSabanas +
                                ". Máximo permitido: " + Math.max(0, maxPermitido));
            }

            if (nuevosCauchos > limiteCauchos) {
                int maxPermitido = limiteCauchos - cauchosActuales;
                return new ValidationResult(false,
                        "🚨 Exceso de cauchos para " + medidasSabanas +
                                ". Máximo permitido: " + Math.max(0, maxPermitido));
            }

            if (nuevasFundas > limiteFundas) {
                int maxPermitido = limiteFundas - fundasActuales;
                return new ValidationResult(false,
                        "🚨 Exceso de fundas para " + medidasSabanas +
                                ". Máximo permitido: " + Math.max(0, maxPermitido));
            }

            return new ValidationResult(true, "✅ Producción válida para " + medidasSabanas);

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
            List<Pedido> pedidosActivos = pedidoRepository.findPedidosIncompletosPorMedida(medidasSabanas);

            if (pedidosActivos.isEmpty()) {
                return new ValidationResult(false, "❌ No hay pedidos activos para la medida: " + medidasSabanas);
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

    // ✅ Validar producción en corte
    @PostMapping("/validar-produccion-corte")
    @ResponseBody
    public ValidationResult validarProduccionCorte(
            @RequestParam String medidasSabanas,
            @RequestParam int juegosCompletados) {

        try {
            if (juegosCompletados != 1) {
                return new ValidationResult(false, "❌ Solo puedes registrar un juego a la vez.");
            }

            List<Pedido> pedidosActivos = pedidoRepository.findPedidosIncompletosPorMedida(medidasSabanas);

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
                return new ValidationResult(false, "🚨 Exceso de juegos completos. Solo puedes registrar "
                        + Math.max(0, maxPermitido) + " juego(s) más.");
            }

            for (Pedido pedido : pedidosActivos) {
                int juegosFaltantes = pedido.getJuegos() - pedido.getCantidadEntregada();

                if (juegosFaltantes <= 0)
                    continue;

                pedido.setCantidadEntregada(pedido.getCantidadEntregada() + 1);
                pedidoRepository.save(pedido);
                break;
            }

            int juegosRestantes = totalJuegosSolicitados - (juegosYaEntregados + 1);

            return new ValidationResult(true,
                    "✅ Registro exitoso. Has entregado 1 juego. Quedan " + juegosRestantes + " juego(s) pendientes.");

        } catch (Exception e) {
            return new ValidationResult(false, "❌ Error en validación: " + e.getMessage());
        }
    }

    // ✅ Actualizar cantidad entregada
    @PostMapping("/actualizar-entrega")
    public String actualizarEntrega(
            @RequestParam Long pedidoId,
            @RequestParam int cantidadNueva,
            RedirectAttributes redirectAttributes) {

        try {
            Optional<Pedido> pedidoOpt = pedidoRepository.findById(pedidoId);

            if (pedidoOpt.isEmpty()) {
                redirectAttributes.addFlashAttribute("error", "❌ Pedido no encontrado");
                return "redirect:/pedidos";
            }

            Pedido pedido = pedidoOpt.get();

            if (pedido.getCantidadEntregada() + cantidadNueva > pedido.getJuegos()) {
                int maxPermitido = pedido.getJuegos() - pedido.getCantidadEntregada();
                redirectAttributes.addFlashAttribute("error",
                        "❌ No puedes entregar más de lo solicitado. Máximo: " + maxPermitido + " juegos");
                return "redirect:/pedidos";
            }

            pedido.setCantidadEntregada(pedido.getCantidadEntregada() + cantidadNueva);
            pedidoRepository.save(pedido);

            String estadoPedido = pedido.getCantidadEntregada() >= pedido.getJuegos() ? "COMPLETADO" : "PENDIENTE";

            redirectAttributes.addFlashAttribute("mensaje",
                    "✅ Entrega actualizada. Pedido ID: " + pedidoId + " | Entregados: " + pedido.getCantidadEntregada()
                            + "/" + pedido.getJuegos() + " | Estado: " + estadoPedido);

        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "❌ Error al actualizar entrega: " + e.getMessage());
        }

        return "redirect:/pedidos";
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
}
