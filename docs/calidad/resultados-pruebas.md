# Resultados de las pruebas automatizadas de SIGRID

Documento del Plan de Garantía de Calidad. Reúne qué se probó, con qué resultado, cuánta parte del código ejercitan las pruebas y qué hallazgos aparecieron.

| Dato | Valor |
|---|---|
| Fecha de ejecución | 13/10/2026 |
| Rama | `calidad/pruebas-junit` |
| Proyecto Maven | `SIGRID/` (carpeta `D:\SIGRID\SIGRID`) |
| Java | JDK 21.0.10 (el código se compila con `source/target 11`) |
| Herramientas | JUnit Jupiter 5.10.3, Mockito 5.17.0, Maven Surefire 3.2.5, JaCoCo 0.8.12 |
| Qué se necesita | Solo Java y Maven. **No** hace falta MySQL, GlassFish ni red (la primera vez Maven descarga las herramientas). |

## 1. Cómo ejecutar las pruebas y dónde ver el informe

Desde la carpeta del proyecto Maven (`SIGRID/`, donde está el `pom.xml`):

```bash
JAVA_HOME="/c/Program Files/Java/jdk-21.0.10" mvn test
```

En PowerShell:

```powershell
$env:JAVA_HOME = "C:\Program Files\Java\jdk-21.0.10"
mvn test
```

Notas:

- La variable `JAVA_HOME` del sistema apunta a `C:\Program Files\Java\jdk-21`, una carpeta que no existe. Por eso se indica el JDK 21 real en cada comando. El `java` del PATH es el 25 y **no** debe usarse para las pruebas (las versiones de JaCoCo y Mockito usadas no lo soportan oficialmente).
- No usar `mvn clean`: la carpeta `SIGRID/target/` está versionada en git (388 archivos) y `clean` la borraría.
- Una sola clase de prueba: `mvn test -Dtest=TarifasServicioTest`.
- Ver las pruebas deshabilitadas fallar (los posibles defectos de la sección 5):
  `mvn test -Djunit.jupiter.conditions.deactivate="org.junit.*DisabledCondition"`.

Dónde ver los resultados:

| Qué | Dónde |
|---|---|
| Informe de cobertura (abrir en el navegador) | `SIGRID/target/site/jacoco/index.html` |
| Cobertura en CSV / XML | `SIGRID/target/site/jacoco/jacoco.csv` y `jacoco.xml` |
| Resultado de cada clase de prueba | `SIGRID/target/surefire-reports/` (cada caso con su código del plan, por ejemplo `CP-22: ...`) |
| Reporte de V&V del plan (Apéndice F: tablas 40, 41 y 42) | [reporte-vv.md](reporte-vv.md), generado con `python docs/calidad/generar_reporte_vv.py` después de `mvn test` |

## 2. Resumen de resultados

| Medida | Valor |
|---|---|
| Clases de prueba | 26 |
| Pruebas (métodos) | 609 |
| Ejecuciones (casos, contando cada valor de las parametrizadas) | 668 |
| Pasan | 662 |
| Deshabilitadas (defectos D-1 a D-5) | 6 |
| Fallan | 0 |
| Tiempo total | unos segundos (la mayor parte es el hash PBKDF2 de 120 000 iteraciones) |

Las pruebas deshabilitadas (seis, que corresponden a cinco defectos) **no son pruebas rotas**: describen la regla esperada y el código actual no la cumple (sección 5.1). D-2 y D-3 están confirmadas por el responsable del proyecto; D-1 es un posible defecto, y D-4 y D-5 son discrepancias entre el plan de la cátedra y el código que el equipo todavía no resolvió. Se dejaron con `@Disabled` y una explicación, tal como pide la regla de calidad de no modificar una prueba para que "pase" cuando el código está mal. Cada una se ejecutó activada para comprobar que falla por el motivo que se declara.

## 3. Pruebas por clase

Cada nombre describe un único comportamiento. En las pruebas parametrizadas, "N casos" son los valores distintos con que se ejecuta la misma prueba (por ejemplo, los puntajes 1, 2, 3, 4 y 5).

### `util.ComprobanteArchivosTest` (21 pruebas, 29 casos)

| Prueba | Qué verifica | Resultado |
|---|---|---|
| `detectaUnPdfPorSusPrimerosBytes` | Detecta un PDF por sus primeros bytes | Pasa |
| `detectaUnPngPorSusPrimerosBytes` | Detecta un PNG por sus primeros bytes | Pasa |
| `detectaUnJpegPorSusPrimerosBytes` | Detecta un JPEG por sus primeros bytes | Pasa |
| `elTipoSaleDeLosBytesYNoDelNombreDelArchivo` | El tipo sale de los bytes y no del nombre del archivo | Pasa |
| `rechazaUnEjecutableRenombradoComoPdf` | Rechaza un ejecutable renombrado como PDF | Pasa |
| `rechazaUnTextoPlano` | Rechaza un texto plano | Pasa |
| `rechazaUnArchivoVacio` | Rechaza un archivo vacío | Pasa |
| `noReconoceUnPdfDeSoloCuatroBytes` | No reconoce un PDF de solo cuatro bytes | Pasa |
| `reconoceUnPdfDeCincoBytes` | Reconoce un PDF de cinco bytes | Pasa |
| `noReconoceUnPngDeSoloOchoBytes` | No reconoce un PNG de solo ocho bytes | Pasa |
| `noReconoceUnJpegDeSoloTresBytes` | No reconoce un JPEG de solo tres bytes | Pasa |
| `rechazaUnJpegCuyoTercerByteNoEsFF` | Rechaza un JPEG cuyo tercer byte no es FF | Pasa |
| `asignaElTipoMimeSegunLaExtensionDelNombreGuardado` | Asigna el tipo MIME según la extensión del nombre guardado | Pasa |
| `guardaElArchivoYDevuelveSuRuta` | Guarda el archivo y devuelve su ruta | Pasa |
| `laRutaEsNulaSiElArchivoNoExiste` | La ruta es nula si el archivo no existe | Pasa |
| `laRutaEsNulaSiElNombreIntentaSalirseDeLaCarpeta` | La ruta es nula si el nombre intenta salirse de la carpeta | Pasa |
| `borraElArchivoGuardado` | Borra el archivo guardado | Pasa |
| `borrarUnArchivoInexistenteNoLanzaExcepcion` | Borrar un archivo inexistente no lanza excepción | Pasa |
| `rechazaUnaFirmaConUnSoloByteIncorrecto` | Rechaza una firma con un solo byte incorrecto | Pasa (9 casos) |
| `sinPropiedadGuardaEnSigridComprobantesDentroDeLaCarpetaDelUsuario` | Sin propiedad guarda en sigrid comprobantes dentro de la carpeta del usuario | Pasa |
| `borrarNoLanzaExcepcionSiNoSePuedeEliminarElElemento` | Borrar no lanza excepción si no se puede eliminar el elemento | Pasa |

### `util.PasswordUtilTest` (14 pruebas, 14 casos, 1 deshabilitada)

| Prueba | Qué verifica | Resultado |
|---|---|---|
| `generaElHashConTresPartesIteracionesSalYHash` | Genera el hash con tres partes iteraciones sal y hash | Pasa |
| `dosHashesDeLaMismaContrasenaUsanSalDistinta` | Dos hashes de la misma contraseña usan sal distinta | Pasa |
| `dosHashesDeLaMismaContrasenaSeVerificanAmbos` | Dos hashes de la misma contraseña se verifican ambos | Pasa |
| `verificaLaContrasenaCorrecta` | Verifica la contraseña correcta | Pasa |
| `rechazaUnaContrasenaIncorrecta` | Rechaza una contraseña incorrecta | Pasa |
| `distingueMayusculasDeMinusculas` | Distingue mayúsculas de minúsculas | Pasa |
| `verificaContrasenasConAcentosYEnie` | Verifica contraseñas con acentos y eñe | Pasa |
| `rechazaUnaContrasenaNulaSinLanzarExcepcion` | Rechaza una contraseña nula sin lanzar excepción | Pasa |
| `rechazaUnHashNuloSinLanzarExcepcion` | Rechaza un hash nulo sin lanzar excepción | Pasa |
| `rechazaUnHashConCantidadDePartesIncorrecta` | Rechaza un hash con cantidad de partes incorrecta | Pasa |
| `rechazaUnHashVacio` | Rechaza un hash vacío | Pasa |
| `laContrasenaVaciaSeHasheaYSoloVerificaConVacia` | La contraseña vacía se hashea y solo verifica con vacía | Pasa |
| `generarHashConContrasenaNulaLanzaNullPointerException` | Generar hash con contraseña nula lanza null pointer exception | Pasa |
| `rechazaUnHashMalFormadoSinLanzarExcepcion` | Rechaza un hash mal formado sin lanzar excepción | Deshabilitada (posible defecto D-1) |

### `bean.AdminNavBeanTest` (2 pruebas, 2 casos)

| Prueba | Qué verifica | Resultado |
|---|---|---|
| `CP-41: el menú dice "Grado de satisfacción" y no "Valoraciones"` | Cp 41 el menú dice grado de satisfacción y no valoraciones | Pasa |
| `laSeccionRenombradaSigueEnLaMismaDireccion` | La seccion renombrada sigue en la misma direccion | Pasa |

### `servicio.CambiosReservaServicioListadosTest` (11 pruebas, 11 casos)

| Prueba | Qué verifica | Resultado |
|---|---|---|
| `elPedidoDiceConCuantasHorasDeAnticipacionSeHizo` | El pedido dice con cuantas horas de anticipación se hizo | Pasa |
| `conMasDe72HorasLaAnticipacionSeDiceEnDias` | Con más de 72 horas la anticipación se dice en días | Pasa |
| `elPedidoDeUnaReservaConCreditoDiceHastaCuandoVale` | El pedido de una reserva con crédito dice hasta cuando vale | Pasa |
| `elPedidoMuestraLaEsperaYElMonto` | El pedido muestra la espera y el monto | Pasa |
| `elPedidoMuestraElTurnoActualYElNuevoSiEsReprogramacion` | El pedido muestra el turno actual y el nuevo si es reprogramación | Pasa |
| `unaCancelacionNoTieneTurnoNuevo` | Una cancelación no tiene turno nuevo | Pasa |
| `unCreditoQueVenceHoyEstaVigenteYPorVencer` | Un crédito que vence hoy está vigente y por vencer | Pasa |
| `unCreditoQueVenceEnSieteDiasEstaPorVencerYEnOchoNo` | Un crédito que vence en siete días está por vencer y en ocho no | Pasa |
| `elPlazoDiceCuantosDiasQuedan` | El plazo dice cuantos días quedan | Pasa |
| `unCreditoVencidoNoEstaVigenteNiPorVencerYDiceHaceCuanto` | Un crédito vencido no está vigente ni por vencer y dice hace cuanto | Pasa |
| `laReprogramacionMuestraDeQueTurnoAQueTurnoYCuando` | La reprogramación muestra de que turno a que turno y cuando | Pasa |

### `servicio.CambiosReservaServicioTest` (55 pruebas, 59 casos)

| Prueba | Qué verifica | Resultado |
|---|---|---|
| `cp22_cancelaAutomaticamenteConCredito` | Cp 22 cancela automaticamente con crédito | Pasa |
| `cp22_noCreaPedidoNiInvolucraAlAdministrador` | Cp 22 no crea pedido ni involucra al administrador | Pasa |
| `cp22_noCancelaDosVeces` | Cp 22 no cancela dos veces | Pasa |
| `cp23_noCancelaA47Horas59Minutos` | Cp 23 no cancela a 47 horas 59 minutos | Pasa |
| `cp24_cancelaExactamenteA48Horas` | Cp 24 cancela exactamente a 48 horas | Pasa |
| `cp21_noCancelaLaReservaDeOtroSocio` | Cp 21 no cancela la reserva de otro socio | Pasa |
| `cp25_conElCreditoDeLaCancelacionAutomaticaSeReprogramaConAprobacion` | Cp 25 con el crédito de la cancelación automatica se reprograma con aprobacion | Pasa |
| `unTurnoQueEmpiezaEn47HorasEstaFueraDePlazo` | Un turno que empieza en 47 horas está fuera de plazo | Pasa |
| `unTurnoQueEmpiezaEn48HorasExactasEstaEnPlazo` | Un turno que empieza en 48 horas exactas está en plazo | Pasa |
| `unTurnoQueEmpiezaEn49HorasEstaEnPlazo` | Un turno que empieza en 49 horas está en plazo | Pasa |
| `unTurnoQueEmpiezaEn47Horas59MinutosEstaFueraDePlazo` | Un turno que empieza en 47 horas 59 minutos está fuera de plazo | Pasa |
| `rechazaPedirLaCancelacionConMenosDe48Horas` | Rechaza pedir la cancelación con menos de 48 horas | Pasa |
| `rechazaPedirLaCancelacionConUnMinutoMenosDe48Horas` | Rechaza pedir la cancelación con un minuto menos de 48 horas | Pasa |
| `rechazaElPedidoDeUnaReservaQueEsDeOtroSocio` | Rechaza el pedido de una reserva que es de otro socio | Pasa |
| `rechazaElPedidoDeUnaReservaInexistente` | Rechaza el pedido de una reserva inexistente | Pasa |
| `rechazaElPedidoSinMotivo` | Rechaza el pedido sin motivo | Pasa (3 casos) |
| `rechazaUnSegundoPedidoPendienteParaLaMismaReserva` | Rechaza un segundo pedido pendiente para la misma reserva | Pasa |
| `rechazaPedirLaCancelacionDeUnaReservaQueNoEstaConfirmada` | Rechaza pedir la cancelación de una reserva que no está confirmada | Pasa |
| `elMotivoDeLaCancelacionSeRecortaA255CaracteresEnElHistorial` | El motivo de la cancelación se recorta a 255 caracteres en el historial | Pasa |
| `elMotivoDeLaReprogramacionSeRecortaA255Caracteres` | El motivo de la reprogramación se recorta a 255 caracteres | Pasa |
| `aceptaReprogramarConUnCreditoVigenteAUnTurnoLibreDeLaMismaInstalacion` | Acepta reprogramar con un crédito vigente a un turno libre de la misma instalación | Pasa |
| `aceptaUsarElCreditoElUltimoDiaDeSuVigencia` | Acepta usar el crédito el último día de su vigencia | Pasa |
| `rechazaReprogramarConElCreditoVencidoAyer` | Rechaza reprogramar con el crédito vencido ayer | Pasa |
| `rechazaReprogramarUnaReservaConfirmadaQueNoTieneCredito` | Rechaza reprogramar una reserva confirmada que no tiene crédito | Pasa |
| `rechazaReprogramarAUnTurnoDeOtraInstalacion` | Rechaza reprogramar a un turno de otra instalación | Pasa |
| `rechazaReprogramarAUnTurnoQueYaEstaReservado` | Rechaza reprogramar a un turno que ya está reservado | Pasa |
| `aceptaReprogramarAUnTurnoQueEmpiezaEnExactamente48Horas` | Acepta reprogramar a un turno que empieza en exactamente 48 horas | Pasa |
| `rechazaReprogramarAUnTurnoQueEmpiezaEn47Horas59Minutos` | Rechaza reprogramar a un turno que empieza en 47 horas 59 minutos | Pasa |
| `rechazaReprogramarSinElegirElTurnoNuevo` | Rechaza reprogramar sin elegir el turno nuevo | Pasa |
| `alAprobarUnaCancelacionLaReservaQuedaCanceladaConCreditoDe30Dias` | Al aprobar una cancelación la reserva queda cancelada con crédito de 30 días | Pasa |
| `alAprobarUnaCancelacionDejaElTurnoAnteriorEnElHistorial` | Al aprobar una cancelación deja el turno anterior en el historial | Pasa |
| `unaReservaCanceladaSinPagoNoDejaCredito` | Una reserva cancelada sin pago no deja crédito | Pasa |
| `noApruebaUnPedidoQueYaEstabaResuelto` | No aprueba un pedido que ya estaba resuelto | Pasa |
| `noApruebaUnPedidoInexistente` | No aprueba un pedido inexistente | Pasa |
| `noApruebaLaCancelacionDeUnaReservaQueYaNoEstaConfirmada` | No aprueba la cancelación de una reserva que ya no está confirmada | Pasa |
| `alAprobarUnaReprogramacionLaReservaVuelveAConfirmadaYConsumeElCredito` | Al aprobar una reprogramación la reserva vuelve a confirmada y consume el crédito | Pasa |
| `noApruebaUnaReprogramacionSiElCreditoVencioAntesDelPedido` | No aprueba una reprogramación si el crédito venció antes del pedido | Pasa |
| `noApruebaUnaReprogramacionSiElTurnoPedidoSeOcupoMientrasTanto` | No aprueba una reprogramación si el turno pedido se ocupó mientras tanto | Pasa |
| `alRechazarUnPedidoLaReservaQuedaComoEstabaYSeGuardaElMotivo` | Al rechazar un pedido la reserva queda como estaba y se guarda el motivo | Pasa |
| `rechazarUnaReprogramacionDejaElCreditoVigenteHastaSuVencimiento` | Rechazar una reprogramación deja el crédito vigente hasta su vencimiento | Pasa |
| `rechazarSinMotivoNoGuardaMotivo` | Rechazar sin motivo no guarda motivo | Pasa |
| `noRechazaUnPedidoQueYaEstabaResuelto` | No rechaza un pedido que ya estaba resuelto | Pasa |
| `laCancelacionPorElPredioNoExige48HorasYDejaCreditoDe30Dias` | La cancelación por el predio no exige 48 horas y deja crédito de 30 días | Pasa |
| `laCancelacionPorElPredioExigeUnMotivo` | La cancelación por el predio exige un motivo | Pasa (3 casos) |
| `laCancelacionPorElPredioRechazaUnTurnoQueYaEmpezo` | La cancelación por el predio rechaza un turno que ya empezó | Pasa |
| `laCancelacionPorElPredioRechazaUnaReservaNoConfirmada` | La cancelación por el predio rechaza una reserva no confirmada | Pasa |
| `rechazaReprogramarAUnTurnoDeUnaInstalacionNoHabilitada` | Rechaza reprogramar a un turno de una instalación no habilitada | Pasa |
| `rechazaReprogramarAUnTurnoQueYaPaso` | Rechaza reprogramar a un turno que ya paso | Pasa |
| `rechazaReprogramarSinElegirLaFecha` | Rechaza reprogramar sin elegir la fecha | Pasa |
| `rechazaReprogramarUnaReservaCanceladaSinFechaLimiteDeCredito` | Rechaza reprogramar una reserva cancelada sin fecha limite de crédito | Pasa |
| `noApruebaUnaReprogramacionDeUnaReservaCanceladaSinCredito` | No aprueba una reprogramación de una reserva cancelada sin crédito | Pasa |
| `noApruebaUnaReprogramacionDeUnaReservaQueNoEstaCancelada` | No aprueba una reprogramación de una reserva que no está cancelada | Pasa |
| `noRechazaUnPedidoInexistente` | No rechaza un pedido inexistente | Pasa |
| `rechazarConMotivoNuloNoGuardaMotivo` | Rechazar con motivo nulo no guarda motivo | Pasa |
| `laCancelacionPorElPredioRechazaUnaReservaInexistente` | La cancelación por el predio rechaza una reserva inexistente | Pasa |

### `servicio.HorarioServicioTest` (37 pruebas, 44 casos)

| Prueba | Qué verifica | Resultado |
|---|---|---|
| `dentroDeLaFranjaEstaAbiertaYDiceHastaCuando` | Dentro de la franja está abierta y dice hasta cuando | Pasa |
| `abreExactamenteALaHoraDeApertura` | Abre exactamente a la hora de apertura | Pasa |
| `unMinutoAntesDeAbrirTodaviaEstaCerrada` | Un minuto antes de abrir todavía está cerrada | Pasa |
| `unMinutoAntesDelCierreSigueAbierta` | Un minuto antes del cierre sigue abierta | Pasa |
| `aLaHoraDeCierreYaEstaCerradaYDiceCuandoAbreLaSemanaSiguiente` | A la hora de cierre ya está cerrada y dice cuando abre la semana siguiente | Pasa |
| `siAbreMasTardeHoyDiceLaHora` | Si abre más tarde hoy dice la hora | Pasa |
| `siHayDosFranjasMasTardeDiceLaPrimera` | Si hay dos franjas más tarde dice la primera | Pasa |
| `siAbreMananaLoDiceAsi` | Si abre mañana lo dice asi | Pasa |
| `siAbreOtroDiaDiceElDiaYLaFecha` | Si abre otro día dice el día y la fecha | Pasa |
| `unaFranjaDeUnDiaPuntualSeSumaALasSemanales` | Una franja de un día puntual se suma a las semanales | Pasa |
| `laFranjaDeUnDiaPuntualPuedeSerLaProximaApertura` | La franja de un día puntual puede ser la proxima apertura | Pasa |
| `buscaLaProximaAperturaHastaCatorceDiasAdelante` | Busca la proxima apertura hasta catorce días adelante | Pasa |
| `siLaProximaAperturaEstaAMasDeCatorceDiasDiceSoloCerrada` | Si la proxima apertura está a más de catorce días dice solo cerrada | Pasa |
| `sinHorariosCargadosLoDice` | Sin horarios cargados lo dice | Pasa |
| `elDiaDeHoyMuestraComoEstaAhora` | El día de hoy muestra como está ahora | Pasa |
| `otroDiaMuestraTodasSusFranjas` | Otro día muestra todas sus franjas | Pasa |
| `unDiaSinFranjasDiceCerradaEseDia` | Un día sin franjas dice cerrada ese día | Pasa |
| `agrupaLosDiasConElMismoHorarioYLosAbrevia` | Agrupa los días con el mismo horario y los abrevia | Pasa |
| `despuesDeLasSemanalesVanLosDiasPuntualesQueTodaviaNoPasaron` | Después de las semanales van los días puntuales que todavía no pasaron | Pasa |
| `elTextoDeDiasAbreviaLasRachasDeTresOMas` | El texto de días abrevia las rachas de tres o más | Pasa (8 casos) |
| `agregaUnaFilaPorCadaDiaElegido` | Agrega una fila por cada día elegido | Pasa |
| `rechazaUnaInstalacionInexistente` | Rechaza una instalación inexistente | Pasa |
| `rechazaUnaInstalacionQueSeReservaPorTurnos` | Rechaza una instalación que se reserva por turnos | Pasa |
| `exigeLaHoraDeAperturaYLaDeCierre` | Exige la hora de apertura y la de cierre | Pasa |
| `elCierreTieneQueSerPosteriorALaApertura` | El cierre tiene que ser posterior a la apertura | Pasa |
| `exigeAlMenosUnDia` | Exige al menos un día | Pasa |
| `rechazaUnaFranjaQueSeSuperponeConOtraDelMismoDia` | Rechaza una franja que se superpone con otra del mismo día | Pasa |
| `unaFranjaQueEmpiezaJustoCuandoOtraTerminaNoSeSuperpone` | Una franja que empieza justo cuando otra termina no se superpone | Pasa |
| `unaFranjaQueTerminaJustoCuandoOtraEmpiezaNoSeSuperpone` | Una franja que termina justo cuando otra empieza no se superpone | Pasa |
| `lasFranjasDeOtroDiaNoSeSuperponen` | Las franjas de otro día no se superponen | Pasa |
| `siUnSoloDiaChocaNoSeAgregaNingunoDeLosOtros` | Si un solo día choca no se agrega ninguno de los otros | Pasa |
| `agregaLaFranjaDeUnDiaPuntual` | Agrega la franja de un día puntual | Pasa |
| `elDiaPuntualPuedeSerHoyPeroNoUnDiaAnterior` | El día puntual puede ser hoy pero no un día anterior | Pasa |
| `elDiaPuntualEsObligatorio` | El día puntual es obligatorio | Pasa |
| `elDiaPuntualNoSePuedeSuperponerConElHorarioSemanalDeEseDia` | El día puntual no se puede superponer con el horario semanal de ese día | Pasa |
| `elDiaPuntualPuedeIrDespuesDelHorarioSemanalSinTocarlo` | El día puntual puede ir después del horario semanal sin tocarlo | Pasa |
| `quitaSoloLasFranjasDeEsaInstalacion` | Quita solo las franjas de esa instalación | Pasa |

### `servicio.InstalacionesServicioTest` (44 pruebas, 47 casos)

| Prueba | Qué verifica | Resultado |
|---|---|---|
| `poneEnMantenimientoUnaCanchaSinReservasEnElPeriodo` | Pone en mantenimiento una cancha sin reservas en el periodo | Pasa |
| `laBajaDefinitivaNoGuardaFechaDeReactivacion` | La baja definitiva no guarda fecha de reactivacion | Pasa |
| `recortaElMotivoA255Caracteres` | Recorta el motivo a 255 caracteres | Pasa |
| `rechazaSacarDeServicioUnaInstalacionInexistente` | Rechaza sacar de servicio una instalación inexistente | Pasa |
| `rechazaSacarDeServicioUnaInstalacionYaDadaDeBaja` | Rechaza sacar de servicio una instalación ya dada de baja | Pasa |
| `rechazaPonerEnMantenimientoUnaInstalacionQueYaEstaEnMantenimiento` | Rechaza poner en mantenimiento una instalación que ya está en mantenimiento | Pasa |
| `exigeElMotivo` | Exige el motivo | Pasa (3 casos) |
| `rechazaUnaFechaDeReactivacionAnteriorAHoy` | Rechaza una fecha de reactivacion anterior a hoy | Pasa |
| `aceptaUnaFechaDeReactivacionDeHoy` | Acepta una fecha de reactivacion de hoy | Pasa |
| `bloqueaSiHayUnaSolicitudConComprobantePorConfirmar` | Bloquea si hay una solicitud con comprobante por confirmar | Pasa |
| `laSolicitudPorConfirmarBloqueaAunqueSeMarqueCancelarReservas` | La solicitud por confirmar bloquea aunque se marque cancelar reservas | Pasa |
| `pideMarcarLaOpcionDeCancelarSiHayUnaReserva` | Pide marcar la opcion de cancelar si hay una reserva | Pasa |
| `pideMarcarLaOpcionDeCancelarSiHayVariasReservas` | Pide marcar la opcion de cancelar si hay varias reservas | Pasa |
| `alCancelarLasConfirmadasVanPorElPredioYLasQueEsperanPagoSeLiberan` | Al cancelar las confirmadas van por el predio y las que esperan pago se liberan | Pasa |
| `laCausaDeUnaBajaDefinitivaEmpiezaConBajaDe` | La causa de una baja definitiva empieza con baja de | Pasa |
| `siFallaLaCancelacionDeUnaReservaLaInstalacionSigueEnServicio` | Si falla la cancelación de una reserva la instalación sigue en servicio | Pasa |
| `unaReservaQueYaEmpezoHoyNoCuentaComoConflicto` | Una reserva que ya empezó hoy no cuenta como conflicto | Pasa |
| `clasificaLosConflictosEnConfirmadasEsperandoPagoYPorConfirmar` | Clasifica los conflictos en confirmadas esperando pago y por confirmar | Pasa |
| `sinFechaDeFinBuscaConflictosHastaCincoAniosAdelante` | Sin fecha de fin busca conflictos hasta cinco anios adelante | Pasa |
| `reactivarVuelveAHabilitarYLimpiaLosDatosDeLaBaja` | Reactivar vuelve a habilitar y limpia los datos de la baja | Pasa |
| `laBajaDefinitivaNoSeReviertePorReactivar` | La baja definitiva no se revierte por reactivar | Pasa |
| `noReactivaUnaInstalacionQueYaEstaHabilitada` | No reactiva una instalación que ya está habilitada | Pasa |
| `noReactivaUnaInstalacionInexistente` | No reactiva una instalación inexistente | Pasa |
| `editaLaDescripcionSinEspaciosYLaCapacidad` | Edita la descripcion sin espacios y la capacidad | Pasa |
| `unaDescripcionEnBlancoQuedaNulaYLaCapacidadVaciaEsSinLimite` | Una descripcion en blanco queda nula y la capacidad vacía es sin limite | Pasa |
| `rechazaUnaCapacidadQueNoEsMayorACero` | Rechaza una capacidad que no es mayor a cero | Pasa (2 casos) |
| `aceptaUnaDescripcionDeExactamente1000Caracteres` | Acepta una descripcion de exactamente 1000 caracteres | Pasa |
| `rechazaUnaDescripcionDeMasDe1000Caracteres` | Rechaza una descripcion de más de 1000 caracteres | Pasa |
| `laDemandaSumaReservasConfirmadasEIngresosSoloDeLasConfirmadasEnLosUltimos30Dias` | La demanda suma reservas confirmadas e ingresos solo de las confirmadas en los ultimos 30 días | Pasa |
| `laOcupacionEsElPorcentajeDeLasFranjasDeLosUltimos30Dias` | La ocupacion es el porcentaje de las franjas de los ultimos 30 días | Pasa |
| `laOcupacionNoPasaDe100` | La ocupacion no pasa de 100 | Pasa |
| `sinTurnosLaOcupacionEsCero` | Sin turnos la ocupacion es cero | Pasa |
| `elRankingVaDeLaMasALaMenosSolicitadaConPorcentajesSobreLaPrimera` | El ranking va de la más a la menos solicitada con porcentajes sobre la primera | Pasa |
| `sinReservasElRankingMuestraCeroPorCiento` | Sin reservas el ranking muestra cero por ciento | Pasa |
| `losPreciosSeMuestranPorTipoDeSocioYLasDeAccesoLibreNoTienenPrecio` | Los precios se muestran por tipo de socio y las de acceso libre no tienen precio | Pasa |
| `marcaConOrdenIncorrectoLaFilaEnQueElAlumnoPagaMasQueElExterno` | Marca con orden incorrecto la fila en que el alumno paga más que el externo | Pasa |
| `noHayFichaDeUnaInstalacionInexistente` | No hay ficha de una instalación inexistente | Pasa |
| `laFichaDeUnaCanchaHabilitadaMuestraEstadoTurnosYOcupacion` | La ficha de una cancha habilitada muestra estado turnos y ocupacion | Pasa |
| `laFichaMuestraLaReactivacionEstimadaDeUnMantenimiento` | La ficha muestra la reactivacion estimada de un mantenimiento | Pasa |
| `laFichaAvisaQueLaFechaEstimadaDeReactivacionYaPaso` | La ficha avisa que la fecha estimada de reactivacion ya paso | Pasa |
| `laFichaDeUnaBajaDefinitivaLoDice` | La ficha de una baja definitiva lo dice | Pasa |
| `laFichaMuestraSoloLasTresProximasReservasQueTodaviaNoTerminaron` | La ficha muestra solo las tres proximas reservas que todavía no terminaron | Pasa |
| `cp36_cadaPrecioMuestraDesdeCuandoRigeYElProximoCambio` | Cp 36 cada precio muestra desde cuando rige y el proximo cambio | Pasa |
| `cp36_unPrecioQueTodaviaNoRigeSeVeComoProximoCambio` | Cp 36 un precio que todavía no rige se ve como proximo cambio | Pasa |

### `servicio.MembresiasServicioListadosTest` (16 pruebas, 16 casos)

| Prueba | Qué verifica | Resultado |
|---|---|---|
| `ordenaPendientesVigentesVencidasYCanceladasConservandoElOrdenPorFecha` | Ordena pendientes vigentes vencidas y canceladas conservando el orden por fecha | Pasa |
| `unaVigenteQueVenceHoyOMananaLoDiceYEstaPorVencer` | Una vigente que vence hoy o mañana lo dice y está por vencer | Pasa |
| `unaVigenteEstaPorVencerHastaSieteDiasInclusive` | Una vigente está por vencer hasta siete días inclusive | Pasa |
| `unaPendienteConComprobanteDiceQueHayQueValidarlo` | Una pendiente con comprobante dice que hay que validarlo | Pasa |
| `unaPendienteSinComprobanteEstaEsperandolo` | Una pendiente sin comprobante está esperandolo | Pasa |
| `unaCanceladaPorComprobanteDenegadoLoDiceYUnaBajaTambien` | Una cancelada por comprobante denegado lo dice y una baja tambien | Pasa |
| `unaVencidaAvisaQueElCarnetSeCongelaSiNoRenovo` | Una vencida avisa que el carnet se congela si no renovó | Pasa |
| `laFilaMuestraElSocioSusInicialesYSuTipo` | La fila muestra el socio sus iniciales y su tipo | Pasa |
| `laListaDeProximosVencimientosPideAlDaoDesdeHoyHastaLaFechaLimite` | La lista de próximos vencimientos pide al dao desde hoy hasta la fecha limite | Pasa |
| `elIngresoDelMesSeComparaConElMesAnteriorYSeReparteEntreLosTipos` | El ingreso del mes se compara con el mes anterior y se reparte entre los tipos | Pasa |
| `siElMesBajoLaComparacionEsNegativa` | Si el mes bajo la comparacion es negativa | Pasa |
| `siElMesAnteriorNoTuvoIngresosNoHayPorcentaje` | Si el mes anterior no tuvo ingresos no hay porcentaje | Pasa |
| `sinIngresosEnNingunMesLoDice` | Sin ingresos en ningún mes lo dice | Pasa |
| `lasCuotasSonLaTarifaVigenteDeCadaTipoONulaSiNoHay` | Las cuotas son la tarifa vigente de cada tipo o nula si no hay | Pasa |
| `losTiposDeSocioSonLosNombresDeLasCategorias` | Los tipos de socio son los nombres de las categorias | Pasa |
| `cp36_cadaCuotaMuestraDesdeCuandoRigeYElProximoCambio` | Cp 36 cada cuota muestra desde cuando rige y el proximo cambio | Pasa |

### `servicio.MembresiasServicioTest` (24 pruebas, 24 casos, 2 deshabilitadas)

| Prueba | Qué verifica | Resultado |
|---|---|---|
| `laMembresiaEmpiezaElDiaDeLaValidacionYVenceUnMesDespues` | La membresía empieza el día de la validación y vence un mes después | Pasa |
| `siSePagaDespuesDelVencimientoLaMembresiaEmpiezaElDiaDelPago` | Si se paga después del vencimiento la membresía empieza el día del pago | Pasa |
| `unMesDesdeElTreintaYUnoDeEneroTerminaElUltimoDiaDeFebrero` | Un mes desde el treinta y uno de enero termina el último día de febrero | Pasa |
| `alAceptarElSocioQuedaActivoYSuCarnetSeActiva` | Al aceptar el socio queda activo y su carnet se activa | Pasa |
| `alAceptarElPagoQuedaConfirmadoConQuienLoValido` | Al aceptar el pago queda confirmado con quién lo validó | Pasa |
| `alAceptarSinCarnetNoFallaYElSocioQuedaActivo` | Al aceptar sin carnet no falla y el socio queda activo | Pasa |
| `laRenovacionPorAdelantadoNuncaEmpiezaAntesDeQueTermineLaVigente` | La renovación por adelantado nunca empieza antes de que termine la vigente | Pasa |
| `laRenovacionPorAdelantadoMantieneElMismoDiaDelMes` | La renovación por adelantado mantiene el mismo día del mes | Deshabilitada (posible defecto D-3) |
| `siSePagaElMismoDiaDelVencimientoLaNuevaEmpiezaEseDia` | Si se paga el mismo día del vencimiento la nueva empieza ese día | Deshabilitada (posible defecto D-3) |
| `noAceptaUnaSolicitudQueYaEstabaResuelta` | No acepta una solicitud que ya estaba resuelta | Pasa |
| `noAceptaSiElPagoYaFueValidado` | No acepta si el pago ya fue validado | Pasa |
| `noAceptaUnaSolicitudSinComprobante` | No acepta una solicitud sin comprobante | Pasa |
| `noAceptaUnaSolicitudInexistente` | No acepta una solicitud inexistente | Pasa |
| `alDenegarElPagoQuedaRechazadoYLaSolicitudCancelada` | Al denegar el pago queda rechazado y la solicitud cancelada | Pasa |
| `noDeniegaUnaSolicitudQueYaEstabaResuelta` | No deniega una solicitud que ya estaba resuelta | Pasa |
| `cancelarLaUnicaVigenteCongelaAlSocio` | Cancelar la única vigente congela al socio | Pasa |
| `cancelarUnaVigenteNoCongelaAlSocioSiTieneOtraRenovada` | Cancelar una vigente no congela al socio si tiene otra renovada | Pasa |
| `noCancelaUnaMembresiaQueNoEstaVigente` | No cancela una membresía que no está vigente | Pasa |
| `alVencerLaUnicaVigenteElSocioQuedaNoActivoYElCarnetInactivo` | Al vencer la única vigente el socio queda no activo y el carnet inactivo | Pasa |
| `alVencerNoCongelaAlSocioQueYaRenovo` | Al vencer no congela al socio que ya renovó | Pasa |
| `alVencerCongelaACadaSocioQueQuedoSinMembresia` | Al vencer congela a cada socio que quedó sin membresía | Pasa |
| `alVencerNoFallaSiElSocioNoTieneCarnet` | Al vencer no falla si el socio no tiene carnet | Pasa |
| `sinMembresiasVencidasNoCambiaNada` | Sin membresías vencidas no cambia nada | Pasa |
| `noCancelaUnaMembresiaInexistente` | No cancela una membresía inexistente | Pasa |

### `servicio.PanelSocioServicioAvisosTest` (25 pruebas, 28 casos)

| Prueba | Qué verifica | Resultado |
|---|---|---|
| `avisaQueLaMembresiaEstaVencidaYQueNoSePuedeReservar` | Avisa que la membresía está vencida y que no se puede reservar | Pasa |
| `unSocioQueNuncaTuvoMembresiaRecibeElAvisoDeQueTodaviaNoLaTiene` | Un socio que nunca tuvo membresía recibe el aviso de que todavía no la tiene | Pasa |
| `avisaCuandoLaMembresiaVenceEnSieteDiasOMenos` | Avisa cuando la membresía vence en siete días o menos | Pasa (4 casos) |
| `noAvisaSiLaMembresiaVenceEnOchoDias` | No avisa si la membresía vence en ocho días | Pasa |
| `avisaDeLaValoracionPendienteDeUnTurnoTerminado` | Avisa de la valoración pendiente de un turno terminado | Pasa |
| `sinComprobanteAvisaHastaQueHoraTieneParaSubirlo` | Sin comprobante avisa hasta que hora tiene para subirlo | Pasa |
| `conComprobanteAvisaQueSeEstaRevisando` | Con comprobante avisa que se está revisando | Pasa |
| `unTurnoConfirmadoDeMananaAvisaQueEsManana` | Un turno confirmado de mañana avisa que es mañana | Pasa |
| `unaConfirmadaLejanaYRecienteAvisaQueSeConfirmo` | Una confirmada lejana y reciente avisa que se confirmo | Pasa |
| `unaConfirmadaLejanaConfirmadaHaceMasDeUnaSemanaYaNoAvisa` | Una confirmada lejana confirmada hace más de una semana ya no avisa | Pasa |
| `unaRechazadaRecienteAvisaConElMotivo` | Una rechazada reciente avisa con el motivo | Pasa |
| `unaRechazadaDeHaceMasDeDosSemanasYaNoAvisa` | Una rechazada de hace más de dos semanas ya no avisa | Pasa |
| `unaCanceladaConCreditoVigenteAvisaHastaCuandoPuedeUsarlo` | Una cancelada con crédito vigente avisa hasta cuando puede usarlo | Pasa |
| `unPedidoDeCancelacionSinResponderAvisaQueEstaEnRevision` | Un pedido de cancelación sin responder avisa que está en revision | Pasa |
| `unaCancelacionAprobadaAvisaDelCreditoDe30Dias` | Una cancelación aprobada avisa del crédito de 30 días | Pasa |
| `unaCancelacionRechazadaAvisaElMotivoYQueLaReservaSigueEnPie` | Una cancelación rechazada avisa el motivo y que la reserva sigue en pie | Pasa |
| `unaRespuestaDeHaceMasDeDosSemanasYaNoAvisa` | Una respuesta de hace más de dos semanas ya no avisa | Pasa |
| `ordenaLosAvisosDelMasAlMenosImportante` | Ordena los avisos del más al menos importante | Pasa |
| `elPerfilDeUnSocioVigenteCalculaDiasRestantesYProgreso` | El perfil de un socio vigente calcula días restantes y progreso | Pasa |
| `elProgresoLlegaA100ElDiaDelVencimiento` | El progreso llega a 100 el día del vencimiento | Pasa |
| `elProgresoNuncaEsNegativoSiLaMembresiaTodaviaNoEmpezo` | El progreso nunca es negativo si la membresía todavía no empezó | Pasa |
| `elPerfilDeUnSocioSinMembresiaMuestraLaUltimaVencida` | El perfil de un socio sin membresía muestra la ultima vencida | Pasa |
| `elPerfilUsaLaPrimeraPalabraDelNombreComoNombreDePila` | El perfil usa la primera palabra del nombre como nombre de pila | Pasa |
| `sinNombreElNombreDePilaEsLaParteLocalDelCorreo` | Sin nombre el nombre de pila es la parte local del correo | Pasa |
| `unVigenteSinSuscripcionVigenteNoTieneFechaDeVencimiento` | Un vigente sin suscripcion vigente no tiene fecha de vencimiento | Pasa |

### `servicio.PanelSocioServicioComprobanteTest` (19 pruebas, 19 casos, 1 deshabilitada)

| Prueba | Qué verifica | Resultado |
|---|---|---|
| `guardaElComprobanteYCreaElPagoPorElPrecioDeLaTarifa` | Guarda el comprobante y crea el pago por el precio de la tarifa | Pasa |
| `guardaUnPngConLaExtensionPng` | Guarda un PNG con la extensión PNG | Pasa |
| `aceptaUnArchivoDeExactamente5MB` | Acepta un archivo de exactamente 5 MB | Pasa |
| `rechazaUnArchivoDeUnByteMasQue5MB` | Rechaza un archivo de un byte más que 5 MB | Pasa |
| `rechazaSinArchivo` | Rechaza sin archivo | Pasa |
| `rechazaUnArchivoVacio` | Rechaza un archivo vacío | Pasa |
| `rechazaUnArchivoQueNoEsImagenNiPdf` | Rechaza un archivo que no es imagen ni PDF | Pasa |
| `rechazaSinLaFechaDeLaTransferencia` | Rechaza sin la fecha de la transferencia | Pasa |
| `aceptaUnaTransferenciaHechaJustoAhora` | Acepta una transferencia hecha justo ahora | Pasa |
| `aceptaUnaTransferenciaDeExactamente7DiasAtras` | Acepta una transferencia de exactamente 7 días atrás | Pasa |
| `rechazaUnaTransferenciaDeUnMinutoMasDe7Dias` | Rechaza una transferencia de un minuto más de 7 días | Pasa |
| `rechazaUnaTransferenciaConFechaDeUnaHoraEnElFuturo` | Rechaza una transferencia con fecha de una hora en el futuro | Pasa |
| `rechazaUnaTransferenciaAdelantadaMasDe25Minutos` | Rechaza una transferencia adelantada más de 25 minutos | Pasa |
| `aceptaUnaTransferenciaAdelantadaHasta25Minutos` | Acepta una transferencia adelantada hasta 25 minutos | Deshabilitada (posible defecto D-2) |
| `rechazaUnComprobanteCuyoHashYaSeUsoEnOtraOperacion` | Rechaza un comprobante cuyo hash ya se uso en otra operación | Pasa |
| `rechazaUnaReservaQueYaTieneComprobante` | Rechaza una reserva que ya tiene comprobante | Pasa |
| `rechazaUnaReservaDeOtroSocio` | Rechaza una reserva de otro socio | Pasa |
| `rechazaUnaReservaQueYaNoEsperaElPago` | Rechaza una reserva que ya no espera el pago | Pasa |
| `siNoSePuedeGuardarElArchivoAvisaAlSocioQueReintente` | Si no se puede guardar el archivo avisa al socio que reintente | Pasa |

### `servicio.PanelSocioServicioConsultasTest` (24 pruebas, 24 casos)

| Prueba | Qué verifica | Resultado |
|---|---|---|
| `buscaElSocioDeUnaCuentaActiva` | Busca el socio de una cuenta activa | Pasa |
| `unaCuentaDeUnSocioDadoDeBajaNoTieneSocio` | Una cuenta de un socio dado de baja no tiene socio | Pasa |
| `unaCuentaQueNoEsDeUnSocioNoTieneSocio` | Una cuenta que no es de un socio no tiene socio | Pasa |
| `sinIdDeUsuarioNoHaySocio` | Sin id de usuario no hay socio | Pasa |
| `unaInstalacionHabilitadaConPrecioSePuedeReservar` | Una instalación habilitada con precio se puede reservar | Pasa |
| `unaInstalacionEnMantenimientoMuestraHastaCuando` | Una instalación en mantenimiento muestra hasta cuando | Pasa |
| `unaInstalacionDadaDeBajaNoEstaDisponible` | Una instalación dada de baja no está disponible | Pasa |
| `unaInstalacionSinPrecioParaElTipoDeSocioNoSePuedeReservar` | Una instalación sin precio para el tipo de socio no se puede reservar | Pasa |
| `unaFranjaQueEmpezaEn48HorasExactasEstaLibre` | Una franja que empeza en 48 horas exactas está libre | Pasa |
| `unaFranjaQueEmpezaEn46HorasNoSePuedeElegir` | Una franja que empeza en 46 horas no se puede elegir | Pasa |
| `unaFranjaDeHoyQueYaEmpezoDiceYaPaso` | Una franja de hoy que ya empezó dice ya paso | Pasa |
| `unaFranjaReservadaPorOtroSocioDiceReservado` | Una franja reservada por otro socio dice reservado | Pasa |
| `unaFranjaReservadaPorElPropioSocioDiceTuReserva` | Una franja reservada por el propio socio dice tu reserva | Pasa |
| `lasFranjasDeUnDiaConviertenCadaMotivoSegunSuEstado` | Las franjas de un día convierten cada motivo según su estado | Pasa |
| `lasFranjasParaReprogramarSonLasDeLaInstalacionDeLaReservaPropia` | Las franjas para reprogramar son las de la instalación de la reserva propia | Pasa |
| `noMuestraFranjasParaReprogramarUnaReservaAjena` | No muestra franjas para reprogramar una reserva ajena | Pasa |
| `losDiasReservablesVanDelTercerDiaHastaDentroDe30Dias` | Los días reservables van del tercer día hasta dentro de 30 días | Pasa |
| `cuentaCuantasReservasProximasTieneElSocioCadaDia` | Cuenta cuantas reservas proximas tiene el socio cada día | Pasa |
| `laCuotaMensualSaleDeLaTarifaVigenteDelTipoDeSocio` | La cuota mensual sale de la tarifa vigente del tipo de socio | Pasa |
| `sinTarifaVigenteNoHayCuotaNiPrecio` | Sin tarifa vigente no hay cuota ni precio | Pasa |
| `elPrecioDelTurnoEsElDeLaTarifaVigenteEseDia` | El precio del turno es el de la tarifa vigente ese día | Pasa |
| `cp36_laTarifaDelSocioMuestraDesdeCuandoRigeYElProximoCambio` | Cp 36 la tarifa del socio muestra desde cuando rige y el proximo cambio | Pasa |
| `cp36_laTarifaDelSocioSinCambioProgramadoLoDice` | Cp 36 la tarifa del socio sin cambio programado lo dice | Pasa |
| `cp36_laCuotaDelSocioMuestraDesdeCuandoRigeYElProximoCambio` | Cp 36 la cuota del socio muestra desde cuando rige y el proximo cambio | Pasa |

### `servicio.PanelSocioServicioMisReservasTest` (20 pruebas, 20 casos)

| Prueba | Qué verifica | Resultado |
|---|---|---|
| `unaConfirmadaConExactamente48HorasPermitePedirLaCancelacion` | Una confirmada con exactamente 48 horas permite pedir la cancelación | Pasa |
| `unaConfirmadaConMenosDe48HorasYaNoPermitePedirLaCancelacion` | Una confirmada con menos de 48 horas ya no permite pedir la cancelación | Pasa |
| `unaConfirmadaConMasDe48HorasPermitePedirLaCancelacion` | Una confirmada con más de 48 horas permite pedir la cancelación | Pasa |
| `conUnPedidoPendienteSeMuestraCancelacionPedidaYNoSeOfreceOtro` | Con un pedido pendiente se muestra cancelación pedida y no se ofrece otro | Pasa |
| `elMontoSaleDelPagoCuandoLoHay` | El monto sale del pago cuando lo hay | Pasa |
| `sinPagoElMontoSaleDeLaTarifa` | Sin pago el monto sale de la tarifa | Pasa |
| `unaConfirmadaQueTerminoSeMuestraRealizadaYPideValoracion` | Una confirmada que terminó se muestra realizada y pide valoración | Pasa |
| `unaRealizadaYaValoradaMuestraElPuntaje` | Una realizada ya valorada muestra el puntaje | Pasa |
| `unaRealizadaAnteriorALaFuncionDeValoracionesNoPideValorar` | Una realizada anterior a la función de valoraciones no pide valorar | Pasa |
| `sinComprobanteSeOfreceSubirloYLiberarConElLimiteDe60Minutos` | Sin comprobante se ofrece subirlo y liberar con el limite de 60 minutos | Pasa |
| `conComprobanteQuedaEnRevisionYNoSePuedeLiberar` | Con comprobante queda en revision y no se puede liberar | Pasa |
| `unaCanceladaConCreditoVigentePermiteReprogramar` | Una cancelada con crédito vigente permite reprogramar | Pasa |
| `elUltimoDiaDelCreditoTodaviaPermiteReprogramar` | El último día del crédito todavía permite reprogramar | Pasa |
| `conElCreditoVencidoAyerNoPermiteReprogramar` | Con el crédito vencido ayer no permite reprogramar | Pasa |
| `conUnPedidoDeReprogramacionPendienteNoSeOfreceReprogramarOtraVez` | Con un pedido de reprogramación pendiente no se ofrece reprogramar otra vez | Pasa |
| `unaRechazadaMuestraElMotivoDelAdministrador` | Una rechazada muestra el motivo del administrador | Pasa |
| `unaRechazadaSinMotivoDiceQueElTurnoQuedoLibre` | Una rechazada sin motivo dice que el turno quedó libre | Pasa |
| `elSeguimientoDeUnaConfirmadaTieneCuatroPasos` | El seguimiento de una confirmada tiene cuatro pasos | Pasa |
| `elSeguimientoSinComprobanteMarcaElComprobanteComoPasoActual` | El seguimiento sin comprobante marca el comprobante como paso actual | Pasa |
| `elSeguimientoDeUnaCanceladaTerminaEnReservaCancelada` | El seguimiento de una cancelada termina en reserva cancelada | Pasa |

### `servicio.PanelSocioServicioReservaTest` (35 pruebas, 35 casos)

| Prueba | Qué verifica | Resultado |
|---|---|---|
| `reservaConExactamente48HorasDeAnticipacion` | Reserva con exactamente 48 horas de anticipación | Pasa |
| `reservaConMasDe48HorasDeAnticipacion` | Reserva con más de 48 horas de anticipación | Pasa |
| `rechazaReservarConMenosDe48HorasDeAnticipacion` | Rechaza reservar con menos de 48 horas de anticipación | Pasa |
| `rechazaReservarConUnMinutoMenosDe48Horas` | Rechaza reservar con un minuto menos de 48 horas | Pasa |
| `rechazaUnTurnoQueYaPaso` | Rechaza un turno que ya paso | Pasa |
| `buscaLaTarifaDeLaInstalacionYDelTipoDeSocioParaEseDia` | Busca la tarifa de la instalación y del tipo de socio para ese día | Pasa |
| `aceptaReservarUnTurnoDentroDeLosProximos30Dias` | Acepta reservar un turno dentro de los próximos 30 días | Pasa |
| `rechazaReservarUnTurnoAMasDe30DiasDeHoy` | Rechaza reservar un turno a más de 30 días de hoy | Pasa |
| `rechazaReservarUnDiaAnteriorAHoy` | Rechaza reservar un día anterior a hoy | Pasa |
| `rechazaReservarSiLaMembresiaNoEstaVigente` | Rechaza reservar si la membresía no está vigente | Pasa |
| `rechazaReservarSiElSocioFueDadoDeBaja` | Rechaza reservar si el socio fue dado de baja | Pasa |
| `rechazaReservarSiElSocioNoExiste` | Rechaza reservar si el socio no existe | Pasa |
| `aceptaReservarConDosReservasEsperandoElComprobante` | Acepta reservar con dos reservas esperando el comprobante | Pasa |
| `rechazaReservarConTresReservasEsperandoElComprobante` | Rechaza reservar con tres reservas esperando el comprobante | Pasa |
| `rechazaUnaInstalacionDeAccesoLibre` | Rechaza una instalación de acceso libre | Pasa |
| `rechazaUnaInstalacionEnMantenimiento` | Rechaza una instalación en mantenimiento | Pasa |
| `rechazaSiLaInstalacionNoTienePrecioParaElTipoDeSocio` | Rechaza si la instalación no tiene precio para el tipo de socio | Pasa |
| `rechazaUnTurnoQueYaTomoOtroSocio` | Rechaza un turno que ya tomó otro socio | Pasa |
| `traduceElChoqueConLaRestriccionUnicaDeLaBaseEnUnMensajeParaElSocio` | Traduce el choque con la restricción única de la base en un mensaje para el socio | Pasa |
| `exigeElegirUnTurno` | Exige elegir un turno | Pasa |
| `rechazaReservarSiHayUnTurnoTerminadoSinValorar` | Rechaza reservar si hay un turno terminado sin valorar | Pasa |
| `noExigeValorarUnTurnoQueYaFueValorado` | No exige valorar un turno que ya fue valorado | Pasa |
| `noExigeValorarTurnosAnterioresALaFuncionDeValoraciones` | No exige valorar turnos anteriores a la función de valoraciones | Pasa |
| `liberaUnaReservaPropiaQueEsperaElComprobante` | Libera una reserva propia que espera el comprobante | Pasa |
| `noLiberaUnaReservaQueYaTieneComprobante` | No libera una reserva que ya tiene comprobante | Pasa |
| `noLiberaUnaReservaDeOtroSocio` | No libera una reserva de otro socio | Pasa |
| `noLiberaUnaReservaQueYaNoEsperaElPago` | No libera una reserva que ya no espera el pago | Pasa |
| `liberaLasReservasSinComprobanteCreadasAntesDeLos60Minutos` | Libera las reservas sin comprobante creadas antes de los 60 minutos | Pasa |
| `noLiberaLaReservaCuyoComprobanteSeSubioJustoAntes` | No libera la reserva cuyo comprobante se subió justo antes | Pasa |
| `sinReservasVencidasNoLiberaNada` | Sin reservas vencidas no libera nada | Pasa |
| `elPrimerDiaReservableEsElDeDentroDeDosDias` | El primer día reservable es el de dentro de dos días | Pasa |
| `elMensajeDeValoracionesPendientesVaEnPluralConDosOMas` | El mensaje de valoraciones pendientes va en plural con dos o más | Pasa |
| `exigeElegirElDia` | Exige elegir el día | Pasa |
| `noLiberaLaReservaQueMientrasTantoFueConfirmada` | No libera la reserva que mientras tanto fue confirmada | Pasa |
| `noLiberaSinIndicarLaReserva` | No libera sin indicar la reserva | Pasa |

### `servicio.PanelSocioServicioValoracionTest` (24 pruebas, 24 casos)

| Prueba | Qué verifica | Resultado |
|---|---|---|
| `CP-37: acepta los puntajes de 1 a 5[1] 1` | Cp 37 acepta los puntajes de 1 a 5 1 1 | Pasa |
| `CP-37: acepta los puntajes de 1 a 5[2] 2` | Cp 37 acepta los puntajes de 1 a 5 2 2 | Pasa |
| `CP-37: acepta los puntajes de 1 a 5[3] 3` | Cp 37 acepta los puntajes de 1 a 5 3 3 | Pasa |
| `CP-37: acepta los puntajes de 1 a 5[4] 4` | Cp 37 acepta los puntajes de 1 a 5 4 4 | Pasa |
| `CP-37: acepta los puntajes de 1 a 5[5] 5` | Cp 37 acepta los puntajes de 1 a 5 5 5 | Pasa |
| `CP-38: rechaza puntajes 0 y 6[1] -1` | Cp 38 rechaza puntajes 0 y 6 1 1 | Pasa |
| `CP-38: rechaza puntajes 0 y 6[2] 0` | Cp 38 rechaza puntajes 0 y 6 2 0 | Pasa |
| `CP-38: rechaza puntajes 0 y 6[3] 6` | Cp 38 rechaza puntajes 0 y 6 3 6 | Pasa |
| `CP-38: rechaza puntajes 0 y 6[4] 10` | Cp 38 rechaza puntajes 0 y 6 4 10 | Pasa |
| `guardaLaValoracionConSuPuntajeComentarioYFecha` | Guarda la valoración con su puntaje comentario y fecha | Pasa |
| `rechazaUnPuntajeNulo` | Rechaza un puntaje nulo | Pasa |
| `guardaCompletoUnComentarioDeExactamente500Caracteres` | Guarda completo un comentario de exactamente 500 caracteres | Pasa |
| `recortaA500CaracteresUnComentarioMasLargo` | Recorta a 500 caracteres un comentario más largo | Pasa |
| `guardaComoNuloUnComentarioEnBlanco` | Guarda como nulo un comentario en blanco | Pasa |
| `guardaComoNuloUnComentarioNulo` | Guarda como nulo un comentario nulo | Pasa |
| `quitaLosEspaciosDeLosExtremosDelComentario` | Quita los espacios de los extremos del comentario | Pasa |
| `rechazaValorarUnaReservaQueYaFueValorada` | Rechaza valorar una reserva que ya fue valorada | Pasa |
| `rechazaLaValoracionSiLaBaseDetectaQueYaExistia` | Rechaza la valoración si la base detecta que ya existía | Pasa |
| `rechazaValorarUnTurnoQueTodaviaNoTermino` | Rechaza valorar un turno que todavía no terminó | Pasa |
| `aceptaValorarJustoCuandoTerminaElTurno` | Acepta valorar justo cuando termina el turno | Pasa |
| `rechazaValorarUnaReservaQueNoEstaConfirmada` | Rechaza valorar una reserva que no está confirmada | Pasa |
| `rechazaValorarUnaReservaDeOtroSocio` | Rechaza valorar una reserva de otro socio | Pasa |
| `rechazaValorarUnaReservaInexistente` | Rechaza valorar una reserva inexistente | Pasa |
| `rechazaValorarSinIndicarLaReserva` | Rechaza valorar sin indicar la reserva | Pasa |

### `dto.PromedioInstalacionTest` (5 pruebas, 5 casos)

| Prueba | Qué verifica | Resultado |
|---|---|---|
| `conPromedioDe4EsNivelAlto` | Con promedio de 4 es nivel alto | Pasa |
| `conPromedioDe3Coma5EsNivelMedioYNoParaRevisar` | Con promedio de 3 coma 5 es nivel medio y no para revisar | Pasa |
| `conPromedioPorDebajoDe3Coma5EsNivelBajoYParaRevisar` | Con promedio por debajo de 3 coma 5 es nivel bajo y para revisar | Pasa |
| `sinValoracionesEsNivelSinYNoSeMarcaParaRevisar` | Sin valoraciones es nivel sin y no se marca para revisar | Pasa |
| `elPorcentajeDeLaBarraEsElPromedioSobre5` | El porcentaje de la barra es el promedio sobre 5 | Pasa |

### `servicio.PruebaDeHumoTest` (1 prueba, 1 caso)

| Prueba | Qué verifica | Resultado |
|---|---|---|
| `cargaUnaClaseDeProduccionYGuardaElMensajeTalCual` | Carga una clase de producción y guarda el mensaje tal cual | Pasa |

### `servicio.RegistroServicioTest` (33 pruebas, 41 casos, 1 deshabilitada)

| Prueba | Qué verifica | Resultado |
|---|---|---|
| `rechazaUnCorreoConFormatoInvalido` | Rechaza un correo con formato inválido | Pasa (5 casos) |
| `rechazaUnCorreoNulo` | Rechaza un correo nulo | Pasa |
| `rechazaUnaContrasenaDeCincoCaracteres` | Rechaza una contraseña de cinco caracteres | Pasa |
| `aceptaUnaContrasenaDeSeisCaracteres` | Acepta una contraseña de seis caracteres | Pasa |
| `rechazaUnaContrasenaSinLetrasNumerosOSimbolo` | Rechaza una contraseña sin letras numeros o simbolo | Deshabilitada (posible defecto D-4) |
| `rechazaCuandoLasContrasenasNoCoinciden` | Rechaza cuando las contraseñas no coinciden | Pasa |
| `rechazaUnCorreoQueYaTieneUnaCuentaActiva` | Rechaza un correo que ya tiene una cuenta activa | Pasa |
| `permiteRegistrarseConElCorreoDeUnSocioDadoDeBaja` | Permite registrarse con el correo de un socio dado de baja | Pasa |
| `rechazaUnDniQueNoTieneEntre6Y15Digitos` | Rechaza un DNI que no tiene entre 6 y 15 dígitos | Pasa (4 casos) |
| `aceptaUnDniDeSeisYDeQuinceDigitos` | Acepta un DNI de seis y de quince dígitos | Pasa (2 casos) |
| `rechazaUnaAltaSinNombre` | Rechaza una alta sin nombre | Pasa |
| `rechazaUnTelefonoConGuiones` | Rechaza un teléfono con guiones | Pasa |
| `rechazaUnaFechaDeNacimientoVaciaOMalEscrita` | Rechaza una fecha de nacimiento vacía o mal escrita | Pasa |
| `rechazaUnaFechaDeNacimientoDeHoy` | Rechaza una fecha de nacimiento de hoy | Pasa |
| `rechazaUnAlumnoSinLegajo` | Rechaza un alumno sin legajo | Pasa |
| `rechazaUnaAltaSinTipoDeSocio` | Rechaza una alta sin tipo de socio | Pasa |
| `rechazaUnDniQueYaPerteneceAOtroSocio` | Rechaza un DNI que ya pertenece a otro socio | Pasa |
| `alRegistrarVuelveAValidarElPasoUno` | Al registrar vuelve a validar el paso uno | Pasa |
| `registraUsuarioSocioYCarnetEstudiantilParaUnAlumno` | Registra usuario socio y carnet estudiantil para un alumno | Pasa |
| `elSocioNuevoNaceSinMembresiaHastaQueSeValideSuPago` | El socio nuevo nace sin membresía hasta que se valide su pago | Pasa |
| `registraUnExternoConCarnetGeneralYSinGuardarLegajo` | Registra un externo con carnet general y sin guardar legajo | Pasa |
| `alVolverARegistrarseArchivaElCorreoYElDniDeLaFichaDadaDeBaja` | Al volver a registrarse archiva el correo y el DNI de la ficha dada de baja | Pasa |
| `elDniArchivadoSeCortaA15CaracteresParaEntrarEnLaColumna` | El DNI archivado se corta a 15 caracteres para entrar en la columna | Pasa |
| `registraUnDocenteInternoConSuCategoria` | Registra un docente interno con su categoria | Pasa |
| `registraUnNoDocenteInternoConSuCategoria` | Registra un no docente interno con su categoria | Pasa |
| `rechazaUnaCategoriaInternaDesconocida` | Rechaza una categoria interna desconocida | Pasa |
| `rechazaUnSocioInternoSinCategoria` | Rechaza un socio interno sin categoria | Pasa |
| `rechazaUnaContrasenaNula` | Rechaza una contraseña nula | Pasa |
| `unNombreNuloSeTrataComoVacio` | Un nombre nulo se trata como vacío | Pasa |
| `unDniNuloSeTrataComoVacio` | Un DNI nulo se trata como vacío | Pasa |
| `unLegajoNuloSeTrataComoVacio` | Un legajo nulo se trata como vacío | Pasa |
| `unApellidoNuloSeTrataComoVacio` | Un apellido nulo se trata como vacío | Pasa |
| `unTelefonoNuloSeTrataComoVacio` | Un teléfono nulo se trata como vacío | Pasa |

### `servicio.ReportesServicioCargaTest` (5 pruebas, 5 casos)

| Prueba | Qué verifica | Resultado |
|---|---|---|
| `soloSonIngresosLosPagosConfirmadosYValidadosAunqueLaReservaSeHayaCancelado` | Solo son ingresos los pagos confirmados y validados aunque la reserva se haya cancelado | Pasa |
| `laTarifaPromedioDeUnaInstalacionEsElPromedioDeSusTarifasVigentes` | La tarifa promedio de una instalación es el promedio de sus tarifas vigentes | Pasa |
| `laCuotaDeCadaTipoEsLaVigenteOCeroSiNoHay` | La cuota de cada tipo es la vigente o cero si no hay | Pasa |
| `cuentaLosSociosActivosYElTotalSinLosDadosDeBaja` | Cuenta los socios activos y el total sin los dados de baja | Pasa |
| `cargaLosTurnosDeCadaInstalacionReservable` | Carga los turnos de cada instalación reservable | Pasa |

### `servicio.ReportesServicioTest` (59 pruebas, 67 casos)

| Prueba | Qué verifica | Resultado |
|---|---|---|
| `CP-41: el resumen de reportes dice "Grado de satisfacción"` | Cp 41 el resumen de reportes dice grado de satisfacción | Pasa |
| `losUltimos7DiasTerminanHoyYSeComparanConLos7Anteriores` | Los ultimos 7 días terminan hoy y se comparan con los 7 anteriores | Pasa |
| `losUltimos30DiasIncluyenHoyYSeComparanConLos30Anteriores` | Los ultimos 30 días incluyen hoy y se comparan con los 30 anteriores | Pasa |
| `losUltimos90DiasSeComparanConLos90Anteriores` | Los ultimos 90 días se comparan con los 90 anteriores | Pasa |
| `losUltimos12MesesSeComparanConLos12Anteriores` | Los ultimos 12 meses se comparan con los 12 anteriores | Pasa |
| `esteMesVaDelPrimeroAHoyYSeComparaConLosMismosDiasDelMesAnterior` | Este mes va del primero a hoy y se compara con los mismos días del mes anterior | Pasa |
| `siElMesAnteriorEsMasCortoElPeriodoPrevioTerminaEnSuUltimoDia` | Si el mes anterior es más corto el periodo previo termina en su último día | Pasa |
| `elMesAnteriorSeComparaConElMesPrevioCompleto` | El mes anterior se compara con el mes previo completo | Pasa |
| `unPeriodoDesconocidoUsaLosUltimos30Dias` | Un periodo desconocido usa los ultimos 30 días | Pasa |
| `elBalanceSumaReservasYMembresiasDelPeriodoYLoComparaConElAnterior` | El balance suma reservas y membresías del periodo y lo compara con el anterior | Pasa |
| `elBalanceSeparaLoCobradoPorReservasYPorMembresias` | El balance separa lo cobrado por reservas y por membresías | Pasa |
| `elTicketPromedioEsLoCobradoPorReservasSobreLaCantidadDeReservas` | El ticket promedio es lo cobrado por reservas sobre la cantidad de reservas | Pasa |
| `sinIngresosEnNingunPeriodoNoHayCambios` | Sin ingresos en ningún periodo no hay cambios | Pasa |
| `unIngresoSeCuentaElDiaEnQueSeValidaYNoElDiaEnQueSePago` | Un ingreso se cuenta el día en que se valida y no el día en que se pago | Pasa |
| `unPeriodoDeUnMesSeAgrupaPorDiaConLaBarraMasAltaAl100` | Un periodo de un mes se agrupa por día con la barra más alta al 100 | Pasa |
| `unPeriodoDeTresMesesSeAgrupaPorSemanaDesdeElLunes` | Un periodo de tres meses se agrupa por semana desde el lunes | Pasa |
| `unPeriodoDeUnAnioSeAgrupaPorMes` | Un periodo de un anio se agrupa por mes | Pasa |
| `laGranularidadSePuedeForzarYSoloSeMuestraUnaEtiquetaDeCadaTres` | La granularidad se puede forzar y solo se muestra una etiqueta de cada tres | Pasa |
| `laInstalacionQueMasFacturaVaPrimeraConSuParticipacion` | La instalación que más factura va primera con su participacion | Pasa |
| `unaInstalacionSinIngresosApareceEnCero` | Una instalación sin ingresos aparece en cero | Pasa |
| `lasMembresiasNoCuentanEnLosIngresosPorInstalacion` | Las membresías no cuentan en los ingresos por instalación | Pasa |
| `cadaTipoDeSocioAportaPorReservasYPorMembresias` | Cada tipo de socio aporta por reservas y por membresías | Pasa |
| `elDineroPerdidoSonLosCreditosQueVencieronEnElPeriodoSinUsarse` | El dinero perdido son los creditos que vencieron en el periodo sin usarse | Pasa |
| `elDineroEnCreditosVigentesEsElQueTodaviaSePuedeReprogramar` | El dinero en creditos vigentes es el que todavía se puede reprogramar | Pasa |
| `unaReservaCanceladaSinPagoConfirmadoNoGeneraDineroEnJuego` | Una reserva cancelada sin pago confirmado no genera dinero en juego | Pasa |
| `loPendienteDeValidarSumaComprobantesDeReservasYDeMembresias` | Lo pendiente de validar suma comprobantes de reservas y de membresías | Pasa |
| `laProyeccionCuentaLasQueVencenEnLosProximos30DiasConSuCuota` | La proyeccion cuenta las que vencen en los próximos 30 días con su cuota | Pasa |
| `laOcupacionEsTurnosReservadosSobreTurnosDisponibles` | La ocupacion es turnos reservados sobre turnos disponibles | Pasa |
| `lasCanceladasYRechazadasNoOcupanTurno` | Las canceladas y rechazadas no ocupan turno | Pasa |
| `laOcupacionNoPasaDe100` | La ocupacion no pasa de 100 | Pasa |
| `sinTurnosLaOcupacionEsCero` | Sin turnos la ocupacion es cero | Pasa |
| `losDiasDeMantenimientoSeDescuentanDeLosTurnosDisponibles` | Los días de mantenimiento se descuentan de los turnos disponibles | Pasa |
| `elIngresoNoPercibidoEsLaTarifaPromedioPorLosTurnosSinOfrecer` | El ingreso no percibido es la tarifa promedio por los turnos sin ofrecer | Pasa |
| `elIndicadorDeOcupacionSeComparaEnPuntosConElPeriodoAnterior` | El indicador de ocupacion se compara en puntos con el periodo anterior | Pasa |
| `cadaInstalacionSePintaVerdeDesdeEl50AmbarDesdeEl30YRojoPorDebajo` | Cada instalación se pinta verde desde el 50 ambar desde el 30 y rojo por debajo | Pasa |
| `unaInstalacionEnMantenimientoTodoElPeriodoNoTieneOcupacion` | Una instalación en mantenimiento todo el periodo no tiene ocupacion | Pasa |
| `laFranjaPicoEsLaDeMayorOcupacion` | La franja pico es la de mayor ocupacion | Pasa |
| `laFranjaVaciaEsLaDeMenorOcupacion` | La franja vacía es la de menor ocupacion | Pasa |
| `elMapaDeCalorMuestraLaOcupacionPorHoraYDiaDeLaSemana` | El mapa de calor muestra la ocupacion por hora y día de la semana | Pasa |
| `losTurnosSinReservasSonLosDeInstalacionesHabilitadasQueNadieReservo` | Los turnos sin reservas son los de instalaciones habilitadas que nadie reservo | Pasa |
| `elMantenimientoMuestraLosDiasYElIngresoNoPercibidoEstimado` | El mantenimiento muestra los días y el ingreso no percibido estimado | Pasa |
| `cuentaLasSuscripcionesPorEstadoConLaMayorAl100` | Cuenta las suscripciones por estado con la mayor al 100 | Pasa |
| `losVencimientosSeAgrupanPorSemanasDeLosProximos28Dias` | Los vencimientos se agrupan por semanas de los próximos 28 días | Pasa |
| `unaRenovacionHastaQuinceDiasTardeTodaviaCuentaComoRenovacion` | Una renovación hasta quince días tarde todavía cuenta como renovación | Pasa |
| `siRenovaronTodasLaTasaEsDel100SinAlerta` | Si renovaron todas la tasa es del 100 sin alerta | Pasa |
| `sinMembresiasTerminadasLaTasaNoSeCalcula` | Sin membresías terminadas la tasa no se calcula | Pasa |
| `loCobradoPorMembresiasSeAgrupaPorTipoDeSocio` | Lo cobrado por membresías se agrupa por tipo de socio | Pasa |
| `laValidacionEsElTiempoMedioEntreQueElSocioPagaYElAdministradorValida` | La validación es el tiempo medio entre que el socio paga y el administrador valida | Pasa |
| `laDuracionSeEscribeEnMinutosHorasODias` | La duracion se escribe en minutos horas o días | Pasa (9 casos) |
| `elResumenCuentaLasConfirmadasYElPorcentajeDeCancelaciones` | El resumen cuenta las confirmadas y el porcentaje de cancelaciones | Pasa |
| `laValoracionPromedioDelResumenSoloCuentaLasDelPeriodo` | La valoración promedio del resumen solo cuenta las del periodo | Pasa |
| `unaValoracionPromedioPorDebajoDe3Coma5SeMarcaComoAlerta` | Una valoración promedio por debajo de 3 coma 5 se marca como alerta | Pasa |
| `sinNadaQueAtenderUnaSolaAlertaDiceQueEstaTodoEnOrden` | Sin nada que atender una sola alerta dice que está todo en orden | Pasa |
| `avisaLasMembresiasQueVencenEnLosProximos7DiasYNoLasDe8` | Avisa las membresías que vencen en los próximos 7 días y no las de 8 | Pasa |
| `avisaLosComprobantesQueEsperanValidacion` | Avisa los comprobantes que esperan validación | Pasa |
| `avisaLasInstalacionesConValoracionPromedioPorDebajoDe3Coma5` | Avisa las instalaciones con valoración promedio por debajo de 3 coma 5 | Pasa |
| `avisaLasFranjasSinNingunaReservaEnElPeriodo` | Avisa las franjas sin ninguna reserva en el periodo | Pasa |
| `avisaLosCreditosQueVencenEnLosProximos7DiasYNoLosDe8` | Avisa los creditos que vencen en los próximos 7 días y no los de 8 | Pasa |
| `avisaLasInstalacionesEnMantenimientoConSuFechaDeReactivacion` | Avisa las instalaciones en mantenimiento con su fecha de reactivacion | Pasa |

### `servicio.ReservaServicioListadosTest` (18 pruebas, 18 casos)

| Prueba | Qué verifica | Resultado |
|---|---|---|
| `unaConfirmadaSeMuestraConfirmadaYCobradaElDiaDeLaConfirmacion` | Una confirmada se muestra confirmada y cobrada el día de la confirmacion | Pasa |
| `unaCanceladaConCreditoVigenteLoDiceEnElTexto` | Una cancelada con crédito vigente lo dice en el texto | Pasa |
| `unaCanceladaConCreditoVencidoAyerNoDiceConCredito` | Una cancelada con crédito vencido ayer no dice con crédito | Pasa |
| `unaPendienteConComprobanteEstaPorConfirmar` | Una pendiente con comprobante está por confirmar | Pasa |
| `unaPendienteSinComprobanteEstaEsperandoElPago` | Una pendiente sin comprobante está esperando el pago | Pasa |
| `unaRechazadaNoOcupaElTurno` | Una rechazada no ocupa el turno | Pasa |
| `unaReservaTerminadaSeMarcaFinalizada` | Una reserva terminada se marca finalizada | Pasa |
| `unaReservaDeHoyQueTerminaJustoAhoraYaEstaFinalizada` | Una reserva de hoy que termina justo ahora ya está finalizada | Pasa |
| `unaReservaDeHoyQueTodaviaNoTerminoNoEstaFinalizada` | Una reserva de hoy que todavía no terminó no está finalizada | Pasa |
| `sinPagoNiTarifaElMontoEsNulo` | Sin pago ni tarifa el monto es nulo | Pasa |
| `elBuscadorUsaTextoSinTildesNiMayusculas` | El buscador usa texto sin tildes ni mayúsculas | Pasa |
| `laSolicitudMuestraLaEsperaDesdeQueElSocioPago` | La solicitud muestra la espera desde que el socio pago | Pasa |
| `siElPagoEsPosteriorAAhoraLaEsperaEsCero` | Si el pago es posterior a ahora la espera es cero | Pasa |
| `unaSolicitudParaHoyOMananaEsUrgenteYParaPasadoMananaNo` | Una solicitud para hoy o mañana es urgente y para pasado mañana no | Pasa |
| `unaSolicitudDeUnTurnoQueYaPasoEstaVencida` | Una solicitud de un turno que ya paso está vencida | Pasa |
| `unaSolicitudDeHoyEstaVencidaSoloSiElTurnoYaTermino` | Una solicitud de hoy está vencida solo si el turno ya terminó | Pasa |
| `laSolicitudMuestraCuandoDiceElSocioQueHizoLaTransferencia` | La solicitud muestra cuando dice el socio que hizo la transferencia | Pasa |
| `sinComprobanteGuardadoNoHayFechaDeOperacion` | Sin comprobante guardado no hay fecha de operación | Pasa |

### `servicio.ReservaServicioTest` (14 pruebas, 20 casos, 1 deshabilitada)

| Prueba | Qué verifica | Resultado |
|---|---|---|
| `confirmarDejaLaReservaYElPagoConfirmadosConQuienLosValido` | Confirmar deja la reserva y el pago confirmados con quién los válido | Pasa |
| `rechazarLiberaElTurnoYDejaElPagoRechazado` | Rechazar libera el turno y deja el pago rechazado | Pasa |
| `rechazarGuardaElMotivoSinEspaciosSobrantes` | Rechazar guarda el motivo sin espacios sobrantes | Pasa |
| `rechazarRecortaElMotivoA255Caracteres` | Rechazar recorta el motivo a 255 caracteres | Pasa |
| `rechazarConMotivoEnBlancoNoGuardaMotivo` | Rechazar con motivo en blanco no guarda motivo | Pasa |
| `rechazarSinMotivoNoSePermiteYLaSolicitudSigueEsperando` | Rechazar sin motivo no se permite y la solicitud sigue esperando | Deshabilitada (posible defecto D-5) |
| `noResuelveUnaSolicitudQueYaEstabaConfirmada` | No resuelve una solicitud que ya estaba confirmada | Pasa |
| `noResuelveUnaReservaSinComprobante` | No resuelve una reserva sin comprobante | Pasa |
| `noResuelveUnaSolicitudConElPagoYaValidado` | No resuelve una solicitud con el pago ya validado | Pasa |
| `noResuelveUnaReservaInexistente` | No resuelve una reserva inexistente | Pasa |
| `textoEsperaUsaMinutosHorasYDias` | Texto espera usa minutos horas y días | Pasa (7 casos) |
| `cuandoDiceHoyYMananaParaLosDosPrimerosDias` | Cuando dice hoy y mañana para los dos primeros días | Pasa |
| `cuandoMuestraElDiaDeLaSemanaYLaFechaParaElResto` | Cuando muestra el día de la semana y la fecha para el resto | Pasa |
| `normalizarQuitaTildesYMayusculasParaBuscar` | Normalizar quita tildes y mayúsculas para buscar | Pasa |

### `servicio.SociosServicioTest` (35 pruebas, 42 casos)

| Prueba | Qué verifica | Resultado |
|---|---|---|
| `marcaPorVencerALosVigentesQueVencenDentroDelPlazo` | Marca por vencer a los vigentes que vencen dentro del plazo | Pasa |
| `noMarcaPorVencerSiVenceUnDiaDespuesDelPlazo` | No marca por vencer si vence un día después del plazo | Pasa |
| `unSocioSinMembresiaVigenteNoEstaPorVencer` | Un socio sin membresía vigente no está por vencer | Pasa |
| `unSocioQueNuncaTuvoMembresiaNoTieneFechaDeVencimiento` | Un socio que nunca tuvo membresía no tiene fecha de vencimiento | Pasa |
| `laFichaDeUnSocioVigenteMuestraCuantosDiasFaltan` | La ficha de un socio vigente muestra cuantos días faltan | Pasa |
| `laFichaDescribeElVencimientoCercano` | La ficha describe el vencimiento cercano | Pasa (4 casos) |
| `laFichaDeUnSocioSinSuscripcionesDiceSinMembresia` | La ficha de un socio sin suscripciones dice sin membresía | Pasa |
| `laFichaDeUnSocioConMembresiaVencidaDiceExpirada` | La ficha de un socio con membresía vencida dice expirada | Pasa |
| `cuentaSoloLasCuotasConPagoConfirmadoYValidado` | Cuenta solo las cuotas con pago confirmado y validado | Pasa |
| `elCarnetCongeladoSeMuestraComoCongelado` | El carnet congelado se muestra como congelado | Pasa |
| `noHayFichaDeUnSocioInexistenteNiDadoDeBaja` | No hay ficha de un socio inexistente ni dado de baja | Pasa |
| `elNumeroDeSocioLlevaCincoDigitos` | El número de socio lleva cinco dígitos | Pasa |
| `lasInicialesSonLasDelNombreYElApellido` | Las iniciales son las del nombre y el apellido | Pasa |
| `sinNombreNiApellidoLaInicialEsLaDelCorreo` | Sin nombre ni apellido la inicial es la del correo | Pasa |
| `paraEditarDevuelveLosDatosActualesDelSocio` | Para editar devuelve los datos actuales del socio | Pasa |
| `noSePuedeEditarUnSocioDadoDeBaja` | No se puede editar un socio dado de baja | Pasa |
| `guardaLosCambiosDelSocioYDeSuCuenta` | Guarda los cambios del socio y de su cuenta | Pasa |
| `exigeUnTelefono` | Exige un teléfono | Pasa (3 casos) |
| `exigeUnNombre` | Exige un nombre | Pasa |
| `exigeUnApellido` | Exige un apellido | Pasa |
| `rechazaUnCorreoInvalido` | Rechaza un correo inválido | Pasa |
| `rechazaUnDniQueNoTieneEntre6Y15Digitos` | Rechaza un DNI que no tiene entre 6 y 15 dígitos | Pasa (3 casos) |
| `rechazaUnaFechaDeNacimientoMalEscrita` | Rechaza una fecha de nacimiento mal escrita | Pasa |
| `rechazaUnaFechaDeNacimientoDeHoy` | Rechaza una fecha de nacimiento de hoy | Pasa |
| `aceptaUnaFechaDeNacimientoDeAyer` | Acepta una fecha de nacimiento de ayer | Pasa |
| `rechazaUnTipoDeSocioInexistente` | Rechaza un tipo de socio inexistente | Pasa |
| `exigeLegajoSiLaCategoriaLoRequiere` | Exige legajo si la categoria lo requiere | Pasa |
| `noGuardaLegajoSiLaCategoriaNoLoRequiere` | No guarda legajo si la categoria no lo requiere | Pasa |
| `rechazaUnCorreoQueYaUsaOtraCuenta` | Rechaza un correo que ya usa otra cuenta | Pasa |
| `permiteConservarElPropioCorreo` | Permite conservar el propio correo | Pasa |
| `rechazaUnDniQueYaTieneOtroSocio` | Rechaza un DNI que ya tiene otro socio | Pasa |
| `permiteConservarElPropioDni` | Permite conservar el propio DNI | Pasa |
| `darDeBajaDejaLaBajaLogicaYDeshabilitaLaCuenta` | Dar de baja deja la baja logica y deshabilita la cuenta | Pasa |
| `noDaDeBajaUnSocioQueYaEstabaDadoDeBaja` | No da de baja un socio que ya estaba dado de baja | Pasa |
| `noDaDeBajaUnSocioInexistente` | No da de baja un socio inexistente | Pasa |

### `servicio.TarifasServicioTest` (45 pruebas, 50 casos)

| Prueba | Qué verifica | Resultado |
|---|---|---|
| `ordenValidoCuandoElAlumnoPagaMenosYElExternoMas` | Orden válido cuando el alumno paga menos y el externo más | Pasa |
| `ordenInvalidoCuandoElAlumnoPagaLoMismoQueElExterno` | Orden inválido cuando el alumno paga lo mismo que el externo | Pasa |
| `ordenInvalidoCuandoElAlumnoPagaMasQueUnDocente` | Orden inválido cuando el alumno paga más que un docente | Pasa |
| `ordenInvalidoCuandoElAlumnoPagaLoMismoQueUnNoDocente` | Orden inválido cuando el alumno paga lo mismo que un no docente | Pasa |
| `ordenInvalidoCuandoElExternoPagaMenosQueUnNoDocente` | Orden inválido cuando el externo paga menos que un no docente | Pasa |
| `ordenInvalidoCuandoElExternoPagaLoMismoQueUnDocente` | Orden inválido cuando el externo paga lo mismo que un docente | Pasa |
| `ordenValidoSiTodaviaFaltaCargarUnPrecio` | Orden válido si todavía falta cargar un precio | Pasa |
| `agregaUnPrecioNuevoPorTipoDeSocioConLaFechaDesdeLaQueRige` | Agrega un precio nuevo por tipo de socio con la fecha desde la que rige | Pasa |
| `cierraElPrecioAnteriorElDiaPrevioALaNuevaVigencia` | Cierra el precio anterior el día previo a la nueva vigencia | Pasa |
| `nuncaEditaElPrecioDelRegistroAnterior` | Nunca edita el precio del registro anterior | Pasa |
| `agregaSoloLaFilaDeLosTiposDeSocioQueCambiaron` | Agrega solo la fila de los tipos de socio que cambiaron | Pasa |
| `aceptaQueElCambioRijaDesdeHoyYCierraElAnteriorAyer` | Acepta que el cambio rija desde hoy y cierra el anterior ayer | Pasa |
| `rechazaUnaFechaAnteriorAHoy` | Rechaza una fecha anterior a hoy | Pasa |
| `rechazaElCambioSinFecha` | Rechaza el cambio sin fecha | Pasa |
| `rechazaUnPrecioQueNoEsUnNumeroMayorACeroConHastaDosDecimales` | Rechaza un precio que no es un número mayor a cero con hasta dos decimales | Pasa (6 casos) |
| `aceptaElPrecioMaximoQueEntraEnLaColumnaDeLaBase` | Acepta el precio máximo que entra en la columna de la base | Pasa |
| `aceptaLaComaComoSeparadorDecimal` | Acepta la coma como separador decimal | Pasa |
| `rechazaCuandoNingunPrecioCambia` | Rechaza cuando ningún precio cambia | Pasa |
| `rechazaUnOrdenEnElQueElAlumnoPagaMasQueElExterno` | Rechaza un orden en el que el alumno paga más que el externo | Pasa |
| `rechazaUnaFechaIgualALaDeUnPrecioYaProgramado` | Rechaza una fecha igual a la de un precio ya programado | Pasa |
| `rechazaUnaFechaAnteriorALaDeUnPrecioYaProgramado` | Rechaza una fecha anterior a la de un precio ya programado | Pasa |
| `rechazaCambiarPreciosDeUnaInstalacionQueNoExiste` | Rechaza cambiar precios de una instalación que no existe | Pasa |
| `rechazaCambiarPreciosSinIndicarLaInstalacion` | Rechaza cambiar precios sin indicar la instalación | Pasa |
| `rechazaCambiarPreciosDeUnaInstalacionDeAccesoLibre` | Rechaza cambiar precios de una instalación de acceso libre | Pasa |
| `cambiaLaCuotaDeMembresiaCerrandoLaAnteriorYAgregandoLaNueva` | Cambia la cuota de membresía cerrando la anterior y agregando la nueva | Pasa |
| `exigeCargarLosCuatroTiposDeSocioJuntos` | Exige cargar los cuatro tipos de socio juntos | Pasa |
| `muestraLoQueRigeHoyYNoElPrecioProgramado` | Muestra lo que rige hoy y no el precio programado | Pasa |
| `dejaVacioElTipoDeSocioQueTodaviaNoTienePrecio` | Deja vacío el tipo de socio que todavía no tiene precio | Pasa |
| `elHistorialMarcaComoProgramadoElCambioFuturo` | El historial marca como programado el cambio futuro | Pasa |
| `cargaLosPrimerosPreciosDeUnaInstalacionSinHistorial` | Carga los primeros precios de una instalación sin historial | Pasa |
| `alCambiarSoloCierraElUltimoPrecioDelTipoDeSocioYNoLosAnteriores` | Al cambiar solo cierra el último precio del tipo de socio y no los anteriores | Pasa |
| `noAcortaNiReabreUnPrecioQueYaEstabaCerradoAntesDeLaNuevaFecha` | No acorta ni reabre un precio que ya estaba cerrado antes de la nueva fecha | Pasa |
| `siHayDosPreciosAbiertosRigeElDeFechaDesdeMasReciente` | Si hay dos precios abiertos rige el de fecha desde más reciente | Pasa |
| `ordenValidoSiLaListaNoTieneAlumnoNiExterno` | Orden válido si la lista no tiene alumno ni externo | Pasa |
| `ordenValidoSiLaListaTieneAlumnoPeroNoExterno` | Orden válido si la lista tiene alumno pero no externo | Pasa |
| `alCambiarCierraElPrecioMasRecienteAunqueElHistorialVengaEnOrdenAscendente` | Al cambiar cierra el precio más reciente aunque el historial venga en orden ascendente | Pasa |
| `elPrecioVigenteEsElMasRecienteAunqueElHistorialVengaEnOrdenAscendente` | El precio vigente es el más reciente aunque el historial venga en orden ascendente | Pasa |
| `acortaUnPrecioCuyaFechaDeFinEraPosteriorALaNuevaVigencia` | Acorta un precio cuya fecha de fin era posterior a la nueva vigencia | Pasa |
| `elPrecioVigenteEsElMasRecienteAunqueElHistorialVengaEnOrdenDescendente` | El precio vigente es el más reciente aunque el historial venga en orden descendente | Pasa |
| `ordenInvalidoCuandoSoloHayAlumnoYExternoConElMismoPrecio` | Orden inválido cuando solo hay alumno y externo con el mismo precio | Pasa |
| `ordenValidoCuandoSoloHayAlumnoYExternoYElExternoPagaMas` | Orden válido cuando solo hay alumno y externo y el externo paga más | Pasa |
| `cp36_laVigenciaDiceDesdeCuandoRigeElPrecio` | Cp 36 la vigencia dice desde cuando rige el precio | Pasa |
| `cp36_sinPrecioVigenteNoHayVigencia` | Cp 36 sin precio vigente no hay vigencia | Pasa |
| `cp36_elProximoCambioDiceElPrecioYLaFecha` | Cp 36 el proximo cambio dice el precio y la fecha | Pasa |
| `cp36_sinCambioProgramadoLoDice` | Cp 36 sin cambio programado lo dice | Pasa |

### `servicio.UsuarioServicioTest` (8 pruebas, 8 casos)

| Prueba | Qué verifica | Resultado |
|---|---|---|
| `autenticaConElCorreoYLaContrasenaCorrectos` | Autentica con el correo y la contraseña correctos | Pasa |
| `unaCuentaHabilitadaIngresaSinConsultarNingunaMembresia` | Una cuenta habilitada ingresa sin consultar ninguna membresía | Pasa |
| `ignoraLosEspaciosAlrededorDelCorreo` | Ignora los espacios alrededor del correo | Pasa |
| `rechazaUnaContrasenaIncorrecta` | Rechaza una contraseña incorrecta | Pasa |
| `rechazaUnaCuentaDeshabilitadaAunConLaContrasenaCorrecta` | Rechaza una cuenta deshabilitada aún con la contraseña correcta | Pasa |
| `rechazaUnCorreoQueNoExiste` | Rechaza un correo que no existe | Pasa |
| `rechazaUnCorreoNuloSinConsultarLaBase` | Rechaza un correo nulo sin consultar la base | Pasa |
| `rechazaUnaContrasenaNulaSinConsultarLaBase` | Rechaza una contraseña nula sin consultar la base | Pasa |

### `servicio.ValoracionesServicioTest` (15 pruebas, 15 casos)

| Prueba | Qué verifica | Resultado |
|---|---|---|
| `armaLaFilaConSocioInstalacionTurnoPuntajeYFecha` | Arma la fila con socio instalación turno puntaje y fecha | Pasa |
| `unComentarioNuloQuedaVacioYSinMarcaDeComentario` | Un comentario nulo queda vacío y sin marca de comentario | Pasa |
| `quitaLosEspaciosDelComentario` | Quita los espacios del comentario | Pasa |
| `unaInstalacionSinValoracionesNoTienePromedioNiTendencia` | Una instalación sin valoraciones no tiene promedio ni tendencia | Pasa |
| `calculaElPromedioConUnDecimalYComa` | Calcula el promedio con un decimal y coma | Pasa |
| `ignoraLasValoracionesDeOtraInstalacion` | Ignora las valoraciones de otra instalación | Pasa |
| `laTendenciaEsPositivaSiMejoraronLosUltimos30Dias` | La tendencia es positiva si mejoraron los ultimos 30 días | Pasa |
| `laTendenciaEsNegativaSiEmpeoraronLosUltimos30Dias` | La tendencia es negativa si empeoraron los ultimos 30 días | Pasa |
| `sinCambiosSiLosDosPeriodosTienenElMismoPromedio` | Sin cambios si los dos periodos tienen el mismo promedio | Pasa |
| `noHayTendenciaSiSoloHayValoracionesRecientes` | No hay tendencia si solo hay valoraciones recientes | Pasa |
| `unaValoracionDeExactamente30DiasCuentaComoRecienteYLaDe31ComoPrevia` | Una valoración de exactamente 30 días cuenta como reciente y la de 31 como previa | Pasa |
| `unaValoracionDeExactamente60DiasCuentaComoPrevia` | Una valoración de exactamente 60 días cuenta como previa | Pasa |
| `unaValoracionDeMasDe60DiasNoEntraEnLaTendencia` | Una valoración de más de 60 días no entra en la tendencia | Pasa |
| `ordenaLasPeorValoradasPrimeroConLasDeMismoPromedioPorNombreYLasSinDatosAlFinal` | Ordena las peor valoradas primero con las de mismo promedio por nombre y las sin datos al final | Pasa |
| `elPromedioSeMuestraConUnDecimal` | El promedio se muestra con un decimal | Pasa |


## 4. Cobertura

La cobertura la mide JaCoCo sobre el código de producción mientras corren las pruebas. "Líneas" es el porcentaje de líneas ejecutadas por alguna prueba; "ramas" es el porcentaje de caminos de cada `if`/`&&`/`||` ejecutados. Una cobertura alta no prueba que las verificaciones sean buenas; sirve sobre todo para encontrar código que **ninguna** prueba toca.

### 4.1 Por paquete

| Paquete | Líneas | Ramas | Métodos |
|---|---|---|---|
| **`servicio`** | 88.1 % (2175/2469) | 80.0 % (1624/2030) | 80.7 % |
| **`util`** | 94.9 % (37/39) | 100.0 % (44/44) | 100.0 % |
| `repositorio` | 70.0 % (420/600) | 9.4 % (19/202) | 74.5 % |
| `dto` | 65.7 % (659/1003) | 59.5 % (25/42) | 55.3 % |
| `dao` | 0.7 % (2/301) | 0.0 % (0/14) | 1.0 % |
| `bean` | 1.6 % (22/1361) | 0.2 % (1/603) | 0.6 % |
| `seguridad` | 0.0 % (0/40) | 0.0 % (0/18) | 0.0 % |
| `resources` | 0.0 % (0/4) | n/a (0/0) | 0.0 % |
| `(raíz)` | 0.0 % (0/1) | n/a (0/0) | 0.0 % |
| **Total** | 57.0 % (3315/5818) | 58.0 % (1713/2953) | 44.4 % |

Cómo leer esta tabla: `servicio` y `util` son el alcance de este conjunto de pruebas. El total incluye paquetes que no se probaron a propósito (`bean`, con las pantallas JSF; `dao`, con las consultas a la base; y `seguridad`), que figuran en 0 %, por lo que el total solo no es un indicador de calidad. El porcentaje de `repositorio` y `dto` es incidental: son clases de datos que las pruebas construyen o recorren pero no verifican por sí mismas.

### 4.2 Capa de servicio y util, por clase

| Clase | Líneas | Ramas |
|---|---|---|
| `util.ComprobanteArchivos` | 100.0 % (20/20) | 100.0 % (38/38) |
| `util.PasswordUtil` | 89.5 % (17/19) | 100.0 % (6/6) |
| `servicio.MembresiasServicio` | 100.0 % (151/151) | 93.3 % (97/104) |
| `servicio.ReglaReservaException` | 100.0 % (2/2) | n/a (0/0) |
| `servicio.RegistroServicio` | 100.0 % (84/84) | 100.0 % (70/70) |
| `servicio.ReservaServicio` | 100.0 % (109/109) | 93.3 % (84/90) |
| `servicio.UsuarioServicio` | 100.0 % (9/9) | 100.0 % (10/10) |
| `servicio.ValoracionesServicio` | 100.0 % (58/58) | 100.0 % (32/32) |
| `servicio.ReportesServicio` | 99.8 % (540/541) | 84.2 % (404/480) |
| `servicio.HorarioServicio` | 99.2 % (128/129) | 98.1 % (104/106) |
| `servicio.TarifasServicio` | 98.5 % (130/132) | 100.0 % (104/104) |
| `servicio.PanelSocioServicio` | 98.2 % (385/392) | 89.7 % (315/351) |
| `servicio.CambiosReservaServicio` | 97.7 % (171/175) | 91.3 % (115/126) |
| `servicio.SociosServicio` | 91.9 % (171/186) | 75.3 % (137/182) |
| `servicio.InstalacionesServicio` | 80.7 % (192/238) | 71.0 % (137/193) |
| `servicio.DashboardServicio` | 12.4 % (17/137) | 5.4 % (6/111) |
| `servicio.MembresiaJob` | 0.0 % (0/24) | 0.0 % (0/8) |
| `servicio.CalendarioServicio` | 0.0 % (0/74) | 0.0 % (0/51) |

### 4.3 Núcleo de reglas

El porcentaje de la capa `servicio` completa mezcla las reglas con métodos que solo arman textos para las pantallas (listados, avisos, reportes). Para ver cuánto de lo **que importa** está probado, se calculó la cobertura de los métodos que implementan las reglas pedidas (anticipación de 48 h, crédito, reservas, comprobantes, membresías, tarifas, valoraciones, seguridad). La lista de métodos incluidos está a continuación para que se pueda auditar:

| Clase | Líneas | Ramas |
|---|---|---|
| `PanelSocioServicio` | 98.4 % (124/126) | 100.0 % (106/106) |
| `CambiosReservaServicio` | 100.0 % (104/104) | 97.7 % (86/88) |
| `MembresiasServicio` | 100.0 % (50/50) | 100.0 % (30/30) |
| `TarifasServicio` | 100.0 % (85/85) | 100.0 % (90/90) |
| `ReservaServicio` | 100.0 % (20/20) | 100.0 % (22/22) |
| `RegistroServicio` | 100.0 % (83/83) | 100.0 % (70/70) |
| `UsuarioServicio` | 100.0 % (8/8) | 100.0 % (10/10) |
| `PasswordUtil` | 89.5 % (17/19) | 100.0 % (6/6) |
| `ComprobanteArchivos` | 100.0 % (20/20) | 100.0 % (38/38) |
| **Núcleo de reglas** | **99.2 % (511/515)** | **99.6 % (458/460)** |

Métodos incluidos: `PanelSocioServicio`: reservar, subirComprobante, valorar, liberar, liberarVencidas, propiaPendiente, sinValorar, debeValorarse, primerDiaReservable, sha256; `CambiosReservaServicio`: fueraDePlazo, solicitar, aprobar, rechazar, cancelarPorElPredio, validarDestino, cancelar, mover, registrar, recortar; `MembresiasServicio`: aceptar, denegar, cancelar, vencerVencidas, congelarSiCorresponde, estaPorValidar, validarPago; `TarifasServicio`: ordenValido, cambiarAlquiler, cambiarMembresia, aplicar, enVigencia, parsear; `ReservaServicio`: confirmar, rechazar, resolver; `RegistroServicio`: validarPasoUno, registrar, resolverCategoria, bajaDe, liberar, archivado; `UsuarioServicio`: autenticar; `PasswordUtil`: generarHash, verificar, hashear; `ComprobanteArchivos`: guardar, borrar, ruta, extension, tipoMime, carpeta.

### 4.4 Lo que falta cubrir (críticos con menor cobertura)

| # | Clase y métodos | Líneas | Ramas | Qué hace / por qué es crítico | Qué pruebas faltan |
|---|---|---|---|---|---|
| 1 | `DashboardServicio` (calendario, plano, resumenDelDia, aFilas, estadoInstalaciones, porcentajeOcupacion, cuando, textoEstado, claseEstado, etiquetaDia, icono) | 2.6 % (3/114) | 1.0 % (1/103) | Tablero principal del administrador: resumen del día, plano del predio y calendario. | Resumen con y sin reservas, porcentaje de ocupación, íconos por disciplina y etiquetas del calendario. |
| 2 | `CalendarioServicio` (reservasDelMes, reservasDelDia, calendario, proximas, anteriores, inicioGrilla, aDto, cuando, textoEstado, claseEstado, nota, etiquetaDia) | 0.0 % (0/69) | 0.0 % (0/51) | Calendario de reservas del administrador (mes, próximas y anteriores). | Grilla de un mes que empieza a mitad de semana, próximas y anteriores y la nota de cada reserva. |
| 3 | `MembresiaJob` (iniciar, detener, liberarReservas, vencerMembresias) | 0.0 % (0/22) | 0.0 % (0/8) | Tarea programada: vence membresías cada 60 minutos y libera reservas sin comprobante cada 5. | No se puede sin el contenedor (ver 6.1). Haría falta una prueba de integración. |

Lo que queda sin cubrir dentro del núcleo de reglas (cuatro líneas y una rama) **no se puede ejercer desde una prueba unitaria**:

- `PanelSocioServicio.sha256` y `PasswordUtil.hashear`: el `catch (NoSuchAlgorithmException…)`. SHA-256 y PBKDF2WithHmacSHA256 los trae toda JVM; solo se alcanzarían alterando la JVM.
- `CambiosReservaServicio.mover` (`if (r.getEstado() == CANCELADA)`): la rama "la reserva no estaba cancelada" no es alcanzable desde la API pública, porque una reprogramación solo se aprueba si la reserva está cancelada con crédito. Es una condición defensiva.

Las clases que antes estaban en 0 % (`InstalacionesServicio`, `SociosServicio`, `ValoracionesServicio`, `HorarioServicio`, los reportes, los listados del administrador y los avisos y estados del panel del socio) ahora tienen pruebas. Lo que queda sin cobertura es el tablero y el calendario del administrador, que arman textos y grillas para pantalla, y `MembresiaJob`, que no se puede probar sin el contenedor (sección 6.1).

### 4.5 ¿Las pruebas detectan errores? (mutación manual)

Una cobertura alta solo dice que el código se ejecutó, no que alguna verificación lo controle. Para comprobarlo se tomó una **copia temporal** del proyecto, se rompió a propósito una regla por vez en esa copia (el código de producción del repositorio no se tocó) y se corrió toda la suite. Si algún test falla, la rotura fue detectada.

| # | Qué se rompió | Resultado | Primera prueba que lo detectó |
|---|---|---|---|
| M01 | Anticipación de 48 h pasa a 47 h | Detectada (12 fallan) | `CambiosReservaServicioTest.rechazaPedirLaCancelacionConMenosDe48Horas` |
| M02 | Se acepta un turno que empieza en 47 h 59 min | Detectada (3) | `CambiosReservaServicioTest.rechazaReprogramarAUnTurnoQueEmpiezaEn47Horas59Minutos` |
| M03 | El crédito pasa de 30 a 31 días | Detectada (3) | `CambiosReservaServicioTest.alAprobarUnaCancelacionLaReservaQuedaCanceladaConCreditoDe30Dias` |
| M04 | El tope de reservas sin comprobante pasa de 3 a 4 | Detectada (1) | `PanelSocioServicioReservaTest.rechazaReservarConTresReservasEsperandoElComprobante` |
| M05 | El límite de archivo pasa de 5 MB a 6 MB | Detectada (1) | `PanelSocioServicioComprobanteTest.rechazaUnArchivoDeUnByteMasQue5MB` |
| M06 | El plazo para pagar pasa de 60 a 90 minutos | Detectada (7) | `PanelSocioServicioAvisosTest.sinComprobanteAvisaHastaQueHoraTieneParaSubirlo` |
| M07 | Se reserva hasta 31 días adelante en vez de 30 | Detectada (3) | `PanelSocioServicioConsultasTest.losDiasReservablesVanDelTercerDiaHastaDentroDe30Dias` |
| M08 | El puntaje máximo pasa de 5 a 6 | Detectada (1) | `PanelSocioServicioValoracionTest.rechazaLosPuntajesFueraDeRango` |
| M09 | El comentario admite 501 caracteres | Detectada (1) | `PanelSocioServicioValoracionTest.recortaA500CaracteresUnComentarioMasLargo` |
| M10 | La transferencia puede tener 8 días en vez de 7 | Detectada (1) | `PanelSocioServicioComprobanteTest.rechazaUnaTransferenciaDeUnMinutoMasDe7Dias` |
| M11 | Alumno y externo pueden pagar lo mismo | **Sobrevivió la primera vez**; detectada después | `TarifasServicioTest.ordenInvalidoCuandoSoloHayAlumnoYExternoConElMismoPrecio` |
| M12 | Un PDF se reconoce con solo 4 bytes | Detectada (1) | `ComprobanteArchivosTest.noReconoceUnPdfDeSoloCuatroBytes` |
| M13 | La membresía dura 2 meses | Detectada (4) | `MembresiasServicioTest.laMembresiaEmpiezaElDiaDeLaValidacionYVenceUnMesDespues` |
| M14 | `verificar()` de contraseñas devuelve siempre `true` | Detectada (4) | `UsuarioServicioTest.rechazaUnaContrasenaIncorrecta` |
| M15 | El login ignora la cuenta deshabilitada | Detectada (1) | `UsuarioServicioTest.rechazaUnaCuentaDeshabilitadaAunConLaContrasenaCorrecta` |
| M16 | La capacidad 0 se acepta | Detectada (1) | `InstalacionesServicioTest.rechazaUnaCapacidadQueNoEsMayorACero` |
| M17 | Se acepta nacimiento de hoy al editar un socio | Detectada (1) | `SociosServicioTest.rechazaUnaFechaDeNacimientoDeHoy` |
| M18 | La ventana de tendencia pasa de 30 a 31 días | Detectada (6) | `ValoracionesServicioTest.laTendenciaEsNegativaSiEmpeoraronLosUltimos30Dias` |
| M19 | La ocupación "verde" empieza en más de 50 % en vez de 50 % | Detectada (1) | `ReportesServicioTest.cadaInstalacionSePintaVerdeDesdeEl50AmbarDesdeEl30YRojoPorDebajo` |
| M20 | También se liberan reservas con comprobante cargado | Detectada (1) | `PanelSocioServicioReservaTest.noLiberaLaReservaCuyoComprobanteSeSubioJustoAntes` |

Resultado: **19 de 20 detectadas en la primera pasada**. La que sobrevivió (M11) mostró un hueco real: con solo alumno y externo y el mismo precio, la comparación directa no estaba aislada porque la regla de "los otros tipos" también lo rechazaba. Se agregaron dos pruebas y la mutación quedó detectada, o sea 20 de 20.

Alcance de esta comprobación: son 20 roturas elegidas a mano sobre constantes y comparaciones de las reglas pedidas. No reemplaza una herramienta de mutación (por ejemplo PIT), que genera cientos de variantes automáticamente.

La tabla se midió sobre la rama `calidad/pruebas-junit`, antes de CC-01, CC-03 y CC-04 (sección 10). Las pruebas que cita siguen existiendo; no se volvió a medir.

## 5. Discrepancias y posibles defectos

Los identificadores `D-n` son defectos con una prueba deshabilitada: **D-2 y D-3 están confirmados** por el responsable del proyecto, **D-1 es un posible defecto** a confirmar y **D-4 y D-5 son discrepancias entre el plan de garantía y el código** que el equipo tiene que resolver (corregir el código o corregir el plan). Los `O-n` son observaciones que no tienen prueba deshabilitada. En ningún caso se modificó el código de producción: corregir un defecto cambia comportamiento y requiere una decisión del equipo.

### 5.1 Defectos (prueba deshabilitada)

**D-1. `PasswordUtil.verificar` lanza excepción con un hash mal formado** (posible defecto, pendiente de confirmar)

- Dónde: `util/PasswordUtil.verificar` (`SIGRID/src/main/java/com/sigrid/sigrid/util/PasswordUtil.java`).
- Comportamiento observado: con `null`, con `""` o con un hash de menos de tres partes devuelve `false`. Con un hash de **tres partes mal formado** lanza excepción: `verificar("abc", "abc:xyz:123")` produce `NumberFormatException: For input string: "abc"` y `verificar("abc", "120000:!!!:!!!")` produce `IllegalArgumentException: Illegal base64 character`.
- Por qué importa: `UsuarioServicio.autenticar` llama a este método. Un dato corrupto en la columna `usuario.password_hash` convertiría el intento de login de esa cuenta en un error del servidor en lugar de un rechazo.
- Regla esperada: un hash que no se puede interpretar es una contraseña incorrecta (`false`).
- Prueba: `PasswordUtilTest.rechazaUnHashMalFormadoSinLanzarExcepcion`. Ejecutada activada: falla con `NumberFormatException`.
- Severidad estimada: baja (requiere un dato ya corrupto), de corrección sencilla.

**D-2. Tolerancia de la fecha futura del comprobante: el código usa 5 minutos y la regla es 25** (defecto confirmado)

- Dónde: `servicio/PanelSocioServicio.subirComprobante` (línea `if (operacion.isAfter(ahora.plusMinutes(5)))`).
- Comportamiento observado: una transferencia con fecha 25 minutos adelantada se rechaza con "La fecha de la transferencia no puede ser futura." Solo se acepta hasta 5 minutos de adelanto.
- Regla confirmada por el responsable del proyecto: se tolera hasta 25 minutos (diferencia de reloj entre el banco y el servidor).
- Prueba: `PanelSocioServicioComprobanteTest.aceptaUnaTransferenciaAdelantadaHasta25Minutos`. Ejecutada activada: falla con `ReglaReservaException: La fecha de la transferencia no puede ser futura.`
- Pruebas complementarias que pasan: `rechazaUnaTransferenciaAdelantadaMasDe25Minutos` (26 minutos se rechaza) y `aceptaUnaTransferenciaHechaJustoAhora`.
- Corrección sugerida (no aplicada): cambiar `plusMinutes(5)` por `plusMinutes(25)` y quitar el `@Disabled` de la prueba.

**D-3. La renovación por adelantado empieza el día siguiente al vencimiento, no el mismo día** (defecto confirmado)

- Dónde: `servicio/MembresiasServicio.aceptar` (línea `vigentes.get(0).getFechaVencimiento().plusDays(1)`).
- Comportamiento observado: si el socio ya tiene una membresía vigente que vence el 23/10 y se acepta una renovación, la nueva empieza el **24/10** y vence el **24/11**. Cada renovación corre el ciclo un día más (23/10, 24/11, 25/12...).
- Regla confirmada por el responsable del proyecto: la membresía vence el mismo día del mes siguiente (empezó el 23/09, vence el 23/10); si se renueva antes de vencer, la nueva empieza el mismo día en que vence la vigente (23/10) y vence el 23/11; si se paga después del vencimiento, la fecha de inicio pasa a ser el día del pago.
- Pruebas (deshabilitadas): `MembresiasServicioTest.laRenovacionPorAdelantadoMantieneElMismoDiaDelMes` y `siSePagaElMismoDiaDelVencimientoLaNuevaEmpiezaEseDia`. Ejecutadas activadas: fallan con `expected: <2026-10-23> but was: <2026-10-24>`.
- Parte que sí cumple la regla y está probada: sin vigente, la membresía empieza el día de la validación (incluso si se paga después del vencimiento) y vence un mes después (`laMembresiaEmpiezaElDiaDeLaValidacionYVenceUnMesDespues`, `siSePagaDespuesDelVencimientoLaMembresiaEmpiezaElDiaDelPago`), y una renovación nunca empieza antes de que termine la vigente.
- Corrección sugerida (no aplicada): quitar el `.plusDays(1)` y los `@Disabled` de las dos pruebas.

**D-4. La contraseña no exige letras, números y un símbolo** (discrepancia entre el plan y el código, sin decidir)

- Dónde: `servicio/RegistroServicio.validarPasoUno` (solo comprueba `password.length() < 6`). No se encontró ninguna validación equivalente en `resources/scripts/register.js`.
- Qué dice el plan: RNF-03 y CP-02 piden que el registro rechace contraseñas sin letras, números o símbolo especial.
- Comportamiento observado: `Clave2026` (sin símbolo), `Clave#abcd` (sin número) y `123#456` (sin letras) se aceptan.
- Prueba (deshabilitada): `RegistroServicioTest.rechazaUnaContrasenaSinLetrasNumerosOSimbolo` (3 valores). Ejecutada activada: falla en los tres con `la contraseña "..." debería rechazarse ==> expected: not <null>`.
- Qué hay que decidir: si se agrega la regla al código (y se actualiza `aceptaUnaContrasenaDeSeisCaracteres`, que hoy acepta `123456`) o si se corrige el plan para que diga "al menos 6 caracteres".

**D-5. El rechazo de una solicitud no exige motivo** (discrepancia entre el plan y el código, sin decidir)

- Dónde: `servicio/ReservaServicio.rechazar` (el motivo es opcional) y la pantalla del administrador ("Motivo (opcional)").
- Qué dice el plan: RF-05 y CP-20 piden que rechazar sin motivo no se permita.
- Comportamiento observado: `rechazar(idReserva, idAdmin, "   ")` devuelve `true` y deja la reserva `RECHAZADA`.
- Prueba (deshabilitada): `ReservaServicioTest.rechazarSinMotivoNoSePermiteYLaSolicitudSigueEsperando`. Ejecutada activada: falla con `expected: <false> but was: <true>`.
- Qué hay que decidir: si se vuelve obligatorio el motivo (y se actualizan `rechazarLiberaElTurnoYDejaElPagoRechazado` y `rechazarConMotivoEnBlancoNoGuardaMotivo`, que hoy fijan lo contrario) o si se corrige el plan.

### 5.2 Reglas confirmadas que el código ya cumple

**O-1. Comentario de valoración de más de 500 caracteres.** El código lo **recorta** a 500 en lugar de rechazarlo (`PanelSocioServicio.valorar`). El responsable del proyecto confirmó que el límite es 500. Pruebas: `guardaCompletoUnComentarioDeExactamente500Caracteres` y `recortaA500CaracteresUnComentarioMasLargo`. Si se prefiriera avisar al socio en lugar de recortar en silencio, habría que cambiar el código y esa prueba.

**O-2. Reservar con membresía vencida.** El código decide por `socio.estado == ACTIVO` y no mira las fechas de la suscripción (`PanelSocioServicio.reservar`). El responsable del proyecto confirmó que ese es el diseño: si el socio está activo es porque su membresía está vigente, y el vencimiento (regla de D-3) se encarga de dejarlo `NO_ACTIVO`. Pruebas: `rechazaReservarSiLaMembresiaNoEstaVigente`, `rechazaReservarSiElSocioFueDadoDeBaja`, `alVencerLaUnicaVigenteElSocioQuedaNoActivoYElCarnetInactivo`. Riesgo residual que las pruebas no pueden verificar: el paso a `NO_ACTIVO` lo hace `MembresiaJob` cada 60 minutos (y `MembresiasServicio.vencerVencidas` al abrir el panel del administrador). Entre el vencimiento y la siguiente corrida un socio vencido todavía figura activo. Solo una prueba de integración con el contenedor puede comprobar la agenda.

### 5.3 Observaciones

- **O-3. `PasswordUtil.generarHash(null)` lanza `NullPointerException`.** No hay una regla escrita sobre qué debe pasar. Desde la interfaz no se llega a ese caso (el registro exige al menos 6 caracteres), pero la contraseña vacía `""` sí se hashea y verifica sin error. La prueba `generarHashConContrasenaNulaLanzaNullPointerException` solo registra el comportamiento actual; si se define una regla, hay que ajustarla.
- **O-4. Reloj interno.** El diseño pasa "ahora" como parámetro a casi todos los servicios, pero hay excepciones que usan el reloj del sistema: `ReservaServicio.resolver` (confirmar/rechazar), `MembresiasServicio.validarPago` y la comprobación "la fecha de nacimiento tiene que ser anterior a hoy" de `RegistroServicio.registrar`. En esos tres casos las pruebas verifican contra un rango "antes/después" o contra la fecha actual real, no contra una fecha fija.
- **O-5. La firma PNG se verifica de forma parcial.** `ComprobanteArchivos.extension` compara solo cuatro bytes (`0x89 P N G`) y exige más de 8 bytes; no valida la firma completa de 8 bytes. Un archivo que empieza con esos cuatro bytes pero no es un PNG válido se acepta como imagen.
- **O-6. El límite de 5 MB no está donde dice la documentación.** Se controla en `PanelSocioServicio.subirComprobante` y no en `ComprobanteArchivos`. Un archivo de exactamente 5 MB (5 242 880 bytes) se acepta; 5 MB más un byte se rechaza.
- **O-7. Las reglas de valoración viven en `PanelSocioServicio.valorar`.** `ValoracionesServicio` es de solo lectura (listados y promedios). Conviene corregir la documentación.
- **O-8. La regla de 48 horas está duplicada.** `CambiosReservaServicio.fueraDePlazo` se usa al reservar, al pedir cancelaciones y al validar un turno nuevo; pero `PanelSocioServicio.armar` repite la comparación (`inicio.isBefore(ahora.plusHours(HORAS_ANTICIPACION))`) para decidir si se ofrece el botón de cancelar. Hoy dan el mismo resultado; si alguien cambia una y no la otra, la pantalla ofrecería una acción que el servicio rechaza. `armar` tiene pruebas de su resultado (`PanelSocioServicioMisReservasTest`), pero ninguna obliga a que las dos comparaciones coincidan.

## 6. Clases no testeables sin refactor y clases no probadas

### 6.1 No testeables sin refactor (o sin integración)

| Clase / grupo | Por qué no se puede probar así |
|---|---|
| `servicio/MembresiaJob` | Obtiene su ejecutor con `@Resource ManagedScheduledExecutorService` y arranca con un evento CDI (`@Observes @Initialized`); sus métodos son privados. Sin contenedor no se puede construir ni disparar. Lo que ejecuta (`vencerVencidas`, `liberarVencidas`) sí está probado en los servicios. Haría falta una prueba de integración o dejar de usar `@Resource` en el campo. |
| `dao/*` (18 clases) | Son consultas JPQL sobre un `EntityManager`; probarlas exige una base de datos. Con Mockito se simulan, **no se verifican**. |
| Reglas que viven en la base o en las consultas | (a) El corte de los 60 minutos de la liberación (`fechaReserva < :limite`), (b) el corte de membresías vencidas (`fechaVencimiento < :hoy`) y vigentes (`>= :hoy`), (c) la restricción `UNIQUE turno_activo_key` contra la doble reserva, el hash único del comprobante y `uq_valoracion_reserva`, (d) el `CHECK` de puntaje 1 a 5 y los conteos `contarSinComprobanteDe` / `hayPendienteDe`. Las pruebas confirman que el servicio entrega el valor correcto a la consulta (por ejemplo, `ahora - 60 minutos`) y reacciona bien al resultado, no que la consulta lo calcule bien. |
| `bean/*` (19 clases JSF) | Dependen de `FacesContext`, de la sesión y de los ámbitos CDI. La lógica está en los servicios, que sí se prueban. |

### 6.2 Se pueden probar, pero quedaron fuera de este alcance

- `seguridad/AdminFilter`, `SocioFilter`, `CodificacionFilter` y `ComprobanteServlet`: usan la API de servlets (son interfaces, se podrían simular con Mockito). Son de seguridad (control de acceso por rol): conviene probarlos en una segunda etapa.
- `servicio/DashboardServicio` y `CalendarioServicio`: tablero y calendario del administrador; arman textos y grillas para las pantallas (ver 4.4).

## 7. Limitaciones de este conjunto de pruebas

1. **Mockito simula los DAO.** No se ejecuta SQL ni JPQL, no hay transacciones (`@Transactional` no actúa en pruebas unitarias) ni concurrencia: el bloqueo pesimista (`buscarParaResolver`) y la carrera por el mismo turno no se ejercen.
2. **No hay pruebas de interfaz ni de integración.** Nada verifica las páginas JSF, los filtros de seguridad, el despliegue ni la tarea programada.
3. **La cobertura global (56 %) queda por debajo de la de las reglas por alcance, no por descuido.** El núcleo de reglas está en el 99 % de líneas y de ramas; la capa de servicio completa, en el 88 %. Una cobertura alta no garantiza que cada verificación sea fuerte: por eso se hizo la mutación manual de 4.5, que cubre 20 reglas y no reemplaza a una herramienta automática.
4. **Las pruebas fijan los mensajes de error actuales.** Cambiar el texto de un mensaje rompe la prueba correspondiente, aunque la regla siga igual.
5. **Seis pruebas están deshabilitadas** y expresan la regla esperada, no el comportamiento actual (D-1, D-2, dos de D-3, D-4 y D-5). D-2 y D-3 están confirmadas por el responsable; D-1, D-4 y D-5 quedan por decidir. Si se corrige el código, hay que quitarles el `@Disabled`.
6. **Datos de prueba ficticios y fechas fijas** (12/10/2026 10:00 como "ahora"), salvo los casos del punto O-4.
7. **Solo se ejecutó en JDK 21.0.10 y Windows.** No se probó en el JDK 11 de destino de compilación ni en otros sistemas.
8. **Dependen del contenido de `src/main`:** si se renombra un método o se cambia una firma, las pruebas dejan de compilar (a propósito: avisan del cambio).

## 8. Reproducir los números de este documento

```bash
cd SIGRID
JAVA_HOME="/c/Program Files/Java/jdk-21.0.10" mvn test
# resultados en target/surefire-reports y target/site/jacoco/index.html
```

Las tablas de las secciones 3 y 4 se generaron a partir de `target/surefire-reports/TEST-*.xml` y `target/site/jacoco/jacoco.csv`/`jacoco.xml` de esa corrida.

La relación con los casos de prueba del plan (CP-01 a CP-53, tablas 41 y 42 del Apéndice F) está en [reporte-vv.md](reporte-vv.md). El código del caso va en el `@DisplayName` de cada prueba (convención de la sección 4.2.3.6 del plan) y el `pom.xml` configura Surefire para que ese nombre salga en el XML de resultados.

## 9. Ejecución automática en GitHub (opcional)

El archivo `.github/workflows/pruebas.yml` hace que GitHub corra las pruebas solo, en cada push y en cada pull request:

| Paso | Qué hace |
|---|---|
| Disparador | `push` y `pull_request` (cualquier rama) |
| Entorno | `ubuntu-latest`, JDK 21 (Temurin), caché de Maven |
| Comando | `mvn -B clean test`, dentro de la carpeta `SIGRID` (donde está el `pom.xml`) |
| Resultado | Si alguna prueba falla, el trabajo queda en rojo. El informe de cobertura se sube como artefacto `informe-jacoco` (se descarga desde la pestaña *Actions* de cada corrida) |

Detalles que conviene saber:

- **Está en la rama pero no activado.** GitHub solo lo ejecuta cuando la rama se sube (`push`). Mientras no se suba, no hace nada.
- **Usa `clean` a propósito.** La carpeta `SIGRID/target/` está versionada y trae clases compiladas viejas; sin limpiarla, Maven podría probar clases que no corresponden al código fuente actual.
- **No se pudo comprobar en GitHub desde esta máquina.** Lo que sí se verificó es la receta: en una copia limpia del proyecto (sin `target/`), `mvn clean test` con JDK 21 corrió 650 casos, 0 fallas, 6 deshabilitadas y generó `target/site/jacoco/index.html`. La primera corrida real en GitHub puede necesitar algún ajuste de entorno. En principio no debería depender de la zona horaria ni del idioma del servidor: las pruebas usan fechas fijas (salvo los casos de O-4) y el código fija el idioma `es-AR` de forma explícita.
- **No falla por cobertura baja:** no hay un umbral configurado, igual que en la corrida local.

## 10. Cambios de la cátedra verificados con esta suite

### CC-01. Cancelación automática (rama `cambio/cc-01-cancelacion-automatica`)

Lo que pide el plan (Tabla 12): si se cumple la regla de anticipación, el sistema cancela en el acto, genera el crédito de 30 días y registra el historial, sin intervención del administrador. El administrador solo aprueba o rechaza reprogramaciones.

- **Código:** `CambiosReservaServicio.solicitar` con tipo `CANCELACION` ya no crea un pedido: reutiliza `cancelar()` (reserva `CANCELADA`, crédito de 30 días, fila en `historial_reserva`) dentro de la misma transacción. El resto de las reglas no cambia (reserva propia, motivo obligatorio, solo `CONFIRMADA`, 48 horas). En la vista del socio el botón pasa a "Cancelar reserva" y el mensaje avisa del crédito.
- **Pruebas escritas primero (fallaban):** `cp22_cancelaAutomaticamenteConCredito`, `cp22_noCreaPedidoNiInvolucraAlAdministrador`, `cp24_cancelaExactamenteA48Horas` y `cp25_conElCreditoDeLaCancelacionAutomaticaSeReprogramaConAprobacion`. Ya pasaban como regresión: `cp21_noCancelaLaReservaDeOtroSocio`, `cp22_noCancelaDosVeces` y `cp23_noCancelaA47Horas59Minutos`.
- **Pruebas que cambiaron a propósito** (fijaban el comportamiento anterior, un pedido pendiente): `aceptaPedirLaCancelacionConExactamente48Horas` y `aceptaPedirLaCancelacionConMasDe48Horas` se eliminan (las reemplazan `cp24_` y `cp22_`), y `recortaElMotivoA255Caracteres` se reparte en `elMotivoDeLaCancelacionSeRecortaA255CaracteresEnElHistorial` y `elMotivoDeLaReprogramacionSeRecortaA255Caracteres`.
- **Lo que no se tocó:** los pedidos de cancelación que ya estuvieran `PENDIENTE` en la base siguen pudiendo aprobarse o rechazarse (`aprobar`/`rechazar` conservan esa rama), igual que los avisos del socio sobre ellos. La reprogramación sigue con aprobación del administrador (CP-25 y CP-26).
- **Pendiente de la vista:** el campo `puedePedirCancelacion` de `MiReserva` conserva su nombre (solo cambió el texto visible). La pantalla no se probó en el navegador: no hay pruebas de interfaz (sección 7).

### CC-03. Vigencia de las tarifas (rama `cambio/cc-03-vigencia-de-tarifas`, sale de la de CC-01)

Lo que pide el plan (Tabla 12): cada pantalla de tarifas muestra, por instalación y tipo de socio, el precio vigente, desde cuándo rige y el próximo cambio programado; igual para la cuota de membresía. Se verifica con CP-36 (nivel S, revisión manual).

- **Código:** `TarifasServicio.vigencia` ("Rige desde el 01/10/2026") y `TarifasServicio.proximo` ("Desde el 01/11/2026: $ 22.000" o "Sin cambios programados"). Los usan `InstalacionesServicio.precios` (tabla del administrador), `MembresiasServicio.tarifas` (cuota del administrador) y `PanelSocioServicio.instalaciones` y `cuota` (tarjeta de tarifas del socio). Los DAO ganan `buscarProxima` (alquiler y membresía) y `listarProgramadas` (alquiler). El esquema de la base no cambia.
- **Datos que cambian de tipo:** `PrecioFila.getPrecios()` pasa de `List<String>` a `List<PrecioVigente>`; `TarifaFila` e `InstalacionReservable` ganan `vigencia` y `proximo`; `PanelSocioServicio.cuota` devuelve una `TarifaFila` en lugar de un `String`.
- **Pruebas escritas primero (fallaban 9):** `cp36_laVigenciaDiceDesdeCuandoRigeElPrecio`, `cp36_elProximoCambioDiceElPrecioYLaFecha`, `cp36_sinCambioProgramadoLoDice`, `cp36_cadaPrecioMuestraDesdeCuandoRigeYElProximoCambio`, `cp36_unPrecioQueTodaviaNoRigeSeVeComoProximoCambio`, `cp36_laTarifaDelSocioMuestraDesdeCuandoRigeYElProximoCambio`, `cp36_laTarifaDelSocioSinCambioProgramadoLoDice`, `cp36_laCuotaDelSocioMuestraDesdeCuandoRigeYElProximoCambio` y `cp36_cadaCuotaMuestraDesdeCuandoRigeYElProximoCambio`.
- **Pruebas que cambiaron:** las dos de la cuota y la de precios del administrador se ajustaron al tipo nuevo; a las de la cuota se les agregó la fecha `vigenteDesde`, que la columna exige (`NOT NULL`).
- **Lo que no se pudo comprobar:** las consultas JPQL nuevas y las tres pantallas (inicio del socio, precios de instalaciones y cuota de membresías) no se ejercitaron contra la base ni en el navegador; solo se comprobó que las vistas son XML bien formado. Antes de dar CP-36 por aprobado hay que desplegar y mirarlas.

### CC-04. "Grado de satisfacción" en lugar de "Valoraciones" (rama `cambio/cc-04-grado-de-satisfaccion`, sale de la de CC-03)

Lo que pide el plan (Tabla 12): el nombre nuevo aparece en el menú, los títulos, los reportes y los PDF. El propio plan deja abierto si también se renombra en el panel del socio. Se verifica con CP-41 (nivel S, revisión manual).

- **Alcance aplicado:** el panel del administrador. Menú lateral (`AdminNavBean`), título de la pestaña y encabezado de la página (`valoraciones-dashboard-admin.xhtml`; el PDF sale de esa misma página por impresión, así que toma el título nuevo) y el resumen y las alertas de reportes (`ReportesServicio`: el indicador "Valoración promedio" pasa a "Grado de satisfacción" y la alerta "Baja valoración de los socios" pasa a "Grado de satisfacción bajo").
- **Lo que no se renombró:** el panel del socio ("Valorá tu turno", "Valoración pendiente", los avisos y el bloqueo para reservar), a la espera de la decisión del equipo. Tampoco cambian la dirección de la página (`/admin/valoraciones-dashboard-admin`), los nombres de clases, la tabla `valoracion_reserva` ni la regla de negocio.
- **Vocabulario:** el nombre de la sección es "Grado de satisfacción"; cada respuesta individual pasa a llamarse "calificación" (como en CP-37 a CP-40 del plan), para que la palabra "Valoraciones" no quede en ninguna pantalla del administrador.
- **Pruebas escritas primero (fallaban 6):** `AdminNavBeanTest.cp41_elMenuDiceGradoDeSatisfaccionYNoValoraciones` y `laSeccionRenombradaSigueEnLaMismaDireccion` (nuevas) y cuatro de `ReportesServicioTest` que fijaban los textos viejos.
- **Lo que no se pudo comprobar:** la página renombrada no se abrió en el navegador (solo se comprobó que es XML bien formado).
