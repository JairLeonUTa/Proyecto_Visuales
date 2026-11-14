package proyecto_visuales;

import java.util.List;

/** Pruebas de ValidadorHorarios (no requieren base de datos). */
public class ValidadorHorariosTest {

    public static void main(String[] args) {
        probarMinimoDosDias();
        System.out.println("OK: pruebas de ValidadorHorarios superadas");
    }

    static void probarMinimoDosDias() {
        verificar(!ValidadorHorarios.diasValidos(null), "sin días no es válido");
        verificar(!ValidadorHorarios.diasValidos(List.of("MONDAY")), "un solo día no basta");
        verificar(!ValidadorHorarios.diasValidos(List.of("MONDAY", "MONDAY")), "repetir el mismo día no cuenta");
        verificar(ValidadorHorarios.diasValidos(List.of("MONDAY", "WEDNESDAY")), "dos días distintos son válidos");
    }

    private static void verificar(boolean condicion, String mensaje) {
        if (!condicion) {
            throw new AssertionError(mensaje);
        }
    }
}
