package controller;

import java.net.URL;
import java.util.ResourceBundle;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;

/**
 * Módulo Productos: pestañas Catálogo (producto_menu.fxml) y Precios (Excel)
 * (precios_excel.fxml). Al volver al catálogo se recarga la tabla, por si se
 * importaron precios nuevos.
 */
public class ProductosController implements Initializable {

    @FXML
    private TabPane pestanas;
    @FXML
    private Tab tabCatalogo;
    @FXML
    private Tab tabPrecios;

    /** Controlador del catálogo (fx:include fx:id="catalogo"). */
    @FXML
    private Producto_menuController catalogoController;

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        pestanas.getSelectionModel().selectedItemProperty().addListener((obs, anterior, actual) -> {
            if (actual == tabCatalogo && catalogoController != null) {
                catalogoController.recargarTablaProductos();
                catalogoController.cargarContador();
            }
        });
    }
}
