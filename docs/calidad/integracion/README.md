# Pruebas de integración (CP-04, CP-12, CP-16, CP-42 y CP-52)

Los cinco casos de nivel I del plan de garantía: necesitan la base de datos, así que no se pueden hacer con las pruebas unitarias (esas corren sin MySQL). `PruebasIntegracion.java` ejecuta los servicios y las consultas reales de SIGRID contra **una base MySQL de prueba** cargada con `sigrid_schema.sql` (que trae los datos de la demo).

No forma parte del proyecto Maven: se compila y se corre aparte, igual que la medición de `docs/calidad/cp45/`, y usa EclipseLink y el driver de MySQL que ya trae GlassFish (no agrega dependencias).

## Qué comprueba cada caso

| Caso | Qué hace | Contra qué se compara |
|---|---|---|
| CP-04 | Registra con un correo que ya existe (también en mayúsculas y con espacios) y con un DNI que ya existe | El mensaje del servicio, que no se cree ninguna fila, y que la base rechace el `INSERT` directo (`uq_usuario_email`, `uq_socio_dni`). Control: con datos nuevos la misma alta se crea |
| CP-12 | Dos socios piden el mismo turno y día con dos conexiones: una vez paso a paso (el segundo espera en el `INSERT` hasta que el primero confirma) y 20 veces saliendo a la vez | Queda una sola reserva viva y el otro recibe "Ese turno acaba de ser reservado por otra persona" |
| CP-16 | Sube un archivo y vuelve a subir el mismo a otra reserva (del mismo socio y de otro); después, el mismo archivo en dos cargas a la vez | Un solo comprobante con ese hash, sin pagos nuevos, y el mensaje "Ese comprobante ya fue usado en otra operación" |
| CP-42 | Pide la ocupación por instalación en los 6 períodos de la pantalla y en 6 días sueltos | Turnos reservados y disponibles contados con SQL sobre las tablas |
| CP-52 | Filtra el listado del administrador por estado, período e instalación (468 combinaciones) | Los mismos números de reserva que devuelve una consulta SQL directa, y el total informado |

Deja la base como la encontró (borra las reservas, pagos y comprobantes que crea) y los archivos van a una carpeta temporal, no a la de comprobantes reales.

## Cómo repetirlo

1. Compilar el proyecto: dentro de `SIGRID`, `mvn compile`.
2. Crear la base de prueba **con otro nombre** (el programa se niega a correr sobre `sigrid`):

```bash
sed -e 's/^ CREATE DATABASE sigrid;/ CREATE DATABASE sigrid_integ;/' -e 's/^ USE sigrid;/ USE sigrid_integ;/' sigrid_schema.sql > /tmp/schema_integ.sql
mysql -uUSUARIO -p --default-character-set=utf8mb4 < /tmp/schema_integ.sql
```

3. Armar la configuración (se reutiliza la de CP-45) y compilar, desde `docs/calidad/integracion`:

```bash
export DB_USER=USUARIO DB_PASSWORD=CLAVE DB_NAME=sigrid_integ
python ../cp45/armar.py
javac -encoding UTF-8 -cp "$(cat ../cp45/cp.txt)" -d salida PruebasIntegracion.java
```

4. Correrlo (el separador del classpath es `;` en Windows y `:` en Linux). Tarda unos 10 segundos:

```bash
java -Dfile.encoding=UTF-8 -cp "salida;$(cat ../cp45/cp.txt)" PruebasIntegracion resultados.txt
```

Las líneas `[EL Warning] ... Duplicate entry` que aparecen son esperadas: es la base rechazando la segunda reserva o el segundo comprobante.

5. Regenerar el reporte de V&V (lee `resultados.txt`): desde la raíz, `python docs/calidad/generar_reporte_vv.py`.
6. Al terminar: `DROP DATABASE sigrid_integ;` y borrar `docs/calidad/cp45/META-INF/` y `cp.txt` (tienen la clave; están en el `.gitignore`).

## Resultado (13/10/2026)

Los cinco casos aprobados; el detalle de cada uno está en `resultados.txt`. Para confirmar que las pruebas detectan el problema, se quitaron en la base de prueba los `UNIQUE` de correo, de turno y día y de hash: CP-04, CP-12 y CP-16 fallaron, como corresponde.

## Observaciones

- **Dos cargas del mismo archivo a la vez (D-6).** La base deja una sola, pero la segunda no recibe el mensaje de "ya fue usado": `subirComprobante` manda el `INSERT` al cerrar la transacción y el error sale como una excepción técnica (`RollbackException`) que la pantalla no atrapa. `reservar` ya resuelve el mismo problema mandando el `INSERT` en el momento (`ReservaDAO.insertar`).
- **CP-42:** la pantalla de reportes no ofrece un período de un día (el menor es "Últimos 7 días") ni elegir una instalación: muestra todas juntas. El día suelto se comprobó llamando al servicio.
- **CP-52:** la pantalla filtra la instalación por disciplina (desplegable) o escribiendo el nombre en el buscador; no hay un desplegable de instalaciones.
