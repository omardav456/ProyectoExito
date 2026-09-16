package com.exito.stockai.config;

import static org.assertj.core.api.Assertions.assertThat;

import com.exito.stockai.repository.AreaModeloRepository;
import com.exito.stockai.repository.CategoriaModeloRepository;
import com.exito.stockai.repository.SubcategoriaModeloRepository;
import com.exito.stockai.repository.ModeloMatematicoRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@ActiveProfiles("test")
@TestPropertySource(properties = {
    "app.seed.catalog-enabled=true",
    "app.seed.demo-enabled=false"
})
@Transactional
class CatalogSeederTest {

    @Autowired
    private ModeloMatematicoRepository modeloRepository;
    @Autowired
    private AreaModeloRepository areaRepository;
    @Autowired
    private CategoriaModeloRepository categoriaRepository;
    @Autowired
    private SubcategoriaModeloRepository subcategoriaRepository;

    @Test
    void cargaMilModelosDesdeJson() {
        long modelos = modeloRepository.count();
        assertThat(modelos).isEqualTo(1000);
    }

    @Test
    void construyeTaxonomiaDeCatorceAreas() {
        assertThat(areaRepository.count()).isGreaterThanOrEqualTo(14);
        assertThat(categoriaRepository.count()).isPositive();
        assertThat(subcategoriaRepository.count()).isPositive();
    }

    @Test
    void losModelosPertenecenACategorias() {
        var sinRelacion = modeloRepository.findAll().stream()
                .filter(m -> m.getSubcategoria() == null || m.getSubcategoria().getCategoria() == null
                        || m.getSubcategoria().getCategoria().getArea() == null)
                .count();
        assertThat(sinRelacion).isZero();
    }
}