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
import java.awt.Color;
import java.awt.GridLayout;
import java.awt.Dimension;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.awt.image.BufferedImage;
import java.time.DayOfWeek;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.TextStyle;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;

/**
 * Ventana principal del control de asistencia mediante QR.
 * <p>
 * Ejecución: abrir el proyecto en NetBeans, encender MySQL en XAMPP y ejecutar el
 * proyecto (Run Project); esta clase es la principal. "Iniciar cámara" muestra la
 * webcam y lee los QR generados en Administración; también se puede escribir la
 * cédula y pulsar "Registrar manual". La asistencia solo se guarda si el estudiante
 * está inscrito en una materia que tiene clase en ese día y hora; se da una tolerancia de
 * 5 minutos desde la hora de inicio y llegar después de ese margen se marca como atraso.
 */
public class VentanaPrincipal extends JFrame {
    private static final long serialVersionUID = 1L;
    private static final DateTimeFormatter HORA = DateTimeFormatter.ofPattern("HH:mm");
    static final int TOLERANCIA_ATRASO_MINUTOS = 5;
    private static final Color COLOR_OK = new Color(0, 128, 0);
    private static final Color COLOR_AVISO = new Color(204, 102, 0);
    private static final Color COLOR_ERROR = new Color(192, 0, 0);
    private static final Color COLOR_INFO = new Color(51, 51, 51);
    static final Object[] COLUMNAS = {"#", "Cédula", "Estudiante", "Materia", "Fecha", "Hora", "Atraso"};

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
        ajustarColumnas();
        lblEstado.setFont(lblEstado.getFont().deriveFont(java.awt.Font.BOLD, lblEstado.getFont().getSize() + 4f));
        lblPorcentaje.setFont(lblPorcentaje.getFont().deriveFont(java.awt.Font.BOLD));
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
        setExtendedState(JFrame.MAXIMIZED_BOTH);
    }

    private void ajustarColumnas() {
        int[] anchos = {40, 90, 180, 200, 90, 80, 60};
        for (int i = 0; i < anchos.length; i++) {
            tabla.getColumnModel().getColumn(i).setPreferredWidth(anchos[i]);
        }
        tabla.setRowHeight(22);
    }

    /** Abre la primera webcam disponible y, si se puede, empieza a buscar QR en sus imágenes. */
    private void iniciarCamara() {
        List<Webcam> camaras = Webcam.getWebcams();
        if (camaras.isEmpty()) {
            mostrarErrorCamara("No se detectó ninguna cámara web conectada.");
            return;
        }
        webcam = camaras.get(0);
        try {
            abrirCamara();
        } catch (RuntimeException ex) {
            mostrarErrorCamara("No se pudo iniciar la cámara: " + ex.getMessage());
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
        mostrarEstado("Cámara activa; esperando QR...", COLOR_INFO);
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

    /** Detiene la lectura de QR, quita la vista previa y libera la webcam. */
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
        mostrarEstado("Cámara detenida.", COLOR_INFO);
    }

    private void mostrarEstado(String texto, Color color) {
        lblEstado.setForeground(color);
        lblEstado.setText(texto);
    }

    private void mostrarErrorCamara(String mensaje) {
        mostrarEstado(mensaje, COLOR_ERROR);
        JOptionPane.showMessageDialog(this, mensaje, "Cámara web", JOptionPane.ERROR_MESSAGE);
    }

    /** Hilo lector: cada 300 ms toma una imagen de la webcam y busca un QR en ella. */
    private void leerQR() {
        while (escaneando) {
            BufferedImage imagen = webcam == null ? null : webcam.getImage();
            if (imagen == null && escaneando) {
                SwingUtilities.invokeLater(() -> {
                    detenerCamara();
                    mostrarErrorCamara("La cámara dejó de enviar imágenes. Revise que no la use otra aplicación.");
                });
                return;
            }
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

    /** Devuelve el texto del QR que aparece en la imagen, o null si no hay uno legible. */
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

    /**
     * Registra la asistencia de la cédula leída: el estudiante debe existir y tener
     * clase en una materia inscrita en este momento. Ignora la misma lectura repetida
     * durante 2,5 segundos para no registrar dos veces el mismo QR.
     */
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
            mostrarEstado("Estudiante no registrado: " + matricula, COLOR_ERROR);
            return;
        }
        LocalDate hoy = LocalDate.now();
        String hora = LocalTime.now().format(HORA);
        List<Object[]> materias = MateriaDAO.materiasEnHorario(matricula, hoy.getDayOfWeek().name(), hora);
        if (materias.isEmpty()) {
            mostrarEstado("Fuera de horario: " + estudiante.getNombre() + " no tiene clase ahora ("
                    + diaEnEspanol(hoy.getDayOfWeek().name()) + " " + hora + ").", COLOR_AVISO);
            return;
        }
        Object[] materia = materias.get(0);
        String codigo = String.valueOf(materia[0]);
        String inicio = String.valueOf(materia[3]);
        boolean atraso = esAtraso(hora, inicio);
        if (RegistroDAO.guardarRegistro(matricula, codigo, hoy.toString(), hora, atraso)) {
            mostrarEstado("Asistencia registrada: " + estudiante.getNombre() + " " + estudiante.getApellido()
                    + " - " + codigo + (atraso ? " (atraso de " + minutosTarde(hora, inicio) + " min)" : " (a tiempo)"),
                    atraso ? COLOR_AVISO : COLOR_OK);
        } else {
            mostrarEstado("No se registró: " + RegistroDAO.getUltimoError(), COLOR_ERROR);
        }
        actualizarVista();
    }

    /** El QR contiene la cédula; si trae más datos separados por "|", se usa solo el primero. */
    static String extraerMatricula(String contenido) {
        if (contenido == null) {
            return "";
        }
        return contenido.trim().split("\\|", 2)[0].trim();
    }

    /** Hay atraso cuando la hora de entrada pasa la hora de inicio más la tolerancia de 5 minutos. */
    static boolean esAtraso(String horaEntrada, String horaInicio) {
        LocalTime entrada = LocalTime.parse(horaEntrada.substring(0, 5));
        LocalTime inicio = LocalTime.parse(horaInicio.substring(0, 5));
        return entrada.isAfter(inicio.plusMinutes(TOLERANCIA_ATRASO_MINUTOS));
    }

    static long minutosTarde(String horaEntrada, String horaInicio) {
        LocalTime entrada = LocalTime.parse(horaEntrada.substring(0, 5));
        LocalTime inicio = LocalTime.parse(horaInicio.substring(0, 5));
        return Math.max(0, Duration.between(inicio, entrada).toMinutes());
    }

    private void mostrarHorario() {
        String matricula = txtMatricula.getText().trim();
        if (matricula.isEmpty()) {
            mostrarEstado("Escriba una cédula para ver su horario.", COLOR_AVISO);
            JOptionPane.showMessageDialog(this, "Escriba la cédula del estudiante para ver su horario.",
                    "Horario", JOptionPane.WARNING_MESSAGE);
            return;
        }
        Estudiante estudiante = EstudianteDAO.buscarEstudiantePorMatricula(matricula);
        if (estudiante == null) {
            mostrarEstado("Estudiante no registrado: " + matricula, COLOR_ERROR);
            JOptionPane.showMessageDialog(this, "No existe un estudiante con cédula " + matricula + ".",
                    "Horario", JOptionPane.WARNING_MESSAGE);
            return;
        }
        List<Object[]> horario = MateriaDAO.horarioDeEstudiante(matricula);
        if (horario.isEmpty()) {
            String motivo = MateriaDAO.getUltimoError().isEmpty()
                    ? estudiante.getNombre() + " no está inscrito en ninguna materia." : MateriaDAO.getUltimoError();
            mostrarEstado(motivo, COLOR_AVISO);
            JOptionPane.showMessageDialog(this, motivo, "Horario", JOptionPane.INFORMATION_MESSAGE);
            return;
        }
        String hoy = LocalDate.now().getDayOfWeek().name();
        String hora = LocalTime.now().format(HORA);
        DefaultTableModel modelo = new DefaultTableModel(new Object[]{"Día", "Materia", "Inicio", "Fin", "Estado"}, 0) {
            @Override
            public boolean isCellEditable(int fila, int columna) {
                return false;
            }
        };
        String claseActual = null;
        int filaActual = -1;
        for (Object[] h : horario) {
            String inicio = String.valueOf(h[3]).substring(0, 5);
            String fin = String.valueOf(h[4]).substring(0, 5);
            boolean esHoy = hoy.equals(h[2]);
            boolean enClase = esHoy && hora.compareTo(inicio) >= 0 && hora.compareTo(fin) < 0;
            String estado = enClase ? "En clase ahora" : esHoy ? "Hoy" : "";
            if (enClase && claseActual == null) {
                claseActual = h[0] + " - " + h[1] + " (" + inicio + " a " + fin + ")";
                filaActual = modelo.getRowCount();
            }
            modelo.addRow(new Object[]{diaEnEspanol(String.valueOf(h[2])), h[0] + " - " + h[1], inicio, fin, estado});
        }
        JTable tablaHorario = new JTable(modelo);
        tablaHorario.setRowHeight(22);
        tablaHorario.getColumnModel().getColumn(1).setPreferredWidth(240);
        if (filaActual >= 0) {
            tablaHorario.setRowSelectionInterval(filaActual, filaActual);
        }
        JScrollPane scroll = new JScrollPane(tablaHorario);
        scroll.setPreferredSize(new Dimension(620, Math.min(320, 40 + 22 * horario.size())));

        JLabel lblAlumno = new JLabel("Estudiante: " + estudiante.getNombre() + " " + estudiante.getApellido()
                + " (" + matricula + ")");
        JLabel lblAhora = new JLabel(claseActual == null
                ? "Ahora (" + diaEnEspanol(hoy) + " " + hora + "): no tiene clase, la asistencia no se puede registrar."
                : "Ahora (" + diaEnEspanol(hoy) + " " + hora + "): tiene clase de " + claseActual + ".");
        lblAhora.setForeground(claseActual == null ? COLOR_AVISO : COLOR_OK);
        lblAhora.setFont(lblAhora.getFont().deriveFont(java.awt.Font.BOLD));
        JPanel encabezado = new JPanel(new GridLayout(2, 1, 0, 4));
        encabezado.add(lblAlumno);
        encabezado.add(lblAhora);
        JPanel contenido = new JPanel(new BorderLayout(0, 10));
        contenido.add(encabezado, BorderLayout.NORTH);
        contenido.add(scroll, BorderLayout.CENTER);

        mostrarEstado(claseActual == null ? "Sin clase en este momento para " + matricula + "."
                : "Clase actual: " + claseActual, claseActual == null ? COLOR_AVISO : COLOR_OK);
        JOptionPane.showMessageDialog(this, contenido, "Horario semanal", JOptionPane.PLAIN_MESSAGE);
    }

    private void mostrarReporte() {
        String matricula = txtMatricula.getText().trim();
        if (matricula.isEmpty()) {
            mostrarEstado("Escriba una cédula para ver su reporte.", COLOR_AVISO);
            JOptionPane.showMessageDialog(this, "Escriba la cédula del estudiante para ver su reporte.",
                    "Reporte de asistencia", JOptionPane.WARNING_MESSAGE);
            return;
        }
        Estudiante estudiante = EstudianteDAO.buscarEstudiantePorMatricula(matricula);
        if (estudiante == null) {
            mostrarEstado("Estudiante no registrado: " + matricula, COLOR_ERROR);
            JOptionPane.showMessageDialog(this, "No existe un estudiante con cédula " + matricula + ".",
                    "Reporte de asistencia", JOptionPane.WARNING_MESSAGE);
            return;
        }
        List<Object[]> filas = RegistroDAO.porcentajePorEstudiante(matricula, RegistroDAO.SEMANAS_POR_PARCIAL);
        if (filas.isEmpty()) {
            String motivo = RegistroDAO.getUltimoError().isEmpty()
                    ? estudiante.getNombre() + " no está inscrito en ninguna materia." : RegistroDAO.getUltimoError();
            mostrarEstado(motivo, COLOR_AVISO);
            JOptionPane.showMessageDialog(this, motivo, "Reporte de asistencia", JOptionPane.INFORMATION_MESSAGE);
            return;
        }
        int asistencias = 0;
        int atrasos = 0;
        int clases = 0;
        double suma = 0;
        for (Object[] fila : filas) {
            asistencias += ((Number) fila[1]).intValue();
            clases += ((Number) fila[2]).intValue();
            suma += ((Number) fila[3]).doubleValue();
            atrasos += ((Number) fila[4]).intValue();
        }
        double global = suma / filas.size();

        DefaultTableModel modelo = new DefaultTableModel(RegistroDAO.COLUMNAS_REPORTE, 0) {
            @Override
            public boolean isCellEditable(int fila, int columna) {
                return false;
            }
        };
        RegistroDAO.reporteAsistencia(matricula).forEach(modelo::addRow);
        JTable tablaReporte = new JTable(modelo);
        tablaReporte.setRowHeight(22);
        DefaultTableCellRenderer centrado = new DefaultTableCellRenderer();
        centrado.setHorizontalAlignment(SwingConstants.CENTER);
        for (int i = 1; i < RegistroDAO.COLUMNAS_REPORTE.length; i++) {
            tablaReporte.getColumnModel().getColumn(i).setCellRenderer(centrado);
        }
        tablaReporte.getColumnModel().getColumn(0).setPreferredWidth(260);
        tablaReporte.getColumnModel().getColumn(3).setPreferredWidth(130);
        JScrollPane scroll = new JScrollPane(tablaReporte);
        scroll.setPreferredSize(new Dimension(760, Math.min(300, 40 + 22 * filas.size())));

        JLabel lblAlumno = new JLabel("Estudiante: " + estudiante.getNombre() + " " + estudiante.getApellido()
                + " (" + matricula + ")");
        JLabel lblParcial = new JLabel("Parcial de " + RegistroDAO.SEMANAS_POR_PARCIAL
                + " semanas. Atraso: llegar más de " + TOLERANCIA_ATRASO_MINUTOS + " minutos después del inicio.");
        JLabel lblGlobal = new JLabel(String.format("Porcentaje global: %.0f%%   (%d de %d clases, %d %s)",
                global, asistencias, clases, atrasos, atrasos == 1 ? "atraso" : "atrasos"));
        lblGlobal.setFont(lblGlobal.getFont().deriveFont(java.awt.Font.BOLD, lblGlobal.getFont().getSize() + 3f));
        JPanel encabezado = new JPanel(new GridLayout(2, 1, 0, 4));
        encabezado.add(lblAlumno);
        encabezado.add(lblParcial);
        JPanel contenido = new JPanel(new BorderLayout(0, 10));
        contenido.add(encabezado, BorderLayout.NORTH);
        contenido.add(scroll, BorderLayout.CENTER);
        contenido.add(lblGlobal, BorderLayout.SOUTH);

        refrescarPorcentaje();
        JOptionPane.showMessageDialog(this, contenido, "Reporte de asistencia", JOptionPane.PLAIN_MESSAGE);
    }

    static String diaEnEspanol(String dia) {
        try {
            String nombre = DayOfWeek.valueOf(dia).getDisplayName(TextStyle.FULL, Locale.forLanguageTag("es"));
            return Character.toUpperCase(nombre.charAt(0)) + nombre.substring(1);
        } catch (IllegalArgumentException ex) {
            return dia;
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

    /** Muestra el porcentaje de asistencia del parcial (8 semanas) por materia y el global. */
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
        btnReporte = new javax.swing.JButton();
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

        lblMatricula.setText("Cédula:");

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

        btnHorario.setText("Ver horario");
        btnHorario.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnHorarioActionPerformed(evt);
            }
        });

        btnReporte.setText("Reporte de asistencia");
        btnReporte.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnReporteActionPerformed(evt);
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
                "#", "Cédula", "Estudiante", "Materia", "Fecha", "Hora", "Atraso"
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
                        .addComponent(btnReporte)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(btnRefrescar)
                        .addGap(0, 160, Short.MAX_VALUE))
                    .addGroup(layout.createSequentialGroup()
                        .addComponent(panelCamara, javax.swing.GroupLayout.DEFAULT_SIZE, 520, Short.MAX_VALUE)
                        .addGap(18, 18, 18)
                        .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addComponent(lblRegistros)
                            .addComponent(scrollRegistros, javax.swing.GroupLayout.DEFAULT_SIZE, 680, Short.MAX_VALUE)))
                    .addComponent(lblEstado, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addGroup(layout.createSequentialGroup()
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
                    .addComponent(btnReporte)
                    .addComponent(btnRefrescar))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(panelCamara, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addGroup(layout.createSequentialGroup()
                        .addComponent(lblRegistros)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(scrollRegistros, javax.swing.GroupLayout.DEFAULT_SIZE, 451, Short.MAX_VALUE)))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addComponent(lblEstado)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
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

    private void btnReporteActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnReporteActionPerformed
        mostrarReporte();
    }//GEN-LAST:event_btnReporteActionPerformed

    private void btnRefrescarActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnRefrescarActionPerformed
        actualizarVista();
    }//GEN-LAST:event_btnRefrescarActionPerformed

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new VentanaPrincipal().setVisible(true));
    }

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JButton btnDetener;
    private javax.swing.JButton btnHorario;
    private javax.swing.JButton btnIniciar;
    private javax.swing.JButton btnRefrescar;
    private javax.swing.JButton btnRegistrar;
    private javax.swing.JButton btnReporte;
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
