// Regenerates the ES5 reader-mode JS bundles consumed by the desktop HtmlUnit
// parser (DesktopFeedItemParserWorker). HtmlUnit's JS engine is Rhino-derived
// and ES5-only, so each library is bundled with esbuild (IIFE + global name)
// and then transpiled down to ES5 with Babel. A trailing global-alias shim is
// appended so the hand-written reader-content-parser.js can reference the
// libraries by their familiar names.
//
// Usage:
//   node build.mjs          # write the bundles into shared/.../resources
//   node build.mjs --check  # build in memory and diff against the committed
//                           # bundles; exit 1 if they differ (CI / verification)
//
// Forking Readability: point the "@mozilla/readability" dependency in
// package.json at your fork (a git URL or a "file:" path), re-run `npm install`,
// then `npm run build`.

import { build } from "esbuild";
import { transform } from "@babel/core";
import { createRequire } from "node:module";
import { readFileSync, writeFileSync } from "node:fs";
import { dirname, relative, resolve } from "node:path";
import { fileURLToPath } from "node:url";

const require = createRequire(import.meta.url);
const here = dirname(fileURLToPath(import.meta.url));
const resourcesDir = resolve(here, "../../shared/src/jvmMain/resources");

const targets = [
  {
    entry: require.resolve("@mozilla/readability/Readability.js"),
    globalName: "ReadabilityModule",
    alias: "Readability",
    outFile: resolve(resourcesDir, "readability-es5.js"),
  },
  {
    entry: require.resolve("turndown/lib/turndown.browser.cjs.js"),
    globalName: "TurndownModule",
    alias: "TurndownService",
    outFile: resolve(resourcesDir, "turndown-es5.js"),
  },
];

async function bundleOne(target) {
  const result = await build({
    entryPoints: [target.entry],
    bundle: true,
    format: "iife",
    globalName: target.globalName,
    platform: "browser",
    minify: false,
    legalComments: "none",
    write: false,
  });

  const transpiled = transform(result.outputFiles[0].text, {
    babelrc: false,
    configFile: false,
    compact: false,
    comments: true,
    presets: [["@babel/preset-env", { targets: { ie: "11" } }]],
    // Quote reserved-word member access (e.g. obj.return -> obj["return"]) for
    // maximally conservative ES5, matching the committed bundles.
    plugins: ["@babel/plugin-transform-member-expression-literals"],
  }).code;

  // Match the committed layout: the alias shim sits on its own line after the
  // bundle's closing `}();`, with a trailing newline.
  return `${transpiled}\n;var ${target.alias}=${target.globalName};\n`;
}

const check = process.argv.includes("--check");
let mismatch = false;

for (const target of targets) {
  const generated = await bundleOne(target);
  const rel = relative(resolve(here, "../.."), target.outFile);

  if (check) {
    let current = "";
    try {
      current = readFileSync(target.outFile, "utf8");
    } catch {
      // missing file counts as a mismatch
    }
    if (current === generated) {
      console.log(`ok    ${rel}`);
    } else {
      mismatch = true;
      console.log(`DIFF  ${rel} (regenerated output differs from committed file)`);
    }
  } else {
    writeFileSync(target.outFile, generated);
    console.log(`wrote ${rel}`);
  }
}

if (check && mismatch) {
  process.exitCode = 1;
}
