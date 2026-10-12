# Resultados de las pruebas automatizadas de SIGRID

Documento del Plan de Garantía de Calidad. Reúne qué se probó, con qué resultado, cuánta parte del código ejercitan las pruebas y qué hallazgos aparecieron.

| Dato | Valor |
|---|---|
| Fecha de ejecución | 12/10/2026 |
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
| Resultado de cada clase de prueba | `SIGRID/target/surefire-reports/` |

## 2. Resumen de resultados

| Medida | Valor |
|---|---|
| Clases de prueba | 12 |
| Pruebas (métodos) | 269 |
| Ejecuciones (casos, contando cada valor de las parametrizadas) | 307 |
| Pasan | 304 |
| Deshabilitadas (posibles defectos) | 3 |
| Fallan | 0 |
| Tiempo total | unos segundos (la mayor parte es el hash PBKDF2 de 120 000 iteraciones) |

Las 3 pruebas deshabilitadas **no son pruebas rotas**: describen una regla que el responsable del proyecto confirmó y que el código actual no cumple (sección 5.1). Se dejaron con `@Disabled` y una explicación, tal como pide la regla de calidad de no modificar una prueba para que "pase" cuando el código está mal.

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

### `servicio.CambiosReservaServicioTest` (48 pruebas, 52 casos)

| Prueba | Qué verifica | Resultado |
|---|---|---|
| `unTurnoQueEmpiezaEn47HorasEstaFueraDePlazo` | Un turno que empieza en 47 horas está fuera de plazo | Pasa |
| `unTurnoQueEmpiezaEn48HorasExactasEstaEnPlazo` | Un turno que empieza en 48 horas exactas está en plazo | Pasa |
| `unTurnoQueEmpiezaEn49HorasEstaEnPlazo` | Un turno que empieza en 49 horas está en plazo | Pasa |
| `unTurnoQueEmpiezaEn47Horas59MinutosEstaFueraDePlazo` | Un turno que empieza en 47 horas 59 minutos está fuera de plazo | Pasa |
| `aceptaPedirLaCancelacionConExactamente48Horas` | Acepta pedir la cancelación con exactamente 48 horas | Pasa |
| `aceptaPedirLaCancelacionConMasDe48Horas` | Acepta pedir la cancelación con más de 48 horas | Pasa |
| `rechazaPedirLaCancelacionConMenosDe48Horas` | Rechaza pedir la cancelación con menos de 48 horas | Pasa |
| `rechazaPedirLaCancelacionConUnMinutoMenosDe48Horas` | Rechaza pedir la cancelación con un minuto menos de 48 horas | Pasa |
| `rechazaElPedidoDeUnaReservaQueEsDeOtroSocio` | Rechaza el pedido de una reserva que es de otro socio | Pasa |
| `rechazaElPedidoDeUnaReservaInexistente` | Rechaza el pedido de una reserva inexistente | Pasa |
| `rechazaElPedidoSinMotivo` | Rechaza el pedido sin motivo | Pasa (3 casos) |
| `rechazaUnSegundoPedidoPendienteParaLaMismaReserva` | Rechaza un segundo pedido pendiente para la misma reserva | Pasa |
| `rechazaPedirLaCancelacionDeUnaReservaQueNoEstaConfirmada` | Rechaza pedir la cancelación de una reserva que no está confirmada | Pasa |
| `recortaElMotivoA255Caracteres` | Recorta el motivo a 255 caracteres | Pasa |
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

### `servicio.MembresiasServicioTest` (23 pruebas, 23 casos, 1 deshabilitada)

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

### `servicio.PanelSocioServicioValoracionTest` (17 pruebas, 24 casos)

| Prueba | Qué verifica | Resultado |
|---|---|---|
| `guardaLaValoracionConSuPuntajeComentarioYFecha` | Guarda la valoración con su puntaje comentario y fecha | Pasa |
| `aceptaLosPuntajesDe1A5` | Acepta los puntajes de 1 a 5 | Pasa (5 casos) |
| `rechazaLosPuntajesFueraDeRango` | Rechaza los puntajes fuera de rango | Pasa (4 casos) |
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

### `servicio.PruebaDeHumoTest` (1 prueba, 1 caso)

| Prueba | Qué verifica | Resultado |
|---|---|---|
| `cargaUnaClaseDeProduccionYGuardaElMensajeTalCual` | Carga una clase de producción y guarda el mensaje tal cual | Pasa |

### `servicio.RegistroServicioTest` (32 pruebas, 40 casos)

| Prueba | Qué verifica | Resultado |
|---|---|---|
| `rechazaUnCorreoConFormatoInvalido` | Rechaza un correo con formato inválido | Pasa (5 casos) |
| `rechazaUnCorreoNulo` | Rechaza un correo nulo | Pasa |
| `rechazaUnaContrasenaDeCincoCaracteres` | Rechaza una contraseña de cinco caracteres | Pasa |
| `aceptaUnaContrasenaDeSeisCaracteres` | Acepta una contraseña de seis caracteres | Pasa |
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

### `servicio.ReservaServicioTest` (13 pruebas, 19 casos)

| Prueba | Qué verifica | Resultado |
|---|---|---|
| `confirmarDejaLaReservaYElPagoConfirmadosConQuienLosValido` | Confirmar deja la reserva y el pago confirmados con quién los válido | Pasa |
| `rechazarLiberaElTurnoYDejaElPagoRechazado` | Rechazar libera el turno y deja el pago rechazado | Pasa |
| `rechazarGuardaElMotivoSinEspaciosSobrantes` | Rechazar guarda el motivo sin espacios sobrantes | Pasa |
| `rechazarRecortaElMotivoA255Caracteres` | Rechazar recorta el motivo a 255 caracteres | Pasa |
| `rechazarConMotivoEnBlancoNoGuardaMotivo` | Rechazar con motivo en blanco no guarda motivo | Pasa |
| `noResuelveUnaSolicitudQueYaEstabaConfirmada` | No resuelve una solicitud que ya estaba confirmada | Pasa |
| `noResuelveUnaReservaSinComprobante` | No resuelve una reserva sin comprobante | Pasa |
| `noResuelveUnaSolicitudConElPagoYaValidado` | No resuelve una solicitud con el pago ya validado | Pasa |
| `noResuelveUnaReservaInexistente` | No resuelve una reserva inexistente | Pasa |
| `textoEsperaUsaMinutosHorasYDias` | Texto espera usa minutos horas y días | Pasa (7 casos) |
| `cuandoDiceHoyYMananaParaLosDosPrimerosDias` | Cuando dice hoy y mañana para los dos primeros días | Pasa |
| `cuandoMuestraElDiaDeLaSemanaYLaFechaParaElResto` | Cuando muestra el día de la semana y la fecha para el resto | Pasa |
| `normalizarQuitaTildesYMayusculasParaBuscar` | Normalizar quita tildes y mayúsculas para buscar | Pasa |

### `servicio.TarifasServicioTest` (39 pruebas, 44 casos)

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

### `servicio.UsuarioServicioTest` (7 pruebas, 7 casos)

| Prueba | Qué verifica | Resultado |
|---|---|---|
| `autenticaConElCorreoYLaContrasenaCorrectos` | Autentica con el correo y la contraseña correctos | Pasa |
| `ignoraLosEspaciosAlrededorDelCorreo` | Ignora los espacios alrededor del correo | Pasa |
| `rechazaUnaContrasenaIncorrecta` | Rechaza una contraseña incorrecta | Pasa |
| `rechazaUnaCuentaDeshabilitadaAunConLaContrasenaCorrecta` | Rechaza una cuenta deshabilitada aún con la contraseña correcta | Pasa |
| `rechazaUnCorreoQueNoExiste` | Rechaza un correo que no existe | Pasa |
| `rechazaUnCorreoNuloSinConsultarLaBase` | Rechaza un correo nulo sin consultar la base | Pasa |
| `rechazaUnaContrasenaNulaSinConsultarLaBase` | Rechaza una contraseña nula sin consultar la base | Pasa |


## 4. Cobertura

La cobertura la mide JaCoCo sobre el código de producción mientras corren las pruebas. "Líneas" es el porcentaje de líneas ejecutadas por alguna prueba; "ramas" es el porcentaje de caminos de cada `if`/`&&`/`||` ejecutados. Una cobertura alta no prueba que las verificaciones sean buenas; sirve sobre todo para encontrar código que **ninguna** prueba toca.

### 4.1 Por paquete

| Paquete | Líneas | Ramas | Métodos |
|---|---|---|---|
| **`servicio`** | 22.9 % (561/2450) | 21.5 % (434/2016) | 24.1 % |
| **`util`** | 94.9 % (37/39) | 100.0 % (44/44) | 100.0 % |
| `repositorio` | 59.8 % (359/600) | 6.9 % (14/202) | 61.5 % |
| `dto` | 4.2 % (41/987) | 0.0 % (0/42) | 4.9 % |
| `dao` | 0.7 % (2/291) | 0.0 % (0/14) | 1.0 % |
| `bean` | 0.0 % (0/1361) | 0.0 % (0/603) | 0.0 % |
| `seguridad` | 0.0 % (0/40) | 0.0 % (0/18) | 0.0 % |
| `resources` | 0.0 % (0/4) | n/a (0/0) | 0.0 % |
| `(raíz)` | 0.0 % (0/1) | n/a (0/0) | 0.0 % |
| **Total** | 17.3 % (1000/5773) | 16.7 % (492/2939) | 17.3 % |

Cómo leer esta tabla: el total (17 %) es bajo porque este conjunto de pruebas apunta solo a las reglas de negocio (`servicio` y `util`). Los paquetes `bean` (pantallas JSF), `dao` (consultas a la base), `dto` y `seguridad` no están dentro del alcance y figuran en 0 % o casi. El porcentaje de `repositorio` y `dto` es incidental: son clases de datos que las pruebas construyen pero no verifican por sí mismas.

### 4.2 Capa de servicio y util, por clase

| Clase | Líneas | Ramas |
|---|---|---|
| `util.ComprobanteArchivos` | 100.0 % (20/20) | 100.0 % (38/38) |
| `util.PasswordUtil` | 89.5 % (17/19) | 100.0 % (6/6) |
| `servicio.ReglaReservaException` | 100.0 % (2/2) | n/a (0/0) |
| `servicio.RegistroServicio` | 100.0 % (84/84) | 100.0 % (70/70) |
| `servicio.UsuarioServicio` | 100.0 % (9/9) | 100.0 % (10/10) |
| `servicio.TarifasServicio` | 98.4 % (127/129) | 100.0 % (100/100) |
| `servicio.CambiosReservaServicio` | 62.2 % (107/172) | 68.5 % (85/124) |
| `servicio.ReservaServicio` | 35.8 % (39/109) | 37.8 % (34/90) |
| `servicio.MembresiasServicio` | 35.3 % (53/150) | 28.8 % (30/104) |
| `servicio.PanelSocioServicio` | 33.7 % (130/386) | 30.0 % (104/347) |
| `servicio.DashboardServicio` | 7.3 % (10/137) | 0.9 % (1/111) |
| `servicio.MembresiaJob` | 0.0 % (0/24) | 0.0 % (0/8) |
| `servicio.HorarioServicio` | 0.0 % (0/129) | 0.0 % (0/106) |
| `servicio.ReportesServicio` | 0.0 % (0/541) | 0.0 % (0/480) |
| `servicio.InstalacionesServicio` | 0.0 % (0/232) | 0.0 % (0/189) |
| `servicio.SociosServicio` | 0.0 % (0/186) | 0.0 % (0/182) |
| `servicio.ValoracionesServicio` | 0.0 % (0/58) | 0.0 % (0/32) |
| `servicio.CalendarioServicio` | 0.0 % (0/74) | 0.0 % (0/51) |

### 4.3 Núcleo de reglas

El porcentaje de la capa `servicio` completa mezcla las reglas con métodos que solo arman textos para las pantallas (listados, avisos, reportes). Para ver cuánto de lo **que importa** está probado, se calculó la cobertura de los métodos que implementan las reglas pedidas (anticipación de 48 h, crédito, reservas, comprobantes, membresías, tarifas, valoraciones, seguridad). La lista de métodos incluidos está a continuación para que se pueda auditar:

| Clase | Líneas | Ramas |
|---|---|---|
| `PanelSocioServicio` | 98.4 % (124/126) | 98.1 % (104/106) |
| `CambiosReservaServicio` | 100.0 % (101/101) | 98.8 % (85/86) |
| `MembresiasServicio` | 100.0 % (50/50) | 100.0 % (30/30) |
| `TarifasServicio` | 100.0 % (85/85) | 100.0 % (90/90) |
| `ReservaServicio` | 100.0 % (20/20) | 100.0 % (22/22) |
| `RegistroServicio` | 100.0 % (83/83) | 100.0 % (70/70) |
| `UsuarioServicio` | 100.0 % (8/8) | 100.0 % (10/10) |
| `PasswordUtil` | 89.5 % (17/19) | 100.0 % (6/6) |
| `ComprobanteArchivos` | 100.0 % (20/20) | 100.0 % (38/38) |
| **Núcleo de reglas** | **99.2 % (508/512)** | **99.3 % (455/458)** |

Métodos incluidos: `PanelSocioServicio`: reservar, subirComprobante, valorar, liberar, liberarVencidas, propiaPendiente, sinValorar, debeValorarse, primerDiaReservable, sha256; `CambiosReservaServicio`: fueraDePlazo, solicitar, aprobar, rechazar, cancelarPorElPredio, validarDestino, cancelar, mover, registrar, recortar; `MembresiasServicio`: aceptar, denegar, cancelar, vencerVencidas, congelarSiCorresponde, estaPorValidar, validarPago; `TarifasServicio`: ordenValido, cambiarAlquiler, cambiarMembresia, aplicar, enVigencia, parsear; `ReservaServicio`: confirmar, rechazar, resolver; `RegistroServicio`: validarPasoUno, registrar, resolverCategoria, bajaDe, liberar, archivado; `UsuarioServicio`: autenticar; `PasswordUtil`: generarHash, verificar, hashear; `ComprobanteArchivos`: guardar, borrar, ruta, extension, tipoMime, carpeta.

### 4.4 Lo que falta cubrir (críticos con menor cobertura)

| # | Clase y métodos | Líneas | Ramas | Qué hace / por qué es crítico | Qué pruebas faltan |
|---|---|---|---|---|---|
| 1 | `InstalacionesServicio` (retirar, reactivar, editar) | 0.0 % (0/56) | 0.0 % (0/62) | Baja o mantenimiento de una cancha: bloquea si hay solicitudes por confirmar, cancela reservas confirmadas con crédito (`cancelarPorElPredio`) y libera las pendientes de pago. | Con solicitudes por confirmar; con reservas y sin marcar "cancelar"; con cancelación (confirmadas con crédito, pendientes liberadas); fecha de reactivación anterior a hoy; ya en mantenimiento o dada de baja; `reactivar` y `editar` (capacidad ≤ 0, descripción de más de 1000 caracteres). |
| 2 | `SociosServicio` (guardar, darDeBaja) | 0.0 % (0/48) | 0.0 % (0/48) | Edición del socio con DNI y correo únicos, legajo obligatorio por categoría, y baja lógica que deshabilita la cuenta. | DNI o correo de otro socio, legajo faltante, fecha de nacimiento no anterior a hoy, DNI de 6 a 15 dígitos; `darDeBaja` deja `bajaLogica` y `cuentaHabilitada = false`; baja de un socio ya dado de baja. |
| 3 | `PanelSocioServicio` (armar, avisos, perfil, pasos) | 0.0 % (0/182) | 0.0 % (0/179) | Decide qué acciones ve el socio: subir comprobante, liberar, pedir cancelación (48 h), reprogramar con crédito, valorar; y los avisos de membresía vencida o por vencer (7 días). | Una reserva por cada estado: confirmada a 47/48/49 h (`pedir`), pendiente sin y con comprobante, cancelada con crédito vigente y vencido, valoración pendiente; avisos con vencimiento a 7 y 8 días. |
| 4 | `ReportesServicio` (cargar, resumen, dineroEnJuego, alertas, balance, evolucion, renovacion) | 0.0 % (0/240) | 0.0 % (0/184) | Cálculo de ingresos, dinero en juego, balance y alertas que ve el administrador (541 líneas sin ninguna prueba). | Definir los valores esperados de cada métrica con un conjunto chico de datos fijos (por ejemplo tres pagos de distintos meses) y verificar sumas, porcentajes y la variación contra el mes anterior. |
| 5 | `ValoracionesServicio` (promedios, listar) | 0.0 % (0/53) | 0.0 % (0/30) | Promedio por instalación y tendencia de los últimos 30 días contra los 30 anteriores; orden de las peor valoradas. | Sin valoraciones (promedio "—"), solo recientes, recientes y previas (tendencia positiva, negativa y sin cambios), valoración justo en el corte de 30 días, orden y desempate por nombre. |

Lo que queda sin cubrir dentro del núcleo de reglas (cuatro líneas y tres ramas) **no se puede ejercer desde una prueba unitaria**:

- `PanelSocioServicio.sha256` y `PasswordUtil.hashear`: el `catch (NoSuchAlgorithmException…)`. SHA-256 y PBKDF2WithHmacSHA256 los trae toda JVM; solo se alcanzarían alterando la JVM.
- `CambiosReservaServicio.mover` (`if (r.getEstado() == CANCELADA)`): la rama "la reserva no estaba cancelada" no es alcanzable desde la API pública, porque una reprogramación solo se aprueba si la reserva está cancelada con crédito. Es una condición defensiva.

Un segundo bloque de pruebas cubrió los casos que un primer informe de cobertura había marcado como pendientes: el re-registro de una persona dada de baja, la reprogramación a una instalación no habilitada o a un turno que ya pasó, la primera carga de precios, los precios con fecha de fin posterior y el historial en cualquier orden, y el error de escritura del archivo.

## 5. Discrepancias y posibles defectos

Los identificadores `D-n` son posibles defectos con una prueba deshabilitada. Los `O-n` son observaciones que no tienen prueba deshabilitada.

### 5.1 Posibles defectos (prueba deshabilitada)

**D-1. `PasswordUtil.verificar` lanza excepción con un hash mal formado**

- Dónde: `util/PasswordUtil.verificar` (`SIGRID/src/main/java/com/sigrid/sigrid/util/PasswordUtil.java`).
- Comportamiento observado: con `null`, con `""` o con un hash de menos de tres partes devuelve `false`. Con un hash de **tres partes mal formado** lanza excepción: `verificar("abc", "abc:xyz:123")` produce `NumberFormatException: For input string: "abc"` y `verificar("abc", "120000:!!!:!!!")` produce `IllegalArgumentException: Illegal base64 character`.
- Por qué importa: `UsuarioServicio.autenticar` llama a este método. Un dato corrupto en la columna `usuario.password_hash` convertiría el intento de login de esa cuenta en un error del servidor en lugar de un rechazo.
- Regla esperada: un hash que no se puede interpretar es una contraseña incorrecta (`false`).
- Prueba: `PasswordUtilTest.rechazaUnHashMalFormadoSinLanzarExcepcion`. Ejecutada activada: falla con `NumberFormatException`.
- Severidad estimada: baja (requiere un dato ya corrupto), de corrección sencilla.

**D-2. Tolerancia de la fecha futura del comprobante: el código usa 5 minutos y la regla confirmada es 25**

- Dónde: `servicio/PanelSocioServicio.subirComprobante` (línea `if (operacion.isAfter(ahora.plusMinutes(5)))`).
- Comportamiento observado: una transferencia con fecha 25 minutos adelantada se rechaza con "La fecha de la transferencia no puede ser futura." Solo se acepta hasta 5 minutos de adelanto.
- Regla confirmada por el responsable del proyecto: se tolera hasta 25 minutos (diferencia de reloj entre el banco y el servidor).
- Prueba: `PanelSocioServicioComprobanteTest.aceptaUnaTransferenciaAdelantadaHasta25Minutos`. Ejecutada activada: falla con `ReglaReservaException: La fecha de la transferencia no puede ser futura.`
- A confirmar: si el "25" de la regla fuera un error de tipeo y la tolerancia correcta fuera 5 minutos, no hay defecto y la prueba debe ajustarse a 5 minutos.
- Prueba complementaria que sí pasa: `rechazaUnaTransferenciaAdelantadaMasDe25Minutos` (26 minutos se rechaza con ambas versiones de la regla).

**D-3. La renovación por adelantado empieza el día siguiente al vencimiento, no el mismo día**

- Dónde: `servicio/MembresiasServicio.aceptar` (línea `vigentes.get(0).getFechaVencimiento().plusDays(1)`).
- Comportamiento observado: si el socio ya tiene una membresía vigente que vence el 23/10 y se acepta una renovación, la nueva empieza el **24/10** y vence el **24/11**. Cada renovación corre el ciclo un día más (23/10, 24/11, 25/12...).
- Regla confirmada por el responsable del proyecto: la membresía vence el mismo día del mes siguiente (empezó el 23/09, vence el 23/10). Si se paga ese mismo día, el ciclo continúa; si se paga más tarde, la fecha de inicio pasa a ser el día del pago.
- Interpretación adoptada en la prueba (a confirmar): quien renueva por adelantado continúa el ciclo; la nueva membresía empieza el día en que vence la vigente (23/10) y vence un mes después (23/11).
- Prueba: `MembresiasServicioTest.laRenovacionPorAdelantadoMantieneElMismoDiaDelMes`. Ejecutada activada: falla con `expected: <2026-10-23> but was: <2026-10-24>`.
- Parte que sí cumple la regla y está probada: sin vigente, la membresía empieza el día de la validación (incluso si se paga después del vencimiento) y vence un mes después (`laMembresiaEmpiezaElDiaDeLaValidacionYVenceUnMesDespues`, `siSePagaDespuesDelVencimientoLaMembresiaEmpiezaElDiaDelPago`).

### 5.2 Reglas confirmadas que el código ya cumple

**O-1. Comentario de valoración de más de 500 caracteres.** El código lo **recorta** a 500 en lugar de rechazarlo (`PanelSocioServicio.valorar`). El responsable del proyecto confirmó que el límite es 500. Pruebas: `guardaCompletoUnComentarioDeExactamente500Caracteres` y `recortaA500CaracteresUnComentarioMasLargo`. Si se prefiriera avisar al socio en lugar de recortar en silencio, habría que cambiar el código y esa prueba.

**O-2. Reservar con membresía vencida.** El código decide por `socio.estado == ACTIVO` y no mira las fechas de la suscripción (`PanelSocioServicio.reservar`). El responsable del proyecto confirmó que ese es el diseño: si el socio está activo es porque su membresía está vigente, y el vencimiento (regla de D-3) se encarga de dejarlo `NO_ACTIVO`. Pruebas: `rechazaReservarSiLaMembresiaNoEstaVigente`, `rechazaReservarSiElSocioFueDadoDeBaja`, `alVencerLaUnicaVigenteElSocioQuedaNoActivoYElCarnetInactivo`. Riesgo residual que las pruebas no pueden verificar: el paso a `NO_ACTIVO` lo hace `MembresiaJob` cada 60 minutos (y `MembresiasServicio.vencerVencidas` al abrir el panel del administrador). Entre el vencimiento y la siguiente corrida un socio vencido todavía figura activo. Solo una prueba de integración con el contenedor puede comprobar la agenda.

### 5.3 Observaciones

- **O-3. `PasswordUtil.generarHash(null)` lanza `NullPointerException`.** No hay una regla escrita sobre qué debe pasar. Desde la interfaz no se llega a ese caso (el registro exige al menos 6 caracteres), pero la contraseña vacía `""` sí se hashea y verifica sin error. La prueba `generarHashConContrasenaNulaLanzaNullPointerException` solo registra el comportamiento actual; si se define una regla, hay que ajustarla.
- **O-4. Reloj interno.** El diseño pasa "ahora" como parámetro a casi todos los servicios, pero hay excepciones que usan el reloj del sistema: `ReservaServicio.resolver` (confirmar/rechazar), `MembresiasServicio.validarPago` y la comprobación "la fecha de nacimiento tiene que ser anterior a hoy" de `RegistroServicio.registrar`. En esos tres casos las pruebas verifican contra un rango "antes/después" o contra la fecha actual real, no contra una fecha fija.
- **O-5. La firma PNG se verifica de forma parcial.** `ComprobanteArchivos.extension` compara solo cuatro bytes (`0x89 P N G`) y exige más de 8 bytes; no valida la firma completa de 8 bytes. Un archivo que empieza con esos cuatro bytes pero no es un PNG válido se acepta como imagen.
- **O-6. El límite de 5 MB no está donde dice la documentación.** Se controla en `PanelSocioServicio.subirComprobante` y no en `ComprobanteArchivos`. Un archivo de exactamente 5 MB (5 242 880 bytes) se acepta; 5 MB más un byte se rechaza.
- **O-7. Las reglas de valoración viven en `PanelSocioServicio.valorar`.** `ValoracionesServicio` es de solo lectura (listados y promedios). Conviene corregir la documentación.
- **O-8. La regla de 48 horas está duplicada.** `CambiosReservaServicio.fueraDePlazo` se usa al reservar, al pedir cancelaciones y al validar un turno nuevo; pero `PanelSocioServicio.armar` repite la comparación (`inicio.isBefore(ahora.plusHours(HORAS_ANTICIPACION))`) para decidir si se ofrece el botón de cancelar. Hoy dan el mismo resultado; si alguien cambia una y no la otra, la pantalla ofrecería una acción que el servicio rechaza. `armar` no tiene pruebas.

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
- `servicio/DashboardServicio`, `CalendarioServicio`, `HorarioServicio`, `InstalacionesServicio`, `ReportesServicio` (983 líneas), `SociosServicio` y `ValoracionesServicio`: tienen reglas (baja de instalaciones, edición y baja de socios, promedios) o cálculos de reportes; ver 4.4.

## 7. Limitaciones de este conjunto de pruebas

1. **Mockito simula los DAO.** No se ejecuta SQL ni JPQL, no hay transacciones (`@Transactional` no actúa en pruebas unitarias) ni concurrencia: el bloqueo pesimista (`buscarParaResolver`) y la carrera por el mismo turno no se ejercen.
2. **No hay pruebas de interfaz ni de integración.** Nada verifica las páginas JSF, los filtros de seguridad, el despliegue ni la tarea programada.
3. **La cobertura global es baja (17 %) por alcance, no por descuido.** El núcleo de reglas está en el 99 % de líneas y de ramas; la capa de servicio completa, en el 23 %. Una cobertura alta no garantiza que cada verificación sea fuerte: no se aplicaron pruebas de mutación.
4. **Las pruebas fijan los mensajes de error actuales.** Cambiar el texto de un mensaje rompe la prueba correspondiente, aunque la regla siga igual.
5. **Tres pruebas están deshabilitadas** y expresan la regla confirmada, no el comportamiento actual (D-1, D-2, D-3). D-2 y D-3 dependen de la interpretación indicada y deben confirmarse.
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
