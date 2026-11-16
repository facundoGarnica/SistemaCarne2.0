package controller;

import Util.CopiaPrecios;
import Util.CopiaPrecios.Importacion;
import Util.DialogoUtil;
import Util.PreciosExcel;
import Util.PreciosExcel.FilaPrecio;
import dao.ProductoDAO;
import java.awt.Desktop;
import java.io.File;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.format.DateTimeFormatter;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.ResourceBundle;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Label;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.layout.VBox;
import javafx.stage.FileChooser;
import model.Producto;

/**
 * Productos > Precios (Excel): exporta la lista de precios y la importa con
 * vista previa. Al importar solo cambia el precio (por código); antes de
 * aplicar pregunta si se sobrescriben los precios viejos por los nuevos.
 */
public class PreciosExcelController implements Initializable {

    /** Una fila de la vista previa: lo que dice el Excel contra lo que hay en el sistema. */
    public record Cambio(FilaPrecio fila, Producto actual, Estado estado) {
    }

    public enum Estado {
        CAMBIA("Cambia el precio"),
        IGUAL("Sin cambios"),
        NUEVO("No está en el sistema"),
        ERROR("Error");

        final String texto;

        Estado(String texto) {
            this.texto = texto;
        }
    }

    @FXML private Label lblExportado;
    @FXML private Label lblArchivo;
    @FXML private VBox boxVistaPrevia;
    @FXML private Label lblCambian, lblIguales, lblNuevos, lblErrores;
    @FXML private TableView<Cambio> tblVistaPrevia;
    @FXML private TableColumn<Cambio, String> colCodigo, colProducto, colPrecioActual, colPrecioNuevo, colEstado;
    @FXML private CheckBox chkAgregarNuevos;
    @FXML private Button btnSobrescribir;
    @FXML private Label lblUltimaImportacion;
    @FXML private Button btnDeshacer;

    private static final DateTimeFormatter FECHA_HORA = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");
    private final ProductoDAO productoDAO = new ProductoDAO();
    private final DecimalFormat moneda =
            new DecimalFormat("$#,##0.##", DecimalFormatSymbols.getInstance(Locale.forLanguageTag("es-AR")));
    private List<Cambio> cambios = new ArrayList<>();

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        colCodigo.setCellValueFactory(c -> new SimpleStringProperty(
                c.getValue().fila().codigo() != null ? String.valueOf(c.getValue().fila().codigo()) : "—"));
        colProducto.setCellValueFactory(c -> new SimpleStringProperty(
                c.getValue().actual() != null ? c.getValue().actual().getNombre() : c.getValue().fila().producto()));
        colPrecioActual.setCellValueFactory(c -> new SimpleStringProperty(
                c.getValue().actual() != null && c.getValue().actual().getPrecio() != null
                        ? moneda.format(c.getValue().actual().getPrecio()) : "—"));
        colPrecioNuevo.setCellValueFactory(c -> new SimpleStringProperty(
                c.getValue().fila().precio() != null ? moneda.format(c.getValue().fila().precio()) : "—"));
        colEstado.setCellValueFactory(c -> new SimpleStringProperty(
                c.getValue().estado() == Estado.ERROR
                        ? "Fila " + c.getValue().fila().filaExcel() + ": " + c.getValue().fila().error()
                        : c.getValue().estado().texto));
        // Los cambios y los errores resaltados
        colEstado.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty ? null : item);
                getStyleClass().removeAll("ps-texto-rojo");
                setStyle(null);
                if (!empty && getTableRow() != null && getTableRow().getItem() != null) {
                    Estado e = getTableRow().getItem().estado();
                    if (e == Estado.ERROR) {
                        getStyleClass().add("ps-texto-rojo");
                    } else if (e == Estado.CAMBIA) {
                        setStyle("-fx-font-weight: bold;");
                    }
                }
            }
        });
        chkAgregarNuevos.selectedProperty().addListener((obs, a, b) -> actualizarBoton());
        mostrarUltimaImportacion();
    }

    // ===================== EXPORTAR =====================

    @FXML
    private void exportar() {
        List<Producto> productos = productoDAO.buscarTodos();
        if (productos == null || productos.isEmpty()) {
            aviso(Alert.AlertType.WARNING, "Sin productos", "No hay productos para exportar.");
            return;
        }
        productos.sort((a, b) -> Integer.compare(a.getCodigo(), b.getCodigo()));

        FileChooser fc = new FileChooser();
        fc.setTitle("Guardar lista de precios");
        fc.getExtensionFilters().add(new FileChooser.ExtensionFilter("Excel (*.xlsx)", "*.xlsx"));
        fc.setInitialFileName("Precios_" + LocalDate.now() + ".xlsx");
        File archivo = fc.showSaveDialog(lblExportado.getScene().getWindow());
        if (archivo == null) {
            return;
        }
        try {
            PreciosExcel.exportar(productos, archivo);
            lblExportado.setText("✓ " + productos.size() + " productos exportados a " + archivo.getName());
        } catch (Exception e) {
            e.printStackTrace();
            aviso(Alert.AlertType.ERROR, "No se pudo exportar", e.getMessage());
        }
    }

    // ===================== IMPORTAR =====================

    @FXML
    private void elegirArchivo() {
        FileChooser fc = new FileChooser();
        fc.setTitle("Elegir Excel con los precios");
        fc.getExtensionFilters().add(new FileChooser.ExtensionFilter("Excel (*.xlsx, *.xls)", "*.xlsx", "*.xls"));
        File archivo = fc.showOpenDialog(lblArchivo.getScene().getWindow());
        if (archivo == null) {
            return;
        }
        try {
            List<FilaPrecio> filas = PreciosExcel.leer(archivo);
            if (filas.isEmpty()) {
                aviso(Alert.AlertType.WARNING, "Archivo vacío", "El Excel no tiene filas con productos.");
                return;
            }
            lblArchivo.setText(archivo.getName() + " · " + filas.size() + " filas");
            armarVistaPrevia(filas);
        } catch (Exception e) {
            e.printStackTrace();
            aviso(Alert.AlertType.ERROR, "No se pudo leer el Excel", e.getMessage());
        }
    }

    /** Compara cada fila del Excel con el producto del mismo código. */
    private void armarVistaPrevia(List<FilaPrecio> filas) {
        Map<Integer, Producto> porCodigo = new HashMap<>();
        List<Producto> productos = productoDAO.buscarTodos();
        if (productos != null) {
            for (Producto p : productos) {
                porCodigo.put(p.getCodigo(), p);
            }
        }

        // Si un código aparece repetido en el Excel, vale la última fila
        Map<Integer, FilaPrecio> ultimaPorCodigo = new LinkedHashMap<>();
        List<Cambio> resultado = new ArrayList<>();
        for (FilaPrecio f : filas) {
            if (f.error() != null) {
                resultado.add(new Cambio(f, null, Estado.ERROR));
            } else {
                ultimaPorCodigo.put(f.codigo(), f);
            }
        }
        for (FilaPrecio f : ultimaPorCodigo.values()) {
            Producto actual = porCodigo.get(f.codigo());
            Estado e;
            if (actual == null) {
                e = Estado.NUEVO;
            } else if (actual.getPrecio() != null && Math.abs(actual.getPrecio() - f.precio()) < 0.005) {
                e = Estado.IGUAL;
            } else {
                e = Estado.CAMBIA;
            }
            resultado.add(new Cambio(f, actual, e));
        }
        // Primero lo que cambia, después nuevos, errores y sin cambios
        resultado.sort((a, b) -> Integer.compare(orden(a.estado()), orden(b.estado())));
        cambios = resultado;

        lblCambian.setText(String.valueOf(contar(Estado.CAMBIA)));
        lblIguales.setText(String.valueOf(contar(Estado.IGUAL)));
        lblNuevos.setText(String.valueOf(contar(Estado.NUEVO)));
        lblErrores.setText(String.valueOf(contar(Estado.ERROR)));
        chkAgregarNuevos.setSelected(false);
        chkAgregarNuevos.setDisable(contar(Estado.NUEVO) == 0);
        tblVistaPrevia.setItems(FXCollections.observableArrayList(resultado));
        boxVistaPrevia.setVisible(true);
        boxVistaPrevia.setManaged(true);
        actualizarBoton();
    }

    private static int orden(Estado e) {
        return switch (e) {
            case CAMBIA -> 0;
            case NUEVO -> 1;
            case ERROR -> 2;
            case IGUAL -> 3;
        };
    }

    private long contar(Estado e) {
        return cambios.stream().filter(c -> c.estado() == e).count();
    }

    private void actualizarBoton() {
        long aAplicar = contar(Estado.CAMBIA) + (chkAgregarNuevos.isSelected() ? contar(Estado.NUEVO) : 0);
        btnSobrescribir.setDisable(aAplicar == 0);
    }

    @FXML
    private void sobrescribir() {
        long cambian = contar(Estado.CAMBIA);
        long nuevos = chkAgregarNuevos.isSelected() ? contar(Estado.NUEVO) : 0;

        StringBuilder mensaje = new StringBuilder();
        if (cambian > 0) {
            mensaje.append("¿Sobrescribir los precios viejos por los nuevos en ")
                    .append(cambian).append(cambian == 1 ? " producto?" : " productos?");
        }
        if (nuevos > 0) {
            mensaje.append(mensaje.length() > 0 ? "\n\n" : "")
                    .append("También se van a agregar ").append(nuevos)
                    .append(nuevos == 1 ? " producto nuevo." : " productos nuevos.");
        }
        long errores = contar(Estado.ERROR);
        if (errores > 0) {
            mensaje.append("\n\nLas ").append(errores).append(" filas con errores no se importan.");
        }
        Alert confirmar = new Alert(Alert.AlertType.CONFIRMATION, mensaje.toString(), ButtonType.YES, ButtonType.NO);
        DialogoUtil.aplicarEstilo(confirmar);
        confirmar.setTitle("Sobrescribir precios");
        confirmar.setHeaderText(null);
        if (confirmar.showAndWait().orElse(ButtonType.NO) != ButtonType.YES) {
            return;
        }

        // Copia de seguridad de todos los precios antes de tocar nada
        Path copia;
        try {
            copia = CopiaPrecios.guardarCopia(productoDAO.buscarTodos());
        } catch (Exception e) {
            e.printStackTrace();
            aviso(Alert.AlertType.ERROR, "No se pudo hacer la copia de seguridad",
                    "No se cambió ningún precio.\n\n" + e.getMessage());
            return;
        }

        try {
            Map<Long, Double> precios = new LinkedHashMap<>();
            List<Integer> codigosCambiados = new ArrayList<>();
            for (Cambio c : cambios) {
                if (c.estado() == Estado.CAMBIA) {
                    precios.put(c.actual().getId(), c.fila().precio());
                    codigosCambiados.add(c.actual().getCodigo());
                }
            }
            int actualizados = precios.isEmpty() ? 0 : productoDAO.actualizarPrecios(precios);

            int agregados = 0;
            List<Long> idsAgregados = new ArrayList<>();
            if (nuevos > 0) {
                for (Cambio c : cambios) {
                    if (c.estado() == Estado.NUEVO) {
                        Producto p = new Producto();
                        p.setCodigo(c.fila().codigo());
                        p.setNombre(c.fila().producto().isBlank() ? "Producto " + c.fila().codigo() : c.fila().producto());
                        p.setPrecio(c.fila().precio());
                        p.setTipo(c.fila().tipo().isBlank() ? null : c.fila().tipo());
                        productoDAO.guardar(p);
                        if (p.getId() != null) {
                            idsAgregados.add(p.getId());
                            agregados++;
                        }
                    }
                }
            }

            String deshacer = "";
            try {
                CopiaPrecios.registrar(copia, codigosCambiados, idsAgregados);
                deshacer = "\n\nSi te equivocaste, tocá \"Deshacer última importación\".";
            } catch (Exception e) {
                e.printStackTrace();
                deshacer = "\n\nNo se pudo anotar la importación para deshacerla, pero los precios anteriores quedaron en:\n" + copia;
            }
            aviso(Alert.AlertType.INFORMATION, "Precios importados",
                    actualizados + (actualizados == 1 ? " precio actualizado" : " precios actualizados")
                    + (agregados > 0 ? " y " + agregados + (agregados == 1 ? " producto agregado." : " productos agregados.") : ".")
                    + deshacer);
            cancelarImportacion();
            mostrarUltimaImportacion();
        } catch (Exception e) {
            e.printStackTrace();
            aviso(Alert.AlertType.ERROR, "No se pudieron guardar los precios",
                    "No se cambió ningún precio.\n\n" + e.getMessage());
        }
    }

    @FXML
    private void cancelarImportacion() {
        cambios = new ArrayList<>();
        tblVistaPrevia.getItems().clear();
        boxVistaPrevia.setVisible(false);
        boxVistaPrevia.setManaged(false);
        lblArchivo.setText("");
    }

    // ===================== DESHACER =====================

    private void mostrarUltimaImportacion() {
        Importacion imp = CopiaPrecios.ultima();
        btnDeshacer.setDisable(imp == null);
        if (imp == null) {
            lblUltimaImportacion.setText("No hay ninguna importación para deshacer.");
            return;
        }
        int n = imp.codigosCambiados().size();
        int m = imp.idsAgregados().size();
        lblUltimaImportacion.setText("Última importación: " + imp.fecha().format(FECHA_HORA) + " · "
                + n + (n == 1 ? " precio cambiado" : " precios cambiados")
                + (m > 0 ? " · " + m + (m == 1 ? " producto agregado" : " productos agregados") : ""));
    }

    @FXML
    private void deshacerImportacion() {
        Importacion imp = CopiaPrecios.ultima();
        if (imp == null) {
            mostrarUltimaImportacion();
            return;
        }
        int n = imp.codigosCambiados().size();
        int m = imp.idsAgregados().size();
        StringBuilder mensaje = new StringBuilder("¿Volver a los precios que había antes de la importación del ")
                .append(imp.fecha().format(FECHA_HORA)).append("?\n\n")
                .append(n).append(n == 1 ? " precio vuelve" : " precios vuelven").append(" a su valor anterior.");
        if (m > 0) {
            mensaje.append("\nSe ").append(m == 1 ? "borra el producto que agregó" : "borran los " + m + " productos que agregó")
                    .append(" la importación (si ya tiene ventas, se deja).");
        }
        mensaje.append("\n\nLos demás precios no se tocan.");
        Alert confirmar = new Alert(Alert.AlertType.CONFIRMATION, mensaje.toString(), ButtonType.YES, ButtonType.NO);
        DialogoUtil.aplicarEstilo(confirmar);
        confirmar.setTitle("Deshacer última importación");
        confirmar.setHeaderText(null);
        if (confirmar.showAndWait().orElse(ButtonType.NO) != ButtonType.YES) {
            return;
        }

        try {
            // Precios de la copia, por código
            Map<Integer, Double> anteriores = new HashMap<>();
            for (FilaPrecio f : PreciosExcel.leer(imp.copia().toFile())) {
                if (f.codigo() != null && f.precio() != null && f.precio() > 0) {
                    anteriores.put(f.codigo(), f.precio());
                }
            }
            Map<Integer, Producto> porCodigo = new HashMap<>();
            for (Producto p : productoDAO.buscarTodos()) {
                porCodigo.put(p.getCodigo(), p);
            }
            Map<Long, Double> restaurar = new LinkedHashMap<>();
            for (Integer codigo : imp.codigosCambiados()) {
                Producto p = porCodigo.get(codigo);
                Double precio = anteriores.get(codigo);
                if (p != null && precio != null) {
                    restaurar.put(p.getId(), precio);
                }
            }
            int restaurados = restaurar.isEmpty() ? 0 : productoDAO.actualizarPrecios(restaurar);

            int borrados = 0, quedaron = 0;
            for (Long id : imp.idsAgregados()) {
                if (productoDAO.obtenerProductoPorId(id) == null) {
                    continue; // ya no estaba
                }
                productoDAO.eliminar(id);
                if (productoDAO.obtenerProductoPorId(id) == null) {
                    borrados++;
                } else {
                    quedaron++;
                }
            }
            CopiaPrecios.borrarRegistro();
            mostrarUltimaImportacion();

            aviso(Alert.AlertType.INFORMATION, "Importación deshecha",
                    restaurados + (restaurados == 1 ? " precio volvió" : " precios volvieron") + " a su valor anterior."
                    + (borrados > 0 ? "\n" + borrados + (borrados == 1 ? " producto agregado se borró." : " productos agregados se borraron.") : "")
                    + (quedaron > 0 ? "\n" + quedaron + (quedaron == 1 ? " producto no se pudo borrar (tiene ventas o stock)." : " productos no se pudieron borrar (tienen ventas o stock).") : ""));
        } catch (Exception e) {
            e.printStackTrace();
            aviso(Alert.AlertType.ERROR, "No se pudo deshacer",
                    "No se cambió ningún precio.\n\n" + e.getMessage());
        }
    }

    @FXML
    private void abrirCarpetaCopias() {
        try {
            Files.createDirectories(CopiaPrecios.carpeta());
            Desktop.getDesktop().open(CopiaPrecios.carpeta().toFile());
        } catch (Exception e) {
            aviso(Alert.AlertType.ERROR, "No se pudo abrir la carpeta", e.getMessage());
        }
    }

    private void aviso(Alert.AlertType tipo, String titulo, String mensaje) {
        Alert a = new Alert(tipo);
        DialogoUtil.aplicarEstilo(a);
        a.setTitle(titulo);
        a.setHeaderText(null);
        a.setContentText(mensaje);
        a.showAndWait();
    }
}
