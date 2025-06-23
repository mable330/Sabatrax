package com.example.demo.controller;

import com.example.demo.model.Empleado;
import com.example.demo.repository.EmpleadoRepository;
import com.example.demo.service.CorreoService;
import jakarta.mail.MessagingException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

@Controller
@RequestMapping("/registro-empleado")
public class RegistroEmpleadoController {

    @Autowired
    private EmpleadoRepository repo;

    @Autowired
    private CorreoService correoService;

    @GetMapping
    public String mostrarFormulario(Model model) {
        model.addAttribute("empleado", new Empleado());
        return "registro-empleado";
    }

    @PostMapping
    public String registrarEmpleado(@ModelAttribute("empleado") Empleado empleado, Model model) {
        try {
            // Guardar en base de datos
            repo.save(empleado);

            // Usar el servicio para enviar el correo
            String asunto = "Bienvenido a la empresa, " + empleado.getNombre();
            String mensaje = "Hola <b>" + empleado.getNombre() + "</b>, tu registro fue exitoso. Gracias por unirte.";
            correoService.enviarCorreo(empleado.getCorreo(), asunto, mensaje);

            model.addAttribute("mensajeExito", "Empleado registrado y correo enviado con éxito.");
            return "redirect:/empleado";

        } catch (MessagingException e) {
            System.err.println("❌ Error al enviar correo: " + e.getMessage());
            model.addAttribute("error", "Empleado guardado, pero no se pudo enviar el correo.");
            return "registro-empleado";

        } catch (Exception e) {
            System.err.println("❌ Error general: " + e.getMessage());
            model.addAttribute("error", "No se pudo registrar el empleado.");
            return "registro-empleado";
        }
    }
}
