package proyecto_visuales;

import java.sql.Connection;
import java.sql.PreparedStatement;

public class EstudianteDAOTest {

    public static void main(String[] args) {
        probarBusquedaPorMatricula();
        System.out.println("OK: pruebas de EstudianteDAO superadas");
    }

    static void probarBusquedaPorMatricula() {
        String matricula = "TEST-BUSCAR";
        borrar(matricula);
        ejecutar("insert into estudiantes(matricula,nombre,apellido,email) values (?,?,?,?)",
                matricula, "Ana", "Prueba", "ana@prueba.com");
        try {
            Estudiante est = EstudianteDAO.buscarEstudiantePorMatricula(matricula);
            verificar(est != null, "debe encontrar la matrícula insertada");
            verificar("Ana".equals(est.getNombre()) && "Prueba".equals(est.getApellido()),
                    "nombre y apellido llegan en campos separados");
        } finally {
            borrar(matricula);
        }
    }

    private static void verificar(boolean condicion, String mensaje) {
        if (!condicion) {
            throw new AssertionError(mensaje);
        }
    }

    private static void ejecutar(String sql, String... parametros) {
        try (Connection cc = new conexion().conectar();
             PreparedStatement pst = cc.prepareStatement(sql)) {
            for (int i = 0; i < parametros.length; i++) {
                pst.setString(i + 1, parametros[i]);
            }
            pst.executeUpdate();
        } catch (Exception ex) {
            throw new IllegalStateException("No se pudo ejecutar: " + sql, ex);
        }
    }

    private static void borrar(String matricula) {
        ejecutar("delete from estudiantes where matricula=?", matricula);
    }
}
