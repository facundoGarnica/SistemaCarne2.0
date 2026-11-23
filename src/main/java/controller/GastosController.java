package controller;

import Util.DialogoUtil;
import Util.HorarioLocal;
import dao.AdelantoDAO;
import dao.ArqueoCajaDAO;
import model.ArqueoCaja;
import dao.DetalleVentaDAO;
import dao.EmpleadoDAO;
import dao.GastoDAO;
import dao.VentaDAO;
import java.text.DecimalFormat;
import javafx.application.Platform;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.control.cell.PropertyValueFactory;
import model.DetalleVenta;
import model.Gastos;
import model.PrintTicket;
import model.Venta;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class GastosController {

    // Tablas y Columnas (Gastos)
    @FXML private DatePicker dpFechaGasto, dpDesdeGasto, dpHastaGasto;
    @FXML private ComboBox<String> cbMedioPagoGasto, cbCategoriaGasto, cbFiltroCategoriaGasto;
    @FXML private ToggleGroup grupoPeriodoGastos;
    @FXML private ToggleButton tglGastosRango;
    @FXML private javafx.scene.layout.HBox boxRangoGastos;
    @FXML private Label lblTotalGastado, lblGastadoEfectivo, lblGastadoVirtual;
    @FXML private Button btnGuardarGasto, btnCancelarGasto;
    @FXML private TableColumn<Gastos, String> colGastoCategoria;

    private static final String TODAS_LAS_CATEGORIAS = "Todas las categorías";
    private static final List<String> CATEGORIAS_GASTO = List.of("Mercadería", "Servicios", "Alquiler", "Impuestos", "Otros");
    private static final DateTimeFormatter FORMATO_DIA_MES = DateTimeFormatter.ofPattern("dd/MM");
    /** Gastos del período cargado (sin filtrar por categoría). */
    private final javafx.collections.ObservableList<Gastos> gastosPeriodo = FXCollections.observableArrayList();
    private final javafx.collections.transformation.FilteredList<Gastos> gastosFiltrados = new javafx.collections.transformation.FilteredList<>(gastosPeriodo, g -> true);
    /** Gasto que se está editando (el seleccionado en la tabla) o null si es un alta. */
    private Gastos gastoEnEdicion;
    @FXML private TextField txtDescripcionGasto, txtMontoGasto;
    @FXML private TableView<Gastos> tvGastos;
    @FXML private TableColumn<Gastos, LocalDate> colGastoFecha;
    @FXML private TableColumn<Gastos, String> colGastoDesc, colGastoMedioPago;
    @FXML private TableColumn<Gastos, Double> colGastoMonto;
    @FXML private Button btnBorrarGasto;

    // Tablas y Columnas (Reportes y Balance)
    @FXML private ToggleButton tbLunes, tbMartes, tbMiercoles, tbJueves, tbViernes, tbSabado, tbDomingo;
    @FXML private Label lblSemanaDias, lblMostrandoDias, lblSueldosNota;
    @FXML private Hyperlink lnkTodaLaSemana;
    @FXML private Button btnSemanaSiguienteBalance;
    @FXML private DatePicker dpDesdeBalance, dpHastaBalance;
    @FXML private javafx.scene.layout.HBox boxDiasBalance;
    /** true cuando se busca un período o día puntual con Desde/Hasta (se ocultan las tarjetas de días). */
    private boolean modoRangoBalance = false;
    @FXML private Label lblBalVentasEf, lblBalVentasVi, lblBalVentasTot;
    @FXML private Label lblBalManianaEf, lblBalManianaVi, lblBalManianaTot;
    @FXML private Label lblBalTardeEf, lblBalTardeVi, lblBalTardeTot;
    @FXML private Label lblBalGastosEf, lblBalGastosVi, lblBalGastosTot;
    @FXML private Label lblBalSueldosEf, lblBalSueldosVi, lblBalSueldosTot;
    @FXML private Label lblBalAdelEf, lblBalAdelVi, lblBalAdelTot;
    @FXML private Label lblBalResEf, lblBalResVi, lblBalResTot;
    /** Lunes de la semana que muestra el balance. */
    private LocalDate lunesBalance = LocalDate.now().with(DayOfWeek.MONDAY);
    /** Período del balance cuando no hay días marcados (lunes a domingo, o hasta hoy si es la semana actual). */
    private LocalDate inicioReporte, finReporte;
    private static final String[] DIAS_CORTOS_BALANCE = {"Lun", "Mar", "Mié", "Jue", "Vie", "Sáb", "Dom"};
    private ToggleButton[] botonesDias; // índice 0 = lunes ... 6 = domingo

    // Últimos totales calculados en generarReporte(), reutilizados al imprimir el resumen
    private double repTotalVentas, repVentasEfectivo, repVentasVirtual;
    private double repTotalGastos, repGastosEfectivo, repGastosTransferencia;
    private double repTotalSueldos, repSueldosEfectivo, repSueldosTransferencia;
    private boolean repSueldosPendientes; // el rango no incluye sábado: los sueldos impactan recién ese día
    private double repTotalAdelantos, repAdelantosEfectivo, repAdelantosTransferencia;
    private double repQuedaEfectivo, repQuedaVirtual, repBalanceFinal;

    // Tablas y Columnas (Reportes Productos)
    @FXML private DatePicker dpInicioProductos, dpFinProductos;
    @FXML private Label lblTotalKilosProductos, lblTotalFacturadoProductos, lblProductoTop;
    @FXML private Label lblCategoriaTop, lblTituloDetalleProductos, lblResumenDetalleProductos;
    @FXML private TableView<ProductoReporte> tvReporteProductos;
    @FXML private TableColumn<ProductoReporte, ProductoReporte> colRPPosicion, colRPNombre;
    @FXML private TableColumn<ProductoReporte, Double> colRPKilos, colRPPrecio, colRPTotal;
    @FXML private ListView<CategoriaReporte> lvCategoriasProductos;
    @FXML private Button btnImprimirCategoria;
    @FXML private ToggleGroup grupoPeriodoProductos;
    @FXML private ToggleButton tglProductosRango;
    @FXML private javafx.scene.layout.HBox boxRangoProductos;
    /** Categorías del último reporte (sin la fila "Todas"), para el ticket impreso. */
    private List<CategoriaReporte> categoriasReporte = new ArrayList<>();
    // Todos los productos del último reporte; la tabla de detalle muestra este listado filtrado por la categoría elegida
    private final ObservableList<ProductoReporte> productosReporte = FXCollections.observableArrayList();
    private final FilteredList<ProductoReporte> productosFiltrados = new FilteredList<>(productosReporte);

    // --- DAOs ---
    private final GastoDAO gastoDAO = new GastoDAO();
    private final VentaDAO ventaDAO = new VentaDAO();
    private final DetalleVentaDAO detalleVentaDAO = new DetalleVentaDAO();
    private final EmpleadoDAO empleadoDAO = new EmpleadoDAO();
    private final AdelantoDAO adelantoDAO = new AdelantoDAO();

    private final DecimalFormat formatoMoneda = new DecimalFormat("#,##0", java.text.DecimalFormatSymbols.getInstance(new java.util.Locale("es", "AR")));
    private final DecimalFormat formatoKilos = new DecimalFormat("#,##0.00");

    @FXML
    public void initialize() {
        configurarTablas();
        cargarDatosIniciales();

        dpFechaGasto.setValue(LocalDate.now());
        dpDesdeGasto.setValue(LocalDate.now().with(DayOfWeek.MONDAY));
        dpHastaGasto.setValue(LocalDate.now());
        configurarGastos();
        dpInicioProductos.setValue(LocalDate.now().with(DayOfWeek.MONDAY));
        dpFinProductos.setValue(LocalDate.now());

        botonesDias = new ToggleButton[] { tbLunes, tbMartes, tbMiercoles, tbJueves, tbViernes, tbSabado, tbDomingo };
        cambiarSemanaBalance(LocalDate.now().with(DayOfWeek.MONDAY));

        filtrarGastos();
        generarReporteProductos();

        // Al elegir un gasto se carga en el formulario para editarlo
        tvGastos.getSelectionModel().selectedItemProperty().addListener((obs, oldSelection, seleccionado) -> {
            btnBorrarGasto.setDisable(seleccionado == null);
            if (seleccionado == null && gastoEnEdicion != null) {
                // Se deseleccionó el gasto que se estaba editando: el formulario vuelve a "alta"
                limpiarCamposGasto();
            }
            gastoEnEdicion = seleccionado;

            if (seleccionado != null) {
                dpFechaGasto.setValue(seleccionado.getFecha());
                txtDescripcionGasto.setText(seleccionado.getDescripcion());
                cbCategoriaGasto.setValue(categoriaDe(seleccionado));
                cbMedioPagoGasto.setValue(seleccionado.getMedioPago());
                txtMontoGasto.setText(formatoMoneda.format(seleccionado.getMonto() != null ? seleccionado.getMonto() : 0.0));
            }
            actualizarModoFormularioGasto();
        });
    }

    /** Pills de período, filtro por categoría y columnas propias de la pestaña Gastos. */
    private void configurarGastos() {
        tvGastos.setItems(gastosFiltrados);

        grupoPeriodoGastos.selectedToggleProperty().addListener((obs, anterior, nuevo) -> {
            if (nuevo == null && anterior != null) {
                anterior.setSelected(true);
            }
            boolean esRango = grupoPeriodoGastos.getSelectedToggle() == tglGastosRango;
            boxRangoGastos.setVisible(esRango);
            boxRangoGastos.setManaged(esRango);
        });

        cbCategoriaGasto.setItems(FXCollections.observableArrayList(CATEGORIAS_GASTO));
        cbCategoriaGasto.getSelectionModel().selectFirst();

        List<String> filtros = new ArrayList<>();
        filtros.add(TODAS_LAS_CATEGORIAS);
        filtros.addAll(CATEGORIAS_GASTO);
        cbFiltroCategoriaGasto.setItems(FXCollections.observableArrayList(filtros));
        cbFiltroCategoriaGasto.getSelectionModel().selectFirst();
        cbFiltroCategoriaGasto.valueProperty().addListener((obs, anterior, nuevo) -> aplicarFiltroCategoriaGasto());

        colGastoFecha.setCellFactory(column -> new TableCell<>() {
            @Override
            protected void updateItem(LocalDate item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty || item == null ? null : item.format(FORMATO_DIA_MES));
            }
        });
        colGastoCategoria.setCellValueFactory(cellData -> new SimpleStringProperty(categoriaDe(cellData.getValue())));
        colGastoCategoria.setCellFactory(column -> new TableCell<>() {
            private final Label chip = new Label();
            {
                chip.getStyleClass().add("ps-chip-gris");
            }

            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                setText(null);
                if (empty || item == null) {
                    setGraphic(null);
                    return;
                }
                chip.setText(item);
                setGraphic(chip);
            }
        });
    }

    private String categoriaDe(Gastos gasto) {
        return gasto.getCategoria() == null || gasto.getCategoria().isBlank() ? "Otros" : gasto.getCategoria();
    }

    private void configurarTablas() {
        // Configuración tabla Gastos
        colGastoFecha.setCellValueFactory(cellData -> new SimpleObjectProperty<>(cellData.getValue().getFecha()));
        colGastoDesc.setCellValueFactory(cellData -> new SimpleStringProperty(cellData.getValue().getDescripcion()));
        colGastoMedioPago.setCellValueFactory(cellData -> new SimpleStringProperty(cellData.getValue().getMedioPago()));
        colGastoMonto.setCellValueFactory(cellData -> new SimpleObjectProperty<>(cellData.getValue().getMonto()));
        colGastoMonto.setCellFactory(column -> celdaMoneda());

        // Configuración tabla Reportes Productos
        colRPPosicion.setCellValueFactory(cellData -> new SimpleObjectProperty<>(cellData.getValue()));
        colRPPosicion.setCellFactory(column -> new TableCell<>() {
            @Override
            protected void updateItem(ProductoReporte item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty || item == null ? null : String.valueOf(getIndex() + 1));
            }
        });
        // Producto: nombre + "· categoría" en gris
        colRPNombre.setCellValueFactory(cellData -> new SimpleObjectProperty<>(cellData.getValue()));
        colRPNombre.setCellFactory(column -> new TableCell<>() {
            private final Label nombre = new Label();
            private final Label categoria = new Label();
            private final HBox contenido = new HBox(nombre, categoria);
            {
                nombre.getStyleClass().add("ps-prod-nombre");
                categoria.getStyleClass().add("ps-prod-cat");
                contenido.setAlignment(Pos.CENTER_LEFT);
                nombre.setMinWidth(Region.USE_PREF_SIZE);
            }

            @Override
            protected void updateItem(ProductoReporte item, boolean empty) {
                super.updateItem(item, empty);
                setText(null);
                if (empty || item == null) {
                    setGraphic(null);
                    return;
                }
                nombre.setText(item.getNombre());
                categoria.setText(" · " + categoriaDe(item.getTipo()));
                setGraphic(contenido);
            }
        });
        colRPKilos.setCellValueFactory(new PropertyValueFactory<>("kilos"));
        colRPPrecio.setCellValueFactory(new PropertyValueFactory<>("precioPromedio"));
        colRPTotal.setCellValueFactory(new PropertyValueFactory<>("total"));
        colRPKilos.setCellFactory(column -> celdaKilos());
        colRPPrecio.setCellFactory(column -> celdaMoneda());
        colRPTotal.setCellFactory(column -> celdaMoneda());
        tvReporteProductos.setItems(productosFiltrados);

        // Lista de categorías: "Todas" + cada categoría con su total, kilos, % y barra
        lvCategoriasProductos.setCellFactory(lv -> new ListCell<>() {
            private final Label nombre = new Label();
            private final Label total = new Label();
            private final Label kilos = new Label();
            private final Label porcentaje = new Label();
            private final Region barraRelleno = new Region();
            private final javafx.scene.layout.StackPane barra = new javafx.scene.layout.StackPane(barraRelleno);
            private final HBox filaKilos;
            private final javafx.scene.layout.VBox contenido;
            {
                nombre.getStyleClass().add("ps-cat-nombre");
                total.getStyleClass().add("ps-cat-total");
                kilos.getStyleClass().add("ps-cat-sub");
                porcentaje.getStyleClass().add("ps-cat-sub");
                barra.getStyleClass().add("ps-cat-barra");
                barraRelleno.getStyleClass().add("ps-cat-barra-relleno");
                barra.setAlignment(Pos.CENTER_LEFT);
                barraRelleno.setMaxWidth(Region.USE_PREF_SIZE);

                Region e1 = new Region();
                HBox.setHgrow(e1, Priority.ALWAYS);
                HBox filaNombre = new HBox(nombre, e1, total);
                filaNombre.setAlignment(Pos.CENTER_LEFT);
                Region e2 = new Region();
                HBox.setHgrow(e2, Priority.ALWAYS);
                filaKilos = new HBox(kilos, e2, porcentaje);
                contenido = new javafx.scene.layout.VBox(4, filaNombre, filaKilos, barra);
                javafx.scene.layout.VBox.setMargin(barra, new javafx.geometry.Insets(6, 0, 0, 0));
                // El ancho del relleno es proporcional al porcentaje de la categoría
                barra.widthProperty().addListener((obs, a, b) -> ajustarBarra());
            }

            private void ajustarBarra() {
                CategoriaReporte item = getItem();
                double pct = item == null || item.categoria() == null ? 0 : item.porcentaje();
                barraRelleno.setPrefWidth(Math.max(pct > 0 ? 8 : 0, barra.getWidth() * pct / 100.0));
            }

            @Override
            protected void updateItem(CategoriaReporte item, boolean empty) {
                super.updateItem(item, empty);
                setText(null);
                if (empty || item == null) {
                    setGraphic(null);
                    return;
                }
                boolean esTodas = item.categoria() == null;
                nombre.setText(esTodas ? "Todas" : item.categoria());
                total.setText("$" + formatoMoneda.format(item.total()));
                kilos.setText(formatoKilosCorto(item.kilos()) + " kg");
                porcentaje.setText(Math.round(item.porcentaje()) + "%");
                filaKilos.setVisible(!esTodas);
                filaKilos.setManaged(!esTodas);
                barra.setVisible(!esTodas);
                barra.setManaged(!esTodas);
                ajustarBarra();
                setGraphic(contenido);
            }
        });
        // Al elegir una categoría, el detalle muestra solo sus productos
        lvCategoriasProductos.getSelectionModel().selectedItemProperty()
                .addListener((obs, anterior, seleccionada) -> aplicarFiltroCategoria(seleccionada));

        grupoPeriodoProductos.selectedToggleProperty().addListener((obs, anterior, nuevo) -> {
            if (nuevo == null && anterior != null) {
                anterior.setSelected(true);
            }
            boolean esRango = grupoPeriodoProductos.getSelectedToggle() == tglProductosRango;
            boxRangoProductos.setVisible(esRango);
            boxRangoProductos.setManaged(esRango);
        });

    }

    private <T> TableCell<T, Double> celdaMoneda() {
        return new TableCell<>() {
            @Override
            protected void updateItem(Double item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty ? null : (item == null ? "-" : "$" + formatoMoneda.format(item)));
            }
        };
    }

    private <T> TableCell<T, Double> celdaKilos() {
        return new TableCell<>() {
            @Override
            protected void updateItem(Double item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty || item == null ? null : formatoKilos.format(item) + " kg");
            }
        };
    }

    private void cargarDatosIniciales() {
        try {
            cbMedioPagoGasto.setItems(FXCollections.observableArrayList("Efectivo", "Transferencia"));
            cbMedioPagoGasto.getSelectionModel().selectFirst();
        } catch (Exception e) {
            System.err.println("Error al cargar datos iniciales: " + e.getMessage());
        }
    }

    // --- Lógica de Gastos ---

    /** Alta de un gasto nuevo, o guarda los cambios del gasto seleccionado. */
    @FXML
    private void guardarGasto() {
        LocalDate fecha = dpFechaGasto.getValue();
        String medioPago = cbMedioPagoGasto.getValue();
        String categoria = cbCategoriaGasto.getValue();

        if (fecha == null || medioPago == null || categoria == null) {
            mostrarAdvertencia("Faltan datos", "Completá la fecha, la categoría y el medio de pago del gasto.");
            return;
        }

        Double monto = leerMontoGasto();
        if (monto == null) {
            return;
        }

        boolean esEdicion = gastoEnEdicion != null;
        Gastos gasto = esEdicion ? gastoEnEdicion : new Gastos();
        gasto.setFecha(fecha);
        gasto.setDescripcion(txtDescripcionGasto.getText());
        gasto.setCategoria(categoria);
        gasto.setMedioPago(medioPago);
        gasto.setMonto(monto);

        if (esEdicion) {
            gastoDAO.actualizar(gasto);
        } else {
            gastoDAO.guardar(gasto);
        }

        tvGastos.getSelectionModel().clearSelection();
        limpiarCamposGasto();
        recargarPeriodoGastos();
    }

    @FXML
    private void cancelarEdicionGasto() {
        tvGastos.getSelectionModel().clearSelection();
        limpiarCamposGasto();
    }

    /** Lee el monto aceptando "18500", "18.500" o "$ 18.500". Devuelve null (y avisa) si no es válido. */
    private Double leerMontoGasto() {
        String texto = txtMontoGasto.getText() == null ? "" : txtMontoGasto.getText()
                .replace("$", "").replace(" ", "").replace(".", "").replace(",", ".").trim();
        if (texto.isEmpty()) {
            mostrarAdvertencia("Falta el monto", "Ingresá el monto del gasto.");
            return null;
        }
        try {
            double monto = Double.parseDouble(texto);
            if (monto <= 0) {
                mostrarAdvertencia("Monto inválido", "El monto del gasto tiene que ser mayor a cero.");
                return null;
            }
            return monto;
        } catch (NumberFormatException e) {
            mostrarError("Monto inválido", "El monto del gasto debe ser un número válido.");
            return null;
        }
    }

    @FXML
    private void borrarGasto() {
        Gastos seleccionado = tvGastos.getSelectionModel().getSelectedItem();
        if (seleccionado == null) {
            return;
        }

        boolean confirmado = mostrarConfirmacion(
                "Confirmar eliminación",
                "¿Eliminar el gasto \"" + seleccionado.getDescripcion() + "\" por $"
                + formatoMoneda.format(seleccionado.getMonto()) + "?"
        );
        if (!confirmado) {
            return;
        }

        gastoDAO.eliminar(seleccionado.getId());
        tvGastos.getSelectionModel().clearSelection();
        limpiarCamposGasto();
        recargarPeriodoGastos();
    }

    @FXML
    private void filtrarGastos() {
        LocalDate desde = dpDesdeGasto.getValue();
        LocalDate hasta = dpHastaGasto.getValue();

        if (desde == null || hasta == null) {
            mostrarAdvertencia("Faltan fechas", "Seleccioná las fechas 'Desde' y 'Hasta' para filtrar los gastos.");
            return;
        }
        if (desde.isAfter(hasta)) {
            mostrarAdvertencia("Rango inválido", "La fecha 'Desde' no puede ser posterior a la fecha 'Hasta'.");
            return;
        }

        mostrarGastos(gastoDAO.buscarPorRango(desde, hasta));
    }

    /** Vuelve a cargar el período que se está viendo (después de guardar o borrar). */
    private void recargarPeriodoGastos() {
        filtrarGastos();
    }

    /** Carga los gastos del período (más recientes primero) y recalcula totales. */
    private void mostrarGastos(List<Gastos> lista) {
        List<Gastos> gastos = lista != null ? new ArrayList<>(lista) : new ArrayList<>();
        gastos.sort(java.util.Comparator.comparing(Gastos::getFecha,
                java.util.Comparator.nullsLast(java.util.Comparator.reverseOrder())));
        gastosPeriodo.setAll(gastos);
        actualizarTotalesGastos();
    }

    private void aplicarFiltroCategoriaGasto() {
        String categoria = cbFiltroCategoriaGasto.getValue();
        tvGastos.getSelectionModel().clearSelection();
        if (categoria == null || TODAS_LAS_CATEGORIAS.equals(categoria)) {
            gastosFiltrados.setPredicate(g -> true);
        } else {
            gastosFiltrados.setPredicate(g -> categoria.equals(categoriaDe(g)));
        }
        actualizarTotalesGastos();
    }

    /** Total, efectivo y virtual de los gastos visibles (respeta el filtro de categoría). */
    private void actualizarTotalesGastos() {
        double total = 0, efectivo = 0;
        for (Gastos g : gastosFiltrados) {
            double monto = g.getMonto() != null ? g.getMonto() : 0.0;
            total += monto;
            if ("Efectivo".equalsIgnoreCase(g.getMedioPago())) {
                efectivo += monto;
            }
        }
        lblTotalGastado.setText("$" + formatoMoneda.format(total));
        lblGastadoEfectivo.setText("$" + formatoMoneda.format(efectivo));
        lblGastadoVirtual.setText("$" + formatoMoneda.format(total - efectivo));
    }

    /** Cambia el botón del formulario según sea un alta o la edición del gasto seleccionado. */
    private void actualizarModoFormularioGasto() {
        boolean editando = gastoEnEdicion != null;
        btnGuardarGasto.setText(editando ? "Guardar cambios" : "Guardar");
        btnCancelarGasto.setVisible(editando);
        btnCancelarGasto.setManaged(editando);
    }

    @FXML
    private void mostrarGastosDeHoy() {
        dpDesdeGasto.setValue(LocalDate.now());
        dpHastaGasto.setValue(LocalDate.now());
        filtrarGastos();
    }

    @FXML
    private void mostrarSemanalGastos() {
        dpDesdeGasto.setValue(LocalDate.now().with(DayOfWeek.MONDAY));
        dpHastaGasto.setValue(LocalDate.now());
        filtrarGastos();
    }

    @FXML
    private void mostrarGastosDelMes() {
        dpDesdeGasto.setValue(LocalDate.now().withDayOfMonth(1));
        dpHastaGasto.setValue(LocalDate.now());
        filtrarGastos();
    }

    @FXML
    private void mostrarSelectorRangoGastos() {
        dpDesdeGasto.requestFocus();
    }

    // --- Lógica de Reportes y Balances ---

    private static final String[] MESES_BALANCE = {"enero", "febrero", "marzo", "abril", "mayo", "junio",
            "julio", "agosto", "septiembre", "octubre", "noviembre", "diciembre"};

    @FXML
    private void semanaAnteriorBalance() {
        cambiarSemanaBalance(lunesBalance.minusWeeks(1));
    }

    @FXML
    private void semanaSiguienteBalance() {
        cambiarSemanaBalance(lunesBalance.plusWeeks(1));
    }

    /** "Reporte semanal": vuelve a la semana actual completa. */
    @FXML
    private void mostrarReporteSemanal() {
        cambiarSemanaBalance(LocalDate.now().with(DayOfWeek.MONDAY));
    }

    /**
     * Cambia la semana del balance: arma las tarjetas de los días (con lo
     * vendido cada día), desmarca los días y recalcula toda la semana.
     */
    private void cambiarSemanaBalance(LocalDate lunes) {
        LocalDate hoy = LocalDate.now();
        lunesBalance = lunes;
        LocalDate domingo = lunes.plusDays(6);

        // Sin días marcados se toma la semana entera (hasta hoy si es la semana en curso)
        inicioReporte = lunes;
        finReporte = domingo.isAfter(hoy) && !lunes.isAfter(hoy) ? hoy : domingo;

        // No se puede avanzar más allá de la semana actual
        btnSemanaSiguienteBalance.setDisable(!domingo.isBefore(hoy.with(DayOfWeek.MONDAY).plusDays(6)));

        // Vuelve al modo semana: se muestran las tarjetas de días
        modoRangoBalance = false;
        boxDiasBalance.setVisible(true);
        boxDiasBalance.setManaged(true);
        dpDesdeBalance.setValue(inicioReporte);
        dpHastaBalance.setValue(finReporte);

        actualizarLabelSemana();
        actualizarTarjetasDias();
        desmarcarDias();
        generarReporte();
    }

    /**
     * Botón "Buscar": balance de un período libre (Desde/Hasta) o de un día
     * puntual (misma fecha en las dos). Oculta las tarjetas de días.
     */
    @FXML
    private void buscarRangoBalance() {
        LocalDate desde = dpDesdeBalance.getValue();
        LocalDate hasta = dpHastaBalance.getValue();

        if (desde == null && hasta == null) {
            mostrarAdvertencia("Faltan fechas", "Elegí la fecha 'Desde' y la fecha 'Hasta' (o solo una para ver un día).");
            return;
        }
        // Si se completa una sola fecha, se busca ese día
        if (desde == null) desde = hasta;
        if (hasta == null) hasta = desde;
        if (desde.isAfter(hasta)) {
            mostrarAdvertencia("Rango inválido", "La fecha 'Desde' no puede ser posterior a la fecha 'Hasta'.");
            return;
        }
        dpDesdeBalance.setValue(desde);
        dpHastaBalance.setValue(hasta);

        modoRangoBalance = true;
        inicioReporte = desde;
        finReporte = hasta;
        lunesBalance = hasta.with(DayOfWeek.MONDAY);
        btnSemanaSiguienteBalance.setDisable(false);

        desmarcarDias();
        boxDiasBalance.setVisible(false);
        boxDiasBalance.setManaged(false);

        lblSemanaDias.setText(descripcionPeriodoBalance());
        generarReporte();
    }

    /** "Viernes 6 de noviembre" para un día, o "Del 01/10/2026 al 15/10/2026" para un período. */
    private String descripcionPeriodoBalance() {
        if (inicioReporte.equals(finReporte)) {
            String[] dias = {"Lunes", "Martes", "Miércoles", "Jueves", "Viernes", "Sábado", "Domingo"};
            String texto = dias[inicioReporte.getDayOfWeek().getValue() - 1] + " " + inicioReporte.getDayOfMonth()
                    + " de " + MESES_BALANCE[inicioReporte.getMonthValue() - 1];
            if (inicioReporte.getYear() != LocalDate.now().getYear()) {
                texto += " de " + inicioReporte.getYear();
            }
            return texto;
        }
        DateTimeFormatter f = DateTimeFormatter.ofPattern("dd/MM/yyyy");
        return "Del " + inicioReporte.format(f) + " al " + finReporte.format(f);
    }

    /** Tarjetas Lun..Dom con el total vendido por día; los días futuros quedan deshabilitados. */
    private void actualizarTarjetasDias() {
        LocalDate hoy = LocalDate.now();
        double[] ventasPorDia = new double[7];
        List<Venta> ventasSemana = ventaDAO.buscarVentasPorFecha(lunesBalance, lunesBalance.plusDays(6));
        if (ventasSemana != null) {
            for (Venta v : ventasSemana) {
                if (v.getFecha() == null || v.getTotal() == null) continue;
                int i = v.getFecha().getDayOfWeek().getValue() - 1;
                ventasPorDia[i] += v.getTotal();
            }
        }

        for (int i = 0; i < 7; i++) {
            LocalDate dia = lunesBalance.plusDays(i);
            boolean futuro = dia.isAfter(hoy);

            Label nombre = new Label(DIAS_CORTOS_BALANCE[i] + " " + dia.getDayOfMonth());
            nombre.getStyleClass().add("ps-dia-nombre");
            Label monto = new Label(futuro || ventasPorDia[i] == 0 ? "—" : "$" + formatoMoneda.format(ventasPorDia[i]));
            monto.getStyleClass().add("ps-dia-monto");
            javafx.scene.layout.VBox contenido = new javafx.scene.layout.VBox(2, nombre, monto);
            contenido.setAlignment(javafx.geometry.Pos.CENTER);

            botonesDias[i].setText(null);
            botonesDias[i].setGraphic(contenido);
            botonesDias[i].setDisable(futuro);
        }
    }

    /** Link "Toda la semana": desmarca los días y muestra la semana completa. */
    @FXML
    private void calcularBalance() {
        if (modoRangoBalance) {
            // "Volver a la semana": la semana de la fecha 'Hasta' buscada
            cambiarSemanaBalance(lunesBalance);
            return;
        }
        desmarcarDias();
        generarReporte();
    }

    /**
     * Al marcar/desmarcar un día se recalcula el balance sumando todos los
     * días marcados. Si no queda ninguno marcado, vuelve a la semana entera.
     */
    @FXML
    private void filtrarPorDias() {
        generarReporte();
    }

    private void desmarcarDias() {
        for (ToggleButton b : botonesDias) {
            b.setSelected(false);
        }
    }

    private boolean hayDiasSeleccionados() {
        for (ToggleButton b : botonesDias) {
            if (b.isSelected()) {
                return true;
            }
        }
        return false;
    }

    /** Lunes de la semana a la que se refieren las tarjetas de días. */
    private LocalDate lunesSemanaDias() {
        return lunesBalance;
    }

    /** "Semana del 2 al 8 de noviembre" (o "del 28 de octubre al 3 de noviembre"). */
    private void actualizarLabelSemana() {
        LocalDate lunes = lunesSemanaDias();
        LocalDate domingo = lunes.plusDays(6);
        String desde = lunes.getMonth() == domingo.getMonth()
                ? String.valueOf(lunes.getDayOfMonth())
                : lunes.getDayOfMonth() + " de " + MESES_BALANCE[lunes.getMonthValue() - 1];
        String texto = "Semana del " + desde + " al " + domingo.getDayOfMonth() + " de " + MESES_BALANCE[domingo.getMonthValue() - 1];
        if (domingo.getYear() != LocalDate.now().getYear()) {
            texto += " de " + domingo.getYear();
        }
        lblSemanaDias.setText(texto);
    }

    /**
     * Períodos a incluir en el balance. Sin días marcados: la semana entera.
     * Con días marcados: esos días, agrupando los consecutivos en un mismo
     * período (así los sueldos del sábado se calculan igual que con un rango
     * y no se cuentan dos veces).
     */
    private List<LocalDate[]> periodosReporte() {
        List<LocalDate[]> periodos = new ArrayList<>();
        if (!hayDiasSeleccionados()) {
            periodos.add(new LocalDate[] { inicioReporte, finReporte });
            return periodos;
        }

        LocalDate lunes = lunesSemanaDias();
        LocalDate desde = null;
        for (int i = 0; i < 7; i++) {
            if (botonesDias[i].isSelected()) {
                if (desde == null) {
                    desde = lunes.plusDays(i);
                }
            } else if (desde != null) {
                periodos.add(new LocalDate[] { desde, lunes.plusDays(i - 1) });
                desde = null;
            }
        }
        if (desde != null) {
            periodos.add(new LocalDate[] { desde, lunes.plusDays(6) });
        }
        return periodos;
    }

    /** Texto con los días marcados ("Lun 2, Mié 4"), para la pantalla y el ticket. */
    private String descripcionDiasSeleccionados() {
        LocalDate lunes = lunesSemanaDias();
        List<String> dias = new ArrayList<>();
        for (int i = 0; i < 7; i++) {
            if (botonesDias[i].isSelected()) {
                dias.add(DIAS_CORTOS_BALANCE[i] + " " + lunes.plusDays(i).getDayOfMonth());
            }
        }
        return String.join(", ", dias);
    }

    // ===================== CONTROL DE CAJA =====================

    @FXML private HBox boxArqueoManiana, boxArqueoTarde;
    @FXML private javafx.scene.layout.VBox boxControlCaja, boxControlCajaDetalle;
    @FXML private Label lblControlCajaTitulo, lblControlCajaTotal;
    private final ArqueoCajaDAO arqueoDAO = new ArqueoCajaDAO();
    private ControlArqueo arqueoManiana, arqueoTarde;

    /**
     * Casillas de un turno: "Coincide" (los billetes son lo que dice el
     * sistema) o "No coincide" + cuánto hay en la caja. Con un solo día en
     * pantalla se pueden marcar; con varios días muestra un resumen.
     */
    private final class ControlArqueo {
        final String turno;
        final HBox box;
        final CheckBox chkSi = new CheckBox("Coincide");
        final CheckBox chkNo = new CheckBox("No coincide");
        final TextField txtContado = new TextField();
        final Label lblEstado = new Label();
        final Label lblResumen = new Label();
        LocalDate dia;
        double sistema;
        boolean cargando;

        ControlArqueo(String turno, HBox box) {
            this.turno = turno;
            this.box = box;
            chkSi.getStyleClass().add("ps-arqueo-chk");
            chkNo.getStyleClass().add("ps-arqueo-chk");
            txtContado.setPromptText("Hay en caja $");
            txtContado.setPrefWidth(115);
            chkSi.setMinWidth(Region.USE_PREF_SIZE);
            chkNo.setMinWidth(Region.USE_PREF_SIZE);
            txtContado.setMinWidth(90);
            txtContado.getStyleClass().add("ps-arqueo-txt");
            lblEstado.getStyleClass().add("ps-arqueo-estado");
            lblEstado.setMinWidth(Region.USE_PREF_SIZE);
            lblResumen.getStyleClass().add("ps-arqueo-estado");

            chkSi.selectedProperty().addListener((o, antes, si) -> {
                if (cargando) return;
                if (si) {
                    cargando = true;
                    chkNo.setSelected(false);
                    cargando = false;
                    mostrarCampo(false);
                    guardar(sistema);
                } else if (!chkNo.isSelected()) {
                    borrar();
                }
            });
            chkNo.selectedProperty().addListener((o, antes, no) -> {
                if (cargando) return;
                if (no) {
                    cargando = true;
                    chkSi.setSelected(false);
                    cargando = false;
                    mostrarCampo(true);
                    txtContado.requestFocus();
                    Double monto = parsearMonto(txtContado.getText());
                    if (monto != null) {
                        guardar(monto);
                    } else {
                        borrar();
                        lblEstado.setText("Escribí cuánto hay");
                        marcarEstado(null);
                    }
                } else {
                    mostrarCampo(false);
                    if (!chkSi.isSelected()) borrar();
                }
            });
            txtContado.setOnAction(e -> guardarContado());
            txtContado.focusedProperty().addListener((o, antes, foco) -> {
                if (!foco) guardarContado();
            });
        }

        void mostrarCampo(boolean ver) {
            txtContado.setVisible(ver);
            txtContado.setManaged(ver);
        }

        void guardarContado() {
            if (cargando || !chkNo.isSelected()) return;
            Double monto = parsearMonto(txtContado.getText());
            if (monto == null) {
                lblEstado.setText("Escribí cuánto hay");
                marcarEstado(null);
                return;
            }
            guardar(monto);
        }

        void guardar(double contado) {
            try {
                arqueoDAO.guardar(dia, turno, sistema, contado);
                ArqueoCaja a = arqueoDAO.buscar(dia, turno);
                mostrarEstado(a);
                refrescarControlCaja();
            } catch (Exception e) {
                e.printStackTrace();
                mostrarAdvertencia("No se pudo guardar", "No se pudo guardar el control de caja:\n" + e.getMessage());
            }
        }

        void borrar() {
            try {
                arqueoDAO.borrar(dia, turno);
                mostrarEstado(null);
                refrescarControlCaja();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        /** Un solo día: casillas para marcar. */
        void mostrarDia(LocalDate dia, double sistema, ArqueoCaja a) {
            this.dia = dia;
            this.sistema = sistema;
            cargando = true;
            boolean noCoincide = a != null && !a.coincide();
            chkSi.setSelected(a != null && a.coincide());
            chkNo.setSelected(noCoincide);
            txtContado.setText(noCoincide ? formatoMoneda.format(a.getMontoContado()) : "");
            mostrarCampo(noCoincide);
            cargando = false;
            mostrarEstado(a);
            box.getChildren().setAll(chkSi, chkNo, txtContado, lblEstado);
        }

        /** Varios días: cuántos se contaron y la diferencia. */
        void mostrarResumen(List<ArqueoCaja> delTurno) {
            if (delTurno.isEmpty()) {
                lblResumen.setText("Sin contar");
                marcar(lblResumen, null);
            } else {
                double dif = delTurno.stream().mapToDouble(ArqueoCaja::diferencia).sum();
                int n = delTurno.size();
                lblResumen.setText((n == 1 ? "1 día contado" : n + " días contados") + " · " + textoDiferencia(dif));
                marcar(lblResumen, Math.abs(dif) < 0.5);
            }
            box.getChildren().setAll(lblResumen);
        }

        void mostrarEstado(ArqueoCaja a) {
            if (a == null) {
                lblEstado.setText("");
                marcarEstado(null);
                return;
            }
            lblEstado.setText(textoDiferencia(a.diferencia()));
            marcarEstado(a.coincide());
        }

        void marcarEstado(Boolean ok) {
            marcar(lblEstado, ok);
        }
    }

    private static void marcar(Label l, Boolean ok) {
        l.getStyleClass().removeAll("ps-arqueo-ok", "ps-arqueo-mal");
        if (ok != null) {
            l.getStyleClass().add(ok ? "ps-arqueo-ok" : "ps-arqueo-mal");
        }
    }

    /** "✓ Coincide", "Faltan $500" o "Sobran $200". */
    private String textoDiferencia(double dif) {
        if (Math.abs(dif) < 0.5) return "✓ Coincide";
        return (dif < 0 ? "Faltan $" : "Sobran $") + formatoMoneda.format(Math.abs(dif));
    }

    /** Acepta "15000", "15.000", "15.000,50" y "$ 15.000". */
    private static Double parsearMonto(String texto) {
        if (texto == null) return null;
        String t = texto.replace("$", "").replace(" ", "").trim();
        if (t.isEmpty()) return null;
        if (t.contains(",")) {
            t = t.replace(".", "").replace(",", ".");
        } else if (t.matches("\\d{1,3}(\\.\\d{3})+")) {
            t = t.replace(".", "");
        }
        try {
            double v = Double.parseDouble(t);
            return v < 0 ? null : v;
        } catch (NumberFormatException e) {
            return null;
        }
    }

    /** El día mostrado, si el balance es de un solo día; si no, null. */
    private LocalDate diaUnicoBalance() {
        List<LocalDate[]> p = periodosReporte();
        return p.size() == 1 && p.get(0)[0] != null && p.get(0)[0].equals(p.get(0)[1]) ? p.get(0)[0] : null;
    }

    private List<ArqueoCaja> arqueosDelPeriodo() {
        List<ArqueoCaja> todos = new ArrayList<>();
        for (LocalDate[] p : periodosReporte()) {
            if (p[0] != null && p[1] != null) {
                todos.addAll(arqueoDAO.buscarPorRango(p[0], p[1]));
            }
        }
        todos.sort(java.util.Comparator.comparing(ArqueoCaja::getFecha)
                .thenComparing(a -> HorarioLocal.MANIANA.equals(a.getTurno()) ? 0 : 1));
        return todos;
    }

    /** Casillas de cada turno y tarjeta "Control de caja" del período. */
    private void mostrarArqueos(double efectivoManiana, double efectivoTarde) {
        if (arqueoManiana == null) {
            arqueoManiana = new ControlArqueo(HorarioLocal.MANIANA, boxArqueoManiana);
            arqueoTarde = new ControlArqueo(HorarioLocal.TARDE, boxArqueoTarde);
        }
        List<ArqueoCaja> arqueos = arqueosDelPeriodo();
        LocalDate dia = diaUnicoBalance();
        if (dia != null) {
            arqueoManiana.mostrarDia(dia, efectivoManiana, buscarArqueo(arqueos, dia, HorarioLocal.MANIANA));
            arqueoTarde.mostrarDia(dia, efectivoTarde, buscarArqueo(arqueos, dia, HorarioLocal.TARDE));
        } else {
            arqueoManiana.mostrarResumen(arqueos.stream().filter(a -> HorarioLocal.MANIANA.equals(a.getTurno())).toList());
            arqueoTarde.mostrarResumen(arqueos.stream().filter(a -> HorarioLocal.TARDE.equals(a.getTurno())).toList());
        }
        mostrarControlCaja(arqueos);
    }

    private static ArqueoCaja buscarArqueo(List<ArqueoCaja> arqueos, LocalDate dia, String turno) {
        for (ArqueoCaja a : arqueos) {
            if (dia.equals(a.getFecha()) && turno.equals(a.getTurno())) return a;
        }
        return null;
    }

    private void refrescarControlCaja() {
        mostrarControlCaja(arqueosDelPeriodo());
    }

    /** Tarjeta debajo del balance: cada turno contado y la diferencia total. */
    private void mostrarControlCaja(List<ArqueoCaja> arqueos) {
        boolean hay = !arqueos.isEmpty();
        boxControlCaja.setVisible(hay);
        boxControlCaja.setManaged(hay);
        boxControlCajaDetalle.getChildren().clear();
        if (!hay) return;

        double total = arqueos.stream().mapToDouble(ArqueoCaja::diferencia).sum();
        long conDiferencia = arqueos.stream().filter(a -> !a.coincide()).count();
        lblControlCajaTitulo.setText(arqueos.size() + (arqueos.size() == 1 ? " turno contado" : " turnos contados")
                + (conDiferencia > 0 ? " · " + conDiferencia + " con diferencia" : ""));
        lblControlCajaTotal.setText(textoDiferencia(total));
        marcar(lblControlCajaTotal, Math.abs(total) < 0.5);

        DateTimeFormatter f = DateTimeFormatter.ofPattern("dd/MM");
        for (ArqueoCaja a : arqueos) {
            Label cuando = new Label(DIAS_CORTOS_BALANCE[a.getFecha().getDayOfWeek().getValue() - 1] + " "
                    + a.getFecha().format(f) + " · " + a.getTurno());
            cuando.getStyleClass().add("ps-arqueo-linea");
            cuando.setMinWidth(170);
            Label detalle = new Label("Sistema $" + formatoMoneda.format(valor(a.getMontoSistema()))
                    + "   ·   Contado $" + formatoMoneda.format(valor(a.getMontoContado())));
            detalle.getStyleClass().add("ps-bal-nota");
            Region esp = new Region();
            HBox.setHgrow(esp, Priority.ALWAYS);
            Label dif = new Label(textoDiferencia(a.diferencia()));
            dif.getStyleClass().add("ps-arqueo-estado");
            marcar(dif, a.coincide());
            HBox fila = new HBox(14, cuando, detalle, esp, dif);
            fila.setAlignment(Pos.CENTER_LEFT);
            fila.getStyleClass().add("ps-arqueo-fila");
            boxControlCajaDetalle.getChildren().add(fila);
        }
    }

    private static double valor(Double d) {
        return d == null ? 0 : d;
    }

    /** 0 = mañana, 1 = tarde. Las ventas fuera de horario van al turno más cercano. */
    private static int indiceTurno(Venta v) {
        String franja = HorarioLocal.franjaMasCercana(v.getFecha() != null ? v.getFecha().toLocalTime() : null);
        return HorarioLocal.TARDE.equals(franja) ? 1 : 0;
    }

    /** Escribe un monto en la tabla de balance: "—" si es cero, "−$x" si es un egreso. */
    private void setMontoBalance(Label label, double valor, boolean egreso) {
        if (Math.abs(valor) < 0.005) {
            label.setText("—");
        } else if (egreso) {
            label.setText("−$" + formatoMoneda.format(valor));
        } else {
            label.setText(valor < 0 ? "−$" + formatoMoneda.format(-valor) : "$" + formatoMoneda.format(valor));
        }
    }

    private List<Venta> buscarVentasPeriodos(List<LocalDate[]> periodos) {
        List<Venta> ventas = new ArrayList<>();
        for (LocalDate[] p : periodos) {
            List<Venta> v = ventaDAO.buscarVentasPorFecha(p[0], p[1]);
            if (v != null) {
                ventas.addAll(v);
            }
        }
        return ventas;
    }

    @FXML
    public void generarReporte() {
        if (!hayDiasSeleccionados() && (inicioReporte == null || finReporte == null)) {
            return;
        }

        List<LocalDate[]> periodos = periodosReporte();

        // 1. Ventas del período, desglosadas por medio de pago
        List<Venta> ventasEnRango = buscarVentasPeriodos(periodos);

        double totalVentas = 0.0;
        double totalEfectivo = 0.0;
        double totalVirtual = 0.0;

        // Por turno: [0] mañana, [1] tarde; cada uno {efectivo, virtual}
        double[][] porTurno = new double[2][2];
        for (Venta v : ventasEnRango) {
            totalVentas += v.getTotal();
            // Pago mixto: la parte en efectivo a Efectivo y la del QR a Virtual
            totalEfectivo += v.montoEnEfectivo();
            totalVirtual += v.montoEnVirtual();
            int t = indiceTurno(v);
            porTurno[t][0] += v.montoEnEfectivo();
            porTurno[t][1] += v.montoEnVirtual();
        }
        Label[][] filasTurno = {
            { lblBalManianaEf, lblBalManianaVi, lblBalManianaTot },
            { lblBalTardeEf, lblBalTardeVi, lblBalTardeTot }
        };
        for (int t = 0; t < 2; t++) {
            setMontoBalance(filasTurno[t][0], porTurno[t][0], false);
            setMontoBalance(filasTurno[t][1], porTurno[t][1], false);
            setMontoBalance(filasTurno[t][2], porTurno[t][0] + porTurno[t][1], false);
        }
        // Control de caja: billetes contados por turno
        mostrarArqueos(porTurno[0][0], porTurno[1][0]);

        // 2. Gastos del período, desglosados por medio de pago (consultas agregadas: no se trae todo el historial)
        // 3. Sueldos del período, desglosados por medio de pago.
        //    Los empleados cobran solo los sábados: los turnos de la semana (dom a sáb)
        //    se descuentan completos en el sábado, no el día en que se trabajaron.
        // 3b. Adelantos entregados en el período (salen de la caja/cuenta el día que se entregan)
        double totalGastosEfectivo = 0.0, totalGastosTransferencia = 0.0;
        double totalSueldosEfectivo = 0.0, totalSueldosTransferencia = 0.0;
        double sueldosTrabajadosEfectivo = 0.0, sueldosTrabajadosTransferencia = 0.0;
        double totalAdelantosEfectivo = 0.0, totalAdelantosTransferencia = 0.0;
        boolean incluyeDiaDePago = false;

        for (LocalDate[] p : periodos) {
            LocalDate inicio = p[0];
            LocalDate fin = p[1];
            totalGastosEfectivo += gastoDAO.sumarGastosPorRangoYMedioPago(inicio, fin, "Efectivo");
            totalGastosTransferencia += gastoDAO.sumarGastosPorRangoYMedioPago(inicio, fin, "Transferencia");
            totalSueldosEfectivo += empleadoDAO.totalSueldosEfectivoEnRango(inicio, fin);
            totalSueldosTransferencia += empleadoDAO.totalSueldosTransferenciaEnRango(inicio, fin);
            sueldosTrabajadosEfectivo += empleadoDAO.totalSueldosTrabajadosEfectivoEnRango(inicio, fin);
            sueldosTrabajadosTransferencia += empleadoDAO.totalSueldosTrabajadosTransferenciaEnRango(inicio, fin);
            totalAdelantosEfectivo += adelantoDAO.sumarPorRangoYMedioPago(inicio, fin, "Efectivo");
            totalAdelantosTransferencia += adelantoDAO.sumarPorRangoYMedioPago(inicio, fin, "Transferencia");
            incluyeDiaDePago |= empleadoDAO.rangoIncluyeDiaDePago(inicio, fin);
        }

        double totalGastos = totalGastosEfectivo + totalGastosTransferencia;
        double totalAdelantos = totalAdelantosEfectivo + totalAdelantosTransferencia;

        // 4. Balance por medio de pago
        double quedaEfectivo = totalEfectivo - totalGastosEfectivo - totalSueldosEfectivo - totalAdelantosEfectivo;
        double quedaVirtual = totalVirtual - totalGastosTransferencia - totalSueldosTransferencia - totalAdelantosTransferencia;
        double balanceFinal = quedaEfectivo + quedaVirtual;

        // 5. Interfaz
        repTotalVentas = totalVentas;
        repVentasEfectivo = totalEfectivo;
        repVentasVirtual = totalVirtual;
        repTotalGastos = totalGastos;
        repGastosEfectivo = totalGastosEfectivo;
        repGastosTransferencia = totalGastosTransferencia;
        // Si el rango no incluye sábado, los sueldos se muestran (lo trabajado) pero no descuentan del balance
        repSueldosPendientes = !incluyeDiaDePago;
        double sueldosMostradosEfectivo = totalSueldosEfectivo;
        double sueldosMostradosTransferencia = totalSueldosTransferencia;
        if (repSueldosPendientes) {
            sueldosMostradosEfectivo = sueldosTrabajadosEfectivo;
            sueldosMostradosTransferencia = sueldosTrabajadosTransferencia;
        }
        repTotalSueldos = sueldosMostradosEfectivo + sueldosMostradosTransferencia;
        repSueldosEfectivo = sueldosMostradosEfectivo;
        repSueldosTransferencia = sueldosMostradosTransferencia;
        repTotalAdelantos = totalAdelantos;
        repAdelantosEfectivo = totalAdelantosEfectivo;
        repAdelantosTransferencia = totalAdelantosTransferencia;
        repQuedaEfectivo = quedaEfectivo;
        repQuedaVirtual = quedaVirtual;
        repBalanceFinal = balanceFinal;

        // Tabla de balance (los egresos se muestran en negativo; "—" si no hay movimiento)
        setMontoBalance(lblBalVentasEf, totalEfectivo, false);
        setMontoBalance(lblBalVentasVi, totalVirtual, false);
        setMontoBalance(lblBalVentasTot, totalVentas, false);
        setMontoBalance(lblBalGastosEf, totalGastosEfectivo, true);
        setMontoBalance(lblBalGastosVi, totalGastosTransferencia, true);
        setMontoBalance(lblBalGastosTot, totalGastos, true);
        setMontoBalance(lblBalSueldosEf, repSueldosEfectivo, true);
        setMontoBalance(lblBalSueldosVi, repSueldosTransferencia, true);
        setMontoBalance(lblBalSueldosTot, repTotalSueldos, true);
        setMontoBalance(lblBalAdelEf, totalAdelantosEfectivo, true);
        setMontoBalance(lblBalAdelVi, totalAdelantosTransferencia, true);
        setMontoBalance(lblBalAdelTot, totalAdelantos, true);
        setMontoBalance(lblBalResEf, quedaEfectivo, false);
        setMontoBalance(lblBalResVi, quedaVirtual, false);
        setMontoBalance(lblBalResTot, balanceFinal, false);

        // Si el período no incluye el sábado, los sueldos se muestran (lo trabajado) pero no se descuentan
        lblSueldosNota.setVisible(repSueldosPendientes);
        lblSueldosNota.setManaged(repSueldosPendientes);
        for (Label l : new Label[] { lblBalSueldosEf, lblBalSueldosVi, lblBalSueldosTot }) {
            l.getStyleClass().remove("ps-bal-pendiente");
            if (repSueldosPendientes) l.getStyleClass().add("ps-bal-pendiente");
        }
        for (Label l : new Label[] { lblBalResEf, lblBalResVi, lblBalResTot }) {
            l.getStyleClass().remove("ps-negativo");
        }
        if (quedaEfectivo < 0) lblBalResEf.getStyleClass().add("ps-negativo");
        if (quedaVirtual < 0) lblBalResVi.getStyleClass().add("ps-negativo");
        if (balanceFinal < 0) lblBalResTot.getStyleClass().add("ps-negativo");

        if (modoRangoBalance) {
            lblMostrandoDias.setText(inicioReporte.equals(finReporte)
                    ? "Mostrando: un solo día"
                    : "Mostrando: " + (java.time.temporal.ChronoUnit.DAYS.between(inicioReporte, finReporte) + 1) + " días");
            lnkTodaLaSemana.setText("Volver a la semana");
            lnkTodaLaSemana.setVisible(true);
        } else {
            lblMostrandoDias.setText(hayDiasSeleccionados()
                    ? "Mostrando: " + descripcionDiasSeleccionados()
                    : "Mostrando: toda la semana");
            lnkTodaLaSemana.setText("Toda la semana");
            lnkTodaLaSemana.setVisible(hayDiasSeleccionados());
        }
    }

    /**
     * Imprime en ticket el balance actualmente mostrado en pantalla para el
     * rango de fechas seleccionado (Desde/Hasta), con el mismo desglose de
     * ventas, gastos y balance final que se ve en la pestaña, terminando en
     * el resumen de utilidad neta.
     */
    @FXML
    private void imprimirResumenReporte(ActionEvent event) {
        LocalDate inicio = inicioReporte;
        LocalDate fin = finReporte;
        boolean porDias = hayDiasSeleccionados();

        if (!porDias && (inicio == null || fin == null)) {
            mostrarAdvertencia("Faltan fechas", "Seleccioná las fechas 'Desde' y 'Hasta' para imprimir el resumen.");
            return;
        }

        try {
            generarReporte();

            List<Venta> ventasEnRango = buscarVentasPeriodos(periodosReporte());

            double efectivoManiana = 0.0, virtualManiana = 0.0;
            double efectivoTarde = 0.0, virtualTarde = 0.0;

            if (ventasEnRango != null) {
                for (Venta v : ventasEnRango) {
                    // Pago mixto: cada parte a su medio (igual que en pantalla)
                    double ef = v.montoEnEfectivo();
                    double vi = v.montoEnVirtual();
                    // Fuera de horario: al turno más cercano
                    if (indiceTurno(v) == 0) {
                        efectivoManiana += ef;
                        virtualManiana += vi;
                    } else {
                        efectivoTarde += ef;
                        virtualTarde += vi;
                    }
                }
            }

            StringBuilder ticket = new StringBuilder();

            ticket.append(centrar("CARNICERIA MONCHO")).append("\n");
            ticket.append(centrar("RESUMEN DE CAJA")).append("\n");
            ticket.append(lineaSeparadora()).append("\n");

            if (porDias) {
                ticket.append("Dias: ").append(descripcionDiasSeleccionados()).append("\n");
            } else if (inicio.equals(fin)) {
                ticket.append("Fecha: ").append(inicio).append("\n");
            } else {
                ticket.append("Desde: ").append(inicio).append("\n");
                ticket.append("Hasta: ").append(fin).append("\n");
            }
            ticket.append("Impreso: ")
                    .append(LocalDateTime.now().format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm")))
                    .append("\n");
            ticket.append(lineaSeparadora()).append("\n");

            ticket.append("VENTAS\n");
            ticket.append(fila("Total ventas", "$" + formatoMoneda.format(repTotalVentas))).append("\n");
            ticket.append(fila("  Efectivo", "$" + formatoMoneda.format(repVentasEfectivo))).append("\n");
            ticket.append(fila("  Virtual", "$" + formatoMoneda.format(repVentasVirtual))).append("\n");
            ticket.append(lineaSeparadora()).append("\n");

            ticket.append("VENTAS MAÑANA (08:30 a 13:00)\n");
            ticket.append(fila("  Efectivo", "$" + formatoMoneda.format(efectivoManiana))).append("\n");
            ticket.append(fila("  Virtual", "$" + formatoMoneda.format(virtualManiana))).append("\n");
            ticket.append(lineaSeparadora()).append("\n");

            ticket.append("VENTAS TARDE (17:00 a 21:00)\n");
            ticket.append(fila("  Efectivo", "$" + formatoMoneda.format(efectivoTarde))).append("\n");
            ticket.append(fila("  Virtual", "$" + formatoMoneda.format(virtualTarde))).append("\n");
            ticket.append(lineaSeparadora()).append("\n");

            // Control de caja: billetes contados contra el sistema
            List<ArqueoCaja> arqueos = arqueosDelPeriodo();
            if (!arqueos.isEmpty()) {
                ticket.append("CONTROL DE CAJA\n");
                DateTimeFormatter fdm = DateTimeFormatter.ofPattern("dd/MM");
                double difTotal = 0;
                for (ArqueoCaja a : arqueos) {
                    String cuando = "  " + a.getFecha().format(fdm) + " " + a.getTurno();
                    double dif = a.diferencia();
                    difTotal += dif;
                    String estado = a.coincide() ? "OK"
                            : (dif < 0 ? "-$" : "+$") + formatoMoneda.format(Math.abs(dif));
                    ticket.append(fila(cuando, estado)).append("\n");
                }
                ticket.append(fila("Diferencia", Math.abs(difTotal) < 0.5 ? "$0"
                        : (difTotal < 0 ? "-$" : "+$") + formatoMoneda.format(Math.abs(difTotal)))).append("\n");
                ticket.append(lineaSeparadora()).append("\n");
            }

            ticket.append("GASTOS\n");
            ticket.append(fila("Total gastos", "$" + formatoMoneda.format(repTotalGastos))).append("\n");
            ticket.append(fila("  Efectivo", "$" + formatoMoneda.format(repGastosEfectivo))).append("\n");
            ticket.append(fila("  Transferencia", "$" + formatoMoneda.format(repGastosTransferencia))).append("\n");
            ticket.append(fila("Sueldos", "$" + formatoMoneda.format(repTotalSueldos))).append("\n");
            if (repSueldosPendientes) {
                ticket.append("  (solo impactara el sabado)\n");
            }
            ticket.append(fila("  Efectivo", "$" + formatoMoneda.format(repSueldosEfectivo))).append("\n");
            ticket.append(fila("  Transferencia", "$" + formatoMoneda.format(repSueldosTransferencia))).append("\n");
            ticket.append(fila("Adelantos", "$" + formatoMoneda.format(repTotalAdelantos))).append("\n");
            ticket.append(fila("  Efectivo", "$" + formatoMoneda.format(repAdelantosEfectivo))).append("\n");
            ticket.append(fila("  Transferencia", "$" + formatoMoneda.format(repAdelantosTransferencia))).append("\n");
            ticket.append(lineaSeparadora()).append("\n");

            ticket.append("BALANCE\n");
            ticket.append(fila("Queda efectivo", "$" + formatoMoneda.format(repQuedaEfectivo))).append("\n");
            ticket.append(fila("Queda virtual", "$" + formatoMoneda.format(repQuedaVirtual))).append("\n");
            ticket.append(lineaSeparadora()).append("\n");

            ticket.append(centrar("UTILIDAD NETA")).append("\n");
            ticket.append(centrar("$" + formatoMoneda.format(repBalanceFinal))).append("\n");
            ticket.append("\n\n\n");

            imprimirEnSegundoPlano((Node) event.getSource(), ticket.toString());

        } catch (Exception e) {
            e.printStackTrace();
            mostrarError("Error de impresión", "No se pudo imprimir el resumen.");
        }
    }

    // --- Lógica de Reportes de Productos ---

    @FXML
    private void mostrarProductosDeHoy() {
        dpInicioProductos.setValue(LocalDate.now());
        dpFinProductos.setValue(LocalDate.now());
        generarReporteProductos();
    }

    @FXML
    private void mostrarReporteSemanalProductos() {
        dpInicioProductos.setValue(LocalDate.now().with(DayOfWeek.MONDAY));
        dpFinProductos.setValue(LocalDate.now());
        generarReporteProductos();
    }

    @FXML
    private void mostrarProductosDelMes() {
        dpInicioProductos.setValue(LocalDate.now().withDayOfMonth(1));
        dpFinProductos.setValue(LocalDate.now());
        generarReporteProductos();
    }

    @FXML
    private void mostrarSelectorRangoProductos() {
        dpInicioProductos.requestFocus();
    }

    @FXML
    public void generarReporteProductos() {
        LocalDate inicio = dpInicioProductos.getValue();
        LocalDate fin = dpFinProductos.getValue();

        if (inicio == null || fin == null) {
            mostrarAdvertencia("Faltan fechas", "Seleccioná las fechas 'Desde' y 'Hasta' para generar el reporte.");
            return;
        }
        if (inicio.isAfter(fin)) {
            mostrarAdvertencia("Rango inválido", "La fecha 'Desde' no puede ser posterior a la fecha 'Hasta'.");
            return;
        }

        List<DetalleVenta> detalles = detalleVentaDAO.buscarPorRangoFechas(inicio, fin);

        LinkedHashMap<Long, ProductoAcumulado> acumulados = new LinkedHashMap<>();
        if (detalles != null) {
            for (DetalleVenta d : detalles) {
                if (d.getProducto() == null) {
                    continue;
                }
                Long idProducto = d.getProducto().getId();
                double peso = d.getPeso() != null ? d.getPeso() : 0.0;
                double precio = d.getPrecio() != null ? d.getPrecio() : 0.0;
                String tipo = d.getProducto().getTipo();

                ProductoAcumulado acumulado = acumulados.computeIfAbsent(idProducto,
                        k -> new ProductoAcumulado(d.getProducto().getNombre(), tipo));
                acumulado.kilos += peso;
                acumulado.total += peso * precio;
            }
        }

        List<ProductoReporte> filas = new ArrayList<>();
        double totalKilos = 0.0;
        double totalFacturado = 0.0;
        ProductoAcumulado masVendido = null;

        for (ProductoAcumulado acumulado : acumulados.values()) {
            double precioPromedio = acumulado.kilos > 0 ? acumulado.total / acumulado.kilos : 0.0;
            filas.add(new ProductoReporte(acumulado.nombre, acumulado.tipo, acumulado.kilos, precioPromedio, acumulado.total));

            totalKilos += acumulado.kilos;
            totalFacturado += acumulado.total;
            if (masVendido == null || acumulado.kilos > masVendido.kilos) {
                masVendido = acumulado;
            }
        }

        filas.sort((a, b) -> Double.compare(b.getTotal(), a.getTotal()));

        categoriasReporte = calcularCategorias(filas, totalFacturado);

        // Mantener la categoría elegida si sigue existiendo en el nuevo período
        CategoriaReporte elegida = lvCategoriasProductos.getSelectionModel().getSelectedItem();
        String categoriaElegida = elegida != null ? elegida.categoria() : null;

        productosReporte.setAll(filas);
        List<CategoriaReporte> items = new ArrayList<>();
        if (!filas.isEmpty()) {
            // Fila "Todas" (categoria null) arriba de todo
            items.add(new CategoriaReporte(null, totalKilos, totalFacturado, 100.0));
            items.addAll(categoriasReporte);
        }
        lvCategoriasProductos.setItems(FXCollections.observableArrayList(items));

        CategoriaReporte aSeleccionar = items.stream()
                .filter(c -> java.util.Objects.equals(c.categoria(), categoriaElegida))
                .findFirst().orElse(items.isEmpty() ? null : items.get(0));
        if (aSeleccionar != null) {
            lvCategoriasProductos.getSelectionModel().select(aSeleccionar);
        } else {
            aplicarFiltroCategoria(null);
        }

        lblTotalKilosProductos.setText(formatoKilosCorto(totalKilos) + " kg");
        lblTotalFacturadoProductos.setText("$" + formatoMoneda.format(totalFacturado));
        lblProductoTop.setText(masVendido != null ? masVendido.nombre : "—");
        if (categoriasReporte.isEmpty()) {
            lblCategoriaTop.setText("—");
        } else {
            CategoriaReporte top = categoriasReporte.get(0);
            lblCategoriaTop.setText(top.categoria() + " · " + Math.round(top.porcentaje()) + "%");
        }
    }

    /** Kilos con un decimal y coma: 97,4 */
    private String formatoKilosCorto(double kilos) {
        return new DecimalFormat("#,##0.0", java.text.DecimalFormatSymbols.getInstance(new java.util.Locale("es", "AR")))
                .format(kilos);
    }

    /** Agrupa los productos por categoría (Carniceria, Pollo, Cerdo...), ordenadas de la que más dinero generó a la que menos. */
    private List<CategoriaReporte> calcularCategorias(List<ProductoReporte> productos, double totalFacturado) {
        LinkedHashMap<String, double[]> acumulado = new LinkedHashMap<>(); // [kilos, total]
        for (ProductoReporte p : productos) {
            double[] valores = acumulado.computeIfAbsent(categoriaDe(p.getTipo()), k -> new double[2]);
            valores[0] += p.getKilos();
            valores[1] += p.getTotal();
        }
        List<CategoriaReporte> categorias = new ArrayList<>();
        acumulado.forEach((categoria, valores) -> categorias.add(new CategoriaReporte(categoria, valores[0], valores[1],
                totalFacturado > 0 ? valores[1] * 100.0 / totalFacturado : 0.0)));
        categorias.sort((a, b) -> Double.compare(b.total(), a.total()));
        return categorias;
    }

    private static String categoriaDe(String tipo) {
        return tipo != null && !tipo.isBlank() ? tipo : "Sin categoría";
    }

    /** Filtra la tabla por la categoría elegida (null o "Todas" = todos los productos). */
    private void aplicarFiltroCategoria(CategoriaReporte categoria) {
        if (categoria == null || categoria.categoria() == null) {
            productosFiltrados.setPredicate(null);
            lblTituloDetalleProductos.setText("Todos los productos");
            btnImprimirCategoria.setText("Imprimir categoría");
            btnImprimirCategoria.setDisable(true);
        } else {
            productosFiltrados.setPredicate(p -> categoriaDe(p.getTipo()).equals(categoria.categoria()));
            lblTituloDetalleProductos.setText(categoria.categoria());
            btnImprimirCategoria.setText("Imprimir " + categoria.categoria());
            btnImprimirCategoria.setDisable(false);
        }
        double total = productosFiltrados.stream().mapToDouble(ProductoReporte::getTotal).sum();
        int n = productosFiltrados.size();
        lblResumenDetalleProductos.setText(n == 0 ? ""
                : n + (n == 1 ? " producto" : " productos") + " · $" + formatoMoneda.format(total));
        tvReporteProductos.refresh(); // renumera la columna "#"
    }

    /** Fila de la lista de categorías ("Todas" tiene categoria == null). */
    private record CategoriaReporte(String categoria, double kilos, double total, double porcentaje) { }

    /** "Imprimir todo": todas las categorías, aunque en pantalla haya una filtrada. */
    @FXML
    private void imprimirReporteProductos(ActionEvent event) {
        imprimirProductos((Node) event.getSource(), null);
    }

    /** "Imprimir <categoría>": solo los productos de la categoría elegida en la lista. */
    @FXML
    private void imprimirCategoriaProductos(ActionEvent event) {
        CategoriaReporte elegida = lvCategoriasProductos.getSelectionModel().getSelectedItem();
        if (elegida == null || elegida.categoria() == null) {
            mostrarAdvertencia("Elegí una categoría", "Tocá una categoría de la lista para imprimir solo sus productos.");
            return;
        }
        imprimirProductos((Node) event.getSource(), elegida.categoria());
    }

    /**
     * Ticket del reporte de productos del período elegido.
     * @param soloCategoria null = todas las categorías; si no, solo esa.
     */
    private void imprimirProductos(Node origen, String soloCategoria) {
        LocalDate inicio = dpInicioProductos.getValue();
        LocalDate fin = dpFinProductos.getValue();

        if (inicio == null || fin == null) {
            mostrarAdvertencia("Faltan fechas", "Seleccioná las fechas 'Desde' y 'Hasta' para imprimir el reporte.");
            return;
        }

        try {
            generarReporteProductos();

            List<ProductoReporte> filas = soloCategoria == null
                    ? productosReporte
                    : productosReporte.stream().filter(p -> categoriaDe(p.getTipo()).equals(soloCategoria)).toList();
            if (filas == null || filas.isEmpty()) {
                mostrarAdvertencia("Sin datos", soloCategoria == null
                        ? "No hay productos vendidos en el rango seleccionado."
                        : "No hay productos de " + soloCategoria + " vendidos en el rango seleccionado.");
                return;
            }

            StringBuilder ticket = new StringBuilder();

            ticket.append(centrar("CARNICERIA MONCHO")).append("\n");
            ticket.append(centrar("REPORTE DE PRODUCTOS")).append("\n");
            if (soloCategoria != null) {
                ticket.append(centrar("CATEGORIA: " + soloCategoria.toUpperCase())).append("\n");
            }
            ticket.append(lineaSeparadora()).append("\n");

            if (inicio.equals(fin)) {
                ticket.append("Fecha: ").append(inicio).append("\n");
            } else {
                ticket.append("Desde: ").append(inicio).append("\n");
                ticket.append("Hasta: ").append(fin).append("\n");
            }
            ticket.append("Impreso: ")
                    .append(LocalDateTime.now().format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm")))
                    .append("\n");
            ticket.append(lineaSeparadora()).append("\n");

            // Resumen: dinero generado por cada categoría (con una sola, solo esa
            // y su porcentaje sobre el total vendido)
            List<CategoriaReporte> categorias = soloCategoria == null
                    ? categoriasReporte
                    : categoriasReporte.stream().filter(c -> c.categoria().equals(soloCategoria)).toList();
            ticket.append(soloCategoria == null ? "DINERO POR CATEGORIA\n" : "PARTICIPACION EN LAS VENTAS\n");
            for (CategoriaReporte categoria : categorias) {
                ticket.append(fila(ajustar(categoria.categoria(), 14).trim()
                                + String.format(" %.0f%%", categoria.porcentaje()),
                        "$" + formatoMoneda.format(categoria.total()))).append("\n");
            }
            ticket.append(lineaSeparadora()).append("\n");

            // Detalle de productos agrupado por categoría (mismo orden que el resumen)
            LinkedHashMap<String, List<ProductoReporte>> porCategoria = new LinkedHashMap<>();
            for (CategoriaReporte categoria : categorias) {
                porCategoria.put(categoria.categoria(), new ArrayList<>());
            }
            for (ProductoReporte productoFila : filas) {
                porCategoria.computeIfAbsent(categoriaDe(productoFila.getTipo()), k -> new ArrayList<>()).add(productoFila);
            }

            for (Map.Entry<String, List<ProductoReporte>> categoria : porCategoria.entrySet()) {
                ticket.append(categoria.getKey().toUpperCase()).append("\n");

                double kilosCategoria = 0.0;
                double totalCategoria = 0.0;
                for (ProductoReporte productoFila : categoria.getValue()) {
                    ticket.append(ajustar(productoFila.getNombre(), 16))
                            .append(ajustar(formatoKilos.format(productoFila.getKilos()) + "kg", 8))
                            .append("$").append(Math.round(productoFila.getTotal()))
                            .append("\n");
                    kilosCategoria += productoFila.getKilos();
                    totalCategoria += productoFila.getTotal();
                }
                ticket.append(fila("Subtotal kilos", formatoKilos.format(kilosCategoria) + " kg")).append("\n");
                ticket.append(fila("Subtotal $", "$" + formatoMoneda.format(totalCategoria))).append("\n");
                ticket.append(lineaSeparadora()).append("\n");
            }

            if (soloCategoria == null) {
                ticket.append(fila("Total kilos", lblTotalKilosProductos.getText())).append("\n");
                ticket.append(fila("Total facturado", lblTotalFacturadoProductos.getText())).append("\n");
            } else {
                // Con una sola categoría el subtotal es el total; se aclara de todas formas
                double kilos = filas.stream().mapToDouble(ProductoReporte::getKilos).sum();
                double total = filas.stream().mapToDouble(ProductoReporte::getTotal).sum();
                ticket.append(fila("Total kilos", formatoKilos.format(kilos) + " kg")).append("\n");
                ticket.append(fila("Total " + ajustar(soloCategoria, 12).trim(), "$" + formatoMoneda.format(total))).append("\n");
            }
            ticket.append("\n\n\n");

            imprimirEnSegundoPlano(origen, ticket.toString());

        } catch (Exception e) {
            e.printStackTrace();
            mostrarError("Error de impresión", "No se pudo imprimir el reporte de productos.");
        }
    }

    /** Ajusta un texto a un ancho fijo, truncando o rellenando con espacios a la derecha. */
    private String ajustar(String texto, int largo) {
        if (texto.length() >= largo) {
            return texto.substring(0, largo);
        }
        return String.format("%-" + largo + "s", texto);
    }

    /** Acumulador mutable de kilos/importe vendidos por producto en un rango de fechas. */
    private static class ProductoAcumulado {
        final String nombre;
        final String tipo;
        double kilos = 0.0;
        double total = 0.0;

        ProductoAcumulado(String nombre, String tipo) {
            this.nombre = nombre;
            this.tipo = tipo;
        }
    }

    /** Fila de la tabla de reporte de productos: producto, categoría, kilos, precio promedio y total vendido. */
    public static class ProductoReporte {
        private final String nombre;
        private final String tipo;
        private final Double kilos;
        private final Double precioPromedio;
        private final Double total;

        public ProductoReporte(String nombre, String tipo, Double kilos, Double precioPromedio, Double total) {
            this.nombre = nombre;
            this.tipo = tipo;
            this.kilos = kilos;
            this.precioPromedio = precioPromedio;
            this.total = total;
        }

        public String getNombre() {
            return nombre;
        }

        public String getTipo() {
            return tipo;
        }

        public Double getKilos() {
            return kilos;
        }

        public Double getPrecioPromedio() {
            return precioPromedio;
        }

        public Double getTotal() {
            return total;
        }
    }

    /** Fila de la tabla de Reportes y Balance: concepto con su total y desglose por medio de pago. */
    public static class FilaReporte {
        private final String concepto;
        private final Double total;
        private final Double efectivo;
        private final Double virtual;
        private final boolean balance;

        public FilaReporte(String concepto, Double total, Double efectivo, Double virtual, boolean balance) {
            this.concepto = concepto;
            this.total = total;
            this.efectivo = efectivo;
            this.virtual = virtual;
            this.balance = balance;
        }

        public String getConcepto() {
            return concepto;
        }

        public Double getTotal() {
            return total;
        }

        public Double getEfectivo() {
            return efectivo;
        }

        public Double getVirtual() {
            return virtual;
        }

        public boolean isBalance() {
            return balance;
        }
    }

    /** Alinea etiqueta a la izquierda y valor a la derecha en el ancho de ticket (32 columnas). */
    private String fila(String etiqueta, String valor) {
        int ancho = 32;
        int espacios = Math.max(1, ancho - etiqueta.length() - valor.length());
        return etiqueta + " ".repeat(espacios) + valor;
    }

    private String lineaSeparadora() {
        return "-".repeat(32);
    }

    // --- Actualización de Tablas y Limpieza ---

    private void limpiarCamposGasto() {
        gastoEnEdicion = null;
        dpFechaGasto.setValue(LocalDate.now());
        txtDescripcionGasto.clear();
        cbCategoriaGasto.getSelectionModel().selectFirst();
        cbMedioPagoGasto.getSelectionModel().selectFirst();
        txtMontoGasto.clear();
        actualizarModoFormularioGasto();
    }

    private String centrar(String texto) {
        int ancho = 32;
        int espacios = Math.max(0, (ancho - texto.length()) / 2);
        return " ".repeat(espacios) + texto;
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

    // --- Utilidades de alertas ---

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

    private void mostrarInfo(String titulo, String mensaje) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        DialogoUtil.aplicarEstilo(alert);
        alert.setTitle(titulo);
        alert.setHeaderText(null);
        alert.setContentText(mensaje);
        alert.showAndWait();
    }

    private boolean mostrarConfirmacion(String titulo, String mensaje) {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        DialogoUtil.aplicarEstilo(alert);
        alert.setTitle(titulo);
        alert.setHeaderText(null);
        alert.setContentText(mensaje);
        return alert.showAndWait().orElse(ButtonType.CANCEL) == ButtonType.OK;
    }
}
