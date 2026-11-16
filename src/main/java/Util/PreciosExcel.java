package Util;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import model.Producto;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.CellType;
import org.apache.poi.ss.usermodel.DataFormatter;
import org.apache.poi.ss.usermodel.Font;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

/**
 * Exportar e importar la lista de precios en Excel.
 * Columnas: Código | Producto | Precio | Tipo (primera fila = encabezados).
 */
public final class PreciosExcel {

    public static final String[] ENCABEZADOS = {"Código", "Producto", "Precio", "Tipo"};

    /** Una fila leída del Excel. Si algo no se pudo leer, error != null. */
    public record FilaPrecio(int filaExcel, Integer codigo, String producto, Double precio, String tipo, String error) {
    }

    private PreciosExcel() {
    }

    // ===================== EXPORTAR =====================

    public static void exportar(List<Producto> productos, File archivo) throws IOException {
        try (Workbook libro = new XSSFWorkbook(); FileOutputStream out = new FileOutputStream(archivo)) {
            Sheet hoja = libro.createSheet("Precios");

            CellStyle estiloEncabezado = libro.createCellStyle();
            Font negrita = libro.createFont();
            negrita.setBold(true);
            estiloEncabezado.setFont(negrita);
            CellStyle estiloPrecio = libro.createCellStyle();
            estiloPrecio.setDataFormat(libro.createDataFormat().getFormat("#,##0.00"));

            Row encabezado = hoja.createRow(0);
            for (int i = 0; i < ENCABEZADOS.length; i++) {
                Cell c = encabezado.createCell(i);
                c.setCellValue(ENCABEZADOS[i]);
                c.setCellStyle(estiloEncabezado);
            }

            int fila = 1;
            for (Producto p : productos) {
                Row r = hoja.createRow(fila++);
                r.createCell(0).setCellValue(p.getCodigo());
                r.createCell(1).setCellValue(p.getNombre() != null ? p.getNombre() : "");
                Cell precio = r.createCell(2);
                precio.setCellValue(p.getPrecio() != null ? p.getPrecio() : 0.0);
                precio.setCellStyle(estiloPrecio);
                r.createCell(3).setCellValue(p.getTipo() != null ? p.getTipo() : "");
            }
            for (int i = 0; i < ENCABEZADOS.length; i++) {
                hoja.autoSizeColumn(i);
            }
            hoja.createFreezePane(0, 1);
            libro.write(out);
        }
    }

    // ===================== IMPORTAR =====================

    /**
     * Lee el Excel. Busca las columnas por el nombre del encabezado (en
     * cualquier orden); si no las encuentra, usa el orden Código, Producto,
     * Precio, Tipo. Las filas vacías se saltean.
     */
    public static List<FilaPrecio> leer(File archivo) throws IOException {
        List<FilaPrecio> filas = new ArrayList<>();
        DataFormatter formato = new DataFormatter(new Locale("es", "AR"));
        try (InputStream in = new FileInputStream(archivo); Workbook libro = WorkbookFactory.create(in)) {
            Sheet hoja = libro.getSheetAt(0);
            int colCodigo = 0, colProducto = 1, colPrecio = 2, colTipo = 3;
            int primeraFila = hoja.getFirstRowNum();

            Row encabezado = hoja.getRow(primeraFila);
            boolean hayEncabezado = false;
            if (encabezado != null) {
                for (Cell c : encabezado) {
                    String t = normalizar(formato.formatCellValue(c));
                    if (t.startsWith("codigo") || t.equals("cod")) { colCodigo = c.getColumnIndex(); hayEncabezado = true; }
                    else if (t.startsWith("producto") || t.startsWith("nombre")) { colProducto = c.getColumnIndex(); hayEncabezado = true; }
                    else if (t.startsWith("precio")) { colPrecio = c.getColumnIndex(); hayEncabezado = true; }
                    else if (t.startsWith("tipo") || t.startsWith("categoria")) { colTipo = c.getColumnIndex(); hayEncabezado = true; }
                }
            }

            for (int i = hayEncabezado ? primeraFila + 1 : primeraFila; i <= hoja.getLastRowNum(); i++) {
                Row r = hoja.getRow(i);
                if (r == null) {
                    continue;
                }
                String textoCodigo = formato.formatCellValue(r.getCell(colCodigo)).trim();
                String producto = formato.formatCellValue(r.getCell(colProducto)).trim();
                String tipo = formato.formatCellValue(r.getCell(colTipo)).trim();
                Cell celdaPrecio = r.getCell(colPrecio);
                String textoPrecio = formato.formatCellValue(celdaPrecio).trim();
                if (textoCodigo.isEmpty() && producto.isEmpty() && textoPrecio.isEmpty()) {
                    continue; // fila vacía
                }

                Integer codigo = leerEntero(r.getCell(colCodigo), textoCodigo);
                Double precio = leerPrecio(celdaPrecio, textoPrecio);
                String error = null;
                if (codigo == null) {
                    error = "Código inválido";
                } else if (precio == null) {
                    error = "Precio inválido";
                } else if (precio <= 0) {
                    error = "El precio tiene que ser mayor a 0";
                }
                filas.add(new FilaPrecio(i + 1, codigo, producto, precio, tipo, error));
            }
        } catch (IOException e) {
            throw e;
        } catch (Exception e) {
            throw new IOException("No se pudo leer el archivo. ¿Es un Excel (.xlsx o .xls)?", e);
        }
        return filas;
    }

    private static Integer leerEntero(Cell celda, String texto) {
        if (celda != null && celda.getCellType() == CellType.NUMERIC) {
            double v = celda.getNumericCellValue();
            return v == Math.floor(v) ? (int) v : null;
        }
        String limpio = texto.replace(".", "").replace(" ", "");
        try {
            return Integer.parseInt(limpio);
        } catch (NumberFormatException e) {
            return null;
        }
    }

    /** Acepta celdas numéricas y textos como "1500", "1.500", "1.500,50" o "$ 1.500". */
    private static Double leerPrecio(Cell celda, String texto) {
        if (celda != null && (celda.getCellType() == CellType.NUMERIC
                || (celda.getCellType() == CellType.FORMULA && celda.getCachedFormulaResultType() == CellType.NUMERIC))) {
            return celda.getNumericCellValue();
        }
        String limpio = texto.replace("$", "").replace(" ", "");
        if (limpio.isEmpty()) {
            return null;
        }
        if (limpio.contains(",")) {
            limpio = limpio.replace(".", "").replace(",", ".");   // 1.500,50
        } else if (limpio.matches("\\d{1,3}(\\.\\d{3})+")) {
            limpio = limpio.replace(".", "");                       // 1.500
        }
        try {
            return Double.parseDouble(limpio);
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private static String normalizar(String s) {
        return java.text.Normalizer.normalize(s.trim().toLowerCase(Locale.ROOT), java.text.Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "");
    }
}
