package proyecto_visuales;

import java.net.HttpURLConnection;
import java.net.URI;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;

public final class Reloj {

    private static final ZoneId ZONA = ZoneId.of("America/Guayaquil");
    private static volatile Instant base;
    private static volatile long nanoBase;

    private Reloj() {
    }

    public static boolean sincronizar() {
        try {
            HttpURLConnection con = (HttpURLConnection) URI.create("https://www.google.com").toURL().openConnection();
            con.setRequestMethod("HEAD");
            con.setConnectTimeout(3000);
            con.setReadTimeout(3000);
            long fecha = con.getHeaderFieldDate("Date", 0);
            con.disconnect();
            if (fecha > 0) {
                nanoBase = System.nanoTime();
                base = Instant.ofEpochMilli(fecha);
                return true;
            }
        } catch (Exception ex) {
            System.err.println("No se pudo obtener la hora de internet: " + ex.getMessage());
        }
        return false;
    }

    public static boolean usaHoraDeInternet() {
        return base != null;
    }

    public static LocalDateTime ahora() {
        Instant inicio = base;
        if (inicio == null) {
            return LocalDateTime.now(ZONA);
        }
        return LocalDateTime.ofInstant(inicio.plusNanos(System.nanoTime() - nanoBase), ZONA);
    }

    public static LocalDate hoy() {
        return ahora().toLocalDate();
    }

    public static LocalTime hora() {
        return ahora().toLocalTime();
    }
}
