package proyecto_visuales;

import java.time.LocalTime;
import java.util.List;

/** Reglas de los horarios de una materia. */
public final class ValidadorHorarios {

    /** Días que se pueden elegir para una materia, de lunes a viernes. */
    public static final List<String> DIAS_LABORABLES =
            List.of("MONDAY", "TUESDAY", "WEDNESDAY", "THURSDAY", "FRIDAY");

    private ValidadorHorarios() {
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

    /** Cada día elegido debe ser laborable, no repetirse y tener su propio rango válido. */
    public static boolean horariosValidos(List<HorarioMateria> horarios) {
        return horarios != null && horarios.stream().allMatch(h -> h != null)
                && diasValidos(horarios.stream().map(HorarioMateria::diaSemana).toList())
                && horarios.stream().allMatch(h -> horarioValido(h.horaInicio(), h.horaFin()));
    }
}
