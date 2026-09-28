// Script de register.xhtml (alta pública). Se carga junto con script.js (compartido)
// desde templates/public-home.xhtml, así que cada bloque revisa que sus elementos
// existan antes de usarlos.
document.addEventListener('DOMContentLoaded', () => {

  // Callback del f:ajax del botón "Continuar" (paso 1): revisa el marcador que
  // deja el bean (data-paso-uno-ok) y, si el correo/contraseña son válidos,
  // dispara la animación que corre el panel visual y expone el paso 2.
  window.sigridRegistroPasoUno = function (datos) {
    if (datos.status !== 'success') {
      return;
    }
    // f:ajax render="@form" reemplaza el <form> entero: para cuando llega este
    // evento el DOM ya tiene el formulario nuevo, así que hay que buscarlo por id
    // en el document (NO con datos.source.closest('form'), que apunta al botón
    // viejo ya desprendido del árbol, con el marcador desactualizado de antes del envío).
    const marcador = document.querySelector('#pasoUnoForm [data-paso-uno-ok]');
    const wrap = document.getElementById('registroWrap');
    if (marcador && wrap && marcador.dataset.pasoUnoOk === 'true') {
      wrap.classList.add('register-wrap-paso2');
    }
  };

  const pasoDosForm = document.getElementById('pasoDosForm');
  if (!pasoDosForm) {
    return;
  }

  // Declarados antes que los toggles: la restauración de estado (más abajo, tras
  // una recarga por error en "Finalizar registro") los usa apenas arranca el script.
  const campoNombre = document.getElementById('pasoDosForm:nombre');
  const campoApellido = document.getElementById('pasoDosForm:apellido');
  const campoDni = document.getElementById('pasoDosForm:dni');
  const campoTelefono = document.getElementById('pasoDosForm:telefono');
  const campoLegajo = document.getElementById('pasoDosForm:legajo');
  const btnFinalizar = document.getElementById('pasoDosForm:btnFinalizar');

  // ============ Toggle: tipo de socio (Externo / Interno) ============
  const tipoSocioToggle = document.getElementById('tipoSocioToggle');
  const tipoSocioInput = document.getElementById('pasoDosForm:tipoSocio');
  const camposInternos = document.getElementById('regCamposInternos');

  // ============ Toggle: categoría (Alumno / Docente / No Docente) ============
  const categoriaToggle = document.getElementById('categoriaToggle');
  const categoriaInput = document.getElementById('pasoDosForm:categoriaInterna');

  const marcarCategoria = (valor) => {
    if (!categoriaToggle) {
      return;
    }
    categoriaToggle.querySelectorAll('.reg-toggle-btn').forEach(b => {
      b.classList.toggle('reg-toggle-activo', b.dataset.categoria === valor);
    });
  };

  if (categoriaToggle && categoriaInput) {
    categoriaToggle.querySelectorAll('.reg-toggle-btn').forEach(btn => {
      btn.addEventListener('click', () => {
        marcarCategoria(btn.dataset.categoria);
        categoriaInput.value = btn.dataset.categoria;
        validarPasoDos();
      });
    });
  }

  if (tipoSocioToggle && tipoSocioInput && camposInternos) {
    const aplicarTipoSocio = (tipo, limpiar) => {
      tipoSocioToggle.querySelectorAll('.reg-toggle-btn').forEach(b => {
        b.classList.toggle('reg-toggle-activo', b.dataset.tipoSocio === tipo);
      });
      tipoSocioInput.value = tipo || '';

      const esInterno = tipo === 'INTERNO';
      // El bloque de Legajo/Categoría siempre ocupa su lugar (nunca aparece ni
      // desaparece): solo cambia entre activo/atenuado, así la tarjeta no salta.
      camposInternos.classList.toggle('reg-internos-inactivo', !esInterno);
      if (!esInterno && limpiar) {
        // Externo: no hace falta legajo/categoría, se limpian para no arrastrar una elección vieja.
        if (campoLegajo) {
          campoLegajo.value = '';
        }
        if (categoriaInput) {
          categoriaInput.value = '';
        }
        marcarCategoria(null);
      }
    };

    tipoSocioToggle.querySelectorAll('.reg-toggle-btn').forEach(btn => {
      btn.addEventListener('click', () => {
        aplicarTipoSocio(btn.dataset.tipoSocio, true);
        validarPasoDos();
      });
    });

    // Si la página se recarga después de un "Finalizar registro" con error, el
    // servidor devuelve los valores ya elegidos (quedaron en el bean): se
    // restaura el estado visual de los toggles para que coincida con ellos.
    if (tipoSocioInput.value) {
      aplicarTipoSocio(tipoSocioInput.value, false);
      marcarCategoria(categoriaInput ? categoriaInput.value : null);
    }
  }

  // ============ Mini calendario (fecha de nacimiento) ============
  const nacimientoVisible = document.getElementById('nacimientoVisible');
  const nacimientoIso = document.getElementById('pasoDosForm:nacimientoIso');
  const miniCalendario = document.getElementById('miniCalendario');
  const miniCalMes = document.getElementById('miniCalMes');
  const miniCalAnio = document.getElementById('miniCalAnio');
  const miniCalDias = document.getElementById('miniCalDias');
  const miniCalPrev = document.getElementById('miniCalPrev');
  const miniCalNext = document.getElementById('miniCalNext');

  if (nacimientoVisible && nacimientoIso && miniCalendario && miniCalMes && miniCalAnio && miniCalDias) {
    const MESES = ['Enero', 'Febrero', 'Marzo', 'Abril', 'Mayo', 'Junio', 'Julio', 'Agosto', 'Septiembre', 'Octubre', 'Noviembre', 'Diciembre'];
    const hoy = new Date();
    let vista = { anio: hoy.getFullYear() - 18, mes: hoy.getMonth() }; // 18 años atrás: punto de partida más útil que "hoy"
    let elegida = null;

    // Si la página se recarga con una fecha ya elegida (quedó en el bean tras un
    // "Finalizar registro" con error), se restaura el texto visible y el mes/año
    // de arranque del calendario, en vez de perder la elección.
    if (nacimientoIso.value) {
      const [anioIso, mesIso, diaIso] = nacimientoIso.value.split('-').map(Number);
      if (anioIso && mesIso && diaIso) {
        elegida = nacimientoIso.value;
        vista = { anio: anioIso, mes: mesIso - 1 };
        nacimientoVisible.value = `${String(diaIso).padStart(2, '0')}/${String(mesIso).padStart(2, '0')}/${anioIso}`;
      }
    }

    MESES.forEach((nombre, i) => {
      const opt = document.createElement('option');
      opt.value = String(i);
      opt.textContent = nombre;
      miniCalMes.appendChild(opt);
    });
    for (let a = hoy.getFullYear(); a >= hoy.getFullYear() - 100; a--) {
      const opt = document.createElement('option');
      opt.value = String(a);
      opt.textContent = String(a);
      miniCalAnio.appendChild(opt);
    }

    const pad = (n) => String(n).padStart(2, '0');

    const renderCalendario = () => {
      miniCalMes.value = String(vista.mes);
      miniCalAnio.value = String(vista.anio);
      miniCalDias.innerHTML = '';

      const primerDia = new Date(vista.anio, vista.mes, 1);
      // Lunes = 0 ... Domingo = 6 (el calendario arranca en lunes, como el resto del sitio en es-AR)
      const offset = (primerDia.getDay() + 6) % 7;
      const diasEnMes = new Date(vista.anio, vista.mes + 1, 0).getDate();
      const diasMesAnterior = new Date(vista.anio, vista.mes, 0).getDate();

      const agregarDia = (numero, deEsteMes, anio, mes) => {
        const celda = document.createElement('button');
        celda.type = 'button';
        celda.className = 'mini-calendar-dia' + (deEsteMes ? '' : ' mini-calendar-dia-otro-mes');
        celda.textContent = String(numero);
        const iso = `${anio}-${pad(mes + 1)}-${pad(numero)}`;
        if (iso === elegida) {
          celda.classList.add('mini-calendar-dia-elegido');
        }
        celda.addEventListener('click', () => {
          elegida = iso;
          nacimientoIso.value = iso;
          nacimientoVisible.value = `${pad(numero)}/${pad(mes + 1)}/${anio}`;
          if (!deEsteMes) {
            vista = { anio, mes };
          }
          renderCalendario();
          miniCalendario.hidden = true;
          validarPasoDos();
        });
        miniCalDias.appendChild(celda);
      };

      for (let i = offset; i > 0; i--) {
        const mesAnt = vista.mes === 0 ? 11 : vista.mes - 1;
        const anioAnt = vista.mes === 0 ? vista.anio - 1 : vista.anio;
        agregarDia(diasMesAnterior - i + 1, false, anioAnt, mesAnt);
      }
      for (let d = 1; d <= diasEnMes; d++) {
        agregarDia(d, true, vista.anio, vista.mes);
      }
      const restantes = (7 - (miniCalDias.children.length % 7)) % 7;
      for (let d = 1; d <= restantes; d++) {
        const mesSig = vista.mes === 11 ? 0 : vista.mes + 1;
        const anioSig = vista.mes === 11 ? vista.anio + 1 : vista.anio;
        agregarDia(d, false, anioSig, mesSig);
      }
    };

    const moverMes = (delta) => {
      vista.mes += delta;
      if (vista.mes < 0) {
        vista.mes = 11;
        vista.anio--;
      } else if (vista.mes > 11) {
        vista.mes = 0;
        vista.anio++;
      }
      renderCalendario();
    };

    nacimientoVisible.addEventListener('click', () => {
      miniCalendario.hidden = !miniCalendario.hidden;
      if (!miniCalendario.hidden) {
        renderCalendario();
      }
    });
    miniCalPrev.addEventListener('click', () => moverMes(-1));
    miniCalNext.addEventListener('click', () => moverMes(1));
    miniCalMes.addEventListener('change', () => { vista.mes = Number(miniCalMes.value); renderCalendario(); });
    miniCalAnio.addEventListener('change', () => { vista.anio = Number(miniCalAnio.value); renderCalendario(); });

    document.addEventListener('click', (e) => {
      if (!miniCalendario.hidden && !e.target.closest('.mini-calendar-wrap')) {
        miniCalendario.hidden = true;
      }
    });
  }

  // ============ Validación en vivo del paso 2 (habilita "Finalizar registro") ============
  // btnFinalizar es un <input type="submit"> normal, SIN el atributo disabled: si se
  // deja disabled de entrada y se lo habilita por JS al tipear, el envío depende de que
  // el navegador reconozca a tiempo el toggle de la propiedad; para no arriesgar eso, el
  // botón siempre puede enviar el form y el gateo es visual (pointer-events) + este guard
  // en el submit del form (que además cubre el Enter, al que pointer-events no aplica).
  let pasoDosValido = false;

  function validarPasoDos() {
    if (!btnFinalizar) {
      return;
    }
    const nombreOk = !!campoNombre && campoNombre.value.trim().length > 0;
    const apellidoOk = !!campoApellido && campoApellido.value.trim().length > 0;
    const dniOk = !!campoDni && /^\d{6,15}$/.test(campoDni.value.trim());
    const telefonoOk = !!campoTelefono && /^\d{6,30}$/.test(campoTelefono.value.trim());
    const fechaOk = !!nacimientoIso && nacimientoIso.value && new Date(nacimientoIso.value) < new Date();

    const tipoSocio = tipoSocioInput ? tipoSocioInput.value : '';
    let tipoOk = tipoSocio === 'EXTERNO';
    if (tipoSocio === 'INTERNO') {
      const legajoOk = !!campoLegajo && campoLegajo.value.trim().length > 0;
      const categoriaOk = !!categoriaInput && categoriaInput.value;
      tipoOk = legajoOk && categoriaOk;
    }

    pasoDosValido = nombreOk && apellidoOk && dniOk && telefonoOk && fechaOk && tipoOk;
    btnFinalizar.classList.toggle('btn-deshabilitado', !pasoDosValido);
  }

  [campoNombre, campoApellido, campoDni, campoTelefono, campoLegajo].forEach(campo => {
    if (campo) {
      campo.addEventListener('input', validarPasoDos);
    }
  });

  pasoDosForm.addEventListener('submit', (e) => {
    if (!pasoDosValido) {
      e.preventDefault();
    }
  });

  validarPasoDos();
});
