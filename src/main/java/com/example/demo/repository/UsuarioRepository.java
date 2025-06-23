package com.example.demo.repository;

import com.example.demo.model.Usuario;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface UsuarioRepository extends JpaRepository<Usuario, String> {

    Usuario findByTelefonoAndClave(String telefono, String clave);

    // ✅ Este es el método que falta:
    List<Usuario> findByRol(String rol);

    Usuario findByTelefono(String telefono);

}
