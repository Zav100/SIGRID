# Informe de RTF: diseño visual de las pantallas (RTF-03, caso CP-53)

Plantilla del Apéndice E del plan de garantía, preparada para la revisión del diseño visual. CP-53 se aprueba cuando la cátedra y el equipo aprueban el diseño y no quedan observaciones críticas. Lo que dice *[completar]* lo llena el equipo en la reunión.

Los hallazgos de abajo salen de las pruebas de sistema del 14/10/2026 ([sistema/README.md](sistema/README.md)). El grupo puede aceptarlos, cambiarlos o agregar otros.

## E.1 Datos de la reunión

**Tabla 38. Datos de la reunión (informe de RTF)**

| Campo | Contenido |
|---|---|
| RTF y producto revisado | RTF-03: pantallas del socio y del administrador de SIGRID v1.8 (rama `calidad/pruebas-de-sistema`) |
| Fecha, hora y duración | [completar] |
| Moderador y secretario | [completar] |
| Participantes | [completar] |
| Material revisado | Las 57 capturas de `sistema/evidencia/cp50/` (19 pantallas en 360, 768 y 1280 px), las capturas en escala de grises de `sistema/evidencia/cp49-*`, la medición de contraste (`cp47-contraste.json`, `cp48-contraste.json`), los PDF de `sistema/evidencia/` y la aplicación desplegada |
| Resultado | [completar: Aprobado / Aprobado con cambios / Rechazado] |

## E.2 Hallazgos y acciones

**Tabla 39. Hallazgos y acciones (informe de RTF)**

| Código | Descripción del hallazgo | Severidad | Responsable | Fecha límite |
|---|---|---|---|---|
| INC-07 | En la grilla del calendario del administrador el estado de cada reserva se distingue solo por el color (CP-49) | Media (propuesta) | [completar] | [completar] |
| INC-08 | A 360 px el panel del administrador se desborda 27 px (el botón de salir queda cortado) y "Mis reservas" del socio también (pestañas, tarjeta del crédito, importe partido) (CP-50) | Media (propuesta) | [completar] | [completar] |
| INC-09 | A 1280 px tres tablas del administrador esconden sus últimas columnas (CP-50) | Baja (propuesta) | [completar] | [completar] |
| INC-10 | Contraste del carnet digital (3,85:1 y 4,20:1) y del campo de comentario del socio (CP-47) | Media (propuesta) | [completar] | [completar] |
| INC-11 | Contraste del contorno de los filtros (1,2:1 a 1,4:1) y de los días de otros meses de los calendarios (2,0:1 a 3,0:1) (CP-48) | Media (propuesta) | [completar] | [completar] |
| INC-12 | En el PDF de reservas el horario y el monto se parten en dos renglones (CP-46) | Baja (propuesta) | [completar] | [completar] |
| INC-13 | "Mis reservas" repite la fecha del turno (CP-51) | Baja (propuesta) | [completar] | [completar] |
| [completar] | Lo que agregue el grupo de revisión | | | |

Para decidir en la reunión:

- **CC-04 en el panel del socio:** el administrador ya dice "Grado de satisfacción"; el socio sigue con "valoración" (7 textos).
- **Pantallas nuevas para revisar:** en Reportes, el período "Un día" y la tarjeta "Ocupación de una instalación" (CP-42); en Reservas, el filtro "Todas las instalaciones" (CP-52). Se agregaron el 14/10/2026 para que coincidan con el plan.

## E.3 Decisión y firmas

Decisión final del grupo de revisión: [completar]

Firma del líder de SQA: ____________________ Firma del revisor de la cátedra: ____________________
