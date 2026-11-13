package controller;

import Util.ClaveAdminUtil;
import Util.DialogoUtil;
import dao.MercadoPagoTokenDAO;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.input.MouseEvent;
import model.MercadoPagoToken;

/**
 * Pantalla de Mercado Pago (una sola, sin pestañas):
 *
 *  - Cuenta: access token y user ID. Los campos muestran los valores
 *    guardados pero están bloqueados: al hacer clic en cualquiera se pide la
 *    contraseña de administrador y, si es correcta, se habilitan para editar.
 *    Al guardar o descartar se vuelven a bloquear.
 *  - Cajas del sistema: qué QR (External POS ID) usa cada caja. Es solo
 *    lectura: el QR de una caja se asigna desde la sección "QR de tu cuenta"
 *    (MercadoPagoCajasController, incluida con fx:include fx:id="cajas").
 */
public class MercadoPagoTokenController {

    @FXML
    private TextField txtToken;

    @FXML
    private TextField txtUserId;

    @FXML
    private Label lblQrCaja1, lblQrCaja2, lblQrCaja3;

    @FXML
    private Label lblIdCaja1, lblIdCaja2, lblIdCaja3;

    @FXML
    private Label lblEstado;

    @FXML
    private Label lblBloqueo;

    @FXML
    private Button btnGuardar;

    @FXML
    private Button btnDescartar;

    // Controlador de la sección "QR de tu cuenta" (fx:include fx:id="cajas")
    @FXML
    private MercadoPagoCajasController cajasController;

    private TextField[] campos;

    private boolean edicionHabilitada = false;

    private final MercadoPagoTokenDAO tokenDAO = new MercadoPagoTokenDAO();

    private MercadoPagoToken tokenActual;

    @FXML
    public void initialize() {
        campos = new TextField[] { txtToken, txtUserId };

        // Clic sobre un campo bloqueado: pedir contraseña antes de dejar editar
        for (TextField campo : campos) {
            campo.addEventFilter(MouseEvent.MOUSE_PRESSED, event -> {
                if (!edicionHabilitada) {
                    event.consume();
                    solicitarEdicion(campo);
                }
            });
        }

        // La sección de QR avisa cuando cambia una asignación o cuando trae las cajas
        // (para mostrar el nombre de cada QR en las tarjetas de las cajas).
        cajasController.setOnConfiguracionCambiada(this::recargarCajasDesdeBase);

        cargarToken();
        bloquearEdicion();
    }

    /**
     * Cambiaron los QR asignados (o se trajeron las cajas de Mercado Pago):
     * se actualizan las tarjetas de las cajas sin pisar el token/user ID que
     * se esté editando.
     */
    private void recargarCajasDesdeBase() {
        MercadoPagoToken guardado = tokenDAO.obtenerToken();
        if (!edicionHabilitada) {
            cargarToken();
            return;
        }
        if (guardado != null) {
            tokenActual = guardado;
        }
        mostrarCajas();
    }

    private void solicitarEdicion(TextField campoElegido) {

        if (!ClaveAdminUtil.validar("Ingresá la contraseña para modificar la configuración de Mercado Pago.")) {
            return;
        }

        edicionHabilitada = true;
        for (TextField campo : campos) {
            campo.setEditable(true);
        }
        btnGuardar.setDisable(false);
        btnDescartar.setDisable(false);

        lblBloqueo.getStyleClass().remove("mp-aviso-bloqueado");
        lblBloqueo.getStyleClass().add("mp-aviso-editando");
        lblBloqueo.setText("Editando · guardá o descartá los cambios");

        Platform.runLater(() -> {
            campoElegido.requestFocus();
            campoElegido.end();
        });
    }

    private void bloquearEdicion() {

        edicionHabilitada = false;
        for (TextField campo : campos) {
            campo.setEditable(false);
        }
        btnGuardar.setDisable(true);
        btnDescartar.setDisable(true);

        lblBloqueo.getStyleClass().remove("mp-aviso-editando");
        if (!lblBloqueo.getStyleClass().contains("mp-aviso-bloqueado")) {
            lblBloqueo.getStyleClass().add("mp-aviso-bloqueado");
        }
        lblBloqueo.setText("Protegido · hacé clic en un campo para editar");
    }

    @FXML
    private void guardarToken() {

        String valorToken = txtToken.getText().trim();

        if (valorToken.isEmpty()) {
            mostrarAlerta("Debe ingresar un token");
            txtToken.requestFocus();
            return;
        }

        // Se relee la configuración para no pisar los QR asignados mientras se editaba
        MercadoPagoToken guardado = tokenDAO.obtenerToken();
        MercadoPagoToken token = guardado != null ? guardado : new MercadoPagoToken();

        token.setToken(valorToken);
        token.setUserId(txtUserId.getText().trim());

        if (guardado != null) {
            tokenDAO.actualizar(token);
        } else {
            tokenDAO.guardar(token);
        }

        mostrarAlerta("Datos guardados correctamente");

        cargarToken();
        bloquearEdicion();
        cajasController.refrescarAsignaciones();
    }

    @FXML
    private void descartarCambios() {
        cargarToken();
        bloquearEdicion();
    }

    @FXML
    private void eliminarToken() {

        if (tokenActual == null) {
            mostrarAlerta("No hay configuración para eliminar");
            return;
        }

        if (!ClaveAdminUtil.validar("Ingresá la contraseña para eliminar la configuración de Mercado Pago.")) {
            return;
        }

        Alert confirmacion = new Alert(Alert.AlertType.CONFIRMATION);
        DialogoUtil.aplicarEstilo(confirmacion);
        confirmacion.setTitle("Eliminar configuración");
        confirmacion.setHeaderText(null);
        confirmacion.setContentText("Se borrarán el token, el User ID y los QR de las 3 cajas.\n"
                + "No se podrá cobrar con Mercado Pago hasta volver a cargarlos.\n\n¿Desea continuar?");

        if (confirmacion.showAndWait().orElse(ButtonType.CANCEL) != ButtonType.OK) {
            return;
        }

        tokenDAO.eliminar(tokenActual.getId());

        mostrarAlerta("Configuración eliminada");

        cargarToken();
        bloquearEdicion();
        cajasController.refrescarAsignaciones();
    }

    /**
     * Lee la configuración guardada y la pone en los campos y en las tarjetas de las cajas.
     */
    private void cargarToken() {

        tokenActual = tokenDAO.obtenerToken();

        if (tokenActual != null) {
            txtToken.setText(valorOVacio(tokenActual.getToken()));
            txtUserId.setText(valorOVacio(tokenActual.getUserId()));
        } else {
            for (TextField campo : campos) {
                campo.clear();
            }
        }

        mostrarCajas();
        actualizarEstado();
    }

    /** Tarjetas Caja 1/2/3: nombre del QR (si ya se trajeron las cajas) y su External POS ID. */
    private void mostrarCajas() {
        Label[] nombres = { lblQrCaja1, lblQrCaja2, lblQrCaja3 };
        Label[] ids = { lblIdCaja1, lblIdCaja2, lblIdCaja3 };

        for (int numero = 1; numero <= 3; numero++) {
            String externalId = tokenActual != null ? tokenActual.getExternalPosIdCaja(numero) : null;
            Label nombre = nombres[numero - 1];
            Label id = ids[numero - 1];

            nombre.getStyleClass().remove("mp-caja-sin-qr");
            if (externalId == null || externalId.isBlank()) {
                nombre.setText("Sin QR asignado");
                nombre.getStyleClass().add("mp-caja-sin-qr");
                id.setText("");
            } else {
                String nombreQr = cajasController.nombreDeCaja(externalId);
                nombre.setText(nombreQr != null ? nombreQr : externalId);
                id.setText(nombreQr != null ? "External POS ID: " + externalId : "QR asignado");
            }
        }
    }

    private void actualizarEstado() {

        boolean configurado = tokenActual != null
                && tokenActual.getToken() != null
                && !tokenActual.getToken().isBlank();

        lblEstado.getStyleClass().removeAll("mp-estado-activo", "mp-estado-inactivo");
        lblEstado.getStyleClass().add(configurado ? "mp-estado-activo" : "mp-estado-inactivo");
        lblEstado.setText(configurado ? "Configurado" : "Sin configurar");
    }

    private String valorOVacio(String valor) {
        return valor != null ? valor : "";
    }

    private void mostrarAlerta(String mensaje) {

        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        DialogoUtil.aplicarEstilo(alert);

        alert.setTitle("Información");
        alert.setHeaderText(null);
        alert.setContentText(mensaje);

        alert.showAndWait();
    }
}
