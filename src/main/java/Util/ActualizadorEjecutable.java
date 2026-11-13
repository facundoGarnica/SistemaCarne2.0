package Util;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.security.MessageDigest;
import java.util.Arrays;

/**
 * Mantiene al día el SistemaCarne.exe (el archivo en el que se hace clic y
 * su ícono).
 *
 * Las actualizaciones reemplazan solo app\SistemaCarne.jar; el .exe (con el
 * ícono grabado adentro) quedaría siempre el de la primera instalación. Por
 * eso generar-instalador.bat mete el .exe nuevo dentro del .jar que se
 * publica, y al arrancar el sistema lo compara con el instalado y, si es
 * distinto, lo reemplaza.
 *
 * Windows no deja borrar ni pisar un .exe abierto, pero sí renombrarlo: se
 * aparta el actual como SistemaCarne.exe.viejo y se pone el nuevo. El viejo
 * se borra la próxima vez que se abra el sistema.
 */
public final class ActualizadorEjecutable {

    private static final String RECURSO_EXE = "/SistemaCarne.exe";

    private ActualizadorEjecutable() {
    }

    /** Revisa y, si hace falta, reemplaza el .exe. En segundo plano y sin molestar si falla. */
    public static void revisarEnSegundoPlano() {
        Thread hilo = new Thread(() -> {
            try {
                revisar();
            } catch (Exception e) {
                System.err.println("No se pudo actualizar SistemaCarne.exe: " + e.getMessage());
            }
        }, "actualizar-exe");
        hilo.setDaemon(true);
        hilo.start();
    }

    static void revisar() throws Exception {
        Path exe = ConfiguracionApp.ejecutable();
        if (exe == null) {
            return; // desde el IDE o con el .jar suelto: no hay .exe que mantener
        }
        Path viejo = exe.resolveSibling(exe.getFileName() + ".viejo");
        Path nuevo = exe.resolveSibling(exe.getFileName() + ".nuevo");
        borrar(nuevo);
        try {
            borrar(viejo); // de una actualización anterior
        } catch (IOException e) {
            // sigue en uso: se borra la próxima vez
        }

        byte[] incluido;
        try (InputStream in = ActualizadorEjecutable.class.getResourceAsStream(RECURSO_EXE)) {
            if (in == null) {
                return; // este .jar no trae .exe
            }
            incluido = in.readAllBytes();
        }
        if (incluido.length < 10_000 || Arrays.equals(hash(incluido), hash(Files.readAllBytes(exe)))) {
            return; // ya es el mismo
        }

        // 1. El nuevo, completo, al lado
        Files.write(nuevo, incluido);
        // 2. Apartar el actual (se puede renombrar aunque esté abierto)
        Files.move(exe, viejo, StandardCopyOption.REPLACE_EXISTING);
        // 3. Poner el nuevo; si falla, volver a dejar el actual
        try {
            Files.move(nuevo, exe);
        } catch (IOException e) {
            Files.move(viejo, exe);
            throw e;
        }
        System.out.println("SistemaCarne.exe actualizado (ícono nuevo).");
        refrescarIconosDeWindows();
    }

    /** jpackage deja el .exe como solo lectura: se puede renombrar, pero para borrarlo hay que sacarle la marca. */
    private static void borrar(Path archivo) throws IOException {
        if (Files.exists(archivo)) {
            Files.setAttribute(archivo, "dos:readonly", false);
            Files.delete(archivo);
        }
    }

    /** Pide a Windows que vuelva a leer los íconos (accesos directos incluidos). */
    private static void refrescarIconosDeWindows() {
        try {
            new ProcessBuilder("ie4uinit.exe", "-show").start();
        } catch (IOException e) {
            // no está en esta versión de Windows: el ícono se ve al reiniciar el Explorador
        }
    }

    private static byte[] hash(byte[] datos) throws Exception {
        return MessageDigest.getInstance("SHA-256").digest(datos);
    }
}
