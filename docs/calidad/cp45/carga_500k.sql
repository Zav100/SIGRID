-- Escenario de CP-45: 500.000 reservas en una base de PRUEBA (sigrid_cc06, nunca la real).
USE sigrid_cc06;
SET SESSION cte_max_recursion_depth = 1000000;
SET SESSION foreign_key_checks = 0;
SET SESSION unique_checks = 0;

-- 30 instalaciones más, con 5 turnos cada una (150 turnos): un historial de unos 7 años
INSERT INTO instalacion (nombre, tipo_disciplina, capacidad, tipo_acceso, estado)
SELECT CONCAT('Cancha extra ', n.i),
       CASE n.i % 3 WHEN 0 THEN 'Fútbol' WHEN 1 THEN 'Vóley' ELSE 'Paddle' END,
       10, 'ARANCELADO', 'HABILITADA'
FROM (WITH RECURSIVE s(i) AS (SELECT 1 UNION ALL SELECT i + 1 FROM s WHERE i < 30) SELECT i FROM s) n;

INSERT INTO turno (id_instalacion, hora_inicio, hora_fin)
SELECT i.id_instalacion, MAKETIME(h.h, 0, 0), MAKETIME(h.h + 1, 0, 0)
FROM instalacion i
JOIN (SELECT 8 h UNION SELECT 10 UNION SELECT 12 UNION SELECT 16 UNION SELECT 18) h
WHERE i.nombre LIKE 'Cancha extra %';

DROP TABLE IF EXISTS tmp_turnos;
CREATE TABLE tmp_turnos AS
SELECT t.id_turno, ROW_NUMBER() OVER (ORDER BY t.id_turno) - 1 AS k
FROM turno t JOIN instalacion i ON i.id_instalacion = t.id_instalacion
WHERE i.nombre LIKE 'Cancha extra %';
SELECT COUNT(*) INTO @T FROM tmp_turnos;

DROP TABLE IF EXISTS tmp_socios;
CREATE TABLE tmp_socios AS
SELECT s.id_socio, s.id_usuario, ROW_NUMBER() OVER (ORDER BY s.id_socio) - 1 AS k FROM socio s;
SELECT COUNT(*) INTO @S FROM tmp_socios;
SELECT id_usuario INTO @admin FROM usuario WHERE id_rol = (SELECT id_rol FROM rol WHERE nombre_rol = 'ADMINISTRADOR') LIMIT 1;
SELECT MIN(id_tarifa) INTO @tarifa FROM tarifa_alquiler;

DROP TABLE IF EXISTS nums;
CREATE TABLE nums (i INT UNSIGNED NOT NULL PRIMARY KEY);
INSERT INTO nums
WITH RECURSIVE s(i) AS (SELECT 1 UNION ALL SELECT i + 1 FROM s WHERE i < 500000)
SELECT i FROM s;

-- cada reserva ocupa un (turno, día) distinto: el primero es el más reciente y los demás van hacia atrás
-- i % 10: 0-5 confirmada, 6 cancelada con crédito, 7 cancelada sin pago, 8 rechazada, 9 pendiente (solo las de los últimos 30 días; el resto, cancelada)
DROP TABLE IF EXISTS tmp_res;
CREATE TABLE tmp_res AS
SELECT n.i,
       DATE_SUB('2026-10-31', INTERVAL ((n.i - 1) DIV @T) DAY) AS fecha,
       tt.id_turno,
       ss.id_socio, ss.id_usuario,
       CASE
         WHEN n.i % 10 <= 5 THEN 'CONFIRMADA'
         WHEN n.i % 10 = 6 THEN 'CANCELADA'
         WHEN n.i % 10 = 7 THEN 'CANCELADA'
         WHEN n.i % 10 = 8 THEN 'RECHAZADA'
         WHEN ((n.i - 1) DIV @T) < 30 THEN 'PENDIENTE_PAGO'
         ELSE 'CANCELADA'
       END AS estado,
       CASE
         WHEN n.i % 10 <= 6 THEN 'CONFIRMADO'
         WHEN n.i % 10 = 8 THEN 'RECHAZADO'
         WHEN n.i % 10 = 9 AND ((n.i - 1) DIV @T) < 30 AND n.i % 20 = 9 THEN 'PENDIENTE_VALIDACION'
         ELSE NULL
       END AS pago_estado
FROM nums n
JOIN tmp_turnos tt ON tt.k = (n.i - 1) % @T
JOIN tmp_socios ss ON ss.k = n.i % @S;

INSERT INTO pago (id_pago, id_usuario, monto, estado, fecha_pago, fecha_validacion, id_admin_validador)
SELECT 1000000 + r.i, r.id_usuario, 15000 + (r.i % 5) * 2500, r.pago_estado,
       TIMESTAMP(DATE_SUB(r.fecha, INTERVAL 3 DAY), '10:00:00'),
       CASE WHEN r.pago_estado = 'PENDIENTE_VALIDACION' THEN NULL ELSE TIMESTAMP(DATE_SUB(r.fecha, INTERVAL 2 DAY), '12:00:00') END,
       CASE WHEN r.pago_estado = 'PENDIENTE_VALIDACION' THEN NULL ELSE @admin END
FROM tmp_res r
WHERE r.pago_estado IS NOT NULL;

INSERT INTO reserva (id_socio, id_turno, fecha_turno, id_tarifa, id_pago, estado, fecha_reserva, fecha_confirmacion,
                     fecha_limite_reprogramacion, id_admin_confirmador)
SELECT r.id_socio, r.id_turno, r.fecha, @tarifa,
       CASE WHEN r.pago_estado IS NULL THEN NULL ELSE 1000000 + r.i END,
       r.estado,
       TIMESTAMP(DATE_SUB(r.fecha, INTERVAL 7 DAY), '09:00:00'),
       CASE WHEN r.estado = 'CONFIRMADA' THEN TIMESTAMP(DATE_SUB(r.fecha, INTERVAL 2 DAY), '12:00:00') ELSE NULL END,
       CASE WHEN r.i % 10 = 6 THEN DATE_ADD(r.fecha, INTERVAL 30 DAY) ELSE NULL END,
       CASE WHEN r.estado = 'CONFIRMADA' THEN @admin ELSE NULL END
FROM tmp_res r;

ANALYZE TABLE reserva, pago, turno;
SELECT estado, COUNT(*) FROM reserva GROUP BY estado;
SELECT COUNT(*) AS reservas, MIN(fecha_turno) AS desde, MAX(fecha_turno) AS hasta FROM reserva;
DROP TABLE tmp_res; DROP TABLE nums;
