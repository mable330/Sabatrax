package com.example.demo.service;

import com.example.demo.model.Maquina;
import com.example.demo.repository.MaquinaRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class MaquinaService {

    private final MaquinaRepository maquinaRepository;

    @Autowired
    public MaquinaService(MaquinaRepository maquinaRepository) {
        this.maquinaRepository = maquinaRepository;
    }

    /**
     * Obtiene actividades por sus IDs
     * 
     * @param ids Lista de IDs de actividades
     * @return Lista de entidades Maquina
     */
    public List<Maquina> obtenerActividadesPorIds(List<String> ids) {
        return maquinaRepository.findAllById(ids);
    }

    /**
     * Método adicional para obtener todas las actividades
     * 
     * @return Lista completa de actividades
     */
    public List<Maquina> obtenerTodasLasActividades() {
        return maquinaRepository.findAll();
    }

    /**
     * Método para guardar/actualizar una actividad
     * 
     * @param maquina Entidad a guardar
     * @return Entidad guardada
     */
    public Maquina guardarActividad(Maquina maquina) {
        return maquinaRepository.save(maquina);
    }

    /**
     * Método para eliminar una actividad
     * 
     * @param id ID de la actividad a eliminar
     */
    public void eliminarActividad(String id) {
        maquinaRepository.deleteById(id);
    }
}
