package proyecto_visuales;

import java.sql.Connection;
import java.sql.DriverManager;
import javax.swing.JOptionPane;

public class conexion {

    public Connection conectar() {
        Connection conn = null;
        try {
            Class.forName("com.mysql.jdbc.Driver");
            conn = DriverManager.getConnection("jdbc:mysql://localhost/proyecto", "root", "");
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(null, ex);
        }
        return conn;
    }
}
