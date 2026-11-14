package proyecto_visuales;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.EncodeHintType;
import com.google.zxing.client.j2se.MatrixToImageWriter;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel;
import java.awt.Desktop;
import java.awt.Image;
import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.swing.ImageIcon;
import javax.swing.JFrame;

/** Administración de estudiantes, materias, horarios e inscripciones. */
public class VentanaAdmin extends JFrame {
    private static final long serialVersionUID = 1L;
    private List<HorarioMateria> horariosSeleccionados = new ArrayList<>(List.of(
            new HorarioMateria("MONDAY", "08:00", "10:00"),
            new HorarioMateria("WEDNESDAY", "08:00", "10:00")));

    public VentanaAdmin() {
        initComponents();
        actualizarTextoDias();
    }

    private void crearEstudiante() {
        String matricula = txtMatricula.getText().trim();
        String nombre = txtNombre.getText().trim();
        String apellido = txtApellido.getText().trim();
        if (matricula.isEmpty() || nombre.isEmpty() || apellido.isEmpty()) {
            mostrarEstado("Matrícula, nombre y apellido son obligatorios.");
            return;
        }
        Estudiante estudiante = new Estudiante(matricula, nombre, apellido, txtEmail.getText().trim());
        if (!EstudianteDAO.guardarEstudiante(estudiante)) {
            mostrarEstado("No se creó el estudiante. " + EstudianteDAO.getUltimoError());
            return;
        }
        try {
            File archivo = generarQR(matricula);
            lblQr.setText("");
            lblQr.setIcon(new ImageIcon(new ImageIcon(archivo.getAbsolutePath()).getImage()
                    .getScaledInstance(180, 180, Image.SCALE_SMOOTH)));
            mostrarEstado("Estudiante creado. QR guardado en: " + archivo.getAbsolutePath());
        } catch (Exception ex) {
            mostrarEstado("Estudiante creado, pero no se pudo generar el QR: " + ex.getMessage());
        }
    }

    /** Genera qrs/qr_MATRICULA.png con la matrícula como contenido del QR. */
    static File generarQR(String matricula) throws Exception {
        Path carpeta = Paths.get("qrs");
        Files.createDirectories(carpeta);
        Map<EncodeHintType, Object> hints = new HashMap<>();
        hints.put(EncodeHintType.ERROR_CORRECTION, ErrorCorrectionLevel.H);
        hints.put(EncodeHintType.MARGIN, 2);
        BitMatrix qr = new QRCodeWriter().encode(matricula, BarcodeFormat.QR_CODE, 320, 320, hints);
        File archivo = carpeta.resolve("qr_" + matricula + ".png").toFile();
        MatrixToImageWriter.writeToPath(qr, "PNG", archivo.toPath());
        return archivo;
    }

    private void actualizarTextoDias() {
        StringBuilder texto = new StringBuilder();
        for (HorarioMateria h : horariosSeleccionados) {
            if (texto.length() > 0) {
                texto.append(", ");
            }
            texto.append(h.diaSemana(), 0, 3).append(' ').append(h.horaInicio()).append('-').append(h.horaFin());
        }
        lblDiasSeleccionados.setText(texto.toString());
    }

    private void abrirCarpetaQR() {
        try {
            Path carpeta = Paths.get("qrs");
            Files.createDirectories(carpeta);
            if (Desktop.isDesktopSupported()) {
                Desktop.getDesktop().open(carpeta.toFile());
            } else {
                mostrarEstado("Los QR se guardan en: " + carpeta.toAbsolutePath());
            }
        } catch (Exception ex) {
            mostrarEstado("No se pudo abrir la carpeta de QR: " + ex.getMessage());
        }
    }

    private void mostrarEstado(String mensaje) {
        lblEstado.setText(mensaje);
    }

    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        lblTitulo = new javax.swing.JLabel();
        lblDatosEstudiante = new javax.swing.JLabel();
        lblMatricula = new javax.swing.JLabel();
        txtMatricula = new javax.swing.JTextField();
        lblNombre = new javax.swing.JLabel();
        txtNombre = new javax.swing.JTextField();
        lblApellido = new javax.swing.JLabel();
        txtApellido = new javax.swing.JTextField();
        lblEmail = new javax.swing.JLabel();
        txtEmail = new javax.swing.JTextField();
        lblQr = new javax.swing.JLabel();
        btnCrearEstudiante = new javax.swing.JButton();
        btnAbrirQR = new javax.swing.JButton();
        lblHorario = new javax.swing.JLabel();
        lblCodigoMateria = new javax.swing.JLabel();
        txtCodigoMateria = new javax.swing.JTextField();
        lblNombreMateria = new javax.swing.JLabel();
        txtNombreMateria = new javax.swing.JTextField();
        lblDia = new javax.swing.JLabel();
        btnSeleccionarDias = new javax.swing.JButton();
        lblDiasSeleccionados = new javax.swing.JLabel();
        btnCrearMateria = new javax.swing.JButton();
        lblInscripcion = new javax.swing.JLabel();
        lblEstudianteInscribir = new javax.swing.JLabel();
        cmbEstudiantes = new javax.swing.JComboBox();
        lblMateriaInscribir = new javax.swing.JLabel();
        cmbMaterias = new javax.swing.JComboBox();
        btnInscribir = new javax.swing.JButton();
        lblEstado = new javax.swing.JLabel();

        setDefaultCloseOperation(javax.swing.WindowConstants.DISPOSE_ON_CLOSE);
        setTitle("Administración - Asistencia QR");
        setMinimumSize(new java.awt.Dimension(820, 590));

        lblTitulo.setText("Administración de estudiantes y horarios");

        lblDatosEstudiante.setText("1. Datos del estudiante");

        lblMatricula.setText("Matrícula:");

        lblNombre.setText("Nombre:");

        lblApellido.setText("Apellido:");

        lblEmail.setText("Email:");

        lblQr.setText("El QR aparecerá aquí");
        lblQr.setBorder(javax.swing.BorderFactory.createTitledBorder("QR del estudiante"));

        btnCrearEstudiante.setText("Crear estudiante y generar QR");

        btnAbrirQR.setText("Abrir carpeta de QR");

        lblHorario.setText("2. Crear materia y asignar horario");

        lblCodigoMateria.setText("Código:");

        lblNombreMateria.setText("Materia:");

        lblDia.setText("Horarios:");

        btnSeleccionarDias.setText("Seleccionar días...");

        lblDiasSeleccionados.setText("2 días configurados");

        btnCrearMateria.setText("Crear materia y horario");

        lblInscripcion.setText("3. Inscribir al estudiante en una materia");

        lblEstudianteInscribir.setText("Estudiante:");

        lblMateriaInscribir.setText("Materia:");

        btnInscribir.setText("Inscribir estudiante");

        lblEstado.setText("Listo.");

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(getContentPane());
        getContentPane().setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, 18)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(layout.createSequentialGroup()
                        .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addComponent(lblMatricula)
                            .addComponent(lblNombre)
                            .addComponent(lblApellido)
                            .addComponent(lblEmail))
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, javax.swing.GroupLayout.DEFAULT_SIZE, 12)
                        .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addComponent(txtMatricula, javax.swing.GroupLayout.PREFERRED_SIZE, 180, javax.swing.GroupLayout.PREFERRED_SIZE)
                            .addComponent(txtNombre, javax.swing.GroupLayout.PREFERRED_SIZE, 180, javax.swing.GroupLayout.PREFERRED_SIZE)
                            .addComponent(txtApellido, javax.swing.GroupLayout.PREFERRED_SIZE, 180, javax.swing.GroupLayout.PREFERRED_SIZE)
                            .addComponent(txtEmail, javax.swing.GroupLayout.PREFERRED_SIZE, 180, javax.swing.GroupLayout.PREFERRED_SIZE))
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, javax.swing.GroupLayout.DEFAULT_SIZE, 12)
                        .addComponent(lblQr, 200, 200, 200))
                    .addGroup(layout.createSequentialGroup()
                        .addComponent(lblCodigoMateria)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, javax.swing.GroupLayout.DEFAULT_SIZE, 12)
                        .addComponent(txtCodigoMateria, javax.swing.GroupLayout.PREFERRED_SIZE, 140, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, javax.swing.GroupLayout.DEFAULT_SIZE, 18)
                        .addComponent(lblNombreMateria)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, javax.swing.GroupLayout.DEFAULT_SIZE, 10)
                        .addComponent(txtNombreMateria, javax.swing.GroupLayout.PREFERRED_SIZE, 190, javax.swing.GroupLayout.PREFERRED_SIZE))
                    .addGroup(layout.createSequentialGroup()
                        .addComponent(lblDia)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, javax.swing.GroupLayout.DEFAULT_SIZE, 10)
                        .addComponent(btnSeleccionarDias, javax.swing.GroupLayout.PREFERRED_SIZE, 150, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, javax.swing.GroupLayout.DEFAULT_SIZE, 8)
                        .addComponent(lblDiasSeleccionados))
                    .addGroup(layout.createSequentialGroup()
                        .addComponent(lblEstudianteInscribir)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, javax.swing.GroupLayout.DEFAULT_SIZE, 8)
                        .addComponent(cmbEstudiantes, javax.swing.GroupLayout.PREFERRED_SIZE, 200, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, javax.swing.GroupLayout.DEFAULT_SIZE, 12)
                        .addComponent(lblMateriaInscribir)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, javax.swing.GroupLayout.DEFAULT_SIZE, 8)
                        .addComponent(cmbMaterias, javax.swing.GroupLayout.PREFERRED_SIZE, 200, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, javax.swing.GroupLayout.DEFAULT_SIZE, 10)
                        .addComponent(btnInscribir))
                    .addGroup(layout.createSequentialGroup()
                        .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addComponent(lblTitulo)
                            .addComponent(lblDatosEstudiante)
                            .addComponent(lblHorario)
                            .addComponent(btnCrearMateria)
                            .addComponent(lblInscripcion)
                            .addComponent(lblEstado)
                            .addGroup(layout.createSequentialGroup()
                                .addComponent(btnCrearEstudiante)
                                .addGap(18, 18, 18)
                                .addComponent(btnAbrirQR)))
                        .addGap(0, 0, Short.MAX_VALUE)))
                .addContainerGap(18, Short.MAX_VALUE))
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, 14)
                .addComponent(lblTitulo)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, javax.swing.GroupLayout.DEFAULT_SIZE, 14)
                .addComponent(lblDatosEstudiante)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, javax.swing.GroupLayout.DEFAULT_SIZE, 8)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(layout.createSequentialGroup()
                        .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                            .addComponent(lblMatricula)
                            .addComponent(txtMatricula, javax.swing.GroupLayout.PREFERRED_SIZE, 28, javax.swing.GroupLayout.PREFERRED_SIZE))
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, javax.swing.GroupLayout.DEFAULT_SIZE, 6)
                        .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                            .addComponent(lblNombre)
                            .addComponent(txtNombre, javax.swing.GroupLayout.PREFERRED_SIZE, 28, javax.swing.GroupLayout.PREFERRED_SIZE))
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, javax.swing.GroupLayout.DEFAULT_SIZE, 6)
                        .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                            .addComponent(lblApellido)
                            .addComponent(txtApellido, javax.swing.GroupLayout.PREFERRED_SIZE, 28, javax.swing.GroupLayout.PREFERRED_SIZE))
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, javax.swing.GroupLayout.DEFAULT_SIZE, 6)
                        .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                            .addComponent(lblEmail)
                            .addComponent(txtEmail, javax.swing.GroupLayout.PREFERRED_SIZE, 28, javax.swing.GroupLayout.PREFERRED_SIZE)))
                    .addComponent(lblQr, 210, 210, 210))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, javax.swing.GroupLayout.DEFAULT_SIZE, 8)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(btnCrearEstudiante)
                    .addComponent(btnAbrirQR))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, javax.swing.GroupLayout.DEFAULT_SIZE, 14)
                .addComponent(lblHorario)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, javax.swing.GroupLayout.DEFAULT_SIZE, 8)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(lblCodigoMateria)
                    .addComponent(txtCodigoMateria, javax.swing.GroupLayout.PREFERRED_SIZE, 28, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(lblNombreMateria)
                    .addComponent(txtNombreMateria, javax.swing.GroupLayout.PREFERRED_SIZE, 28, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, javax.swing.GroupLayout.DEFAULT_SIZE, 6)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(lblDia)
                    .addComponent(btnSeleccionarDias, javax.swing.GroupLayout.PREFERRED_SIZE, 28, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(lblDiasSeleccionados))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, javax.swing.GroupLayout.DEFAULT_SIZE, 8)
                .addComponent(btnCrearMateria)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, javax.swing.GroupLayout.DEFAULT_SIZE, 14)
                .addComponent(lblInscripcion)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, javax.swing.GroupLayout.DEFAULT_SIZE, 8)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(lblEstudianteInscribir)
                    .addComponent(cmbEstudiantes, javax.swing.GroupLayout.PREFERRED_SIZE, 28, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(lblMateriaInscribir)
                    .addComponent(cmbMaterias, javax.swing.GroupLayout.PREFERRED_SIZE, 28, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(btnInscribir))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, javax.swing.GroupLayout.DEFAULT_SIZE, 12)
                .addComponent(lblEstado)
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, 14))
        );

        pack();
        setLocationRelativeTo(null);
    }// </editor-fold>//GEN-END:initComponents

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JButton btnAbrirQR;
    private javax.swing.JButton btnCrearEstudiante;
    private javax.swing.JButton btnCrearMateria;
    private javax.swing.JButton btnInscribir;
    private javax.swing.JButton btnSeleccionarDias;
    private javax.swing.JComboBox cmbEstudiantes;
    private javax.swing.JComboBox cmbMaterias;
    private javax.swing.JLabel lblApellido;
    private javax.swing.JLabel lblCodigoMateria;
    private javax.swing.JLabel lblDatosEstudiante;
    private javax.swing.JLabel lblDia;
    private javax.swing.JLabel lblDiasSeleccionados;
    private javax.swing.JLabel lblEmail;
    private javax.swing.JLabel lblEstado;
    private javax.swing.JLabel lblEstudianteInscribir;
    private javax.swing.JLabel lblHorario;
    private javax.swing.JLabel lblInscripcion;
    private javax.swing.JLabel lblMateriaInscribir;
    private javax.swing.JLabel lblMatricula;
    private javax.swing.JLabel lblNombre;
    private javax.swing.JLabel lblNombreMateria;
    private javax.swing.JLabel lblQr;
    private javax.swing.JLabel lblTitulo;
    private javax.swing.JTextField txtApellido;
    private javax.swing.JTextField txtCodigoMateria;
    private javax.swing.JTextField txtEmail;
    private javax.swing.JTextField txtMatricula;
    private javax.swing.JTextField txtNombre;
    private javax.swing.JTextField txtNombreMateria;
    // End of variables declaration//GEN-END:variables

    public static void main(String[] args) {
        java.awt.EventQueue.invokeLater(() -> new VentanaAdmin().setVisible(true));
    }
}
