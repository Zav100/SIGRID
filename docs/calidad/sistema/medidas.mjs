// Funciones que se ejecutan DENTRO de la página (se inyectan como texto): miden contraste (CP-47 y CP-48),
// desbordes y superposiciones (CP-50) y cómo se muestran los estados (CP-49). No usan nada de fuera del navegador.

/**
 * Contraste de todo el texto visible, con la fórmula de WCAG 2.1 (luminancia relativa) sobre los colores calculados
 * por el navegador. Umbral 4,5:1 para texto normal y 3:1 para texto grande (24 px, o 18,66 px en negrita), para los
 * íconos que son lo único que identifica un control o un dato, y para el contorno de los campos.
 * El fondo de cada texto se obtiene componiendo los fondos de sus ancestros; si hay un degradado se toma su color
 * menos favorable (peor caso), y si hay una imagen se anota aparte porque no se puede medir.
 * No se miden: los íconos que acompañan a un texto que dice lo mismo (son redundantes), los controles deshabilitados
 * y lo que solo existe para lectores de pantalla.
 */
export function medirContraste() {
  const leer = (c) => {
    let m = c && c.match(/rgba?\(([^)]+)\)/);
    if (m) {
      const v = m[1].split(/[,\s/]+/).filter(Boolean).map(parseFloat);
      return { r: v[0], g: v[1], b: v[2], a: v.length > 3 ? v[3] : 1 };
    }
    m = c && c.match(/color\(srgb ([\d.]+) ([\d.]+) ([\d.]+)(?: \/ ([\d.]+))?\)/);
    return m ? { r: m[1] * 255, g: m[2] * 255, b: m[3] * 255, a: m[4] === undefined ? 1 : +m[4] } : null;
  };
  const sobre = (arriba, abajo) => ({
    r: arriba.r * arriba.a + abajo.r * (1 - arriba.a), g: arriba.g * arriba.a + abajo.g * (1 - arriba.a),
    b: arriba.b * arriba.a + abajo.b * (1 - arriba.a), a: 1,
  });
  const lum = (c) => {
    const f = (v) => { v /= 255; return v <= 0.03928 ? v / 12.92 : Math.pow((v + 0.055) / 1.055, 2.4); };
    return 0.2126 * f(c.r) + 0.7152 * f(c.g) + 0.0722 * f(c.b);
  };
  const razon = (a, b) => { const x = lum(a), y = lum(b); return (Math.max(x, y) + 0.05) / (Math.min(x, y) + 0.05); };
  const hex = (c) => '#' + [c.r, c.g, c.b].map((v) => Math.round(v).toString(16).padStart(2, '0')).join('');
  const nombre = (e) => e.tagName.toLowerCase() + (typeof e.className === 'string' && e.className.trim()
    ? '.' + e.className.trim().split(/\s+/).slice(0, 2).join('.') : '');

  /** Fondos posibles detrás de un elemento (más de uno si hay un degradado) y si hay una imagen. */
  const fondos = (el, sinImagenes = false) => {
    const capas = [];
    let imagen = false;
    let degradado = false;
    let bases = null;
    for (let e = el; e && e.nodeType === 1 && !bases; e = e.parentElement) {
      const s = getComputedStyle(e);
      const img = s.backgroundImage;
      if (img && img !== 'none') {
        if (/url\(/.test(img) && !(sinImagenes && e === el)) imagen = true; // la flechita de un desplegable no es un fondo
        const colores = (img.match(/rgba?\([^)]+\)/g) || []).map(leer).filter((c) => c && c.a > 0);
        if (/gradient/.test(img) && colores.length) {
          degradado = true;
          if (colores.every((c) => c.a === 1)) { bases = colores; break; }
          capas.push(...colores.slice(0, 1));
        }
      }
      const c = leer(s.backgroundColor);
      if (c && c.a > 0) {
        if (c.a === 1) bases = [c]; else capas.push(c);
      }
    }
    if (!bases) bases = [{ r: 255, g: 255, b: 255, a: 1 }];
    return { imagen, degradado, colores: bases.map((b) => capas.reduceRight((abajo, capa) => sobre(capa, abajo), b)) };
  };
  const opacidad = (el) => {
    let o = 1;
    for (let e = el; e && e.nodeType === 1; e = e.parentElement) o *= parseFloat(getComputedStyle(e).opacity);
    return o;
  };
  const seVe = (e) => {
    if (e.checkVisibility && !e.checkVisibility({ visibilityProperty: true, contentVisibilityAuto: true })) return false;
    const s = getComputedStyle(e);
    const b = e.getBoundingClientRect();
    if (s.display === 'none' || s.visibility === 'hidden' || b.width < 2 || b.height < 2) return false;
    if (s.clip === 'rect(0px, 0px, 0px, 0px)' || s.clipPath === 'inset(50%)') return false; // solo para lectores de pantalla
    return !e.closest('[disabled], [aria-disabled=true], [hidden]') && opacidad(e) > 0.05;
  };
  const textoSinIconos = (e) => {
    const copia = e.cloneNode(true);
    copia.querySelectorAll('.icon, [aria-hidden=true]').forEach((x) => x.remove());
    return copia.textContent.trim();
  };

  const grupos = new Map();
  const sobreImagen = [];
  let medidos = 0;
  let iconosConTexto = 0;
  let minimo = 99;
  const anotar = (el, texto, color, tipo, umbral) => {
    const f = fondos(el, /^(SELECT|INPUT|TEXTAREA)$/.test(el.tagName));
    if (f.imagen) {
      if (sobreImagen.length < 12) sobreImagen.push(`${nombre(el)}: ${texto.trim().slice(0, 30)}`);
      return;
    }
    const frente0 = { ...color, a: color.a * opacidad(el) };
    let peor = 99;
    let peorFondo = f.colores[0];
    for (const fondo of f.colores) {
      const r = razon(sobre(frente0, fondo), fondo);
      if (r < peor) { peor = r; peorFondo = fondo; }
    }
    medidos++;
    minimo = Math.min(minimo, peor);
    if (peor + 0.005 >= umbral) return;
    const frente = hex(sobre(frente0, peorFondo));
    const clave = [tipo, frente, hex(peorFondo), umbral, nombre(el)].join('|');
    const g = grupos.get(clave) || { tipo, razon: Math.round(peor * 100) / 100, umbral, color: frente, fondo: hex(peorFondo),
      degradado: f.degradado, elemento: nombre(el), ejemplo: texto.trim().slice(0, 40), veces: 0 };
    g.veces++;
    grupos.set(clave, g);
  };

  const hojas = document.createTreeWalker(document.body, NodeFilter.SHOW_TEXT);
  for (let n = hojas.nextNode(); n; n = hojas.nextNode()) {
    const el = n.parentElement;
    if (!n.nodeValue.trim() || !el || /^(SCRIPT|STYLE|NOSCRIPT|OPTION)$/.test(el.tagName) || !seVe(el)) continue;
    const s = getComputedStyle(el);
    const color = leer(s.color);
    if (!color) continue;
    const tam = parseFloat(s.fontSize);
    if (/Material Symbols/.test(s.fontFamily)) {
      // un ícono cuenta si es lo único que hay: en un control (botón de cerrar sesión, flechas) o en un dato (estrellas)
      const control = el.closest('a, button, summary, [role=button]');
      if (textoSinIconos(control || el.parentElement)) { iconosConTexto++; continue; }
      anotar(el, n.nodeValue, color, 'ícono sin texto', 3);
      continue;
    }
    const grande = tam >= 24 || (tam >= 18.66 && parseInt(s.fontWeight, 10) >= 700);
    anotar(el, n.nodeValue, color, grande ? 'texto grande' : 'texto', grande ? 3 : 4.5);
  }
  for (const el of document.querySelectorAll('input:not([type=hidden]):not([type=checkbox]):not([type=radio]):not([type=file]):not([type=submit]), select, textarea')) {
    if (!seVe(el)) continue;
    const s = getComputedStyle(el);
    if (el.tagName !== 'SELECT' && el.placeholder && !el.value) {
      const c = leer(getComputedStyle(el, '::placeholder').color);
      if (c) anotar(el, el.placeholder, c, 'texto de ayuda del campo', 4.5);
    } else {
      const c = leer(s.color);
      if (c) anotar(el, el.value || el.tagName, c, 'texto', 4.5);
    }
    // el campo tiene que distinguirse de lo que lo rodea por el contorno o por el relleno (WCAG 1.4.11).
    // Si el campo no tiene contorno propio, el que se ve es el de la caja que lo envuelve (por ejemplo el buscador).
    let caja = el;
    for (let i = 0; i < 2 && caja.parentElement; i++) {
      const sc = getComputedStyle(caja);
      const fondo = leer(sc.backgroundColor);
      if (parseFloat(sc.borderTopWidth) > 0 || (fondo && fondo.a > 0) || parseFloat(sc.boxShadow === 'none' ? 0 : 1)) break;
      caja = caja.parentElement;
    }
    const sc = getComputedStyle(caja);
    const fuera = fondos(caja.parentElement).colores[0];
    const borde = leer(sc.borderTopColor);
    const relleno = fondos(caja).colores[0];
    const conBorde = borde && parseFloat(sc.borderTopWidth) > 0 ? razon(sobre(borde, fuera), fuera) : 1;
    const mejor = Math.max(conBorde, razon(relleno, fuera));
    medidos++;
    if (mejor + 0.005 < 3) {
      const colorBorde = conBorde > 1 ? hex(sobre(borde, fuera)) : hex(relleno);
      const clave = ['contorno de campo', colorBorde, hex(fuera), nombre(el)].join('|');
      const g = grupos.get(clave) || { tipo: 'contorno de campo', razon: Math.round(mejor * 100) / 100, umbral: 3,
        color: colorBorde, fondo: hex(fuera), degradado: false, elemento: nombre(el),
        ejemplo: (el.getAttribute('aria-label') || el.placeholder || el.name || '').slice(0, 40), veces: 0 };
      g.veces++;
      grupos.set(clave, g);
    }
  }
  return { medidos, iconosConTexto, sobreImagen, minimo: Math.round(minimo * 100) / 100,
    fallas: [...grupos.values()].sort((a, b) => a.razon - b.razon) };
}

/**
 * Desbordes y superposiciones al ancho actual de la ventana:
 *  - pagina: píxeles que la página entera se pasa del ancho (aparece la barra horizontal);
 *  - conBarra: zonas con desplazamiento horizontal propio (por ejemplo una tabla ancha en su tarjeta);
 *  - cortados: textos que quedan cortados por el borde de su caja;
 *  - fuera: botones, enlaces, campos o títulos que quedan total o parcialmente fuera de la pantalla;
 *  - tapados: los que quedan debajo de otro elemento;
 *  - estrujados: textos largos en una columna tan angosta que quedan de una palabra por renglón;
 *  - importesPartidos: importes ("$ 17.500") partidos en dos renglones.
 * "ignorar" son selectores de patrones hechos a propósito (un carrusel con flechas, un panel deslizante).
 * Los elementos que están completamente a la izquierda de la pantalla son el menú plegado: se cuentan aparte.
 */
export function medirDesbordes(ignorar) {
  const ancho = document.documentElement.clientWidth;
  const nombre = (e) => e.tagName.toLowerCase() + (e.id ? '#' + e.id : '') + (typeof e.className === 'string' && e.className.trim()
    ? '.' + e.className.trim().split(/\s+/).slice(0, 2).join('.') : '');
  const seVe = (e) => {
    if (e.checkVisibility && !e.checkVisibility({ visibilityProperty: true, contentVisibilityAuto: true })) return false;
    const b = e.getBoundingClientRect();
    return b.width > 1 && b.height > 1;
  };
  const ignorado = (e) => (ignorar || []).some((sel) => e.closest(sel));
  const dice = (e) => (e.textContent || e.value || '').trim().replace(/\s+/g, ' ');
  const textoPropio = (e) => [...e.childNodes].filter((n) => n.nodeType === 3).map((n) => n.nodeValue).join('').trim();
  const r = { ancho, pagina: Math.max(0, document.documentElement.scrollWidth - ancho), conBarra: [], cortados: [],
    fuera: [], tapados: [], estrujados: [], importesPartidos: [], menuPlegado: 0 };
  const sumar = (lista, dato) => {
    const igual = lista.find((x) => x.elemento === dato.elemento);
    if (igual) igual.veces++; else lista.push({ ...dato, veces: 1 });
  };
  const enBarra = (e) => {
    for (let x = e.parentElement; x; x = x.parentElement) {
      if (/auto|scroll/.test(getComputedStyle(x).overflowX) && x.scrollWidth > x.clientWidth + 3) return true;
    }
    return false;
  };
  for (const e of document.body.querySelectorAll('*')) {
    if (!seVe(e) || ignorado(e)) continue;
    const s = getComputedStyle(e);
    const sobra = e.scrollWidth - e.clientWidth;
    if (sobra > 3 && e.clientWidth > 0 && !/^(INPUT|SELECT|TEXTAREA)$/.test(e.tagName)) {
      if (/auto|scroll/.test(s.overflowX)) {
        sumar(r.conBarra, { elemento: nombre(e), sobra });
      } else if (/hidden|clip/.test(s.overflowX) && s.textOverflow !== 'ellipsis') {
        // solo cuenta si lo que queda afuera es texto (no un adorno)
        const caja = e.getBoundingClientRect();
        for (const h of e.querySelectorAll('*')) {
          if (!textoPropio(h) || !seVe(h) || /Material Symbols/.test(getComputedStyle(h).fontFamily)) continue;
          if (getComputedStyle(h).textOverflow === 'ellipsis') continue;
          const b = h.getBoundingClientRect();
          if (b.bottom > caja.top && b.top < caja.bottom && (b.right > caja.right + 3 || b.left < caja.left - 3)) {
            sumar(r.cortados, { elemento: nombre(e), sobra: Math.round(Math.max(b.right - caja.right, caja.left - b.left)), texto: textoPropio(h).slice(0, 40) });
            break;
          }
        }
      }
    }
    const propio = textoPropio(e);
    if (propio && !/Material Symbols/.test(s.fontFamily)) {
      const b = e.getBoundingClientRect();
      const renglon = parseFloat(s.lineHeight) || parseFloat(s.fontSize) * 1.4;
      if (propio.length >= 25 && b.width < 80 && b.height > renglon * 4.5) {
        sumar(r.estrujados, { elemento: nombre(e), ancho: Math.round(b.width), renglones: Math.round(b.height / renglon), texto: propio.slice(0, 40) });
      }
      const lineas = () => { const rg = document.createRange(); rg.selectNodeContents(e); return new Set([...rg.getClientRects()].map((x) => Math.round(x.top))).size; };
      if (/^\$\s?[\d.]+$/.test(propio) && e.children.length === 0 && lineas() > 1) {
        sumar(r.importesPartidos, { elemento: nombre(e), texto: propio });
      }
    }
  }
  for (const e of document.body.querySelectorAll('a, button, input:not([type=hidden]), select, textarea, summary, h1, h2, h3, [class*=chip]')) {
    if (!seVe(e) || ignorado(e)) continue;
    e.scrollIntoView({ block: 'center', inline: 'nearest', behavior: 'instant' });
    const b = e.getBoundingClientRect();
    if (b.right <= 0) { r.menuPlegado++; continue; }
    if (b.right > ancho + 2 || b.left < -2) {
      if (!enBarra(e)) sumar(r.fuera, { elemento: nombre(e), texto: dice(e).slice(0, 30), izquierda: Math.round(b.left), derecha: Math.round(b.right) });
      continue;
    }
    const arriba = document.elementFromPoint(b.left + b.width / 2, b.top + b.height / 2);
    if (arriba && arriba !== e && !e.contains(arriba) && !arriba.contains(e)) {
      const etiqueta = arriba.closest('label');
      if (etiqueta && etiqueta.contains(e)) continue;
      sumar(r.tapados, { elemento: nombre(e), texto: dice(e).slice(0, 30), tapadoPor: nombre(arriba) });
    }
  }
  for (const x of document.querySelectorAll('*')) if (x.scrollLeft) x.scrollLeft = 0; // la medición desplaza las tablas: se vuelven a su lugar
  window.scrollTo({ top: 0, left: 0, behavior: 'instant' });
  return r;
}

/** Cómo se muestra cada indicador de estado: con texto, con ícono o solo con el color. */
export function medirEstados() {
  const familias = ['adm-chip', 'so-chip', 'cal-ev'];
  const salida = [];
  for (const familia of familias) {
    const porClase = new Map();
    for (const e of document.querySelectorAll('.' + familia)) {
      const b = e.getBoundingClientRect();
      if (b.width < 1 || getComputedStyle(e).display === 'none') continue;
      const variante = [...e.classList].find((c) => c.startsWith(familia + '-')) || familia;
      const texto = e.textContent.trim().replace(/\s+/g, ' ');
      const icono = !!e.querySelector('.icon, svg, img');
      const ayuda = e.getAttribute('title') || e.getAttribute('aria-label') || '';
      const d = porClase.get(variante) || { clase: variante, veces: 0, textos: new Set(), icono, ayuda: !!ayuda };
      d.veces++;
      if (d.textos.size < 4) d.textos.add(texto.slice(0, 40));
      porClase.set(variante, d);
    }
    for (const d of porClase.values()) salida.push({ ...d, textos: [...d.textos] });
  }
  return salida;
}
