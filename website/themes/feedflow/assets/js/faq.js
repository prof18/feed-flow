/* FAQ: search filter + keyboard accessibility */
(function () {
  const searchInput = document.querySelector('.faq-search');
  if (!searchInput) return;

  /* Show the search input (hidden by default for no-JS) */
  const wrap = document.querySelector('.faq-search-wrap');
  if (wrap) wrap.hidden = false;

  const items = Array.from(document.querySelectorAll('.faq-item-page'));

  searchInput.addEventListener('input', () => {
    const q = searchInput.value.trim().toLowerCase();

    items.forEach((item) => {
      const title = item.querySelector('summary')?.textContent?.toLowerCase() || '';
      const body  = item.querySelector('.faq-answer-page')?.textContent?.toLowerCase() || '';
      const match = !q || title.includes(q) || body.includes(q);
      item.hidden = !match;
      if (match && q) item.open = true;
    });
  });
})();

/* Press kit: copy-to-clipboard buttons */
(function () {
  document.querySelectorAll('.copy-btn').forEach((btn) => {
    btn.addEventListener('click', async () => {
      const target = btn.dataset.copy
        ? document.querySelector(btn.dataset.copy)
        : btn.closest('.boilerplate-block, .quote-card');

      if (!target) return;

      const text = target.querySelector('p')?.textContent?.trim()
        || target.textContent?.trim()
        || '';

      try {
        await navigator.clipboard.writeText(text);
        const orig = btn.textContent;
        btn.textContent = 'Copied!';
        setTimeout(() => { btn.textContent = orig; }, 1800);
      } catch {
        /* clipboard API not available */
      }
    });
  });
})();

/* Scroll reveal */
(function () {
  const els = document.querySelectorAll('.reveal');
  if (!els.length) return;
  if (window.matchMedia('(prefers-reduced-motion: reduce)').matches) {
    els.forEach(el => el.classList.add('visible'));
    return;
  }
  const io = new IntersectionObserver((entries) => {
    entries.forEach(entry => {
      if (entry.isIntersecting) {
        entry.target.classList.add('visible');
        io.unobserve(entry.target);
      }
    });
  }, { threshold: 0.08 });

  els.forEach(el => io.observe(el));
})();

/* FAQ: support email, assembled on click so the address isn't in the page source */
(function () {
  document.querySelectorAll('.js-support-email').forEach((link) => {
    link.hidden = false;
    link.addEventListener('click', (event) => {
      event.preventDefault();
      const address = atob(link.dataset.e);
      const subject = encodeURIComponent('FeedFlow question');
      const body = encodeURIComponent(
        'Describe your question or problem. Please include your platform (Android, iPhone, iPad, Mac, Windows or Linux), the app version and, if it\'s about a feed, its link.\n\n'
      );
      window.location.href = 'mailto:' + address + '?subject=' + subject + '&body=' + body;
    });
  });
})();
