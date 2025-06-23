package com.example.demo.controller;

import com.example.demo.model.Empaque;
import com.example.demo.model.Pedido;
import com.example.demo.model.Usuario;
import com.example.demo.model.PrecioActividad;
import com.example.demo.repository.EmpaqueRepository;
import com.example.demo.repository.PedidoRepository;
import com.example.demo.repository.UsuarioRepository;
import com.example.demo.repository.PrecioActividadRepository;

import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

@Controller
@RequestMapping("/empaque")
public class EmpaqueController {

    @Autowired
    private EmpaqueRepository repository;

    @Autowired
    private UsuarioRepository usuarioRepository;

    // 🔥 CORREGIDO: Era "PredidoRepository", ahora es "PedidoRepository"
    @Autowired
    private PedidoRepository pedidoRepository;

    // 🔥 NUEVO: Repositorio para precios dinámicos
    @Autowired
    private PrecioActividadRepository precioActividadRepository;

    // ✅ Mostrar la lista de empaques
    @GetMapping
    public String mostrarLista(Model model, HttpSession session) {
        if (!isAuthenticated(session))
            return "redirect:/login";

        String telefono = (String) session.getAttribute("telefono");
        Usuario usuario = usuarioRepository.findById(telefono).orElse(null);
        if (usuario == null)
            return "redirect:/login";

        List<Empaque> empaques = repository.findByUsuario(usuario);

        model.addAttribute("empaque", empaques);
        model.addAttribute("Ntelefono", telefono);
        model.addAttribute("nombreEmpleado", usuario.getNombre());
        model.addAttribute("mensajeBienvenida", "Bienvenido a " + usuario.getActividad());
        model.addAttribute("totalRegistros", empaques.size());

        return "empaque";
    }

    // ✅ Mostrar formulario de registro
    @GetMapping("/registroE")
    public String mostrarFormularioRegistro(Model model, HttpSession session) {
        if (!isAuthenticated(session))
            return "redirect:/login";

        model.addAttribute("empaque", new Empaque());
        return "registroE";
    }

    // ✅ Registrar actividad y actualizar pedido - CORREGIDO
    @PostMapping("/registroE")
    @Transactional
    public String registrarActividad(
            @ModelAttribute Empaque empaque,
            @RequestParam("imagenFile") MultipartFile imagenFile,
            HttpSession session, Model model) {

        if (!isAuthenticated(session))
            return "redirect:/login";

        try {
            String telefono = (String) session.getAttribute("telefono");
            Usuario usuario = usuarioRepository.findById(telefono).orElse(null);

            if (usuario == null)
                return "redirect:/login";

            String medidas = empaque.getMedidas();
            int cantidadNueva;

            try {
                cantidadNueva = Integer.parseInt(empaque.getJuegos());
            } catch (NumberFormatException e) {
                model.addAttribute("error", "🚨 La cantidad de juegos debe ser un número válido.");
                return "registroE";
            }

            LocalDate fechaActual = LocalDate.now();

            // 🔥 CORRECCIÓN: Buscar solo pedidos ACTIVOS (no completados)
            List<Pedido> pedidosActivos = pedidoRepository.findByMedidasSabanasAndFechaEnvioAfter(medidas, fechaActual);

            // 🔥 NUEVA VALIDACIÓN: Filtrar solo pedidos que NO estén completados
            List<Pedido> pedidosPendientes = pedidosActivos.stream()
                    .filter(p -> p.getCantidadEntregada() < p.getJuegos())
                    .collect(Collectors.toList());

            if (pedidosPendientes.isEmpty()) {
                // Verificar si hay pedidos pero todos están completados
                if (!pedidosActivos.isEmpty()) {
                    model.addAttribute("error",
                            "🚨 Todos los pedidos para la medida '" + medidas + "' ya están completados.");
                } else {
                    model.addAttribute("error", "🚨 No hay pedidos activos para la medida '" + medidas + "'.");
                }
                return "registroE";
            }

            // 🔥 CORRECCIÓN: Calcular solo sobre pedidos pendientes
            int totalJuegosSolicitados = pedidosPendientes.stream().mapToInt(Pedido::getJuegos).sum();
            int totalJuegosEntregados = pedidosPendientes.stream().mapToInt(Pedido::getCantidadEntregada).sum();

            if (totalJuegosEntregados + cantidadNueva > totalJuegosSolicitados) {
                model.addAttribute("error", "🚨 Exceso de producción. Solo puedes registrar " +
                        Math.max(0, totalJuegosSolicitados - totalJuegosEntregados) + " juegos más.");
                return "registroE";
            }

            empaque.setId(UUID.randomUUID().toString());
            empaque.setFecha(new Date());
            empaque.setUsuario(usuario);

            if (imagenFile != null && !imagenFile.isEmpty()) {
                empaque.setImagen(imagenFile.getBytes());
            }

            repository.save(empaque);

            // 🔥 CORRECCIÓN: Actualizar solo pedidos pendientes
            actualizarPedidosDespuesDeProduccion(medidas);

            return "redirect:/empaque";

        } catch (Exception e) {
            model.addAttribute("error", "Error al registrar la actividad: " + e.getMessage());
            return "registroE";
        }
    }

    // 🔥 ACTUALIZADO: Cálculo de salario DINÁMICO basado en precios registrados
    @PostMapping("/calcular")
    public String calcularSalario(@RequestParam("selectedRows") List<String> ids, Model model, HttpSession session) {
        if (!isAuthenticated(session))
            return "redirect:/login";

        String telefono = (String) session.getAttribute("telefono");
        Usuario usuario = usuarioRepository.findById(telefono).orElse(null);
        if (usuario == null)
            return "redirect:/login";

        List<Empaque> todos = repository.findByUsuario(usuario);
        List<Empaque> seleccionados = todos.stream().filter(e -> ids.contains(e.getId())).collect(Collectors.toList());

        // 🔥 NUEVO: Cálculo dinámico basado en precios registrados
        int total = calcularSalarioDinamico(seleccionados, usuario.getActividad());

        model.addAttribute("total", total);
        model.addAttribute("empaque", todos);
        model.addAttribute("Ntelefono", telefono);
        model.addAttribute("nombreEmpleado", usuario.getNombre());
        model.addAttribute("mensajeBienvenida", "Bienvenido a " + usuario.getActividad());
        model.addAttribute("totalRegistros", todos.size());
        model.addAttribute("registrosSeleccionados", seleccionados.size()); // Info adicional

        return "empaque";
    }

    // ✅ Eliminar registros y actualizar pedido
    @PostMapping("/eliminar")
    @Transactional
    public String eliminarRegistros(@RequestParam("selectedRows") List<String> ids, HttpSession session) {
        if (!isAuthenticated(session))
            return "redirect:/login";

        String telefono = (String) session.getAttribute("telefono");
        Usuario usuario = usuarioRepository.findById(telefono).orElse(null);
        if (usuario == null)
            return "redirect:/login";

        List<Empaque> registros = repository.findAllById(ids);
        List<String> idsValidos = registros.stream()
                .filter(e -> e.getUsuario().getTelefono().equals(telefono))
                .map(Empaque::getId)
                .toList();

        if (!idsValidos.isEmpty()) {
            repository.deleteAllById(idsValidos);

            for (Empaque registro : registros) {
                if (idsValidos.contains(registro.getId())) {
                    actualizarPedidosDespuesDeProduccion(registro.getMedidas());
                }
            }
        }

        return "redirect:/empaque";
    }

    @GetMapping("/imagen/{id}")
    @ResponseBody
    public ResponseEntity<byte[]> verImagen(@PathVariable String id, HttpSession session) {
        if (!isAuthenticated(session)) {
            return ResponseEntity.status(401).build();
        }

        String telefonoUsuario = (String) session.getAttribute("telefono");
        Optional<Empaque> optionalEmpaque = repository.findById(id);

        if (optionalEmpaque.isEmpty() || optionalEmpaque.get().getImagen() == null
                || !optionalEmpaque.get().getUsuario().getTelefono().equals(telefonoUsuario)) {
            return ResponseEntity.notFound().build();
        }

        return ResponseEntity.ok()
                .contentType(org.springframework.http.MediaType.IMAGE_JPEG)
                .body(optionalEmpaque.get().getImagen());
    }

    // ================================
    // 🔥 NUEVOS MÉTODOS PARA PRECIOS DINÁMICOS
    // ================================

    /**
     * 🔥 NUEVO: Cálculo dinámico de salario basado en precios registrados
     * Funciona igual que en MaquinaController
     */
    private int calcularSalarioDinamico(List<Empaque> seleccionados, String actividadUsuario) {
        // Obtener todos los precios para la actividad del usuario
        List<PrecioActividad> preciosActividad = precioActividadRepository
                .findByActividad(actividadUsuario.toLowerCase());

        int totalSalario = 0;

        for (Empaque empaque : seleccionados) {
            String tipoEmpaque = empaque.getTipoEmpaque();
            int cantidad;

            try {
                cantidad = Integer.parseInt(empaque.getJuegos());
            } catch (NumberFormatException e) {
                continue; // Saltar si no es un número válido
            }

            // Buscar el precio específico para este tipo de empaque
            Integer precioPorUnidad = preciosActividad.stream()
                    .filter(precio -> precio.getDescripcion().equalsIgnoreCase(tipoEmpaque))
                    .findFirst()
                    .map(PrecioActividad::getPrecio)
                    .orElse(getPrecioDefault(tipoEmpaque)); // Fallback a precios por defecto

            // Calcular: cantidad * precio por unidad
            totalSalario += cantidad * precioPorUnidad;
        }

        return totalSalario;
    }

    /**
     * 🔥 Precios por defecto si no hay precios registrados en la base de datos
     * Mantiene compatibilidad con el sistema anterior
     */
    private Integer getPrecioDefault(String tipoEmpaque) {
        return switch (tipoEmpaque) {
            case "Bolsa" -> 4000;
            case "Carton" -> 450;
            default -> 0;
        };
    }

    // ================================
    // MÉTODOS AUXILIARES - CORREGIDOS
    // ================================

    // 🔥 MÉTODO CORREGIDO: Solo actualizar pedidos que NO estén completados
    private void actualizarPedidosDespuesDeProduccion(String medidas) {
        LocalDate fechaActual = LocalDate.now();
        List<Pedido> pedidosActivos = pedidoRepository.findByMedidasSabanasAndFechaEnvioAfter(medidas, fechaActual);

        if (pedidosActivos.isEmpty())
            return;

        // 🔥 CORRECCIÓN: Filtrar solo pedidos NO completados
        List<Pedido> pedidosPendientes = pedidosActivos.stream()
                .filter(p -> p.getCantidadEntregada() < p.getJuegos())
                .collect(Collectors.toList());

        if (pedidosPendientes.isEmpty())
            return;

        List<Empaque> producciones = repository.findByMedidas(medidas);
        int juegosProducidos = producciones.stream().mapToInt(e -> {
            try {
                return Integer.parseInt(e.getJuegos());
            } catch (NumberFormatException ex) {
                return 0;
            }
        }).sum();

        // 🔥 CORRECCIÓN: Procesar solo pedidos pendientes
        for (Pedido pedido : pedidosPendientes) {
            int juegosFaltantes = pedido.getJuegos() - pedido.getCantidadEntregada();

            if (juegosFaltantes <= 0)
                continue;

            int juegosAEntregar = Math.min(juegosProducidos, pedido.getJuegos());
            pedido.setCantidadEntregada(juegosAEntregar);
            pedidoRepository.save(pedido);
        }
    }

    private boolean isAuthenticated(HttpSession session) {
        return session.getAttribute("telefono") != null && session.getAttribute("actividad") != null;
    }
}