package controller;

import Util.Actualizador;
import Util.Actualizador.Version;
import Util.ConfiguracionApp;
import Util.DialogoUtil;
import java.awt.Desktop;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.format.DateTimeFormatter;
import java.util.ResourceBundle;
import javafx.application.Platform;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressBar;
import javafx.scene.layout.VBox;

/**
 * Pestaña "Actualizaciones": compara la versión instalada con la última
 * publicada en GitHub, la descarga e instala (con backup previo de la base)
 * y permite volver a la versión anterior.
 */
public class ActualizacionesController implements Initializable {

    private static final DateTimeFormatter FECHA = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    @FXML private Label lblVersionInstalada;
    @FXML private Label lblVersionDisponible;
    @FXML private Label lblEstado;
    @FXML private Label lblTituloNovedades;
    @FXML private Label lblFechaPublicacion;
    @FXML private Label lblNotas;
    @FXML private Label lblAvisoInstalacion;
    @FXML private Label lblProgreso;
    @FXML private Label lblUltimoBackup;
    @FXML private VBox boxProgreso;
    @FXML private ProgressBar barraProgreso;
    @FXML private Button btnBuscar;
    @FXML private Button btnInstalar;
    @FXML private Button btnBackup;
    @FXML private Button btnVolverAtras;

    private Version ultima;
    private boolean ocupado;

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        lblVersionInstalada.setText(ConfiguracionApp.version());
        btnVolverAtras.setDisable(!ConfiguracionApp.esInstalacion() || !Actualizador.hayVersionAnterior());
        if (!ConfiguracionApp.esInstalacion()) {
            lblAvisoInstalacion.setText("Estás usando el sistema desde el IDE: instalar solo funciona en la versión instalada (.exe).");
        }
        buscarActualizaciones();
    }

    @FXML
    private void buscarActualizaciones() {
        if (ocupado) {
            return;
        }
        lblEstado.setText("Buscando…");
        btnBuscar.setDisable(true);
        btnInstalar.setDisable(true);

        Task<Version> tarea = new Task<>() {
            @Override
            protected Version call() throws Exception {
                return Actualizador.buscarUltimaVersion();
            }
        };
        tarea.setOnSucceeded(e -> {
            btnBuscar.setDisable(false);
            mostrarVersion(tarea.getValue());
        });
        tarea.setOnFailed(e -> {
            btnBuscar.setDisable(false);
            lblVersionDisponible.setText("—");
            lblEstado.setText("Sin conexión");
            lblNotas.setText("No se pudo consultar GitHub. Revisá la conexión a internet y tocá \"Buscar actualizaciones\".");
        });
        iniciar(tarea);
    }

    private void mostrarVersion(Version v) {
        ultima = v;
        if (v == null) {
            lblVersionDisponible.setText("—");
            lblEstado.setText("Sin versiones publicadas");
            lblTituloNovedades.setText("Novedades");
            lblFechaPublicacion.setText("");
            lblNotas.setText("Todavía no se publicó ninguna versión en GitHub.");
            return;
        }

        lblVersionDisponible.setText(v.numero());
        lblTituloNovedades.setText("Novedades de la versión " + v.numero());
        lblFechaPublicacion.setText(v.fecha() != null ? "Publicada el " + v.fecha().format(FECHA) : "");
        lblNotas.setText(v.notas().isBlank() ? "(Sin descripción)" : v.notas().trim());

        boolean nueva = Actualizador.esMasNueva(v);
        lblEstado.setText(nueva ? "Hay una versión nueva" : "Estás al día");
        btnInstalar.setDisable(!nueva || !ConfiguracionApp.esInstalacion());
    }

    // ===================== INSTALAR =====================

    @FXML
    private void instalar() {
        if (ultima == null || ocupado) {
            return;
        }
        if (!confirmar("Instalar actualización",
                "Se va a instalar la versión " + ultima.numero() + ".\n\n"
                + "1. Se hace una copia de la base de datos.\n"
                + "2. Se descarga la versión nueva.\n"
                + "3. El sistema se cierra y se vuelve a abrir solo.\n\n"
                + "Asegurate de no tener una venta en curso. ¿Continuar?")) {
            return;
        }
        backupYDescargar();
    }

    private void backupYDescargar() {
        mostrarProgreso("Haciendo copia de la base de datos…", -1);
        Task<Path> backup = tareaBackup();
        backup.setOnSucceeded(e -> {
            mostrarBackup(backup.getValue());
            descargar();
        });
        backup.setOnFailed(e -> {
            ocultarProgreso();
            if (confirmar("No se pudo hacer la copia",
                    "No se pudo hacer la copia de la base:\n" + backup.getException().getMessage()
                    + "\n\n¿Instalar igual, sin copia?")) {
                descargar();
            }
        });
        iniciar(backup);
    }

    private void descargar() {
        mostrarProgreso("Descargando versión " + ultima.numero() + "…", 0);
        Task<Path> descarga = new Task<>() {
            @Override
            protected Path call() throws Exception {
                return Actualizador.descargar(ultima, p -> updateProgress(p < 0 ? -1 : p, 1));
            }
        };
        barraProgreso.progressProperty().bind(descarga.progressProperty());
        descarga.setOnSucceeded(e -> {
            barraProgreso.progressProperty().unbind();
            lblProgreso.setText("Instalando… el sistema se va a cerrar y volver a abrir.");
            try {
                Actualizador.instalarAlCerrar(descarga.getValue());
                cerrarSistema();
            } catch (Exception ex) {
                ocultarProgreso();
                error("No se pudo instalar", ex.getMessage());
            }
        });
        descarga.setOnFailed(e -> {
            barraProgreso.progressProperty().unbind();
            ocultarProgreso();
            error("No se pudo descargar", descarga.getException().getMessage());
        });
        iniciar(descarga);
    }

    // ===================== BACKUP / VOLVER ATRÁS =====================

    @FXML
    private void hacerBackup() {
        if (ocupado) {
            return;
        }
        btnBackup.setDisable(true);
        lblUltimoBackup.setText("Haciendo copia…");
        Task<Path> backup = tareaBackup();
        backup.setOnSucceeded(e -> {
            btnBackup.setDisable(false);
            mostrarBackup(backup.getValue());
        });
        backup.setOnFailed(e -> {
            btnBackup.setDisable(false);
            lblUltimoBackup.setText("");
            error("No se pudo hacer la copia", backup.getException().getMessage());
        });
        iniciar(backup);
    }

    @FXML
    private void abrirCarpetaBackups() {
        try {
            Path carpeta = ConfiguracionApp.carpetaBackups();
            Files.createDirectories(carpeta);
            Desktop.getDesktop().open(carpeta.toFile());
        } catch (Exception e) {
            error("No se pudo abrir la carpeta", e.getMessage());
        }
    }

    @FXML
    private void volverAtras() {
        if (ocupado || !confirmar("Volver a la versión anterior",
                "El sistema se va a cerrar y volver a abrir con la versión que tenías antes de la última actualización.\n\n¿Continuar?")) {
            return;
        }
        try {
            Actualizador.volverAVersionAnteriorAlCerrar();
            cerrarSistema();
        } catch (Exception e) {
            error("No se pudo volver atrás", e.getMessage());
        }
    }

    // ===================== AUXILIARES =====================

    private Task<Path> tareaBackup() {
        return new Task<>() {
            @Override
            protected Path call() throws Exception {
                return Actualizador.hacerBackup();
            }
        };
    }

    private void iniciar(Task<?> tarea) {
        ocupado = true;
        tarea.runningProperty().addListener((obs, antes, corriendo) -> {
            if (!corriendo) {
                ocupado = false;
            }
        });
        Thread hilo = new Thread(tarea, "actualizador");
        hilo.setDaemon(true);
        hilo.start();
    }

    private void mostrarBackup(Path archivo) {
        lblUltimoBackup.setText("Última copia: " + archivo.getFileName());
    }

    private void mostrarProgreso(String texto, double progreso) {
        btnInstalar.setDisable(true);
        btnBuscar.setDisable(true);
        lblProgreso.setText(texto);
        barraProgreso.progressProperty().unbind();
        barraProgreso.setProgress(progreso);
        boxProgreso.setVisible(true);
        boxProgreso.setManaged(true);
    }

    private void ocultarProgreso() {
        boxProgreso.setVisible(false);
        boxProgreso.setManaged(false);
        btnBuscar.setDisable(false);
        btnInstalar.setDisable(ultima == null || !Actualizador.esMasNueva(ultima) || !ConfiguracionApp.esInstalacion());
    }

    /** Cierra el sistema para liberar el .jar; el .bat lo vuelve a abrir. */
    private void cerrarSistema() {
        Platform.exit();
        System.exit(0);
    }

    private boolean confirmar(String titulo, String mensaje) {
        Alert a = new Alert(Alert.AlertType.CONFIRMATION, mensaje, ButtonType.OK, ButtonType.CANCEL);
        DialogoUtil.aplicarEstilo(a);
        a.setTitle(titulo);
        a.setHeaderText(null);
        return a.showAndWait().orElse(ButtonType.CANCEL) == ButtonType.OK;
    }

    private void error(String titulo, String mensaje) {
        Alert a = new Alert(Alert.AlertType.ERROR);
        DialogoUtil.aplicarEstilo(a);
        a.setTitle(titulo);
        a.setHeaderText(null);
        a.setContentText(mensaje);
        a.showAndWait();
    }
}
