// Geometry lifted verbatim from the Figma blueprint
// (file gzweOdjwQnGs6vlvfDt0hl, page 13:2, the ENGLISH "Android Tablet Board" —
// nodes 18:79 / 18:82 / 18:85 / 18:88 / 18:91. The ru-RU export board 698:2 is
// used only for compression floors.) Do not round these to a grid.
//
// Google Play takes this 16:9 landscape set for both the 7-inch and 10-inch
// tablet slots, so one render serves both.

export const platform = {
  id: 'android_tablet',
  canvas: { w: 2560, h: 1440, radius: 32, bg: '#f9f6f0' },

  blobs: [
    { x: 1760, y: -140, size: 980, color: '#DFF1F4', opacity: 0.92 },
    { x: -54, y: 920, size: 280, color: '#E8F6EF', opacity: 1 },
  ],

  // Measured off the frozen ENGLISH board. `verify.mjs` checks the English
  // render against these; never fill them in from the renderer's own output.
  figmaReference: {
    '01_just_a_list': { headTop: 88.44, headSize: 116, subTop: 224.44, devTop: 367 },
    '02_read_your_way': { headTop: 91.22, headSize: 116, subTop: 227.22, devTop: 386.46 },
    '03_stay_organized': { headTop: 95.77, headSize: 116, subTop: 231.77, devTop: 389 },
    // 04 is a split: the mockup's top is Figma's own 340, which is exactly
    // (1440 - 760) / 2 — the canvas midline the renderer centres on.
    '04_sync_your_way': { headTop: 568.28, headSize: 116, subTop: 704.28, devTop: 340 },
    // devTop is the comparison frame's top: Figma's content top (487, the
    // chips) minus the frame's own 60px shadow pad.
    '05_make_the_list_yours': { headTop: 112, headSize: 116, subTop: 248, devTop: 427 },
  },

  // Figma's mockups drift progressively left of centre across screens 01-03
  // (23, 31 and 44px against a centred 505.2). All three are the same width, so
  // that reads as drift rather than intent; the renderer centres them.
  centreDevice: true,

  rhythm: {
    preferred: { top: 88.44, headlineToSub: 22.32, subToDevice: 59.28 },
    min: { top: 72, headlineToSub: 18, subToDevice: 40 },
    minBottom: 48,
    deviceMinScale: 0.8,
  },

  text: {
    left: 150,
    maxWidth: 2260,
    headline: { family: 'Poppins', weight: 700, lineHeight: 0.98, color: '#273437' },
    sub: { family: 'Poppins', weight: 400, size: 55, lineHeight: 'normal', color: '#565e71' },
  },

  screens: [
    {
      id: '01_just_a_list', key: 'android_tablet_01', kind: 'phone',
      headlineSize: 116, textWidth: 1040, subWidth: 1106.766,
      headlineToSub: 22.32, subToDevice: 59.24,
      device: {
        x: 481.97, w: 1549.589, h: 983.652, radius: 42.108, bg: '#141c1a',
        screen: { x: 37.06, y: 30.32, w: 1475.478, h: 923.016, radius: 20.212, bg: 'transparent' },
        notch: { x: 0, y: 0, w: 0, h: 0, color: 'transparent' },
        capture: '01_home.png',
      },
    },
    {
      id: '02_read_your_way', key: 'android_tablet_02', kind: 'phone',
      headlineSize: 116, textWidth: 1040, subWidth: 2133.965,
      top: 91.22, textLeft: 144.06, headlineToSub: 22.32, subToDevice: 75.92,
      device: {
        x: 473.8, w: 1549.589, h: 982.666, radius: 47.244, bg: '#141c1a',
        screen: { x: 36.85, y: 30.24, w: 1475.889, h: 922.195, radius: 20.787, bg: 'transparent' },
        notch: { x: 0, y: 0, w: 0, h: 0, color: 'transparent' },
        capture: '02_reader.png',
      },
    },
    {
      id: '03_stay_organized', key: 'android_tablet_03', kind: 'phone',
      headlineSize: 116, textWidth: 1040, subWidth: 2172.559,
      top: 95.77, headlineToSub: 22.32, subToDevice: 73.91,
      device: {
        x: 461.48, w: 1549.589, h: 983.117, radius: 51.311, bg: '#141c1a',
        screen: { x: 40.02, y: 31.81, w: 1469.544, h: 919.491, radius: 22.577, bg: '#ffffff' },
        notch: { x: 0, y: 0, w: 0, h: 0, color: 'transparent' },
        capture: '03_organization.png',
      },
    },
    {
      // Side-by-side: text column on the left at x 83, mockup on the right.
      // The mockup's Figma top (340) is exactly the canvas midline.
      id: '04_sync_your_way', key: 'android_tablet_04', kind: 'split',
      headlineSize: 116, textWidth: 1040, subWidth: 1060.921,
      top: 568.28, textLeft: 83, headlineToSub: 22.32,
      device: {
        x: 1277, w: 1200, h: 760, radius: 50, bg: '#141c1a',
        screen: { x: 34, y: 26, w: 1132, h: 708, radius: 18, bg: '#ffffff' },
        notch: { x: 0, y: 0, w: 0, h: 0, color: 'transparent' },
        capture: '04_accounts.png',
        captureInset: { x: 0, y: -28, w: 1360, h: 850 },
      },
    },
    {
      // Two separate cards with a 216px gap, so each chip centres on its own
      // card. Figma hand-placed its chips 17 and 28px left of their cards'
      // centres; centring is the documented departure every platform makes.
      id: '05_make_the_list_yours', key: 'android_tablet_05', kind: 'comparison',
      headlineSize: 116, textWidth: 1400, subWidth: 1498,
      top: 112, headlineToSub: 22.32, subToDevice: 95.68,
      comparison: {
        x: 370, w: 1820, h: 832, pad: 60,
        card: {
          w: 734, h: 620, y: 92, radius: 30,
          border: 'none',
          shadow: '0px 18px 36px 0px rgba(36,43,48,0.12)',
        },
        panels: [
          {
            x: 0, capture: '05_list_layout.png',
            img: { x: 0, y: -10, w: 1520, h: 950 },
            chip: { labelKey: 'compact_label' },
          },
          {
            // Slightly wider card and a heavier shadow than its neighbour;
            // both are Figma's values, reproduced rather than normalised.
            x: 950, w: 750, capture: '05_card_layout.png',
            img: { x: 0, y: -10, w: 1520, h: 950 },
            chip: {
              labelKey: 'rich_cards_label',
              style: { shadow: '0px 20px 42px 0px rgba(36,43,48,0.16)' },
            },
          },
        ],
        // Figma sets these chips in Inter Medium; rendered in Poppins Medium
        // like every other platform's chips, for the same reason as macOS and
        // iPad — no third font family for a 26px label.
        chip: {
          y: 0, h: 56, padX: 29.5, bg: '#dbf2eb', border: '2px solid #bad6cf',
          color: '#30474a', size: 26, weight: 500, radius: 28, shadow: 'none',
        },
      },
    },
  ],
};
