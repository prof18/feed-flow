// Geometry lifted verbatim from the Figma blueprint
// (file gzweOdjwQnGs6vlvfDt0hl, "Android Phone Board (ru-RU Export)").
// Do not round these to a grid — they are the design's real values.

export const platform = {
  id: 'android_phone',
  canvas: { w: 1080, h: 2400, radius: 32, bg: '#f9f6f0' },

  // Both decorative circles are identical on every screen in this platform.
  blobs: [
    { x: 480, y: -120, size: 860, color: '#DFF1F4', opacity: 0.92 },
    { x: -40, y: 980, size: 260, color: '#E8F6EF', opacity: 1 },
  ],

  // Two rhythms, both taken from Figma. `preferred` is the English board's
  // generous spacing; `min` is what the ru-RU export compressed to in order to
  // fit two-line Cyrillic headlines. Short copy gets `preferred`; longer
  // translations are squeezed toward `min` before anything else gives.
  // Measured off the frozen Figma ENGLISH board (nodes 18:3 / 18:6 / 18:9 /
  // 18:12 / 18:15). `verify.mjs` checks the English render against these.
  // Never take these from a localised export board — those carry compressed
  // spacing tuned for one language.
  figmaReference: {
    '01_just_a_list': { headTop: 112, headSize: 112, subTop: 244, devTop: 508 },
    '02_read_your_way': { headTop: 112, headSize: 108, subTop: 244, devTop: 598 },
    '03_stay_organized': { headTop: 112, headSize: 108, subTop: 249, devTop: 588 },
    '04_sync_your_way': { headTop: 112, headSize: 108, subTop: 244, devTop: 508.8 },
    '05_make_the_list_yours': { headTop: 112, headSize: 88, subTop: 241, devTop: 514 },
  },

  centreDevice: true,

  rhythm: {
    preferred: { top: 112, headlineToSub: 24, subToDevice: 88 },
    // Floors, only reached after the device has already shrunk as far as it
    // may. The top margin is deliberately protected: breathing room above the
    // headline reads worse when lost than a slightly smaller phone does.
    min: { top: 88, headlineToSub: 20, subToDevice: 60 },
    minBottom: 48,
    deviceMinScale: 0.8,
  },

  // Text boxes are capped so the right margin can never be tighter than the
  // left. Figma had screen 05's headline at 980 wide -- 8px from the edge.
  text: {
    left: 92,
    maxWidth: 896,
    headline: { family: 'Montserrat', weight: 700, lineHeight: 0.98, color: '#273437' },
    sub: { family: 'Montserrat', weight: 400, size: 55, lineHeight: 'normal', color: '#565e71' },
  },

  screens: [
    {
      id: '01_just_a_list', key: 'android_phone_01', kind: 'phone',
      headlineSize: 112, textWidth: 860, subWidth: 834.016,
      headlineToSub: 22, subToDevice: 98,
      device: {
        x: 131.17, w: 817.669, h: 1812.182, radius: 72.259, bg: '#111a18',
        screen: { x: 30.42, y: 53.24, w: 764.425, h: 1720.907, radius: 53.244, bg: '#fbfcfa' },
        notch: { x: 273.82, y: 17.11, w: 155.928, h: 20.917, color: '#202b29' },
        capture: '01_home.png',
      },
    },
    {
      id: '02_read_your_way', key: 'android_phone_02', kind: 'phone',
      headlineSize: 108, textWidth: 900, subWidth: 835.735,
      headlineToSub: 26, subToDevice: 105,
      device: {
        x: 152.27, w: 775.47, h: 1719.437, radius: 72.76, bg: '#111a18',
        screen: { x: 29.68, y: 50.74, w: 724.729, h: 1626.572, radius: 53.613, bg: '#fbfcfa' },
        notch: { x: 310.19, y: 19.15, w: 176.156, h: 23.934, color: '#202b29' },
        capture: '02_reader.png',
      },
    },
    {
      id: '03_stay_organized', key: 'android_phone_03', kind: 'phone',
      headlineSize: 108, textWidth: 900, subWidth: 900,
      headlineToSub: 31, subToDevice: 90,
      device: {
        x: 153.37, w: 768.924, h: 1704.923, radius: 60.754, bg: '#111a18',
        screen: { x: 29.43, y: 50.31, w: 718.612, h: 1612.842, radius: 43.667, bg: '#fbfcfa' },
        notch: { x: 307.57, y: 18.99, w: 174.669, h: 23.732, color: '#202b29' },
        capture: '03_organization.png',
        captureInset: { x: -3.8, y: -0.95, w: 726.206, h: 1613.791 },
      },
    },
    {
      id: '04_sync_your_way', key: 'android_phone_04', kind: 'phone',
      headlineSize: 108, textWidth: 900, subWidth: 840,
      headlineToSub: 26, subToDevice: 98.8,
      device: {
        x: 137, w: 810, h: 1796, radius: 64, bg: '#111a18',
        screen: { x: 31, y: 53, w: 757, h: 1699, radius: 46, bg: '#fbfcfa' },
        notch: { x: 324, y: 20, w: 184, h: 25, color: '#202b29' },
        capture: '04_accounts.png',
        captureInset: { x: -10, y: -2, w: 776, h: 1724 },
      },
    },
    {
      id: '05_make_the_list_yours', key: 'android_phone_05', kind: 'comparison',
      headlineSize: 88, textWidth: 980, subWidth: 861.217,
      headlineToSub: 43, subToDevice: 107,
      comparison: {
        x: 42.61, w: 960, h: 1588, pad: 12,
        card: {
          w: 637.2, h: 1425.6, y: 118, radius: 30.24,
          border: '1.08px solid #e8edf2',
          shadow: '0px 21.6px 38.88px 0px rgba(20,18,8,0.12)',
        },
        // Both captures are drawn at 2/3 of their native 1080x2400 and
        // nudged so the same list rows line up across the two cards.
        panels: [
          { x: 18, capture: '05_list_layout.png', img: { x: -2.06, y: -39.76, w: 720, h: 1600 },
            chip: { labelKey: 'compact_label' } },
          { x: 369, capture: '05_card_layout.png', img: { x: 2.63, y: -41.04, w: 677.28, h: 1505.13 },
            chip: { labelKey: 'rich_cards_label' } },
        ],
        // Chips size themselves to their label and centre on the card they
        // annotate, so a long translation widens the pill instead of being
        // clipped by a hand-measured width.
        chip: {
          y: 84, h: 55.12, padX: 23.32, bg: '#e0f2eb', border: '1.06px solid #abcfbd',
          color: '#303b40', size: 23.32, weight: 500, radius: 999,
          shadow: '0px 8.48px 16.96px 0px rgba(20,18,8,0.08)',
        },
      },
    },
  ],

  featureGraphic: {
    id: 'feature_graphic', key: 'android_phone_feature_graphic',
    w: 1024, h: 500, bg: '#fbfcfa',
    blobs: [
      { x: 676, y: -42, size: 360, color: '#DFF1F4', opacity: 0.92 },
      { x: -26, y: 354, size: 112, color: '#E8F6EF', opacity: 1 },
    ],
    // Both Figma boards centre this block vertically (English centre 244.3,
    // ru-RU 245, against a canvas centre of 250), so it is centred rather than
    // pinned — with a floor on the padding above and below.
    headline: {
      x: 81, w: 328, size: 68, lineHeight: 1.1, weight: 700, color: '#273437',
      // 40px, not the 20 the Figma board squeaked by with: six-line
      // languages (ru) otherwise fill the canvas edge to edge.
      minPadding: 40, minScale: 0.6,
    },
    phones: [
      { x: 706, y: 30, w: 190.92, h: 423.132, radius: 16.872, rotate: 0,
        shadow: '0px 18px 30px 0px rgba(17,26,24,0.16)',
        screen: { x: 7.1, y: 12.43, w: 178.488, h: 401.82, radius: 12.432 },
        notch: { x: 63.94, y: 4, w: 36.408, h: 4.884 },
        capture: 'fg_home.png' },
      { x: 541.13, y: 194, w: 123.541, h: 273.926, radius: 11.592, rotate: 5,
        shadow: '0px 13.02px 22.32px 0px rgba(17,26,24,0.14)',
        wrap: { w: 146.945, h: 283.651 },
        screen: { x: 4.73, y: 8.08, w: 115.458, h: 259.131, radius: 8.541 },
        notch: { x: 49.42, y: 3.05, w: 28.064, h: 3.813 },
        capture: 'fg_reader.png' },
    ],
  },
};
