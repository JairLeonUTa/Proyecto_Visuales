package proyecto_visuales;

import java.util.List;

/** Pruebas de ValidadorHorarios (no requieren base de datos). */
public class ValidadorHorariosTest {

    public static void main(String[] args) {
        probarMinimoDosDias();
        probarSoloLunesAViernes();
        probarHorariosPorDia();
        System.out.println("OK: pruebas de ValidadorHorarios superadas");
    }

    static void probarMinimoDosDias() {
        verificar(!ValidadorHorarios.diasValidos(null), "sin días no es válido");
        verificar(!ValidadorHorarios.diasValidos(List.of("MONDAY")), "un solo día no basta");
        verificar(!ValidadorHorarios.diasValidos(List.of("MONDAY", "MONDAY")), "repetir el mismo día no cuenta");
        verificar(ValidadorHorarios.diasValidos(List.of("MONDAY", "WEDNESDAY")), "dos días distintos son válidos");
    }

    static void probarSoloLunesAViernes() {
        verificar(ValidadorHorarios.esDiaLaborable("FRIDAY"), "el viernes es laborable");
        verificar(!ValidadorHorarios.esDiaLaborable("SATURDAY"), "el sábado no es laborable");
        verificar(!ValidadorHorarios.esDiaLaborable("SUNDAY"), "el domingo no es laborable");
        verificar(!ValidadorHorarios.diasValidos(List.of("MONDAY", "SATURDAY")), "no se aceptan fines de semana");
        verificar(ValidadorHorarios.DIAS_LABORABLES.size() == 5, "son cinco días laborables");
    }

    static void probarHorariosPorDia() {
        verificar(ValidadorHorarios.horarioValido("08:00", "10:00"), "08:00-10:00 es válido");
        verificar(!ValidadorHorarios.horarioValido("10:00", "08:00"), "el inicio va antes del fin");
        verificar(!ValidadorHorarios.horarioValido("ocho", "10:00"), "el formato HH:mm es obligatorio");
        verificar(ValidadorHorarios.horariosValidos(List.of(
                new HorarioMateria("MONDAY", "08:00", "10:00"),
                new HorarioMateria("WEDNESDAY", "13:00", "15:00"))), "cada día puede tener su propio horario");
        verificar(!ValidadorHorarios.horariosValidos(List.of(
                new HorarioMateria("MONDAY", "08:00", "10:00"),
                new HorarioMateria("WEDNESDAY", "15:00", "13:00"))), "un rango inválido en un día invalida todo");
        verificar(!ValidadorHorarios.horariosValidos(List.of(
                new HorarioMateria("MONDAY", "08:00", "10:00"),
                new HorarioMateria("MONDAY", "13:00", "15:00"))), "no se repite el mismo día");
    }

    private static void verificar(boolean condicion, String mensaje) {
        if (!condicion) {
            throw new AssertionError(mensaje);
        }
    }
}
