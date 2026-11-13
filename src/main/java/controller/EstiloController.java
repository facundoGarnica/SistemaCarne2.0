package controller;

import Util.DialogoUtil;
import Util.EstiloApp;
import Util.EstiloApp.Paleta;
import java.net.URL;
import java.util.ResourceBundle;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.ContentDisplay;
import javafx.scene.control.Label;
import javafx.scene.control.Toggle;
import javafx.scene.control.ToggleButton;
import javafx.scene.control.ToggleGroup;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.TilePane;
import javafx.scene.layout.VBox;

/**
 * Configuración > Estilo del sistema: elegir una de las 6 paletas (se aplica
 * al instante) y el zoom (se aplica al reiniciar).
 */
public class EstiloController implements Initializable {

    @FXML
    private TilePane contenedorPaletas;
    @FXML
    private HBox contenedorZoom;
    @FXML
    private Label lblZoomInfo;
    @FXML
    private Button btnReiniciar;

    private final ToggleGroup grupoPaletas = new ToggleGroup();
    private final ToggleGroup grupoZoom = new ToggleGroup();

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        armarPaletas();
        armarZoom();
    }

    // ===================== PALETAS =====================

    private void armarPaletas() {
        Paleta actual = EstiloApp.paleta();
        for (Paleta p : Paleta.values()) {
            ToggleButton tarjeta = new ToggleButton();
            tarjeta.setUserData(p);
            tarjeta.setToggleGroup(grupoPaletas);
            tarjeta.getStyleClass().add("est-paleta");
            tarjeta.setContentDisplay(ContentDisplay.GRAPHIC_ONLY);
            tarjeta.setMaxWidth(Double.MAX_VALUE);
            tarjeta.setPrefWidth(300);
            tarjeta.setGraphic(vistaPrevia(p));
            tarjeta.setSelected(p == actual);
            contenedorPaletas.getChildren().add(tarjeta);
        }
        evitarQuedarSinSeleccion(grupoPaletas);
        grupoPaletas.selectedToggleProperty().addListener((obs, anterior, nuevo) -> {
            if (nuevo != null) {
                EstiloApp.cambiarPaleta((Paleta) nuevo.getUserData());
            }
        });
    }

    /** Muestra de la paleta: barra oscura con botón, resaltado y fondo, más el nombre. */
    private VBox vistaPrevia(Paleta p) {
        String oscuro = p.muestras[0], primario = p.muestras[1], suave = p.muestras[2], fondo = p.muestras[3];

        Region barra = muestra(oscuro, 22);
        Region boton = muestra(primario, 22);
        boton.setMaxWidth(70);
        Region resaltado = muestra(suave, 14);
        Region fondoMuestra = muestra(fondo, 14);

        HBox fila1 = new HBox(6, barra, boton);
        HBox.setHgrow(barra, Priority.ALWAYS);
        boton.setPrefWidth(70);
        HBox fila2 = new HBox(6, resaltado, fondoMuestra);
        HBox.setHgrow(resaltado, Priority.ALWAYS);
        HBox.setHgrow(fondoMuestra, Priority.ALWAYS);

        Label nombre = new Label(p.nombre);
        nombre.getStyleClass().add("est-paleta-nombre");
        Label descripcion = new Label(p.descripcion);
        descripcion.getStyleClass().add("est-paleta-desc");

        VBox caja = new VBox(8, fila1, fila2, nombre, descripcion);
        caja.setMaxWidth(Double.MAX_VALUE);
        return caja;
    }

    private Region muestra(String color, double alto) {
        Region r = new Region();
        r.setMinHeight(alto);
        r.setPrefHeight(alto);
        r.setMaxWidth(Double.MAX_VALUE);
        r.setStyle("-fx-background-color: " + color + "; -fx-background-radius: 6;"
                + " -fx-border-color: rgba(0,0,0,0.08); -fx-border-radius: 6;");
        return r;
    }

    // ===================== ZOOM =====================

    private void armarZoom() {
        double guardado = EstiloApp.zoomGuardado();
        for (double z : EstiloApp.ZOOMS) {
            ToggleButton b = new ToggleButton(porcentaje(z));
            b.setUserData(z);
            b.setToggleGroup(grupoZoom);
            b.setMinWidth(Region.USE_PREF_SIZE);
            b.getStyleClass().add("ps-pill");
            b.setSelected(Math.abs(z - guardado) < 0.001);
            contenedorZoom.getChildren().add(b);
        }
        evitarQuedarSinSeleccion(grupoZoom);
        grupoZoom.selectedToggleProperty().addListener((obs, anterior, nuevo) -> {
            if (nuevo != null) {
                EstiloApp.guardarZoom((Double) nuevo.getUserData());
                actualizarInfoZoom();
            }
        });
        actualizarInfoZoom();
    }

    private void actualizarInfoZoom() {
        double enUso = EstiloApp.zoomAlArrancar();
        double elegido = EstiloApp.zoomGuardado();
        boolean pendiente = Math.abs(enUso - elegido) > 0.001;
        if (pendiente) {
            lblZoomInfo.setText("En uso: " + porcentaje(enUso) + ". Elegiste " + porcentaje(elegido)
                    + (EstiloApp.puedeReiniciar()
                            ? ": tocá \"Reiniciar ahora\" para aplicarlo."
                            : ": cerrá y volvé a abrir el sistema para aplicarlo."));
        } else {
            lblZoomInfo.setText("En uso: " + porcentaje(enUso) + ".");
        }
        btnReiniciar.setDisable(!pendiente || !EstiloApp.puedeReiniciar());
    }

    @FXML
    private void reiniciar() {
        Alert a = new Alert(Alert.AlertType.CONFIRMATION,
                "El sistema se va a cerrar y volver a abrir con el nuevo tamaño.\n\n¿Continuar?",
                ButtonType.OK, ButtonType.CANCEL);
        DialogoUtil.aplicarEstilo(a);
        a.setTitle("Reiniciar");
        a.setHeaderText(null);
        if (a.showAndWait().orElse(ButtonType.CANCEL) != ButtonType.OK) {
            return;
        }
        try {
            EstiloApp.reiniciar();
        } catch (Exception e) {
            Alert err = new Alert(Alert.AlertType.INFORMATION, e.getMessage());
            DialogoUtil.aplicarEstilo(err);
            err.setHeaderText(null);
            err.showAndWait();
        }
    }

    // ===================== AUXILIARES =====================

    private static String porcentaje(double z) {
        return Math.round(z * 100) + " %";
    }

    /** Un ToggleGroup no debe quedar sin selección al tocar la opción ya elegida. */
    private static void evitarQuedarSinSeleccion(ToggleGroup grupo) {
        grupo.selectedToggleProperty().addListener((obs, anterior, nuevo) -> {
            if (nuevo == null && anterior != null) {
                grupo.selectToggle(anterior);
            }
        });
    }
}
