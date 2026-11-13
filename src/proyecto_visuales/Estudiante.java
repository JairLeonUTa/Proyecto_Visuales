package proyecto_visuales;

public class Estudiante {
    private String matricula;
    private String nombre;
    private String apellido;
    private String email;

    public Estudiante() {
    }

    public Estudiante(String matricula, String nombre, String apellido, String email) {
        this.matricula = matricula;
        this.nombre = nombre;
        this.apellido = apellido;
        this.email = email;
    }

    public String getMatricula() {
        return matricula;
    }

    public void setMatricula(String matricula) {
        this.matricula = matricula;
    }

    public String getNombre() {
        return nombre;
    }

    public void setNombre(String nombre) {
        this.nombre = nombre;
    }

    public String getApellido() {
        return apellido;
    }

    public void setApellido(String apellido) {
        this.apellido = apellido;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }
}
