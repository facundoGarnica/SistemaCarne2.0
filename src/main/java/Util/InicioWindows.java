package Util;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Base64;
import java.util.Locale;
import java.util.concurrent.TimeUnit;

/**
 * "Iniciar con Windows": registra el SistemaCarne.exe instalado en
 * HKCU\Software\Microsoft\Windows\CurrentVersion\Run.
 *
 * Así Windows abre siempre el .exe de la instalación (el que actualiza el
 * sistema), en lugar de una copia puesta en la carpeta Inicio, que nunca se
 * actualiza y además hace que Windows abra sus carpetas (backups, etc.) al
 * prender la PC.
 */
public final class InicioWindows {

    private static final String CLAVE = "HKCU\\Software\\Microsoft\\Windows\\CurrentVersion\\Run";
    private static final String NOMBRE = "SistemaCarne";

    private InicioWindows() {
    }

    /** True si se puede usar: Windows y abierto con el SistemaCarne.exe. */
    public static boolean disponible() {
        return esWindows() && ConfiguracionApp.ejecutable() != null && !estaEnCarpetaInicio();
    }

    /** True si Windows ya abre el sistema al iniciar. */
    public static boolean estaActivo() {
        return rutaRegistrada() != null;
    }

    /** Ruta que Windows abre al iniciar (sin comillas), o null si no está activo. */
    public static String rutaRegistrada() {
        if (!esWindows()) {
            return null;
        }
        Resultado r = ejecutar("reg", "query", CLAVE, "/v", NOMBRE);
        if (r.codigo != 0) {
            return null;
        }
        for (String linea : r.salida.split("\\R")) {
            int i = linea.indexOf("REG_SZ");
            if (linea.trim().startsWith(NOMBRE) && i >= 0) {
                return linea.substring(i + "REG_SZ".length()).trim().replace("\"", "");
            }
        }
        return null;
    }

    public static void activar() throws Exception {
        Path exe = ConfiguracionApp.ejecutable();
        if (exe == null) {
            throw new IllegalStateException("Solo funciona con el sistema instalado (SistemaCarne.exe).");
        }
        String valor = "\"" + exe.toAbsolutePath() + "\"";
        powershell("Set-ItemProperty -Path 'HKCU:\\Software\\Microsoft\\Windows\\CurrentVersion\\Run' -Name '"
                + NOMBRE + "' -Value '" + valor.replace("'", "''") + "'");
        if (!estaActivo()) {
            throw new IllegalStateException("Windows no guardó el inicio automático.");
        }
    }

    public static void desactivar() throws Exception {
        if (!estaActivo()) {
            return;
        }
        Resultado r = ejecutar("reg", "delete", CLAVE, "/v", NOMBRE, "/f");
        if (r.codigo != 0) {
            throw new IllegalStateException(r.salida.trim());
        }
    }

    /**
     * Al arrancar: si el inicio automático apunta a otra ruta (por ejemplo,
     * se movió la instalación), lo corrige para que abra este .exe.
     */
    public static void revisarEnSegundoPlano() {
        if (!disponible()) {
            return;
        }
        Thread hilo = new Thread(() -> {
            try {
                String registrada = rutaRegistrada();
                Path exe = ConfiguracionApp.ejecutable();
                if (registrada != null && exe != null
                        && !Paths.get(registrada).toAbsolutePath().equals(exe.toAbsolutePath())) {
                    activar();
                    System.out.println("Inicio con Windows corregido: " + exe);
                }
            } catch (Exception e) {
                System.err.println("No se pudo revisar el inicio con Windows: " + e.getMessage());
            }
        }, "inicio-windows");
        hilo.setDaemon(true);
        hilo.start();
    }

    /**
     * True si este sistema se está ejecutando desde dentro de la carpeta
     * Inicio de Windows (una copia que no se actualiza).
     */
    public static boolean estaEnCarpetaInicio() {
        if (!esWindows()) {
            return false;
        }
        Path aqui = ConfiguracionApp.carpetaInstalacion().toAbsolutePath().normalize();
        String sufijo = "Microsoft\\Windows\\Start Menu\\Programs\\Startup";
        for (String variable : new String[]{"APPDATA", "ProgramData"}) {
            String base = System.getenv(variable);
            if (base != null && aqui.startsWith(Paths.get(base, sufijo).toAbsolutePath().normalize())) {
                return true;
            }
        }
        return aqui.toString().toLowerCase(Locale.ROOT).contains("\\start menu\\programs\\startup");
    }

    /** Carpeta Inicio del usuario (para mostrarla o abrirla). */
    public static Path carpetaInicioUsuario() {
        String base = System.getenv("APPDATA");
        return base == null ? null : Paths.get(base, "Microsoft", "Windows", "Start Menu", "Programs", "Startup");
    }

    // ===================== AUXILIARES =====================

    private static boolean esWindows() {
        return System.getProperty("os.name", "").toLowerCase(Locale.ROOT).contains("win");
    }

    /** Ejecuta un comando de PowerShell sin problemas de comillas (EncodedCommand). */
    private static void powershell(String comando) {
        String cifrado = Base64.getEncoder().encodeToString(
                ("$ErrorActionPreference='Stop'; " + comando).getBytes(StandardCharsets.UTF_16LE));
        Resultado r = ejecutar("powershell", "-NoProfile", "-NonInteractive", "-EncodedCommand", cifrado);
        if (r.codigo != 0) {
            throw new IllegalStateException(r.salida.trim());
        }
    }

    private record Resultado(int codigo, String salida) {
    }

    private static Resultado ejecutar(String... comando) {
        try {
            Process p = new ProcessBuilder(comando).redirectErrorStream(true).start();
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            try (InputStream in = p.getInputStream()) {
                in.transferTo(out);
            }
            if (!p.waitFor(20, TimeUnit.SECONDS)) {
                p.destroyForcibly();
                return new Resultado(-1, "Tardó demasiado");
            }
            return new Resultado(p.exitValue(), out.toString(java.nio.charset.Charset.defaultCharset()));
        } catch (Exception e) {
            return new Resultado(-1, String.valueOf(e.getMessage()));
        }
    }
}
