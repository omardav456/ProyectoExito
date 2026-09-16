package com.exito.stockai.controller;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import tools.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class ProductoControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void listarProductosEsArray() throws Exception {
        mockMvc.perform(get("/api/productos"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray());
    }

    @Test
    void crearProducto() throws Exception {
        String catJson = "{\"nombre\":\"TestCat\",\"descripcion\":\"test\",\"color\":\"#fff\",\"activa\":true}";
        String catResult = mockMvc.perform(post("/api/categorias")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(catJson))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        Long catId = objectMapper.readTree(catResult).get("id").asLong();

        String prodJson = """
                {"nombre":"TestProd","categoriaId":%d,"stockActual":50,"stockMinimo":10,"precio":1000,"tasaReposicion":5,"unidad":"uds","activo":true}
                """.formatted(catId);
        mockMvc.perform(post("/api/productos")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(prodJson))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.nombre").value("TestProd"))
                .andExpect(jsonPath("$.categoriaId").value(catId))
                .andExpect(jsonPath("$.stockActual").value(50))
                .andExpect(jsonPath("$.activo").value(true));
    }

    @Test
    void crearProductoSinCategoriaFalla() throws Exception {
        String prodJson = """
                {"nombre":"NoCat","stockActual":10,"stockMinimo":5,"precio":500,"tasaReposicion":2,"unidad":"uds","activo":true}
                """;
        mockMvc.perform(post("/api/productos")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(prodJson))
                .andExpect(status().isBadRequest());
    }

    @Test
    void buscarProductosPorCategoria() throws Exception {
        String catJson = "{\"nombre\":\"FilterCat\",\"descripcion\":\"filter\",\"color\":\"#000\",\"activa\":true}";
        String catResult = mockMvc.perform(post("/api/categorias")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(catJson))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        Long catId = objectMapper.readTree(catResult).get("id").asLong();

        String prod1 = """
                {"nombre":"ProdA","categoriaId":%d,"stockActual":10,"stockMinimo":5,"precio":100,"tasaReposicion":2,"unidad":"uds","activo":true}
                """.formatted(catId);
        String prod2 = """
                {"nombre":"ProdB","categoriaId":%d,"stockActual":20,"stockMinimo":5,"precio":200,"tasaReposicion":3,"unidad":"uds","activo":true}
                """.formatted(catId);

        mockMvc.perform(post("/api/productos").contentType(MediaType.APPLICATION_JSON).content(prod1))
                .andExpect(status().isCreated());
        mockMvc.perform(post("/api/productos").contentType(MediaType.APPLICATION_JSON).content(prod2))
                .andExpect(status().isCreated());

        mockMvc.perform(get("/api/productos").param("categoriaId", catId.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$.length()").value(2));
    }

    @Test
    void buscarProductosPorNombre() throws Exception {
        String catJson = "{\"nombre\":\"SearchCat\",\"descripcion\":\"search\",\"color\":\"#aaa\",\"activa\":true}";
        String catResult = mockMvc.perform(post("/api/categorias")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(catJson))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        Long catId = objectMapper.readTree(catResult).get("id").asLong();

        String prod = """
                {"nombre":"UniqueAlpha","categoriaId":%d,"stockActual":5,"stockMinimo":1,"precio":50,"tasaReposicion":1,"unidad":"uds","activo":true}
                """.formatted(catId);
        mockMvc.perform(post("/api/productos").contentType(MediaType.APPLICATION_JSON).content(prod))
                .andExpect(status().isCreated());

        mockMvc.perform(get("/api/productos").param("q", "Unique"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].nombre").value("UniqueAlpha"));
    }

    @Test
    void buscarPorId() throws Exception {
        String catJson = "{\"nombre\":\"ByIdCat\",\"descripcion\":\"byid\",\"color\":\"#111\",\"activa\":true}";
        String catResult = mockMvc.perform(post("/api/categorias")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(catJson))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        Long catId = objectMapper.readTree(catResult).get("id").asLong();

        String prod = """
                {"nombre":"ByIdProd","categoriaId":%d,"stockActual":7,"stockMinimo":2,"precio":99,"tasaReposicion":1,"unidad":"uds","activo":true}
                """.formatted(catId);
        String prodResult = mockMvc.perform(post("/api/productos")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(prod))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        Long prodId = objectMapper.readTree(prodResult).get("id").asLong();

        mockMvc.perform(get("/api/productos/" + prodId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.nombre").value("ByIdProd"));
    }

    @Test
    void buscarPorIdNoExistente() throws Exception {
        mockMvc.perform(get("/api/productos/99999"))
                .andExpect(status().isNotFound());
    }

    @Test
    void eliminarProducto() throws Exception {
        String catJson = "{\"nombre\":\"DelCat\",\"description\":\"del\",\"color\":\"#222\",\"activa\":true}";
        String catResult = mockMvc.perform(post("/api/categorias")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(catJson))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        Long catId = objectMapper.readTree(catResult).get("id").asLong();

        String prod = """
                {"nombre":"DelProd","categoriaId":%d,"stockActual":1,"stockMinimo":1,"precio":10,"tasaReposicion":1,"unidad":"uds","activo":true}
                """.formatted(catId);
        String prodResult = mockMvc.perform(post("/api/productos")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(prod))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        Long prodId = objectMapper.readTree(prodResult).get("id").asLong();

        mockMvc.perform(delete("/api/productos/" + prodId))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/productos/" + prodId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.activo").value(false));
    }
}
