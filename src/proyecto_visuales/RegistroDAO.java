package proyecto_visuales;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

public class RegistroDAO {

    // Método para registrar la asistencia de un estudiante en una materia
    public boolean registrarAsistencia(int idInscripcion, String fecha, String estado) {
        String sql = "INSERT INTO registros (id_inscripcion, fecha, estado) VALUES (?, ?, ?)";
        try (Connection con = ConexionBD.obtenerConexion();
             PreparedStatement ps = con.prepareStatement(sql)) {
            
            ps.setInt(1, idInscripcion);
            ps.setString(2, fecha);
            ps.setString(3, estado);
            
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            System.err.println("Error al registrar asistencia: " + e.getMessage());
            return false;
        }
    }

    // Método para consultar el porcentaje de asistencia de un estudiante
    public double obtenerPorcentajeAsistencia(int idInscripcion) {
        String sqlTotal = "SELECT COUNT(*) FROM registros WHERE id_inscripcion = ?";
        String sqlPresentes = "SELECT COUNT(*) FROM registros WHERE id_inscripcion = ? AND estado = 'PRESENTE'";
        
        int totalClases = 0;
        int totalPresentes = 0;
        
        try (Connection con = ConexionBD.obtenerConexion()) {
            try (PreparedStatement psTotal = con.prepareStatement(sqlTotal)) {
                psTotal.setInt(1, idInscripcion);
                ResultSet rsTotal = psTotal.executeQuery();
                if (rsTotal.next()) {
                    totalClases = rsTotal.getInt(1);
                }
            }
            
            if (totalClases == 0) return 0.0;
            
            try (PreparedStatement psPresentes = con.prepareStatement(sqlPresentes)) {
                psPresentes.setInt(1, idInscripcion);
                ResultSet rsPresentes = psPresentes.executeQuery();
                if (rsPresentes.next()) {
                    totalPresentes = rsPresentes.getInt(1);
                }
            }
            
            return ((double) totalPresentes / totalClases) * 100.0;
        } catch (SQLException e) {
            System.err.println("Error al calcular porcentaje de asistencia: " + e.getMessage());
            return 0.0;
        }
    }
}
