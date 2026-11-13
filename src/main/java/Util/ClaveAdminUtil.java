package Util;

import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonBar;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Dialog;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.layout.VBox;

/**
 * Pide la contraseña de administrador (texto oculto) antes de una acción
 * protegida. Si es incorrecta muestra el error; si se cancela no muestra nada.
 */
public class ClaveAdminUtil {

    private static final String PASSWORD_ADMIN = "2808";

    private ClaveAdminUtil() {
    }

    public static boolean validar(String motivo) {

        Dialog<String> dialog = new Dialog<>();
        DialogoUtil.aplicarEstilo(dialog);
        dialog.setTitle("Acceso restringido");
        dialog.setHeaderText(motivo);

        ButtonType btnConfirmar = new ButtonType("Confirmar", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(btnConfirmar, ButtonType.CANCEL);

        PasswordField campoClave = new PasswordField();
        campoClave.setPromptText("Contraseña");

        VBox contenido = new VBox(10, new Label("Contraseña:"), campoClave);
        contenido.setPadding(new Insets(15, 10, 10, 10));
        dialog.getDialogPane().setContent(contenido);

        dialog.setResultConverter(boton -> boton == btnConfirmar ? campoClave.getText() : null);
        Platform.runLater(campoClave::requestFocus);

        String clave = dialog.showAndWait().orElse(null);

        if (clave == null) {
            return false;
        }

        if (!PASSWORD_ADMIN.equals(clave)) {
            Alert error = new Alert(Alert.AlertType.ERROR);
            DialogoUtil.aplicarEstilo(error);
            error.setTitle("Acceso denegado");
            error.setHeaderText(null);
            error.setContentText("Contraseña incorrecta.");
            error.showAndWait();
            return false;
        }

        return true;
    }
}
