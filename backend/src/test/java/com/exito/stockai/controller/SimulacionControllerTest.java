package com.exito.stockai.controller;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

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
class SimulacionControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void ejecutarSimulacion() throws Exception {
        String json = """
                {"inventarioInicial":600,"reposicion":100,"ventasSemana":[90,85,90,95,115,140,125],"dias":30}
                """;
        mockMvc.perform(post("/api/simulaciones/ejecutar")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.inventarioFinal").exists())
                .andExpect(jsonPath("$.balanceOk").value(true))
                .andExpect(jsonPath("$.reposicionTotal").value(3000))
                .andExpect(jsonPath("$.ventasTotales").value(3135));
    }

    @Test
    void ejecutarSimulacionDatosInvalidos() throws Exception {
        String json = """
                {"inventarioInicial":-1,"reposicion":0,"ventasSemana":[10],"dias":5}
                """;
        mockMvc.perform(post("/api/simulaciones/ejecutar")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isBadRequest());
    }

    @Test
    void ejecutarSimulacionSinVentasSemana() throws Exception {
        String json = """
                {"inventarioInicial":100,"reposicion":10,"dias":7}
                """;
        mockMvc.perform(post("/api/simulaciones/ejecutar")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isBadRequest());
    }

    @Test
    void ejecutarSimulacionVentasSemanaIncorrectas() throws Exception {
        String json = """
                {"inventarioInicial":100,"reposicion":10,"ventasSemana":[1,2,3],"dias":7}
                """;
        mockMvc.perform(post("/api/simulaciones/ejecutar")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isBadRequest());
    }

    @Test
    void ejecutarSimulacionDiasCero() throws Exception {
        String json = """
                {"inventarioInicial":100,"reposicion":10,"ventasSemana":[1,2,3,4,5,6,7],"dias":0}
                """;
        mockMvc.perform(post("/api/simulaciones/ejecutar")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isBadRequest());
    }

    @Test
    void listarSimulacionesVacio() throws Exception {
        mockMvc.perform(get("/api/simulaciones"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$").isEmpty());
    }

    @Test
    void ejecutarYListar() throws Exception {
        String json = """
                {"inventarioInicial":100,"reposicion":5,"ventasSemana":[10,10,10,10,10,10,10],"dias":7}
                """;
        mockMvc.perform(post("/api/simulaciones/ejecutar")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isCreated());

        mockMvc.perform(get("/api/simulaciones"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].modeloCodigo").value("STOCKFLOW_UHT"));
    }

    @Test
    void ejecutarSimulacionAgotamiento() throws Exception {
        String json = """
                {"inventarioInicial":10,"reposicion":0,"ventasSemana":[100,100,100,100,100,100,100],"dias":7}
                """;
        mockMvc.perform(post("/api/simulaciones/ejecutar")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.diaAgotamiento").value(1))
                .andExpect(jsonPath("$.inventarioFinal").value(-690));
    }
}
