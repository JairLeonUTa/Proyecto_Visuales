package proyecto_visuales;

import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/** Reglas de los horarios de una materia. */
public final class ValidadorHorarios {

    /** Días que se pueden elegir para una materia, de lunes a viernes. */
    public static final List<String> DIAS_LABORABLES =
            List.of("MONDAY", "TUESDAY", "WEDNESDAY", "THURSDAY", "FRIDAY");

    public static final List<String> HORAS_CLASE = horasClase();

    private static final DateTimeFormatter AM_PM = DateTimeFormatter.ofPattern("h:mm a", Locale.US);

    private ValidadorHorarios() {
    }

    private static List<String> horasClase() {
        List<String> horas = new ArrayList<>();
        for (LocalTime t = LocalTime.of(7, 0); !t.isAfter(LocalTime.of(20, 0)); t = t.plusMinutes(30)) {
            horas.add(t.toString());
        }
        return List.copyOf(horas);
    }

    public static String horaAmPm(String hora) {
        try {
            return LocalTime.parse(normalizarHora(hora).substring(0, 5)).format(AM_PM);
        } catch (RuntimeException ex) {
            return hora;
        }
    }

    /** Una materia necesita al menos dos días distintos, todos de lunes a viernes. */
    public static boolean diasValidos(List<String> dias) {
        return dias != null && dias.size() >= 2
                && dias.stream().allMatch(ValidadorHorarios::esDiaLaborable)
                && dias.stream().distinct().count() == dias.size();
    }

    public static boolean esDiaLaborable(String dia) {
        return dia != null && DIAS_LABORABLES.contains(dia);
    }

    /** El rango es válido si ambas horas usan HH:mm y el inicio es anterior al fin. */
    public static boolean horarioValido(String inicio, String fin) {
        try {
            return LocalTime.parse(inicio).isBefore(LocalTime.parse(fin));
        } catch (RuntimeException ex) {
            return false;
        }
    }

    public static String normalizarHora(String texto) {
        String hora = texto == null ? "" : texto.trim();
        if (hora.matches("\\d:\\d{2}")) {
            return "0" + hora;
        }
        return hora;
    }

    /** Cada día elegido debe ser laborable, no repetirse y tener su propio rango válido. */
    public static boolean horariosValidos(List<HorarioMateria> horarios) {
        return horarios != null && horarios.stream().allMatch(h -> h != null)
                && diasValidos(horarios.stream().map(HorarioMateria::diaSemana).toList())
                && horarios.stream().allMatch(h -> horarioValido(h.horaInicio(), h.horaFin()));
    }
}
