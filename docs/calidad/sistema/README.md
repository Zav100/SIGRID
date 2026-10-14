# Pruebas de sistema y de accesibilidad (casos S y A del plan)

Los casos de nivel S (sistema) y A (accesibilidad, adaptación y rendimiento) del plan de garantía, ejecutados contra **SIGRID desplegado en GlassFish** con una **base MySQL de prueba**. `recorrido.mjs` abre las pantallas con Microsoft Edge sin ventana, inicia sesión con las cuentas de prueba de `sigrid_schema.sql`, hace lo que haría una persona (filtrar, exportar, cambiar precios), compara lo que se ve con lo que hay en la base y guarda capturas y PDF en `evidencia/`. El resultado de cada caso queda en `resultados.txt`, que lee `docs/calidad/generar_reporte_vv.py`.

No forma parte del proyecto Maven y no instala nada: usa Node 22 o superior (que ya trae lo necesario para hablar con el navegador) y el Edge de Windows.

| Archivo | Para qué sirve |
|---|---|
| `recorrido.mjs` | Los casos: cada uno abre las pantallas, actúa, compara con la base y guarda la evidencia |
| `navegador.mjs` | Maneja Edge sin ventana: abrir, escribir, hacer clic, capturar, imprimir a PDF |
| `medidas.mjs` | Lo que se mide dentro de la página: contraste (WCAG 2.1), desbordes y superposiciones, y cómo se muestran los estados |
| `resultados.txt` | Resultado y detalle de cada caso de la última ejecución |
| `evidencia/` | Capturas, PDF y mediciones (`cp47-contraste.json`, `cp48-contraste.json`, `cp50/medidas.json`) |

## Resultado (14/10/2026)

| Caso | Resultado | Qué se comprobó | Evidencia |
|---|---|---|---|
| CP-05 | Aprobado | Sin sesión, las 14 pantallas de `/admin` y `/socio` llevan al login. Con contraseña incorrecta no entra. Cada rol llega a su panel. El socio que abre cualquiera de las 10 pantallas de `/admin` (o el archivo de un comprobante) vuelve al login. Al cerrar sesión, `/admin` vuelve a pedir login | `cp05-*.png` |
| CP-36 | Aprobado | Se programó desde las pantallas un cambio de precios y de cuotas a 15 días. Administrador: 32 precios (instalación por categoría y las 4 cuotas) muestran precio vigente, "Rige desde el ..." y el próximo cambio, iguales a la base. Socio: lo mismo para sus 8 tarifas | `cp36-*.png` |
| CP-41 | Aprobado | Menú, títulos, ayudas, versión para imprimir, pestañas de Reportes y PDF del administrador dicen "Grado de satisfacción"; "Valoraciones" no aparece en ningún texto | `cp41-*` |
| CP-43 | Aprobado | El PDF de un día trae exactamente las reservas de ese día (mismo orden, horario, instalación y socio que la base), con encabezado, período y fecha de emisión | `cp43-reservas-de-un-dia.pdf` |
| CP-44 | Aprobado | Período sin reservas: no se imprime nada y aparece el aviso. También sin período y con el período al revés | `cp44-*.png` |
| CP-45 | Aprobado | Con 500.403 reservas, contra la aplicación desplegada (GlassFish con 512 MB): abrir Reservas 1,7 s, filtrar una semana 0,3 s, PDF de un día o de una semana 0,5 s, buscar un nombre en todo el historial 2,1 s, sin agotar la memoria. Meta de la Tabla 3: 5 s o menos (todavía "[A CONFIRMAR]" en el plan) | `cp45-*` |
| CP-46 | Aprobado | 225 reservas en 12 páginas: cada una repite el encabezado de la tabla y lleva "Emitido el ..." y "Página N de 12". Observación de prolijidad: INC-12 | `cp46-*` |
| CP-47 | **Falló** | 714 textos y campos medidos en 9 vistas del socio: 3 combinaciones de color no llegan al mínimo (INC-10) | `cp47-contraste.json` |
| CP-48 | **Falló** | 3.239 textos y campos medidos en 13 vistas del administrador: 7 combinaciones no llegan al mínimo (INC-11) | `cp48-contraste.json` |
| CP-49 | **Falló** | Las listas muestran el estado con texto, pero en la grilla del calendario del administrador el estado de cada reserva se distingue solo por el color (INC-07) | `cp49-*-en-grises.png` |
| CP-50 | **Falló** | 19 pantallas en 360, 768 y 1280 px (57 capturas). A 360 px el panel del administrador se desborda (el botón de salir queda cortado) y "Mis reservas" del socio también (INC-08). A 1280 px tres tablas del administrador no entran en su tarjeta (INC-09) | `cp50/` |
| CP-51 | Aprobado | "Próximas", "Historial" y "Todas" del socio coinciden con sus 9 reservas en la base; cada una con instalación, día, horario y estado | `cp51-*.png` |
| CP-53 | No ejecutado | Lo aprueban la cátedra y el equipo en la RTF-03: se preparó el informe con los hallazgos ([rtf-03-diseno-visual.md](../rtf-03-diseno-visual.md)) y las capturas de todas las pantallas están en `evidencia/cp50/` | — |

Las incidencias (INC-07 a INC-13) están en [incidencias.md](../incidencias.md) con la plantilla del Apéndice G.

### Cómo se decidió cada resultado

- **Contraste (CP-47, CP-48).** El plan pide medirlo con un verificador (axe, Lighthouse). Acá se usa una medición propia sobre la página real: para cada texto visible se toman los colores que calcula el navegador, se compone el fondo con los de sus contenedores y se aplica la fórmula de WCAG 2.1. El mínimo es 4,5:1 para el texto normal y 3:1 para el texto grande, los íconos que son lo único que identifica un control y el contorno de los campos. Si el fondo es un degradado se toma su color menos favorable. No se exigen los íconos que acompañan a un texto que dice lo mismo, ni los controles deshabilitados. Conviene confirmarlo con Lighthouse (pasos más abajo).
- **Adaptación (CP-50).** Se mide si la página entera se desborda, si un botón o un título queda fuera de la pantalla o tapado por otro elemento, si un texto queda cortado o estrujado, si un importe se parte en dos renglones y si una tabla necesita desplazamiento propio. Las tablas con desplazamiento propio se aceptan en 360 y 768 px, que es lo esperable en un teléfono. A 1280 px se cuentan como problema. Dos patrones hechos a propósito no se cuentan: el carrusel de fotos de Instalaciones (se recorre con flechas) y el panel deslizante del registro. El menú plegado del administrador se comprueba abriéndolo con su botón. Las 57 capturas se revisaron a ojo además de medirse.
- **Estados (CP-49).** Se revisa cada etiqueta de estado de las pantallas del administrador y del socio, y se guardan las pantallas en escala de grises para ver si se distinguen sin color.

## Cómo repetirlo

Se usa un **dominio de GlassFish aparte** (`sigridcalidad`, puertos 9180 y 9148) y una **base aparte** (`sigrid_pruebas`): no se toca el dominio de trabajo ni la base `sigrid`. CP-36 programa un cambio de precios y por eso el recorrido se niega a correr contra `sigrid`.

1. Compilar: dentro de `SIGRID`, `mvn package -DskipTests` (con JDK 21).
2. Crear la base de prueba desde el esquema (trae los datos de la demo, anclados al día de hoy):

```bash
sed -e 's/^ CREATE DATABASE sigrid;/ CREATE DATABASE sigrid_pruebas;/' -e 's/^ USE sigrid;/ USE sigrid_pruebas;/' sigrid_schema.sql > /tmp/schema_pruebas.sql
mysql -uUSUARIO -p --default-character-set=utf8mb4 < /tmp/schema_pruebas.sql
```

3. Crear el dominio, la conexión a la base y desplegar. En PowerShell, con `$env:AS_JAVA` y `$env:JAVA_HOME` apuntando al JDK 21:

```powershell
asadmin create-domain --portbase 9100 --nopassword=true sigridcalidad
asadmin start-domain sigridcalidad
asadmin --port 9148 create-jdbc-connection-pool --datasourceclassname com.mysql.cj.jdbc.MysqlDataSource --restype javax.sql.DataSource --property 'user=USUARIO:password=CLAVE:databaseName=sigrid_pruebas:serverName=localhost:portNumber=3306:useSSL=false:allowPublicKeyRetrieval=true:serverTimezone=America/Argentina/Buenos_Aires' sigridPool
asadmin --port 9148 create-jdbc-resource --connectionpoolid sigridPool sigridJNDI
asadmin --port 9148 deploy --name SIGRID-oficial --contextroot SIGRID SIGRID\target\SIGRID-oficial.war
```

4. Correr los casos desde `docs/calidad/sistema`. `SIGRID_CLAVE` es la contraseña de las cuentas de prueba, que figura en `sigrid_schema.sql`. Tarda unos 5 minutos:

```bash
export SIGRID_CLAVE=... DB_USER=USUARIO DB_PASSWORD=CLAVE DB_NAME=sigrid_pruebas
node recorrido.mjs              # todos menos CP-45
node recorrido.mjs CP-36 CP-50  # solo algunos
```

5. CP-45 va al final, porque cambia los datos: cargar las 500.000 reservas con `docs/calidad/cp45/carga_500k.sql` (cambiando `USE sigrid_cc06;` por `USE sigrid_pruebas;`), reiniciar la aplicación (`asadmin --port 9148 disable SIGRID-oficial` y `enable`) y correr `node recorrido.mjs CP-45`. Si además se define `ASADMIN` (ruta de `asadmin.bat`) y `SERVER_LOG` (el `server.log` del dominio), informa la memoria del servidor y busca `OutOfMemoryError`.
6. Regenerar el reporte de V&V desde la raíz: `python docs/calidad/generar_reporte_vv.py`.
7. Al terminar: `asadmin stop-domain sigridcalidad`, `asadmin delete-domain sigridcalidad` y `DROP DATABASE sigrid_pruebas;`.

## Lo que tiene que hacer una persona

**Confirmar el contraste con Lighthouse.** Lo pide el plan; tarda unos 10 minutos:

1. Desplegar como en los pasos 1 a 3 (o usar el GlassFish de siempre) y abrir la aplicación en Edge o Chrome.
2. Iniciar sesión como socio (`socio.completo@demo.sigrid`) y abrir **Inicio**.
3. Apretar **F12**, ir a la pestaña **Lighthouse** (si no se ve, está en el botón `>>`), marcar solo **Accesibilidad**, elegir **Escritorio** y apretar **Analizar la carga de la página**.
4. En el resultado, abrir **Contraste**: ahí lista cada texto que no llega al mínimo. Anotar la puntuación y los elementos.
5. Repetir con **Mis reservas**, y con el administrador en **Calendario** y **Reservas**.
6. Si Lighthouse marca algo que esta medición no marcó (o al revés), avisar para revisarlo.

**CP-53 (RTF-03).** Es una revisión del diseño con la cátedra. Hay que completar fecha, participantes y decisión en [rtf-03-diseno-visual.md](../rtf-03-diseno-visual.md). Los hallazgos ya están cargados.

## Observaciones que no son incidencias

- **Panel del socio:** sigue diciendo "valoración" en 7 textos (por ejemplo "Valoración pendiente"). CC-04 se aplicó solo al administrador hasta que el equipo decida.
- **Dirección con la sesión:** la primera vez que se abre el sitio sin cookies, GlassFish agrega `;jsessionid=...` a la dirección. Es el comportamiento por defecto del servidor. Se puede desactivar dejando solo la cookie (`<tracking-mode>COOKIE</tracking-mode>` en `web.xml`) para que el identificador no quede en el historial ni en los enlaces copiados.
- **Reportes con 500.000 reservas:** abrir Reportes (30 días) tarda 3,6 s y pasar a 12 meses 10,3 s, con unos 500 MB del servidor. No es parte de CP-45 (que habla de listar y exportar), pero pasa de la meta de 5 s de RNF-07. Ya estaba anotado como pendiente: hay que pasar esos cálculos a consultas de agregación.
- **La dirección de la pantalla** sigue siendo `valoraciones-dashboard-admin.xhtml`. No se ve en la pantalla, solo en la barra de direcciones.

## Limitaciones

- Se probó solo con Edge (motor Chromium) en Windows. La numeración "Página N de M" de los PDF la dibuja Chromium; Firefox no la dibuja.
- La medición de contraste no es la de axe ni la de Lighthouse. Los textos sobre fotos no se pueden medir así, aunque en las pantallas del socio y del administrador no hubo ninguno.
- La medición de adaptación es automática y se completó mirando las capturas. Una superposición que no tape el centro de un control puede pasar desapercibida.
- Los estados de pasar el mouse por encima y del foco del teclado no se midieron (el foco lo cubre `ContrasteCssTest`).
