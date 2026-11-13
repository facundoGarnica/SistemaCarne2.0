package controller;

import Util.DialogoUtil;
import dao.TarjetaDAO;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.URL;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.ResourceBundle;
import java.util.Set;
import java.util.stream.Collectors;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Alert;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.control.TextInputDialog;
import javafx.stage.FileChooser;
import model.Tarjeta;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

public class TarjetaController implements Initializable {

    private static final int MAX_SELLOS = 6;
    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    // Cantidad de dígitos "base" del código EAN-13 (sin contar el dígito
    // verificador, que se calcula y se agrega aparte). EAN-13 completo
    // siempre tiene 13 dígitos: 12 base + 1 verificador.
    private static final int EAN13_DIGITOS_BASE = 12;

    @FXML
    private Button btnBuscar;
    @FXML
    private Button btnLimpiarFiltro;
    @FXML
    private Button btnActivar;
    @FXML
    private Button btnDesactivar;
    @FXML
    private Button btnGenerar;
    @FXML
    private Button btnGenerarLote;
    @FXML
    private Button btnExportarExcel;
    @FXML
    private Button btnSumarSello;
    @FXML
    private Button btnQuitarSello;
    @FXML
    private Button btnReiniciar;
    @FXML
    private Button btnEliminar;

    @FXML
    private TextField txtCodigo;

    @FXML
    private TextField txtDescuento;
    
    @FXML
    private TextField txtTope;

    @FXML
    private ComboBox<String> cmbFiltroEstado;

    @FXML
    private TableView<Tarjeta> tblTarjetas;

    @FXML
    private TableColumn<Tarjeta, String> colCodigo;
    @FXML
    private TableColumn<Tarjeta, String> colEstado;
    @FXML
    private TableColumn<Tarjeta, String> colSellos;
    @FXML
    private TableColumn<Tarjeta, String> colDescuento;
    @FXML
    private TableColumn<Tarjeta, String> colTope;
    @FXML
    private TableColumn<Tarjeta, String> colFecha;

    @FXML
    private Label lblCodigoSeleccionado;
    @FXML
    private Label lblEstado;
    @FXML
    private Label lblFechaActivacion;
    @FXML
    private Label lblSellos;

    private final TarjetaDAO tarjetaDAO = new TarjetaDAO();

    private List<Tarjeta> todasLasTarjetas;

    @Override
    public void initialize(URL url, ResourceBundle rb) {

        configurarTabla();
        configurarFiltroEstado();
        cargarTabla();

        tblTarjetas.getSelectionModel().selectedItemProperty().addListener((observable, oldValue, newValue) -> {
            if (newValue != null) {
                mostrarTarjeta(newValue);
            } else {
                limpiarPanelSeleccion();
            }
        });
    }

    private void configurarTabla() {

        colCodigo.setCellValueFactory(cellData ->
                new SimpleStringProperty(cellData.getValue().getCodigoBarra()));

        colEstado.setCellValueFactory(cellData ->
                new SimpleStringProperty(cellData.getValue().getEstado() ? "Activa" : "Inactiva"));

        colSellos.setCellValueFactory(cellData ->
                new SimpleStringProperty(cellData.getValue().getSellosActuales() + " / " + MAX_SELLOS));
        colDescuento.setCellValueFactory(cellData ->
                new SimpleStringProperty(cellData.getValue().getDescuentoString()));
        colTope.setCellValueFactory(cellData ->
                 new SimpleStringProperty(cellData.getValue().getTopeString()));

        colFecha.setCellValueFactory(cellData -> {

            if (cellData.getValue().getFechaActivacion() == null) {
                return new SimpleStringProperty("-");
            }

            return new SimpleStringProperty(
                    cellData.getValue().getFechaActivacion().format(FORMATTER));
        });
    }

    private void configurarFiltroEstado() {
        cmbFiltroEstado.setItems(FXCollections.observableArrayList("Todos", "Activa", "Inactiva"));
        cmbFiltroEstado.getSelectionModel().selectFirst();
    }

    private void cargarTabla() {
        todasLasTarjetas = tarjetaDAO.buscarTodos();
        aplicarFiltroActual();
    }

    private void mostrarTarjeta(Tarjeta tarjeta) {

        lblCodigoSeleccionado.setText(tarjeta.getCodigoBarra());
        lblEstado.setText("Estado: " + (tarjeta.getEstado() ? "ACTIVA" : "INACTIVA"));

        lblFechaActivacion.setText("Activación: " +
                (tarjeta.getFechaActivacion() == null ? "-" : tarjeta.getFechaActivacion().format(FORMATTER)));

        lblSellos.setText(tarjeta.getSellosActuales() + " / " + MAX_SELLOS);
    }

    private void limpiarPanelSeleccion() {
        lblCodigoSeleccionado.setText("---");
        lblEstado.setText("Estado: ---");
        lblFechaActivacion.setText("Activación: ---");
        lblSellos.setText("0 / " + MAX_SELLOS);
    }

    private void refrescarSeleccionActual() {

        Tarjeta seleccionada = tblTarjetas.getSelectionModel().getSelectedItem();
        cargarTabla();

        if (seleccionada == null) {
            return;
        }

        todasLasTarjetas.stream()
                .filter(t -> t.getId() == seleccionada.getId())
                .findFirst()
                .ifPresent(t -> {
                    tblTarjetas.getSelectionModel().select(t);
                    mostrarTarjeta(t);
                });
    }

    private void mostrarAlerta(AlertType tipo, String titulo, String mensaje) {
        Alert alert = new Alert(tipo);
        DialogoUtil.aplicarEstilo(alert);
        alert.setTitle(titulo);
        alert.setHeaderText(null);
        alert.setContentText(mensaje);
        alert.showAndWait();
    }

    @FXML
    private void buscar() {
        aplicarFiltroActual();

        ObservableList<Tarjeta> resultado = tblTarjetas.getItems();

        if (resultado.isEmpty()) {
            limpiarPanelSeleccion();
            mostrarAlerta(AlertType.INFORMATION, "Sin resultados",
                    "No se encontraron tarjetas con ese criterio.");
        } else if (resultado.size() == 1) {
            tblTarjetas.getSelectionModel().select(0);
            tblTarjetas.scrollTo(0);
        }
    }

    @FXML
    private void limpiarFiltro() {
        txtCodigo.clear();
        cmbFiltroEstado.getSelectionModel().selectFirst();
        cargarTabla();
        limpiarPanelSeleccion();
    }

   @FXML
    private void generarTarjetas() {

        String codigo = generarCodigoUnico();
        String descuentotxt = txtDescuento.getText().trim();
        String topeTxt = txtTope.getText().trim();
        if (descuentotxt.isEmpty()) {
            mostrarAlerta(AlertType.ERROR,
                    "Error",
                    "Debe ingresar un descuento.");
            return;
        }
        if (topeTxt.isEmpty()) {
            mostrarAlerta(AlertType.ERROR,
                    "Error",
                    "Debe ingresar un tope.");
            return;
        }

        Double descuentoNumber;
        Double topeNumber;
        try {
            descuentoNumber = Double.valueOf(descuentotxt);
            topeNumber = Double.valueOf(topeTxt);
        } catch (NumberFormatException e) {
            mostrarAlerta(AlertType.ERROR,
                    "Error",
                    "Debe ingresar un número entero.");
            return;
        }

        Tarjeta nueva = new Tarjeta();
        nueva.setCodigoBarra(codigo);
        nueva.setEstado(false);
        nueva.setSellosActuales(0);
        nueva.setFechaActivacion(null);
        nueva.setDescuento(descuentoNumber);
        nueva.setTope(topeNumber);

        tarjetaDAO.guardar(nueva);

        cargarTabla();
    }

    /**
     * Pide al usuario la cantidad de tarjetas a crear (por defecto 100) y
     * si quiere que se creen ya activas, y dispara la generación en lote
     * usando el descuento y tope ya cargados en los campos de texto para
     * todas las tarjetas del lote.
     */
    @FXML
    private void generarTarjetasMasivo() {

        TextInputDialog dialogCantidad = new TextInputDialog("100");
        DialogoUtil.aplicarEstilo(dialogCantidad);
        dialogCantidad.setTitle("Generar tarjetas en lote");
        dialogCantidad.setHeaderText(null);
        dialogCantidad.setContentText("¿Cuántas tarjetas querés generar?");

        dialogCantidad.showAndWait().ifPresent(input -> {

            int cantidad;
            try {
                cantidad = Integer.parseInt(input.trim());
            } catch (NumberFormatException e) {
                mostrarAlerta(AlertType.ERROR, "Error", "Ingresá un número entero válido.");
                return;
            }

            if (cantidad <= 0) {
                mostrarAlerta(AlertType.ERROR, "Error", "La cantidad debe ser mayor a 0.");
                return;
            }

            ButtonType btnSi = new ButtonType("Sí");
            ButtonType btnNo = new ButtonType("No");

            Alert dialogActivar = new Alert(AlertType.CONFIRMATION);
            DialogoUtil.aplicarEstilo(dialogActivar);
            dialogActivar.setTitle("Activar tarjetas");
            dialogActivar.setHeaderText(null);
            dialogActivar.setContentText("¿Querés que las " + cantidad + " tarjetas se creen ya activas?");
            dialogActivar.getButtonTypes().setAll(btnSi, btnNo);

            boolean activarDeEntrada = dialogActivar.showAndWait()
                    .filter(respuesta -> respuesta == btnSi)
                    .isPresent();

            generarTarjetasEnLote(cantidad, activarDeEntrada);
        });
    }

    /**
     * Crea y guarda `cantidad` tarjetas nuevas, sin sellos, con el
     * descuento y tope de los campos de texto actuales, y un código
     * EAN-13 único por tarjeta (único contra la base y también dentro del
     * propio lote, ya que la base recién se refresca al final). Si
     * `activarDeEntrada` es true, las tarjetas quedan activas con la
     * fecha de activación en el momento de la creación; si es false,
     * quedan inactivas como antes.
     */
    private void generarTarjetasEnLote(int cantidad, boolean activarDeEntrada) {

        String descuentoTxt = txtDescuento.getText().trim();
        String topeTxt = txtTope.getText().trim();

        if (descuentoTxt.isEmpty()) {
            mostrarAlerta(AlertType.ERROR, "Error", "Debe ingresar un descuento.");
            return;
        }
        if (topeTxt.isEmpty()) {
            mostrarAlerta(AlertType.ERROR, "Error", "Debe ingresar un tope.");
            return;
        }

        Double descuentoNumber;
        Double topeNumber;
        try {
            descuentoNumber = Double.valueOf(descuentoTxt);
            topeNumber = Double.valueOf(topeTxt);
        } catch (NumberFormatException e) {
            mostrarAlerta(AlertType.ERROR, "Error", "Debe ingresar un número válido para descuento y tope.");
            return;
        }

        Set<String> codigosDelLote = new HashSet<>();
        LocalDateTime fechaActivacion = activarDeEntrada ? LocalDateTime.now() : null;

        for (int i = 0; i < cantidad; i++) {
            String codigo = generarCodigoUnico(codigosDelLote);
            codigosDelLote.add(codigo);

            Tarjeta nueva = new Tarjeta();
            nueva.setCodigoBarra(codigo);
            nueva.setEstado(activarDeEntrada);
            nueva.setSellosActuales(0);
            nueva.setFechaActivacion(fechaActivacion);
            nueva.setDescuento(descuentoNumber);
            nueva.setTope(topeNumber);

            tarjetaDAO.guardar(nueva);
        }

        cargarTabla();

        String estadoTexto = activarDeEntrada ? "activas" : "inactivas";
        mostrarAlerta(AlertType.INFORMATION, "Listo",
                cantidad + " tarjetas generadas correctamente (" + estadoTexto + ").");
    }

    /**
     * Le pide al usuario dónde guardar el archivo y exporta TODAS las
     * tarjetas actualmente cargadas (respeta el filtro/búsqueda vigente en
     * la tabla) a un .xlsx con columnas ID | Cliente | Codigo.
     */
    @FXML
    private void exportarExcel() {

        List<Tarjeta> tarjetasAExportar = tblTarjetas.getItems();

        if (tarjetasAExportar == null || tarjetasAExportar.isEmpty()) {
            mostrarAlerta(AlertType.WARNING, "Sin tarjetas", "No hay tarjetas para exportar.");
            return;
        }

        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Guardar listado de tarjetas");
        fileChooser.setInitialFileName("tarjetas.xlsx");
        fileChooser.getExtensionFilters().add(
                new FileChooser.ExtensionFilter("Archivos Excel (*.xlsx)", "*.xlsx"));

        File archivo = fileChooser.showSaveDialog(tblTarjetas.getScene().getWindow());
        if (archivo == null) {
            return; // el usuario canceló el diálogo
        }

        try {
            generarExcelTarjetas(archivo, tarjetasAExportar);
            mostrarAlerta(AlertType.INFORMATION, "Listo",
                    "Excel generado correctamente en:\n" + archivo.getAbsolutePath());
        } catch (IOException e) {
            mostrarAlerta(AlertType.ERROR, "Error", "No se pudo generar el Excel: " + e.getMessage());
        }
    }

    /**
     * Crea el archivo .xlsx con las columnas ID | Cliente | Codigo. En
     * "Cliente" va el ID de la tarjeta escrito en texto (uno, dos, tres...)
     * y en "Codigo" el código de barra EAN-13 tal cual está guardado.
     */
    private void generarExcelTarjetas(File archivo, List<Tarjeta> tarjetas) throws IOException {

        try (Workbook workbook = new XSSFWorkbook()) {
            Sheet sheet = workbook.createSheet("Tarjetas");

            Row header = sheet.createRow(0);
            header.createCell(0).setCellValue("ID");
            header.createCell(1).setCellValue("Cliente");
            header.createCell(2).setCellValue("Codigo");

            int filaActual = 1;
            for (Tarjeta tarjeta : tarjetas) {
                Row row = sheet.createRow(filaActual++);
                long id = tarjeta.getId();
                row.createCell(0).setCellValue(id);
                row.createCell(1).setCellValue(numeroATexto(id));
                row.createCell(2).setCellValue(tarjeta.getCodigoBarra());
            }

            for (int col = 0; col < 3; col++) {
                sheet.autoSizeColumn(col);
            }

            try (FileOutputStream fos = new FileOutputStream(archivo)) {
                workbook.write(fos);
            }
        }
    }

    // ------------------------------------------------------------------
    // Conversión de números a texto en español (para la columna "Cliente")
    // ------------------------------------------------------------------

    private static final String[] UNIDADES = {
        "", "uno", "dos", "tres", "cuatro", "cinco", "seis", "siete", "ocho", "nueve"
    };

    // Índice 0 -> 10, índice 1 -> 11, ..., índice 19 -> 29
    private static final String[] DIEZ_A_VEINTINUEVE = {
        "diez", "once", "doce", "trece", "catorce", "quince",
        "dieciséis", "diecisiete", "dieciocho", "diecinueve", "veinte",
        "veintiuno", "veintidós", "veintitrés", "veinticuatro", "veinticinco",
        "veintiséis", "veintisiete", "veintiocho", "veintinueve"
    };

    // Indexado por el dígito de las decenas (3 -> treinta, ..., 9 -> noventa)
    private static final String[] DECENAS = {
        "", "", "", "treinta", "cuarenta", "cincuenta", "sesenta", "setenta", "ochenta", "noventa"
    };

    // Indexado por el dígito de las centenas (1 -> ciento, ..., 9 -> novecientos)
    private static final String[] CENTENAS = {
        "", "ciento", "doscientos", "trescientos", "cuatrocientos", "quinientos",
        "seiscientos", "setecientos", "ochocientos", "novecientos"
    };

    /**
     * Convierte un número entero (hasta cientos de miles) a su escritura
     * en español, en minúscula y sin acentos de mayúscula (ej: 57 ->
     * "cincuenta y siete", 100 -> "cien", 101 -> "ciento uno").
     */
    private String numeroATexto(long n) {

        if (n == 0) {
            return "cero";
        }
        if (n < 0) {
            return "menos " + numeroATexto(-n);
        }

        long millones = n / 1_000_000;
        long resto1 = n % 1_000_000;
        long miles = resto1 / 1000;
        long resto = resto1 % 1000;

        StringBuilder sb = new StringBuilder();

        if (millones > 0) {
            sb.append(millones == 1 ? "un millón" : numeroMenorMil(millones) + " millones");
            sb.append(" ");
        }

        if (miles > 0) {
            sb.append(miles == 1 ? "mil" : numeroMenorMil(miles) + " mil");
            sb.append(" ");
        }

        if (resto > 0) {
            sb.append(numeroMenorMil(resto));
        }

        return sb.toString().trim();
    }

    /** Convierte un número entre 1 y 999 a texto en español. */
    private String numeroMenorMil(long n) {

        if (n == 100) {
            return "cien";
        }

        long centena = n / 100;
        long resto = n % 100;

        StringBuilder sb = new StringBuilder();

        if (centena > 0) {
            sb.append(CENTENAS[(int) centena]);
            if (resto > 0) {
                sb.append(" ");
            }
        }

        if (resto > 0) {
            sb.append(numeroMenorCien(resto));
        }

        return sb.toString();
    }

    /** Convierte un número entre 1 y 99 a texto en español. */
    private String numeroMenorCien(long n) {

        if (n < 10) {
            return UNIDADES[(int) n];
        }
        if (n < 30) {
            return DIEZ_A_VEINTINUEVE[(int) n - 10];
        }

        long decena = n / 10;
        long unidad = n % 10;

        if (unidad == 0) {
            return DECENAS[(int) decena];
        }
        return DECENAS[(int) decena] + " y " + UNIDADES[(int) unidad];
    }

    /**
     * Genera un código de barra EAN-13 válido (13 dígitos: 12 aleatorios +
     * 1 dígito verificador calculado) que no exista ya entre las tarjetas
     * cargadas. Es el mismo formato (EAN-13) que usa el generador de
     * tarjetas web para imprimir los códigos de barra.
     */
    private String generarCodigoUnico() {
        return generarCodigoUnico(Collections.emptySet());
    }

    /**
     * Igual que {@link #generarCodigoUnico()}, pero además evita los
     * códigos incluidos en `codigosReservados` (usado durante la
     * generación en lote, donde todavía no se refrescó `todasLasTarjetas`
     * con las tarjetas recién guardadas).
     */
    private String generarCodigoUnico(Set<String> codigosReservados) {

        String codigo;

        do {
            codigo = generarCodigoEAN13();
        } while (existeCodigo(codigo) || codigosReservados.contains(codigo));

        return codigo;
    }

    /**
     * Genera un código EAN-13 completo: 12 dígitos aleatorios (con cero a
     * la izquierda si corresponde) más el dígito verificador calculado
     * según el algoritmo estándar de EAN-13/UPC.
     */
    private String generarCodigoEAN13() {
        StringBuilder base = new StringBuilder(EAN13_DIGITOS_BASE);
        for (int i = 0; i < EAN13_DIGITOS_BASE; i++) {
            base.append((int) (Math.random() * 10));
        }
        String base12 = base.toString();
        int digitoVerificador = calcularDigitoVerificadorEAN13(base12);
        return base12 + digitoVerificador;
    }

    private void aplicarFiltroActual() {
        String codigo = txtCodigo.getText().trim();
        String filtroEstado = cmbFiltroEstado.getSelectionModel().getSelectedItem();

        List<Tarjeta> filtradas = todasLasTarjetas.stream()
                .filter(t -> codigo.isEmpty() || t.getCodigoBarra().contains(codigo))
                .filter(t -> {
                    if (filtroEstado == null || filtroEstado.equals("Todos")) {
                        return true;
                    }
                    boolean activa = filtroEstado.equals("Activa");
                    return t.getEstado() == activa;
                })
                .collect(Collectors.toList());

        tblTarjetas.setItems(FXCollections.observableArrayList(filtradas));
    }

    /**
     * Calcula el dígito verificador de un código EAN-13 a partir de sus
     * primeros 12 dígitos, siguiendo el algoritmo estándar: los dígitos en
     * posición par (índice 0-based par) pesan 1 y los de posición impar
     * pesan 3; la suma se redondea al múltiplo de 10 superior más cercano
     * y la diferencia es el dígito verificador.
     */
    private int calcularDigitoVerificadorEAN13(String base12) {
        int suma = 0;
        for (int i = 0; i < base12.length(); i++) {
            int digito = Character.getNumericValue(base12.charAt(i));
            suma += (i % 2 == 0) ? digito : digito * 3;
        }
        int resto = suma % 10;
        return (resto == 0) ? 0 : 10 - resto;
    }

    private boolean existeCodigo(String codigo) {
        return todasLasTarjetas != null && todasLasTarjetas.stream()
                .anyMatch(t -> t.getCodigoBarra().equals(codigo));
    }

    @FXML
    private void activarTarjeta() {

        Tarjeta tarjeta = tblTarjetas.getSelectionModel().getSelectedItem();

        if (tarjeta == null) {
            mostrarAlerta(AlertType.WARNING, "Sin selección", "Seleccioná una tarjeta primero.");
            return;
        }

        if (tarjeta.getEstado()) {
            mostrarAlerta(AlertType.INFORMATION, "Tarjeta activa", "La tarjeta ya se encuentra activa.");
            return;
        }

        tarjeta.setEstado(true);
        tarjeta.setFechaActivacion(LocalDateTime.now());

        tarjetaDAO.actualizar(tarjeta);
        refrescarSeleccionActual();
    }

    @FXML
    private void desactivarTarjeta() {
 
        Tarjeta tarjeta = tblTarjetas.getSelectionModel().getSelectedItem();
 
        if (tarjeta == null) {
            mostrarAlerta(AlertType.WARNING, "Sin selección", "Seleccioná una tarjeta primero.");
            return;
        }
 
        if (!tarjeta.getEstado()) {
            mostrarAlerta(AlertType.INFORMATION, "Tarjeta inactiva", "La tarjeta ya se encuentra inactiva.");
            return;
        }
 
        tarjeta.setEstado(false);
 
        tarjetaDAO.actualizar(tarjeta);
        refrescarSeleccionActual();
    }


    @FXML
    private void sumarSello() {

        Tarjeta tarjeta = tblTarjetas.getSelectionModel().getSelectedItem();

        if (tarjeta == null) {
            mostrarAlerta(AlertType.WARNING, "Sin selección", "Seleccioná una tarjeta primero.");
            return;
        }

        if (!tarjeta.getEstado()) {
            mostrarAlerta(AlertType.WARNING, "Tarjeta inactiva", "Activá la tarjeta antes de sumar sellos.");
            return;
        }

        if (tarjeta.getSellosActuales() >= MAX_SELLOS) {
            mostrarAlerta(AlertType.INFORMATION, "Máximo alcanzado",
                    "La tarjeta ya tiene el máximo de " + MAX_SELLOS + " sellos.");
            return;
        }

        tarjeta.setSellosActuales(tarjeta.getSellosActuales() + 1);

        tarjetaDAO.actualizar(tarjeta);
        refrescarSeleccionActual();
    }

    @FXML
    private void quitarSello() {

        Tarjeta tarjeta = tblTarjetas.getSelectionModel().getSelectedItem();

        if (tarjeta == null) {
            mostrarAlerta(AlertType.WARNING, "Sin selección", "Seleccioná una tarjeta primero.");
            return;
        }

        if (tarjeta.getSellosActuales() <= 0) {
            mostrarAlerta(AlertType.INFORMATION, "Sin sellos", "La tarjeta no tiene sellos para quitar.");
            return;
        }

        tarjeta.setSellosActuales(tarjeta.getSellosActuales() - 1);

        tarjetaDAO.actualizar(tarjeta);
        refrescarSeleccionActual();
    }

    @FXML
    private void reiniciarSellos() {

        Tarjeta tarjeta = tblTarjetas.getSelectionModel().getSelectedItem();

        if (tarjeta == null) {
            mostrarAlerta(AlertType.WARNING, "Sin selección", "Seleccioná una tarjeta primero.");
            return;
        }

        Alert confirmacion = new Alert(AlertType.CONFIRMATION,
                "¿Reiniciar los sellos de la tarjeta " + tarjeta.getCodigoBarra() + " a 0?");
        DialogoUtil.aplicarEstilo(confirmacion);
        confirmacion.setHeaderText(null);

        confirmacion.showAndWait().ifPresent(respuesta -> {
            if (respuesta.getButtonData().isDefaultButton()) {
                tarjeta.setSellosActuales(0);
                tarjetaDAO.actualizar(tarjeta);
                refrescarSeleccionActual();
            }
        });
    }

    @FXML
    private void eliminarTarjeta() {

        Tarjeta tarjeta = tblTarjetas.getSelectionModel().getSelectedItem();

        if (tarjeta == null) {
            mostrarAlerta(AlertType.WARNING, "Sin selección", "Seleccioná una tarjeta primero.");
            return;
        }

        Alert confirmacion = new Alert(AlertType.CONFIRMATION,
                "¿Eliminar definitivamente la tarjeta " + tarjeta.getCodigoBarra() + "?");
        DialogoUtil.aplicarEstilo(confirmacion);
        confirmacion.setHeaderText(null);

        confirmacion.showAndWait().ifPresent(respuesta -> {
            if (respuesta.getButtonData().isDefaultButton()) {
                tarjetaDAO.eliminar(tarjeta.getId());
                cargarTabla();
                limpiarPanelSeleccion();
            }
        });
    }


}