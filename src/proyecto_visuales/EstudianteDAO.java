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
}
