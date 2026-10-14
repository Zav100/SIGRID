# Incidencias de SIGRID (Apéndice G del plan de garantía)

Cada problema encontrado en las pruebas, registrado con la plantilla de la Tabla 43. La **severidad es una propuesta** del ingeniero de pruebas: la asigna el líder de SQA (Tabla 31). El **estado** usa los de la sección 7.3. Los campos que decide el equipo dicen *[completar]*.

En los demás documentos se las nombra por su código de defecto: INC-nn corresponde a D-n (INC-01 es D-1, y así).

| Código | Defecto | Caso | Título | Severidad propuesta | Estado |
|---|---|---|---|---|---|
| [INC-01](#inc-01) | D-1 | CP-07 | Un hash de contraseña mal formado rompía el inicio de sesión | Baja | En verificación |
| [INC-02](#inc-02) | D-2 | CP-15 | El comprobante toleraba 5 minutos de adelanto y la regla es 25 | Baja | En verificación |
| [INC-03](#inc-03) | D-3 | CP-32 | La renovación por adelantado empezaba un día después del vencimiento | Alta | En verificación |
| [INC-04](#inc-04) | D-4 | CP-02 | La contraseña no exigía letras, números y un símbolo | Alta | En verificación |
| [INC-05](#inc-05) | D-5 | CP-20 | Se puede rechazar una solicitud sin dar motivo | Media | En análisis (diferida) |
| [INC-06](#inc-06) | D-6 | CP-16 | Dos cargas simultáneas del mismo comprobante: la segunda recibe un error técnico | Baja | Nueva |
| [INC-07](#inc-07) | D-7 | CP-49 | En la grilla del calendario el estado de la reserva se distingue solo por el color | Media | Nueva |
| [INC-08](#inc-08) | D-8 | CP-50 | A 360 px el panel del administrador y "Mis reservas" del socio se desbordan | Media | Nueva |
| [INC-09](#inc-09) | D-9 | CP-50 | A 1280 px tres tablas del administrador no entran en su tarjeta | Baja | Nueva |
| [INC-10](#inc-10) | D-10 | CP-47 | Contraste insuficiente en el carnet digital y en el campo de comentario del socio | Media | Nueva |
| [INC-11](#inc-11) | D-11 | CP-48 | Contraste insuficiente en el contorno de los filtros y en los días de otros meses del calendario | Media | Nueva |
| [INC-12](#inc-12) | D-12 | CP-46 | En el PDF de reservas el horario y el monto se parten en dos renglones | Baja | Nueva |
| [INC-13](#inc-13) | D-13 | CP-51 | "Mis reservas" repite la fecha del turno | Baja | Nueva |

---

## INC-01

| Campo | Contenido |
|---|---|
| Código | INC-01 (D-1) |
| Título | `PasswordUtil.verificar` lanzaba una excepción con un hash mal formado, en vez de rechazar la contraseña |
| Fecha y quién la detectó | 12/10/2026, ingeniero de pruebas (pruebas unitarias) |
| Origen | Caso CP-07 |
| Requisito o caso afectado | RNF-03, CP-07 |
| Pasos para repetirla | (1) Dejar en `usuario.password_hash` un valor de tres partes con la primera no numérica o con Base64 inválido. (2) Intentar iniciar sesión con esa cuenta |
| Resultado obtenido | `NumberFormatException` o `IllegalArgumentException`: el inicio de sesión de esa cuenta termina en error |
| Resultado esperado | La contraseña se rechaza como incorrecta |
| Evidencia | Prueba deshabilitada que fallaba con `NumberFormatException` (commit `dfc6502`) |
| Severidad (Tabla 31) | Baja: falla parcial que no afecta los datos |
| Estado (sección 7.3) | En verificación: corregido el 13/10/2026 (commit `32e9168`, rama `correccion/defectos-d1-a-d4`) |
| Responsable de la corrección | [completar] |
| Solicitud de cambio | [completar: PR de la rama `correccion/defectos-d1-a-d4`] |
| Caso de regresión agregado | `PasswordUtilTest.rechazaUnHashMalFormadoSinLanzarExcepcion` |
| Verificó y fecha de cierre | [completar] |

## INC-02

| Campo | Contenido |
|---|---|
| Código | INC-02 (D-2) |
| Título | El comprobante de transferencia toleraba 5 minutos de adelanto; la regla confirmada es 25 |
| Fecha y quién la detectó | 12/10/2026, ingeniero de pruebas (pruebas unitarias); regla confirmada por el responsable del proyecto |
| Origen | Caso CP-15 |
| Requisito o caso afectado | RF-03, CP-15 |
| Pasos para repetirla | (1) Reservar un turno. (2) Subir el comprobante declarando una transferencia 25 minutos posterior a la hora del servidor |
| Resultado obtenido | Se rechaza: "La fecha de la transferencia no puede ser futura" |
| Resultado esperado | Se acepta hasta 25 minutos de adelanto (diferencia de reloj entre el banco y el servidor) |
| Evidencia | Prueba deshabilitada que fallaba (commit `dfc6502`) |
| Severidad (Tabla 31) | Baja |
| Estado (sección 7.3) | En verificación: corregido el 13/10/2026 (commit `32e9168`) |
| Responsable de la corrección | [completar] |
| Solicitud de cambio | [completar: PR de la rama `correccion/defectos-d1-a-d4`] |
| Caso de regresión agregado | `PanelSocioServicioComprobanteTest.aceptaUnaTransferenciaAdelantadaHasta25Minutos` |
| Verificó y fecha de cierre | [completar] |

## INC-03

| Campo | Contenido |
|---|---|
| Código | INC-03 (D-3) |
| Título | La renovación de la membresía pagada por adelantado empezaba el día siguiente al vencimiento |
| Fecha y quién la detectó | 12/10/2026, ingeniero de pruebas (pruebas unitarias); regla confirmada por el responsable del proyecto |
| Origen | Caso CP-32 |
| Requisito o caso afectado | RF-01, CP-32 |
| Pasos para repetirla | (1) Socio con membresía del 23/09 al 23/10. (2) Renovar antes del 23/10. (3) El administrador acepta el comprobante |
| Resultado obtenido | La nueva va del 24/10 al 24/11: cada renovación corre el ciclo un día |
| Resultado esperado | La nueva va del 23/10 al 23/11 |
| Evidencia | Pruebas deshabilitadas que fallaban (commit `dfc6502`) |
| Severidad (Tabla 31) | Alta: falla una regla de negocio |
| Estado (sección 7.3) | En verificación: corregido el 13/10/2026 (commit `32e9168`) |
| Responsable de la corrección | [completar] |
| Solicitud de cambio | [completar: PR de la rama `correccion/defectos-d1-a-d4`] |
| Caso de regresión agregado | `MembresiasServicioTest.laRenovacionPorAdelantadoMantieneElMismoDiaDelMes` y `siSePagaElMismoDiaDelVencimientoLaNuevaEmpiezaEseDia` |
| Verificó y fecha de cierre | [completar] |

## INC-04

| Campo | Contenido |
|---|---|
| Código | INC-04 (D-4) |
| Título | El registro aceptaba contraseñas sin letras, sin números o sin símbolo |
| Fecha y quién la detectó | 12/10/2026, ingeniero de pruebas (pruebas unitarias) |
| Origen | Caso CP-02 |
| Requisito o caso afectado | RNF-03, RNF-04, CP-02 |
| Pasos para repetirla | (1) Registrarse con la contraseña `123456` o `Clave2026` |
| Resultado obtenido | Se acepta (solo se exigían 6 caracteres) |
| Resultado esperado | Se rechaza con un mensaje que pide letras, números y un símbolo |
| Evidencia | Prueba deshabilitada que fallaba (commit `dfc6502`) |
| Severidad (Tabla 31) | Alta |
| Estado (sección 7.3) | En verificación: corregido el 13/10/2026 (commit `32e9168`) |
| Responsable de la corrección | [completar] |
| Solicitud de cambio | [completar: PR de la rama `correccion/defectos-d1-a-d4`] |
| Caso de regresión agregado | `RegistroServicioTest.rechazaUnaContrasenaSinLetrasNumerosOSimbolo` (5 contraseñas) |
| Verificó y fecha de cierre | [completar] |

## INC-05

| Campo | Contenido |
|---|---|
| Código | INC-05 (D-5) |
| Título | El administrador puede rechazar una solicitud sin escribir el motivo |
| Fecha y quién la detectó | 12/10/2026, ingeniero de pruebas (pruebas unitarias) |
| Origen | Caso CP-20 |
| Requisito o caso afectado | RF-05, CP-20 |
| Pasos para repetirla | (1) Como administrador, abrir Solicitudes pendientes. (2) Denegar una solicitud dejando el motivo vacío |
| Resultado obtenido | Se rechaza sin motivo (la pantalla dice "Motivo (opcional)") |
| Resultado esperado | No se permite rechazar sin motivo |
| Evidencia | `ReservaServicioTest.rechazarSinMotivoNoSePermiteYLaSolicitudSigueEsperando` (deshabilitada) |
| Severidad (Tabla 31) | Media |
| Estado (sección 7.3) | En análisis: el equipo decidió el 13/10/2026 dejar el motivo opcional por ahora. CP-20 figura como "Falló" hasta que se implemente o se corrija el plan |
| Responsable de la corrección | [completar] |
| Solicitud de cambio | — |
| Caso de regresión agregado | La prueba de arriba (se habilita al implementarlo) |
| Verificó y fecha de cierre | [completar] |

## INC-06

| Campo | Contenido |
|---|---|
| Código | INC-06 (D-6) |
| Título | Dos cargas simultáneas del mismo comprobante: la segunda termina en un error técnico en lugar del aviso |
| Fecha y quién la detectó | 13/10/2026, ingeniero de pruebas (prueba de integración contra MySQL) |
| Origen | Caso CP-16 |
| Requisito o caso afectado | RF-03, CP-16 |
| Pasos para repetirla | (1) Dos socios con una reserva esperando pago cada uno. (2) Los dos suben el mismo archivo exactamente al mismo tiempo. Lo reproduce `docs/calidad/integracion/PruebasIntegracion.java` |
| Resultado obtenido | La base guarda uno solo (UNIQUE `uq_comprobante_hash`), pero el segundo recibe `RollbackException`, que `SocioReservasBean.subir` no atrapa: vería la página de error |
| Resultado esperado | El segundo recibe "Ese comprobante ya fue usado en otra operación." |
| Evidencia | `docs/calidad/integracion/resultados.txt` (línea OBS-16) |
| Severidad (Tabla 31) | Baja: los datos quedan bien |
| Estado (sección 7.3) | Nueva |
| Responsable de la corrección | [completar] |
| Solicitud de cambio | Propuesta: hacer en `subirComprobante` lo mismo que hace `reservar` (mandar el INSERT en el momento y traducir el error) |
| Caso de regresión agregado | `PruebasIntegracion` (CP-16) ya lo detecta; habría que exigir el mensaje |
| Verificó y fecha de cierre | [completar] |

## INC-07

| Campo | Contenido |
|---|---|
| Código | INC-07 (D-7) |
| Título | En la grilla del calendario del administrador el estado de cada reserva se distingue solo por el color |
| Fecha y quién la detectó | 14/10/2026, ingeniero de pruebas (prueba de sistema) |
| Origen | Caso CP-49 |
| Requisito o caso afectado | CC-05, RNF-08, CP-49 (WCAG 2.1, criterio 1.4.1 "Uso del color") |
| Pasos para repetirla | (1) Iniciar sesión como administrador. (2) Abrir Calendario. (3) Mirar las reservas dentro de los días del mes (por ejemplo en escala de grises) |
| Resultado obtenido | Cada reserva de la grilla muestra hora e instalación ("19:00 Fútbol 11"); confirmada, pendiente, cancelada y "ya pasó" se distinguen solo por el color de fondo. La leyenda también usa solo puntos de color |
| Resultado esperado | Cada estado se distingue sin depender del color (texto, ícono o forma) |
| Evidencia | `docs/calidad/sistema/evidencia/cp49-admin-calendario-en-grises.png`; detalle en `sistema/resultados.txt` |
| Severidad (Tabla 31) | Media: hay una salida alternativa (al elegir el día, la lista de la derecha dice el estado con texto) |
| Estado (sección 7.3) | Nueva |
| Responsable de la corrección | [completar] |
| Solicitud de cambio | Propuesta: agregar un ícono chico por estado a cada reserva de la grilla (`cal-ev` en `admin/calendario-dashboard-admin.xhtml`) y a la leyenda |
| Caso de regresión agregado | `recorrido.mjs` CP-49 |
| Verificó y fecha de cierre | [completar] |

## INC-08

| Campo | Contenido |
|---|---|
| Código | INC-08 (D-8) |
| Título | A 360 px de ancho el panel del administrador y "Mis reservas" del socio se desbordan |
| Fecha y quién la detectó | 14/10/2026, ingeniero de pruebas (prueba de adaptación) |
| Origen | Caso CP-50 |
| Requisito o caso afectado | RNF-01, CP-50 |
| Pasos para repetirla | (1) Abrir el sitio con una ventana de 360 px (un teléfono). (2) Iniciar sesión como administrador y recorrer las pantallas. (3) Iniciar sesión como socio y abrir Mis reservas |
| Resultado obtenido | **Administrador:** en las 10 pantallas, la barra de arriba se pasa 27 px del ancho: el botón de cerrar sesión queda cortado por el borde derecho y aparece la barra horizontal; en Grado de satisfacción la página se pasa 235 px. **Socio, Mis reservas:** las pestañas se pasan 13 px ("Todas" queda cortada); en la tarjeta del crédito el texto queda en una columna de 70 px (una palabra por renglón); el importe de una reserva se parte en dos renglones ("$" arriba, "17.500" abajo). A 768 y 1280 px no pasa |
| Resultado esperado | Sin desbordes ni elementos cortados o superpuestos en 360, 768 y 1280 px |
| Evidencia | `docs/calidad/sistema/evidencia/cp50/admin-*-360.png`, `cp50/socio-mis-reservas-360.png`, `cp50/medidas.json` |
| Severidad (Tabla 31) | Media: en el teléfono se puede desplazar de costado para llegar al botón |
| Estado (sección 7.3) | Nueva |
| Responsable de la corrección | [completar] |
| Solicitud de cambio | [completar] |
| Caso de regresión agregado | `recorrido.mjs` CP-50 |
| Verificó y fecha de cierre | [completar] |

## INC-09

| Campo | Contenido |
|---|---|
| Código | INC-09 (D-9) |
| Título | A 1280 px tres tablas del administrador no entran en su tarjeta y esconden las últimas columnas |
| Fecha y quién la detectó | 14/10/2026, ingeniero de pruebas (prueba de adaptación) |
| Origen | Caso CP-50 |
| Requisito o caso afectado | RNF-01, CP-50 |
| Pasos para repetirla | (1) Ventana de 1280 px. (2) Como administrador, abrir Dashboard (Solicitudes por confirmar), Reservas (Historial de reservas) e Instalaciones (Precios de reserva por tipo de socio) |
| Resultado obtenido | Hay que desplazar la tabla de costado 96, 160 y 54 px para ver las últimas columnas. En Reservas los botones Aceptar y Denegar quedan cortados; en Instalaciones, la columna Externo. La barra para desplazar queda al pie de la tabla, fuera de la vista |
| Resultado esperado | A 1280 px las tablas entran completas (en un teléfono es esperable desplazarlas) |
| Evidencia | `docs/calidad/sistema/evidencia/cp50/admin-reservas-1280.png`, `admin-instalaciones-1280.png`, `admin-inicio-1280.png` |
| Severidad (Tabla 31) | Baja |
| Estado (sección 7.3) | Nueva |
| Responsable de la corrección | [completar] |
| Solicitud de cambio | [completar] |
| Caso de regresión agregado | `recorrido.mjs` CP-50 |
| Verificó y fecha de cierre | [completar] |

## INC-10

| Campo | Contenido |
|---|---|
| Código | INC-10 (D-10) |
| Título | Contraste insuficiente en el carnet digital y en el campo de comentario del socio |
| Fecha y quién la detectó | 14/10/2026, ingeniero de pruebas (medición de contraste sobre la aplicación desplegada) |
| Origen | Caso CP-47 |
| Requisito o caso afectado | CC-05, RNF-08, CP-47 |
| Pasos para repetirla | (1) Iniciar sesión como socio. (2) Inicio: mirar el carnet. (3) Mis reservas con una calificación pendiente: el campo de comentario |
| Resultado obtenido | Carnet: las etiquetas "Nombre y apellido" y "N.º de socio" (`id-label`, #c69aa5) quedan en 3,85:1 sobre el bordó más claro del degradado. La insignia "Socio" (`id-card-badge`, #edc06a) queda en 4,20:1. El contorno del campo de comentario (#ddd9d5) queda en 1,38:1 contra su fondo |
| Resultado esperado | 4,5:1 el texto normal; 3:1 el contorno de los campos |
| Evidencia | `docs/calidad/sistema/evidencia/cp47-contraste.json` |
| Severidad (Tabla 31) | Media: no cumple un requisito no funcional; el texto se lee |
| Estado (sección 7.3) | Nueva |
| Responsable de la corrección | [completar] |
| Solicitud de cambio | Propuesta: aclarar las etiquetas del carnet y oscurecer el borde de los campos. Agregar esos pares a `ContrasteCssTest` |
| Caso de regresión agregado | `recorrido.mjs` CP-47 |
| Verificó y fecha de cierre | [completar] |

## INC-11

| Campo | Contenido |
|---|---|
| Código | INC-11 (D-11) |
| Título | Contraste insuficiente en el contorno de los filtros y en los días de otros meses de los calendarios del administrador |
| Fecha y quién la detectó | 14/10/2026, ingeniero de pruebas (medición de contraste sobre la aplicación desplegada) |
| Origen | Caso CP-48 |
| Requisito o caso afectado | CC-05, RNF-08, CP-48 |
| Pasos para repetirla | (1) Iniciar sesión como administrador. (2) Abrir Reservas, Socios o Membresías: mirar el buscador, los desplegables y las fechas. (3) Abrir Calendario y Dashboard: mirar los días del mes anterior y del siguiente |
| Resultado obtenido | El contorno del buscador de la página y de los filtros (#eae6e2) da 1,19:1 a 1,22:1. El de las fechas Desde y Hasta (#ddd9d5) da 1,38:1. En el Calendario, los días de otros meses y sus reservas dan 2,02:1 a 3,03:1. En el calendario chico del Dashboard dan 2,67:1 |
| Resultado esperado | 3:1 el contorno de los campos; 4,5:1 el texto |
| Evidencia | `docs/calidad/sistema/evidencia/cp48-contraste.json` |
| Severidad (Tabla 31) | Media |
| Estado (sección 7.3) | Nueva |
| Responsable de la corrección | [completar] |
| Solicitud de cambio | Propuesta: un borde más oscuro para `adm-search`, `soc-select` y `rv-fecha-input`; los días de otros meses con texto gris de 4,5:1 (se pueden seguir distinguiendo con un fondo distinto) |
| Caso de regresión agregado | `recorrido.mjs` CP-48 |
| Verificó y fecha de cierre | [completar] |

## INC-12

| Campo | Contenido |
|---|---|
| Código | INC-12 (D-12) |
| Título | En el PDF de reservas el horario y el monto se parten en dos renglones |
| Fecha y quién la detectó | 14/10/2026, ingeniero de pruebas (prueba de sistema) |
| Origen | Caso CP-46 |
| Requisito o caso afectado | CC-06 ("PDF prolijos"), CP-46 |
| Pasos para repetirla | (1) Como administrador, en Reservas elegir los últimos 30 días. (2) Exportar PDF en hoja A4 |
| Resultado obtenido | En 147 de 225 filas el horario sale como "18:00 –" y "19:00" en dos renglones, y en 71 el monto como "$" y "40.000". Las filas quedan altas y entran unas 19 por hoja (12 hojas) |
| Resultado esperado | Cada dato en un renglón |
| Evidencia | `docs/calidad/sistema/evidencia/cp46-reservas-de-30-dias.pdf`, `cp46-como-sale-la-primera-hoja.png` |
| Severidad (Tabla 31) | Baja (la Tabla 31 pone de ejemplo un defecto del PDF) |
| Estado (sección 7.3) | Nueva |
| Responsable de la corrección | [completar] |
| Solicitud de cambio | Propuesta: que las columnas de horario y de monto no se partan (`white-space: nowrap` en la hoja de impresión del informe) |
| Caso de regresión agregado | `recorrido.mjs` CP-46 (informa cuántas filas se parten) |
| Verificó y fecha de cierre | [completar] |

## INC-13

| Campo | Contenido |
|---|---|
| Código | INC-13 (D-13) |
| Título | "Mis reservas" del socio repite la fecha del turno |
| Fecha y quién la detectó | 14/10/2026, ingeniero de pruebas (revisión de las capturas de CP-51) |
| Origen | Caso CP-51 |
| Requisito o caso afectado | RF-02, CP-51 |
| Pasos para repetirla | (1) Iniciar sesión como socio con reservas a más de dos días. (2) Abrir Mis reservas |
| Resultado obtenido | "vie 23/10/2026 · 23/10/2026 · 12:00 – 17:00": la fecha sale dos veces. La plantilla muestra `cuando` y `fecha`, y `cuando` ya trae la fecha salvo para "Hoy" y "Mañana" |
| Resultado esperado | "vie 23/10/2026 · 12:00 – 17:00" (y "Mañana · 14/10/2026 · ..." como hoy) |
| Evidencia | `docs/calidad/sistema/evidencia/cp51-socio-todas.png` |
| Severidad (Tabla 31) | Baja |
| Estado (sección 7.3) | Nueva |
| Responsable de la corrección | [completar] |
| Solicitud de cambio | [completar] |
| Caso de regresión agregado | [completar al corregir] |
| Verificó y fecha de cierre | [completar] |
