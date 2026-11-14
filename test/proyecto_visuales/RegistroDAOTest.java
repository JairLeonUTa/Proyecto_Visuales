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
            probarListado();
            probarPorcentajeSinRegistros();
            probarFlujoCompleto();
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

    static void probarListado() {
        reiniciar();
        String hoy = LocalDate.now().toString();
        verificar(RegistroDAO.guardarRegistro(MATRICULA, MATERIA, hoy, "08:00", false), "registro de prueba");
        List<Object[]> filas = RegistroDAO.listarRegistros(200);
        Object[] fila = filas.stream().filter(f -> MATRICULA.equals(f[1])).findFirst().orElse(null);
        verificar(fila != null, "el listado incluye el registro nuevo");
        verificar(fila.length == 7, "cada fila tiene las 7 columnas de la tabla");
        verificar("Prueba Registro".equals(fila[2]), "muestra el nombre completo del estudiante");
        verificar("No".equals(fila[6]), "muestra si llegó atrasado");
    }

    static void probarPorcentajeSinRegistros() {
        reiniciar();
        List<Object[]> filas = RegistroDAO.porcentajePorEstudiante(MATRICULA, 8);
        verificar(filas.size() == 1, "hay una fila por materia inscrita");
        verificar(((Number) filas.get(0)[1]).intValue() == 0, "sin registros no hay asistencias");
        verificar(((Number) filas.get(0)[3]).doubleValue() == 0.0, "sin registros el porcentaje es 0");
    }

    static void probarFlujoCompleto() {
        reiniciar();
        String hoy = LocalDate.now().toString();
        verificar(RegistroDAO.guardarRegistro(MATRICULA, MATERIA, hoy, "08:20", true), "registrar asistencia");
        verificar(!RegistroDAO.guardarRegistro("TEST-NADIE", MATERIA, hoy, "08:20", false),
                "no registra a un estudiante inexistente");
        verificar(RegistroDAO.listarRegistrosPorMatricula(MATRICULA, 10).size() == 1, "filtra por matrícula");
        verificar(RegistroDAO.listarRegistrosPorFecha(hoy, 500).stream().anyMatch(f -> MATRICULA.equals(f[1])),
                "filtra por fecha");
        Object[] porcentaje = RegistroDAO.porcentajePorEstudiante(MATRICULA, 8).get(0);
        verificar(((Number) porcentaje[1]).intValue() == 1, "cuenta la asistencia en el parcial");
        verificar(((Number) porcentaje[2]).intValue() == 16, "8 semanas x 2 días = 16 clases");
        verificar(Math.abs(((Number) porcentaje[3]).doubleValue() - 6.25) < 0.001, "1 de 16 clases = 6.25%");
        verificar("6%".equals(RegistroDAO.reporteAsistencia(MATRICULA).get(0)[3]), "el reporte formatea el porcentaje");
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
