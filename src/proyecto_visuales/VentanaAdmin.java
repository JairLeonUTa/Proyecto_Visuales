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
        cargarMaterias();
        cargarEstudiantes();
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
        cargarEstudiantes();
        seleccionarEstudiante(matricula);
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

    private void crearMateria() {
        String codigo = txtCodigoMateria.getText().trim();
        String nombre = txtNombreMateria.getText().trim();
        if (codigo.isEmpty() || nombre.isEmpty()) {
            mostrarEstado("Código y nombre de materia son obligatorios.");
            return;
        }
        if (!ValidadorHorarios.horariosValidos(horariosSeleccionados)) {
            mostrarEstado("Configure al menos dos días con horarios válidos.");
            return;
        }
        if (MateriaDAO.guardarMateria(codigo, nombre, horariosSeleccionados)) {
            txtCodigoMateria.setText("");
            txtNombreMateria.setText("");
            cargarMaterias();
            seleccionarMateria(codigo);
            mostrarEstado("Materia y horarios guardados.");
        } else {
            mostrarEstado("No se pudo guardar la materia. " + MateriaDAO.getUltimoError());
        }
    }

    private void inscribirEstudiante() {
        EstudianteItem estudiante = (EstudianteItem) cmbEstudiantes.getSelectedItem();
        MateriaItem materia = (MateriaItem) cmbMaterias.getSelectedItem();
        if (estudiante == null || materia == null) {
            mostrarEstado("Seleccione un estudiante y una materia.");
            return;
        }
        if (InscripcionDAO.estaInscrito(estudiante.matricula, materia.codigo)) {
            mostrarEstado(estudiante.nombre + " ya está inscrito en " + materia.nombre + ".");
        } else if (InscripcionDAO.inscribir(estudiante.matricula, materia.codigo)) {
            mostrarEstado(estudiante.nombre + " inscrito en " + materia.nombre + ".");
        } else {
            mostrarEstado("No se inscribió. " + InscripcionDAO.getUltimoError());
        }
    }

    private void seleccionarDias() {
        List<HorarioMateria> nuevos = SelectorHorarios.seleccionar(this, horariosSeleccionados);
        if (nuevos != null) {
            horariosSeleccionados = nuevos;
            actualizarTextoDias();
        }
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

    @SuppressWarnings("unchecked")
    private void cargarMaterias() {
        cmbMaterias.removeAllItems();
        for (Object[] materia : MateriaDAO.cargarMaterias()) {
            cmbMaterias.addItem(new MateriaItem(String.valueOf(materia[0]), String.valueOf(materia[1])));
        }
    }

    private void seleccionarMateria(String codigo) {
        for (int i = 0; i < cmbMaterias.getItemCount(); i++) {
            if (((MateriaItem) cmbMaterias.getItemAt(i)).codigo.equals(codigo)) {
                cmbMaterias.setSelectedIndex(i);
                return;
            }
        }
    }

    @SuppressWarnings("unchecked")
    private void cargarEstudiantes() {
        cmbEstudiantes.removeAllItems();
        for (Estudiante estudiante : EstudianteDAO.cargarEstudiantes()) {
            cmbEstudiantes.addItem(new EstudianteItem(estudiante.getMatricula(),
                    estudiante.getNombre() + " " + estudiante.getApellido()));
        }
    }

    private void seleccionarEstudiante(String matricula) {
        for (int i = 0; i < cmbEstudiantes.getItemCount(); i++) {
            if (((EstudianteItem) cmbEstudiantes.getItemAt(i)).matricula.equals(matricula)) {
                cmbEstudiantes.setSelectedIndex(i);
                return;
            }
        }
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

    /** Elemento del combo de materias: muestra "código - nombre". */
    public static final class MateriaItem {
        private final String codigo;
        private final String nombre;

        public MateriaItem(String codigo, String nombre) {
            this.codigo = codigo;
            this.nombre = nombre;
        }

        @Override
        public String toString() {
            return codigo + " - " + nombre;
        }
    }

    /** Elemento del combo de estudiantes: muestra "matrícula - nombre". */
    public static final class EstudianteItem {
        private final String matricula;
        private final String nombre;

        public EstudianteItem(String matricula, String nombre) {
            this.matricula = matricula;
            this.nombre = nombre;
        }

        @Override
        public String toString() {
            return matricula + " - " + nombre;
        }
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
        btnCrearEstudiante.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnCrearEstudianteActionPerformed(evt);
            }
        });

        btnAbrirQR.setText("Abrir carpeta de QR");
        btnAbrirQR.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnAbrirQRActionPerformed(evt);
            }
        });

        lblHorario.setText("2. Crear materia y asignar horario");

        lblCodigoMateria.setText("Código:");

        lblNombreMateria.setText("Materia:");

        lblDia.setText("Horarios:");

        btnSeleccionarDias.setText("Seleccionar días...");
        btnSeleccionarDias.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnSeleccionarDiasActionPerformed(evt);
            }
        });

        lblDiasSeleccionados.setText("2 días configurados");

        btnCrearMateria.setText("Crear materia y horario");
        btnCrearMateria.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnCrearMateriaActionPerformed(evt);
            }
        });

        lblInscripcion.setText("3. Inscribir al estudiante en una materia");

        lblEstudianteInscribir.setText("Estudiante:");

        lblMateriaInscribir.setText("Materia:");

        btnInscribir.setText("Inscribir estudiante");
        btnInscribir.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnInscribirActionPerformed(evt);
            }
        });

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

    private void btnSeleccionarDiasActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnSeleccionarDiasActionPerformed
        seleccionarDias();
    }//GEN-LAST:event_btnSeleccionarDiasActionPerformed

    private void btnCrearEstudianteActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnCrearEstudianteActionPerformed
        crearEstudiante();
    }//GEN-LAST:event_btnCrearEstudianteActionPerformed

    private void btnAbrirQRActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnAbrirQRActionPerformed
        abrirCarpetaQR();
    }//GEN-LAST:event_btnAbrirQRActionPerformed

    private void btnCrearMateriaActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnCrearMateriaActionPerformed
        crearMateria();
    }//GEN-LAST:event_btnCrearMateriaActionPerformed

    private void btnInscribirActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnInscribirActionPerformed
        inscribirEstudiante();
    }//GEN-LAST:event_btnInscribirActionPerformed

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
