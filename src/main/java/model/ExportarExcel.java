package model;

import model.Venta;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import java.io.FileOutputStream;
import java.time.format.DateTimeFormatter;
import java.util.List;

public class ExportarExcel {

    public static void exportarVentas(List<Venta> ventas, String ruta) {

        try (Workbook workbook = new XSSFWorkbook()) {

            Sheet hoja = workbook.createSheet("Ventas");

            // Encabezados
            Row encabezado = hoja.createRow(0);

            encabezado.createCell(0).setCellValue("Fecha");
            encabezado.createCell(1).setCellValue("Monto");

            DateTimeFormatter formato =
                    DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

            int fila = 1;

            for (Venta venta : ventas) {

                Row row = hoja.createRow(fila++);

                row.createCell(0).setCellValue(
                        venta.getFecha().format(formato)
                );

                // Solo lo cobrado en virtual (en un pago mixto, la parte por QR)
                row.createCell(1).setCellValue(
                        venta.montoEnVirtual()
                );
            }

            hoja.autoSizeColumn(0);
            hoja.autoSizeColumn(1);

            FileOutputStream archivo = new FileOutputStream(ruta);

            workbook.write(archivo);

            archivo.close();

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

}