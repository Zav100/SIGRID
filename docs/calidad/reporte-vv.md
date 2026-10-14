# Reporte de verificación y validación (V&V) — SIGRID

Generado automáticamente por `docs/calidad/generar_reporte_vv.py` con la plantilla del Apéndice F del plan de garantía de calidad. Para regenerarlo: `mvn test` dentro de `SIGRID/` y luego `python docs/calidad/generar_reporte_vv.py` desde la raíz del repositorio.

## F.1 Datos generales

**Tabla 40. Datos generales (reporte de V&V)**

| Campo | Contenido |
|---|---|
| Reporte N.º | V&V-01 |
| Fecha y responsable de la ejecución | 14/10/2026 — [completar: ingeniero de pruebas] |
| Versión del código | rama `calidad/pruebas-de-sistema`, commit `5ea5ab5` |
| Entorno de pruebas | Windows 11, Java 21, Maven; JUnit 5 + Mockito, sin MySQL ni GlassFish. Integración: servicios y consultas reales con EclipseLink contra MySQL, sin GlassFish. Sistema: SIGRID desplegado en GlassFish 7 con una base MySQL de prueba, recorrido con Edge sin ventana |
| Alcance de la ejecución | Niveles U, I, S, A. No ejecutados: CP-53 |
| Datos de prueba utilizados | Ficticios, armados en `DatosDePrueba` (reloj fijo: lunes 12/10/2026 10:00); en integración y sistema, los datos de la demo de `sigrid_schema.sql` en una base aparte, y para CP-45 500.403 reservas generadas |
| Informe de cobertura adjunto | Sí (`SIGRID/target/site/jacoco/index.html`) |

## F.2 Resumen de resultados

**Tabla 41. Resumen de resultados (reporte de V&V)**

| Indicador | Meta | Resultado | ¿Cumple? |
|---|---|---|---|
| Casos ejecutados | 53 de 53 | 52 de 53 | No |
| Casos aprobados, fallidos y no ejecutados | Solo se informa | 47 aprobados, 5 fallidos, 1 no ejecutados | — |
| Casos críticos aprobados | 100 % | 29 de 29 (100 %) | Sí |
| Cobertura de líneas de los servicios (JaCoCo) | 70 % o más | 88.8 % (2363 de 2662 líneas) | Sí |
| Cobertura de ramas de los servicios (JaCoCo) | Solo se informa | 80.7 % (1757 de 2176 ramas) | — |
| Defectos críticos y altos abiertos | 0 | 0 (ninguno) | Sí |
| Defectos abiertos y cerrados, por severidad | Solo se informa | Abiertos: 4 baja, 5 media. Cerrados: 2 alta, 2 baja | — |
| Tiempo medio de corrección de un defecto (días) | Solo se informa | 1 día (D-1 a D-4: informados el 12/10/2026 y corregidos el 13/10/2026) | — |
| Requerimientos sin caso de prueba | 0 | 0 | Sí |
| Contraste en las pantallas clave (CP-47 y CP-48) | 4,5:1 texto normal; 3:1 texto grande y componentes | Medido en las pantallas desplegadas: socio 3 combinaciones por debajo, administrador 7 combinaciones por debajo (D-10, D-11) | No |
| Exportación a PDF con 500.000 reservas (CP-45) | 5 s o menos (Tabla 3, [A CONFIRMAR]) | Con 500.403 reservas, listado y PDF de un período acotado en 2,1 s o menos, sin agotar la memoria | Sí |
| Cambios CC-01 a CC-06 verificados | 6 de 6 | 5 de 6 (CC-01, CC-02, CC-03, CC-04, CC-06); con casos fallidos: CC-05 | No |

Pruebas automáticas ejecutadas: **716** (715 pasan, 0 fallan, 1 deshabilitadas). 574 de ellas no corresponden a ningún caso del plan: son pruebas de regresión adicionales sobre las demás reglas del sistema.

### Defectos

La severidad es una propuesta; la asigna el líder de SQA (Tabla 31). Cada defecto está registrado con la plantilla del Apéndice G en [incidencias.md](incidencias.md).

| Código | Incidencia | Caso | Severidad propuesta | Qué pasa | Estado |
|---|---|---|---|---|---|
| D-1 | [INC-01](incidencias.md#inc-01) | CP-07 | Baja | PasswordUtil.verificar lanzaba excepción con un hash mal formado en lugar de devolver false | corregido el 13/10/2026 |
| D-2 | [INC-02](incidencias.md#inc-02) | CP-15 | Baja | El comprobante toleraba 5 minutos de adelanto; la regla confirmada es 25 | corregido el 13/10/2026 |
| D-3 | [INC-03](incidencias.md#inc-03) | CP-32 | Alta | La renovación por adelantado empezaba el día siguiente al vencimiento, no el mismo día | corregido el 13/10/2026 |
| D-4 | [INC-04](incidencias.md#inc-04) | CP-02 | Alta | La contraseña solo exigía 6 caracteres; el plan pide letras, números y un símbolo | corregido el 13/10/2026 |
| D-5 | [INC-05](incidencias.md#inc-05) | CP-20 | Media | El rechazo de una solicitud admite no dar motivo; el plan lo exige | diferido por decisión del equipo (13/10/2026): el motivo sigue opcional por ahora |
| D-6 | [INC-06](incidencias.md#inc-06) | CP-16 | Baja | Dos cargas del mismo comprobante a la vez: la base deja una sola, pero la segunda recibe un error técnico en lugar del mensaje "ya fue usado" | abierto (encontrado el 13/10/2026 en la prueba de integración) |
| D-7 | [INC-07](incidencias.md#inc-07) | CP-49 | Media | En la grilla del calendario del administrador el estado de cada reserva se distingue solo por el color | abierto (encontrado el 14/10/2026 en la prueba de sistema) |
| D-8 | [INC-08](incidencias.md#inc-08) | CP-50 | Media | A 360 px el panel del administrador se desborda 27 px (el botón de salir queda cortado) y "Mis reservas" del socio también | abierto (14/10/2026) |
| D-9 | [INC-09](incidencias.md#inc-09) | CP-50 | Baja | A 1280 px tres tablas del administrador no entran en su tarjeta y esconden sus últimas columnas | abierto (14/10/2026) |
| D-10 | [INC-10](incidencias.md#inc-10) | CP-47 | Media | Contraste insuficiente en el carnet digital (3,85:1 y 4,20:1) y en el contorno del campo de comentario del socio | abierto (14/10/2026) |
| D-11 | [INC-11](incidencias.md#inc-11) | CP-48 | Media | Contraste insuficiente en el contorno de los filtros (1,2:1 a 1,4:1) y en los días de otros meses de los calendarios (2,0:1 a 3,0:1) | abierto (14/10/2026) |
| D-12 | [INC-12](incidencias.md#inc-12) | CP-46 | Baja | En el PDF de reservas el horario y el monto se parten en dos renglones | abierto (14/10/2026) |
| D-13 | [INC-13](incidencias.md#inc-13) | CP-51 | Baja | "Mis reservas" del socio repite la fecha del turno | abierto (14/10/2026) |

## F.3 Resultado por caso de prueba

**Tabla 42. Resultado de cada caso de prueba**

| Caso | Descripción | Nivel | Crítico | Resultado | Incidencia y observaciones |
|---|---|---|---|---|---|
| CP-01 | Registrar un socio interno con datos válidos y legajo | U | No | Aprobado | — |
| CP-02 | Registrar con contraseña sin símbolo especial | U | No | Aprobado | D-4 corregido (13/10/2026): la contraseña exige letras, números y un símbolo; un espacio no cuenta. |
| CP-03 | Registrar un socio interno sin legajo | U | No | Aprobado | — |
| CP-04 | Registrar con un correo o un DNI ya existente | I | No | Aprobado | Prueba de integración contra una base MySQL de prueba (detalle más abajo). |
| CP-05 | Iniciar sesión como socio y como administrador; ruta /admin | S | No | Aprobado | Prueba sobre la aplicación desplegada (detalle más abajo). |
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
| CP-36 | Abrir las pantallas de tarifas del socio y del administrador | S | Sí | Aprobado | Prueba sobre la aplicación desplegada (detalle más abajo). |
| CP-37 | Calificar un turno realizado con puntaje de 1 a 5 y comentario de hasta 500 | U | Sí | Aprobado | — |
| CP-38 | Calificar con puntaje 0 o 6, o con un comentario de 501 caracteres | U | Sí | Aprobado | El plan dice que un comentario de 501 caracteres se rechaza; el equipo decidió (D1) que se recorta a 500. Hay que actualizar el texto del caso. |
| CP-39 | Calificar por segunda vez la misma reserva | U | Sí | Aprobado | — |
| CP-40 | Un turno realizado desde el 25/09/2026 sin calificar | U | Sí | Aprobado | — |
| CP-41 | Revisar menú, títulos, reportes y PDF ("Grado de satisfacción") | S | Sí | Aprobado | Prueba sobre la aplicación desplegada (detalle más abajo). El panel del socio sigue diciendo "valoración" (el cambio se aplicó solo al administrador hasta que el equipo decida). |
| CP-42 | Reporte de ocupación de un día para una instalación | I | No | Aprobado | Prueba de integración contra una base MySQL de prueba (detalle más abajo). La pantalla no ofrece un período de un día ni elegir una instalación (el menor es 7 días y muestra todas): el día suelto se comprobó llamando al servicio. |
| CP-43 | Exportar el PDF de reservas de un solo día | S | No | Aprobado | Prueba sobre la aplicación desplegada (detalle más abajo). |
| CP-44 | Exportar el PDF de un período sin reservas | S | No | Aprobado | Prueba sobre la aplicación desplegada (detalle más abajo). |
| CP-45 | Con 500.000 reservas, listar y exportar un período acotado | A | No | Aprobado | Prueba sobre la aplicación desplegada (detalle más abajo). La meta de la Tabla 3 (5 s) sigue "[A CONFIRMAR]" en el plan. Reportes de 12 meses con 500.000 reservas: 10,3 s (fuera del caso, pero pasa la meta de RNF-07). |
| CP-46 | Exportar un PDF de varias páginas | S | No | Aprobado | Prueba sobre la aplicación desplegada (detalle más abajo). Observación D-12: en hoja A4 el horario y el monto se parten en dos renglones. |
| CP-47 | Medir el contraste de las pantallas del socio | A | No | Falló | Prueba sobre la aplicación desplegada (detalle más abajo). Defecto D-10 (ver incidencias.md). |
| CP-48 | Medir el contraste de las pantallas del administrador | A | No | Falló | Prueba sobre la aplicación desplegada (detalle más abajo). Defecto D-11 (ver incidencias.md). |
| CP-49 | Revisar los estados de reserva sin depender solo del color | S | No | Falló | Prueba sobre la aplicación desplegada (detalle más abajo). Defecto D-7 (ver incidencias.md). |
| CP-50 | Abrir las pantallas principales en 360, 768 y 1280 px | A | No | Falló | Prueba sobre la aplicación desplegada (detalle más abajo). Defecto D-8 y D-9 (ver incidencias.md). |
| CP-51 | El socio abre su historial de reservas | S | No | Aprobado | Prueba sobre la aplicación desplegada (detalle más abajo). Observación D-13: "Mis reservas" repite la fecha del turno. |
| CP-52 | El administrador filtra el listado de reservas | I | No | Aprobado | Prueba de integración contra una base MySQL de prueba (detalle más abajo). La pantalla filtra la instalación por disciplina o por nombre en el buscador; no hay un desplegable de instalaciones. |
| CP-53 | Revisar el diseño visual en RTF-03 | S | No | No ejecutado | Falta la prueba de sistema manual (RTF). Lo aprueban la cátedra y el equipo en la RTF-03: informe preparado con los hallazgos en [rtf-03-diseno-visual.md](rtf-03-diseno-visual.md) y capturas de todas las pantallas en sistema/evidencia/cp50. |

### Pruebas de integración (nivel I)

Pruebas de integración de SIGRID. Ejecutadas el 13/10/2026 23:42 contra la base de prueba sigrid_integ (MySQL 8.0.36). No son parte de `mvn test`: se corren aparte con `docs/calidad/integracion/PruebasIntegracion.java` (cómo repetirlas, en su [README](integracion/README.md)).

| Caso | Resultado | Qué se comprobó |
|---|---|---|
| CP-04 | Aprobado | Correo repetido (también en mayúsculas y con espacios) y DNI repetido: el servicio los rechaza con su mensaje y no se crea ninguna fila; si algo se saltea el servicio, la base rechaza el INSERT (uq_usuario_email y uq_socio_dni); con datos nuevos la misma alta se crea. |
| CP-12 | Aprobado | Paso a paso: el segundo pedido pasa la consulta previa, queda esperando en el INSERT y, cuando el primero confirma, recibe "Ese turno acaba de ser reservado por otra persona. Elegí otro."; queda una sola reserva viva. A la vez (20 rondas, dos conexiones): siempre una sola reserva y el otro socio recibe ese mensaje (ganó 4 veces uno y 16 el otro). |
| CP-16 | Aprobado | Primera carga: pago y comprobante creados y archivo guardado. El mismo archivo en otra reserva (del mismo socio y de otro socio): "Ese comprobante ya fue usado en otra operación.", sin pago ni comprobante nuevos. Dos cargas a la vez del mismo archivo: queda una sola (uq_comprobante_hash); la otra responde <ERROR RollbackException>. |
| CP-42 | Aprobado | 84 comparaciones (7 instalaciones por los 6 períodos de la pantalla y 6 días sueltos): los turnos reservados y disponibles de cada instalación y el total coinciden con lo contado por SQL (1355 turnos ocupados sumando todos los períodos). La pantalla no ofrece un período de un día (el menor es 7 días): el día suelto se comprobó llamando al servicio. |
| CP-52 | Aprobado | 468 combinaciones de estado (6), período (6) e instalación (5 disciplinas del desplegable y 7 nombres en el buscador) sobre 403 reservas: el listado trae exactamente las mismas reservas que la consulta SQL directa, sin repetidas, y el total informado coincide (206 combinaciones con resultados). La pantalla filtra la instalación por disciplina o escribiendo el nombre en el buscador: no hay un desplegable de instalaciones. |

### Pruebas de sistema y de accesibilidad (niveles S y A)

Pruebas de sistema y de accesibilidad de SIGRID. Última ejecución: 14/10/2026 00:29, contra http://localhost:9180/SIGRID (GlassFish 7, Edge sin ventana) con la base de prueba sigrid_pruebas (MySQL 8.0.36). Se corren con `docs/calidad/sistema/recorrido.mjs` (cómo repetirlas, en su [README](sistema/README.md)); la evidencia de cada caso está en `docs/calidad/sistema/`.

| Caso | Resultado | Qué se comprobó | Evidencia |
|---|---|---|---|
| CP-05 | Aprobado | Sin sesión, las 10 pantallas de /admin y las 4 de /socio llevan al login. Con contraseña incorrecta no entra (aviso: "Correo o contraseña incorrectos."). El socio llega a /socio/inicio y abre sus 4 pantallas; en las 10 de /admin y en /admin/comprobante vuelve al login. El administrador llega a /admin/inicio y abre sus 10 pantallas; si abre /socio va a /login.xhtml. Al cerrar sesión (queda en /index.xhtml) /admin vuelve a pedir login. | `evidencia/cp05-clave-incorrecta.png`, `evidencia/cp05-socio-llega-a-su-panel.png`, `evidencia/cp05-socio-en-admin-vuelve-al-login.png`, `evidencia/cp05-admin-llega-a-su-panel.png` |
| CP-36 | Aprobado | Se programó desde las pantallas un cambio de precios de la Cancha de Fútbol 11 y de las cuotas para el 29/10/2026 (avisos: "Cambiaste los precios de Cancha de Fútbol 11: rigen desde el 29/10/2026." y "Cambiaste la cuota mensual: rige desde el 29/10/2026."). Administrador: 32 precios (instalación por categoría, y las 4 cuotas) muestran el precio vigente, "Rige desde el ..." y el próximo cambio ("Desde el 29/10/2026: $ ..." en los 8 que cambian, "Sin cambios programados" en el resto), iguales a los de la base. Socio (Alumno UNSE): sus 8 tarifas (instalaciones y cuota) muestran lo mismo. El precio vigente no cambió al programar el nuevo. | `evidencia/cp36-admin-precios-por-instalacion.png`, `evidencia/cp36-admin-cuota-mensual.png`, `evidencia/cp36-socio-tarifas-vigentes.png` |
| CP-41 | Aprobado | Panel del administrador: 13 vistas (las 10 pantallas y las pestañas de Reportes), en pantalla, en la versión para imprimir y en las ayudas emergentes: el menú, el título de la pantalla, el indicador de Reportes y los PDF dicen "Grado de satisfacción" y no aparece "Valoraciones" en ningún texto (título de la pestaña del navegador: "Grado de satisfacción / Panel de administración"). Observación: el panel del socio sigue usando "valoración" en 7 textos (por ejemplo "Valoración pendiente"); el equipo había dejado esa parte para decidir. La dirección de la pantalla sigue siendo valoraciones-dashboard-admin.xhtml. | `evidencia/cp41-pantalla-grado-de-satisfaccion.png`, `evidencia/cp41-grado-de-satisfaccion.pdf`, `evidencia/cp41-reportes-resumen.png`, `evidencia/cp41-reportes-resumen.pdf`, `evidencia/cp41-socio-sigue-diciendo-valoracion.png` |
| CP-43 | Aprobado | Reservas del 26/09/2026: el informe trae las 13 reservas de ese día y solo esas, en el mismo orden y con el mismo horario, instalación y socio que la base. Encabezado: "Reporte de reservas", Período "26/09/2026", Reservas 13, Emitido "14/10/2026 00:25". El PDF no nombra ningún otro día y lleva "Página 1 de ...". | `evidencia/cp43-reservas-de-un-dia.pdf` |
| CP-44 | Aprobado | Período sin reservas (27/11/2026 al 03/12/2026): no se abre la impresión ni se arma el informe; aviso: "No hay reservas en el período elegido (27/11/2026 al 03/12/2026).". Sin elegir período: "Elegí el período (desde y hasta) antes de exportar.". Con el período al revés: "El período está al revés: la fecha desde es posterior a la fecha hasta.". | `evidencia/cp44-aviso-periodo-sin-reservas.png`, `evidencia/cp44-aviso-sin-periodo.png` |
| CP-45 | Aprobado | Con 500.403 reservas en la base, contra la aplicación desplegada (GlassFish con 512 MB de memoria) y con el navegador: abrir Reservas (indicadores, rankings y primeras 25 filas): 1.7 s (Mostrando 25 de 500403); filtrar una semana en el listado: 0.3 s (Mostrando 25 de 1106); exportar PDF, un día: 0.5 s (154 reservas); imprimir a PDF el informe de un día (en el navegador): 0.5 s; exportar PDF, una semana: 0.5 s (1106 reservas); exportar PDF, un mes: 0.5 s (aviso: El período tiene 4.716 reservas y un PDF admite hasta 2.000: acotá el período o los filtros.); exportar PDF, un año: 0.5 s (aviso: El período tiene 55.130 reservas y un PDF admite hasta 2.000: acotá el período o los filtros.); buscar un nombre en todo el historial (sin período): 2.1 s (Mostrando 25 de 35762). Meta de la Tabla 3: 5 s o menos [A CONFIRMAR en el plan]. Un PDF de una semana: 1106 reservas; de un año: aviso: El período tiene 55.130 reservas y un PDF admite hasta 2.000: acotá el período o los filtros.. Memoria del servidor: al empezar 502 MB, después de los PDF 240 MB, después de Reportes 12 meses 330 MB. OutOfMemoryError en el registro del servidor: 0. Fuera del caso (RNF-07, reportes): abrir Reportes (últimos 30 días): 3.6 s; Reportes, cambiar a 12 meses: 10.3 s. | `evidencia/cp45-pdf-de-un-dia-con-500000.pdf`, `evidencia/cp45-reportes-12-meses-con-500000.png` |
| CP-46 | Aprobado | Reservas de los últimos 30 días (14/09/2026 al 14/10/2026): 225 reservas en 12 páginas. Cada página repite el encabezado de la tabla (Turno, Horario, Instalación, Socio, Estado, Monto) y lleva al pie "Emitido el ..." y "Página N de 12"; empieza en la primera reserva del período y termina en la última. Los PDF de socios (3 páginas) y de membresias (6 páginas) también salen numerados. Impreso con Edge (motor Chromium), que es el que dibuja la numeración. Observación de prolijidad: en hoja A4 el horario se parte en dos renglones en 120 de 225 filas y el monto en 70 ("$" arriba y el importe abajo), por eso entran menos filas por hoja. | `evidencia/cp46-reservas-de-30-dias.pdf`, `evidencia/cp46-como-sale-la-primera-hoja.png`, `evidencia/cp46-socios.pdf`, `evidencia/cp46-membresias.pdf` |
| CP-47 | Falló | 9 vistas, 714 textos, íconos y campos medidos con la fórmula de WCAG 2.1 sobre los colores que calcula el navegador (detalle en evidencia/cp47-contraste.json). 0 textos sobre una imagen no se pueden medir así; 149 íconos acompañan a un texto que dice lo mismo y no se exigen. No llegan al mínimo 3 combinaciones de color (13 apariciones): contorno de campo #ddd9d5 sobre #fffdf3 = 1.38:1 (mínimo 3:1; textarea.so-input.so-textarea, "Contanos qué te gustó o qué podemos mejo", 1 veces en /socio/mis-reservas.xhtml (calificación pendiente)) // texto #c69aa5 sobre #82203c (degradado, peor caso) = 3.85:1 (mínimo 4.5:1; span.id-label, "Nombre y apellido", 9 veces en /socio/inicio.xhtml, /socio/inicio.xhtml (membresía vencida), /socio/inicio.xhtml (calificación pendiente)) // texto #edc06a sobre #913b53 (degradado, peor caso) = 4.20:1 (mínimo 4.5:1; span.id-card-badge, "Socio", 3 veces en /socio/inicio.xhtml, /socio/inicio.xhtml (membresía vencida), /socio/inicio.xhtml (calificación pendiente)) | — |
| CP-48 | Falló | 13 vistas, 3239 textos, íconos y campos medidos con la fórmula de WCAG 2.1 sobre los colores que calcula el navegador (detalle en evidencia/cp48-contraste.json). 0 textos sobre una imagen no se pueden medir así; 351 íconos acompañan a un texto que dice lo mismo y no se exigen. No llegan al mínimo 7 combinaciones de color (73 apariciones): contorno de campo #eae6e2 sobre #fbfaf8 = 1.19:1 (mínimo 3:1; input, "Buscar en esta página", 13 veces en /admin/inicio-dashboard-admin.xhtml, /admin/calendario-dashboard-admin.xhtml, /admin/socios-dashboard-admin.xhtml) // contorno de campo #eae6e2 sobre #fefdfb = 1.22:1 (mínimo 3:1; input, "Buscar socios", 21 veces en /admin/socios-dashboard-admin.xhtml, /admin/membresias-dashboard-admin.xhtml, /admin/reservas-dashboard-admin.xhtml) // contorno de campo #ddd9d5 sobre #fefdfb = 1.38:1 (mínimo 3:1; input.rv-fecha-input, "Turnos desde el día", 2 veces en /admin/reservas-dashboard-admin.xhtml) // texto #afaba7 sobre #f3f0ed = 2.02:1 (mínimo 4.5:1; b, "12:00", 12 veces en /admin/calendario-dashboard-admin.xhtml) // texto #b3b0ad sobre #fbfaf8 = 2.08:1 (mínimo 4.5:1; span.cal-day-num, "28", 6 veces en /admin/calendario-dashboard-admin.xhtml) // texto #9e9d9c sobre #fefdfb = 2.67:1 (mínimo 4.5:1; span.adm-day-num, "28", 11 veces en /admin/inicio-dashboard-admin.xhtml) // texto #929190 sobre #fbfaf8 = 3.03:1 (mínimo 4.5:1; span.cal-day-num, "1", 8 veces en /admin/calendario-dashboard-admin.xhtml) | — |
| CP-49 | Falló | Etiquetas de estado con texto en las listas del administrador y del socio (adm-chip-off: -100 % respecto de ayer, Cancelada, -40 % respecto de septiembre; adm-chip-ok: Confirmada, Vigente; adm-chip-warn: Por confirmar, Urgente, Pendiente, Programada; adm-chip-muted: Cancelación, Completada, Cancelada; adm-chip: Reprogramación; so-chip: verifiedAlumno UNSE; so-chip-ok: Membresía vigente; so-chip-warn: Cancelación pedida; so-chip-off: Cancelada; so-chip-muted: Realizada, Alumno UNSE; so-chip-glass: Confirmada). Capturas en escala de grises de 8 pantallas. Pero hay estados que se distinguen solo por el color: administrador, Calendario: 32 indicadores "cal-ev-muted" (dicen "12:00 Salón SUM", sin texto ni ícono del estado) // administrador, Calendario: 17 indicadores "cal-ev-ok" (dicen "09:00 Fútbol 11", sin texto ni ícono del estado) // administrador, Calendario: 5 indicadores "cal-ev-warn" (dicen "19:00 Fútbol 11", sin texto ni ícono del estado) // administrador, Calendario: 2 indicadores "cal-ev-off" (dicen "20:00 Fútbol 11", sin texto ni ícono del estado). La leyenda ("Confirmada Pendiente Cancelada Ya pasó") también usa solo el color. | — |
| CP-50 | Falló | 19 pantallas (5 públicas, 4 del socio y 10 del administrador) en 360, 768 y 1280 px: 57 capturas en evidencia/cp50 y las medidas en medidas.json. El menú del administrador se pliega y se abre con su botón (360 px: 10 opciones a la vista; 768 px: 10 opciones a la vista). Tablas con desplazamiento horizontal propio: instalaciones: div.table-wrap.reveal (360 px); inicio: div.adm-table-wrap (360, 1280 px); socios: div.adm-table-wrap (360, 768 px); membresias: div.adm-table-wrap (360, 768 px); reservas: div.adm-table-wrap (360, 768, 1280 px); instalaciones: div.adm-table-wrap (360, 1280 px); valoraciones: div.adm-table-wrap (360, 768 px). Problemas (27): /socio/mis-reservas.xhtml a 360 px: la página se desborda 13 px hacia la derecha // /socio/mis-reservas.xhtml a 360 px: "Todas 9" (a#filtros:j_idt95.so-seg) queda cortado por el borde derecho // /socio/mis-reservas.xhtml a 360 px: texto estrujado en una columna de 70 px (8 renglones): "Turno cancelado: 18/10/2026 · 17:00 – 18" // /socio/mis-reservas.xhtml a 360 px: el importe "$ 17.500" se parte en dos renglones (span.so-res-monto) // /admin/inicio-dashboard-admin.xhtml a 360 px: la página se desborda 27 px hacia la derecha // /admin/inicio-dashboard-admin.xhtml a 360 px: "logout" (a.adm-icon-btn) queda cortado por el borde derecho // /admin/calendario-dashboard-admin.xhtml a 360 px: la página se desborda 27 px hacia la derecha // /admin/calendario-dashboard-admin.xhtml a 360 px: "logout" (a.adm-icon-btn) queda cortado por el borde derecho // /admin/socios-dashboard-admin.xhtml a 360 px: la página se desborda 27 px hacia la derecha // /admin/socios-dashboard-admin.xhtml a 360 px: "logout" (a.adm-icon-btn) queda cortado por el borde derecho // /admin/membresias-dashboard-admin.xhtml a 360 px: la página se desborda 27 px hacia la derecha // /admin/membresias-dashboard-admin.xhtml a 360 px: "logout" (a.adm-icon-btn) queda cortado por el borde derecho // /admin/reservas-dashboard-admin.xhtml a 360 px: la página se desborda 27 px hacia la derecha // /admin/reservas-dashboard-admin.xhtml a 360 px: "logout" (a.adm-icon-btn) queda cortado por el borde derecho // /admin/solicitudes-dashboard-admin.xhtml a 360 px: la página se desborda 27 px hacia la derecha // /admin/solicitudes-dashboard-admin.xhtml a 360 px: "logout" (a.adm-icon-btn) queda cortado por el borde derecho // /admin/cancelaciones-dashboard-admin.xhtml a 360 px: la página se desborda 27 px hacia la derecha // /admin/cancelaciones-dashboard-admin.xhtml a 360 px: "logout" (a.adm-icon-btn) queda cortado por el borde derecho // /admin/instalaciones-dashboard-admin.xhtml a 360 px: la página se desborda 27 px hacia la derecha // /admin/instalaciones-dashboard-admin.xhtml a 360 px: "logout" (a.adm-icon-btn) queda cortado por el borde derecho // /admin/valoraciones-dashboard-admin.xhtml a 360 px: la página se desborda 235 px hacia la derecha // /admin/valoraciones-dashboard-admin.xhtml a 360 px: "logout" (a.adm-icon-btn) queda cortado por el borde derecho // /admin/reportes-dashboard-admin.xhtml a 360 px: la página se desborda 27 px hacia la derecha // /admin/reportes-dashboard-admin.xhtml a 360 px: "logout" (a.adm-icon-btn) queda cortado por el borde derecho // /admin/inicio-dashboard-admin.xhtml a 1280 px: la tabla (div.adm-table-wrap) no entra en su tarjeta y hay que desplazarla 96 px para ver las últimas columnas // /admin/reservas-dashboard-admin.xhtml a 1280 px: la tabla (div.adm-table-wrap) no entra en su tarjeta y hay que desplazarla 160 px para ver las últimas columnas // /admin/instalaciones-dashboard-admin.xhtml a 1280 px: la tabla (div.adm-table-wrap) no entra en su tarjeta y hay que desplazarla 54 px para ver las últimas columnas | — |
| CP-51 | Aprobado | socio.completo@demo.sigrid: "Próximas" 4, "Historial" 5 y "Todas" 9, iguales a las 9 reservas del socio en la base (4 pendientes o confirmadas que todavía no terminaron). Cada una muestra instalación, día, horario y estado (CONFIRMADA → "Realizada"; CONFIRMADA → "Confirmada"; RECHAZADA → "Rechazada"; CANCELADA → "Cancelada"; PENDIENTE_PAGO → "Comprobante en revisión"; CONFIRMADA → "Cancelación pedida"). Instalaciones del historial: Cancha de Beach Vóley, Cancha de Fútbol 11, Quincho 2. | `evidencia/cp51-socio-proximas.png`, `evidencia/cp51-socio-historial.png`, `evidencia/cp51-socio-todas.png` |

## Matriz de trazabilidad con resultados (Tabla 30)

Un requerimiento figura como *Aprobado* si todos sus casos lo están.

| Requerimiento | Casos | Resultado |
|---|---|---|
| RF-01 Gestión de socios | CP-01, CP-03, CP-04, CP-06, CP-32, CP-33 | Aprobado |
| RF-02 Seguimiento del uso | CP-51, CP-52 | Aprobado |
| RF-03 Pagos y cobros | CP-13, CP-14, CP-15, CP-16, CP-19, CP-34, CP-35 | Aprobado |
| RF-04 Reservas | CP-08, CP-09, CP-10, CP-11, CP-17, CP-18, CP-27, CP-28 | Aprobado |
| RF-05 Administración de reservas e instalaciones | CP-19, CP-20, CP-31 | Falló (CP-20) |
| RF-06 Reportes | CP-42 | Aprobado |
| RF-07 Grado de satisfacción | CP-37, CP-38, CP-39, CP-40 | Aprobado |
| RNF-01 Diseño adaptable | CP-50 | Falló (CP-50) |
| RNF-02 Estilo gráfico actual | CP-53 | Parcial: 0 de 1 aprobados |
| RNF-03 Contraseñas | CP-02, CP-07 | Aprobado |
| RNF-04 Mensajes de error | CP-02 | Aprobado |
| RNF-05 Seguridad y acceso | CP-05, CP-14, CP-21 | Aprobado |
| RNF-06 Integridad y concurrencia | CP-12 | Aprobado |
| RNF-07 Rendimiento de listados y PDF | CP-45 | Aprobado |
| RNF-08 Accesibilidad visual | CP-47, CP-48, CP-49 | Falló (CP-47, CP-48, CP-49) |
| CC-01 Cancelación automática | CP-22, CP-23, CP-24, CP-25, CP-26 | Aprobado |
| CC-02 Crédito y mantenimiento | CP-29, CP-30 | Aprobado |
| CC-03 Vigencia de tarifas | CP-36 | Aprobado |
| CC-04 Grado de satisfacción (nombre) | CP-41 | Aprobado |
| CC-05 Colores y contrastes | CP-47, CP-48, CP-49 | Falló (CP-47, CP-48, CP-49) |
| CC-06 PDF con filtros de período | CP-43, CP-44, CP-45, CP-46 | Aprobado |

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
