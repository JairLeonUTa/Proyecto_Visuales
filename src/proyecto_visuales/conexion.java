/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this license
 */
package proyecto_visuales;

import javax.swing.JOptionPane;
import java.sql.*;

/**
 * Conexion a la base MySQL "proyecto" de XAMPP.
 *
 * @author Rolando Sebastian Miranda
 */
public class conexion {

    public Connection conectar() {
        Connection conn = null;
        try {
            Class.forName("com.mysql.jdbc.Driver");
            conn = DriverManager.getConnection("jdbc:mysql://localhost/proyecto", "root", "");
            //JOptionPane.showMessageDialog(null, "esta conectado a la base");

        } catch (Exception ex) {
            JOptionPane.showMessageDialog(null, ex);
        }
        return conn;
    }
}
