package proyecto_visuales;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

/** Acceso a materias y a sus horarios por día (tabla horarios_materia). */
public class MateriaDAO {

    private static String ultimoError = "";

    private MateriaDAO() {
    }

    /** Mensaje del último fallo; queda vacío cuando la operación termina bien. */
    public static String getUltimoError() {
        return ultimoError;
    }

    /** Guarda la materia y una fila en horarios_materia por cada día elegido. */
    public static boolean guardarMateria(String codigo, String nombre, List<HorarioMateria> horarios) {
        ultimoError = "";
        if (!ValidadorHorarios.horariosValidos(horarios)) {
            ultimoError = "La materia necesita al menos dos días con horarios válidos.";
            return false;
        }
        Connection cc = new conexion().conectar();
        if (cc == null) {
            ultimoError = "No hay conexión con MySQL.";
            return false;
        }
        try (cc) {
            int idMateria = insertarMateria(cc, codigo, nombre, horarios.get(0));
            insertarHorarios(cc, idMateria, horarios);
            return true;
        } catch (SQLException ex) {
            ultimoError = "Error al guardar la materia: " + ex.getMessage();
            return false;
        }
    }

    private static int insertarMateria(Connection cc, String codigo, String nombre, HorarioMateria primero)
            throws SQLException {
        try (PreparedStatement ps = cc.prepareStatement(
                "INSERT INTO materias(codigo,nombre,dia_semana,hora_inicio,hora_fin) VALUES (?,?,?,?,?)",
                Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, codigo);
            ps.setString(2, nombre);
            ps.setString(3, primero.diaSemana());
            ps.setString(4, primero.horaInicio());
            ps.setString(5, primero.horaFin());
            ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (!keys.next()) {
                    throw new SQLException("No se obtuvo el ID de la materia.");
                }
                return keys.getInt(1);
            }
        }
    }

    private static void insertarHorarios(Connection cc, int idMateria, List<HorarioMateria> horarios)
            throws SQLException {
        try (PreparedStatement ps = cc.prepareStatement(
                "INSERT INTO horarios_materia(id_materia,dia_semana,hora_inicio,hora_fin) VALUES (?,?,?,?)")) {
            for (HorarioMateria dia : horarios) {
                ps.setInt(1, idMateria);
                ps.setString(2, dia.diaSemana());
                ps.setString(3, dia.horaInicio());
                ps.setString(4, dia.horaFin());
                ps.addBatch();
            }
            ps.executeBatch();
        }
    }

    /** Lista las materias como filas {código, nombre, día, inicio, fin}. */
    public static List<Object[]> cargarMaterias() {
        ultimoError = "";
        List<Object[]> lista = new ArrayList<>();
        Connection cc = new conexion().conectar();
        if (cc == null) {
            ultimoError = "No hay conexión con MySQL.";
            return lista;
        }
        try (cc; Statement st = cc.createStatement();
             ResultSet rs = st.executeQuery("SELECT * FROM materias ORDER BY codigo")) {
            while (rs.next()) {
                lista.add(new Object[]{
                    rs.getString("codigo"),
                    rs.getString("nombre"),
                    rs.getString("dia_semana"),
                    rs.getString("hora_inicio"),
                    rs.getString("hora_fin")
                });
            }
        } catch (SQLException ex) {
            ultimoError = "Error al listar materias: " + ex.getMessage();
        }
        return lista;
    }
}
