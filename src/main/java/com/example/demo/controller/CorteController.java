package com.example.demo.controller;

import com.example.demo.model.Corte;
import com.example.demo.model.Pedido;
import com.example.demo.model.Usuario;
import com.example.demo.model.PrecioActividad;
import com.example.demo.repository.CorteRepository;
import com.example.demo.repository.UsuarioRepository;
import com.example.demo.repository.PedidoRepository;
import com.example.demo.repository.PrecioActividadRepository;

import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDate;
import java.util.Date;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

@Controller
@RequestMapping("/corte")
public class CorteController {

    @Autowired
    private CorteRepository repository;

    @Autowired
    private UsuarioRepository usuarioRepository;

    @Autowired
    private PedidoRepository pedidoRepository;

    @Autowired
    private PrecioActividadRepository precioActividadRepository;

    // ✅ Mostrar formulario de registro
    @GetMapping("/registroC")
    public String mostrarFormularioRegistro(Model model, HttpSession session) {
        if (!isAuthenticated(session)) {
            return "redirect:/login";
        }

        String telefono = (String) session.getAttribute("telefono");
        Usuario usuario = usuarioRepository.findByTelefono(telefono);
        if (usuario == null) {
            return "redirect:/login";
        }

        model.addAttribute("corte", new Corte());
        model.addAttribute("Ntelefono", telefono);
        model.addAttribute("nombreEmpleado", usuario.getNombre());
        model.addAttribute("totalRegistros", repository.findByUsuario(usuario).size());

        return "registroC";
    }

    // ✅ Guardar actividad - VALIDACIÓN CORREGIDA PARA PROCESO COMPARTIDO
    @PostMapping("/registroC")
    @Transactional
    public String guardarDesdeFormulario(
            @ModelAttribute("corte") Corte corte,
            @RequestParam(value = "imagenFile", required = false) MultipartFile imagen,
            HttpSession session,
            Model model) {

        String telefono = (String) session.getAttribute("telefono");
        if (telefono == null)
            return "redirect:/login";

        Usuario usuario = usuarioRepository.findByTelefono(telefono);
        if (usuario == null)
            return "redirect:/login";

        try {
            String medidas = corte.getMedidas();
            int juegosNuevos = Integer.parseInt(corte.getJuegos());
            LocalDate fechaActual = LocalDate.now();

            // 🔥 Buscar el pedido más reciente para la medida

            Pedido pedidoActual = pedidoRepository.findByMedidasSabanas(medidas)
                    .stream()
                    .findFirst()
                    .orElse(null);

            if (pedidoActual == null) {
                model.addAttribute("error", "🚨 No hay pedidos activos para la medida '" + medidas + "'.");
                return "registroC";
            }

            // 🔥 Contar juegos cortados desde la fecha de creación del pedido actual
            int juegosYaCortados = repository.contarJuegosCortadosPorPedido(pedidoActual.getId());

            // 🔥 Límite basado en el pedido actual
            int limiteJuegosCorte = pedidoActual.getJuegos();

            // Validar que no exceda el límite
            if (juegosYaCortados + juegosNuevos > limiteJuegosCorte) {
                int maxPermitido = Math.max(0, limiteJuegosCorte - juegosYaCortados);
                model.addAttribute("error",
                        "🚨 Exceso de juegos cortados para " + medidas +
                                ". Máximo permitido: " + maxPermitido +
                                " (Ya tienes: " + juegosYaCortados + "/" + limiteJuegosCorte + ")");
                return "registroC";
            }

            // 🔥 Guardar la actividad
            corte.setUsuario(usuario);
            corte.setId(UUID.randomUUID().toString());
            corte.setFecha(LocalDate.now());
            corte.setPedido(pedidoActual);
            if (imagen != null && !imagen.isEmpty()) {
                corte.setImagen(imagen.getBytes());
            }
            repository.save(corte);

            // 🔥 Mensaje de éxito con progreso
            int nuevosJuegosCortados = juegosYaCortados + juegosNuevos;
            double porcentajeCompletado = (nuevosJuegosCortados * 100.0) / limiteJuegosCorte;

            model.addAttribute("mensaje",
                    "✅ Registro exitoso: " + juegosNuevos + " juego(s) cortado(s) para " + medidas +
                            "\n📊 Progreso en corte: " + nuevosJuegosCortados + "/" + limiteJuegosCorte
                            + " juegos (" + String.format("%.1f", porcentajeCompletado) + "%)" +
                            "\n📋 Estado: "
                            + (nuevosJuegosCortados >= limiteJuegosCorte ? "COMPLETO ✅" : "EN PROCESO ⏳"));

            return "redirect:/corte?exito";

        } catch (NumberFormatException e) {
            model.addAttribute("error", "Error: El número de juegos debe ser un valor numérico válido.");
            return "registroC";
        } catch (Exception e) {
            model.addAttribute("error", "Error al guardar: " + e.getMessage());
            return "registroC";
        }
    }

    // ✅ Mostrar la lista de cortes
    @GetMapping
    public String mostrarLista(Model model, HttpSession session) {
        if (!isAuthenticated(session))
            return "redirect:/login";

        String telefono = (String) session.getAttribute("telefono");
        Usuario usuario = usuarioRepository.findById(telefono).orElse(null);
        if (usuario == null)
            return "redirect:/login";

        List<Corte> cortes = repository.findByUsuario(usuario);

        model.addAttribute("corte", cortes);
        model.addAttribute("Ntelefono", telefono);
        model.addAttribute("actividad", session.getAttribute("actividad"));
        model.addAttribute("nombreEmpleado", usuario.getNombre());
        model.addAttribute("mensajeBienvenida", "Bienvenido a " + usuario.getActividad());
        model.addAttribute("totalRegistros", cortes.size());

        return "corte";
    }

    // ✅ Cálculo de salario DINÁMICO basado en precios registrados
    @PostMapping("/calcular")
    public String calcularSalario(
            @RequestParam("selectedRows") List<String> ids,
            Model model, HttpSession session) {

        if (!isAuthenticated(session))
            return "redirect:/login";

        String telefono = (String) session.getAttribute("telefono");
        Usuario usuario = usuarioRepository.findById(telefono).orElse(null);
        if (usuario == null)
            return "redirect:/login";

        List<Corte> todos = repository.findByUsuario(usuario);
        List<Corte> seleccionados = todos.stream()
                .filter(c -> ids.contains(c.getId()))
                .collect(Collectors.toList());

        // 🔥 NUEVO: Cálculo dinámico basado en precios registrados
        int total = calcularSalarioDinamico(seleccionados, usuario.getActividad());

        model.addAttribute("total", total);
        model.addAttribute("corte", todos);
        model.addAttribute("Ntelefono", telefono);
        model.addAttribute("nombreEmpleado", usuario.getNombre());
        model.addAttribute("actividad", usuario.getActividad());
        model.addAttribute("mensajeBienvenida", "Bienvenido a " + usuario.getActividad());
        model.addAttribute("totalRegistros", todos.size());
        model.addAttribute("registrosSeleccionados", seleccionados.size());

        model.addAttribute("idsSeleccionados", ids);

        return "corte";
    }

    // ✅ Eliminar registros seleccionados
    @PostMapping("/eliminar")
    public String eliminarRegistros(@RequestParam("selectedRows") List<String> ids, HttpSession session) {
        if (!isAuthenticated(session))
            return "redirect:/login";

        String telefono = (String) session.getAttribute("telefono");
        Usuario usuario = usuarioRepository.findById(telefono).orElse(null);
        if (usuario == null)
            return "redirect:/login";

        List<Corte> registros = repository.findAllById(ids);
        List<String> idsValidos = registros.stream()
                .filter(c -> c.getUsuario().getTelefono().equals(telefono))
                .map(Corte::getId)
                .toList();

        if (!idsValidos.isEmpty()) {
            repository.deleteAllById(idsValidos);
        }

        return "redirect:/corte";
    }

    // ✅ Mostrar imagen
    @GetMapping("/imagen/{id}")
    @ResponseBody
    public ResponseEntity<byte[]> verImagen(@PathVariable String id, HttpSession session) {
        if (!isAuthenticated(session)) {
            return ResponseEntity.status(401).build();
        }

        String telefonoUsuario = (String) session.getAttribute("telefono");
        Optional<Corte> optionalCorte = repository.findById(id);

        if (optionalCorte.isEmpty() || optionalCorte.get().getImagen() == null
                || !optionalCorte.get().getUsuario().getTelefono().equals(telefonoUsuario)) {
            return ResponseEntity.notFound().build();
        }

        MediaType contentType = MediaType.IMAGE_JPEG;
        try {
            String fileType = determineImageType(optionalCorte.get().getImagen());
            if ("png".equalsIgnoreCase(fileType))
                contentType = MediaType.IMAGE_PNG;
            else if ("gif".equalsIgnoreCase(fileType))
                contentType = MediaType.IMAGE_GIF;
        } catch (Exception ignored) {
        }

        return ResponseEntity.ok()
                .contentType(contentType)
                .body(optionalCorte.get().getImagen());
    }

    // ✅ Estadísticas de producción
    @GetMapping("/estadisticas/{medidas}")
    @ResponseBody
    public ResponseEntity<?> obtenerEstadisticasProduccion(@PathVariable String medidas, HttpSession session) {
        if (!isAuthenticated(session)) {
            return ResponseEntity.status(401).build();
        }

        try {
            LocalDate fechaActual = LocalDate.now();
            LocalDate fechaInicio = fechaActual.minusDays(60);

            int juegosCortados = repository.contarJuegosPorMedidaDesde(medidas, fechaInicio);

            List<Pedido> pedidosActivos = pedidoRepository.findByMedidasSabanasAndFechaEnvioAfter(medidas, fechaActual);
            int totalSolicitado = pedidosActivos.stream().mapToInt(Pedido::getJuegos).sum();

            var estadisticas = new java.util.HashMap<String, Object>();
            estadisticas.put("medidas", medidas);
            estadisticas.put("juegosCortados", juegosCortados);
            estadisticas.put("juegosNecesarios", totalSolicitado);
            estadisticas.put("porcentajeCompletado",
                    totalSolicitado > 0 ? (juegosCortados * 100.0 / totalSolicitado) : 0);
            estadisticas.put("puedeProducirMas", juegosCortados < totalSolicitado);
            estadisticas.put("pedidoCompleto", juegosCortados >= totalSolicitado);
            estadisticas.put("faltanJuegos", Math.max(0, totalSolicitado - juegosCortados));

            return ResponseEntity.ok(estadisticas);
        } catch (Exception e) {
            return ResponseEntity.status(500).body("Error al obtener estadísticas: " + e.getMessage());
        }
    }

    // ================================
    // MÉTODOS AUXILIARES
    // ================================

    private String determineImageType(byte[] imageData) {
        if (imageData.length > 4) {
            if (imageData[0] == (byte) 0x89 && imageData[1] == (byte) 0x50 &&
                    imageData[2] == (byte) 0x4E && imageData[3] == (byte) 0x47)
                return "png";
            if (imageData[0] == (byte) 0xFF && imageData[1] == (byte) 0xD8)
                return "jpeg";
            if (imageData[0] == (byte) 0x47 && imageData[1] == (byte) 0x49 &&
                    imageData[2] == (byte) 0x46 && imageData[3] == (byte) 0x38)
                return "gif";
        }
        return "jpeg";
    }

    // 🔥 NUEVO: Cálculo dinámico de salario basado en precios registrados
    private int calcularSalarioDinamico(List<Corte> seleccionados, String actividadUsuario) {
        // Obtener todos los precios para la actividad del usuario
        List<PrecioActividad> preciosActividad = precioActividadRepository
                .findByActividad(actividadUsuario.toLowerCase());

        int totalSalario = 0;

        for (Corte corte : seleccionados) {
            // Para corte, la "descripción" del trabajo sería "Juegos" o "Corte"
            int juegos = Integer.parseInt(corte.getJuegos());

            // Buscar el precio específico para corte/juegos
            Integer precioPorJuego = preciosActividad.stream()
                    .filter(precio -> precio.getDescripcion().equalsIgnoreCase("Juegos") ||
                            precio.getDescripcion().equalsIgnoreCase("Corte"))
                    .findFirst()
                    .map(PrecioActividad::getPrecio)
                    .orElse(getPrecioDefaultCorte()); // Fallback a precio por defecto

            // Calcular: juegos * precio por juego
            totalSalario += juegos * precioPorJuego;
        }

        return totalSalario;
    }

    // 🔥 Precio por defecto para corte si no hay precios registrados
    private Integer getPrecioDefaultCorte() {
        return 200; // Precio por defecto por juego cortado
    }

    private boolean isAuthenticated(HttpSession session) {
        return session.getAttribute("telefono") != null && session.getAttribute("actividad") != null;
    }
}