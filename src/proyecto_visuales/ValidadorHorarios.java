package proyecto_visuales;

import java.util.List;

/** Reglas de los horarios de una materia. */
public final class ValidadorHorarios {

    private ValidadorHorarios() {
    }

    /** Una materia necesita al menos dos días distintos. */
    public static boolean diasValidos(List<String> dias) {
        return dias != null && dias.stream().distinct().count() >= 2;
    }
}
