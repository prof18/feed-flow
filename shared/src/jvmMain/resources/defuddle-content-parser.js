// Reader content parser — extracts the article with Defuddle (full build) and
// returns it as Markdown via Defuddle's built-in markdown converter.

// HtmlUnit's computed style objects throw Java NullPointerExceptions on property
// access for elements that belong to a detached (DOMParser-created) document,
// because that document has no enclosing WebWindow — and those host exceptions
// cannot be caught from JavaScript. CSS is disabled in the WebClient anyway, so
// computed styles carry no real information: always return a plain object with
// neutral defaults instead of the native style object.
(function patchGetComputedStyle() {
  var DEFAULTS = {
    display: 'block',
    visibility: 'visible',
    position: 'static',
    width: 'auto',
    height: 'auto',
    opacity: '1',
    cssFloat: 'none',
    overflow: 'visible',
  };
  window.getComputedStyle = function () {
    var safe = {};
    for (var key in DEFAULTS) {
      safe[key] = DEFAULTS[key];
    }
    safe.getPropertyValue = function (name) {
      var camel = String(name).replace(/-([a-z])/g, function (m, c) {
        return c.toUpperCase();
      });
      return Object.prototype.hasOwnProperty.call(DEFAULTS, camel) ? DEFAULTS[camel] : '';
    };
    return safe;
  };

  // getBoundingClientRect throws the same uncatchable host exception for detached
  // elements. Defuddle only uses it as a fallback for image dimensions and ignores
  // zero values, so a zero rect is a safe stub.
  var zeroRect = function () {
    return { top: 0, left: 0, right: 0, bottom: 0, width: 0, height: 0, x: 0, y: 0 };
  };
  if (typeof Element !== 'undefined' && Element.prototype) {
    Element.prototype.getBoundingClientRect = zeroRect;
  }

  // HtmlUnit's CSS engine does not support the :scope pseudo-class and throws a
  // SyntaxError DOMException for it, which makes Defuddle abort all processing
  // and fall back to the raw document. Rewrite :scope queries using a temporary
  // attribute anchor, and degrade any other unsupported selector to an empty
  // result instead of an exception.
  function shimQuerySelectors(proto) {
    if (!proto || !proto.querySelectorAll) return;
    var origAll = proto.querySelectorAll;
    var origOne = proto.querySelector;
    var SCOPE_ATTR = 'data-htmlunit-scope-shim';

    function queryWithScope(el, selector, queryFirst) {
      el.setAttribute(SCOPE_ATTR, '1');
      try {
        var rewritten = selector.replace(/:scope/g, '[' + SCOPE_ATTR + '="1"]');
        var root = el.ownerDocument || el;
        return queryFirst ? origOne.call(root, rewritten) : origAll.call(root, rewritten);
      } finally {
        el.removeAttribute(SCOPE_ATTR);
      }
    }

    proto.querySelectorAll = function (selector) {
      try {
        var sel = String(selector);
        if (sel.indexOf(':scope') !== -1 && this.setAttribute) {
          return queryWithScope(this, sel, false);
        }
        return origAll.call(this, selector);
      } catch (e) {
        return [];
      }
    };
    proto.querySelector = function (selector) {
      try {
        var sel = String(selector);
        if (sel.indexOf(':scope') !== -1 && this.setAttribute) {
          return queryWithScope(this, sel, true);
        }
        return origOne.call(this, selector);
      } catch (e) {
        return null;
      }
    };
  }
  if (typeof Element !== 'undefined') shimQuerySelectors(Element.prototype);
  if (typeof Document !== 'undefined') shimQuerySelectors(Document.prototype);
  if (typeof DocumentFragment !== 'undefined') shimQuerySelectors(DocumentFragment.prototype);
})();

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

function absolutizeUrls(doc, baseUrl) {
  var srcEls = doc.querySelectorAll('[src]');
  for (var i = 0; i < srcEls.length; i++) {
    var src = srcEls[i].getAttribute('src');
    if (src && src.indexOf('http') !== 0 && src.indexOf('data:') !== 0) {
      try {
        srcEls[i].setAttribute('src', new URL(src, baseUrl).href);
      } catch (e) { /* keep original */ }
    }
  }
  var hrefEls = doc.querySelectorAll('[href]');
  for (var j = 0; j < hrefEls.length; j++) {
    var href = hrefEls[j].getAttribute('href');
    if (href && href.indexOf('http') !== 0 && href.indexOf('#') !== 0 && href.indexOf('mailto:') !== 0) {
      try {
        hrefEls[j].setAttribute('href', new URL(href, baseUrl).href);
      } catch (e) { /* keep original */ }
    }
  }
}

function deduplicateImages(doc, baseURI) {
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

function removeBannerImage(doc, imageUrl, baseURI) {
  if (!imageUrl) return;
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

// HtmlUnit's DOMParser throws a Java NullPointerException when the parsed HTML
// contains <iframe>/<frame> elements, because the detached document has no
// parent WebWindow to host the frame. Strip them before parsing.
function stripFrames(htmlContent) {
  return htmlContent
    .replace(/<iframe\b[^>]*>[\s\S]*?<\/iframe>/gi, '')
    .replace(/<iframe\b[^>]*\/>/gi, '')
    .replace(/<frameset\b[^>]*>[\s\S]*?<\/frameset>/gi, '')
    .replace(/<frame\b[^>]*>/gi, '');
}

function parseReaderContent(htmlContent, link, bannerImage) {
  var domParser = new DOMParser();
  var doc = domParser.parseFromString(stripFrames(htmlContent), 'text/html');

  // Defuddle converts the extracted content to markdown internally, so all DOM
  // cleanup has to happen on the source document before extraction.
  absolutizeUrls(doc, link);
  removeBannerImage(doc, bannerImage, link);
  deduplicateImages(doc, link);

  var defuddle = new Defuddle(doc, { url: link, markdown: true });
  var result = defuddle.parse();

  if (!result || !result.content) {
    return JSON.stringify({ error: 'Defuddle returned no content' });
  }

  return JSON.stringify({
    content: result.content,
    title: result.title || null,
    siteName: result.site || null
  });
}
