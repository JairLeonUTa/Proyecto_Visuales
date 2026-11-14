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
        Connection cc = new conexion().conectar();
        if (cc == null) {
            ultimoError = "No hay conexión con MySQL. Verifique que XAMPP esté encendido.";
            return false;
        }
        try (cc) {
            int idEst = obtenerIdEstudiante(cc, matricula);
            if (idEst == -1) {
                ultimoError = "Estudiante no registrado: " + matricula + ".";
                return false;
            }
            int idMat = obtenerIdMateria(cc, codigoMateria);
            if (idMat == -1) {
                ultimoError = "La materia " + codigoMateria + " no existe.";
                return false;
            }
            if (!estaInscrito(cc, idEst, idMat)) {
                ultimoError = "El estudiante " + matricula + " no está inscrito en " + codigoMateria + ".";
                return false;
            }
            if (yaRegistrado(cc, idEst, idMat, fecha)) {
                ultimoError = "Ya registró asistencia hoy en " + codigoMateria + ".";
                return false;
            }
            try (PreparedStatement pst = cc.prepareStatement(
                    "INSERT INTO registros(id_estudiante, id_materia, fecha, hora_entrada, es_atraso) VALUES (?,?,?,?,?)")) {
                pst.setInt(1, idEst);
                pst.setInt(2, idMat);
                pst.setString(3, fecha);
                pst.setString(4, horaEntrada);
                pst.setInt(5, atraso ? 1 : 0);
                return pst.executeUpdate() > 0;
            }
        } catch (SQLException ex) {
            ultimoError = ex.getErrorCode() == 1062
                    ? "Ya registró asistencia hoy en " + codigoMateria + "."
                    : "Error al registrar asistencia: " + ex.getMessage();
            return false;
        }
    }

    private static boolean yaRegistrado(Connection cc, int idEst, int idMat, String fecha) throws SQLException {
        try (PreparedStatement ps = cc.prepareStatement(
                "SELECT 1 FROM registros WHERE id_estudiante = ? AND id_materia = ? AND fecha = ? LIMIT 1")) {
            ps.setInt(1, idEst);
            ps.setInt(2, idMat);
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

    private static int obtenerIdMateria(Connection cc, String codigo) throws SQLException {
        try (PreparedStatement ps = cc.prepareStatement("SELECT id_materia FROM materias WHERE codigo = ?")) {
            ps.setString(1, codigo);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? rs.getInt(1) : -1;
            }
        }
    }

    private static boolean estaInscrito(Connection cc, int idEst, int idMat) throws SQLException {
        try (PreparedStatement ps = cc.prepareStatement(
                "SELECT 1 FROM inscripciones WHERE id_estudiante = ? AND id_materia = ?")) {
            ps.setInt(1, idEst);
            ps.setInt(2, idMat);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next();
            }
        }
    }
}
