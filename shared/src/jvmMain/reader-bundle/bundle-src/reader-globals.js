import { DOMParser, parseHTML } from 'linkedom';
import { URL, URLSearchParams } from 'whatwg-url';

globalThis.URL = globalThis.URL || URL;
globalThis.URLSearchParams = globalThis.URLSearchParams || URLSearchParams;

if (typeof globalThis.fetch !== 'function') {
  globalThis.fetch = function () {
    return Promise.reject(new Error('fetch is disabled in the GraalJS reader runtime'));
  };
}

if (typeof globalThis.window === 'undefined') {
  globalThis.window = globalThis;
}

globalThis.DOMParser = DOMParser;
globalThis.parseHTML = parseHTML;
globalThis.window.DOMParser = DOMParser;

export { parseHTML };
