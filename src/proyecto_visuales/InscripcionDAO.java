package proyecto_visuales;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

/** Operaciones de inscripción estudiante-materia. */
public class InscripcionDAO {

    private static String ultimoError = "";

    private InscripcionDAO() {
    }

    /** Mensaje del último fallo; queda vacío cuando la operación termina bien. */
    public static String getUltimoError() {
        return ultimoError;
    }

    /** Indica si el estudiante ya está inscrito en la materia. */
    public static boolean estaInscrito(String matricula, String codigoMateria) {
        ultimoError = "";
        String sql = "SELECT 1 FROM inscripciones i "
                + "JOIN estudiantes e ON e.id_estudiante = i.id_estudiante "
                + "JOIN materias m ON m.id_materia = i.id_materia "
                + "WHERE e.matricula = ? AND m.codigo = ?";
        Connection cc = new conexion().conectar();
        if (cc == null) {
            ultimoError = "No hay conexión con MySQL.";
            return false;
        }
        try (cc; PreparedStatement ps = cc.prepareStatement(sql)) {
            ps.setString(1, matricula);
            ps.setString(2, codigoMateria);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next();
            }
        } catch (SQLException ex) {
            ultimoError = "Error al consultar la inscripción: " + ex.getMessage();
            return false;
        }
    }
}
