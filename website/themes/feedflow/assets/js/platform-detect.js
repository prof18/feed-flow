/* Detect user OS and route the hero CTA to the appropriate store */
(function () {
  var ua = navigator.userAgent;
  var platform = (navigator.userAgentData && navigator.userAgentData.platform) || navigator.platform || '';

  var detected = null;
  if (/iPhone|iPad|iPod/.test(ua))                            detected = 'ios';
  else if (/Android/.test(ua))                                detected = 'android';
  else if (/Mac/.test(platform) || /Macintosh/.test(ua))      detected = 'macos';
  else if (/Win/.test(platform) || /Windows/.test(ua))        detected = 'windows';
  else if (/Linux/.test(platform) && !/Android/.test(ua))     detected = 'linux';

  if (!detected) return;

  var ctaLink  = document.querySelector('.js-platform-cta');
  var ctaLabel = document.querySelector('.js-cta-label');
  if (!ctaLink || !ctaLabel) return;

  /* Labels for each platform */
  var labels = {
    android: 'Download for Android',
    ios:     'Download for iPhone',
    macos:   'Download for macOS',
    windows: 'Download for Windows',
    linux:   'Download for Linux',
  };

  /* Href comes from data attributes set by the template */
  var dataKey = detected + 'Href';
  var href = ctaLink.dataset[dataKey];

  if (labels[detected]) ctaLabel.textContent = labels[detected];
  if (href) {
    ctaLink.href = href;
    ctaLink.setAttribute('target', '_blank');
    ctaLink.setAttribute('rel', 'noopener');
  }

  /* Highlight the matching platform card */
  var card = document.querySelector('[data-platform="' + detected + '"]');
  if (card) card.classList.add('platform-card-active');
})();
