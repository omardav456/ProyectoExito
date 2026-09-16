package com.exito.stockai.engine;

import java.util.ArrayList;
import java.util.List;

/**
 * Motor de cálculo Stock & Flow — port fiel de codigo.py.
 * I(t+1) = I(t) + R − V(t)   (patrón semanal cíclico).
 *
 * Esta clase es LA fuente de verdad matemática del sistema.
 * Referencia: Solucion/codigo.py (única fuente original).
 */
public final class MotorStockFlow {

    private static final String[] DIAS_SEMANA = {
        "Lunes", "Martes", "Miércoles", "Jueves", "Viernes", "Sábado", "Domingo"
    };

    private MotorStockFlow() {}

    public record Fila(
        int dia,
        String diaSemana,
        int inventarioInicial,
        int reposicion,
        int venta,
        int inventarioFinal
    ) {}

    public record Resultado(
        int inventarioInicial,
        int reposicion,
        List<Integer> ventasSemana,
        int dias,
        List<Fila> filas,
        int inventarioFinal,
        int ventasTotales,
        int reposicionTotal,
        int balance,
        boolean balanceOk,
        int inventarioMin,
        int diaMin,
        Integer diaAgotamiento,   // null = ∞ (no se agota)
        double ventaPromedio,
        double neteDiarioPromedio,
        // derivados para el panel
        Integer cantidadSemanal,       // Q = max(0, 7·V̄ − I)
        Double diasHastaAgotamiento   // D = I/(V̄−R); null si V̄≤R
    ) {}

    /**
     * Ejecuta la simulación idéntica a simular_inventario() en codigo.py,
     * con las mismas validaciones y la aserción de balance.
     */
    public static Resultado simular(int inventarioInicial,
                                    int reposicion,
                                    List<Integer> ventasSemana,
                                    int dias) {
        // ── Validaciones (iguales a codigo.py) ──
        if (dias <= 0)
            throw new IllegalArgumentException("'dias' debe ser un entero positivo, se recibió: " + dias);
        if (inventarioInicial < 0)
            throw new IllegalArgumentException("'inventario_inicial' no puede ser negativo: " + inventarioInicial);
        if (reposicion < 0)
            throw new IllegalArgumentException("'reposicion' no puede ser negativa: " + reposicion);
        if (ventasSemana == null || ventasSemana.isEmpty())
            throw new IllegalArgumentException("'ventas_semana' no puede estar vacío");
        for (int v : ventasSemana) {
            if (v < 0)
                throw new IllegalArgumentException("Las ventas no pueden ser negativas: " + v);
        }

        int longitudSemana = ventasSemana.size();
        int inventario = inventarioInicial;
        List<Fila> filas = new ArrayList<>();
        int ventasTotales = 0;
        int reposicionTotal = 0;

        for (int dia = 1; dia <= dias; dia++) {
            int venta = ventasSemana.get((dia - 1) % longitudSemana);
            int invInicialDia = inventario;

            inventario = inventario + reposicion - venta;

            filas.add(new Fila(
                dia,
                DIAS_SEMANA[(dia - 1) % 7],
                invInicialDia,
                reposicion,
                venta,
                inventario
            ));
            ventasTotales += venta;
            reposicionTotal += reposicion;
        }

        // Verificación de balance (assertion de codigo.py)
        int balance = inventarioInicial + reposicionTotal - ventasTotales;
        boolean balanceOk = (inventario == balance);

        // Inventario mínimo
        Fila minFila = filas.stream()
            .min((a, b) -> Integer.compare(a.inventarioFinal(), b.inventarioFinal()))
            .orElseThrow();

        // Día de agotamiento
        Integer diaAgotamiento = filas.stream()
            .filter(f -> f.inventarioFinal() < 0)
            .map(Fila::dia)
            .findFirst()
            .orElse(null);

        double ventaPromedio = (double) ventasTotales / dias;
        double neteDiarioPromedio = reposicion - ventaPromedio;

        // Derivados para panel administrativo (Motor de Cálculo)
        int cantidadSemanal = Math.max(0, (int) Math.round(7.0 * ventaPromedio) - inventarioInicial);
        Double diasHastaAgotamiento;
        if (ventaPromedio > reposicion && inventarioInicial > 0) {
            diasHastaAgotamiento = (double) inventarioInicial / (ventaPromedio - reposicion);
        } else {
            diasHastaAgotamiento = null; // ∞
        }

        return new Resultado(
            inventarioInicial, reposicion, ventasSemana, dias,
            filas, inventario, ventasTotales, reposicionTotal,
            balance, balanceOk, minFila.inventarioFinal(), minFila.dia(),
            diaAgotamiento, ventaPromedio, neteDiarioPromedio,
            cantidadSemanal, diasHastaAgotamiento
        );
    }

    /** Predicción simple para demanda semanal: promedio ponderado. */
    public static double[] predecir7Dias(List<Double> historico) {
        if (historico == null || historico.isEmpty()) return new double[7];
        double[] result = new double[7];
        int n = historico.size();
        // Promedio de los últimos 14 días con peso exponencial
        for (int i = 0; i < 7; i++) {
            double suma = 0, pesoTotal = 0;
            for (int j = 0; j < Math.min(n, 28); j++) {
                double peso = Math.pow(0.85, j);
                int idx = n - 1 - j;
                if (idx < 0) break;
                suma += historico.get(idx) * peso;
                pesoTotal += peso;
            }
            result[i] = pesoTotal > 0 ? suma / pesoTotal : 0;
        }
        return result;
    }
}