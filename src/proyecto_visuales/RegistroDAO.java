package proyecto_visuales;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;

/** Acceso a la tabla registros (asistencias). */
public class RegistroDAO {

    private static String ultimoError = "";

    private RegistroDAO() {
    }

    public static String getUltimoError() {
        return ultimoError;
    }
}
