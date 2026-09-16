package com.exito.stockai.controller;

import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.exito.stockai.repository.ModeloMatematicoRepository;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@TestPropertySource(properties = {
        "app.security.enabled=true",
        "app.seed.catalog-enabled=true",
        "app.seed.demo-enabled=true"
})
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class ModeloNuevoMotorEjecucionTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private ModeloMatematicoRepository modeloRepository;

    private String tokenAdmin;
    private final Map<String, Long> idsPorNombre = new HashMap<>();

    @BeforeAll
    void preparar() throws Exception {
        tokenAdmin = login("admin@exito.co", "admin123");
        String resp = mockMvc.perform(get("/api/modelos?estado=IMPLEMENTADO&size=200")
                        .header("Authorization", tokenAdmin))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.total", greaterThanOrEqualTo(100)))
                .andReturn().getResponse().getContentAsString();
        JsonNode modelos = objectMapper.readTree(resp).get("modelos");
        List<String> objetivos = List.of(
                "Media Movil Simple (SMA)",
                "EOQ con Descuentos por Volumen",
                "Cola M/M/1",
                "VRP Con Flota Homogenea",
                "Margen Bruto por Categoria",
                "VaR Value at Risk",
                "Curva de Lorenz de Inventario");
        for (JsonNode m : modelos) {
            String nombre = m.get("nombre").asText();
            if (objetivos.contains(nombre)) {
                idsPorNombre.put(nombre, m.get("id").asLong());
            }
        }
    }

    private String login(String email, String password) throws Exception {
        String body = "{\"email\":\"%s\",\"password\":\"%s\"}".formatted(email, password);
        String resp = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        return "Bearer " + objectMapper.readTree(resp).get("token").asText();
    }

    private void ejecutarOk(String nombre, String motorEsperado) throws Exception {
        Long id = idsPorNombre.get(nombre);
        if (id == null) {
            org.junit.jupiter.api.Assertions.fail("No se encontró el modelo implementado: " + nombre);
        }
        mockMvc.perform(post("/api/modelos/{id}/ejecutar", id)
                        .header("Authorization", tokenAdmin)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"parametros\":{},\"fuente\":\"SIMULADA\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.estado").value("OK"))
                .andExpect(jsonPath("$.motor").value(motorEsperado))
                .andExpect(jsonPath("$.resultado").isNotEmpty());
    }

    @Test
    void seRegistranAlMenos100ModelosImplementados() {
        long impl = modeloRepository.findAll().stream()
                .filter(m -> "IMPLEMENTADO".equals(m.getEstado().name()))
                .count();
        org.junit.jupiter.api.Assertions.assertTrue(impl >= 100, "Modelos IMPLEMENTADO < 100: " + impl);
    }

    @Test
    void unModeloPorFamiliaEjecutaConMotorOk() throws Exception {
        ejecutarOk("Media Movil Simple (SMA)", "SMA");
        ejecutarOk("EOQ con Descuentos por Volumen", "EOQ_DESCUENTOS");
        ejecutarOk("Cola M/M/1", "MM1");
        ejecutarOk("VRP Con Flota Homogenea", "VRP");
        ejecutarOk("Margen Bruto por Categoria", "MARGEN");
        ejecutarOk("VaR Value at Risk", "VAR");
        ejecutarOk("Curva de Lorenz de Inventario", "LORENZ");
    }

    @Test
    void modeloPropuestoSigueDevuelve409() throws Exception {
        mockMvc.perform(get("/api/modelos?estado=PROPUESTO&size=1")
                        .header("Authorization", tokenAdmin))
                .andExpect(status().isOk());
        var propuesto = modeloRepository.findAll().stream()
                .filter(m -> "PROPUESTO".equals(m.getEstado().name()))
                .findFirst().orElse(null);
        if (propuesto == null) {
            return;
        }
        mockMvc.perform(post("/api/modelos/{id}/ejecutar", propuesto.getId())
                        .header("Authorization", tokenAdmin)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"parametros\":{},\"fuente\":\"SIMULADA\"}"))
                .andExpect(status().isConflict());
    }

    @Test
    void pronosticoConFuenteRealInyectaSerie() throws Exception {
        Long id = idsPorNombre.get("Media Movil Simple (SMA)");
        if (id == null) {
            return;
        }
        mockMvc.perform(post("/api/modelos/{id}/ejecutar", id)
                        .header("Authorization", tokenAdmin)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"parametros\":{},\"fuente\":\"REAL\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.estado").value("OK"))
                .andExpect(jsonPath("$.parametros.serie").isArray());
    }

    @Test
    void estadisticasReportan100Implementados() throws Exception {
        mockMvc.perform(get("/api/modelos/estadisticas")
                        .header("Authorization", tokenAdmin))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.porEstado.IMPLEMENTADO").value(100));
    }
}