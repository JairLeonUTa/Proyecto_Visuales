package proyecto_visuales;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Types;
import java.util.ArrayList;
import java.util.List;

/**
 * Acceso a la tabla estudiantes: guardar, buscar, listar, actualizar y eliminar.
 * Todas las operaciones usan consultas preparadas y no muestran ventanas: si algo
 * falla devuelven false, null o una lista vacía y el motivo queda en
 * {@link #getUltimoError()} para que la interfaz lo muestre.
 */
public class EstudianteDAO {

    private static String ultimoError = "";

    private EstudianteDAO() {
    }

    /** Mensaje del último fallo; queda vacío cuando la operación termina bien. */
    public static String getUltimoError() {
        return ultimoError;
    }

    /** Abre la conexión o lanza una excepción clara si MySQL no está disponible. */
    private static Connection abrirConexion() throws SQLException {
        Connection cc = new conexion().conectar();
        if (cc == null) {
            throw new SQLException("No hay conexión con MySQL. Verifique que XAMPP esté encendido.");
        }
        return cc;
    }

    private static Estudiante mapear(ResultSet rs) throws SQLException {
        Estudiante est = new Estudiante();
        est.setMatricula(rs.getString("matricula"));
        est.setNombre(rs.getString("nombre"));
        est.setApellido(rs.getString("apellido"));
        String email = rs.getString("email");
        est.setEmail(email == null ? "" : email);
        return est;
    }

    /** Busca por matrícula; devuelve null si no existe o si la consulta falló. */
    public static Estudiante buscarEstudiantePorMatricula(String matricula) {
        ultimoError = "";
        try (Connection cc = abrirConexion();
             PreparedStatement pst = cc.prepareStatement("select * from estudiantes where matricula=?")) {
            pst.setString(1, matricula);
            try (ResultSet rs = pst.executeQuery()) {
                if (rs.next()) {
                    return mapear(rs);
                }
            }
        } catch (SQLException ex) {
            ultimoError = "Error al buscar estudiante: " + ex.getMessage();
        }
        return null;
    }

    /** Lista todos los estudiantes ordenados por apellido y nombre. */
    public static List<Estudiante> cargarEstudiantes() {
        List<Estudiante> lista = new ArrayList<>();
        ultimoError = "";
        try (Connection cc = abrirConexion();
             Statement st = cc.createStatement();
             ResultSet rs = st.executeQuery("select * from estudiantes order by apellido, nombre")) {
            while (rs.next()) {
                lista.add(mapear(rs));
            }
        } catch (SQLException ex) {
            ultimoError = "Error al listar estudiantes: " + ex.getMessage();
        }
        return lista;
    }

    /**
     * Guarda un estudiante nuevo. Matrícula, nombre y apellido son obligatorios;
     * devuelve false si faltan datos, si la matrícula ya existe o si falla MySQL.
     */
    public static boolean guardarEstudiante(Estudiante estudiante) {
        ultimoError = "";
        if (estudiante == null || vacio(estudiante.getMatricula())
                || vacio(estudiante.getNombre()) || vacio(estudiante.getApellido())) {
            ultimoError = "Matrícula, nombre y apellido son obligatorios.";
            return false;
        }
        String sql = "insert into estudiantes(matricula,nombre,apellido,email) values (?,?,?,?)";
        try (Connection cc = abrirConexion();
             PreparedStatement pst = cc.prepareStatement(sql)) {
            pst.setString(1, estudiante.getMatricula().trim());
            pst.setString(2, estudiante.getNombre().trim());
            pst.setString(3, estudiante.getApellido().trim());
            asignarEmail(pst, 4, estudiante.getEmail());
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

    /** Actualiza nombre, apellido y email del estudiante con esa matrícula. */
    public static boolean actualizarEstudiante(Estudiante est) {
        ultimoError = "";
        if (est == null || vacio(est.getNombre()) || vacio(est.getApellido())) {
            ultimoError = "Nombre y apellido son obligatorios.";
            return false;
        }
        String sql = "update estudiantes set nombre=?, apellido=?, email=? where matricula=?";
        try (Connection cc = abrirConexion();
             PreparedStatement pst = cc.prepareStatement(sql)) {
            pst.setString(1, est.getNombre().trim());
            pst.setString(2, est.getApellido().trim());
            asignarEmail(pst, 3, est.getEmail());
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

    /** Elimina al estudiante; sus inscripciones se borran en cascada. */
    public static boolean eliminarEstudiante(String matricula) {
        ultimoError = "";
        try (Connection cc = abrirConexion();
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

    /** Guarda NULL cuando el email viene vacío, porque es un dato opcional. */
    private static void asignarEmail(PreparedStatement pst, int indice, String email) throws SQLException {
        if (vacio(email)) {
            pst.setNull(indice, Types.VARCHAR);
        } else {
            pst.setString(indice, email.trim());
        }
    }
}
