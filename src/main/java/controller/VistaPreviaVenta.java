package controller;

import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Group;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.control.ToggleButton;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.shape.Rectangle;

/**
 * Maqueta en miniatura de Nueva Venta para Configuración > Estilo.
 *
 * Usa las mismas clases de estilo que crear_ventas.fxml (con crear_ventas.css),
 * así que se ve igual que la pantalla real. No carga la pantalla real porque su
 * controlador se conecta a la base y a Mercado Pago.
 *
 * {@link #mostrarPaleta(String)} pinta la maqueta con los colores de una
 * paleta (estilo en línea) sin cambiar la del resto del sistema.
 */
public class VistaPreviaVenta extends StackPane {

    /** Tamaño "real" de la maqueta antes de achicarla. */
    private static final double ANCHO = 1100, ALTO = 640;

    private final AnchorPane pantalla = new AnchorPane();
    private final Rectangle recorte = new Rectangle();

    public VistaPreviaVenta(double escala) {
        pantalla.getStylesheets().add(getClass().getResource("/styles/crear_ventas.css").toExternalForm());
        pantalla.getStyleClass().addAll("mainFxmlClass", "nv-root");
        pantalla.setPrefSize(ANCHO, ALTO);
        pantalla.setMinSize(ANCHO, ALTO);
        pantalla.setMaxSize(ANCHO, ALTO);

        HBox barra = barraSuperior();
        AnchorPane.setTopAnchor(barra, 10.0);
        AnchorPane.setLeftAnchor(barra, 10.0);
        AnchorPane.setRightAnchor(barra, 10.0);

        HBox tablas = tablas();
        AnchorPane.setTopAnchor(tablas, 76.0);
        AnchorPane.setLeftAnchor(tablas, 10.0);
        AnchorPane.setRightAnchor(tablas, 10.0);
        AnchorPane.setBottomAnchor(tablas, 190.0);

        HBox inferior = panelInferior();
        AnchorPane.setLeftAnchor(inferior, 10.0);
        AnchorPane.setRightAnchor(inferior, 10.0);
        AnchorPane.setBottomAnchor(inferior, 10.0);

        pantalla.getChildren().addAll(barra, tablas, inferior);

        // Achicada y recortada con esquinas redondeadas
        recorte.setArcWidth(16);
        recorte.setArcHeight(16);
        setClip(recorte);
        setAlignment(Pos.TOP_LEFT);
        getChildren().add(new Group(pantalla));
        getStyleClass().add("est-preview");
        setEscala(escala);

        // Es solo para mirar
        setMouseTransparent(true);
        setFocusTraversable(false);
    }

    /** Ancho de la maqueta sin achicar: sirve para calcular la escala que entra en un espacio. */
    public static double anchoReal() {
        return ANCHO;
    }

    /** Cambia el tamaño de la vista previa (1.0 = tamaño real de la pantalla). */
    public final void setEscala(double escala) {
        pantalla.setScaleX(escala);
        pantalla.setScaleY(escala);
        recorte.setWidth(ANCHO * escala);
        recorte.setHeight(ALTO * escala);
        setPrefSize(ANCHO * escala, ALTO * escala);
        setMinSize(ANCHO * escala, ALTO * escala);
        setMaxSize(ANCHO * escala, ALTO * escala);
    }

    /** Pinta la maqueta con los colores dados ("-c-primario: #...; ..."). */
    public void mostrarPaleta(String variablesEnLinea) {
        pantalla.setStyle(variablesEnLinea);
    }

    // ===================== PARTES =====================

    private HBox barraSuperior() {
        Button regresar = new Button("< Regresar");
        regresar.getStyleClass().add("boton");
        regresar.setMinWidth(Button.USE_PREF_SIZE);

        HBox cajas = new HBox(8, caja("Caja 1", true), caja("Caja 2", false), caja("Caja 3", false));
        cajas.setAlignment(Pos.CENTER);

        Label texto = new Label("Código de barra");
        texto.getStyleClass().add("textoBarra");
        texto.setMinWidth(Label.USE_PREF_SIZE);
        TextField codigo = new TextField("7790001234");
        codigo.getStyleClass().add("codigoBarra");
        codigo.setPrefWidth(230);
        HBox contenidoBarra = new HBox(10, texto, codigo);
        contenidoBarra.setAlignment(Pos.CENTER_LEFT);
        contenidoBarra.setPadding(new Insets(0, 10, 0, 10));
        StackPane barraCodigo = new StackPane(contenidoBarra);
        barraCodigo.getStyleClass().add("anchoBarra");
        barraCodigo.setPrefHeight(52);
        HBox.setHgrow(barraCodigo, Priority.ALWAYS);

        HBox barra = new HBox(20, regresar, cajas, barraCodigo);
        barra.setAlignment(Pos.CENTER_LEFT);
        return barra;
    }

    private ToggleButton caja(String texto, boolean elegida) {
        ToggleButton b = new ToggleButton(texto);
        b.getStyleClass().add("boton-caja");
        b.setPrefWidth(110);
        b.setSelected(elegida);
        return b;
    }

    private HBox tablas() {
        TableView<String[]> venta = new TableView<>(FXCollections.observableArrayList(
                new String[]{"Asado", "$9.800", "1,250", "$12.250"},
                new String[]{"Vacío", "$11.500", "0,980", "$11.270"},
                new String[]{"Chorizo", "$6.900", "0,500", "$3.450"}));
        venta.getStyleClass().add("tablaCrearVenta");
        venta.getColumns().add(columna("Producto", 0));
        venta.getColumns().add(columna("Precio", 1));
        venta.getColumns().add(columna("Peso", 2));
        venta.getColumns().add(columna("Total", 3));
        venta.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_FLEX_LAST_COLUMN);
        venta.getSelectionModel().select(1);
        HBox.setHgrow(venta, Priority.ALWAYS);

        TableView<String[]> vuelto = new TableView<>(FXCollections.<String[]>observableArrayList(
                java.util.List.<String[]>of(new String[]{"$30.000", "$3.030"})));
        vuelto.getStyleClass().add("tablaCrearVenta");
        vuelto.getColumns().add(columna("Recibido", 0));
        vuelto.getColumns().add(columna("Vuelto", 1));
        vuelto.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_FLEX_LAST_COLUMN);
        vuelto.setPrefWidth(360);

        return new HBox(15, venta, vuelto);
    }

    private TableColumn<String[], String> columna(String titulo, int indice) {
        TableColumn<String[], String> c = new TableColumn<>(titulo);
        c.setCellValueFactory(f -> new SimpleStringProperty(f.getValue()[indice]));
        c.setSortable(false);
        return c;
    }

    private HBox panelInferior() {
        VBox medio = tarjetaOscura("Medio de pago", "EFECTIVO", "medioPago");
        VBox total = tarjetaOscura("Total a pagar", "$26.970", "numeroTotal");

        Label tituloMp = new Label("Transferencias de Mercado Pago");
        tituloMp.getStyleClass().add("subtitulo");
        Button anterior = new Button("<");
        anterior.getStyleClass().add("botonDeslizante");
        Button siguiente = new Button(">");
        siguiente.getStyleClass().add("botonDeslizante");
        HBox cabecera = new HBox(10, anterior, siguiente, tituloMp);
        cabecera.setAlignment(Pos.CENTER_LEFT);

        HBox datos = new HBox(10, datoMp("Monto:", "$26.970", false), datoMp("Hora:", "10:42", false),
                datoMp("Aprobado:", "Si", true));
        VBox.setVgrow(datos, Priority.ALWAYS);
        VBox mp = new VBox(8, cabecera, datos);
        mp.getStyleClass().addAll("contenedor", "nv-panel-mp");
        HBox.setHgrow(mp, Priority.ALWAYS);

        HBox inferior = new HBox(15, medio, total, mp);
        inferior.setPrefHeight(170);
        return inferior;
    }

    private VBox tarjetaOscura(String titulo, String valor, String claseValor) {
        Label t = new Label(titulo);
        t.getStyleClass().add("titulo");
        Label v = new Label(valor);
        v.getStyleClass().add(claseValor);
        VBox caja = new VBox(t, v);
        caja.setAlignment(Pos.CENTER);
        caja.setPrefWidth(300);
        caja.getStyleClass().addAll("contenedor", "nv-card-oscura");
        return caja;
    }

    private VBox datoMp(String titulo, String valor, boolean aprobado) {
        Label t = new Label(titulo);
        t.getStyleClass().add("subtitulo");
        Label v = new Label(valor);
        v.getStyleClass().add("texto-MP");
        VBox caja = new VBox(t, v);
        caja.setAlignment(Pos.CENTER);
        caja.getStyleClass().add("anchor-pane-blanco");
        if (aprobado) {
            caja.getStyleClass().add("mp-aprobado");
        }
        HBox.setHgrow(caja, Priority.ALWAYS);
        caja.setMaxWidth(Double.MAX_VALUE);
        return caja;
    }
}
