package Util;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import dao.MercadoPagoTokenDAO;
import model.MercadoPagoToken;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.Locale;
import java.util.UUID;

/**
 * Genera cobros sobre el QR estático ya impreso en el mostrador, usando la
 * "Orders API" de Mercado Pago (POST /v1/orders, type: "qr", mode: "static").
 *
 * Requiere que en la tabla mercadoPagoToken ya estén cargados: token
 * (access token) y el External POS ID de cada caja del sistema que cobre con
 * QR (el external_id de la caja de Mercado Pago asociada al QR físico
 * impreso). Esos datos se cargan desde MercadoPagoTokenController /
 * TokenMercadoPago.fxml.
 *
 * IMPORTANTE: una order solo puede cancelarse mientras su status es
 * "created". Si ya fue pagada (u otro estado), la API responde con un
 * conflicto: no llamar a cancelarCobroDinamico sobre una order que ya
 * se dio por cobrada.
 */
public class MercadoPagoQRService {

    private static final String BASE_URL = "https://api.mercadopago.com";

    private final HttpClient httpClient;
    private final ObjectMapper objectMapper;
    private final MercadoPagoTokenDAO tokenDAO;

    public MercadoPagoQRService() {
        this.httpClient = HttpClient.newHttpClient();
        this.objectMapper = new ObjectMapper();
        this.tokenDAO = new MercadoPagoTokenDAO();
    }

    /*
     =========================================
     CARGAR Y VALIDAR CONFIGURACIÓN
     =========================================
     */
    private MercadoPagoToken cargarConfiguracion() {

        MercadoPagoToken config = tokenDAO.obtenerToken();

        if (config == null
                || config.getToken() == null || config.getToken().isBlank()) {
            throw new IllegalStateException(
                    "No hay un access token de MercadoPago configurado. "
                    + "Cargalo desde la pantalla de configuración de MercadoPago."
            );
        }

        return config;
    }

    /** External POS ID (QR impreso) asignado a la caja indicada (1, 2 o 3). */
    private String obtenerExternalPosId(MercadoPagoToken config, int numeroCaja) {

        String externalPosId = config.getExternalPosIdCaja(numeroCaja);

        if (externalPosId == null || externalPosId.isBlank()) {
            throw new IllegalStateException(
                    "La Caja " + numeroCaja + " no tiene un QR asignado (External POS ID). "
                    + "Asignalo en Configuración de Mercado Pago → Cajas y QR."
            );
        }

        return externalPosId;
    }

    private String formatearMonto(double monto) {
        return String.format(Locale.US, "%.2f", monto);
    }

    /*
     =========================================
     GENERAR COBRO SOBRE EL QR ESTÁTICO
     =========================================
     Crea una order tipo "qr" en modo "static": el monto queda asociado a
     la caja (external_pos_id de la caja del sistema indicada), por lo que
     el QR físico ya impreso de esa caja pasa a cobrar ese importe. Devuelve
     el id de la order generada (necesario para consultar el estado del
     pago o cancelarla).
     */
    public String generarCobroDinamico(
            double monto,
            String descripcion,
            String externalReference,
            int numeroCaja
    ) {

        if (monto <= 0) {
            throw new IllegalArgumentException(
                    "El monto a cobrar debe ser mayor a 0"
            );
        }

        if (descripcion == null || descripcion.isBlank()) {
            throw new IllegalArgumentException(
                    "La descripción del cobro es obligatoria"
            );
        }

        if (externalReference == null || externalReference.isBlank()) {
            throw new IllegalArgumentException(
                    "La referencia externa (external_reference) es obligatoria"
            );
        }

        MercadoPagoToken config = cargarConfiguracion();
        String externalPosId = obtenerExternalPosId(config, numeroCaja);

        String url = BASE_URL + "/v1/orders";

        String montoFormateado = formatearMonto(monto);

        ObjectNode qrConfig = objectMapper.createObjectNode();
        qrConfig.put("external_pos_id", externalPosId);
        qrConfig.put("mode", "static");

        ObjectNode configNode = objectMapper.createObjectNode();
        configNode.set("qr", qrConfig);

        ObjectNode pago = objectMapper.createObjectNode();
        pago.put("amount", montoFormateado);

        ArrayNode payments = objectMapper.createArrayNode();
        payments.add(pago);

        ObjectNode transactions = objectMapper.createObjectNode();
        transactions.set("payments", payments);

        ObjectNode body = objectMapper.createObjectNode();
        body.put("type", "qr");
        body.put("total_amount", montoFormateado);
        body.put("description", descripcion);
        body.put("external_reference", externalReference);
        body.set("config", configNode);
        body.set("transactions", transactions);

        try {

            String jsonBody = objectMapper.writeValueAsString(body);

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .header("Authorization", "Bearer " + config.getToken())
                    .header("Content-Type", "application/json")
                    .header("X-Idempotency-Key", UUID.randomUUID().toString())
                    .POST(HttpRequest.BodyPublishers.ofString(
                            jsonBody, StandardCharsets.UTF_8
                    ))
                    .build();

            HttpResponse<String> response = httpClient.send(
                    request, HttpResponse.BodyHandlers.ofString()
            );

            if (!esRespuestaExitosa(response.statusCode())) {
                throw new RuntimeException(
                        "Error al generar el cobro QR en MercadoPago. Código HTTP: "
                        + response.statusCode() + ". Respuesta: " + response.body()
                );
            }

            JsonNode root = objectMapper.readTree(response.body());

            return root.path("id").asText();

        } catch (IOException | InterruptedException e) {

            if (e instanceof InterruptedException) {
                Thread.currentThread().interrupt();
            }

            throw new RuntimeException(
                    "No se pudo comunicar con la API de MercadoPago para generar el cobro QR",
                    e
            );
        }
    }

    /*
     =========================================
     CONSULTAR ESTADO DE LA ORDER
     =========================================
     Se usa para hacer polling desde la pantalla de venta hasta que el
     pago se acredite. Devuelve el campo "status" de la order (ej.:
     "created" mientras espera el pago). Confirmar contra la referencia
     de "Recursos" de Mercado Pago el valor exacto que indica pago
     acreditado antes de usarlo para dar por cobrada una venta real.
     */
    public String consultarEstadoPago(String orderId) {
        return consultarOrden(orderId).path("status").asText();
    }

    /*
     =========================================
     CONSULTAR LA ORDER COMPLETA
     =========================================
     Igual que consultarEstadoPago(), pero devuelve el JSON completo de la
     order en lugar de solo el status. Se usa cuando además del estado hace
     falta el monto realmente acreditado (ver obtenerMontoPagado()), para
     verificar contra Mercado Pago que lo cobrado coincide con lo esperado
     antes de dar la venta por confirmada.
     */
    public JsonNode consultarOrden(String orderId) {

        if (orderId == null || orderId.isBlank()) {
            throw new IllegalArgumentException(
                    "El id de la order es obligatorio"
            );
        }

        MercadoPagoToken config = cargarConfiguracion();

        String url = BASE_URL + "/v1/orders/" + orderId;

        try {

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .header("Authorization", "Bearer " + config.getToken())
                    .header("Content-Type", "application/json")
                    .GET()
                    .build();

            HttpResponse<String> response = httpClient.send(
                    request, HttpResponse.BodyHandlers.ofString()
            );

            if (!esRespuestaExitosa(response.statusCode())) {
                throw new RuntimeException(
                        "Error al consultar el estado de la order en MercadoPago. Código HTTP: "
                        + response.statusCode() + ". Respuesta: " + response.body()
                );
            }

            return objectMapper.readTree(response.body());

        } catch (IOException | InterruptedException e) {

            if (e instanceof InterruptedException) {
                Thread.currentThread().interrupt();
            }

            throw new RuntimeException(
                    "No se pudo comunicar con la API de MercadoPago para consultar la order",
                    e
            );
        }
    }

    /*
     =========================================
     MONTO REALMENTE ACREDITADO
     =========================================
     Extrae de la order el monto efectivamente pagado (no el que se pidió
     cobrar), para poder verificar contra Mercado Pago que la venta se cobró
     por el importe correcto antes de mostrarla como confirmada. Prioriza
     el monto por pago dentro de "transactions.payments" (paid_amount si
     está presente, o amount) y, si no hay transacciones, cae al
     total_amount de la order.
     */
    public double obtenerMontoPagado(JsonNode orden) {

        if (orden == null) {
            return 0.0;
        }

        double total = 0.0;
        JsonNode payments = orden.path("transactions").path("payments");

        if (payments.isArray() && payments.size() > 0) {
            for (JsonNode pago : payments) {
                JsonNode montoNode = pago.hasNonNull("paid_amount")
                        ? pago.path("paid_amount")
                        : pago.path("amount");
                total += parseMontoSeguro(montoNode);
            }
            return total;
        }

        return parseMontoSeguro(orden.path("total_amount"));
    }

    private double parseMontoSeguro(JsonNode montoNode) {
        try {
            if (montoNode == null || montoNode.isMissingNode() || montoNode.isNull()) {
                return 0.0;
            }
            return Double.parseDouble(montoNode.asText());
        } catch (NumberFormatException e) {
            return 0.0;
        }
    }

    /*
     =========================================
     CANCELAR COBRO
     =========================================
     Cancela la order mientras no fue pagada, para que el QR impreso no
     quede mostrando un monto que ya no corresponde. Ver advertencia en
     el Javadoc de la clase sobre este endpoint.
     */
    public void cancelarCobroDinamico(String orderId) {

        if (orderId == null || orderId.isBlank()) {
            return;
        }

        MercadoPagoToken config = cargarConfiguracion();

        String url = BASE_URL + "/v1/orders/" + orderId + "/cancel";

        try {

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .header("Authorization", "Bearer " + config.getToken())
                    .header("Content-Type", "application/json")
                    .header("X-Idempotency-Key", UUID.randomUUID().toString())
                    .POST(HttpRequest.BodyPublishers.noBody())
                    .build();

            HttpResponse<String> response = httpClient.send(
                    request, HttpResponse.BodyHandlers.ofString()
            );

            if (!esRespuestaExitosa(response.statusCode())) {
                throw new RuntimeException(
                        "Error al cancelar el cobro QR en MercadoPago. Código HTTP: "
                        + response.statusCode() + ". Respuesta: " + response.body()
                );
            }

        } catch (IOException | InterruptedException e) {

            if (e instanceof InterruptedException) {
                Thread.currentThread().interrupt();
            }

            throw new RuntimeException(
                    "No se pudo comunicar con la API de MercadoPago para cancelar el cobro QR",
                    e
            );
        }
    }

    /*
     =========================================
     REEMBOLSAR COBRO (TOTAL)
     =========================================
     Reembolsa el total de una order ya pagada. Disponible hasta 180 días
     después del pago. Para reembolso parcial habría que enviar body con
     amount + transaction_id, no contemplado acá porque no aplica al caso
     de uso actual (venta de mostrador cobrada por completo).
     */
    public void reembolsarCobro(String orderId) {

        if (orderId == null || orderId.isBlank()) {
            throw new IllegalArgumentException(
                    "El id de la order es obligatorio"
            );
        }

        MercadoPagoToken config = cargarConfiguracion();

        String url = BASE_URL + "/v1/orders/" + orderId + "/refund";

        try {

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .header("Authorization", "Bearer " + config.getToken())
                    .header("Content-Type", "application/json")
                    .header("X-Idempotency-Key", UUID.randomUUID().toString())
                    .POST(HttpRequest.BodyPublishers.noBody())
                    .build();

            HttpResponse<String> response = httpClient.send(
                    request, HttpResponse.BodyHandlers.ofString()
            );

            if (!esRespuestaExitosa(response.statusCode())) {
                throw new RuntimeException(
                        "Error al reembolsar el cobro QR en MercadoPago. Código HTTP: "
                        + response.statusCode() + ". Respuesta: " + response.body()
                );
            }

        } catch (IOException | InterruptedException e) {

            if (e instanceof InterruptedException) {
                Thread.currentThread().interrupt();
            }

            throw new RuntimeException(
                    "No se pudo comunicar con la API de MercadoPago para reembolsar el cobro QR",
                    e
            );
        }
    }

    private boolean esRespuestaExitosa(int statusCode) {
        return statusCode >= 200 && statusCode < 300;
    }

    /*
     =========================================
     VERIFICAR SI EL PAGO YA FUE ACREDITADO
     =========================================
     Según la Orders API, una order tipo "qr" pasa a status "processed"
     cuando el pago fue capturado con éxito. Se usa junto con
     consultarEstadoPago() para el polling desde la pantalla de venta.
     */
    public boolean esPagoAprobado(String status) {
        return "processed".equalsIgnoreCase(status);
    }

}
