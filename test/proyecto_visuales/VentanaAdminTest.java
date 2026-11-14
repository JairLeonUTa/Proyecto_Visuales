package proyecto_visuales;

import java.util.Arrays;
import java.util.List;

/** Validaciones que usa la ventana de administración al crear materias (sin base de datos). */
public class VentanaAdminTest {

    public static void main(String[] args) {
        verificar(ValidadorHorarios.horarioValido("08:00", "10:00"), "rango válido");
        verificar(!ValidadorHorarios.horarioValido("10:00", "08:00"), "inicio después del fin");
        verificar(!ValidadorHorarios.horarioValido("ocho", "10:00"), "hora sin formato HH:mm");
        verificar(!ValidadorHorarios.diasValidos(List.of("MONDAY")), "un solo día");
        verificar(ValidadorHorarios.diasValidos(List.of("MONDAY", "WEDNESDAY")), "dos días");
        verificar(!ValidadorHorarios.diasValidos(List.of("MONDAY", "MONDAY")), "día repetido");
        verificar(!ValidadorHorarios.horariosValidos(List.of(
                new HorarioMateria("SATURDAY", "08:00", "10:00"),
                new HorarioMateria("WEDNESDAY", "13:00", "15:00"))), "sábado no permitido");
        verificar(!ValidadorHorarios.horariosValidos(Arrays.asList(null,
                new HorarioMateria("WEDNESDAY", "13:00", "15:00"))), "horario nulo");

        verificar(SelectorHorarios.validar(List.of(
                new HorarioMateria("MONDAY", "08:00", "10:00"),
                new HorarioMateria("FRIDAY", "13:00", "15:00"))) == null, "el selector acepta horarios válidos");
        verificar(SelectorHorarios.validar(List.of(
                new HorarioMateria("MONDAY", "08:00", "10:00"))).contains("al menos dos"),
                "el selector pide dos días");
        verificar(SelectorHorarios.validar(List.of(
                new HorarioMateria("MONDAY", "08:00", "10:00"),
                new HorarioMateria("FRIDAY", "15:00", "13:00"))).contains("FRIDAY"),
                "el selector indica qué día tiene el rango inválido");
        System.out.println("OK: validaciones de la ventana de administración superadas");
    }

    private static void verificar(boolean condicion, String mensaje) {
        if (!condicion) {
            throw new AssertionError(mensaje);
        }
    }
}
