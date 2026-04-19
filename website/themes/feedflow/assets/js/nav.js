/* Nav: scroll-triggered blur background + mobile hamburger */
(function () {
  const nav = document.querySelector('.site-nav');
  if (!nav) return;

  /* Scroll blur */
  const update = () => {
    nav.classList.toggle('scrolled', window.scrollY > 60);
  };
  window.addEventListener('scroll', update, { passive: true });
  update();

  /* Hamburger toggle */
  const hamburger = document.querySelector('.nav-hamburger');
  const mobileNav  = document.getElementById('mobile-nav');

  function closeMobileNav() {
    hamburger.setAttribute('aria-expanded', 'false');
    mobileNav.classList.remove('is-open');
  }

  if (hamburger && mobileNav) {
    hamburger.addEventListener('click', () => {
      const isOpen = hamburger.getAttribute('aria-expanded') === 'true';
      hamburger.setAttribute('aria-expanded', String(!isOpen));
      mobileNav.classList.toggle('is-open', !isOpen);
    });

    /* Close on any link or download button click */
    mobileNav.querySelectorAll('a').forEach((link) => {
      link.addEventListener('click', closeMobileNav);
    });

    /* Close when viewport grows past mobile breakpoint */
    window.matchMedia('(min-width: 768px)').addEventListener('change', (e) => {
      if (e.matches) closeMobileNav();
    });
  }
})();
