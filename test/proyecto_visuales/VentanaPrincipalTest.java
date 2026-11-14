package proyecto_visuales;

import com.github.sarxos.webcam.Webcam;
import com.google.zxing.BarcodeFormat;
import com.google.zxing.client.j2se.MatrixToImageWriter;
import java.awt.image.BufferedImage;
import java.time.LocalDate;
import java.util.List;
import javax.swing.table.DefaultTableModel;
import com.google.zxing.qrcode.QRCodeWriter;

/**
 * Integración de la ventana principal: lectura de QR (la misma que usa la cámara),
 * registro de asistencia y filas de la tabla. La parte de base de datos requiere XAMPP.
 */
public class VentanaPrincipalTest {

    public static void main(String[] args) throws Exception {
        probarLecturaQR();
        probarReglasDeRegistro();
        informarCamaras();
        probarRegistroYTabla();
        System.out.println("OK: integración de cámara, registro y tabla verificada");
    }

    /** Genera un QR como el de Administración y lo decodifica con el lector de la cámara. */
    static void probarLecturaQR() throws Exception {
        BufferedImage imagen = MatrixToImageWriter.toBufferedImage(
                new QRCodeWriter().encode("20230001", BarcodeFormat.QR_CODE, 320, 320));
        verificar("20230001".equals(VentanaPrincipal.decodificarQR(imagen)), "lee la matrícula del QR");
        verificar(VentanaPrincipal.decodificarQR(new BufferedImage(200, 200, BufferedImage.TYPE_INT_RGB)) == null,
                "una imagen sin QR no devuelve nada");
        verificar(VentanaPrincipal.decodificarQR(null) == null, "sin imagen de cámara no falla");
    }

    static void probarReglasDeRegistro() {
        verificar("20230001".equals(VentanaPrincipal.extraerMatricula(" 20230001|Ana Pérez ")),
                "usa solo la matrícula del contenido");
        verificar(VentanaPrincipal.esAtraso("08:05", "08:00:00"), "llegar después del inicio es atraso");
        verificar(!VentanaPrincipal.esAtraso("08:00", "08:00:00"), "llegar a la hora no es atraso");
    }

    /** No falla si no hay webcam: solo informa cuántas detecta el equipo. */
    static void informarCamaras() {
        List<Webcam> camaras = Webcam.getWebcams();
        System.out.println("Cámaras detectadas: " + camaras.size()
                + (camaras.isEmpty() ? "" : " (" + camaras.get(0).getName() + ")"));
    }

    /** Registra una asistencia y comprueba que la tabla de la ventana pueda mostrarla. */
    static void probarRegistroYTabla() {
        String matricula = "TEST-VP";
        String materia = "TEST-VPM";
        limpiar(matricula, materia);
        try {
            verificar(EstudianteDAO.guardarEstudiante(new Estudiante(matricula, "Integracion", "Ventana", "")),
                    "crear estudiante de prueba");
            verificar(MateriaDAO.guardarMateria(materia, "Materia integración", List.of(
                    new HorarioMateria("TUESDAY", "08:00", "10:00"),
                    new HorarioMateria("THURSDAY", "08:00", "10:00"))), "crear materia de prueba");
            verificar(InscripcionDAO.inscribir(matricula, materia), "inscribir");
            verificar(RegistroDAO.guardarRegistro(matricula, materia, LocalDate.now().toString(), "08:10", true),
                    "registrar asistencia");
            DefaultTableModel modelo = new DefaultTableModel(VentanaPrincipal.COLUMNAS, 0);
            RegistroDAO.listarRegistrosPorMatricula(matricula, 10).forEach(modelo::addRow);
            verificar(modelo.getRowCount() == 1, "la tabla recibe el registro");
            verificar("Sí".equals(modelo.getValueAt(0, 6)), "la columna Atraso muestra el atraso");
        } finally {
            limpiar(matricula, materia);
        }
    }

    private static void limpiar(String matricula, String materia) {
        try (var cc = new conexion().conectar()) {
            for (String sql : new String[]{
                "DELETE r FROM registros r JOIN estudiantes e ON e.id_estudiante = r.id_estudiante WHERE e.matricula = ?",
                "DELETE FROM estudiantes WHERE matricula = ?"}) {
                try (var ps = cc.prepareStatement(sql)) {
                    ps.setString(1, matricula);
                    ps.executeUpdate();
                }
            }
            try (var ps = cc.prepareStatement("DELETE FROM materias WHERE codigo = ?")) {
                ps.setString(1, materia);
                ps.executeUpdate();
            }
        } catch (Exception ex) {
            throw new IllegalStateException("No se pudieron borrar los datos de prueba", ex);
        }
    }

    private static void verificar(boolean condicion, String mensaje) {
        if (!condicion) {
            throw new AssertionError(mensaje);
        }
    }
}
