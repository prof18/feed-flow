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
  var noisyElements = doc.querySelectorAll(
    'script:not([type="application/ld+json" i]):not([type^="math/" i]), style, frame, frameset, noscript, canvas, object, embed, applet, base'
  );
  for (var i = 0; i < noisyElements.length; i++) {
    noisyElements[i].remove();
  }
}

function nowMillis() {
  return Date.now();
}

function installPerformanceClock(win) {
  if (!globalThis.performance) {
    globalThis.performance = {};
  }
  if (typeof globalThis.performance.now !== 'function') {
    globalThis.performance.now = nowMillis;
  }
  if (win && !win.performance) {
    win.performance = globalThis.performance;
  }
}

function summarizeOptions(options) {
  if (!options) return 'default';

  var keys = Object.keys(options);
  if (keys.length === 0) return 'default';

  return keys.map(function (key) {
    return key + '=' + String(options[key]);
  }).join(',');
}

function summarizeProfile(profile) {
  if (!profile) return null;

  var keys = Object.keys(profile);
  keys.sort(function (left, right) {
    return Number(profile[right] || 0) - Number(profile[left] || 0);
  });

  return keys.slice(0, 6).map(function (key) {
    return key + '=' + profile[key] + 'ms';
  }).join(',');
}

function installDefuddleProfiler(parser, timings) {
  var profiledParser = parser && parser.defuddle ? parser.defuddle : parser;
  if (!profiledParser || typeof profiledParser.parseInternal !== 'function') return;

  var originalParseInternal = profiledParser.parseInternal;
  profiledParser.parseInternal = function (overrideOptions) {
    var passStart = nowMillis();
    var result = null;
    try {
      result = originalParseInternal.call(this, overrideOptions || {});
      return result;
    } finally {
      timings.defuddleProfiles.push({
        elapsedMillis: nowMillis() - passStart,
        options: summarizeOptions(overrideOptions),
        wordCount: result && typeof result.wordCount === 'number' ? result.wordCount : null,
        contentChars: result && result.content ? String(result.content).length : null,
        steps: summarizeProfile(result && result.profile),
      });
    }
  };
}

globalThis.parseReaderContent = function parseReaderContent(htmlContent, url, imageUrl) {
  var totalStart = nowMillis();
  var timings = {
    inputChars: String(htmlContent || '').length,
    domMillis: null,
    cleanupMillis: null,
    defuddleMillis: null,
    totalMillis: null,
    defuddleProfiles: [],
  };

  try {
    var domStart = nowMillis();
    var win = parseHTML(String(htmlContent || ''));
    var doc = win.document;
    timings.domMillis = nowMillis() - domStart;

    var cleanupStart = nowMillis();
    installPerformanceClock(win);
    installNeutralComputedStyle(win);
    removeNoisyElements(doc);
    absolutizeUrls(doc, url);
    removeMatchingImage(doc, imageUrl, url);
    deduplicateImages(doc, url);
    timings.cleanupMillis = nowMillis() - cleanupStart;

    var defuddleStart = nowMillis();
    var parser = new Defuddle(doc, { url: url, markdown: true, useAsync: false, profile: true });
    installDefuddleProfiler(parser, timings);
    var result = parser.parse();
    timings.defuddleMillis = nowMillis() - defuddleStart;
    timings.totalMillis = nowMillis() - totalStart;

    if (!result || !result.content) {
      return JSON.stringify({ error: 'Defuddle returned no content', timings: timings });
    }

    return JSON.stringify({
      content: result.content,
      title: result.title || null,
      siteName: result.site || null,
      timings: timings,
    });
  } catch (error) {
    timings.totalMillis = nowMillis() - totalStart;
    return JSON.stringify({
      error: String(error) + (error && error.stack ? '\n' + error.stack : ''),
      timings: timings,
    });
  }
};
