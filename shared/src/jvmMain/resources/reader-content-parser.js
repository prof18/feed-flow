// Reader content parser — pre-processes HTML before Readability, then converts to Markdown.
function normalizeUrl(url, baseURI) {
  try {
    return new URL(url, baseURI).href;
  } catch (e) {
    return url;
  }
}

function urlPath(url, baseURI) {
  try {
    var u = new URL(url, baseURI);
    return u.origin + u.pathname;
  } catch (e) {
    return url;
  }
}

function processNoScriptImages(doc) {
  var noscripts = doc.querySelectorAll('noscript');
  for (var i = 0; i < noscripts.length; i++) {
    var ns = noscripts[i];
    var tmp = doc.createElement('div');
    tmp.innerHTML = ns.textContent;
    var img = tmp.querySelector('img');
    if (img && ns.parentNode) {
      ns.parentNode.insertBefore(img, ns);
      ns.parentNode.removeChild(ns);
    }
  }
}

function cleanContent(doc) {
  var junkSelectors = [
    '.share', '.sharing', '.social', '.social-share', '.social-links',
    '.ad', '.ads', '.advertisement', '.advert',
    '.promo', '.promotion', '.sponsored',
    '.related', '.related-posts', '.related-articles',
    '.newsletter', '.subscribe', '.subscription',
    '.comments', '.comment-section',
    '.sidebar', '.widget',
    '.nav', '.navigation', '.breadcrumb',
    '.footer', '.site-footer',
    '.cookie', '.gdpr',
    '[aria-label="advertisement"]',
    '[data-ad]',
    '.outbrain', '.taboola', '.revcontent',
  ];
  for (var s = 0; s < junkSelectors.length; s++) {
    try {
      var els = doc.querySelectorAll(junkSelectors[s]);
      for (var i = 0; i < els.length; i++) {
        if (els[i].parentNode) els[i].parentNode.removeChild(els[i]);
      }
    } catch (e) { /* ignore unknown selectors */ }
  }
}

function deduplicateImages(doc) {
  var baseURI = doc.baseURI || '';
  var seen = {};
  var imgs = doc.querySelectorAll('img');
  for (var i = 0; i < imgs.length; i++) {
    var img = imgs[i];
    var src = img.getAttribute('src');
    if (!src) continue;
    var normalized = normalizeUrl(src, baseURI);
    if (seen[normalized]) {
      if (img.parentNode) img.parentNode.removeChild(img);
    } else {
      seen[normalized] = true;
    }
  }
}

function removeFirstH1(doc) {
  var h1 = doc.querySelector('h1');
  if (h1 && h1.parentNode) {
    h1.parentNode.removeChild(h1);
  }
}

function resolveLazySrc(img) {
  return img.getAttribute('data-src')
    || img.getAttribute('data-lazy-src')
    || img.getAttribute('data-original')
    || img.getAttribute('data-lazy');
}

function removeFirstImageTagByUrl(doc, imageUrl) {
  if (!imageUrl) return;
  var baseURI = doc.baseURI || '';
  var targetFull = normalizeUrl(imageUrl, baseURI);
  var targetPath = urlPath(imageUrl, baseURI);
  var imgs = doc.querySelectorAll('img');
  var firstImg = null;
  for (var i = 0; i < imgs.length; i++) {
    var img = imgs[i];
    var src = img.getAttribute('src');
    if (!src) continue;
    if (firstImg === null) firstImg = img;
    // Exact URL match
    if (normalizeUrl(src, baseURI) === targetFull) {
      if (img.parentNode) img.parentNode.removeChild(img);
      return;
    }
    // Path match (ignores query-param size variants like ?w=800 vs ?w=1200)
    if (urlPath(src, baseURI) === targetPath) {
      if (img.parentNode) img.parentNode.removeChild(img);
      return;
    }
  }
  // Fallback: remove the first image in the article — it's almost always the hero image
  if (firstImg && firstImg.parentNode) {
    firstImg.parentNode.removeChild(firstImg);
  }
}

function parseReaderContent(htmlContent, link, bannerImage) {
  var domParser = new DOMParser();
  var doc = domParser.parseFromString(htmlContent, 'text/html');

  // Inject <base> so relative URLs resolve correctly
  var base = doc.createElement('base');
  base.href = link;
  doc.head.insertBefore(base, doc.head.firstChild);

  // Normalize lazy-loaded images before Readability strips unknown attributes
  var imgs = doc.querySelectorAll('img');
  for (var i = 0; i < imgs.length; i++) {
    var img = imgs[i];
    var lazySrc = resolveLazySrc(img);
    var src = img.getAttribute('src');
    if (lazySrc && (!src || src.indexOf('data:') === 0)) {
      img.setAttribute('src', lazySrc);
    }
  }

  // Preprocessing
  processNoScriptImages(doc);
  cleanContent(doc);
  removeFirstH1(doc);
  if (bannerImage) {
    removeFirstImageTagByUrl(doc, bannerImage);
  }
  deduplicateImages(doc);

  var reader = new Readability(doc);
  var article = reader.parse();

  if (!article) {
    return JSON.stringify({ error: 'Readability returned null' });
  }

  var turndown = new TurndownService({ headingStyle: 'atx', codeBlockStyle: 'fenced' });
  var markdown = turndown.turndown(article.content || '');

  return JSON.stringify({
    content: markdown,
    title: article.title || null,
    siteName: article.siteName || null
  });
}
