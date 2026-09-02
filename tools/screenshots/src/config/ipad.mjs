// Geometry lifted verbatim from the Figma blueprint
// (file gzweOdjwQnGs6vlvfDt0hl, page 8:2, the ENGLISH "iPad Board" — nodes
// 18:39 / 18:42 / 218:3 / 18:45 / 18:48. The ru-RU export board 695:4 is used
// only for compression floors.) Do not round these to a grid.
//
// The board has FIVE screens, not the four the old Figma skill claimed, and no
// "01"-"04" page-number markers.

export const platform = {
  id: 'ipad',
  canvas: { w: 2732, h: 2048, radius: 32, bg: '#f9f6f0' },

  blobs: [
    { x: 1640, y: -140, size: 1160, color: '#DFF1F4', opacity: 0.92 },
    { x: -80, y: 1160, size: 320, color: '#E8F6EF', opacity: 1 },
  ],

  // Measured off the frozen ENGLISH board. `verify.mjs` checks the English
  // render against these; never fill them in from the renderer's own output.
  figmaReference: {
    '01_just_a_list': { headTop: 117.59, headSize: 127, subTop: 258.91, devTop: 416.74 },
    '02_read_your_way': { headTop: 117.59, headSize: 127, subTop: 257.91, devTop: 416.74 },
    '03_stay_organized': { headTop: 117.59, headSize: 127, subTop: 257.91, devTop: 416.74 },
    '04_sync_your_way': { headTop: 117.59, headSize: 127, subTop: 257.91, devTop: 416.74 },
    // devTop is the comparison frame's top: Figma's content top (607, the
    // chips) minus the frame's own 40px shadow pad.
    '05_make_the_list_yours': { headTop: 132, headSize: 127, subTop: 296, devTop: 567 },
  },

  // Figma already centres every mockup here to within 0.005px, so this changes
  // nothing on the English board — it just keeps long translations centred.
  centreDevice: true,

  rhythm: {
    preferred: { top: 117.59, headlineToSub: 15.86, subToDevice: 76.29 },
    min: { top: 88, headlineToSub: 14, subToDevice: 50 },
    minBottom: 48,
    deviceMinScale: 0.8,
  },

  text: {
    left: 150,
    maxWidth: 2432,
    headline: { family: 'Poppins', weight: 700, lineHeight: 0.98, color: '#273437' },
    sub: { family: 'Poppins', weight: 400, size: 55, lineHeight: 'normal', color: '#565e71' },
  },

  screens: [
    {
      // Screens 01-04 all use one flat capture with the device shell already
      // rendered into the PNG — no bezel, screen inset or notch node tree — so
      // the device is transparent, square-cornered and fully filled.
      id: '01_just_a_list', key: 'ipad_01', kind: 'phone',
      headlineSize: 127, textWidth: 1120, subWidth: 1521.366,
      headlineToSub: 16.86, subToDevice: 75.29,
      device: {
        x: 273.91, w: 2184.189, h: 1555.407, radius: 0, bg: 'transparent',
        screen: { x: 0, y: 0, w: 2184.189, h: 1555.407, radius: 0, bg: 'transparent' },
        notch: { x: 0, y: 0, w: 0, h: 0, color: 'transparent' },
        capture: '01_home.png',
      },
    },
    {
      id: '02_read_your_way', key: 'ipad_02', kind: 'phone',
      headlineSize: 127, textWidth: 1120, subWidth: 2163.421,
      headlineToSub: 15.86, subToDevice: 76.29,
      device: {
        x: 273.91, w: 2184.19, h: 1555.408, radius: 0, bg: 'transparent',
        screen: { x: 0, y: 0, w: 2184.19, h: 1555.408, radius: 0, bg: 'transparent' },
        notch: { x: 0, y: 0, w: 0, h: 0, color: 'transparent' },
        capture: '02_reader.png',
      },
    },
    {
      id: '03_stay_organized', key: 'ipad_03', kind: 'phone',
      headlineSize: 127, textWidth: 1120, subWidth: 2163.421,
      headlineToSub: 15.86, subToDevice: 76.29,
      device: {
        x: 273.91, w: 2184.19, h: 1555.408, radius: 0, bg: 'transparent',
        screen: { x: 0, y: 0, w: 2184.19, h: 1555.408, radius: 0, bg: 'transparent' },
        notch: { x: 0, y: 0, w: 0, h: 0, color: 'transparent' },
        capture: '03_organization.png',
      },
    },
    {
      id: '04_sync_your_way', key: 'ipad_04', kind: 'phone',
      headlineSize: 127, textWidth: 1120, subWidth: 2163.421,
      headlineToSub: 15.86, subToDevice: 76.29,
      device: {
        x: 273.91, w: 2184.19, h: 1555.408, radius: 0, bg: 'transparent',
        screen: { x: 0, y: 0, w: 2184.19, h: 1555.408, radius: 0, bg: 'transparent' },
        notch: { x: 0, y: 0, w: 0, h: 0, color: 'transparent' },
        capture: '04_accounts.png',
      },
    },
    {
      // Two separate cards with a 242px gap, so each chip centres on its own
      // card. Figma's hand-placed chip centres (841 and 1891) are reproduced
      // exactly by that rule.
      id: '05_make_the_list_yours', key: 'ipad_05', kind: 'comparison',
      headlineSize: 127, textWidth: 1500, subWidth: 1633,
      top: 132, headlineToSub: 39.54, subToDevice: 188.46,
      comparison: {
        x: 397, w: 1938, h: 1321, pad: 40,
        card: {
          w: 808, h: 1184, y: 57, radius: 26,
          border: '1px solid #d9e3ea',
          shadow: '0px 16px 28px 0px rgba(0,0,0,0.08)',
        },
        panels: [
          {
            x: 0, y: 57, capture: '05_list_layout.png',
            img: { x: -1, y: -38.88, w: 1772.2, h: 1221.5 },
            chip: { labelKey: 'compact_label' },
          },
          {
            // Figma sets this card 3px lower than its neighbour and gives its
            // chip a different fill, border and size. Both are reproduced
            // rather than normalised — they look like drift, but that is the
            // designer's call, not the renderer's.
            x: 1050, y: 60, capture: '05_card_layout.png',
            img: { x: -1, y: -38.96, w: 1776, h: 1224.1 },
            chip: {
              labelKey: 'rich_cards_label',
              style: {
                h: 42, bg: '#c7edf5', border: '1.5px solid #9cd6e0',
                color: '#3d616b', size: 19,
              },
            },
          },
        ],
        // Figma sets these chips in Inter Semi Bold, which no other board uses
        // and which is not vendored. Rendered in Poppins Medium like every
        // other platform's chips; at 18px on a 2732px canvas the difference is
        // invisible and it avoids a third font family.
        chip: {
          y: 0, h: 38, padX: 49, bg: '#ddf3f4', border: '1px solid #c2e6e9',
          color: '#47606b', size: 18, weight: 500, radius: 999, shadow: 'none',
        },
      },
    },
  ],
};
