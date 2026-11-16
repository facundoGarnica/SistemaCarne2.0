package Util;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Properties;
import java.util.stream.Collectors;
import model.Producto;

/**
 * Copia de seguridad de los precios antes de cada importación por Excel, y
 * el registro de la última importación para poder deshacerla.
 *
 * <pre>
 *   backups\precios\
 *      Precios_antes_AAAA-MM-DD_HH-MM-SS.xlsx   ← todos los precios antes de importar
 *      ultima-importacion.properties             ← qué cambió la última importación
 * </pre>
 */
public final class CopiaPrecios {

    private static final DateTimeFormatter NOMBRE = DateTimeFormatter.ofPattern("yyyy-MM-dd_HH-mm-ss");
    private static final String REGISTRO = "ultima-importacion.properties";

    /** Lo que hizo la última importación: la copia previa, los códigos cambiados y los productos agregados. */
    public record Importacion(Path copia, LocalDateTime fecha, List<Integer> codigosCambiados, List<Long> idsAgregados) {
    }

    private CopiaPrecios() {
    }

    public static Path carpeta() {
        return ConfiguracionApp.carpetaBackups().resolve("precios");
    }

    /** Guarda en un Excel todos los precios actuales. Devuelve el archivo. */
    public static Path guardarCopia(List<Producto> productos) throws IOException {
        Files.createDirectories(carpeta());
        List<Producto> ordenados = new ArrayList<>(productos);
        ordenados.sort((a, b) -> Integer.compare(a.getCodigo(), b.getCodigo()));
        Path archivo = carpeta().resolve("Precios_antes_" + LocalDateTime.now().format(NOMBRE) + ".xlsx");
        PreciosExcel.exportar(ordenados, archivo.toFile());
        return archivo;
    }

    /** Anota la importación recién hecha (reemplaza a la anterior). */
    public static void registrar(Path copia, Collection<Integer> codigosCambiados, Collection<Long> idsAgregados) throws IOException {
        Properties p = new Properties();
        p.setProperty("copia", copia.getFileName().toString());
        p.setProperty("fecha", LocalDateTime.now().toString());
        p.setProperty("codigos", unir(codigosCambiados));
        p.setProperty("agregados", unir(idsAgregados));
        try (Writer w = Files.newBufferedWriter(carpeta().resolve(REGISTRO), StandardCharsets.UTF_8)) {
            p.store(w, "Ultima importacion de precios por Excel (para deshacer)");
        }
    }

    /** La última importación que se puede deshacer, o null si no hay. */
    public static Importacion ultima() {
        Path registro = carpeta().resolve(REGISTRO);
        if (!Files.isRegularFile(registro)) {
            return null;
        }
        try (Reader r = Files.newBufferedReader(registro, StandardCharsets.UTF_8)) {
            Properties p = new Properties();
            p.load(r);
            Path copia = carpeta().resolve(p.getProperty("copia", ""));
            if (!Files.isRegularFile(copia)) {
                return null;
            }
            List<Integer> codigos = new ArrayList<>();
            for (String s : partes(p.getProperty("codigos"))) {
                codigos.add(Integer.parseInt(s));
            }
            List<Long> agregados = new ArrayList<>();
            for (String s : partes(p.getProperty("agregados"))) {
                agregados.add(Long.parseLong(s));
            }
            return new Importacion(copia, LocalDateTime.parse(p.getProperty("fecha")), codigos, agregados);
        } catch (Exception e) {
            System.err.println("No se pudo leer " + registro + ": " + e.getMessage());
            return null;
        }
    }

    /** Después de deshacer: ya no hay nada para deshacer (la copia Excel queda guardada). */
    public static void borrarRegistro() throws IOException {
        Files.deleteIfExists(carpeta().resolve(REGISTRO));
    }

    private static String unir(Collection<?> valores) {
        return valores.stream().map(String::valueOf).collect(Collectors.joining(","));
    }

    private static List<String> partes(String texto) {
        List<String> r = new ArrayList<>();
        if (texto != null) {
            for (String s : texto.split(",")) {
                if (!s.isBlank()) {
                    r.add(s.trim());
                }
            }
        }
        return r;
    }
}
