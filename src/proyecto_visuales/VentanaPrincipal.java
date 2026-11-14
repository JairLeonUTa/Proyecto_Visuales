package proyecto_visuales;

import com.github.sarxos.webcam.Webcam;
import com.github.sarxos.webcam.WebcamPanel;
import com.google.zxing.BinaryBitmap;
import com.google.zxing.ChecksumException;
import com.google.zxing.FormatException;
import com.google.zxing.NotFoundException;
import com.google.zxing.client.j2se.BufferedImageLuminanceSource;
import com.google.zxing.common.HybridBinarizer;
import com.google.zxing.qrcode.QRCodeReader;
import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.awt.image.BufferedImage;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import javax.swing.JFrame;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.SwingUtilities;
import javax.swing.table.DefaultTableModel;

/** Ventana principal del control de asistencia mediante QR. */
public class VentanaPrincipal extends JFrame {
    private static final long serialVersionUID = 1L;
    private static final DateTimeFormatter HORA = DateTimeFormatter.ofPattern("HH:mm");
    static final Object[] COLUMNAS = {"#", "Matrícula", "Estudiante", "Materia", "Fecha", "Hora", "Atraso"};

    private Webcam webcam;
    private WebcamPanel webcamPanel;
    private volatile boolean escaneando;
    private String ultimaMatricula = "";
    private long ultimaLectura;
    private final DefaultTableModel modeloTabla = new DefaultTableModel(COLUMNAS, 0) {
        @Override
        public boolean isCellEditable(int fila, int columna) {
            return false;
        }
    };

    public VentanaPrincipal() {
        initComponents();
        tabla.setModel(modeloTabla);
        panelCamara.setLayout(new BorderLayout());
        btnDetener.setEnabled(false);
        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                detenerCamara();
            }
        });
        crearMenuAdministracion();
        cargarRegistros();
        refrescarPorcentaje();
    }

    private void iniciarCamara() {
        List<Webcam> camaras = Webcam.getWebcams();
        if (camaras.isEmpty()) {
            lblEstado.setText("No se detectó ninguna cámara web conectada.");
            return;
        }
        webcam = camaras.get(0);
        try {
            abrirCamara();
        } catch (RuntimeException ex) {
            lblEstado.setText("No se pudo iniciar la cámara: " + ex.getMessage());
            return;
        }
        webcamPanel = new WebcamPanel(webcam);
        webcamPanel.setFPSDisplayed(false);
        panelCamara.removeAll();
        panelCamara.add(webcamPanel, BorderLayout.CENTER);
        panelCamara.revalidate();
        panelCamara.repaint();
        webcamPanel.start();
        btnIniciar.setEnabled(false);
        btnDetener.setEnabled(true);
        escaneando = true;
        lblEstado.setText("Cámara activa; esperando QR...");
        Thread lector = new Thread(this::leerQR, "lector-qr");
        lector.setDaemon(true);
        lector.start();
    }

    private void abrirCamara() {
        try {
            webcam.setViewSize(new Dimension(1280, 720));
        } catch (IllegalArgumentException ex) {
            webcam.setViewSize(new Dimension(640, 480));
        }
        webcam.open();
    }

    private void detenerCamara() {
        escaneando = false;
        if (webcamPanel != null) {
            webcamPanel.stop();
            panelCamara.removeAll();
            panelCamara.repaint();
            webcamPanel = null;
        }
        if (webcam != null && webcam.isOpen()) {
            webcam.close();
        }
        btnIniciar.setEnabled(true);
        btnDetener.setEnabled(false);
        lblEstado.setText("Cámara detenida.");
    }

    private void leerQR() {
        while (escaneando) {
            BufferedImage imagen = webcam == null ? null : webcam.getImage();
            String texto = decodificarQR(imagen);
            if (texto != null) {
                SwingUtilities.invokeLater(() -> procesarQR(texto));
            }
            try {
                Thread.sleep(300);
            } catch (InterruptedException ex) {
                Thread.currentThread().interrupt();
                return;
            }
        }
    }

    static String decodificarQR(BufferedImage imagen) {
        if (imagen == null) {
            return null;
        }
        try {
            BinaryBitmap bitmap = new BinaryBitmap(new HybridBinarizer(new BufferedImageLuminanceSource(imagen)));
            return new QRCodeReader().decode(bitmap).getText();
        } catch (NotFoundException | ChecksumException | FormatException ex) {
            return null;
        }
    }

    private void procesarQR(String contenido) {
        String matricula = extraerMatricula(contenido);
        if (matricula.isEmpty()) {
            return;
        }
        txtMatricula.setText(matricula);
        lblUltimoQR.setText("Último QR: " + matricula + " (" + LocalTime.now().format(HORA) + ")");
        long ahora = System.currentTimeMillis();
        if (matricula.equals(ultimaMatricula) && ahora - ultimaLectura < 2500) {
            return;
        }
        ultimaMatricula = matricula;
        ultimaLectura = ahora;

        Estudiante estudiante = EstudianteDAO.buscarEstudiantePorMatricula(matricula);
        if (estudiante == null) {
            lblEstado.setText("Estudiante no registrado: " + matricula);
            return;
        }
        LocalDate hoy = LocalDate.now();
        String hora = LocalTime.now().format(HORA);
        List<Object[]> materias = MateriaDAO.materiasEnHorario(matricula, hoy.getDayOfWeek().name(), hora);
        if (materias.isEmpty()) {
            lblEstado.setText("Fuera de horario: " + estudiante.getNombre() + " no tiene clase ahora ("
                    + hoy.getDayOfWeek() + " " + hora + ").");
            return;
        }
        Object[] materia = materias.get(0);
        String codigo = String.valueOf(materia[0]);
        boolean atraso = esAtraso(hora, String.valueOf(materia[3]));
        if (RegistroDAO.guardarRegistro(matricula, codigo, hoy.toString(), hora, atraso)) {
            lblEstado.setText("Asistencia registrada: " + estudiante.getNombre() + " " + estudiante.getApellido()
                    + " - " + codigo + (atraso ? " (atraso)" : ""));
        } else {
            lblEstado.setText("No se registró: " + RegistroDAO.getUltimoError());
        }
        actualizarVista();
    }

    /** El QR contiene la matrícula; si trae más datos separados por "|", se usa solo el primero. */
    static String extraerMatricula(String contenido) {
        if (contenido == null) {
            return "";
        }
        return contenido.trim().split("\\|", 2)[0].trim();
    }

    /** Hay atraso cuando la hora de entrada es posterior a la hora de inicio de la clase. */
    static boolean esAtraso(String horaEntrada, String horaInicio) {
        LocalTime entrada = LocalTime.parse(horaEntrada.substring(0, 5));
        LocalTime inicio = LocalTime.parse(horaInicio.substring(0, 5));
        return entrada.isAfter(inicio);
    }

    private void mostrarHorario() {
        String matricula = txtMatricula.getText().trim();
        if (matricula.isEmpty()) {
            lblEstado.setText("Escriba una matrícula para ver su horario activo.");
            return;
        }
        String dia = LocalDate.now().getDayOfWeek().name();
        String hora = LocalTime.now().format(HORA);
        List<Object[]> materias = MateriaDAO.materiasEnHorario(matricula, dia, hora);
        if (materias.isEmpty()) {
            lblEstado.setText("Sin materia activa para " + matricula + " ahora (" + dia + " " + hora + ").");
        } else {
            Object[] m = materias.get(0);
            lblEstado.setText("Materia activa: " + m[0] + " - " + m[1] + " (" + m[3] + " a " + m[4] + ")");
        }
    }

    private void actualizarVista() {
        cargarRegistros();
        refrescarPorcentaje();
    }

    private void cargarRegistros() {
        modeloTabla.setRowCount(0);
        for (Object[] registro : RegistroDAO.listarRegistros(200)) {
            modeloTabla.addRow(registro);
        }
    }

    private void refrescarPorcentaje() {
        String matricula = txtMatricula.getText().trim();
        List<Object[]> filas = matricula.isEmpty()
                ? List.of() : RegistroDAO.porcentajePorEstudiante(matricula, RegistroDAO.SEMANAS_POR_PARCIAL);
        if (filas.isEmpty()) {
            lblPorcentaje.setText("Porcentaje: --");
            return;
        }
        double suma = 0;
        StringBuilder texto = new StringBuilder("Porcentaje: ");
        for (Object[] fila : filas) {
            double porcentaje = ((Number) fila[3]).doubleValue();
            suma += porcentaje;
            texto.append(fila[0]).append(' ').append(String.format("%.0f%%", porcentaje)).append("  ");
        }
        texto.append("| Global: ").append(String.format("%.0f%%", suma / filas.size()));
        lblPorcentaje.setText(texto.toString());
    }

    private void crearMenuAdministracion() {
        JMenu menuAdmin = new JMenu("Administración");
        JMenuItem abrirAdmin = new JMenuItem("Estudiantes, materias e inscripciones");
        abrirAdmin.addActionListener(e -> abrirAdministracion());
        menuAdmin.add(abrirAdmin);
        JMenuBar barra = new JMenuBar();
        barra.add(menuAdmin);
        setJMenuBar(barra);
    }

    private void abrirAdministracion() {
        VentanaAdmin admin = new VentanaAdmin();
        admin.setLocationRelativeTo(this);
        admin.setVisible(true);
    }

    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        btnIniciar = new javax.swing.JButton();
        btnDetener = new javax.swing.JButton();
        lblMatricula = new javax.swing.JLabel();
        txtMatricula = new javax.swing.JTextField();
        btnRegistrar = new javax.swing.JButton();
        btnHorario = new javax.swing.JButton();
        btnRefrescar = new javax.swing.JButton();
        panelCamara = new javax.swing.JPanel();
        lblRegistros = new javax.swing.JLabel();
        scrollRegistros = new javax.swing.JScrollPane();
        tabla = new javax.swing.JTable();
        lblEstado = new javax.swing.JLabel();
        lblUltimoQR = new javax.swing.JLabel();
        lblPorcentaje = new javax.swing.JLabel();

        setDefaultCloseOperation(javax.swing.WindowConstants.EXIT_ON_CLOSE);
        setTitle("Control de Asistencia por Código QR - Webcam");
        setMinimumSize(new java.awt.Dimension(900, 560));

        btnIniciar.setText("Iniciar cámara");
        btnIniciar.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnIniciarActionPerformed(evt);
            }
        });

        btnDetener.setText("Detener cámara");
        btnDetener.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnDetenerActionPerformed(evt);
            }
        });

        lblMatricula.setText("Matrícula:");

        txtMatricula.setText("20230001");
        txtMatricula.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                txtMatriculaActionPerformed(evt);
            }
        });

        btnRegistrar.setText("Registrar manual");
        btnRegistrar.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnRegistrarActionPerformed(evt);
            }
        });

        btnHorario.setText("Ver horario activo");
        btnHorario.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnHorarioActionPerformed(evt);
            }
        });

        btnRefrescar.setText("Refrescar");
        btnRefrescar.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnRefrescarActionPerformed(evt);
            }
        });

        panelCamara.setBorder(javax.swing.BorderFactory.createTitledBorder("Cámara web"));

        javax.swing.GroupLayout panelCamaraLayout = new javax.swing.GroupLayout(panelCamara);
        panelCamara.setLayout(panelCamaraLayout);
        panelCamaraLayout.setHorizontalGroup(
            panelCamaraLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGap(0, 0, Short.MAX_VALUE)
        );
        panelCamaraLayout.setVerticalGroup(
            panelCamaraLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGap(0, 450, Short.MAX_VALUE)
        );

        lblRegistros.setText("Registros de asistencia");

        tabla.setModel(new javax.swing.table.DefaultTableModel(
            new Object [][] {

            },
            new String [] {
                "#", "Matrícula", "Estudiante", "Materia", "Fecha", "Hora", "Atraso"
            }
        ) {
            boolean[] canEdit = new boolean [] {
                false, false, false, false, false, false, false
            };

            public boolean isCellEditable(int rowIndex, int columnIndex) {
                return canEdit [columnIndex];
            }
        });
        tabla.setFillsViewportHeight(true);
        scrollRegistros.setViewportView(tabla);

        lblEstado.setText("Listo.");

        lblUltimoQR.setText("Último QR: --");

        lblPorcentaje.setText("Porcentaje: --");

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(getContentPane());
        getContentPane().setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(layout.createSequentialGroup()
                        .addComponent(btnIniciar)
                        .addGap(18, 18, 18)
                        .addComponent(btnDetener)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                        .addComponent(lblMatricula)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(txtMatricula, javax.swing.GroupLayout.PREFERRED_SIZE, 110, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(btnRegistrar)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(btnHorario)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(btnRefrescar)
                        .addGap(0, 320, Short.MAX_VALUE))
                    .addGroup(layout.createSequentialGroup()
                        .addComponent(panelCamara, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                        .addGap(18, 18, 18)
                        .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addComponent(lblRegistros)
                            .addComponent(scrollRegistros, javax.swing.GroupLayout.PREFERRED_SIZE, 400, javax.swing.GroupLayout.PREFERRED_SIZE)))
                    .addGroup(layout.createSequentialGroup()
                        .addComponent(lblEstado)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                        .addComponent(lblUltimoQR)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, 30, Short.MAX_VALUE)
                        .addComponent(lblPorcentaje)))
                .addContainerGap())
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(btnIniciar)
                    .addComponent(btnDetener)
                    .addComponent(lblMatricula)
                    .addComponent(txtMatricula, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(btnRegistrar)
                    .addComponent(btnHorario)
                    .addComponent(btnRefrescar))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(panelCamara, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addGroup(layout.createSequentialGroup()
                        .addComponent(lblRegistros)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(scrollRegistros, javax.swing.GroupLayout.DEFAULT_SIZE, 451, Short.MAX_VALUE)))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(lblEstado)
                    .addComponent(lblUltimoQR)
                    .addComponent(lblPorcentaje))
                .addContainerGap())
        );

        pack();
        setLocationRelativeTo(null);
    }// </editor-fold>//GEN-END:initComponents

    private void btnIniciarActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnIniciarActionPerformed
        iniciarCamara();
    }//GEN-LAST:event_btnIniciarActionPerformed

    private void btnDetenerActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnDetenerActionPerformed
        detenerCamara();
    }//GEN-LAST:event_btnDetenerActionPerformed

    private void txtMatriculaActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_txtMatriculaActionPerformed
        procesarQR(txtMatricula.getText());
    }//GEN-LAST:event_txtMatriculaActionPerformed

    private void btnRegistrarActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnRegistrarActionPerformed
        procesarQR(txtMatricula.getText());
    }//GEN-LAST:event_btnRegistrarActionPerformed

    private void btnHorarioActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnHorarioActionPerformed
        mostrarHorario();
    }//GEN-LAST:event_btnHorarioActionPerformed

    private void btnRefrescarActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnRefrescarActionPerformed
        actualizarVista();
    }//GEN-LAST:event_btnRefrescarActionPerformed

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JButton btnDetener;
    private javax.swing.JButton btnHorario;
    private javax.swing.JButton btnIniciar;
    private javax.swing.JButton btnRefrescar;
    private javax.swing.JButton btnRegistrar;
    private javax.swing.JLabel lblEstado;
    private javax.swing.JLabel lblMatricula;
    private javax.swing.JLabel lblPorcentaje;
    private javax.swing.JLabel lblRegistros;
    private javax.swing.JLabel lblUltimoQR;
    private javax.swing.JPanel panelCamara;
    private javax.swing.JScrollPane scrollRegistros;
    private javax.swing.JTable tabla;
    private javax.swing.JTextField txtMatricula;
    // End of variables declaration//GEN-END:variables
}
