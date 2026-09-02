#!/usr/bin/env node
import { startServer } from './src/server.mjs';
import { loadPlatforms } from './src/platforms.mjs';
import { availableLocales } from './src/copy.mjs';

const platforms = await loadPlatforms();
const { port } = await startServer({ platforms, port: 4321, preview: true });

console.log(`FeedFlow screenshot preview -> http://127.0.0.1:${port}/`);
console.log(`Platforms: ${Object.keys(platforms).join(', ')}`);
console.log(`Locales:   ${availableLocales().join(', ')}`);
console.log('Config and template edits apply on reload -- no restart needed.');
