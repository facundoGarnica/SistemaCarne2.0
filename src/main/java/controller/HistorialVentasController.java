/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/javafx/FXMLController.java to edit this template
 */
package controller;

import Util.ComprobanteVenta;
import Util.DialogoUtil;
import Util.HorarioLocal;
import dao.GastoDAO;
import dao.VentaDAO;
import dao.DetalleVentaDAO;
import java.io.File;
import java.net.URL;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.ResourceBundle;
import javafx.application.Platform;
import javafx.beans.property.SimpleObjectProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.ComboBox;
import javafx.scene.control.DatePicker;
import javafx.scene.control.Label;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextInputDialog;
import javafx.scene.control.ToggleButton;
import javafx.scene.control.ToggleGroup;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.stage.FileChooser;
import model.DetalleVenta;
import model.ExportarExcel;
import model.PrintTicket;
import model.Venta;

/**
 * FXML Controller class
 *
 * @author garca
 */
public class HistorialVentasController implements Initializable {

    private static final String PASSWORD_ADMIN = "2808";
    private static final Locale LOCALE_AR = new Locale("es", "AR");
    private static final DateTimeFormatter FORMATO_FECHA = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final DateTimeFormatter FORMATO_HORA = DateTimeFormatter.ofPattern("HH:mm");
    private static final DateTimeFormatter FORMATO_DIA_MES = DateTimeFormatter.ofPattern("dd/MM");
    private static final DateTimeFormatter FORMATO_DIA_LARGO = DateTimeFormatter.ofPattern("EEEE d 'de' MMMM", LOCALE_AR);
    private static final DateTimeFormatter FORMATO_MES = DateTimeFormatter.ofPattern("MMMM yyyy", LOCALE_AR);

    private static final String TODOS_LOS_PAGOS = "Todos los pagos";

    private final DecimalFormat formatoMoneda = new DecimalFormat("#,##0", DecimalFormatSymbols.getInstance(LOCALE_AR));
    private final DecimalFormat formatoPeso = new DecimalFormat("#,##0.###", DecimalFormatSymbols.getInstance(LOCALE_AR));

    private VentaDAO ventaDao;
    private DetalleVentaDAO detalleVentaDao;
    private GastoDAO gastoDao;

    /** Ventas del período cargado (sin filtrar por medio de pago). */
    private ObservableList<Venta> observableVentas = FXCollections.observableArrayList();
    /** Ventas visibles en la tabla (filtradas por medio de pago). */
    private FilteredList<Venta> ventasFiltradas;

    /** Período actualmente mostrado en pantalla. */
    private LocalDate periodoDesde = LocalDate.now();
    private LocalDate periodoHasta = LocalDate.now();

    @FXML
    private Label lblPeriodo;
    @FXML
    private ToggleGroup grupoPeriodo;
    @FXML
    private ToggleButton tglHoy;
    @FXML
    private ToggleButton tglRango;
    @FXML
    private HBox boxRango;
    @FXML
    private DatePicker datePickerDesde;
    @FXML
    private DatePicker datePickerHasta;
    @FXML
    private ComboBox<String> cbxMedioPago;

    @FXML
    private Label lblTotalVendido;
    @FXML
    private Label lblCantidadVentas;
    @FXML
    private Label lblEfectivo;
    @FXML
    private Label lblVirtual;

    // Tabla de Ventas
    @FXML
    private TableView<Venta> tlbVerVentas;
    @FXML
    private TableColumn<Venta, LocalDateTime> colHora;
    @FXML
    private TableColumn<Venta, Venta> colMedioPago;
    @FXML
    private TableColumn<Venta, Double> colTotal;

    // Panel de detalle
    @FXML
    private Label lblSinSeleccion;
    @FXML
    private VBox panelDetalle;
    @FXML
    private Label lblVentaNumero;
    @FXML
    private Label lblVentaPago;
    @FXML
    private Label lblVentaFecha;
    @FXML
    private VBox boxItems;
    @FXML
    private Label lblVentaTotal;
    @FXML
    private Button btnEliminar;
    @FXML
    private Button btnImprimir;

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        ventaDao = new VentaDAO();
        detalleVentaDao = new DetalleVentaDAO();
        gastoDao = new GastoDAO();

        ventasFiltradas = new FilteredList<>(observableVentas, v -> true);
        tlbVerVentas.setItems(ventasFiltradas);

        configurarFiltros();
        configurarColumnasVentas();
        configurarSeleccionVenta();

        // Por defecto se muestran las ventas del día
        buscarVentasDeHoy();
    }

    private void configurarFiltros() {
        // Siempre tiene que quedar un período elegido: no se permite "des-seleccionar" la pill activa
        grupoPeriodo.selectedToggleProperty().addListener((obs, anterior, nuevo) -> {
            if (nuevo == null && anterior != null) {
                anterior.setSelected(true);
            }
            boolean esRango = grupoPeriodo.getSelectedToggle() == tglRango;
            boxRango.setVisible(esRango);
            boxRango.setManaged(esRango);
        });

        cbxMedioPago.setItems(FXCollections.observableArrayList(TODOS_LOS_PAGOS, "Efectivo", "Virtual"));
        cbxMedioPago.getSelectionModel().select(TODOS_LOS_PAGOS);
        cbxMedioPago.valueProperty().addListener((obs, anterior, nuevo) -> aplicarFiltroMedioPago());
    }

    private void configurarColumnasVentas() {
        colHora.setCellValueFactory(cellData -> new SimpleObjectProperty<>(cellData.getValue().getFecha()));
        colMedioPago.setCellValueFactory(cellData -> new SimpleObjectProperty<>(cellData.getValue()));
        colTotal.setCellValueFactory(cellData -> new SimpleObjectProperty<>(cellData.getValue().getTotal()));

        colHora.setCellFactory(column -> new TableCell<Venta, LocalDateTime>() {
            @Override
            protected void updateItem(LocalDateTime item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                } else if (esPeriodoDeUnDia()) {
                    setText(item.format(FORMATO_HORA));
                } else {
                    setText(item.format(FORMATO_DIA_MES) + "  " + item.format(FORMATO_HORA));
                }
            }
        });

        // Pago: chip con el medio + cantidad de productos
        colMedioPago.setCellFactory(column -> new TableCell<Venta, Venta>() {
            private final Label chip = new Label();
            private final Label productos = new Label();
            private final HBox contenido = new HBox(10, chip, productos);

            {
                chip.getStyleClass().add("hv-chip");
                productos.getStyleClass().add("hv-muted");
                contenido.setAlignment(Pos.CENTER_LEFT);
            }

            @Override
            protected void updateItem(Venta venta, boolean empty) {
                super.updateItem(venta, empty);
                setText(null);
                if (empty || venta == null) {
                    setGraphic(null);
                    return;
                }
                String medio = venta.getMedioPago() == null || venta.getMedioPago().isBlank() ? "---" : venta.getMedioPago();
                chip.setText(medio);
                int cantidad = venta.getDetalleVentas() == null ? 0 : venta.getDetalleVentas().size();
                productos.setText(cantidad + " prod.");
                setGraphic(contenido);
            }
        });

        colTotal.setCellFactory(column -> new TableCell<Venta, Double>() {
            @Override
            protected void updateItem(Double item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty || item == null ? null : moneda(item));
            }
        });
    }

    private void configurarSeleccionVenta() {
        tlbVerVentas.getSelectionModel().selectedItemProperty().addListener((obs, anterior, nueva) -> {
            if (nueva != null) {
                mostrarDetalleVenta(nueva);
            } else {
                limpiarDetalle();
            }
        });
    }

    private void mostrarDetalleVenta(Venta venta) {
        List<DetalleVenta> detalles;
        try {
            detalles = detalleVentaDao.buscarPorVenta(venta);
        } catch (Exception e) {
            e.printStackTrace();
            mostrarError("Error", "No se pudieron cargar los productos de la venta seleccionada.");
            limpiarDetalle();
            return;
        }

        lblVentaNumero.setText("Venta #" + String.format("%04d", venta.getId()));
        lblVentaPago.setText(venta.getMedioPago() == null ? "---"
                : venta.esMixto()
                        ? "Mixto · Efectivo " + moneda(venta.montoEnEfectivo()) + " + QR " + moneda(venta.montoEnVirtual())
                        : venta.getMedioPago());
        lblVentaFecha.setText(venta.getFecha() == null ? ""
                : venta.getFecha().format(FORMATO_DIA_MES) + " · " + venta.getFecha().format(FORMATO_HORA));
        lblVentaTotal.setText(moneda(venta.getTotal()));

        boxItems.getChildren().clear();
        for (DetalleVenta detalle : detalles) {
            boxItems.getChildren().add(crearFilaItem(detalle));
        }

        lblSinSeleccion.setVisible(false);
        panelDetalle.setVisible(true);
    }

    private Node crearFilaItem(DetalleVenta detalle) {
        double peso = detalle.getPeso() == null ? 0.0 : detalle.getPeso();
        double precio = detalle.getPrecio() == null ? 0.0 : detalle.getPrecio();

        Label nombre = new Label(detalle.getProducto() != null ? detalle.getProducto().getNombre() : "Producto");
        nombre.getStyleClass().add("hv-item-name");
        nombre.setWrapText(true);

        Label calculo = new Label(formatoPeso.format(peso) + " kg × " + moneda(precio));
        calculo.getStyleClass().add("hv-item-sub");

        VBox textos = new VBox(1, nombre, calculo);
        HBox.setHgrow(textos, Priority.ALWAYS);

        Label total = new Label(moneda(Math.round(precio * peso)));
        total.getStyleClass().add("hv-item-total");
        total.setMinWidth(Region.USE_PREF_SIZE);

        HBox fila = new HBox(12, textos, total);
        fila.setAlignment(Pos.TOP_LEFT);
        return fila;
    }

    private void limpiarDetalle() {
        boxItems.getChildren().clear();
        panelDetalle.setVisible(false);
        lblSinSeleccion.setVisible(true);
    }

    /**
     * Punto único por el que pasan todas las búsquedas: carga las ventas del
     * período, limpia la selección/detalle y recalcula estadísticas.
     */
    private void mostrarVentas(List<Venta> ventas, LocalDate desde, LocalDate hasta) {
        periodoDesde = desde;
        periodoHasta = hasta;
        actualizarSubtitulo();

        colHora.setText(esPeriodoDeUnDia() ? "Hora" : "Fecha");
        observableVentas.setAll(ventas);
        tlbVerVentas.getSelectionModel().clearSelection();
        tlbVerVentas.refresh();
        limpiarDetalle();
        actualizarEstadisticas();
    }

    private void aplicarFiltroMedioPago() {
        String medio = cbxMedioPago.getValue();
        tlbVerVentas.getSelectionModel().clearSelection();
        if (medio == null || TODOS_LOS_PAGOS.equals(medio)) {
            ventasFiltradas.setPredicate(v -> true);
        } else if ("Efectivo".equals(medio)) {
            // Los pagos mixtos aparecen en los dos filtros (tienen una parte de cada uno)
            ventasFiltradas.setPredicate(v -> esEfectivo(v) || v.esMixto());
        } else {
            ventasFiltradas.setPredicate(v -> !esEfectivo(v));
        }
        actualizarEstadisticas();
    }

    private boolean esEfectivo(Venta venta) {
        return venta.getMedioPago() != null && venta.getMedioPago().equalsIgnoreCase("Efectivo");
    }

    private boolean esPeriodoDeUnDia() {
        return periodoDesde != null && periodoDesde.equals(periodoHasta);
    }

    private void actualizarSubtitulo() {
        LocalDate hoy = LocalDate.now();
        String texto;
        if (esPeriodoDeUnDia()) {
            texto = capitalizar(periodoDesde.format(FORMATO_DIA_LARGO));
            if (periodoDesde.getYear() != hoy.getYear()) {
                texto += " de " + periodoDesde.getYear();
            }
        } else if (periodoDesde.getDayOfMonth() == 1
                && periodoDesde.getMonth() == periodoHasta.getMonth()
                && periodoDesde.getYear() == periodoHasta.getYear()
                && (periodoHasta.equals(hoy) || periodoHasta.equals(periodoDesde.withDayOfMonth(periodoDesde.lengthOfMonth())))) {
            texto = capitalizar(periodoDesde.format(FORMATO_MES));
        } else {
            texto = "Del " + periodoDesde.format(FORMATO_FECHA) + " al " + periodoHasta.format(FORMATO_FECHA);
        }
        lblPeriodo.setText(texto);
    }

    private String capitalizar(String texto) {
        return texto.isEmpty() ? texto : Character.toUpperCase(texto.charAt(0)) + texto.substring(1);
    }

    private String moneda(double valor) {
        return "$" + formatoMoneda.format(valor);
    }

    @FXML
    public void buscarVentasDeHoy() {
        grupoPeriodo.selectToggle(tglHoy);
        LocalDate hoy = LocalDate.now();
        mostrarVentas(ventaDao.buscarVentasDeHoy(), hoy, hoy);
    }

    @FXML
    public void buscarVentasDeAyer() {
        LocalDate ayer = LocalDate.now().minusDays(1);
        mostrarVentas(ventaDao.buscarVentasPorFecha(ayer, ayer), ayer, ayer);
    }

    @FXML
    public void buscarVentasDeLaSemana() {
        LocalDate desde = LocalDate.now().with(DayOfWeek.MONDAY);
        LocalDate hasta = LocalDate.now();
        mostrarVentas(ventaDao.buscarVentasPorFecha(desde, hasta), desde, hasta);
    }

    @FXML
    public void buscarVentasDelMes() {
        LocalDate desde = LocalDate.now().withDayOfMonth(1);
        LocalDate hasta = LocalDate.now();
        mostrarVentas(ventaDao.buscarVentasPorFecha(desde, hasta), desde, hasta);
    }

    @FXML
    public void mostrarSelectorRango() {
        // Precarga el período actual para que sea fácil ajustarlo
        if (datePickerDesde.getValue() == null) {
            datePickerDesde.setValue(periodoDesde);
        }
        if (datePickerHasta.getValue() == null) {
            datePickerHasta.setValue(periodoHasta);
        }
        datePickerDesde.requestFocus();
    }

    @FXML
    public void buscarVentasPorFiltro() {
        LocalDate fechaDesde = datePickerDesde.getValue();
        LocalDate fechaHasta = datePickerHasta.getValue();

        if (fechaDesde == null || fechaHasta == null) {
            mostrarAdvertencia("Faltan fechas", "Seleccioná las fechas 'Desde' y 'Hasta' para buscar en un rango.");
            return;
        }

        if (fechaDesde.isAfter(fechaHasta)) {
            mostrarAdvertencia("Rango inválido", "La fecha 'Desde' no puede ser posterior a la fecha 'Hasta'.");
            return;
        }

        mostrarVentas(ventaDao.buscarVentasPorFecha(fechaDesde, fechaHasta), fechaDesde, fechaHasta);
    }

    @FXML
    public void eliminarVentaSeleccionada() {
        Venta ventaSeleccionada = tlbVerVentas.getSelectionModel().getSelectedItem();
        if (ventaSeleccionada == null) {
            return;
        }

        if (!validarPasswordAdmin()) {
            mostrarError("Acceso denegado", "Contraseña incorrecta.");
            return;
        }

        Alert confirmacion = new Alert(Alert.AlertType.CONFIRMATION);
        DialogoUtil.aplicarEstilo(confirmacion);
        confirmacion.setTitle("Confirmación de eliminación");
        confirmacion.setHeaderText("¿Eliminar esta venta?");
        confirmacion.setContentText("Se eliminará la venta del " + ventaSeleccionada.getFecha().format(FORMATO_FECHA)
                + " por " + moneda(ventaSeleccionada.getTotal())
                + " y se restaurará el stock de los productos vendidos. Esta acción no se puede deshacer.");

        Optional<ButtonType> resultado = confirmacion.showAndWait();
        if (resultado.isEmpty() || resultado.get() != ButtonType.OK) {
            return;
        }

        try {
            ventaDao.eliminarVenta(ventaSeleccionada.getId());

            // Sacamos la venta de la lista ya cargada en memoria: no hace falta
            // volver a consultar la base de datos para refrescar la tabla.
            observableVentas.remove(ventaSeleccionada);
            tlbVerVentas.getSelectionModel().clearSelection();
            limpiarDetalle();
            actualizarEstadisticas();

        } catch (Exception e) {
            e.printStackTrace();
            mostrarError("Error al eliminar", "No se pudo eliminar la venta seleccionada.\n\n" + e.getMessage());
        }
    }

    @FXML
    private void btnImprimirVenta() {
        imprimirVentaSeleccionada();
    }

    public void imprimirVentaSeleccionada() {
        Venta ventaSeleccionada = tlbVerVentas.getSelectionModel().getSelectedItem();
        if (ventaSeleccionada == null) {
            return;
        }

        try {
            List<DetalleVenta> detalles = detalleVentaDao.buscarPorVenta(ventaSeleccionada);

            imprimirEnSegundoPlano(btnImprimir, ComprobanteVenta.construir(ventaSeleccionada, detalles));

        } catch (Exception e) {
            e.printStackTrace();
            mostrarError("Error al imprimir", "No se pudo imprimir el ticket de la venta seleccionada.");
        }
    }

    /**
     * Envía un ticket a la impresora en un hilo aparte y deshabilita el botón
     * que disparó la impresión mientras dura el trabajo. El envío a la
     * impresora (job.print) puede bloquear varios segundos o minutos según el
     * spooler; hacerlo en el hilo de JavaFX congela la UI y hace que los
     * clicks repetidos del usuario, al no ver respuesta, queden encolados y
     * se disparen todos juntos apenas el hilo de UI se libera, imprimiendo el
     * ticket varias veces.
     */
    private void imprimirEnSegundoPlano(Node boton, String contenido) {
        if (boton != null) {
            boton.setDisable(true);
        }
        Thread hiloImpresion = new Thread(() -> {
            try {
                PrintTicket.print(contenido);
            } finally {
                if (boton != null) {
                    Platform.runLater(() -> boton.setDisable(false));
                }
            }
        });
        hiloImpresion.setDaemon(true);
        hiloImpresion.start();
    }

    private String centrar(String texto) {
        int ancho = 32;
        int espacios = Math.max(0, (ancho - texto.length()) / 2);
        return " ".repeat(espacios) + texto;
    }

    @FXML
    public void exportarVentasExcel() {
        // Exporta las ventas virtuales del período elegido en pantalla
        LocalDate fechaDesde = periodoDesde;
        LocalDate fechaHasta = periodoHasta;

        try {
            List<Venta> ventas = ventaDao.buscarVentasPorFecha(fechaDesde, fechaHasta);

            // Solo ventas virtuales
            ventas = ventas.stream()
                    .filter(v -> {
                        String medio = v.getMedioPago();
                        return medio != null
                                && (medio.equalsIgnoreCase("Virtual")
                                || v.esMixto()
                                || medio.toLowerCase().contains("virtual")
                                || medio.toLowerCase().contains("transferencia")
                                || medio.toLowerCase().contains("tarjeta"));
                    })
                    .toList();

            if (ventas.isEmpty()) {
                mostrarAdvertencia("Sin resultados", "No existen ventas virtuales en ese rango de fechas.");
                return;
            }

            FileChooser fileChooser = new FileChooser();
            fileChooser.setTitle("Guardar reporte de ventas virtuales");
            fileChooser.getExtensionFilters().add(
                    new FileChooser.ExtensionFilter("Excel (*.xlsx)", "*.xlsx")
            );
            fileChooser.setInitialFileName("VentasVirtuales_" + fechaDesde + "_" + fechaHasta + ".xlsx");

            File archivo = fileChooser.showSaveDialog(tlbVerVentas.getScene().getWindow());

            if (archivo != null) {
                ExportarExcel.exportarVentas(ventas, archivo.getAbsolutePath());
            }

        } catch (Exception e) {
            e.printStackTrace();
            mostrarError("Error al exportar", "No se pudo generar el archivo Excel.\n\n" + e.getMessage());
        }
    }

    /**
     * Devuelve el período actualmente mostrado en pantalla (Hoy, Ayer,
     * Esta semana, Este mes o el rango elegido) para usarlo en el cierre de caja.
     */
    private LocalDate[] obtenerRangoFiltroActual() {
        return new LocalDate[]{periodoDesde, periodoHasta};
    }

    /**
     * Imprime un ticket con el resumen de ventas del período filtrado,
     * desglosado por franja horaria del local (mañana 08:30-13:00 y tarde
     * 17:00-21:00), más el total gastado en el mismo período.
     */
    @FXML
    public void imprimirReporteCompleto(ActionEvent event) {
        LocalDate[] rango = obtenerRangoFiltroActual();
        LocalDate desde = rango[0];
        LocalDate hasta = rango[1];

        try {
            List<Venta> ventas = ventaDao.buscarVentasPorFecha(desde, hasta);
            if (ventas == null || ventas.isEmpty()) {
                mostrarAdvertencia("Sin datos", "No hay ventas registradas en el período seleccionado.");
                return;
            }

            Map<String, ResumenFranja> porFranja = new LinkedHashMap<>();
            porFranja.put(HorarioLocal.MANIANA, new ResumenFranja());
            porFranja.put(HorarioLocal.TARDE, new ResumenFranja());
            porFranja.put(HorarioLocal.FUERA_DE_HORARIO, new ResumenFranja());

            double totalGeneral = 0.0;
            for (Venta venta : ventas) {
                LocalDateTime fecha = venta.getFecha();
                String franja = HorarioLocal.determinarFranja(fecha != null ? fecha.toLocalTime() : null);
                ResumenFranja resumen = porFranja.get(franja);

                double total = venta.getTotal();
                resumen.cantidad++;
                resumen.total += total;
                // Pago mixto: cada parte va a su columna
                resumen.efectivo += venta.montoEnEfectivo();
                resumen.virtual += venta.montoEnVirtual();
                totalGeneral += total;
            }

            double totalGastado = gastoDao.sumarGastosPorRango(desde, hasta);

            StringBuilder ticket = new StringBuilder();

            ticket.append(centrar("CARNICERIA MONCHO")).append("\n");
            ticket.append(centrar("REPORTE POR HORARIO")).append("\n");
            ticket.append(lineaSeparadora()).append("\n");

            if (desde.equals(hasta)) {
                ticket.append("Fecha: ").append(desde.format(FORMATO_FECHA)).append("\n");
            } else {
                ticket.append("Desde: ").append(desde.format(FORMATO_FECHA)).append("\n");
                ticket.append("Hasta: ").append(hasta.format(FORMATO_FECHA)).append("\n");
            }
            ticket.append("Impreso: ")
                    .append(LocalDateTime.now().format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm")))
                    .append("\n");
            ticket.append(lineaSeparadora()).append("\n");

            ticket.append("MAÑANA (08:30 a 13:00)\n");
            ticket.append(fila(porFranja.get(HorarioLocal.MANIANA))).append("\n");
            ticket.append(lineaSeparadora()).append("\n");

            ticket.append("TARDE (17:00 a 21:00)\n");
            ticket.append(fila(porFranja.get(HorarioLocal.TARDE))).append("\n");
            ticket.append(lineaSeparadora()).append("\n");

            ResumenFranja fueraDeHorario = porFranja.get(HorarioLocal.FUERA_DE_HORARIO);
            if (fueraDeHorario.cantidad > 0) {
                ticket.append("FUERA DE HORARIO\n");
                ticket.append(fila(fueraDeHorario)).append("\n");
                ticket.append(lineaSeparadora()).append("\n");
            }

            ticket.append("TOTAL GENERAL\n");
            ticket.append(filaMontos("Total vendido", "$" + formatoMoneda.format(totalGeneral))).append("\n");
            ticket.append(filaMontos("Total gastado", "$" + formatoMoneda.format(totalGastado))).append("\n");
            ticket.append("\n\n\n");

            imprimirEnSegundoPlano((Node) event.getSource(), ticket.toString());

        } catch (Exception e) {
            e.printStackTrace();
            mostrarError("Error de impresión", "No se pudo imprimir el reporte completo.");
        }
    }

    /** Acumulador de cantidad/total/efectivo/virtual de ventas dentro de una franja horaria. */
    private static class ResumenFranja {
        int cantidad = 0;
        double total = 0.0;
        double efectivo = 0.0;
        double virtual = 0.0;
    }

    private String fila(ResumenFranja resumen) {
        StringBuilder sb = new StringBuilder();
        sb.append(filaMontos("Ventas", String.valueOf(resumen.cantidad))).append("\n");
        sb.append(filaMontos("Total vendido", "$" + formatoMoneda.format(resumen.total))).append("\n");
        sb.append(filaMontos("  Efectivo", "$" + formatoMoneda.format(resumen.efectivo))).append("\n");
        sb.append(filaMontos("  Virtual", "$" + formatoMoneda.format(resumen.virtual)));
        return sb.toString();
    }

    /** Alinea etiqueta a la izquierda y valor a la derecha en el ancho de ticket (32 columnas). */
    private String filaMontos(String etiqueta, String valor) {
        int ancho = 32;
        int espacios = Math.max(1, ancho - etiqueta.length() - valor.length());
        return etiqueta + " ".repeat(espacios) + valor;
    }

    private String lineaSeparadora() {
        return "-".repeat(32);
    }

    private void actualizarEstadisticas() {
        double totalVendido = 0.0;
        double totalEfectivo = 0.0;
        double totalVirtual = 0.0;

        for (Venta venta : ventasFiltradas) {
            double totalVenta = venta.getTotal() == null ? 0.0 : venta.getTotal();
            totalVendido += totalVenta;

            // Pago mixto: cada parte suma donde va. Cualquier medio que no sea
            // efectivo (virtual, transferencia, tarjeta) cuenta como virtual.
            totalEfectivo += venta.montoEnEfectivo();
            totalVirtual += venta.montoEnVirtual();
        }

        lblCantidadVentas.setText(String.valueOf(ventasFiltradas.size()));
        lblTotalVendido.setText(moneda(totalVendido));
        lblEfectivo.setText(moneda(totalEfectivo));
        lblVirtual.setText(moneda(totalVirtual));
    }

    private boolean validarPasswordAdmin() {
        TextInputDialog dialog = new TextInputDialog();
        DialogoUtil.aplicarEstilo(dialog);
        dialog.setTitle("Autorización requerida");
        dialog.setHeaderText(null);
        dialog.setContentText("Ingrese contraseña:");
        dialog.getEditor().setPromptText("Contraseña");

        Optional<String> resultado = dialog.showAndWait();
        return resultado.isPresent() && resultado.get().equals(PASSWORD_ADMIN);
    }

    private void mostrarAdvertencia(String titulo, String mensaje) {
        Alert alert = new Alert(Alert.AlertType.WARNING);
        DialogoUtil.aplicarEstilo(alert);
        alert.setTitle(titulo);
        alert.setHeaderText(null);
        alert.setContentText(mensaje);
        alert.showAndWait();
    }

    private void mostrarError(String titulo, String mensaje) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        DialogoUtil.aplicarEstilo(alert);
        alert.setTitle(titulo);
        alert.setHeaderText(null);
        alert.setContentText(mensaje);
        alert.showAndWait();
    }
}
