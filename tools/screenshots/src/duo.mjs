// iPhone Duo boards. There is no Figma board for the Duo: the composition
// reuses the iPhone board's copy, background, blobs and type, re-laid for the
// Duo's squarer canvases and its three poses (closed, open landscape, open
// rotated to portrait).
//
// Geometry is authored once in INNER-display pixels (2007 wide) and scaled to
// each canvas, so the outer-display set (1398 wide) is the same board. Captures
// come from the iOS 27.1 iPhone Duo simulator and are framed with the `frames`
// CLI (Night Sky):
//   closed          -> "iPhone Duo Outer Portrait"  1574x2194
//   open            -> "iPhone Duo Inner Landscape" 3093x2247
//   open, rotated   -> "iPhone Duo Inner Portrait"  2247x3093
// Screen 05's cards use the raw (unframed) outer-display captures, 1398x2034.
// Both platforms read `assets/captures/iphone_duo/`.

const AUTHORED_W = 2007;
const FRONT_SHADOW = '0px 30px 56px rgba(17,26,24,0.24)';

export function duoPlatform({ id, canvas }) {
  const k = canvas.w / AUTHORED_W;
  const s = (v) => Math.round(v * k * 100) / 100;
  const layer = (l) => ({ ...l, x: s(l.x), y: s(l.y), w: s(l.w), h: s(l.h) });
  const screen = ({ layers, comparison, ...sc }) => ({
    ...sc,
    headlineSize: s(sc.headlineSize ?? 181),
    textWidth: s(1665),
    subWidth: s(1665),
    headlineToSub: s(sc.headlineToSub ?? 60),
    subToDevice: s(sc.subToDevice ?? 150),
    ...(sc.minBottom !== undefined && { minBottom: s(sc.minBottom) }),
    ...(layers && { composite: { images: layers.map(layer) } }),
    ...(comparison && { comparison: scaleComparison(comparison, s) }),
  });

  return {
    id,
    canvas: { ...canvas, radius: 0, bg: '#fcfaf4' },
    assetDir: 'iphone_duo',
    blobs: [
      { x: s(871), y: s(-217), size: s(1525), color: '#DFF1F4', opacity: 0.92 },
      { x: s(-68), y: s(1770), size: s(467), color: '#E8F6EF', opacity: 1 },
    ],
    centreDevice: true,

    rhythm: {
      preferred: { top: s(205), headlineToSub: s(60), subToDevice: s(150) },
      min: { top: s(160), headlineToSub: s(40), subToDevice: s(90) },
      minBottom: s(90),
      deviceMinScale: 0.75,
    },

    text: {
      left: s(171),
      maxWidth: s(1665),
      headline: { family: 'Poppins', weight: 700, lineHeight: 0.98, color: '#273437' },
      sub: { family: 'Poppins', weight: 400, size: s(86), lineHeight: 'normal', color: '#565e71' },
    },

    screens: [
      // The same timeline open (sidebar + list) and closed, in front.
      screen({
        id: '01_just_a_list', key: 'iphone_01', kind: 'composite',
        layers: [
          { x: 0, y: 0, w: 1680, h: 1220, capture: '01_timeline_open.png' },
          { x: 960, y: 760, w: 840, h: 1171, capture: '01_timeline.png', shadow: FRONT_SHADOW },
        ],
      }),
      // Open and rotated to portrait: the reader fills the inner display.
      screen({
        id: '02_read_your_way', key: 'iphone_02', kind: 'composite',
        layers: [{ x: 0, y: 0, w: 1440, h: 1982, capture: '02_reader.png' }],
      }),
      // Open landscape (sidebar + reader) behind, the closed sidebar in front.
      screen({
        id: '03_stay_organized', key: 'iphone_03', kind: 'composite',
        layers: [
          { x: 120, y: 0, w: 1680, h: 1220, capture: '03_sidebar_reader_open.png' },
          { x: 0, y: 760, w: 840, h: 1171, capture: '03_sidebar.png', shadow: FRONT_SHADOW },
        ],
      }),
      // Closed, running past the bottom edge.
      screen({
        id: '04_sync_your_way', key: 'iphone_04', kind: 'composite',
        minBottom: -520,
        layers: [{ x: 0, y: 0, w: 1440, h: 2007, capture: '04_accounts.png' }],
      }),
      // Raw outer-display captures as overlapping cards, as on the iPhone.
      // The captures carry the Duo's side rail (status + toolbar) on the right;
      // each card is sized to show the list column, so the rail falls outside.
      screen({
        id: '05_make_the_list_yours', key: 'iphone_05', kind: 'comparison',
        headlineSize: 160,
        comparison: {
          w: 1800, h: 1960, pad: 50,
          card: {
            w: 1000, h: 1780, y: 130, radius: 48, border: 'none',
            shadow: '0px 14px 34px 0px rgba(17,26,24,0.1)',
          },
          panels: [
            { x: 0, capture: '05_list_layout.png', img: { x: -40, y: -60, w: 1290, h: 1877 },
              chip: { labelKey: 'compact_label' } },
            { x: 700, capture: '05_card_layout.png', img: { x: -40, y: -60, w: 1290, h: 1877 },
              chip: { labelKey: 'rich_cards_label' } },
          ],
          chip: {
            y: 0, h: 87, padX: 58, bg: '#e8f6ef', border: '3px solid #c3e0d5',
            color: '#3d5d53', size: 34, weight: 400, radius: 999, shadow: 'none',
          },
        },
      }),
    ],
  };
}

function scaleComparison(c, s) {
  const img = (i) => ({ x: s(i.x), y: s(i.y), w: s(i.w), h: s(i.h) });
  return {
    x: 0,
    w: s(c.w), h: s(c.h), pad: s(c.pad),
    card: { ...c.card, w: s(c.card.w), h: s(c.card.h), y: s(c.card.y), radius: s(c.card.radius) },
    panels: c.panels.map((p) => ({ ...p, x: s(p.x), img: img(p.img) })),
    chip: { ...c.chip, y: s(c.chip.y), h: s(c.chip.h), padX: s(c.chip.padX), size: s(c.chip.size) },
  };
}
