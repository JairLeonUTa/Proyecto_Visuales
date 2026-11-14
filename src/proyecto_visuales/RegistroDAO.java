package proyecto_visuales;

import java.sql.Connection;
import java.sql.PreparedStatement;
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
            PreparedStatement pst = cc.prepareStatement(sql);
            pst.setString(1, fecha);
            pst.setString(2, horaEntrada);
            pst.setInt(3, atraso ? 1 : 0);
            pst.setString(4, matricula);
            pst.setString(5, codigoMateria);
            return pst.executeUpdate() > 0;
        } catch (SQLException ex) {
            ultimoError = "Error al registrar asistencia: " + ex.getMessage();
            return false;
        }
    }
}
