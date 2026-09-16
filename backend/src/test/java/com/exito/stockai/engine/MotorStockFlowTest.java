package com.exito.stockai.engine;

import static org.junit.jupiter.api.Assertions.*;

import java.util.List;
import org.junit.jupiter.api.Test;

class MotorStockFlowTest {

    @Test
    void casoNormal() {
        // I(0)=600, R=100, ventas=[90,85,90,95,115,140,125], 30 days
        // sum ventas = 740. Over 30 days: 4 cycles + 2 days = 4*740 + (90+85) = 3135
        // Final = 600 + 30*100 - 3135 = 465
        var r = MotorStockFlow.simular(600, 100, List.of(90, 85, 90, 95, 115, 140, 125), 30);
        assertTrue(r.balanceOk());
        assertEquals(465, r.inventarioFinal());
        assertEquals(3000, r.reposicionTotal());
        assertEquals(3135, r.ventasTotales());
        assertEquals(465, r.balance());
        assertEquals(30, r.filas().size());
        assertNull(r.diaAgotamiento());
    }

    @Test
    void inventarioInicialNegativoLanza() {
        assertThrows(IllegalArgumentException.class,
                () -> MotorStockFlow.simular(-1, 100, List.of(50), 5));
    }

    @Test
    void diasNegativosLanza() {
        assertThrows(IllegalArgumentException.class,
                () -> MotorStockFlow.simular(100, 50, List.of(50), -1));
    }

    @Test
    void ventasNegativasLanza() {
        assertThrows(IllegalArgumentException.class,
                () -> MotorStockFlow.simular(100, 50, List.of(-10), 5));
    }

    @Test
    void reposicionNegativaLanza() {
        assertThrows(IllegalArgumentException.class,
                () -> MotorStockFlow.simular(100, -1, List.of(50), 5));
    }

    @Test
    void ventasVaciasLanza() {
        assertThrows(IllegalArgumentException.class,
                () -> MotorStockFlow.simular(100, 50, List.of(), 5));
    }

    @Test
    void ventasNulasLanza() {
        assertThrows(IllegalArgumentException.class,
                () -> MotorStockFlow.simular(100, 50, null, 5));
    }

    @Test
    void agotamiento() {
        // Very low inventory, high demand, no replenishment
        var r = MotorStockFlow.simular(10, 0, List.of(100, 100, 100, 100, 100, 100, 100), 7);
        assertNotNull(r.diaAgotamiento());
        assertEquals(1, r.diaAgotamiento());
    }

    @Test
    void ventasPromedioMayorQueReposicion() {
        // 7 days: 7*10 = 70 sold, 7*5 = 35 entered. Initial 50, final 15.
        var r = MotorStockFlow.simular(50, 5, List.of(10), 7);
        assertTrue(r.balanceOk());
        assertEquals(15, r.inventarioFinal());
        assertEquals(35, r.reposicionTotal());
        assertEquals(70, r.ventasTotales());
        assertNotNull(r.cantidadSemanal());
    }

    @Test
    void ventaPromedioIgualAReposicionInfinito() {
        // R=50, avg V=50 -> net 0 -> D = infinity
        var r = MotorStockFlow.simular(200, 50, List.of(50), 30);
        assertNull(r.diasHastaAgotamiento());
        assertNull(r.diaAgotamiento());
    }

    @Test
    void cantidadSemanalFormula() {
        // Q = max(0, 7*Vbar - I)
        // Vbar = 30, 7*30 = 210, I = 100, Q = max(0, 210-100) = 110
        var r = MotorStockFlow.simular(100, 20, List.of(30, 30, 30, 30, 30, 30, 30), 7);
        assertEquals(110, r.cantidadSemanal());
    }

    @Test
    void cantidadSemanalInventarioAlto() {
        // Vbar = 10, 7*10 = 70, I = 200 -> Q = max(0, 70-200) = 0
        var r = MotorStockFlow.simular(200, 5, List.of(10), 7);
        assertEquals(0, r.cantidadSemanal());
    }

    @Test
    void primeraFilaInventarioInicial() {
        var r = MotorStockFlow.simular(100, 10, List.of(5, 5, 5), 3);
        assertEquals(100, r.filas().get(0).inventarioInicial());
        assertEquals(105, r.filas().get(0).inventarioFinal()); // 100+10-5
    }

    @Test
    void filasTodasLasDias() {
        var r = MotorStockFlow.simular(50, 5, List.of(10), 10);
        assertEquals(10, r.filas().size());
        for (int i = 0; i < 10; i++) {
            assertEquals(i + 1, r.filas().get(i).dia());
            assertEquals(5, r.filas().get(i).reposicion());
            assertEquals(10, r.filas().get(i).venta());
        }
    }

    @Test
    void inventarioMinimo() {
        // Inventory: 100 -> 80 -> 60 -> 40 -> 20 -> 0 -> -20
        var r = MotorStockFlow.simular(100, 0, List.of(20), 6);
        assertEquals(-20, r.inventarioMin());
        assertEquals(6, r.diaMin());
    }

    @Test
    void ventaPromedioYNeteDiario() {
        // R=10, avg V = 30, nete = 10-30 = -20
        var r = MotorStockFlow.simular(100, 10, List.of(30), 10);
        assertEquals(30.0, r.ventaPromedio(), 0.001);
        assertEquals(-20.0, r.neteDiarioPromedio(), 0.001);
    }

    @Test
    void diasHastaAgotamientoCalculo() {
        // Vbar = 20, R = 5, I = 100 -> D = 100/(20-5) = 6.666...
        var r = MotorStockFlow.simular(100, 5, List.of(20), 30);
        assertNotNull(r.diasHastaAgotamiento());
        assertEquals(100.0 / 15.0, r.diasHastaAgotamiento(), 0.001);
    }

    @Test
    void predecir7DiasConstante() {
        var historico = List.of(50.0, 50.0, 50.0, 50.0, 50.0);
        double[] pred = MotorStockFlow.predecir7Dias(historico);
        assertEquals(7, pred.length);
        for (double v : pred) {
            assertEquals(50.0, v, 0.001);
        }
    }

    @Test
    void predecir7DiasVacio() {
        double[] pred = MotorStockFlow.predecir7Dias(List.of());
        assertEquals(7, pred.length);
        for (double v : pred) {
            assertEquals(0.0, v, 0.001);
        }
    }

    @Test
    void predecir7DiasNulo() {
        double[] pred = MotorStockFlow.predecir7Dias(null);
        assertEquals(7, pred.length);
    }
}
