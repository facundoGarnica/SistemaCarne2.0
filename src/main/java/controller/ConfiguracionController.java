package controller;

import java.net.URL;
import java.util.ResourceBundle;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;

/**
 * Configuración: contenedor de las pestañas Estilo del sistema, Mercado Pago
 * y Actualizaciones (cada una con su propio FXML y controlador).
 */
public class ConfiguracionController implements Initializable {

    @FXML
    private TabPane pestanas;
    @FXML
    private Tab tabEstilo;
    @FXML
    private Tab tabMercadoPago;
    @FXML
    private Tab tabActualizaciones;

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        // Nada: cada pestaña se inicializa con su propio controlador
    }

    /** Abre directamente la pestaña Actualizaciones (cuando hay una versión nueva). */
    public void mostrarActualizaciones() {
        pestanas.getSelectionModel().select(tabActualizaciones);
    }
}
