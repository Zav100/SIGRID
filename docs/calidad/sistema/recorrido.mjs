// Casos de nivel S (sistema) y A (accesibilidad y adaptación) del plan de garantía, ejecutados contra SIGRID
// desplegado en GlassFish con una base MySQL de PRUEBA. Abre las pantallas con Edge sin ventana, inicia sesión con
// las cuentas de prueba de sigrid_schema.sql, compara lo que se ve con lo que hay en la base y guarda capturas y PDF
// en evidencia/. Deja el resultado de cada caso en resultados.txt, que lee docs/calidad/generar_reporte_vv.py.
//
//   node recorrido.mjs            todos los casos
//   node recorrido.mjs CP-05 CP-36   solo esos
//
// Variables de entorno: SIGRID_URL (por defecto http://localhost:9180/SIGRID), SIGRID_CLAVE (la contraseña de las
// cuentas de prueba, está en sigrid_schema.sql), DB_USER, DB_PASSWORD, DB_NAME (por defecto sigrid_pruebas), MYSQL.
import { execFileSync } from 'node:child_process';
import { existsSync, mkdirSync, readFileSync, writeFileSync } from 'node:fs';
import { dirname, join } from 'node:path';
import { fileURLToPath } from 'node:url';
import { abrirNavegador, ingresar } from './navegador.mjs';
import { medirContraste, medirDesbordes, medirEstados } from './medidas.mjs';

const AQUI = dirname(fileURLToPath(import.meta.url));
const EVID = join(AQUI, 'evidencia');
const BASE = process.env.SIGRID_URL || 'http://localhost:9180/SIGRID';
const CLAVE = process.env.SIGRID_CLAVE;
const DB = process.env.DB_NAME || 'sigrid_pruebas';
const MYSQL = process.env.MYSQL || 'C:/Program Files/MySQL/MySQL Server 8.0/bin/mysql.exe';
const ADMIN = 'admin@sigrid.com';
const SOCIO = 'socio.completo@demo.sigrid';
const PAGINAS_ADMIN = ['inicio', 'calendario', 'socios', 'membresias', 'reservas', 'solicitudes', 'cancelaciones',
  'instalaciones', 'valoraciones', 'reportes'].map((x) => `/admin/${x}-dashboard-admin.xhtml`);
const PAGINAS_SOCIO = ['inicio', 'mis-reservas', 'avisos', 'nueva-reserva'].map((x) => `/socio/${x}.xhtml`);
const PAGINAS_PUBLICAS = ['/index.xhtml', '/instalaciones.xhtml', '/faqpage.xhtml', '/login.xhtml', '/register.xhtml'];

if (!CLAVE || !process.env.DB_USER) {
  console.error('Faltan SIGRID_CLAVE, DB_USER y DB_PASSWORD (ver README.md).');
  process.exit(2);
}
if (DB === 'sigrid') {
  console.error('CP-36 programa un cambio de precios: no se corre contra la base real "sigrid". Usá una de prueba (DB_NAME).');
  process.exit(2);
}

// ---------- apoyo ----------

/** Consulta a la base de prueba: filas como arreglos de texto. */
function sql(consulta) {
  const salida = execFileSync(MYSQL, ['-u' + process.env.DB_USER, '-p' + process.env.DB_PASSWORD, '-N', '-B',
    '--default-character-set=utf8mb4', DB, '-e', consulta], { encoding: 'utf8', stdio: ['ignore', 'pipe', 'ignore'] });
  return salida.split(/\r?\n/).filter((l) => l.length).map((l) => l.split('\t'));
}
const uno = (consulta) => sql(consulta)[0][0];
const dmy = (iso) => iso.slice(8, 10) + '/' + iso.slice(5, 7) + '/' + iso.slice(0, 4);
const monto = (n) => '$ ' + Math.round(Number(n)).toString().replace(/\B(?=(\d{3})+(?!\d))/g, '.');
const sumarDias = (iso, n) => { const d = new Date(iso + 'T12:00:00'); d.setDate(d.getDate() + n); return d.toISOString().slice(0, 10); };
const limpio = (t) => String(t).replace(/\s+/g, ' ').trim();
const textoPdf = (archivo) => execFileSync(process.env.PDFTOTEXT || 'pdftotext', ['-layout', '-enc', 'UTF-8', archivo, '-'], { encoding: 'utf8' });

class Falla extends Error {}
function verificar(condicion, queFallo) {
  if (!condicion) throw new Falla(queFallo);
}
function igual(esperado, obtenido, que) {
  verificar(String(esperado) === String(obtenido), `${que}: se esperaba <${esperado}> y salió <${obtenido}>`);
}

let p; // el navegador
const enPagina = (funcion, ...args) => p.js(`(${funcion.toString()})(...${JSON.stringify(args)})`);
const ruta = async (r) => new URL(await p.ir(BASE + r)).pathname.replace(/^\/SIGRID/, '').replace(/;jsessionid=[^?#]*/, '');
const texto = () => p.js('document.body.innerText');
const evid = (nombre) => join(EVID, nombre);
const rel = (nombre) => 'evidencia/' + nombre;
/** Recorre la página hacia abajo para que aparezcan las secciones que se muestran al llegar a ellas. */
async function bajarHastaElFinal() {
  await p.js(`(async () => { for (let y = 0; y < document.documentElement.scrollHeight; y += 500) { window.scrollTo({ top: y, behavior: 'instant' }); await new Promise(r => setTimeout(r, 120)); } window.scrollTo({ top: 0, behavior: 'instant' }); await new Promise(r => setTimeout(r, 300)); })()`);
}
async function captura(nombre) {
  await bajarHastaElFinal();
  await p.captura(evid(nombre));
  return rel(nombre);
}
/** Lo que una persona hace con "Exportar PDF" de una pantalla común: imprimir la página a PDF. */
async function imprimirPagina(nombre) {
  await p.js('window.__impresiones = 0; sigridImprimirPagina();');
  await p.hasta('window.__impresiones > 0', 5000);
  await p.pdf(evid(nombre));
  await p.js("window.dispatchEvent(new Event('afterprint'))");
  return rel(nombre);
}

// ---------- CP-05 ----------

async function cp05() {
  const evidencia = [];
  await p.tamano(1280, 900);
  await p.olvidarSesion();
  for (const r of [...PAGINAS_ADMIN, ...PAGINAS_SOCIO]) igual('/login.xhtml', await ruta(r), 'sin sesión, ' + r);

  igual('/SIGRID/login.xhtml', await ingresar(p, BASE, ADMIN, 'clave-incorrecta-1'), 'administrador con contraseña incorrecta');
  const aviso = limpio(await p.js("(document.querySelector('.auth-error, .auth-alert, [class*=error], [class*=alert], .ui-messages, ul li') || {}).innerText || ''"));
  evidencia.push(await captura('cp05-clave-incorrecta.png'));
  igual('/login.xhtml', await ruta(PAGINAS_ADMIN[0]), 'después de la contraseña incorrecta, /admin');

  igual('/SIGRID/socio/inicio.xhtml', await ingresar(p, BASE, SOCIO, CLAVE), 'el socio inicia sesión');
  evidencia.push(await captura('cp05-socio-llega-a-su-panel.png'));
  for (const r of PAGINAS_SOCIO) igual(r, await ruta(r), 'socio en ' + r);
  for (const r of PAGINAS_ADMIN) igual('/login.xhtml', await ruta(r), 'socio en ' + r);
  evidencia.push(await captura('cp05-socio-en-admin-vuelve-al-login.png'));
  const comprobante = await p.js(`fetch(${JSON.stringify(BASE + '/admin/comprobante?reserva=1')}, { redirect: 'manual' }).then(r => r.type)`);
  igual('opaqueredirect', comprobante, 'socio pidiendo /admin/comprobante (archivo de un comprobante)');
  igual('/socio/inicio.xhtml', await ruta('/socio/inicio.xhtml'), 'el socio sigue con su sesión después del rebote');

  igual('/SIGRID/admin/inicio-dashboard-admin.xhtml', await ingresar(p, BASE, ADMIN, CLAVE), 'el administrador inicia sesión');
  evidencia.push(await captura('cp05-admin-llega-a-su-panel.png'));
  for (const r of PAGINAS_ADMIN) igual(r, await ruta(r), 'administrador en ' + r);
  const adminEnSocio = await ruta('/socio/inicio.xhtml');

  await p.ir(BASE + PAGINAS_ADMIN[0]);
  await p.clic('.adm-logout a', { navega: true });
  const trasSalir = await p.js('location.pathname');
  igual('/login.xhtml', await ruta(PAGINAS_ADMIN[0]), 'después de cerrar sesión, /admin');
  return {
    detalle: `Sin sesión, las ${PAGINAS_ADMIN.length} pantallas de /admin y las ${PAGINAS_SOCIO.length} de /socio llevan al login. `
      + `Con contraseña incorrecta no entra (aviso: "${aviso}"). El socio llega a /socio/inicio y abre sus ${PAGINAS_SOCIO.length} pantallas; `
      + `en las ${PAGINAS_ADMIN.length} de /admin y en /admin/comprobante vuelve al login. El administrador llega a /admin/inicio y abre sus `
      + `${PAGINAS_ADMIN.length} pantallas; si abre /socio va a ${adminEnSocio}. Al cerrar sesión (queda en ${trasSalir.replace('/SIGRID', '')}) /admin vuelve a pedir login.`,
    evidencia,
  };
}

// ---------- CP-36 ----------

/** Precio vigente hoy y próximo cambio de cada instalación y categoría (o de cada cuota), leídos de la base. */
function tarifasDeLaBase(tabla, hoy) {
  const porInstalacion = tabla === 'tarifa_alquiler';
  const filas = sql(`SELECT ${porInstalacion ? 'i.nombre' : "'Membresía'"}, c.nombre_categoria, t.precio, t.vigente_desde
      FROM ${tabla} t JOIN categoria_socio c ON c.id_categoria_socio = t.id_categoria_socio
      ${porInstalacion ? 'JOIN instalacion i ON i.id_instalacion = t.id_instalacion' : ''} ORDER BY 1, 2, 4`);
  const mapa = new Map();
  for (const [nombre, categoria, precio, desde] of filas) {
    const clave = nombre + '|' + categoria;
    const d = mapa.get(clave) || { nombre, categoria, vigente: null, proxima: null };
    if (desde <= hoy) d.vigente = { precio, desde };
    else if (!d.proxima) d.proxima = { precio, desde };
    mapa.set(clave, d);
  }
  return [...mapa.values()];
}
const textoProximo = (t) => (t.proxima ? `Desde el ${dmy(t.proxima.desde)}: ${monto(t.proxima.precio)}` : 'Sin cambios programados');

async function programarCambio(formulario, guardar, desde, suba) {
  await enPagina((f, fecha, mas) => {
    const form = document.getElementById(f);
    form.closest('details').open = true;
    for (const campo of form.querySelectorAll('input[type=number]')) {
      campo.value = String(Number(campo.value) + mas);
      campo.dispatchEvent(new Event('change', { bubbles: true }));
    }
    const dia = form.querySelector('input[type=date]');
    dia.value = fecha;
    dia.dispatchEvent(new Event('change', { bubbles: true }));
  }, formulario, desde, suba);
  await p.clic(`#${formulario} a.adm-act-ok`, { espera: 1500 });
  return limpio(await p.js("[...document.querySelectorAll('.adm-flash')].map(e => e.innerText).join(' / ')"));
}

async function cp36() {
  const evidencia = [];
  const hoy = uno('SELECT CURDATE()');
  const desde = sumarDias(hoy, 15);
  await p.tamano(1280, 900);
  await ingresar(p, BASE, ADMIN, CLAVE);

  // se programa un cambio desde la pantalla (si esta base ya lo tiene de una corrida anterior, no se repite)
  let avisoPrecios = 'ya estaba programado';
  let avisoCuotas = 'ya estaba programado';
  await p.ir(BASE + '/admin/instalaciones-dashboard-admin.xhtml');
  if (uno(`SELECT COUNT(*) FROM tarifa_alquiler t JOIN instalacion i ON i.id_instalacion = t.id_instalacion WHERE i.nombre = 'Cancha de Fútbol 11' AND t.vigente_desde > CURDATE()`) === '0') {
    await p.clic('a[title="Ver la ficha de Cancha de Fútbol 11"]', { espera: 1200 });
    avisoPrecios = await programarCambio('preciosCambioForm', 'Guardar precios', desde, 1500);
  }
  await p.ir(BASE + '/admin/membresias-dashboard-admin.xhtml');
  if (uno('SELECT COUNT(*) FROM tarifa_membresia WHERE vigente_desde > CURDATE()') === '0') {
    avisoCuotas = await programarCambio('cuotasForm', 'Guardar cuotas', desde, 500);
  }
  const alquiler = tarifasDeLaBase('tarifa_alquiler', hoy);
  const cuotas = tarifasDeLaBase('tarifa_membresia', hoy);
  verificar(alquiler.some((t) => t.proxima), 'no quedó ningún cambio de precio programado en la base (aviso: ' + avisoPrecios + ')');
  verificar(cuotas.some((t) => t.proxima), 'no quedó ningún cambio de cuota programado en la base (aviso: ' + avisoCuotas + ')');

  // administrador: tabla de precios por instalación y categoría
  await p.ir(BASE + '/admin/instalaciones-dashboard-admin.xhtml');
  const tabla = await enPagina(() => {
    const t = document.querySelector('table.ins-precios');
    const categorias = [...t.querySelectorAll('thead th')].slice(1).map((th) => th.textContent.trim());
    const filas = [...t.querySelectorAll('tbody tr')].map((tr) => ({
      nombre: (tr.querySelector('.ins-nombre') || tr.cells[0]).textContent.replace(/^\s*\S+\s+/, (m) => (/^[a-z_]+\s*$/.test(m.trim()) ? '' : m)).trim(),
      celdas: [...tr.querySelectorAll('td.ins-num')].map((td) => ({
        precio: td.childNodes[0].textContent.trim(),
        notas: [...td.querySelectorAll('small')].map((s) => s.textContent.trim()),
      })),
    }));
    return { categorias, filas };
  });
  let comparadas = 0;
  for (const t of alquiler) {
    const fila = tabla.filas.find((f) => f.nombre.endsWith(t.nombre));
    verificar(fila, 'la tabla de precios del administrador no muestra ' + t.nombre);
    const celda = fila.celdas[tabla.categorias.indexOf(t.categoria)];
    verificar(celda, `${t.nombre}: falta la columna ${t.categoria}`);
    const donde = `administrador, ${t.nombre}, ${t.categoria}`;
    igual(monto(t.vigente.precio), celda.precio, donde + ', precio vigente');
    igual('Rige desde el ' + dmy(t.vigente.desde), celda.notas[0], donde + ', desde cuándo rige');
    igual(textoProximo(t), celda.notas[1], donde + ', próximo cambio');
    comparadas++;
  }
  evidencia.push(await captura('cp36-admin-precios-por-instalacion.png'));

  // administrador: cuota mensual
  await p.ir(BASE + '/admin/membresias-dashboard-admin.xhtml');
  const listaCuotas = await enPagina(() => [...document.querySelectorAll('.mem-tarifas li')].map((li) => ({
    categoria: li.querySelector('span').childNodes[0].textContent.trim(),
    notas: [...li.querySelectorAll('small')].map((s) => s.textContent.trim()),
    precio: li.querySelector('strong').textContent.trim(),
  })));
  for (const t of cuotas) {
    const item = listaCuotas.find((c) => c.categoria === t.categoria);
    verificar(item, 'la tarjeta de cuotas no muestra ' + t.categoria);
    const donde = `administrador, cuota de ${t.categoria}`;
    igual(monto(t.vigente.precio), item.precio, donde + ', precio vigente');
    igual('Rige desde el ' + dmy(t.vigente.desde), item.notas[0], donde + ', desde cuándo rige');
    igual(textoProximo(t), item.notas[1], donde + ', próximo cambio');
    comparadas++;
  }
  evidencia.push(await captura('cp36-admin-cuota-mensual.png'));

  // socio: "Tarifas vigentes" de su categoría
  await ingresar(p, BASE, SOCIO, CLAVE);
  const categoria = uno(`SELECT c.nombre_categoria FROM socio s JOIN usuario u ON u.id_usuario = s.id_usuario
      JOIN categoria_socio c ON c.id_categoria_socio = s.id_categoria_socio WHERE u.email = '${SOCIO}'`);
  const lineas = (await enPagina(() => {
    const h = [...document.querySelectorAll('h2, h3')].find((x) => x.textContent.includes('Tarifas vigentes'));
    return h.closest('section, .so-card').innerText;
  })).split('\n').map((l) => l.trim()).filter(Boolean);
  const reservables = new Set(sql("SELECT nombre FROM instalacion WHERE tipo_acceso = 'ARANCELADO' AND estado <> 'DESHABILITADA'").map((f) => f[0]));
  let delSocio = 0;
  for (const t of [...alquiler, ...cuotas].filter((x) => x.categoria === categoria)) {
    if (t.nombre !== 'Membresía' && !reservables.has(t.nombre)) continue;
    const i = lineas.indexOf(t.nombre);
    verificar(i >= 0, `el panel del socio no muestra la tarifa de ${t.nombre}`);
    const bloque = lineas.slice(i, i + 5).join(' | ');
    const donde = `socio (${categoria}), ${t.nombre}`;
    verificar(bloque.includes(monto(t.vigente.precio)), `${donde}, precio vigente ${monto(t.vigente.precio)}: se ve <${bloque}>`);
    verificar(bloque.includes('Rige desde el ' + dmy(t.vigente.desde)), `${donde}, desde cuándo rige: se ve <${bloque}>`);
    verificar(bloque.includes(textoProximo(t)), `${donde}, próximo cambio <${textoProximo(t)}>: se ve <${bloque}>`);
    delSocio++;
  }
  evidencia.push(await captura('cp36-socio-tarifas-vigentes.png'));
  const conCambio = [...alquiler, ...cuotas].filter((t) => t.proxima).length;
  return {
    detalle: `Se programó desde las pantallas un cambio de precios de la Cancha de Fútbol 11 y de las cuotas para el ${dmy(desde)} `
      + `(avisos: "${avisoPrecios}" y "${avisoCuotas}"). Administrador: ${comparadas} precios (instalación por categoría, y las 4 cuotas) `
      + `muestran el precio vigente, "Rige desde el ..." y el próximo cambio ("Desde el ${dmy(desde)}: $ ..." en los ${conCambio} que cambian, `
      + `"Sin cambios programados" en el resto), iguales a los de la base. Socio (${categoria}): sus ${delSocio} tarifas (instalaciones y cuota) `
      + 'muestran lo mismo. El precio vigente no cambió al programar el nuevo.',
    evidencia,
  };
}

// ---------- CP-41 ----------

const VIEJO = /valoraci[oó]n|valoraciones|valorad[oa]s?\b/i;

/** Todo el texto que una persona puede leer en la página: lo visible, lo que sale al imprimir y las ayudas emergentes. */
async function textosDeLaPagina() {
  const pantalla = await texto();
  await p.cdp('Emulation.setEmulatedMedia', { media: 'print' });
  const impresion = await texto();
  await p.cdp('Emulation.setEmulatedMedia', { media: '' });
  const ayudas = await p.js("[document.title, ...[...document.querySelectorAll('[title], [aria-label], [placeholder], [alt]')].map(e => [e.title, e.getAttribute('aria-label'), e.getAttribute('placeholder'), e.getAttribute('alt')].filter(Boolean).join(' '))].join('\\n')");
  return [pantalla, impresion, ayudas].join('\n');
}
const apariciones = (t) => t.split('\n').filter((l) => VIEJO.test(l)).map((l) => limpio(l).slice(0, 70));

async function cp41() {
  const evidencia = [];
  await p.tamano(1280, 900);
  await ingresar(p, BASE, ADMIN, CLAVE);
  const viejos = [];
  let revisadas = 0;
  for (const r of PAGINAS_ADMIN) {
    await p.ir(BASE + r);
    const menu = limpio(await p.js("document.querySelector('.adm-sidebar, aside, nav').innerText"));
    verificar(menu.includes('Grado de satisfacción'), `el menú de ${r} no dice "Grado de satisfacción": <${menu}>`);
    const vistas = r.includes('reportes') ? await p.js("[...document.querySelectorAll('.rep-tabs a, .rep-tab, [role=tab]')].map(a => a.textContent.trim())") : [''];
    for (const vista of vistas.length ? vistas : ['']) {
      if (vista) {
        await enPagina((nombre) => [...document.querySelectorAll('.rep-tabs a, .rep-tab, [role=tab]')].find((a) => a.textContent.trim() === nombre).click(), vista);
        await p.dormir(1500);
      }
      apariciones(await textosDeLaPagina()).forEach((l) => viejos.push(`${r}${vista ? ' (' + vista + ')' : ''}: ${l}`));
      revisadas++;
    }
  }
  await p.ir(BASE + '/admin/valoraciones-dashboard-admin.xhtml');
  igual('Grado de satisfacción', limpio(await p.js("document.querySelector('h1').innerText")), 'título de la pantalla');
  const tituloPestana = await p.js('document.title');
  evidencia.push(await captura('cp41-pantalla-grado-de-satisfaccion.png'));
  const pdfGrado = await imprimirPagina('cp41-grado-de-satisfaccion.pdf');
  evidencia.push(pdfGrado);
  await p.ir(BASE + '/admin/reportes-dashboard-admin.xhtml');
  const resumen = await texto();
  verificar(resumen.includes('Grado de satisfacción'), 'el resumen de reportes no muestra el indicador "Grado de satisfacción"');
  evidencia.push(await captura('cp41-reportes-resumen.png'));
  const pdfReportes = await imprimirPagina('cp41-reportes-resumen.pdf');
  evidencia.push(pdfReportes);
  for (const [nombre, archivo] of [['PDF de Grado de satisfacción', pdfGrado], ['PDF de Reportes', pdfReportes]]) {
    const t = textoPdf(join(AQUI, archivo));
    verificar(t.includes('Grado de satisfacción'), `el ${nombre} no dice "Grado de satisfacción"`);
    apariciones(t).forEach((l) => viejos.push(`${nombre}: ${l}`));
  }
  verificar(viejos.length === 0, 'todavía aparece el nombre anterior en el panel del administrador: ' + viejos.slice(0, 6).join(' // '));

  // panel del socio: no era parte del cambio (decisión pendiente del equipo); se informa cuánto aparece
  await ingresar(p, BASE, 'socio.valorar@demo.sigrid', CLAVE);
  const delSocio = [];
  for (const r of PAGINAS_SOCIO) {
    await p.ir(BASE + r);
    apariciones(await texto()).forEach((l) => delSocio.push(l));
  }
  evidencia.push(await captura('cp41-socio-sigue-diciendo-valoracion.png'));
  return {
    detalle: `Panel del administrador: ${revisadas} vistas (las ${PAGINAS_ADMIN.length} pantallas y las pestañas de Reportes), en pantalla, `
      + 'en la versión para imprimir y en las ayudas emergentes: el menú, el título de la pantalla, el indicador de Reportes y los PDF dicen '
      + `"Grado de satisfacción" y no aparece "Valoraciones" en ningún texto (título de la pestaña del navegador: "${tituloPestana}"). `
      + `Observación: el panel del socio sigue usando "valoración" en ${delSocio.length} textos (por ejemplo "${delSocio[0] || ''}"); `
      + 'el equipo había dejado esa parte para decidir. La dirección de la pantalla sigue siendo valoraciones-dashboard-admin.xhtml.',
    evidencia,
  };
}

// ---------- CP-43, CP-44 y CP-46 ----------

async function filtrarReservas(desde, hasta) {
  await p.ir(BASE + '/admin/reservas-dashboard-admin.xhtml');
  await p.escribir('input[aria-label="Turnos desde el día"]', desde);
  await p.dormir(900);
  await p.escribir('input[aria-label="Turnos hasta el día"]', hasta);
  await p.dormir(900);
}
/** Aprieta "Exportar PDF": devuelve si la página mandó a imprimir y el aviso que mostró. */
async function exportar() {
  await p.js('window.__impresiones = 0');
  await p.clic('#filtros a[title^="Exportar a PDF"]', { espera: 2500 });
  return {
    imprimio: (await p.js('window.__impresiones')) > 0,
    aviso: limpio(await p.js("[...document.querySelectorAll('.adm-flash')].map(e => e.innerText).join(' / ')")),
    hayInforme: await p.js("!!document.querySelector('.rv-informe')"),
  };
}
const leerInforme = () => enPagina(() => ({
  cabecera: Object.fromEntries([...document.querySelectorAll('.rv-informe-cab dt')].map((dt) => [dt.textContent.trim(), dt.nextElementSibling.textContent.trim()])),
  columnas: [...document.querySelectorAll('.rv-informe-tabla thead th')].map((th) => th.textContent.trim()),
  filas: [...document.querySelectorAll('.rv-informe-tabla tbody tr')].map((tr) => [...tr.cells].map((td) => td.textContent.trim())),
}));
const reservasDeLaBase = (desde, hasta) => sql(`SELECT DATE_FORMAT(r.fecha_turno, '%d/%m/%Y'), CONCAT(TIME_FORMAT(t.hora_inicio, '%H:%i'), ' – ', TIME_FORMAT(t.hora_fin, '%H:%i')),
    i.nombre, CONCAT(u.nombre, ' ', u.apellido) FROM reserva r JOIN turno t ON t.id_turno = r.id_turno
    JOIN instalacion i ON i.id_instalacion = t.id_instalacion JOIN socio s ON s.id_socio = r.id_socio
    JOIN usuario u ON u.id_usuario = s.id_usuario WHERE r.fecha_turno BETWEEN '${desde}' AND '${hasta}'
    ORDER BY r.fecha_turno, t.hora_inicio, i.nombre, r.id_reserva`);

async function cp43() {
  await p.tamano(1280, 900);
  await ingresar(p, BASE, ADMIN, CLAVE);
  const dia = uno('SELECT fecha_turno FROM reserva GROUP BY fecha_turno ORDER BY COUNT(*) DESC, fecha_turno DESC LIMIT 1');
  const enBase = reservasDeLaBase(dia, dia);
  await filtrarReservas(dia, dia);
  const r = await exportar();
  verificar(r.imprimio && r.hayInforme, `no se abrió la impresión del informe (aviso: "${r.aviso}")`);
  const informe = await leerInforme();
  igual(enBase.length, informe.filas.length, 'reservas en el informe del ' + dmy(dia));
  igual(enBase.length, informe.cabecera['Reservas'], 'cantidad que dice el encabezado');
  verificar(informe.cabecera['Período'].includes(dmy(dia)), `el período del encabezado no nombra el ${dmy(dia)}: <${informe.cabecera['Período']}>`);
  verificar(/\d{2}\/\d{2}\/\d{4} \d{2}:\d{2}/.test(informe.cabecera['Emitido']), `falta la fecha de emisión: <${informe.cabecera['Emitido']}>`);
  enBase.forEach((b, i) => {
    const f = informe.filas[i];
    verificar(f[0].includes(b[0]) && f[1] === b[1] && f[2] === b[2] && f[3] === b[3],
      `fila ${i + 1}: la base tiene <${b.join(' · ')}> y el informe <${f.slice(0, 4).join(' · ')}>`);
  });
  const archivo = evid('cp43-reservas-de-un-dia.pdf');
  await p.pdf(archivo);
  const pdf = textoPdf(archivo);
  for (const esperado of ['Reporte de reservas', 'Período', dmy(dia), 'Emitido', 'Página 1 de']) {
    verificar(pdf.includes(esperado), `el PDF no contiene "${esperado}"`);
  }
  const otrosDias = (pdf.match(/\b\d{2}\/\d{2}\/\d{4}\b/g) || []).filter((d) => d !== dmy(dia) && d !== informe.cabecera['Emitido'].slice(0, 10));
  verificar(otrosDias.length === 0, 'el PDF nombra otros días: ' + [...new Set(otrosDias)].join(', '));
  await p.js("window.dispatchEvent(new Event('afterprint'))");
  return {
    detalle: `Reservas del ${dmy(dia)}: el informe trae las ${enBase.length} reservas de ese día y solo esas, en el mismo orden y con el mismo `
      + `horario, instalación y socio que la base. Encabezado: "Reporte de reservas", Período "${informe.cabecera['Período']}", Reservas `
      + `${informe.cabecera['Reservas']}, Emitido "${informe.cabecera['Emitido']}". El PDF no nombra ningún otro día y lleva "Página 1 de ...".`,
    evidencia: [rel('cp43-reservas-de-un-dia.pdf')],
  };
}

async function cp44() {
  const evidencia = [];
  await p.tamano(1280, 900);
  await ingresar(p, BASE, ADMIN, CLAVE);
  const ultimo = uno('SELECT MAX(fecha_turno) FROM reserva');
  const desde = sumarDias(ultimo, 30);
  const hasta = sumarDias(ultimo, 36);
  igual(0, reservasDeLaBase(desde, hasta).length, 'reservas en la base en el período elegido');
  await filtrarReservas(desde, hasta);
  const vacio = await exportar();
  verificar(!vacio.imprimio && !vacio.hayInforme, 'se armó un informe para un período sin reservas');
  verificar(/no hay reservas/i.test(vacio.aviso), `no apareció el aviso de que no hay datos: <${vacio.aviso}>`);
  evidencia.push(await captura('cp44-aviso-periodo-sin-reservas.png'));

  await p.ir(BASE + '/admin/reservas-dashboard-admin.xhtml');
  const sinPeriodo = await exportar();
  verificar(!sinPeriodo.imprimio && !sinPeriodo.hayInforme && sinPeriodo.aviso, `sin período se imprimió igual (aviso: "${sinPeriodo.aviso}")`);
  evidencia.push(await captura('cp44-aviso-sin-periodo.png'));

  await filtrarReservas(hasta, desde);
  const alReves = await exportar();
  verificar(!alReves.imprimio && !alReves.hayInforme && alReves.aviso, `con el período al revés se imprimió igual (aviso: "${alReves.aviso}")`);
  return {
    detalle: `Período sin reservas (${dmy(desde)} al ${dmy(hasta)}): no se abre la impresión ni se arma el informe; aviso: "${vacio.aviso}". `
      + `Sin elegir período: "${sinPeriodo.aviso}". Con el período al revés: "${alReves.aviso}".`,
    evidencia,
  };
}

async function cp46() {
  await p.tamano(1280, 900);
  await ingresar(p, BASE, ADMIN, CLAVE);
  const hoy = uno('SELECT CURDATE()');
  const desde = sumarDias(hoy, -30);
  const enBase = reservasDeLaBase(desde, hoy);
  await filtrarReservas(desde, hoy);
  const r = await exportar();
  verificar(r.imprimio && r.hayInforme, `no se abrió la impresión del informe (aviso: "${r.aviso}")`);
  const informe = await leerInforme();
  igual(enBase.length, informe.filas.length, 'reservas en el informe de 30 días');
  // prolijidad: celdas de horario o de monto que se parten en dos renglones al imprimir (hoja A4)
  await p.cdp('Emulation.setEmulatedMedia', { media: 'print' });
  await p.tamano(718, 1000);
  const partidas = await enPagina(() => {
    const alto = (td) => td.getBoundingClientRect().height - parseFloat(getComputedStyle(td).paddingTop) - parseFloat(getComputedStyle(td).paddingBottom);
    const renglon = (td) => parseFloat(getComputedStyle(td).lineHeight) || parseFloat(getComputedStyle(td).fontSize) * 1.5;
    const dosRenglones = (td) => { const r = document.createRange(); r.selectNodeContents(td); return r.getClientRects().length > 1 && r.getBoundingClientRect().height > renglon(td) * 1.5; };
    const filas = [...document.querySelectorAll('.rv-informe-tabla tbody tr')];
    return { horario: filas.filter((tr) => dosRenglones(tr.cells[1])).length, monto: filas.filter((tr) => dosRenglones(tr.cells[5])).length, filas: filas.length, alto: alto(filas[0].cells[0]) };
  });
  const { data } = await p.cdp('Page.captureScreenshot', { format: 'png', clip: { x: 0, y: 0, width: 718, height: 1000, scale: 1 }, captureBeyondViewport: true });
  writeFileSync(evid('cp46-como-sale-la-primera-hoja.png'), Buffer.from(data, 'base64'));
  await p.cdp('Emulation.setEmulatedMedia', { media: '' });
  await p.tamano(1280, 900);
  const archivo = evid('cp46-reservas-de-30-dias.pdf');
  await p.pdf(archivo);
  const paginas = textoPdf(archivo).split('\f').filter((t) => t.trim());
  verificar(paginas.length > 1, 'el PDF salió en una sola página');
  paginas.forEach((t, i) => {
    verificar(t.includes(`Página ${i + 1} de ${paginas.length}`), `la página ${i + 1} no dice "Página ${i + 1} de ${paginas.length}"`);
    verificar(informe.columnas.every((c) => t.includes(c)), `la página ${i + 1} no repite el encabezado de la tabla (${informe.columnas.join(', ')})`);
    verificar(t.includes('Emitido el'), `la página ${i + 1} no lleva la fecha de emisión al pie`);
  });
  const completo = paginas.join(' ');
  verificar(paginas[0].includes(enBase[0][0]) && paginas[paginas.length - 1].includes(enBase[enBase.length - 1][0]),
    'el PDF no empieza en la primera reserva del período y termina en la última');
  igual(enBase.filter((f) => f[2] === 'Salón SUM').length, (completo.match(/Salón SUM/g) || []).length, 'reservas del Salón SUM contadas en el PDF');
  await p.js("window.dispatchEvent(new Event('afterprint'))");

  // los otros "Exportar PDF" del panel: también numeran las páginas
  const otros = [];
  for (const [nombre, r2] of [['socios', '/admin/socios-dashboard-admin.xhtml'], ['membresias', '/admin/membresias-dashboard-admin.xhtml']]) {
    await p.ir(BASE + r2);
    const arch = await imprimirPagina(`cp46-${nombre}.pdf`);
    const pags = textoPdf(join(AQUI, arch)).split('\f').filter((t) => t.trim());
    pags.forEach((t, i) => verificar(t.includes(`Página ${i + 1} de ${pags.length}`), `PDF de ${nombre}: la página ${i + 1} no está numerada`));
    otros.push(`${nombre} (${pags.length} página${pags.length === 1 ? '' : 's'})`);
  }
  return {
    detalle: `Reservas de los últimos 30 días (${dmy(desde)} al ${dmy(hoy)}): ${enBase.length} reservas en ${paginas.length} páginas. Cada página `
      + `repite el encabezado de la tabla (${informe.columnas.join(', ')}) y lleva al pie "Emitido el ..." y "Página N de ${paginas.length}"; `
      + `empieza en la primera reserva del período y termina en la última. Los PDF de ${otros.join(' y de ')} también salen numerados. `
      + 'Impreso con Edge (motor Chromium), que es el que dibuja la numeración. '
      + (partidas.horario + partidas.monto ? `Observación de prolijidad: en hoja A4 el horario se parte en dos renglones en ${partidas.horario} de ${partidas.filas} filas y el monto en ${partidas.monto} ("$" arriba y el importe abajo), por eso entran menos filas por hoja.` : 'Ninguna celda de horario o de monto se parte en dos renglones.'),
    evidencia: [rel('cp46-reservas-de-30-dias.pdf'), rel('cp46-como-sale-la-primera-hoja.png'), rel('cp46-socios.pdf'), rel('cp46-membresias.pdf')],
  };
}

// ---------- CP-49 ----------

async function cp49() {
  const evidencia = [];
  await p.tamano(1280, 900);
  const soloColor = [];
  const conTexto = new Map();
  const revisar = async (pantalla, archivo) => {
    for (const e of await enPagina(medirEstados)) {
      const dice = e.textos.filter(Boolean);
      const esChip = !e.clase.startsWith('cal-ev');
      if (esChip && dice.length) {
        conTexto.set(e.clase, (conTexto.get(e.clase) || new Set()).add(dice[0]));
      } else if (!e.icono && !e.ayuda) {
        soloColor.push(`${pantalla}: ${e.veces} indicadores "${e.clase}" (dicen "${dice[0] || ''}", sin texto ni ícono del estado)`);
      }
    }
    await p.js("document.documentElement.style.filter = 'grayscale(1)'");
    evidencia.push(await captura(archivo));
    await p.js("document.documentElement.style.filter = ''");
  };
  await ingresar(p, BASE, ADMIN, CLAVE);
  for (const [nombre, r] of [['Reservas', 'reservas'], ['Solicitudes pendientes', 'solicitudes'], ['Cancelaciones', 'cancelaciones'],
    ['Calendario', 'calendario'], ['Dashboard', 'inicio'], ['Membresías', 'membresias']]) {
    await p.ir(BASE + `/admin/${r}-dashboard-admin.xhtml`);
    await revisar('administrador, ' + nombre, `cp49-admin-${r}-en-grises.png`);
  }
  const leyenda = limpio(await (async () => { await p.ir(BASE + '/admin/calendario-dashboard-admin.xhtml'); return p.js("(document.querySelector('.cal-legend, .cal-leyenda, [class*=legend], [class*=leyenda]') || {}).innerText || ''"); })());
  await ingresar(p, BASE, SOCIO, CLAVE);
  await p.ir(BASE + '/socio/mis-reservas.xhtml');
  await enPagina(() => [...document.querySelectorAll('.so-seg')].find((a) => a.textContent.includes('Todas')).click());
  await p.dormir(1200);
  await revisar('socio, Mis reservas', 'cp49-socio-mis-reservas-en-grises.png');
  await p.ir(BASE + '/socio/inicio.xhtml');
  await revisar('socio, Inicio', 'cp49-socio-inicio-en-grises.png');

  const estados = [...conTexto.entries()].map(([clase, textos]) => `${clase}: ${[...textos].join(', ')}`).join('; ');
  const detalle = `Etiquetas de estado con texto en las listas del administrador y del socio (${estados}). `
    + `Capturas en escala de grises de 8 pantallas. `;
  verificar(soloColor.length === 0, detalle + 'Pero hay estados que se distinguen solo por el color: ' + soloColor.join(' // ')
    + `. La leyenda ("${leyenda}") también usa solo el color.`);
  return { detalle: detalle + 'Ningún estado depende solo del color.', evidencia };
}

// ---------- CP-51 ----------

async function cp51() {
  const evidencia = [];
  await p.tamano(1280, 900);
  await ingresar(p, BASE, SOCIO, CLAVE);
  await p.ir(BASE + '/socio/mis-reservas.xhtml');
  const leer = async (pestana) => {
    await enPagina((n) => [...document.querySelectorAll('.so-seg')].find((a) => a.textContent.includes(n)).click(), pestana);
    await p.dormir(1200);
    return enPagina(() => ({
      contadores: Object.fromEntries([...document.querySelectorAll('.so-seg')].map((a) => [a.childNodes[0].textContent.trim(), a.querySelector('.so-seg-n').textContent.trim()])),
      tarjetas: [...document.querySelectorAll('article.so-res')].map((a) => ({
        instalacion: a.querySelector('.so-res-name').textContent.trim(),
        cuando: a.querySelector('.so-res-sub').textContent.trim(),
        estado: a.querySelector('.so-chip').textContent.trim(),
      })),
    }));
  };
  const proximas = await leer('Próximas');
  evidencia.push(await captura('cp51-socio-proximas.png'));
  const historial = await leer('Historial');
  evidencia.push(await captura('cp51-socio-historial.png'));
  const todas = await leer('Todas');
  evidencia.push(await captura('cp51-socio-todas.png'));

  const enBase = sql(`SELECT r.id_reserva, i.nombre, DATE_FORMAT(r.fecha_turno, '%d/%m/%Y'),
      CONCAT(TIME_FORMAT(t.hora_inicio, '%H:%i'), ' – ', TIME_FORMAT(t.hora_fin, '%H:%i')), r.estado,
      TIMESTAMP(r.fecha_turno, t.hora_fin) > NOW() AND r.estado IN ('PENDIENTE_PAGO','CONFIRMADA')
      FROM reserva r JOIN turno t ON t.id_turno = r.id_turno JOIN instalacion i ON i.id_instalacion = t.id_instalacion
      JOIN socio s ON s.id_socio = r.id_socio JOIN usuario u ON u.id_usuario = s.id_usuario WHERE u.email = '${SOCIO}'
      ORDER BY r.fecha_turno, t.hora_inicio`);
  igual(enBase.length, todas.tarjetas.length, 'reservas del socio en "Todas"');
  igual(enBase.length, todas.contadores['Todas'], 'contador de "Todas"');
  const esperado = { CONFIRMADA: /confirmada|realizada|completada|finalizada|pedid|valor/i, CANCELADA: /cancelada/i, RECHAZADA: /rechazada/i,
    PENDIENTE_PAGO: /comprobante|pago|revisión/i };
  const pares = new Set();
  for (const [id, instalacion, fecha, horario, estado] of enBase) {
    const t = todas.tarjetas.find((x) => x.instalacion === instalacion && x.cuando.includes(fecha) && x.cuando.includes(horario));
    verificar(t, `la reserva ${id} (${instalacion}, ${fecha} ${horario}) no aparece en el historial del socio`);
    verificar(esperado[estado].test(t.estado), `la reserva ${id} está ${estado} en la base y la pantalla dice "${t.estado}"`);
    pares.add(`${estado} → "${t.estado}"`);
  }
  const vivas = enBase.filter((f) => f[5] === '1');
  igual(vivas.length, proximas.tarjetas.length, 'reservas en "Próximas" (pendientes o confirmadas que todavía no terminaron)');
  igual(enBase.length - vivas.length, historial.tarjetas.length, 'reservas en "Historial"');
  igual(proximas.tarjetas.length, proximas.contadores['Próximas'], 'contador de "Próximas"');
  igual(historial.tarjetas.length, historial.contadores['Historial'], 'contador de "Historial"');
  verificar(todas.tarjetas.every((t) => t.instalacion && t.estado), 'hay una reserva sin instalación o sin estado');
  const usadas = [...new Set(historial.tarjetas.map((t) => t.instalacion))].sort();
  return {
    detalle: `${SOCIO}: "Próximas" ${proximas.tarjetas.length}, "Historial" ${historial.tarjetas.length} y "Todas" ${todas.tarjetas.length}, `
      + `iguales a las ${enBase.length} reservas del socio en la base (${vivas.length} pendientes o confirmadas que todavía no terminaron). `
      + `Cada una muestra instalación, día, horario y estado (${[...pares].join('; ')}). Instalaciones del historial: ${usadas.join(', ')}.`,
    evidencia,
  };
}

// ---------- CP-50 ----------

const ANCHOS = [[360, 740], [768, 1024], [1280, 800]];
// patrones hechos a propósito que la medición no debe contar como desborde
const A_PROPOSITO = {
  '/instalaciones.xhtml': ['.carousel-wrap'], // carrusel de fotos: se recorre con las flechas
  '/register.xhtml': ['#registroWrap'], // los dos pasos del registro se deslizan dentro de un panel
};

async function cp50() {
  const evidencia = [];
  const problemas = [];
  const conBarra = new Map();
  const medidas = [];
  const menus = [];
  const medirGrupo = async (grupo, paginas) => {
    for (const [ancho, alto] of ANCHOS) {
      await p.tamano(ancho, alto);
      for (const r of paginas) {
        await p.ir(BASE + r);
        await bajarHastaElFinal();
        const m = await enPagina(medirDesbordes, A_PROPOSITO[r] || []);
        const nombre = `cp50/${grupo}-${r.split('/').pop().replace('-dashboard-admin', '').replace('.xhtml', '')}-${ancho}.png`;
        await p.captura(evid(nombre));
        evidencia.push(rel(nombre));
        medidas.push({ ruta: r, ...m });
        const donde = `${r} a ${ancho} px`;
        if (m.pagina > 2) problemas.push(`${donde}: la página se desborda ${m.pagina} px hacia la derecha`);
        m.fuera.forEach((t) => problemas.push(`${donde}: "${t.texto}" (${t.elemento}) queda ${t.derecha > ancho ? 'cortado por el borde derecho' : 'fuera de la pantalla'}`));
        m.tapados.forEach((t) => problemas.push(`${donde}: "${t.texto}" (${t.elemento}) queda tapado por ${t.tapadoPor}`));
        m.cortados.forEach((t) => problemas.push(`${donde}: texto cortado en ${t.elemento}: "${t.texto}"`));
        m.estrujados.forEach((t) => problemas.push(`${donde}: texto estrujado en una columna de ${t.ancho} px (${t.renglones} renglones): "${t.texto}"`));
        m.importesPartidos.forEach((t) => problemas.push(`${donde}: el importe "${t.texto}" se parte en dos renglones (${t.elemento})`));
        m.conBarra.forEach((t) => {
          const clave = `${r.split('/').pop().replace('-dashboard-admin', '').replace('.xhtml', '')}: ${t.elemento}`;
          conBarra.set(clave, [...(conBarra.get(clave) || []), ancho]);
          if (ancho === 1280) problemas.push(`${donde}: la tabla (${t.elemento}) no entra en su tarjeta y hay que desplazarla ${t.sobra} px para ver las últimas columnas`);
        });
        // el menú del administrador se pliega en pantallas chicas: tiene que poder abrirse
        if (m.menuPlegado && r === PAGINAS_ADMIN[0]) {
          await p.clic('#admMenuBtn', { espera: 700 });
          const visibles = await p.js("[...document.querySelectorAll('.adm-nav-link')].filter(a => { const b = a.getBoundingClientRect(); return b.left >= 0 && b.right <= innerWidth; }).length");
          const archivo = `cp50/admin-menu-abierto-${ancho}.png`;
          await p.captura(evid(archivo), { completa: false });
          evidencia.push(rel(archivo));
          menus.push(`${ancho} px: ${visibles} opciones a la vista`);
          if (!visibles) problemas.push(`${donde}: el botón de menú no muestra las opciones`);
        }
      }
    }
  };
  await p.olvidarSesion();
  await medirGrupo('publico', PAGINAS_PUBLICAS);
  await p.tamano(1280, 800);
  await ingresar(p, BASE, SOCIO, CLAVE);
  await medirGrupo('socio', PAGINAS_SOCIO);
  await p.tamano(1280, 800);
  await ingresar(p, BASE, ADMIN, CLAVE);
  await medirGrupo('admin', PAGINAS_ADMIN);
  writeFileSync(evid('cp50/medidas.json'), JSON.stringify(medidas, null, 1));
  const barras = [...conBarra.entries()].map(([k, anchos]) => `${k} (${[...new Set(anchos)].join(', ')} px)`);
  const total = PAGINAS_PUBLICAS.length + PAGINAS_SOCIO.length + PAGINAS_ADMIN.length;
  const detalle = `${total} pantallas (${PAGINAS_PUBLICAS.length} públicas, ${PAGINAS_SOCIO.length} del socio y ${PAGINAS_ADMIN.length} del administrador) `
    + `en 360, 768 y 1280 px: ${total * 3} capturas en evidencia/cp50 y las medidas en medidas.json. El menú del administrador se pliega `
    + `y se abre con su botón (${menus.join('; ') || 'no se plegó'}). Tablas con desplazamiento horizontal propio: ${barras.length ? barras.join('; ') : 'ninguna'}. `;
  verificar(problemas.length === 0, detalle + `Problemas (${problemas.length}): ` + problemas.join(' // '));
  return { detalle: detalle + 'Ninguna página se desborda y no hay textos cortados, controles fuera de la pantalla ni tapados.', evidencia: [rel('cp50/')] };
}

// ---------- CP-42 y CP-52 en la pantalla (el caso es de integración: acá se comprueba que la pantalla lo ofrece) ----------

/** Ejecuta una acción que dispara un pedido AJAX de JSF y espera a que termine. */
async function conAjax(accion) {
  await p.js("window.__ajax = 0; if (!window.__oyente) { window.__oyente = true; (window.faces || window.jsf).ajax.addOnEvent((d) => { if (d.status === 'success') window.__ajax++; }); }");
  await accion();
  await p.hasta('window.__ajax > 0', 60000);
  await p.dormir(300);
}
const elegir = (selector, valor) => conAjax(() => enPagina((sel, v) => {
  const s = document.querySelector(sel);
  if (!s) throw new Error('No está ' + sel);
  s.value = String(v);
  s.dispatchEvent(new Event('change', { bubbles: true }));
}, selector, valor));

async function cp42pantalla() {
  await p.tamano(1280, 900);
  await ingresar(p, BASE, ADMIN, CLAVE);
  const [dia, idInstalacion, nombre] = sql(`SELECT r.fecha_turno, i.id_instalacion, i.nombre FROM reserva r
      JOIN turno t ON t.id_turno = r.id_turno JOIN instalacion i ON i.id_instalacion = t.id_instalacion
      WHERE r.estado IN ('PENDIENTE_PAGO','CONFIRMADA') GROUP BY r.fecha_turno, i.id_instalacion, i.nombre
      ORDER BY COUNT(*) DESC, r.fecha_turno DESC LIMIT 1`)[0];
  await p.ir(BASE + '/admin/reportes-dashboard-admin.xhtml');
  await elegir('select[aria-label="Período del reporte"]', 'DIA');
  await conAjax(() => p.escribir('input[aria-label="Día del reporte"]', dia));
  const periodo = limpio(await p.js("document.getElementById('periodoTexto').innerText"));
  verificar(periodo.includes(dmy(dia)), `el período no dice el día elegido: <${periodo}>`);
  await conAjax(() => enPagina(() => [...document.querySelectorAll('.rep-tabs a')].find((a) => a.textContent.includes('Ocupación')).click()));
  await elegir('select[aria-label="Instalación del reporte de ocupación"]', idInstalacion);
  const enPantalla = await enPagina(() => [...document.querySelectorAll('section[aria-labelledby=tTurnos] li')].map((li) => ({
    horario: li.querySelector('.rep-rank-name').textContent.trim(),
    valor: li.querySelector('.rep-rank-num').textContent.trim(),
    detalle: (li.querySelector('.rep-rank-detail') || { textContent: '' }).textContent.trim(),
  })));
  const resumen = limpio(await p.js("(document.getElementById('resumenInstalacion') || {}).innerText || ''"));
  const enBase = sql(`SELECT CONCAT(TIME_FORMAT(t.hora_inicio, '%H:%i'), ' – ', TIME_FORMAT(t.hora_fin, '%H:%i')),
      COALESCE((SELECT CONCAT(u.nombre, ' ', u.apellido) FROM reserva r JOIN socio s ON s.id_socio = r.id_socio
      JOIN usuario u ON u.id_usuario = s.id_usuario WHERE r.id_turno = t.id_turno AND r.fecha_turno = '${dia}'
      AND r.estado IN ('PENDIENTE_PAGO','CONFIRMADA')), '') FROM turno t WHERE t.id_instalacion = ${idInstalacion}
      ORDER BY t.hora_inicio`);
  igual(enBase.length, enPantalla.length, `turnos de ${nombre} el ${dmy(dia)}`);
  enBase.forEach(([horario, socio], k) => {
    const t = enPantalla[k];
    igual(horario, t.horario, `turno ${k + 1}`);
    igual(socio ? 'Reservado' : 'Libre', t.valor, `${horario}`);
    verificar(!socio || t.detalle.startsWith(socio + ' · '), `${horario}: la base dice ${socio} y la pantalla <${t.detalle}>`);
  });
  const reservados = enBase.filter((f) => f[1]).length;
  verificar(resumen.startsWith(nombre) && resumen.includes(`${reservados} de ${enBase.length} turnos`),
    `el resumen no coincide: <${resumen}>`);
  const evidencia = [await captura('cp42-ocupacion-de-un-dia-de-una-instalacion.png')];
  return {
    detalle: `En la pantalla: Reportes → período "Un día" (${dmy(dia)}) → Ocupación y uso → instalación "${nombre}". Muestra "${resumen}" `
      + `y los ${enPantalla.length} turnos uno por uno; los ${reservados} reservados con el socio y su estado, igual que la base.`,
    evidencia,
  };
}

async function cp52pantalla() {
  await p.tamano(1280, 900);
  await ingresar(p, BASE, ADMIN, CLAVE);
  const [nombre, cuantas] = sql(`SELECT i.nombre, COUNT(*) FROM reserva r JOIN turno t ON t.id_turno = r.id_turno
      JOIN instalacion i ON i.id_instalacion = t.id_instalacion GROUP BY i.nombre ORDER BY COUNT(*) DESC LIMIT 1`)[0];
  await p.ir(BASE + '/admin/reservas-dashboard-admin.xhtml');
  await elegir('select[aria-label="Filtrar por instalación"]', nombre);
  const mostrando = async () => limpio(await p.js("(document.body.innerText.match(/Mostrando [\\d.]+ de [\\d.]+/) || [''])[0]"));
  const total = (await mostrando()).split(' de ')[1];
  igual(cuantas, total, `reservas de ${nombre} que informa el listado`);
  const deOtra = await enPagina((n) => [...document.querySelectorAll('#filas table tbody tr')]
    .map((tr) => tr.cells[1].querySelector('.adm-cell-main').textContent.trim()).filter((x) => x !== n), nombre);
  verificar(deOtra.length === 0, 'el listado muestra reservas de otras instalaciones: ' + deOtra.join(', '));
  // instalación, estado y período juntos, y el PDF
  const hoy = uno('SELECT CURDATE()');
  const desde = sumarDias(hoy, -30);
  await elegir('select[aria-label="Filtrar por estado de la reserva"]', 'CONFIRMADA');
  await conAjax(() => p.escribir('input[aria-label="Turnos desde el día"]', desde));
  await conAjax(() => p.escribir('input[aria-label="Turnos hasta el día"]', hoy));
  const combinado = uno(`SELECT COUNT(*) FROM reserva r JOIN turno t ON t.id_turno = r.id_turno JOIN instalacion i ON i.id_instalacion = t.id_instalacion
      WHERE i.nombre = '${nombre}' AND r.estado = 'CONFIRMADA' AND r.fecha_turno BETWEEN '${desde}' AND '${hoy}'`);
  igual(combinado, (await mostrando()).split(' de ')[1], `confirmadas de ${nombre} del ${dmy(desde)} al ${dmy(hoy)}`);
  const evidencia = [await captura('cp52-reservas-filtradas-por-instalacion.png')];
  await p.js('window.__impresiones = 0');
  await conAjax(() => p.clic('#filtros a[title^="Exportar a PDF"]', { espera: 0 }));
  await p.dormir(300);
  const filtros = limpio(await p.js("(() => { const dt = [...document.querySelectorAll('.rv-informe-cab dt')].find(d => d.textContent.trim() === 'Filtros'); return dt ? dt.nextElementSibling.textContent : ''; })()"));
  const filasPdf = await p.js("document.querySelectorAll('.rv-informe-tabla tbody tr').length");
  verificar(filtros.includes('Instalación: ' + nombre), `el PDF no dice la instalación elegida: <${filtros}>`);
  igual(combinado, filasPdf, 'reservas en el PDF');
  await p.pdf(evid('cp52-pdf-de-una-instalacion.pdf'));
  evidencia.push(rel('cp52-pdf-de-una-instalacion.pdf'));
  return {
    detalle: `En la pantalla: Reservas → filtro "Todas las instalaciones" → "${nombre}": el listado informa ${total} reservas, `
      + `las mismas que la base, y todas las filas son de esa instalación. Con instalación, estado "Confirmadas" y período `
      + `${dmy(desde)} al ${dmy(hoy)}: ${combinado}, igual que la base; el PDF las trae y dice "${filtros}".`,
    evidencia,
  };
}

// ---------- CP-45 ----------

const META_MS = 5000; // Tabla 3 del plan: "5 s o menos [A CONFIRMAR: tiempo máximo]"

/** Memoria que usa la JVM de GlassFish en este momento (MB), si se configuró ASADMIN. */
function memoriaServidor() {
  if (!process.env.ASADMIN) return null;
  const salida = execFileSync(process.env.ASADMIN, ['--port', process.env.ASADMIN_PUERTO || '9148', 'generate-jvm-report', '--type', 'memory'],
    { encoding: 'utf8', shell: true, stdio: ['ignore', 'pipe', 'ignore'] });
  const m = salida.split('Heap Memory Usage:')[1].match(/uses at this time: ([\d.,]+) Bytes/);
  return Math.round(Number(m[1].replace(/[.,]/g, '')) / 1048576);
}

async function cp45() {
  const total = Number(uno('SELECT COUNT(*) FROM reserva'));
  verificar(total >= 500000, `la base tiene ${total} reservas: para este caso hay que cargar docs/calidad/cp45/carga_500k.sql (ver README)`);
  await p.tamano(1280, 900);
  await ingresar(p, BASE, ADMIN, CLAVE);
  const tiempos = [];
  const lento = [];
  const medir = async (que, accion, limite = META_MS) => {
    const t = Date.now();
    const dato = await accion();
    const ms = Date.now() - t;
    tiempos.push(`${que}: ${(ms / 1000).toFixed(1)} s${dato ? ' (' + dato + ')' : ''}`);
    if (limite && ms > limite) lento.push(`${que} tardó ${(ms / 1000).toFixed(1)} s`);
    return dato;
  };
  const esperarAjax = async (accion) => {
    await p.js("window.__ajax = 0; if (!window.__oyente) { window.__oyente = true; (window.faces || window.jsf).ajax.addOnEvent((d) => { if (d.status === 'success') window.__ajax++; }); }");
    await accion();
    await p.hasta('window.__ajax > 0', 120000);
  };
  const mostrando = () => p.js("(document.body.innerText.match(/Mostrando [\\d.]+ de [\\d.]+/) || [''])[0]");
  const memoria = [];
  const anotarMemoria = (cuando) => { const mb = memoriaServidor(); if (mb !== null) memoria.push(`${cuando} ${mb} MB`); };
  anotarMemoria('al empezar');

  await medir('abrir Reservas (indicadores, rankings y primeras 25 filas)', async () => { await p.ir(BASE + '/admin/reservas-dashboard-admin.xhtml'); return mostrando(); });
  const hoy = uno('SELECT CURDATE()');
  const semana = [sumarDias(hoy, -6), hoy];
  const filtrar = async (desde, hasta) => {
    await esperarAjax(() => p.escribir('input[aria-label="Turnos desde el día"]', desde));
    await esperarAjax(() => p.escribir('input[aria-label="Turnos hasta el día"]', hasta));
    return mostrando();
  };
  await medir('filtrar una semana en el listado', () => filtrar(...semana));
  const exportarYMedir = async (que, desde, hasta) => {
    await filtrar(desde, hasta);
    await p.js("window.__impresiones = 0; document.querySelectorAll('.adm-flash').forEach(e => e.remove())");
    const r = await medir(`exportar PDF, ${que}`, async () => {
      await esperarAjax(() => p.clic('#filtros a[title^="Exportar a PDF"]', { espera: 0 }));
      await p.dormir(300);
      const filas = await p.js("document.querySelectorAll('.rv-informe-tabla tbody tr').length");
      const aviso = limpio(await p.js("[...document.querySelectorAll('.adm-flash')].map(e => e.innerText).join(' ')"));
      return (await p.js('window.__impresiones')) > 0 ? `${filas} reservas` : `aviso: ${aviso}`;
    });
    return r;
  };
  const dia = await exportarYMedir('un día', hoy, hoy);
  verificar(/reservas/.test(dia), 'el PDF de un día no se armó: ' + dia);
  const unDia = Number(dia.match(/\d+/)[0]);
  igual(uno(`SELECT COUNT(*) FROM reserva WHERE fecha_turno = '${hoy}'`), unDia, 'reservas del PDF de un día contra la base');
  await medir('imprimir a PDF el informe de un día (en el navegador)', async () => { await p.pdf(evid('cp45-pdf-de-un-dia-con-500000.pdf')); return ''; });
  await p.js("window.dispatchEvent(new Event('afterprint'))");
  const sem = await exportarYMedir('una semana', ...semana);
  const mes = await exportarYMedir('un mes', sumarDias(hoy, -29), hoy);
  verificar(/acot|2\.?000/i.test(mes), 'con más de 2.000 reservas el PDF tendría que pedir acotar el período: ' + mes);
  const anio = await exportarYMedir('un año', sumarDias(hoy, -364), hoy);
  anotarMemoria('después de los PDF');
  await p.ir(BASE + '/admin/reservas-dashboard-admin.xhtml');
  await medir('buscar un nombre en todo el historial (sin período)', async () => {
    // el buscador busca al soltar una tecla (keyup), como cuando la persona escribe
    await esperarAjax(async () => { await p.escribir('input[aria-label="Buscar reservas"]', 'Tomás'); await p.js("document.querySelector('input[aria-label=\"Buscar reservas\"]').dispatchEvent(new KeyboardEvent('keyup', { key: 's', bubbles: true }))"); });
    return mostrando();
  });
  // reportes: no son parte de CP-45, se informan por la observación de RNF-07
  const reportes = [];
  await medir('abrir Reportes (últimos 30 días)', async () => { await p.ir(BASE + '/admin/reportes-dashboard-admin.xhtml'); return ''; }, 0);
  reportes.push(tiempos[tiempos.length - 1]);
  await medir('Reportes, cambiar a 12 meses', async () => {
    await esperarAjax(() => enPagina(() => { const s = [...document.querySelectorAll('select')].find((x) => [...x.options].some((o) => o.value === '12M')); s.value = '12M'; s.dispatchEvent(new Event('change', { bubbles: true })); }));
    return '';
  }, 0);
  reportes.push(tiempos[tiempos.length - 1]);
  tiempos.length -= 2;
  anotarMemoria('después de Reportes 12 meses');
  await p.captura(evid('cp45-reportes-12-meses-con-500000.png'), { completa: false });
  const oom = existsSync(process.env.SERVER_LOG || '') ? (readFileSync(process.env.SERVER_LOG, 'utf8').match(/OutOfMemoryError/g) || []).length : null;
  const detalle = `Con ${total.toLocaleString('es-AR')} reservas en la base, contra la aplicación desplegada (GlassFish con 512 MB de memoria) y con el `
    + `navegador: ${tiempos.join('; ')}. Meta de la Tabla 3: ${META_MS / 1000} s o menos [A CONFIRMAR en el plan]. `
    + `Un PDF de una semana: ${sem}; de un año: ${anio}. `
    + (memoria.length ? `Memoria del servidor: ${memoria.join(', ')}. ` : '')
    + (oom === null ? '' : `OutOfMemoryError en el registro del servidor: ${oom}. `)
    + `Fuera del caso (RNF-07, reportes): ${reportes.join('; ')}.`;
  verificar(lento.length === 0 && !oom, detalle + ' No cumple: ' + lento.join('; '));
  return { detalle, evidencia: [rel('cp45-pdf-de-un-dia-con-500000.pdf'), rel('cp45-reportes-12-meses-con-500000.png')] };
}

// ---------- CP-47 y CP-48 ----------

async function contrasteDe(caso, vistas) {
  const informe = [];
  let medidos = 0;
  let sobreImagen = 0;
  let redundantes = 0;
  const fallas = [];
  for (const v of vistas) {
    await v.abrir();
    const m = await enPagina(medirContraste);
    informe.push({ vista: v.nombre, ...m });
    medidos += m.medidos;
    sobreImagen += m.sobreImagen.length;
    redundantes += m.iconosConTexto;
    m.fallas.forEach((f) => fallas.push({ vista: v.nombre, ...f }));
  }
  mkdirSync(EVID, { recursive: true });
  writeFileSync(evid(`${caso}-contraste.json`), JSON.stringify(informe, null, 1));
  const porPar = new Map();
  for (const f of fallas) {
    const clave = `${f.tipo}|${f.color}|${f.fondo}`;
    const d = porPar.get(clave) || { ...f, vistas: new Set(), veces: 0 };
    d.vistas.add(f.vista);
    d.veces += f.veces;
    d.razon = Math.min(d.razon, f.razon);
    porPar.set(clave, d);
  }
  const lista = [...porPar.values()].sort((a, b) => a.razon - b.razon);
  const detalle = `${vistas.length} vistas, ${medidos} textos, íconos y campos medidos con la fórmula de WCAG 2.1 sobre los colores que calcula el `
    + `navegador (detalle en evidencia/${caso}-contraste.json). ${sobreImagen} textos sobre una imagen no se pueden medir así; `
    + `${redundantes} íconos acompañan a un texto que dice lo mismo y no se exigen. `;
  verificar(lista.length === 0, detalle + `No llegan al mínimo ${lista.length} combinaciones de color (${fallas.reduce((s, f) => s + f.veces, 0)} apariciones): `
    + lista.slice(0, 12).map((f) => `${f.tipo} ${f.color} sobre ${f.fondo}${f.degradado ? ' (degradado, peor caso)' : ''} = ${f.razon.toFixed(2)}:1 (mínimo ${f.umbral}:1; ${f.elemento}, "${f.ejemplo}", ${f.veces} veces en ${[...f.vistas].slice(0, 3).join(', ')})`).join(' // ')
    + (lista.length > 12 ? ` // y ${lista.length - 12} más` : ''));
  return { detalle: detalle + 'Todo llega al mínimo: 4,5:1 el texto normal y 3:1 el texto grande, los íconos sin texto y el contorno de los campos.',
    evidencia: [rel(`${caso}-contraste.json`)] };
}

async function cp47() {
  await p.tamano(1280, 900);
  await ingresar(p, BASE, SOCIO, CLAVE);
  const vistas = PAGINAS_SOCIO.map((r) => ({ nombre: r, abrir: () => p.ir(BASE + r) }));
  vistas.push({
    nombre: '/socio/mis-reservas.xhtml (Todas)',
    abrir: async () => {
      await p.ir(BASE + '/socio/mis-reservas.xhtml');
      await enPagina(() => [...document.querySelectorAll('.so-seg')].find((a) => a.textContent.includes('Todas')).click());
      await p.dormir(1200);
    },
  });
  for (const [correo, que] of [['socio.vencido@demo.sigrid', 'membresía vencida'], ['socio.valorar@demo.sigrid', 'calificación pendiente']]) {
    vistas.push({ nombre: `/socio/inicio.xhtml (${que})`, abrir: async () => { await ingresar(p, BASE, correo, CLAVE); await p.ir(BASE + '/socio/inicio.xhtml'); } });
    vistas.push({ nombre: `/socio/mis-reservas.xhtml (${que})`, abrir: () => p.ir(BASE + '/socio/mis-reservas.xhtml') });
  }
  return contrasteDe('cp47', vistas);
}

async function cp48() {
  await p.tamano(1280, 900);
  await ingresar(p, BASE, ADMIN, CLAVE);
  const vistas = PAGINAS_ADMIN.filter((r) => !r.includes('reportes')).map((r) => ({ nombre: r, abrir: () => p.ir(BASE + r) }));
  await p.ir(BASE + '/admin/reportes-dashboard-admin.xhtml');
  const pestanas = await p.js("[...document.querySelectorAll('.rep-tabs a, .rep-tab, [role=tab]')].map(a => a.textContent.trim())");
  for (const nombre of pestanas) {
    vistas.push({
      nombre: `/admin/reportes (${nombre})`,
      abrir: async () => {
        await p.ir(BASE + '/admin/reportes-dashboard-admin.xhtml');
        await enPagina((n) => [...document.querySelectorAll('.rep-tabs a, .rep-tab, [role=tab]')].find((a) => a.textContent.trim() === n).click(), nombre);
        await p.dormir(1500);
      },
    });
  }
  return contrasteDe('cp48', vistas);
}

// ---------- principal ----------

const CASOS = {
  'CP-05': ['Iniciar sesión como socio y como administrador; un socio abre una ruta de /admin', cp05],
  'CP-36': ['Abrir las pantallas de tarifas del socio y del administrador', cp36],
  'CP-41': ['Revisar menú, títulos, reportes y PDF ("Grado de satisfacción")', cp41],
  'CP-42': ['Reporte de ocupación de un día para una instalación (en la pantalla)', cp42pantalla],
  'CP-43': ['Exportar el PDF de reservas de un solo día', cp43],
  'CP-44': ['Exportar el PDF de un período sin reservas', cp44],
  'CP-45': ['Con 500.000 reservas, listar y exportar un período acotado', cp45],
  'CP-46': ['Exportar un PDF de varias páginas', cp46],
  'CP-47': ['Medir el contraste de las pantallas del socio', cp47],
  'CP-48': ['Medir el contraste de las pantallas del administrador', cp48],
  'CP-49': ['Revisar los estados de reserva sin depender solo del color', cp49],
  'CP-50': ['Abrir las pantallas principales en 360, 768 y 1280 px', cp50],
  'CP-51': ['El socio abre su historial de reservas', cp51],
  'CP-52': ['El administrador filtra el listado de reservas por fecha, instalación y estado (en la pantalla)', cp52pantalla],
};

// CP-45 necesita la base con 500.000 reservas: se corre aparte, al final (ver README.md)
const pedidos = process.argv.slice(2).length ? process.argv.slice(2) : Object.keys(CASOS).filter((c) => c !== 'CP-45');
const archivo = join(AQUI, 'resultados.txt');
const anteriores = new Map();
if (existsSync(archivo)) {
  for (const l of readFileSync(archivo, 'utf8').split(/\r?\n/)) if (l.startsWith('CP-')) anteriores.set(l.split('|')[0], l);
}
mkdirSync(EVID, { recursive: true });
const MINUTOS_POR_CASO = 10;

/** Escribe resultados.txt con lo que hay hasta ahora (se llama después de cada caso, para no perder lo hecho). */
function guardar() {
  const ahora = new Date();
  const fecha = ahora.toLocaleDateString('es-AR', { day: '2-digit', month: '2-digit', year: 'numeric' }) + ' '
    + ahora.toLocaleTimeString('es-AR', { hour: '2-digit', minute: '2-digit', hour12: false });
  writeFileSync(archivo, `# Pruebas de sistema y de accesibilidad de SIGRID. Última ejecución: ${fecha}, contra ${BASE} (GlassFish 7, `
    + `Edge sin ventana) con la base de prueba ${DB} (MySQL ${uno('SELECT VERSION()')}). Formato: caso|resultado|detalle|evidencia\n`
    + [...anteriores.keys()].sort().map((k) => anteriores.get(k)).join('\n') + '\n', 'utf8');
}

p = await abrirNavegador();
let fallaron = 0;
try {
  for (const codigo of pedidos) {
    const [titulo, correr] = CASOS[codigo];
    let estado = 'Aprobado';
    let detalle;
    let evidencia = [];
    let reloj;
    try {
      // si el navegador se cuelga, el caso termina con error en vez de quedar esperando para siempre
      const limite = new Promise((_, mal) => { reloj = setTimeout(() => mal(new Error(`el caso tardó más de ${MINUTOS_POR_CASO} minutos`)), MINUTOS_POR_CASO * 60000); });
      ({ detalle, evidencia } = await Promise.race([correr(), limite]));
    } catch (e) {
      estado = e instanceof Falla ? 'Falló' : 'Error';
      detalle = e instanceof Falla ? e.message : 'ERROR DE LA PRUEBA (no del sistema): ' + (e.stack || e);
      fallaron++;
      if (!(e instanceof Falla)) {
        await p.cerrar();
        p = await abrirNavegador();
      }
    } finally {
      clearTimeout(reloj);
    }
    detalle = limpio(detalle).replace(/\|/g, '/');
    anteriores.set(codigo, [codigo, estado, detalle, (evidencia || []).join(';')].join('|'));
    guardar();
    console.log(`${codigo} ${titulo}\n   -> ${estado.toUpperCase()}: ${detalle}\n`);
  }
} finally {
  await p.cerrar();
}
console.log('Escrito ' + archivo);
process.exit(fallaron ? 1 : 0);
