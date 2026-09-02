// Geometry lifted verbatim from the Figma blueprint
// (file gzweOdjwQnGs6vlvfDt0hl, page 9:2, board "macOS Board" — the ENGLISH
// board, nodes 18:54 / 18:57. The ru-RU export board 704:2 is used only for the
// compression floors.) Do not round these to a grid — they are the design's
// real values.
//
// Only screens 01 and 02 are here. The board's other two screens need layout
// kinds `src/render/page.mjs` does not have, and shared code is off limits:
//
//   03 "Sync your way"  — side-by-side: the text column sits at x 113.55 with
//      its headline at y 724, and the mockup sits beside it at x 1093.55 /
//      y 218.44. Both halves are centred on the canvas' vertical midline
//      (text block 724→1076, mockup 218.44→1581.56, both centre 900). The
//      `phone` kind can only stack text above a horizontally-centred device,
//      and `text.left` plus `rhythm.preferred.top` are platform-wide, so
//      neither the column x nor the y could be expressed. Needs a `split` kind
//      with per-screen text x/width and a vertically-centred pair of columns.
//
//   04 "Make the list yours" — two NON-overlapping cards (734x1040 at x 516.5
//      and 727x1040 at x 1636.5) each with its own chip centred over it, and a
//      headline top of 150 rather than 99. `comparison` centres chip i at
//      (panel[i].x + panel[i+1].x) / 2, which is only the card's centre when
//      the cards overlap edge-to-edge as they do on Android Phone; here it
//      would put "Dense list" 193px to the right of its card. It also needs a
//      per-card width (734 vs 727) and a per-screen top margin.
//
// Captures for all four screens are in assets/captures/macos/ so neither gap
// needs another Figma round-trip.

export const platform = {
  id: 'macos',
  canvas: { w: 2880, h: 1800, radius: 32, bg: '#f9f6f0' },

  // Identical on every screen of this board, and the same two circles the
  // Android board uses, scaled to the wider canvas.
  blobs: [
    { x: 1980, y: -180, size: 1200, color: '#DFF1F4', opacity: 0.92 },
    { x: -60, y: 980, size: 300, color: '#E8F6EF', opacity: 1 },
  ],

  // Measured off the frozen Figma ENGLISH board (nodes 18:54 / 18:57).
  // `verify.mjs` checks the English render against these. Never take them from
  // a localised export board — those carry spacing tuned for one language.
  figmaReference: {
    '01_three_panes': { headTop: 99, headSize: 125, subTop: 259.1, devTop: 394.5 },
    '02_read_your_way': { headTop: 99, headSize: 125, subTop: 259.1, devTop: 364.76 },
    '03_sync_your_way': { headTop: 724, headSize: 125, subTop: 904, devTop: 218.44 },
    // devTop here is the comparison frame's top, which is Figma's content top
    // (541.72, the chips) minus the frame's own 40px shadow pad.
    '04_make_the_list_yours': { headTop: 150, headSize: 118, subTop: 310.86, devTop: 501.72 },
  },

  centreDevice: true,

  rhythm: {
    preferred: { top: 99, headlineToSub: 37.1, subToDevice: 52.4 },
    // Floors, from the ru-RU export board (704:3 / 704:10), which compressed
    // the top margin to 36/30, the headline gap to 24/18 and the mockup gap to
    // 90/40. A floor can never sit above what English already uses, and screen
    // 02's English mockup gap is only ~19, so subToDevice floors at 18.
    min: { top: 36, headlineToSub: 18, subToDevice: 18 },
    // Both mockups run to the bottom edge on this board — screen 01 leaves
    // 34.7px and screen 02 actually bleeds 1.1px off-canvas — so every render
    // takes a little relief from the first step of the ladder (device scale)
    // to buy back a legal bottom margin. 44 keeps the mockups as close to
    // full-bleed as the 40px floor allows.
    // Negative on purpose: these desktop mockups run to the very bottom edge
    // and screen 02 bleeds 1.1px off-canvas in Figma, which `overflow: hidden`
    // clips exactly as Figma does. A positive floor would shrink every mockup
    // by 1-3% to manufacture a margin the design does not want.
    minBottom: -4,
    deviceMinScale: 0.8,
  },

  // Capped so the right margin can never be tighter than the left.
  text: {
    left: 160,
    maxWidth: 2560,
    headline: { family: 'Poppins', weight: 700, lineHeight: 0.98, color: '#273437' },
    sub: { family: 'Poppins', weight: 400, size: 55, lineHeight: 'normal', color: '#565e71' },
  },

  // macOS mockups are flat images: the window chrome, its rounded corners and
  // its drop shadow are all baked into the capture, so there is no bezel,
  // no screen inset and no notch to draw. The device box is the image box —
  // which includes the capture's transparent shadow margin, and is what gets
  // centred (Figma's own x values are already dead centre to 3 decimals).
  screens: [
    {
      id: '01_three_panes', key: 'macos_01', kind: 'phone',
      headlineSize: 125, textWidth: 1180, subWidth: 1738.901,
      headlineToSub: 37.1, subToDevice: 52.4,
      device: {
        x: 227.974, w: 2424.053, h: 1370.821, radius: 0, bg: 'transparent',
        screen: { x: 0, y: 0, w: 2424.053, h: 1370.821, radius: 0, bg: 'transparent' },
        notch: { x: 0, y: 0, w: 0, h: 0, color: 'transparent' },
        capture: '01_three_panes.png',
      },
    },
    {
      id: '02_read_your_way', key: 'macos_02', kind: 'phone',
      headlineSize: 125, textWidth: 1180, subWidth: 1820,
      headlineToSub: 37.1, subToDevice: 22.66,
      device: {
        x: 471.085, w: 1937.83, h: 1436.367, radius: 0, bg: 'transparent',
        screen: { x: 0, y: 0, w: 1937.83, h: 1436.367, radius: 0, bg: 'transparent' },
        notch: { x: 0, y: 0, w: 0, h: 0, color: 'transparent' },
        capture: '02_reader.png',
      },
    },
    {
      // Side-by-side: the text column sits left, the mockup right. Both are
      // vertically centred in Figma; the mockup exactly (218.44 == (1800-h)/2),
      // the text not quite, so the text keeps its own top.
      id: '03_sync_your_way', key: 'macos_03', kind: 'split',
      headlineSize: 125, textWidth: 980, subWidth: 936.939,
      top: 724, textLeft: 113.55, headlineToSub: 57.5,
      device: {
        x: 1093.55, w: 1672.909, h: 1363.111, radius: 0, bg: 'transparent',
        screen: { x: 0, y: 0, w: 1672.909, h: 1363.111, radius: 0, bg: 'transparent' },
        notch: { x: 0, y: 0, w: 0, h: 0, color: 'transparent' },
        capture: '03_sync.png',
      },
    },
    {
      // Two cards side by side with a 386px gap — they do NOT overlap, so each
      // chip centres on its own card rather than on a shared seam.
      id: '04_make_the_list_yours', key: 'macos_04', kind: 'comparison',
      headlineSize: 118, textWidth: 1600, subWidth: 1400.131,
      top: 150, headlineToSub: 45.22, subToDevice: 107.5,
      comparison: {
        x: 476.5, w: 1927, h: 1180, pad: 40,
        card: {
          w: 734, h: 1040, y: 60, radius: 26,
          border: '1px solid #d9e3ea',
          shadow: '0px 16px 28px 0px rgba(0,0,0,0.08)',
        },
        panels: [
          { x: 0, w: 734, capture: '04_list_layout.png',
            img: { x: -1, y: -1, w: 1468.153, h: 1050 },
            chip: { labelKey: 'compact_label' } },
          { x: 1120, w: 727, capture: '04_card_layout.png',
            img: { x: -1, y: -1, w: 1454.171, h: 1040 },
            chip: { labelKey: 'rich_cards_label' } },
        ],
        // Figma sets these chips in Inter Semi Bold, which no other board in
        // the file uses and which is not vendored here. Rendered in Poppins
        // Medium like every other platform's chips: at 19px on a 2880px canvas
        // the difference is invisible, and it avoids a third font family.
        chip: {
          y: 0, h: 42, padX: 39, bg: '#ddf3f4', border: '1px solid #c2e6e9',
          color: '#47606b', size: 19, weight: 500, radius: 999, shadow: 'none',
        },
      },
    },
  ],
};
