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
        JPanel panel = new JPanel(new GridLayout(0, 2, 8, 4));
        panel.add(new JLabel("Día"));
        panel.add(new JLabel("Desde (HH:mm)"));
        for (int i = 0; i < dias.size(); i++) {
            casillas[i] = new JCheckBox(dias.get(i));
            inicios[i] = new JTextField("08:00", 6);
            panel.add(casillas[i]);
            panel.add(inicios[i]);
        }
        int opcion = JOptionPane.showConfirmDialog(padre, panel,
                "Seleccione días y hora de inicio", JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);
        if (opcion != JOptionPane.OK_OPTION) {
            return null;
        }
        List<HorarioMateria> seleccion = new ArrayList<>();
        for (int i = 0; i < dias.size(); i++) {
            if (casillas[i].isSelected()) {
                seleccion.add(new HorarioMateria(dias.get(i), inicios[i].getText().trim(), "10:00"));
            }
        }
        return seleccion;
    }
}
