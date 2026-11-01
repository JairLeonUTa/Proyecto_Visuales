-- Esquema de la base "proyecto" (MySQL/XAMPP) - Sistema de asistencia QR
-- Ejecutar en phpMyAdmin > SQL
CREATE DATABASE IF NOT EXISTS proyecto CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
USE proyecto;

CREATE TABLE IF NOT EXISTS estudiantes (
    id_estudiante INT AUTO_INCREMENT PRIMARY KEY,
    matricula VARCHAR(30) NOT NULL UNIQUE,
    nombre VARCHAR(100) NOT NULL,
    apellido VARCHAR(100) NOT NULL,
    email VARCHAR(150)
);

CREATE TABLE IF NOT EXISTS materias (
    id_materia INT AUTO_INCREMENT PRIMARY KEY,
    codigo VARCHAR(30) NOT NULL UNIQUE,
    nombre VARCHAR(120) NOT NULL,
    dia_semana VARCHAR(15) NOT NULL,      -- MONDAY, TUESDAY... (DayOfWeek.name())
    hora_inicio TIME NOT NULL,
    hora_fin TIME NOT NULL
);

CREATE TABLE IF NOT EXISTS inscripciones (
    id_estudiante INT NOT NULL,
    id_materia INT NOT NULL,
    PRIMARY KEY (id_estudiante, id_materia),
    FOREIGN KEY (id_estudiante) REFERENCES estudiantes(id_estudiante) ON DELETE CASCADE,
    FOREIGN KEY (id_materia) REFERENCES materias(id_materia) ON DELETE CASCADE
);

CREATE TABLE IF NOT EXISTS registros (
    id_registro INT AUTO_INCREMENT PRIMARY KEY,
    id_estudiante INT NOT NULL,
    id_materia INT NOT NULL,
    fecha DATE NOT NULL,
    hora_entrada TIME NOT NULL,
    es_atraso TINYINT(1) NOT NULL DEFAULT 0,
    UNIQUE KEY uq_asistencia_dia (id_estudiante, id_materia, fecha),
    FOREIGN KEY (id_estudiante) REFERENCES estudiantes(id_estudiante),
    FOREIGN KEY (id_materia) REFERENCES materias(id_materia)
);
