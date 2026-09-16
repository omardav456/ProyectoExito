package com.exito.stockai.engine;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.exito.stockai.engine.motores.MotoresClasificacion;
import com.exito.stockai.engine.motores.MotoresColas;
import com.exito.stockai.engine.motores.MotoresFinanzas;
import com.exito.stockai.engine.motores.MotoresInventario;
import com.exito.stockai.engine.motores.MotoresPronostico;
import com.exito.stockai.engine.motores.MotoresRiesgo;
import com.exito.stockai.engine.motores.MotoresTransporte;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.springframework.context.annotation.Bean;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class MotoresImplementadosTest {

    private List<MotorMatematico> motores;

    @BeforeAll
    void levantarMotores() throws Exception {
        motores = new ArrayList<>();
        List<Class<?>> confs = List.of(
                MotoresPronostico.class,
                MotoresInventario.class,
                MotoresColas.class,
                MotoresTransporte.class,
                MotoresFinanzas.class,
                MotoresRiesgo.class,
                MotoresClasificacion.class);
        for (Class<?> cfg : confs) {
            Object inst = cfg.getDeclaredConstructor().newInstance();
            for (Method m : cfg.getDeclaredMethods()) {
                if (m.isAnnotationPresent(Bean.class) && MotorMatematico.class.isAssignableFrom(m.getReturnType())) {
                    m.setAccessible(true);
                    motores.add((MotorMatematico) m.invoke(inst));
                }
            }
        }
    }

    @Test
    void hayCienMotoresNuevosRegistrados() {
        assertEquals(100, motores.size());
    }

    @Test
    void losNombresDeMotorSonUnicos() {
        Set<String> nombres = new HashSet<>();
        for (MotorMatematico m : motores) {
            assertTrue(nombres.add(m.nombreMotor()), "Nombre duplicado: " + m.nombreMotor());
        }
    }

    @Test
    void todosEjecutanConParametrosVacios() {
        for (MotorMatematico m : motores) {
            ResultadoMotor res = m.ejecutar(Map.of());
            assertNotNull(res.resultado(), "Sin resultado: " + m.nombreMotor());
            assertFalse(res.resultado().isEmpty(), "Resultado vacío: " + m.nombreMotor());
            assertNotNull(res.mensaje(), "Sin mensaje: " + m.nombreMotor());
            assertFalse(res.interpretacion() == null || res.interpretacion().isBlank(),
                    "Sin interpretación: " + m.nombreMotor());
        }
    }

    @Test
    void losMotoresDePronosticoUsanDatosReales() {
        long reales = motores.stream().filter(MotorMatematico::usaDatosReales).count();
        assertEquals(24, reales);
    }

    @Test
    void smaEjecutaConSerieReal() {
        MotorMatematico sma = motores.stream()
                .filter(m -> m.nombreMotor().equals("SMA"))
                .findFirst().orElseThrow();
        ResultadoMotor res = sma.ejecutar(Map.of("serie", List.of(10d, 12d, 14d, 11d, 13d, 15d, 12d)));
        assertNotNull(res.resultado().get("pronostico"));
    }

    @Test
    void mm1EjecutaConTasaPersonalizada() {
        MotorMatematico mm1 = motores.stream()
                .filter(m -> m.nombreMotor().equals("MM1"))
                .findFirst().orElseThrow();
        ResultadoMotor res = mm1.ejecutar(Map.of("lambda", 50.0, "mu", 80.0));
        Object lq = res.resultado().get("Lq");
        assertNotNull(lq);
        assertEquals(1.04, ((Number) lq).doubleValue(), 0.001);
    }
}