package proyecto_visuales;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

public class EstudianteDAO {

    private static String ultimoError = "";

    private EstudianteDAO() {
    }

    public static String getUltimoError() {
        return ultimoError;
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

    public static List<Estudiante> cargarEstudiantes() {
        List<Estudiante> lista = new ArrayList<>();
        try (Connection cc = new conexion().conectar();
             Statement st = cc.createStatement();
             ResultSet rs = st.executeQuery("select * from estudiantes order by apellido, nombre")) {
            while (rs.next()) {
                lista.add(mapear(rs));
            }
        } catch (SQLException ex) {
            System.err.println("Error al listar estudiantes: " + ex.getMessage());
        }
        return lista;
    }

    public static boolean guardarEstudiante(Estudiante estudiante) {
        ultimoError = "";
        if (estudiante == null || vacio(estudiante.getMatricula())
                || vacio(estudiante.getNombre()) || vacio(estudiante.getApellido())) {
            ultimoError = "Matrícula, nombre y apellido son obligatorios.";
            return false;
        }
        String sql = "insert into estudiantes(matricula,nombre,apellido,email) values (?,?,?,?)";
        try (Connection cc = new conexion().conectar();
             PreparedStatement pst = cc.prepareStatement(sql)) {
            pst.setString(1, estudiante.getMatricula().trim());
            pst.setString(2, estudiante.getNombre().trim());
            pst.setString(3, estudiante.getApellido().trim());
            pst.setString(4, estudiante.getEmail());
            return pst.executeUpdate() > 0;
        } catch (SQLException ex) {
            if (ex.getErrorCode() == 1062) {
                ultimoError = "La matrícula " + estudiante.getMatricula().trim() + " ya está registrada.";
            } else {
                ultimoError = "Error al guardar estudiante: " + ex.getMessage();
            }
            return false;
        }
    }

    public static boolean actualizarEstudiante(Estudiante est) {
        ultimoError = "";
        if (est == null || vacio(est.getNombre()) || vacio(est.getApellido())) {
            ultimoError = "Nombre y apellido son obligatorios.";
            return false;
        }
        String sql = "update estudiantes set nombre=?, apellido=?, email=? where matricula=?";
        try {
            Connection cc = new conexion().conectar();
            PreparedStatement pst = cc.prepareStatement(sql);
            pst.setString(1, est.getNombre().trim());
            pst.setString(2, est.getApellido().trim());
            pst.setString(3, est.getEmail());
            pst.setString(4, est.getMatricula());
            int n = pst.executeUpdate();
            if (n == 0) {
                ultimoError = "No existe un estudiante con matrícula " + est.getMatricula() + ".";
            }
            return n > 0;
        } catch (SQLException ex) {
            ultimoError = "Error al actualizar estudiante: " + ex.getMessage();
            return false;
        }
    }

    public static boolean eliminarEstudiante(String matricula) {
        ultimoError = "";
        try (Connection cc = new conexion().conectar();
             PreparedStatement pst = cc.prepareStatement("delete from estudiantes where matricula=?")) {
            pst.setString(1, matricula);
            int n = pst.executeUpdate();
            if (n == 0) {
                ultimoError = "No existe un estudiante con matrícula " + matricula + ".";
            }
            return n > 0;
        } catch (SQLException ex) {
            ultimoError = "Error al eliminar estudiante: " + ex.getMessage();
            return false;
        }
    }

    private static boolean vacio(String texto) {
        return texto == null || texto.trim().isEmpty();
    }
}
