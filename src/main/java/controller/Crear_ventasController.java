/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/javafx/FXMLController.java to edit this template
 */
package controller;


import dao.TarjetaDAO;
import model.Tarjeta;
import Util.CalcularVuelto;
import Util.DatosPagos;
import Util.DialogoUtil;
import Util.HibernateUtil;
import Util.MercadoPagoApi;
import Util.MercadoPagoQRService;
import Util.Transaccion;
import Util.WifiHotspotManager;
import dao.DetalleVentaDAO;
import dao.EmpleadoDAO;
import dao.ProductoDAO;
import dao.StockDAO;
import dao.VentaDAO;
import java.io.IOException;
import java.net.URL;
import java.text.DecimalFormat;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.List;
import java.util.ResourceBundle;
import java.util.Set;

import com.fasterxml.jackson.databind.JsonNode;

import javafx.animation.Animation;
import javafx.animation.FadeTransition;
import javafx.animation.Interpolator;
import javafx.animation.KeyFrame;
import javafx.animation.KeyValue;
import javafx.animation.PauseTransition;
import javafx.animation.SequentialTransition;
import javafx.animation.Timeline;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;
import javafx.beans.binding.Bindings;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressBar;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.control.ToggleButton;
import javafx.scene.control.ToggleGroup;
import javafx.scene.control.Tooltip;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import javafx.util.Duration;
import model.DetalleVenta;
import model.Empleado;
import model.Producto;
import model.TurnoEmpleados;
import model.Venta;

/**
 * FXML Controller class
 *
 * @author garca
 */
public class Crear_ventasController implements Initializable {

    /**
     * Initializes the controller class.
     */

    // Configuración de la tarjeta de fidelidad
    private static final int MAX_SELLOS_TARJETA = 6;
    private Timeline temporizadorActual;
    private int indicePagoActual = 0;
    private EmpleadoDAO empleadoDAO;
    private TarjetaDAO tarjetaDAO;
    private MercadoPagoApi MP;
    private MercadoPagoQRService mpQRService;
    // Protege los datos de QR de las cajas, que se leen/escriben también desde
    // los hilos que generan la orden y sondean el pago.
    private final Object lockQR = new Object();
    private Timeline temporizadorQR;
    // Animación en curso del cartel de QR (aparición/desvanecimiento); se
    // detiene antes de mostrar otro estado para que no lo oculte al terminar.
    private Animation animacionBannerQR;
    private static final long TIEMPO_ESPERA_QR_MS = 300_000L; // 5 minutos
    // Referencia a la instancia activa de esta pantalla, para poder cancelar
    // una orden QR pendiente desde App.java al cerrar la ventana (X).
    private static Crear_ventasController instanciaActual;
    private List<DatosPagos> datos;
    private Venta venta;
    private VentaDAO ventaDao;
    private DetalleVentaDAO detalleVentaDao;
    // Cache para evitar crear DAOs repetidamente
    private ProductoDAO productoDAO;
    private StockDAO stockDao;

    @FXML
    private AnchorPane rootPane;
    @FXML
    private TextField txtCodigoDeBarra;
    @FXML
    private Button btnWifi;

    // Selector de caja: cada caja lleva su propia venta en paralelo (carrito,
    // medio de pago, descuento y cobro QR). La pantalla muestra siempre la
    // caja seleccionada; al cambiar de caja no se pierde nada de las otras.
    @FXML
    private ToggleButton tbCaja1, tbCaja2, tbCaja3;
    private ToggleButton[] botonesCaja;
    private int cajaActual = 1;
    private final EstadoCaja[] cajas = { new EstadoCaja(1), new EstadoCaja(2), new EstadoCaja(3) };
    // Lista fija que muestra la tabla: refleja el carrito de la caja visible
    // (Bindings.bindContent). No se le cambia la lista a la tabla con
    // setItems() al cambiar de caja porque, con la selección desactivada,
    // la tabla dejaba de redibujar las filas nuevas.
    private final ObservableList<ItemCarrito> productosEnPantalla = FXCollections.observableArrayList();
    private EstadoCaja cajaEnTabla;
    // Recuadros de la barra del código, uno fijo por caja, para las cajas que
    // no están en pantalla: "QR Caja n" con su barra de tiempo mientras
    // espera el pago, o "Caja n ✓ $monto" cuando cobró (queda hasta que se
    // entra a esa caja).
    @FXML
    private VBox slotCaja1, slotCaja2, slotCaja3;
    @FXML
    private Label lblPagoCaja1, lblPagoCaja2, lblPagoCaja3;
    @FXML
    private ProgressBar barraCaja1, barraCaja2, barraCaja3;
    private VBox[] slotsCaja;
    private Label[] textosSlot;
    private ProgressBar[] barrasSlot;

    // Estado en memoria de la zona WiFi local, para no consultar el SO en cada toggle
    private volatile boolean wifiLocalActivo = false;

    @FXML
    private TableView<ItemCarrito> tblVistaProductos;
    @FXML
    private TableColumn<ItemCarrito, String> colProducto;
    @FXML
    private TableColumn<ItemCarrito, Double> colPrecio;
    @FXML
    private TableColumn<ItemCarrito, Double> colPeso;
    @FXML
    private TableColumn<ItemCarrito, Double> colTotal;

    @FXML
    private Label lblCantidadPagar;
    @FXML
    private Label lblMedioPago;
    /** Debajo del medio de pago: "Efectivo $X · QR $Y" o "QR $X · sin cobrar $Y". */
    @FXML
    private Label lblDetalleMixto;
    /** Mercado Pago no acepta cobros QR de este monto o menos. */
    private static final int MONTO_MINIMO_QR = 15;
    @FXML
    private Label lblBadgeDescuento;

    @FXML
    private TableView<Transaccion> tbolVueltos;

    @FXML
    private TableColumn<Transaccion, String> colRecibido;

    @FXML
    private TableColumn<Transaccion, String> colVuelto;

    //Variables para guardar cliente
    //variables para mostrar mercadoPago
    @FXML
    private Label lblMonto;
    @FXML
    private Label lblHora;
    @FXML
    private Label lblEstado;
    @FXML
    private VBox vBoxAprobado;
    @FXML
    private StackPane panelPagoExitoso;
    @FXML
    private Label lblTituloPagoExitoso;
    @FXML
    private Label lblMontoRecibido;
    @FXML
    private Label lblSubtextoPagoExitoso;
    @FXML
    private StackPane panelQRListo;
    @FXML
    private Label lblQRListo;
    @FXML
    private ProgressBar barraQR;

    @Override
    public void initialize(URL url, ResourceBundle rb) {

    instanciaActual = this;

    tbolVueltos.setPlaceholder(new Label(""));
    tblVistaProductos.setPlaceholder(new Label(""));
    tbolVueltos.setVisible(false);    
    tbolVueltos.setManaged(false);    
    tbolVueltos.setPlaceholder(new Label(""));
    tblVistaProductos.setPlaceholder(new Label(""));

    // Inicializar DAOs
    empleadoDAO = new EmpleadoDAO();
    ventaDao = new VentaDAO();
    detalleVentaDao = new DetalleVentaDAO();
    productoDAO = new ProductoDAO();
    stockDao = new StockDAO();
    tarjetaDAO = new TarjetaDAO();
    mpQRService = new MercadoPagoQRService();

    // Tablas de calcular vueltos
    colRecibido.setCellValueFactory(new PropertyValueFactory<>("dineroRecibido"));
    colVuelto.setCellValueFactory(new PropertyValueFactory<>("vuelto"));

    // ==============================================================
    // CONFIGURACIÓN DE COLUMNAS DE TABLA
    // ==============================================================
    
    colProducto.setCellValueFactory(cellData -> new javafx.beans.property.SimpleStringProperty(cellData.getValue().getProducto().getNombre()));
    colPrecio.setCellValueFactory(cellData -> new SimpleDoubleProperty(cellData.getValue().getProducto().getPrecio()).asObject());
    colPeso.setCellValueFactory(cellData -> new SimpleDoubleProperty(cellData.getValue().getPeso()).asObject());
    // Columna Total
    colTotal.setCellValueFactory(cellData -> {
        ItemCarrito item = cellData.getValue();
        double precioUnitario = item.getProducto().getPrecio();
        double peso = item.getPeso();
        Double descuento = item.getProducto().getDescuento();

        double precioFinal;
        if (descuento != null && descuento > 0) {
            precioFinal = precioUnitario - (precioUnitario * descuento / 100);
        } else {
            precioFinal = precioUnitario;
        }

        double total = precioFinal * peso;
        double totalRedondeado = Math.round(total);
        return new SimpleDoubleProperty(totalRedondeado).asObject();
    });

    // La tabla muestra el carrito de la caja seleccionada
    tblVistaProductos.setItems(productosEnPantalla);
    mostrarProductosDeCaja(caja());

    // Mantener foco siempre en el TextField
    Platform.runLater(() -> txtCodigoDeBarra.requestFocus());

    // Deshabilitar selección en las tablas
    tblVistaProductos.setFocusTraversable(false);
    tblVistaProductos.setSelectionModel(null);
    tblVistaProductos.getColumns().forEach(column -> column.setSortable(false));
    tblVistaProductos.setOnMouseClicked(event -> event.consume());

    txtCodigoDeBarra.setFocusTraversable(false);

    // Selector de caja
    botonesCaja = new ToggleButton[] { tbCaja1, tbCaja2, tbCaja3 };
    ToggleGroup grupoCajas = new ToggleGroup();
    for (ToggleButton boton : botonesCaja) {
        boton.setToggleGroup(grupoCajas);
    }
    // Marcar en el botón las cajas que tienen una venta en curso
    for (EstadoCaja c : cajas) {
        c.productos.addListener((ListChangeListener<ItemCarrito>) cambio -> actualizarIndicadoresCajas());
    }

    // Recuadros de QR/pago de las otras cajas: clic en el recuadro lleva a esa caja
    slotsCaja = new VBox[] { slotCaja1, slotCaja2, slotCaja3 };
    textosSlot = new Label[] { lblPagoCaja1, lblPagoCaja2, lblPagoCaja3 };
    barrasSlot = new ProgressBar[] { barraCaja1, barraCaja2, barraCaja3 };
    for (int i = 0; i < slotsCaja.length; i++) {
        final int numeroCaja = i + 1;
        slotsCaja[i].setOnMouseClicked(e -> seleccionarCaja(numeroCaja));
    }

    // ==============================================================
    // LISTENER DE FOCO DEL TEXTFIELD
    // ==============================================================
    
    txtCodigoDeBarra.focusedProperty().addListener((obs, oldFocused, newFocused) -> {
        if (!newFocused) {
            Platform.runLater(() -> txtCodigoDeBarra.requestFocus());
        }
    });

    // ==============================================================
    // EVENT FILTER PARA TECLAS GLOBALES
    // ==============================================================

    txtCodigoDeBarra.sceneProperty().addListener((obs, oldScene, newScene) -> {
        if (newScene != null) {
            System.out.println("Escena detectada. Activando eventos de teclado.");

            newScene.addEventFilter(KeyEvent.KEY_PRESSED, event -> {

                switch (event.getCode()) {
                    case F1:
                        if ("Virtual".equals(caja().medioPago) || Venta.MIXTO.equals(caja().medioPago)) {
                            cancelarOrdenQRPendiente(caja());
                        }
                        setMedioPago("Efectivo");
                        actualizarTotales();
                        event.consume();
                        break;
                    case F2:
                        setMedioPago("Virtual");
                        actualizarTotales();
                        generarCobroQR();
                        event.consume();
                        break;
                    // "+" del teclado numérico (al lado de / * - de las cajas): QR personalizado
                    case ADD:
                        qrPersonalizado();
                        event.consume();
                        break;
                    case F4:
                        MostrarTransferencias();
                        event.consume();
                        break;
                    case F5:
                        guardarVenta();
                        event.consume();
                        break;
                    case F7:
                        AgregarHuesos();
                        event.consume();
                        break;
                    case F8:
                        AgregarCarbon();
                        event.consume();
                        break;
                    case F9:
                        AgregarHuevos();
                        event.consume();
                        break;
                    case F11:
                        toggleWifiLocal();
                        event.consume();
                        break;
                    case F12:
                        cancelarVentaEnCurso();
                        event.consume();
                        break;
                    case LEFT:
                        pagoAnterior();
                        event.consume();
                        break;
                    case RIGHT:
                        pagoSiguiente();
                        event.consume();
                        break;
                    case ENTER:
                        if (event.getTarget() == txtCodigoDeBarra) {
                            SepararCodigo();
                            event.consume();
                        }
                        break;
                    case ESCAPE:
                        cancelarVentaEnCurso();
                        event.consume();
                        break;
                    // Teclado numérico: / * - (arriba del 7 8 9) cambian de caja.
                    // No se usan los dígitos porque el lector de códigos los escribe.
                    case DIVIDE:
                        seleccionarCaja(1);
                        event.consume();
                        break;
                    case MULTIPLY:
                        seleccionarCaja(2);
                        event.consume();
                        break;
                    case SUBTRACT:
                        seleccionarCaja(3);
                        event.consume();
                        break;
                }
            });

            // Consumir el KEY_PRESSED no evita que el carácter se escriba en el
            // campo de código, así que también se descarta el KEY_TYPED.
            newScene.addEventFilter(KeyEvent.KEY_TYPED, event -> {
                String caracter = event.getCharacter();
                if ("/".equals(caracter) || "*".equals(caracter) || "-".equals(caracter) || "+".equals(caracter)) {
                    event.consume();
                }
            });
        }
    });

    // ==============================================================
    // ESTADO INICIAL DEL BOTÓN DE WIFI LOCAL
    // ==============================================================

    inicializarBotonWifi();

    // ==============================================================
    // PRECALENTAMIENTO DE HIBERNATE
    // ==============================================================

    new Thread(() -> {
        try {
            System.out.println("Inicializando Hibernate...");
            long inicio = System.currentTimeMillis();
            HibernateUtil.getSessionFactory();
            productoDAO.buscarPorCodigo(-1);
            long tiempo = System.currentTimeMillis() - inicio;
            System.out.println("✓ Hibernate inicializado en " + tiempo + "ms");
        } catch (Exception e) {
            System.out.println("Precalentamiento completado");
        }
    }).start();
}

    @FXML
    private void seleccionarCajaClick(ActionEvent event) {
        for (int i = 0; i < botonesCaja.length; i++) {
            if (botonesCaja[i] == event.getSource()) {
                seleccionarCaja(i + 1);
                return;
            }
        }
    }

    /**
     * Cambia la caja activa (1, 2 o 3), marca su botón en verde y muestra su
     * venta en curso. Las otras cajas conservan todo (carrito, medio de pago,
     * descuento y QR pendiente) y siguen cobrando en segundo plano.
     * Siempre deja seleccionada la caja actual, aunque se vuelva a hacer
     * clic sobre ella (el ToggleGroup por sí solo la deseleccionaría).
     */
    private void seleccionarCaja(int numeroCaja) {
        botonesCaja[numeroCaja - 1].setSelected(true);

        if (numeroCaja != cajaActual) {
            cajaActual = numeroCaja;
            // Lo que estuviera a medio escribir era de la otra caja
            txtCodigoDeBarra.clear();
            mostrarCajaActual();
        }

        // Al entrar a la caja, su aviso de pago ya se vio
        quitarAvisoPago(caja());

        txtCodigoDeBarra.requestFocus();
    }

    /** Caja que se está mostrando en pantalla. */
    private EstadoCaja caja() {
        return cajas[cajaActual - 1];
    }

    private boolean esCajaVisible(EstadoCaja c) {
        return c == caja();
    }

    /** Vuelca en pantalla la venta en curso de la caja seleccionada. */
    private void mostrarCajaActual() {
        EstadoCaja c = caja();
        mostrarProductosDeCaja(c);
        lblMedioPago.setText(c.medioPago);
        actualizarTotales();
        refrescarBannerQR();
        actualizarIndicadoresCajas();
    }

    /**
     * Hace que la tabla refleje el carrito de la caja indicada: copia su
     * contenido a la lista fija de la tabla y la mantiene sincronizada
     * mientras esa caja esté en pantalla.
     */
    private void mostrarProductosDeCaja(EstadoCaja c) {
        if (cajaEnTabla != null) {
            Bindings.unbindContent(productosEnPantalla, cajaEnTabla.productos);
        }
        Bindings.bindContent(productosEnPantalla, c.productos);
        cajaEnTabla = c;
    }

    private void setMedioPago(String medioPago) {
        caja().medioPago = medioPago;
        // Cualquier cambio de medio de pago (F1, F2, venta nueva) vuelve al QR por el total
        caja().montoQRPersonalizado = 0.0;
        caja().restoEnEfectivo = true;
        lblMedioPago.setText(medioPago);
        actualizarDetallePago(caja());
    }

    // ==============================================================
    // QR PERSONALIZADO: el QR por un monto escrito a mano, y el resto
    //  - en efectivo (casilla marcada) → venta "Mixto", o
    //  - sin cobrar (casilla desmarcada) → venta Virtual por el monto del QR
    // ==============================================================

    /** Total de la venta sin el redondeo a 10 del efectivo (como se cobra en virtual). */
    private double totalSinRedondeo(EstadoCaja c) {
        return Math.round(c.sumaPrecios - calcularDescuentoFidelidad(c));
    }

    /**
     * QR personalizado sin cobrar el resto (ej: picada de $4.050 cobrada $4.000):
     * la venta es Virtual y su total es el monto del QR.
     */
    private boolean esQRSinResto(EstadoCaja c) {
        return "Virtual".equals(c.medioPago) && c.montoQRPersonalizado > 0;
    }

    /** En pago Mixto, la parte en efectivo: total − lo que va por QR. */
    private double parteEfectivoMixto(EstadoCaja c) {
        return Math.max(0, calcularTotalFinal(c) - c.montoQRPersonalizado);
    }

    /** Debajo de "Medio de pago": cómo se divide el cobro (o qué no se cobra). */
    private void actualizarDetallePago(EstadoCaja c) {
        String texto = null;
        if (Venta.MIXTO.equals(c.medioPago) && c.montoQRPersonalizado > 0) {
            texto = String.format("Efectivo $%,.0f · QR $%,.0f", parteEfectivoMixto(c), c.montoQRPersonalizado);
        } else if (esQRSinResto(c)) {
            double sinCobrar = totalSinRedondeo(c) - c.montoQRPersonalizado;
            if (sinCobrar >= 1) {
                texto = String.format("QR $%,.0f · sin cobrar $%,.0f", c.montoQRPersonalizado, sinCobrar);
            }
        }
        lblDetalleMixto.setVisible(texto != null);
        lblDetalleMixto.setManaged(texto != null);
        lblDetalleMixto.setText(texto == null ? "" : texto);
    }

    /**
     * "QR personalizado (+)": pide el monto del QR y muestra el resto. La
     * casilla "Cobrar el resto en efectivo" (se marca y desmarca con +) decide:
     * marcada → pago mixto (QR + efectivo, el vuelto se calcula sobre el resto);
     * desmarcada → el resto no se cobra ni se registra.
     */
    @FXML
    private void qrPersonalizado() {
        EstadoCaja c = caja();
        if (c.productos.isEmpty()) {
            mostrarNotificacion("✗ Primero agregá los productos de la venta", "toast-error");
            return;
        }
        if (c.ventaYaCobrada) {
            mostrarNotificacion("✓ Esta venta ya fue cobrada. Presione F5 para guardarla.", "toast-exito");
            return;
        }
        double total = totalSinRedondeo(c);

        javafx.scene.control.Dialog<Double> dialogo = new javafx.scene.control.Dialog<>();
        DialogoUtil.aplicarEstilo(dialogo);
        dialogo.setTitle("QR personalizado");
        dialogo.setHeaderText(String.format("Total de la venta: $%,.0f", total));
        ButtonType generar = new ButtonType("Generar QR (F5)", javafx.scene.control.ButtonBar.ButtonData.OK_DONE);
        dialogo.getDialogPane().getButtonTypes().addAll(generar, ButtonType.CANCEL);

        TextField txtMonto = new TextField();
        txtMonto.setPromptText("Ej: 4000");
        txtMonto.setStyle("-fx-font-size: 22px; -fx-pref-height: 50px;");
        if (c.montoQRPersonalizado > 0) {
            txtMonto.setText(String.format("%.0f", c.montoQRPersonalizado));
        }
        Label lblResto = new Label();
        lblResto.setStyle("-fx-font-size: 20px; -fx-font-weight: bold;");
        javafx.scene.control.CheckBox chkResto = new javafx.scene.control.CheckBox("Cobrar el resto en efectivo   (tecla +)");
        chkResto.setStyle("-fx-font-size: 17px;");
        chkResto.setSelected(c.montoQRPersonalizado > 0 ? Venta.MIXTO.equals(c.medioPago) : true);
        VBox contenido = new VBox(10, new Label("Monto a cobrar por QR:"), txtMonto, lblResto, chkResto);
        contenido.setPadding(new javafx.geometry.Insets(16, 20, 10, 20));
        contenido.setPrefWidth(480);
        dialogo.getDialogPane().setContent(contenido);

        Button botonGenerar = (Button) dialogo.getDialogPane().lookupButton(generar);
        Runnable actualizar = () -> {
            Double qr = leerMonto(txtMonto.getText());
            boolean valido = qr != null && Math.round(qr) > MONTO_MINIMO_QR && qr <= total;
            botonGenerar.setDisable(!valido);
            if (qr == null) {
                lblResto.setText("Resto: —");
                chkResto.setDisable(true);
                return;
            }
            if (qr > total) {
                lblResto.setText("El QR no puede ser mayor que el total");
                chkResto.setDisable(true);
                return;
            }
            if (Math.round(qr) <= MONTO_MINIMO_QR) {
                lblResto.setText(String.format("El QR tiene que ser mayor a $%d", MONTO_MINIMO_QR));
                chkResto.setDisable(true);
                return;
            }
            double resto = total - Math.round(qr);
            lblResto.setText(String.format("Resto: $%,.0f", resto));
            chkResto.setDisable(resto < 1);
        };
        txtMonto.textProperty().addListener((obs, a, b) -> actualizar.run());
        chkResto.selectedProperty().addListener((obs, a, b) -> actualizar.run());
        actualizar.run();

        // + marca/desmarca la casilla (sin escribirse en el campo); F5 acepta igual que Enter
        dialogo.getDialogPane().addEventFilter(javafx.scene.input.KeyEvent.KEY_PRESSED, ev -> {
            if (ev.getCode() == javafx.scene.input.KeyCode.F5) {
                ev.consume();
                if (!botonGenerar.isDisabled()) {
                    botonGenerar.fire();
                }
            } else if (ev.getCode() == javafx.scene.input.KeyCode.ADD) {
                ev.consume();
                if (!chkResto.isDisabled()) {
                    chkResto.setSelected(!chkResto.isSelected());
                }
            }
        });
        dialogo.getDialogPane().addEventFilter(javafx.scene.input.KeyEvent.KEY_TYPED, ev -> {
            if ("+".equals(ev.getCharacter())) {
                ev.consume();
            }
        });
        Platform.runLater(txtMonto::requestFocus);

        dialogo.setResultConverter(b -> b == generar ? leerMonto(txtMonto.getText()) : null);
        Double monto = dialogo.showAndWait().orElse(null);
        if (monto != null) {
            double qr = Math.round(monto);
            boolean conResto = qr < total && chkResto.isSelected();
            setMedioPago(conResto ? Venta.MIXTO : "Virtual");
            c.montoQRPersonalizado = qr;
            c.restoEnEfectivo = conResto;
            actualizarTotales();
            // Si había un QR por otro monto, generarCobroQR lo reemplaza
            generarCobroQR();
        }
        txtCodigoDeBarra.requestFocus();
    }

    /** "450", "1.500" o "$ 1.500" → 450 / 1500. null si no es un número. */
    private static Double leerMonto(String texto) {
        if (texto == null) {
            return null;
        }
        String limpio = texto.replace("$", "").replace(" ", "").replace(".", "").replace(",", ".").trim();
        if (limpio.isEmpty()) {
            return null;
        }
        try {
            return Double.parseDouble(limpio);
        } catch (NumberFormatException e) {
            return null;
        }
    }

    /**
     * Marca en el botón de cada caja si tiene algo pendiente, para no
     * olvidarla: "Caja 2 ✓" si ya cobró por QR y falta guardar la venta
     * (F5), o "Caja 2 •" si tiene productos cargados o un QR esperando pago.
     */
    private void actualizarIndicadoresCajas() {
        for (EstadoCaja c : cajas) {
            String marca = "";
            if (c.ventaYaCobrada) {
                marca = " ✓";
            } else if (!c.productos.isEmpty() || c.ordenQRActualId != null || c.generandoQR) {
                marca = " •";
            }
            botonesCaja[c.numero - 1].setText("Caja " + c.numero + marca);
            actualizarRecuadroCaja(c);
        }
    }

    /**
     * Muestra en el recuadro fijo de la caja (barra del código) su estado,
     * solo si la caja no está en pantalla (la visible usa el cartel grande):
     *  - pagó por QR y todavía no se entró a verla: "Caja n ✓ $monto"
     *    (naranja, con el monto esperado al pasar el mouse, si no coincide);
     *  - tiene un QR esperando pago: "QR Caja n · $monto" (azul) con una
     *    barra que se vacía hasta el vencimiento real de la orden (mismo
     *    deadlineQRMillis que usa el sondeo del pago);
     *  - ya cobró por QR, se vio, pero falta guardarla (F5): "Caja n ✓ $total";
     *  - tiene productos cargados: "Caja n · Efectivo $total" (gris), con el
     *    medio de pago elegido y el total a cobrar;
     *  - nada pendiente: el recuadro queda vacío.
     * Solo redibuja si el estado cambió, para no reiniciar la barra cada vez
     * que se llama (se llama ante cualquier cambio de las cajas).
     */
    private void actualizarRecuadroCaja(EstadoCaja c) {
        String estado;
        if (esCajaVisible(c)) {
            estado = "";
        } else if (c.pagoSinVer) {
            estado = "pagado:" + c.montoPagadoSinVer;
        } else if (c.ordenQRActualId != null && !c.ventaYaCobrada) {
            estado = "qr:" + c.ordenQRActualId;
        } else if (c.ventaYaCobrada) {
            estado = "cobrada:" + calcularTotalFinal(c);
        } else if (!c.productos.isEmpty()) {
            estado = "venta:" + c.medioPago + ":" + calcularTotalFinal(c);
        } else {
            estado = "";
        }

        if (estado.equals(c.estadoRecuadro)) {
            return;
        }
        c.estadoRecuadro = estado;

        VBox recuadro = slotsCaja[c.numero - 1];
        Label texto = textosSlot[c.numero - 1];
        ProgressBar barra = barrasSlot[c.numero - 1];

        if (c.animacionBarraRecuadro != null) {
            c.animacionBarraRecuadro.stop();
            c.animacionBarraRecuadro = null;
        }
        recuadro.getStyleClass().removeAll("aviso-pago-distinto", "aviso-qr-pendiente", "aviso-venta-en-curso");
        if (c.tooltipRecuadro != null) {
            Tooltip.uninstall(recuadro, c.tooltipRecuadro);
            c.tooltipRecuadro = null;
        }

        if (estado.isEmpty()) {
            recuadro.setVisible(false);
            return;
        }

        if (c.pagoSinVer) {
            texto.setText("Caja " + c.numero + " ✓ $" + String.format("%,.0f", c.montoPagadoSinVer));
            barra.setVisible(false);
            barra.setManaged(false);

            if (Math.abs(c.montoPagadoSinVer - c.montoEsperadoSinVer) >= 1.0) {
                recuadro.getStyleClass().add("aviso-pago-distinto");
                c.tooltipRecuadro = new Tooltip("Verificar: se esperaban $" + String.format("%,.0f", c.montoEsperadoSinVer));
                Tooltip.install(recuadro, c.tooltipRecuadro);
            }
        } else if (estado.startsWith("cobrada:")) {
            texto.setText("Caja " + c.numero + " ✓ $" + String.format("%,.0f", calcularTotalFinal(c)));
            barra.setVisible(false);
            barra.setManaged(false);
        } else if (estado.startsWith("venta:")) {
            String medio = "---".equals(c.medioPago) ? "" : c.medioPago + " ";
            texto.setText("Caja " + c.numero + " · " + medio + "$" + String.format("%,.0f", calcularTotalFinal(c)));
            recuadro.getStyleClass().add("aviso-venta-en-curso");
            barra.setVisible(false);
            barra.setManaged(false);
        } else {
            texto.setText("QR Caja " + c.numero + " · $" + String.format("%,.0f", c.montoQRActual));
            recuadro.getStyleClass().add("aviso-qr-pendiente");
            barra.setVisible(true);
            barra.setManaged(true);

            long restanteMs = Math.max(0, c.deadlineQRMillis - System.currentTimeMillis());
            barra.setProgress((double) restanteMs / TIEMPO_ESPERA_QR_MS);
            c.animacionBarraRecuadro = new Timeline(new KeyFrame(Duration.millis(restanteMs),
                    new KeyValue(barra.progressProperty(), 0.0, Interpolator.LINEAR)));
            c.animacionBarraRecuadro.play();
        }

        recuadro.setOpacity(0);
        recuadro.setVisible(true);
        FadeTransition fadeIn = new FadeTransition(Duration.millis(300), recuadro);
        fadeIn.setFromValue(0);
        fadeIn.setToValue(1);
        fadeIn.play();
    }

    /**
     * Consulta en segundo plano el estado real de la Zona WiFi (Mobile
     * Hotspot de Windows) al abrir la pantalla de ventas, para que el
     * botón arranque mostrando el color correcto sin bloquear la UI.
     */
    private void inicializarBotonWifi() {
        new Thread(() -> {
            boolean activo = WifiHotspotManager.estaActivo();
            wifiLocalActivo = activo;
            Platform.runLater(() -> actualizarBotonWifi(activo));
        }).start();
    }

    /**
     * Prende o apaga la Zona WiFi (Mobile Hotspot de Windows, con el SSID
     * y contraseña que ya tiene configurados el sistema) para que los
     * clientes se conecten. Botón o tecla F11.
     */
    @FXML
    public void toggleWifiLocal() {
        btnWifi.setDisable(true);
        boolean encenderAhora = !wifiLocalActivo;

        new Thread(() -> {
            boolean exito = encenderAhora ? WifiHotspotManager.activar() : WifiHotspotManager.desactivar();
            boolean estadoFinal = exito ? encenderAhora : wifiLocalActivo;
            wifiLocalActivo = estadoFinal;

            Platform.runLater(() -> {
                actualizarBotonWifi(estadoFinal);
                btnWifi.setDisable(false);
                if (!exito) {
                    mostrarNotificacion(
                            "✗ No se pudo " + (encenderAhora ? "activar" : "desactivar") + " la zona WiFi",
                            "toast-error"
                    );
                }
            });
        }).start();
    }

    private void actualizarBotonWifi(boolean activo) {
        btnWifi.getStyleClass().remove("wifi-activo");
        if (activo) {
            btnWifi.getStyleClass().add("wifi-activo");
            btnWifi.setText("📶 WiFi Local: ON");
        } else {
            btnWifi.setText("📶 WiFi Local: OFF");
        }
    }

    private void mostrarNotificacion(String mensaje, String estilo) {
        Label toast = new Label(mensaje);
        toast.getStyleClass().add(estilo);
        toast.setOpacity(0);

        AnchorPane.setTopAnchor(toast, 60.0);
        AnchorPane.setRightAnchor(toast, 15.0);

        rootPane.getChildren().add(toast);

        FadeTransition fadeIn = new FadeTransition(Duration.millis(200), toast);
        fadeIn.setFromValue(0);
        fadeIn.setToValue(1);

        PauseTransition espera = new PauseTransition(Duration.seconds(5));

        FadeTransition fadeOut = new FadeTransition(Duration.millis(400), toast);
        fadeOut.setFromValue(1);
        fadeOut.setToValue(0);
        fadeOut.setOnFinished(e -> rootPane.getChildren().remove(toast));

        new SequentialTransition(fadeIn, espera, fadeOut).play();

        txtCodigoDeBarra.requestFocus();
    }

    /**
     * Calcula el importe de descuento de fidelidad ya aplicando el tope
     * máximo de la tarjeta (nunca descuenta más de $TOPE, sin importar
     * qué tan alto sea el porcentaje o el total de la venta).
     */
    private double calcularDescuentoFidelidad(EstadoCaja c) {

        if (!c.descuentoFidelidadActivo) {
            return 0.0;
        }

        double descuentoCalculado = c.sumaPrecios * (c.descuentoFidelidad / 100.0);

        return Math.min(descuentoCalculado, c.tope);
    }

    /**
     * Calcula el total final a pagar de la venta actual, aplicando:
     * 1) El descuento de fidelidad (con su tope) si fue ganado con la tarjeta.
     * 2) El redondeo a múltiplo de 10 solo cuando el medio de pago es Efectivo.
     *
     * Centraliza el cálculo para que el total mostrado en pantalla, el ticket
     * impreso y la venta guardada en base de datos sean siempre consistentes.
     */
    private double calcularTotalFinal(EstadoCaja c) {

        // QR personalizado sin cobrar el resto: la venta vale lo que se cobró por QR
        if (esQRSinResto(c)) {
            return c.montoQRPersonalizado;
        }

        double base = c.sumaPrecios - calcularDescuentoFidelidad(c);

        if ("Efectivo".equals(c.medioPago)) {
            int entero = (int) Math.round(base);
            int unidades = entero % 10;
            if (unidades >= 6) {
                entero = (entero / 10) * 10 + 10;
            } else {
                entero = (entero / 10) * 10;
            }
            return entero;
        }

        return Math.round(base);
    }

    /** Actualiza total, badge de descuento y tabla de vueltos de la caja visible. */
    private void actualizarTotales() {
        EstadoCaja c = caja();
        String medioPago = c.medioPago;
        double totalMostrar = calcularTotalFinal(c);

        lblCantidadPagar.setText(c.productos.isEmpty() ? "---" : String.format("$ %,.0f", totalMostrar));

        if (c.descuentoFidelidadActivo) {

            DecimalFormat df = new DecimalFormat("#.##");

            // Descuento que correspondería sin tope, y el que realmente se aplica
            double descuentoSinTope = c.sumaPrecios * (c.descuentoFidelidad / 100.0);
            double descuentoAplicado = Math.min(descuentoSinTope, c.tope);

            if (descuentoSinTope > c.tope) {
                lblBadgeDescuento.setText(
                        "Descuento tarjeta " + df.format(c.descuentoFidelidad)
                        + "% — tope aplicado: -$" + df.format(c.tope)
                );
            } else {
                lblBadgeDescuento.setText(
                        "-" + df.format(c.descuentoFidelidad) + "% TARJETA (-$" + df.format(descuentoAplicado) + ")"
                );
            }

            lblBadgeDescuento.setVisible(true);
            lblBadgeDescuento.setManaged(true);

        } else {
            lblBadgeDescuento.setVisible(false);
            lblBadgeDescuento.setManaged(false);
        }

        actualizarDetallePago(c);

        // Tabla de vueltos en Efectivo, y en pago Mixto sobre la parte en efectivo
        boolean mixto = Venta.MIXTO.equals(medioPago) && c.montoQRPersonalizado > 0;
        if (medioPago.equals("Efectivo") || mixto) {
            tbolVueltos.setVisible(true);
            tbolVueltos.setManaged(true);

            CalcularVuelto calculador = new CalcularVuelto();
            calculador.CalcularVuelto(mixto ? parteEfectivoMixto(c) : totalMostrar);

            tbolVueltos.setItems(
                    FXCollections.observableArrayList(calculador.GetListaTransacciones())
            );
        } else {
            tbolVueltos.setVisible(false);
            tbolVueltos.setManaged(false);
            tbolVueltos.getItems().clear();
        }
    }

    /**
     * Procesa el escaneo de una tarjeta de fidelidad (código de barra
     * EAN-13, validado previamente contra la base en SepararCodigo().
     * Suma 1 sello a la tarjeta y, si al sumarlo llega a los 6 sellos,
     * aplica el descuento (y tope) configurados en la tarjeta sobre el total
     * de la venta actual.
     */
    private void procesarCodigoTarjeta(String codigoBarra) {

        Tarjeta tarjeta = tarjetaDAO.buscarPorCodigoBarra(codigoBarra);

        if (tarjeta == null) {
            mostrarNotificacion("✗ Tarjeta no encontrada", "toast-error");
            return;
        }

        if (!tarjeta.getEstado()) {
            mostrarNotificacion("✗ Tarjeta inactiva", "toast-error");
            return;
        }

        if (tarjeta.getSellosActuales() >= MAX_SELLOS_TARJETA) {
            mostrarNotificacion("✗ Tarjeta completa (uso no válido)", "toast-error");
            return;
        }

        EstadoCaja c = caja();

        // Evita sumar varios sellos en la misma venta por escanear dos veces
        if (!c.tarjetasSelladas.add(codigoBarra)) {
            mostrarNotificacion("✗ Esta tarjeta ya se selló en esta venta", "toast-error");
            return;
        }

        int nuevosSellos = tarjeta.getSellosActuales() + 1;
        tarjeta.setSellosActuales(nuevosSellos);
        tarjetaDAO.actualizar(tarjeta);

        if (nuevosSellos == MAX_SELLOS_TARJETA) {

            // Tarjeta completa: aplicar descuento a esta venta y reiniciar sellos
            c.descuentoFidelidad = tarjeta.getDescuento();
            c.tope = tarjeta.getTope();
            c.descuentoFidelidadActivo = true;
            actualizarTotales();
            regenerarQRSiCambioTotal();

            mostrarNotificacion("✓ ¡Tarjeta completa! Descuento aplicado", "toast-exito");

        } else {
            mostrarNotificacion(
                    "✓ Sello " + nuevosSellos + "/" + MAX_SELLOS_TARJETA + " registrado",
                    "toast-exito"
            );
        }
    }

 private void procesarCodigoEmpleado(String token) {

    try {

        // VALIDAR LONGITUD
        if (token == null || token.length() != 13) {

            mostrarError(
                "Token inválido",
                "El token debe tener 13 dígitos."
            );

            return;
        }

        // ==============================
        // EXTRAER DATOS DEL TOKEN
        // ==============================

        // ID empleado -> posiciones 2 a 5
        Long empleadoId = Long.parseLong(
                token.substring(2, 6)
        );

        // Turno -> posición 6
        String turno = token.substring(6, 7).equals("1")
                ? "Mañana"
                : "Tarde";

        // ==============================
        // BUSCAR EMPLEADO
        // ==============================

        Empleado emp = null;

        for (Empleado e : empleadoDAO.buscarTodos()) {

            if (e.getId().equals(empleadoId)) {
                emp = e;
                break;
            }
        }

        if (emp == null) {

            mostrarError(
                "Empleado no encontrado",
                "No existe un empleado con ID: " + empleadoId
            );

            return;
        }

        // ==============================
        // VALIDAR TOKEN REAL
        // ==============================

        boolean tokenValido =
                token.equals(emp.getTokenM())
                || token.equals(emp.getTokenT());

        if (!tokenValido) {

            mostrarError(
                "Token inválido",
                "El token no coincide con el empleado."
            );

            return;
        }

        LocalDate hoy = LocalDate.now();

        // ==============================
        // VALIDAR DUPLICADO
        // ==============================

        boolean yaRegistro = empleadoDAO.yaRegistroTurno(
                emp.getId(),
                hoy,
                turno
        );

        if (yaRegistro) {

            mostrarAdvertencia(
                "Turno ya registrado",
                emp.getNombre()
                + " ya registró el turno "
                + turno
                + " hoy."
            );

            return;
        }

        // ==============================
        // CREAR REGISTRO
        // ==============================

        TurnoEmpleados nuevoTurno = new TurnoEmpleados();

        nuevoTurno.setEmpleado(emp);
        nuevoTurno.setFecha(hoy);
        nuevoTurno.setTurno(turno);
        nuevoTurno.setToken(token);

        Double pagoTurno = turno.equalsIgnoreCase("Mañana")
                ? emp.getPagoManiana()
                : emp.getPagoTarde();

        if (pagoTurno == null) {

            mostrarError(
                "Sueldo no configurado",
                emp.getNombre() + " no tiene un sueldo configurado para el turno " + turno + "."
            );

            return;
        }

        nuevoTurno.setMontoHistorial(pagoTurno);

        empleadoDAO.registrarTurno(nuevoTurno);

        // ==============================
        // LOG
        // ==============================

        System.out.println("=========== EMPLEADO DETECTADO ===========");
        System.out.println("EMPLEADO: " + emp.getNombre());
        System.out.println("ID: " + emp.getId());
        System.out.println("TURNO: " + turno);
        System.out.println("TOKEN: " + token);
        System.out.println("=========================================");

        // ==============================
        // ALERTA
        // ==============================

        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        DialogoUtil.aplicarEstilo(alert);

        alert.setTitle("Turno registrado");
        alert.setHeaderText(null);

        alert.setContentText(
                "Empleado: " + emp.getNombre()
                + "\nTurno: " + turno
        );

        alert.showAndWait();

    } catch (Exception e) {

        e.printStackTrace();

        mostrarError(
                "Error",
                "No se pudo registrar el turno del empleado."
        );
    }
}

    public String getMedioPago() {
        return caja().medioPago;
    }

    public void AgregarHuesos() {
        txtCodigoDeBarra.setText("2100480000014");
        SepararCodigo();
    }

    public void AgregarCarbon() {
        txtCodigoDeBarra.setText("2100590000010");
        SepararCodigo();
    }

    public void AgregarHuevos() {
        txtCodigoDeBarra.setText("2100620000010");
        SepararCodigo();
    }

    /**
     * Genera el cobro dinámico sobre el QR estático de la caja visible por
     * el total actual a pagar. Se dispara al elegir "Virtual" (F2). Corre en
     * un hilo aparte para no trabar la UI con la llamada de red. Cada caja
     * cobra con su propio QR (External POS ID configurado para esa caja).
     *
     * Antes de crear la nueva orden, cancela cualquier orden QR pendiente
     * anterior de la misma caja (si existía) para no dejar dos órdenes
     * activas al mismo tiempo sobre el mismo QR estático, que es lo que
     * provocaba el error de "un QR generado sobre otro". También ignora
     * llamadas repetidas mientras ya se está generando una orden, o si ya hay
     * una orden activa por el mismo monto (evita crear una orden nueva en
     * cada repique de la tecla F2 sin que haya cambiado nada que cobrar).
     */
    private void generarCobroQR() {
        EstadoCaja c = caja();

        if (c.ventaYaCobrada) {
            mostrarNotificacion("✓ Esta venta ya fue cobrada. Presione F5 para guardarla.", "toast-exito");
            return;
        }

        // QR personalizado (+): el monto escrito; si no, el total de la venta
        double monto = c.montoQRPersonalizado > 0 ? c.montoQRPersonalizado : calcularTotalFinal(c);

        if (monto <= 0) {
            return;
        }
        if (monto <= MONTO_MINIMO_QR) {
            mostrarNotificacion(String.format("✗ El cobro por QR tiene que ser mayor a $%d", MONTO_MINIMO_QR), "toast-error");
            return;
        }

        String ordenAnterior;
        int miSolicitud;
        synchronized (lockQR) {
            // Mismo monto y ya hay una orden activa (o en camino): nada que hacer
            if ((c.ordenQRActualId != null || c.generandoQR) && monto == c.montoQRActual) {
                return;
            }

            // Si había una generación en curso por otro monto, queda invalidada:
            // al terminar, ese hilo cancela su propia orden.
            miSolicitud = ++c.solicitudQR;
            c.generandoQR = true;
            c.montoQRActual = monto;
            ordenAnterior = c.ordenQRActualId;
            c.ordenQRActualId = null; // invalida el polling de la orden anterior, si había
        }
        actualizarIndicadoresCajas();

        String referencia = "POS-C" + c.numero + "-" + System.currentTimeMillis();

        new Thread(() -> {
            String ordenId = null;
            try {
                if (ordenAnterior != null) {
                    try {
                        mpQRService.cancelarCobroDinamico(ordenAnterior);
                    } catch (Exception e) {
                        System.out.println("No se pudo cancelar la orden QR anterior: " + e.getMessage());
                        verificarPosibleCobroNoDetectado(c, ordenAnterior, monto);
                    }
                }

                ordenId = mpQRService.generarCobroDinamico(monto, "Venta mostrador", referencia, c.numero);

                boolean vigente;
                synchronized (lockQR) {
                    vigente = miSolicitud == c.solicitudQR;
                    if (vigente) {
                        c.ordenQRActualId = ordenId;
                        c.deadlineQRMillis = System.currentTimeMillis() + TIEMPO_ESPERA_QR_MS;
                    }
                }

                if (!vigente) {
                    // Mientras se generaba, la venta se canceló/guardó o se pidió
                    // otro monto: dar de baja esta orden para que el QR físico
                    // no quede cobrando.
                    System.out.println("Orden QR " + ordenId + " ya no corresponde, se cancela.");
                    try {
                        mpQRService.cancelarCobroDinamico(ordenId);
                    } catch (Exception e) {
                        System.out.println("No se pudo cancelar la orden QR descartada: " + e.getMessage());
                        verificarPosibleCobroNoDetectado(c, ordenId, monto);
                    }
                    return;
                }

                System.out.println("=========================================");
                System.out.println("ORDEN QR GENERADA - Caja " + c.numero + " - Order ID: " + ordenId);
                System.out.println("external_reference: " + referencia);
                System.out.println("=========================================");

                Platform.runLater(() -> {
                    actualizarIndicadoresCajas();
                    if (esCajaVisible(c)) {
                        mostrarBannerQRListo(c);
                    }
                });

            } catch (Exception e) {
                e.printStackTrace();

                if (miSolicitud == c.solicitudQR) {
                    Platform.runLater(() -> mostrarNotificacion(
                            "✗ Caja " + c.numero + ": no se pudo generar el cobro QR. " + e.getMessage(),
                            "toast-error"
                    ));
                }
                return;
            } finally {
                synchronized (lockQR) {
                    if (miSolicitud == c.solicitudQR) {
                        c.generandoQR = false;
                    }
                }
                Platform.runLater(this::actualizarIndicadoresCajas);
            }

            esperarConfirmacionPago(c, ordenId, monto);
        }).start();
    }

    /**
     * Invalida cualquier orden QR activa o en proceso de generación de la
     * caja y devuelve el id de la orden activa (o null) para que el llamador
     * la cancele en Mercado Pago. Si había una generación en curso, ese hilo
     * detecta que ya no está vigente y cancela su propia orden.
     */
    private String invalidarOrdenQR(EstadoCaja c) {
        synchronized (lockQR) {
            c.solicitudQR++;
            c.generandoQR = false;
            String ordenId = c.ordenQRActualId;
            c.ordenQRActualId = null;
            c.montoQRActual = 0.0;
            return ordenId;
        }
    }

    /**
     * Si el total cambió (se agregó un producto o se aplicó el descuento de
     * la tarjeta) mientras había un QR Virtual activo, se vuelve a generar
     * por el monto nuevo; si no, el QR físico seguiría cobrando el monto viejo.
     */
    private void regenerarQRSiCambioTotal() {
        EstadoCaja c = caja();
        if (c.ventaYaCobrada) {
            return;
        }
        boolean personalizado = c.montoQRPersonalizado > 0;
        // QR personalizado: no se adivina qué hacer con la diferencia; se cancela
        // y se pide elegir de nuevo (sin resto, el producto nuevo no se cobraría;
        // en mixto, el QR podría quedar mayor que el total)
        if (personalizado && (esQRSinResto(c)
                || (Venta.MIXTO.equals(c.medioPago) && c.montoQRPersonalizado >= totalSinRedondeo(c)))) {
            cancelarOrdenQRPendiente(c);
            setMedioPago("---");
            actualizarTotales();
            mostrarNotificacion("✗ El total cambió: volvé a elegir el pago (F1, F2 o +)", "toast-error");
            return;
        }
        if ("Virtual".equals(c.medioPago) && !personalizado
                && (c.ordenQRActualId != null || c.generandoQR)) {
            generarCobroQR();
        }
    }

    /**
     * Sondea (polling) el estado de la order QR generada hasta que el pago
     * quede acreditado, hasta que la venta en curso de la caja se
     * cancele/cambie de orden, o hasta agotar el tiempo de espera. Corre en
     * el hilo en el que fue llamado (ya es un hilo aparte, iniciado desde
     * generarCobroQR()). Al detectar el pago acreditado, verifica contra
     * Mercado Pago el monto realmente acreditado y muestra el overlay verde
     * de éxito con ese monto, aunque en ese momento se esté mostrando otra
     * caja. Un error transitorio de red en una consulta puntual no aborta
     * el sondeo completo: se reintenta en el siguiente ciclo.
     *
     * Si se agota el tiempo sin que se acredite el pago, la orden se
     * cancela y se limpia sola (banner y estado del QR), en vez de quedar
     * indefinidamente activa y cobrable en el QR físico.
     *
     * El límite se mide contra deadlineQRMillis de la caja (reloj real) en
     * lugar de contar iteraciones fijas: así el tiempo de red de cada
     * consultarOrden() no se va acumulando, y la barra del cartel
     * "QR GENERADO" —que se vacía contra ese mismo deadlineQRMillis— nunca
     * queda desincronizada del momento real en que la orden se cancela.
     */
    private void esperarConfirmacionPago(EstadoCaja c, String ordenId, double montoEsperado) {
        final int intervaloMs = 2000;
        final long deadline = c.deadlineQRMillis;

        while (System.currentTimeMillis() < deadline) {

            long restante = deadline - System.currentTimeMillis();
            try {
                Thread.sleep(Math.max(0, Math.min(intervaloMs, restante)));
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return;
            }

            // La venta en curso fue cancelada o se generó otra orden: dejar de sondear esta.
            if (!ordenId.equals(c.ordenQRActualId)) {
                return;
            }

            try {
                JsonNode orden = mpQRService.consultarOrden(ordenId);
                String estado = orden.path("status").asText();

                if (mpQRService.esPagoAprobado(estado)) {
                    double montoPagado = mpQRService.obtenerMontoPagado(orden);
                    c.ventaYaCobrada = true;
                    c.ordenQRActualId = null;
                    Platform.runLater(() -> {
                        actualizarIndicadoresCajas();
                        mostrarPagoExitoso(c, "QR", montoPagado, montoEsperado);
                    });
                    return;
                }
            } catch (Exception e) {
                System.out.println("Error consultando estado de la orden QR (se reintenta): " + e.getMessage());
            }
        }

        // Se agotó el tiempo sin pago: cancelar la orden y limpiar el QR solo,
        // para que no quede activa cobrando un monto viejo indefinidamente.
        if (ordenId.equals(c.ordenQRActualId)) {
            expirarOrdenQRPorTiempo(c, ordenId, montoEsperado);
        }
    }

    /**
     * Cancela en Mercado Pago la orden QR que expiró sin pago y avisa
     * "QR EXPIRADO CAJA n: $monto" para que el cajero sepa que debe volver a
     * generar el QR (F2) si el cliente todavía quiere pagar de forma
     * virtual. Si la caja está en pantalla se muestra en el cartel rojo; si
     * no, como notificación, para no tapar la caja que se está atendiendo.
     */
    private void expirarOrdenQRPorTiempo(EstadoCaja c, String ordenId, double monto) {
        try {
            mpQRService.cancelarCobroDinamico(ordenId);
        } catch (Exception e) {
            System.out.println("No se pudo cancelar la orden QR expirada " + ordenId + ": " + e.getMessage());
            verificarPosibleCobroNoDetectado(c, ordenId, monto);
        }

        if (ordenId.equals(c.ordenQRActualId)) {
            c.ordenQRActualId = null;
            c.montoQRActual = 0.0;

            String texto = "QR EXPIRADO CAJA " + c.numero + ": $" + String.format("%,.0f", monto);
            Platform.runLater(() -> {
                actualizarIndicadoresCajas();
                if (esCajaVisible(c)) {
                    mostrarBannerQRTemporal(texto);
                } else {
                    mostrarNotificacion("⚠ " + texto, "toast-error");
                }
            });
        }
    }

    /**
     * Muestra en el cartel el estado de QR de la caja visible: el cartel
     * verde con su barra si tiene una orden esperando pago, o nada si no.
     * Se usa al cambiar de caja.
     */
    private void refrescarBannerQR() {
        EstadoCaja c = caja();
        if (c.ordenQRActualId != null && !c.ventaYaCobrada) {
            mostrarBannerQRListo(c);
        } else {
            ocultarBannerQRListoInmediato();
        }
    }

    /**
     * Muestra el cartel persistente "QR GENERADO CAJA n: $monto", en verde,
     * con una barra que se vacía a medida que se acaba el tiempo del QR. A
     * diferencia del toast genérico, no se auto-oculta: queda visible
     * mientras se espera el pago y solo se retira explícitamente (venta
     * concretada, cambio de medio de pago, cancelación o cambio de caja).
     */
    private void mostrarBannerQRListo(EstadoCaja c) {
        detenerTemporizadorQR();
        detenerAnimacionBannerQR();

        String extra = "";
        if (Venta.MIXTO.equals(c.medioPago) && c.montoQRPersonalizado > 0) {
            extra = String.format("  (+ $%,.0f en efectivo)", parteEfectivoMixto(c));
        }
        lblQRListo.setText("QR GENERADO CAJA " + c.numero + ": $" + String.format("%,.0f", c.montoQRActual) + extra);
        barraQR.setVisible(true);
        barraQR.setManaged(true);
        panelQRListo.getStyleClass().remove("qr-rojo");

        panelQRListo.setOpacity(0);
        panelQRListo.setVisible(true);
        panelQRListo.setManaged(true);
        panelQRListo.toFront();

        FadeTransition fadeIn = new FadeTransition(Duration.millis(250), panelQRListo);
        fadeIn.setFromValue(0);
        fadeIn.setToValue(1);
        animacionBannerQR = fadeIn;
        fadeIn.play();

        iniciarTemporizadorQR(c);
    }

    /**
     * Vacía la barra del cartel "QR GENERADO" en forma continua hasta el
     * vencimiento del QR. Es solo visual (el que efectivamente vence y
     * cancela la orden es el polling en esperarConfirmacionPago() /
     * expirarOrdenQRPorTiempo()), pero parte del tiempo que realmente le
     * queda según deadlineQRMillis de la caja, el mismo que usa ese polling,
     * así la barra nunca queda desincronizada de la orden.
     */
    private void iniciarTemporizadorQR(EstadoCaja c) {
        long restanteMs = Math.max(0, c.deadlineQRMillis - System.currentTimeMillis());
        barraQR.setProgress((double) restanteMs / TIEMPO_ESPERA_QR_MS);

        temporizadorQR = new Timeline(new KeyFrame(Duration.millis(restanteMs),
                new KeyValue(barraQR.progressProperty(), 0.0, Interpolator.LINEAR)));
        temporizadorQR.play();
    }

    /** Detiene la barra de tiempo del cartel de QR, si estaba corriendo. */
    private void detenerTemporizadorQR() {
        if (temporizadorQR != null) {
            temporizadorQR.stop();
            temporizadorQR = null;
        }
    }

    /** Detiene la animación en curso del cartel, para que no lo oculte al terminar. */
    private void detenerAnimacionBannerQR() {
        if (animacionBannerQR != null) {
            animacionBannerQR.stop();
            animacionBannerQR = null;
        }
    }

    /** Oculta el cartel de QR listo con un desvanecimiento de 3 segundos. */
    private void ocultarBannerQRListo() {
        detenerTemporizadorQR();
        detenerAnimacionBannerQR();

        if (!panelQRListo.isVisible()) {
            return;
        }

        FadeTransition fadeOut = new FadeTransition(Duration.seconds(3), panelQRListo);
        fadeOut.setFromValue(panelQRListo.getOpacity());
        fadeOut.setToValue(0);
        fadeOut.setOnFinished(e -> {
            panelQRListo.setVisible(false);
            panelQRListo.setManaged(false);
        });
        animacionBannerQR = fadeOut;
        fadeOut.play();
    }

    /** Oculta el cartel de QR listo sin animación (venta cancelada, cambio de medio de pago o de caja). */
    private void ocultarBannerQRListoInmediato() {
        detenerTemporizadorQR();
        detenerAnimacionBannerQR();
        panelQRListo.setVisible(false);
        panelQRListo.setManaged(false);
        panelQRListo.setOpacity(0);
        panelQRListo.getStyleClass().remove("qr-rojo");
    }

    /**
     * Muestra el cartel de QR en rojo con el texto indicado (QR expirado o
     * cancelado, con su monto) durante 2 segundos y luego se desvanece solo.
     * Se usa para los casos en los que la orden QR se dio de baja sin que
     * el cliente llegara a pagar: expiración por tiempo (F2) o cancelación
     * manual de la venta (F12/ESC).
     */
    private void mostrarBannerQRTemporal(String texto) {
        detenerTemporizadorQR();
        detenerAnimacionBannerQR();
        lblQRListo.setText(texto);
        barraQR.setVisible(false);
        barraQR.setManaged(false);
        if (!panelQRListo.getStyleClass().contains("qr-rojo")) {
            panelQRListo.getStyleClass().add("qr-rojo");
        }

        panelQRListo.setOpacity(0);
        panelQRListo.setVisible(true);
        panelQRListo.setManaged(true);
        panelQRListo.toFront();

        FadeTransition fadeIn = new FadeTransition(Duration.millis(250), panelQRListo);
        fadeIn.setFromValue(0);
        fadeIn.setToValue(1);

        PauseTransition espera = new PauseTransition(Duration.seconds(2));

        FadeTransition fadeOut = new FadeTransition(Duration.millis(300), panelQRListo);
        fadeOut.setFromValue(1);
        fadeOut.setToValue(0);
        fadeOut.setOnFinished(e -> {
            panelQRListo.setVisible(false);
            panelQRListo.setManaged(false);
            panelQRListo.getStyleClass().remove("qr-rojo");
        });

        SequentialTransition secuencia = new SequentialTransition(fadeIn, espera, fadeOut);
        animacionBannerQR = secuencia;
        secuencia.play();
    }

    /**
     * Muestra el overlay verde grande de pago recibido centrado en la
     * pantalla, durante unos 5 segundos, con fundido de entrada y salida.
     * Indica la caja que cobró, porque puede llegar mientras se atiende
     * otra. Incluye el monto realmente acreditado según Mercado Pago
     * (consultado con el token configurado), no el monto que se pidió
     * cobrar. Si no coincide exacto (dentro de $1 por redondeo) se resalta
     * en amarillo para que se verifique manualmente.
     */
    private void mostrarPagoExitoso(EstadoCaja c, String titulo, double montoPagado, double montoEsperado) {
        // Pagó otra caja: aviso fijo en la esquina para no tapar la venta en curso
        if (!esCajaVisible(c)) {
            mostrarAvisoPagoOtraCaja(c, montoPagado, montoEsperado);
            return;
        }

        ocultarBannerQRListo();

        boolean coincide = Math.abs(montoPagado - montoEsperado) < 1.0;

        lblTituloPagoExitoso.setText(titulo + " · CAJA " + c.numero);
        lblMontoRecibido.setText("Pago recibido: $" + String.format("%,.0f", montoPagado));
        lblMontoRecibido.setStyle(coincide ? null : "-fx-text-fill: #f1c40f;");
        if (coincide) {
            lblSubtextoPagoExitoso.setText("Operación exitosa");
        } else {
            lblSubtextoPagoExitoso.setText("Verificar: se esperaban $" + String.format("%,.0f", montoEsperado));
        }

        panelPagoExitoso.setOpacity(0);
        panelPagoExitoso.setVisible(true);
        panelPagoExitoso.setManaged(true);
        panelPagoExitoso.toFront();

        FadeTransition fadeIn = new FadeTransition(Duration.millis(300), panelPagoExitoso);
        fadeIn.setFromValue(0);
        fadeIn.setToValue(1);

        PauseTransition espera = new PauseTransition(Duration.seconds(5));

        FadeTransition fadeOut = new FadeTransition(Duration.millis(400), panelPagoExitoso);
        fadeOut.setFromValue(1);
        fadeOut.setToValue(0);
        fadeOut.setOnFinished(e -> {
            panelPagoExitoso.setVisible(false);
            panelPagoExitoso.setManaged(false);
            txtCodigoDeBarra.requestFocus();
        });

        new SequentialTransition(fadeIn, espera, fadeOut).play();
    }

    /**
     * Avisa que una caja que no está en pantalla cobró su QR: su recuadro
     * en la barra del código pasa a "Caja n ✓ $monto" y el botón de la caja
     * titila en verde. No se ocultan solos: quedan hasta que se entra a esa
     * caja (con su tecla, su botón o haciendo clic en el recuadro), para que
     * el pago no pase desapercibido mientras se atiende a otro cliente.
     */
    private void mostrarAvisoPagoOtraCaja(EstadoCaja c, double montoPagado, double montoEsperado) {
        quitarAvisoPago(c);

        c.pagoSinVer = true;
        c.montoPagadoSinVer = montoPagado;
        c.montoEsperadoSinVer = montoEsperado;
        actualizarRecuadroCaja(c);

        // Botón de la caja titilando en verde hasta que se entre
        ToggleButton boton = botonesCaja[c.numero - 1];
        c.parpadeoBoton = new Timeline(new KeyFrame(Duration.millis(500), e -> {
            if (boton.getStyleClass().contains("caja-pagada")) {
                boton.getStyleClass().remove("caja-pagada");
            } else {
                boton.getStyleClass().add("caja-pagada");
            }
        }));
        c.parpadeoBoton.setCycleCount(Timeline.INDEFINITE);
        c.parpadeoBoton.play();
    }

    /** Quita el aviso de pago y el parpadeo del botón de la caja, si los tenía. */
    private void quitarAvisoPago(EstadoCaja c) {
        c.pagoSinVer = false;
        actualizarRecuadroCaja(c);

        if (c.parpadeoBoton != null) {
            c.parpadeoBoton.stop();
            c.parpadeoBoton = null;
        }
        botonesCaja[c.numero - 1].getStyleClass().remove("caja-pagada");
    }

    /**
     * Cancela la order QR pendiente de la caja (solo válido mientras su
     * status sea "created", es decir, todavía no fue pagada) y, si la caja
     * está en pantalla, muestra en rojo "QR CANCELADO CAJA n: $monto"
     * durante 2 segundos. Se usa al cambiar a Efectivo (F1) mientras había
     * una orden QR activa, al abortar la venta con F12/ESC y al salir de la
     * pantalla — nunca después de un cobro ya confirmado con F5, porque
     * Mercado Pago rechaza la cancelación de una order que ya fue pagada.
     */
    private void cancelarOrdenQRPendiente(EstadoCaja c) {
        double montoCancelado = c.montoQRActual;
        String ordenId = invalidarOrdenQR(c);
        actualizarIndicadoresCajas();
        if (ordenId == null) {
            return;
        }

        new Thread(() -> {
            try {
                mpQRService.cancelarCobroDinamico(ordenId);
            } catch (Exception e) {
                System.out.println("No se pudo cancelar la orden QR " + ordenId + ": " + e.getMessage());
                verificarPosibleCobroNoDetectado(c, ordenId, montoCancelado);
            }
        }).start();

        if (esCajaVisible(c)) {
            mostrarBannerQRTemporal("QR CANCELADO CAJA " + c.numero + ": $" + String.format("%,.0f", montoCancelado));
        }
    }

    /**
     * Se llama cuando falla la cancelación de una orden QR (F12/ESC/F1/
     * vencimiento/cierre de pantalla). La causa más probable es que el pago
     * se acreditó justo antes de cancelar (Mercado Pago no deja cancelar una
     * order ya pagada). En vez de asumir en silencio que quedó cancelada,
     * se re-consulta el estado real: si de hecho se cobró, se le avisa al
     * cajero con una alerta bloqueante para que verifique y registre la
     * venta a mano si corresponde.
     */
    private void verificarPosibleCobroNoDetectado(EstadoCaja c, String ordenId, double monto) {
        try {
            JsonNode orden = mpQRService.consultarOrden(ordenId);
            String estado = orden.path("status").asText();

            if (mpQRService.esPagoAprobado(estado)) {
                double montoPagado = mpQRService.obtenerMontoPagado(orden);
                Platform.runLater(() -> mostrarError(
                        "¡Atención! Posible cobro no registrado (Caja " + c.numero + ")",
                        "La orden QR de la Caja " + c.numero + " por $" + String.format("%,.0f", monto)
                        + " no se pudo cancelar porque Mercado Pago indica que SÍ se cobró "
                        + "(monto acreditado: $" + String.format("%,.0f", montoPagado) + ").\n\n"
                        + "Verificar en Mercado Pago y, si corresponde, registrar la venta manualmente."
                ));
                return;
            }
        } catch (Exception ex) {
            System.out.println("No se pudo verificar el estado real de la orden " + ordenId + ": " + ex.getMessage());
        }

        Platform.runLater(() -> mostrarNotificacion(
                "⚠ Caja " + c.numero + ": no se pudo confirmar en Mercado Pago la cancelación del QR de $"
                + String.format("%,.0f", monto) + ". Verificar manualmente.",
                "toast-error"
        ));
    }

    private void cancelarVentaEnCurso() {
        cancelarOrdenQRPendiente(caja());
        limpiarTodo();
    }

    /**
     * Se invoca desde App al cerrar la ventana (X). Cancela de forma
     * bloqueante (sin hilo aparte, ya que la app se está cerrando) cualquier
     * orden QR pendiente de las tres cajas, para no dejar un QR físico
     * cobrando un monto después de cerrar la aplicación.
     */
    public static void cancelarOrdenQRPendienteAlCerrarApp() {
        if (instanciaActual != null) {
            instanciaActual.cancelarOrdenesQRPendientesBloqueante();
        }
    }

    private void cancelarOrdenesQRPendientesBloqueante() {
        for (EstadoCaja c : cajas) {
            String ordenId = invalidarOrdenQR(c);
            if (ordenId == null) {
                continue;
            }

            try {
                mpQRService.cancelarCobroDinamico(ordenId);
            } catch (Exception e) {
                System.out.println("No se pudo cancelar la orden QR " + ordenId
                        + " de la Caja " + c.numero + " al cerrar la app: " + e.getMessage());
            }
        }
    }

    @FXML
    public void MostrarTransferencias() {
        MP = new MercadoPagoApi();
        MP.obtenerUltimosPagos();
        datos = MP.getListaDatos();

        // Mostrar el primer pago (más reciente) por defecto
        indicePagoActual = 0;
        actualizarPago();
        // Iniciar el temporizador que limpia la información después de 10 segundos
        iniciarTemporizadorLimpieza();
    }

    private void limpiarDatosMP() {
        // Limpiar los labels
        lblMonto.setText("--");
        lblHora.setText("--");
        lblEstado.setText("--");

        // Volver al color neutro del recuadro (definido en crear_ventas.css)
        vBoxAprobado.getStyleClass().removeAll("mp-aprobado", "mp-rechazado");
    }

    private void actualizarPago() {
        if (datos == null || datos.isEmpty() || indicePagoActual < 0 || indicePagoActual >= datos.size()) {
            return; // Evita errores si la lista está vacía o el índice es inválido
        }

        DatosPagos pago = datos.get(indicePagoActual);
        lblMonto.setText(String.valueOf(pago.getMonto()));
        lblHora.setText(pago.getFecha());

        vBoxAprobado.getStyleClass().removeAll("mp-aprobado", "mp-rechazado");
        if (pago.getEstado().equals("approved")) {
            lblEstado.setText("Si");
            vBoxAprobado.getStyleClass().add("mp-aprobado");
        } else {
            lblEstado.setText("No");
            vBoxAprobado.getStyleClass().add("mp-rechazado");
        }
    }

    @FXML
    public void pagoSiguiente() {
        if (indicePagoActual > 0) {
            indicePagoActual--;
            actualizarPago();
            reiniciarTemporizadorLimpieza();
        }
    }

    @FXML
    public void pagoAnterior() {
        if (datos != null && indicePagoActual < datos.size() - 1) {
            indicePagoActual++;
            actualizarPago();
            reiniciarTemporizadorLimpieza();
        }
    }
// Nuevo método para reiniciar el temporizador

    private void reiniciarTemporizadorLimpieza() {
        // Detener el temporizador anterior si existe
        if (temporizadorActual != null) {
            temporizadorActual.stop();
        }

        // Crear nuevo temporizador
        temporizadorActual = new Timeline(new KeyFrame(Duration.seconds(10), e -> limpiarDatosMP()));
        temporizadorActual.setCycleCount(1);
        temporizadorActual.play();
    }

// Método corregido iniciarTemporizadorLimpieza
    private void iniciarTemporizadorLimpieza() {
        reiniciarTemporizadorLimpieza(); // Usar el nuevo método
    }

    @FXML
    private void cerrarVentas(ActionEvent event) {
        // Verificar si alguna caja tiene una venta en progreso (incluye un QR
        // virtual pendiente, aunque el carrito ya esté vacío)
        StringBuilder cajasEnCurso = new StringBuilder();
        for (EstadoCaja c : cajas) {
            if (c.tieneVentaEnCurso()) {
                cajasEnCurso.append(cajasEnCurso.length() > 0 ? ", " : "").append("Caja ").append(c.numero);
            }
        }

        if (cajasEnCurso.length() > 0) {
            Alert confirmAlert = new Alert(Alert.AlertType.CONFIRMATION);
            DialogoUtil.aplicarEstilo(confirmAlert);
            confirmAlert.setTitle("Confirmar salida");
            confirmAlert.setHeaderText("Hay ventas en progreso: " + cajasEnCurso);
            confirmAlert.setContentText("¿Está seguro que desea salir? Se perderán los datos no guardados.");

            if (confirmAlert.showAndWait().orElse(ButtonType.CANCEL) != ButtonType.OK) {
                return;
            }
        }

        // Si quedaba alguna orden QR pendiente, cancelarla antes de irse de la
        // pantalla: de lo contrario el QR físico sigue cobrando ese monto
        // aunque el cajero ya haya salido de Ventas.
        for (EstadoCaja c : cajas) {
            cancelarOrdenQRPendiente(c);
        }

        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/menu.fxml"));
            Parent root = loader.load();

            // Obtener el stage desde el botón que dispara el evento
            Scene escenaActual = ((Node) event.getSource()).getScene();
            Stage stage = (Stage) escenaActual.getWindow();

            // La escena nueva toma el tamaño real de la ventana (y no el
            // prefWidth/prefHeight de menu.fxml, pensado para 1920x1080), así
            // el menú se adapta a cualquier monitor y no queda tapado por la
            // barra de tareas.
            stage.setScene(new Scene(root, escenaActual.getWidth(), escenaActual.getHeight()));
            stage.setMaximized(true);
            stage.show();

        } catch (IOException e) {
            e.printStackTrace();
            mostrarError("Error", "No se pudo cargar el menú principal.");
        }
    }

    //Metodo para separar el codigo de barra
    @FXML
    public void SepararCodigo() {
        String codigo = txtCodigoDeBarra.getText().trim();

        // Validar que el código no esté vacío
        if (codigo.isEmpty()) {
            mostrarAdvertencia("Campo vacío", "Por favor ingrese un código de barras.");
            return;
        }

        if (codigo.length() != 13) {
            // Error si el código no tiene 13 dígitos
            mostrarError("Código inválido",
                    "El código ingresado no es válido. Debe tener exactamente 13 dígitos.\n"
                    + "Código ingresado: " + codigo + " (longitud: " + codigo.length() + ")");
            limpiarCampoCodigoYEnfocar();
        } else {
            try {
                // Validar que todos los caracteres sean números
                if (!codigo.matches("\\d{13}")) {
                    mostrarError("Código inválido", "El código debe contener solo números.");
                    limpiarCampoCodigoYEnfocar();
                    return;
                }

                // Detectar código de empleado
                if (codigo.startsWith("99")) {

                    procesarCodigoEmpleado(codigo);

                    limpiarCampoCodigoYEnfocar();

                    return;
                }

                // Detectar tarjeta de fidelidad (EAN-13). Ya no se puede
                // distinguir por longitud como antes (código de 5 dígitos),
                // porque ahora las tarjetas también tienen 13 dígitos igual
                // que los productos. Se consulta la base: si existe una
                // tarjeta con ese código exacto, se procesa como tarjeta;
                // si no existe ninguna, se interpreta como producto.
                Tarjeta tarjetaEscaneada = tarjetaDAO.buscarPorCodigoBarra(codigo);

                if (tarjetaEscaneada != null) {
                    procesarCodigoTarjeta(codigo);
                    limpiarCampoCodigoYEnfocar();
                    return;
                }

                // Extraer partes del código (producto)
                String tipoProducto = codigo.substring(0, 2);  // Primeros 2 dígitos
                String codigoProducto = codigo.substring(2, 6); // 4 dígitos 
                String pesoImporte = codigo.substring(6, 12);   // 6 dígitos para peso/importe
                String digitoControl = codigo.substring(12);    // Último dígito

                int codigoInt = Integer.parseInt(codigoProducto);
                Double pesoImporteFloat = Double.valueOf(pesoImporte);

                // Convertir peso a formato decimal
                Double peso = pesoImporteFloat / 1000.0; // Dividir para obtener kg

                System.out.println("Tipo de producto: " + tipoProducto);
                System.out.println("Código del producto: " + codigoInt);
                System.out.println("Peso: " + peso + " kg");
                System.out.println("Dígito de control: " + digitoControl);

                // Llamar a BuscarProducto
                BuscarProducto(codigoInt, tipoProducto, peso);

            } catch (NumberFormatException e) {
                System.out.println("Error: El código contiene caracteres no numéricos.");
                mostrarError("Error de formato", "El código contiene caracteres inválidos.");
                limpiarCampoCodigoYEnfocar();
            } catch (Exception e) {
                System.out.println("Error inesperado al procesar código: " + e.getMessage());
                mostrarError("Error", "Ocurrió un error al procesar el código de barras.");
                limpiarCampoCodigoYEnfocar();
            }
        }
    }

    public void BuscarProducto(int codigo, String tipoProducto, Double peso) {
        System.out.println("Buscando producto con código: " + codigo);

        try {
            Producto ProductoEncontrado = productoDAO.buscarPorCodigo(codigo);

            if (ProductoEncontrado == null) {
                System.out.println("No se encontró el producto con código: " + codigo);
                mostrarAdvertencia("Producto no encontrado",
                        "No se encontró ningún producto con el código: " + codigo);
                limpiarCampoCodigoYEnfocar();
                return;
            }

            System.out.println("Producto encontrado: " + ProductoEncontrado.getNombre());

            // Establecer el peso
            double pesoFinal;
            if (tipoProducto.equals("21")) {
                pesoFinal = 1.00;
            } else {
                if (peso <= 0) {
                    mostrarAdvertencia("Peso inválido", "El peso del producto debe ser mayor a 0.");
                    limpiarCampoCodigoYEnfocar();
                    return;
                }
                pesoFinal = peso;
            }

            // Agregar el producto a la tabla
            agregarProductoATabla(ProductoEncontrado, pesoFinal);

        } catch (Exception e) {
            System.out.println("Error al buscar producto: " + e.getMessage());
            e.printStackTrace();
            mostrarError("Error de base de datos", "No se pudo consultar el producto en la base de datos.");
            limpiarCampoCodigoYEnfocar();
        }
    }

    // Método para agregar producto a la tabla
    private void agregarProductoATabla(Producto producto, double peso) {
        try {
            EstadoCaja c = caja();
            c.productos.add(new ItemCarrito(producto, peso));

            // Sumar el total real del producto (con su descuento)
            c.sumaPrecios += Math.round(precioConDescuento(producto) * peso);

            actualizarTotales();
            regenerarQRSiCambioTotal();

            limpiarCampoCodigoYEnfocar();


        } catch (Exception e) {
            System.out.println("Error al agregar producto a la tabla: " + e.getMessage());
            mostrarError("Error", "No se pudo agregar el producto a la venta.");
        }
    }


    // Método para limpiar la tabla de la caja visible (útil para nuevas ventas)
    public void limpiarTabla() {
        EstadoCaja c = caja();
        c.productos.clear();
        c.sumaPrecios = 0.0;
        lblCantidadPagar.setText("---");
        lblBadgeDescuento.setVisible(false);
        lblBadgeDescuento.setManaged(false);
        System.out.println("Tabla de productos de la Caja " + c.numero + " limpiada.");
    }

    // Método para obtener la lista de productos de la caja visible (lo uso para procesar la venta)
    public ObservableList<ItemCarrito> getProductosEnVenta() {
        return caja().productos;
    }

    // Método para validar que la venta de la caja visible esté lista para procesar
    private boolean validarVentaParaProcesar() {
        EstadoCaja c = caja();

        if (c.productos.isEmpty()) {
            mostrarAdvertencia("Venta vacía", "Debe agregar al menos un producto para generar la venta.");
            return false;
        }

        if (c.sumaPrecios <= 0) {
            mostrarAdvertencia("Total inválido", "El total de la venta debe ser mayor a $0.00");
            return false;
        }

        if ("---".equals(c.medioPago)) {
            mostrarAdvertencia("Medio de pago",
                    "Debe seleccionar un medio de pago.\n"
                    + "Presione F1 para Efectivo o F2 para Virtual.");
            return false;
        }

        return true;
    }

  public void guardarVenta() {
    if (!validarVentaParaProcesar()) {
        return;
    }
    try {
        EstadoCaja c = caja();
        double totalRedondeado = calcularTotalFinal(c);

        venta = new Venta();
        venta.setFecha(LocalDateTime.now());
        venta.setMedioPago(c.medioPago);
        // Con QR personalizado sin resto, totalRedondeado ya es el monto del QR
        venta.setTotal(totalRedondeado);
        if (Venta.MIXTO.equals(c.medioPago)) {
            // Pago mixto: se guarda la parte en efectivo; el resto fue por QR
            venta.setMontoEfectivo(parteEfectivoMixto(c));
        }

        GenerarDetalleVenta();
        limpiarTodo();
        // Terminada la venta en caja 2 o 3, se vuelve a la caja 1
        if (cajaActual != 1) {
            seleccionarCaja(1);
        }

    } catch (Exception e) {
        e.printStackTrace();
        mostrarError("Error al procesar venta",
                "No se pudo completar el procesamiento de la venta:\n\n" + e.getMessage()
                + "\n\nVerifique:\n"
                + "• Conexión a la base de datos\n"
                + "• Configuración de entidades (Venta, DetalleVenta, Producto)\n"
                + "• Relaciones en Hibernate/JPA\n"
                + "• Que todos los productos tengan ID válido\n"
                + "• Transacciones de base de datos");
    }
}
    // Categoría cuyo stock se trackea y se descuenta en cada venta
    private static final String CATEGORIA_CON_STOCK = "Seco";

    // Método extraído del controlador de referencia para generar detalles
    public void GenerarDetalleVenta() {
        List<ItemCarrito> productos = caja().productos;
        List<DetalleVenta> detalles = new java.util.ArrayList<>();
        for (ItemCarrito item : productos) {
            Producto producto = item.getProducto();

            DetalleVenta nuevoDetalle = new DetalleVenta();
            nuevoDetalle.setVenta(venta);
            nuevoDetalle.setProducto(producto);
            nuevoDetalle.setPeso(item.getPeso());
            // Precio realmente cobrado (con el descuento del producto), para que
            // el historial y el cálculo de ganancias coincidan con lo cobrado
            nuevoDetalle.setPrecio(precioConDescuento(producto));
            detalles.add(nuevoDetalle);
        }

        // Venta + detalles en una sola transacción (si falla, lanza excepción
        // y el carrito NO se limpia, así se puede reintentar con F5)
        ventaDao.guardarConDetalles(venta, detalles);

        // Descontar stock solo para productos "Seco" (si no tiene stock asignado
        // o el stock ya está en 0, no pasa nada: la venta se registra igual)
        for (ItemCarrito item : productos) {
            if (CATEGORIA_CON_STOCK.equals(item.getProducto().getTipo())) {
                stockDao.descontarStockPorProducto(item.getProducto(), item.getPeso());
            }
        }
    }

    private static double precioConDescuento(Producto producto) {
        double precioUnitario = producto.getPrecio();
        Double descuento = producto.getDescuento();
        if (descuento != null && descuento > 0) {
            return precioUnitario - (precioUnitario * descuento / 100);
        }
        return precioUnitario;
    }

    /** Deja la caja visible lista para una venta nueva (las otras cajas no se tocan). */
    public void limpiarTodo() {
        try {
            EstadoCaja c = caja();

            // Red de seguridad: si por el motivo que sea queda una orden QR sin
            // resolver al limpiar la venta (por ejemplo, se guardó/abortó la
            // venta con una orden Virtual todavía pendiente), se cancela acá
            // para que el QR físico no siga cobrando un monto de una venta que
            // el sistema ya dio por cerrada.
            String ordenHuerfana = invalidarOrdenQR(c);
            if (ordenHuerfana != null) {
                new Thread(() -> {
                    try {
                        mpQRService.cancelarCobroDinamico(ordenHuerfana);
                    } catch (Exception e) {
                        System.out.println("No se pudo cancelar la orden QR huérfana "
                                + ordenHuerfana + ": " + e.getMessage());
                    }
                }).start();
            }

            c.tarjetasSelladas.clear();
            c.ventaYaCobrada = false;
            ocultarBannerQRListoInmediato();
            limpiarTabla();
            setMedioPago("---");
            txtCodigoDeBarra.clear();
            venta = null;
            c.descuentoFidelidadActivo = false;
            c.descuentoFidelidad = 0.0;
            c.tope = 0.0;
            actualizarIndicadoresCajas();

            tbolVueltos.getItems().clear();
            tbolVueltos.setVisible(false);       
            tbolVueltos.setManaged(false);       
            tbolVueltos.setPlaceholder(new Label(""));
            tblVistaProductos.setPlaceholder(new Label(""));

            Platform.runLater(() -> txtCodigoDeBarra.requestFocus());

        } catch (Exception e) {
            System.out.println("Error al limpiar sistema: " + e.getMessage());
        }
    }

    // Métodos de utilidad para mostrar alertas
    private void mostrarError(String titulo, String mensaje) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        DialogoUtil.aplicarEstilo(alert);
        alert.setTitle(titulo);
        alert.setHeaderText(null);
        alert.setContentText(mensaje);
        alert.showAndWait();
    }

    private void mostrarAdvertencia(String titulo, String mensaje) {
        Alert alert = new Alert(Alert.AlertType.WARNING);
        DialogoUtil.aplicarEstilo(alert);
        alert.setTitle(titulo);
        alert.setHeaderText(null);
        alert.setContentText(mensaje);
        alert.showAndWait();
    }

    private void mostrarInformacion(String titulo, String mensaje) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        DialogoUtil.aplicarEstilo(alert);
        alert.setTitle(titulo);
        alert.setHeaderText(null);
        alert.setContentText(mensaje);
        alert.showAndWait();
    }

    private void limpiarCampoCodigoYEnfocar() {
        txtCodigoDeBarra.clear();
        Platform.runLater(() -> txtCodigoDeBarra.requestFocus());
    }

    // Getter para el total de la caja visible (útil para otros controladores)
    public double getTotalVenta() {
        return caja().sumaPrecios;
    }

    // Método para obtener información de la venta actual de la caja visible
    public String getResumenVenta() {
        EstadoCaja c = caja();

        if (c.productos.isEmpty()) {
            return "No hay productos en la venta actual";
        }

        StringBuilder resumen = new StringBuilder();
        resumen.append("=== RESUMEN DE VENTA - CAJA ").append(c.numero).append(" ===\n");
        resumen.append("Productos: ").append(c.productos.size()).append("\n");
        resumen.append("Total: $").append(String.format("%.2f", c.sumaPrecios)).append("\n");
        resumen.append("Medio de pago: ").append(c.medioPago).append("\n");
        resumen.append("\nDetalle:\n");

        for (ItemCarrito item : c.productos) {
            resumen.append("- ").append(item.getProducto().getNombre())
                    .append(" x $").append(item.getProducto().getPrecio())
                    .append("\n");
        }

        return resumen.toString();
    }

    /**
     * Representa una línea del carrito de la venta en curso: un producto
     * junto con el peso/cantidad elegido para esa línea. El peso es propio
     * de la venta (no del producto), por eso no vive en la entidad Producto.
     */
    public static class ItemCarrito {

        private final Producto producto;
        private final double peso;

        public ItemCarrito(Producto producto, double peso) {
            this.producto = producto;
            this.peso = peso;
        }

        public Producto getProducto() {
            return producto;
        }

        public double getPeso() {
            return peso;
        }
    }

    /**
     * Venta en curso de una caja (1, 2 o 3). Cada caja tiene la suya y las
     * tres conviven: al cambiar de caja solo cambia cuál se muestra.
     *
     * Los datos del cobro QR (orden, monto, vencimiento, cobrada) se leen y
     * escriben también desde los hilos que generan la orden y sondean el
     * pago, por eso son volatile y se modifican bajo lockQR.
     */
    private static class EstadoCaja {

        final int numero;

        final ObservableList<ItemCarrito> productos = FXCollections.observableArrayList();
        double sumaPrecios = 0.0;
        String medioPago = "---";
        // QR personalizado (+): monto escrito a mano; 0 = el QR va por el total
        double montoQRPersonalizado = 0.0;
        // true: el resto se cobra en efectivo (venta Mixto); false: el resto no se cobra
        boolean restoEnEfectivo = true;

        // Descuento de tarjeta de fidelidad ganado en esta venta
        boolean descuentoFidelidadActivo = false;
        double descuentoFidelidad = 0.0;
        double tope = 0.0;
        // Tarjetas ya selladas en esta venta (1 sello por venta)
        final Set<String> tarjetasSelladas = new HashSet<>();

        // Cobro QR con el QR propio de la caja
        volatile String ordenQRActualId;
        volatile boolean generandoQR = false;
        volatile double montoQRActual = 0.0;
        // Se incrementa cada vez que se pide un QR nuevo o se cancela el actual;
        // un hilo que termina de generar una orden con un número viejo la descarta.
        volatile int solicitudQR = 0;
        volatile long deadlineQRMillis = 0L;
        // true cuando el pago QR de la venta en curso ya se confirmó
        volatile boolean ventaYaCobrada = false;

        // Aviso de la caja cuando no está en pantalla (solo hilo de JavaFX):
        // pago QR que todavía no se vio, parpadeo del botón y recuadro de la
        // barra del código ("QR Caja n" con su barra o "Caja n ✓ $monto")
        boolean pagoSinVer = false;
        double montoPagadoSinVer = 0.0;
        double montoEsperadoSinVer = 0.0;
        Timeline parpadeoBoton;
        String estadoRecuadro = "";
        Timeline animacionBarraRecuadro;
        Tooltip tooltipRecuadro;

        EstadoCaja(int numero) {
            this.numero = numero;
        }

        boolean tieneVentaEnCurso() {
            return (!productos.isEmpty() && sumaPrecios > 0) || ordenQRActualId != null;
        }
    }
}