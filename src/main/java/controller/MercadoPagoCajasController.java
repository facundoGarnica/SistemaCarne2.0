package controller;

import Dto.CajaMercadoPagoDTO;
import Util.ClaveAdminUtil;
import Util.DialogoUtil;
import Util.MercadoPagoPosService;
import dao.MercadoPagoTokenDAO;
import java.awt.Desktop;
import java.net.URI;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import javafx.application.Platform;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.VBox;
import model.MercadoPagoToken;

/**
 * Sección "QR de tu cuenta" de la pantalla de Mercado Pago: trae las cajas
 * (POS) de la cuenta, muestra sus datos y su QR, permite cambiar el
 * External POS ID y asignar cada QR a una caja del sistema (1, 2 o 3).
 * Toda modificación pide la contraseña de administrador.
 */
public class MercadoPagoCajasController {

    @FXML
    private Label lblResumen;
    @FXML
    private Button btnTraer;

    @FXML
    private TableView<CajaMercadoPagoDTO> tblCajas;
    @FXML
    private TableColumn<CajaMercadoPagoDTO, String> colNombre;
    @FXML
    private TableColumn<CajaMercadoPagoDTO, String> colExternalId;
    @FXML
    private TableColumn<CajaMercadoPagoDTO, String> colSucursal;
    @FXML
    private TableColumn<CajaMercadoPagoDTO, String> colCajaSistema;
    @FXML
    private TableColumn<CajaMercadoPagoDTO, String> colEstado;

    @FXML
    private Label lblSinSeleccion;
    @FXML
    private VBox panelDetalle;
    @FXML
    private Label lblDetalleNombre;
    @FXML
    private ImageView imgQr;
    @FXML
    private Label lblQrEstado;
    @FXML
    private Button btnPdf;
    @FXML
    private GridPane gridDatos;
    @FXML
    private TextField txtNuevoExternalId;
    @FXML
    private Button btnGuardarExternalId;

    private final ObservableList<CajaMercadoPagoDTO> cajas = FXCollections.observableArrayList();
    private final MercadoPagoPosService posService = new MercadoPagoPosService();
    private final MercadoPagoTokenDAO tokenDAO = new MercadoPagoTokenDAO();

    // Avisa a la pestaña Configuración cuando cambian los QR asignados a las cajas
    private Runnable onConfiguracionCambiada;

    @FXML
    public void initialize() {

        colNombre.setCellValueFactory(new PropertyValueFactory<>("nombre"));
        colExternalId.setCellValueFactory(new PropertyValueFactory<>("externalId"));
        // La API v2 no siempre trae external_store_id: se muestra el store_id en su lugar
        colSucursal.setCellValueFactory(c -> new SimpleStringProperty(
                c.getValue().getExternalStoreId() != null ? c.getValue().getExternalStoreId() : c.getValue().getStoreId()
        ));
        colCajaSistema.setCellValueFactory(new PropertyValueFactory<>("cajaAsignada"));
        colEstado.setCellValueFactory(new PropertyValueFactory<>("estado"));

        // Resaltar en verde las cajas que ya usa el sistema
        colCajaSistema.setCellFactory(columna -> new TableCell<>() {
            @Override
            protected void updateItem(String valor, boolean vacio) {
                super.updateItem(valor, vacio);
                getStyleClass().remove("mp-celda-asignada");
                setText(vacio ? null : valor);
                if (!vacio && valor != null && !"—".equals(valor)) {
                    getStyleClass().add("mp-celda-asignada");
                }
            }
        });

        Label vacia = new Label("Tocá \"Traer de Mercado Pago\" para ver los QR de tu cuenta.");
        vacia.getStyleClass().add("ps-placeholder");
        tblCajas.setPlaceholder(vacia);
        tblCajas.setItems(cajas);

        tblCajas.getSelectionModel().selectedItemProperty().addListener(
                (obs, anterior, seleccionada) -> mostrarDetalle(seleccionada)
        );
    }

    public void setOnConfiguracionCambiada(Runnable onConfiguracionCambiada) {
        this.onConfiguracionCambiada = onConfiguracionCambiada;
    }

    /**
     * Nombre de la caja de Mercado Pago con ese External POS ID, o null si
     * todavía no se trajeron las cajas (o no existe en la cuenta).
     */
    public String nombreDeCaja(String externalId) {
        if (externalId == null || externalId.isBlank()) {
            return null;
        }
        return cajas.stream()
                .filter(c -> externalId.equals(c.getExternalId()))
                .map(CajaMercadoPagoDTO::getNombre)
                .filter(n -> n != null && !n.isBlank())
                .findFirst()
                .orElse(null);
    }

    /*
     =========================================
     TRAER CAJAS DESDE MERCADO PAGO
     =========================================
     */
    @FXML
    private void traerCajas() {
        cargarCajas(null);
    }

    /** Consulta las cajas en segundo plano y, si se indica, vuelve a seleccionar una por id. */
    private void cargarCajas(String idSeleccionar) {

        btnTraer.setDisable(true);
        btnTraer.setText("Consultando...");

        new Thread(() -> {
            try {
                List<CajaMercadoPagoDTO> resultado = posService.listarCajas();

                Platform.runLater(() -> {
                    cajas.setAll(resultado);
                    refrescarAsignaciones();
                    // Las tarjetas "Cajas del sistema" ahora pueden mostrar el nombre de cada QR
                    notificarCambioConfiguracion();
                    lblResumen.setText(resultado.size() + (resultado.size() == 1 ? " caja encontrada" : " cajas encontradas")
                            + " · actualizado a las " + LocalTime.now().format(DateTimeFormatter.ofPattern("HH:mm")));
                    restaurarBoton();

                    if (idSeleccionar != null) {
                        cajas.stream()
                                .filter(c -> idSeleccionar.equals(c.getId()))
                                .findFirst()
                                .ifPresent(c -> tblCajas.getSelectionModel().select(c));
                    }
                });

            } catch (Exception e) {
                e.printStackTrace();
                Platform.runLater(() -> {
                    restaurarBoton();
                    mostrarError("No se pudieron traer las cajas", e.getMessage());
                });
            }
        }).start();
    }

    private void restaurarBoton() {
        btnTraer.setDisable(false);
        btnTraer.setText("Traer de Mercado Pago");
    }

    /**
     * Recalcula qué caja del sistema usa cada QR, según la configuración
     * guardada. Se llama también desde la pestaña Configuración al guardar.
     */
    public void refrescarAsignaciones() {

        MercadoPagoToken config = tokenDAO.obtenerToken();

        for (CajaMercadoPagoDTO caja : cajas) {
            caja.setCajaAsignada(cajaDelSistema(config, caja.getExternalId()));
        }

        tblCajas.refresh();
        mostrarDetalle(tblCajas.getSelectionModel().getSelectedItem());
    }

    private String cajaDelSistema(MercadoPagoToken config, String externalId) {

        if (config == null || externalId == null || externalId.isBlank()) {
            return "—";
        }

        StringBuilder asignadas = new StringBuilder();
        for (int numero = 1; numero <= 3; numero++) {
            if (externalId.equals(config.getExternalPosIdCaja(numero))) {
                asignadas.append(asignadas.length() > 0 ? ", " : "").append("Caja ").append(numero);
            }
        }
        return asignadas.length() > 0 ? asignadas.toString() : "—";
    }

    /*
     =========================================
     DETALLE DE LA CAJA SELECCIONADA
     =========================================
     */
    private void mostrarDetalle(CajaMercadoPagoDTO caja) {

        boolean haySeleccion = caja != null;
        panelDetalle.setVisible(haySeleccion);
        lblSinSeleccion.setVisible(!haySeleccion);

        if (!haySeleccion) {
            return;
        }

        lblDetalleNombre.setText(valor(caja.getNombre()));
        txtNuevoExternalId.setText(caja.getExternalId() != null ? caja.getExternalId() : "");
        btnPdf.setDisable(caja.getQrPdf() == null || caja.getQrPdf().isBlank());

        cargarImagenQr(caja.getQrImagen());

        gridDatos.getChildren().clear();
        int fila = 0;
        agregarDato("Caja del sistema", caja.getCajaAsignada(), fila++);
        agregarDato("ID", caja.getId(), fila++);
        agregarDato("External POS ID", caja.getExternalId(), fila++);
        agregarDato("Sucursal (ID)", caja.getStoreId(), fila++);
        agregarDato("Sucursal (external)", caja.getExternalStoreId(), fila++);
        agregarDato("Categoría (MCC)", caja.getCategoria(), fila++);
        agregarDato("Modo de operación", caja.getModoOperacion(), fila++);
        agregarDato("Estado", caja.getEstado(), fila++);
        agregarDato("UUID", caja.getUuid(), fila++);
        agregarDato("Creada", caja.getFechaCreacion(), fila++);
        agregarDato("Actualizada", caja.getFechaActualizacion(), fila);
    }

    private void agregarDato(String titulo, String dato, int fila) {

        Label lblTitulo = new Label(titulo);
        lblTitulo.getStyleClass().add("mp-dato-titulo");

        Label lblValor = new Label(valor(dato));
        lblValor.getStyleClass().add("mp-dato-valor");
        lblValor.setWrapText(true);

        gridDatos.add(lblTitulo, 0, fila);
        gridDatos.add(lblValor, 1, fila);
    }

    private void cargarImagenQr(String url) {

        imgQr.setImage(null);

        if (url == null || url.isBlank()) {
            lblQrEstado.setText("Sin imagen de QR");
            return;
        }

        lblQrEstado.setText("Cargando QR...");
        Image imagen = new Image(url, true);

        imagen.progressProperty().addListener((obs, anterior, progreso) -> {
            if (progreso.doubleValue() >= 1.0 && !imagen.isError()) {
                lblQrEstado.setText("");
            }
        });
        imagen.errorProperty().addListener((obs, anterior, error) -> {
            if (error) {
                lblQrEstado.setText("No se pudo cargar la imagen del QR");
            }
        });

        imgQr.setImage(imagen);
    }

    @FXML
    private void abrirPdf() {

        CajaMercadoPagoDTO caja = tblCajas.getSelectionModel().getSelectedItem();
        if (caja == null || caja.getQrPdf() == null) {
            return;
        }

        // Desktop (AWT) no debe ejecutarse en el hilo de JavaFX
        new Thread(() -> {
            try {
                Desktop.getDesktop().browse(new URI(caja.getQrPdf()));
            } catch (Exception e) {
                e.printStackTrace();
                Platform.runLater(() -> mostrarError("No se pudo abrir el PDF", e.getMessage()));
            }
        }).start();
    }

    /*
     =========================================
     CAMBIAR EXTERNAL POS ID EN MERCADO PAGO
     =========================================
     */
    @FXML
    private void guardarExternalId() {

        CajaMercadoPagoDTO caja = tblCajas.getSelectionModel().getSelectedItem();
        if (caja == null) {
            return;
        }

        String nuevo = txtNuevoExternalId.getText().trim();
        String anterior = caja.getExternalId();

        if (nuevo.equals(anterior)) {
            mostrarInfo("Sin cambios", "El External POS ID es el mismo que ya tiene la caja.");
            return;
        }

        if (!MercadoPagoPosService.esExternalIdValido(nuevo)) {
            mostrarError("External POS ID inválido",
                    "Solo se permiten letras y números, sin espacios, guiones ni símbolos (máximo 40).");
            txtNuevoExternalId.requestFocus();
            return;
        }

        boolean repetido = cajas.stream().anyMatch(c -> c != caja && nuevo.equalsIgnoreCase(c.getExternalId()));
        if (repetido) {
            mostrarError("External POS ID repetido", "Otra caja de tu cuenta ya usa \"" + nuevo + "\".");
            return;
        }

        if (!ClaveAdminUtil.validar("Ingresá la contraseña para cambiar el External POS ID en Mercado Pago.")) {
            return;
        }

        btnGuardarExternalId.setDisable(true);

        new Thread(() -> {
            try {
                posService.actualizarExternalId(caja, nuevo);

                // Si alguna caja del sistema usaba el ID anterior, pasa a usar el nuevo
                String cajasActualizadas = reemplazarEnConfiguracion(anterior, nuevo);

                Platform.runLater(() -> {
                    btnGuardarExternalId.setDisable(false);
                    notificarCambioConfiguracion();
                    mostrarInfo("External POS ID actualizado",
                            "La caja \"" + valor(caja.getNombre()) + "\" ahora tiene el External POS ID \"" + nuevo + "\"."
                            + (cajasActualizadas.isEmpty() ? ""
                                    : "\n\nTambién se actualizó la configuración de " + cajasActualizadas + "."));
                    cargarCajas(caja.getId());
                });

            } catch (Exception e) {
                e.printStackTrace();
                Platform.runLater(() -> {
                    btnGuardarExternalId.setDisable(false);
                    mostrarError("No se pudo actualizar la caja", e.getMessage());
                });
            }
        }).start();
    }

    private String reemplazarEnConfiguracion(String anterior, String nuevo) {

        MercadoPagoToken config = tokenDAO.obtenerToken();
        if (config == null || anterior == null || anterior.isBlank()) {
            return "";
        }

        StringBuilder actualizadas = new StringBuilder();
        for (int numero = 1; numero <= 3; numero++) {
            if (anterior.equals(config.getExternalPosIdCaja(numero))) {
                config.setExternalPosIdCaja(numero, nuevo);
                actualizadas.append(actualizadas.length() > 0 ? ", " : "").append("Caja ").append(numero);
            }
        }

        if (actualizadas.length() > 0) {
            tokenDAO.actualizar(config);
        }
        return actualizadas.toString();
    }

    /*
     =========================================
     ASIGNAR QR A UNA CAJA DEL SISTEMA
     =========================================
     */
    @FXML
    private void asignarCaja(ActionEvent event) {

        CajaMercadoPagoDTO caja = tblCajas.getSelectionModel().getSelectedItem();
        if (caja == null) {
            return;
        }

        int numeroCaja = Integer.parseInt((String) ((Node) event.getSource()).getUserData());
        String externalId = caja.getExternalId();

        if (externalId == null || externalId.isBlank()) {
            mostrarError("Caja sin External POS ID",
                    "Esta caja no tiene External POS ID. Asignale uno antes de usarla en el sistema.");
            return;
        }

        MercadoPagoToken config = tokenDAO.obtenerToken();
        if (config == null) {
            mostrarError("Sin configuración", "Primero cargá el access token en la sección Cuenta.");
            return;
        }

        if (externalId.equals(config.getExternalPosIdCaja(numeroCaja))) {
            mostrarInfo("Sin cambios", "Este QR ya está asignado a la Caja " + numeroCaja + ".");
            return;
        }

        if (!ClaveAdminUtil.validar("Ingresá la contraseña para asignar este QR a la Caja " + numeroCaja + ".")) {
            return;
        }

        // Un mismo QR no puede cobrar en dos cajas a la vez: se quita de la caja anterior
        StringBuilder quitadoDe = new StringBuilder();
        for (int numero = 1; numero <= 3; numero++) {
            if (numero != numeroCaja && externalId.equals(config.getExternalPosIdCaja(numero))) {
                config.setExternalPosIdCaja(numero, "");
                quitadoDe.append(quitadoDe.length() > 0 ? ", " : "").append("Caja ").append(numero);
            }
        }

        config.setExternalPosIdCaja(numeroCaja, externalId);
        tokenDAO.actualizar(config);

        notificarCambioConfiguracion();
        refrescarAsignaciones();

        mostrarInfo("QR asignado",
                "La Caja " + numeroCaja + " ahora cobra con el QR \"" + valor(caja.getNombre()) + "\" (" + externalId + ")."
                + (quitadoDe.length() > 0 ? "\n\nSe quitó de " + quitadoDe + ", que quedó sin QR." : ""));
    }

    private void notificarCambioConfiguracion() {
        if (onConfiguracionCambiada != null) {
            onConfiguracionCambiada.run();
        }
    }

    private String valor(String dato) {
        return dato == null || dato.isBlank() ? "—" : dato;
    }

    private void mostrarInfo(String titulo, String mensaje) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
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
