// Reader content helpers — converts feed-provided HTML to Markdown.
function normalizeUrl(url, baseURI) {
  try {
    return new URL(url, baseURI).href;
  } catch (e) {
    return url;
  }
}

function resolveLazySrc(img) {
  return img.getAttribute('data-src')
    || img.getAttribute('data-lazy-src')
    || img.getAttribute('data-original')
    || img.getAttribute('data-lazy');
}

// Converts feed-provided HTML (content:encoded / description) straight to Markdown;
// feed content is already article-only. A <base> is injected so relative image/link
// URLs resolve against the article URL.
function convertFeedContentToMarkdown(htmlContent, baseUrl) {
  try {
    var domParser = new DOMParser();
    var doc = domParser.parseFromString(htmlContent, 'text/html');

    if (baseUrl) {
      var base = doc.createElement('base');
      base.href = baseUrl;
      doc.head.insertBefore(base, doc.head.firstChild);

      var imgs = doc.querySelectorAll('img');
      for (var i = 0; i < imgs.length; i++) {
        var img = imgs[i];
        var lazySrc = resolveLazySrc(img);
        var src = img.getAttribute('src');
        if (lazySrc && (!src || src.indexOf('data:') === 0)) {
          img.setAttribute('src', lazySrc);
        }
        var currentSrc = img.getAttribute('src');
        if (currentSrc) {
          img.setAttribute('src', normalizeUrl(currentSrc, baseUrl));
        }
      }

      var anchors = doc.querySelectorAll('a');
      for (var a = 0; a < anchors.length; a++) {
        var href = anchors[a].getAttribute('href');
        if (href) {
          anchors[a].setAttribute('href', normalizeUrl(href, baseUrl));
        }
      }
    }

    var turndown = new TurndownService({ headingStyle: 'atx', codeBlockStyle: 'fenced' });
    var markdown = turndown.turndown(doc.body ? doc.body.innerHTML : htmlContent);

    return JSON.stringify({ content: markdown });
  } catch (e) {
    return JSON.stringify({ error: e.toString() });
  }
}
