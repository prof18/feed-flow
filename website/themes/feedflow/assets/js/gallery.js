/* Gallery: click-to-preview lightbox */
(function () {
  const lightbox = document.querySelector('.gallery-lightbox');
  if (!lightbox) return;

  const dialog = lightbox.querySelector('.gallery-lightbox-dialog');
  const image = lightbox.querySelector('.gallery-lightbox-image');
  const caption = lightbox.querySelector('.gallery-lightbox-caption');
  const triggers = Array.from(document.querySelectorAll('.gallery-trigger'));

  if (!dialog || !image || !caption || !triggers.length) return;

  let lastTrigger = null;

  function closeLightbox() {
    lightbox.hidden = true;
    lightbox.setAttribute('aria-hidden', 'true');
    document.body.classList.remove('gallery-lightbox-open');
    image.removeAttribute('src');
    image.alt = '';
    caption.textContent = '';

    if (lastTrigger) {
      lastTrigger.focus();
      lastTrigger = null;
    }
  }

  function openLightbox(trigger) {
    const source = trigger.querySelector('img');
    if (!source) return;

    lastTrigger = trigger;
    image.src = source.currentSrc || source.src;
    image.alt = source.alt || '';
    caption.textContent = source.alt || '';

    lightbox.hidden = false;
    lightbox.setAttribute('aria-hidden', 'false');
    document.body.classList.add('gallery-lightbox-open');

    window.requestAnimationFrame(() => {
      const closeButton = lightbox.querySelector('.gallery-lightbox-close');
      closeButton?.focus();
    });
  }

  triggers.forEach((trigger) => {
    trigger.addEventListener('click', () => openLightbox(trigger));
  });

  lightbox.addEventListener('click', (event) => {
    if (event.target instanceof HTMLElement && event.target.hasAttribute('data-gallery-close')) {
      closeLightbox();
    }
  });

  document.addEventListener('keydown', (event) => {
    if (event.key === 'Escape' && !lightbox.hidden) {
      closeLightbox();
    }
  });
})();
