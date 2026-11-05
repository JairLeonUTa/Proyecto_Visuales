package Proyecto_Visuales;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import javax.swing.JOptionPane;
import proyecto_visuales.conexion;

public class MateriaDAO {

    public static boolean guardarMateria(String codigo, String nombre, String diaSemana, String horaInicio, String horaFin) {
        try {
            conexion cn =new conexion();
            Connection cc = cn.conectar();
            String sql = "insert into materias(codigo,nombre,dia_semana,hora_inicio,hora_fin) values (?,?,?,?,?)";
            PreparedStatement pst = cc.prepareStatement(sql);
            pst.setString(1, codigo);
            pst.setString(2, nombre);
            pst.setString(3, diaSemana);
            pst.setString(4, horaInicio);
            pst.setString(5, horaFin);
            int n = pst.executeUpdate();
            if (n > 0) return true;
        } catch (SQLException ex) {
            JOptionPane.showMessageDialog(null, ex);
        }
        return false;
    }

    public static List<Object[]> cargarMaterias() {
        List<Object[]> lista = new ArrayList<>();
        try {
            conexion cn = new conexion();
            Connection cc = cn.conectar();
            String sql = "select * from materias";
            Statement st = cc.createStatement();
            ResultSet rs = st.executeQuery(sql);
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
            JOptionPane.showMessageDialog(null, ex);
        }
        return lista;
    }

    public static List<Object[]> materiasEnHorario(String matricula, String diaSemana, String ahoraHHmm) {
        List<Object[]> lista = new ArrayList<>();
        try {
            conexion cn = new conexion();
            Connection cc = cn.conectar();
            String sql = "select m.codigo, m.nombre, m.dia_semana, m.hora_inicio, m.hora_fin "
                    + "from materias m join inscripciones i on i.id_materia=m.id_materia "
                    + "join estudiantes e on e.id_estudiante=i.id_estudiante "
                    + "where e.matricula=? and m.dia_semana=? and m.hora_inicio<=? and m.hora_fin>=?";
            PreparedStatement pst = cc.prepareStatement(sql);
            pst.setString(1, matricula);
            pst.setString(2, diaSemana);
            pst.setString(3, ahoraHHmm + ":00");
            pst.setString(4, ahoraHHmm + ":00");
            ResultSet rs = pst.executeQuery();
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
            JOptionPane.showMessageDialog(null, ex);
        }
        return lista;
    }
}