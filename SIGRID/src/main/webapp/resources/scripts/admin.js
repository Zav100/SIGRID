// Script común del panel de administración (se carga desde templates/admin-dashboard.xhtml).
document.addEventListener('DOMContentLoaded', () => {

  // Menú lateral en pantallas chicas: se abre con el botón de la barra superior
  const menuBtn = document.getElementById('admMenuBtn');
  const backdrop = document.getElementById('admBackdrop');
  const setMenu = (abierto) => {
    document.body.classList.toggle('adm-menu-open', abierto);
    if (menuBtn) menuBtn.setAttribute('aria-expanded', String(abierto));
  };
  if (menuBtn) menuBtn.addEventListener('click', () => setMenu(!document.body.classList.contains('adm-menu-open')));
  if (backdrop) backdrop.addEventListener('click', () => setMenu(false));

  // Búsqueda: oculta las filas (.js-buscable) que no contienen el texto, sin tildes ni mayúsculas.
  // Se consulta el DOM en cada pasada porque las cards que se actualizan por AJAX traen filas nuevas.
  const buscar = document.getElementById('admBuscar');
  const normalizar = (texto) => texto.normalize('NFD').replace(/[̀-ͯ]/g, '').toLowerCase();
  const filtrar = () => {
    const consulta = normalizar(buscar.value.trim());
    document.querySelectorAll('.js-buscable').forEach((fila) => {
      fila.hidden = consulta !== '' && !normalizar(fila.textContent).includes(consulta);
    });
  };
  if (buscar) {
    buscar.addEventListener('input', filtrar);
    const ajax = (window.faces || window.jsf || {}).ajax;
    if (ajax) ajax.addOnEvent((e) => { if (e.status === 'success') filtrar(); });
  }

  // Plano de instalaciones: al tocar un espacio con reservas se abre su tarjeta con el listado.
  // Los eventos se escuchan en document porque el plano se vuelve a dibujar por AJAX (otro día del calendario).
  const cerrarTarjetas = () => {
    document.querySelectorAll('.adm-plan-pop:not([hidden])').forEach((t) => { t.hidden = true; });
    document.querySelectorAll('.adm-space[aria-expanded="true"]').forEach((b) => b.setAttribute('aria-expanded', 'false'));
  };
  const abrirTarjeta = (espacio) => {
    const plano = espacio.closest('.adm-plan');
    const tarjeta = plano.querySelector('.adm-plan-pop[data-espacio="' + espacio.dataset.espacio + '"]');
    cerrarTarjetas();
    tarjeta.hidden = false;
    espacio.setAttribute('aria-expanded', 'true');

    // centrada bajo el espacio (o arriba si abajo no entra) y sin salirse del plano
    const p = plano.getBoundingClientRect();
    const s = espacio.getBoundingClientRect();
    const left = s.left - p.left + s.width / 2 - tarjeta.offsetWidth / 2;
    let top = s.bottom - p.top + 8;
    if (top + tarjeta.offsetHeight > p.height) top = s.top - p.top - tarjeta.offsetHeight - 8;
    tarjeta.style.left = Math.max(0, Math.min(left, p.width - tarjeta.offsetWidth)) + 'px';
    tarjeta.style.top = Math.max(0, top) + 'px';
  };
  document.addEventListener('click', (e) => {
    const espacio = e.target.closest('.adm-space.is-click');
    if (espacio) {
      espacio.getAttribute('aria-expanded') === 'true' ? cerrarTarjetas() : abrirTarjeta(espacio);
    } else if (e.target.closest('.adm-plan-close') || !e.target.closest('.adm-plan-pop')) {
      cerrarTarjetas();
    }
  });

  // Formularios plegables de la ficha de instalaciones: tras cada actualización AJAX el contenedor indica en data-abrir cuál
  // quedó a medias (por ejemplo, falló al guardar) y se lo deja abierto; el resto vuelve a dibujarse cerrado.
  const ajaxApi = (window.faces || window.jsf || {}).ajax;
  if (ajaxApi) {
    ajaxApi.addOnEvent((e) => {
      if (e.status !== 'success') return;
      document.querySelectorAll('[data-abrir]').forEach((c) => {
        if (c.dataset.abrir) c.querySelectorAll('details[data-panel="' + c.dataset.abrir + '"]').forEach((d) => { d.open = true; });
      });
    });
  }

  // Atajos: "/" enfoca la búsqueda y Escape cierra el menú y las tarjetas (o limpia la búsqueda)
  document.addEventListener('keydown', (e) => {
    const escribiendo = /^(INPUT|TEXTAREA|SELECT)$/.test(document.activeElement.tagName);
    if (e.key === '/' && !escribiendo && buscar) {
      e.preventDefault();
      buscar.focus();
    } else if (e.key === 'Escape') {
      setMenu(false);
      cerrarTarjetas();
      if (document.activeElement === buscar && buscar.value) {
        buscar.value = '';
        buscar.dispatchEvent(new Event('input'));
      }
    }
  });
});

// "Exportar PDF" de las listas con "Ver más": después de mostrar todas las filas (AJAX) abre el cuadro de impresión
function sigridImprimir(datos) {
  if (datos.status === 'success') {
    setTimeout(function () { window.print(); }, 100);
  }
}
