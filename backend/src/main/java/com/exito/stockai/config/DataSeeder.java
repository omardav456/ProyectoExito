package com.exito.stockai.config;

import com.exito.stockai.model.inventario.Alerta;
import com.exito.stockai.model.inventario.Categoria;
import com.exito.stockai.model.inventario.DemandaDiaria;
import com.exito.stockai.model.inventario.MovimientoInventario;
import com.exito.stockai.model.inventario.Producto;
import com.exito.stockai.model.inventario.Recomendacion;
import com.exito.stockai.model.inventario.enums.TipoAlerta;
import com.exito.stockai.model.inventario.enums.TipoDemanda;
import com.exito.stockai.model.inventario.enums.TipoMovimiento;
import java.math.BigDecimal;
import java.time.LocalDate;
import com.exito.stockai.model.modelos.AplicacionModelo;
import com.exito.stockai.model.modelos.AreaModelo;
import com.exito.stockai.model.modelos.CategoriaModelo;
import com.exito.stockai.model.modelos.MetodoNumerico;
import com.exito.stockai.model.modelos.ModeloMatematico;
import com.exito.stockai.model.modelos.ParametroModelo;
import com.exito.stockai.model.modelos.SubcategoriaModelo;
import com.exito.stockai.model.modelos.VariableModelo;
import com.exito.stockai.model.modelos.enums.ComplejidadModelo;
import com.exito.stockai.model.modelos.enums.EstadoModelo;
import com.exito.stockai.model.modelos.enums.RolVariable;
import com.exito.stockai.config.motores.ModeloMotorMapeo;
import com.exito.stockai.repository.AplicacionModeloRepository;
import com.exito.stockai.repository.AlertaRepository;
import com.exito.stockai.repository.AreaModeloRepository;
import com.exito.stockai.repository.CategoriaModeloRepository;
import com.exito.stockai.repository.CategoriaRepository;
import com.exito.stockai.repository.DemandaDiariaRepository;
import com.exito.stockai.repository.MetodoNumericoRepository;
import com.exito.stockai.repository.ModeloMatematicoRepository;
import com.exito.stockai.repository.MovimientoInventarioRepository;
import com.exito.stockai.repository.ParametroModeloRepository;
import com.exito.stockai.repository.ProductoRepository;
import com.exito.stockai.repository.RecomendacionRepository;
import com.exito.stockai.repository.SubcategoriaModeloRepository;
import com.exito.stockai.repository.VariableModeloRepository;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

@Component
@ConditionalOnProperty("app.seed.catalog-enabled")
public class DataSeeder implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(DataSeeder.class);

    private static final Map<DayOfWeek, Double> FACTOR_DIARIO = Map.of(
            DayOfWeek.MONDAY, 0.80,
            DayOfWeek.TUESDAY, 0.75,
            DayOfWeek.WEDNESDAY, 0.82,
            DayOfWeek.THURSDAY, 0.95,
            DayOfWeek.FRIDAY, 1.10,
            DayOfWeek.SATURDAY, 1.30,
            DayOfWeek.SUNDAY, 1.15);

    private final ObjectMapper objectMapper;
    private final ModeloMatematicoRepository modeloMatematicoRepository;
    private final AreaModeloRepository areaRepository;
    private final CategoriaModeloRepository categoriaModeloRepository;
    private final SubcategoriaModeloRepository subcategoriaRepository;
    private final VariableModeloRepository variableRepository;
    private final ParametroModeloRepository parametroRepository;
    private final AplicacionModeloRepository aplicacionRepository;
    private final MetodoNumericoRepository metodoRepository;
    private final DemandaDiariaRepository demandaDiariaRepository;
    private final MovimientoInventarioRepository movimientoRepository;
    private final ProductoRepository productoRepository;
    private final CategoriaRepository categoriaRepository;
    private final AlertaRepository alertaRepository;
    private final RecomendacionRepository recomendacionRepository;
    private final TransactionTemplate transactionTemplate;

    public DataSeeder(ObjectMapper objectMapper,
                      ModeloMatematicoRepository modeloMatematicoRepository,
                      AreaModeloRepository areaRepository,
                      CategoriaModeloRepository categoriaModeloRepository,
                      SubcategoriaModeloRepository subcategoriaRepository,
                      VariableModeloRepository variableRepository,
                      ParametroModeloRepository parametroRepository,
                      AplicacionModeloRepository aplicacionRepository,
                      MetodoNumericoRepository metodoRepository,
                      DemandaDiariaRepository demandaDiariaRepository,
                      MovimientoInventarioRepository movimientoRepository,
                      ProductoRepository productoRepository,
                      CategoriaRepository categoriaRepository,
                      AlertaRepository alertaRepository,
                      RecomendacionRepository recomendacionRepository,
                      PlatformTransactionManager transactionManager) {
        this.objectMapper = objectMapper;
        this.modeloMatematicoRepository = modeloMatematicoRepository;
        this.areaRepository = areaRepository;
        this.categoriaModeloRepository = categoriaModeloRepository;
        this.subcategoriaRepository = subcategoriaRepository;
        this.variableRepository = variableRepository;
        this.parametroRepository = parametroRepository;
        this.aplicacionRepository = aplicacionRepository;
        this.metodoRepository = metodoRepository;
        this.demandaDiariaRepository = demandaDiariaRepository;
        this.movimientoRepository = movimientoRepository;
        this.productoRepository = productoRepository;
        this.categoriaRepository = categoriaRepository;
        this.alertaRepository = alertaRepository;
        this.recomendacionRepository = recomendacionRepository;
        this.transactionTemplate = new TransactionTemplate(transactionManager);
    }

    @Override
    public void run(String... args) {
        ejecutarSeccion("referencia-demo", this::seedReferenciaDemo);
        ejecutarSeccion("demanda-movimientos", this::seedDemandaYMovimientos);
        ejecutarSeccion("catalogo", this::seedCatalogo);
        ejecutarSeccion("modelos-piloto", this::marcarModelosPiloto);
    }

    private void ejecutarSeccion(String seccion, Runnable accion) {
        try {
            transactionTemplate.executeWithoutResult(estado -> accion.run());
        } catch (RuntimeException e) {
            log.error("DataSeeder: falló la sección '{}': {}", seccion, e.getMessage(), e);
        }
    }

    // ---------------------------------------------------------------
    // Reference demo data - categorias, productos, alertas y
    // recomendaciones (equivalente a V2__seed.sql para bases vacías,
    // útil en entornos como H2 donde Flyway de PostgreSQL no aplica).
    // ---------------------------------------------------------------

    private void seedReferenciaDemo() {
        long existentes = productoRepository.count();
        if (existentes > 0) {
            log.info("DataSeeder: ya existen {} productos; se omite el seed de referencia", existentes);
            return;
        }

        var categorias = List.of(
                Categoria.builder().nombre("Alimentos").descripcion("Productos de despensa y alimentación").color("#FFD100").activa(true).build(),
                Categoria.builder().nombre("Bebidas").descripcion("Gaseosas, aguas y bebidas").color("#60a5fa").activa(true).build(),
                Categoria.builder().nombre("Aseo").descripcion("Limpieza del hogar y detergentes").color("#34d399").activa(true).build(),
                Categoria.builder().nombre("Cuidado Personal").descripcion("Higiene y cuidado personal").color("#f472b6").activa(true).build(),
                Categoria.builder().nombre("Hogar").descripcion("Artículos para el hogar").color("#fb923c").activa(true).build(),
                Categoria.builder().nombre("Frescos").descripcion("Carnes, lácteos y productos refrigerados").color("#f87171").activa(true).build());
        categoriaRepository.saveAll(categorias);

        var categoriaPorNombre = new java.util.HashMap<String, Categoria>();
        categorias.forEach(c -> categoriaPorNombre.put(c.getNombre(), c));

        var productoData = List.of( // nombre, categoria, stock, stockMin, precio, reposicion
                List.of("Arroz Diana x 500g", "Alimentos", 340, 80, 4200, 48),
                List.of("Leche Alquería x 1L", "Frescos", 65, 100, 4600, 75),
                List.of("Coca-Cola x 1.5L", "Bebidas", 220, 60, 7600, 55),
                List.of("Aceite Girasol x 3L", "Alimentos", 180, 40, 23500, 22),
                List.of("Jabón Ariel x 1kg", "Aseo", 95, 50, 18600, 30),
                List.of("Shampoo Head&Shoulders", "Cuidado Personal", 145, 30, 24300, 18),
                List.of("Papel Higiénico x 12", "Hogar", 400, 60, 28900, 40),
                List.of("Pollo Entero Fresco", "Frescos", 28, 40, 14500, 50),
                List.of("Detergente Fabuloso 3L", "Aseo", 112, 35, 15700, 25),
                List.of("Agua Cristal x 600ml", "Bebidas", 310, 80, 3200, 90),
                List.of("Baterías AA x 4", "Hogar", 0, 20, 9800, 30));

        var productos = new ArrayList<Producto>();
        var productoPorNombre = new java.util.HashMap<String, Producto>();
        for (var d : productoData) {
            var producto = Producto.builder()
                    .nombre((String) d.get(0))
                    .descripcion(String.format("%s en presentación de tienda. Categoría %s.",
                            d.get(0), d.get(1)))
                    .categoria(categoriaPorNombre.get((String) d.get(1)))
                    .stockActual((Integer) d.get(2))
                    .stockMinimo((Integer) d.get(3))
                    .precio(new BigDecimal(d.get(4).toString()))
                    .tasaReposicion((Integer) d.get(5))
                    .unidad("uds")
                    .activo(true)
                    .build();
            productos.add(producto);
            productoPorNombre.put(producto.getNombre(), producto);
        }
        productoRepository.saveAll(productos);

        alertaRepository.saveAll(List.of(
                Alerta.builder().producto(productoPorNombre.get("Pollo Entero Fresco")).tipo(TipoAlerta.CRITICO).prioridad("ALTA")
                        .mensaje("Pollo Entero Fresco: stock para menos de 1 día — reposición urgente").build(),
                Alerta.builder().producto(productoPorNombre.get("Leche Alquería x 1L")).tipo(TipoAlerta.CRITICO).prioridad("ALTA")
                        .mensaje("Leche Alquería: stock para 1 día — requiere pedido hoy").build(),
                Alerta.builder().producto(productoPorNombre.get("Coca-Cola x 1.5L")).tipo(TipoAlerta.ADVERTENCIA).prioridad("MEDIA")
                        .mensaje("Coca-Cola 1.5L: stock bajo para el fin de semana de alta demanda").build(),
                Alerta.builder().producto(productoPorNombre.get("Jabón Ariel x 1kg")).tipo(TipoAlerta.ADVERTENCIA).prioridad("MEDIA")
                        .mensaje("Jabón Ariel: 3 días de inventario, demanda en alza").build(),
                Alerta.builder().producto(productoPorNombre.get("Papel Higiénico x 12")).tipo(TipoAlerta.INFO).prioridad("BAJA")
                        .mensaje("Papel Higiénico: sobrestock detectado — revisar próximo pedido").build(),
                Alerta.builder().producto(null).tipo(TipoAlerta.INFO).prioridad("MEDIA")
                        .mensaje("Sábado proyectado: demanda pico semanal — preparar personal").build()));

        recomendacionRepository.saveAll(List.of(
                Recomendacion.builder().producto(null).tipo("OPERATIVA")
                        .titulo("Ampliar horario — Sábado")
                        .descripcion("La demanda proyectada del sábado supera el promedio semanal. Extender horario de apertura y aumentar personal de caja y reposición.")
                        .accionSugerida("Sugerida al gerente de tienda").prioridad("ALTA").fecha(LocalDate.now()).activa(true).build(),
                Recomendacion.builder().producto(productoPorNombre.get("Pollo Entero Fresco")).tipo("OPERATIVA")
                        .titulo("Refuerzo de frescos — Viernes tarde")
                        .descripcion("Pollo y lácteos en nivel crítico. Coordinar entrega del proveedor antes de las 14:00 del viernes.")
                        .accionSugerida("Notificar a proveedores").prioridad("ALTA").fecha(LocalDate.now()).activa(true).build(),
                Recomendacion.builder().producto(null).tipo("OPERATIVA")
                        .titulo("Reducir horario — Martes")
                        .descripcion("Demanda mínima semanal. Evaluar reducción del turno de tarde y aprovechar para inventario físico.")
                        .accionSugerida("Propuesta para aprobación").prioridad("MEDIA").fecha(LocalDate.now()).activa(true).build(),
                Recomendacion.builder().producto(productoPorNombre.get("Papel Higiénico x 12")).tipo("STOCK")
                        .titulo("Pausar pedido — Papel Higiénico")
                        .descripcion("Stock para 10 días. Suspender el próximo pedido hasta bajar a 150 unidades para evitar sobrestock.")
                        .accionSugerida("Cancelar orden programada").prioridad("MEDIA").fecha(LocalDate.now()).activa(true).build()));

        log.info("DataSeeder: sembradas {} categorías, {} productos, {} alertas y {} recomendaciones demo",
                categorias.size(), productos.size(), 6, 4);
    }

    // ---------------------------------------------------------------
    // Part A - demo demanda + movimientos
    // ---------------------------------------------------------------

    private void seedDemandaYMovimientos() {
        long existentes = demandaDiariaRepository.count();
        if (existentes > 0) {
            log.info("DataSeeder: ya existen {} registros de demanda; se omite la generación demo", existentes);
            return;
        }

        var hoy = LocalDate.now();
        var productos = productoRepository.findAll().stream()
                .filter(p -> Boolean.TRUE.equals(p.getActivo()))
                .toList();
        if (productos.isEmpty()) {
            log.info("DataSeeder: no hay productos activos; se omite la generación demo de demanda");
            return;
        }

        var demandas = new ArrayList<DemandaDiaria>();
        var movimientos = new ArrayList<MovimientoInventario>();

        for (var producto : productos) {
            int tasa = producto.getTasaReposicion();

            // Demanda real de los últimos 14 días
            for (int i = 13; i >= 0; i--) {
                var fecha = hoy.minusDays(i);
                var unidades = demandaDiaria(fecha, tasa);
                demandas.add(DemandaDiaria.builder()
                        .producto(producto)
                        .fecha(fecha)
                        .unidades(unidades)
                        .tipo(TipoDemanda.REAL)
                        .build());
            }

            // Demanda prevista (forecast) de los próximos 7 días
            for (int i = 1; i <= 7; i++) {
                var fecha = hoy.plusDays(i);
                var unidades = demandaDiaria(fecha, tasa);
                demandas.add(DemandaDiaria.builder()
                        .producto(producto)
                        .fecha(fecha)
                        .unidades(unidades)
                        .tipo(TipoDemanda.PREVISTA)
                        .build());
            }

            // Movimientos de los últimos 14 días
            for (int i = 13; i >= 0; i--) {
                var fecha = hoy.minusDays(i);
                int dow = fecha.getDayOfWeek().getValue();
                if (dow <= 5) {
                    movimientos.add(MovimientoInventario.builder()
                            .producto(producto)
                            .tipo(TipoMovimiento.ENTRADA)
                            .cantidad(tasa)
                            .fecha(fecha)
                            .descripcion("Reposición diaria")
                            .build());
                }
                var salida = demandaDiaria(fecha, tasa);
                movimientos.add(MovimientoInventario.builder()
                        .producto(producto)
                        .tipo(TipoMovimiento.SALIDA)
                        .cantidad(salida)
                        .fecha(fecha)
                        .descripcion("Venta del día")
                        .build());
            }
        }

        demandaDiariaRepository.saveAll(demandas);
        movimientoRepository.saveAll(movimientos);
        log.info("DataSeeder: generadas {} demandas y {} movimientos demo",
                demandas.size(), movimientos.size());
    }

    private int demandaDiaria(LocalDate fecha, int tasa) {
        double factor = FACTOR_DIARIO.getOrDefault(fecha.getDayOfWeek(), 1.0);
        return Math.max(0, (int) Math.round(tasa * factor));
    }

    // ---------------------------------------------------------------
    // Part B - catálogo de modelos matemáticos (JSON loader)
    // ---------------------------------------------------------------

    private void seedCatalogo() {
        long existentes = modeloMatematicoRepository.count();
        if (existentes > 0) {
            log.info("DataSeeder: ya existen {} modelos matemáticos; se omite el catálogo", existentes);
            return;
        }

        try (var in = getClass().getResourceAsStream("/catalogo_modelos.json")) {
            if (in == null) {
                log.info("DataSeeder: no se encontró catalogo_modelos.json en el classpath; catálogo omitido");
                return;
            }
            var root = objectMapper.readTree(in);
            seedAreas(root);
            log.info("DataSeeder: catálogo de modelos cargado desde catalogo_modelos.json");
        } catch (IOException e) {
            log.info("DataSeeder: no se pudo cargar el catálogo de modelos ({}); se omite", e.getMessage());
        }
    }

    // ---------------------------------------------------------------
    // Motores matemáticos conectados al catálogo (modelos PILOTO)
    // Los 3 modelos ejecutables de esta entrega: EOQ, ABC y Stock & Flow.
    // Se aplica en cada arranque para corregir la referencia del motor.
    // ---------------------------------------------------------------

    private void marcarModelosPiloto() {
        asignarPiloto("EOQ", n -> n.contains("eoq") && n.contains("clasico"));
        asignarPiloto("ABC", n -> n.contains("abc") && n.contains("pareto"));
        asignarPiloto("STOCKFLOW_UHT", n -> n.contains("stock de seguridad por csl"));
        marcarModelosImplementados();
    }

    private void marcarModelosImplementados() {
        java.util.Map<String, String> porNombre = ModeloMotorMapeo.ENTRADA;
        for (var modelo : modeloMatematicoRepository.findAll()) {
            if (modelo.getMotorImpl() != null) {
                continue;
            }
            String nombre = modelo.getNombre() == null ? "" : modelo.getNombre().trim();
            String motor = porNombre.get(nombre);
            if (motor == null) {
                continue;
            }
            modelo.setMotorImpl(motor);
            modelo.setEstado(EstadoModelo.IMPLEMENTADO);
            modeloMatematicoRepository.save(modelo);
            log.info("DataSeeder: modelo implementado: {} ({} -> {})",
                    modelo.getCodigo(), nombre, motor);
        }
    }

    private void asignarPiloto(String motor, java.util.function.Predicate<String> predicado) {
        for (var modelo : modeloMatematicoRepository.findAll()) {
            if (modelo.getMotorImpl() != null) {
                continue;
            }
            String nombre = modelo.getNombre() == null ? "" : modelo.getNombre().toLowerCase(Locale.ROOT);
            if (predicado.test(nombre)) {
                modelo.setMotorImpl(motor);
                modelo.setEstado(EstadoModelo.PILOTO);
                modeloMatematicoRepository.save(modelo);
                log.info("DataSeeder: modelo piloto registrado: {} ({} -> {})",
                        modelo.getCodigo(), modelo.getNombre(), motor);
                return;
            }
        }
    }

    private void seedAreas(JsonNode root) {
        var metodos = seedMetodos(root.get("metodos"));
        var contador = new AtomicInteger(1);

        for (var areaNode : root.path("areas")) {
            var area = AreaModelo.builder()
                    .nombre(areaNode.path("nombre").asText())
                    .descripcion(areaNode.path("descripcion").asText(null))
                    .build();
            areaRepository.save(area);

            for (var catNode : areaNode.path("categorias")) {
                var categoria = CategoriaModelo.builder()
                        .area(area)
                        .nombre(catNode.path("nombre").asText())
                        .descripcion(catNode.path("descripcion").asText(null))
                        .build();
                categoriaModeloRepository.save(categoria);

                for (var subNode : catNode.path("subcategorias")) {
                    var subcategoria = SubcategoriaModelo.builder()
                            .categoria(categoria)
                            .nombre(subNode.path("nombre").asText())
                            .descripcion(subNode.path("descripcion").asText(null))
                            .build();
                    subcategoriaRepository.save(subcategoria);

                    for (var modeloNode : subNode.path("modelos")) {
                        seedModelo(modeloNode, subcategoria, metodos, contador);
                    }
                }
            }
        }
    }

    private Map<String, MetodoNumerico> seedMetodos(JsonNode metodosNode) {
        var metodos = new LinkedHashMap<String, MetodoNumerico>();
        if (metodosNode == null || metodosNode.isMissingNode()) {
            return metodos;
        }
        for (var nombreNode : metodosNode) {
            var nombre = nombreNode.asText();
            var metodo = MetodoNumerico.builder()
                    .nombre(nombre)
                    .build();
            metodoRepository.save(metodo);
            metodos.put(nombre, metodo);
        }
        return metodos;
    }

    private void seedModelo(JsonNode mNode, SubcategoriaModelo subcategoria,
                            Map<String, MetodoNumerico> metodos, AtomicInteger contador) {
        var codigo = String.format("MM-%04d", contador.getAndIncrement());
        var metodoSugerido = mNode.path("metodo").asText(null);

        var modelo = ModeloMatematico.builder()
                .subcategoria(subcategoria)
                .codigo(codigo)
                .nombre(mNode.path("nombre").asText())
                .descripcion(mNode.path("descripcion").asText(null))
                .problema(mNode.path("problema").asText(null))
                .tipoModelo(mNode.path("tipoModelo").asText(null))
                .metodoSugerido(metodoSugerido)
                .complejidad(ComplejidadModelo.valueOf(mNode.path("complejidad").asText().toUpperCase()))
                .entradaEsperada(mNode.path("entrada").asText(null))
                .salidaEsperada(mNode.path("salida").asText(null))
                .estado(EstadoModelo.PROPUESTO)
                .documentacion(mNode.path("documentacion").asText(null))
                .build();

        if (metodoSugerido != null) {
            var metodo = metodos.get(metodoSugerido);
            if (metodo != null) {
                modelo.getMetodos().add(metodo);
            }
        }

        modeloMatematicoRepository.save(modelo);

        for (var vNode : mNode.path("variables")) {
            variableRepository.save(VariableModelo.builder()
                    .modelo(modelo)
                    .nombre(vNode.path("nombre").asText())
                    .simbolo(vNode.path("simbolo").asText(null))
                    .rol(RolVariable.valueOf(vNode.path("rol").asText().toUpperCase()))
                    .tipoDato(vNode.path("tipoDato").asText(null))
                    .unidad(vNode.path("unidad").asText(null))
                    .descripcion(vNode.path("descripcion").asText(null))
                    .build());
        }

        for (var pNode : mNode.path("parametros")) {
            parametroRepository.save(ParametroModelo.builder()
                    .modelo(modelo)
                    .nombre(pNode.path("nombre").asText())
                    .simbolo(pNode.path("simbolo").asText(null))
                    .valorPorDefecto(pNode.path("valorPorDefecto").asText(null))
                    .unidad(pNode.path("unidad").asText(null))
                    .descripcion(pNode.path("descripcion").asText(null))
                    .build());
        }

        for (var aNode : mNode.path("aplicaciones")) {
            aplicacionRepository.save(AplicacionModelo.builder()
                    .modelo(modelo)
                    .descripcion(aNode.asText())
                    .build());
        }
    }
}