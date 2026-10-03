-- =====================================================================
-- SIGRID - Sistema de Gestión de Reservas de Instalaciones y Socios
-- Polideportivo UNSE - Esquema de base de datos (DDL)
--
-- Fuente : ERS v1.8 + especificación del modelo de datos + bienesoftDB.sql.
-- Motor  : MySQL 8.0.16+ (InnoDB, utf8mb4); CHECK aplicados desde 8.0.16.
-- Stack  : JSF + JPA + GlassFish (DAO/DTO/beans). Todo lo que se resuelve
--          con Java queda FUERA de la BD.
-- Alcance: estructura + tres bloques finales de datos: (10) lo mínimo para poder
--          ingresar (roles y una cuenta de administrador), (11) datos de prueba
--          para desarrollo y (12) las cuentas armadas para la demo con clientes.
--          El archivo se corre entero y recrea la base desde cero (DROP DATABASE).
--
-- Convenciones:
--   * IDs INT UNSIGNED AUTO_INCREMENT.
--   * Todas las FK son ON DELETE RESTRICT.
--   * Bajas lógicas (baja_logica / campo estado); nunca DELETE físico.
--
-- Fuera de la BD (se resuelve en Java):
--   * Recuperar contraseña, notificaciones por email, auditoría de acciones,
--     FAQ y muro de novedades: no hay tablas para eso.
--   * Un pago es de MEMBRESÍA si lo referencia suscripcion_socio y de
--     ALQUILER si lo referencia reserva (no hay concepto_pago ni medio_pago:
--     el único medio es transferencia bancaria, sin pasarela).
--   * socio.estado, carnet_digital.estado y suscripcion_socio.estado
--     (VIGENTE -> VENCIDA) los escribe un job/timer o el servicio.
--   * Reservas PENDIENTE_PAGO sin comprobante (id_pago NULL): un timer las
--     pasa a CANCELADA tras N minutos para liberar el turno (RF-04.8).
--   * Plazos de 48 h de anticipación para cancelar/reprogramar y ventana de 30 días
--     de reprogramación (el crédito es la propia reserva CANCELADA con fecha_limite_reprogramacion).
--   * Coherencia de roles (socio -> rol SOCIO; id_admin_* -> ADMINISTRADOR),
--     legajo obligatorio según categoría, tipo de carnet según categoría,
--     disponibilidad de la instalación, solapamiento de vigencias de tarifas.
-- =====================================================================

 SET NAMES utf8mb4;
 SET SQL_SAFE_UPDATES = 0;
-- DROP DATABASE IF EXISTS sigrid;
 CREATE DATABASE sigrid;
 USE sigrid;


-- =====================================================================
-- (1) TIPOS / ENUMs
-- MySQL no tiene CREATE TYPE: los ENUM se declaran inline en cada columna
-- (en JPA: @Enumerated(EnumType.STRING)).
--   socio.estado ................ ACTIVO | NO_ACTIVO
--   suscripcion_socio.estado .... PENDIENTE_PAGO | VIGENTE | VENCIDA | CANCELADA
--   carnet_digital.tipo_carnet .. GENERAL | ESTUDIANTIL
--   carnet_digital.estado ....... ACTIVO | INACTIVO
--   instalacion.tipo_acceso ..... LIBRE | ARANCELADO. ARANCELADO se reserva por turno y con pago; LIBRE (la pileta) NO se
--                                 reserva: se entra sin turno mientras esté abierta (horario_apertura) y sin límite por persona.
--   instalacion.estado .......... HABILITADA | DESHABILITADA_MANTENIMIENTO | DESHABILITADA
--   reserva.estado .............. PENDIENTE_PAGO | CONFIRMADA | CANCELADA | RECHAZADA
--   pago.estado ................. PENDIENTE_VALIDACION | CONFIRMADO | RECHAZADO
-- =====================================================================


-- =====================================================================
-- (2) TABLAS DE CATÁLOGO / MAESTRAS
-- =====================================================================

CREATE TABLE rol (
  id_rol     INT UNSIGNED NOT NULL AUTO_INCREMENT,
  nombre_rol VARCHAR(50)  NOT NULL COMMENT 'SOCIO, ADMINISTRADOR. Catálogo abierto (sin ENUM) para agregar roles a futuro.',
  PRIMARY KEY (id_rol),
  UNIQUE KEY uq_rol_nombre (nombre_rol)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4
  COMMENT='Catálogo de roles del sistema.';

CREATE TABLE categoria_socio (
  id_categoria_socio INT UNSIGNED NOT NULL AUTO_INCREMENT,
  nombre_categoria   VARCHAR(50)  NOT NULL COMMENT 'Alumno UNSE, Docente UNSE, No Docente UNSE, Externo.',
  requiere_legajo    BOOLEAN      NOT NULL DEFAULT FALSE COMMENT 'TRUE para Alumno/Docente/No Docente; FALSE para Externo.',
  PRIMARY KEY (id_categoria_socio),
  UNIQUE KEY uq_categoria_socio_nombre (nombre_categoria)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4
  COMMENT='Categorías de socio: internas (UNSE) y Externo. Define tarifas diferenciadas (el externo paga más).';


-- =====================================================================
-- (3) TABLAS DE USUARIOS Y SOCIOS
-- =====================================================================

CREATE TABLE usuario (
  id_usuario        INT UNSIGNED NOT NULL AUTO_INCREMENT,
  id_rol            INT UNSIGNED NOT NULL,
  nombre            VARCHAR(100) NULL COMMENT 'NULL hasta completar el paso 2 del alta (datos personales).',
  apellido          VARCHAR(100) NULL COMMENT 'NULL hasta completar el paso 2 del alta (datos personales).',
  email             VARCHAR(255) NOT NULL COMMENT 'Se usa para el login. Único; la collation _ci lo hace insensible a mayúsculas.',
  password_hash     VARCHAR(255) NOT NULL COMMENT 'Hash bcrypt/Argon2 generado por la app (RNF-05). Nunca texto plano.',
  cuenta_habilitada BOOLEAN      NOT NULL DEFAULT TRUE COMMENT 'Si la cuenta puede iniciar sesión; FALSE = baja lógica del usuario. NO es el estado de membresía (ver socio.estado).',
  fecha_alta        TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (id_usuario),
  UNIQUE KEY uq_usuario_email (email),
  CONSTRAINT fk_usuario_rol FOREIGN KEY (id_rol) REFERENCES rol (id_rol) ON DELETE RESTRICT
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4
  COMMENT='Cuenta de acceso. Es Socio o Administrador según id_rol.';

CREATE TABLE socio (
  id_socio           INT UNSIGNED NOT NULL AUTO_INCREMENT,
  id_usuario         INT UNSIGNED NOT NULL COMMENT 'Relación 1 a 1 con usuario (rol SOCIO).',
  id_categoria_socio INT UNSIGNED NOT NULL,
  dni                VARCHAR(15)  NOT NULL COMMENT 'Solo dígitos, sin puntos. Único: una persona, un socio (ERS 2.4).',
  fecha_nacimiento   DATE         NOT NULL,
  telefono           VARCHAR(30)  NOT NULL,
  -- TODO: el ERS (CU-01 paso 6, RF-01.2) pide la foto de perfil pero no dice si es obligatoria
  --       (el carnet la muestra, RF-01.7.2). Se deja NULL.
  foto_perfil_url    VARCHAR(255) NULL,
  -- TODO: no se definió unicidad de legajo (global ni por categoría). Sin UNIQUE por ahora;
  --       evaluar UNIQUE (id_categoria_socio, legajo).
  legajo             VARCHAR(20)  NULL COMMENT 'Solo Alumno/Docente/No Docente (categoria_socio.requiere_legajo); NULL para Externo.',
  estado             ENUM('ACTIVO','NO_ACTIVO') NOT NULL DEFAULT 'NO_ACTIVO' COMMENT 'Estado de membresía, explícito (no se infiere de la suscripción). Lo escribe la app al acreditar pago o vencer sin renovación (RF-01.4).',
  baja_logica        BOOLEAN      NOT NULL DEFAULT FALSE COMMENT 'Baja del perfil de socio: a pedido del socio (RF-01.6) o manual del administrador (RF-01.10).',
  PRIMARY KEY (id_socio),
  UNIQUE KEY uq_socio_usuario (id_usuario),
  UNIQUE KEY uq_socio_dni (dni),
  CONSTRAINT fk_socio_usuario   FOREIGN KEY (id_usuario)         REFERENCES usuario (id_usuario)                 ON DELETE RESTRICT,
  CONSTRAINT fk_socio_categoria FOREIGN KEY (id_categoria_socio) REFERENCES categoria_socio (id_categoria_socio) ON DELETE RESTRICT
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4
  COMMENT='Extiende a usuario cuando el rol es SOCIO. Ser socio es independiente del estado de la membresía.';

CREATE TABLE suscripcion_socio (
  id_suscripcion    INT UNSIGNED NOT NULL AUTO_INCREMENT,
  id_socio          INT UNSIGNED NOT NULL,
  id_pago           INT UNSIGNED NULL COMMENT 'NULL mientras está PENDIENTE_PAGO. Un pago financia a lo sumo una suscripción. FK diferida al bloque 9.',
  estado            ENUM('PENDIENTE_PAGO','VIGENTE','VENCIDA','CANCELADA') NOT NULL DEFAULT 'PENDIENTE_PAGO' COMMENT 'CANCELADA cubre la baja voluntaria de la membresía (RF-01.9), que no da de baja al socio.',
  fecha_inicio      DATE         NULL,
  fecha_vencimiento DATE         NULL,
  fecha_solicitud   TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (id_suscripcion),
  UNIQUE KEY uq_suscripcion_pago (id_pago),
  CONSTRAINT fk_suscripcion_socio FOREIGN KEY (id_socio) REFERENCES socio (id_socio) ON DELETE RESTRICT
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4
  COMMENT='Historial de membresías: cada renovación es una fila nueva, nunca se pisa la anterior.';

CREATE TABLE carnet_digital (
  id_carnet     INT UNSIGNED NOT NULL AUTO_INCREMENT,
  id_socio      INT UNSIGNED NOT NULL,
  tipo_carnet   ENUM('GENERAL','ESTUDIANTIL') NOT NULL COMMENT 'ESTUDIANTIL para Alumno UNSE; GENERAL para el resto (RF-01.7.1).',
  codigo_qr_url VARCHAR(255) NULL COMMENT 'Reservado para el QR de acceso físico (requerimiento diferido, ERS 2.6). Sin uso: la app no debe depender de que tenga valor.',
  fecha_emision TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
  estado        ENUM('ACTIVO','INACTIVO') NOT NULL DEFAULT 'INACTIVO' COMMENT 'Nace INACTIVO (CU-01 paso 12); se activa según la vigencia de la membresía.',
  PRIMARY KEY (id_carnet),
  UNIQUE KEY uq_carnet_socio (id_socio),
  CONSTRAINT fk_carnet_socio FOREIGN KEY (id_socio) REFERENCES socio (id_socio) ON DELETE RESTRICT
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4
  COMMENT='Un carnet digital por socio (1 a 1).';


-- =====================================================================
-- (4) TABLAS DE INSTALACIONES Y TURNOS
-- =====================================================================

CREATE TABLE instalacion (
  id_instalacion    INT UNSIGNED NOT NULL AUTO_INCREMENT,
  nombre            VARCHAR(100) NOT NULL COMMENT 'Único (CU-09 paso 6), incluso entre las dadas de baja.',
  descripcion       TEXT         NULL,
  tipo_disciplina   VARCHAR(100) NOT NULL COMMENT 'Texto libre (fútbol, vóley, natación, SUM...).',
  capacidad         INT UNSIGNED NULL COMMENT 'NULL = no aplica / sin límite definido.',
  tipo_acceso       ENUM('LIBRE','ARANCELADO') NOT NULL COMMENT 'ARANCELADO: se reserva un turno y se paga. LIBRE: no se reserva (sin turnos ni tarifa), se entra en el horario de apertura.',
  estado            ENUM('HABILITADA','DESHABILITADA_MANTENIMIENTO','DESHABILITADA') NOT NULL DEFAULT 'HABILITADA' COMMENT 'DESHABILITADA_MANTENIMIENTO es temporal (RF-05.3). DESHABILITADA es la baja definitiva y hace de baja lógica (no hay columna baja_logica).',
  motivo_baja       VARCHAR(255) NULL COMMENT 'Se completa cuando estado <> HABILITADA.',
  fecha_inicio_baja DATE         NULL,
  fecha_fin_baja    DATE         NULL COMMENT 'Fecha estimada de reactivación (si aplica).',
  PRIMARY KEY (id_instalacion),
  UNIQUE KEY uq_instalacion_nombre (nombre)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4
  COMMENT='Espacios del polideportivo.';

CREATE TABLE tarifa_alquiler (
  id_tarifa          INT UNSIGNED  NOT NULL AUTO_INCREMENT,
  id_instalacion     INT UNSIGNED  NOT NULL,
  id_categoria_socio INT UNSIGNED  NOT NULL,
  precio             DECIMAL(10,2) NOT NULL,
  vigente_desde      DATE          NOT NULL,
  vigente_hasta      DATE          NULL COMMENT 'NULL = vigente actualmente.',
  PRIMARY KEY (id_tarifa),
  UNIQUE KEY uq_tarifa_alquiler_vigencia (id_instalacion, id_categoria_socio, vigente_desde),
  CONSTRAINT fk_tarifa_alquiler_instalacion FOREIGN KEY (id_instalacion)     REFERENCES instalacion (id_instalacion)          ON DELETE RESTRICT,
  CONSTRAINT fk_tarifa_alquiler_categoria   FOREIGN KEY (id_categoria_socio) REFERENCES categoria_socio (id_categoria_socio) ON DELETE RESTRICT
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4
  COMMENT='Historial de precios de alquiler por instalación y categoría. Nunca se edita un precio: se agrega uno nuevo con vigencia.';

CREATE TABLE tarifa_membresia (
  id_tarifa_membresia INT UNSIGNED  NOT NULL AUTO_INCREMENT,
  id_categoria_socio  INT UNSIGNED  NOT NULL,
  precio              DECIMAL(10,2) NOT NULL,
  vigente_desde       DATE          NOT NULL,
  vigente_hasta       DATE          NULL COMMENT 'NULL = vigente actualmente.',
  PRIMARY KEY (id_tarifa_membresia),
  UNIQUE KEY uq_tarifa_membresia_vigencia (id_categoria_socio, vigente_desde),
  CONSTRAINT fk_tarifa_membresia_categoria FOREIGN KEY (id_categoria_socio) REFERENCES categoria_socio (id_categoria_socio) ON DELETE RESTRICT
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4
  COMMENT='Historial de precios de la cuota mensual por categoría (sin instalación). Nunca se edita un precio: se agrega uno nuevo.';

CREATE TABLE turno (
  id_turno       INT UNSIGNED NOT NULL AUTO_INCREMENT,
  id_instalacion INT UNSIGNED NOT NULL,
  hora_inicio    TIME         NOT NULL,
  hora_fin       TIME         NOT NULL,
  PRIMARY KEY (id_turno),
  UNIQUE KEY uq_turno_instalacion_hora (id_instalacion, hora_inicio),
  CONSTRAINT fk_turno_instalacion FOREIGN KEY (id_instalacion) REFERENCES instalacion (id_instalacion) ON DELETE RESTRICT,
  CONSTRAINT chk_turno_horas CHECK (hora_fin > hora_inicio)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4
  COMMENT='Franjas horarias FIJAS por instalación (plantilla semanal, sin fecha). El día concreto vive en reserva.fecha_turno.';

CREATE TABLE horario_apertura (
  id_horario     INT UNSIGNED NOT NULL AUTO_INCREMENT,
  id_instalacion INT UNSIGNED NOT NULL,
  dia_semana     TINYINT UNSIGNED NULL COMMENT 'Franja que se repite cada semana: 1 = lunes ... 7 = domingo. NULL si es de un día específico.',
  fecha          DATE         NULL COMMENT 'Franja de un día específico (se suma a las semanales). NULL si es semanal.',
  hora_apertura  TIME         NOT NULL,
  hora_cierre    TIME         NOT NULL,
  PRIMARY KEY (id_horario),
  KEY idx_horario_instalacion (id_instalacion),
  CONSTRAINT fk_horario_instalacion FOREIGN KEY (id_instalacion) REFERENCES instalacion (id_instalacion) ON DELETE RESTRICT,
  CONSTRAINT chk_horario_horas CHECK (hora_cierre > hora_apertura),
  CONSTRAINT chk_horario_dia CHECK ((dia_semana IS NULL) <> (fecha IS NULL)),
  CONSTRAINT chk_horario_semana CHECK (dia_semana IS NULL OR dia_semana BETWEEN 1 AND 7)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4
  COMMENT='Cuándo está abierta una instalación de acceso LIBRE (la pileta): franjas por día de la semana y/o días específicos. Quién la usa y por cuánto tiempo no se registra.';


-- =====================================================================
-- (5) TABLAS DE RESERVAS
-- =====================================================================

CREATE TABLE reserva (
  id_reserva                  INT UNSIGNED NOT NULL AUTO_INCREMENT,
  id_socio                    INT UNSIGNED NOT NULL,
  id_turno                    INT UNSIGNED NOT NULL,
  fecha_turno                 DATE         NOT NULL COMMENT 'Día calendario del turno reservado (turno es solo la franja horaria).',
  -- TODO: el ERS (CU-05 paso 2) distingue instalaciones LIBRES de ARANCELADAS pero no dice si
  --       reservar una LIBRE exige pago. Se permite id_tarifa e id_pago NULL para ese caso.
  id_tarifa                   INT UNSIGNED NULL COMMENT 'Tarifa aplicada al reservar (el monto es tarifa_alquiler.precio; no se duplica). NULL si la instalación es LIBRE.',
  id_pago                     INT UNSIGNED NULL COMMENT 'NULL hasta que se sube el comprobante. Un pago financia a lo sumo una reserva. FK diferida al bloque 9.',
  -- TODO: CU-13 habla de reservas "Finalizadas" (turno ya transcurrido) pero el modelo no tiene
  --       FINALIZADA en el ENUM. Se asume derivada en Java (CONFIRMADA y fecha_turno + hora_fin < ahora).
  estado                      ENUM('PENDIENTE_PAGO','CONFIRMADA','CANCELADA','RECHAZADA') NOT NULL DEFAULT 'PENDIENTE_PAGO' COMMENT 'PENDIENTE_PAGO y CONFIRMADA ocupan el turno; CANCELADA y RECHAZADA lo liberan.',
  fecha_reserva               TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT 'Cuándo se solicitó.',
  fecha_confirmacion          TIMESTAMP    NULL DEFAULT NULL COMMENT 'Cuándo el administrador la confirmó.',
  fecha_limite_reprogramacion DATE         NULL COMMENT 'Se completa al cancelar (hoy + 30 días): hasta cuándo puede reprogramar sin perder el derecho (ERS 2.4).',
  id_admin_confirmador        INT UNSIGNED NULL,
  -- Control de concurrencia (RF-04.8): un (turno, día) admite una sola reserva "viva". Vale NULL
  -- cuando la reserva está CANCELADA/RECHAZADA, y NULL no choca en el UNIQUE. Si dos socios
  -- intentan el mismo turno, el segundo INSERT falla y Java lo informa. En la entidad JPA
  -- mapear con insertable=false, updatable=false (o no mapearla).
  turno_activo_key            VARCHAR(30) GENERATED ALWAYS AS (
    CASE WHEN estado IN ('PENDIENTE_PAGO','CONFIRMADA') THEN CONCAT(id_turno, '-', fecha_turno) ELSE NULL END
  ) STORED COMMENT 'Columna derivada; solo sirve al UNIQUE uq_turno_fecha_activa.',
  PRIMARY KEY (id_reserva),
  UNIQUE KEY uq_reserva_pago (id_pago),
  UNIQUE KEY uq_turno_fecha_activa (turno_activo_key),
  CONSTRAINT fk_reserva_socio  FOREIGN KEY (id_socio)             REFERENCES socio (id_socio)                 ON DELETE RESTRICT,
  CONSTRAINT fk_reserva_turno  FOREIGN KEY (id_turno)             REFERENCES turno (id_turno)                 ON DELETE RESTRICT,
  CONSTRAINT fk_reserva_tarifa FOREIGN KEY (id_tarifa)            REFERENCES tarifa_alquiler (id_tarifa)      ON DELETE RESTRICT,
  CONSTRAINT fk_reserva_admin  FOREIGN KEY (id_admin_confirmador) REFERENCES usuario (id_usuario)             ON DELETE RESTRICT
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4
  COMMENT='Reservas. Sin baja lógica aparte: el propio estado (CANCELADA/RECHAZADA) cumple ese rol.';

CREATE TABLE historial_reserva (
  id_historial      INT UNSIGNED NOT NULL AUTO_INCREMENT,
  id_reserva        INT UNSIGNED NOT NULL,
  id_turno_anterior INT UNSIGNED NOT NULL,
  fecha_anterior    DATE         NOT NULL COMMENT 'Día del turno antes del cambio (junto con id_turno_anterior).',
  motivo            VARCHAR(255) NOT NULL COMMENT 'Texto libre: reprogramación, cancelación administrativa, etc.',
  fecha_cambio      TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (id_historial),
  CONSTRAINT fk_historial_reserva FOREIGN KEY (id_reserva)        REFERENCES reserva (id_reserva) ON DELETE RESTRICT,
  CONSTRAINT fk_historial_turno   FOREIGN KEY (id_turno_anterior) REFERENCES turno (id_turno)     ON DELETE RESTRICT
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4
  COMMENT='Rastro de cada reprogramación: la reserva se actualiza pero queda el turno anterior.';

-- IF NOT EXISTS: en una base que ya existe se puede correr solo este CREATE TABLE.
CREATE TABLE IF NOT EXISTS solicitud_cambio_reserva (
  id_solicitud        INT UNSIGNED NOT NULL AUTO_INCREMENT,
  id_reserva          INT UNSIGNED NOT NULL,
  tipo                ENUM('CANCELACION','REPROGRAMACION') NOT NULL,
  id_turno_nuevo      INT UNSIGNED NULL COMMENT 'Solo REPROGRAMACION: turno pedido (de la misma instalación).',
  fecha_nueva         DATE         NULL COMMENT 'Solo REPROGRAMACION: día pedido.',
  motivo              VARCHAR(255) NOT NULL COMMENT 'Lo que escribe el socio.',
  estado              ENUM('PENDIENTE','APROBADA','RECHAZADA') NOT NULL DEFAULT 'PENDIENTE',
  fecha_solicitud     TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT 'Los 48 h de anticipación se miden contra este momento, no contra el de la aprobación.',
  fecha_resolucion    TIMESTAMP    NULL DEFAULT NULL,
  id_admin_resolutor  INT UNSIGNED NULL,
  motivo_rechazo      VARCHAR(255) NULL,
  PRIMARY KEY (id_solicitud),
  KEY idx_solicitud_estado (estado), -- bandeja "Cancelaciones y reprogramaciones"
  CONSTRAINT fk_solicitud_reserva FOREIGN KEY (id_reserva)         REFERENCES reserva (id_reserva) ON DELETE RESTRICT,
  CONSTRAINT fk_solicitud_turno   FOREIGN KEY (id_turno_nuevo)     REFERENCES turno (id_turno)     ON DELETE RESTRICT,
  CONSTRAINT fk_solicitud_admin   FOREIGN KEY (id_admin_resolutor) REFERENCES usuario (id_usuario) ON DELETE RESTRICT,
  CONSTRAINT chk_solicitud_destino CHECK (tipo = 'CANCELACION' OR (id_turno_nuevo IS NOT NULL AND fecha_nueva IS NOT NULL))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4
  COMMENT='Pedidos del socio de cancelar o reprogramar una reserva; el administrador solo los aprueba o rechaza. Que haya un solo pedido pendiente por reserva y las reglas de plazo se validan en Java.';


-- =====================================================================
-- (6) TABLAS DE PAGOS
-- =====================================================================

CREATE TABLE pago (
  id_pago            INT UNSIGNED  NOT NULL AUTO_INCREMENT,
  id_usuario         INT UNSIGNED  NOT NULL COMMENT 'Quien paga.',
  monto              DECIMAL(10,2) NOT NULL,
  estado             ENUM('PENDIENTE_VALIDACION','CONFIRMADO','RECHAZADO') NOT NULL DEFAULT 'PENDIENTE_VALIDACION',
  fecha_pago         TIMESTAMP     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT 'Cuándo se registró la carga del comprobante.',
  fecha_validacion   TIMESTAMP     NULL DEFAULT NULL COMMENT 'Cuándo el administrador lo confirmó o rechazó.',
  id_admin_validador INT UNSIGNED  NULL COMMENT 'Administrador que validó (trazabilidad).',
  motivo_rechazo     VARCHAR(255)  NULL,
  PRIMARY KEY (id_pago),
  CONSTRAINT fk_pago_usuario FOREIGN KEY (id_usuario)         REFERENCES usuario (id_usuario) ON DELETE RESTRICT,
  CONSTRAINT fk_pago_admin   FOREIGN KEY (id_admin_validador) REFERENCES usuario (id_usuario) ON DELETE RESTRICT
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4
  COMMENT='Pago por transferencia bancaria (membresía o alquiler), validado a mano por el administrador. Sin baja lógica: el estado (RECHAZADO) cumple ese rol.';

CREATE TABLE comprobante_pago (
  id_comprobante            INT UNSIGNED NOT NULL AUTO_INCREMENT,
  id_pago                   INT UNSIGNED NOT NULL COMMENT '1 a 1 con pago.',
  archivo_url               VARCHAR(255) NOT NULL,
  -- TODO: RF-03.6 pide impedir reutilizar el comprobante por "mismo archivo O misma referencia de
  --       transferencia". Solo hay hash_archivo (detecta el mismo archivo, no una recaptura de la
  --       misma transferencia). Para cubrirlo agregar, p. ej., nro_operacion VARCHAR(64) NULL UNIQUE.
  hash_archivo              CHAR(64)     NOT NULL COMMENT 'SHA-256 del contenido en hexadecimal. UNIQUE: el mismo comprobante no puede usarse en dos transacciones (RF-03.6); un duplicado falla en la BD.',
  fecha_operacion_declarada DATETIME     NOT NULL COMMENT 'Fecha/hora de la transferencia que declara el socio.',
  PRIMARY KEY (id_comprobante),
  UNIQUE KEY uq_comprobante_pago (id_pago),
  UNIQUE KEY uq_comprobante_hash (hash_archivo),
  CONSTRAINT fk_comprobante_pago FOREIGN KEY (id_pago) REFERENCES pago (id_pago) ON DELETE RESTRICT
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4
  COMMENT='Comprobante de la transferencia, separado de pago para controlar duplicados sin mezclarlo con el estado.';


-- =====================================================================
-- (7) TABLAS DE VALORACIONES
-- =====================================================================

CREATE TABLE valoracion_reserva (
  id_valoracion    INT UNSIGNED NOT NULL AUTO_INCREMENT,
  id_reserva       INT UNSIGNED NOT NULL COMMENT 'UNIQUE: una reserva se valora una sola vez. Instalación y socio salen de la reserva.',
  puntaje          TINYINT      NOT NULL,
  comentario       TEXT         NULL,
  fecha_valoracion TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (id_valoracion),
  UNIQUE KEY uq_valoracion_reserva (id_reserva),
  CONSTRAINT fk_valoracion_reserva  FOREIGN KEY (id_reserva) REFERENCES reserva (id_reserva) ON DELETE RESTRICT,
  CONSTRAINT chk_valoracion_puntaje CHECK (puntaje BETWEEN 1 AND 5)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4
  COMMENT='Valoración (1 a 5 + comentario opcional) de una reserva finalizada (RF-08).';


-- =====================================================================
-- (8) ÍNDICES ADICIONALES
-- Ya cubiertos por UNIQUE definidos en las tablas (no se duplican):
--   usuario.email                 -> uq_usuario_email
--   socio.dni                     -> uq_socio_dni
--   comprobante_pago.hash_archivo -> uq_comprobante_hash
--   turno (id_instalacion, ...)   -> prefijo de uq_turno_instalacion_hora
-- Los índices compuestos sobre id_socio sirven también como índice de la FK.
-- =====================================================================

CREATE INDEX idx_reserva_socio_estado      ON reserva (id_socio, estado);            -- "Mis Reservas" filtradas
CREATE INDEX idx_suscripcion_socio_estado  ON suscripcion_socio (id_socio, estado);  -- saber si el socio está al día
CREATE INDEX idx_pago_estado               ON pago (estado);                         -- bandeja "Confirmaciones pendientes"
CREATE INDEX idx_reserva_fecha_turno       ON reserva (fecha_turno);                 -- informe de uso diario (de bienesoftDB)
CREATE INDEX idx_suscripcion_vencimiento   ON suscripcion_socio (fecha_vencimiento); -- alertas de vencimiento (de bienesoftDB)


-- =====================================================================
-- (9) FOREIGN KEYS DIFERIDAS
-- suscripcion_socio (bloque 3) y reserva (bloque 5) apuntan a pago, que se
-- crea en el bloque 6. Los UNIQUE sobre id_pago sirven de índice de la FK.
-- =====================================================================

ALTER TABLE suscripcion_socio
  ADD CONSTRAINT fk_suscripcion_pago FOREIGN KEY (id_pago) REFERENCES pago (id_pago) ON DELETE RESTRICT;

ALTER TABLE reserva
  ADD CONSTRAINT fk_reserva_pago FOREIGN KEY (id_pago) REFERENCES pago (id_pago) ON DELETE RESTRICT;


-- =====================================================================
-- (10) DATOS INICIALES
-- Roles y la cuenta de administrador para ingresar al panel (/admin).
-- Es idempotente (INSERT IGNORE sobre los UNIQUE de rol y usuario): se puede
-- ejecutar SOLO este bloque sobre una base ya creada, sin borrar nada.
--
--   Correo     : admin@sigrid.com
--   Contraseña : Admin123!      <- cambiarla antes de un uso real
--
-- password_hash es PBKDF2 en el formato de PasswordUtil.generarHash
-- ("iteraciones:sal:hash", en Base64), el mismo que verifica el login.
-- =====================================================================

INSERT IGNORE INTO rol (nombre_rol) VALUES ('ADMINISTRADOR'), ('SOCIO');

INSERT IGNORE INTO usuario (id_rol, nombre, apellido, email, password_hash)
SELECT id_rol, 'Ana', 'Pérez', 'admin@sigrid.com',
       '120000:oYLPLGj3glpFkviMWoNcyw==:HqlNgIyuNtfwqirTmWx7xcDntn0EmC+qGFd10+z/dHw='
  FROM rol
 WHERE nombre_rol = 'ADMINISTRADOR';


-- =====================================================================
-- (11) DATOS DE PRUEBA (solo para desarrollo)
-- Las instalaciones reales del polideportivo (cancha de fútbol 11, cancha de
-- rugby, cancha de beach vóley, salón SUM, pileta y 3 quinchos con asador) con
-- sus turnos, 10 socios y reservas repartidas alrededor de HOY (las fechas son
-- relativas a @hoy, así que siempre quedan actuales), para ver el dashboard
-- del administrador con contenido. Necesita el bloque (10).
--
-- Los socios de prueba usan la contraseña Admin123! y correos @demo.sigrid.
-- A diferencia del (10), NO es idempotente (los pagos de prueba no tienen clave
-- natural y se duplicarían): correrlo una sola vez. Para sacarlo, ver la
-- LIMPIEZA al final de este bloque.
-- =====================================================================

-- Fecha y hora "de hoy" de los datos de prueba. Por defecto son las de ahora; para dejar la demo lista para OTRO día se
-- fijan ANTES de correr el archivo, en la misma sesión (ej. la demo del lunes 5/10/2026):
--     SET @hoy = '2026-10-05';  SET @ahora = '2026-10-05 08:00:00';
SET @hoy = DATE(COALESCE(@hoy, CURDATE()));
SET @ahora = COALESCE(@ahora, NOW());

-- ---------------------------------------------------------------------
-- Categorías de socio (catálogo que necesita cualquier socio)
-- ---------------------------------------------------------------------
INSERT IGNORE INTO categoria_socio (nombre_categoria, requiere_legajo) VALUES
  ('Alumno UNSE', TRUE),
  ('Docente UNSE', TRUE),
  ('No Docente UNSE', TRUE),
  ('Externo', FALSE);

-- ---------------------------------------------------------------------
-- Instalaciones: 7 habilitadas y la cancha de rugby en mantenimiento.
-- Los nombres deben conservar la palabra que las identifica en el plano del
-- dashboard (fútbol, rugby, beach, sum, pileta, "quincho 1/2/3").
-- ---------------------------------------------------------------------
INSERT IGNORE INTO instalacion
  (nombre, descripcion, tipo_disciplina, capacidad, tipo_acceso, estado, motivo_baja, fecha_inicio_baja, fecha_fin_baja) VALUES
  ('Cancha de Fútbol 11',   'Césped natural con iluminación.',        'Fútbol',             22, 'ARANCELADO', 'HABILITADA', NULL, NULL, NULL),
  ('Cancha de Rugby',       'Césped natural.',                        'Rugby',              30, 'ARANCELADO', 'DESHABILITADA_MANTENIMIENTO',
     'Riego y nivelación del césped', @hoy, @hoy + INTERVAL 5 DAY),
  ('Cancha de Beach Vóley', 'Cancha de arena reglamentaria.',         'Beach vóley',        12, 'ARANCELADO', 'HABILITADA', NULL, NULL, NULL),
  ('Salón SUM',             'Salón de usos múltiples para eventos.',  'Salón de eventos',  100, 'ARANCELADO', 'HABILITADA', NULL, NULL, NULL),
  ('Pileta',                'Pileta comunitaria. No se reserva: se entra mientras esté abierta.', 'Natación', 40, 'LIBRE', 'HABILITADA', NULL, NULL, NULL),
  ('Quincho 1',             'Quincho con asador.',                    'Quincho con asador', 15, 'ARANCELADO', 'HABILITADA', NULL, NULL, NULL),
  ('Quincho 2',             'Quincho con asador.',                    'Quincho con asador', 15, 'ARANCELADO', 'HABILITADA', NULL, NULL, NULL),
  ('Quincho 3',             'Quincho con asador.',                    'Quincho con asador', 15, 'ARANCELADO', 'HABILITADA', NULL, NULL, NULL);

-- Franjas fijas de una hora para las canchas (la pileta no se reserva: tiene horario de apertura)
INSERT IGNORE INTO turno (id_instalacion, hora_inicio, hora_fin)
SELECT i.id_instalacion, h.ini, h.fin
  FROM instalacion i
  JOIN (SELECT '09:00:00' AS ini, '10:00:00' AS fin UNION ALL
        SELECT '10:00:00', '11:00:00' UNION ALL
        SELECT '16:00:00', '17:00:00' UNION ALL
        SELECT '17:00:00', '18:00:00' UNION ALL
        SELECT '18:00:00', '19:00:00' UNION ALL
        SELECT '19:00:00', '20:00:00' UNION ALL
        SELECT '20:00:00', '21:00:00') h
 WHERE i.nombre IN ('Cancha de Fútbol 11', 'Cancha de Rugby', 'Cancha de Beach Vóley');

-- El SUM y los quinchos se alquilan por franjas largas: mediodía y noche
INSERT IGNORE INTO turno (id_instalacion, hora_inicio, hora_fin)
SELECT i.id_instalacion, h.ini, h.fin
  FROM instalacion i
  JOIN (SELECT '12:00:00' AS ini, '17:00:00' AS fin UNION ALL
        SELECT '18:00:00', '23:00:00') h
 WHERE i.nombre IN ('Salón SUM', 'Quincho 1', 'Quincho 2', 'Quincho 3');

-- La pileta no se reserva: está abierta por franjas semanales (1 = lunes ... 7 = domingo) y algún día específico
INSERT INTO horario_apertura (id_instalacion, dia_semana, fecha, hora_apertura, hora_cierre)
SELECT i.id_instalacion, h.dia, h.fecha, h.ini, h.fin
  FROM instalacion i
  JOIN (SELECT 1 AS dia, NULL AS fecha, '09:00:00' AS ini, '19:00:00' AS fin UNION ALL
        SELECT 2, NULL, '09:00:00', '19:00:00' UNION ALL
        SELECT 3, NULL, '09:00:00', '19:00:00' UNION ALL
        SELECT 4, NULL, '09:00:00', '19:00:00' UNION ALL
        SELECT 5, NULL, '09:00:00', '19:00:00' UNION ALL
        SELECT 6, NULL, '10:00:00', '20:00:00' UNION ALL
        SELECT 7, NULL, '10:00:00', '20:00:00' UNION ALL
        SELECT NULL, @hoy + INTERVAL 9 DAY, '20:00:00', '23:00:00') h
 WHERE i.nombre = 'Pileta';

-- ---------------------------------------------------------------------
-- Socios de prueba (usuario + socio)
-- ---------------------------------------------------------------------
INSERT IGNORE INTO usuario (id_rol, nombre, apellido, email, password_hash)
SELECT r.id_rol, d.nombre, d.apellido, d.email,
       '120000:oYLPLGj3glpFkviMWoNcyw==:HqlNgIyuNtfwqirTmWx7xcDntn0EmC+qGFd10+z/dHw='
  FROM rol r
  JOIN (SELECT 'Matías' AS nombre, 'Rojas' AS apellido, 'matias.rojas@demo.sigrid' AS email UNION ALL
        SELECT 'Lucía',     'Torres',   'lucia.torres@demo.sigrid' UNION ALL
        SELECT 'Facundo',   'Díaz',     'facundo.diaz@demo.sigrid' UNION ALL
        SELECT 'Camila',    'Ruiz',     'camila.ruiz@demo.sigrid' UNION ALL
        SELECT 'Bruno',     'López',    'bruno.lopez@demo.sigrid' UNION ALL
        SELECT 'Sofía',     'Herrera',  'sofia.herrera@demo.sigrid' UNION ALL
        SELECT 'Tomás',     'Acuña',    'tomas.acuna@demo.sigrid' UNION ALL
        SELECT 'Valentina', 'Paz',      'valentina.paz@demo.sigrid' UNION ALL
        SELECT 'Nicolás',   'Ledesma',  'nicolas.ledesma@demo.sigrid' UNION ALL
        SELECT 'Agustina',  'Coronel',  'agustina.coronel@demo.sigrid') d
 WHERE r.nombre_rol = 'SOCIO';

INSERT IGNORE INTO socio (id_usuario, id_categoria_socio, dni, fecha_nacimiento, telefono, legajo, estado)
SELECT u.id_usuario, c.id_categoria_socio, d.dni, d.nacimiento, d.telefono, d.legajo, d.estado
  FROM (SELECT 'matias.rojas@demo.sigrid' AS email, 'Alumno UNSE' AS categoria, '40111222' AS dni, '2001-03-14' AS nacimiento, '3855111222' AS telefono, 'A-1024' AS legajo, 'ACTIVO' AS estado UNION ALL
        SELECT 'lucia.torres@demo.sigrid',     'Alumno UNSE',     '41222333', '2002-07-02', '3855222333', 'A-1187', 'ACTIVO' UNION ALL
        SELECT 'facundo.diaz@demo.sigrid',     'Docente UNSE',    '30333444', '1984-11-21', '3855333444', 'D-0311', 'ACTIVO' UNION ALL
        SELECT 'camila.ruiz@demo.sigrid',      'Alumno UNSE',     '42444555', '2003-01-30', '3855444555', 'A-1302', 'ACTIVO' UNION ALL
        SELECT 'bruno.lopez@demo.sigrid',      'Externo',         '35555666', '1991-05-09', '3855555666', NULL,     'ACTIVO' UNION ALL
        SELECT 'sofia.herrera@demo.sigrid',    'No Docente UNSE', '32666777', '1987-09-17', '3855666777', 'N-0208', 'ACTIVO' UNION ALL
        SELECT 'tomas.acuna@demo.sigrid',      'Externo',         '38777888', '1996-12-03', '3855777888', NULL,     'ACTIVO' UNION ALL
        SELECT 'valentina.paz@demo.sigrid',    'Alumno UNSE',     '43888999', '2004-04-25', '3855888999', 'A-1450', 'NO_ACTIVO' UNION ALL
        SELECT 'nicolas.ledesma@demo.sigrid',  'Docente UNSE',    '29999000', '1982-08-11', '3855999000', 'D-0127', 'ACTIVO' UNION ALL
        SELECT 'agustina.coronel@demo.sigrid', 'Alumno UNSE',     '42000111', '2003-10-06', '3855000111', 'A-1391', 'NO_ACTIVO') d
  JOIN usuario u ON u.email = d.email
  JOIN categoria_socio c ON c.nombre_categoria = d.categoria;

-- Membresías vigentes de los socios activos: tres vencen dentro de la semana y cinco se pagaron en los primeros días del mes
INSERT INTO suscripcion_socio (id_socio, estado, fecha_inicio, fecha_vencimiento)
SELECT s.id_socio, 'VIGENTE', @hoy + INTERVAL d.dias DAY - INTERVAL 30 DAY, @hoy + INTERVAL d.dias DAY
  FROM (SELECT 'matias.rojas@demo.sigrid' AS email, 2 AS dias UNION ALL
        SELECT 'lucia.torres@demo.sigrid',    4  UNION ALL
        SELECT 'facundo.diaz@demo.sigrid',    6  UNION ALL
        SELECT 'camila.ruiz@demo.sigrid',     28 UNION ALL
        SELECT 'bruno.lopez@demo.sigrid',     29 UNION ALL
        SELECT 'sofia.herrera@demo.sigrid',   26 UNION ALL
        SELECT 'tomas.acuna@demo.sigrid',     30 UNION ALL
        SELECT 'nicolas.ledesma@demo.sigrid', 27) d
  JOIN usuario u ON u.email = d.email
  JOIN socio s ON s.id_usuario = u.id_usuario;

-- ---------------------------------------------------------------------
-- Panel de membresías: carnets, cuota mensual por tipo de socio (el externo paga más) e historial de
-- renovaciones con sus pagos. Hay dos solicitudes con comprobante por validar y una sin comprobante.
-- ---------------------------------------------------------------------
INSERT INTO carnet_digital (id_socio, tipo_carnet, estado)
SELECT s.id_socio, IF(c.nombre_categoria = 'Alumno UNSE', 'ESTUDIANTIL', 'GENERAL'), IF(s.estado = 'ACTIVO', 'ACTIVO', 'INACTIVO')
  FROM socio s
  JOIN categoria_socio c ON c.id_categoria_socio = s.id_categoria_socio
  JOIN usuario u ON u.id_usuario = s.id_usuario
 WHERE u.email LIKE '%@demo.sigrid';

INSERT IGNORE INTO tarifa_membresia (id_categoria_socio, precio, vigente_desde)
SELECT c.id_categoria_socio, d.precio, '2026-01-01'
  FROM categoria_socio c
  JOIN (SELECT 'Alumno UNSE' AS nombre, 3000.00 AS precio UNION ALL
        SELECT 'Docente UNSE',    4500.00 UNION ALL
        SELECT 'No Docente UNSE', 4000.00 UNION ALL
        SELECT 'Externo',         8000.00) d ON d.nombre = c.nombre_categoria;

-- Renovaciones anteriores de los socios activos (una por mes hacia atrás): vencidas, y una cancelada
INSERT INTO suscripcion_socio (id_socio, estado, fecha_inicio, fecha_vencimiento)
SELECT s.id_socio, IF(d.email = 'nicolas.ledesma@demo.sigrid', 'CANCELADA', 'VENCIDA'),
       @hoy + INTERVAL d.dias DAY - INTERVAL (30 * (d.k + 1)) DAY,
       @hoy + INTERVAL d.dias DAY - INTERVAL (30 * d.k) DAY
  FROM (SELECT 'matias.rojas@demo.sigrid' AS email, 2 AS dias, 1 AS k UNION ALL
        SELECT 'matias.rojas@demo.sigrid',    2, 2 UNION ALL
        SELECT 'matias.rojas@demo.sigrid',    2, 3 UNION ALL
        SELECT 'lucia.torres@demo.sigrid',    4, 1 UNION ALL
        SELECT 'lucia.torres@demo.sigrid',    4, 2 UNION ALL
        SELECT 'facundo.diaz@demo.sigrid',    6, 1 UNION ALL
        SELECT 'camila.ruiz@demo.sigrid',    28, 1 UNION ALL
        SELECT 'bruno.lopez@demo.sigrid',    29, 1 UNION ALL
        SELECT 'sofia.herrera@demo.sigrid',  26, 1 UNION ALL
        SELECT 'tomas.acuna@demo.sigrid',    30, 1 UNION ALL
        SELECT 'nicolas.ledesma@demo.sigrid', 27, 1) d
  JOIN usuario u ON u.email = d.email
  JOIN socio s ON s.id_usuario = u.id_usuario;

-- Cada una de esas membresías (vigentes incluidas) se pagó al precio de la cuota, confirmado por el administrador
INSERT INTO pago (id_usuario, monto, estado, fecha_pago, fecha_validacion, id_admin_validador)
SELECT so.id_usuario, t.precio, 'CONFIRMADO', TIMESTAMP(s.fecha_inicio), TIMESTAMP(s.fecha_inicio) + INTERVAL 2 HOUR,
       (SELECT id_usuario FROM usuario WHERE email = 'admin@sigrid.com')
  FROM suscripcion_socio s
  JOIN socio so ON so.id_socio = s.id_socio
  JOIN tarifa_membresia t ON t.id_categoria_socio = so.id_categoria_socio AND t.vigente_hasta IS NULL
 WHERE s.id_pago IS NULL AND s.estado IN ('VIGENTE', 'VENCIDA', 'CANCELADA');

UPDATE suscripcion_socio s
  JOIN socio so ON so.id_socio = s.id_socio
  JOIN pago p ON p.id_usuario = so.id_usuario AND p.fecha_pago = TIMESTAMP(s.fecha_inicio)
   SET s.id_pago = p.id_pago
 WHERE s.id_pago IS NULL AND s.estado IN ('VIGENTE', 'VENCIDA', 'CANCELADA');

UPDATE suscripcion_socio SET fecha_solicitud = TIMESTAMP(fecha_inicio) - INTERVAL 1 DAY WHERE fecha_inicio IS NOT NULL;

-- Solicitudes por validar: valentina.paz (vuelve a asociarse) y sofia.herrera (renueva por adelantado)
INSERT INTO suscripcion_socio (id_socio, estado, fecha_solicitud)
SELECT s.id_socio, 'PENDIENTE_PAGO', @ahora - INTERVAL 5 HOUR FROM socio s JOIN usuario u ON u.id_usuario = s.id_usuario
 WHERE u.email = 'valentina.paz@demo.sigrid';
SET @suscripcion = LAST_INSERT_ID();
INSERT INTO pago (id_usuario, monto, fecha_pago)
VALUES ((SELECT id_usuario FROM usuario WHERE email = 'valentina.paz@demo.sigrid'), 3000.00, @ahora - INTERVAL 5 HOUR);
SET @pago = LAST_INSERT_ID();
INSERT INTO comprobante_pago (id_pago, archivo_url, hash_archivo, fecha_operacion_declarada)
VALUES (@pago, 'demo/membresia-1.pdf', SHA2('demo-membresia-1', 256), @ahora - INTERVAL 5 HOUR);
UPDATE suscripcion_socio SET id_pago = @pago WHERE id_suscripcion = @suscripcion;

INSERT INTO suscripcion_socio (id_socio, estado, fecha_solicitud)
SELECT s.id_socio, 'PENDIENTE_PAGO', @ahora - INTERVAL 2 HOUR FROM socio s JOIN usuario u ON u.id_usuario = s.id_usuario
 WHERE u.email = 'sofia.herrera@demo.sigrid';
SET @suscripcion = LAST_INSERT_ID();
INSERT INTO pago (id_usuario, monto, fecha_pago)
VALUES ((SELECT id_usuario FROM usuario WHERE email = 'sofia.herrera@demo.sigrid'), 4000.00, @ahora - INTERVAL 2 HOUR);
SET @pago = LAST_INSERT_ID();
INSERT INTO comprobante_pago (id_pago, archivo_url, hash_archivo, fecha_operacion_declarada)
VALUES (@pago, 'demo/membresia-2.pdf', SHA2('demo-membresia-2', 256), @ahora - INTERVAL 2 HOUR);
UPDATE suscripcion_socio SET id_pago = @pago WHERE id_suscripcion = @suscripcion;

-- Y una solicitud que todavía espera que el socio cargue el comprobante
INSERT INTO suscripcion_socio (id_socio, estado, fecha_solicitud)
SELECT s.id_socio, 'PENDIENTE_PAGO', @ahora - INTERVAL 26 HOUR FROM socio s JOIN usuario u ON u.id_usuario = s.id_usuario
 WHERE u.email = 'agustina.coronel@demo.sigrid';

-- ---------------------------------------------------------------------
-- Reservas: dia = días desde hoy (negativo = pasado). Las CONFIRMADAS las
-- confirmó la cuenta de administrador.
-- ---------------------------------------------------------------------
INSERT INTO reserva (id_socio, id_turno, fecha_turno, estado, fecha_reserva, fecha_confirmacion, id_admin_confirmador)
SELECT s.id_socio, t.id_turno, @hoy + INTERVAL d.dia DAY, d.estado,
       @ahora - INTERVAL d.hace_horas HOUR,
       IF(d.estado = 'CONFIRMADA', @ahora - INTERVAL (d.hace_horas - 1) HOUR, NULL),
       IF(d.estado = 'CONFIRMADA', (SELECT id_usuario FROM usuario WHERE email = 'admin@sigrid.com'), NULL)
  FROM (
        -- hoy
        SELECT 'facundo.diaz@demo.sigrid' AS email, 'Quincho 1' AS instalacion, '12:00:00' AS hora, 0 AS dia, 'CONFIRMADA' AS estado, 40 AS hace_horas UNION ALL
        SELECT 'bruno.lopez@demo.sigrid',      'Cancha de Fútbol 11',   '19:00:00',  0, 'PENDIENTE_PAGO',  4 UNION ALL
        SELECT 'matias.rojas@demo.sigrid',     'Cancha de Fútbol 11',   '18:00:00',  0, 'CONFIRMADA',      3 UNION ALL
        SELECT 'sofia.herrera@demo.sigrid',    'Salón SUM',             '18:00:00',  0, 'CONFIRMADA',     44 UNION ALL
        SELECT 'lucia.torres@demo.sigrid',     'Cancha de Beach Vóley', '19:00:00',  0, 'CONFIRMADA',      2 UNION ALL
        SELECT 'tomas.acuna@demo.sigrid',      'Cancha de Beach Vóley', '20:00:00',  0, 'CANCELADA',      60 UNION ALL
        SELECT 'nicolas.ledesma@demo.sigrid',  'Cancha de Fútbol 11',   '20:00:00',  0, 'CONFIRMADA',     20 UNION ALL
        -- mañana
        SELECT 'agustina.coronel@demo.sigrid', 'Quincho 2',             '12:00:00',  1, 'CONFIRMADA',     18 UNION ALL
        SELECT 'bruno.lopez@demo.sigrid',      'Cancha de Fútbol 11',   '18:00:00',  1, 'PENDIENTE_PAGO',  2 UNION ALL
        SELECT 'valentina.paz@demo.sigrid',    'Cancha de Beach Vóley', '20:00:00',  1, 'CANCELADA',      22 UNION ALL
        -- próximos días
        SELECT 'matias.rojas@demo.sigrid',     'Cancha de Beach Vóley', '17:00:00',  2, 'CONFIRMADA',     30 UNION ALL
        SELECT 'tomas.acuna@demo.sigrid',      'Cancha de Fútbol 11',   '19:00:00',  2, 'PENDIENTE_PAGO',  5 UNION ALL
        SELECT 'facundo.diaz@demo.sigrid',     'Cancha de Fútbol 11',   '20:00:00',  3, 'CANCELADA',      72 UNION ALL
        SELECT 'nicolas.ledesma@demo.sigrid',  'Cancha de Beach Vóley', '19:00:00',  6, 'PENDIENTE_PAGO',  3 UNION ALL
        SELECT 'sofia.herrera@demo.sigrid',    'Salón SUM',             '18:00:00',  9, 'CONFIRMADA',     48 UNION ALL
        SELECT 'bruno.lopez@demo.sigrid',      'Cancha de Fútbol 11',   '17:00:00', 12, 'CONFIRMADA',     60 UNION ALL
        -- días pasados (llenan el calendario hacia atrás)
        SELECT 'matias.rojas@demo.sigrid',     'Cancha de Fútbol 11',   '18:00:00', -1, 'CONFIRMADA',     80 UNION ALL
        SELECT 'lucia.torres@demo.sigrid',     'Cancha de Beach Vóley', '19:00:00', -1, 'CONFIRMADA',     70 UNION ALL
        SELECT 'tomas.acuna@demo.sigrid',      'Quincho 3',             '12:00:00', -4, 'CANCELADA',     100 UNION ALL
        SELECT 'nicolas.ledesma@demo.sigrid',  'Cancha de Fútbol 11',   '20:00:00', -7, 'CONFIRMADA',    120 UNION ALL
        -- refuerzo para la demo: la mañana y la tarde de hoy (el plano se ve ocupado a cualquier hora)
        SELECT 'camila.ruiz@demo.sigrid',      'Cancha de Fútbol 11',   '09:00:00',  0, 'CONFIRMADA',      3 UNION ALL
        SELECT 'sofia.herrera@demo.sigrid',    'Cancha de Beach Vóley', '10:00:00',  0, 'CONFIRMADA',      2 UNION ALL
        SELECT 'tomas.acuna@demo.sigrid',      'Cancha de Fútbol 11',   '16:00:00',  0, 'CONFIRMADA',     52 UNION ALL
        SELECT 'nicolas.ledesma@demo.sigrid',  'Quincho 3',             '18:00:00',  0, 'CONFIRMADA',     36 UNION ALL
        -- refuerzo: dos solicitudes más con comprobante (una con el comprobante correcto y otra con el monto equivocado)
        SELECT 'camila.ruiz@demo.sigrid',      'Quincho 2',             '12:00:00',  4, 'PENDIENTE_PAGO',  3 UNION ALL
        SELECT 'facundo.diaz@demo.sigrid',     'Salón SUM',             '18:00:00',  8, 'PENDIENTE_PAGO',  2 UNION ALL
        -- refuerzo: reservas confirmadas de las próximas dos semanas (el calendario del mes se ve con movimiento)
        SELECT 'lucia.torres@demo.sigrid',     'Cancha de Beach Vóley', '18:00:00',  3, 'CONFIRMADA',     48 UNION ALL
        SELECT 'lucia.torres@demo.sigrid',     'Quincho 1',             '18:00:00',  5, 'CONFIRMADA',     70 UNION ALL
        SELECT 'matias.rojas@demo.sigrid',     'Cancha de Fútbol 11',   '19:00:00',  5, 'CONFIRMADA',     66 UNION ALL
        SELECT 'camila.ruiz@demo.sigrid',      'Cancha de Beach Vóley', '17:00:00',  7, 'CONFIRMADA',     90 UNION ALL
        SELECT 'facundo.diaz@demo.sigrid',     'Quincho 3',             '12:00:00',  9, 'CONFIRMADA',    100 UNION ALL
        SELECT 'tomas.acuna@demo.sigrid',      'Cancha de Fútbol 11',   '18:00:00', 10, 'CONFIRMADA',    110 UNION ALL
        SELECT 'nicolas.ledesma@demo.sigrid',  'Salón SUM',             '12:00:00', 11, 'CONFIRMADA',    120 UNION ALL
        SELECT 'sofia.herrera@demo.sigrid',    'Cancha de Beach Vóley', '18:00:00', 13, 'CONFIRMADA',    130 UNION ALL
        SELECT 'lucia.torres@demo.sigrid',     'Cancha de Fútbol 11',   '20:00:00', 14, 'CONFIRMADA',    140 UNION ALL
        -- refuerzo: una reserva cancelada con crédito que vence pronto (ver "Cancelaciones y reprogramaciones")
        SELECT 'sofia.herrera@demo.sigrid',    'Quincho 2',             '12:00:00', -2, 'CANCELADA',     300
       ) d
  JOIN usuario u ON u.email = d.email
  JOIN socio s ON s.id_usuario = u.id_usuario
  JOIN instalacion i ON i.nombre = d.instalacion
  JOIN turno t ON t.id_instalacion = i.id_instalacion AND t.hora_inicio = d.hora;

-- ---------------------------------------------------------------------
-- Comprobantes cargados: seis solicitudes pendientes ya tienen su transferencia para revisar (son las que aparecen en
-- "Solicitudes por confirmar"; el importe es el precio de la tarifa del socio). Las imágenes de muestra están en
-- sigrid-comprobantes/demo (carpeta del usuario que corre GlassFish). La última tiene a propósito un monto distinto
-- en la imagen, para mostrar cuándo el administrador deniega. Las reservas que siguen esperando el comprobante no le
-- llegan al administrador (y el timer las cancela a la hora).
-- ---------------------------------------------------------------------
DROP TEMPORARY TABLE IF EXISTS tmp_comp;
CREATE TEMPORARY TABLE tmp_comp (email VARCHAR(255), dia INT, monto DECIMAL(10,2), hace_horas INT, archivo VARCHAR(100));
INSERT INTO tmp_comp VALUES
  ('bruno.lopez@demo.sigrid',     0, 35000.00, 3, 'demo/comprobante-1.png'),
  ('bruno.lopez@demo.sigrid',     1, 35000.00, 1, 'demo/comprobante-2.png'),
  ('tomas.acuna@demo.sigrid',     2, 35000.00, 4, 'demo/comprobante-3.png'),
  ('nicolas.ledesma@demo.sigrid', 6, 15000.00, 2, 'demo/comprobante-4.png'),
  ('camila.ruiz@demo.sigrid',     4, 12600.00, 2, 'demo/comprobante-5.png'),
  ('facundo.diaz@demo.sigrid',    8, 40000.00, 1, 'demo/comprobante-6.png');

SET @base = (SELECT COALESCE(MAX(id_pago), 0) FROM pago);
SET @n = 0;
DROP TEMPORARY TABLE IF EXISTS tmp_comp_pago;
CREATE TEMPORARY TABLE tmp_comp_pago AS
SELECT c.*, @base + (@n := @n + 1) AS id_pago_nuevo, u.id_usuario
  FROM tmp_comp c JOIN usuario u ON u.email = c.email;

INSERT INTO pago (id_pago, id_usuario, monto, fecha_pago)
SELECT id_pago_nuevo, id_usuario, monto, @ahora - INTERVAL hace_horas HOUR FROM tmp_comp_pago;

INSERT INTO comprobante_pago (id_pago, archivo_url, hash_archivo, fecha_operacion_declarada)
SELECT id_pago_nuevo, archivo, SHA2(CONCAT('demo-', archivo), 256), @ahora - INTERVAL hace_horas HOUR FROM tmp_comp_pago;

UPDATE reserva r
  JOIN socio s ON s.id_socio = r.id_socio JOIN usuario u ON u.id_usuario = s.id_usuario
  JOIN tmp_comp_pago c ON c.email = u.email AND r.fecha_turno = @hoy + INTERVAL c.dia DAY
   SET r.id_pago = c.id_pago_nuevo
 WHERE r.estado = 'PENDIENTE_PAGO' AND r.id_pago IS NULL;
DROP TEMPORARY TABLE tmp_comp_pago;
DROP TEMPORARY TABLE tmp_comp;

-- ---------------------------------------------------------------------
-- Tarifas y pagos de las reservas de prueba (idempotente, se puede volver a correr).
-- Toda reserva tiene costo: cada instalación tiene su tarifa por categoría de socio (los alumnos
-- pagan un 30 % menos y los externos un 40 % más; la pileta no se reserva y no tiene tarifa), cada reserva de prueba queda asociada a la tarifa de su socio y las
-- CONFIRMADAS que todavía no tenían un pago reciben el suyo, ya validado por el administrador.
-- ---------------------------------------------------------------------
INSERT IGNORE INTO tarifa_alquiler (id_instalacion, id_categoria_socio, precio, vigente_desde)
SELECT i.id_instalacion, c.id_categoria_socio,
       ROUND(CASE i.nombre
               WHEN 'Cancha de Fútbol 11'   THEN 25000
               WHEN 'Cancha de Rugby'       THEN 30000
               WHEN 'Cancha de Beach Vóley' THEN 15000
               WHEN 'Salón SUM'             THEN 40000
               ELSE 18000 END * CASE c.nombre_categoria WHEN 'Externo' THEN 1.4 WHEN 'Alumno UNSE' THEN 0.7 ELSE 1 END, 2),
       '2025-01-01'
  FROM instalacion i
 CROSS JOIN categoria_socio c
 WHERE i.nombre IN ('Cancha de Fútbol 11', 'Cancha de Rugby', 'Cancha de Beach Vóley', 'Salón SUM',
                    'Quincho 1', 'Quincho 2', 'Quincho 3');

UPDATE reserva r
  JOIN socio s ON s.id_socio = r.id_socio
  JOIN usuario u ON u.id_usuario = s.id_usuario
  JOIN turno t ON t.id_turno = r.id_turno
  JOIN tarifa_alquiler ta ON ta.id_instalacion = t.id_instalacion AND ta.id_categoria_socio = s.id_categoria_socio
   SET r.id_tarifa = ta.id_tarifa
 WHERE u.email LIKE '%@demo.sigrid' AND r.id_tarifa IS NULL;

SET @base = (SELECT COALESCE(MAX(id_pago), 0) FROM pago);
SET @n = 0;
DROP TEMPORARY TABLE IF EXISTS tmp_pago_reserva;
CREATE TEMPORARY TABLE tmp_pago_reserva AS
SELECT r.id_reserva, s.id_usuario, ta.precio AS monto, r.fecha_confirmacion, r.id_admin_confirmador,
       @base + (@n := @n + 1) AS id_pago_nuevo
  FROM reserva r
  JOIN socio s ON s.id_socio = r.id_socio
  JOIN usuario u ON u.id_usuario = s.id_usuario
  JOIN tarifa_alquiler ta ON ta.id_tarifa = r.id_tarifa
 WHERE u.email LIKE '%@demo.sigrid' AND r.estado = 'CONFIRMADA' AND r.id_pago IS NULL
 ORDER BY r.id_reserva;

INSERT INTO pago (id_pago, id_usuario, monto, estado, fecha_pago, fecha_validacion, id_admin_validador)
SELECT id_pago_nuevo, id_usuario, monto, 'CONFIRMADO', COALESCE(fecha_confirmacion, @ahora),
       COALESCE(fecha_confirmacion, @ahora), id_admin_confirmador
  FROM tmp_pago_reserva;

UPDATE reserva r JOIN tmp_pago_reserva t ON t.id_reserva = r.id_reserva SET r.id_pago = t.id_pago_nuevo;
DROP TEMPORARY TABLE tmp_pago_reserva;

-- ---------------------------------------------------------------------
-- Cancelaciones y reprogramaciones (NO idempotente, correrlo una sola vez): dos créditos (uno vigente y otro vencido, con su pago
-- confirmado), tres pedidos de socios por aprobar y una reprogramación ya hecha
-- ---------------------------------------------------------------------
INSERT INTO pago (id_usuario, monto, estado, fecha_pago, fecha_validacion, id_admin_validador)
VALUES ((SELECT id_usuario FROM usuario WHERE email = 'facundo.diaz@demo.sigrid'), 25000.00, 'CONFIRMADO',
        @ahora - INTERVAL 10 DAY, @ahora - INTERVAL 10 DAY, (SELECT id_usuario FROM usuario WHERE email = 'admin@sigrid.com'));
SET @pago = LAST_INSERT_ID();
UPDATE reserva r
  JOIN socio s ON s.id_socio = r.id_socio JOIN usuario u ON u.id_usuario = s.id_usuario
   SET r.id_pago = @pago, r.fecha_limite_reprogramacion = @hoy + INTERVAL 20 DAY
 WHERE u.email = 'facundo.diaz@demo.sigrid' AND r.fecha_turno = @hoy + INTERVAL 3 DAY AND r.estado = 'CANCELADA';

INSERT INTO pago (id_usuario, monto, estado, fecha_pago, fecha_validacion, id_admin_validador)
VALUES ((SELECT id_usuario FROM usuario WHERE email = 'tomas.acuna@demo.sigrid'), 18000.00, 'CONFIRMADO',
        @ahora - INTERVAL 40 DAY, @ahora - INTERVAL 40 DAY, (SELECT id_usuario FROM usuario WHERE email = 'admin@sigrid.com'));
SET @pago = LAST_INSERT_ID();
UPDATE reserva r
  JOIN socio s ON s.id_socio = r.id_socio JOIN usuario u ON u.id_usuario = s.id_usuario
   SET r.id_pago = @pago, r.fecha_limite_reprogramacion = @hoy - INTERVAL 2 DAY
 WHERE u.email = 'tomas.acuna@demo.sigrid' AND r.fecha_turno = @hoy - INTERVAL 4 DAY AND r.estado = 'CANCELADA';

-- sofia.herrera canceló su quincho hace una semana y le queda un crédito que vence en 4 días
INSERT INTO pago (id_usuario, monto, estado, fecha_pago, fecha_validacion, id_admin_validador)
VALUES ((SELECT id_usuario FROM usuario WHERE email = 'sofia.herrera@demo.sigrid'), 18000.00, 'CONFIRMADO',
        @ahora - INTERVAL 12 DAY, @ahora - INTERVAL 12 DAY, (SELECT id_usuario FROM usuario WHERE email = 'admin@sigrid.com'));
SET @pago = LAST_INSERT_ID();
UPDATE reserva r
  JOIN socio s ON s.id_socio = r.id_socio JOIN usuario u ON u.id_usuario = s.id_usuario
   SET r.id_pago = @pago, r.fecha_limite_reprogramacion = @hoy + INTERVAL 4 DAY
 WHERE u.email = 'sofia.herrera@demo.sigrid' AND r.fecha_turno = @hoy - INTERVAL 2 DAY AND r.estado = 'CANCELADA';

-- matias.rojas pide cancelar su turno de Beach Vóley (queda con crédito si se aprueba)
INSERT INTO solicitud_cambio_reserva (id_reserva, tipo, motivo, fecha_solicitud)
SELECT r.id_reserva, 'CANCELACION', 'Se lesionó un compañero y no llegamos a completar el equipo.', @ahora - INTERVAL 6 HOUR
  FROM reserva r JOIN socio s ON s.id_socio = r.id_socio JOIN usuario u ON u.id_usuario = s.id_usuario
 WHERE u.email = 'matias.rojas@demo.sigrid' AND r.fecha_turno = @hoy + INTERVAL 2 DAY AND r.estado = 'CONFIRMADA';

-- lucia.torres pide cancelar su turno de Beach Vóley
INSERT INTO solicitud_cambio_reserva (id_reserva, tipo, motivo, fecha_solicitud)
SELECT r.id_reserva, 'CANCELACION', 'Ese día tengo examen.', @ahora - INTERVAL 20 HOUR
  FROM reserva r JOIN socio s ON s.id_socio = r.id_socio JOIN usuario u ON u.id_usuario = s.id_usuario
 WHERE u.email = 'lucia.torres@demo.sigrid' AND r.fecha_turno = @hoy + INTERVAL 3 DAY AND r.estado = 'CONFIRMADA';

-- facundo.diaz quiere usar su crédito (de la reserva que canceló) en otro turno de la cancha de fútbol 11
INSERT INTO solicitud_cambio_reserva (id_reserva, tipo, id_turno_nuevo, fecha_nueva, motivo, fecha_solicitud)
SELECT r.id_reserva, 'REPROGRAMACION', r.id_turno, @hoy + INTERVAL 8 DAY, 'Uso el crédito de la reserva que cancelé.', @ahora - INTERVAL 2 HOUR
  FROM reserva r JOIN socio s ON s.id_socio = r.id_socio JOIN usuario u ON u.id_usuario = s.id_usuario
 WHERE u.email = 'facundo.diaz@demo.sigrid' AND r.fecha_turno = @hoy + INTERVAL 3 DAY AND r.estado = 'CANCELADA';

-- sofia.herrera ya había pasado su turno del SUM del día 7 al día 9
INSERT INTO historial_reserva (id_reserva, id_turno_anterior, fecha_anterior, motivo, fecha_cambio)
SELECT r.id_reserva, r.id_turno, @hoy + INTERVAL 7 DAY, 'Reprogramación: el evento se postergó una semana.', @ahora - INTERVAL 2 DAY
  FROM reserva r JOIN socio s ON s.id_socio = r.id_socio JOIN usuario u ON u.id_usuario = s.id_usuario
 WHERE u.email = 'sofia.herrera@demo.sigrid' AND r.fecha_turno = @hoy + INTERVAL 9 DAY AND r.estado = 'CONFIRMADA';

-- bruno.lopez ya había pasado su turno de fútbol del día 10 al día 12
INSERT INTO historial_reserva (id_reserva, id_turno_anterior, fecha_anterior, motivo, fecha_cambio)
SELECT r.id_reserva, r.id_turno, @hoy + INTERVAL 10 DAY, 'Reprogramación: el rival suspendió el partido.', @ahora - INTERVAL 5 DAY
  FROM reserva r JOIN socio s ON s.id_socio = r.id_socio JOIN usuario u ON u.id_usuario = s.id_usuario
 WHERE u.email = 'bruno.lopez@demo.sigrid' AND r.fecha_turno = @hoy + INTERVAL 12 DAY AND r.estado = 'CONFIRMADA';


-- ---------------------------------------------------------------------
-- Valoraciones (NO idempotente, correrlo una sola vez; necesita todo lo anterior): reservas ya realizadas
-- (dia negativo = días atrás) que los socios valoraron al terminar, de 1 a 5 estrellas con comentario opcional.
-- Las tres reservas pasadas que ya existían se valoran; las demás se crean confirmadas y con su pago.
-- La cancha de rugby no tiene ninguna (está en mantenimiento) y la pileta no se reserva.
-- ---------------------------------------------------------------------
DROP TEMPORARY TABLE IF EXISTS tmp_valoracion;
CREATE TEMPORARY TABLE tmp_valoracion AS
SELECT 'matias.rojas@demo.sigrid' AS email, 'Cancha de Fútbol 11' AS instalacion, '18:00:00' AS hora, -1 AS dia, 5 AS puntaje, 'Cancha en excelente estado, el césped impecable.' AS comentario UNION ALL
SELECT 'lucia.torres@demo.sigrid',     'Cancha de Beach Vóley', '19:00:00',  -1, 4, NULL UNION ALL
SELECT 'nicolas.ledesma@demo.sigrid',  'Cancha de Fútbol 11',   '20:00:00',  -7, 3, 'Buena cancha, pero los vestuarios estaban sucios.' UNION ALL
SELECT 'facundo.diaz@demo.sigrid',     'Cancha de Fútbol 11',   '17:00:00',  -2, 4, NULL UNION ALL
SELECT 'bruno.lopez@demo.sigrid',      'Cancha de Fútbol 11',   '19:00:00',  -3, 2, 'Las luces del fondo estaban apagadas y jugamos con poca visibilidad.' UNION ALL
SELECT 'tomas.acuna@demo.sigrid',      'Cancha de Fútbol 11',   '20:00:00',  -5, 3, NULL UNION ALL
SELECT 'sofia.herrera@demo.sigrid',    'Cancha de Fútbol 11',   '18:00:00',  -9, 4, NULL UNION ALL
SELECT 'lucia.torres@demo.sigrid',     'Cancha de Fútbol 11',   '17:00:00', -12, 5, 'Excelente, todo en orden y la cancha muy cuidada.' UNION ALL
SELECT 'camila.ruiz@demo.sigrid',      'Cancha de Fútbol 11',   '19:00:00', -15, 2, 'El arco tenía la red rota y no había pelotas.' UNION ALL
SELECT 'matias.rojas@demo.sigrid',     'Cancha de Fútbol 11',   '20:00:00', -20, 4, NULL UNION ALL
SELECT 'lucia.torres@demo.sigrid',     'Cancha de Fútbol 11',   '16:00:00', -40, 5, NULL UNION ALL
SELECT 'bruno.lopez@demo.sigrid',      'Cancha de Fútbol 11',   '18:00:00', -45, 5, 'Muy buena cancha.' UNION ALL
SELECT 'camila.ruiz@demo.sigrid',      'Cancha de Beach Vóley', '18:00:00',  -2, 5, 'Arena bien nivelada, muy buena.' UNION ALL
SELECT 'bruno.lopez@demo.sigrid',      'Cancha de Beach Vóley', '17:00:00',  -6, 3, 'La red estaba floja.' UNION ALL
SELECT 'nicolas.ledesma@demo.sigrid',  'Cancha de Beach Vóley', '19:00:00', -10, 4, NULL UNION ALL
SELECT 'sofia.herrera@demo.sigrid',    'Cancha de Beach Vóley', '20:00:00', -14, 5, NULL UNION ALL
SELECT 'tomas.acuna@demo.sigrid',      'Cancha de Beach Vóley', '17:00:00', -25, 3, NULL UNION ALL
SELECT 'camila.ruiz@demo.sigrid',      'Cancha de Beach Vóley', '17:00:00', -38, 4, NULL UNION ALL
SELECT 'sofia.herrera@demo.sigrid',    'Salón SUM',             '18:00:00',  -8, 5, 'Salón amplio y limpio, perfecto para el cumpleaños.' UNION ALL
SELECT 'facundo.diaz@demo.sigrid',     'Salón SUM',             '12:00:00', -16, 4, 'Muy bien, pero el sonido no funcionaba.' UNION ALL
SELECT 'bruno.lopez@demo.sigrid',      'Salón SUM',             '18:00:00', -30, 2, 'El aire acondicionado no andaba y hacía mucho calor.' UNION ALL
SELECT 'matias.rojas@demo.sigrid',     'Quincho 1',             '12:00:00',  -3, 5, 'Asador en perfecto estado.' UNION ALL
SELECT 'lucia.torres@demo.sigrid',     'Quincho 2',             '18:00:00',  -7, 4, NULL UNION ALL
SELECT 'camila.ruiz@demo.sigrid',      'Quincho 3',             '12:00:00', -11, 3, 'Faltaba limpiar las mesas.' UNION ALL
SELECT 'tomas.acuna@demo.sigrid',      'Quincho 2',             '12:00:00', -18, 1, 'Nos entregaron el quincho con mucha demora y sin limpiar.' UNION ALL
SELECT 'nicolas.ledesma@demo.sigrid',  'Quincho 1',             '18:00:00', -22, 4, NULL;

-- Reservas realizadas (confirmadas por el administrador) de las que todavía no existían
INSERT INTO reserva (id_socio, id_turno, fecha_turno, id_tarifa, estado, fecha_reserva, fecha_confirmacion, id_admin_confirmador)
SELECT s.id_socio, t.id_turno, @hoy + INTERVAL v.dia DAY, ta.id_tarifa, 'CONFIRMADA',
       TIMESTAMP(@hoy + INTERVAL v.dia DAY) - INTERVAL 3 DAY,
       TIMESTAMP(@hoy + INTERVAL v.dia DAY) - INTERVAL 3 DAY + INTERVAL 2 HOUR,
       (SELECT id_usuario FROM usuario WHERE email = 'admin@sigrid.com')
  FROM tmp_valoracion v
  JOIN usuario u ON u.email = v.email
  JOIN socio s ON s.id_usuario = u.id_usuario
  JOIN instalacion i ON i.nombre = v.instalacion
  JOIN turno t ON t.id_instalacion = i.id_instalacion AND t.hora_inicio = v.hora
  JOIN tarifa_alquiler ta ON ta.id_instalacion = i.id_instalacion AND ta.id_categoria_socio = s.id_categoria_socio
 WHERE NOT EXISTS (SELECT 1 FROM reserva x
                    WHERE x.id_turno = t.id_turno AND x.fecha_turno = @hoy + INTERVAL v.dia DAY
                      AND x.estado IN ('PENDIENTE_PAGO', 'CONFIRMADA'));

-- Su pago, ya validado por el administrador (igual que el bloque de tarifas y pagos de más arriba)
SET @base = (SELECT COALESCE(MAX(id_pago), 0) FROM pago);
SET @n = 0;
DROP TEMPORARY TABLE IF EXISTS tmp_pago_reserva;
CREATE TEMPORARY TABLE tmp_pago_reserva AS
SELECT r.id_reserva, s.id_usuario, ta.precio AS monto, r.fecha_confirmacion, r.id_admin_confirmador,
       @base + (@n := @n + 1) AS id_pago_nuevo
  FROM reserva r
  JOIN socio s ON s.id_socio = r.id_socio
  JOIN usuario u ON u.id_usuario = s.id_usuario
  JOIN tarifa_alquiler ta ON ta.id_tarifa = r.id_tarifa
 WHERE u.email LIKE '%@demo.sigrid' AND r.estado = 'CONFIRMADA' AND r.id_pago IS NULL
 ORDER BY r.id_reserva;

INSERT INTO pago (id_pago, id_usuario, monto, estado, fecha_pago, fecha_validacion, id_admin_validador)
SELECT id_pago_nuevo, id_usuario, monto, 'CONFIRMADO', fecha_confirmacion, fecha_confirmacion, id_admin_confirmador
  FROM tmp_pago_reserva;

UPDATE reserva r JOIN tmp_pago_reserva t ON t.id_reserva = r.id_reserva SET r.id_pago = t.id_pago_nuevo;
DROP TEMPORARY TABLE tmp_pago_reserva;

-- La valoración se deja una hora después de terminar el turno
INSERT INTO valoracion_reserva (id_reserva, puntaje, comentario, fecha_valoracion)
SELECT r.id_reserva, v.puntaje, v.comentario, TIMESTAMP(r.fecha_turno, t.hora_fin) + INTERVAL 1 HOUR
  FROM tmp_valoracion v
  JOIN usuario u ON u.email = v.email
  JOIN socio s ON s.id_usuario = u.id_usuario
  JOIN instalacion i ON i.nombre = v.instalacion
  JOIN turno t ON t.id_instalacion = i.id_instalacion AND t.hora_inicio = v.hora
  JOIN reserva r ON r.id_socio = s.id_socio AND r.id_turno = t.id_turno
                AND r.fecha_turno = @hoy + INTERVAL v.dia DAY AND r.estado = 'CONFIRMADA';
DROP TEMPORARY TABLE tmp_valoracion;

-- ---------------------------------------------------------------------
-- Historia para los reportes (NO idempotente, correrlo una sola vez; necesita todo lo anterior): reservas realizadas
-- hace 2 a 6 meses (con su pago) y dos socios cuya membresía venció sin renovar, para que los gráficos de ingresos,
-- la ocupación y la tasa de renovación tengan con qué comparar. También hace más realista cuánto tardó el
-- administrador en validar cada comprobante (1 a 7 horas).
-- ---------------------------------------------------------------------
DROP TEMPORARY TABLE IF EXISTS tmp_historia;
CREATE TEMPORARY TABLE tmp_historia AS
WITH RECURSIVE seq(n) AS (SELECT 1 UNION ALL SELECT n + 1 FROM seq WHERE n < 48)
SELECT n,
       ELT(1 + n MOD 8, 'matias.rojas@demo.sigrid', 'lucia.torres@demo.sigrid', 'facundo.diaz@demo.sigrid', 'camila.ruiz@demo.sigrid',
                        'bruno.lopez@demo.sigrid', 'sofia.herrera@demo.sigrid', 'tomas.acuna@demo.sigrid', 'nicolas.ledesma@demo.sigrid') AS email,
       ELT(1 + n MOD 8, 'Cancha de Fútbol 11', 'Cancha de Fútbol 11', 'Cancha de Beach Vóley', 'Salón SUM',
                        'Quincho 1', 'Cancha de Beach Vóley', 'Cancha de Fútbol 11', 'Quincho 2') AS instalacion,
       ELT(1 + n MOD 8, '18:00:00', '20:00:00', '19:00:00', '18:00:00', '12:00:00', '17:00:00', '19:00:00', '18:00:00') AS hora,
       -(50 + n * 3) AS dia
  FROM seq;

INSERT INTO reserva (id_socio, id_turno, fecha_turno, id_tarifa, estado, fecha_reserva, fecha_confirmacion, id_admin_confirmador)
SELECT s.id_socio, t.id_turno, @hoy + INTERVAL h.dia DAY, ta.id_tarifa, 'CONFIRMADA',
       TIMESTAMP(@hoy + INTERVAL h.dia DAY) - INTERVAL 3 DAY,
       TIMESTAMP(@hoy + INTERVAL h.dia DAY) - INTERVAL 3 DAY + INTERVAL 2 HOUR,
       (SELECT id_usuario FROM usuario WHERE email = 'admin@sigrid.com')
  FROM tmp_historia h
  JOIN usuario u ON u.email = h.email
  JOIN socio s ON s.id_usuario = u.id_usuario
  JOIN instalacion i ON i.nombre = h.instalacion
  JOIN turno t ON t.id_instalacion = i.id_instalacion AND t.hora_inicio = h.hora
  JOIN tarifa_alquiler ta ON ta.id_instalacion = i.id_instalacion AND ta.id_categoria_socio = s.id_categoria_socio;
DROP TEMPORARY TABLE tmp_historia;

-- Reservas realizadas de los últimos 44 días con demanda distinta según la hora (más a la tarde-noche) y el día
-- (más los fines de semana), para que el mapa de calor de ocupación tenga contraste. Se reparte a los socios por turno.
INSERT INTO reserva (id_socio, id_turno, fecha_turno, id_tarifa, estado, fecha_reserva, fecha_confirmacion, id_admin_confirmador)
SELECT s.id_socio, t.id_turno, @hoy - INTERVAL q.k DAY, ta.id_tarifa, 'CONFIRMADA',
       TIMESTAMP(@hoy - INTERVAL q.k DAY) - INTERVAL 3 DAY,
       TIMESTAMP(@hoy - INTERVAL q.k DAY) - INTERVAL 3 DAY + INTERVAL 2 HOUR,
       (SELECT id_usuario FROM usuario WHERE email = 'admin@sigrid.com')
  FROM (WITH RECURSIVE dias(k) AS (SELECT 1 UNION ALL SELECT k + 1 FROM dias WHERE k < 44) SELECT k FROM dias) q
  JOIN instalacion i ON i.tipo_acceso = 'ARANCELADO' AND i.nombre <> 'Cancha de Rugby'
  JOIN turno t ON t.id_instalacion = i.id_instalacion
  JOIN usuario u ON u.email = ELT(1 + (q.k + t.id_turno) MOD 8, 'matias.rojas@demo.sigrid', 'lucia.torres@demo.sigrid',
                                  'facundo.diaz@demo.sigrid', 'camila.ruiz@demo.sigrid', 'bruno.lopez@demo.sigrid',
                                  'sofia.herrera@demo.sigrid', 'tomas.acuna@demo.sigrid', 'nicolas.ledesma@demo.sigrid')
  JOIN socio s ON s.id_usuario = u.id_usuario
  JOIN tarifa_alquiler ta ON ta.id_instalacion = i.id_instalacion AND ta.id_categoria_socio = s.id_categoria_socio
 WHERE (q.k * 37 + t.id_turno * 17) MOD 100 <
       (CASE HOUR(t.hora_inicio) WHEN 20 THEN 45 WHEN 19 THEN 40 WHEN 18 THEN 35 WHEN 17 THEN 25 WHEN 16 THEN 12
                                 WHEN 12 THEN 18 WHEN 10 THEN 8 ELSE 6 END
        + IF(DAYOFWEEK(@hoy - INTERVAL q.k DAY) IN (1, 7), 15, 0))
   AND NOT EXISTS (SELECT 1 FROM reserva x
                    WHERE x.id_turno = t.id_turno AND x.fecha_turno = @hoy - INTERVAL q.k DAY
                      AND x.estado IN ('PENDIENTE_PAGO', 'CONFIRMADA'));

SET @base = (SELECT COALESCE(MAX(id_pago), 0) FROM pago);
SET @n = 0;
DROP TEMPORARY TABLE IF EXISTS tmp_pago_reserva;
CREATE TEMPORARY TABLE tmp_pago_reserva AS
SELECT r.id_reserva, s.id_usuario, ta.precio AS monto, r.fecha_confirmacion, r.id_admin_confirmador,
       @base + (@n := @n + 1) AS id_pago_nuevo
  FROM reserva r
  JOIN socio s ON s.id_socio = r.id_socio
  JOIN usuario u ON u.id_usuario = s.id_usuario
  JOIN tarifa_alquiler ta ON ta.id_tarifa = r.id_tarifa
 WHERE u.email LIKE '%@demo.sigrid' AND r.estado = 'CONFIRMADA' AND r.id_pago IS NULL
 ORDER BY r.id_reserva;

INSERT INTO pago (id_pago, id_usuario, monto, estado, fecha_pago, fecha_validacion, id_admin_validador)
SELECT id_pago_nuevo, id_usuario, monto, 'CONFIRMADO', fecha_confirmacion, fecha_confirmacion, id_admin_confirmador
  FROM tmp_pago_reserva;

UPDATE reserva r JOIN tmp_pago_reserva t ON t.id_reserva = r.id_reserva SET r.id_pago = t.id_pago_nuevo;
DROP TEMPORARY TABLE tmp_pago_reserva;

-- Membresías que vencieron sin renovar: valentina.paz (renovó una vez y después dejó) y agustina.coronel
INSERT INTO suscripcion_socio (id_socio, estado, fecha_inicio, fecha_vencimiento)
SELECT s.id_socio, 'VENCIDA', @hoy - INTERVAL d.desde DAY, @hoy - INTERVAL d.hasta DAY
  FROM (SELECT 'valentina.paz@demo.sigrid' AS email, 115 AS desde, 85 AS hasta UNION ALL
        SELECT 'valentina.paz@demo.sigrid',           85,         55 UNION ALL
        SELECT 'agustina.coronel@demo.sigrid',        60,         30) d
  JOIN usuario u ON u.email = d.email
  JOIN socio s ON s.id_usuario = u.id_usuario;

INSERT INTO pago (id_usuario, monto, estado, fecha_pago, fecha_validacion, id_admin_validador)
SELECT so.id_usuario, t.precio, 'CONFIRMADO', TIMESTAMP(s.fecha_inicio), TIMESTAMP(s.fecha_inicio) + INTERVAL 2 HOUR,
       (SELECT id_usuario FROM usuario WHERE email = 'admin@sigrid.com')
  FROM suscripcion_socio s
  JOIN socio so ON so.id_socio = s.id_socio
  JOIN tarifa_membresia t ON t.id_categoria_socio = so.id_categoria_socio AND t.vigente_hasta IS NULL
 WHERE s.id_pago IS NULL AND s.estado IN ('VIGENTE', 'VENCIDA', 'CANCELADA');

UPDATE suscripcion_socio s
  JOIN socio so ON so.id_socio = s.id_socio
  JOIN pago p ON p.id_usuario = so.id_usuario AND p.fecha_pago = TIMESTAMP(s.fecha_inicio)
   SET s.id_pago = p.id_pago
 WHERE s.id_pago IS NULL AND s.estado IN ('VIGENTE', 'VENCIDA', 'CANCELADA');

UPDATE suscripcion_socio SET fecha_solicitud = TIMESTAMP(fecha_inicio) - INTERVAL 1 DAY WHERE fecha_inicio IS NOT NULL;

-- "Socio desde" (el carnet y la ficha lo toman de usuario.fecha_alta): la cuenta se creó un día antes de su primera solicitud de membresía
UPDATE usuario u JOIN socio s ON s.id_usuario = u.id_usuario
   SET u.fecha_alta = COALESCE((SELECT MIN(x.fecha_solicitud) FROM suscripcion_socio x WHERE x.id_socio = s.id_socio), u.fecha_alta) - INTERVAL 1 DAY
 WHERE u.email LIKE '%@demo.sigrid';

-- Cuánto tardó el administrador en validar cada comprobante de reserva: entre 1 y 7 horas
UPDATE pago p JOIN reserva r ON r.id_pago = p.id_pago
   SET p.fecha_pago = p.fecha_validacion - INTERVAL (1 + r.id_reserva MOD 7) HOUR
 WHERE p.estado = 'CONFIRMADO' AND p.fecha_pago = p.fecha_validacion;


-- =====================================================================
-- (12) CUENTAS PARA LA DEMO CON CLIENTES (necesita los bloques 10 y 11)
-- Cuatro socios armados a propósito (contraseña Admin123!), cada uno con varios casos a la vez:
--   socio.completo@demo.sigrid  Alumno, membresía vigente, SIN valoraciones pendientes. Tiene: reserva confirmada de mañana
--                               (ya no se cancela), confirmada con más de 48 h (se puede pedir la cancelación), una con la
--                               cancelación ya pedida, una con el comprobante EN REVISIÓN, una RECHAZADA, una CANCELADA con
--                               crédito de reprogramación vigente (la reprogramás desde Mis reservas) e historial de turnos realizados y valorados.
--   socio.valorar@demo.sigrid   Externo, vigente (vence en 5 días), con 2 valoraciones pendientes: no puede reservar hasta valorarlas.
--   socio.vencido@demo.sigrid   Docente, membresía vencida (carnet gris, no puede reservar), con historial.
--   socio.reservar@demo.sigrid  No Docente, membresía vigente y nada pendiente: la cuenta para hacer una reserva EN VIVO de punta a punta
--                               (reservar, subir el comprobante, que el administrador la confirme). Ver sigrid-comprobantes en el README de la demo.
-- ES REINICIABLE: al empezar borra estas cuatro cuentas y las vuelve a crear con fechas de HOY, así que se puede correr SOLO
-- este bloque antes de cada demo para dejar todo como nuevo. Si un turno que necesita ya lo ocupaba otra reserva, la cancela.
-- =====================================================================

-- Si se corre SOLO este bloque en una sesión nueva, @hoy y @ahora toman la fecha y hora de ahora (el lunes de la demo, antes de empezar)
SET @hoy = DATE(COALESCE(@hoy, CURDATE()));
SET @ahora = COALESCE(@ahora, NOW());

-- Reinicio: borra lo que hubiera de estas cuatro cuentas (en el orden que piden las FK)
DROP TEMPORARY TABLE IF EXISTS t12_socios;
CREATE TEMPORARY TABLE t12_socios AS
SELECT s.id_socio, s.id_usuario FROM socio s JOIN usuario u ON u.id_usuario = s.id_usuario
 WHERE u.email IN ('socio.completo@demo.sigrid', 'socio.valorar@demo.sigrid', 'socio.vencido@demo.sigrid', 'socio.reservar@demo.sigrid');

UPDATE suscripcion_socio SET id_pago = NULL WHERE id_socio IN (SELECT id_socio FROM t12_socios);
UPDATE reserva SET id_pago = NULL WHERE id_socio IN (SELECT id_socio FROM t12_socios);
DELETE FROM solicitud_cambio_reserva WHERE id_reserva IN (SELECT id_reserva FROM reserva WHERE id_socio IN (SELECT id_socio FROM t12_socios));
DELETE FROM historial_reserva WHERE id_reserva IN (SELECT id_reserva FROM reserva WHERE id_socio IN (SELECT id_socio FROM t12_socios));
DELETE FROM valoracion_reserva WHERE id_reserva IN (SELECT id_reserva FROM reserva WHERE id_socio IN (SELECT id_socio FROM t12_socios));
DELETE FROM comprobante_pago WHERE id_pago IN (SELECT id_pago FROM pago WHERE id_usuario IN (SELECT id_usuario FROM t12_socios));
DELETE FROM reserva WHERE id_socio IN (SELECT id_socio FROM t12_socios);
DELETE FROM suscripcion_socio WHERE id_socio IN (SELECT id_socio FROM t12_socios);
DELETE FROM pago WHERE id_usuario IN (SELECT id_usuario FROM t12_socios);
DELETE FROM carnet_digital WHERE id_socio IN (SELECT id_socio FROM t12_socios);
DELETE FROM socio WHERE id_socio IN (SELECT id_socio FROM t12_socios);
DELETE FROM usuario WHERE email IN ('socio.completo@demo.sigrid', 'socio.valorar@demo.sigrid', 'socio.vencido@demo.sigrid', 'socio.reservar@demo.sigrid');
DROP TEMPORARY TABLE t12_socios;

-- Cuentas y socios
INSERT INTO usuario (id_rol, nombre, apellido, email, password_hash)
SELECT r.id_rol, d.nombre, d.apellido, d.email,
       '120000:oYLPLGj3glpFkviMWoNcyw==:HqlNgIyuNtfwqirTmWx7xcDntn0EmC+qGFd10+z/dHw='
  FROM rol r
  JOIN (SELECT 'Martina' AS nombre, 'Gómez' AS apellido, 'socio.completo@demo.sigrid' AS email UNION ALL
        SELECT 'Joaquín', 'Vera',    'socio.valorar@demo.sigrid' UNION ALL
        SELECT 'Elena',   'Navarro', 'socio.vencido@demo.sigrid' UNION ALL
        SELECT 'Lautaro', 'Medina',  'socio.reservar@demo.sigrid') d
 WHERE r.nombre_rol = 'SOCIO';

INSERT INTO socio (id_usuario, id_categoria_socio, dni, fecha_nacimiento, telefono, legajo, estado)
SELECT u.id_usuario, c.id_categoria_socio, d.dni, d.nacimiento, d.telefono, d.legajo, d.estado
  FROM (SELECT 'socio.completo@demo.sigrid' AS email, 'Alumno UNSE' AS categoria, '44100200' AS dni, '2004-06-18' AS nacimiento, '3855100200' AS telefono, 'A-2001' AS legajo, 'ACTIVO' AS estado UNION ALL
        SELECT 'socio.valorar@demo.sigrid', 'Externo',      '36200300', '1992-02-11', '3855200300', NULL,     'ACTIVO' UNION ALL
        SELECT 'socio.vencido@demo.sigrid', 'Docente UNSE', '27300400', '1980-09-30', '3855300400', 'D-0500', 'NO_ACTIVO' UNION ALL
        SELECT 'socio.reservar@demo.sigrid', 'No Docente UNSE', '33400500', '1988-03-22', '3855400500', 'N-0420', 'ACTIVO') d
  JOIN usuario u ON u.email = d.email
  JOIN categoria_socio c ON c.nombre_categoria = d.categoria;

DROP TEMPORARY TABLE IF EXISTS t12_socios;
CREATE TEMPORARY TABLE t12_socios AS
SELECT s.id_socio, s.id_usuario FROM socio s JOIN usuario u ON u.id_usuario = s.id_usuario
 WHERE u.email IN ('socio.completo@demo.sigrid', 'socio.valorar@demo.sigrid', 'socio.vencido@demo.sigrid', 'socio.reservar@demo.sigrid');

INSERT INTO carnet_digital (id_socio, tipo_carnet, estado)
SELECT s.id_socio, IF(c.nombre_categoria = 'Alumno UNSE', 'ESTUDIANTIL', 'GENERAL'), IF(s.estado = 'ACTIVO', 'ACTIVO', 'INACTIVO')
  FROM socio s JOIN categoria_socio c ON c.id_categoria_socio = s.id_categoria_socio
 WHERE s.id_socio IN (SELECT id_socio FROM t12_socios);

-- Membresías (desde/hasta = días desde hoy): la vigente y las anteriores, ya vencidas
INSERT INTO suscripcion_socio (id_socio, estado, fecha_inicio, fecha_vencimiento)
SELECT s.id_socio, d.estado, @hoy + INTERVAL d.desde DAY, @hoy + INTERVAL d.hasta DAY
  FROM (SELECT 'socio.completo@demo.sigrid' AS email, 'VIGENTE' AS estado, -4 AS desde,  26 AS hasta UNION ALL
        SELECT 'socio.completo@demo.sigrid', 'VENCIDA', -34, -4 UNION ALL
        SELECT 'socio.completo@demo.sigrid', 'VENCIDA', -64, -34 UNION ALL
        SELECT 'socio.valorar@demo.sigrid',  'VIGENTE', -25,   5 UNION ALL
        SELECT 'socio.valorar@demo.sigrid',  'VENCIDA', -55, -25 UNION ALL
        SELECT 'socio.vencido@demo.sigrid',  'VENCIDA', -40, -10 UNION ALL
        SELECT 'socio.vencido@demo.sigrid',  'VENCIDA', -70, -40 UNION ALL
        SELECT 'socio.reservar@demo.sigrid', 'VIGENTE',  -3,  27 UNION ALL
        SELECT 'socio.reservar@demo.sigrid', 'VENCIDA', -33,  -3 UNION ALL
        SELECT 'socio.reservar@demo.sigrid', 'VENCIDA', -63, -33) d
  JOIN usuario u ON u.email = d.email
  JOIN socio s ON s.id_usuario = u.id_usuario;

INSERT INTO pago (id_usuario, monto, estado, fecha_pago, fecha_validacion, id_admin_validador)
SELECT so.id_usuario, t.precio, 'CONFIRMADO', TIMESTAMP(s.fecha_inicio), TIMESTAMP(s.fecha_inicio) + INTERVAL 2 HOUR,
       (SELECT id_usuario FROM usuario WHERE email = 'admin@sigrid.com')
  FROM suscripcion_socio s
  JOIN socio so ON so.id_socio = s.id_socio
  JOIN tarifa_membresia t ON t.id_categoria_socio = so.id_categoria_socio AND t.vigente_hasta IS NULL
 WHERE s.id_socio IN (SELECT id_socio FROM t12_socios) AND s.id_pago IS NULL;

UPDATE suscripcion_socio s
  JOIN socio so ON so.id_socio = s.id_socio
  JOIN pago p ON p.id_usuario = so.id_usuario AND p.fecha_pago = TIMESTAMP(s.fecha_inicio)
   SET s.id_pago = p.id_pago, s.fecha_solicitud = TIMESTAMP(s.fecha_inicio) - INTERVAL 1 DAY
 WHERE s.id_socio IN (SELECT id_socio FROM t12_socios) AND s.id_pago IS NULL;

UPDATE usuario u JOIN socio s ON s.id_usuario = u.id_usuario
   SET u.fecha_alta = COALESCE((SELECT MIN(x.fecha_solicitud) FROM suscripcion_socio x WHERE x.id_socio = s.id_socio), u.fecha_alta) - INTERVAL 1 DAY
 WHERE s.id_socio IN (SELECT id_socio FROM t12_socios);

-- Reservas. dia = días desde hoy (negativo = pasado); pago = cómo quedó su pago (NULL = sin pago, no pasa acá);
-- limite = días hasta que vence el crédito de reprogramación (solo canceladas con pago); puntaje/comentario = su valoración.
-- Las de dia -1 de socio.valorar son las que le faltan valorar (terminaron ayer; el sistema exige valorar desde el 25/09/2026).
DROP TEMPORARY TABLE IF EXISTS t12_res;
CREATE TEMPORARY TABLE t12_res (
  email VARCHAR(255), instalacion VARCHAR(100), hora TIME, dia INT, estado VARCHAR(20), pago VARCHAR(25),
  hace_horas INT, limite INT NULL, puntaje INT NULL, comentario VARCHAR(255) NULL
);
INSERT INTO t12_res VALUES
  -- socio.completo: lo que viene
  ('socio.completo@demo.sigrid', 'Quincho 1',             '12:00:00',  1, 'CONFIRMADA',     'CONFIRMADO',           30, NULL, NULL, NULL),
  ('socio.completo@demo.sigrid', 'Cancha de Beach Vóley', '18:00:00',  5, 'CONFIRMADA',     'CONFIRMADO',           60, NULL, NULL, NULL),
  ('socio.completo@demo.sigrid', 'Salón SUM',             '12:00:00', 10, 'CONFIRMADA',     'CONFIRMADO',           50, NULL, NULL, NULL),
  ('socio.completo@demo.sigrid', 'Cancha de Fútbol 11',   '19:00:00',  7, 'PENDIENTE_PAGO', 'PENDIENTE_VALIDACION',  3, NULL, NULL, NULL),
  ('socio.completo@demo.sigrid', 'Cancha de Beach Vóley', '19:00:00',  3, 'RECHAZADA',      'RECHAZADO',            30, NULL, NULL, NULL),
  ('socio.completo@demo.sigrid', 'Cancha de Fútbol 11',   '17:00:00',  4, 'CANCELADA',      'CONFIRMADO',          200,   22, NULL, NULL),
  -- socio.completo: historial realizado y valorado
  ('socio.completo@demo.sigrid', 'Cancha de Fútbol 11',   '18:00:00', -46, 'CONFIRMADA',    'CONFIRMADO',         1176, NULL, 5, 'Todo impecable, volvemos seguro.'),
  ('socio.completo@demo.sigrid', 'Cancha de Beach Vóley', '17:00:00', -49, 'CONFIRMADA',    'CONFIRMADO',         1248, NULL, 4, NULL),
  ('socio.completo@demo.sigrid', 'Quincho 2',             '18:00:00', -52, 'CONFIRMADA',    'CONFIRMADO',         1320, NULL, 3, 'Faltaba limpiar el asador.'),
  -- socio.valorar: dos turnos de ayer sin valorar y uno viejo ya valorado
  ('socio.valorar@demo.sigrid',  'Cancha de Fútbol 11',   '16:00:00',  -1, 'CONFIRMADA',    'CONFIRMADO',          96, NULL, NULL, NULL),
  ('socio.valorar@demo.sigrid',  'Salón SUM',             '12:00:00',  -1, 'CONFIRMADA',    'CONFIRMADO',          96, NULL, NULL, NULL),
  ('socio.valorar@demo.sigrid',  'Quincho 3',             '18:00:00', -47, 'CONFIRMADA',    'CONFIRMADO',        1200, NULL, 4, NULL),
  -- socio.vencido: historial de cuando todavía era socio activo
  ('socio.vencido@demo.sigrid',  'Cancha de Fútbol 11',   '20:00:00', -48, 'CONFIRMADA',    'CONFIRMADO',        1224, NULL, 4, NULL),
  ('socio.vencido@demo.sigrid',  'Quincho 1',             '12:00:00', -50, 'CONFIRMADA',    'CONFIRMADO',        1272, NULL, NULL, NULL),
  ('socio.vencido@demo.sigrid',  'Salón SUM',             '18:00:00', -51, 'CONFIRMADA',    'CONFIRMADO',        1296, NULL, 5, 'Salón amplio y limpio.'),
  -- socio.reservar: una reserva confirmada más adelante y dos turnos viejos ya valorados (nada pendiente de valorar)
  ('socio.reservar@demo.sigrid', 'Cancha de Fútbol 11',   '18:00:00',  9, 'CONFIRMADA',     'CONFIRMADO',           80, NULL, NULL, NULL),
  ('socio.reservar@demo.sigrid', 'Cancha de Beach Vóley', '19:00:00', -21, 'CONFIRMADA',    'CONFIRMADO',          600, NULL, 5, 'Arena bien nivelada, volvemos seguro.'),
  ('socio.reservar@demo.sigrid', 'Quincho 1',             '18:00:00', -30, 'CONFIRMADA',    'CONFIRMADO',          800, NULL, 4, NULL);

-- Si otra reserva ocupaba alguno de estos turnos (por ejemplo la decoración aleatoria de los reportes), se cancela para que el turno sea de la demo
UPDATE reserva x
  JOIN turno t ON t.id_turno = x.id_turno
  JOIN instalacion i ON i.id_instalacion = t.id_instalacion
  JOIN t12_res d ON d.instalacion = i.nombre AND d.hora = t.hora_inicio AND x.fecha_turno = @hoy + INTERVAL d.dia DAY
   SET x.estado = 'CANCELADA'
 WHERE x.estado IN ('PENDIENTE_PAGO', 'CONFIRMADA') AND d.estado IN ('PENDIENTE_PAGO', 'CONFIRMADA')
   AND x.id_socio NOT IN (SELECT id_socio FROM t12_socios);

INSERT INTO reserva (id_socio, id_turno, fecha_turno, id_tarifa, estado, fecha_reserva, fecha_confirmacion, fecha_limite_reprogramacion, id_admin_confirmador)
SELECT s.id_socio, t.id_turno, @hoy + INTERVAL d.dia DAY, ta.id_tarifa, d.estado,
       @ahora - INTERVAL d.hace_horas HOUR,
       IF(d.estado = 'CONFIRMADA', @ahora - INTERVAL (d.hace_horas - 2) HOUR, NULL),
       IF(d.limite IS NULL, NULL, @hoy + INTERVAL d.limite DAY),
       IF(d.estado = 'CONFIRMADA', (SELECT id_usuario FROM usuario WHERE email = 'admin@sigrid.com'), NULL)
  FROM t12_res d
  JOIN usuario u ON u.email = d.email
  JOIN socio s ON s.id_usuario = u.id_usuario
  JOIN instalacion i ON i.nombre = d.instalacion
  JOIN turno t ON t.id_instalacion = i.id_instalacion AND t.hora_inicio = d.hora
  JOIN tarifa_alquiler ta ON ta.id_instalacion = i.id_instalacion AND ta.id_categoria_socio = s.id_categoria_socio;

-- Los pagos de esas reservas: confirmado (validado por el administrador), en revisión o rechazado (con motivo)
SET @base = (SELECT COALESCE(MAX(id_pago), 0) FROM pago);
SET @n = 0;
DROP TEMPORARY TABLE IF EXISTS t12_pago;
CREATE TEMPORARY TABLE t12_pago AS
SELECT r.id_reserva, s.id_usuario, ta.precio AS monto, d.pago, r.fecha_reserva, @base + (@n := @n + 1) AS id_pago_nuevo
  FROM t12_res d
  JOIN usuario u ON u.email = d.email
  JOIN socio s ON s.id_usuario = u.id_usuario
  JOIN instalacion i ON i.nombre = d.instalacion
  JOIN turno t ON t.id_instalacion = i.id_instalacion AND t.hora_inicio = d.hora
  JOIN reserva r ON r.id_socio = s.id_socio AND r.id_turno = t.id_turno
                AND r.fecha_turno = @hoy + INTERVAL d.dia DAY AND r.estado = d.estado
  JOIN tarifa_alquiler ta ON ta.id_tarifa = r.id_tarifa
 WHERE d.pago IS NOT NULL
 ORDER BY r.id_reserva;

INSERT INTO pago (id_pago, id_usuario, monto, estado, fecha_pago, fecha_validacion, id_admin_validador, motivo_rechazo)
SELECT id_pago_nuevo, id_usuario, monto, pago, fecha_reserva + INTERVAL 30 MINUTE,
       IF(pago = 'PENDIENTE_VALIDACION', NULL, fecha_reserva + INTERVAL 2 HOUR),
       IF(pago = 'PENDIENTE_VALIDACION', NULL, (SELECT id_usuario FROM usuario WHERE email = 'admin@sigrid.com')),
       IF(pago = 'RECHAZADO', 'El monto transferido no coincide con el precio del turno', NULL)
  FROM t12_pago;

-- El en revisión y el rechazado tienen su comprobante (imágenes de muestra en sigrid-comprobantes/demo)
INSERT INTO comprobante_pago (id_pago, archivo_url, hash_archivo, fecha_operacion_declarada)
SELECT id_pago_nuevo, IF(pago = 'RECHAZADO', 'demo/comprobante-rechazado.png', 'demo/comprobante-revision.png'),
       SHA2(CONCAT('t12-comprobante-', id_reserva), 256), fecha_reserva + INTERVAL 20 MINUTE
  FROM t12_pago WHERE pago <> 'CONFIRMADO';

UPDATE reserva r JOIN t12_pago p ON p.id_reserva = r.id_reserva SET r.id_pago = p.id_pago_nuevo;
DROP TEMPORARY TABLE t12_pago;

-- Valoraciones del historial (una hora después de terminar el turno)
INSERT INTO valoracion_reserva (id_reserva, puntaje, comentario, fecha_valoracion)
SELECT r.id_reserva, d.puntaje, d.comentario, TIMESTAMP(r.fecha_turno, t.hora_fin) + INTERVAL 1 HOUR
  FROM t12_res d
  JOIN usuario u ON u.email = d.email
  JOIN socio s ON s.id_usuario = u.id_usuario
  JOIN instalacion i ON i.nombre = d.instalacion
  JOIN turno t ON t.id_instalacion = i.id_instalacion AND t.hora_inicio = d.hora
  JOIN reserva r ON r.id_socio = s.id_socio AND r.id_turno = t.id_turno
                AND r.fecha_turno = @hoy + INTERVAL d.dia DAY AND r.estado = d.estado
 WHERE d.puntaje IS NOT NULL;

-- socio.completo ya pidió cancelar su turno del SUM (queda por aprobar en "Cancelaciones" del administrador)
INSERT INTO solicitud_cambio_reserva (id_reserva, tipo, motivo, fecha_solicitud)
SELECT r.id_reserva, 'CANCELACION', 'Se suspendió el evento familiar.', @ahora - INTERVAL 3 HOUR
  FROM reserva r
  JOIN turno t ON t.id_turno = r.id_turno
  JOIN instalacion i ON i.id_instalacion = t.id_instalacion
 WHERE r.id_socio = (SELECT s.id_socio FROM socio s JOIN usuario u ON u.id_usuario = s.id_usuario WHERE u.email = 'socio.completo@demo.sigrid')
   AND i.nombre = 'Salón SUM' AND r.fecha_turno = @hoy + INTERVAL 10 DAY AND r.estado = 'CONFIRMADA';

-- ... y hace 8 días le aprobaron la cancelación que le dejó su crédito de reprogramación (aviso "Cancelación aprobada")
INSERT INTO solicitud_cambio_reserva (id_reserva, tipo, motivo, estado, fecha_solicitud, fecha_resolucion, id_admin_resolutor)
SELECT r.id_reserva, 'CANCELACION', 'Un imprevisto familiar.', 'APROBADA', @ahora - INTERVAL 8 DAY - INTERVAL 5 HOUR, @ahora - INTERVAL 8 DAY,
       (SELECT id_usuario FROM usuario WHERE email = 'admin@sigrid.com')
  FROM reserva r
  JOIN turno t ON t.id_turno = r.id_turno
  JOIN instalacion i ON i.id_instalacion = t.id_instalacion
 WHERE r.id_socio = (SELECT s.id_socio FROM socio s JOIN usuario u ON u.id_usuario = s.id_usuario WHERE u.email = 'socio.completo@demo.sigrid')
   AND i.nombre = 'Cancha de Fútbol 11' AND r.fecha_turno = @hoy + INTERVAL 4 DAY AND r.estado = 'CANCELADA';

DROP TEMPORARY TABLE t12_res;
DROP TEMPORARY TABLE t12_socios;


-- =====================================================================
-- LIMPIEZA (opcional): quita todo lo del bloque (11). Descomentar y ejecutar.
-- El orden respeta las FK. No toca al administrador ni a los roles. Quita también las
-- instalaciones de la versión anterior del bloque (fútbol 5, vóley, básquet, tenis, gimnasio, hockey).
-- =====================================================================
-- UPDATE suscripcion_socio SET id_pago = NULL WHERE id_socio IN (SELECT id_socio FROM socio WHERE id_usuario IN (SELECT id_usuario FROM usuario WHERE email LIKE '%@demo.sigrid'));
-- DELETE FROM carnet_digital WHERE id_socio IN (SELECT id_socio FROM socio WHERE id_usuario IN (SELECT id_usuario FROM usuario WHERE email LIKE '%@demo.sigrid'));
-- DELETE FROM solicitud_cambio_reserva WHERE id_reserva IN (SELECT id_reserva FROM reserva WHERE id_socio IN (SELECT id_socio FROM socio WHERE id_usuario IN (SELECT id_usuario FROM usuario WHERE email LIKE '%@demo.sigrid')));
-- DELETE FROM historial_reserva WHERE id_reserva IN (SELECT id_reserva FROM reserva WHERE id_socio IN (SELECT id_socio FROM socio WHERE id_usuario IN (SELECT id_usuario FROM usuario WHERE email LIKE '%@demo.sigrid')));
-- UPDATE reserva SET id_pago = NULL WHERE id_socio IN (SELECT id_socio FROM socio WHERE id_usuario IN (SELECT id_usuario FROM usuario WHERE email LIKE '%@demo.sigrid'));
-- DELETE FROM comprobante_pago WHERE archivo_url LIKE 'demo/%';
-- DELETE FROM pago WHERE id_usuario IN (SELECT id_usuario FROM usuario WHERE email LIKE '%@demo.sigrid');
-- DELETE FROM valoracion_reserva WHERE id_reserva IN (SELECT id_reserva FROM reserva WHERE id_socio IN (SELECT id_socio FROM socio WHERE id_usuario IN (SELECT id_usuario FROM usuario WHERE email LIKE '%@demo.sigrid')));
-- DELETE FROM reserva WHERE id_socio IN (SELECT id_socio FROM socio WHERE id_usuario IN (SELECT id_usuario FROM usuario WHERE email LIKE '%@demo.sigrid'));
-- DELETE FROM suscripcion_socio WHERE id_socio IN (SELECT id_socio FROM socio WHERE id_usuario IN (SELECT id_usuario FROM usuario WHERE email LIKE '%@demo.sigrid'));
-- DELETE FROM socio WHERE id_usuario IN (SELECT id_usuario FROM usuario WHERE email LIKE '%@demo.sigrid');
-- DELETE FROM usuario WHERE email LIKE '%@demo.sigrid';
-- DELETE FROM horario_apertura WHERE id_instalacion IN (SELECT id_instalacion FROM instalacion WHERE nombre = 'Pileta');
-- DELETE FROM turno WHERE id_instalacion IN (SELECT id_instalacion FROM instalacion WHERE nombre IN ('Cancha de Fútbol 11', 'Cancha de Fútbol 5', 'Cancha de Vóley', 'Cancha de Básquet', 'Cancha de Tenis', 'Pileta', 'Gimnasio', 'Cancha de Rugby', 'Cancha de Hockey', 'Cancha de Beach Vóley', 'Salón SUM', 'Quincho 1', 'Quincho 2', 'Quincho 3'));
-- DELETE FROM instalacion WHERE nombre IN ('Cancha de Fútbol 11', 'Cancha de Fútbol 5', 'Cancha de Vóley', 'Cancha de Básquet', 'Cancha de Tenis', 'Pileta', 'Gimnasio', 'Cancha de Rugby', 'Cancha de Hockey', 'Cancha de Beach Vóley', 'Salón SUM', 'Quincho 1', 'Quincho 2', 'Quincho 3');
