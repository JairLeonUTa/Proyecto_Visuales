# Defensa del proyecto: control de asistencia mediante QR

**Duración:** 10 minutos, incluida una demostración de 1 minuto y medio.

> Guía para exponer: explica las ideas con tus palabras. No leas las tablas completas ni el código generado por NetBeans.

## 1. Objetivo — 0:00 a 1:00

“Nuestro proyecto es una aplicación de escritorio que permite controlar la asistencia de estudiantes mediante códigos QR. También admite el registro manual de la cédula.

El sistema permite crear estudiantes, generar sus QR, crear materias con horarios, realizar inscripciones y consultar asistencias, atrasos y porcentajes.

Su objetivo es agilizar el registro y evitar asistencias duplicadas o fuera del horario de clase.”

## 2. Organización del código — 1:00 a 2:00

“La aplicación está desarrollada en Java con Swing para la interfaz y MySQL para almacenar los datos. Se organiza en ventanas, modelos y clases de acceso a datos, llamadas DAO.”

```text
Usuario → Ventanas → DAO → Conexión JDBC → MySQL
```

| Archivo en `src/proyecto_visuales/` | Función |
|---|---|
| `VentanaPrincipal.java` | Cámara, registro manual, horarios y reportes. |
| `VentanaAdmin.java` | Crear, actualizar y eliminar estudiantes; generar QR, materias e inscripciones. |
| `Estudiante.java` | Modelo con cédula, nombre, apellido y email. |
| `HorarioMateria.java` | `record` con día, hora de inicio y hora de fin. |
| `EstudianteDAO.java` | Crear, buscar, listar, actualizar y eliminar estudiantes. |
| `MateriaDAO.java` | Guardar materias y consultar sus horarios. |
| `InscripcionDAO.java` | Relacionar estudiantes con materias. |
| `RegistroDAO.java` | Guardar asistencias y calcular porcentajes. |
| `conexion.java` | Abrir la conexión con MySQL. |
| `ValidadorHorarios.java` | Comprobar días y rangos de horas. |
| `SelectorHorarios.java` | Diálogo para seleccionar días y horarios. |

**Punto de entrada:** `VentanaPrincipal.main()`. La clase `Proyecto_Visuales.java` tiene un `main` vacío y no es el inicio configurado.

**Archivos de apoyo:** `.form` guarda el diseño de NetBeans; `lib/` contiene bibliotecas; `database/`, los scripts SQL; `qrs/`, las imágenes; `test/`, las pruebas; `build/`, los archivos compilados.

## 3. Base de datos — 2:00 a 3:00

“La base se llama `proyecto` y tiene cinco tablas.”

| Tabla | Información |
|---|---|
| `estudiantes` | Datos personales y cédula única. |
| `materias` | Código, nombre y un horario conservado en sus columnas. |
| `horarios_materia` | Todos los días y horarios de cada materia. |
| `inscripciones` | Relación entre estudiantes y materias. |
| `registros` | Fecha, hora de entrada y condición de atraso. |

“Un estudiante puede inscribirse en varias materias y una materia puede tener varios estudiantes. Cada materia tiene varios horarios.

La base utiliza claves foráneas para mantener las relaciones y restricciones únicas para impedir cédulas, inscripciones y asistencias duplicadas.”

**Regla clave:** solo una asistencia por estudiante, materia y fecha.

**Detalle técnico:** se guarda el primer horario en `materias`, pero la consulta de clase actual y el horario semanal utilizan `horarios_materia`. El script de migración copia los horarios de una base anterior a esta tabla.

## 4. Administración y validaciones — 3:00 a 4:00

“Primero se crea el estudiante. Cédula, nombre y apellido son obligatorios; el email es opcional. Después se genera un QR que contiene únicamente su cédula y se guarda como imagen PNG en `qrs/`.

Luego se crea una materia con al menos dos días distintos de lunes a viernes. Cada día tiene su propio horario y la hora de inicio debe ser anterior a la de fin.

Finalmente se inscribe al estudiante en la materia. Crear al estudiante no significa que ya esté inscrito.”

“También se puede seleccionar un estudiante para actualizar sus datos o eliminarlo con confirmación. La cédula no cambia al actualizar. Eliminar borra permanentemente al estudiante, sus asistencias e inscripciones; se advierte antes de confirmar. La opción Nuevo estudiante permite volver al modo de creación.”

**Integridad:** la materia y sus horarios se guardan en una transacción: si falla una parte, se ejecuta `rollback` para deshacer la operación.

## 5. Registro de asistencia — 4:00 a 6:00

“La cámara toma una imagen aproximadamente cada 300 milisegundos. ZXing interpreta el QR y obtiene su texto.

Tanto la lectura de cámara como el registro manual llaman al mismo método: `procesarQR()`.”

```text
Obtener cédula
       ↓
Buscar estudiante
       ↓
Consultar materias inscritas con clase ahora
       ↓
Calcular atraso
       ↓
Comprobar duplicados y guardar
       ↓
Actualizar tabla y porcentajes
```

“Para registrar, el estudiante debe existir, estar inscrito y tener clase en el día y la hora actuales. El horario incluye el inicio y el fin.

Existe una tolerancia de cinco minutos: si la clase empieza a las 08:00, llegar a las 08:05 no es atraso; llegar a las 08:06 sí.”

| Llegada a una clase de 08:00 a 10:00 | Resultado |
|---|---|
| 07:59 | Fuera de horario. |
| 08:05 | A tiempo. |
| 08:06 | Asistencia con atraso. |
| 10:01 | Fuera de horario. |

“Para evitar lecturas continuas, se ignora la misma cédula durante 2,5 segundos. Además, el DAO verifica duplicados y MySQL tiene una restricción única.”

**Separación importante:** la ventana comprueba el horario y calcula el atraso. `RegistroDAO` verifica estudiante, materia, inscripción y duplicado antes de insertar.

## 6. Reportes y porcentajes — 6:00 a 7:00

“El sistema muestra los últimos 200 registros y permite consultar el horario semanal y el reporte de un estudiante.”

```text
Clases esperadas = 8 semanas × días de clase por semana
Porcentaje = asistencias × 100 / clases esperadas
```

**Ejemplo:** dos días por semana equivalen a 16 clases. Si el estudiante registra 12 asistencias, obtiene un 75 %.

“Los atrasos cuentan como asistencia y también se muestran por separado. El porcentaje global es el promedio de los porcentajes de las materias.”

**Precisión para la defensa:** actualmente se cuentan registros de los últimos 56 días; no hay fechas configuradas de inicio y fin del parcial.

## 7. Tecnología, pruebas y límites — 7:00 a 8:00

“El proyecto está configurado para Java 21 y se compila con Ant desde NetBeans. Utiliza Swing para las ventanas, JDBC y MySQL Connector para la base, ZXing para los QR y Webcam Capture para la cámara.

Las consultas con datos del usuario utilizan `PreparedStatement`, que separa los parámetros del SQL. Los bloques `try-with-resources` cierran conexiones y consultas.

La lectura de cámara utiliza un hilo separado y las actualizaciones de Swing se envían con `invokeLater`.”

**Pruebas existentes:** modelos, validación de horarios, conexión, operaciones de estudiantes, duplicados, porcentajes y decodificación de QR. Son clases con `main()` y comprobaciones; varias requieren MySQL y crean y eliminan datos de prueba.

**Limitaciones actuales:**

- No hay inicio de sesión para Administración.
- Un QR se puede copiar: identifica una cédula, no verifica identidad.
- No se detectan cruces de horarios; se toma la primera materia encontrada.
- Algunas consultas pueden bloquear temporalmente la interfaz.

## 8. Demostración — 8:00 a 9:30

### Preparación antes de exponer

- Iniciar MySQL y cargar `database/schema.sql`.
- Confirmar la conexión a `localhost/proyecto`.
- Tener un estudiante inscrito y su QR disponible.
- Configurar una materia con clase en el día y la hora de la defensa.
- Comprobar la cámara; tener el registro manual como alternativa.

### Recorrido en vivo

1. Mostrar Administración: selector de estudiantes, botones de actualizar y eliminar, materia e inscripción.
2. Volver a la ventana principal y registrar con QR o cédula.
3. Señalar el registro y el indicador de atraso.
4. Esperar más de 2,5 segundos y repetir para mostrar el rechazo del duplicado.
5. Abrir el horario y el reporte de asistencia.

> Si no hay clase en ese momento, el rechazo por horario es el comportamiento correcto. No confundirlo con un fallo de la cámara.

## 9. Cierre — 9:30 a 10:00

“En conclusión, el proyecto integra una interfaz gráfica, lectura de QR y una base de datos para gestionar asistencia.

Su lógica central comprueba la inscripción, el horario y los duplicados antes de registrar. Además, ofrece información de atrasos y porcentajes.

Como mejoras futuras proponemos autenticación, detección de cruces de horarios y periodos académicos con fechas definidas.”

---

## Preguntas posibles — apoyo, fuera del guion

**¿Qué contiene el QR?**

Solo la cédula. Los demás datos se consultan en MySQL.

**¿Por qué usar DAO?**

Para separar las consultas SQL de la interfaz.

**¿Cómo evitan duplicados?**

Con una comprobación en el DAO y una restricción única en MySQL.

**¿Qué pasa si falla la cámara?**

Se informa el problema y puede utilizarse el registro manual.

**¿Qué pasa si falla MySQL?**

No se puede guardar la asistencia; el sistema informa el problema.

**¿Por qué usar una transacción?**

Para guardar materia y horarios como una sola operación o deshacer todo si falla.

**¿El sistema comprueba que el QR pertenece a quien lo presenta?**

No. Esa verificación de identidad no está implementada.

**¿El porcentaje global se calcula con todas las clases juntas?**

No. Actualmente es el promedio de los porcentajes por materia.

**¿Actualizar y eliminar estudiantes está en la ventana?**

Sí. Se selecciona un estudiante para cargar sus datos. Actualizar guarda nombre, apellido y email. Eliminar pide confirmación y borra al estudiante junto con sus asistencias e inscripciones, sin posibilidad de deshacer.
