package com.exito.stockai.service.inventario;

import com.exito.stockai.dto.CategoriaRequest;
import com.exito.stockai.dto.CategoriaResponse;
import com.exito.stockai.model.inventario.Categoria;
import com.exito.stockai.repository.CategoriaRepository;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class CategoriaService {

    private final CategoriaRepository repository;

    public CategoriaService(CategoriaRepository repository) {
        this.repository = repository;
    }

    public List<CategoriaResponse> listar() {
        return repository.findAllByOrderByNombreAsc().stream()
                .map(this::toResponse)
                .toList();
    }

    public CategoriaResponse buscarPorId(Long id) {
        return toResponse(repository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Categoría no encontrada: " + id)));
    }

    @Transactional
    public CategoriaResponse crear(CategoriaRequest req) {
        if (req.nombre() == null || req.nombre().isBlank()) {
            throw new IllegalArgumentException("El nombre es obligatorio");
        }
        var categoria = Categoria.builder()
                .nombre(req.nombre())
                .descripcion(req.descripcion())
                .color(req.color())
                .activa(req.activa() != null ? req.activa() : true)
                .build();
        return toResponse(repository.save(categoria));
    }

    @Transactional
    public CategoriaResponse actualizar(Long id, CategoriaRequest req) {
        var categoria = repository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Categoría no encontrada: " + id));
        if (req.nombre() != null) categoria.setNombre(req.nombre());
        if (req.descripcion() != null) categoria.setDescripcion(req.descripcion());
        if (req.color() != null) categoria.setColor(req.color());
        if (req.activa() != null) categoria.setActiva(req.activa());
        return toResponse(repository.save(categoria));
    }

    @Transactional
    public void eliminar(Long id) {
        var categoria = repository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Categoría no encontrada: " + id));
        categoria.setActiva(false);
        repository.save(categoria);
    }

    private CategoriaResponse toResponse(Categoria c) {
        return new CategoriaResponse(c.getId(), c.getNombre(), c.getDescripcion(), c.getColor(), c.getActiva());
    }
}
