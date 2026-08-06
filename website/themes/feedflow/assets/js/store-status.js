/* Store status strip: reserve space for the fixed nav + hero/page headers below it */
(function () {
  const strip = document.querySelector('.ff-strip');
  if (!strip) return;

  const setHeight = () => {
    document.documentElement.style.setProperty('--banner-h', strip.offsetHeight + 'px');
  };

  setHeight();
  window.addEventListener('resize', setHeight);
})();
