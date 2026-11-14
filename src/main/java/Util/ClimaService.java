package Util;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.prefs.Preferences;

/**
 * Clima actual para la pantalla de inicio, desde Open-Meteo (gratis, sin
 * registro ni clave). La ciudad se elige en Configuración > Estilo del sistema
 * y se guarda en esta PC.
 */
public final class ClimaService {

    /** Ciudad elegida: nombre para mostrar y coordenadas. */
    public record Ciudad(String nombre, String detalle, double lat, double lon) {
        public String texto() {
            return detalle == null || detalle.isBlank() ? nombre : nombre + ", " + detalle;
        }
    }

    /** Clima actual y del día. */
    public record Clima(double temperatura, double sensacion, int humedad, double viento,
                        int codigo, boolean esDeDia, double maxima, double minima, int probLluvia) {
    }

    private static final Preferences PREFS = Preferences.userRoot().node("sistemacarne/clima");
    private static final ObjectMapper JSON = new ObjectMapper();
    private static final HttpClient HTTP = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(10))
            .followRedirects(HttpClient.Redirect.NORMAL)
            .build();

    /** El último clima obtenido, para no consultar de nuevo cada vez que se abre el menú. */
    private static Clima ultimo;
    private static LocalDateTime ultimoMomento;
    private static Ciudad ultimaCiudad;

    private ClimaService() {
    }

    // ===================== CIUDAD =====================

    public static Ciudad ciudadGuardada() {
        String nombre = PREFS.get("nombre", null);
        if (nombre == null) {
            return null;
        }
        return new Ciudad(nombre, PREFS.get("detalle", ""), PREFS.getDouble("lat", 0), PREFS.getDouble("lon", 0));
    }

    public static void guardarCiudad(Ciudad c) {
        PREFS.put("nombre", c.nombre());
        PREFS.put("detalle", c.detalle() == null ? "" : c.detalle());
        PREFS.putDouble("lat", c.lat());
        PREFS.putDouble("lon", c.lon());
        ultimo = null; // que el inicio muestre la ciudad nueva
    }

    /** Busca ciudades por nombre (por ejemplo "Rosario"). */
    public static List<Ciudad> buscarCiudades(String nombre) throws IOException, InterruptedException {
        String url = "https://geocoding-api.open-meteo.com/v1/search?count=8&language=es&format=json&name="
                + URLEncoder.encode(nombre.trim(), StandardCharsets.UTF_8);
        JsonNode raiz = JSON.readTree(pedir(url));
        List<Ciudad> ciudades = new ArrayList<>();
        for (JsonNode r : raiz.path("results")) {
            List<String> partes = new ArrayList<>();
            for (String campo : new String[]{"admin1", "country"}) {
                String v = r.path(campo).asText("");
                if (!v.isBlank()) {
                    partes.add(v);
                }
            }
            ciudades.add(new Ciudad(r.path("name").asText(), String.join(", ", partes),
                    r.path("latitude").asDouble(), r.path("longitude").asDouble()));
        }
        return ciudades;
    }

    // ===================== CLIMA =====================

    /**
     * Clima actual de la ciudad. Reutiliza el último si tiene menos de 10
     * minutos (el menú se abre muchas veces por día).
     */
    public static synchronized Clima climaActual(Ciudad c) throws IOException, InterruptedException {
        if (ultimo != null && c.equals(ultimaCiudad) && ultimoMomento != null
                && ultimoMomento.isAfter(LocalDateTime.now().minusMinutes(10))) {
            return ultimo;
        }
        String url = String.format(Locale.ROOT,
                "https://api.open-meteo.com/v1/forecast?latitude=%.4f&longitude=%.4f"
                + "&current=temperature_2m,apparent_temperature,relative_humidity_2m,weather_code,wind_speed_10m,is_day"
                + "&daily=temperature_2m_max,temperature_2m_min,precipitation_probability_max"
                + "&timezone=auto&forecast_days=1",
                c.lat(), c.lon());
        JsonNode raiz = JSON.readTree(pedir(url));
        JsonNode actual = raiz.path("current");
        JsonNode diario = raiz.path("daily");
        Clima clima = new Clima(
                actual.path("temperature_2m").asDouble(),
                actual.path("apparent_temperature").asDouble(),
                actual.path("relative_humidity_2m").asInt(),
                actual.path("wind_speed_10m").asDouble(),
                actual.path("weather_code").asInt(),
                actual.path("is_day").asInt(1) == 1,
                diario.path("temperature_2m_max").path(0).asDouble(),
                diario.path("temperature_2m_min").path(0).asDouble(),
                diario.path("precipitation_probability_max").path(0).asInt());
        ultimo = clima;
        ultimaCiudad = c;
        ultimoMomento = LocalDateTime.now();
        return clima;
    }

    private static String pedir(String url) throws IOException, InterruptedException {
        HttpRequest req = HttpRequest.newBuilder(URI.create(url))
                .header("User-Agent", "SistemaCarne")
                .timeout(Duration.ofSeconds(15))
                .GET().build();
        HttpResponse<String> resp = HTTP.send(req, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
        if (resp.statusCode() != 200) {
            throw new IOException("El servicio del clima respondió " + resp.statusCode());
        }
        return resp.body();
    }

    // ===================== TEXTOS =====================

    /** Descripción del código de clima (códigos WMO que usa Open-Meteo). */
    public static String descripcion(int codigo) {
        return switch (codigo) {
            case 0 -> "Despejado";
            case 1 -> "Mayormente despejado";
            case 2 -> "Parcialmente nublado";
            case 3 -> "Nublado";
            case 45, 48 -> "Niebla";
            case 51, 53, 55 -> "Llovizna";
            case 56, 57 -> "Llovizna helada";
            case 61 -> "Lluvia débil";
            case 63 -> "Lluvia";
            case 65 -> "Lluvia fuerte";
            case 66, 67 -> "Lluvia helada";
            case 71, 73, 75, 77 -> "Nieve";
            case 80, 81 -> "Chaparrones";
            case 82 -> "Chaparrones fuertes";
            case 85, 86 -> "Nevadas";
            case 95 -> "Tormenta";
            case 96, 99 -> "Tormenta con granizo";
            default -> "—";
        };
    }

    /** Símbolo del clima (caracteres que Windows dibuja sin fuentes extra). */
    public static String icono(int codigo, boolean esDeDia) {
        if (codigo <= 1) {
            return esDeDia ? "☀" : "☾";
        }
        if (codigo == 2) {
            return esDeDia ? "⛅" : "☁";
        }
        if (codigo == 3 || codigo == 45 || codigo == 48) {
            return "☁";
        }
        if ((codigo >= 71 && codigo <= 77) || codigo == 85 || codigo == 86) {
            return "❄";
        }
        if (codigo >= 95) {
            return "⚡";
        }
        return "☂"; // llovizna, lluvia, chaparrones
    }
}
