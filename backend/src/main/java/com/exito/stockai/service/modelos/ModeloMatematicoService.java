package com.exito.stockai.service.modelos;

import com.exito.stockai.dto.ModeloDetailResponse;
import com.exito.stockai.dto.ModeloDetailResponse.ParametroDto;
import com.exito.stockai.dto.ModeloDetailResponse.VariableDto;
import com.exito.stockai.dto.ModeloListResponse;
import com.exito.stockai.dto.ModeloListResponse.ModeloResumen;
import com.exito.stockai.dto.TaxonomiaResponse;
import com.exito.stockai.dto.TaxonomiaResponse.AreaDto;
import com.exito.stockai.dto.TaxonomiaResponse.CategoriaDto;
import com.exito.stockai.dto.TaxonomiaResponse.SubcatDto;
import com.exito.stockai.model.modelos.ModeloMatematico;
import com.exito.stockai.model.modelos.enums.ComplejidadModelo;
import com.exito.stockai.model.modelos.enums.EstadoModelo;
import com.exito.stockai.repository.AplicacionModeloRepository;
import com.exito.stockai.repository.AreaModeloRepository;
import com.exito.stockai.repository.CategoriaModeloRepository;
import com.exito.stockai.repository.MetodoNumericoRepository;
import com.exito.stockai.repository.ModeloMatematicoRepository;
import com.exito.stockai.repository.ParametroModeloRepository;
import com.exito.stockai.repository.SubcategoriaModeloRepository;
import com.exito.stockai.repository.VariableModeloRepository;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class ModeloMatematicoService {

    private final ModeloMatematicoRepository modeloRepository;
    private final VariableModeloRepository variableRepository;
    private final ParametroModeloRepository parametroRepository;
    private final AplicacionModeloRepository aplicacionRepository;
    private final MetodoNumericoRepository metodoRepository;
    private final AreaModeloRepository areaRepository;
    private final CategoriaModeloRepository categoriaModeloRepository;
    private final SubcategoriaModeloRepository subcategoriaRepository;

    public ModeloMatematicoService(ModeloMatematicoRepository modeloRepository,
                                   VariableModeloRepository variableRepository,
                                   ParametroModeloRepository parametroRepository,
                                   AplicacionModeloRepository aplicacionRepository,
                                   MetodoNumericoRepository metodoRepository,
                                   AreaModeloRepository areaRepository,
                                   CategoriaModeloRepository categoriaModeloRepository,
                                   SubcategoriaModeloRepository subcategoriaRepository) {
        this.modeloRepository = modeloRepository;
        this.variableRepository = variableRepository;
        this.parametroRepository = parametroRepository;
        this.aplicacionRepository = aplicacionRepository;
        this.metodoRepository = metodoRepository;
        this.areaRepository = areaRepository;
        this.categoriaModeloRepository = categoriaModeloRepository;
        this.subcategoriaRepository = subcategoriaRepository;
    }

    public ModeloListResponse buscar(Long areaId, Long categoriaId, Long subcategoriaId,
                                     String tipo, String complejidad, String estado,
                                     String q, int page, int size) {
        ComplejidadModelo complejidadEnum = complejidad != null ? ComplejidadModelo.valueOf(complejidad) : null;
        EstadoModelo estadoEnum = estado != null ? EstadoModelo.valueOf(estado) : null;

        var pageResult = modeloRepository.buscar(
                areaId, categoriaId, subcategoriaId, tipo,
                complejidadEnum, estadoEnum, q,
                PageRequest.of(page, size));

        var modelos = pageResult.getContent().stream()
                .map(this::toResumen)
                .toList();

        long total = modeloRepository.contar(
                areaId, categoriaId, subcategoriaId, tipo,
                complejidadEnum, estadoEnum, q);

        return new ModeloListResponse(total, page, size, modelos);
    }

    public ModeloDetailResponse buscarPorId(Long id) {
        var modelo = modeloRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Modelo no encontrado: " + id));
        return toDetail(modelo);
    }

    public ModeloDetailResponse buscarPorCodigo(String codigo) {
        var modelo = modeloRepository.findByCodigo(codigo)
                .orElseThrow(() -> new IllegalArgumentException("Modelo no encontrado: " + codigo));
        return toDetail(modelo);
    }

    public TaxonomiaResponse taxonomia() {
        var areas = areaRepository.findAllByOrderByNombreAsc();
        var areaDtos = areas.stream().map(area -> {
            var cats = categoriaModeloRepository.findByAreaIdOrderByNombreAsc(area.getId());
            var catDtos = cats.stream().map(cat -> {
                var subcats = subcategoriaRepository.findByCategoriaIdOrderByNombreAsc(cat.getId());
                var subcatDtos = subcats.stream()
                        .map(s -> new SubcatDto(s.getId(), s.getNombre(), s.getDescripcion()))
                        .toList();
                return new CategoriaDto(cat.getId(), cat.getNombre(), cat.getDescripcion(), subcatDtos);
            }).toList();
            return new AreaDto(area.getId(), area.getNombre(), area.getDescripcion(), catDtos);
        }).toList();
        return new TaxonomiaResponse(areaDtos);
    }

    public Map<String, Object> estadisticas() {
        var stats = new LinkedHashMap<String, Object>();
        stats.put("totalModelos", modeloRepository.count());
        stats.put("porTipo", toMap(modeloRepository.contarPorTipo()));
        stats.put("porEstado", toMap(modeloRepository.contarPorEstado()));
        return stats;
    }

    private Map<String, Long> toMap(List<Object[]> rows) {
        var map = new LinkedHashMap<String, Long>();
        for (var row : rows) {
            var key = row[0] != null ? row[0].toString() : "N/A";
            map.put(key, (Long) row[1]);
        }
        return map;
    }

    private ModeloResumen toResumen(ModeloMatematico m) {
        return new ModeloResumen(
                m.getId(),
                m.getCodigo(),
                m.getNombre(),
                m.getTipoModelo(),
                m.getMetodoSugerido(),
                m.getComplejidad() != null ? m.getComplejidad().name() : null,
                m.getEstado() != null ? m.getEstado().name() : null,
                m.getSubcategoria().getCategoria().getArea().getNombre(),
                m.getSubcategoria().getCategoria().getNombre(),
                m.getSubcategoria().getNombre()
        );
    }

    private ModeloDetailResponse toDetail(ModeloMatematico m) {
        var variables = variableRepository.findByModeloIdOrderByNombreAsc(m.getId()).stream()
                .map(v -> new VariableDto(v.getId(), v.getNombre(), v.getSimbolo(),
                        v.getRol() != null ? v.getRol().name() : null,
                        v.getTipoDato(), v.getUnidad(), v.getDescripcion()))
                .toList();

        var parametros = parametroRepository.findByModeloIdOrderByNombreAsc(m.getId()).stream()
                .map(p -> new ParametroDto(p.getId(), p.getNombre(), p.getSimbolo(),
                        p.getValorPorDefecto(), p.getUnidad(), p.getDescripcion()))
                .toList();

        var metodos = m.getMetodos() != null
                ? m.getMetodos().stream().map(mt -> mt.getNombre()).toList()
                : List.<String>of();

        var aplicaciones = aplicacionRepository.findByModeloIdOrderByDescripcionAsc(m.getId()).stream()
                .map(a -> a.getDescripcion())
                .toList();

        return new ModeloDetailResponse(
                m.getId(),
                m.getCodigo(),
                m.getNombre(),
                m.getDescripcion(),
                m.getProblema(),
                m.getTipoModelo(),
                m.getMetodoSugerido(),
                m.getComplejidad() != null ? m.getComplejidad().name() : null,
                m.getEntradaEsperada(),
                m.getSalidaEsperada(),
                m.getEstado() != null ? m.getEstado().name() : null,
                m.getMotorImpl(),
                m.getDocumentacion(),
                m.getSubcategoria().getCategoria().getArea().getNombre(),
                m.getSubcategoria().getCategoria().getNombre(),
                m.getSubcategoria().getNombre(),
                variables,
                parametros,
                metodos,
                aplicaciones
        );
    }
}
