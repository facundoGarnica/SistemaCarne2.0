package carniceria.sistemacarne;

import Util.ActualizadorEjecutable;
import Util.EstiloApp;
import controller.Crear_ventasController;
import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

import java.io.IOException;

/**
 * JavaFX App
 */
public class App extends Application {

    private static Scene scene;

    @Override
    public void start(Stage stage) throws IOException {
        scene = new Scene(loadFXML("/fxml/crear_ventas"), 640, 480);
        stage.setScene(scene);
        //esto agranda la pantalla al maximo (ventana con bordes, ocupando toda la pantalla)
        stage.setMaximized(true);
        // Si se cierra la ventana con una orden QR de venta pendiente (sin
        // pasar por F12/ESC ni por "Regresar"), cancelarla para que el QR
        // físico no quede cobrando un monto con la app ya cerrada.
        stage.setOnCloseRequest(event -> Crear_ventasController.cancelarOrdenQRPendienteAlCerrarApp());
        // Paleta de colores elegida en Configuración > Estilo, en todas las ventanas
        EstiloApp.instalar();
        stage.show();
        // Si esta versión trae un SistemaCarne.exe distinto (por ejemplo, ícono nuevo), lo instala
        ActualizadorEjecutable.revisarEnSegundoPlano();
    }

    static void setRoot(String fxml) throws IOException {
        scene.setRoot(loadFXML(fxml));
    }

    private static Parent loadFXML(String fxml) throws IOException {
        FXMLLoader fxmlLoader = new FXMLLoader(App.class.getResource(fxml + ".fxml"));
        return fxmlLoader.load();
    }

    public static void main(String[] args) {
        // El zoom (Configuración > Estilo) se fija antes de arrancar JavaFX
        EstiloApp.aplicarZoomAntesDeArrancar();
        launch();
    }

}