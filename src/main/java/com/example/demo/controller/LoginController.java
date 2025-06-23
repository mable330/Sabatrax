package com.example.demo.controller;

import com.example.demo.model.Usuario;
import com.example.demo.repository.UsuarioRepository;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
public class LoginController {

    @Autowired
    private UsuarioRepository usuarioRepository;

    @GetMapping("/login")
    public String showLoginForm() {
        return "login";
    }

    // Logout mejorado
    @GetMapping("/logout")
    public String logout(HttpSession session) {
        session.invalidate();
        return "redirect:/login?mensaje=sesion_cerrada";
    }

    // Mantener compatibilidad con la ruta /lista existente
    @RequestMapping("/lista")
    public String mostrarRegistroMaquina(HttpSession session) {
        // Verificar autenticación
        if (!isAuthenticated(session)) {
            return "redirect:/login";
        }

        // Verificar que sea empleado de máquina
        String actividad = (String) session.getAttribute("actividad");
        if (!"Maquina".equalsIgnoreCase(actividad)) {
            return "redirect:/login?error=acceso_denegado";
        }

        return "lista";
    }

    @PostMapping("/login")
    public String login(
            @RequestParam String telefono,
            @RequestParam String password,
            HttpSession session,
            Model model) {

        // Validación básica
        if (telefono == null || telefono.trim().isEmpty() ||
                password == null || password.trim().isEmpty()) {
            model.addAttribute("error", "Teléfono y contraseña son obligatorios.");
            return "login";
        }

        try {
            // Buscar usuario por teléfono y contraseña
            Usuario usuario = usuarioRepository.findByTelefonoAndClave(telefono.trim(), password);

            if (usuario == null) {
                model.addAttribute("error", "Número de teléfono o contraseña incorrectos.");
                return "login";
            }

            // Guardar información completa en sesión
            session.setAttribute("usuarioId", usuario.getTelefono()); // Usar teléfono como ID
            session.setAttribute("telefono", usuario.getTelefono());
            session.setAttribute("nombre", usuario.getNombre());
            session.setAttribute("apellido", usuario.getApellido());
            session.setAttribute("rol", usuario.getRol());
            session.setAttribute("actividad", usuario.getActividad());
            session.setAttribute("authenticated", true);

            // Redirección según rol y actividad
            return redirectToUserArea(usuario);

        } catch (Exception e) {
            model.addAttribute("error", "Error en el sistema. Intente nuevamente.");
            return "login";
        }
    }

    // Método para determinar redirección según rol
    private String redirectToUserArea(Usuario usuario) {
        String rol = usuario.getRol().toUpperCase();

        switch (rol) {
            case "ADMINISTRADOR":
                return "redirect:/admin";

            case "EMPLEADO":
                String actividad = usuario.getActividad();
                if (actividad != null) {
                    switch (actividad.toLowerCase()) {
                        case "maquina":
                            return "redirect:/lista";
                        case "corte":
                            return "redirect:/corte";
                        case "empaque":
                            return "redirect:/empaque"; // Ruta correcta para empaque
                        default:
                            return "redirect:/empleado/dashboard";
                    }
                }
                return "redirect:/empleado/dashboard";

            default:
                return "redirect:/login?error=rol_no_reconocido";
        }
    }

    @GetMapping("/nuevo")
    public String mostrarFormulario(HttpSession session, Model model) {
        // Verificar autenticación
        if (!isAuthenticated(session)) {
            return "redirect:/login";
        }

        String nombre = (String) session.getAttribute("nombre");
        String actividad = (String) session.getAttribute("actividad");

        model.addAttribute("nombreEmpleado", nombre);
        model.addAttribute("actividad", actividad);

        return "nuevo-maquina"; // Create this template
    }

    // Rutas adicionales para otras actividades (preparadas para el futuro)

    @PostMapping("/admin")
    public String adminPanel(HttpSession session, Model model) {
        if (!isAuthenticated(session)) {
            return "redirect:/login";
        }

        if (!"ADMINISTRADOR".equalsIgnoreCase((String) session.getAttribute("rol"))) {
            return "redirect:/login?error=acceso_denegado";
        }

        model.addAttribute("nombreAdmin", session.getAttribute("nombre"));
        return "admin/panel"; // templates/admin/panel.html
    }

    @GetMapping("/empleado/dashboard")
    public String empleadoDashboard(HttpSession session, Model model) {
        if (!isAuthenticated(session)) {
            return "redirect:/login";
        }

        model.addAttribute("nombreEmpleado", session.getAttribute("nombre"));
        model.addAttribute("actividad", session.getAttribute("actividad"));
        return "empleado/dashboard";
    }

    // Ruta para la página principal
    @GetMapping("/")
    public String index(HttpSession session) {
        if (isAuthenticated(session)) {
            String rol = (String) session.getAttribute("rol");
            if ("ADMINISTRADOR".equalsIgnoreCase(rol)) {
                return "redirect:/admin";
            } else {
                String actividad = (String) session.getAttribute("actividad");
                if (actividad != null) {
                    switch (actividad.toLowerCase()) {
                        case "maquina":
                            return "redirect:/lista";
                        case "corte":
                            return "redirect:/corte";
                        case "empaque":
                            return "redirect:/empaque";
                        default:
                            return "redirect:/empleado/dashboard";
                    }
                }
                return "redirect:/empleado/dashboard";
            }
        }
        return "redirect:/login";
    }

    // Método auxiliar para verificar autenticación
    private boolean isAuthenticated(HttpSession session) {
        Boolean authenticated = (Boolean) session.getAttribute("authenticated");
        String telefono = (String) session.getAttribute("telefono");
        return authenticated != null && authenticated && telefono != null;
    }
}