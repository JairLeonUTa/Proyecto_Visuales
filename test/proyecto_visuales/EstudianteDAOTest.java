package proyecto_visuales;

import java.sql.Connection;
import java.sql.PreparedStatement;

public class EstudianteDAOTest {

    public static void main(String[] args) {
        probarBusquedaPorMatricula();
        probarActualizacion();
        probarEliminacion();
        probarMatriculaInexistente();
        probarCrudCompleto();
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

    static void probarActualizacion() {
        String matricula = "TEST-ACTUALIZAR";
        borrar(matricula);
        verificar(EstudianteDAO.guardarEstudiante(new Estudiante(matricula, "Luis", "Viejo", "")),
                "debe guardar el estudiante de prueba");
        try {
            verificar(EstudianteDAO.actualizarEstudiante(
                    new Estudiante(matricula, "Luis", "Nuevo", "luis@prueba.com")), "debe actualizar");
            Estudiante est = EstudianteDAO.buscarEstudiantePorMatricula(matricula);
            verificar("Nuevo".equals(est.getApellido()), "el apellido queda actualizado");
            verificar("luis@prueba.com".equals(est.getEmail()), "el email queda actualizado");
        } finally {
            borrar(matricula);
        }
    }

    static void probarEliminacion() {
        String matricula = "TEST-ELIMINAR";
        borrar(matricula);
        verificar(EstudianteDAO.guardarEstudiante(new Estudiante(matricula, "Eva", "Borrar", "")),
                "debe guardar el estudiante de prueba");
        verificar(EstudianteDAO.eliminarEstudiante(matricula), "debe eliminar");
        verificar(EstudianteDAO.buscarEstudiantePorMatricula(matricula) == null, "ya no debe existir");
    }

    static void probarMatriculaInexistente() {
        String matricula = "TEST-NO-EXISTE";
        borrar(matricula);
        verificar(EstudianteDAO.buscarEstudiantePorMatricula(matricula) == null, "buscar devuelve null");
        verificar(!EstudianteDAO.actualizarEstudiante(new Estudiante(matricula, "X", "Y", "")),
                "actualizar debe fallar");
        verificar(!EstudianteDAO.eliminarEstudiante(matricula), "eliminar debe fallar");
        verificar(!EstudianteDAO.getUltimoError().isEmpty(), "debe explicar por qué falló");
    }

    static void probarCrudCompleto() {
        String matricula = "TEST-CRUD";
        borrar(matricula);
        int antes = EstudianteDAO.cargarEstudiantes().size();
        verificar(EstudianteDAO.guardarEstudiante(new Estudiante(matricula, "Carla", "Ruiz", "")), "crear");
        verificar(!EstudianteDAO.guardarEstudiante(new Estudiante(matricula, "Otra", "Persona", "")),
                "la matrícula es única");
        verificar(EstudianteDAO.getUltimoError().contains("ya está registrada"), "avisa la matrícula duplicada");
        verificar(!EstudianteDAO.guardarEstudiante(new Estudiante("TEST-SIN-NOMBRE", "", "Ruiz", "")),
                "el nombre es obligatorio");
        verificar(EstudianteDAO.cargarEstudiantes().size() == antes + 1, "listar incluye al nuevo");
        verificar("".equals(EstudianteDAO.buscarEstudiantePorMatricula(matricula).getEmail()),
                "el email vacío es opcional");
        verificar(EstudianteDAO.actualizarEstudiante(new Estudiante(matricula, "Carla", "Ruiz Mora", "c@p.com")),
                "actualizar");
        verificar(EstudianteDAO.eliminarEstudiante(matricula), "eliminar");
        verificar(EstudianteDAO.cargarEstudiantes().size() == antes, "listar ya no lo incluye");
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
