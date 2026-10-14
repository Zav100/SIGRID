# CP-45: medición con 500.000 reservas

Material para repetir la medición del caso CP-45 del plan de garantía (CC-06): con 500.000 reservas cargadas, listar y exportar un período acotado resolviendo el filtro en SQL y sin agotar la memoria.

No forma parte del proyecto Maven ni de las pruebas unitarias (esas corren sin base de datos). Esto usa **una base MySQL de prueba** y las consultas y servicios reales de SIGRID.

## Qué hace cada archivo

| Archivo | Para qué sirve |
|---|---|
| `carga_500k.sql` | Agrega 30 instalaciones de prueba con 5 turnos cada una y 500.000 reservas (60 % confirmadas, 30 % canceladas, 10 % rechazadas y unas 450 pendientes) con sus pagos, repartidas en unos 9 años |
| `armar.py` | Escribe `META-INF/persistence.xml` y `cp.txt` (classpath) leyendo el usuario y la clave de MySQL de variables de entorno. Usa EclipseLink y el driver de MySQL que ya trae GlassFish: no agrega dependencias al proyecto |
| `MedirReservas.java` | Mide tiempos y memoria de lo que usan las pantallas (listado, buscador, indicadores, PDF y reportes) y comprueba que los filtros en SQL dan lo mismo que clasificar cada reserva en memoria |

## Cómo repetirlo

1. Compilar el proyecto (deja las clases en `SIGRID/target/classes`): dentro de `SIGRID`, `mvn compile`.
2. Crear la base de prueba **con otro nombre** a partir del esquema (nunca correr esto sobre `sigrid`):

```bash
sed -e 's/^ CREATE DATABASE sigrid;/ CREATE DATABASE sigrid_cc06;/' -e 's/^ USE sigrid;/ USE sigrid_cc06;/' sigrid_schema.sql > /tmp/schema_cc06.sql
mysql -uUSUARIO -p --default-character-set=utf8mb4 < /tmp/schema_cc06.sql
mysql -uUSUARIO -p --default-character-set=utf8mb4 < docs/calidad/cp45/carga_500k.sql
```

3. Armar la configuración y compilar el medidor (en `docs/calidad/cp45`):

```bash
export DB_USER=USUARIO DB_PASSWORD=CLAVE        # y GLASSFISH_HOME si GlassFish no está en C:/glassfish7/glassfish
python armar.py
javac -encoding UTF-8 -cp "$(cat cp.txt)" -d salida MedirReservas.java
```

4. Correrlo (el separador del classpath es `;` en Windows y `:` en Linux):

```bash
java -Xmx1024m -cp "salida;$(cat cp.txt)" MedirReservas nuevo         # lo que usan las pantallas
java -Xmx3g    -cp "salida;$(cat cp.txt)" MedirReservas equivalencia  # SQL contra clasificación en memoria
java -Xmx1024m -cp "salida;$(cat cp.txt)" MedirReservas anterior      # cargar todas las reservas (versión previa)
```

5. Al terminar: `DROP DATABASE sigrid_cc06;`, y borrar `META-INF/` y `cp.txt` (tienen la clave; están en el `.gitignore`).

## Resultados (13/10/2026)

Equipo de desarrollo: Windows 11, MySQL 8.0.36 local, JDK 21 (`-Xmx1024m`), EclipseLink 4 de GlassFish 7. La base y la aplicación corren en la misma máquina; la memoria es el heap de la JVM de la medición (no el de GlassFish).

| Operación | Tiempo | Filas |
|---|---|---|
| Contar todas las reservas | 29 ms | 500.403 |
| Listado, primera tanda de 25 (sin filtros) | 150 ms | 25 de 500.403 |
| Listado, un mes (octubre 2026) | 45 ms | 25 de 4.780 |
| Listado, por estado (confirmadas, canceladas, rechazadas, por confirmar, esperando pago) | 26 a 125 ms | 25 |
| Listado, Fútbol y todavía no terminaron | 28 ms | 25 de 954 |
| Listado, ya terminaron | 450 ms | 25 de 497.546 |
| Buscador (nombre, número, fecha) en todo el historial | 1,8 a 2,6 s | 0 a 152 |
| Próximas confirmadas (pestaña Predio de Cancelaciones) | 480 ms | 1.705 |
| Indicadores, rankings y recaudación de la página de reservas | 1,2 s | — |
| PDF de un día | 36 ms | 154 |
| PDF de una semana | 140 ms | 1.061 |
| PDF de un mes (4.780 reservas) | 4 ms | aviso: acotar (máximo 2.000) |
| PDF de un período sin reservas | 2 ms | aviso: no hay reservas |
| PDF de un año (46.003 reservas) | 13 ms | aviso: acotar |
| Reportes, reservas de 7 días y su previo | 2,1 s | 3.268 |
| Reportes, 30 días y su previo (cargar y calcular el resumen) | 2,3 s, 270 MB | 10.391 |
| Reportes, 90 días y su previo | 3,0 s | 28.431 |
| Reportes, 12 meses y su previo (cargar y calcular el resumen) | 6,7 s, 470 a 570 MB | 110.936 |
| **Versión anterior**: cargar todas las reservas | **sin terminar: OutOfMemoryError con 512 MB y con 1 GB** | 500.403 |

La comprobación de equivalencia (16 combinaciones de estado, período, fecha y disciplina) da el mismo número por SQL que clasificando las 500.403 reservas en memoria.

## Lo que queda

- Los reportes de **12 meses** con este volumen leen unas 110.000 reservas y necesitan más de 500 MB: para esa escala hay que pasar los cálculos a consultas de agregación (hoy se arman en memoria con las reservas del período).
- El buscador libre no puede usar un índice (busca texto dentro del nombre, el correo y el DNI): en todo el historial tarda unos 2,5 s; acotando el período baja a milisegundos.
- La consulta de reportes recorre la tabla para encontrar los pagos validados del período (no hay índice sobre `pago.fecha_validacion`); un índice ahí es un cambio de esquema que no se hizo sin consultar.
