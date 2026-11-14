package proyecto_visuales;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

/**
 * Asistencias: registra la entrada de un estudiante a una materia (una sola vez por
 * día), lista los registros para la tabla de la ventana y calcula el porcentaje de
 * asistencia del parcial de ocho semanas. No muestra ventanas: si algo falla, el motivo
 * queda en {@link #getUltimoError()}.
 */
public class RegistroDAO {

    /** Duración de cada parcial en semanas. */
    public static final int SEMANAS_POR_PARCIAL = 8;

    /** Columnas de las filas que devuelve {@link #reporteAsistencia(String)}. */
    public static final String[] COLUMNAS_REPORTE = {"Materia", "Asistencias", "Clases del parcial", "Porcentaje"};

    private static final String SELECT_REGISTROS =
            "SELECT r.id_registro, e.matricula, e.nombre, e.apellido, m.codigo, m.nombre AS materia, "
            + "r.fecha, r.hora_entrada, r.es_atraso FROM registros r "
            + "JOIN estudiantes e ON e.id_estudiante = r.id_estudiante "
            + "JOIN materias m ON m.id_materia = r.id_materia ";

    private static String ultimoError = "";

    private RegistroDAO() {
    }

    /** Mensaje del último fallo; queda vacío cuando la operación termina bien. */
    public static String getUltimoError() {
        return ultimoError;
    }

    /**
     * Registra la asistencia. Verifica que el estudiante exista, que esté inscrito en la
     * materia y que no haya marcado ya ese día.
     * @param fecha fecha en formato yyyy-MM-dd
     * @param horaEntrada hora de llegada HH:mm
     * @param atraso true si llegó después de la hora de inicio de la clase
     */
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

    /**
     * Últimos registros, del más reciente al más antiguo, como filas para la tabla:
     * {#, matrícula, estudiante, materia, fecha, hora, atraso}.
     */
    public static List<Object[]> listarRegistros(int limite) {
        return consultarRegistros("", null, limite);
    }

    /** Registros de un solo estudiante, con las mismas columnas que {@link #listarRegistros(int)}. */
    public static List<Object[]> listarRegistrosPorMatricula(String matricula, int limite) {
        return consultarRegistros("WHERE e.matricula = ? ", matricula, limite);
    }

    /** Registros de una fecha (yyyy-MM-dd), con las mismas columnas que {@link #listarRegistros(int)}. */
    public static List<Object[]> listarRegistrosPorFecha(String fecha, int limite) {
        return consultarRegistros("WHERE r.fecha = ? ", fecha, limite);
    }

    private static List<Object[]> consultarRegistros(String filtro, String valor, int limite) {
        ultimoError = "";
        List<Object[]> lista = new ArrayList<>();
        Connection cc = new conexion().conectar();
        if (cc == null) {
            ultimoError = "No hay conexión con MySQL.";
            return lista;
        }
        String sql = SELECT_REGISTROS + filtro + "ORDER BY r.id_registro DESC LIMIT ?";
        try (cc; PreparedStatement pst = cc.prepareStatement(sql)) {
            int i = 1;
            if (valor != null) {
                pst.setString(i++, valor);
            }
            pst.setInt(i, limite);
            try (ResultSet rs = pst.executeQuery()) {
                while (rs.next()) {
                    lista.add(new Object[]{
                        rs.getInt("id_registro"),
                        rs.getString("matricula"),
                        rs.getString("nombre") + " " + rs.getString("apellido"),
                        rs.getString("codigo") + " - " + rs.getString("materia"),
                        rs.getString("fecha"),
                        rs.getString("hora_entrada"),
                        rs.getInt("es_atraso") == 1 ? "Sí" : "No"
                    });
                }
            }
        } catch (SQLException ex) {
            ultimoError = "Error al listar registros: " + ex.getMessage();
        }
        return lista;
    }

    /**
     * Porcentaje de asistencia del estudiante en cada materia inscrita, considerando
     * el parcial actual (máximo ocho semanas). Clases del parcial = semanas x días de
     * clase por semana de la materia. Filas {materia, asistencias, clases, porcentaje}.
     */
    public static List<Object[]> porcentajePorEstudiante(String matricula, int semanasPorParcial) {
        ultimoError = "";
        List<Object[]> lista = new ArrayList<>();
        int semanas = Math.max(1, Math.min(semanasPorParcial, SEMANAS_POR_PARCIAL));
        String sql = "SELECT m.codigo, m.nombre, "
                + "(SELECT COUNT(*) FROM horarios_materia h WHERE h.id_materia = m.id_materia) AS dias, "
                + "(SELECT COUNT(*) FROM registros r WHERE r.id_materia = m.id_materia "
                + "AND r.id_estudiante = e.id_estudiante "
                + "AND r.fecha > DATE_SUB(CURDATE(), INTERVAL ? DAY)) AS asistidas "
                + "FROM inscripciones i JOIN estudiantes e ON e.id_estudiante = i.id_estudiante "
                + "JOIN materias m ON m.id_materia = i.id_materia "
                + "WHERE e.matricula = ? ORDER BY m.codigo";
        Connection cc = new conexion().conectar();
        if (cc == null) {
            ultimoError = "No hay conexión con MySQL.";
            return lista;
        }
        try (cc; PreparedStatement pst = cc.prepareStatement(sql)) {
            pst.setInt(1, semanas * 7);
            pst.setString(2, matricula);
            try (ResultSet rs = pst.executeQuery()) {
                while (rs.next()) {
                    int asistidas = rs.getInt("asistidas");
                    int clases = semanas * rs.getInt("dias");
                    double porcentaje = clases == 0 ? 0.0 : Math.min(100.0, asistidas * 100.0 / clases);
                    lista.add(new Object[]{
                        rs.getString("codigo") + " - " + rs.getString("nombre"),
                        asistidas,
                        clases,
                        porcentaje
                    });
                }
            }
        } catch (SQLException ex) {
            ultimoError = "Error al calcular el porcentaje: " + ex.getMessage();
        }
        return lista;
    }

    /** Filas listas para una JTable con {@link #COLUMNAS_REPORTE}; el porcentaje va formateado. */
    public static List<Object[]> reporteAsistencia(String matricula) {
        List<Object[]> filas = new ArrayList<>();
        for (Object[] fila : porcentajePorEstudiante(matricula, SEMANAS_POR_PARCIAL)) {
            filas.add(new Object[]{fila[0], fila[1], fila[2], String.format("%.0f%%", (Double) fila[3])});
        }
        return filas;
    }
}
