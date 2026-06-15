# reader-parser-bundle

Regenerates the ES5 JavaScript bundles used by the **desktop** reader-mode
parser. The desktop parser ([`DesktopFeedItemParserWorker.kt`](../../shared/src/jvmMain/kotlin/com/prof18/feedflow/shared/domain/parser/DesktopFeedItemParserWorker.kt))
runs JavaScript inside HtmlUnit's Rhino-derived engine, which only supports
ES5. These third-party libraries ship as modern ESM/CJS, so they are bundled
and transpiled here.

## Outputs

Written to `shared/src/jvmMain/resources/`:

| File | Library | Bundled global |
| --- | --- | --- |
| `readability-es5.js` | [`@mozilla/readability`](https://github.com/mozilla/readability) | `Readability` |
| `turndown-es5.js` | [`turndown`](https://github.com/mixmark-io/turndown) | `TurndownService` |

`reader-content-parser.js` (the glue that calls `new Readability(doc).parse()`
and then Turndown) is **hand-written** and is *not* regenerated here.

## Pipeline

For each library:

1. **esbuild** bundles the CJS/ESM entry into a single IIFE assigned to a global
   (`--format=iife --global-name=...`), no minification.
2. **Babel** (`@babel/preset-env`, target `ie 11`) transpiles the bundle to ES5.
3. A `;var <Alias>=<Module>;` shim is appended so the glue can use the familiar
   global name (HtmlUnit's engine has no module loader).

Pinned tool/library versions live in `package.json`.

## Usage

```bash
npm install        # first time
npm run build      # regenerate the two bundles in-place
npm run verify     # build in memory and diff against the committed bundles (CI-friendly)
```

## Forking Readability

To experiment with a patched Readability:

1. Point the `@mozilla/readability` dependency in `package.json` at your fork —
   a git URL (`github:you/readability#branch`) or a local checkout
   (`file:../../../readability`).
2. `npm install`
3. `npm run build`
4. Rebuild/run the desktop app to test against real pages.
