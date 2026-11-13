package controller;

import Util.DialogoUtil;
import dao.AdelantoDAO;
import dao.EmpleadoDAO;
import java.nio.charset.StandardCharsets;
import java.text.DecimalFormat;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import javafx.application.Platform;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import model.Adelanto;
import model.Empleado;
import model.PrintTicket;
import model.TurnoEmpleados;

public class PersonalController {

    private static final String PASSWORD_ADMIN = "2808";

    // Tablas y Columnas (Turnos)
    @FXML private DatePicker dpFechaTurno;
    @FXML private DatePicker dpDesdeTurno, dpHastaTurno;
    @FXML private ComboBox<Empleado> cbEmpleado;
    @FXML private ComboBox<String> cbTurno;
    @FXML private TableView<TurnoEmpleados> tvTurnos;
    @FXML private TableColumn<TurnoEmpleados, LocalDate> colTurnoFecha;
    @FXML private TableColumn<TurnoEmpleados, String> colTurnoEmpleado, colTurnoTipo;
    @FXML private TableColumn<TurnoEmpleados, Double> colTurnoMonto;
    @FXML private Button btnBorrarTurno;
    @FXML private ToggleGroup grupoPeriodoTurnos;
    @FXML private ToggleButton tglTurnosHoy, tglTurnosSemana, tglTurnosRango;
    @FXML private javafx.scene.layout.HBox boxRangoTurnos;
    @FXML private Label lblContadorTurnos;

    // Tablas y Columnas (Gestión Empleados)
    @FXML private TextField txtNombreEmpleado, txtPagoManiana, txtPagoTarde;
    @FXML private ComboBox<String> cbMedioPagoEmpleado;
    @FXML private TableView<Empleado> tvGestionEmpleados;
    @FXML private TableColumn<Empleado, String> colGestionNombre;
    @FXML private TableColumn<Empleado, Double> colGestionManiana, colGestionTarde;
    @FXML private TableColumn<Empleado, String> colTokenM, colTokenT, colGestionMedioPago;
    @FXML private Button btnEditarEmpleado, btnRehacerToken, btnImprimirM, btnImprimirT, btnDarBaja;

    // Tablas y Columnas (Adelantos)
    @FXML private DatePicker dpFechaAdelanto, dpDesdeAdelanto, dpHastaAdelanto;
    @FXML private ComboBox<String> cbMedioPagoAdelanto;
    @FXML private TextField txtDescripcionAdelanto, txtMontoAdelanto;
    @FXML private Label lblTotalTrabajadoAdelanto, lblTotalAdelantadoAdelanto, lblNetoAdelanto;
    @FXML private ToggleGroup grupoPeriodoAdelantos;
    @FXML private ToggleButton tglAdelantosRango;
    @FXML private javafx.scene.layout.HBox boxRangoAdelantos;
    @FXML private Label lblSinEmpleadoAdelanto, lblEmpleadoAdelanto, lblLeQuedanAdelanto, lblNuevoAdelantoTitulo;
    @FXML private javafx.scene.layout.VBox panelEmpleadoAdelanto, boxAdelantosEmpleado;
    @FXML private Button btnGuardarAdelanto, btnCancelarEdicionAdelanto;

    private static final javafx.css.PseudoClass PSEUDO_TOTAL = javafx.css.PseudoClass.getPseudoClass("total");
    /** Adelantos del período cargado (para el panel del empleado seleccionado). */
    private List<Adelanto> adelantosPeriodo = new ArrayList<>();
    /** Adelanto que se está editando en el formulario (null = alta de uno nuevo). */
    private Adelanto adelantoEnEdicion;
    /** true mientras se muestran todos los adelantos ("Ver todo"): ninguna pill queda activa. */
    private boolean mostrandoTodosLosAdelantos = false;
    @FXML private TableView<SaldoEmpleado> tvSaldoEmpleados;
    @FXML private TableColumn<SaldoEmpleado, String> colSaldoEmpleado;
    @FXML private TableColumn<SaldoEmpleado, Double> colSaldoTrabajado, colSaldoAdelantado, colSaldoNeto;

    // Resumen semanal de empleado (para imprimir y entregar)
    @FXML private DatePicker dpDesdeResumen, dpHastaResumen;
    @FXML private ToggleGroup grupoPeriodoResumen;
    @FXML private ToggleButton tglResumenRango;
    @FXML private javafx.scene.layout.HBox boxRangoResumen;
    @FXML private ListView<ResumenEmpleado> lvEmpleadosResumen;
    @FXML private Label lblSinEmpleadoResumen, lblCompNombre, lblCompPeriodoTitulo, lblCompPeriodo;
    @FXML private Label lblCompTrabajado, lblCompAdelantos, lblCompNeto, lblCompCantTurnos, lblCompCantAdelantos;
    @FXML private javafx.scene.layout.VBox panelComprobante, boxCompTurnos, boxCompAdelantos;
    @FXML private Button btnImprimirResumen;

    // --- DAOs ---
    private final EmpleadoDAO empleadoDAO = new EmpleadoDAO();
    private final AdelantoDAO adelantoDAO = new AdelantoDAO();

    private final DecimalFormat formatoMoneda = new DecimalFormat("#,##0", java.text.DecimalFormatSymbols.getInstance(new java.util.Locale("es", "AR")));
    private static final DateTimeFormatter FORMATO_DIA_MES = DateTimeFormatter.ofPattern("dd/MM");

    @FXML
    public void initialize() {
        configurarTablas();
        cargarDatosIniciales();

        dpFechaTurno.setValue(LocalDate.now());
        dpDesdeTurno.setValue(LocalDate.now().with(DayOfWeek.MONDAY));
        dpHastaTurno.setValue(LocalDate.now());
        configurarPeriodoTurnos();
        dpFechaAdelanto.setValue(LocalDate.now());
        dpDesdeAdelanto.setValue(LocalDate.now().with(DayOfWeek.MONDAY));
        dpHastaAdelanto.setValue(LocalDate.now());
        configurarPeriodoAdelantos();
        configurarResumen();
        dpDesdeResumen.setValue(LocalDate.now().with(DayOfWeek.MONDAY));
        dpHastaResumen.setValue(LocalDate.now().with(DayOfWeek.MONDAY).plusDays(6));
        generarResumenSemanal();

        filtrarTurnos();
        filtrarAdelantos();

        tvGestionEmpleados.getSelectionModel().selectedItemProperty().addListener((obs, oldSelection, seleccionado) -> {
            boolean haySeleccion = seleccionado != null;
            btnEditarEmpleado.setDisable(!haySeleccion);
            btnRehacerToken.setDisable(!haySeleccion);
            btnImprimirM.setDisable(!haySeleccion);
            btnImprimirT.setDisable(!haySeleccion);
            btnDarBaja.setDisable(!haySeleccion);

            if (seleccionado != null) {
                txtNombreEmpleado.setText(seleccionado.getNombre());
                txtPagoManiana.setText(String.valueOf(seleccionado.getPagoManiana()));
                txtPagoTarde.setText(String.valueOf(seleccionado.getPagoTarde()));
                cbMedioPagoEmpleado.setValue(seleccionado.getMedioPago());
            }
        });

        tvTurnos.getSelectionModel().selectedItemProperty().addListener((obs, oldSelection, seleccionado) ->
                btnBorrarTurno.setDisable(seleccionado == null));

        // Al elegir un empleado en la tabla de saldos se muestra su panel de adelantos
        tvSaldoEmpleados.getSelectionModel().selectedItemProperty().addListener((obs, anterior, seleccionado) -> {
            if (seleccionado != null && seleccionado.total()) {
                // La fila "Total" no es seleccionable
                Platform.runLater(() -> tvSaldoEmpleados.getSelectionModel().clearSelection());
                return;
            }
            mostrarPanelEmpleadoAdelanto(seleccionado);
        });
    }

    private void configurarTablas() {
        // Configuración tabla Turnos
        colTurnoFecha.setCellValueFactory(cellData -> new SimpleObjectProperty<>(cellData.getValue().getFecha()));
        colTurnoTipo.setCellValueFactory(cellData -> new SimpleStringProperty(cellData.getValue().getTurno()));
        colTurnoMonto.setCellValueFactory(cellData -> new SimpleObjectProperty<>(cellData.getValue().getMontoHistorial()));
        colTurnoEmpleado.setCellValueFactory(cellData ->
            new SimpleStringProperty(cellData.getValue().getEmpleado() != null ?
                                     cellData.getValue().getEmpleado().getNombre() : "N/A"));
        colTurnoMonto.setCellFactory(column -> celdaMoneda());
        colTurnoFecha.setCellFactory(column -> new TableCell<>() {
            @Override
            protected void updateItem(LocalDate item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty || item == null ? null : item.format(FORMATO_DIA_MES));
            }
        });
        // Turno como "chip"
        colTurnoTipo.setCellFactory(column -> new TableCell<>() {
            private final Label chip = new Label();
            {
                chip.getStyleClass().add("ps-chip");
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

        // Configuración Tabla Gestión de Empleados
        colGestionNombre.setCellValueFactory(new PropertyValueFactory<>("nombre"));
        colGestionManiana.setCellValueFactory(new PropertyValueFactory<>("pagoManiana"));
        colGestionTarde.setCellValueFactory(new PropertyValueFactory<>("pagoTarde"));
        colGestionManiana.setCellFactory(column -> celdaMoneda());
        colGestionTarde.setCellFactory(column -> celdaMoneda());
        colTokenM.setCellValueFactory(new PropertyValueFactory<>("tokenM"));
        colTokenT.setCellValueFactory(new PropertyValueFactory<>("tokenT"));
        colGestionMedioPago.setCellValueFactory(new PropertyValueFactory<>("medioPago"));

        // Configuración tabla Saldo por empleado (pestaña Adelantos)
        colSaldoEmpleado.setCellValueFactory(cellData -> new SimpleStringProperty(cellData.getValue().empleado()));
        colSaldoTrabajado.setCellValueFactory(cellData -> new SimpleObjectProperty<>(cellData.getValue().trabajado()));
        colSaldoAdelantado.setCellValueFactory(cellData -> new SimpleObjectProperty<>(cellData.getValue().adelantado()));
        colSaldoNeto.setCellValueFactory(cellData -> new SimpleObjectProperty<>(cellData.getValue().neto()));
        colSaldoTrabajado.setCellFactory(column -> celdaMoneda());
        // Adelantado: "−$10.000", o una raya si no pidió nada
        colSaldoAdelantado.setCellFactory(column -> new TableCell<>() {
            @Override
            protected void updateItem(Double item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                } else {
                    setText(item == 0 ? "—" : "−$" + formatoMoneda.format(item));
                }
            }
        });
        colSaldoNeto.setCellFactory(column -> new TableCell<>() {
            @Override
            protected void updateItem(Double item, boolean empty) {
                super.updateItem(item, empty);
                getStyleClass().remove("ps-negativo");
                if (empty || item == null) {
                    setText(null);
                    return;
                }
                setText(item < 0 ? "−$" + formatoMoneda.format(-item) : "$" + formatoMoneda.format(item));
                if (item < 0) getStyleClass().add("ps-negativo");
            }
        });
        // La última fila es el total: se pinta distinto
        tvSaldoEmpleados.setRowFactory(tv -> new TableRow<>() {
            @Override
            protected void updateItem(SaldoEmpleado item, boolean empty) {
                super.updateItem(item, empty);
                pseudoClassStateChanged(PSEUDO_TOTAL, !empty && item != null && item.total());
            }
        });
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

    private void cargarDatosIniciales() {
        try {
            List<Empleado> empleados = empleadoDAO.buscarTodos();
            if (empleados != null) {
                cbEmpleado.setItems(FXCollections.observableArrayList(empleados));
            }
            cbTurno.setItems(FXCollections.observableArrayList("Mañana", "Tarde"));

            cbMedioPagoEmpleado.setItems(FXCollections.observableArrayList("Efectivo", "Transferencia"));
            cbMedioPagoEmpleado.getSelectionModel().selectFirst();

            cbMedioPagoAdelanto.setItems(FXCollections.observableArrayList("Efectivo", "Transferencia"));
            cbMedioPagoAdelanto.getSelectionModel().selectFirst();

            actualizarTablaGestionEmpleados();
        } catch (Exception e) {
            System.err.println("Error al cargar datos iniciales: " + e.getMessage());
        }
    }

    // --- Lógica de Empleados y Turnos ---

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

    @FXML
    private void agregarEmpleado() {
        if (txtNombreEmpleado.getText().isBlank()) {
            mostrarAdvertencia("Falta el nombre", "Ingresá el nombre del empleado.");
            return;
        }

        try {
            Empleado nuevo = new Empleado();

            nuevo.setNombre(txtNombreEmpleado.getText().trim());
            nuevo.setPagoManiana(Double.parseDouble(txtPagoManiana.getText()));
            nuevo.setPagoTarde(Double.parseDouble(txtPagoTarde.getText()));
            nuevo.setMedioPago(cbMedioPagoEmpleado.getValue());

            // Guardar primero para obtener ID
            empleadoDAO.guardar(nuevo);

            // Generar tokens
            nuevo.setTokenM(generarToken(nuevo.getId(), "Mañana"));
            nuevo.setTokenT(generarToken(nuevo.getId(), "Tarde"));

            empleadoDAO.actualizar(nuevo);

            txtNombreEmpleado.clear();
            txtPagoManiana.clear();
            txtPagoTarde.clear();
            cbMedioPagoEmpleado.getSelectionModel().selectFirst();

            actualizarTablaGestionEmpleados();
            recargarCombosEmpleado();

        } catch (NumberFormatException e) {
            mostrarError("Datos inválidos", "Los sueldos de mañana y tarde deben ser números válidos.");
        } catch (Exception e) {
            e.printStackTrace();
            mostrarError("Error", "No se pudo guardar el empleado.");
        }
    }

    @FXML
    private void editarEmpleado() {
        Empleado seleccionado = tvGestionEmpleados.getSelectionModel().getSelectedItem();
        if (seleccionado == null) {
            mostrarAdvertencia("Sin selección", "Seleccione un empleado para editar.");
            return;
        }

        if (!validarPasswordAdmin()) {
            mostrarError("Acceso denegado", "Contraseña incorrecta.");
            return;
        }

        try {
            seleccionado.setNombre(txtNombreEmpleado.getText());
            seleccionado.setPagoManiana(Double.parseDouble(txtPagoManiana.getText()));
            seleccionado.setPagoTarde(Double.parseDouble(txtPagoTarde.getText()));
            seleccionado.setMedioPago(cbMedioPagoEmpleado.getValue());

            empleadoDAO.actualizar(seleccionado);
            actualizarTablaGestionEmpleados();
            recargarCombosEmpleado();

            txtNombreEmpleado.clear();
            txtPagoManiana.clear();
            txtPagoTarde.clear();
            cbMedioPagoEmpleado.getSelectionModel().selectFirst();
            tvGestionEmpleados.getSelectionModel().clearSelection();

            mostrarInfo("Empleado actualizado", "Los datos del empleado fueron actualizados.");

        } catch (NumberFormatException e) {
            mostrarError("Error", "Los sueldos deben ser números válidos.");
        } catch (Exception e) {
            e.printStackTrace();
            mostrarError("Error", "No se pudo actualizar el empleado.");
        }
    }

    @FXML
    private void eliminarEmpleado() {
        Empleado seleccionado = tvGestionEmpleados.getSelectionModel().getSelectedItem();
        if (seleccionado == null) {
            mostrarAdvertencia("Sin selección", "Seleccione un empleado para eliminar.");
            return;
        }

        if (!validarPasswordAdmin()) {
            mostrarError("Acceso denegado", "Contraseña incorrecta.");
            return;
        }

        boolean confirmado = mostrarConfirmacion(
                "Confirmar eliminación",
                "¿Desea eliminar al empleado " + seleccionado.getNombre() + "?\n\n"
                + "También se eliminarán todos sus turnos."
        );
        if (!confirmado) {
            return;
        }

        try {
            empleadoDAO.eliminar(seleccionado.getId());

            actualizarTablaGestionEmpleados();
            recargarCombosEmpleado();

            mostrarInfo("Empleado eliminado", "El empleado fue eliminado correctamente.");

        } catch (Exception e) {
            e.printStackTrace();
            mostrarError("Error", "No se pudo eliminar el empleado.");
        }
    }

    private void recargarCombosEmpleado() {
        List<Empleado> empleados = empleadoDAO.buscarTodos();
        cbEmpleado.setItems(FXCollections.observableArrayList(empleados));
        filtrarAdelantos();
        generarResumenSemanal();
    }

    private String generarToken(Long idEmpleado, String turno) {
        String prefijo = "99";
        String id = String.format("%04d", idEmpleado);
        String tipo = turno.equalsIgnoreCase("Mañana") ? "1" : "2";
        String random = String.format("%05d", (int) (Math.random() * 100000));

        // 12 digitos base
        String base = prefijo + id + tipo + random;

        // checksum EAN13 real
        int suma = 0;
        for (int i = 0; i < 12; i++) {
            int digito = Character.getNumericValue(base.charAt(i));
            suma += (i % 2 == 0) ? digito : digito * 3;
        }

        int checksum = (10 - (suma % 10)) % 10;
        return base + checksum;
    }

    @FXML
    private void generarNuevoTokenEmpleado() {
        Empleado seleccionado = tvGestionEmpleados.getSelectionModel().getSelectedItem();
        if (seleccionado == null) {
            mostrarAdvertencia("Sin selección", "Seleccione un empleado.");
            return;
        }

        if (!validarPasswordAdmin()) {
            mostrarError("Acceso denegado", "Contraseña incorrecta.");
            return;
        }

        try {
            seleccionado.setTokenM(generarToken(seleccionado.getId(), "Mañana"));
            seleccionado.setTokenT(generarToken(seleccionado.getId(), "Tarde"));

            empleadoDAO.actualizar(seleccionado);
            actualizarTablaGestionEmpleados();

            mostrarInfo("Tokens actualizados", "Se generaron nuevos tokens para el empleado.");

        } catch (Exception e) {
            e.printStackTrace();
            mostrarError("Error", "No se pudieron generar los tokens.");
        }
    }

    @FXML
    private void guardarTurno() {
        Empleado emp = cbEmpleado.getValue();
        String tipoTurno = cbTurno.getValue();
        LocalDate fecha = dpFechaTurno.getValue();

        if (emp == null || tipoTurno == null || fecha == null) {
            mostrarAdvertencia("Faltan datos", "Seleccioná empleado, turno y fecha para registrar el turno.");
            return;
        }

        if (empleadoDAO.yaRegistroTurno(emp.getId(), fecha, tipoTurno)) {
            mostrarAdvertencia("Turno duplicado",
                    "El empleado " + emp.getNombre() + " ya tiene registrado el turno "
                    + tipoTurno + " para la fecha " + fecha);
            return;
        }

        TurnoEmpleados nuevoTurno = new TurnoEmpleados();
        nuevoTurno.setEmpleado(emp);
        nuevoTurno.setFecha(fecha);
        nuevoTurno.setTurno(tipoTurno);

        double montoAPagar = tipoTurno.equalsIgnoreCase("Mañana") ? emp.getPagoManiana() : emp.getPagoTarde();
        nuevoTurno.setMontoHistorial(montoAPagar);
        nuevoTurno.setToken(tipoTurno.equalsIgnoreCase("Mañana") ? emp.getTokenM() : emp.getTokenT());

        empleadoDAO.registrarTurno(nuevoTurno);

        filtrarTurnos();
        filtrarAdelantos();
        limpiarCamposTurno();
    }

    @FXML
    private void borrarTurno() {
        TurnoEmpleados seleccionado = tvTurnos.getSelectionModel().getSelectedItem();
        if (seleccionado == null) {
            return;
        }

        if (!validarPasswordAdmin()) {
            mostrarError("Acceso denegado", "Contraseña incorrecta.");
            return;
        }

        String nombreEmpleado = seleccionado.getEmpleado() != null ? seleccionado.getEmpleado().getNombre() : "N/A";
        boolean confirmado = mostrarConfirmacion(
                "Confirmar eliminación",
                "¿Eliminar el turno " + seleccionado.getTurno() + " de " + nombreEmpleado
                + " del " + seleccionado.getFecha() + "?"
        );
        if (!confirmado) {
            return;
        }

        empleadoDAO.eliminarTurno(seleccionado.getId());
        filtrarTurnos();
        filtrarAdelantos();
    }

    @FXML
    private void filtrarTurnos() {
        LocalDate desde = dpDesdeTurno.getValue();
        LocalDate hasta = dpHastaTurno.getValue();

        if (desde == null || hasta == null) {
            mostrarAdvertencia("Faltan fechas", "Seleccioná las fechas 'Desde' y 'Hasta' para filtrar los turnos.");
            return;
        }
        if (desde.isAfter(hasta)) {
            mostrarAdvertencia("Rango inválido", "La fecha 'Desde' no puede ser posterior a la fecha 'Hasta'.");
            return;
        }

        mostrarTurnos(empleadoDAO.obtenerTurnosPorRango(desde, hasta));
    }

    /**
     * Muestra los turnos en la tabla (más recientes primero) y actualiza el
     * contador "N turnos · $total".
     */
    private void mostrarTurnos(List<TurnoEmpleados> lista) {
        List<TurnoEmpleados> turnos = lista != null ? new ArrayList<>(lista) : new ArrayList<>();
        turnos.sort(Comparator.comparing(TurnoEmpleados::getFecha, Comparator.nullsLast(Comparator.reverseOrder()))
                .thenComparing(t -> t.getTurno() == null ? "" : t.getTurno()));
        tvTurnos.setItems(FXCollections.observableArrayList(turnos));

        double total = turnos.stream().mapToDouble(t -> t.getMontoHistorial() != null ? t.getMontoHistorial() : 0.0).sum();
        lblContadorTurnos.setText(turnos.size() + (turnos.size() == 1 ? " turno" : " turnos")
                + " · $" + formatoMoneda.format(total));
    }

    /** Siempre queda un período elegido y el selector de rango se muestra solo con "Rango…". */
    private void configurarPeriodoTurnos() {
        grupoPeriodoTurnos.selectedToggleProperty().addListener((obs, anterior, nuevo) -> {
            if (nuevo == null && anterior != null && !mostrandoTodosLosTurnos) {
                anterior.setSelected(true);
            }
            boolean esRango = grupoPeriodoTurnos.getSelectedToggle() == tglTurnosRango;
            boxRangoTurnos.setVisible(esRango);
            boxRangoTurnos.setManaged(esRango);
        });
    }

    /** true mientras se muestran todos los turnos ("Ver todo"): ninguna pill queda activa. */
    private boolean mostrandoTodosLosTurnos = false;

    @FXML
    private void mostrarTurnosDeHoy() {
        dpDesdeTurno.setValue(LocalDate.now());
        dpHastaTurno.setValue(LocalDate.now());
        filtrarTurnos();
    }

    @FXML
    private void mostrarSemanalTurnos() {
        dpDesdeTurno.setValue(LocalDate.now().with(DayOfWeek.MONDAY));
        dpHastaTurno.setValue(LocalDate.now());
        filtrarTurnos();
    }

    @FXML
    private void mostrarTurnosSemanaPasada() {
        LocalDate lunesPasado = LocalDate.now().with(DayOfWeek.MONDAY).minusWeeks(1);
        dpDesdeTurno.setValue(lunesPasado);
        dpHastaTurno.setValue(lunesPasado.plusDays(6));
        filtrarTurnos();
    }

    @FXML
    private void mostrarSelectorRangoTurnos() {
        // Deja cargado el período actual para ajustarlo y aplicar
        dpDesdeTurno.requestFocus();
    }

    @FXML
    private void actualizarTodo() {
        if (!validarPasswordAdmin()) {
            mostrarError("Acceso denegado", "Contraseña incorrecta.");
            return;
        }

        actualizarTablaTurnos();
        actualizarTablaAdelantos();
        mostrarInfo("Actualizado", "Se muestran todos los turnos y adelantos registrados.");
    }


    // --- Lógica de Adelantos ---

    /** Empleado elegido en la tabla de saldos (o null si no hay ninguno). */
    private SaldoEmpleado empleadoSeleccionadoAdelanto() {
        SaldoEmpleado s = tvSaldoEmpleados.getSelectionModel().getSelectedItem();
        return s == null || s.total() ? null : s;
    }

    /** Alta de un adelanto nuevo, o guarda los cambios si se está editando uno. */
    @FXML
    private void guardarAdelanto() {
        if (adelantoEnEdicion != null) {
            guardarEdicionAdelanto();
            return;
        }

        SaldoEmpleado saldo = empleadoSeleccionadoAdelanto();
        Empleado emp = saldo != null ? buscarEmpleadoPorId(saldo.id()) : null;
        LocalDate fecha = dpFechaAdelanto.getValue();
        String medioPago = cbMedioPagoAdelanto.getValue();
        Double monto = leerMontoAdelanto();

        if (emp == null) {
            mostrarAdvertencia("Elegí un empleado", "Seleccioná un empleado en la tabla para registrarle el adelanto.");
            return;
        }
        if (fecha == null || medioPago == null) {
            mostrarAdvertencia("Faltan datos", "Completá la fecha y el medio de pago del adelanto.");
            return;
        }
        if (monto == null) {
            return;
        }

        Adelanto adelanto = new Adelanto();
        adelanto.setEmpleado(emp);
        adelanto.setFecha(fecha);
        adelanto.setDescripcion(txtDescripcionAdelanto.getText());
        adelanto.setMedioPago(medioPago);
        adelanto.setMonto(monto);

        adelantoDAO.guardar(adelanto);
        limpiarCamposAdelanto();
        filtrarAdelantos();
    }

    /** Carga un adelanto en el formulario para editarlo (pide contraseña). */
    private void empezarEdicionAdelanto(Adelanto adelanto) {
        if (!validarPasswordAdmin()) {
            mostrarError("Acceso denegado", "Contraseña incorrecta.");
            return;
        }
        adelantoEnEdicion = adelanto;
        txtMontoAdelanto.setText(formatoMoneda.format(adelanto.getMonto() != null ? adelanto.getMonto() : 0.0));
        cbMedioPagoAdelanto.setValue(adelanto.getMedioPago());
        txtDescripcionAdelanto.setText(adelanto.getDescripcion());
        dpFechaAdelanto.setValue(adelanto.getFecha());
        actualizarModoFormularioAdelanto();
        txtMontoAdelanto.requestFocus();
    }

    private void guardarEdicionAdelanto() {
        LocalDate fecha = dpFechaAdelanto.getValue();
        String medioPago = cbMedioPagoAdelanto.getValue();
        Double monto = leerMontoAdelanto();

        if (fecha == null || medioPago == null) {
            mostrarAdvertencia("Faltan datos", "Completá la fecha y el medio de pago del adelanto.");
            return;
        }
        if (monto == null) {
            return;
        }

        adelantoEnEdicion.setFecha(fecha);
        adelantoEnEdicion.setDescripcion(txtDescripcionAdelanto.getText());
        adelantoEnEdicion.setMedioPago(medioPago);
        adelantoEnEdicion.setMonto(monto);
        adelantoDAO.actualizar(adelantoEnEdicion);

        limpiarCamposAdelanto();
        filtrarAdelantos();
    }

    @FXML
    private void cancelarEdicionAdelanto() {
        limpiarCamposAdelanto();
    }

    private void borrarAdelanto(Adelanto adelanto) {
        if (!validarPasswordAdmin()) {
            mostrarError("Acceso denegado", "Contraseña incorrecta.");
            return;
        }

        String nombreEmpleado = adelanto.getEmpleado() != null ? adelanto.getEmpleado().getNombre() : "N/A";
        boolean confirmado = mostrarConfirmacion(
                "Confirmar eliminación",
                "¿Eliminar el adelanto de " + nombreEmpleado + " por $"
                + formatoMoneda.format(adelanto.getMonto()) + "?"
        );
        if (!confirmado) {
            return;
        }

        adelantoDAO.eliminar(adelanto.getId());
        if (adelantoEnEdicion != null && adelantoEnEdicion.getId().equals(adelanto.getId())) {
            limpiarCamposAdelanto();
        }
        filtrarAdelantos();
    }

    /** Lee el monto aceptando "10000", "10.000" o "$ 10.000". Devuelve null (y avisa) si no es válido. */
    private Double leerMontoAdelanto() {
        String texto = txtMontoAdelanto.getText() == null ? "" : txtMontoAdelanto.getText()
                .replace("$", "").replace(" ", "").replace(".", "").replace(",", ".").trim();
        if (texto.isEmpty()) {
            mostrarAdvertencia("Falta el monto", "Ingresá el monto del adelanto.");
            return null;
        }
        try {
            double monto = Double.parseDouble(texto);
            if (monto <= 0) {
                mostrarAdvertencia("Monto inválido", "El monto del adelanto tiene que ser mayor a cero.");
                return null;
            }
            return monto;
        } catch (NumberFormatException e) {
            mostrarError("Monto inválido", "El monto del adelanto debe ser un número válido.");
            return null;
        }
    }

    private Empleado buscarEmpleadoPorId(Long id) {
        if (id == null) return null;
        List<Empleado> empleados = empleadoDAO.buscarTodos();
        return empleados == null ? null
                : empleados.stream().filter(e -> id.equals(e.getId())).findFirst().orElse(null);
    }

    @FXML
    private void filtrarAdelantos() {
        LocalDate desde = dpDesdeAdelanto.getValue();
        LocalDate hasta = dpHastaAdelanto.getValue();

        if (desde == null || hasta == null) {
            mostrarAdvertencia("Faltan fechas", "Seleccioná las fechas 'Desde' y 'Hasta' para filtrar los adelantos.");
            return;
        }
        if (desde.isAfter(hasta)) {
            mostrarAdvertencia("Rango inválido", "La fecha 'Desde' no puede ser posterior a la fecha 'Hasta'.");
            return;
        }

        mostrarAdelantos(adelantoDAO.buscarPorRango(desde, hasta), empleadoDAO.obtenerTurnosPorRango(desde, hasta));
    }

    // Fila de la tabla "Saldo por empleado": lo trabajado contra lo adelantado en el período.
    // La fila con total=true es la de totales al pie.
    private record SaldoEmpleado(Long id, String empleado, double trabajado, double adelantado, boolean total) {
        double neto() { return trabajado - adelantado; }
    }

    /**
     * Calcula total trabajado, total adelantado y neto a pagar (general y por
     * empleado), refresca la tabla y mantiene seleccionado el mismo empleado.
     */
    private void mostrarAdelantos(List<Adelanto> adelantos, List<TurnoEmpleados> turnos) {
        adelantosPeriodo = adelantos != null ? new ArrayList<>(adelantos) : new ArrayList<>();

        Map<Long, String> nombres = new LinkedHashMap<>();
        Map<Long, Double> trabajadoPorEmpleado = new HashMap<>();
        Map<Long, Double> adelantadoPorEmpleado = new HashMap<>();

        // Todos los empleados aparecen, aunque no tengan movimientos en el período,
        // así se les puede registrar un adelanto.
        List<Empleado> empleados = empleadoDAO.buscarTodos();
        if (empleados != null) {
            for (Empleado e : empleados) {
                nombres.put(e.getId(), e.getNombre());
            }
        }
        if (turnos != null) {
            for (TurnoEmpleados t : turnos) {
                if (t.getEmpleado() == null) continue;
                Long id = t.getEmpleado().getId();
                nombres.putIfAbsent(id, t.getEmpleado().getNombre());
                trabajadoPorEmpleado.merge(id, t.getMontoHistorial() != null ? t.getMontoHistorial() : 0.0, Double::sum);
            }
        }
        for (Adelanto a : adelantosPeriodo) {
            if (a.getEmpleado() == null) continue;
            Long id = a.getEmpleado().getId();
            nombres.putIfAbsent(id, a.getEmpleado().getNombre());
            adelantadoPorEmpleado.merge(id, a.getMonto() != null ? a.getMonto() : 0.0, Double::sum);
        }

        List<SaldoEmpleado> saldos = new ArrayList<>();
        nombres.forEach((id, nombre) -> saldos.add(new SaldoEmpleado(id, nombre,
                trabajadoPorEmpleado.getOrDefault(id, 0.0),
                adelantadoPorEmpleado.getOrDefault(id, 0.0), false)));
        saldos.sort(Comparator.comparing(SaldoEmpleado::empleado, String.CASE_INSENSITIVE_ORDER));

        double totalTrabajado = saldos.stream().mapToDouble(SaldoEmpleado::trabajado).sum();
        double totalAdelantado = saldos.stream().mapToDouble(SaldoEmpleado::adelantado).sum();
        double neto = totalTrabajado - totalAdelantado;

        Long idSeleccionado = empleadoSeleccionadoAdelanto() != null ? empleadoSeleccionadoAdelanto().id() : null;

        List<SaldoEmpleado> filas = new ArrayList<>(saldos);
        if (!saldos.isEmpty()) {
            filas.add(new SaldoEmpleado(null, "Total", totalTrabajado, totalAdelantado, true));
        }
        tvSaldoEmpleados.setItems(FXCollections.observableArrayList(filas));

        lblTotalTrabajadoAdelanto.setText("$" + formatoMoneda.format(totalTrabajado));
        lblTotalAdelantadoAdelanto.setText("$" + formatoMoneda.format(totalAdelantado));
        lblNetoAdelanto.setText(neto < 0 ? "−$" + formatoMoneda.format(-neto) : "$" + formatoMoneda.format(neto));
        lblNetoAdelanto.getStyleClass().remove("ps-negativo");
        if (neto < 0) lblNetoAdelanto.getStyleClass().add("ps-negativo");

        // Volver a seleccionar el mismo empleado para que el panel no se cierre
        SaldoEmpleado aSeleccionar = idSeleccionado == null ? null
                : saldos.stream().filter(s -> idSeleccionado.equals(s.id())).findFirst().orElse(null);
        if (aSeleccionar != null) {
            tvSaldoEmpleados.getSelectionModel().select(aSeleccionar);
        } else {
            mostrarPanelEmpleadoAdelanto(null);
        }
    }

    /** Arma el panel derecho: nombre, cuánto le queda, adelantos del período y formulario. */
    private void mostrarPanelEmpleadoAdelanto(SaldoEmpleado saldo) {
        if (saldo == null || saldo.total()) {
            panelEmpleadoAdelanto.setVisible(false);
            lblSinEmpleadoAdelanto.setVisible(true);
            limpiarCamposAdelanto();
            return;
        }

        // Si se cambia de empleado mientras se editaba un adelanto de otro, se cancela la edición
        if (adelantoEnEdicion != null && adelantoEnEdicion.getEmpleado() != null
                && !adelantoEnEdicion.getEmpleado().getId().equals(saldo.id())) {
            limpiarCamposAdelanto();
        }

        lblEmpleadoAdelanto.setText(saldo.empleado());
        double neto = saldo.neto();
        lblLeQuedanAdelanto.setText(neto < 0
                ? "Debe $" + formatoMoneda.format(-neto)
                : "Le quedan $" + formatoMoneda.format(neto));
        lblLeQuedanAdelanto.getStyleClass().remove("ps-negativo");
        if (neto < 0) lblLeQuedanAdelanto.getStyleClass().add("ps-negativo");

        boxAdelantosEmpleado.getChildren().clear();
        List<Adelanto> delEmpleado = adelantosPeriodo.stream()
                .filter(a -> a.getEmpleado() != null && saldo.id().equals(a.getEmpleado().getId()))
                .sorted(Comparator.comparing(Adelanto::getFecha, Comparator.nullsLast(Comparator.reverseOrder())))
                .toList();

        if (delEmpleado.isEmpty()) {
            Label vacio = new Label("Sin adelantos en este período");
            vacio.getStyleClass().add("ps-item-sub");
            vacio.setPadding(new javafx.geometry.Insets(10, 0, 10, 0));
            boxAdelantosEmpleado.getChildren().add(vacio);
        } else {
            for (Adelanto a : delEmpleado) {
                boxAdelantosEmpleado.getChildren().add(crearFilaAdelanto(a));
            }
        }

        lblSinEmpleadoAdelanto.setVisible(false);
        panelEmpleadoAdelanto.setVisible(true);
    }

    private Node crearFilaAdelanto(Adelanto a) {
        String desc = a.getDescripcion() == null || a.getDescripcion().isBlank() ? "Adelanto" : a.getDescripcion();
        Label titulo = new Label(desc);
        titulo.getStyleClass().add("ps-item-name");

        String fecha = a.getFecha() != null ? a.getFecha().format(FORMATO_DIA_MES) : "";
        String medio = a.getMedioPago() != null ? a.getMedioPago() : "";
        Label sub = new Label(fecha + (medio.isEmpty() ? "" : " · " + medio));
        sub.getStyleClass().add("ps-item-sub");

        javafx.scene.layout.VBox textos = new javafx.scene.layout.VBox(2, titulo, sub);
        javafx.scene.layout.HBox.setHgrow(textos, javafx.scene.layout.Priority.ALWAYS);

        Label monto = new Label("$" + formatoMoneda.format(a.getMonto() != null ? a.getMonto() : 0.0));
        monto.getStyleClass().add("ps-item-total");
        monto.setMinWidth(javafx.scene.layout.Region.USE_PREF_SIZE);

        Button editar = botonIcono(
                "M3 17.25V21h3.75L17.81 9.94l-3.75-3.75L3 17.25zM20.71 7.04a1 1 0 0 0 0-1.41l-2.34-2.34a1 1 0 0 0-1.41 0l-1.83 1.83 3.75 3.75 1.83-1.83z",
                "ps-icon-btn", "Editar adelanto");
        editar.setOnAction(e -> empezarEdicionAdelanto(a));

        Button borrar = botonIcono(
                "M9 3v1H4v2h1v13a2 2 0 0 0 2 2h10a2 2 0 0 0 2-2V6h1V4h-5V3H9zM7 6h10v13H7V6zm2 2v9h2V8H9zm4 0v9h2V8h-2z",
                "ps-icon-btn-danger", "Eliminar adelanto");
        borrar.setOnAction(e -> borrarAdelanto(a));

        javafx.scene.layout.HBox fila = new javafx.scene.layout.HBox(10, textos, monto, editar, borrar);
        fila.setAlignment(javafx.geometry.Pos.CENTER_LEFT);
        fila.getStyleClass().add("ps-item");
        return fila;
    }

    private Button botonIcono(String svg, String estilo, String tooltip) {
        javafx.scene.shape.SVGPath icono = new javafx.scene.shape.SVGPath();
        icono.setContent(svg);
        icono.setScaleX(0.7);
        icono.setScaleY(0.7);
        icono.getStyleClass().add("ps-icon");
        Button b = new Button();
        b.setGraphic(icono);
        b.getStyleClass().add(estilo);
        b.setTooltip(new Tooltip(tooltip));
        return b;
    }

    /** Cambia el título y los botones del formulario según sea alta o edición. */
    private void actualizarModoFormularioAdelanto() {
        boolean editando = adelantoEnEdicion != null;
        lblNuevoAdelantoTitulo.setText(editando ? "Editar adelanto" : "Nuevo adelanto");
        btnGuardarAdelanto.setText(editando ? "Guardar cambios" : "Registrar adelanto");
        btnCancelarEdicionAdelanto.setVisible(editando);
        btnCancelarEdicionAdelanto.setManaged(editando);
    }

    /** Siempre queda un período elegido y el selector de rango se muestra solo con "Rango…". */
    private void configurarPeriodoAdelantos() {
        grupoPeriodoAdelantos.selectedToggleProperty().addListener((obs, anterior, nuevo) -> {
            if (nuevo == null && anterior != null && !mostrandoTodosLosAdelantos) {
                anterior.setSelected(true);
            }
            boolean esRango = grupoPeriodoAdelantos.getSelectedToggle() == tglAdelantosRango;
            boxRangoAdelantos.setVisible(esRango);
            boxRangoAdelantos.setManaged(esRango);
        });
    }

    @FXML
    private void mostrarSemanalAdelantos() {
        dpDesdeAdelanto.setValue(LocalDate.now().with(DayOfWeek.MONDAY));
        dpHastaAdelanto.setValue(LocalDate.now());
        filtrarAdelantos();
    }

    @FXML
    private void mostrarAdelantosSemanaPasada() {
        LocalDate lunesPasado = LocalDate.now().with(DayOfWeek.MONDAY).minusWeeks(1);
        dpDesdeAdelanto.setValue(lunesPasado);
        dpHastaAdelanto.setValue(lunesPasado.plusDays(6));
        filtrarAdelantos();
    }

    @FXML
    private void mostrarAdelantosDelMes() {
        dpDesdeAdelanto.setValue(LocalDate.now().withDayOfMonth(1));
        dpHastaAdelanto.setValue(LocalDate.now());
        filtrarAdelantos();
    }

    @FXML
    private void mostrarSelectorRangoAdelantos() {
        dpDesdeAdelanto.requestFocus();
    }

    private void limpiarCamposAdelanto() {
        adelantoEnEdicion = null;
        dpFechaAdelanto.setValue(LocalDate.now());
        cbMedioPagoAdelanto.getSelectionModel().selectFirst();
        txtDescripcionAdelanto.clear();
        txtMontoAdelanto.clear();
        actualizarModoFormularioAdelanto();
    }

    // --- Lógica de Resumen Semanal de Empleado (para imprimir y entregar) ---

    private static final String[] DIAS_CORTOS = {"Lun", "Mar", "Mié", "Jue", "Vie", "Sáb", "Dom"};
    private static final DateTimeFormatter FORMATO_FECHA_LARGA = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    /** Turnos y adelantos de un empleado en el período del resumen. */
    private record ResumenEmpleado(Empleado empleado, List<TurnoEmpleados> turnos, List<Adelanto> adelantos) {
        double trabajado() {
            return turnos.stream().mapToDouble(t -> t.getMontoHistorial() != null ? t.getMontoHistorial() : 0.0).sum();
        }
        double adelantado() {
            return adelantos.stream().mapToDouble(a -> a.getMonto() != null ? a.getMonto() : 0.0).sum();
        }
        double neto() { return trabajado() - adelantado(); }
        boolean tieneMovimientos() { return !turnos.isEmpty() || !adelantos.isEmpty(); }
    }

    private void configurarResumen() {
        grupoPeriodoResumen.selectedToggleProperty().addListener((obs, anterior, nuevo) -> {
            if (nuevo == null && anterior != null) {
                anterior.setSelected(true);
            }
            boolean esRango = grupoPeriodoResumen.getSelectedToggle() == tglResumenRango;
            boxRangoResumen.setVisible(esRango);
            boxRangoResumen.setManaged(esRango);
        });

        // Cada empleado: nombre, cantidad de turnos y neto a pagar
        lvEmpleadosResumen.setCellFactory(lv -> new ListCell<>() {
            private final Label nombre = new Label();
            private final Label turnos = new Label();
            private final Label neto = new Label();
            private final javafx.scene.layout.HBox contenido;
            {
                nombre.getStyleClass().add("ps-emp-name");
                turnos.getStyleClass().add("ps-emp-sub");
                neto.getStyleClass().add("ps-emp-neto");
                neto.setMinWidth(javafx.scene.layout.Region.USE_PREF_SIZE);
                javafx.scene.layout.VBox textos = new javafx.scene.layout.VBox(1, nombre, turnos);
                javafx.scene.layout.HBox.setHgrow(textos, javafx.scene.layout.Priority.ALWAYS);
                contenido = new javafx.scene.layout.HBox(10, textos, neto);
                contenido.setAlignment(javafx.geometry.Pos.CENTER_LEFT);
            }

            @Override
            protected void updateItem(ResumenEmpleado item, boolean empty) {
                super.updateItem(item, empty);
                setText(null);
                if (empty || item == null) {
                    setGraphic(null);
                    return;
                }
                nombre.setText(item.empleado().getNombre());
                int n = item.turnos().size();
                turnos.setText(n == 1 ? "1 turno" : n + " turnos");
                neto.setText(moneda(item.neto()));
                neto.getStyleClass().remove("ps-negativo");
                if (item.neto() < 0) neto.getStyleClass().add("ps-negativo");
                setGraphic(contenido);
            }
        });

        lvEmpleadosResumen.getSelectionModel().selectedItemProperty()
                .addListener((obs, anterior, seleccionado) -> mostrarComprobante(seleccionado));
    }

    @FXML
    private void mostrarSemanaActualResumen() {
        LocalDate lunes = LocalDate.now().with(DayOfWeek.MONDAY);
        dpDesdeResumen.setValue(lunes);
        dpHastaResumen.setValue(lunes.plusDays(6));
        generarResumenSemanal();
    }

    @FXML
    private void mostrarSemanaPasadaResumen() {
        LocalDate lunesPasado = LocalDate.now().with(DayOfWeek.MONDAY).minusWeeks(1);
        dpDesdeResumen.setValue(lunesPasado);
        dpHastaResumen.setValue(lunesPasado.plusDays(6));
        generarResumenSemanal();
    }

    @FXML
    private void mostrarSelectorRangoResumen() {
        dpDesdeResumen.requestFocus();
    }

    /** Carga turnos y adelantos del período y arma la lista de empleados. */
    @FXML
    private void generarResumenSemanal() {
        LocalDate desde = dpDesdeResumen.getValue();
        LocalDate hasta = dpHastaResumen.getValue();

        if (desde == null || hasta == null) {
            mostrarAdvertencia("Faltan fechas", "Seleccioná las fechas 'Desde' y 'Hasta'.");
            return;
        }
        if (desde.isAfter(hasta)) {
            mostrarAdvertencia("Rango inválido", "La fecha 'Desde' no puede ser posterior a la fecha 'Hasta'.");
            return;
        }

        List<TurnoEmpleados> turnos = empleadoDAO.obtenerTurnosPorRango(desde, hasta);
        List<Adelanto> adelantos = adelantoDAO.buscarPorRango(desde, hasta);
        List<Empleado> empleados = empleadoDAO.buscarTodos();

        Comparator<TurnoEmpleados> ordenTurnos = Comparator
                .comparing(TurnoEmpleados::getFecha, Comparator.nullsLast(Comparator.naturalOrder()))
                .thenComparing(t -> "Tarde".equalsIgnoreCase(t.getTurno()) ? 1 : 0);
        Comparator<Adelanto> ordenAdelantos = Comparator
                .comparing(Adelanto::getFecha, Comparator.nullsLast(Comparator.naturalOrder()));

        List<ResumenEmpleado> resumenes = new ArrayList<>();
        if (empleados != null) {
            for (Empleado e : empleados) {
                List<TurnoEmpleados> suyos = turnos == null ? List.of() : turnos.stream()
                        .filter(t -> t.getEmpleado() != null && e.getId().equals(t.getEmpleado().getId()))
                        .sorted(ordenTurnos).toList();
                List<Adelanto> susAdelantos = adelantos == null ? List.of() : adelantos.stream()
                        .filter(a -> a.getEmpleado() != null && e.getId().equals(a.getEmpleado().getId()))
                        .sorted(ordenAdelantos).toList();
                resumenes.add(new ResumenEmpleado(e, suyos, susAdelantos));
            }
        }
        // Primero los que trabajaron o pidieron adelantos; después por nombre
        resumenes.sort(Comparator.comparing((ResumenEmpleado r) -> !r.tieneMovimientos())
                .thenComparing(r -> r.empleado().getNombre(), String.CASE_INSENSITIVE_ORDER));

        Long idSeleccionado = lvEmpleadosResumen.getSelectionModel().getSelectedItem() != null
                ? lvEmpleadosResumen.getSelectionModel().getSelectedItem().empleado().getId() : null;

        lvEmpleadosResumen.setItems(FXCollections.observableArrayList(resumenes));

        ResumenEmpleado aSeleccionar = resumenes.stream()
                .filter(r -> idSeleccionado != null && idSeleccionado.equals(r.empleado().getId()))
                .findFirst()
                .orElse(resumenes.isEmpty() ? null : resumenes.get(0));
        if (aSeleccionar != null) {
            lvEmpleadosResumen.getSelectionModel().select(aSeleccionar);
        } else {
            mostrarComprobante(null);
        }
    }

    /** Arma la vista previa del comprobante del empleado elegido. */
    private void mostrarComprobante(ResumenEmpleado r) {
        if (r == null) {
            panelComprobante.setVisible(false);
            lblSinEmpleadoResumen.setVisible(true);
            btnImprimirResumen.setDisable(true);
            return;
        }

        LocalDate desde = dpDesdeResumen.getValue();
        LocalDate hasta = dpHastaResumen.getValue();
        boolean esSemana = desde != null && hasta != null
                && desde.getDayOfWeek() == DayOfWeek.MONDAY && hasta.equals(desde.plusDays(6));

        lblCompNombre.setText(r.empleado().getNombre());
        lblCompPeriodoTitulo.setText(esSemana ? "Semana del" : "Período del");
        lblCompPeriodo.setText(desde == null || hasta == null ? ""
                : desde.format(FORMATO_DIA_MES) + " al " + hasta.format(FORMATO_FECHA_LARGA));

        lblCompTrabajado.setText(moneda(r.trabajado()));
        lblCompAdelantos.setText(r.adelantado() == 0 ? "$0" : "−" + moneda(r.adelantado()));
        lblCompNeto.setText(moneda(r.neto()));
        lblCompNeto.getStyleClass().remove("ps-negativo");
        if (r.neto() < 0) lblCompNeto.getStyleClass().add("ps-negativo");

        lblCompCantTurnos.setText(String.valueOf(r.turnos().size()));
        boxCompTurnos.getChildren().clear();
        if (r.turnos().isEmpty()) {
            boxCompTurnos.getChildren().add(filaVaciaComprobante("Sin turnos en el período"));
        }
        for (TurnoEmpleados t : r.turnos()) {
            double monto = t.getMontoHistorial() != null ? t.getMontoHistorial() : 0.0;
            boxCompTurnos.getChildren().add(filaComprobante(diaCorto(t.getFecha()), t.getTurno(), null, moneda(monto)));
        }

        lblCompCantAdelantos.setText(String.valueOf(r.adelantos().size()));
        boxCompAdelantos.getChildren().clear();
        if (r.adelantos().isEmpty()) {
            boxCompAdelantos.getChildren().add(filaVaciaComprobante("Sin adelantos en el período"));
        }
        for (Adelanto a : r.adelantos()) {
            double monto = a.getMonto() != null ? a.getMonto() : 0.0;
            String desc = a.getDescripcion() != null && !a.getDescripcion().isBlank() ? a.getDescripcion() : "Adelanto";
            String fecha = a.getFecha() != null ? a.getFecha().format(FORMATO_DIA_MES) : "";
            boxCompAdelantos.getChildren().add(filaComprobante(fecha, desc, a.getMedioPago(), "−" + moneda(monto)));
        }

        lblSinEmpleadoResumen.setVisible(false);
        panelComprobante.setVisible(true);
        btnImprimirResumen.setDisable(false);
    }

    private Node filaComprobante(String fecha, String texto, String detalle, String monto) {
        Label lFecha = new Label(fecha);
        lFecha.getStyleClass().add("ps-comp-fecha");
        lFecha.setMinWidth(110);
        lFecha.setPrefWidth(110);

        Label lTexto = new Label(texto);
        lTexto.getStyleClass().add("ps-comp-texto");
        javafx.scene.layout.HBox centro = new javafx.scene.layout.HBox(lTexto);
        centro.setAlignment(javafx.geometry.Pos.CENTER_LEFT);
        if (detalle != null && !detalle.isBlank()) {
            Label lDetalle = new Label(" · " + detalle);
            lDetalle.getStyleClass().add("ps-comp-detalle");
            centro.getChildren().add(lDetalle);
        }
        javafx.scene.layout.HBox.setHgrow(centro, javafx.scene.layout.Priority.ALWAYS);

        Label lMonto = new Label(monto);
        lMonto.getStyleClass().add("ps-comp-texto");
        lMonto.setMinWidth(javafx.scene.layout.Region.USE_PREF_SIZE);

        javafx.scene.layout.HBox fila = new javafx.scene.layout.HBox(10, lFecha, centro, lMonto);
        fila.setAlignment(javafx.geometry.Pos.CENTER_LEFT);
        fila.getStyleClass().add("ps-comp-fila");
        return fila;
    }

    private Node filaVaciaComprobante(String texto) {
        Label l = new Label(texto);
        l.getStyleClass().add("ps-comp-detalle");
        javafx.scene.layout.HBox fila = new javafx.scene.layout.HBox(l);
        fila.getStyleClass().add("ps-comp-fila");
        return fila;
    }

    private String diaCorto(LocalDate fecha) {
        if (fecha == null) return "";
        return DIAS_CORTOS[fecha.getDayOfWeek().getValue() - 1] + " " + fecha.format(FORMATO_DIA_MES);
    }

    private String moneda(double valor) {
        return valor < 0 ? "−$" + formatoMoneda.format(-valor) : "$" + formatoMoneda.format(valor);
    }

    /** Imprime el comprobante del empleado seleccionado. */
    @FXML
    private void imprimirResumenSemanal(ActionEvent event) {
        ResumenEmpleado r = lvEmpleadosResumen.getSelectionModel().getSelectedItem();
        if (r == null) {
            mostrarAdvertencia("Elegí un empleado", "Seleccioná un empleado de la lista para imprimir su comprobante.");
            return;
        }
        try {
            imprimirEnSegundoPlano((Node) event.getSource(), List.of(construirTicketResumen(r)));
        } catch (Exception e) {
            e.printStackTrace();
            mostrarError("Error de impresión", "No se pudo imprimir el resumen semanal.");
        }
    }

    /** Imprime un comprobante por cada empleado que tuvo turnos o adelantos en el período. */
    @FXML
    private void imprimirTodosResumenSemanal(ActionEvent event) {
        List<String> tickets = lvEmpleadosResumen.getItems().stream()
                .filter(ResumenEmpleado::tieneMovimientos)
                .map(this::construirTicketResumen)
                .toList();
        if (tickets.isEmpty()) {
            mostrarAdvertencia("Sin datos", "Ningún empleado tiene turnos ni adelantos en este período.");
            return;
        }
        boolean confirmado = mostrarConfirmacion("Imprimir todos",
                "Se van a imprimir " + tickets.size() + (tickets.size() == 1 ? " comprobante." : " comprobantes, uno por empleado."));
        if (!confirmado) {
            return;
        }
        imprimirEnSegundoPlano((Node) event.getSource(), tickets);
    }

    private String construirTicketResumen(ResumenEmpleado r) {
        Empleado emp = r.empleado();
        LocalDate desde = dpDesdeResumen.getValue();
        LocalDate hasta = dpHastaResumen.getValue();

        StringBuilder ticket = new StringBuilder();

        ticket.append(centrar("CARNICERIA MONCHO")).append("\n");
        ticket.append(centrar("RESUMEN SEMANAL")).append("\n");
        ticket.append(centrar(emp.getNombre())).append("\n");
        ticket.append(lineaSeparadora()).append("\n");

        ticket.append("Medio de pago: ").append(emp.getMedioPago()).append("\n");
        if (desde.equals(hasta)) {
            ticket.append("Fecha: ").append(nombreDiaConFecha(desde)).append("\n");
        } else {
            ticket.append("Desde: ").append(nombreDiaConFecha(desde)).append("\n");
            ticket.append("Hasta: ").append(nombreDiaConFecha(hasta)).append("\n");
        }
        ticket.append("Impreso: ")
                .append(LocalDateTime.now().format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm")))
                .append("\n");
        ticket.append(lineaSeparadora()).append("\n");

        ticket.append("TURNOS TRABAJADOS\n");
        if (r.turnos().isEmpty()) {
            ticket.append("Sin turnos registrados en el período\n");
        } else {
            LocalDate diaActual = null;
            for (TurnoEmpleados t : r.turnos()) {
                LocalDate fecha = t.getFecha();
                if (!fecha.equals(diaActual)) {
                    if (diaActual != null) {
                        ticket.append(lineaSeparadora()).append("\n");
                    }
                    ticket.append(nombreDiaConFecha(fecha)).append(":\n");
                    diaActual = fecha;
                }
                double monto = t.getMontoHistorial() != null ? t.getMontoHistorial() : 0.0;
                ticket.append(fila("  " + t.getTurno(), "$" + formatoMoneda.format(monto))).append("\n");
            }
        }
        ticket.append(lineaSeparadora()).append("\n");
        ticket.append(fila("Total trabajado", moneda(r.trabajado()))).append("\n");
        ticket.append(lineaSeparadora()).append("\n");

        ticket.append("ADELANTOS\n");
        if (r.adelantos().isEmpty()) {
            ticket.append("Sin adelantos en el período\n");
        } else {
            LocalDate diaAdelanto = null;
            for (Adelanto a : r.adelantos()) {
                LocalDate fecha = a.getFecha();
                if (!fecha.equals(diaAdelanto)) {
                    if (diaAdelanto != null) {
                        ticket.append(lineaSeparadora()).append("\n");
                    }
                    ticket.append(nombreDiaConFecha(fecha)).append(":\n");
                    diaAdelanto = fecha;
                }
                String desc = a.getDescripcion() != null && !a.getDescripcion().isBlank()
                        ? a.getDescripcion() : a.getMedioPago();
                double monto = a.getMonto() != null ? a.getMonto() : 0.0;
                ticket.append(fila("  " + desc, "$" + formatoMoneda.format(monto))).append("\n");
            }
        }
        ticket.append(lineaSeparadora()).append("\n");
        ticket.append(fila("Total adelantado", moneda(r.adelantado()))).append("\n");
        ticket.append(lineaSeparadora()).append("\n");

        ticket.append(centrar("NETO A PAGAR")).append("\n");
        ticket.append(centrar(moneda(r.neto()))).append("\n");
        ticket.append("\n\n");
        ticket.append("________________________________\n");
        ticket.append(centrar("Firma del empleado")).append("\n");
        ticket.append("\n\n\n");

        return ticket.toString();
    }

    // --- Actualización de Tablas y Limpieza ---

    private void actualizarTablaTurnos() {
        mostrarTurnos(empleadoDAO.obtenerTodosLosTurnos());

        // "Ver todo" no corresponde a ningún período: se apagan las pills
        mostrandoTodosLosTurnos = true;
        grupoPeriodoTurnos.selectToggle(null);
        mostrandoTodosLosTurnos = false;
    }

    private void actualizarTablaAdelantos() {
        mostrarAdelantos(adelantoDAO.buscarTodos(), empleadoDAO.obtenerTodosLosTurnos());

        // "Ver todo" no corresponde a ningún período: se apagan las pills
        mostrandoTodosLosAdelantos = true;
        grupoPeriodoAdelantos.selectToggle(null);
        mostrandoTodosLosAdelantos = false;
    }

    private void actualizarTablaGestionEmpleados() {
        List<Empleado> lista = empleadoDAO.buscarTodos();
        if (lista != null) tvGestionEmpleados.setItems(FXCollections.observableArrayList(lista));
    }

    private void limpiarCamposTurno() {
        dpFechaTurno.setValue(LocalDate.now());
        cbEmpleado.setValue(null);
        cbTurno.setValue(null);
    }

    // --- Impresión de credenciales ---

    @FXML
    private void imprimirM() {
        imprimirTokenSeleccionado("Mañana");
    }

    @FXML
    private void imprimirT() {
        imprimirTokenSeleccionado("Tarde");
    }

    private void imprimirTokenSeleccionado(String turno) {
        Empleado emp = tvGestionEmpleados.getSelectionModel().getSelectedItem();
        if (emp == null) {
            mostrarAdvertencia("Sin selección", "Seleccione un empleado.");
            return;
        }

        String codigo = turno.equals("Mañana") ? emp.getTokenM() : emp.getTokenT();
        imprimirCredencial(emp.getNombre(), turno, codigo);
    }

    private void imprimirCredencial(String nombre, String turno, String codigo) {
        try {
            StringBuilder ticket = new StringBuilder();

            ticket.append(centrar("CARNICERIA MONCHO")).append("\n");
            ticket.append("\n");
            ticket.append("Persona:\n").append(nombre).append("\n\n");
            ticket.append("Turno:\n").append(turno).append("\n\n");
            ticket.append("Codigo:\n").append(codigo).append("\n\n");
            ticket.append(centrar("PRESENTAR AL INGRESAR")).append("\n");

            PrintTicket.print(ticket.toString());
            imprimirCodigoBarras(codigo);

        } catch (Exception e) {
            e.printStackTrace();
            mostrarError("Error de impresión", "No se pudo imprimir la credencial.");
        }
    }

    private void imprimirCodigoBarras(String codigo) {
        try {
            byte[] barcode = codigo.getBytes(StandardCharsets.US_ASCII);

            byte[] comando = new byte[]{
                0x1D, 0x68, 100,
                0x1D, 0x77, 3,
                0x1D, 0x48, 2,
                0x1D, 0x6B, 67, 13
            };

            java.io.ByteArrayOutputStream out = new java.io.ByteArrayOutputStream();
            out.write(comando);
            out.write(barcode);
            out.write('\n');
            out.write('\n');
            out.write('\n');

            PrintTicket.printBytes(out.toByteArray());

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * Replica el reemplazo de Ñ/ñ que hace PrintTicket antes de imprimir. Es
     * necesario aplicarlo acá también porque "ñ" -> "ni" agrega un carácter:
     * si no se descuenta antes de calcular el padding, renglones como
     * "Mañana" quedan un carácter más largos de lo esperado recién al
     * imprimir, y la impresora corta el último carácter (ej: un "0" del
     * monto) a un renglón aparte.
     */
    private String normalizarParaAncho(String texto) {
        return texto == null ? "" : texto.replace("Ñ", "NI").replace("ñ", "ni");
    }

    private String centrar(String texto) {
        String t = normalizarParaAncho(texto);
        int ancho = 32;
        int espacios = Math.max(0, (ancho - t.length()) / 2);
        return " ".repeat(espacios) + t;
    }

    /** Alinea etiqueta a la izquierda y valor a la derecha en el ancho de ticket (32 columnas). */
    private String fila(String etiqueta, String valor) {
        String et = normalizarParaAncho(etiqueta);
        String val = normalizarParaAncho(valor);
        int ancho = 32;
        int espacios = Math.max(1, ancho - et.length() - val.length());
        return et + " ".repeat(espacios) + val;
    }

    private String lineaSeparadora() {
        return "-".repeat(32);
    }

    private static final String[] NOMBRES_DIA = {
            "Lunes", "Martes", "Miércoles", "Jueves", "Viernes", "Sábado", "Domingo"
    };

    /** Ej: "Lunes 14/9" (la impresora quita tildes automáticamente). */
    private String nombreDiaConFecha(LocalDate fecha) {
        String dia = NOMBRES_DIA[fecha.getDayOfWeek().getValue() - 1];
        return dia + " " + fecha.getDayOfMonth() + "/" + fecha.getMonthValue();
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
    private void imprimirEnSegundoPlano(Node boton, List<String> contenidos) {
        if (boton != null) {
            boton.setDisable(true);
        }
        Thread hiloImpresion = new Thread(() -> {
            try {
                for (String contenido : contenidos) {
                    PrintTicket.print(contenido);
                }
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
