#!/usr/bin/env python3
"""Genera docs/calidad/reporte-vv.md (Apéndice F del plan de garantía): Tablas 40, 41 y 42 y la trazabilidad
caso de prueba -> pruebas automáticas.

Uso (desde la raíz del repositorio, después de correr `mvn test` en la carpeta SIGRID):
    python docs/calidad/generar_reporte_vv.py

Lee:  SIGRID/target/surefire-reports/TEST-*.xml  (resultados; cada caso sale con su @DisplayName "CP-22: ...")
      SIGRID/target/site/jacoco/jacoco.csv       (cobertura)
Escribe: docs/calidad/reporte-vv.md

El código de cada caso (CP-nn) está en el @DisplayName de las pruebas: así la trazabilidad no depende de un
archivo aparte que se desactualice.
"""
import csv
import datetime
import glob
import os
import platform
import re
import subprocess
import sys
import xml.etree.ElementTree as ET

RAIZ = os.path.abspath(os.path.join(os.path.dirname(__file__), '..', '..'))
SUREFIRE = os.path.join(RAIZ, 'SIGRID', 'target', 'surefire-reports')
JACOCO = os.path.join(RAIZ, 'SIGRID', 'target', 'site', 'jacoco', 'jacoco.csv')
SALIDA = os.path.join(RAIZ, 'docs', 'calidad', 'reporte-vv.md')

# Caso | nivel | crítico | requisito | descripción corta  (Tablas 19 a 25, 29 y 42 del plan)
CASOS = """\
CP-01|U|No|RF-01|Registrar un socio interno con datos válidos y legajo
CP-02|U|No|RNF-03, RNF-04|Registrar con contraseña sin símbolo especial
CP-03|U|No|RF-01|Registrar un socio interno sin legajo
CP-04|I|No|RF-01|Registrar con un correo o un DNI ya existente
CP-05|S|No|RNF-05|Iniciar sesión como socio y como administrador; ruta /admin
CP-06|U|No|RF-01|Iniciar sesión con cuenta dada de baja y con membresía vencida
CP-07|U|No|RNF-03|Guardar la misma contraseña para dos usuarios (hashes distintos)
CP-08|U|No|RF-04|Un socio ACTIVO reserva un turno libre dentro de los 30 días
CP-09|U|No|RF-04|Un socio NO_ACTIVO intenta reservar
CP-10|U|Sí|RF-04|Reservar un turno a más de 30 días
CP-11|U|Sí|RF-04|Intentar una cuarta reserva sin comprobante
CP-12|I|Sí|RNF-06|Dos socios reservan a la vez el mismo turno y día
CP-13|U|Sí|RF-03|Subir un comprobante PDF, JPG o PNG de hasta 5 MB con fecha válida
CP-14|U|Sí|RF-03, RNF-05|Subir un archivo de más de 5 MB o con extensión falsa
CP-15|U|Sí|RF-03|Subir un comprobante con fecha futura o de más de 7 días
CP-16|I|Sí|RF-03|Subir dos veces el mismo archivo
CP-17|U|Sí|RF-04|Pasan 60 minutos sin comprobante y corre la tarea programada
CP-18|U|Sí|RF-04|El socio libera un turno todavía no pagado
CP-19|U|No|RF-03, RF-05|El administrador confirma una reserva con comprobante
CP-20|U|No|RF-05|El administrador rechaza con motivo y luego sin motivo
CP-21|U|No|RNF-05|Un socio envía el identificador de una reserva ajena
CP-22|U|Sí|CC-01|Cancelar una reserva CONFIRMADA con más de 48 horas (automática, con crédito)
CP-23|U|Sí|CC-01|Cancelar a 47 horas y 59 minutos del turno
CP-24|U|Sí|CC-01|Cancelar exactamente a las 48 horas del turno
CP-25|U|Sí|CC-01|Pedir la reprogramación con crédito vigente y que el administrador la apruebe
CP-26|U|Sí|CC-01|El administrador rechaza la reprogramación
CP-27|U|Sí|RF-04|Reprogramar a otra instalación o enviar una segunda solicitud
CP-28|U|Sí|RF-04|Un crédito llega a los 30 días sin mantenimiento
CP-29|U|Sí|CC-02|Mantenimiento de más de 30 días con un crédito vigente
CP-30|U|Sí|CC-02|Se reactiva la instalación que estuvo en mantenimiento
CP-31|U|Sí|RF-05|El predio cancela una reserva por mantenimiento o clima
CP-32|U|Sí|RF-01|El administrador valida el comprobante de la cuota
CP-33|U|Sí|RF-01|Corre la tarea horaria con una membresía vencida
CP-34|U|Sí|RF-03|Cargar una tarifa nueva con vigencia futura
CP-35|U|Sí|RF-03|Cargar tarifas fuera de orden o repetidas
CP-36|S|Sí|CC-03|Abrir las pantallas de tarifas del socio y del administrador
CP-37|U|Sí|RF-07|Calificar un turno realizado con puntaje de 1 a 5 y comentario de hasta 500
CP-38|U|Sí|RF-07|Calificar con puntaje 0 o 6, o con un comentario de 501 caracteres
CP-39|U|Sí|RF-07|Calificar por segunda vez la misma reserva
CP-40|U|Sí|RF-07|Un turno realizado desde el 25/09/2026 sin calificar
CP-41|S|Sí|CC-04|Revisar menú, títulos, reportes y PDF ("Grado de satisfacción")
CP-42|I|No|RF-06|Reporte de ocupación de un día para una instalación
CP-43|S|No|CC-06|Exportar el PDF de reservas de un solo día
CP-44|S|No|CC-06|Exportar el PDF de un período sin reservas
CP-45|A|No|RNF-07|Con 500.000 reservas, listar y exportar un período acotado
CP-46|S|No|CC-06|Exportar un PDF de varias páginas
CP-47|A|No|CC-05|Medir el contraste de las pantallas del socio
CP-48|A|No|CC-05|Medir el contraste de las pantallas del administrador
CP-49|S|No|CC-05|Revisar los estados de reserva sin depender solo del color
CP-50|A|No|RNF-01|Abrir las pantallas principales en 360, 768 y 1280 px
CP-51|S|No|RF-02|El socio abre su historial de reservas
CP-52|I|No|RF-02|El administrador filtra el listado de reservas
CP-53|S|No|RNF-02|Revisar el diseño visual en RTF-03
"""

# Matriz de trazabilidad (Tabla 30)
MATRIZ = [
    ("RF-01 Gestión de socios", "01 03 04 06 32 33"), ("RF-02 Seguimiento del uso", "51 52"),
    ("RF-03 Pagos y cobros", "13 14 15 16 19 34 35"), ("RF-04 Reservas", "08 09 10 11 17 18 27 28"),
    ("RF-05 Administración de reservas e instalaciones", "19 20 31"), ("RF-06 Reportes", "42"),
    ("RF-07 Grado de satisfacción", "37 38 39 40"), ("RNF-01 Diseño adaptable", "50"),
    ("RNF-02 Estilo gráfico actual", "53"), ("RNF-03 Contraseñas", "02 07"), ("RNF-04 Mensajes de error", "02"),
    ("RNF-05 Seguridad y acceso", "05 14 21"), ("RNF-06 Integridad y concurrencia", "12"),
    ("RNF-07 Rendimiento de listados y PDF", "45"), ("RNF-08 Accesibilidad visual", "47 48 49"),
    ("CC-01 Cancelación automática", "22 23 24 25 26"), ("CC-02 Crédito y mantenimiento", "29 30"),
    ("CC-03 Vigencia de tarifas", "36"), ("CC-04 Grado de satisfacción (nombre)", "41"),
    ("CC-05 Colores y contrastes", "47 48 49"), ("CC-06 PDF con filtros de período", "43 44 45 46"),
]

# Defectos con prueba deshabilitada (@Disabled). La severidad es una propuesta: la asigna el líder de SQA (Tabla 31).
# caso, severidad propuesta, qué pasa, estado
DEFECTOS = [
    ("D-1", "CP-07", "Baja", "PasswordUtil.verificar lanza excepción con un hash mal formado en lugar de devolver false",
     "posible defecto, sin confirmar"),
    ("D-2", "CP-15", "Baja", "El comprobante tolera 5 minutos de adelanto; la regla confirmada es 25", "confirmado"),
    ("D-3", "CP-32", "Alta", "La renovación por adelantado empieza el día siguiente al vencimiento, no el mismo día",
     "confirmado"),
    ("D-4", "CP-02", "Alta", "La contraseña solo exige 6 caracteres; el plan pide letras, números y un símbolo",
     "discrepancia plan/código, sin decidir"),
    ("D-5", "CP-20", "Media", "El rechazo de una solicitud admite no dar motivo; el plan lo exige",
     "discrepancia plan/código, sin decidir"),
]
# casos que FALLAN por un defecto (su prueba deshabilitada es justamente el criterio de aceptación del caso)
FALLA_POR = {"CP-02": "D-4", "CP-20": "D-5"}
# casos que PASAN pero tienen una prueba deshabilitada sobre un detalle vecino
OBSERVA = {"CP-07": "D-1", "CP-15": "D-2", "CP-32": "D-3"}
NOTAS = {
    "CP-38": "El plan dice que un comentario de 501 caracteres se rechaza; el equipo decidió (D1) que se recorta a 500. "
             "Hay que actualizar el texto del caso.",
    "CP-20": "Con motivo se rechaza y queda guardado (aprobado); sin motivo el código lo permite (ver D-5).",
    "CP-33": "La prueba cubre el servicio que usa la tarea horaria (MembresiasServicio.vencerVencidas); la clase "
             "MembresiaJob (el temporizador) no se prueba en unitarias.",
}
# cambios de la cátedra ya implementados en el código (los casos S/A siguen necesitando la revisión manual)
IMPLEMENTADOS = {"CC-01", "CC-03", "CC-04", "CC-05"}
PENDIENTE_DE = {"CP-22": "CC-01", "CP-29": "CC-02", "CP-30": "CC-02", "CP-36": "CC-03", "CP-41": "CC-04",
                "CP-43": "CC-06", "CP-44": "CC-06", "CP-45": "CC-06", "CP-46": "CC-06", "CP-47": "CC-05",
                "CP-48": "CC-05", "CP-49": "CC-05"}
FALTA_NIVEL = {"I": "prueba de integración con una base MySQL de pruebas (no existe en este conjunto)",
               "S": "prueba de sistema manual (RTF)",
               "A": "medición de accesibilidad o rendimiento"}


def comando(*args):
    try:
        return subprocess.check_output(args, cwd=RAIZ, text=True, stderr=subprocess.DEVNULL).strip()
    except Exception:
        return "?"


def leer_resultados():
    """Lista de (clase, nombre, estado) con estado OK / SALTADA / FALLA."""
    pruebas = []
    for ruta in sorted(glob.glob(os.path.join(SUREFIRE, 'TEST-*.xml'))):
        for tc in ET.parse(ruta).getroot().iter('testcase'):
            if tc.find('failure') is not None or tc.find('error') is not None:
                estado = "FALLA"
            elif tc.find('skipped') is not None:
                estado = "SALTADA"
            else:
                estado = "OK"
            pruebas.append((tc.get('classname').split('.')[-1], tc.get('name'), estado))
    return pruebas


def cobertura():
    """Líneas y ramas del paquete servicio (la meta de la Tabla 3 habla de las clases de servicio)."""
    lm = lc = bm = bc = 0
    with open(JACOCO, encoding='utf-8') as f:
        for fila in csv.DictReader(f):
            if fila['PACKAGE'].endswith('.servicio'):
                lm += int(fila['LINE_MISSED']); lc += int(fila['LINE_COVERED'])
                bm += int(fila['BRANCH_MISSED']); bc += int(fila['BRANCH_COVERED'])
    return lc, lm, bc, bm


def main():
    if not os.path.isdir(SUREFIRE) or not os.path.isfile(JACOCO):
        sys.exit("Faltan los resultados: corré primero `mvn test` dentro de la carpeta SIGRID.")
    pruebas = leer_resultados()
    por_caso = {}
    for clase, nombre, estado in pruebas:
        m = re.match(r'(CP-\d\d):', nombre)
        if m:
            por_caso.setdefault(m.group(1), []).append((clase, nombre, estado))

    catalogo = [l.split('|') for l in CASOS.strip().split('\n')]
    assert len(catalogo) == 53
    resultado = {}
    detalle = {}
    for cp, nivel, critico, req, _ in catalogo:
        ts = por_caso.get(cp, [])
        ok = sum(1 for t in ts if t[2] == 'OK')
        saltadas = [t for t in ts if t[2] == 'SALTADA']
        fallan = sum(1 for t in ts if t[2] == 'FALLA')
        obs = []
        if nivel != 'U':
            res = "No ejecutado"
            obs.append("Falta la " + FALTA_NIVEL[nivel] + ".")
            if cp in PENDIENTE_DE and PENDIENTE_DE[cp] in IMPLEMENTADOS:
                obs.append("El cambio " + PENDIENTE_DE[cp] + " ya está implementado; falta la revisión manual.")
            elif cp in PENDIENTE_DE:
                obs.append("Depende del cambio " + PENDIENTE_DE[cp] + ", todavía sin implementar.")
            if ok:
                obs.append("Evidencia unitaria: %d prueba(s) del servicio aprobadas (no reemplazan ese nivel)." % ok)
        elif fallan or cp in FALLA_POR:
            res = "Falló"
            if cp in FALLA_POR:
                obs.append("Defecto %s: prueba deshabilitada con @Disabled." % FALLA_POR[cp])
            if fallan:
                obs.append("%d prueba(s) fallan en esta ejecución." % fallan)
        elif ok:
            res = "Aprobado"
            if cp in OBSERVA:
                obs.append("Defecto %s abierto sobre un detalle de este caso (prueba deshabilitada)." % OBSERVA[cp])
        else:
            res = "No ejecutado"
            obs.append("No hay prueba automática" +
                       (" (cambio %s todavía no implementado)." % PENDIENTE_DE[cp] if cp in PENDIENTE_DE else "."))
        for t in saltadas:
            if cp not in FALLA_POR and cp not in OBSERVA:
                print("AVISO: prueba saltada sin explicar en", cp, t[1], file=sys.stderr)
        if cp in NOTAS:
            obs.append(NOTAS[cp])
        resultado[cp] = res
        detalle[cp] = " ".join(obs)

    n = lambda r: sum(1 for v in resultado.values() if v == r)
    aprobados, fallidos, noejec = n("Aprobado"), n("Falló"), n("No ejecutado")
    criticos = [c[0] for c in catalogo if c[2] == 'Sí']
    crit_ok = sum(1 for c in criticos if resultado[c] == "Aprobado")
    lc, lm, bc, bm = cobertura()
    lineas = 100.0 * lc / (lc + lm)
    ramas = 100.0 * bc / (bc + bm)
    total = len(pruebas)
    ok_tot = sum(1 for p in pruebas if p[2] == 'OK')
    sal_tot = sum(1 for p in pruebas if p[2] == 'SALTADA')
    fal_tot = sum(1 for p in pruebas if p[2] == 'FALLA')
    sin_cp = sum(1 for p in pruebas if not p[1].startswith('CP-'))
    graves = [d for d in DEFECTOS if d[2] in ("Crítica", "Alta")]

    def si(c):
        return "Sí" if c else "No"

    o = []
    w = o.append
    hoy = datetime.date.today().strftime("%d/%m/%Y")
    w("# Reporte de verificación y validación (V&V) — SIGRID\n")
    w("Generado automáticamente por `docs/calidad/generar_reporte_vv.py` con la plantilla del Apéndice F del plan de "
      "garantía de calidad. Para regenerarlo: `mvn test` dentro de `SIGRID/` y luego "
      "`python docs/calidad/generar_reporte_vv.py` desde la raíz del repositorio.\n")
    w("## F.1 Datos generales\n")
    w("**Tabla 40. Datos generales (reporte de V&V)**\n")
    w("| Campo | Contenido |\n|---|---|")
    w("| Reporte N.º | V&V-01 |")
    w("| Fecha y responsable de la ejecución | %s — [completar: ingeniero de pruebas] |" % hoy)
    w("| Versión del código | rama `%s`, commit `%s` |" % (comando("git", "branch", "--show-current"),
                                                          comando("git", "rev-parse", "--short", "HEAD")))
    w("| Entorno de pruebas | %s, Java %s, Maven; JUnit 5 + Mockito; sin MySQL ni GlassFish |" %
      (platform.system() + " " + platform.release(), "21"))
    w("| Alcance de la ejecución | Parcial: casos de nivel U (unitarios). Los de nivel I, S y A no se ejecutaron |")
    w("| Datos de prueba utilizados | Ficticios, armados en `DatosDePrueba` (reloj fijo: lunes 12/10/2026 10:00) |")
    w("| Informe de cobertura adjunto | Sí (`SIGRID/target/site/jacoco/index.html`) |\n")

    w("## F.2 Resumen de resultados\n")
    w("**Tabla 41. Resumen de resultados (reporte de V&V)**\n")
    w("| Indicador | Meta | Resultado | ¿Cumple? |\n|---|---|---|---|")
    w("| Casos ejecutados | 53 de 53 | %d de 53 | %s |" % (aprobados + fallidos, si(aprobados + fallidos == 53)))
    w("| Casos aprobados, fallidos y no ejecutados | Solo se informa | %d aprobados, %d fallidos, %d no ejecutados | — |"
      % (aprobados, fallidos, noejec))
    w("| Casos críticos aprobados | 100 %% | %d de %d (%.0f %%) | %s |" %
      (crit_ok, len(criticos), 100.0 * crit_ok / len(criticos), si(crit_ok == len(criticos))))
    w("| Cobertura de líneas de los servicios (JaCoCo) | 70 %% o más | %.1f %% (%d de %d líneas) | %s |" %
      (lineas, lc, lc + lm, si(lineas >= 70)))
    w("| Cobertura de ramas de los servicios (JaCoCo) | Solo se informa | %.1f %% (%d de %d ramas) | — |" %
      (ramas, bc, bc + bm))
    w("| Defectos críticos y altos abiertos | 0 | %d (%s) | %s |" %
      (len(graves), ", ".join(d[0] for d in graves) or "ninguno", si(not graves)))
    por_sev = {}
    for d in DEFECTOS:
        por_sev[d[2]] = por_sev.get(d[2], 0) + 1
    w("| Defectos abiertos y cerrados, por severidad | Solo se informa | Abiertos: %s. Cerrados: 0 | — |" %
      (", ".join("%d %s" % (v, k.lower()) for k, v in sorted(por_sev.items())) or "ninguno"))
    w("| Tiempo medio de corrección de un defecto (días) | Solo se informa | Sin datos: todavía no se corrigió ninguno | — |")
    sin_caso = [r for r, cps in MATRIZ if not cps.strip()]
    w("| Requerimientos sin caso de prueba | 0 | %d | %s |" % (len(sin_caso), si(not sin_caso)))
    w("| Contraste en las pantallas clave (CP-47 y CP-48) | 4,5:1 texto normal; 3:1 texto grande y componentes | "
      "No medido | No |")
    w("| Exportación a PDF con 500.000 reservas (CP-45) | 6 de 6 | No ejecutado | No |")
    cc_ok = [cc for cc in ("CC-01", "CC-02", "CC-03", "CC-04", "CC-05", "CC-06")
             if all(resultado["CP-" + c] == "Aprobado" for r, cps in MATRIZ if r.startswith(cc) for c in cps.split())]
    falta_manual = sorted(IMPLEMENTADOS - set(cc_ok))
    w("| Cambios CC-01 a CC-06 verificados | 6 de 6 | %d de 6%s%s | %s |\n" %
      (len(cc_ok), " (" + ", ".join(cc_ok) + ")" if cc_ok else "",
       "; implementados con revisión manual pendiente: " + ", ".join(falta_manual) if falta_manual else "",
       si(len(cc_ok) == 6)))
    w("Pruebas automáticas ejecutadas: **%d** (%d pasan, %d fallan, %d deshabilitadas). %d de ellas no corresponden a "
      "ningún caso del plan: son pruebas de regresión adicionales sobre las demás reglas del sistema.\n" %
      (total, ok_tot, fal_tot, sal_tot, sin_cp))

    w("### Defectos abiertos\n")
    w("La severidad es una propuesta; la asigna el líder de SQA (Tabla 31). Detalle y evidencia en "
      "[resultados-pruebas.md](resultados-pruebas.md).\n")
    w("| Código | Caso | Severidad propuesta | Qué pasa | Estado |\n|---|---|---|---|---|")
    for d in DEFECTOS:
        w("| %s | %s | %s | %s | %s |" % (d[0], d[1], d[2], d[3], d[4]))
    w("")

    w("## F.3 Resultado por caso de prueba\n")
    w("**Tabla 42. Resultado de cada caso de prueba**\n")
    w("| Caso | Descripción | Nivel | Crítico | Resultado | Incidencia y observaciones |\n|---|---|---|---|---|---|")
    for cp, nivel, critico, req, desc in catalogo:
        w("| %s | %s | %s | %s | %s | %s |" % (cp, desc, nivel, critico, resultado[cp], detalle[cp] or "—"))
    w("")

    w("## Matriz de trazabilidad con resultados (Tabla 30)\n")
    w("Un requerimiento figura como *Aprobado* si todos sus casos lo están.\n")
    w("| Requerimiento | Casos | Resultado |\n|---|---|---|")
    for req, cps in MATRIZ:
        ids = ["CP-" + c for c in cps.split()]
        rs = [resultado[i] for i in ids]
        if all(r == "Aprobado" for r in rs):
            r = "Aprobado"
        elif any(r == "Falló" for r in rs):
            r = "Falló (%s)" % ", ".join(i for i in ids if resultado[i] == "Falló")
        else:
            r = "Parcial: %d de %d aprobados" % (sum(1 for x in rs if x == "Aprobado"), len(rs))
        w("| %s | %s | %s |" % (req, ", ".join(ids), r))
    w("")

    w("## Anexo: pruebas automáticas de cada caso\n")
    w("| Caso | Clase | Prueba | Resultado |\n|---|---|---|---|")
    simbolo = {"OK": "pasa", "SALTADA": "deshabilitada", "FALLA": "FALLA"}
    for cp, *_ in catalogo:
        for clase, nombre, estado in sorted(por_caso.get(cp, [])):
            w("| %s | `%s` | %s | %s |" % (cp, clase, nombre.split(': ', 1)[1], simbolo[estado]))
    w("")
    with open(SALIDA, 'w', encoding='utf-8', newline='\n') as f:
        f.write("\n".join(o))
    print("Escrito", SALIDA)
    print("Aprobados %d, fallidos %d, no ejecutados %d; críticos %d/%d; líneas servicios %.1f %%" %
          (aprobados, fallidos, noejec, crit_ok, len(criticos), lineas))


if __name__ == '__main__':
    main()
