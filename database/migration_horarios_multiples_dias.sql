USE proyecto;

CREATE TABLE IF NOT EXISTS horarios_materia (
    id_horario INT AUTO_INCREMENT PRIMARY KEY,
    id_materia INT NOT NULL,
    dia_semana VARCHAR(15) NOT NULL,
    hora_inicio TIME NOT NULL,
    hora_fin TIME NOT NULL,
    UNIQUE KEY uq_materia_dia (id_materia, dia_semana),
    FOREIGN KEY (id_materia) REFERENCES materias(id_materia) ON DELETE CASCADE
);

INSERT IGNORE INTO horarios_materia (id_materia, dia_semana, hora_inicio, hora_fin)
SELECT id_materia, dia_semana, hora_inicio, hora_fin
FROM materias;
