/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/javafx/FXMLController.java to edit this template
 */
package controller;

import Util.DialogoUtil;
import dao.ProductoDAO;
import java.io.IOException;
import java.net.URL;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.ResourceBundle;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.transformation.FilteredList;
import javafx.collections.transformation.SortedList;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.geometry.Bounds;
import javafx.scene.Parent;
import javafx.scene.SnapshotParameters;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Label;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableRow;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.effect.GaussianBlur;
import javafx.scene.image.ImageView;
import javafx.scene.image.WritableImage;
import javafx.scene.input.MouseButton;
import javafx.scene.layout.AnchorPane;
import model.Producto;

/**
 * FXML Controller class
 *
 * @author garca
 */
public class Producto_menuController implements Initializable {

    private Crear_productoController crearProductoController;

    @FXML
    private Label labelContador;
    @FXML
    private Label labelTotalProductos;
    @FXML
    private Label labelCategorias;
    @FXML
    private Label labelPrecioPromedio;
    @FXML
    private Label labelPrecioMax;
    @FXML
    private AnchorPane overlayCrearProducto;
    @FXML
    private AnchorPane difuminar;
    @FXML
    private TextField searchField;
    private FilteredList<Producto> listaFiltrada;
    @FXML
    private TableView<Producto> tablaProductos;

    @FXML
    private TableColumn<Producto, Integer> colCodigo;
    @FXML
    private TableColumn<Producto, String> colNombre;
    @FXML
    private TableColumn<Producto, Double> colPrecio;
    @FXML
    private TableColumn<Producto, String> colCategoria;

    @FXML
    private Button btnEditar;
    @FXML
    private Button btnEliminar;

    private ProductoDAO productoDao;
    private List<Producto> listaProductos;

    private final DecimalFormat formatoPrecio = new DecimalFormat("#,##0", DecimalFormatSymbols.getInstance(new Locale("es", "AR")));

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        productoDao = new ProductoDAO();
        listaProductos = productoDao.buscarTodos();
        listaProductos.sort(Comparator.comparingInt(Producto::getCodigo));

        // Configurar columnas
        colNombre.setCellValueFactory(new PropertyValueFactory<>("nombre"));
        colCodigo.setCellValueFactory(new PropertyValueFactory<>("codigo"));
        colPrecio.setCellValueFactory(new PropertyValueFactory<>("precio"));
        colCategoria.setCellValueFactory(new PropertyValueFactory<>("tipo"));

        // Precio sin decimales
        colPrecio.setCellFactory(column -> new TableCell<Producto, Double>() {
            @Override
            protected void updateItem(Double precio, boolean empty) {
                super.updateItem(precio, empty);
                setText(empty || precio == null ? null : "$" + formatoPrecio.format(precio));
            }
        });

        // Búsqueda: FilteredList + SortedList encadenadas sobre la tabla
        listaFiltrada = new FilteredList<>(FXCollections.observableArrayList(listaProductos), p -> true);

        searchField.textProperty().addListener((obs, oldValue, newValue) -> aplicarFiltro(newValue));

        SortedList<Producto> listaOrdenada = new SortedList<>(listaFiltrada);
        listaOrdenada.comparatorProperty().bind(tablaProductos.comparatorProperty());
        tablaProductos.setItems(listaOrdenada);

        colCodigo.setSortType(TableColumn.SortType.ASCENDING);
        tablaProductos.getSortOrder().add(colCodigo);
        tablaProductos.sort();

        // Habilitar Editar/Eliminar solo cuando hay un producto seleccionado
        tablaProductos.getSelectionModel().selectedItemProperty().addListener((obs, oldSel, newSel) -> {
            boolean haySeleccion = newSel != null;
            btnEditar.setDisable(!haySeleccion);
            btnEliminar.setDisable(!haySeleccion);
        });

        // Doble clic sobre una fila para editar directamente
        tablaProductos.setRowFactory(tv -> {
            TableRow<Producto> fila = new TableRow<>();
            fila.setOnMouseClicked(event -> {
                if (event.getClickCount() == 2 && !fila.isEmpty() && event.getButton() == MouseButton.PRIMARY) {
                    editarProducto();
                }
            });
            return fila;
        });

        actualizarContador();
        actualizarEstadisticas();
    }

    /**
     * Aplica el filtro de búsqueda por nombre, código o categoría y actualiza el contador.
     */
    private void aplicarFiltro(String texto) {
        listaFiltrada.setPredicate(producto -> {
            if (texto == null || texto.isBlank()) {
                return true;
            }
            String filtro = texto.toLowerCase().trim();
            return producto.getNombre().toLowerCase().contains(filtro)
                    || String.valueOf(producto.getCodigo()).contains(filtro)
                    || (producto.getTipo() != null && producto.getTipo().toLowerCase().contains(filtro));
        });
        actualizarContador();
    }

    public void ordenarTabla() {
        listaProductos.sort(Comparator.comparingInt(Producto::getCodigo));
        colCodigo.setSortType(TableColumn.SortType.ASCENDING);
        tablaProductos.getSortOrder().setAll(colCodigo);
        tablaProductos.sort();
    }

    public void cargarContador() {
        actualizarContador();
    }

    private void actualizarContador() {
        int totalMostrados = listaFiltrada.size();
        labelContador.setText("Mostrando " + totalMostrados + " producto" + (totalMostrados == 1 ? "" : "s"));
    }

    private void actualizarEstadisticas() {
        labelTotalProductos.setText(String.valueOf(listaProductos.size()));

        long categoriasDistintas = listaProductos.stream()
                .map(Producto::getTipo)
                .filter(tipo -> tipo != null && !tipo.isBlank())
                .distinct()
                .count();
        labelCategorias.setText(String.valueOf(categoriasDistintas));

        double precioPromedio = listaProductos.stream()
                .mapToDouble(p -> p.getPrecio() != null ? p.getPrecio() : 0.0)
                .average()
                .orElse(0.0);
        labelPrecioPromedio.setText("$" + formatoPrecio.format(precioPromedio));

        double precioMax = listaProductos.stream()
                .mapToDouble(p -> p.getPrecio() != null ? p.getPrecio() : 0.0)
                .max()
                .orElse(0.0);
        labelPrecioMax.setText("$" + formatoPrecio.format(precioMax));
    }

    public void difuminarTodo() {
        difuminar.setVisible(true);
        difuminar.setDisable(false);

        overlayCrearProducto.setVisible(true);
        overlayCrearProducto.setDisable(false);

        invocarSpa();

        Platform.runLater(() -> {
            Parent root = difuminar.getScene().getRoot();

            Bounds boundsInScene = difuminar.localToScene(difuminar.getBoundsInLocal());

            SnapshotParameters params = new SnapshotParameters();
            params.setViewport(new javafx.geometry.Rectangle2D(
                    boundsInScene.getMinX(),
                    boundsInScene.getMinY(),
                    boundsInScene.getWidth(),
                    boundsInScene.getHeight()
            ));

            WritableImage snapshot = new WritableImage(
                    (int) boundsInScene.getWidth(),
                    (int) boundsInScene.getHeight()
            );

            root.snapshot(params, snapshot);

            ImageView img = new ImageView(snapshot);
            img.setFitWidth(boundsInScene.getWidth());
            img.setFitHeight(boundsInScene.getHeight());
            img.setEffect(new GaussianBlur(20));

            img.setLayoutX(0);
            img.setLayoutY(0);

            if (difuminar.getChildren().stream().noneMatch(node -> node instanceof ImageView)) {
                difuminar.getChildren().add(0, img);
            }

            difuminar.setStyle("-fx-background-color: rgba(255, 255, 255, 0.3);");
        });
    }

    public void CerrarDifuminarYSpa() {
        difuminar.setDisable(true);
        difuminar.setVisible(false);
        difuminar.getChildren().removeIf(node -> node instanceof ImageView);

        overlayCrearProducto.setDisable(true);
        overlayCrearProducto.setVisible(false);
        overlayCrearProducto.getChildren().clear();
    }

    @FXML
    public void setRutaProducto() {
        difuminarTodo();
    }

    @FXML
    public void invocarSpa() {
        try {
            overlayCrearProducto.getChildren().clear();
            crearProductoController = null;

            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/crear_producto.fxml"));
            Parent root = loader.load();

            crearProductoController = loader.getController();
            crearProductoController.setSpa_productoController(this);

            AnchorPane.setTopAnchor(root, 168.0);
            AnchorPane.setBottomAnchor(root, 167.0);
            AnchorPane.setLeftAnchor(root, 403.0);
            AnchorPane.setRightAnchor(root, 403.0);

            overlayCrearProducto.getChildren().add(root);

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    // Edita el producto seleccionado en la tabla
    public void editarProducto() {
        Producto seleccionado = tablaProductos.getSelectionModel().getSelectedItem();
        if (seleccionado == null) {
            return;
        }

        setRutaProducto();

        // Esperar a que el formulario esté cargado antes de asignar los datos
        Platform.runLater(() -> {
            if (crearProductoController != null) {
                crearProductoController.asignarProductoEditable(seleccionado);
            }
        });
    }

    // Recarga la tabla desde la base de datos, preservando el filtro de búsqueda activo
    public void recargarTablaProductos() {
        listaProductos = productoDao.buscarTodos();
        listaProductos.sort(Comparator.comparingInt(Producto::getCodigo));

        listaFiltrada = new FilteredList<>(FXCollections.observableArrayList(listaProductos), p -> true);

        SortedList<Producto> listaOrdenada = new SortedList<>(listaFiltrada);
        listaOrdenada.comparatorProperty().bind(tablaProductos.comparatorProperty());
        tablaProductos.setItems(listaOrdenada);

        aplicarFiltro(searchField.getText());
        actualizarEstadisticas();
    }

    public Producto obtenerSiguienteProducto(Long idProductoActual) {
        if (listaProductos == null || listaProductos.isEmpty()) {
            return null;
        }

        // Si el producto actual era nuevo (null), devolver el primero de la lista
        if (idProductoActual == null) {
            return listaProductos.get(0);
        }

        int indiceActual = -1;
        for (int i = 0; i < listaProductos.size(); i++) {
            if (listaProductos.get(i).getId().equals(idProductoActual)) {
                indiceActual = i;
                break;
            }
        }

        if (indiceActual == -1) {
            return listaProductos.get(0);
        }

        if (indiceActual + 1 < listaProductos.size()) {
            return listaProductos.get(indiceActual + 1);
        }

        return null;
    }

    @FXML
    public void eliminarSeleccionado() {
        Producto seleccionado = tablaProductos.getSelectionModel().getSelectedItem();

        if (seleccionado == null) {
            return;
        }

        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        DialogoUtil.aplicarEstilo(alert);
        alert.setTitle("Confirmación de eliminación");
        alert.setHeaderText("¿Eliminar producto?");
        alert.setContentText("¿Borrar \"" + seleccionado.getNombre() + "\" (código "
                + seleccionado.getCodigo() + ")? Esta acción no se puede deshacer.");

        Optional<ButtonType> resultado = alert.showAndWait();
        if (resultado.isPresent() && resultado.get() == ButtonType.OK) {
            productoDao.eliminar(seleccionado.getId());
            recargarTablaProductos();
        }
    }
}
