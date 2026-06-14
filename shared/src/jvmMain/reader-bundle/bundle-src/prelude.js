// Injected as the very first code in the bundle (esbuild banner), so these exist
// before any bundled module initializes. Minimal UTF-8 only.
(function () {
  if (typeof globalThis.TextEncoder === 'undefined') {
    globalThis.TextEncoder = class TextEncoder {
      get encoding() { return 'utf-8'; }
      encode(input) {
        var s = String(input == null ? '' : input);
        var out = [];
        for (var i = 0; i < s.length; i++) {
          var c = s.charCodeAt(i);
          if (c < 0x80) {
            out.push(c);
          } else if (c < 0x800) {
            out.push(0xc0 | (c >> 6), 0x80 | (c & 0x3f));
          } else if (c >= 0xd800 && c < 0xdc00) {
            c = 0x10000 + (((c & 0x3ff) << 10) | (s.charCodeAt(++i) & 0x3ff));
            out.push(0xf0 | (c >> 18), 0x80 | ((c >> 12) & 0x3f), 0x80 | ((c >> 6) & 0x3f), 0x80 | (c & 0x3f));
          } else {
            out.push(0xe0 | (c >> 12), 0x80 | ((c >> 6) & 0x3f), 0x80 | (c & 0x3f));
          }
        }
        return new Uint8Array(out);
      }
    };
  }
  if (typeof globalThis.TextDecoder === 'undefined') {
    globalThis.TextDecoder = class TextDecoder {
      get encoding() { return 'utf-8'; }
      decode(buf) {
        var b = buf instanceof Uint8Array ? buf : new Uint8Array(buf && buf.buffer ? buf.buffer : buf || []);
        var s = '';
        for (var i = 0; i < b.length;) {
          var c = b[i++];
          if (c < 0x80) {
            s += String.fromCharCode(c);
          } else if (c < 0xe0) {
            s += String.fromCharCode(((c & 0x1f) << 6) | (b[i++] & 0x3f));
          } else if (c < 0xf0) {
            s += String.fromCharCode(((c & 0x0f) << 12) | ((b[i++] & 0x3f) << 6) | (b[i++] & 0x3f));
          } else {
            var cp = ((c & 0x07) << 18) | ((b[i++] & 0x3f) << 12) | ((b[i++] & 0x3f) << 6) | (b[i++] & 0x3f);
            cp -= 0x10000;
            s += String.fromCharCode(0xd800 + (cp >> 10), 0xdc00 + (cp & 0x3ff));
          }
        }
        return s;
      }
    };
  }
})();
