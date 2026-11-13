package Util;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import dao.MercadoPagoTokenDAO;
import javafx.scene.control.Alert;
import model.MercadoPagoToken;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;

import java.util.ArrayList;
import java.util.List;

public class MercadoPagoApi {

    private static final String URL =
            "https://api.mercadopago.com/v1/payments/search?"
            + "sort=date_created&criteria=desc&limit=10";

    private List<DatosPagos> listaDatos;

    public MercadoPagoApi() {
        listaDatos = new ArrayList<>();
    }

    /*
     =========================================
     OBTENER TOKEN DESDE BASE DE DATOS
     =========================================
     */
    private String obtenerAccessToken() {

        try {

            MercadoPagoTokenDAO dao =
                    new MercadoPagoTokenDAO();

            MercadoPagoToken token =
                    dao.obtenerToken();

            if (token != null) {
                return token.getToken();
            }

        } catch (Exception e) {

            System.out.println(
                    "Error obteniendo token"
            );

            e.printStackTrace();

        }

        return null;
    }

    /*
     =========================================
     OBTENER USER_ID DESDE BASE DE DATOS
     =========================================
     */
    public String obtenerUserId() {

        try {

            MercadoPagoTokenDAO dao =
                    new MercadoPagoTokenDAO();

            MercadoPagoToken token =
                    dao.obtenerToken();

            if (token != null) {
                return token.getUserId();
            }

        } catch (Exception e) {

            System.out.println(
                    "Error obteniendo user_id"
            );

            e.printStackTrace();

        }

        return null;
    }

    /*
     =========================================
     OBTENER EXTERNAL_POS_ID DESDE BASE DE DATOS
     =========================================
     */
    public String obtenerExternalPosId() {

        try {

            MercadoPagoTokenDAO dao =
                    new MercadoPagoTokenDAO();

            MercadoPagoToken token =
                    dao.obtenerToken();

            if (token != null) {
                return token.getExternalPosId();
            }

        } catch (Exception e) {

            System.out.println(
                    "Error obteniendo external_pos_id"
            );

            e.printStackTrace();

        }

        return null;
    }

    /*
     =========================================
     OBTENER ULTIMOS PAGOS
     =========================================
     */
    public void obtenerUltimosPagos() {

        try {

            String accessToken =
                    obtenerAccessToken();

          if (accessToken == null
                || accessToken.isEmpty()) {

            Alert alert =
                    new Alert(Alert.AlertType.WARNING);
            DialogoUtil.aplicarEstilo(alert);

            alert.setTitle(
                    "Token no configurado"
            );

            alert.setHeaderText(
                    "No hay token de MercadoPago"
            );

            alert.setContentText(
                    "Debe configurar el token antes de consultar los pagos."
            );

            alert.showAndWait();

            return;
        }

            HttpResponse<String> response =
                    pedirUltimosPagos(accessToken);

            /*
             VALIDAR RESPUESTA HTTP
             */
            if (response.statusCode() != 200) {

                System.out.println(
                        "Error API: "
                        + response.statusCode()
                );

                System.out.println(
                        response.body()
                );

                return;
            }

            /*
             LIMPIAR LISTA
             */
            listaDatos.clear();

            /*
             PARSEAR JSON
             */
            ObjectMapper objectMapper =
                    new ObjectMapper();

            JsonNode rootNode =
                    objectMapper.readTree(
                            response.body()
                    );

            JsonNode results =
                    rootNode.path("results");

            if (results.isArray()
                    && results.size() > 0) {

                for (JsonNode payment : results) {

                    String cliente =
                            payment.path("payer")
                                   .path("email")
                                   .asText();

                    String fechaCompleta =
                            payment.path("date_created")
                                   .asText();

                    double monto =
                            payment.path(
                                    "transaction_amount"
                            ).asDouble();

                    String estado =
                            payment.path("status")
                                   .asText();

                    /*
                     CONVERTIR FECHA
                     */
                    OffsetDateTime offsetDateTime =
                            OffsetDateTime.parse(
                                    fechaCompleta
                            );

                    ZonedDateTime zonedDateTime =
                            offsetDateTime
                            .atZoneSameInstant(
                                    ZoneId.of(
                                            "America/Argentina/Buenos_Aires"
                                    )
                            );

                    LocalTime hora =
                            zonedDateTime.toLocalTime();

                    String horaMinuto =
                            hora.format(
                                    DateTimeFormatter
                                    .ofPattern("HH:mm")
                            );

                    /*
                     AGREGAR A LISTA
                     */
                    long id = payment.path("id").asLong();
                    listaDatos.add(
                            new DatosPagos(
                                    id,
                                    cliente,
                                    horaMinuto,
                                    monto,
                                    estado
                            )
                    );

                }

            }

        } catch (Exception e) {

            System.out.println(
                    "Error obteniendo pagos"
            );

            e.printStackTrace();

        }

    }

    /*
     =========================================
     CONSULTAR ULTIMOS PAGOS (SIN ALERTAS)
     =========================================
     Igual que obtenerUltimosPagos(), pero pensado para correr en un hilo
     en segundo plano (la espera automática de transferencias): no muestra
     ningún Alert y devuelve el array "results" crudo de Mercado Pago. Si
     falla, lanza excepción para que el que llama decida si reintentar.
     IllegalStateException = no hay token configurado (no tiene sentido
     reintentar).
     */
    public JsonNode consultarUltimosPagos() throws Exception {

        String accessToken =
                obtenerAccessToken();

        if (accessToken == null
                || accessToken.isEmpty()) {
            throw new IllegalStateException(
                    "No hay token de MercadoPago configurado"
            );
        }

        HttpResponse<String> response =
                pedirUltimosPagos(accessToken);

        if (response.statusCode() != 200) {
            throw new RuntimeException(
                    "Error API: " + response.statusCode()
                    + " - " + response.body()
            );
        }

        return new ObjectMapper()
                .readTree(response.body())
                .path("results");
    }

    private HttpResponse<String> pedirUltimosPagos(String accessToken) throws Exception {

        HttpRequest request =
                HttpRequest.newBuilder()
                .uri(URI.create(URL))
                .header(
                        "Authorization",
                        "Bearer " + accessToken
                )
                .header(
                        "Content-Type",
                        "application/json"
                )
                .GET()
                .build();

        return HttpClient.newHttpClient().send(
                request,
                HttpResponse.BodyHandlers
                        .ofString()
        );
    }

    /*
     =========================================
     GETTER LISTA
     =========================================
     */
    public List<DatosPagos> getListaDatos() {
        return listaDatos;
    }

}