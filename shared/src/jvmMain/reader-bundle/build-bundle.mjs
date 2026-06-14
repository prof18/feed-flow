// Builds the single desktop reader JS resource consumed by ReaderModeJsRuntime.
//
// Run from this directory: `npm install && npm run build`.
import * as esbuild from 'esbuild';
import { polyfillNode } from 'esbuild-plugin-polyfill-node';
import fs from 'node:fs';
import path from 'node:path';
import { fileURLToPath } from 'node:url';

const here = path.dirname(fileURLToPath(import.meta.url));
const resources = path.resolve(here, '../resources');
const prelude = fs.readFileSync(path.join(here, 'bundle-src/prelude.js'), 'utf8');

const common = {
  bundle: true,
  format: 'iife',
  platform: 'browser',
  plugins: [polyfillNode({ globals: { buffer: true, process: true } })],
  legalComments: 'none',
  minify: true,
};

await esbuild.build({
  ...common,
  entryPoints: [path.join(here, 'bundle-src/reader-mode-parser.js')],
  outfile: path.join(resources, 'reader-mode-parser.js'),
  banner: { js: prelude },
});

console.log('reader bundle built ->', resources);
