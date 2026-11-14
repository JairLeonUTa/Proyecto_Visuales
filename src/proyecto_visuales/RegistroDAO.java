package proyecto_visuales;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

/** Acceso a la tabla registros (asistencias). */
public class RegistroDAO {

    private static String ultimoError = "";

    private RegistroDAO() {
    }

    public static String getUltimoError() {
        return ultimoError;
    }

    public static boolean guardarRegistro(String matricula, String codigoMateria, String fecha,
            String horaEntrada, boolean atraso) {
        ultimoError = "";
        String sql = "INSERT INTO registros(id_estudiante, id_materia, fecha, hora_entrada, es_atraso) "
                + "SELECT e.id_estudiante, m.id_materia, ?, ?, ? FROM estudiantes e "
                + "JOIN materias m WHERE e.matricula = ? AND m.codigo = ?";
        try {
            Connection cc = new conexion().conectar();
            if (obtenerIdEstudiante(cc, matricula) == -1) {
                ultimoError = "Estudiante no registrado: " + matricula + ".";
                return false;
            }
            if (yaRegistrado(cc, matricula, codigoMateria, fecha)) {
                ultimoError = "Ya registró asistencia hoy en " + codigoMateria + ".";
                return false;
            }
            PreparedStatement pst = cc.prepareStatement(sql);
            pst.setString(1, fecha);
            pst.setString(2, horaEntrada);
            pst.setInt(3, atraso ? 1 : 0);
            pst.setString(4, matricula);
            pst.setString(5, codigoMateria);
            return pst.executeUpdate() > 0;
        } catch (SQLException ex) {
            ultimoError = ex.getErrorCode() == 1062
                    ? "Ya registró asistencia hoy en " + codigoMateria + "."
                    : "Error al registrar asistencia: " + ex.getMessage();
            return false;
        }
    }

    private static boolean yaRegistrado(Connection cc, String matricula, String codigoMateria, String fecha)
            throws SQLException {
        try (PreparedStatement ps = cc.prepareStatement(
                "SELECT 1 FROM registros r JOIN estudiantes e ON e.id_estudiante = r.id_estudiante "
                + "JOIN materias m ON m.id_materia = r.id_materia "
                + "WHERE e.matricula = ? AND m.codigo = ? AND r.fecha = ? LIMIT 1")) {
            ps.setString(1, matricula);
            ps.setString(2, codigoMateria);
            ps.setString(3, fecha);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next();
            }
        }
    }

    private static int obtenerIdEstudiante(Connection cc, String matricula) throws SQLException {
        try (PreparedStatement ps = cc.prepareStatement("SELECT id_estudiante FROM estudiantes WHERE matricula = ?")) {
            ps.setString(1, matricula);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? rs.getInt(1) : -1;
            }
        }
    }
}
