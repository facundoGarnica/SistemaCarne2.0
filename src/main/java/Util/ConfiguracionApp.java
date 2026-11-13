package Util;

import carniceria.sistemacarne.Launcher;
import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Properties;

/**
 * Datos de la instalación: versión del sistema, carpetas y el archivo
 * config.properties de cada PC.
 *
 * Instalado con el .exe queda así:
 * <pre>
 *   C:\SistemaCarne\
 *      SistemaCarne.exe
 *      config.properties      ← datos de MySQL de esta PC
 *      app\SistemaCarne.jar   ← el sistema (lo reemplaza el actualizador)
 *      backups\               ← copias de la base
 *      update\                ← descargas del actualizador
 * </pre>
 */
public final class ConfiguracionApp {

    private static Properties config;

    private ConfiguracionApp() {
    }

    /** Versión del sistema que está corriendo (la del pom.xml al compilar). */
    public static String version() {
        try (InputStream in = ConfiguracionApp.class.getResourceAsStream("/version.properties")) {
            if (in != null) {
                Properties p = new Properties();
                p.load(in);
                String v = p.getProperty("version", "").trim();
                if (!v.isEmpty() && !v.startsWith("${")) {
                    return v;
                }
            }
        } catch (IOException e) {
            // sin versión: se informa como desconocida
        }
        return "0.0.0";
    }

    /**
     * El .jar desde el que corre el sistema, o null si corre desde el IDE
     * (clases sueltas en target/classes).
     */
    public static Path jarActual() {
        try {
            Path p = Paths.get(Launcher.class.getProtectionDomain().getCodeSource().getLocation().toURI());
            if (Files.isRegularFile(p) && p.getFileName().toString().toLowerCase().endsWith(".jar")) {
                return p;
            }
        } catch (Exception e) {
            // no se pudo determinar
        }
        return null;
    }

    /** True si corre instalado (desde un .jar), false si corre desde el IDE. */
    public static boolean esInstalacion() {
        return jarActual() != null;
    }

    /**
     * Carpeta raíz de la instalación (donde está el .exe). Con el .exe el
     * .jar está en raiz\app\; si se ejecuta el .jar suelto, es su carpeta.
     * Desde el IDE es la carpeta de trabajo.
     */
    public static Path carpetaInstalacion() {
        Path jar = jarActual();
        if (jar == null) {
            return Paths.get(System.getProperty("user.dir")).toAbsolutePath();
        }
        Path dir = jar.toAbsolutePath().getParent();
        if (dir.getFileName() != null && dir.getFileName().toString().equalsIgnoreCase("app")
                && dir.getParent() != null) {
            return dir.getParent();
        }
        return dir;
    }

    /** Ruta del SistemaCarne.exe (solo si se abrió con el .exe), o null. */
    public static Path ejecutable() {
        String ruta = System.getProperty("jpackage.app-path");
        if (ruta != null && !ruta.isBlank()) {
            Path p = Paths.get(ruta);
            if (Files.isRegularFile(p)) {
                return p;
            }
        }
        return null;
    }

    public static Path carpetaBackups() {
        return carpetaInstalacion().resolve("backups");
    }

    public static Path carpetaUpdate() {
        return carpetaInstalacion().resolve("update");
    }

    /**
     * Lee config.properties de la carpeta de instalación. Si no existe, se
     * usan los valores de hibernate.cfg.xml.
     */
    public static synchronized Properties config() {
        if (config == null) {
            config = new Properties();
            Path archivo = carpetaInstalacion().resolve("config.properties");
            if (Files.isRegularFile(archivo)) {
                try (Reader r = Files.newBufferedReader(archivo, StandardCharsets.UTF_8)) {
                    config.load(r);
                    System.out.println("Configuración leída de: " + archivo);
                } catch (IOException e) {
                    System.err.println("No se pudo leer " + archivo + ": " + e.getMessage());
                }
            }
        }
        return config;
    }

    /** Valor de config.properties, o null si no está o está vacío. */
    public static String valor(String clave) {
        String v = config().getProperty(clave);
        return (v == null || v.isBlank()) ? null : v.trim();
    }
}
