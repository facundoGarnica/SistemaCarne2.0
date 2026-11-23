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

    /**
     * Como determinarFranja, pero sin "fuera de horario": una venta fuera de
     * los dos horarios va al turno más cercano. Cuenta la distancia dando la
     * vuelta al reloj (las 01:00 están más cerca de las 21:00 que de las 08:30).
     */
    public static String franjaMasCercana(LocalTime hora) {
        if (hora == null) {
            return MANIANA;
        }
        String franja = determinarFranja(hora);
        if (!FUERA_DE_HORARIO.equals(franja)) {
            return franja;
        }
        int m = hora.toSecondOfDay() / 60;
        int aManiana = Math.min(distancia(m, MANIANA_INICIO), distancia(m, MANIANA_FIN));
        int aTarde = Math.min(distancia(m, TARDE_INICIO), distancia(m, TARDE_FIN));
        return aManiana <= aTarde ? MANIANA : TARDE;
    }

    /** Minutos entre dos horas, por el camino más corto del reloj. */
    private static int distancia(int minutos, LocalTime limite) {
        int d = Math.abs(minutos - limite.toSecondOfDay() / 60);
        return Math.min(d, 24 * 60 - d);
    }
}
