package Util;

import java.net.URL;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.prefs.Preferences;
import javafx.scene.image.Image;
import javafx.stage.Stage;
import javafx.application.Platform;
import javafx.collections.ListChangeListener;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Window;

/**
 * Estilo del sistema: paleta de colores y zoom (Configuración > Estilo).
 *
 * Colores: todas las hojas de estilo usan "colores de paleta" (-c-primario,
 * -c-oscuro, ...) definidos en styles/paleta-base.css. Cada paleta es un
 * archivo en styles/paletas/ que los redefine; se agrega al final de las
 * hojas de estilo de cada ventana, así que cambiar la paleta cambia toda la
 * interfaz al instante.
 *
 * Zoom: se aplica al arrancar (escala de JavaFX), por eso requiere reiniciar.
 *
 * Lo elegido se guarda por usuario de Windows (Preferences).
 */
public final class EstiloApp {

    /** Las paletas disponibles. Los colores de muestra son solo para la tarjeta de cada una. */
    public enum Paleta {
        AZUL("azul", "Azul clásico", "Predeterminada", "#2c3e50", "#3498db", "#d6eaf8", "#e3eaf2"),
        VERDE("verde", "Verde esmeralda", "Sobria y descansada", "#1e3b30", "#2e8b62", "#d5ede2", "#e2ece7"),
        GRAFITO("grafito", "Gris grafito", "Neutra, sin color", "#263238", "#546e7a", "#dfe6e9", "#e4e8eb"),
        BORDO("bordo", "Bordó", "Cálida y elegante", "#3a1f29", "#9e3a4f", "#f2dde2", "#eee5e8"),
        INDIGO("indigo", "Índigo", "Azul profundo y serio", "#1f2547", "#3f51b5", "#dde1f5", "#e4e6f0"),
        PETROLEO("petroleo", "Petróleo", "Azul verdoso, tranquila", "#12343b", "#1f7a8c", "#d3ebf0", "#e1eaec"),
        BERENJENA("berenjena", "Berenjena", "Violeta sobrio", "#2e1f38", "#7b4b94", "#ebe0f1", "#ebe6ee"),
        MARINO_DORADO("marino-dorado", "Marino y dorado", "Combinada", "#1f2a44", "#b0812a", "#f2e9d5", "#e8e5de"),
        CARBON_TURQUESA("carbon-turquesa", "Carbón y turquesa", "Combinada", "#2d3436", "#16a085", "#d1f0e9", "#e4e9e8"),
        CHOCOLATE_CREMA("chocolate-crema", "Chocolate y crema", "Combinada", "#3b2a20", "#9c6b3c", "#f1e6d6", "#ece6dd"),
        PIZARRA_NARANJA("pizarra-naranja", "Pizarra y naranja", "Combinada", "#2f3a45", "#d0692a", "#fbe5d4", "#e6e8ea"),
        OLIVA_ARENA("oliva-arena", "Oliva y arena", "Combinada", "#2f3324", "#6b7d3a", "#e9ecd8", "#e9e7de");

        public final String id;
        public final String nombre;
        public final String descripcion;
        /** Muestras: oscuro (barras), primario (botones), suave (resaltados), fondo. */
        public final String[] muestras;

        Paleta(String id, String nombre, String descripcion, String... muestras) {
            this.id = id;
            this.nombre = nombre;
            this.descripcion = descripcion;
            this.muestras = muestras;
        }

        public static Paleta porId(String id) {
            for (Paleta p : values()) {
                if (p.id.equals(id)) {
                    return p;
                }
            }
            return AZUL;
        }
    }

    /** Niveles de zoom ofrecidos (1.0 = 100 %). */
    public static final double[] ZOOMS = {0.8, 0.9, 1.0, 1.1, 1.25, 1.5};

    private static final Preferences PREFS = Preferences.userRoot().node("sistemacarne/estilo");
    private static final String CARPETA_PALETAS = "/styles/paletas/";

    private static Paleta paletaActual;
    /** Zoom con el que arrancó esta ejecución (el elegido rige al reiniciar). */
    private static double zoomAlArrancar = 1.0;
    private static boolean instalado;

    private EstiloApp() {
    }

    // ===================== ZOOM (antes de arrancar JavaFX) =====================

    /**
     * Fija la escala de JavaFX según el zoom guardado. Se llama en main()
     * antes de Application.launch(). Respeta la escala que ya tenga Windows
     * (por ejemplo 125 %) y le aplica el zoom encima.
     */
    public static void aplicarZoomAntesDeArrancar() {
        zoomAlArrancar = zoomGuardado();
        if (Math.abs(zoomAlArrancar - 1.0) < 0.001 || System.getProperty("glass.win.uiScale") != null) {
            return;
        }
        double escalaWindows = 1.0;
        try {
            escalaWindows = java.awt.GraphicsEnvironment.getLocalGraphicsEnvironment()
                    .getDefaultScreenDevice().getDefaultConfiguration().getDefaultTransform().getScaleX();
        } catch (Throwable e) {
            // sin AWT: se asume 100 %
        }
        System.setProperty("glass.win.uiScale", String.valueOf(escalaWindows * zoomAlArrancar));
    }

    public static double zoomGuardado() {
        double z = PREFS.getDouble("zoom", 1.0);
        return (z < 0.5 || z > 2.0) ? 1.0 : z;
    }

    public static double zoomAlArrancar() {
        return zoomAlArrancar;
    }

    public static void guardarZoom(double zoom) {
        PREFS.putDouble("zoom", zoom);
    }

    // ===================== PALETA =====================

    public static synchronized Paleta paleta() {
        if (paletaActual == null) {
            paletaActual = Paleta.porId(PREFS.get("paleta", Paleta.AZUL.id));
        }
        return paletaActual;
    }

    /** Cambia la paleta, la guarda y la aplica a todas las ventanas abiertas. */
    public static void cambiarPaleta(Paleta p) {
        paletaActual = p;
        PREFS.put("paleta", p.id);
        for (Window w : Window.getWindows()) {
            aplicar(w.getScene());
        }
    }

    /**
     * Los colores de una paleta como estilo en línea ("-c-primario: #...; ..."),
     * para pintar una vista previa con esa paleta sin cambiar la del sistema.
     */
    public static String variablesEnLinea(Paleta p) {
        try (java.io.InputStream in = EstiloApp.class.getResourceAsStream(CARPETA_PALETAS + p.id + ".css")) {
            if (in == null) {
                return "";
            }
            String css = new String(in.readAllBytes(), java.nio.charset.StandardCharsets.UTF_8);
            StringBuilder sb = new StringBuilder();
            java.util.regex.Matcher m = java.util.regex.Pattern
                    .compile("(-c-[a-z0-9-]+)\\s*:\\s*(#[0-9a-fA-F]{6})").matcher(css);
            while (m.find()) {
                sb.append(m.group(1)).append(": ").append(m.group(2)).append("; ");
            }
            return sb.toString();
        } catch (java.io.IOException e) {
            return "";
        }
    }

    /** URL de la hoja de estilos de la paleta actual. */
    public static String hojaPaleta() {
        URL url = EstiloApp.class.getResource(CARPETA_PALETAS + paleta().id + ".css");
        return url != null ? url.toExternalForm() : null;
    }

    /**
     * Aplica la paleta a todas las ventanas: las abiertas y las que se abran
     * después (incluye diálogos y cambios de escena). Se llama una vez al
     * arrancar, en App.start().
     */
    public static void instalar() {
        if (instalado) {
            return;
        }
        instalado = true;
        for (Window w : Window.getWindows()) {
            vigilar(w);
        }
        Window.getWindows().addListener((ListChangeListener<Window>) cambio -> {
            while (cambio.next()) {
                for (Window w : cambio.getAddedSubList()) {
                    vigilar(w);
                }
            }
        });
    }

    private static void vigilar(Window w) {
        ponerIcono(w);
        aplicar(w.getScene());
        w.sceneProperty().addListener((obs, anterior, escena) -> aplicar(escena));
    }

    private static void aplicar(Scene escena) {
        if (escena == null) {
            return;
        }
        aplicar(escena.getRoot());
        if (escena.getProperties().putIfAbsent("estiloApp.vigilada", Boolean.TRUE) == null) {
            escena.rootProperty().addListener((obs, anterior, raiz) -> aplicar(raiz));
        }
    }

    /**
     * Deja la hoja de la paleta como la última de la raíz, para que sus
     * colores ganen sobre los valores base de paleta-base.css.
     */
    public static void aplicar(Parent raiz) {
        if (raiz == null) {
            return;
        }
        String hoja = hojaPaleta();
        raiz.getStylesheets().removeIf(s -> s.contains(CARPETA_PALETAS));
        if (hoja != null) {
            raiz.getStylesheets().add(hoja);
        }
    }

    // ===================== ÍCONO =====================

    private static List<Image> iconos;

    /** Ícono del sistema (barra de título y de tareas) en todas las ventanas. */
    private static void ponerIcono(Window w) {
        if (!(w instanceof Stage stage) || !stage.getIcons().isEmpty()) {
            return;
        }
        if (iconos == null) {
            iconos = new ArrayList<>();
            for (int t : new int[]{16, 24, 32, 48, 64, 128, 256}) {
                URL url = EstiloApp.class.getResource("/imagenes/icono-" + t + ".png");
                if (url != null) {
                    iconos.add(new Image(url.toExternalForm()));
                }
            }
        }
        stage.getIcons().setAll(iconos);
    }

    // ===================== REINICIAR =====================

    /** True si el sistema se puede reiniciar solo (instalado con .exe o .jar). */
    public static boolean puedeReiniciar() {
        return ConfiguracionApp.ejecutable() != null || ConfiguracionApp.jarActual() != null;
    }

    /** Abre una nueva instancia del sistema y cierra esta. */
    public static void reiniciar() throws Exception {
        Path exe = ConfiguracionApp.ejecutable();
        ProcessBuilder pb;
        if (exe != null) {
            pb = new ProcessBuilder(exe.toString());
        } else if (ConfiguracionApp.jarActual() != null) {
            Path javaw = Paths.get(System.getProperty("java.home"), "bin", "javaw.exe");
            pb = new ProcessBuilder(javaw.toString(), "-jar", ConfiguracionApp.jarActual().toString());
        } else {
            throw new IllegalStateException("Desde el IDE: cerrá y volvé a ejecutar el sistema.");
        }
        pb.directory(ConfiguracionApp.carpetaInstalacion().toFile());
        pb.start();
        Platform.exit();
        System.exit(0);
    }
}
