// Maneja Microsoft Edge sin ventana por el protocolo de DevTools, con lo que ya trae Node 22 o superior
// (fetch y WebSocket): no instala nada. Lo usan los recorridos de docs/calidad/sistema para abrir las
// pantallas de SIGRID desplegado, iniciar sesión, sacar capturas e imprimir a PDF como lo haría una persona.
import { spawn } from 'node:child_process';
import { mkdirSync, mkdtempSync, rmSync, writeFileSync } from 'node:fs';
import { tmpdir } from 'node:os';
import { dirname, join } from 'node:path';

const dormir = (ms) => new Promise((ok) => setTimeout(ok, ms));

export async function abrirNavegador({
  edge = process.env.EDGE || 'C:/Program Files (x86)/Microsoft/Edge/Application/msedge.exe',
  puerto = 9333,
} = {}) {
  const perfil = mkdtempSync(join(tmpdir(), 'sigrid-edge-'));
  const proceso = spawn(edge, ['--headless=new', `--remote-debugging-port=${puerto}`, `--user-data-dir=${perfil}`,
    '--no-first-run', '--disable-gpu', '--hide-scrollbars', '--lang=es-AR', 'about:blank'], { stdio: 'ignore' });
  let direccion;
  for (let i = 0; i < 100 && !direccion; i++) {
    try {
      direccion = (await (await fetch(`http://127.0.0.1:${puerto}/json/version`)).json()).webSocketDebuggerUrl;
    } catch {
      await dormir(200);
    }
  }
  if (!direccion) throw new Error('Edge no respondió en el puerto ' + puerto);
  const socket = new WebSocket(direccion);
  await new Promise((ok, mal) => { socket.onopen = ok; socket.onerror = mal; });
  let numero = 0;
  const pendientes = new Map();
  const oyentes = new Set();
  socket.onmessage = (m) => {
    const d = JSON.parse(m.data);
    if (d.id) {
      const p = pendientes.get(d.id);
      pendientes.delete(d.id);
      if (d.error) p.mal(new Error(d.error.message)); else p.ok(d.result);
    } else {
      oyentes.forEach((f) => f(d));
    }
  };
  const enviar = (method, params = {}, sessionId) => new Promise((ok, mal) => {
    pendientes.set(++numero, { ok, mal });
    socket.send(JSON.stringify({ id: numero, method, params, sessionId }));
  });
  const { targetId } = await enviar('Target.createTarget', { url: 'about:blank' });
  const { sessionId } = await enviar('Target.attachToTarget', { targetId, flatten: true });
  const cdp = (method, params) => enviar(method, params, sessionId);
  await cdp('Page.enable');
  await cdp('Runtime.enable');
  await cdp('Network.enable');
  // window.print() no hace nada sin ventana: se anota que la página lo pidió y el PDF se saca con pdf()
  await cdp('Page.addScriptToEvaluateOnNewDocument', { source: 'window.__impresiones = 0; window.print = () => { window.__impresiones++; };' });
  oyentes.add((d) => {
    if (d.method === 'Page.javascriptDialogOpening') cdp('Page.handleJavaScriptDialog', { accept: true });
  });

  const esperarEvento = (metodo, ms = 30000) => new Promise((ok, mal) => {
    const f = (d) => { if (d.method === metodo) { oyentes.delete(f); clearTimeout(t); ok(d.params); } };
    const t = setTimeout(() => { oyentes.delete(f); mal(new Error('No llegó ' + metodo)); }, ms);
    oyentes.add(f);
  });

  let ventana = { width: 1280, height: 900, deviceScaleFactor: 1, mobile: false };
  const pagina = {
    cdp,
    dormir,
    /** Abre una dirección y espera a que termine de cargar. Devuelve la dirección final (después de redirecciones). */
    async ir(url) {
      const cargada = esperarEvento('Page.loadEventFired');
      await cdp('Page.navigate', { url });
      await cargada;
      await dormir(400);
      return pagina.js('location.href');
    },
    /** Evalúa JavaScript en la página y devuelve el valor. */
    async js(expresion) {
      const r = await cdp('Runtime.evaluate', { expression: expresion, awaitPromise: true, returnByValue: true });
      if (r.exceptionDetails) {
        throw new Error('JS: ' + (r.exceptionDetails.exception?.description || r.exceptionDetails.text) + '\n' + expresion.slice(0, 200));
      }
      return r.result.value;
    },
    /** Espera hasta que la expresión sea verdadera. */
    async hasta(expresion, ms = 15000) {
      const fin = Date.now() + ms;
      while (Date.now() < fin) {
        if (await pagina.js(`!!(${expresion})`).catch(() => false)) return true;
        await dormir(150);
      }
      throw new Error('No se cumplió: ' + expresion);
    },
    /** Escribe en un campo como lo haría el teclado (dispara input y change). */
    async escribir(selector, texto) {
      await pagina.js(`(() => { const e = document.querySelector(${JSON.stringify(selector)});
        if (!e) throw new Error('No está ' + ${JSON.stringify(selector)});
        e.focus(); e.value = ${JSON.stringify(texto)};
        e.dispatchEvent(new Event('input', { bubbles: true })); e.dispatchEvent(new Event('change', { bubbles: true })); })()`);
    },
    /** Hace clic; si navega es true espera la página nueva, si no deja un momento para el AJAX. */
    async clic(selector, { navega = false, espera = 900 } = {}) {
      const cargada = navega ? esperarEvento('Page.loadEventFired') : null;
      await pagina.js(`(() => { const e = document.querySelector(${JSON.stringify(selector)});
        if (!e) throw new Error('No está ' + ${JSON.stringify(selector)}); e.click(); })()`);
      if (cargada) await cargada;
      await dormir(navega ? 400 : espera);
    },
    /** Ancho y alto de la ventana en píxeles CSS (por debajo de 500 se comporta como un teléfono). */
    async tamano(ancho, alto) {
      ventana = { width: ancho, height: alto, deviceScaleFactor: 1, mobile: ancho < 500 };
      await cdp('Emulation.setDeviceMetricsOverride', ventana);
      await dormir(300);
    },
    /**
     * Captura PNG de la página entera (o solo de lo visible). Para la página entera se agranda la ventana hasta el
     * alto del contenido y se captura lo que se ve; si la página depende del alto de la ventana (una portada que
     * ocupa "toda la pantalla") se captura por fuera de la ventana, que es menos fiel con elementos fijos.
     */
    async captura(archivo, { completa = true } = {}) {
      const params = { format: 'png' };
      if (completa) {
        const alto = async () => Math.ceil((await cdp('Page.getLayoutMetrics')).cssContentSize.height);
        const antes = await alto();
        await cdp('Emulation.setDeviceMetricsOverride', { ...ventana, height: Math.min(antes, 12000) });
        await dormir(500);
        const despues = await alto();
        if (despues > antes * 1.1) {
          await cdp('Emulation.setDeviceMetricsOverride', ventana);
          await dormir(400);
          const { cssContentSize: c } = await cdp('Page.getLayoutMetrics');
          params.captureBeyondViewport = true;
          params.clip = { x: 0, y: 0, width: Math.ceil(c.width), height: Math.min(Math.ceil(c.height), 12000), scale: 1 };
        }
      }
      const { data } = await cdp('Page.captureScreenshot', params);
      if (completa) {
        await cdp('Emulation.setDeviceMetricsOverride', ventana);
        await dormir(200);
      }
      mkdirSync(dirname(archivo), { recursive: true });
      writeFileSync(archivo, Buffer.from(data, 'base64'));
      return archivo;
    },
    /** Lo mismo que "Guardar como PDF" del diálogo de impresión, con la hoja de estilos de impresión de la página. */
    async pdf(archivo) {
      const { data } = await cdp('Page.printToPDF', { printBackground: true, preferCSSPageSize: true });
      mkdirSync(dirname(archivo), { recursive: true });
      writeFileSync(archivo, Buffer.from(data, 'base64'));
      return archivo;
    },
    /** Cierra la sesión del sitio (borra las cookies). */
    async olvidarSesion() {
      await cdp('Network.clearBrowserCookies');
    },
    async cerrar() {
      try { await enviar('Browser.close'); } catch { /* ya estaba cerrado */ }
      socket.close();
      proceso.kill();
      await dormir(500);
      try { rmSync(perfil, { recursive: true, force: true }); } catch { /* Windows a veces lo suelta tarde */ }
    },
  };
  return pagina;
}

/** Inicia sesión en SIGRID por el formulario de login. Devuelve la dirección a la que llegó. */
export async function ingresar(pagina, base, correo, clave) {
  await pagina.olvidarSesion();
  await pagina.ir(base + '/login.xhtml');
  await pagina.escribir('#loginForm\\:email', correo);
  await pagina.escribir('#loginForm\\:password', clave);
  await pagina.clic('#loginForm input[type=submit]', { navega: true });
  return (await pagina.js('location.pathname')).replace(/;jsessionid=[^?#]*/, '');
}
