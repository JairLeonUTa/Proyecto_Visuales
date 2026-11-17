package proyecto_visuales;

import java.awt.Component;
import java.awt.Container;
import java.awt.Window;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;
import javax.swing.Timer;

/** Integración con MySQL: usa identificadores únicos y elimina solo sus propios datos. */
public class VentanaAdminCrudTest {

    public static void main(String[] args) throws Exception {
        String id = "TEST-ADM-" + UUID.randomUUID().toString().substring(0, 8);
        try {
            verificar(MateriaDAO.guardarMateria(id, "Materia de prueba", List.of(
                    new HorarioMateria("MONDAY", "08:00", "10:00"),
                    new HorarioMateria("WEDNESDAY", "08:00", "10:00"))), "crear materia");
            crearEstudianteInscrito(id);
            verificar(EstudianteDAO.eliminarEstudiante(id), "elimina estudiantes inscritos sin asistencias");
            crearEstudianteInscrito(id);
            SwingUtilities.invokeAndWait(() -> probarVentana(id));
        } finally {
            ejecutar("DELETE FROM registros WHERE id_estudiante = "
                    + "(SELECT id_estudiante FROM estudiantes WHERE matricula = ?)", id);
            ejecutar("DELETE FROM inscripciones WHERE id_estudiante = "
                    + "(SELECT id_estudiante FROM estudiantes WHERE matricula = ?)", id);
            ejecutar("DELETE FROM estudiantes WHERE matricula = ?", id);
            ejecutar("DELETE FROM horarios_materia WHERE id_materia = "
                    + "(SELECT id_materia FROM materias WHERE codigo = ?)", id);
            ejecutar("DELETE FROM materias WHERE codigo = ?", id);
        }
        System.out.println("OK: selección, actualización, cancelación y eliminación de estudiantes");
    }

    private static void crearEstudianteInscrito(String id) {
        verificar(EstudianteDAO.guardarEstudiante(new Estudiante(id, "Ana", "Prueba", "")), "crear estudiante");
        verificar(InscripcionDAO.inscribir(id, id), "inscribir estudiante");
    }

    private static void probarVentana(String id) {
        VentanaAdmin ventana = new VentanaAdmin();
        try {
            JComboBox<?> selector = campo(ventana, "cmbEditarEstudiante", JComboBox.class);
            JButton actualizar = campo(ventana, "btnActualizarEstudiante", JButton.class);
            JButton eliminar = campo(ventana, "btnEliminarEstudiante", JButton.class);
            JButton crear = campo(ventana, "btnCrearEstudiante", JButton.class);
            JTextField matricula = campo(ventana, "txtMatricula", JTextField.class);
            JTextField nombre = campo(ventana, "txtNombre", JTextField.class);
            JTextField apellido = campo(ventana, "txtApellido", JTextField.class);
            JTextField email = campo(ventana, "txtEmail", JTextField.class);
            JLabel estado = campo(ventana, "lblEstado", JLabel.class);
            verificar(crear.isEnabled() && !actualizar.isEnabled() && !eliminar.isEnabled(), "estado de creación");
            seleccionar(selector, id);
            verificar(id.equals(matricula.getText()) && !matricula.isEditable(), "cédula cargada y protegida");
            verificar("Ana".equals(nombre.getText()) && "Prueba".equals(apellido.getText())
                    && email.getText().isEmpty(), "carga todos los datos");
            verificar(!crear.isEnabled() && actualizar.isEnabled() && eliminar.isEnabled(), "estado de edición");
            JLabel qr = campo(ventana, "lblQr", JLabel.class);
            verificar(qr.getIcon() != null, "muestra el QR del estudiante seleccionado");

            nombre.setText(" ");
            actualizar.doClick();
            verificar("Ana".equals(EstudianteDAO.buscarEstudiantePorMatricula(id).getNombre()), "rechaza nombre vacío");
            nombre.setText("Actualizada");
            apellido.setText("Nueva");
            email.setText("prueba@example.com");
            actualizar.doClick();
            Estudiante guardado = EstudianteDAO.buscarEstudiantePorMatricula(id);
            verificar("Actualizada".equals(guardado.getNombre()) && "Nueva".equals(guardado.getApellido())
                    && "prueba@example.com".equals(guardado.getEmail()), "guarda los cambios");
            JComboBox<?> inscripciones = campo(ventana, "cmbEstudiantes", JComboBox.class);
            verificar(inscripciones.getSelectedItem().toString().contains("Actualizada Nueva"), "refresca inscripciones");

            selector.setSelectedIndex(0);
            verificar(matricula.isEditable() && matricula.getText().isEmpty() && nombre.getText().isEmpty()
                    && apellido.getText().isEmpty() && email.getText().isEmpty()
                    && crear.isEnabled() && !actualizar.isEnabled() && !eliminar.isEnabled(), "nuevo limpia el formulario");
            verificar(qr.getIcon() == null, "nuevo quita el QR anterior");
            seleccionar(selector, id);
            verificar(RegistroDAO.guardarRegistro(id, id, LocalDate.now().toString(), "08:00", false), "crear asistencia");
            responderEliminacion(ventana, eliminar, "Cancelar");
            verificar(EstudianteDAO.buscarEstudiantePorMatricula(id) != null
                    && InscripcionDAO.estaInscrito(id, id)
                    && RegistroDAO.listarRegistrosPorMatricula(id, 10).size() == 1,
                    "cancelar conserva estudiante, inscripciones y asistencias");
            responderEliminacion(ventana, eliminar, "Eliminar");
            verificar(EstudianteDAO.buscarEstudiantePorMatricula(id) == null, "elimina aun teniendo asistencias");
            verificar(!InscripcionDAO.estaInscrito(id, id)
                    && RegistroDAO.listarRegistrosPorMatricula(id, 10).isEmpty(), "elimina inscripciones y asistencias");
            verificar(estado.getText().contains("eliminad"), "informa la eliminación");
            verificar(selector.getSelectedIndex() == 0 && crear.isEnabled() && !eliminar.isEnabled(), "restablece creación");
            for (int i = 0; i < inscripciones.getItemCount(); i++) {
                verificar(!inscripciones.getItemAt(i).toString().startsWith(id + " - "), "retira del combo de inscripciones");
            }
        } finally {
            ventana.dispose();
        }
    }

    private static void responderEliminacion(VentanaAdmin ventana, JButton boton, String opcion) {
        boolean[] respondido = {false};
        boolean[] cancelarPorDefecto = {false};
        boolean[] advertenciaPermanente = {false};
        Timer respuesta = new Timer(50, e -> {
            for (Window dialogo : ventana.getOwnedWindows()) {
                if (dialogo instanceof JDialog && dialogo.isVisible()) {
                    JOptionPane panel = buscar(dialogo, JOptionPane.class);
                    if (panel != null) {
                        cancelarPorDefecto[0] = "Cancelar".equals(panel.getInitialValue());
                        String mensaje = String.valueOf(panel.getMessage());
                        advertenciaPermanente[0] = mensaje.contains("asistencias")
                                && mensaje.contains("inscripciones") && mensaje.contains("permanente")
                                && mensaje.contains("no se puede deshacer");
                        respondido[0] = true;
                        panel.setValue(opcion);
                    }
                }
            }
        });
        respuesta.start();
        try {
            boton.doClick();
        } finally {
            respuesta.stop();
        }
        verificar(respondido[0] && cancelarPorDefecto[0], "confirma la eliminación con cancelar por defecto");
        verificar(advertenciaPermanente[0], "advierte que se eliminará el historial definitivamente");
    }

    private static void seleccionar(JComboBox<?> selector, String id) {
        for (int i = 0; i < selector.getItemCount(); i++) {
            if (selector.getItemAt(i).toString().startsWith(id + " - ")) {
                selector.setSelectedIndex(i);
                return;
            }
        }
        throw new AssertionError("El selector no contiene al estudiante");
    }

    private static <T> T campo(VentanaAdmin ventana, String nombre, Class<T> tipo) {
        try {
            var campo = VentanaAdmin.class.getDeclaredField(nombre);
            campo.setAccessible(true);
            return tipo.cast(campo.get(ventana));
        } catch (ReflectiveOperationException ex) {
            throw new AssertionError("No se encontró el control " + nombre, ex);
        }
    }

    private static <T extends Component> T buscar(Component componente, Class<T> tipo) {
        if (tipo.isInstance(componente)) {
            return tipo.cast(componente);
        }
        if (componente instanceof Container contenedor) {
            for (Component hijo : contenedor.getComponents()) {
                T encontrado = buscar(hijo, tipo);
                if (encontrado != null) {
                    return encontrado;
                }
            }
        }
        return null;
    }

    private static void ejecutar(String sql, String id) {
        try (var cc = new conexion().conectar(); var ps = cc.prepareStatement(sql)) {
            ps.setString(1, id);
            ps.executeUpdate();
        } catch (Exception ex) {
            throw new IllegalStateException("No se pudo limpiar la prueba", ex);
        }
    }

    private static void verificar(boolean condicion, String mensaje) {
        if (!condicion) {
            throw new AssertionError(mensaje);
        }
    }
}
