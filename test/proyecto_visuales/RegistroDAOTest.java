package proyecto_visuales;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.time.LocalDate;
import java.util.List;

/**
 * Pruebas de RegistroDAO contra la base local "proyecto" (requiere XAMPP encendido).
 * Crea un estudiante TEST-REG y una materia TEST-MAT, y los borra al terminar.
 */
public class RegistroDAOTest {

    private static final String MATRICULA = "TEST-REG";
    private static final String MATERIA = "TEST-MAT";

    public static void main(String[] args) {
        try {
            probarRechazoDuplicado();
        } finally {
            limpiar();
        }
        System.out.println("OK: pruebas de RegistroDAO superadas");
    }

    static void probarRechazoDuplicado() {
        reiniciar();
        String hoy = LocalDate.now().toString();
        verificar(RegistroDAO.guardarRegistro(MATRICULA, MATERIA, hoy, "08:05", true),
                "el primer registro del día se guarda");
        verificar(!RegistroDAO.guardarRegistro(MATRICULA, MATERIA, hoy, "08:10", true),
                "el segundo registro del mismo día se rechaza");
        verificar(RegistroDAO.getUltimoError().contains("Ya registró"), "explica que ya marcó asistencia");
    }

    /** Deja un estudiante inscrito en una materia de dos días y sin registros. */
    private static void reiniciar() {
        limpiar();
        verificar(EstudianteDAO.guardarEstudiante(new Estudiante(MATRICULA, "Prueba", "Registro", "")),
                "crear estudiante de prueba");
        verificar(MateriaDAO.guardarMateria(MATERIA, "Materia de prueba", List.of(
                new HorarioMateria("MONDAY", "08:00", "10:00"),
                new HorarioMateria("WEDNESDAY", "08:00", "10:00"))), "crear materia de prueba");
        verificar(InscripcionDAO.inscribir(MATRICULA, MATERIA), "inscribir estudiante de prueba");
    }

    private static void limpiar() {
        ejecutar("DELETE r FROM registros r JOIN estudiantes e ON e.id_estudiante = r.id_estudiante "
                + "WHERE e.matricula = ?", MATRICULA);
        ejecutar("DELETE FROM estudiantes WHERE matricula = ?", MATRICULA);
        ejecutar("DELETE FROM materias WHERE codigo = ?", MATERIA);
    }

    private static void ejecutar(String sql, String parametro) {
        try (Connection cc = new conexion().conectar();
             PreparedStatement pst = cc.prepareStatement(sql)) {
            pst.setString(1, parametro);
            pst.executeUpdate();
        } catch (Exception ex) {
            throw new IllegalStateException("No se pudo ejecutar: " + sql, ex);
        }
    }

    private static void verificar(boolean condicion, String mensaje) {
        if (!condicion) {
            throw new AssertionError(mensaje);
        }
    }
}
