import { parseHTML } from './reader-globals.js';
import Defuddle from 'defuddle/full';

function installNeutralComputedStyle(win) {
  var defaults = {
    display: 'block',
    visibility: 'visible',
    position: 'static',
    width: 'auto',
    height: 'auto',
    opacity: '1',
    cssFloat: 'none',
    overflow: 'visible',
  };

  function neutralComputedStyle() {
    var safe = {};
    for (var key in defaults) {
      safe[key] = defaults[key];
    }
    safe.getPropertyValue = function (name) {
      var camel = String(name).replace(/-([a-z])/g, function (_match, char) {
        return char.toUpperCase();
      });
      return Object.prototype.hasOwnProperty.call(defaults, camel) ? defaults[camel] : '';
    };
    return safe;
  }

  globalThis.getComputedStyle = neutralComputedStyle;
  if (win) {
    win.getComputedStyle = neutralComputedStyle;
  }
}

function normalizeUrl(url, baseUrl) {
  try {
    return new URL(url, baseUrl).href;
  } catch (_error) {
    return url;
  }
}

function urlPath(url, baseUrl) {
  try {
    var parsedUrl = new URL(url, baseUrl);
    return parsedUrl.origin + parsedUrl.pathname;
  } catch (_error) {
    return url;
  }
}

function absolutizeUrls(doc, baseUrl) {
  var srcElements = doc.querySelectorAll('[src]');
  for (var i = 0; i < srcElements.length; i++) {
    var src = srcElements[i].getAttribute('src');
    if (src && src.indexOf('http') !== 0 && src.indexOf('data:') !== 0) {
      try {
        srcElements[i].setAttribute('src', new URL(src, baseUrl).href);
      } catch (_error) {
        // Keep the original URL when it cannot be resolved.
      }
    }
  }

  var hrefElements = doc.querySelectorAll('[href]');
  for (var j = 0; j < hrefElements.length; j++) {
    var href = hrefElements[j].getAttribute('href');
    if (href && href.indexOf('http') !== 0 && href.indexOf('#') !== 0 && href.indexOf('mailto:') !== 0) {
      try {
        hrefElements[j].setAttribute('href', new URL(href, baseUrl).href);
      } catch (_error) {
        // Keep the original URL when it cannot be resolved.
      }
    }
  }
}

function removeMatchingImage(doc, imageUrl, baseUrl) {
  if (!imageUrl) return;

  var targetFull = normalizeUrl(imageUrl, baseUrl);
  var targetPath = urlPath(imageUrl, baseUrl);
  var images = doc.querySelectorAll('img');
  var firstImage = null;

  for (var i = 0; i < images.length; i++) {
    var image = images[i];
    var src = image.getAttribute('src');
    if (!src) continue;
    if (firstImage === null) firstImage = image;

    if (normalizeUrl(src, baseUrl) === targetFull || urlPath(src, baseUrl) === targetPath) {
      image.remove();
      return;
    }
  }

  if (firstImage) {
    firstImage.remove();
  }
}

function deduplicateImages(doc, baseUrl) {
  var seen = {};
  var images = doc.querySelectorAll('img');
  for (var i = 0; i < images.length; i++) {
    var image = images[i];
    var src = image.getAttribute('src');
    if (!src) continue;

    var normalized = normalizeUrl(src, baseUrl);
    if (seen[normalized]) {
      image.remove();
    } else {
      seen[normalized] = true;
    }
  }
}

function removeNoisyElements(doc) {
  var noisyElements = doc.querySelectorAll('script, style, iframe, frame, frameset, canvas');
  for (var i = 0; i < noisyElements.length; i++) {
    noisyElements[i].remove();
  }
}

function stripKnownNonContent(htmlContent) {
  return String(htmlContent || '')
    .replace(/<script\b[^>]*>[\s\S]*?<\/script>/gi, '')
    .replace(/<style\b[^>]*>[\s\S]*?<\/style>/gi, '')
    .replace(/<iframe\b[^>]*>[\s\S]*?<\/iframe>/gi, '')
    .replace(/<iframe\b[^>]*\/>/gi, '')
    .replace(/<frameset\b[^>]*>[\s\S]*?<\/frameset>/gi, '')
    .replace(/<frame\b[^>]*>/gi, '');
}

globalThis.parseReaderContent = function parseReaderContent(htmlContent, url, imageUrl) {
  try {
    var win = parseHTML(stripKnownNonContent(htmlContent));
    var doc = win.document;

    installNeutralComputedStyle(win);
    removeNoisyElements(doc);
    absolutizeUrls(doc, url);
    removeMatchingImage(doc, imageUrl, url);
    deduplicateImages(doc, url);

    var result = new Defuddle(doc, { url: url, markdown: true, useAsync: false }).parse();
    if (!result || !result.content) {
      return JSON.stringify({ error: 'Defuddle returned no content' });
    }

    return JSON.stringify({
      content: result.content,
      title: result.title || null,
      siteName: result.site || null,
    });
  } catch (error) {
    return JSON.stringify({
      error: String(error) + (error && error.stack ? '\n' + error.stack : ''),
    });
  }
};
