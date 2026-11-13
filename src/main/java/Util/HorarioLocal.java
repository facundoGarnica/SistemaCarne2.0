package Util;

import java.time.LocalTime;

/**
 * Franjas horarias de atención del local, usadas para desglosar reportes
 * de ventas y productos entre turno mañana y turno tarde.
 */
public final class HorarioLocal {

    public static final LocalTime MANIANA_INICIO = LocalTime.of(8, 30);
    public static final LocalTime MANIANA_FIN = LocalTime.of(13, 0);
    public static final LocalTime TARDE_INICIO = LocalTime.of(17, 0);
    public static final LocalTime TARDE_FIN = LocalTime.of(21, 0);

    public static final String MANIANA = "Mañana";
    public static final String TARDE = "Tarde";
    public static final String FUERA_DE_HORARIO = "Fuera de horario";

    private HorarioLocal() {
    }

    public static String determinarFranja(LocalTime hora) {
        if (hora == null) {
            return FUERA_DE_HORARIO;
        }
        if (!hora.isBefore(MANIANA_INICIO) && !hora.isAfter(MANIANA_FIN)) {
            return MANIANA;
        }
        if (!hora.isBefore(TARDE_INICIO) && !hora.isAfter(TARDE_FIN)) {
            return TARDE;
        }
        return FUERA_DE_HORARIO;
    }
}
