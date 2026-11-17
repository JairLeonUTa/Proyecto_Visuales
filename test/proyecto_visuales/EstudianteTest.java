package proyecto_visuales;

public class EstudianteTest {

    public static void main(String[] args) {
        Estudiante est = new Estudiante("0102030405", "Ana", "Pérez", "ana@uta.edu.ec");
        verificar("0102030405".equals(est.getMatricula()), "el constructor guarda la cédula");
        verificar("Ana".equals(est.getNombre()), "el constructor guarda el nombre");
        verificar("Pérez".equals(est.getApellido()), "el apellido va separado del nombre");
        verificar("ana@uta.edu.ec".equals(est.getEmail()), "el constructor guarda el email");

        Estudiante vacio = new Estudiante();
        verificar(vacio.getMatricula() == null, "un estudiante nuevo no tiene cédula");
        vacio.setMatricula("0102030406");
        vacio.setNombre("Luis");
        vacio.setApellido("Mora");
        vacio.setEmail("");
        verificar("0102030406".equals(vacio.getMatricula()) && "Luis".equals(vacio.getNombre())
                && "Mora".equals(vacio.getApellido()) && "".equals(vacio.getEmail()),
                "los setters actualizan cada campo");
        verificar(Estudiante.cedulaValida("0102030405"), "una cédula de 10 dígitos es válida");
        verificar(!Estudiante.cedulaValida("20230001"), "8 dígitos no es una cédula");
        verificar(!Estudiante.cedulaValida("01020304051"), "11 dígitos no es una cédula");
        verificar(!Estudiante.cedulaValida("01020A0405"), "la cédula solo tiene números");
        System.out.println("OK: pruebas del modelo Estudiante superadas");
    }

    private static void verificar(boolean condicion, String mensaje) {
        if (!condicion) {
            throw new AssertionError(mensaje);
        }
    }
}
