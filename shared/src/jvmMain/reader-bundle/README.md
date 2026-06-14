# Desktop reader JS bundle (GraalJS)

The desktop reader runs Defuddle on GraalJS with linkedom as a pure-JS DOM. GraalJS
does not provide the browser globals Defuddle expects, so this directory builds a
small compatibility bundle around the upstream Defuddle package.

## Source Files

- `package.json` pins the upstream packages used by the desktop reader.
- `bundle-src/prelude.js` provides minimal UTF-8 `TextEncoder`/`TextDecoder`
  globals before bundled dependencies initialize.
- `bundle-src/reader-globals.js` installs the DOM/URL/fetch globals GraalJS lacks.
- `bundle-src/reader-mode-parser.js` imports `defuddle/full`, runs article cleanup,
  and exposes `globalThis.parseReaderContent`.
- `build-bundle.mjs` runs esbuild and writes the generated runtime resource.

## Generated Resource

`npm run build` writes a single generated file:

- `../resources/reader-mode-parser.js`

Do not edit the generated file by hand. Change the source files above and rebuild.

## Updating Defuddle

Android and iOS use Defuddle's upstream browser bundle. Desktop uses the same
Defuddle package version through npm, but imports `defuddle/full` because the
desktop renderer needs Markdown output.

When updating Defuddle:

1. Update Android/iOS from the upstream `kepano/defuddle` release as usual.
2. Set the same Defuddle version in this directory's `package.json`.
3. Regenerate the desktop bundle:

```bash
cd shared/src/jvmMain/reader-bundle
npm install
npm run build
```

Re-run the JVM reader tests after regenerating.

## Benchmarking

The JVM benchmark is opt-in so normal test runs stay deterministic:

```bash
FEEDFLOW_READER_BENCHMARK=true ./gradlew --quiet --console=plain :shared:jvmTest --tests "com.prof18.feedflow.shared.domain.parser.ReaderModeJsBenchmarkTest"
```
