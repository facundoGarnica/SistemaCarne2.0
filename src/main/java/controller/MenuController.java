package controller;

import Util.Actualizador;
import Util.ClaveAdminUtil;
import Util.ClimaService;
import Util.ConfiguracionApp;
import javafx.animation.Animation;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.scene.layout.VBox;
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
 * Al entrar muestra solo la hora y la fecha. Historial, Gastos y Personal
 * piden la contraseña de administrador.
 *
 * Atajos de teclado: F1 Nueva Venta, F2 Historial, F3 Productos, F4 Stock,
 * F5 Personal, F6 Gastos, F7 Tarjetas, F8 Configuración.
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
    @FXML
    private VBox panelInicio;
    @FXML
    private Label lblHoraInicio;
    @FXML
    private Label lblFechaInicio;
    @FXML
    private Label lblClimaIcono, lblClimaTemp, lblClimaDesc, lblClimaCiudad, lblClimaDetalle, lblClimaDetalle2;

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

        // Al entrar (y al salir de Nueva Venta) se ve solo la hora y la fecha
        iniciarReloj();
        mostrarInicio();
    }

    // ===================== INICIO: HORA Y FECHA =====================

    private static final java.util.Locale ES_AR = java.util.Locale.forLanguageTag("es-AR");
    private static final java.time.format.DateTimeFormatter FORMATO_HORA =
            java.time.format.DateTimeFormatter.ofPattern("HH:mm", ES_AR);
    private static final java.time.format.DateTimeFormatter FORMATO_FECHA =
            java.time.format.DateTimeFormatter.ofPattern("EEEE d 'de' MMMM 'de' yyyy", ES_AR);

    private Timeline reloj;

    private Timeline relojClima;

    private void iniciarReloj() {
        actualizarReloj();
        reloj = new Timeline(new KeyFrame(Duration.seconds(1), e -> actualizarReloj()));
        reloj.setCycleCount(Animation.INDEFINITE);
        reloj.play();

        actualizarClima();
        relojClima = new Timeline(new KeyFrame(Duration.minutes(15), e -> actualizarClima()));
        relojClima.setCycleCount(Animation.INDEFINITE);
        relojClima.play();
    }

    /** Busca el clima en segundo plano; sin internet o sin ciudad muestra un aviso. */
    private void actualizarClima() {
        ClimaService.Ciudad ciudad = ClimaService.ciudadGuardada();
        if (ciudad == null) {
            mostrarClimaAviso("Clima", "Elegí tu ciudad en Configuración → Estilo del sistema");
            return;
        }
        Thread hilo = new Thread(() -> {
            try {
                ClimaService.Clima c = ClimaService.climaActual(ciudad);
                Platform.runLater(() -> mostrarClima(ciudad, c));
            } catch (Exception e) {
                Platform.runLater(() -> mostrarClimaAviso(ciudad.nombre(), "Clima no disponible (sin conexión)"));
            }
        }, "clima");
        hilo.setDaemon(true);
        hilo.start();
    }

    private void mostrarClima(ClimaService.Ciudad ciudad, ClimaService.Clima c) {
        lblClimaIcono.setText(ClimaService.icono(c.codigo(), c.esDeDia()));
        lblClimaTemp.setText(Math.round(c.temperatura()) + "°");
        lblClimaDesc.setText(ClimaService.descripcion(c.codigo()));
        lblClimaCiudad.setText(ciudad.nombre());
        lblClimaDetalle.setText("Máx " + Math.round(c.maxima()) + "° · Mín " + Math.round(c.minima())
                + "° · Sensación " + Math.round(c.sensacion()) + "°");
        lblClimaDetalle2.setText("Humedad " + c.humedad() + "% · Viento " + Math.round(c.viento())
                + " km/h · Lluvia " + c.probLluvia() + "%");
    }

    private void mostrarClimaAviso(String titulo, String aviso) {
        lblClimaIcono.setText("☁");
        lblClimaTemp.setText("—");
        lblClimaDesc.setText("");
        lblClimaCiudad.setText(titulo);
        lblClimaDetalle.setText(aviso);
        lblClimaDetalle2.setText("");
    }

    private void actualizarReloj() {
        java.time.LocalDateTime ahora = java.time.LocalDateTime.now();
        lblHoraInicio.setText(ahora.format(FORMATO_HORA));
        String fecha = ahora.format(FORMATO_FECHA);
        lblFechaInicio.setText(Character.toUpperCase(fecha.charAt(0)) + fecha.substring(1));
    }

    /** Pantalla de inicio: en blanco, solo la hora y la fecha. */
    @FXML
    private void mostrarInicio() {
        overlayClientes.getChildren().clear();
        overlayClientes.setVisible(false);
        panelInicio.setVisible(true);
        marcarActivo(null);
    }

    /** Historial, Gastos y Personal piden la contraseña de administrador cada vez que se abren. */
    private boolean pedirClave(String modulo) {
        return ClaveAdminUtil.validar("Ingresá la contraseña para abrir " + modulo);
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

            panelInicio.setVisible(false);
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
        if (activo != null) {
            activo.getStyleClass().add(CLASE_ACTIVO);
        }
    }

    /** True si ese módulo ya está abierto (no se vuelve a pedir la contraseña ni a recargar). */
    private boolean yaAbierto(Button boton) {
        return overlayClientes.isVisible() && boton.getStyleClass().contains(CLASE_ACTIVO);
    }

    @FXML
    private void handleHistorialVentas() {
        if (yaAbierto(btnHistorialVentas) || !pedirClave("Historial")) {
            return;
        }
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
        if (yaAbierto(btnPersonal) || !pedirClave("Personal")) {
            return;
        }
        abrirModulo("/fxml/Personal.fxml", "Personal", btnPersonal);
    }

    @FXML
    private void handleGastos() {
        if (yaAbierto(btnGastos) || !pedirClave("Gastos")) {
            return;
        }
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
        if (reloj != null) {
            reloj.stop(); // esta ventana se cierra
        }
        if (relojClima != null) {
            relojClima.stop();
        }
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
