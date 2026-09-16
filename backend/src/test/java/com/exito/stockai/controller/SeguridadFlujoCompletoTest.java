package com.exito.stockai.controller;

import static org.hamcrest.Matchers.greaterThan;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.exito.stockai.model.modelos.ModeloMatematico;
import com.exito.stockai.repository.ModeloMatematicoRepository;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import java.util.Locale;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
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
        "app.seed.demo-enabled=true",
        "app.seed.users-enabled=true"
})
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class SeguridadFlujoCompletoTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private ModeloMatematicoRepository modeloRepository;

    private String tokenAdmin;
    private String tokenEmpleado;
    private String tokenCliente;

    @BeforeAll
    void autenticar() throws Exception {
        tokenAdmin = login("admin@exito.co", "admin123");
        tokenEmpleado = login("empleado@exito.co", "empleado123");
        tokenCliente = login("cliente@exito.co", "cliente123");
    }

    private String login(String email, String password) throws Exception {
        String body = "{\"email\":\"%s\",\"password\":\"%s\"}".formatted(email, password);
        String resp = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").isNotEmpty())
                .andReturn().getResponse().getContentAsString();
        return "Bearer " + objectMapper.readTree(resp).get("token").asText();
    }

    @Test
    void loginConCredencialesInvalidasDevuelve401() throws Exception {
        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"admin@exito.co\",\"password\":\"mala\"}"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void endpointProtegidoSinTokenDevuelve401() throws Exception {
        mockMvc.perform(get("/api/movimientos"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void catalogoDeProductosEsPublicoSinToken() throws Exception {
        mockMvc.perform(get("/api/productos"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray());
    }

    @Test
    void empleadoPendienteNoPuedeIngresarHastaQueAdminLoAprobe() throws Exception {
        String crear = """
                {"email":"nuevo.empleado@exito.co","password":"nuevo123","nombre":"Nuevo Empleado","rol":"EMPLEADO","activo":false}
                """;
        String resp = mockMvc.perform(post("/api/usuarios")
                        .header("Authorization", tokenAdmin)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(crear))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.activo").value(false))
                .andReturn().getResponse().getContentAsString();
        long nuevoId = objectMapper.readTree(resp).path("id").asLong();

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"nuevo.empleado@exito.co\",\"password\":\"nuevo123\"}"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.detalles[0]").value(org.hamcrest.Matchers.containsString("aprobación")));

        String actualizar = """
                {"email":"nuevo.empleado@exito.co","password":"","nombre":"Nuevo Empleado","rol":"EMPLEADO","activo":true}
                """;
        mockMvc.perform(put("/api/usuarios/" + nuevoId)
                        .header("Authorization", tokenAdmin)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(actualizar))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.activo").value(true));

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"nuevo.empleado@exito.co\",\"password\":\"nuevo123\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").isNotEmpty());
    }

    @Test
    void endpointDeModelosSoloAdministrador() throws Exception {
        mockMvc.perform(get("/api/modelos/estadisticas").header("Authorization", tokenAdmin))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/modelos/estadisticas").header("Authorization", tokenEmpleado))
                .andExpect(status().isForbidden());
    }

    @Test
    void auditoriaSoloAdministrador() throws Exception {
        mockMvc.perform(get("/api/auditoria").header("Authorization", tokenEmpleado))
                .andExpect(status().isForbidden());

        mockMvc.perform(get("/api/auditoria").header("Authorization", tokenAdmin))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray());
    }

    @Test
    void compraSimuladaReduceStockYUditoria() throws Exception {
        String productos = mockMvc.perform(get("/api/productos")
                        .header("Authorization", tokenEmpleado))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        JsonNode prod = objectMapper.readTree(productos);
        JsonNode conStock = null;
        for (JsonNode p : prod) {
            if (conStock == null || p.path("stockActual").asInt() > conStock.path("stockActual").asInt()) {
                conStock = p;
            }
        }
        if (conStock == null) {
            throw new IllegalStateException("No hay productos para la compra simulada");
        }
        long productoId = conStock.path("id").asLong();
        int stockAntes = conStock.path("stockActual").asInt();

        String body = "{\"items\":[{\"productoId\":%d,\"cantidad\":2}]}".formatted(productoId);
        mockMvc.perform(post("/api/compras")
                        .header("Authorization", tokenEmpleado)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.total").isNumber())
                .andExpect(jsonPath("$.estado").value("REALIZADA"));

        mockMvc.perform(get("/api/productos/" + productoId).header("Authorization", tokenAdmin))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.stockActual").value(stockAntes - 2));

        mockMvc.perform(get("/api/auditoria").header("Authorization", tokenAdmin))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[?(@.operacion == 'COMPRA_SIMULADA' && @.productoId == %d)]".formatted(productoId)).isNotEmpty());
    }

    @Test
    void compraConStockInsuficienteDevuelve400() throws Exception {
        String productos = mockMvc.perform(get("/api/productos")
                        .header("Authorization", tokenEmpleado))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        JsonNode prod = objectMapper.readTree(productos);
        JsonNode agotado = null;
        for (JsonNode p : prod) {
            if (p.path("stockActual").asInt() == 0) {
                agotado = p;
                break;
            }
        }
        if (agotado == null) {
            agotado = prod.get(0);
        }
        long productoId = agotado.path("id").asLong();
        int stock = agotado.path("stockActual").asInt();

        String body = "{\"items\":[{\"productoId\":%d,\"cantidad\":%d}]}".formatted(productoId, stock + 5);
        mockMvc.perform(post("/api/compras")
                        .header("Authorization", tokenEmpleado)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest());
    }

    @Test
    void clientePuedeConsultarYCrearSoloTienda() throws Exception {
        String productos = mockMvc.perform(get("/api/productos")
                        .header("Authorization", tokenCliente))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        JsonNode prod = objectMapper.readTree(productos);
        JsonNode conStock = null;
        for (JsonNode p : prod) {
            if (conStock == null || p.path("stockActual").asInt() > conStock.path("stockActual").asInt()) {
                conStock = p;
            }
        }
        long productoId = conStock.path("id").asLong();

        String body = "{\"items\":[{\"productoId\":%d,\"cantidad\":1}]}".formatted(productoId);
        mockMvc.perform(post("/api/compras")
                        .header("Authorization", tokenCliente)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.email").value("cliente@exito.co"));

        mockMvc.perform(get("/api/movimientos").header("Authorization", tokenCliente))
                .andExpect(status().isForbidden());
    }

    @Test
    void ejecucionDeModeloEoqDevuelveResultado() throws Exception {
        ModeloMatematico eoq = modeloRepository.findAll().stream()
                .filter(m -> "EOQ".equals(m.getMotorImpl()))
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("No se sembró el modelo EOQ como PILLOTO"));

        String body = """
                {"parametros":{"demandaAnual":1200,"costoPedido":2500,"costoAlmacenamiento":350},"fuente":"SIMULADA"}
                """;
        mockMvc.perform(post("/api/modelos/" + eoq.getId() + "/ejecutar")
                        .header("Authorization", tokenAdmin)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.estado").value("OK"))
                .andExpect(jsonPath("$.fuente").value("SIMULADA"))
                .andExpect(jsonPath("$.resultado.qOptima", greaterThan(0)))
                .andExpect(jsonPath("$.interpretacion").isNotEmpty());
    }

    @Test
    void ejecucionDeModeloPropuestoDevuelve409() throws Exception {
        ModeloMatematico propuesto = modeloRepository.findAll().stream()
                .filter(m -> m.getMotorImpl() == null)
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("No hay modelos PROPUESTO"));

        String body = "{\"parametros\":{},\"fuente\":\"SIMULADA\"}";
        mockMvc.perform(post("/api/modelos/" + propuesto.getId() + "/ejecutar")
                        .header("Authorization", tokenAdmin)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isConflict());
    }
}