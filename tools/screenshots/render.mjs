#!/usr/bin/env node
import { exportScreens, OUT_ROOT } from './src/export.mjs';
import { availableLocales } from './src/copy.mjs';
import { loadPlatforms } from './src/platforms.mjs';

const PLATFORMS = await loadPlatforms();

function parseArgs(argv) {
  const args = { locales: [], platforms: [], all: false };
  for (let i = 0; i < argv.length; i++) {
    const a = argv[i];
    if (a === '--all') args.all = true;
    else if (a === '--locale') args.locales.push(argv[++i]);
    else if (a === '--platform') args.platforms.push(argv[++i]);
    else throw new Error(`Unknown argument: ${a}`);
  }
  if (args.all) args.locales = availableLocales();
  if (!args.locales.length) args.locales = ['base'];
  if (!args.platforms.length) args.platforms = Object.keys(PLATFORMS);
  return args;
}

const args = parseArgs(process.argv.slice(2));
const warnings = [];
const skipped = [];

for (const platformId of args.platforms) {
  const platform = PLATFORMS[platformId];
  if (!platform) throw new Error(`Unknown platform: ${platformId}`);
  const res = await exportScreens({
    platform,
    platformId,
    locales: args.locales,
    onFile: (f) => process.stdout.write(`  ${f}\n`),
  });
  warnings.push(...res.warnings);
  skipped.push(...res.skipped);
}

if (skipped.length) {
  console.log('\nSkipped — copy not translated yet:');
  for (const s of skipped)
    console.log(`  - ${s.locale}: ${s.count} of ${s.total} values are still empty`);
}
if (warnings.length) {
  console.log('\nWarnings:');
  for (const w of warnings) console.log(`  - ${w}`);
}
console.log(`\nDone -> ${OUT_ROOT}`);
