package model;

import javax.print.*;
import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.text.Normalizer;

public class PrintTicket {

    public static void print(String contenido) {

        try {

            PrintService service = PrintServiceLookup.lookupDefaultPrintService();

            if (service == null) {
                System.out.println("No se encontró impresora.");
                return;
            }

            DocPrintJob job = service.createPrintJob();

            InputStream is = new ByteArrayInputStream(normalizarParaImpresora(contenido).getBytes());

            Doc doc = new SimpleDoc(is, DocFlavor.INPUT_STREAM.AUTOSENSE, null);

            job.print(doc, null);

        } catch (Exception e) {
            e.printStackTrace();
        }

    }

    /**
     * Muchas impresoras térmicas no tienen los glifos de la Ñ ni de vocales
     * acentuadas y los imprimen en blanco o como caracteres inválidos. Se
     * reemplaza la Ñ/ñ por "NI"/"ni" (ej: "mañana" -> "maniana") y se le
     * quitan los acentos al resto de las letras (ej: "José" -> "Jose").
     */
    private static String normalizarParaImpresora(String texto) {
        if (texto == null) {
            return null;
        }
        String sinNie = texto.replace("Ñ", "NI").replace("ñ", "ni");
        String descompuesto = Normalizer.normalize(sinNie, Normalizer.Form.NFD);
        return descompuesto.replaceAll("\\p{M}", "");
    }

    public static void printBytes(byte[] bytes) {

    try {

        PrintService service =
                PrintServiceLookup.lookupDefaultPrintService();

        DocPrintJob job = service.createPrintJob();

        DocFlavor flavor =
                DocFlavor.BYTE_ARRAY.AUTOSENSE;

        Doc doc =
                new SimpleDoc(bytes, flavor, null);

        job.print(doc, null);

    } catch (Exception e) {

        e.printStackTrace();
    }
}
}