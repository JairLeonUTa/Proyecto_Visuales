package proyecto_visuales;

public class EstudianteTest {

    public static void main(String[] args) {
        Estudiante est = new Estudiante("20230001", "Ana", "Pérez", "ana@uta.edu.ec");
        verificar("20230001".equals(est.getMatricula()), "el constructor guarda la matrícula");
        verificar("Ana".equals(est.getNombre()), "el constructor guarda el nombre");
        verificar("Pérez".equals(est.getApellido()), "el apellido va separado del nombre");
        verificar("ana@uta.edu.ec".equals(est.getEmail()), "el constructor guarda el email");

        Estudiante vacio = new Estudiante();
        verificar(vacio.getMatricula() == null, "un estudiante nuevo no tiene matrícula");
        vacio.setMatricula("20230002");
        vacio.setNombre("Luis");
        vacio.setApellido("Mora");
        vacio.setEmail("");
        verificar("20230002".equals(vacio.getMatricula()) && "Luis".equals(vacio.getNombre())
                && "Mora".equals(vacio.getApellido()) && "".equals(vacio.getEmail()),
                "los setters actualizan cada campo");
        System.out.println("OK: pruebas del modelo Estudiante superadas");
    }

    private static void verificar(boolean condicion, String mensaje) {
        if (!condicion) {
            throw new AssertionError(mensaje);
        }
    }
}
