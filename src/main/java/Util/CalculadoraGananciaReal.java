package Util;

import Dto.AlertaDTO;
import Dto.ResumenGananciaRealDTO;
import dao.CierreInventarioDAO;
import dao.CompraDAO;
import dao.ConfiguracionGananciaRealDAO;
import dao.DetalleVentaDAO;
import dao.EmpleadoDAO;
import dao.GastoDAO;
import dao.RendimientoConfigDAO;
import dao.VentaDAO;
import java.text.DecimalFormat;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import model.CierreInventario;
import model.Compra;
import model.ConfiguracionGananciaReal;
import model.DetalleVenta;
import model.RendimientoConfig;
import model.Venta;

/**
 * Calcula la ganancia real de un período (Ventas - CMV - Gastos - Sueldos),
 * a diferencia de la "Utilidad Neta" del Resumen de Caja que no descuenta el
 * costo de la mercadería vendida. También arma las alertas de rendimiento de
 * faena y de precio de venta por debajo de lista.
 */
public class CalculadoraGananciaReal {

    private static final String[] CATEGORIAS = {"Carniceria", "Pollo", "Cerdo"};

    private static final DecimalFormat formatoMoneda = new DecimalFormat("#,##0");
    private static final DecimalFormat formatoKilos = new DecimalFormat("#,##0.00");
    private static final DecimalFormat formatoPorcentaje = new DecimalFormat("#0.0");

    private CalculadoraGananciaReal() {
    }

    public static ResumenGananciaRealDTO calcularResumen(LocalDate inicio, LocalDate fin) {
        CompraDAO compraDAO = new CompraDAO();
        CierreInventarioDAO cierreDAO = new CierreInventarioDAO();
        RendimientoConfigDAO rendimientoDAO = new RendimientoConfigDAO();
        ConfiguracionGananciaRealDAO configDAO = new ConfiguracionGananciaRealDAO();
        VentaDAO ventaDAO = new VentaDAO();
        GastoDAO gastoDAO = new GastoDAO();
        EmpleadoDAO empleadoDAO = new EmpleadoDAO();
        DetalleVentaDAO detalleVentaDAO = new DetalleVentaDAO();

        List<DetalleVenta> detalles = detalleVentaDAO.buscarPorRangoFechas(inicio, fin);
        if (detalles == null) {
            detalles = new ArrayList<>();
        }

        boolean tieneDatos = false;
        double cmvTotal = 0.0;
        List<AlertaDTO> alertas = new ArrayList<>();

        for (String categoria : CATEGORIAS) {
            List<Compra> compras = compraDAO.buscarPorRangoYCategoria(inicio, fin, categoria);
            double pesoCompras = 0.0;
            double costoCompras = 0.0;
            for (Compra c : compras) {
                pesoCompras += c.getPesoTotalKg() != null ? c.getPesoTotalKg() : 0.0;
                costoCompras += c.getCostoTotal() != null ? c.getCostoTotal() : 0.0;
            }

            CierreInventario cierre = cierreDAO.buscarPorFechaYCategoria(fin, categoria);
            double kgRestantes = cierre != null && cierre.getKgRestantes() != null ? cierre.getKgRestantes() : 0.0;

            if (!compras.isEmpty() || cierre != null) {
                tieneDatos = true;
            }

            double costoPromedioKg = pesoCompras > 0 ? costoCompras / pesoCompras : 0.0;
            double valorStockFinal = kgRestantes * costoPromedioKg;
            double costoDeLoVendido = costoCompras - valorStockFinal;
            cmvTotal += costoDeLoVendido;

            double kgVendido = 0.0;
            for (DetalleVenta d : detalles) {
                if (d.getProducto() != null && categoria.equals(d.getProducto().getTipo())) {
                    kgVendido += d.getPeso() != null ? d.getPeso() : 0.0;
                }
            }

            if (pesoCompras > 0) {
                RendimientoConfig config = rendimientoDAO.obtenerOCrearDefault(categoria);
                double porcentajeAprovechable = config.getPorcentajeAprovechable() != null ? config.getPorcentajeAprovechable() : 0.0;
                double umbralAlerta = config.getUmbralAlertaPorcentaje() != null ? config.getUmbralAlertaPorcentaje() : 0.90;

                if (porcentajeAprovechable > 0) {
                    double kgEsperado = pesoCompras * porcentajeAprovechable;
                    double kgContabilizado = kgVendido + kgRestantes;

                    if (kgContabilizado < kgEsperado * umbralAlerta) {
                        String mensaje = "Rendimiento por debajo de lo esperado en " + categoria + ": "
                                + "se esperaban ~" + formatoKilos.format(kgEsperado) + " kg vendibles, "
                                + "se contabilizaron " + formatoKilos.format(kgContabilizado) + " kg "
                                + "(vendido + stock). Revisar merma de corte.";
                        alertas.add(new AlertaDTO("RENDIMIENTO", mensaje));
                    }
                }
            }
        }

        double umbralDesviacionPrecio = 0.03;
        try {
            ConfiguracionGananciaReal configPrecio = configDAO.obtenerOCrear();
            if (configPrecio.getUmbralDesviacionPrecio() != null) {
                umbralDesviacionPrecio = configPrecio.getUmbralDesviacionPrecio();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        for (DetalleVenta d : detalles) {
            if (d.getProducto() == null || d.getPrecio() == null || d.getProducto().getPrecio() == null) {
                continue;
            }
            double precioRealizado = d.getPrecio();
            double precioLista = d.getProducto().getPrecio();
            if (precioLista <= 0) {
                continue;
            }

            if (precioRealizado < precioLista * (1 - umbralDesviacionPrecio)) {
                double desviacion = (1 - precioRealizado / precioLista) * 100;
                String mensaje = d.getProducto().getNombre() + " vendido a $" + formatoMoneda.format(precioRealizado)
                        + "/kg, lista dice $" + formatoMoneda.format(precioLista) + "/kg ("
                        + formatoPorcentaje.format(desviacion) + "% por debajo)";
                alertas.add(new AlertaDTO("PRECIO", mensaje));
            }
        }

        double totalVentas = 0.0;
        List<Venta> ventas = ventaDAO.buscarVentasPorFecha(inicio, fin);
        if (ventas != null) {
            for (Venta v : ventas) {
                totalVentas += v.getTotal() != null ? v.getTotal() : 0.0;
            }
        }

        double totalGastos = gastoDAO.sumarGastosPorRango(inicio, fin);
        double totalSueldos = empleadoDAO.totalPagadoSemana(inicio, fin);

        double gananciaReal = totalVentas - cmvTotal - totalGastos - totalSueldos;
        double margenReal = totalVentas > 0 ? gananciaReal / totalVentas * 100 : 0.0;
        double gananciaSobreCosto = cmvTotal > 0 ? gananciaReal / cmvTotal * 100 : 0.0;

        return new ResumenGananciaRealDTO(tieneDatos, totalVentas, totalGastos, totalSueldos,
                cmvTotal, gananciaReal, margenReal, gananciaSobreCosto, alertas);
    }
}
