/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package proyecto_visuales;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import javax.swing.JOptionPane;

/** Operaciones de inscripción estudiante-materia. */
public class InscripcionDAO {

    public static boolean inscribir(String matricula, String codigoMateria) {
        String sql = "INSERT IGNORE INTO inscripciones(id_estudiante, id_materia) "
                + "SELECT e.id_estudiante, m.id_materia FROM estudiantes e "
                + "JOIN materias m WHERE e.matricula=? AND m.codigo=?";
        try {
            Connection cc = new conexion().conectar();
            if (cc == null) return false;
            try (cc; PreparedStatement ps = cc.prepareStatement(sql)) {
                ps.setString(1, matricula);
                ps.setString(2, codigoMateria);
                return ps.executeUpdate() > 0;
            }
        } catch (SQLException ex) {
            JOptionPane.showMessageDialog(null, "No se pudo inscribir: " + ex.getMessage());
            return false;
        }
    }
}