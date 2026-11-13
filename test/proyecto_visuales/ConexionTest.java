package proyecto_visuales;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;

public class ConexionTest {

    public static void main(String[] args) throws Exception {
        Connection cc = new conexion().conectar();
        if (cc == null) {
            throw new AssertionError("No se pudo abrir la conexión a jdbc:mysql://localhost/proyecto");
        }
        try (cc; Statement st = cc.createStatement();
             ResultSet rs = st.executeQuery("SELECT DATABASE()")) {
            rs.next();
            String base = rs.getString(1);
            if (!"proyecto".equals(base)) {
                throw new AssertionError("Base activa inesperada: " + base);
            }
            System.out.println("OK: conexión abierta a la base " + base);
        }
    }
}
