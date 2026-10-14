# Reporte de verificación y validación (V&V) — SIGRID

Generado automáticamente por `docs/calidad/generar_reporte_vv.py` con la plantilla del Apéndice F del plan de garantía de calidad. Para regenerarlo: `mvn test` dentro de `SIGRID/` y luego `python docs/calidad/generar_reporte_vv.py` desde la raíz del repositorio.

## F.1 Datos generales

**Tabla 40. Datos generales (reporte de V&V)**

| Campo | Contenido |
|---|---|
| Reporte N.º | V&V-01 |
| Fecha y responsable de la ejecución | 13/10/2026 — [completar: ingeniero de pruebas] |
| Versión del código | rama `calidad/pruebas-integracion`, commit `6895746` |
| Entorno de pruebas | Windows 11, Java 21, Maven; JUnit 5 + Mockito, sin MySQL ni GlassFish. Integración: servicios y consultas reales con EclipseLink contra MySQL, sin GlassFish |
| Alcance de la ejecución | Parcial: casos de nivel U (unitarios) y de nivel I (integración). Los de nivel S y A no se ejecutaron |
| Datos de prueba utilizados | Ficticios, armados en `DatosDePrueba` (reloj fijo: lunes 12/10/2026 10:00); en integración, los datos de la demo de `sigrid_schema.sql` en una base aparte |
| Informe de cobertura adjunto | Sí (`SIGRID/target/site/jacoco/index.html`) |

## F.2 Resumen de resultados

**Tabla 41. Resumen de resultados (reporte de V&V)**

| Indicador | Meta | Resultado | ¿Cumple? |
|---|---|---|---|
| Casos ejecutados | 53 de 53 | 40 de 53 | No |
| Casos aprobados, fallidos y no ejecutados | Solo se informa | 39 aprobados, 1 fallidos, 13 no ejecutados | — |
| Casos críticos aprobados | 100 % | 27 de 29 (93 %) | No |
| Cobertura de líneas de los servicios (JaCoCo) | 70 % o más | 88.8 % (2363 de 2662 líneas) | Sí |
| Cobertura de ramas de los servicios (JaCoCo) | Solo se informa | 80.7 % (1757 de 2176 ramas) | — |
| Defectos críticos y altos abiertos | 0 | 0 (ninguno) | Sí |
| Defectos abiertos y cerrados, por severidad | Solo se informa | Abiertos: 1 baja, 1 media. Cerrados: 2 alta, 2 baja | — |
| Tiempo medio de corrección de un defecto (días) | Solo se informa | 1 día (D-1 a D-4: informados el 12/10/2026 y corregidos el 13/10/2026) | — |
| Requerimientos sin caso de prueba | 0 | 0 | Sí |
| Contraste en las pantallas clave (CP-47 y CP-48) | 4,5:1 texto normal; 3:1 texto grande y componentes | No medido | No |
| Exportación a PDF con 500.000 reservas (CP-45) | 6 de 6 | No ejecutado | No |
| Cambios CC-01 a CC-06 verificados | 6 de 6 | 2 de 6 (CC-01, CC-02); implementados con revisión manual pendiente: CC-03, CC-04, CC-05, CC-06 | No |

Pruebas automáticas ejecutadas: **716** (715 pasan, 0 fallan, 1 deshabilitadas). 574 de ellas no corresponden a ningún caso del plan: son pruebas de regresión adicionales sobre las demás reglas del sistema.

### Defectos

La severidad es una propuesta; la asigna el líder de SQA (Tabla 31). Detalle y evidencia en [resultados-pruebas.md](resultados-pruebas.md).

| Código | Caso | Severidad propuesta | Qué pasa | Estado |
|---|---|---|---|---|
| D-1 | CP-07 | Baja | PasswordUtil.verificar lanzaba excepción con un hash mal formado en lugar de devolver false | corregido el 13/10/2026 |
| D-2 | CP-15 | Baja | El comprobante toleraba 5 minutos de adelanto; la regla confirmada es 25 | corregido el 13/10/2026 |
| D-3 | CP-32 | Alta | La renovación por adelantado empezaba el día siguiente al vencimiento, no el mismo día | corregido el 13/10/2026 |
| D-4 | CP-02 | Alta | La contraseña solo exigía 6 caracteres; el plan pide letras, números y un símbolo | corregido el 13/10/2026 |
| D-5 | CP-20 | Media | El rechazo de una solicitud admite no dar motivo; el plan lo exige | diferido por decisión del equipo (13/10/2026): el motivo sigue opcional por ahora |
| D-6 | CP-16 | Baja | Dos cargas del mismo comprobante a la vez: la base deja una sola, pero la segunda recibe un error técnico en lugar del mensaje "ya fue usado" | abierto (encontrado el 13/10/2026 en la prueba de integración) |

## F.3 Resultado por caso de prueba

**Tabla 42. Resultado de cada caso de prueba**

| Caso | Descripción | Nivel | Crítico | Resultado | Incidencia y observaciones |
|---|---|---|---|---|---|
| CP-01 | Registrar un socio interno con datos válidos y legajo | U | No | Aprobado | — |
| CP-02 | Registrar con contraseña sin símbolo especial | U | No | Aprobado | D-4 corregido (13/10/2026): la contraseña exige letras, números y un símbolo; un espacio no cuenta. |
| CP-03 | Registrar un socio interno sin legajo | U | No | Aprobado | — |
| CP-04 | Registrar con un correo o un DNI ya existente | I | No | Aprobado | Prueba de integración contra una base MySQL de prueba (detalle más abajo). |
| CP-05 | Iniciar sesión como socio y como administrador; ruta /admin | S | No | No ejecutado | Falta la prueba de sistema manual (RTF). Evidencia unitaria: 1 prueba(s) del servicio aprobadas (no reemplazan ese nivel). |
| CP-06 | Iniciar sesión con cuenta dada de baja y con membresía vencida | U | No | Aprobado | — |
| CP-07 | Guardar la misma contraseña para dos usuarios (hashes distintos) | U | No | Aprobado | D-1 corregido (13/10/2026): un hash mal formado ya no rompe el login, cuenta como contraseña incorrecta. |
| CP-08 | Un socio ACTIVO reserva un turno libre dentro de los 30 días | U | No | Aprobado | — |
| CP-09 | Un socio NO_ACTIVO intenta reservar | U | No | Aprobado | — |
| CP-10 | Reservar un turno a más de 30 días | U | Sí | Aprobado | — |
| CP-11 | Intentar una cuarta reserva sin comprobante | U | Sí | Aprobado | — |
| CP-12 | Dos socios reservan a la vez el mismo turno y día | I | Sí | Aprobado | Prueba de integración contra una base MySQL de prueba (detalle más abajo). |
| CP-13 | Subir un comprobante PDF, JPG o PNG de hasta 5 MB con fecha válida | U | Sí | Aprobado | — |
| CP-14 | Subir un archivo de más de 5 MB o con extensión falsa | U | Sí | Aprobado | — |
| CP-15 | Subir un comprobante con fecha futura o de más de 7 días | U | Sí | Aprobado | D-2 corregido (13/10/2026): la transferencia se tolera hasta 25 minutos en el futuro. |
| CP-16 | Subir dos veces el mismo archivo | I | Sí | Aprobado | Prueba de integración contra una base MySQL de prueba (detalle más abajo). Observación D-6: si el mismo archivo se sube en dos cargas a la vez, la segunda se rechaza (queda un solo comprobante) pero con un error técnico, no con el mensaje. |
| CP-17 | Pasan 60 minutos sin comprobante y corre la tarea programada | U | Sí | Aprobado | — |
| CP-18 | El socio libera un turno todavía no pagado | U | Sí | Aprobado | — |
| CP-19 | El administrador confirma una reserva con comprobante | U | No | Aprobado | — |
| CP-20 | El administrador rechaza con motivo y luego sin motivo | U | No | Falló | Defecto D-5: prueba deshabilitada con @Disabled. Con motivo se rechaza y queda guardado (aprobado); sin motivo el código lo permite: D-5, diferido por decisión del equipo (13/10/2026), se exigirá más adelante o se corregirá el plan. |
| CP-21 | Un socio envía el identificador de una reserva ajena | U | No | Aprobado | — |
| CP-22 | Cancelar una reserva CONFIRMADA con más de 48 horas (automática, con crédito) | U | Sí | Aprobado | — |
| CP-23 | Cancelar a 47 horas y 59 minutos del turno | U | Sí | Aprobado | — |
| CP-24 | Cancelar exactamente a las 48 horas del turno | U | Sí | Aprobado | — |
| CP-25 | Pedir la reprogramación con crédito vigente y que el administrador la apruebe | U | Sí | Aprobado | — |
| CP-26 | El administrador rechaza la reprogramación | U | Sí | Aprobado | — |
| CP-27 | Reprogramar a otra instalación o enviar una segunda solicitud | U | Sí | Aprobado | Con CC-02, si la instalación se da de baja definitiva el crédito también vale en una equivalente (misma disciplina); en cualquier otro caso (otra instalación, mantenimiento) se rechaza. |
| CP-28 | Un crédito llega a los 30 días sin mantenimiento | U | Sí | Aprobado | — |
| CP-29 | Mantenimiento de más de 30 días con un crédito vigente | U | Sí | Aprobado | — |
| CP-30 | Se reactiva la instalación que estuvo en mantenimiento | U | Sí | Aprobado | Decisión del equipo (13/10/2026): el crédito conserva los días que le quedaban al empezar el mantenimiento y los retoma desde la reactivación real; no vuelve a tener 30 completos. Hay que actualizar el texto del caso en el plan ([A CONFIRMAR] resuelto). |
| CP-31 | El predio cancela una reserva por mantenimiento o clima | U | Sí | Aprobado | — |
| CP-32 | El administrador valida el comprobante de la cuota | U | Sí | Aprobado | D-3 corregido (13/10/2026): la renovación por adelantado empieza el día en que vence la vigente. |
| CP-33 | Corre la tarea horaria con una membresía vencida | U | Sí | Aprobado | La prueba cubre el servicio que usa la tarea horaria (MembresiasServicio.vencerVencidas); la clase MembresiaJob (el temporizador) no se prueba en unitarias. |
| CP-34 | Cargar una tarifa nueva con vigencia futura | U | Sí | Aprobado | — |
| CP-35 | Cargar tarifas fuera de orden o repetidas | U | Sí | Aprobado | — |
| CP-36 | Abrir las pantallas de tarifas del socio y del administrador | S | Sí | No ejecutado | Falta la prueba de sistema manual (RTF). El cambio CC-03 ya está implementado; falta la revisión manual. Evidencia unitaria: 11 prueba(s) del servicio aprobadas (no reemplazan ese nivel). |
| CP-37 | Calificar un turno realizado con puntaje de 1 a 5 y comentario de hasta 500 | U | Sí | Aprobado | — |
| CP-38 | Calificar con puntaje 0 o 6, o con un comentario de 501 caracteres | U | Sí | Aprobado | El plan dice que un comentario de 501 caracteres se rechaza; el equipo decidió (D1) que se recorta a 500. Hay que actualizar el texto del caso. |
| CP-39 | Calificar por segunda vez la misma reserva | U | Sí | Aprobado | — |
| CP-40 | Un turno realizado desde el 25/09/2026 sin calificar | U | Sí | Aprobado | — |
| CP-41 | Revisar menú, títulos, reportes y PDF ("Grado de satisfacción") | S | Sí | No ejecutado | Falta la prueba de sistema manual (RTF). El cambio CC-04 ya está implementado; falta la revisión manual. Evidencia unitaria: 2 prueba(s) del servicio aprobadas (no reemplazan ese nivel). |
| CP-42 | Reporte de ocupación de un día para una instalación | I | No | Aprobado | Prueba de integración contra una base MySQL de prueba (detalle más abajo). La pantalla no ofrece un período de un día ni elegir una instalación (el menor es 7 días y muestra todas): el día suelto se comprobó llamando al servicio. |
| CP-43 | Exportar el PDF de reservas de un solo día | S | No | No ejecutado | Falta la prueba de sistema manual (RTF). El cambio CC-06 ya está implementado; falta la revisión manual. Evidencia unitaria: 5 prueba(s) del servicio aprobadas (no reemplazan ese nivel). |
| CP-44 | Exportar el PDF de un período sin reservas | S | No | No ejecutado | Falta la prueba de sistema manual (RTF). El cambio CC-06 ya está implementado; falta la revisión manual. Evidencia unitaria: 1 prueba(s) del servicio aprobadas (no reemplazan ese nivel). |
| CP-45 | Con 500.000 reservas, listar y exportar un período acotado | A | No | No ejecutado | Falta la medición de accesibilidad o rendimiento. El cambio CC-06 ya está implementado; falta la revisión manual. Evidencia unitaria: 3 prueba(s) del servicio aprobadas (no reemplazan ese nivel). Medido aparte con 500.403 reservas (docs/calidad/cp45/README.md): el listado, el buscador y el PDF de un período acotado responden en menos de 3 s y con unos 10 MB de memoria; el código anterior no termina (OutOfMemoryError con 1 GB). Los reportes de 12 meses tardan 6,7 s y usan más de 500 MB. |
| CP-46 | Exportar un PDF de varias páginas | S | No | No ejecutado | Falta la prueba de sistema manual (RTF). El cambio CC-06 ya está implementado; falta la revisión manual. |
| CP-47 | Medir el contraste de las pantallas del socio | A | No | No ejecutado | Falta la medición de accesibilidad o rendimiento. El cambio CC-05 ya está implementado; falta la revisión manual. Evidencia unitaria: 2 prueba(s) del servicio aprobadas (no reemplazan ese nivel). |
| CP-48 | Medir el contraste de las pantallas del administrador | A | No | No ejecutado | Falta la medición de accesibilidad o rendimiento. El cambio CC-05 ya está implementado; falta la revisión manual. Evidencia unitaria: 6 prueba(s) del servicio aprobadas (no reemplazan ese nivel). |
| CP-49 | Revisar los estados de reserva sin depender solo del color | S | No | No ejecutado | Falta la prueba de sistema manual (RTF). El cambio CC-05 ya está implementado; falta la revisión manual. Evidencia unitaria: 2 prueba(s) del servicio aprobadas (no reemplazan ese nivel). |
| CP-50 | Abrir las pantallas principales en 360, 768 y 1280 px | A | No | No ejecutado | Falta la medición de accesibilidad o rendimiento. |
| CP-51 | El socio abre su historial de reservas | S | No | No ejecutado | Falta la prueba de sistema manual (RTF). Evidencia unitaria: 1 prueba(s) del servicio aprobadas (no reemplazan ese nivel). |
| CP-52 | El administrador filtra el listado de reservas | I | No | Aprobado | Prueba de integración contra una base MySQL de prueba (detalle más abajo). La pantalla filtra la instalación por disciplina o por nombre en el buscador; no hay un desplegable de instalaciones. |
| CP-53 | Revisar el diseño visual en RTF-03 | S | No | No ejecutado | Falta la prueba de sistema manual (RTF). |

### Pruebas de integración (nivel I)

Pruebas de integración de SIGRID. Ejecutadas el 13/10/2026 23:42 contra la base de prueba sigrid_integ (MySQL 8.0.36). No son parte de `mvn test`: se corren aparte con `docs/calidad/integracion/PruebasIntegracion.java` (cómo repetirlas, en su [README](integracion/README.md)).

| Caso | Resultado | Qué se comprobó |
|---|---|---|
| CP-04 | Aprobado | Correo repetido (también en mayúsculas y con espacios) y DNI repetido: el servicio los rechaza con su mensaje y no se crea ninguna fila; si algo se saltea el servicio, la base rechaza el INSERT (uq_usuario_email y uq_socio_dni); con datos nuevos la misma alta se crea. |
| CP-12 | Aprobado | Paso a paso: el segundo pedido pasa la consulta previa, queda esperando en el INSERT y, cuando el primero confirma, recibe "Ese turno acaba de ser reservado por otra persona. Elegí otro."; queda una sola reserva viva. A la vez (20 rondas, dos conexiones): siempre una sola reserva y el otro socio recibe ese mensaje (ganó 4 veces uno y 16 el otro). |
| CP-16 | Aprobado | Primera carga: pago y comprobante creados y archivo guardado. El mismo archivo en otra reserva (del mismo socio y de otro socio): "Ese comprobante ya fue usado en otra operación.", sin pago ni comprobante nuevos. Dos cargas a la vez del mismo archivo: queda una sola (uq_comprobante_hash); la otra responde <ERROR RollbackException>. |
| CP-42 | Aprobado | 84 comparaciones (7 instalaciones por los 6 períodos de la pantalla y 6 días sueltos): los turnos reservados y disponibles de cada instalación y el total coinciden con lo contado por SQL (1355 turnos ocupados sumando todos los períodos). La pantalla no ofrece un período de un día (el menor es 7 días): el día suelto se comprobó llamando al servicio. |
| CP-52 | Aprobado | 468 combinaciones de estado (6), período (6) e instalación (5 disciplinas del desplegable y 7 nombres en el buscador) sobre 403 reservas: el listado trae exactamente las mismas reservas que la consulta SQL directa, sin repetidas, y el total informado coincide (206 combinaciones con resultados). La pantalla filtra la instalación por disciplina o escribiendo el nombre en el buscador: no hay un desplegable de instalaciones. |

## Matriz de trazabilidad con resultados (Tabla 30)

Un requerimiento figura como *Aprobado* si todos sus casos lo están.

| Requerimiento | Casos | Resultado |
|---|---|---|
| RF-01 Gestión de socios | CP-01, CP-03, CP-04, CP-06, CP-32, CP-33 | Aprobado |
| RF-02 Seguimiento del uso | CP-51, CP-52 | Parcial: 1 de 2 aprobados |
| RF-03 Pagos y cobros | CP-13, CP-14, CP-15, CP-16, CP-19, CP-34, CP-35 | Aprobado |
| RF-04 Reservas | CP-08, CP-09, CP-10, CP-11, CP-17, CP-18, CP-27, CP-28 | Aprobado |
| RF-05 Administración de reservas e instalaciones | CP-19, CP-20, CP-31 | Falló (CP-20) |
| RF-06 Reportes | CP-42 | Aprobado |
| RF-07 Grado de satisfacción | CP-37, CP-38, CP-39, CP-40 | Aprobado |
| RNF-01 Diseño adaptable | CP-50 | Parcial: 0 de 1 aprobados |
| RNF-02 Estilo gráfico actual | CP-53 | Parcial: 0 de 1 aprobados |
| RNF-03 Contraseñas | CP-02, CP-07 | Aprobado |
| RNF-04 Mensajes de error | CP-02 | Aprobado |
| RNF-05 Seguridad y acceso | CP-05, CP-14, CP-21 | Parcial: 2 de 3 aprobados |
| RNF-06 Integridad y concurrencia | CP-12 | Aprobado |
| RNF-07 Rendimiento de listados y PDF | CP-45 | Parcial: 0 de 1 aprobados |
| RNF-08 Accesibilidad visual | CP-47, CP-48, CP-49 | Parcial: 0 de 3 aprobados |
| CC-01 Cancelación automática | CP-22, CP-23, CP-24, CP-25, CP-26 | Aprobado |
| CC-02 Crédito y mantenimiento | CP-29, CP-30 | Aprobado |
| CC-03 Vigencia de tarifas | CP-36 | Parcial: 0 de 1 aprobados |
| CC-04 Grado de satisfacción (nombre) | CP-41 | Parcial: 0 de 1 aprobados |
| CC-05 Colores y contrastes | CP-47, CP-48, CP-49 | Parcial: 0 de 3 aprobados |
| CC-06 PDF con filtros de período | CP-43, CP-44, CP-45, CP-46 | Parcial: 0 de 4 aprobados |

## Anexo: pruebas automáticas de cada caso

| Caso | Clase | Prueba | Resultado |
|---|---|---|---|
| CP-01 | `RegistroServicioTest` | el socio nuevo queda NO_ACTIVO | pasa |
| CP-01 | `RegistroServicioTest` | registra usuario, socio y carnet (inactivo) de un alumno con legajo | pasa |
| CP-02 | `RegistroServicioTest` | rechaza una contraseña sin letras, sin números o sin símbolo[1] Clave2026 | pasa |
| CP-02 | `RegistroServicioTest` | rechaza una contraseña sin letras, sin números o sin símbolo[2] Clave#abcd | pasa |
| CP-02 | `RegistroServicioTest` | rechaza una contraseña sin letras, sin números o sin símbolo[3] 123#456 | pasa |
| CP-02 | `RegistroServicioTest` | rechaza una contraseña sin letras, sin números o sin símbolo[4] Clave 2026 | pasa |
| CP-02 | `RegistroServicioTest` | rechaza una contraseña sin letras, sin números o sin símbolo[5] 123456 | pasa |
| CP-03 | `RegistroServicioTest` | rechaza un socio interno (alumno) sin legajo | pasa |
| CP-04 | `RegistroServicioTest` | rechaza un DNI repetido y no crea nada (regla del servicio) | pasa |
| CP-04 | `RegistroServicioTest` | rechaza un correo que ya tiene cuenta (regla del servicio) | pasa |
| CP-05 | `UsuarioServicioTest` | autentica con correo y contraseña correctos (regla del servicio) | pasa |
| CP-06 | `UsuarioServicioTest` | la cuenta dada de baja no ingresa aunque la contraseña sea correcta | pasa |
| CP-06 | `UsuarioServicioTest` | una cuenta habilitada ingresa sin que el login mire la membresía | pasa |
| CP-07 | `PasswordUtilTest` | el valor guardado es PBKDF2 con tres partes, no texto plano | pasa |
| CP-07 | `PasswordUtilTest` | la misma contraseña genera hashes distintos (sal propia) | pasa |
| CP-07 | `PasswordUtilTest` | un hash mal formado no debería romper la verificación (D-1) | pasa |
| CP-08 | `PanelSocioServicioReservaTest` | un socio ACTIVO reserva un turno libre: PENDIENTE_PAGO con la tarifa de su categoría | pasa |
| CP-09 | `PanelSocioServicioReservaTest` | un socio NO_ACTIVO no puede reservar | pasa |
| CP-09 | `PanelSocioServicioReservaTest` | un socio dado de baja no puede reservar | pasa |
| CP-10 | `PanelSocioServicioReservaTest` | acepta reservar dentro de los 30 días (límite) | pasa |
| CP-10 | `PanelSocioServicioReservaTest` | rechaza reservar a más de 30 días | pasa |
| CP-11 | `PanelSocioServicioReservaTest` | con dos reservas sin comprobante todavía puede reservar | pasa |
| CP-11 | `PanelSocioServicioReservaTest` | la cuarta reserva sin comprobante se rechaza (máximo 3) | pasa |
| CP-12 | `PanelSocioServicioReservaTest` | el choque con la restricción UNIQUE se informa al socio (regla del servicio) | pasa |
| CP-12 | `PanelSocioServicioReservaTest` | un turno ya tomado por otro socio se rechaza (regla del servicio) | pasa |
| CP-13 | `PanelSocioServicioComprobanteTest` | acepta un archivo de exactamente 5 MB | pasa |
| CP-13 | `PanelSocioServicioComprobanteTest` | guarda un PDF válido y deja el pago PENDIENTE_VALIDACION | pasa |
| CP-13 | `PanelSocioServicioComprobanteTest` | guarda un PNG con extensión png | pasa |
| CP-14 | `ComprobanteArchivosTest` | el tipo sale de los bytes y no de la extensión | pasa |
| CP-14 | `ComprobanteArchivosTest` | rechaza un ejecutable renombrado como PDF | pasa |
| CP-14 | `PanelSocioServicioComprobanteTest` | rechaza un archivo cuyo contenido no es imagen ni PDF | pasa |
| CP-14 | `PanelSocioServicioComprobanteTest` | rechaza un archivo de 5 MB más un byte | pasa |
| CP-15 | `PanelSocioServicioComprobanteTest` | acepta una transferencia adelantada hasta 25 minutos (D-2) | pasa |
| CP-15 | `PanelSocioServicioComprobanteTest` | acepta una transferencia de exactamente 7 días | pasa |
| CP-15 | `PanelSocioServicioComprobanteTest` | rechaza una transferencia adelantada más de 25 minutos | pasa |
| CP-15 | `PanelSocioServicioComprobanteTest` | rechaza una transferencia con fecha futura | pasa |
| CP-15 | `PanelSocioServicioComprobanteTest` | rechaza una transferencia de más de 7 días | pasa |
| CP-16 | `PanelSocioServicioComprobanteTest` | rechaza un archivo repetido (regla del servicio) | pasa |
| CP-17 | `PanelSocioServicioReservaTest` | a los 60 minutos sin comprobante la reserva queda CANCELADA | pasa |
| CP-17 | `PanelSocioServicioReservaTest` | no libera la reserva que justo recibió su comprobante | pasa |
| CP-18 | `PanelSocioServicioReservaTest` | el socio libera un turno todavía no pagado | pasa |
| CP-19 | `ReservaServicioTest` | el administrador confirma: reserva y pago CONFIRMADOS con administrador y fecha | pasa |
| CP-20 | `ReservaServicioTest` | rechazar con motivo guarda el motivo | pasa |
| CP-20 | `ReservaServicioTest` | rechazar deja la reserva RECHAZADA y el turno libre | pasa |
| CP-20 | `ReservaServicioTest` | rechazar sin motivo no se permite | deshabilitada |
| CP-21 | `CambiosReservaServicioTest` | la reserva de otro socio no se cancela | pasa |
| CP-21 | `CambiosReservaServicioTest` | no pide cambios sobre la reserva de otro socio | pasa |
| CP-21 | `PanelSocioServicioComprobanteTest` | no sube un comprobante a la reserva de otro socio | pasa |
| CP-21 | `PanelSocioServicioReservaTest` | no libera la reserva de otro socio | pasa |
| CP-21 | `PanelSocioServicioValoracionTest` | no valora la reserva de otro socio | pasa |
| CP-22 | `CambiosReservaServicioTest` | cancelar con más de 48 h cancela en el acto y deja un crédito de 30 días | pasa |
| CP-22 | `CambiosReservaServicioTest` | la cancelación automática no deja ningún pedido para el administrador | pasa |
| CP-22 | `CambiosReservaServicioTest` | una reserva ya cancelada no se vuelve a cancelar ni suma otro crédito | pasa |
| CP-23 | `CambiosReservaServicioTest` | a 47 h 59 min no se cancela: la reserva sigue CONFIRMADA y sin crédito | pasa |
| CP-23 | `CambiosReservaServicioTest` | rechaza cancelar a 47 h 59 min del turno | pasa |
| CP-23 | `CambiosReservaServicioTest` | rechaza cancelar con menos de 48 h | pasa |
| CP-24 | `CambiosReservaServicioTest` | a exactamente 48 h se cancela con crédito | pasa |
| CP-25 | `CambiosReservaServicioTest` | al aprobar, la reserva vuelve a CONFIRMADA en el turno nuevo | pasa |
| CP-25 | `CambiosReservaServicioTest` | cancelada automáticamente, el socio reprograma con el crédito y el administrador lo aprueba | pasa |
| CP-25 | `CambiosReservaServicioTest` | pide reprogramar con crédito vigente: queda PENDIENTE | pasa |
| CP-26 | `CambiosReservaServicioTest` | al rechazar una reprogramación el crédito sigue vigente y se puede volver a pedir | pasa |
| CP-27 | `CambiosReservaServicioTest` | rechaza reprogramar a otra instalación | pasa |
| CP-27 | `CambiosReservaServicioTest` | rechaza un segundo pedido para la misma reserva | pasa |
| CP-28 | `CambiosReservaServicioTest` | el crédito todavía se usa el último día | pasa |
| CP-28 | `CambiosReservaServicioTest` | sin mantenimiento el crédito sigue venciendo en la fecha guardada | pasa |
| CP-28 | `CambiosReservaServicioTest` | un crédito vencido ayer ya no se puede usar | pasa |
| CP-29 | `CambiosReservaServicioListadosTest` | el administrador ve en pausa el crédito de una instalación en mantenimiento | pasa |
| CP-29 | `CambiosReservaServicioTest` | cancelar una reserva durante el mantenimiento deja el crédito en pausa desde el primer día | pasa |
| CP-29 | `CambiosReservaServicioTest` | durante el mantenimiento al crédito le quedan siempre los mismos días | pasa |
| CP-29 | `CambiosReservaServicioTest` | un crédito no vence mientras la instalación está en mantenimiento, aunque pasen más de 30 días | pasa |
| CP-29 | `CambiosReservaServicioTest` | un crédito que ya había vencido antes del mantenimiento sigue vencido | pasa |
| CP-29 | `PanelSocioServicioMisReservasTest` | con la instalación en mantenimiento el crédito queda en pausa y el socio lo ve | pasa |
| CP-29 | `ReportesServicioTest` | el crédito de una instalación en mantenimiento no cuenta como dinero perdido | pasa |
| CP-29 | `ReservaServicioListadosTest` | la cancelada de una instalación en mantenimiento sigue diciendo con crédito | pasa |
| CP-30 | `InstalacionesServicioTest` | al reactivar, el crédito conserva los días que le quedaban, contados desde la reactivación real | pasa |
| CP-30 | `InstalacionesServicioTest` | reactivar el mismo día que empezó el mantenimiento no cambia el plazo | pasa |
| CP-30 | `InstalacionesServicioTest` | un crédito que ya había vencido antes del mantenimiento no revive al reactivar | pasa |
| CP-31 | `CambiosReservaServicioTest` | cancelación por el predio: CANCELADA con crédito de 30 días | pasa |
| CP-31 | `InstalacionesServicioTest` | al poner en mantenimiento, las confirmadas se cancelan por el predio | pasa |
| CP-32 | `MembresiasServicioTest` | al validar, el socio queda ACTIVO y su carnet activo | pasa |
| CP-32 | `MembresiasServicioTest` | la renovación por adelantado empieza el día del vencimiento (D-3) | pasa |
| CP-32 | `MembresiasServicioTest` | la suscripción dura un mes desde la validación | pasa |
| CP-32 | `MembresiasServicioTest` | pagando el día del vencimiento la nueva empieza ese día (D-3) | pasa |
| CP-33 | `MembresiasServicioTest` | al vencer, suscripción VENCIDA, socio NO_ACTIVO y carnet inactivo | pasa |
| CP-34 | `TarifasServicioTest` | hasta la fecha futura sigue rigiendo la tarifa anterior | pasa |
| CP-34 | `TarifasServicioTest` | la tarifa anterior rige hasta el día previo | pasa |
| CP-34 | `TarifasServicioTest` | nunca se edita la tarifa anterior | pasa |
| CP-34 | `TarifasServicioTest` | una tarifa nueva se agrega con su vigencia futura | pasa |
| CP-35 | `TarifasServicioTest` | rechaza alumno igual a externo | pasa |
| CP-35 | `TarifasServicioTest` | rechaza tarifas fuera de orden (alumno más que externo) | pasa |
| CP-35 | `TarifasServicioTest` | rechaza una tarifa repetida para la misma fecha | pasa |
| CP-36 | `InstalacionesServicioTest` | cada precio muestra desde cuándo rige y el próximo cambio programado | pasa |
| CP-36 | `InstalacionesServicioTest` | un precio que todavía no rige se ve como próximo cambio | pasa |
| CP-36 | `MembresiasServicioListadosTest` | cada cuota muestra desde cuándo rige y el próximo cambio programado | pasa |
| CP-36 | `PanelSocioServicioConsultasTest` | la cuota del socio muestra desde cuándo rige y el próximo cambio | pasa |
| CP-36 | `PanelSocioServicioConsultasTest` | la tarifa del socio muestra desde cuándo rige y el próximo cambio | pasa |
| CP-36 | `PanelSocioServicioConsultasTest` | sin cambio programado la tarifa del socio lo dice | pasa |
| CP-36 | `TarifasServicioTest` | el historial marca el cambio futuro como programado | pasa |
| CP-36 | `TarifasServicioTest` | el precio vigente dice desde cuándo rige | pasa |
| CP-36 | `TarifasServicioTest` | el próximo cambio dice el precio y desde cuándo | pasa |
| CP-36 | `TarifasServicioTest` | sin cambio programado lo dice | pasa |
| CP-36 | `TarifasServicioTest` | sin precio vigente no hay desde cuándo | pasa |
| CP-37 | `PanelSocioServicioValoracionTest` | acepta los puntajes de 1 a 5[1] 1 | pasa |
| CP-37 | `PanelSocioServicioValoracionTest` | acepta los puntajes de 1 a 5[2] 2 | pasa |
| CP-37 | `PanelSocioServicioValoracionTest` | acepta los puntajes de 1 a 5[3] 3 | pasa |
| CP-37 | `PanelSocioServicioValoracionTest` | acepta los puntajes de 1 a 5[4] 4 | pasa |
| CP-37 | `PanelSocioServicioValoracionTest` | acepta los puntajes de 1 a 5[5] 5 | pasa |
| CP-37 | `PanelSocioServicioValoracionTest` | guarda puntaje, comentario y fecha | pasa |
| CP-37 | `PanelSocioServicioValoracionTest` | guarda un comentario de exactamente 500 caracteres | pasa |
| CP-38 | `PanelSocioServicioValoracionTest` | rechaza puntajes 0 y 6[1] -1 | pasa |
| CP-38 | `PanelSocioServicioValoracionTest` | rechaza puntajes 0 y 6[2] 0 | pasa |
| CP-38 | `PanelSocioServicioValoracionTest` | rechaza puntajes 0 y 6[3] 6 | pasa |
| CP-38 | `PanelSocioServicioValoracionTest` | rechaza puntajes 0 y 6[4] 10 | pasa |
| CP-38 | `PanelSocioServicioValoracionTest` | un comentario de más de 500 se recorta a 500 (decisión D1) | pasa |
| CP-39 | `PanelSocioServicioValoracionTest` | rechaza si la base detecta que ya existía | pasa |
| CP-39 | `PanelSocioServicioValoracionTest` | rechaza una segunda valoración de la misma reserva | pasa |
| CP-40 | `PanelSocioServicioReservaTest` | con un turno sin valorar no puede reservar | pasa |
| CP-40 | `PanelSocioServicioReservaTest` | los turnos anteriores al 25/09/2026 no se exigen | pasa |
| CP-40 | `PanelSocioServicioReservaTest` | un turno ya valorado no bloquea | pasa |
| CP-41 | `AdminNavBeanTest` | el menú dice "Grado de satisfacción" y no "Valoraciones" | pasa |
| CP-41 | `ReportesServicioTest` | el resumen de reportes dice "Grado de satisfacción" | pasa |
| CP-42 | `ReportesServicioTest` | la ocupación es reservados sobre disponibles (regla del servicio) | pasa |
| CP-43 | `ReservaServicioFiltrosTest` | el informe de un solo día trae esas reservas con período y fecha de emisión | pasa |
| CP-43 | `ReservaServicioFiltrosTest` | el informe dice qué filtros se aplicaron | pasa |
| CP-43 | `ReservaServicioFiltrosTest` | el período de varios días dice desde y hasta | pasa |
| CP-43 | `ReservaServicioFiltrosTest` | sin período no se exporta: hay que elegirlo antes | pasa |
| CP-43 | `ReservaServicioFiltrosTest` | un período con la fecha desde posterior a la hasta se rechaza | pasa |
| CP-44 | `ReservaServicioFiltrosTest` | un período sin reservas avisa y no arma un documento vacío | pasa |
| CP-45 | `ReportesServicioCargaTest` | la carga pide a la base solo el período y el previo, no toda la tabla de reservas | pasa |
| CP-45 | `ReservaServicioFiltrosTest` | exactamente el máximo todavía entra en un PDF | pasa |
| CP-45 | `ReservaServicioFiltrosTest` | si el período trae más de lo que entra en un PDF pide acotarlo sin cargar las filas | pasa |
| CP-47 | `ContrasteCssTest` | el foco del teclado se ve sobre el fondo claro (3:1) en el panel del socio y del administrador | pasa |
| CP-47 | `ContrasteCssTest` | el texto de las pantallas del socio llega a 4,5:1 sobre las superficies claras | pasa |
| CP-48 | `ContrasteCssTest` | el texto de las pantallas del administrador llega a 4,5:1 sobre las superficies claras | pasa |
| CP-48 | `ContrasteCssTest` | en las barras apiladas reservas y membresías se distinguen entre sí y del fondo (3:1) | pasa |
| CP-48 | `ContrasteCssTest` | la leyenda usa los mismos colores que las barras | pasa |
| CP-48 | `ContrasteCssTest` | los cinco tramos de la dona se ven sobre el fondo (3:1) | pasa |
| CP-48 | `ContrasteCssTest` | los indicadores de bueno, atención y malo se ven sobre el fondo (3:1) | pasa |
| CP-48 | `ContrasteCssTest` | los números del mapa de calor se leen sobre su celda (4,5:1) | pasa |
| CP-49 | `ContrasteCssTest` | la estrella llena y la vacía se distinguen por la forma y el color llega a 3:1 | pasa |
| CP-49 | `ContrasteCssTest` | la serie de membresías lleva una división propia y no depende solo del color | pasa |
| CP-51 | `PanelSocioServicioMisReservasTest` | el socio ve cada reserva con su estado (regla del servicio) | pasa |
