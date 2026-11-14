package proyecto_visuales;

import java.awt.Component;
import java.util.ArrayList;
import java.util.List;
import javax.swing.JList;
import javax.swing.JOptionPane;
import javax.swing.JScrollPane;

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
        JList<String> lista = new JList<>(dias.toArray(new String[0]));
        lista.setVisibleRowCount(dias.size());
        int opcion = JOptionPane.showConfirmDialog(padre, new JScrollPane(lista),
                "Seleccione los días de la materia", JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);
        if (opcion != JOptionPane.OK_OPTION) {
            return null;
        }
        List<HorarioMateria> seleccion = new ArrayList<>();
        for (String dia : lista.getSelectedValuesList()) {
            seleccion.add(new HorarioMateria(dia, "08:00", "10:00"));
        }
        return seleccion;
    }
}
