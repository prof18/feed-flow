#!/usr/bin/env node
// Checks the rendered English layout against the frozen Figma English board,
// then checks margins on every locale. Run after changing any config geometry.
import { chromium } from 'playwright-core';
import { startServer } from './src/server.mjs';
import { availableLocales, loadCopy } from './src/copy.mjs';
import { loadPlatforms } from './src/platforms.mjs';

// Figma's own device x values drifted off-centre (03 by 4px, 04 by 4px, 05 by
// 35px) while 01 and 02 were centred to half a pixel. We centre all of them, so
// horizontal position is checked against "centred", not against Figma.
const TOLERANCE = 1;

const PLATFORMS = await loadPlatforms();

async function measure(page, port, locale, CANVAS_W) {
  await page.goto(`http://127.0.0.1:${port}/?locale=${encodeURIComponent(locale)}`, {
    waitUntil: 'networkidle',
  });
  await page.waitForFunction(() => document.documentElement.dataset.layoutReady === 'true');
  return page.evaluate((CANVAS_W) => {
    const n = (v) => Math.round(parseFloat(v) * 100) / 100;
    // What is actually painted: a clipped frame shows only the part of its
    // cards that falls inside it.
    const paint = (el) => {
      const r = el.getBoundingClientRect();
      const cards = [...el.querySelectorAll('.cmp-card')];
      if (!cards.length) return r;
      const left = Math.max(r.left, Math.min(...cards.map((c) => c.getBoundingClientRect().left)));
      const right = Math.min(r.right, Math.max(...cards.map((c) => c.getBoundingClientRect().right)));
      return { left, right, bottom: r.bottom };
    };
    const rows = [];
    for (const s of document.querySelectorAll('.screen:not(.feature-graphic)')) {
      const text = s.querySelector('.text');
      if (!text) {
        const shot = s.querySelector('.shot');
        if (shot) {
          const sr = s.getBoundingClientRect();
          const r = shot.getBoundingClientRect();
          rows.push({
            id: s.dataset.id,
            plain: true,
            shotL: n(r.left - sr.left),
            shotR: n(sr.right - r.right),
            shotT: n(r.top - sr.top),
            shotB: n(sr.bottom - r.bottom),
          });
        }
        continue;
      }
      const h1 = text.querySelector('h1');
      const sub = text.querySelector('p');
      const dev = s.querySelector('.device');
      const top = n(text.style.top);
      rows.push({
        id: s.dataset.id,
        split: s.classList.contains('split'),
        headTop: top,
        headSize: n(getComputedStyle(h1).fontSize),
        subTop: n(top + h1.offsetHeight + parseFloat(sub.style.marginTop)),
        devTop: n(dev.style.top),
        textL: n(text.style.left),
        headR: n(CANVAS_W - n(text.style.left) - h1.offsetWidth),
        subR: n(CANVAS_W - n(text.style.left) - sub.offsetWidth),
        devL: n(paint(dev).left - s.getBoundingClientRect().left),
        devR: n(s.getBoundingClientRect().right - paint(dev).right),
        bottom: n(
          s.getBoundingClientRect().bottom - dev.getBoundingClientRect().bottom
        ),
        overflow: s.dataset.overflow === 'true',
      });
    }
    const fg = document.querySelector('.screen.feature-graphic');
    if (fg) {
      const el = fg.querySelector('.fg-headline');
      rows.push({
        id: fg.dataset.id,
        fgTop: n(el.style.top),
        fgBottom: n(fg.offsetHeight - parseFloat(el.style.top) - el.offsetHeight),
        overflow: fg.dataset.overflow === 'true',
      });
    }
    return rows;
  }, CANVAS_W);
}

const failures = [];
const browser = await chromium.launch({ channel: 'chrome' }).catch(() => chromium.launch());

for (const [id, platform] of Object.entries(PLATFORMS)) {
  const { server, port } = await startServer({ platform });
  const page = await browser.newPage({ deviceScaleFactor: 1 });

  console.log(`\n${id} — English vs Figma`);
  const en = await measure(page, port, 'base', platform.canvas.w);
  for (const row of en) {
    const ref = platform.figmaReference?.[row.id];
    if (!ref) continue;
    const diffs = Object.entries(ref)
      .map(([k, v]) => [k, Math.round((row[k] - v) * 100) / 100])
      .filter(([, v]) => Math.abs(v) > TOLERANCE);
    if (diffs.length) {
      failures.push(`${id}/${row.id}: ${diffs.map(([k, v]) => `${k} ${v > 0 ? '+' : ''}${v}`).join(', ')}`);
      console.log(`  FAIL ${row.id}: ${diffs.map(([k, v]) => `${k} ${v > 0 ? '+' : ''}${v}`).join(', ')}`);
    } else {
      console.log(`  ok   ${row.id}`);
    }
  }

  console.log(`\n${id} — margins, all locales`);
  const translated =
    platform.localised === false
      ? ['base']
      : availableLocales().filter((l) => loadCopy(l).complete);
  const untranslated =
    platform.localised === false ? [] : availableLocales().filter((l) => !loadCopy(l).complete);
  if (untranslated.length)
    console.log(`  (skipping untranslated: ${untranslated.join(', ')})`);
  for (const locale of translated) {
    const rows = await measure(page, port, locale, platform.canvas.w);
    const problems = [];
    for (const r of rows) {
      if (r.overflow) problems.push(`${r.id} OVERFLOWS`);
      if (r.plain) {
        if (Math.abs(r.shotL - r.shotR) > TOLERANCE)
          problems.push(`${r.id} capture off-centre by ${Math.abs(r.shotL - r.shotR).toFixed(1)}`);
        for (const [edge, v] of [['top', r.shotT], ['bottom', r.shotB]])
          if (v < 40) problems.push(`${r.id} ${edge} margin ${v}`);
        continue;
      }
      if (r.fgTop !== undefined) {
        if (r.fgTop < 40 - TOLERANCE || r.fgBottom < 40 - TOLERANCE)
          problems.push(`${r.id} feature-graphic padding ${r.fgTop}/${r.fgBottom}`);
        continue;
      }
      if (!r.split && Math.abs(r.devL - r.devR) > TOLERANCE)
        problems.push(`${r.id} device off-centre by ${Math.abs(r.devL - r.devR).toFixed(1)}`);
      if (r.headR < r.textL - TOLERANCE)
        problems.push(`${r.id} headline right margin ${r.headR} < left ${r.textL}`);
      if (r.subR < r.textL - TOLERANCE)
        problems.push(`${r.id} sub right margin ${r.subR} < left ${r.textL}`);
      const floor = platform.rhythm?.minBottom ?? 40;
      if (r.bottom < floor - TOLERANCE)
        problems.push(`${r.id} bottom margin ${r.bottom} < ${floor}`);
    }
    if (problems.length) {
      failures.push(...problems.map((p) => `${id}/${locale}/${p}`));
      console.log(`  FAIL ${locale}: ${problems.join('; ')}`);
    } else {
      console.log(`  ok   ${locale}`);
    }
  }

  await page.close();
  server.close();
}

await browser.close();
if (failures.length) {
  console.log(`\n${failures.length} failure(s).`);
  process.exit(1);
}
console.log('\nAll checks passed.');
