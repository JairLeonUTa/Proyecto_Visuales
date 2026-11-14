package proyecto_visuales;

import java.awt.Component;
import java.awt.GridLayout;
import java.util.ArrayList;
import java.util.List;
import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JTextField;

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
        JTextField[] inicios = new JTextField[dias.size()];
        JTextField[] fines = new JTextField[dias.size()];
        JPanel panel = new JPanel(new GridLayout(0, 3, 8, 4));
        panel.add(new JLabel("Día"));
        panel.add(new JLabel("Desde (HH:mm)"));
        panel.add(new JLabel("Hasta (HH:mm)"));
        for (int i = 0; i < dias.size(); i++) {
            HorarioMateria actual = buscar(actuales, dias.get(i));
            casillas[i] = new JCheckBox(dias.get(i), actual != null);
            inicios[i] = new JTextField(actual != null ? actual.horaInicio() : "08:00", 6);
            fines[i] = new JTextField(actual != null ? actual.horaFin() : "10:00", 6);
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
                    seleccion.add(new HorarioMateria(dias.get(i), inicios[i].getText().trim(), fines[i].getText().trim()));
                }
            }
            String error = validar(seleccion);
            if (error == null) {
                return seleccion;
            }
            JOptionPane.showMessageDialog(padre, error, "Horario no válido", JOptionPane.WARNING_MESSAGE);
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
                return "Horario inválido el " + h.diaSemana()
                        + ": use HH:mm y una hora de inicio anterior a la de fin.";
            }
        }
        return null;
    }
}
