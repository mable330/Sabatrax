package com.example.demo.controller;

import com.example.demo.model.Maquina;
import com.example.demo.model.Usuario;
import com.example.demo.model.Pedido;
import com.example.demo.model.PrecioActividad;
import com.example.demo.repository.MaquinaRepository;
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
import java.time.ZoneId;
import java.util.Date;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Controller
@RequestMapping("/lista")
public class MaquinaController {

    @Autowired
    private MaquinaRepository repository;

    @Autowired
    private UsuarioRepository usuarioRepository;

    @Autowired
    private PedidoRepository pedidoRepository;

    @Autowired
    private PrecioActividadRepository precioActividadRepository;

    // ✅ Mostrar formulario de registro
    @GetMapping("/registroM")
    public String mostrarFormularioRegistro(Model model, HttpSession session) {
        if (!isAuthenticated(session)) {
            return "redirect:/login";
        }

        String telefono = (String) session.getAttribute("telefono");
        Usuario usuario = usuarioRepository.findByTelefono(telefono);
        if (usuario == null) {
            return "redirect:/login";
        }

        model.addAttribute("registroActividad", new Maquina());
        model.addAttribute("Ntelefono", telefono);
        model.addAttribute("nombreEmpleado", usuario.getNombre());
        model.addAttribute("totalRegistros", repository.findByUsuario(usuario).size());

        return "registroM";
    }

    // ✅ Guardar actividad - VALIDACIÓN CORREGIDA PARA PROCESO COMPARTIDO
    @PostMapping("/registroM")
    @Transactional
    public String guardarDesdeFormulario(
            @ModelAttribute("registroActividad") Maquina maquina,
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
            String medidas = maquina.getMedidas();
            String tipoSabana = maquina.getTipoSabanas();
            int cantidadNueva = maquina.getCantidad();
            LocalDate fechaActual = LocalDate.now();

            // 🔥 Buscar el pedido más reciente para la medida
            Pedido pedidoActual = pedidoRepository.findByMedidasSabanasAndFechaEnvioAfter(medidas, fechaActual)
                    .stream()
                    .findFirst()
                    .orElse(null);

            if (pedidoActual == null) {
                model.addAttribute("error", "🚨 No hay pedidos activos para la medida '" + medidas + "'.");
                return "registroM";
            }
            // 🔥 Obtener piezas actuales por pedido
            int planasActuales = repository.contarPorPedidoYTipo(pedidoActual.getId(), "Plana");
            int cauchosActuales = repository.contarPorPedidoYTipo(pedidoActual.getId(), "Caucho");
            int fundasActuales = repository.contarPorPedidoYTipo(pedidoActual.getId(), "Fundas");

            int limitePlanas = pedidoActual.getJuegos();
            int limiteCauchos = pedidoActual.getJuegos();
            int limiteFundas = pedidoActual.getJuegos() * 2;

            // Validar límites por tipo de pieza
            switch (tipoSabana) {
                case "Plana" -> {
                    if (planasActuales + cantidadNueva > limitePlanas) {
                        int maxPermitido = Math.max(0, limitePlanas - planasActuales);
                        model.addAttribute("error",
                                "🚨 Exceso de planas para " + medidas +
                                        ". Máximo permitido: " + maxPermitido +
                                        " (Ya tienes: " + planasActuales + "/" + limitePlanas + ")");
                        return "registroM";
                    }
                }
                case "Caucho" -> {
                    if (cauchosActuales + cantidadNueva > limiteCauchos) {
                        int maxPermitido = Math.max(0, limiteCauchos - cauchosActuales);
                        model.addAttribute("error",
                                "🚨 Exceso de cauchos para " + medidas +
                                        ". Máximo permitido: " + maxPermitido +
                                        " (Ya tienes: " + cauchosActuales + "/" + limiteCauchos + ")");
                        return "registroM";
                    }
                }
                case "Fundas" -> {
                    if (fundasActuales + cantidadNueva > limiteFundas) {
                        int maxPermitido = Math.max(0, limiteFundas - fundasActuales);
                        model.addAttribute("error",
                                "🚨 Exceso de fundas para " + medidas +
                                        ". Máximo permitido: " + maxPermitido +
                                        " (Ya tienes: " + fundasActuales + "/" + limiteFundas + ")");
                        return "registroM";
                    }
                }
                default -> {
                    model.addAttribute("error", "🚨 Tipo de sábana no válido: " + tipoSabana);
                    return "registroM";
                }
            }

            // 🔥 Guardar la actividad
            maquina.setUsuario(usuario);
            maquina.setId(UUID.randomUUID().toString());
            maquina.setFecha(new Date());
            maquina.setPedido(pedidoActual); // ✔️ Aquí asignas el pedido

            if (imagen != null && !imagen.isEmpty()) {
                maquina.setImagen(imagen.getBytes());
            }
            repository.save(maquina);

            // 🔥 MENSAJE DE ÉXITO CON PROGRESO
            int nuevasPlanas = planasActuales + (tipoSabana.equals("Plana") ? cantidadNueva : 0);
            int nuevosCauchos = cauchosActuales + (tipoSabana.equals("Caucho") ? cantidadNueva : 0);
            int nuevasFundas = fundasActuales + (tipoSabana.equals("Fundas") ? cantidadNueva : 0);

            int juegosCompletosEnMaquina = Math.min(nuevasPlanas, Math.min(nuevosCauchos, nuevasFundas / 2));

            model.addAttribute("mensaje",
                    "✅ Registro exitoso: " + cantidadNueva + " " + tipoSabana.toLowerCase() + "(s) para " + medidas +
                            "\n📊 Progreso en máquina: " + juegosCompletosEnMaquina + "/" + pedidoActual.getJuegos()
                            + " juegos" +
                            "\n📋 Estado: Planas(" + nuevasPlanas + "/" + limitePlanas +
                            "), Cauchos(" + nuevosCauchos + "/" + limiteCauchos +
                            "), Fundas(" + nuevasFundas + "/" + limiteFundas + ")");

            return "redirect:/lista?exito";

        } catch (Exception e) {
            model.addAttribute("error", "Error al guardar: " + e.getMessage());
            return "registroM";
        }
    }

    // ✅ Mostrar la lista
    @GetMapping
    public String mostrarLista(Model model, HttpSession session) {
        if (!isAuthenticated(session))
            return "redirect:/login";

        String telefono = (String) session.getAttribute("telefono");
        Usuario usuario = usuarioRepository.findById(telefono).orElse(null);
        if (usuario == null)
            return "redirect:/login";

        List<Maquina> maquinas = repository.findByUsuario(usuario);

        model.addAttribute("maquinas", maquinas);
        model.addAttribute("Ntelefono", telefono);
        model.addAttribute("actividad", session.getAttribute("actividad"));
        model.addAttribute("nombreEmpleado", usuario.getNombre());
        model.addAttribute("mensajeBienvenida", "Bienvenido a " + usuario.getActividad());
        model.addAttribute("totalRegistros", maquinas.size());

        return "lista";
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

        List<Maquina> todas = repository.findByUsuario(usuario);
        List<Maquina> seleccionados = todas.stream()
                .filter(m -> ids.contains(m.getId()))
                .collect(Collectors.toList());

        // 🔥 NUEVO: Cálculo dinámico basado en precios registrados
        int total = calcularSalarioDinamico(seleccionados, usuario.getActividad());

        model.addAttribute("total", total);
        model.addAttribute("maquinas", todas);
        model.addAttribute("Ntelefono", telefono);
        model.addAttribute("nombreEmpleado", usuario.getNombre());
        model.addAttribute("actividad", usuario.getActividad());
        model.addAttribute("mensajeBienvenida", "Bienvenido a " + usuario.getActividad());
        model.addAttribute("totalRegistros", todas.size());
        model.addAttribute("registrosSeleccionados", seleccionados.size());

        return "lista";
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

        List<Maquina> registros = repository.findAllById(ids);
        List<String> idsValidos = registros.stream()
                .filter(m -> m.getUsuario().getTelefono().equals(telefono))
                .map(Maquina::getId)
                .toList();

        if (!idsValidos.isEmpty()) {
            repository.deleteAllById(idsValidos);
        }

        return "redirect:/lista";
    }

    // ✅ Mostrar imagen
    @GetMapping("/imagen/{id}")
    @ResponseBody
    public ResponseEntity<byte[]> verImagen(@PathVariable String id, HttpSession session) {
        if (!isAuthenticated(session)) {
            return ResponseEntity.status(401).build();
        }

        String telefonoUsuario = (String) session.getAttribute("telefono");
        Maquina maquina = repository.findById(id).orElse(null);

        if (maquina == null || maquina.getImagen() == null ||
                !maquina.getUsuario().getTelefono().equals(telefonoUsuario)) {
            return ResponseEntity.notFound().build();
        }

        MediaType contentType = MediaType.IMAGE_JPEG;
        try {
            String fileType = determineImageType(maquina.getImagen());
            if ("png".equalsIgnoreCase(fileType))
                contentType = MediaType.IMAGE_PNG;
            else if ("gif".equalsIgnoreCase(fileType))
                contentType = MediaType.IMAGE_GIF;
        } catch (Exception ignored) {
        }

        return ResponseEntity.ok()
                .contentType(contentType)
                .body(maquina.getImagen());
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

            int planas = repository.contarPorMedidaYTipoDesde(medidas, "Plana", fechaInicio);
            int cauchos = repository.contarPorMedidaYTipoDesde(medidas, "Caucho", fechaInicio);
            int fundas = repository.contarPorMedidaYTipoDesde(medidas, "Fundas", fechaInicio);
            int juegosCompletos = Math.min(planas, Math.min(cauchos, fundas / 2));

            List<Pedido> pedidosActivos = pedidoRepository.findByMedidasSabanasAndFechaEnvioAfter(medidas, fechaActual);
            int totalSolicitado = pedidosActivos.stream().mapToInt(Pedido::getJuegos).sum();

            int planasNecesarias = totalSolicitado;
            int cauchosNecesarios = totalSolicitado;
            int fundasNecesarias = totalSolicitado * 2;

            var estadisticas = new java.util.HashMap<String, Object>();
            estadisticas.put("medidas", medidas);
            estadisticas.put("planasProducidas", planas);
            estadisticas.put("cauchosProducidos", cauchos);
            estadisticas.put("fundasProducidas", fundas);
            estadisticas.put("planasNecesarias", planasNecesarias);
            estadisticas.put("cauchosNecesarios", cauchosNecesarios);
            estadisticas.put("fundasNecesarias", fundasNecesarias);
            estadisticas.put("juegosCompletos", juegosCompletos);
            estadisticas.put("totalSolicitado", totalSolicitado);
            estadisticas.put("porcentajeCompletado",
                    totalSolicitado > 0 ? (juegosCompletos * 100.0 / totalSolicitado) : 0);
            estadisticas.put("puedeProducirMas", juegosCompletos < totalSolicitado);
            estadisticas.put("pedidoCompleto", juegosCompletos >= totalSolicitado);
            estadisticas.put("faltanJuegos", Math.max(0, totalSolicitado - juegosCompletos));

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
    private int calcularSalarioDinamico(List<Maquina> seleccionados, String actividadUsuario) {
        // Obtener todos los precios para la actividad del usuario
        List<PrecioActividad> preciosActividad = precioActividadRepository
                .findByActividad(actividadUsuario.toLowerCase());

        int totalSalario = 0;

        for (Maquina maquina : seleccionados) {
            String tipoSabana = maquina.getTipoSabanas();
            int cantidad = maquina.getCantidad();

            // Buscar el precio específico para este tipo de sábana
            Integer precioPorUnidad = preciosActividad.stream()
                    .filter(precio -> precio.getDescripcion().equalsIgnoreCase(tipoSabana))
                    .findFirst()
                    .map(PrecioActividad::getPrecio)
                    .orElse(getPrecioDefault(tipoSabana)); // Fallback a precios por defecto

            // Calcular: cantidad * precio por unidad
            totalSalario += cantidad * precioPorUnidad;
        }

        return totalSalario;
    }

    // 🔥 Precios por defecto si no hay precios registrados
    private Integer getPrecioDefault(String tipoSabana) {
        return switch (tipoSabana) {
            case "Plana", "Caucho" -> 400;
            case "Fundas" -> 50;
            case "Cortinas" -> 1200;
            default -> 0;
        };
    }

    // ⚠️ MÉTODO OBSOLETO: Mantener por compatibilidad pero ya no se usa

    private boolean isAuthenticated(HttpSession session) {
        return session.getAttribute("telefono") != null && session.getAttribute("actividad") != null;
    }
}