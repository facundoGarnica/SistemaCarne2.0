/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/javafx/FXMLController.java to edit this template
 */
package controller;

import Util.DialogoUtil;
import dao.ProductoDAO;
import java.net.URL;
import java.util.ResourceBundle;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Alert;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.Spinner;
import javafx.scene.control.SpinnerValueFactory;
import javafx.scene.control.TextField;
import model.Producto;

/**
 * FXML Controller class
 *
 * @author garca
 */
public class Crear_productoController implements Initializable {

    private static final int CODIGO_MINIMO = 1;
    private static final int CODIGO_MAXIMO = 9999;

    @FXML
    private Label lblTitulo;
    @FXML
    private Label lblSubtitulo;
    @FXML
    private TextField txtNombre;
    @FXML
    private TextField txtPrecio;
    @FXML
    private ComboBox<String> cbxCategoria;
    @FXML
    private Spinner<Integer> spinnerCodigo;

    private Producto_menuController productoMenuController;
    private ProductoDAO productoDao;
    private Producto productoActual = null;

    public void initialize(URL url, ResourceBundle rb) {
        productoDao = new ProductoDAO();
        cbxCategoria.getItems().addAll(
                "Carniceria", "Cerdo", "Pollo", "Varios", "Seco", "Preparados", "Achuras", "promociones"
        );
        cbxCategoria.setValue("Carniceria");

        SpinnerValueFactory<Integer> valueFactory
                = new SpinnerValueFactory.IntegerSpinnerValueFactory(CODIGO_MINIMO, CODIGO_MAXIMO, 1);
        spinnerCodigo.setValueFactory(valueFactory);
        spinnerCodigo.setEditable(true);

        // El editor de un Spinner editable no confirma el valor tipeado hasta perder el foco
        spinnerCodigo.focusedProperty().addListener((obs, estabaEnfocado, estaEnfocado) -> {
            if (!estaEnfocado) {
                confirmarTextoSpinner();
            }
        });

        mostrarModoEdicion(false);
    }

    private void confirmarTextoSpinner() {
        String texto = spinnerCodigo.getEditor().getText();
        try {
            int valor = Integer.parseInt(texto.trim());
            valor = Math.max(CODIGO_MINIMO, Math.min(CODIGO_MAXIMO, valor));
            spinnerCodigo.getValueFactory().setValue(valor);
        } catch (NumberFormatException e) {
            spinnerCodigo.getEditor().setText(String.valueOf(spinnerCodigo.getValue()));
        }
    }

    public void setSpa_productoController(Producto_menuController p) {
        productoMenuController = p;
    }

    @FXML
    public void cerrarOverlay() {
        if (productoMenuController != null) {
            productoMenuController.CerrarDifuminarYSpa();
        }
        limpiarFormulario();
    }

    private boolean guardarProductoBase() {
        String nombre = txtNombre.getText() == null ? "" : txtNombre.getText().trim();
        if (nombre.isEmpty()) {
            mostrarAdvertencia("Falta el nombre", "Ingresá el nombre del producto.");
            return false;
        }

        double precio;
        try {
            precio = Double.parseDouble(txtPrecio.getText().trim());
            if (precio <= 0) {
                mostrarAdvertencia("Precio inválido", "El precio debe ser mayor que 0.");
                return false;
            }
        } catch (NumberFormatException | NullPointerException e) {
            mostrarAdvertencia("Precio inválido", "Ingresá un precio numérico válido.");
            return false;
        }

        if (cbxCategoria.getValue() == null) {
            mostrarAdvertencia("Falta la categoría", "Seleccioná una categoría para el producto.");
            return false;
        }

        confirmarTextoSpinner();
        int codigo = spinnerCodigo.getValue();

        Producto existente = productoDao.buscarPorCodigo(codigo);
        if (existente != null && (productoActual == null || !existente.getId().equals(productoActual.getId()))) {
            mostrarAdvertencia("Código duplicado",
                    "Ya existe el producto \"" + existente.getNombre() + "\" con el código " + codigo + ".");
            return false;
        }

        if (productoActual == null) {
            productoActual = new Producto();
        }

        productoActual.setNombre(nombre);
        productoActual.setCodigo(codigo);
        productoActual.setPrecio(precio);
        productoActual.setTipo(cbxCategoria.getValue());

        if (productoActual.getId() == null) {
            productoDao.guardar(productoActual);
        } else {
            productoDao.actualizar(productoActual);
        }

        if (productoMenuController != null) {
            productoMenuController.recargarTablaProductos();
            productoMenuController.cargarContador();
        }

        return true;
    }

    public void asignarProductoEditable(Producto producto) {
        this.productoActual = producto;
        if (producto != null) {
            txtNombre.setText(producto.getNombre() != null ? producto.getNombre() : "");
            spinnerCodigo.getValueFactory().setValue(producto.getCodigo());
            txtPrecio.setText(producto.getPrecio() != null ? String.valueOf(producto.getPrecio()) : "");
            cbxCategoria.setValue(producto.getTipo());
            mostrarModoEdicion(true);
        } else {
            limpiarFormulario();
        }
    }

    private void limpiarFormulario() {
        txtNombre.clear();
        spinnerCodigo.getValueFactory().setValue(CODIGO_MINIMO);
        txtPrecio.clear();
        cbxCategoria.setValue(null);
        productoActual = null;
        mostrarModoEdicion(false);
    }

    private void mostrarModoEdicion(boolean editando) {
        if (editando) {
            lblTitulo.setText("✏ Editar producto");
            lblSubtitulo.setText("Modificá los datos y guardá los cambios");
        } else {
            lblTitulo.setText("➕ Agregar producto");
            lblSubtitulo.setText("Completá los datos del nuevo producto");
        }
    }

    @FXML
    public void guardarYsalir() {
        if (guardarProductoBase()) {
            if (productoMenuController != null) {
                productoMenuController.ordenarTabla();
            }
            cerrarOverlay();
        }
    }

    @FXML
    public void guardarYSiguiente() {
        Long idProductoActual = (productoActual != null) ? productoActual.getId() : null;
        String categoriaActual = cbxCategoria.getValue();
        int codigoActual = spinnerCodigo.getValue();

        if (!guardarProductoBase()) {
            return;
        }

        if (productoMenuController != null) {
            productoMenuController.ordenarTabla();
        }

        Producto siguienteProducto = productoMenuController != null
                ? productoMenuController.obtenerSiguienteProducto(idProductoActual)
                : null;

        if (siguienteProducto != null) {
            asignarProductoEditable(siguienteProducto);
        } else {
            // No hay más productos: preparar el formulario para uno nuevo,
            // conservando la categoría y sugiriendo el siguiente código
            txtNombre.clear();
            txtPrecio.clear();
            productoActual = null;
            mostrarModoEdicion(false);

            int siguienteCodigo = codigoActual + 1;
            spinnerCodigo.getValueFactory().setValue(siguienteCodigo <= CODIGO_MAXIMO ? siguienteCodigo : CODIGO_MINIMO);
            cbxCategoria.setValue(categoriaActual);
        }

        txtNombre.requestFocus();
    }

    private void mostrarAdvertencia(String titulo, String mensaje) {
        Alert alert = new Alert(Alert.AlertType.WARNING);
        DialogoUtil.aplicarEstilo(alert);
        alert.setTitle(titulo);
        alert.setHeaderText(null);
        alert.setContentText(mensaje);
        alert.showAndWait();
    }
}
