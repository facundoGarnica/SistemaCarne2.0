package controller;

import Util.Actualizador;
import Util.ConfiguracionApp;
import Util.DialogoUtil;
import javafx.application.Platform;
import javafx.scene.control.Label;
import Util.HibernateUtil;
import java.io.IOException;
import java.net.URL;
import java.util.ResourceBundle;
import javafx.animation.FadeTransition;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Region;
import javafx.stage.Screen;
import javafx.stage.Stage;
import javafx.util.Duration;

/**
 * Pantalla principal (back-office): una barra de navegación fina arriba y el
 * módulo elegido ocupando todo el resto de la pantalla.
 *
 * Atajos de teclado: F1 Nueva Venta, F2 Historial, F3 Productos, F4 Stock,
 * F5 Personal, F6 Gastos, F7 Tarjetas, F8 Mercado Pago.
 *
 * @author facun
 */
public class MenuController implements Initializable {

    private static final String CLASE_ACTIVO = "nav-activo";

    @FXML
    private HBox barraNavegacion;
    @FXML
    private AnchorPane contenidoPrincipal;
    @FXML
    private AnchorPane overlayClientes;

    @FXML
    private Button btnCrearVenta;
    @FXML
    private Button btnHistorialVentas;
    @FXML
    private Button btnVisualizarProductos;
    @FXML
    private Button btnIngresarIndividual;
    @FXML
    private Button btnPersonal;
    @FXML
    private Button btnGastos;
    @FXML
    private Button btnVisualizarTarjeta;
    @FXML
    private Button btnConfiguracion;
    @FXML
    private Label lblVersion;

    /** Resultado de la búsqueda de versión nueva (una vez por sesión). */
    private static Boolean hayVersionNueva;

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        try {
            HibernateUtil.getSessionFactory();
            System.out.println("Hibernate inicializado desde MenuController.");
        } catch (Exception e) {
            System.err.println("Error al inicializar Hibernate: " + e.getMessage());
        }

        configurarAtajosDeTeclado();
        lblVersion.setText("v" + ConfiguracionApp.version());
        avisarSiHayVersionNueva();

        // Al entrar se muestra el historial, para no arrancar con la pantalla vacía
        handleHistorialVentas();
    }

    /**
     * Consulta GitHub en segundo plano (una sola vez por sesión) y pinta de
     * verde el botón Configuración si hay una versión nueva. Sin internet
     * no muestra nada.
     */
    private void avisarSiHayVersionNueva() {
        if (hayVersionNueva != null) {
            marcarNovedad(hayVersionNueva);
            return;
        }
        Thread hilo = new Thread(() -> {
            try {
                boolean nueva = Actualizador.esMasNueva(Actualizador.buscarUltimaVersion());
                hayVersionNueva = nueva;
                Platform.runLater(() -> marcarNovedad(nueva));
            } catch (Exception e) {
                // sin conexión: se vuelve a intentar la próxima vez que se abra el menú
            }
        }, "buscar-actualizacion");
        hilo.setDaemon(true);
        hilo.start();
    }

    private void marcarNovedad(boolean nueva) {
        btnConfiguracion.getStyleClass().remove("nav-novedad");
        if (nueva) {
            btnConfiguracion.getStyleClass().add("nav-novedad");
            btnConfiguracion.setText("Configuración ● Hay una actualización");
        } else {
            btnConfiguracion.setText("Configuración");
        }
    }

    /**
     * Registra F1..F8 en la escena. Se usa un handler (no un filter) para que
     * los controles que ya usan alguna de esas teclas (por ejemplo F4 abre un
     * ComboBox o DatePicker) la sigan recibiendo primero.
     */
    private void configurarAtajosDeTeclado() {
        barraNavegacion.sceneProperty().addListener((obs, anterior, escena) -> {
            if (escena != null) {
                escena.addEventHandler(KeyEvent.KEY_PRESSED, this::procesarAtajo);
            }
        });
    }

    private void procesarAtajo(KeyEvent e) {
        switch (e.getCode()) {
            case F1 -> abrirNuevaVenta();
            case F2 -> handleHistorialVentas();
            case F3 -> handleVisualizarProductos();
            case F4 -> handleIngresarIndividual();
            case F5 -> handlePersonal();
            case F6 -> handleGastos();
            case F7 -> handleVisualizarTarjeta();
            case F8 -> handleConfiguracion();
            default -> {
                return;
            }
        }
        e.consume();
    }

    /**
     * Carga un módulo en el área de contenido y marca su botón como activo.
     */
    private FXMLLoader abrirModulo(String fxmlPath, String titulo, Button boton) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource(fxmlPath));
            Parent vista = loader.load();

            overlayClientes.setVisible(true);
            overlayClientes.getChildren().setAll(vista);

            // Para que el contenido se estire dentro del área
            if (vista instanceof Region region) {
                AnchorPane.setTopAnchor(region, 0.0);
                AnchorPane.setBottomAnchor(region, 0.0);
                AnchorPane.setLeftAnchor(region, 0.0);
                AnchorPane.setRightAnchor(region, 0.0);
            }

            marcarActivo(boton);

            // Entrada suave
            vista.setOpacity(0);
            FadeTransition fade = new FadeTransition(Duration.millis(220), vista);
            fade.setToValue(1.0);
            fade.play();
            return loader;

        } catch (IOException e) {
            System.err.println("Error cargando vista: " + fxmlPath);
            e.printStackTrace();
            showErrorAlert("Error", "No se pudo cargar la vista: " + titulo);
            return null;
        }
    }

    private void marcarActivo(Button activo) {
        Button[] botones = {btnHistorialVentas, btnVisualizarProductos, btnIngresarIndividual,
            btnPersonal, btnGastos, btnVisualizarTarjeta, btnConfiguracion};
        for (Button b : botones) {
            b.getStyleClass().remove(CLASE_ACTIVO);
        }
        activo.getStyleClass().add(CLASE_ACTIVO);
    }

    @FXML
    private void handleHistorialVentas() {
        abrirModulo("/fxml/historialVentas.fxml", "Historial y Reportes", btnHistorialVentas);
    }

    @FXML
    private void handleVisualizarProductos() {
        abrirModulo("/fxml/producto_menu.fxml", "Productos", btnVisualizarProductos);
    }

    @FXML
    private void handleIngresarIndividual() {
        abrirModulo("/fxml/stockProducto.fxml", "Stock de productos secos", btnIngresarIndividual);
    }

    @FXML
    private void handlePersonal() {
        abrirModulo("/fxml/Personal.fxml", "Personal", btnPersonal);
    }

    @FXML
    private void handleGastos() {
        abrirModulo("/fxml/Gastos.fxml", "Gastos", btnGastos);
    }

    @FXML
    private void handleVisualizarTarjeta() {
        abrirModulo("/fxml/tarjeta.fxml", "Tarjetas", btnVisualizarTarjeta);
    }

    /** Configuración (Estilo, Mercado Pago, Actualizaciones). Si hay versión nueva, abre en Actualizaciones. */
    @FXML
    private void handleConfiguracion() {
        FXMLLoader loader = abrirModulo("/fxml/configuracion.fxml", "Configuración", btnConfiguracion);
        if (loader != null && Boolean.TRUE.equals(hayVersionNueva)
                && loader.getController() instanceof ConfiguracionController config) {
            config.mostrarActualizaciones();
        }
    }

    @FXML
    private void handleCrearVenta() {
        abrirNuevaVenta();
    }

    /**
     * Abre Nueva Venta a pantalla completa en su propia ventana y cierra esta.
     */
    private void abrirNuevaVenta() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/crear_ventas.fxml"));
            Parent root = loader.load();

            Screen screen = Screen.getPrimary();
            double width = screen.getVisualBounds().getWidth();
            double height = screen.getVisualBounds().getHeight();

            Stage stage = new Stage();
            stage.setTitle("Crear Ventas - Sistema Moderno");
            stage.setScene(new Scene(root, width, height));
            stage.setMaximized(true);
            stage.show();

            // Cerrar ventana actual
            ((Stage) btnCrearVenta.getScene().getWindow()).close();

        } catch (IOException e) {
            e.printStackTrace();
            showErrorAlert("Error", "No se pudo abrir la ventana de ventas");
        }
    }

    private void showErrorAlert(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        DialogoUtil.aplicarEstilo(alert);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
}
