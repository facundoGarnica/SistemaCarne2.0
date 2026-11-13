package controller;

import Dto.AlertaDTO;
import Dto.ResumenGananciaRealDTO;
import Util.CalculadoraGananciaReal;
import Util.DialogoUtil;
import dao.CierreInventarioDAO;
import dao.CompraDAO;
import dao.ConfiguracionGananciaRealDAO;
import dao.RendimientoConfigDAO;
import java.text.DecimalFormat;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import javafx.beans.property.SimpleObjectProperty;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.control.cell.TextFieldTableCell;
import javafx.scene.layout.VBox;
import javafx.util.converter.DoubleStringConverter;
import model.CierreInventario;
import model.Compra;
import model.ConfiguracionGananciaReal;
import model.RendimientoConfig;

/**
 * Pantalla de "Ganancia Real": carga de compras de mercadería, cierre de
 * stock por categoría, resumen de ganancia real + CMV del período con sus
 * alertas, y configuración de los porcentajes de rendimiento usados en el
 * cálculo (todo alimenta a {@link CalculadoraGananciaReal}, reutilizado
 * también por el Resumen de Caja en {@link GastosController}).
 */
public class GananciaRealController {

    private static final List<String> CATEGORIAS = List.of("Carniceria", "Pollo", "Cerdo");

    // --- Registrar compra ---
    @FXML private DatePicker dpFechaCompra, dpDesdeCompra, dpHastaCompra;
    @FXML private ComboBox<String> cbCategoriaCompra;
    @FXML private TextField txtUnidadesCompra, txtKgCompra, txtCostoCompra;
    @FXML private TableView<Compra> tvCompras;
    @FXML private TableColumn<Compra, LocalDate> colCompraFecha;
    @FXML private TableColumn<Compra, String> colCompraCategoria;
    @FXML private TableColumn<Compra, Integer> colCompraUnidades;
    @FXML private TableColumn<Compra, Double> colCompraKg, colCompraCosto;
    @FXML private Button btnBorrarCompra;

    // --- Cierre de stock ---
    @FXML private DatePicker dpFechaCierre, dpDesdeCierre, dpHastaCierre;
    @FXML private ComboBox<String> cbCategoriaCierre;
    @FXML private TextField txtKgRestantes;
    @FXML private TableView<CierreInventario> tvCierres;
    @FXML private TableColumn<CierreInventario, LocalDate> colCierreFecha;
    @FXML private TableColumn<CierreInventario, String> colCierreCategoria;
    @FXML private TableColumn<CierreInventario, Double> colCierreKg;
    @FXML private Button btnBorrarCierre;

    // --- Resumen y alertas ---
    @FXML private DatePicker dpInicioResumen, dpFinResumen;
    @FXML private VBox panelResumenDatos;
    @FXML private Label lblResVentas, lblResCMV, lblResGastos, lblResSueldos;
    @FXML private Label lblResGananciaReal, lblResMargenReal, lblResGananciaSobreCosto;
    @FXML private Label lblNotaResumen, lblSinAlertas;
    @FXML private VBox contenedorAlertas;

    // --- Configuración de rendimiento ---
    @FXML private TableView<RendimientoConfig> tvConfigRendimiento;
    @FXML private TableColumn<RendimientoConfig, String> colCatConfig;
    @FXML private TableColumn<RendimientoConfig, Double> colAprovechableConfig, colUmbralConfig, colCostoRefConfig;
    @FXML private TextField txtUmbralDesviacionPrecio;

    // --- DAOs ---
    private final CompraDAO compraDAO = new CompraDAO();
    private final CierreInventarioDAO cierreDAO = new CierreInventarioDAO();
    private final RendimientoConfigDAO rendimientoDAO = new RendimientoConfigDAO();
    private final ConfiguracionGananciaRealDAO configDAO = new ConfiguracionGananciaRealDAO();

    private final DecimalFormat formatoMoneda = new DecimalFormat("#,##0");
    private final DecimalFormat formatoKilos = new DecimalFormat("#,##0.00");
    private final DecimalFormat formatoPorcentaje = new DecimalFormat("#0.0");

    @FXML
    public void initialize() {
        configurarTablas();

        cbCategoriaCompra.setItems(FXCollections.observableArrayList(CATEGORIAS));
        cbCategoriaCierre.setItems(FXCollections.observableArrayList(CATEGORIAS));

        dpFechaCompra.setValue(LocalDate.now());
        dpDesdeCompra.setValue(LocalDate.now().with(DayOfWeek.MONDAY));
        dpHastaCompra.setValue(LocalDate.now());

        dpFechaCierre.setValue(LocalDate.now());
        dpDesdeCierre.setValue(LocalDate.now().with(DayOfWeek.MONDAY));
        dpHastaCierre.setValue(LocalDate.now());

        dpInicioResumen.setValue(LocalDate.now().with(DayOfWeek.MONDAY));
        dpFinResumen.setValue(LocalDate.now());

        filtrarCompras();
        filtrarCierres();
        cargarConfiguracionRendimiento();
        calcularResumen();

        tvCompras.getSelectionModel().selectedItemProperty().addListener((obs, old, sel) ->
                btnBorrarCompra.setDisable(sel == null));
        tvCierres.getSelectionModel().selectedItemProperty().addListener((obs, old, sel) ->
                btnBorrarCierre.setDisable(sel == null));
    }

    private void configurarTablas() {
        colCompraFecha.setCellValueFactory(new PropertyValueFactory<>("fecha"));
        colCompraCategoria.setCellValueFactory(new PropertyValueFactory<>("categoria"));
        colCompraUnidades.setCellValueFactory(new PropertyValueFactory<>("unidades"));
        colCompraKg.setCellValueFactory(new PropertyValueFactory<>("pesoTotalKg"));
        colCompraCosto.setCellValueFactory(new PropertyValueFactory<>("costoTotal"));
        colCompraKg.setCellFactory(col -> celdaKilos());
        colCompraCosto.setCellFactory(col -> celdaMoneda());

        colCierreFecha.setCellValueFactory(new PropertyValueFactory<>("fechaCierre"));
        colCierreCategoria.setCellValueFactory(new PropertyValueFactory<>("categoria"));
        colCierreKg.setCellValueFactory(new PropertyValueFactory<>("kgRestantes"));
        colCierreKg.setCellFactory(col -> celdaKilos());

        // Tabla de configuración: editable, los % se muestran en escala 0-100 aunque
        // se persisten como fracción (0-1) en RendimientoConfig.
        colCatConfig.setCellValueFactory(new PropertyValueFactory<>("categoria"));

        colAprovechableConfig.setCellValueFactory(c -> new SimpleObjectProperty<>(
                c.getValue().getPorcentajeAprovechable() != null ? c.getValue().getPorcentajeAprovechable() * 100 : 0.0));
        colAprovechableConfig.setCellFactory(TextFieldTableCell.forTableColumn(new DoubleStringConverter()));
        colAprovechableConfig.setOnEditCommit(evt ->
                evt.getRowValue().setPorcentajeAprovechable(evt.getNewValue() / 100.0));

        colUmbralConfig.setCellValueFactory(c -> new SimpleObjectProperty<>(
                c.getValue().getUmbralAlertaPorcentaje() != null ? c.getValue().getUmbralAlertaPorcentaje() * 100 : 0.0));
        colUmbralConfig.setCellFactory(TextFieldTableCell.forTableColumn(new DoubleStringConverter()));
        colUmbralConfig.setOnEditCommit(evt ->
                evt.getRowValue().setUmbralAlertaPorcentaje(evt.getNewValue() / 100.0));

        colCostoRefConfig.setCellValueFactory(c -> new SimpleObjectProperty<>(
                c.getValue().getCostoReferenciaKg() != null ? c.getValue().getCostoReferenciaKg() : 0.0));
        colCostoRefConfig.setCellFactory(TextFieldTableCell.forTableColumn(new DoubleStringConverter()));
        colCostoRefConfig.setOnEditCommit(evt ->
                evt.getRowValue().setCostoReferenciaKg(evt.getNewValue()));
    }

    private <T> TableCell<T, Double> celdaMoneda() {
        return new TableCell<>() {
            @Override
            protected void updateItem(Double item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty || item == null ? null : "$" + formatoMoneda.format(item));
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

    // --- Registrar compra ---

    @FXML
    private void guardarCompra() {
        LocalDate fecha = dpFechaCompra.getValue();
        String categoria = cbCategoriaCompra.getValue();

        if (fecha == null || categoria == null) {
            mostrarAdvertencia("Faltan datos", "Seleccioná la fecha y la categoría de la compra.");
            return;
        }

        try {
            Compra compra = new Compra();
            compra.setFecha(fecha);
            compra.setCategoria(categoria);
            compra.setUnidades(txtUnidadesCompra.getText().isBlank() ? null : Integer.parseInt(txtUnidadesCompra.getText().trim()));
            compra.setPesoTotalKg(Double.parseDouble(txtKgCompra.getText().trim()));
            compra.setCostoTotal(Double.parseDouble(txtCostoCompra.getText().trim()));

            compraDAO.guardar(compra);
            limpiarCamposCompra();
            filtrarCompras();
            mostrarInfo("Compra registrada", "La compra fue guardada correctamente.");
        } catch (NumberFormatException e) {
            mostrarError("Datos inválidos", "Unidades, kilos y costo deben ser números válidos.");
        }
    }

    @FXML
    private void filtrarCompras() {
        LocalDate desde = dpDesdeCompra.getValue();
        LocalDate hasta = dpHastaCompra.getValue();

        if (desde == null || hasta == null) {
            mostrarAdvertencia("Faltan fechas", "Seleccioná las fechas 'Desde' y 'Hasta' para filtrar las compras.");
            return;
        }
        if (desde.isAfter(hasta)) {
            mostrarAdvertencia("Rango inválido", "La fecha 'Desde' no puede ser posterior a la fecha 'Hasta'.");
            return;
        }

        List<Compra> lista = compraDAO.buscarPorRango(desde, hasta);
        if (lista != null) tvCompras.setItems(FXCollections.observableArrayList(lista));
    }

    @FXML
    private void borrarCompra() {
        Compra seleccionada = tvCompras.getSelectionModel().getSelectedItem();
        if (seleccionada == null) {
            return;
        }

        boolean confirmado = mostrarConfirmacion(
                "Confirmar eliminación",
                "¿Eliminar la compra de " + seleccionada.getCategoria() + " del " + seleccionada.getFecha() + "?"
        );
        if (!confirmado) {
            return;
        }

        compraDAO.eliminar(seleccionada.getId());
        filtrarCompras();
    }

    private void limpiarCamposCompra() {
        dpFechaCompra.setValue(LocalDate.now());
        cbCategoriaCompra.setValue(null);
        txtUnidadesCompra.clear();
        txtKgCompra.clear();
        txtCostoCompra.clear();
    }

    // --- Cierre de stock ---

    @FXML
    private void guardarCierre() {
        LocalDate fecha = dpFechaCierre.getValue();
        String categoria = cbCategoriaCierre.getValue();

        if (fecha == null || categoria == null) {
            mostrarAdvertencia("Faltan datos", "Seleccioná la fecha y la categoría del cierre.");
            return;
        }

        try {
            double kg = Double.parseDouble(txtKgRestantes.getText().trim());

            CierreInventario existente = cierreDAO.buscarPorFechaYCategoria(fecha, categoria);
            if (existente != null) {
                boolean confirmado = mostrarConfirmacion(
                        "Cierre ya cargado",
                        "Ya hay un cierre de " + categoria + " para el " + fecha + " ("
                        + formatoKilos.format(existente.getKgRestantes()) + " kg). ¿Reemplazarlo?"
                );
                if (!confirmado) {
                    return;
                }
                cierreDAO.eliminar(existente.getId());
            }

            CierreInventario cierre = new CierreInventario();
            cierre.setFechaCierre(fecha);
            cierre.setCategoria(categoria);
            cierre.setKgRestantes(kg);
            cierreDAO.guardar(cierre);

            txtKgRestantes.clear();
            filtrarCierres();
            mostrarInfo("Cierre registrado", "El cierre de stock fue guardado correctamente.");
        } catch (NumberFormatException e) {
            mostrarError("Dato inválido", "Los kilos restantes deben ser un número válido.");
        }
    }

    @FXML
    private void filtrarCierres() {
        LocalDate desde = dpDesdeCierre.getValue();
        LocalDate hasta = dpHastaCierre.getValue();

        if (desde == null || hasta == null) {
            mostrarAdvertencia("Faltan fechas", "Seleccioná las fechas 'Desde' y 'Hasta' para filtrar los cierres.");
            return;
        }
        if (desde.isAfter(hasta)) {
            mostrarAdvertencia("Rango inválido", "La fecha 'Desde' no puede ser posterior a la fecha 'Hasta'.");
            return;
        }

        List<CierreInventario> lista = cierreDAO.buscarPorRango(desde, hasta);
        if (lista != null) tvCierres.setItems(FXCollections.observableArrayList(lista));
    }

    @FXML
    private void borrarCierre() {
        CierreInventario seleccionado = tvCierres.getSelectionModel().getSelectedItem();
        if (seleccionado == null) {
            return;
        }

        boolean confirmado = mostrarConfirmacion(
                "Confirmar eliminación",
                "¿Eliminar el cierre de " + seleccionado.getCategoria() + " del " + seleccionado.getFechaCierre() + "?"
        );
        if (!confirmado) {
            return;
        }

        cierreDAO.eliminar(seleccionado.getId());
        filtrarCierres();
    }

    // --- Resumen y alertas ---

    @FXML
    private void calcularResumen() {
        LocalDate inicio = dpInicioResumen.getValue();
        LocalDate fin = dpFinResumen.getValue();

        if (inicio == null || fin == null) {
            mostrarAdvertencia("Faltan fechas", "Seleccioná las fechas 'Desde' y 'Hasta' para calcular el resumen.");
            return;
        }
        if (inicio.isAfter(fin)) {
            mostrarAdvertencia("Rango inválido", "La fecha 'Desde' no puede ser posterior a la fecha 'Hasta'.");
            return;
        }

        ResumenGananciaRealDTO resumen = CalculadoraGananciaReal.calcularResumen(inicio, fin);

        panelResumenDatos.setVisible(resumen.isTieneDatos());
        panelResumenDatos.setManaged(resumen.isTieneDatos());
        lblNotaResumen.setVisible(!resumen.isTieneDatos());
        lblNotaResumen.setManaged(!resumen.isTieneDatos());

        if (resumen.isTieneDatos()) {
            lblResVentas.setText("$" + formatoMoneda.format(resumen.getTotalVentas()));
            lblResCMV.setText("$" + formatoMoneda.format(resumen.getCmv()));
            lblResGastos.setText("$" + formatoMoneda.format(resumen.getTotalGastos()));
            lblResSueldos.setText("$" + formatoMoneda.format(resumen.getTotalSueldos()));
            lblResGananciaReal.setText("$" + formatoMoneda.format(resumen.getGananciaReal()));
            lblResMargenReal.setText(formatoPorcentaje.format(resumen.getMargenReal()) + "%");
            lblResGananciaSobreCosto.setText(formatoPorcentaje.format(resumen.getGananciaSobreCosto()) + "%");
            lblResGananciaReal.setStyle(resumen.getGananciaReal() < 0 ? "-fx-text-fill: #ff4d4d;" : "-fx-text-fill: #2ecc71;");
        }

        actualizarListaAlertas(resumen.getAlertas());
    }

    @FXML
    private void mostrarResumenSemanal() {
        dpInicioResumen.setValue(LocalDate.now().with(DayOfWeek.MONDAY));
        dpFinResumen.setValue(LocalDate.now());
        calcularResumen();
    }

    private void actualizarListaAlertas(List<AlertaDTO> alertas) {
        contenedorAlertas.getChildren().clear();

        boolean hayAlertas = alertas != null && !alertas.isEmpty();
        lblSinAlertas.setVisible(!hayAlertas);
        lblSinAlertas.setManaged(!hayAlertas);
        if (!hayAlertas) {
            return;
        }

        for (AlertaDTO alerta : alertas) {
            Label lbl = new Label(alerta.getMensaje());
            lbl.getStyleClass().add("alerta-item");
            lbl.setWrapText(true);
            lbl.setMaxWidth(Double.MAX_VALUE);
            contenedorAlertas.getChildren().add(lbl);
        }
    }

    // --- Configuración de rendimiento ---

    private void cargarConfiguracionRendimiento() {
        List<RendimientoConfig> lista = new ArrayList<>();
        for (String categoria : CATEGORIAS) {
            lista.add(rendimientoDAO.obtenerOCrearDefault(categoria));
        }
        tvConfigRendimiento.setItems(FXCollections.observableArrayList(lista));

        ConfiguracionGananciaReal configPrecio = configDAO.obtenerOCrear();
        double umbral = configPrecio.getUmbralDesviacionPrecio() != null ? configPrecio.getUmbralDesviacionPrecio() : 0.03;
        txtUmbralDesviacionPrecio.setText(formatoPorcentaje.format(umbral * 100));
    }

    @FXML
    private void guardarConfigRendimiento() {
        for (RendimientoConfig config : tvConfigRendimiento.getItems()) {
            rendimientoDAO.actualizar(config);
        }
        mostrarInfo("Configuración guardada", "Los porcentajes de rendimiento fueron actualizados.");
    }

    @FXML
    private void guardarUmbralPrecio() {
        try {
            double pct = Double.parseDouble(txtUmbralDesviacionPrecio.getText().trim().replace(",", "."));
            ConfiguracionGananciaReal config = configDAO.obtenerOCrear();
            config.setUmbralDesviacionPrecio(pct / 100.0);
            configDAO.actualizar(config);
            mostrarInfo("Configuración guardada", "El umbral de desviación de precio fue actualizado.");
        } catch (NumberFormatException e) {
            mostrarError("Dato inválido", "El umbral debe ser un número válido (ej: 3 para 3%).");
        }
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
