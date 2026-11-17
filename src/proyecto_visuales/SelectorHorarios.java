package proyecto_visuales;

import java.awt.Component;
import java.awt.FlowLayout;
import java.awt.GridLayout;
import java.time.LocalTime;
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
        SelectorHora[] inicios = new SelectorHora[dias.size()];
        SelectorHora[] fines = new SelectorHora[dias.size()];
        JPanel panel = new JPanel(new GridLayout(0, 3, 8, 4));
        panel.add(new JLabel("Día"));
        panel.add(new JLabel("Desde"));
        panel.add(new JLabel("Hasta"));
        for (int i = 0; i < dias.size(); i++) {
            HorarioMateria actual = buscar(actuales, dias.get(i));
            casillas[i] = new JCheckBox(VentanaPrincipal.diaEnEspanol(dias.get(i)), actual != null);
            inicios[i] = new SelectorHora(actual != null ? actual.horaInicio() : "08:00");
            fines[i] = new SelectorHora(actual != null ? actual.horaFin() : "10:00");
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
                    seleccion.add(new HorarioMateria(dias.get(i), inicios[i].getHora(), fines[i].getHora()));
                }
            }
            String error = validar(seleccion);
            if (error == null) {
                return seleccion;
            }
            JOptionPane.showMessageDialog(padre, error, "Horario no válido", JOptionPane.WARNING_MESSAGE);
        }
    }

    static final class SelectorHora extends JPanel {
        private final JComboBox<Integer> horas = new JComboBox<>();
        private final JComboBox<String> minutos = new JComboBox<>();

        SelectorHora(String hora) {
            super(new FlowLayout(FlowLayout.LEFT, 2, 0));
            for (int h = ValidadorHorarios.INICIO_JORNADA.getHour(); h <= ValidadorHorarios.FIN_JORNADA.getHour(); h++) {
                horas.addItem(h);
            }
            for (int m = 0; m < 60; m++) {
                minutos.addItem(String.format("%02d", m));
            }
            horas.setMaximumRowCount(14);
            minutos.setMaximumRowCount(12);
            horas.setRenderer(new DefaultListCellRenderer() {
                @Override
                public Component getListCellRendererComponent(JList<?> lista, Object valor, int indice,
                        boolean seleccionado, boolean foco) {
                    return super.getListCellRendererComponent(lista, valor == null ? "" : etiqueta((Integer) valor),
                            indice, seleccionado, foco);
                }
            });
            add(horas);
            add(new JLabel(":"));
            add(minutos);
            setHora(hora);
        }

        static String etiqueta(int hora24) {
            int hora12 = hora24 % 12 == 0 ? 12 : hora24 % 12;
            return hora12 + (hora24 < 12 ? " AM" : " PM");
        }

        void setHora(String hora) {
            try {
                LocalTime t = LocalTime.parse(ValidadorHorarios.normalizarHora(hora).substring(0, 5));
                if (t.getHour() >= ValidadorHorarios.INICIO_JORNADA.getHour()
                        && t.getHour() <= ValidadorHorarios.FIN_JORNADA.getHour()) {
                    horas.setSelectedItem(t.getHour());
                    minutos.setSelectedIndex(t.getMinute());
                }
            } catch (RuntimeException ex) {
                horas.setSelectedIndex(0);
            }
        }

        String getHora() {
            return String.format("%02d:%s", (Integer) horas.getSelectedItem(), minutos.getSelectedItem());
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
            if (!ValidadorHorarios.dentroDeJornada(h.horaInicio(), h.horaFin())) {
                return "Horario inválido el " + VentanaPrincipal.diaEnEspanol(h.diaSemana())
                        + ": las clases van de 7:00 AM a 8:00 PM.";
            }
        }
        return null;
    }
}
