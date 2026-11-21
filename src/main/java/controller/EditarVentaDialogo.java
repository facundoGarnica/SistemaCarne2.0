package controller;

import Util.DialogoUtil;
import dao.ProductoDAO;
import dao.VentaDAO;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonBar;
import javafx.scene.control.ButtonType;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Dialog;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.stage.Window;
import model.DetalleVenta;
import model.Producto;
import model.Venta;

/**
 * Historial de ventas > Editar: permite cambiar el medio de pago, los
 * productos (peso, precio, quitar o agregar) y el total de una venta ya
 * registrada. Guarda todo junto con VentaDAO.editarVenta.
 */
final class EditarVentaDialogo {

    private static final String EFECTIVO = "Efectivo";
    private static final String VIRTUAL = "Virtual";

    /** Una fila editable: el producto con su peso y precio por kg. */
    private final class Fila {
        final Producto producto;
        final TextField txtPeso = new TextField();
        final TextField txtPrecio = new TextField();
        final Label lblSubtotal = new Label();
        HBox nodo;

        Fila(Producto producto, double peso, double precio) {
            this.producto = producto;
            txtPeso.setText(formatoPeso.format(peso));
            txtPrecio.setText(formatoNumero.format(precio));
            txtPeso.setPrefWidth(90);
            txtPrecio.setPrefWidth(120);
            txtPeso.textProperty().addListener((o, a, b) -> recalcular(true));
            txtPrecio.textProperty().addListener((o, a, b) -> recalcular(true));

            Label nombre = new Label(producto.getNombre());
            nombre.getStyleClass().add("ev-nombre");
            nombre.setMaxWidth(Double.MAX_VALUE);
            HBox.setHgrow(nombre, Priority.ALWAYS);
            lblSubtotal.getStyleClass().add("ev-subtotal");
            lblSubtotal.setMinWidth(110);
            lblSubtotal.setAlignment(Pos.CENTER_RIGHT);

            Button quitar = new Button("✕");
            quitar.getStyleClass().add("ev-quitar");
            quitar.setFocusTraversable(false);
            quitar.setOnAction(e -> {
                filas.remove(this);
                boxFilas.getChildren().remove(this.nodo);
                recalcular(true);
            });

            nodo = new HBox(10, nombre, txtPeso, etiqueta("kg  ×  $"), txtPrecio, lblSubtotal, quitar);
            nodo.setAlignment(Pos.CENTER_LEFT);
            nodo.getStyleClass().add("ev-fila");
        }

        Double peso() {
            return numeroPeso(txtPeso.getText());
        }

        Double precio() {
            return numero(txtPrecio.getText());
        }
    }

    private final Venta venta;
    private final DecimalFormat formatoPeso;
    private final DecimalFormat formatoNumero;
    private final DecimalFormat moneda;
    private final List<Fila> filas = new ArrayList<>();
    private final VBox boxFilas = new VBox(6);
    private final ComboBox<String> cbxMedio = new ComboBox<>();
    private final TextField txtEfectivo = new TextField();
    private final HBox boxEfectivo;
    private final TextField txtTotal = new TextField();
    private final Label lblSuma = new Label();
    private final Label lblError = new Label();
    private final TextField txtCodigo = new TextField();
    private final TextField txtPesoNuevo = new TextField();
    private boolean totalEditadoAMano;
    private boolean actualizandoTotal;

    private EditarVentaDialogo(Venta venta, List<DetalleVenta> detalles) {
        this.venta = venta;
        DecimalFormatSymbols ar = DecimalFormatSymbols.getInstance(Locale.forLanguageTag("es-AR"));
        formatoPeso = new DecimalFormat("0.###", ar);
        formatoNumero = new DecimalFormat("0.##", ar);
        moneda = new DecimalFormat("$ #,##0", ar);

        for (DetalleVenta d : detalles) {
            if (d.getProducto() != null) {
                Fila f = new Fila(d.getProducto(), valor(d.getPeso()), valor(d.getPrecio()));
                filas.add(f);
                boxFilas.getChildren().add(f.nodo);
            }
        }

        cbxMedio.getItems().addAll(EFECTIVO, VIRTUAL, Venta.MIXTO);
        cbxMedio.setValue(venta.esMixto() ? Venta.MIXTO
                : EFECTIVO.equalsIgnoreCase(venta.getMedioPago()) ? EFECTIVO : VIRTUAL);
        txtEfectivo.setPrefWidth(120);
        txtEfectivo.setText(venta.esMixto() ? formatoNumero.format(venta.montoEnEfectivo()) : "");
        boxEfectivo = new HBox(8, etiqueta("Parte en efectivo  $"), txtEfectivo);
        boxEfectivo.setAlignment(Pos.CENTER_LEFT);
        cbxMedio.valueProperty().addListener((o, a, b) -> actualizarMixto());
        actualizarMixto();

        txtTotal.setPrefWidth(140);
        txtTotal.setText(formatoNumero.format(valor(venta.getTotal())));
        txtTotal.textProperty().addListener((o, a, b) -> {
            if (!actualizandoTotal) {
                totalEditadoAMano = true;
            }
        });
        recalcular(false);
    }

    /**
     * Abre el editor. Devuelve true si se guardaron cambios.
     */
    static boolean mostrar(Window duenio, Venta venta, List<DetalleVenta> detalles) {
        return new EditarVentaDialogo(venta, detalles).abrir(duenio);
    }

    private boolean abrir(Window duenio) {
        Dialog<ButtonType> dialogo = new Dialog<>();
        DialogoUtil.aplicarEstilo(dialogo);
        dialogo.getDialogPane().getStylesheets().add(
                getClass().getResource("/styles/historial_ventas.css").toExternalForm());
        dialogo.getDialogPane().getStyleClass().add("ev-dialogo");
        if (duenio != null) {
            dialogo.initOwner(duenio);
        }
        dialogo.setTitle("Editar venta");
        dialogo.setHeaderText("Venta #" + String.format("%04d", venta.getId())
                + (venta.getFecha() != null
                        ? " · " + venta.getFecha().format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm")) : ""));

        ButtonType guardar = new ButtonType("Guardar cambios", ButtonBar.ButtonData.OK_DONE);
        dialogo.getDialogPane().getButtonTypes().addAll(guardar, ButtonType.CANCEL);
        dialogo.getDialogPane().setContent(armarContenido());
        dialogo.getDialogPane().setPrefWidth(760);
        dialogo.setResizable(true);

        // Validar y guardar sin cerrar el cuadro si hay un error
        Button btnGuardar = (Button) dialogo.getDialogPane().lookupButton(guardar);
        btnGuardar.addEventFilter(javafx.event.ActionEvent.ACTION, e -> {
            if (!guardar()) {
                e.consume();
            }
        });
        Platform.runLater(txtCodigo::requestFocus);
        return dialogo.showAndWait().orElse(ButtonType.CANCEL) == guardar;
    }

    private Node armarContenido() {
        // Medio de pago
        HBox medio = new HBox(10, titulo("Medio de pago"), cbxMedio, boxEfectivo);
        medio.setAlignment(Pos.CENTER_LEFT);

        // Productos
        ScrollPane scroll = new ScrollPane(boxFilas);
        scroll.setFitToWidth(true);
        scroll.setPrefViewportHeight(260);
        scroll.getStyleClass().add("ev-scroll");

        // Agregar producto por código
        txtCodigo.setPromptText("Código");
        txtCodigo.setPrefWidth(110);
        txtPesoNuevo.setPromptText("Peso / cant.");
        txtPesoNuevo.setPrefWidth(110);
        Button agregar = new Button("Agregar");
        agregar.getStyleClass().add("hv-btn-small");
        agregar.setOnAction(e -> agregarProducto());
        txtCodigo.setOnAction(e -> txtPesoNuevo.requestFocus());
        txtPesoNuevo.setOnAction(e -> agregarProducto());
        HBox boxAgregar = new HBox(10, etiqueta("Agregar producto:"), txtCodigo, txtPesoNuevo, agregar);
        boxAgregar.setAlignment(Pos.CENTER_LEFT);

        // Total
        Region espacio = new Region();
        HBox.setHgrow(espacio, Priority.ALWAYS);
        lblSuma.getStyleClass().add("ev-suma");
        HBox total = new HBox(10, lblSuma, espacio, titulo("Total cobrado  $"), txtTotal);
        total.setAlignment(Pos.CENTER_LEFT);

        lblError.getStyleClass().add("ev-error");
        lblError.setWrapText(true);

        VBox contenido = new VBox(14, medio, titulo("Productos"), scroll, boxAgregar, total, lblError);
        if (venta.getTarjeta() != null) {
            Label aviso = new Label("Esta venta tuvo descuento por tarjeta de fidelidad: revisá el total cobrado.");
            aviso.getStyleClass().add("ev-aviso");
            aviso.setWrapText(true);
            contenido.getChildren().add(1, aviso);
        }
        contenido.setPadding(new Insets(6, 4, 0, 4));
        return contenido;
    }

    private static Label etiqueta(String texto) {
        Label l = new Label(texto);
        l.getStyleClass().add("ev-sub");
        l.setMinWidth(Region.USE_PREF_SIZE);
        return l;
    }

    private Label titulo(String texto) {
        Label l = new Label(texto);
        l.getStyleClass().add("ev-titulo");
        l.setMinWidth(Region.USE_PREF_SIZE);
        return l;
    }

    private void actualizarMixto() {
        boolean mixto = Venta.MIXTO.equals(cbxMedio.getValue());
        boxEfectivo.setVisible(mixto);
        boxEfectivo.setManaged(mixto);
    }

    private void agregarProducto() {
        lblError.setText("");
        Integer codigo;
        try {
            codigo = Integer.parseInt(txtCodigo.getText().trim());
        } catch (NumberFormatException e) {
            lblError.setText("Escribí el código del producto.");
            txtCodigo.requestFocus();
            return;
        }
        Double peso = numeroPeso(txtPesoNuevo.getText());
        if (peso == null || peso <= 0) {
            lblError.setText("Escribí el peso (o la cantidad) del producto a agregar.");
            txtPesoNuevo.requestFocus();
            return;
        }
        Producto p = new ProductoDAO().buscarPorCodigo(codigo);
        if (p == null) {
            lblError.setText("No hay ningún producto con el código " + codigo + ".");
            txtCodigo.requestFocus();
            return;
        }
        // Mismo precio que cobraría Nueva Venta (con el descuento del producto)
        double precio = valor(p.getPrecio());
        if (p.getDescuento() != null && p.getDescuento() > 0) {
            precio = precio - precio * p.getDescuento() / 100;
        }
        Fila f = new Fila(p, peso, precio);
        filas.add(f);
        boxFilas.getChildren().add(f.nodo);
        txtCodigo.clear();
        txtPesoNuevo.clear();
        txtCodigo.requestFocus();
        recalcular(true);
    }

    /** Recalcula subtotales y la suma; si el total no se tocó a mano, lo actualiza. */
    private void recalcular(boolean actualizarTotal) {
        double suma = 0;
        for (Fila f : filas) {
            Double peso = f.peso();
            Double precio = f.precio();
            if (peso != null && precio != null) {
                double sub = Math.round(peso * precio);
                f.lblSubtotal.setText(moneda.format(sub));
                suma += sub;
            } else {
                f.lblSubtotal.setText("—");
            }
        }
        lblSuma.setText("Suma de los productos: " + moneda.format(suma));
        if (actualizarTotal && !totalEditadoAMano) {
            actualizandoTotal = true;
            txtTotal.setText(formatoNumero.format(suma));
            actualizandoTotal = false;
        }
    }

    private boolean guardar() {
        lblError.setText("");
        if (filas.isEmpty()) {
            lblError.setText("La venta tiene que tener al menos un producto. Para borrarla entera usá el botón de eliminar.");
            return false;
        }
        List<DetalleVenta> detalles = new ArrayList<>();
        for (Fila f : filas) {
            Double peso = f.peso();
            Double precio = f.precio();
            if (peso == null || peso <= 0 || precio == null || precio < 0) {
                lblError.setText("Revisá el peso y el precio de " + f.producto.getNombre() + ".");
                return false;
            }
            DetalleVenta d = new DetalleVenta();
            d.setProducto(f.producto);
            d.setPeso(peso);
            d.setPrecio(precio);
            detalles.add(d);
        }
        Double total = numero(txtTotal.getText());
        if (total == null || total <= 0) {
            lblError.setText("El total cobrado tiene que ser mayor a $0.");
            return false;
        }
        String medio = cbxMedio.getValue();
        Double efectivo = null;
        if (Venta.MIXTO.equals(medio)) {
            efectivo = numero(txtEfectivo.getText());
            if (efectivo == null || efectivo <= 0 || efectivo >= total) {
                lblError.setText("En un pago mixto, la parte en efectivo tiene que ser mayor a $0 y menor que el total.");
                return false;
            }
        }
        try {
            new VentaDAO().editarVenta(venta.getId(), medio, efectivo, Math.round(total), detalles);
            return true;
        } catch (Exception e) {
            e.printStackTrace();
            lblError.setText("No se pudo guardar (no se cambió nada): " + e.getMessage());
            return false;
        }
    }

    /** Acepta "1,5", "1.5", "1.500" y "$ 1.500,50". */
    private static Double numero(String texto) {
        if (texto == null) {
            return null;
        }
        String t = texto.replace("$", "").replace(" ", "").trim();
        if (t.isEmpty()) {
            return null;
        }
        if (t.contains(",")) {
            t = t.replace(".", "").replace(",", ".");
        } else if (t.matches("\\d{1,3}(\\.\\d{3})+")) {
            t = t.replace(".", "");
        }
        try {
            return Double.parseDouble(t);
        } catch (NumberFormatException e) {
            return null;
        }
    }

    /** Peso: la coma o el punto son decimales ("1,5" = "1.5" = 1,5 kg). */
    private static Double numeroPeso(String texto) {
        if (texto == null || texto.isBlank()) {
            return null;
        }
        try {
            return Double.parseDouble(texto.trim().replace(" ", "").replace(",", "."));
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private static double valor(Double d) {
        return d == null ? 0.0 : d;
    }
}
