package com.example.demo.controller;

import com.example.demo.model.RegistroActividad;
import com.example.demo.repository.RegistroActividadRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.time.LocalDate;

@Controller
public class RegistroActividadController {

    @Autowired
    private RegistroActividadRepository repo;

    @GetMapping("/registroM")
    public String mostrarFormulario(Model model) {
        model.addAttribute("registroActividad", new RegistroActividad());
        return "registroM";
    }

    @PostMapping("/registroM")
    public String procesarFormulario(
            @ModelAttribute("registroActividad") RegistroActividad registroActividad,
            @RequestParam("imagenFile") MultipartFile imagenFile, // Cambiado a imagenFile
            BindingResult result,
            Model model) {

        try {
            // Verifica si se cargó una imagen
            if (!imagenFile.isEmpty()) {
                registroActividad.setImagen(imagenFile.getBytes());
            } else {
                model.addAttribute("error", "Debe subir una imagen");
                return "registroM";
            }

            // Validar otros campos si es necesario
            if (result.hasErrors()) {
                return "registroM";
            }

            // Guardar el registro
            repo.save(registroActividad);
            return "redirect:/lista?exito";

        } catch (IOException e) {
            model.addAttribute("error", "Error al procesar la imagen: " + e.getMessage());
            return "registroM";
        } catch (Exception e) {
            model.addAttribute("error", "Error al guardar: " + e.getMessage());
            return "registroM";
        }
    }
}