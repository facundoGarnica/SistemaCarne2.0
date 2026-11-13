package Util;

import Dto.CajaMercadoPagoDTO;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import dao.MercadoPagoTokenDAO;
import model.MercadoPagoToken;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Consulta y modifica las cajas (POS) de la cuenta de Mercado Pago, cada una
 * con su QR. Solo necesita el access token (no el external_pos_id).
 *
 *  - Listar:  GET   /v2/pos?limit=&offset=   (paginado, máx. 30 por página)
 *  - Editar:  PATCH /v2/pos/{id}             (requiere X-Idempotency-Key)
 *
 * Se usan los endpoints v2 porque las credenciales de la Orders API (con
 * las que se cobra) no tienen permiso sobre el GET /pos legacy: responde
 * 403 "At least one policy returned UNAUTHORIZED".
 */
public class MercadoPagoPosService {

    private static final String BASE_URL = "https://api.mercadopago.com";
    private static final int TAMANIO_PAGINA = 30;
    private static final DateTimeFormatter FORMATO_FECHA = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    private final HttpClient httpClient;
    private final ObjectMapper objectMapper;
    private final MercadoPagoTokenDAO tokenDAO;

    public MercadoPagoPosService() {
        this.httpClient = HttpClient.newHttpClient();
        this.objectMapper = new ObjectMapper();
        this.tokenDAO = new MercadoPagoTokenDAO();
    }

    private String obtenerAccessToken() {

        MercadoPagoToken config = tokenDAO.obtenerToken();

        if (config == null || config.getToken() == null || config.getToken().isBlank()) {
            throw new IllegalStateException(
                    "No hay un access token de Mercado Pago configurado. "
                    + "Cargalo en la pestaña Configuración."
            );
        }

        return config.getToken();
    }

    /*
     =========================================
     LISTAR TODAS LAS CAJAS DE LA CUENTA
     =========================================
     */
    public List<CajaMercadoPagoDTO> listarCajas() {

        String token = obtenerAccessToken();
        List<CajaMercadoPagoDTO> cajas = new ArrayList<>();
        int offset = 0;

        while (true) {

            String url = BASE_URL + "/v2/pos?limit=" + TAMANIO_PAGINA + "&offset=" + offset;

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .header("Authorization", "Bearer " + token)
                    .GET()
                    .build();

            JsonNode root = enviar(request, "consultar las cajas");

            JsonNode resultados = root.has("data") ? root.path("data") : root.path("results");
            for (JsonNode pos : resultados) {
                cajas.add(convertir(pos));
            }

            int total = root.path("paging").path("total").asInt(cajas.size());
            offset += TAMANIO_PAGINA;

            if (resultados.size() == 0 || offset >= total) {
                break;
            }
        }

        return cajas;
    }

    /*
     =========================================
     CAMBIAR EL EXTERNAL ID DE UNA CAJA
     =========================================
     El external_id solo admite letras y números (máx. 40). Se envía también
     el store_id actual para que la caja siga en la misma sucursal.
     */
    public void actualizarExternalId(CajaMercadoPagoDTO caja, String nuevoExternalId) {

        String token = obtenerAccessToken();

        ObjectNode body = objectMapper.createObjectNode();
        body.put("external_id", nuevoExternalId);
        if (caja.getStoreId() != null && !caja.getStoreId().isBlank()) {
            body.put("store_id", caja.getStoreId());
        }

        try {

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(BASE_URL + "/v2/pos/" + caja.getId()))
                    .header("Authorization", "Bearer " + token)
                    .header("Content-Type", "application/json")
                    .header("X-Idempotency-Key", UUID.randomUUID().toString())
                    .method("PATCH", HttpRequest.BodyPublishers.ofString(
                            objectMapper.writeValueAsString(body), StandardCharsets.UTF_8
                    ))
                    .build();

            enviar(request, "actualizar la caja");

        } catch (IOException e) {
            throw new RuntimeException("No se pudo armar la solicitud a Mercado Pago", e);
        }
    }

    public static boolean esExternalIdValido(String externalId) {
        return externalId != null && externalId.matches("[A-Za-z0-9]{1,40}");
    }

    private JsonNode enviar(HttpRequest request, String accion) {

        try {

            HttpResponse<String> response = httpClient.send(
                    request, HttpResponse.BodyHandlers.ofString()
            );

            if (response.statusCode() < 200 || response.statusCode() >= 300) {
                throw new RuntimeException(
                        "Mercado Pago rechazó la solicitud para " + accion
                        + " (código " + response.statusCode() + "): "
                        + extraerMensajeError(response.body())
                );
            }

            return objectMapper.readTree(response.body());

        } catch (IOException | InterruptedException e) {

            if (e instanceof InterruptedException) {
                Thread.currentThread().interrupt();
            }

            throw new RuntimeException(
                    "No se pudo comunicar con Mercado Pago para " + accion
                    + ". Revisá la conexión a internet.", e
            );
        }
    }

    private String extraerMensajeError(String cuerpo) {
        try {
            JsonNode error = objectMapper.readTree(cuerpo);
            String mensaje = error.path("message").asText("");
            if (mensaje.isBlank() && error.path("errors").isArray() && error.path("errors").size() > 0) {
                mensaje = error.path("errors").get(0).path("message").asText("");
            }
            return mensaje.isBlank() ? cuerpo : mensaje;
        } catch (IOException e) {
            return cuerpo;
        }
    }

    private CajaMercadoPagoDTO convertir(JsonNode pos) {

        CajaMercadoPagoDTO caja = new CajaMercadoPagoDTO();
        caja.setId(texto(pos, "id"));
        caja.setNombre(texto(pos, "name"));
        caja.setExternalId(texto(pos, "external_id"));
        caja.setStoreId(texto(pos, "store_id"));
        caja.setExternalStoreId(texto(pos, "external_store_id"));
        caja.setEstado(texto(pos, "status"));
        caja.setFechaCreacion(formatearFecha(texto(pos, "date_created")));
        caja.setFechaActualizacion(formatearFecha(texto(pos, "date_last_updated")));

        // v2 trae categoría y modo dentro de config.qr; el formato viejo, en la raíz
        JsonNode configQr = pos.path("config").path("qr");
        caja.setCategoria(texto(configQr, "category") != null ? texto(configQr, "category") : texto(pos, "category"));
        caja.setModoOperacion(texto(configQr, "operating_mode"));

        // v2 trae los datos del QR en "qr_response"; el formato viejo, en "qr"
        JsonNode qr = pos.has("qr_response") ? pos.path("qr_response") : pos.path("qr");
        caja.setUuid(texto(qr, "uuid") != null ? texto(qr, "uuid") : texto(pos, "uuid"));
        caja.setQrImagen(texto(qr, "image"));
        caja.setQrPdf(texto(qr, "template_document"));
        caja.setQrPlantillaImagen(texto(qr, "template_image"));

        return caja;
    }

    private String texto(JsonNode nodo, String campo) {
        JsonNode valor = nodo.path(campo);
        return valor.isMissingNode() || valor.isNull() ? null : valor.asText();
    }

    private String formatearFecha(String iso) {
        if (iso == null || iso.isBlank()) {
            return null;
        }
        try {
            return OffsetDateTime.parse(iso).format(FORMATO_FECHA);
        } catch (Exception e) {
            return iso;
        }
    }
}
