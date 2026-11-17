package proyecto_visuales;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.util.List;

public class HorarioSemanalTest {

    private static final String ESTUDIANTE = "9999990020";
    private static final String[] MATERIAS = {"TST-BD", "TST-CV", "TST-MA", "TST-MCS", "TST-RED"};

    public static void main(String[] args) {
        limpiar();
        try {
            crearHorarioUniversidad();
            probarClasesSeguidas();
            probarHorasDeLaSemana();
        } finally {
            limpiar();
        }
        System.out.println("OK: horario semanal con cinco materias y clases seguidas");
    }

    static void crearHorarioUniversidad() {
        guardar("TST-BD", "Base de Datos", List.of(
                new HorarioMateria("MONDAY", "14:00", "16:00"),
                new HorarioMateria("TUESDAY", "17:00", "19:00"),
                new HorarioMateria("THURSDAY", "18:00", "20:00")));
        guardar("TST-CV", "Computación Visual", List.of(
                new HorarioMateria("TUESDAY", "14:00", "17:00"),
                new HorarioMateria("THURSDAY", "14:00", "16:00")));
        guardar("TST-MA", "Metodologías Ágiles", List.of(
                new HorarioMateria("MONDAY", "16:00", "18:00"),
                new HorarioMateria("WEDNESDAY", "14:00", "17:00")));
        guardar("TST-MCS", "Manejo y Configuración del Software", List.of(
                new HorarioMateria("WEDNESDAY", "17:00", "20:00"),
                new HorarioMateria("FRIDAY", "14:00", "16:00")));
        guardar("TST-RED", "Redes", List.of(
                new HorarioMateria("MONDAY", "18:00", "20:00"),
                new HorarioMateria("THURSDAY", "16:00", "18:00"),
                new HorarioMateria("FRIDAY", "16:00", "18:00")));
        guardar("TST-MAN", "Materia de la mañana", List.of(
                new HorarioMateria("TUESDAY", "07:00", "09:00"),
                new HorarioMateria("THURSDAY", "07:00", "09:00")));
        verificar(EstudianteDAO.guardarEstudiante(new Estudiante(ESTUDIANTE, "Ana", "Horario", "")), "crear estudiante");
        for (String codigo : MATERIAS) {
            verificar(InscripcionDAO.inscribir(ESTUDIANTE, codigo), "inscribir en " + codigo);
        }
        verificar(InscripcionDAO.inscribir(ESTUDIANTE, "TST-MAN"), "inscribir en la materia de la mañana");
        verificar(MateriaDAO.horarioDeEstudiante(ESTUDIANTE).size() == 14, "el horario semanal tiene 14 bloques");
    }

    static void probarClasesSeguidas() {
        verificar("TST-BD".equals(materiaEn("MONDAY", "15:59")), "lunes 15:59 sigue Base de Datos");
        verificar("TST-MA".equals(materiaEn("MONDAY", "16:00")), "lunes 16:00 empieza Metodologías");
        verificar("TST-RED".equals(materiaEn("MONDAY", "18:00")), "lunes 18:00 empieza Redes");
        verificar("TST-BD".equals(materiaEn("TUESDAY", "17:03")), "martes 17:03 es Base de Datos");
        verificar("TST-MCS".equals(materiaEn("WEDNESDAY", "17:00")), "miércoles 17:00 empieza Manejo del Software");
        verificar("TST-RED".equals(materiaEn("THURSDAY", "16:00")), "jueves 16:00 empieza Redes");
        verificar("TST-BD".equals(materiaEn("THURSDAY", "18:00")), "jueves 18:00 empieza Base de Datos");
        verificar("TST-RED".equals(materiaEn("FRIDAY", "16:00")), "viernes 16:00 empieza Redes");
    }

    static void probarHorasDeLaSemana() {
        verificar("TST-MAN".equals(materiaEn("TUESDAY", "07:00")), "se puede tener clase desde las 07:00");
        verificar(materiaEn("TUESDAY", "09:00") == null, "a las 09:00 ya terminó la clase de la mañana");
        verificar("TST-BD".equals(materiaEn("THURSDAY", "19:59")), "se puede tener clase hasta las 20:00");
        verificar(materiaEn("THURSDAY", "20:00") == null, "a las 20:00 ya no hay clase");
        verificar(materiaEn("FRIDAY", "13:59") == null, "antes de las 14:00 del viernes no hay clase");
    }

    private static String materiaEn(String dia, String hora) {
        List<Object[]> materias = MateriaDAO.materiasEnHorario(ESTUDIANTE, dia, hora);
        return materias.isEmpty() ? null : String.valueOf(materias.get(0)[0]);
    }

    private static void guardar(String codigo, String nombre, List<HorarioMateria> horarios) {
        verificar(MateriaDAO.guardarMateria(codigo, nombre, horarios), "guardar " + codigo + ": " + MateriaDAO.getUltimoError());
    }

    private static void limpiar() {
        ejecutar("DELETE FROM registros WHERE id_estudiante = "
                + "(SELECT id_estudiante FROM estudiantes WHERE matricula = ?)", ESTUDIANTE);
        ejecutar("DELETE FROM estudiantes WHERE matricula = ?", ESTUDIANTE);
        for (String codigo : MATERIAS) {
            ejecutar("DELETE FROM materias WHERE codigo = ?", codigo);
        }
        ejecutar("DELETE FROM materias WHERE codigo = ?", "TST-MAN");
    }

    private static void ejecutar(String sql, String valor) {
        try (Connection cc = new conexion().conectar(); PreparedStatement ps = cc.prepareStatement(sql)) {
            ps.setString(1, valor);
            ps.executeUpdate();
        } catch (Exception ex) {
            throw new IllegalStateException("No se pudo limpiar la prueba", ex);
        }
    }

    private static void verificar(boolean condicion, String mensaje) {
        if (!condicion) {
            throw new AssertionError(mensaje);
        }
    }
}
