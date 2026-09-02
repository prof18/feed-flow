import { createServer } from 'node:http';
import { readFile } from 'node:fs/promises';
import { extname, join, normalize } from 'node:path';
import { buildPage } from './render/page.mjs';
import { loadCopy, availableLocales } from './copy.mjs';

const TYPES = {
  '.html': 'text/html; charset=utf-8',
  '.css': 'text/css; charset=utf-8',
  '.png': 'image/png',
  '.svg': 'image/svg+xml',
  '.woff2': 'font/woff2',
};

const ASSETS = new URL('../assets/', import.meta.url).pathname;

// In preview mode modules are re-imported per request with a cache-busting
// query, so edits show up on reload instead of serving what was loaded at
// startup.
async function fresh(specifier) {
  return import(`${specifier}?t=${Date.now()}`);
}

export async function startServer({ platform, platforms, port = 0, preview = false }) {
  const map = platforms ?? { [platform.id]: platform };
  const ids = Object.keys(map);

  const server = createServer(async (req, res) => {
    const url = new URL(req.url, 'http://localhost');

    if (url.pathname.startsWith('/assets/')) {
      const rel = normalize(url.pathname.slice('/assets/'.length)).replace(/^(\.\.[/\\])+/, '');
      try {
        const buf = await readFile(join(ASSETS, rel));
        res.writeHead(200, { 'content-type': TYPES[extname(rel)] ?? 'application/octet-stream' });
        return res.end(buf);
      } catch {
        res.writeHead(404);
        return res.end('not found');
      }
    }

    const platformId = url.searchParams.get('platform') ?? ids[0];

    // Preview-only: render PNGs to build/ on demand, so the page has an Export
    // button rather than requiring a trip to the terminal.
    if (preview && url.pathname === '/export') {
      const which = url.searchParams.get('locale') ?? 'base';
      const locales = which === 'all' ? availableLocales() : [which];
      try {
        const { exportScreens } = await fresh('./export.mjs');
        const { loadPlatforms } = await fresh('./platforms.mjs');
        const live = await loadPlatforms();
        const { warnings, skipped, files, outRoot } = await exportScreens({
          platform: live[platformId],
          platformId,
          locales,
        });
        res.writeHead(200, { 'content-type': 'application/json' });
        return res.end(JSON.stringify({ ok: true, count: files.length, outRoot, warnings, skipped }));
      } catch (err) {
        res.writeHead(500, { 'content-type': 'application/json' });
        return res.end(JSON.stringify({ ok: false, error: String(err.message ?? err) }));
      }
    }

    const locale = url.searchParams.get('locale') ?? 'base';
    try {
      let build = buildPage;
      let plat = map[platformId];
      if (preview) {
        ({ buildPage: build } = await fresh('./render/page.mjs'));
        const { loadPlatforms } = await fresh('./platforms.mjs');
        plat = (await loadPlatforms())[platformId];
      }
      if (!plat) throw new Error(`Unknown platform "${platformId}". Known: ${ids.join(', ')}`);

      const copy = loadCopy(locale);
      const html = build(plat, copy, {
        locale,
        preview,
        locales: preview ? availableLocales() : [],
        platforms: preview ? ids : [],
      });
      res.writeHead(200, { 'content-type': TYPES['.html'] });
      return res.end(html);
    } catch (err) {
      res.writeHead(500, { 'content-type': 'text/plain; charset=utf-8' });
      return res.end(String(err.message ?? err));
    }
  });

  await new Promise((resolve) => server.listen(port, '127.0.0.1', resolve));
  return { server, port: server.address().port };
}
