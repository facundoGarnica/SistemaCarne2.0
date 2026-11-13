package Util;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.function.DoubleConsumer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.zip.ZipFile;

/**
 * Actualizaciones del sistema desde GitHub Releases.
 *
 * Para publicar una versión: subir la versión del pom.xml, generar con
 * generar-instalador.bat y crear un Release en GitHub con el tag "vX.Y.Z"
 * adjuntando dist\SistemaCarne.jar (ver INSTALACION.md).
 *
 * Instalar: se descarga el .jar nuevo a update\, el sistema se cierra y un
 * .bat reemplaza app\SistemaCarne.jar (Windows no deja pisarlo mientras está
 * abierto), guarda el anterior en update\SistemaCarne-anterior.jar y vuelve
 * a abrir el sistema.
 */
public final class Actualizador {

    /** Repositorio público de donde se bajan las versiones. */
    public static final String REPOSITORIO = "facundoGarnica/SistemaCarne2.0";

    private static final String URL_ULTIMA =
            "https://api.github.com/repos/" + REPOSITORIO + "/releases/latest";
    private static final String USER_AGENT = "SistemaCarne-Actualizador";
    /** Clase que tiene que estar en el .jar descargado para aceptarlo. */
    private static final String CLASE_CONTROL = "carniceria/sistemacarne/Launcher.class";

    private static final HttpClient HTTP = HttpClient.newBuilder()
            .followRedirects(HttpClient.Redirect.NORMAL)
            .connectTimeout(Duration.ofSeconds(15))
            .build();

    /** Datos de una versión publicada en GitHub. */
    public record Version(String numero, String titulo, String notas, LocalDateTime fecha,
                          String urlJar, long tamanio) {
    }

    private Actualizador() {
    }

    // ===================== CONSULTA =====================

    /**
     * Última versión publicada, o null si todavía no hay ninguna publicada
     * (o la publicada no tiene un .jar adjunto).
     */
    public static Version buscarUltimaVersion() throws IOException, InterruptedException {
        HttpRequest req = HttpRequest.newBuilder(URI.create(URL_ULTIMA))
                .header("Accept", "application/vnd.github+json")
                .header("User-Agent", USER_AGENT)
                .timeout(Duration.ofSeconds(20))
                .GET().build();
        HttpResponse<String> resp = HTTP.send(req, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));

        if (resp.statusCode() == 404) {
            return null;
        }
        if (resp.statusCode() != 200) {
            throw new IOException("GitHub respondió " + resp.statusCode());
        }

        JsonNode json = new ObjectMapper().readTree(resp.body());
        String tag = json.path("tag_name").asText("");
        String numero = tag.startsWith("v") || tag.startsWith("V") ? tag.substring(1) : tag;

        // Preferir el asset "SistemaCarne.jar"; si no, el primer .jar
        String urlJar = null;
        long tamanio = -1;
        for (JsonNode asset : json.path("assets")) {
            String nombre = asset.path("name").asText("");
            if (!nombre.toLowerCase().endsWith(".jar")) {
                continue;
            }
            if (urlJar == null || nombre.equalsIgnoreCase("SistemaCarne.jar")) {
                urlJar = asset.path("browser_download_url").asText(null);
                tamanio = asset.path("size").asLong(-1);
            }
        }
        if (urlJar == null) {
            return null;
        }

        LocalDateTime fecha = null;
        String publicada = json.path("published_at").asText(null);
        if (publicada != null) {
            fecha = OffsetDateTime.parse(publicada).atZoneSameInstant(ZoneId.systemDefault()).toLocalDateTime();
        }

        return new Version(numero, json.path("name").asText(tag), json.path("body").asText(""),
                fecha, urlJar, tamanio);
    }

    /** True si la versión publicada es más nueva que la instalada. */
    public static boolean esMasNueva(Version v) {
        return v != null && compararVersiones(v.numero(), ConfiguracionApp.version()) > 0;
    }

    /** Compara "1.10.2" con "1.9.0" número por número. */
    public static int compararVersiones(String a, String b) {
        int[] x = partes(a);
        int[] y = partes(b);
        for (int i = 0; i < Math.max(x.length, y.length); i++) {
            int xi = i < x.length ? x[i] : 0;
            int yi = i < y.length ? y[i] : 0;
            if (xi != yi) {
                return Integer.compare(xi, yi);
            }
        }
        return 0;
    }

    private static int[] partes(String v) {
        String[] trozos = v.replaceAll("[^0-9.]", "").split("\\.");
        int[] r = new int[trozos.length];
        for (int i = 0; i < trozos.length; i++) {
            try {
                r[i] = Integer.parseInt(trozos[i]);
            } catch (NumberFormatException e) {
                r[i] = 0;
            }
        }
        return r;
    }

    // ===================== DESCARGA =====================

    /**
     * Descarga el .jar de la versión a update\SistemaCarne-nuevo.jar,
     * informando el progreso (0..1, o -1 si no se conoce el tamaño), y
     * verifica que sea un .jar válido del sistema.
     */
    public static Path descargar(Version v, DoubleConsumer progreso) throws IOException, InterruptedException {
        Path carpeta = ConfiguracionApp.carpetaUpdate();
        Files.createDirectories(carpeta);
        Path parcial = carpeta.resolve("SistemaCarne-nuevo.jar.part");
        Path destino = carpeta.resolve("SistemaCarne-nuevo.jar");

        HttpRequest req = HttpRequest.newBuilder(URI.create(v.urlJar()))
                .header("User-Agent", USER_AGENT)
                .header("Accept", "application/octet-stream")
                .GET().build();
        HttpResponse<InputStream> resp = HTTP.send(req, HttpResponse.BodyHandlers.ofInputStream());
        if (resp.statusCode() != 200) {
            throw new IOException("La descarga respondió " + resp.statusCode());
        }

        long total = resp.headers().firstValueAsLong("Content-Length").orElse(v.tamanio());
        try (InputStream in = resp.body(); OutputStream out = Files.newOutputStream(parcial)) {
            byte[] buffer = new byte[64 * 1024];
            long leidos = 0;
            int n;
            while ((n = in.read(buffer)) != -1) {
                out.write(buffer, 0, n);
                leidos += n;
                progreso.accept(total > 0 ? (double) leidos / total : -1);
            }
        }

        verificarJar(parcial);
        Files.move(parcial, destino, StandardCopyOption.REPLACE_EXISTING);
        return destino;
    }

    private static void verificarJar(Path jar) throws IOException {
        try (ZipFile zip = new ZipFile(jar.toFile())) {
            if (zip.getEntry(CLASE_CONTROL) == null) {
                throw new IOException("El archivo descargado no es una versión válida del sistema.");
            }
        } catch (IOException e) {
            Files.deleteIfExists(jar);
            throw e instanceof java.util.zip.ZipException
                    ? new IOException("El archivo descargado está dañado. Probá de nuevo.", e)
                    : e;
        }
    }

    // ===================== BACKUP =====================

    /**
     * Copia de la base con mysqldump en backups\carnedb_AAAA-MM-DD_HH-MM.sql.
     * Devuelve el archivo creado.
     */
    public static Path hacerBackup() throws IOException, InterruptedException {
        String[] conexion = HibernateUtil.datosConexion();
        Matcher m = Pattern.compile("jdbc:mysql://([^:/?]+)(?::(\\d+))?/([^?]+)").matcher(conexion[0]);
        if (!m.find()) {
            throw new IOException("No se entiende la URL de la base: " + conexion[0]);
        }
        String host = m.group(1);
        String puerto = m.group(2) != null ? m.group(2) : "3306";
        String base = m.group(3);

        Path mysqldump = buscarMysqldump();
        Path carpeta = ConfiguracionApp.carpetaBackups();
        Files.createDirectories(carpeta);
        String fecha = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd_HH-mm-ss"));
        Path archivo = carpeta.resolve(base + "_" + fecha + ".sql");
        Path log = carpeta.resolve("ultimo-backup.log");

        List<String> cmd = new ArrayList<>(List.of(
                mysqldump.toString(), "-h", host, "-P", puerto, "-u", conexion[1],
                "--single-transaction", "--routines", "--default-character-set=utf8mb4",
                "--result-file=" + archivo, base));
        ProcessBuilder pb = new ProcessBuilder(cmd);
        // La contraseña por variable de entorno, no en la línea de comandos
        if (conexion[2] != null && !conexion[2].isEmpty()) {
            pb.environment().put("MYSQL_PWD", conexion[2]);
        }
        pb.redirectErrorStream(true);
        pb.redirectOutput(log.toFile());

        int codigo = pb.start().waitFor();
        if (codigo != 0 || !Files.isRegularFile(archivo) || Files.size(archivo) == 0) {
            String detalle = Files.isRegularFile(log) ? Files.readString(log).trim() : "";
            Files.deleteIfExists(archivo);
            throw new IOException("mysqldump falló (código " + codigo + ")"
                    + (detalle.isEmpty() ? "" : ":\n" + detalle));
        }
        return archivo;
    }

    /**
     * mysqldump.exe: primero la carpeta de config.properties (mysql.bin),
     * después el PATH y por último las carpetas típicas de MySQL.
     */
    private static Path buscarMysqldump() throws IOException {
        String bin = ConfiguracionApp.valor("mysql.bin");
        if (bin != null) {
            Path p = Paths.get(bin, "mysqldump.exe");
            if (Files.isRegularFile(p)) {
                return p;
            }
        }
        String path = System.getenv("PATH");
        if (path != null) {
            for (String dir : path.split(File.pathSeparator)) {
                try {
                    Path p = Paths.get(dir.trim(), "mysqldump.exe");
                    if (Files.isRegularFile(p)) {
                        return p;
                    }
                } catch (Exception e) {
                    // entrada inválida del PATH
                }
            }
        }
        for (String raiz : new String[]{"C:\\Program Files\\MySQL", "C:\\Program Files (x86)\\MySQL"}) {
            File[] versiones = new File(raiz).listFiles(File::isDirectory);
            if (versiones == null) {
                continue;
            }
            for (File v : versiones) {
                Path p = v.toPath().resolve("bin").resolve("mysqldump.exe");
                if (Files.isRegularFile(p)) {
                    return p;
                }
            }
        }
        throw new IOException("No se encontró mysqldump.exe. Indicá la carpeta bin de MySQL "
                + "en config.properties (mysql.bin).");
    }

    // ===================== INSTALAR / VOLVER ATRÁS =====================

    /** Hay una versión anterior guardada para volver atrás. */
    public static boolean hayVersionAnterior() {
        return Files.isRegularFile(ConfiguracionApp.carpetaUpdate().resolve("SistemaCarne-anterior.jar"));
    }

    /**
     * Deja programado el reemplazo de app\SistemaCarne.jar por el .jar dado y
     * el reinicio del sistema. Después de llamar a esto hay que cerrar el
     * sistema (System.exit) para liberar el .jar.
     */
    public static void instalarAlCerrar(Path jarNuevo) throws IOException {
        Path actual = ConfiguracionApp.jarActual();
        if (actual == null) {
            throw new IOException("Solo se puede actualizar el sistema instalado (no desde el IDE).");
        }
        Path carpeta = ConfiguracionApp.carpetaUpdate();
        Files.createDirectories(carpeta);
        Path anterior = carpeta.resolve("SistemaCarne-anterior.jar");

        // Cómo volver a abrir: con el .exe si existe; si no, con javaw -jar
        Path exe = ConfiguracionApp.ejecutable();
        String reabrir = exe != null
                ? "start \"\" \"" + exe + "\""
                : "start \"\" \"" + Paths.get(System.getProperty("java.home"), "bin", "javaw.exe")
                        + "\" -jar \"" + actual + "\"";

        String bat = String.join("\r\n",
                "@echo off",
                "chcp 65001 >nul",
                "title Actualizando el sistema...",
                "echo Actualizando el sistema, no cierre esta ventana...",
                "set INTENTOS=0",
                ":esperar",
                // "timeout" no anda sin consola (entrada redirigida): se espera con ping
                "ping -n 3 127.0.0.1 >nul",
                "set /a INTENTOS+=1",
                // Mientras el sistema sigue abierto el .jar está bloqueado y el move falla
                "copy /y \"" + actual + "\" \"" + carpeta.resolve("SistemaCarne-respaldo.tmp") + "\" >nul 2>&1",
                "move /y \"" + jarNuevo + "\" \"" + actual + "\" >nul 2>&1",
                "if errorlevel 1 (",
                "  if %INTENTOS% lss 20 goto esperar",
                "  echo No se pudo actualizar: el sistema sigue abierto.",
                "  echo Cierrelo y vuelva a intentar desde Actualizaciones.",
                "  pause",
                "  exit /b 1",
                ")",
                "move /y \"" + carpeta.resolve("SistemaCarne-respaldo.tmp") + "\" \"" + anterior + "\" >nul 2>&1",
                reabrir,
                "exit /b 0",
                "");
        Path script = carpeta.resolve("aplicar-actualizacion.bat");
        Files.writeString(script, bat, StandardCharsets.UTF_8);

        ProcessBuilder pb = new ProcessBuilder("cmd.exe", "/c", script.toString());
        pb.redirectErrorStream(true);
        pb.redirectOutput(carpeta.resolve("aplicar-actualizacion.log").toFile());
        pb.start();
    }

    /** Programa la vuelta a la versión anterior guardada (y el reinicio). */
    public static void volverAVersionAnteriorAlCerrar() throws IOException {
        Path carpeta = ConfiguracionApp.carpetaUpdate();
        Path anterior = carpeta.resolve("SistemaCarne-anterior.jar");
        if (!Files.isRegularFile(anterior)) {
            throw new IOException("No hay una versión anterior guardada.");
        }
        // Se trabaja con una copia: el .bat guarda la versión actual como "anterior"
        Path restaurar = carpeta.resolve("SistemaCarne-restaurar.jar");
        Files.copy(anterior, restaurar, StandardCopyOption.REPLACE_EXISTING);
        instalarAlCerrar(restaurar);
    }
}
