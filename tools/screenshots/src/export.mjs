import { mkdir } from 'node:fs/promises';
import { join } from 'node:path';
import { chromium } from 'playwright-core';
import { startServer } from './server.mjs';
import { loadCopy } from './copy.mjs';

export const OUT_ROOT = new URL('../build/', import.meta.url).pathname;

async function launch() {
  for (const channel of ['chrome', 'chromium']) {
    try {
      return await chromium.launch({ channel });
    } catch {
      /* try next */
    }
  }
  return chromium.launch();
}

function assertComplete(locale, copy) {
  if (!copy.missing.length) return;
  const shown = copy.missing.slice(0, 5).join('\n    ');
  const more = copy.missing.length > 5 ? `\n    ...and ${copy.missing.length - 5} more` : '';
  throw new Error(
    `Locale "${locale}" is missing ${copy.missing.length} key(s) present in base:\n    ${shown}${more}\n` +
      `Add them to assets/screenshotcopy/${locale}/screenshot_copy.yml. Do not invent translations.`
  );
}

/**
 * Renders `locales` for one platform to build/<locale>/<platform>/.
 * Returns { files, warnings } — warnings describe any spacing relief applied.
 */
export async function exportScreens({ platform, platformId, locales, onFile }) {
  const browser = await launch();
  const { server, port } = await startServer({ platform });
  const files = [];
  const warnings = [];
  const skipped = [];

  try {
    // A platform with no on-image copy is identical in every language.
    const effective = platform.localised === false ? ['base'] : locales;
    for (const locale of effective) {
      const copy = loadCopy(locale);
      assertComplete(locale, copy);

      // A locale whose values are still blank would render empty headlines.
      // Skip it rather than emit a screenshot with no words on it.
      if (copy.untranslated.length) {
        skipped.push({ locale, count: copy.untranslated.length, total: copy.untranslated.length + copy.translated });
        continue;
      }

      const page = await browser.newPage({ deviceScaleFactor: 1 });
      await page.goto(`http://127.0.0.1:${port}/?locale=${encodeURIComponent(locale)}`, {
        waitUntil: 'networkidle',
      });
      await page.waitForFunction(() => document.documentElement.dataset.layoutReady === 'true');

      const outDir = join(OUT_ROOT, locale, platformId);
      await mkdir(outDir, { recursive: true });

      for (const el of await page.locator('.screen').all()) {
        const id = await el.getAttribute('data-id');
        const shrunk = await el.getAttribute('data-shrunk');
        const compressed = await el.getAttribute('data-compressed');
        const overflow = await el.getAttribute('data-overflow');
        const size = await el.getAttribute('data-headline-size');
        const scale = await el.getAttribute('data-device-scale');

        const path = join(outDir, `${id}.png`);
        await el.screenshot({ path });
        files.push(path);
        onFile?.(`${locale}/${platformId}/${id}.png`);

        const where = `${locale}/${platformId}/${id}`;
        if (overflow === 'true') warnings.push(`${where}: OVERFLOWS canvas`);
        else if (shrunk === 'true') warnings.push(`${where}: headline shrunk to ${size}px`);
        else if (compressed === 'true') warnings.push(`${where}: top margin reduced`);
        else if (scale !== null && Number(scale) < 1)
          warnings.push(`${where}: device scaled to ${Math.round(scale * 100)}%`);
      }
      await page.close();
    }
  } finally {
    server.close();
    await browser.close();
  }

  return { files, warnings, skipped, outRoot: OUT_ROOT };
}
