package com.exito.stockai.config.motores;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Mapeo generado por modelos/seleccionar_motores.py.
 * nombre exacto del modelo de catalogo -> motor_impl registrado en Spring.
 */
public final class ModeloMotorMapeo {

    private ModeloMotorMapeo() {}

    public static final Map<String, String> ENTRADA = new LinkedHashMap<>();

    static {
        ENTRADA.put("Media Movil Simple (SMA)", "SMA");
        ENTRADA.put("Media Movil Doble", "SMA_DOBLE");
        ENTRADA.put("Media Movil Ponderada (WMA)", "WMA");
        ENTRADA.put("Suavizado Exponencial Simple (SES)", "SES");
        ENTRADA.put("SES optimizado por Error Cuadratico", "SES_OPT");
        ENTRADA.put("Metodo Holt Doble Exponencial", "HOLT");
        ENTRADA.put("Holt-Winters Aditivo", "HW_ADITIVO");
        ENTRADA.put("Holt-Winters Multiplicativo", "HW_MULT");
        ENTRADA.put("Croston Demanda Intermitente", "CROSTON");
        ENTRADA.put("Croston con Venta Nula", "CROSTON_CEROS");
        ENTRADA.put("SBA Syntetos Boylan", "SBA");
        ENTRADA.put("Tevar Correccion por Ceros", "TEVAR");
        ENTRADA.put("Across-Time Agregacion", "AGREGACION_TEMPORAL");
        ENTRADA.put("Descomposicion Clasica Aditiva", "DESCOMPOSICION_ADITIVA");
        ENTRADA.put("Descomposicion STL", "STL");
        ENTRADA.put("Tendencias Estacionales iOS", "ESTACIONALIDAD_CAL");
        ENTRADA.put("ARIMA (p,d,q)", "ARIMA");
        ENTRADA.put("SARIMA Estacional", "SARIMA");
        ENTRADA.put("ARIMAX con Exogenas", "ARIMAX");
        ENTRADA.put("Tracking Signal", "TRACKING_SIGNAL");
        ENTRADA.put("Forecast con Lift Aplicado", "LIFT_PROMOCION");
        ENTRADA.put("Regresion con Exogenas de Calendario", "REGRESION_CALENDARIO");
        ENTRADA.put("Modelo de Regresion con Variables de Promocion", "REGRESION_PROMO");
        ENTRADA.put("Efecto de Ingreso y Renta", "REGRESION_INGRESO");
        ENTRADA.put("EOQ con Descuentos por Volumen", "EOQ_DESCUENTOS");
        ENTRADA.put("EOQ con Faltantes", "EOQ_FALTANTES");
        ENTRADA.put("EOQ Perdible", "EOQ_PERDIBLE");
        ENTRADA.put("EPQ Lote de Produccion", "EOQ_EPQ");
        ENTRADA.put("EOQ Multi-Producto con Capacidad", "EOQ_CAPACIDAD");
        ENTRADA.put("EOQ con Lead Time de Importacion", "EOQ_IMPORTACION");
        ENTRADA.put("EOQ con Descuento por Pronto Pago", "EOQ_PRONT_PAGO");
        ENTRADA.put("EOQ Estocastico con Nivel de Servicio", "EOQ_ESTOCASTICO");
        ENTRADA.put("EOQ con Costo de Faltante Lineal", "EOQ_FALTANTE_LINEAL");
        ENTRADA.put("EOQ con Capacidad de Proveedor", "EOQ_CAPACIDAD_PROV");
        ENTRADA.put("Politica (r, Q) Reorden Punto-Lote", "POLITICA_RQ");
        ENTRADA.put("Politica (s, S) Nivel Min-Max", "POLITICA_SS");
        ENTRADA.put("Politica (R, S) Revision Periodica", "POLITICA_RS");
        ENTRADA.put("Politica (T, S) Ciclo Fijo con Dos Niveles", "POLITICA_TS");
        ENTRADA.put("Politica de Pedido Ascendente (Up-to-level)", "UP_TO_LEVEL");
        ENTRADA.put("Politica de Punto de Pedido con Revisión Semanal", "POLITICA_SEMANAL");
        ENTRADA.put("Stock de Seguridad por Fill Rate", "STOCK_SEG_FR");
        ENTRADA.put("Stock de Seguridad con Lead Time Variable", "STOCK_SEG_LT");
        ENTRADA.put("Hedge Stock Politica Critica", "HEDGE_STOCK");
        ENTRADA.put("Modelo de Compra de Temporada (Newsvendor)", "NEWSVENDOR");
        ENTRADA.put("Silver-Meal Heuristico", "SILVER_MEAL");
        ENTRADA.put("Wagner-Whitin Dinamico", "WAGNER_WHITIN");
        ENTRADA.put("JRP Reposicion Conjunta", "JRP");
        ENTRADA.put("Grupo de Reposicion por Proveedor", "GRUPO_REPOSICION");
        ENTRADA.put("Modulo de Compra Local vs Importado", "SOURCING_LOCAL_IMP");
        ENTRADA.put("Dual Sourcing Diversificacion", "DUAL_SOURCING");
        ENTRADA.put("Costo de Mantener Inventario (Holding)", "HOLDING_COST");
        ENTRADA.put("DSI Dias de Inventario", "DSI");
        ENTRADA.put("Costo de Capital del Inventario", "WACC");
        ENTRADA.put("Valuacion a Reposicion (Replacement Cost)", "REEMPLAZO");
        ENTRADA.put("Cola M/M/1", "MM1");
        ENTRADA.put("Cola M/M/c/K Capacidad Limitada", "MMC");
        ENTRADA.put("Cola M/M/c/K Capacidad Limitada robusto", "MMCK");
        ENTRADA.put("Cola M/G/1 Pollaczek", "MG1");
        ENTRADA.put("Cola con Llegadas en Lote", "BATCH_QUEUE");
        ENTRADA.put("Ley de Little", "LITTLE");
        ENTRADA.put("Cola de Cajas Express (Prioridad)", "PRIORIDAD");
        ENTRADA.put("Numero de Cajas Optimo por E/S", "C_MAXIMO");
        ENTRADA.put("Capacidad Instalada vs Demanda", "CAPACIDAD_COLA");
        ENTRADA.put("Self-Checkout vs Caja Humana", "SELFCHECKOUT");
        ENTRADA.put("Modelo de Congestion de Pasillos", "CONGESTION");
        ENTRADA.put("Trafico Caliente y Zonas de Exhibicion", "ZONAS");
        ENTRADA.put("Modelo de Aglomeracion y Abandono", "RENEGING");
        ENTRADA.put("VRP Con Flota Homogenea", "VRP");
        ENTRADA.put("CVRP Capacitado", "CVRP");
        ENTRADA.put("VRPTW Ventanas de Tiempo", "VRPTW");
        ENTRADA.put("TSP Problema del Viajante", "TSP");
        ENTRADA.put("TSP Picking Secuencia", "TSP_PICKING");
        ENTRADA.put("P-Mediana para Almacen", "P_MEDIANA_ALM");
        ENTRADA.put("P-Mediana de Red", "P_MEDIANA_RED");
        ENTRADA.put("Problema de Transporte Lineal", "TRANSPORTE_CLASICO");
        ENTRADA.put("Location Set Covering", "SET_COVERING");
        ENTRADA.put("Selective Delivery Cadena Fria", "COLD_CHAIN");
        ENTRADA.put("Coordinacion Proveedor-CEDI (VMI)", "VMI");
        ENTRADA.put("Fleet Sizing", "FLEET");
        ENTRADA.put("Margen Bruto por Categoria", "MARGEN");
        ENTRADA.put("Contribucion Marginal del Frente", "CONTRIBUCION");
        ENTRADA.put("Indicador ROI del Inventario (GMROI)", "GMROI");
        ENTRADA.put("Modelo DPP Direct Product Profit", "DPP");
        ENTRADA.put("Precio Optimo por Margen", "PRICING");
        ENTRADA.put("Markdown Optimo Cycle", "MARKDOWN");
        ENTRADA.put("Newsvendor Multi-Perdido", "NEWSVENDOR_MULTI");
        ENTRADA.put("VaR Value at Risk", "VAR");
        ENTRADA.put("CVaR Valor en Riesgo Condicional", "CVAR");
        ENTRADA.put("Monte Carlo de Demanda", "MONTE_CARLO");
        ENTRADA.put("Analisis de Escenarios Base/Pesimista/Optimista", "ESCENARIOS");
        ENTRADA.put("Stress Test de Cadena", "STRESS");
        ENTRADA.put("Modelo de Riesgo de Fuente de Proveedor", "RIESGO_PROV");
        ENTRADA.put("Probabilidad de Agotamiento (P-Stockout)", "STOCKOUT");
        ENTRADA.put("Indicador Esperado de Faltante (ESC)", "ESC");
        ENTRADA.put("Analisis de Sensibilidad Univariado", "SENSIBILIDAD");
        ENTRADA.put("Curva de Lorenz de Inventario", "LORENZ");
        ENTRADA.put("Clasificacion XYZ por Variabilidad", "XYZ");
        ENTRADA.put("Matriz ABC-XYZ Integrada", "ABC_XYZ");
        ENTRADA.put("Conteo Ciclico con Clasificacion", "CONTEO_CICLICO");
        ENTRADA.put("Modelo de Merma por Caducidad", "MERMA");
    }
}
