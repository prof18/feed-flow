const esc = (s) =>
  String(s).replace(/&/g, '&amp;').replace(/</g, '&lt;').replace(/>/g, '&gt;').replace(/"/g, '&quot;');

const px = (n) => `${n}px`;

function blobs(list) {
  return list
    .map(
      (b) =>
        `<div class="blob" style="left:${px(b.x)};top:${px(b.y)};width:${px(b.size)};height:${px(
          b.size
        )};background:${b.color};opacity:${b.opacity}"></div>`
    )
    .join('');
}

function capture(assetBase, platformId, file, inset, w, h) {
  const src = `${assetBase}/captures/${platformId}/${file}`;
  const s = inset
    ? `left:${px(inset.x)};top:${px(inset.y)};width:${px(inset.w)};height:${px(inset.h)}`
    : `left:0;top:0;width:${px(w)};height:${px(h)}`;
  return `<img class="capture" src="${src}" style="${s}" alt="">`;
}

function phoneDevice(d, platformId, assetBase, canvasW, centre) {
  const { screen: sc, notch } = d;
  const x = centre ? (canvasW - d.w) / 2 : d.x;
  return `<div class="device" style="left:${px(x)};width:${px(d.w)};height:${px(
    d.h
  )};border-radius:${px(d.radius)};background:${d.bg}">
    <div class="device-screen" style="left:${px(sc.x)};top:${px(sc.y)};width:${px(sc.w)};height:${px(
    sc.h
  )};border-radius:${px(sc.radius)};background:${sc.bg}">
      ${capture(assetBase, platformId, d.capture, d.captureInset, sc.w, sc.h)}
    </div>
    <div class="notch" style="left:${px(notch.x)};top:${px(notch.y)};width:${px(notch.w)};height:${px(
    notch.h
  )};background:${notch.color}"></div>
  </div>`;
}

function comparison(c, platformId, assetBase, copy, key, canvasW, centre) {
  const panels = c.panels
    .map(
      (p) => `<div class="cmp-card" style="left:${px(p.x)};top:${px(p.y ?? c.card.y)};width:${px(
        p.w ?? c.card.w
      )};height:${px(c.card.h)};border-radius:${px(c.card.radius)};border:${c.card.border};box-shadow:${
        c.card.shadow
      }">
        <img class="capture" src="${assetBase}/captures/${platformId}/${p.capture}" style="left:${px(
        p.img.x
      )};top:${px(p.img.y)};width:${px(p.img.w)};height:${px(p.img.h)}" alt="">
      </div>`
    )
    .join('');

  // Chips are emitted after the cards so they win the z-order without a z-index.
  const chips = c.panels
    .map((p, i) => {
      const next = c.panels[i + 1];
      const own = p.x + (p.w ?? c.card.w);
      // When the next card overlaps this one, only the part left of it is
      // visible, so centre there. When they are separate, centre on the card.
      const visibleRight = next && next.x < own ? next.x : own;
      return { p, centre: (p.x + visibleRight) / 2 };
    })
    .map(
      ({ p, centre }) => {
        const k = { ...c.chip, ...(p.chip.style ?? {}) };
        return `<div class="chip" style="left:${px(centre)};top:${px(k.y)};height:${px(
          k.h
        )};padding:0 ${px(k.padX)};background:${k.bg};border:${k.border};border-radius:${px(
          k.radius
        )};box-shadow:${k.shadow};color:${k.color};font-size:${px(k.size)};font-weight:${
          k.weight
        }">${esc(copy.get(key, p.chip.labelKey))}</div>`;
      }
    )
    .join('');

  const contentLeft = c.panels[0].x;
  const last = c.panels[c.panels.length - 1];
  const contentRight = Math.min(c.w - c.pad, last.x + (last.w ?? c.card.w));
  const frameX = centre
    ? (canvasW - (contentRight - contentLeft)) / 2 - c.pad - contentLeft
    : c.x;

  return `<div class="device comparison" style="left:${px(frameX)};width:${px(c.w)};height:${px(c.h)}">
    <div class="cmp-inner" style="left:${px(c.pad)};top:${px(c.pad)};width:${px(c.w - c.pad * 2)};height:${px(
    c.h - c.pad * 2
  )}">${panels}${chips}</div>
  </div>`;
}

function plainScreen(p, s, assetBase, centre) {
  const shot = s.shot;
  // Children sit inside the border, so the space to centre within is the
  // canvas minus both borders.
  const inner = p.canvas.w - 2 * (p.canvas.borderWidth ?? 0);
  const shotX = centre ? (inner - shot.w) / 2 : shot.x;
  return `<section class="screen plain" data-id="${s.id}" style="width:${px(p.canvas.w)};height:${px(
    p.canvas.h
  )};background:${p.canvas.bg};${p.canvas.border ? `border:${p.canvas.border};` : ''}${
    p.canvas.radius ? `border-radius:${px(p.canvas.radius)};` : ''
  }">
    ${blobs(p.blobs)}
    <div class="shot" style="left:${px(shotX)};top:${px(shot.y)};width:${px(shot.w)};height:${px(
    shot.h
  )};${shot.shadow ? `box-shadow:${shot.shadow};` : ''}${
    shot.radius ? `border-radius:${px(shot.radius)};` : ''
  }">
      <img src="${assetBase}/captures/${p.id}/${shot.capture}" alt="">
    </div>
  </section>`;
}

function screenHtml(p, s, copy, assetBase) {
  if (s.kind === 'plain') return plainScreen(p, s, assetBase, p.centreDevice);
  const t = p.text;
  const body =
    s.kind === 'comparison'
      ? comparison(s.comparison, p.id, assetBase, copy, s.key, p.canvas.w, p.centreDevice)
      : phoneDevice(s.device, p.id, assetBase, p.canvas.w, p.centreDevice && s.kind !== 'split');

  return `<section class="screen${s.kind === 'split' ? ' split' : ''}" data-id="${s.id}" data-gap1="${
    s.headlineToSub ?? p.rhythm.preferred.headlineToSub
  }" data-gap2="${s.subToDevice ?? p.rhythm.preferred.subToDevice}" data-top="${
    s.top ?? p.rhythm.preferred.top
  }" style="width:${px(p.canvas.w)};height:${px(
    p.canvas.h
  )};background:${p.canvas.bg};border-radius:${px(p.canvas.radius)}">
    ${blobs(p.blobs)}
    <div class="text" style="left:${px(s.textLeft ?? t.left)};width:${px(
      Math.min(s.textWidth, t.maxWidth ?? s.textWidth)
    )}">
      <h1 style="font-size:${px(s.headlineSize)};line-height:${t.headline.lineHeight};color:${
    t.headline.color
  };font-weight:${t.headline.weight}" data-base-size="${s.headlineSize}">${esc(
    copy.get(s.key, 'headline')
  )}</h1>
      <p style="width:${px(
    Math.min(s.subWidth ?? s.textWidth, t.maxWidth ?? Infinity)
  )};font-size:${px(
    t.sub.size
  )};line-height:${t.sub.lineHeight};color:${t.sub.color};font-weight:${
    t.sub.weight
  }">${esc(
    copy.get(s.key, 'subheadline')
  )}</p>
    </div>
    ${body}
  </section>`;
}

function featureGraphicHtml(p, copy, assetBase) {
  const f = p.featureGraphic;
  const phones = f.phones
    .map((ph) => {
      const inner = `<div class="device" style="left:0;top:0;width:${px(ph.w)};height:${px(
        ph.h
      )};border-radius:${px(ph.radius)};background:#111a18;box-shadow:${ph.shadow}">
        <div class="device-screen" style="left:${px(ph.screen.x)};top:${px(ph.screen.y)};width:${px(
        ph.screen.w
      )};height:${px(ph.screen.h)};border-radius:${px(ph.screen.radius)};background:#fbfcfa">
          <img class="capture" src="${assetBase}/captures/${p.id}/${ph.capture}" style="left:0;top:0;width:${px(
        ph.screen.w
      )};height:${px(ph.screen.h)}" alt="">
        </div>
        <div class="notch" style="left:${px(ph.notch.x)};top:${px(ph.notch.y)};width:${px(
        ph.notch.w
      )};height:${px(ph.notch.h)};background:#202b29;border-radius:999px"></div>
      </div>`;
      const w = ph.wrap ?? { w: ph.w, h: ph.h };
      return `<div class="fg-phone" style="left:${px(ph.x)};top:${px(ph.y)};width:${px(w.w)};height:${px(
        w.h
      )}"><div style="transform:rotate(${ph.rotate}deg);width:${px(ph.w)};height:${px(
        ph.h
      )}">${inner}</div></div>`;
    })
    .join('');

  const headline = copy.get(f.key, 'headline');
  return `<section class="screen feature-graphic" data-id="${f.id}" style="width:${px(f.w)};height:${px(
    f.h
  )};background:${f.bg}">
    ${blobs(f.blobs)}
    <div class="fg-headline" data-base-size="${f.headline.size}" style="left:${px(
    f.headline.x
  )};width:${px(f.headline.w)};font-size:${px(f.headline.size)};line-height:${
    f.headline.lineHeight
  };font-weight:${f.headline.weight};color:${f.headline.color}">${esc(headline)}</div>
    ${phones}
  </section>`;
}

export function buildPage(platform, copy, { assetBase = '/assets', locale = 'base', preview = false, locales = [], platforms = [] } = {}) {
  const wrap = (html, id) =>
    preview ? `<div class="stage"><div class="cap">${esc(id)}</div>${html}</div>` : html;

  const screens = platform.screens
    .map((s) => wrap(screenHtml(platform, s, copy, assetBase), s.id))
    .join('\n');
  const fg = platform.featureGraphic
    ? wrap(featureGraphicHtml(platform, copy, assetBase), platform.featureGraphic.id)
    : '';

  const bar = preview
    ? `<div class="bar">
        ${
          platforms.length > 1
            ? platforms
                .map(
                  (pl) =>
                    `<a href="?platform=${encodeURIComponent(pl)}&locale=${encodeURIComponent(
                      locale
                    )}" class="${pl === platform.id ? 'on' : ''}">${esc(pl)}</a>`
                )
                .join('')
            : `<span>${esc(platform.id)}</span>`
        }<span class="sep"></span>
        ${locales
          .map(
            (l) =>
              `<a href="?platform=${encodeURIComponent(platform.id)}&locale=${encodeURIComponent(
                l
              )}" class="${l === locale ? 'on' : ''}">${esc(l)}</a>`
          )
          .join('')}
        <span class="sep"></span>
        <label>zoom <input id="zoom" type="range" min="5" max="50" value="22"> <span id="zv">22%</span></label>
        <span class="sep"></span>
        <button id="exp" data-locale="${esc(locale)}">Export ${esc(locale)}</button>
        <button id="expall" title="Every locale for this platform">Export all locales</button>
        <span id="expmsg"></span>
      </div>`
    : '';

  return `<!doctype html>
<html lang="${esc(locale)}">
<head>
<meta charset="utf-8">
<title>FeedFlow screenshots — ${esc(platform.id)} / ${esc(locale)}</title>
<link rel="stylesheet" href="${assetBase}/fonts/fonts.css">
<style>
  * { margin: 0; padding: 0; box-sizing: border-box; }
  body { background: #2a2a2a; display: flex; flex-wrap: wrap; gap: 40px; padding: 40px;
         font-family: Poppins, Montserrat, sans-serif; }
  body.preview { padding: 96px 40px 40px; align-items: flex-start; }
  body.preview { padding-top: var(--bar-h, 96px); }
  /* Export renders at 1:1; the preview only scales its wrapper, so layout
     measurement -- and therefore the auto-fit -- is identical in both. */
  .stage { flex: none; }
  .stage > .screen { transform-origin: top left; }
  .bar { position: fixed; inset: 0 0 auto 0; z-index: 10; display: flex; align-items: center;
         flex-wrap: wrap; gap: 8px 16px; padding: 12px 20px; background: #1b1b1b;
         border-bottom: 1px solid #383838; color: #ddd;
         font: 13px ui-monospace, SFMono-Regular, Menlo, monospace; }
  .bar a { color: #9fd8c8; text-decoration: none; padding: 3px 8px; border-radius: 5px; }
  .bar a:hover { background: #2c2c2c; }
  .bar a.on { background: #2f4f45; color: #eafff6; }
  .bar .sep { width: 1px; height: 18px; background: #3d3d3d; }
  .bar button { font: inherit; color: #eafff6; background: #2f4f45; border: 1px solid #3f6a5c;
                padding: 4px 10px; border-radius: 6px; cursor: pointer; }
  .bar button:hover:not(:disabled) { background: #3a6155; }
  .bar button:disabled { opacity: 0.5; cursor: default; }
  .bar #expmsg { color: #9a9a9a; }
  .bar #expmsg.err { color: #ff9d9d; }
  .bar label { display: flex; align-items: center; gap: 8px; color: #999; }
  .cap { position: absolute; margin-top: -22px; color: #8a8a8a;
         font: 11px ui-monospace, monospace; }
  .screen { position: relative; overflow: hidden; flex: none; }
  .blob { position: absolute; border-radius: 50%; }
  .text { position: absolute; }
  .text h1 { font-family: Poppins, Montserrat, sans-serif; text-wrap: balance; }
  /* Poppins carries the chip design but has no Cyrillic; the Montserrat
     fallback covers those glyphs per-character instead of silently
     substituting a system face. */
  .chip { font-family: Poppins, Montserrat, sans-serif;
          position: absolute; display: inline-flex; align-items: center; justify-content: center;
          white-space: nowrap; width: max-content; transform: translateX(-50%); }
  .device { position: absolute; overflow: hidden; }
  .device-screen { position: absolute; overflow: hidden; }
  .capture { position: absolute; max-width: none; object-fit: cover; pointer-events: none; }
  .notch { position: absolute; border-radius: 999px; }
  .comparison { overflow: hidden; }
  .cmp-inner { position: absolute; }
  .cmp-card { position: absolute; overflow: hidden; background: #fff; }
  .fg-phone { position: absolute; display: flex; align-items: center; justify-content: center; }
  .shot { position: absolute; overflow: hidden; }
  .shot img { display: block; width: 100%; height: 100%; object-fit: cover; }
  .fg-headline { position: absolute; white-space: pre-line; font-family: Poppins, Montserrat, sans-serif; }
</style>
</head>
<body class="${preview ? 'preview' : ''}">
${bar}
${screens}
${fg}
<script>
// Vertical rhythm is computed, never hand-tuned: the device sits a fixed gap
// below however tall the translated text actually renders. If a long
// translation would push the device past the canvas, the headline shrinks
// until it fits rather than the device moving off-canvas.
const RHYTHM = ${JSON.stringify(platform.rhythm)};
const CANVAS_H = ${platform.canvas.h};
const FG_HEADLINE = ${JSON.stringify(
  platform.featureGraphic ? platform.featureGraphic.headline : null
)};
const MIN_SCALE = 0.6;

// Relief order when a translation runs long, most-preferred first:
//   1. shrink the device (a few percent is invisible),
//   2. close the gap above the device,
//   3. close the gap under the headline,
//   4. give up top margin, down to a protected floor,
//   5. shrink the headline.
// Top margin is defended near the end on purpose: a headline crowded against
// the canvas edge reads far worse than a marginally smaller phone.
function layout() {
  for (const screen of document.querySelectorAll('.screen:not(.feature-graphic)')) {
    const text = screen.querySelector('.text');
    if (!text) continue;

    // A split screen puts the text column beside the mockup, both centred on
    // the canvas' vertical midline; the vertical rhythm does not apply.
    if (screen.classList.contains('split')) {
      const dev = screen.querySelector('.device');
      const h1s = text.querySelector('h1');
      const subs = text.querySelector('p');
      const bases = parseFloat(h1s.dataset.baseSize);
      const gap = parseFloat(screen.dataset.gap1);
      const room = CANVAS_H - RHYTHM.minBottom * 2;
      let size = bases;
      const th = () => {
        subs.style.marginTop = gap + 'px';
        return h1s.offsetHeight + gap + subs.offsetHeight;
      };
      while (th() > room && size > bases * MIN_SCALE) {
        size--;
        h1s.style.fontSize = size + 'px';
      }
      text.style.top = parseFloat(screen.dataset.top) + 'px';
      dev.style.top = (CANVAS_H - dev.offsetHeight) / 2 + 'px';
      screen.dataset.headlineSize = String(size);
      screen.dataset.deviceScale = '1';
      screen.dataset.shrunk = String(size !== bases);
      screen.dataset.overflow = String(th() > room);
      continue;
    }
    const device = screen.querySelector('.device');
    const h1 = text.querySelector('h1');
    const sub = text.querySelector('p');
    const base = parseFloat(h1.dataset.baseSize);
    const deviceH = device.offsetHeight;
    const limit = CANVAS_H - RHYTHM.minBottom;
    const baseGap2 = parseFloat(screen.dataset.gap2);

    let top = parseFloat(screen.dataset.top);
    let gap1 = parseFloat(screen.dataset.gap1);
    let gap2 = baseGap2;
    let size = base;
    let scale = 1;

    const textH = () => {
      sub.style.marginTop = gap1 + 'px';
      return h1.offsetHeight + gap1 + sub.offsetHeight;
    };
    // The device is centred on its own box, so it shrinks in place.
    const bestScale = () =>
      Math.min(1, (limit - top - textH() - gap2) / deviceH);
    const fits = () => top + textH() + gap2 + deviceH * scale <= limit + 0.5;

    scale = bestScale();
    if (scale < RHYTHM.deviceMinScale) {
      scale = RHYTHM.deviceMinScale;
      while (!fits() && gap2 > RHYTHM.min.subToDevice) gap2--;
      while (!fits() && gap1 > RHYTHM.min.headlineToSub) gap1--;
      while (!fits() && top > RHYTHM.min.top) top--;
      while (!fits() && size > base * MIN_SCALE) {
        size--;
        h1.style.fontSize = size + 'px';
      }
    }

    const height = textH();
    text.style.top = top + 'px';
    device.style.transformOrigin = 'top center';
    device.style.transform = 'scale(' + scale + ')';
    device.style.top = top + height + gap2 + 'px';

    screen.dataset.headlineSize = String(size);
    screen.dataset.deviceScale = scale.toFixed(3);
    screen.dataset.compressed = String(top !== parseFloat(screen.dataset.top));
    screen.dataset.shrunk = String(size !== base);
    screen.dataset.overflow = String(!fits());
  }
  for (const fg of document.querySelectorAll('.screen.feature-graphic')) {
    const el = fg.querySelector('.fg-headline');
    const cfg = FG_HEADLINE;
    const canvasH = fg.offsetHeight;
    const base = parseFloat(el.dataset.baseSize);
    const room = canvasH - cfg.minPadding * 2;

    let size = base;
    while (el.offsetHeight > room && size > base * cfg.minScale) {
      size--;
      el.style.fontSize = size + 'px';
    }

    el.style.top = Math.max(cfg.minPadding, (canvasH - el.offsetHeight) / 2) + 'px';
    fg.dataset.headlineSize = String(size);
    fg.dataset.shrunk = String(size !== base);
    fg.dataset.overflow = String(el.offsetHeight > room);
  }

  document.documentElement.dataset.layoutReady = 'true';
}

if (document.fonts && document.fonts.ready) document.fonts.ready.then(layout);
else window.addEventListener('load', layout);

const bar = document.querySelector('.bar');
if (bar) {
  const setBarHeight = () =>
    document.body.style.setProperty('--bar-h', bar.offsetHeight + 24 + 'px');
  setBarHeight();
  addEventListener('resize', setBarHeight);
}

const exp = document.getElementById('exp');
if (exp) {
  const msg = document.getElementById('expmsg');
  const all = document.getElementById('expall');
  const run = async (which, btn) => {
    const label = btn.textContent;
    [exp, all].forEach((b) => (b.disabled = true));
    btn.textContent = 'Exporting...';
    msg.className = '';
    msg.textContent = '';
    try {
      const r = await fetch('/export?locale=' + encodeURIComponent(which), { method: 'POST' });
      const j = await r.json();
      if (!j.ok) throw new Error(j.error);
      const warn = j.warnings.length
        ? ' (' + j.warnings.length + ' warning' + (j.warnings.length > 1 ? 's' : '') + ')'
        : '';
      msg.textContent =
        j.count + ' PNG' + (j.count > 1 ? 's' : '') + ' -> build/' + warn;
    } catch (e) {
      msg.className = 'err';
      msg.textContent = String(e.message || e);
    } finally {
      btn.textContent = label;
      [exp, all].forEach((b) => (b.disabled = false));
    }
  };
  exp.addEventListener('click', () => run(exp.dataset.locale, exp));
  all.addEventListener('click', () => run('all', all));
}

const zoom = document.getElementById('zoom');
if (zoom) {
  const apply = () => {
    const z = zoom.value / 100;
    document.getElementById('zv').textContent = zoom.value + '%';
    for (const stage of document.querySelectorAll('.stage')) {
      const screen = stage.querySelector('.screen');
      screen.style.transform = 'scale(' + z + ')';
      stage.style.width = screen.offsetWidth * z + 'px';
      stage.style.height = screen.offsetHeight * z + 'px';
    }
  };
  zoom.addEventListener('input', apply);
  if (document.fonts && document.fonts.ready) document.fonts.ready.then(apply);
  else window.addEventListener('load', apply);
}
</script>
</body>
</html>`;
}
