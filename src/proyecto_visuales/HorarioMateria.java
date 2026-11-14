package proyecto_visuales;

/**
 * Horario independiente para un día de una materia.
 * El día usa los nombres de DayOfWeek (MONDAY...FRIDAY) y las horas el formato HH:mm.
 */
public record HorarioMateria(String diaSemana, String horaInicio, String horaFin) {
}
