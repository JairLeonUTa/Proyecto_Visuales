package proyecto_visuales;

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
}
