package com.exito.stockai.service.inventario;

import com.exito.stockai.dto.DashboardSummary;
import com.exito.stockai.dto.DashboardSummary.CategoriaStock;
import com.exito.stockai.dto.DashboardSummary.DiaDemanda;
import com.exito.stockai.model.inventario.enums.EstadoAlerta;
import com.exito.stockai.model.inventario.enums.TipoDemanda;
import com.exito.stockai.model.inventario.Producto;
import com.exito.stockai.repository.AlertaRepository;
import com.exito.stockai.repository.CategoriaRepository;
import com.exito.stockai.repository.DemandaDiariaRepository;
import com.exito.stockai.repository.MovimientoInventarioRepository;
import com.exito.stockai.repository.ProductoRepository;
import com.exito.stockai.repository.RecomendacionRepository;
import java.time.LocalDate;
import java.time.temporal.TemporalAdjusters;
import java.time.DayOfWeek;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class DashboardService {

    private static final String[] DIAS_SEMANA = {"Lun", "Mar", "Mié", "Jue", "Vie", "Sáb", "Dom"};

    private final ProductoRepository productoRepository;
    private final AlertaRepository alertaRepository;
    private final RecomendacionRepository recomendacionRepository;
    private final DemandaDiariaRepository demandaDiariaRepository;
    private final MovimientoInventarioRepository movimientoInventarioRepository;
    private final CategoriaRepository categoriaRepository;

    public DashboardService(ProductoRepository productoRepository,
                            AlertaRepository alertaRepository,
                            RecomendacionRepository recomendacionRepository,
                            DemandaDiariaRepository demandaDiariaRepository,
                            MovimientoInventarioRepository movimientoInventarioRepository,
                            CategoriaRepository categoriaRepository) {
        this.productoRepository = productoRepository;
        this.alertaRepository = alertaRepository;
        this.recomendacionRepository = recomendacionRepository;
        this.demandaDiariaRepository = demandaDiariaRepository;
        this.movimientoInventarioRepository = movimientoInventarioRepository;
        this.categoriaRepository = categoriaRepository;
    }

    public DashboardSummary resumen() {
        var activos = productoRepository.findAll().stream()
                .filter(p -> Boolean.TRUE.equals(p.getActivo()))
                .toList();

        long totalProductos = activos.size();
        int stockTotal = activos.stream().mapToInt(Producto::getStockActual).sum();

        long productosCriticos = 0;
        long productosBajos = 0;
        long productosSobrestock = 0;

        for (var p : activos) {
            var ventasPromedio = calcularVentasPromedio(p);
            var estado = calcularEstado(p, ventasPromedio);
            switch (estado) {
                case "AGOTADO", "CRITICO" -> productosCriticos++;
                case "BAJO" -> productosBajos++;
                case "SOBRESTOCK" -> productosSobrestock++;
                default -> { }
            }
        }

        long totalAlertasActivas = alertaRepository.findByEstadoOrderByHoraDesc(EstadoAlerta.ACTIVA).size();
        long totalRecomendacionesActivas = recomendacionRepository.findByActivaTrueOrderByPrioridadDescFechaDesc().size();

        var stockPorCategoria = new ArrayList<CategoriaStock>();
        var catMap = new LinkedHashMap<String, Long>();
        for (var p : activos) {
            catMap.merge(p.getCategoria().getNombre(), (long) p.getStockActual(), Long::sum);
        }
        catMap.forEach((nombre, stock) -> stockPorCategoria.add(new CategoriaStock(nombre, stock)));

        var demandaSemana = calcularDemandaSemana();

        return new DashboardSummary(
                totalProductos,
                stockTotal,
                productosCriticos,
                productosBajos,
                productosSobrestock,
                totalAlertasActivas,
                totalRecomendacionesActivas,
                stockPorCategoria,
                demandaSemana
        );
    }

    private List<DiaDemanda> calcularDemandaSemana() {
        var ahora = LocalDate.now();
        var hace28 = ahora.minusDays(28);

        var demandaReal = demandaDiariaRepository.findByTipoAndFechaBetweenOrderByFechaAsc(
                TipoDemanda.REAL, hace28, ahora);

        var sumaPorDia = new LinkedHashMap<Integer, Long>();
        var countPorDia = new LinkedHashMap<Integer, Integer>();
        for (var d : demandaReal) {
            var dow = d.getFecha().getDayOfWeek().getValue();
            sumaPorDia.merge(dow, (long) d.getUnidades(), Long::sum);
            countPorDia.merge(dow, 1, Integer::sum);
        }

        var resultado = new ArrayList<DiaDemanda>();
        for (int i = 1; i <= 7; i++) {
            long promedio = (sumaPorDia.containsKey(i) && countPorDia.containsKey(i) && countPorDia.get(i) > 0)
                    ? Math.round((double) sumaPorDia.get(i) / countPorDia.get(i))
                    : 0L;
            long forecast = Math.round(promedio * 1.05);
            resultado.add(new DiaDemanda(DIAS_SEMANA[i - 1], promedio, forecast));
        }
        return resultado;
    }

    private double calcularVentasPromedio(Producto p) {
        var registros = demandaDiariaRepository.findByProductoIdAndTipoOrderByFechaDesc(
                p.getId(), TipoDemanda.REAL, PageRequest.of(0, 14));
        if (registros.isEmpty()) {
            return p.getTasaReposicion();
        }
        return registros.stream().mapToInt(d -> d.getUnidades()).average().orElse(p.getTasaReposicion());
    }

    private String calcularEstado(Producto p, double ventasPromedio) {
        if (p.getStockActual() == 0) return "AGOTADO";
        if (p.getStockActual() < p.getStockMinimo() * 0.25) return "CRITICO";
        if (p.getStockActual() < p.getStockMinimo()) return "BAJO";
        if (ventasPromedio > 0 && (p.getStockActual() / ventasPromedio) > 14) return "SOBRESTOCK";
        return "OK";
    }
}
