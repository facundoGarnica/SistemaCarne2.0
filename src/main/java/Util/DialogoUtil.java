package Util;

import javafx.scene.control.Dialog;

/**
 * Aplica la temática visual del sistema (colores, tipografía y botones) a
 * cualquier Alert o Dialog. Las ventanas emergentes de JavaFX no heredan el
 * CSS de la pantalla que las abre (corren en su propia Scene), así que hay
 * que agregarles la hoja de estilos explícitamente.
 */
public class DialogoUtil {

    private static final String HOJA_DE_ESTILOS =
            DialogoUtil.class.getResource("/styles/dialogo.css").toExternalForm();

    private DialogoUtil() {
    }

    public static void aplicarEstilo(Dialog<?> dialogo) {
        dialogo.getDialogPane().getStylesheets().add(HOJA_DE_ESTILOS);
        dialogo.getDialogPane().getStyleClass().add("dialogo-app");
        // Paleta elegida en Configuración > Estilo (va última para ganar)
        EstiloApp.aplicar(dialogo.getDialogPane());
    }
}
