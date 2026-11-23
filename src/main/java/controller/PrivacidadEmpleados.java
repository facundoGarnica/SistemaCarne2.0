package controller;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.BiConsumer;
import java.util.function.Function;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableRow;
import javafx.scene.control.TableView;
import javafx.scene.control.Tooltip;
import javafx.scene.effect.Effect;
import javafx.scene.effect.GaussianBlur;
import javafx.scene.shape.SVGPath;

/**
 * Personal: los datos de cada empleado se ven borrosos hasta que se toca el
 * ojo de ese empleado. Lo que se muestra vale para todas las pestañas del
 * módulo y se vuelve a ocultar cada vez que se abre Personal.
 */
final class PrivacidadEmpleados {

    private static final String OJO_ABIERTO =
            "M12 5c-5 0-9.27 3.11-11 7 1.73 3.89 6 7 11 7s9.27-3.11 11-7c-1.73-3.89-6-7-11-7zm0 12a5 5 0 1 1 0-10 5 5 0 0 1 0 10zm0-8a3 3 0 1 0 0 6 3 3 0 0 0 0-6z";
    private static final String OJO_CERRADO =
            "M12 7a5 5 0 0 1 5 5c0 .65-.13 1.26-.36 1.83l2.92 2.92A11.8 11.8 0 0 0 23 12c-1.73-3.89-6-7-11-7-1.4 0-2.74.25-3.98.7l2.16 2.16C10.74 7.13 11.35 7 12 7zM2 4.27l2.28 2.28.46.46A11.8 11.8 0 0 0 1 12c1.73 3.89 6 7 11 7 1.55 0 3.03-.3 4.38-.84l.42.42L19.73 21 21 19.73 3.27 2 2 4.27zM7.53 9.8l1.55 1.55A3 3 0 0 0 9 12a3 3 0 0 0 3 3c.22 0 .44-.03.65-.08l1.55 1.55A5 5 0 0 1 7 12c0-.79.2-1.53.53-2.2zm4.31-.78 3.15 3.15.02-.16a3 3 0 0 0-3-3l-.17.01z";

    private final Set<Long> visibles = new HashSet<>();
    private final List<Runnable> alCambiar = new ArrayList<>();
    private final Effect borroso = new GaussianBlur(24);

    boolean esVisible(Long idEmpleado) {
        return idEmpleado == null || visibles.contains(idEmpleado);
    }

    /** Efecto para un dato del empleado: nada si está visible, borroso si no. */
    Effect efecto(Long idEmpleado) {
        return esVisible(idEmpleado) ? null : borroso;
    }

    void aplicar(Long idEmpleado, Node... nodos) {
        Effect e = efecto(idEmpleado);
        for (Node n : nodos) {
            if (n != null) {
                n.setEffect(e);
            }
        }
    }

    void alternar(Long idEmpleado) {
        if (idEmpleado == null) {
            return;
        }
        if (!visibles.remove(idEmpleado)) {
            visibles.add(idEmpleado);
        }
        alCambiar.forEach(Runnable::run);
    }

    /** Algo para refrescar cuando se muestra u oculta un empleado. */
    void alCambiar(Runnable r) {
        alCambiar.add(r);
    }

    /** Botón de ojo para un empleado (abierto si se está viendo). */
    Button botonOjo() {
        Button b = new Button();
        b.getStyleClass().add("ps-ojo");
        b.setFocusTraversable(false);
        return b;
    }

    void actualizarOjo(Button b, Long idEmpleado) {
        boolean ver = esVisible(idEmpleado);
        SVGPath icono = new SVGPath();
        icono.setContent(ver ? OJO_ABIERTO : OJO_CERRADO);
        icono.getStyleClass().add("ps-icon");
        icono.setScaleX(0.8);
        icono.setScaleY(0.8);
        b.setGraphic(icono);
        b.setTooltip(new Tooltip(ver ? "Ocultar" : "Mostrar"));
        b.setOnAction(e -> alternar(idEmpleado));
    }

    /**
     * Agrega la columna del ojo a la tabla y deja borrosas las celdas de las
     * filas cuyo empleado no se está viendo. idDe devuelve el id del empleado
     * de la fila (null = fila sin empleado, por ejemplo un total: no se oculta).
     */
    <T> void proteger(TableView<T> tabla, Function<T, Long> idDe, BiConsumer<TableRow<T>, T> extraFila,
            TableColumn<?, ?>... siempreVisibles) {
        java.util.Set<TableColumn<?, ?>> visiblesSiempre = java.util.Set.of(siempreVisibles);
        TableColumn<T, Void> ojo = new TableColumn<>();
        ojo.setPrefWidth(56);
        ojo.setMinWidth(56);
        ojo.setMaxWidth(56);
        ojo.setSortable(false);
        ojo.setReorderable(false);
        ojo.setResizable(false);
        ojo.setCellFactory(col -> new TableCell<>() {
            private final Button boton = botonOjo();

            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                T fila = empty || getTableRow() == null ? null : getTableRow().getItem();
                Long id = fila == null ? null : idDe.apply(fila);
                if (id == null) {
                    setGraphic(null);
                } else {
                    actualizarOjo(boton, id);
                    setGraphic(boton);
                }
            }
        });
        tabla.getColumns().add(0, ojo);

        tabla.setRowFactory(tv -> new TableRow<>() {
            @Override
            protected void updateItem(T item, boolean empty) {
                super.updateItem(item, empty);
                if (extraFila != null) {
                    extraFila.accept(this, empty ? null : item);
                }
                requestLayout();
            }

            @Override
            protected void layoutChildren() {
                super.layoutChildren();
                T item = getItem();
                Effect e = isEmpty() || item == null ? null : efecto(idDe.apply(item));
                for (Node n : getChildrenUnmodifiable()) {
                    if (n instanceof TableCell<?, ?> celda && celda.getTableColumn() != ojo
                            && !visiblesSiempre.contains(celda.getTableColumn())) {
                        celda.setEffect(e);
                    }
                }
            }
        });
        alCambiar(tabla::refresh);
    }
}
