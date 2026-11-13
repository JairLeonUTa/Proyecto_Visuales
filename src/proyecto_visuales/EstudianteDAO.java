package proyecto_visuales;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

public class EstudianteDAO {

    private EstudianteDAO() {
    }

    private static Estudiante mapear(ResultSet rs) throws SQLException {
        Estudiante est = new Estudiante();
        est.setMatricula(rs.getString("matricula"));
        est.setNombre(rs.getString("nombre"));
        est.setApellido(rs.getString("apellido"));
        est.setEmail(rs.getString("email"));
        return est;
    }

    public static Estudiante buscarEstudiantePorMatricula(String matricula) {
        try {
            Connection cc = new conexion().conectar();
            PreparedStatement pst = cc.prepareStatement("select * from estudiantes where matricula=?");
            pst.setString(1, matricula);
            ResultSet rs = pst.executeQuery();
            if (rs.next()) {
                return mapear(rs);
            }
        } catch (SQLException ex) {
            System.err.println("Error al buscar estudiante: " + ex.getMessage());
        }
        return null;
    }
}
