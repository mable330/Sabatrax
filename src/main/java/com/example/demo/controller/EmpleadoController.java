package com.example.demo.controller;

import com.example.demo.model.Empleado;
import com.example.demo.repository.EmpleadoRepository;
import com.example.demo.repository.CorteRepository;
import com.example.demo.repository.MaquinaRepository;
import com.example.demo.repository.EmpaqueRepository;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

@Controller
@RequestMapping("/empleado")
public class EmpleadoController {

    @Autowired
    private EmpleadoRepository repo;

    @Autowired
    private CorteRepository corteRepo;

    @Autowired
    private EmpaqueRepository empaqueRepo;

    @Autowired
    private MaquinaRepository maquinaRepo;

    @GetMapping
    public String listar(@RequestParam(required = false) String telefono, Model model) {
        model.addAttribute("empleados", repo.findAll()); // Importante que sea "empleados"
        model.addAttribute("empleadoSeleccionado",
                telefono != null ? repo.findById(telefono).orElse(null) : null);
        return "empleado"; // Se espera que exista empleado.html
    }

    @PostMapping
    @Transactional // ✅ Borrado atómico
    public String accionesEmpleado(@RequestParam String accion, @ModelAttribute Empleado empleado) {
        switch (accion) {
            case "modificar", "actualizar" -> repo.save(empleado);
            case "eliminar" -> eliminarEmpleado(empleado.getTelefono());
        }
        return "redirect:/empleado";
    }

    private void eliminarEmpleado(String telefono) {
        // 🔥 Primero eliminamos las entidades dependientes
        corteRepo.deleteByUsuarioTelefono(telefono);
        empaqueRepo.deleteByUsuarioTelefono(telefono);
        maquinaRepo.deleteByUsuarioTelefono(telefono);

        // 🔥 Luego eliminamos el empleado
        repo.deleteById(telefono);
    }
}
