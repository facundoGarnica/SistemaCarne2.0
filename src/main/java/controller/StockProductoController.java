package controller;

import Util.DialogoUtil;
import dao.ProductoDAO;
import dao.StockDAO;
import java.net.URL;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.ResourceBundle;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.collections.transformation.SortedList;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonBar;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Dialog;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.control.ToggleButton;
import javafx.scene.control.ToggleGroup;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.shape.Circle;
import model.Producto;
import model.Stock;

public class StockProductoController implements Initializable {

    private static final String CATEGORIA = "Seco";
    private static final String CLAVE_REINICIO = "2808";
    private static final double UMBRAL_STOCK_BAJO = 1.5;
    private static final Locale LOCALE_AR = new Locale("es", "AR");

    private enum EstadoStock {
        SIN_STOCK, STOCK_BAJO, CON_STOCK
    }

    private ProductoDAO productoDao;
    private StockDAO stockDao;
    private Producto productoSeleccionado;

    private final ObservableList<Producto> productosOriginal = FXCollections.observableArrayList();
    private FilteredList<Producto> productosFiltrados;

    @FXML
    private TableView<Producto> tablaStock;
    @FXML
    private TableColumn<Producto, Integer> colCodigo;
    @FXML
    private TableColumn<Producto, String> colProducto;
    @FXML
    private TableColumn<Producto, Double> colCantidad;
    @FXML
    private TableColumn<Producto, String> colEstado;

    // Panel del producto seleccionado
    @FXML
    private Label lblSinSeleccion;
    @FXML
    private VBox panelProducto;
    @FXML
    private Label lblCodigoSeleccionado;
    @FXML
    private Label labelProductoSeleccionado;
    @FXML
    private Label labelStock;
    @FXML
    private TextField txtCantidad;
    @FXML
    private ToggleGroup grupoMovimiento;
    @FXML
    private ToggleButton tglIngreso, tglEgreso;
    @FXML
    private Button btnRegistrarMovimiento;

    // Encabezado, buscador y filtros por estado
    @FXML
    private Label lblCategoria;
    @FXML
    private TextField txtBuscar;
    @FXML
    private ToggleGroup grupoFiltroStock;
    @FXML
    private ToggleButton tglTodos, tglSinStock, tglBajo;

    @FXML
    private Label lblTotalProductos;
    @FXML
    private Label lblSinStock;
    @FXML
    private Label lblStockBajo;

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        productoDao = new ProductoDAO();
        stockDao = new StockDAO();
        lblCategoria.setText(CATEGORIA);

        configurarColumnas();
        configurarBuscadorYFiltro();
        configurarMovimiento();
        configurarListaFiltrada();
        cargarDatosTabla();

        tablaStock.getSelectionModel().selectedItemProperty().addListener((obs, anterior, nuevo) -> {
            productoSeleccionado = nuevo;
            mostrarSeleccion();
        });
    }

    private void configurarBuscadorYFiltro() {
        txtBuscar.textProperty().addListener((obs, anterior, nuevo) -> aplicarFiltro());
        grupoFiltroStock.selectedToggleProperty().addListener((obs, anterior, nuevo) -> {
            // Siempre queda un filtro elegido
            if (nuevo == null && anterior != null) {
                anterior.setSelected(true);
                return;
            }
            aplicarFiltro();
        });
    }

    /** Ingreso / Egreso: siempre hay uno elegido y el botón cambia de texto. */
    private void configurarMovimiento() {
        grupoMovimiento.selectedToggleProperty().addListener((obs, anterior, nuevo) -> {
            if (nuevo == null && anterior != null) {
                anterior.setSelected(true);
                return;
            }
            boolean esIngreso = grupoMovimiento.getSelectedToggle() == tglIngreso;
            btnRegistrarMovimiento.setText(esIngreso ? "Registrar ingreso" : "Registrar egreso");
            btnRegistrarMovimiento.getStyleClass().remove("ps-btn-egreso");
            if (!esIngreso) {
                btnRegistrarMovimiento.getStyleClass().add("ps-btn-egreso");
            }
        });
    }

    private void configurarListaFiltrada() {
        productosFiltrados = new FilteredList<>(productosOriginal, p -> true);
        SortedList<Producto> productosOrdenados = new SortedList<>(productosFiltrados);
        productosOrdenados.comparatorProperty().bind(tablaStock.comparatorProperty());
        tablaStock.setItems(productosOrdenados);
    }

    private void aplicarFiltro() {
        String texto = txtBuscar.getText() == null ? "" : txtBuscar.getText().trim().toLowerCase();
        Object filtro = grupoFiltroStock.getSelectedToggle();

        productosFiltrados.setPredicate(producto -> {
            boolean coincideTexto = texto.isEmpty()
                    || producto.getNombre().toLowerCase().contains(texto)
                    || String.valueOf(producto.getCodigo()).contains(texto);

            boolean coincideEstado;
            if (filtro == tglSinStock) {
                coincideEstado = estadoDe(producto) == EstadoStock.SIN_STOCK;
            } else if (filtro == tglBajo) {
                coincideEstado = estadoDe(producto) == EstadoStock.STOCK_BAJO;
            } else {
                coincideEstado = true;
            }

            return coincideTexto && coincideEstado;
        });
    }

    private EstadoStock estadoDe(Producto producto) {
        double cantidad = cantidadDe(producto);
        if (cantidad <= 0) {
            return EstadoStock.SIN_STOCK;
        }
        if (cantidad <= UMBRAL_STOCK_BAJO) {
            return EstadoStock.STOCK_BAJO;
        }
        return EstadoStock.CON_STOCK;
    }

    private void configurarColumnas() {
        colCodigo.setCellValueFactory(cellData
                -> new SimpleIntegerProperty(cellData.getValue().getCodigo()).asObject());

        colProducto.setCellValueFactory(cellData
                -> new SimpleStringProperty(cellData.getValue().getNombre()));

        colCantidad.setCellValueFactory(cellData
                -> new SimpleDoubleProperty(cantidadDe(cellData.getValue())).asObject());
        colCantidad.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(Double valor, boolean vacio) {
                super.updateItem(valor, vacio);
                getStyleClass().remove("ps-texto-rojo");
                setText(vacio || valor == null ? null : formatearCantidad(valor));
                if (!vacio && valor != null && valor <= 0) {
                    getStyleClass().add("ps-texto-rojo");
                }
            }
        });

        // Estado: punto de color + texto
        colEstado.setCellValueFactory(cellData -> new SimpleStringProperty(textoEstado(estadoDe(cellData.getValue()))));
        colEstado.setCellFactory(col -> new TableCell<>() {
            private final Circle punto = new Circle(5);
            private final Label texto = new Label();
            private final HBox contenido = new HBox(10, punto, texto);
            {
                contenido.setAlignment(Pos.CENTER_LEFT);
            }

            @Override
            protected void updateItem(String valor, boolean vacio) {
                super.updateItem(valor, vacio);
                setText(null);
                Producto producto = getTableRow() != null ? getTableRow().getItem() : null;
                if (vacio || valor == null || producto == null) {
                    setGraphic(null);
                    return;
                }
                EstadoStock estado = estadoDe(producto);
                texto.setText(valor);
                punto.getStyleClass().setAll(switch (estado) {
                    case SIN_STOCK -> "ps-dot-rojo";
                    case STOCK_BAJO -> "ps-dot-naranja";
                    case CON_STOCK -> "ps-dot-gris";
                });
                texto.getStyleClass().setAll("label", switch (estado) {
                    case SIN_STOCK -> "ps-estado-rojo";
                    case STOCK_BAJO -> "ps-estado-naranja";
                    case CON_STOCK -> "ps-estado-gris";
                });
                setGraphic(contenido);
            }
        });
    }

    private String textoEstado(EstadoStock estado) {
        return switch (estado) {
            case SIN_STOCK -> "Sin stock";
            case STOCK_BAJO -> "Bajo";
            case CON_STOCK -> "Normal";
        };
    }

    private static final double EPSILON_CERO = 0.05;

    private double cantidadDe(Producto producto) {
        Stock stock = producto.getStock();
        double cantidad = stock != null ? stock.getCantidad() : 0;
        double redondeada = Math.round(cantidad * 100.0) / 100.0;
        return Math.abs(redondeada) < EPSILON_CERO ? 0 : redondeada;
    }

    private String formatearCantidad(double cantidad) {
        return String.format(LOCALE_AR, "%.2f", cantidad);
    }

    private void cargarDatosTabla() {
        List<Producto> productos = productoDao.obtenerProductoPorTipo(CATEGORIA);
        if (productos == null) {
            productos = List.of();
        }
        productos.sort((p1, p2) -> Integer.compare(p1.getCodigo(), p2.getCodigo()));

        productosOriginal.setAll(productos);
        actualizarEstadisticas(productosOriginal);
    }

    private void actualizarEstadisticas(List<Producto> productos) {
        long total = productos.size();
        long sinStock = productos.stream().filter(p -> estadoDe(p) == EstadoStock.SIN_STOCK).count();
        long stockBajo = productos.stream().filter(p -> estadoDe(p) == EstadoStock.STOCK_BAJO).count();

        lblTotalProductos.setText(String.valueOf(total));
        lblSinStock.setText(String.valueOf(sinStock));
        lblStockBajo.setText(String.valueOf(stockBajo));
    }

    private void mostrarSeleccion() {
        if (productoSeleccionado == null) {
            panelProducto.setVisible(false);
            lblSinSeleccion.setVisible(true);
            txtCantidad.clear();
            return;
        }

        double cantidad = cantidadDe(productoSeleccionado);
        lblCodigoSeleccionado.setText("Código " + productoSeleccionado.getCodigo());
        labelProductoSeleccionado.setText(productoSeleccionado.getNombre());
        labelStock.setText(formatearCantidad(cantidad));
        labelStock.getStyleClass().remove("ps-texto-rojo");
        if (cantidad <= 0) {
            labelStock.getStyleClass().add("ps-texto-rojo");
        }
        txtCantidad.clear();

        lblSinSeleccion.setVisible(false);
        panelProducto.setVisible(true);
    }

    @FXML
    public void actualizarTabla() {
        Long idSeleccionado = productoSeleccionado != null ? productoSeleccionado.getId() : null;
        cargarDatosTabla();
        if (idSeleccionado != null) {
            reseleccionar(idSeleccionado);
        }
    }

    /** Botón "Registrar ingreso/egreso" (o Enter en el campo Cantidad). */
    @FXML
    public void registrarMovimiento() {
        aplicarMovimiento(grupoMovimiento.getSelectedToggle() == tglEgreso ? -1 : 1);
    }

    private void aplicarMovimiento(int signo) {
        if (productoSeleccionado == null) {
            mostrarAdvertencia("Sin selección", "Seleccioná un producto de la tabla.");
            return;
        }

        double cantidad;
        try {
            cantidad = Double.parseDouble(txtCantidad.getText().trim().replace(',', '.'));
            if (cantidad <= 0) {
                mostrarAdvertencia("Cantidad inválida", "Ingresá una cantidad mayor que 0.");
                return;
            }
        } catch (NumberFormatException | NullPointerException e) {
            mostrarAdvertencia("Cantidad inválida", "Ingresá una cantidad numérica válida.");
            return;
        }

        if (signo < 0 && cantidad > cantidadDe(productoSeleccionado)) {
            mostrarAdvertencia("Cantidad insuficiente", "No hay suficiente stock para quitar esa cantidad.");
            return;
        }

        stockDao.sumarOCrearStockPorNombreProducto(productoSeleccionado.getNombre(), cantidad * signo);

        Long idSeleccionado = productoSeleccionado.getId();
        cargarDatosTabla();
        reseleccionar(idSeleccionado);
    }

    private void reseleccionar(Long idProducto) {
        for (Producto p : tablaStock.getItems()) {
            if (p.getId().equals(idProducto)) {
                tablaStock.getSelectionModel().select(p);
                break;
            }
        }
    }

    @FXML
    public void reiniciarTodo() {
        String clave = pedirClave();
        if (clave == null) {
            return;
        }

        if (!CLAVE_REINICIO.equals(clave)) {
            mostrarError("Contraseña incorrecta", "La contraseña ingresada no es válida.");
            return;
        }

        Alert confirmacion = new Alert(Alert.AlertType.CONFIRMATION);
        DialogoUtil.aplicarEstilo(confirmacion);
        confirmacion.setTitle("Confirmar reinicio");
        confirmacion.setHeaderText(null);
        confirmacion.setContentText("¿Seguro que querés poner en 0 el stock de todos los productos de la categoría \""
                + CATEGORIA + "\"? Esta acción no se puede deshacer.");

        Optional<ButtonType> resultado = confirmacion.showAndWait();
        if (resultado.isEmpty() || resultado.get() != ButtonType.OK) {
            return;
        }

        try {
            stockDao.reiniciarStockPorTipo(CATEGORIA);
            cargarDatosTabla();
            mostrarSeleccion();
            mostrarInfo("Stock reiniciado", "El stock de todos los productos \"" + CATEGORIA + "\" se puso en 0.");
        } catch (Exception e) {
            e.printStackTrace();
            mostrarError("Error", "No se pudo reiniciar el stock.");
        }
    }

    private String pedirClave() {
        Dialog<String> dialog = new Dialog<>();
        DialogoUtil.aplicarEstilo(dialog);
        dialog.setTitle("Reiniciar stock");
        dialog.setHeaderText("Ingresá la contraseña para reiniciar todo el stock a 0.");

        ButtonType btnConfirmar = new ButtonType("Confirmar", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(btnConfirmar, ButtonType.CANCEL);

        PasswordField campoClave = new PasswordField();
        campoClave.setPromptText("Contraseña");

        VBox contenido = new VBox(10, new Label("Contraseña:"), campoClave);
        contenido.setPadding(new Insets(15, 10, 10, 10));
        dialog.getDialogPane().setContent(contenido);

        dialog.setResultConverter(boton -> boton == btnConfirmar ? campoClave.getText() : null);

        return dialog.showAndWait().orElse(null);
    }

    private void mostrarAdvertencia(String titulo, String mensaje) {
        Alert alert = new Alert(Alert.AlertType.WARNING);
        DialogoUtil.aplicarEstilo(alert);
        alert.setTitle(titulo);
        alert.setHeaderText(null);
        alert.setContentText(mensaje);
        alert.showAndWait();
    }

    private void mostrarError(String titulo, String mensaje) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        DialogoUtil.aplicarEstilo(alert);
        alert.setTitle(titulo);
        alert.setHeaderText(null);
        alert.setContentText(mensaje);
        alert.showAndWait();
    }

    private void mostrarInfo(String titulo, String mensaje) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        DialogoUtil.aplicarEstilo(alert);
        alert.setTitle(titulo);
        alert.setHeaderText(null);
        alert.setContentText(mensaje);
        alert.showAndWait();
    }
}
