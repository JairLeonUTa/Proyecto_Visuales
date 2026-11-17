package proyecto_visuales;

import java.awt.Component;
import java.awt.GridLayout;
import java.util.ArrayList;
import java.util.List;
import javax.swing.DefaultListCellRenderer;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JOptionPane;
import javax.swing.JPanel;

/** Diálogo de administración para elegir los días laborables de una materia y su horario. */
public final class SelectorHorarios {

    private SelectorHorarios() {
    }

    /**
     * Muestra el selector y devuelve los horarios elegidos, o null si se cancela.
     * @param actuales horarios configurados hasta ahora
     */
    public static List<HorarioMateria> seleccionar(Component padre, List<HorarioMateria> actuales) {
        List<String> dias = ValidadorHorarios.DIAS_LABORABLES;
        JCheckBox[] casillas = new JCheckBox[dias.size()];
        JComboBox<String>[] inicios = nuevosCombos(dias.size());
        JComboBox<String>[] fines = nuevosCombos(dias.size());
        JPanel panel = new JPanel(new GridLayout(0, 3, 8, 4));
        panel.add(new JLabel("Día"));
        panel.add(new JLabel("Desde"));
        panel.add(new JLabel("Hasta"));
        for (int i = 0; i < dias.size(); i++) {
            HorarioMateria actual = buscar(actuales, dias.get(i));
            casillas[i] = new JCheckBox(VentanaPrincipal.diaEnEspanol(dias.get(i)), actual != null);
            elegir(inicios[i], actual != null ? actual.horaInicio() : "08:00");
            elegir(fines[i], actual != null ? actual.horaFin() : "10:00");
            panel.add(casillas[i]);
            panel.add(inicios[i]);
            panel.add(fines[i]);
        }
        while (true) {
            int opcion = JOptionPane.showConfirmDialog(padre, panel,
                    "Seleccione días y configure su horario", JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);
            if (opcion != JOptionPane.OK_OPTION) {
                return null;
            }
            List<HorarioMateria> seleccion = new ArrayList<>();
            for (int i = 0; i < dias.size(); i++) {
                if (casillas[i].isSelected()) {
                    seleccion.add(new HorarioMateria(dias.get(i), (String) inicios[i].getSelectedItem(),
                            (String) fines[i].getSelectedItem()));
                }
            }
            String error = validar(seleccion);
            if (error == null) {
                return seleccion;
            }
            JOptionPane.showMessageDialog(padre, error, "Horario no válido", JOptionPane.WARNING_MESSAGE);
        }
    }

    @SuppressWarnings("unchecked")
    private static JComboBox<String>[] nuevosCombos(int cantidad) {
        JComboBox<String>[] combos = new JComboBox[cantidad];
        for (int i = 0; i < cantidad; i++) {
            combos[i] = new JComboBox<>(ValidadorHorarios.HORAS_CLASE.toArray(new String[0]));
            combos[i].setMaximumRowCount(12);
            combos[i].setRenderer(new DefaultListCellRenderer() {
                @Override
                public Component getListCellRendererComponent(JList<?> lista, Object valor, int indice,
                        boolean seleccionado, boolean foco) {
                    return super.getListCellRendererComponent(lista,
                            valor == null ? "" : ValidadorHorarios.horaAmPm(valor.toString()), indice, seleccionado, foco);
                }
            });
        }
        return combos;
    }

    private static void elegir(JComboBox<String> combo, String hora) {
        String valor = ValidadorHorarios.normalizarHora(hora);
        valor = valor.length() >= 5 ? valor.substring(0, 5) : valor;
        if (ValidadorHorarios.HORAS_CLASE.contains(valor)) {
            combo.setSelectedItem(valor);
        }
    }

    /** Devuelve el horario ya configurado para ese día, o null si el día no estaba elegido. */
    private static HorarioMateria buscar(List<HorarioMateria> horarios, String dia) {
        if (horarios == null) {
            return null;
        }
        for (HorarioMateria h : horarios) {
            if (h != null && dia.equals(h.diaSemana())) {
                return h;
            }
        }
        return null;
    }

    /** Devuelve el motivo por el que la selección no sirve, o null si es válida. */
    static String validar(List<HorarioMateria> seleccion) {
        if (!ValidadorHorarios.diasValidos(seleccion.stream().map(HorarioMateria::diaSemana).toList())) {
            return "Marque al menos dos días de lunes a viernes.";
        }
        for (HorarioMateria h : seleccion) {
            if (!ValidadorHorarios.horarioValido(h.horaInicio(), h.horaFin())) {
                return "Horario inválido el " + VentanaPrincipal.diaEnEspanol(h.diaSemana())
                        + ": la hora de inicio debe ser anterior a la de fin.";
            }
        }
        return null;
    }
}
