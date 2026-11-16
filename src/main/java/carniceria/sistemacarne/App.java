package carniceria.sistemacarne;

import Util.ActualizadorEjecutable;
import Util.ConfiguracionApp;
import Util.DialogoUtil;
import Util.EstiloApp;
import Util.InicioWindows;
import controller.Crear_ventasController;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;
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
        // Inicio con Windows: corrige la ruta si cambió; avisa si es una copia en la carpeta Inicio
        InicioWindows.revisarEnSegundoPlano();
        if (InicioWindows.estaEnCarpetaInicio()) {
            Platform.runLater(App::avisarCopiaEnCarpetaInicio);
        }
    }

    private static void avisarCopiaEnCarpetaInicio() {
        Alert a = new Alert(Alert.AlertType.WARNING);
        DialogoUtil.aplicarEstilo(a);
        a.setTitle("Copia del sistema en la carpeta Inicio");
        a.setHeaderText("Esta copia del sistema no se actualiza");
        a.setContentText("El sistema se abrió desde la carpeta Inicio de Windows:\n"
                + ConfiguracionApp.carpetaInstalacion() + "\n\n"
                + "Esa copia nunca recibe las actualizaciones, y Windows abre sus carpetas al prender la PC.\n\n"
                + "Para arreglarlo:\n"
                + "1. Cerrá el sistema y borrá esa copia de la carpeta Inicio.\n"
                + "2. Abrí el sistema desde su instalación (acceso directo del escritorio).\n"
                + "3. Activá Configuración > Actualizaciones > \"Iniciar con Windows\".");
        ButtonType abrir = new ButtonType("Abrir carpeta Inicio");
        a.getButtonTypes().setAll(abrir, ButtonType.OK);
        if (a.showAndWait().orElse(ButtonType.OK) == abrir) {
            try {
                java.awt.Desktop.getDesktop().open(InicioWindows.carpetaInicioUsuario().toFile());
            } catch (Exception e) {
                System.err.println("No se pudo abrir la carpeta Inicio: " + e.getMessage());
            }
        }
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