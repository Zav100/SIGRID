// Script común de las páginas públicas (se carga desde templates/public-home.xhtml).
// Cada bloque chequea que sus elementos existan, porque no todas las vistas que usan
// la template tienen galería/lightbox (esos elementos son de index.xhtml).
document.addEventListener('DOMContentLoaded', () => {

  // Sticky header shadow on scroll
  const header = document.getElementById('siteHeader');
  if (header) {
    const onScroll = () => header.classList.toggle('scrolled', window.scrollY > 8);
    onScroll();
    window.addEventListener('scroll', onScroll, { passive: true });
  }

  // Mobile nav toggle
  const navToggle = document.getElementById('navToggle');
  const mainNav = document.getElementById('mainNav');
  if (navToggle && mainNav) {
    navToggle.addEventListener('click', () => {
      const isOpen = mainNav.classList.toggle('open');
      navToggle.classList.toggle('open', isOpen);
      navToggle.setAttribute('aria-expanded', String(isOpen));
    });
    mainNav.querySelectorAll('.nav-link').forEach(link => {
      link.addEventListener('click', () => {
        mainNav.classList.remove('open');
        navToggle.classList.remove('open');
        navToggle.setAttribute('aria-expanded', 'false');
      });
    });
  }

  // Reveal-on-scroll animation
  const revealEls = document.querySelectorAll('.reveal');
  if ('IntersectionObserver' in window) {
    const observer = new IntersectionObserver((entries) => {
      entries.forEach(entry => {
        if (entry.isIntersecting) {
          entry.target.classList.add('in-view');
          observer.unobserve(entry.target);
        }
      });
    }, { threshold: 0.15 });
    revealEls.forEach(el => observer.observe(el));
  } else {
    revealEls.forEach(el => el.classList.add('in-view'));
  }

  // Gallery lightbox
  const lightbox = document.getElementById('lightbox');
  const lightboxFrame = document.getElementById('lightboxFrame');
  const lightboxCaption = document.getElementById('lightboxCaption');
  const lightboxClose = document.getElementById('lightboxClose');

  if (lightbox && lightboxFrame && lightboxCaption && lightboxClose) {
    document.querySelectorAll('.gallery-item').forEach(item => {
      item.addEventListener('click', () => {
        lightboxFrame.innerHTML = '';
        const img = item.querySelector('img');
        if (img) {
          lightboxFrame.className = 'lightbox-frame';
          lightboxFrame.appendChild(img.cloneNode(true));
        } else {
          lightboxFrame.className = 'lightbox-frame ' +
            Array.from(item.classList).find(c => c.startsWith('grad-'));
          const span = document.createElement('span');
          span.textContent = item.querySelector('span')?.textContent || '';
          lightboxFrame.appendChild(span);
        }
        lightboxCaption.textContent = item.dataset.caption || '';
        lightbox.classList.add('open');
      });
    });

    const closeLightbox = () => lightbox.classList.remove('open');
    lightboxClose.addEventListener('click', closeLightbox);
    lightbox.addEventListener('click', (e) => { if (e.target === lightbox) closeLightbox(); });
    document.addEventListener('keydown', (e) => { if (e.key === 'Escape') closeLightbox(); });
  }

  // Filtro de categorías + buscador (faqpage.xhtml)
  const faqFiltros = document.querySelectorAll('.faq-filters .filter-pill');
  const faqCategorias = document.querySelectorAll('.faq-category');
  const faqBuscadorInput = document.querySelector('.faq-search input');
  const faqBuscadorBoton = document.querySelector('.faq-search button');

  if (faqFiltros.length && faqCategorias.length) {
    let filtroActivo = 'todas';

    const aplicarFiltros = () => {
      const consulta = (faqBuscadorInput?.value || '').trim().toLowerCase();

      faqCategorias.forEach(categoria => {
        const pasaCategoria = filtroActivo === 'todas' || categoria.dataset.categoria === filtroActivo;
        let algunaPreguntaVisible = false;

        categoria.querySelectorAll('.faq-item').forEach(item => {
          const pregunta = item.querySelector('.faq-question > span')?.textContent.toLowerCase() || '';
          const pasaBusqueda = consulta === '' || pregunta.includes(consulta);
          const visible = pasaCategoria && pasaBusqueda;
          item.classList.toggle('faq-item-oculto', !visible);
          if (visible) algunaPreguntaVisible = true;
        });

        categoria.classList.toggle('faq-category-oculta', !algunaPreguntaVisible);
      });
    };

    faqFiltros.forEach(pill => {
      pill.addEventListener('click', () => {
        faqFiltros.forEach(p => p.classList.remove('active'));
        pill.classList.add('active');
        filtroActivo = pill.dataset.filtro;
        aplicarFiltros();
      });
    });

    if (faqBuscadorInput) {
      faqBuscadorInput.addEventListener('input', aplicarFiltros);
    }
    if (faqBuscadorBoton) {
      faqBuscadorBoton.addEventListener('click', aplicarFiltros);
    }
  }

  // Carrusel de galería (instalaciones.xhtml)
  const carruselTrack = document.getElementById('carruselPredio');
  const carruselPrev = document.getElementById('carruselPrev');
  const carruselNext = document.getElementById('carruselNext');

  if (carruselTrack && carruselPrev && carruselNext) {
    const carruselWrap = carruselTrack.parentElement;
    const carruselSlides = Array.from(carruselTrack.children);
    const total = carruselSlides.length;
    let indiceActivo = 0;

    // Carrusel circular por matemática modular (sin clonar slides ni saltos
    // de posición): cada foto se posiciona con transform/opacity según su
    // distancia circular a la activa. Como nunca se toca width/layout, nada
    // fuera del carrusel se mueve jamás; solo transform y opacity, que no
    // generan reflow.
    const posicionar = () => {
      const anchoWrap = carruselWrap.clientWidth;
      const anchoBase = anchoWrap * 0.42;
      const altoBase = anchoBase / 1.6;
      const paso = anchoBase * 0.55;

      carruselWrap.style.height = `${altoBase * 1.1}px`;

      carruselSlides.forEach((slide, i) => {
        let diff = i - indiceActivo;
        if (diff > total / 2) diff -= total;
        if (diff < -total / 2) diff += total;
        const distancia = Math.abs(diff);

        const escala = distancia === 0 ? 1 : distancia === 1 ? 0.72 : 0.5;
        // Solo se ven 3 fotos (activa + una vecina de cada lado); el resto queda invisible.
        const opacidad = distancia === 0 ? 1 : distancia === 1 ? 0.55 : 0;

        slide.style.width = `${anchoBase}px`;
        slide.style.height = `${altoBase}px`;
        slide.style.transform = `translate(-50%, -50%) translateX(${diff * paso}px) scale(${escala})`;
        slide.style.opacity = String(opacidad);
        slide.style.zIndex = String(100 - distancia);
        slide.classList.toggle('carousel-slide-activa', distancia === 0);
      });
    };

    carruselPrev.addEventListener('click', () => {
      indiceActivo = (indiceActivo - 1 + total) % total;
      posicionar();
    });
    carruselNext.addEventListener('click', () => {
      indiceActivo = (indiceActivo + 1) % total;
      posicionar();
    });
    window.addEventListener('resize', posicionar);
    posicionar();
  }
});
