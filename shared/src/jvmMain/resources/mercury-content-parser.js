// Reader content parser backed by @jocmp/mercury-parser.
// The paired mercury.web.js bundle is vendored from @jocmp/mercury-parser 2.4.11.
// Returns Markdown from pre-fetched HTML so desktop does not issue a second
// network request from inside HtmlUnit. Markdown conversion is handled by
// TurndownService, the same JS library used before the Defuddle migration.

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

    if (normalizeUrl(src, baseURI) === targetFull || urlPath(src, baseURI) === targetPath) {
      if (img.parentNode) img.parentNode.removeChild(img);
      return;
    }
  }

  if (firstImg && firstImg.parentNode) {
    firstImg.parentNode.removeChild(firstImg);
  }
}

function stripFrames(htmlContent) {
  return htmlContent
    .replace(/<iframe\b[^>]*>[\s\S]*?<\/iframe>/gi, '')
    .replace(/<iframe\b[^>]*\/>/gi, '')
    .replace(/<frameset\b[^>]*>[\s\S]*?<\/frameset>/gi, '')
    .replace(/<frame\b[^>]*>/gi, '');
}

function cleanHtmlForMercury(htmlContent, link, bannerImage) {
  var domParser = new DOMParser();
  var doc = domParser.parseFromString(stripFrames(htmlContent), 'text/html');

  absolutizeUrls(doc, link);
  removeBannerImage(doc, bannerImage, link);
  deduplicateImages(doc, link);

  return doc.documentElement.outerHTML;
}

function normalizeText(text) {
  return String(text || '').replace(/\s+/g, ' ').trim();
}

function isTextBlock(node) {
  if (!node || node.nodeType !== 1) return false;
  return /^(P|LI|BLOCKQUOTE|H2|H3|H4|H5|H6)$/.test(node.tagName) && normalizeText(node.textContent).length > 0;
}

function previousTextBlock(node) {
  var current = node;
  while (current) {
    var sibling = current.previousElementSibling;
    while (sibling) {
      if (isTextBlock(sibling)) return sibling;
      var nested = sibling.querySelectorAll ? sibling.querySelectorAll('p,li,blockquote,h2,h3,h4,h5,h6') : [];
      for (var i = nested.length - 1; i >= 0; i--) {
        if (isTextBlock(nested[i])) return nested[i];
      }
      sibling = sibling.previousElementSibling;
    }
    current = current.parentElement;
  }
  return null;
}

function nextTextBlock(node) {
  var current = node;
  while (current) {
    var sibling = current.nextElementSibling;
    while (sibling) {
      if (isTextBlock(sibling)) return sibling;
      var nested = sibling.querySelectorAll ? sibling.querySelectorAll('p,li,blockquote,h2,h3,h4,h5,h6') : [];
      for (var i = 0; i < nested.length; i++) {
        if (isTextBlock(nested[i])) return nested[i];
      }
      sibling = sibling.nextElementSibling;
    }
    current = current.parentElement;
  }
  return null;
}

function nearestMediaBlock(img) {
  var current = img.parentElement;
  while (current && current.parentElement && current.parentElement.tagName !== 'BODY') {
    if (
      current.tagName === 'FIGURE' ||
      /\b(wp-caption|caption|figure|image|photo)\b/i.test(current.getAttribute('class') || '')
    ) {
      return current;
    }
    current = current.parentElement;
  }
  return img;
}

function mediaHtmlFromBlock(block, img) {
  var src = img.getAttribute('src');
  if (!src) return null;

  var captionNode = block.querySelector
    ? block.querySelector('figcaption,.wp-caption-text,[class*="caption"]')
    : null;
  var caption = captionNode ? normalizeText(captionNode.textContent) : '';
  var alt = img.getAttribute('alt') || ' ';
  var html = '<figure><img src="' + src.replace(/"/g, '&quot;') + '" alt="' + alt.replace(/"/g, '&quot;') + '">';
  if (caption) {
    html += '<figcaption>' + caption + '</figcaption>';
  }
  html += '</figure>';
  return html;
}

function imageAltFromMediaHtml(html) {
  var domParser = new DOMParser();
  var doc = domParser.parseFromString(html, 'text/html');
  var img = doc.querySelector('img[alt]');
  return img ? img.getAttribute('alt') : '';
}

function collectMediaByTextAnchor(htmlContent) {
  var domParser = new DOMParser();
  var doc = domParser.parseFromString(htmlContent, 'text/html');
  var imgs = doc.querySelectorAll('img[src]');
  var mediaByAnchor = {};
  var seen = {};

  for (var i = 0; i < imgs.length; i++) {
    var img = imgs[i];
    var src = img.getAttribute('src');
    if (!src || seen[src]) continue;
    seen[src] = true;

    var block = nearestMediaBlock(img);
    var before = previousTextBlock(block);
    var after = nextTextBlock(block);
    var anchor = before || after;
    if (!anchor) continue;

    var key = normalizeText(anchor.textContent);
    if (key.length < 20) continue;

    var html = mediaHtmlFromBlock(block, img);
    if (!html) continue;

    if (!mediaByAnchor[key]) {
      mediaByAnchor[key] = [];
    }
    mediaByAnchor[key].push({
      html: html,
      src: src,
      position: before ? 'after' : 'before'
    });
  }

  return mediaByAnchor;
}

function collectImageAltsBySrc(htmlContent) {
  var domParser = new DOMParser();
  var doc = domParser.parseFromString(htmlContent, 'text/html');
  var imgs = doc.querySelectorAll('img[src]');
  var altsBySrc = {};

  for (var i = 0; i < imgs.length; i++) {
    var img = imgs[i];
    var src = img.getAttribute('src');
    if (!src || altsBySrc[src]) continue;

    var block = nearestMediaBlock(img);
    var html = mediaHtmlFromBlock(block, img);
    if (!html) continue;

    var alt = imageAltFromMediaHtml(html);
    if (alt) {
      altsBySrc[src] = alt;
    }
  }

  return altsBySrc;
}

function collectInlineMarkupByText(htmlContent) {
  var domParser = new DOMParser();
  var doc = domParser.parseFromString(htmlContent, 'text/html');
  var selectors = 'p,li,blockquote,h2,h3,h4,h5,h6';
  var nodes = doc.querySelectorAll(selectors);
  var markupByText = {};

  for (var i = 0; i < nodes.length; i++) {
    var node = nodes[i];
    if (!node.querySelector('a[href],strong,b,em,i,code')) continue;

    var text = normalizeText(node.textContent);
    if (text.length < 4) continue;

    if (Object.prototype.hasOwnProperty.call(markupByText, text)) {
      markupByText[text] = null;
    } else {
      markupByText[text] = node.innerHTML;
    }
  }

  return markupByText;
}

function restoreSourceDetails(extractedHtml, markupByText, mediaByAnchor, imageAltsBySrc) {
  var domParser = new DOMParser();
  var doc = domParser.parseFromString(extractedHtml, 'text/html');
  var selectors = 'p,li,blockquote,h2,h3,h4,h5,h6';
  var nodes = doc.body.querySelectorAll(selectors);
  var restoredMedia = {};
  var imgs = doc.body.querySelectorAll('img[src]');

  for (var imgIndex = 0; imgIndex < imgs.length; imgIndex++) {
    var existing = imgs[imgIndex];
    var existingSrc = existing.getAttribute('src');
    if (existingSrc && !existing.getAttribute('alt') && imageAltsBySrc[existingSrc]) {
      existing.setAttribute('alt', imageAltsBySrc[existingSrc]);
    }
  }

  for (var i = 0; i < nodes.length; i++) {
    var node = nodes[i];
    var text = normalizeText(node.textContent);
    var sourceMarkup = markupByText[text];
    if (sourceMarkup) {
      node.innerHTML = sourceMarkup;
    }

    var media = mediaByAnchor[text];
    if (!media) continue;

    for (var j = 0; j < media.length; j++) {
      var item = media[j];
      var existingImg = doc.body.querySelector('img[src="' + item.src.replace(/"/g, '\\"') + '"]');
      if (existingImg) {
        if (!existingImg.getAttribute('alt')) {
          var restoredAlt = imageAltFromMediaHtml(item.html);
          if (restoredAlt) existingImg.setAttribute('alt', restoredAlt);
        }
        continue;
      }
      if (restoredMedia[item.src]) {
        continue;
      }
      var tmp = doc.createElement('div');
      tmp.innerHTML = item.html;
      var mediaNode = tmp.firstElementChild;
      if (!mediaNode) continue;
      if (item.position === 'before') {
        node.parentNode.insertBefore(mediaNode, node);
      } else {
        node.parentNode.insertBefore(mediaNode, node.nextSibling);
      }
      restoredMedia[item.src] = true;
    }
  }

  return doc.body.innerHTML || extractedHtml;
}

function removeDuplicateMarkdownCaptions(markdown) {
  var blocks = String(markdown || '').split(/\n{2,}/);
  var cleaned = [];

  for (var i = 0; i < blocks.length; i++) {
    var block = blocks[i];
    var imageMatch = block.match(/^!\[([^\]]+)\]\([^)]+\)$/);
    cleaned.push(block);
    if (!imageMatch) {
      continue;
    }

    var previousCaption = '';
    while (i + 1 < blocks.length) {
      var nextCaption = normalizeText(blocks[i + 1]);
      if (!nextCaption) {
        break;
      }

      if (!previousCaption) {
        previousCaption = nextCaption;
        cleaned.push(blocks[i + 1]);
        i++;
        continue;
      }

      if (nextCaption === previousCaption) {
        i++;
      } else {
        break;
      }
    }
  }

  return cleaned.join('\n\n');
}

function parseReaderContent(htmlContent, link, bannerImage) {
  var cleanedHtml = cleanHtmlForMercury(htmlContent, link, bannerImage);
  var sourceInlineMarkup = collectInlineMarkupByText(cleanedHtml);
  var sourceMedia = collectMediaByTextAnchor(cleanedHtml);
  var sourceImageAlts = collectImageAltsBySrc(cleanedHtml);

  return Mercury.parse(link, {
    html: cleanedHtml,
    contentType: 'html',
    fetchAllPages: false
  }).then(function (result) {
    if (!result || !result.content) {
      return JSON.stringify({ error: 'Mercury returned no content' });
    }

    var turndown = new TurndownService({ headingStyle: 'atx', codeBlockStyle: 'fenced' });
    var content = restoreSourceDetails(result.content, sourceInlineMarkup, sourceMedia, sourceImageAlts);
    var markdown = removeDuplicateMarkdownCaptions(turndown.turndown(content));

    return JSON.stringify({
      content: markdown,
      title: result.title || null,
      siteName: result.domain || null
    });
  }).catch(function (error) {
    return JSON.stringify({
      error: error && error.message ? error.message : String(error)
    });
  });
}
