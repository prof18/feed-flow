// Geometry lifted verbatim from the Figma blueprint
// (file gzweOdjwQnGs6vlvfDt0hl, page 6:2, board "iPhone Board" — the ENGLISH
// board, nodes 18:21 / 18:24 / 18:27 / 18:30 / 18:33). Never take these from
// "iPhone Board (ru-RU Export)": that board carries spacing compressed to fit
// Cyrillic and would give every language Russian's cramped margins.
// Do not round these to a grid — they are the design's real values.

export const platform = {
  id: 'iphone',
  // 1284x2778 is the 6.5"/6.7" App Store slot.
  canvas: { w: 1284, h: 2778, radius: 32, bg: '#fcfaf4' },

  // Both decorative circles are identical on every screen in this platform.
  // Figma draws them as ellipses a fraction wider than tall (975.442x973.691
  // and 298.605x298.069); the ~1.7px / ~0.5px difference is not visible, so
  // they are circles here, sized on the Figma width.
  blobs: [
    { x: 557.4, y: -139.1, size: 975.442, color: '#DFF1F4', opacity: 0.92 },
    { x: -43.8, y: 1132.66, size: 298.605, color: '#E8F6EF', opacity: 1 },
  ],

  // Measured off the frozen Figma ENGLISH board. `verify.mjs` checks the
  // English render against these (±1px).
  //
  // One deliberate departure, on screen 05 only. Figma starts that screen's
  // headline at y=186.53 while 01–04 all start at 131.15, and the renderer's
  // top margin is a single platform constant with no per-screen override. The
  // whole 05 text block is therefore lifted to the shared 131.15, keeping
  // Figma's headline→sub gap (44.87) intact and absorbing the 55.4px into the
  // gap below the subheadline, so the comparison cards still land on Figma's y.
  // Nothing else on the board moves.
  figmaReference: {
    '01_just_a_list': { headTop: 131.15, headSize: 116, subTop: 283.45, devTop: 551.44 },
    '02_read_your_way': { headTop: 131.15, headSize: 116, subTop: 286.75, devTop: 563.99 },
    '03_stay_organized': { headTop: 131.15, headSize: 116, subTop: 286.75, devTop: 596.68 },
    '04_sync_your_way': { headTop: 131.15, headSize: 116, subTop: 282.65, devTop: 537.11 },
    // Screen 05 sits lower than 01-04 in Figma (186.53 vs 131.15) and uses a
    // different text left edge; both are per-screen overrides on the screen.
    // devTop is the frame top, which for a comparison screen is Figma's
    // content top (679.08) minus the frame's own 40px shadow pad.
    '05_make_the_list_yours': { headTop: 186.53, headSize: 90, subTop: 319.6, devTop: 639.08 },
  },

  // Figma centred 01, 02 and 04 to the pixel but left 03 17.4px off-centre, so
  // the renderer centres all of them and the verifier checks against centred.
  centreDevice: true,

  rhythm: {
    preferred: { top: 131.15, headlineToSub: 40, subToDevice: 100 },
    // Floors, only reached after the device has already shrunk as far as it
    // may. The top margin is deliberately protected: breathing room above the
    // headline reads worse when lost than a slightly smaller phone does.
    min: { top: 104, headlineToSub: 26, subToDevice: 60 },
    minBottom: 48,
    deviceMinScale: 0.8,
  },

  // Text boxes are capped so the right margin can never be tighter than the
  // left. Figma runs both text left edges at 109.49 on 01–04 and 105.51 on 05;
  // the renderer has one left per platform, so 109.49 wins.
  text: {
    left: 109.49,
    maxWidth: 1065.02,
    headline: { family: 'Poppins', weight: 700, lineHeight: 0.98, color: '#273437' },
    sub: { family: 'Poppins', weight: 400, size: 55, lineHeight: 'normal', color: '#565e71' },
  },

  // Unlike Android Phone, the iPhone board does not build the handset out of a
  // bezel + screen + notch: each screen holds one flattened "capture framed"
  // PNG with the device shell already rendered into it. The device is
  // therefore a transparent, square-cornered box whose whole area is the
  // capture — hence radius 0, no background, and a zero-sized notch.
  screens: [
    {
      id: '01_just_a_list', key: 'iphone_01', kind: 'phone',
      headlineSize: 116, textWidth: 980, subWidth: 1030.186,
      headlineToSub: 38.3, subToDevice: 101.99,
      device: {
        x: 127.12, w: 1029.767, h: 2101.067, radius: 0, bg: 'transparent',
        screen: { x: 0, y: 0, w: 1029.767, h: 2101.067, radius: 0, bg: 'transparent' },
        notch: { x: 0, y: 0, w: 0, h: 0, color: 'transparent' },
        capture: '01_home.png',
      },
    },
    {
      id: '02_read_your_way', key: 'iphone_02', kind: 'phone',
      headlineSize: 116, textWidth: 980, subWidth: 1030.186,
      headlineToSub: 41.6, subToDevice: 111.24,
      device: {
        x: 137.21, w: 1009.584, h: 2059.886, radius: 0, bg: 'transparent',
        screen: { x: 0, y: 0, w: 1009.584, h: 2059.886, radius: 0, bg: 'transparent' },
        notch: { x: 0, y: 0, w: 0, h: 0, color: 'transparent' },
        capture: '02_reader.png',
      },
    },
    {
      id: '03_stay_organized', key: 'iphone_03', kind: 'phone',
      headlineSize: 116, textWidth: 980, subWidth: 1030.186,
      headlineToSub: 41.6, subToDevice: 143.93,
      device: {
        x: 126.91, w: 995.349, h: 2030.841, radius: 0, bg: 'transparent',
        screen: { x: 0, y: 0, w: 995.349, h: 2030.841, radius: 0, bg: 'transparent' },
        notch: { x: 0, y: 0, w: 0, h: 0, color: 'transparent' },
        capture: '03_organization.png',
      },
    },
    {
      id: '04_sync_your_way', key: 'iphone_04', kind: 'phone',
      headlineSize: 116, textWidth: 980, subWidth: 1030.186,
      headlineToSub: 37.5, subToDevice: 88.46,
      device: {
        x: 126.91, w: 1030.186, h: 2101.921, radius: 0, bg: 'transparent',
        screen: { x: 0, y: 0, w: 1030.186, h: 2101.921, radius: 0, bg: 'transparent' },
        notch: { x: 0, y: 0, w: 0, h: 0, color: 'transparent' },
        capture: '04_accounts.png',
      },
    },
    {
      id: '05_make_the_list_yours', key: 'iphone_05', kind: 'comparison',
      headlineSize: 90, textWidth: 980, subWidth: 1005.654,
      top: 186.53, textLeft: 105.51,
      headlineToSub: 44.87, subToDevice: 153.68,
      // Figma leaves the two panels loose in a group rather than in a frame, so
      // the 40px pad here is not a Figma value: it is room for the cards' own
      // 0/10/22 shadow, which the renderer's frame would otherwise clip. The
      // frame top is therefore 40 above Figma's content top of 679.08.
      comparison: {
        x: 28.6815, w: 1226.637, h: 1882.323, pad: 40,
        // Figma gives the two panels different widths (compact 517.581, rich
        // 676.837) but the renderer has one card width. The rich card is drawn
        // second and is opaque, and it starts 469.8 into the compact card, so
        // widening the compact card to the rich card's 676.837 changes nothing
        // that is visible — its overhang lands entirely underneath.
        card: {
          w: 676.837, h: 1704.953, y: 97.37, radius: 32,
          border: 'none',
          shadow: '0px 10px 22px 0px rgba(17,26,24,0.1)',
        },
        // Both captures are 1206x2622 natively and are drawn oversize and
        // offset up, so the same list rows line up across the two cards.
        panels: [
          { x: 0, capture: '05_list_layout.png', img: { x: -8, y: -122.106, w: 874.43, h: 1901.177 },
            chip: { labelKey: 'compact_label' } },
          { x: 469.8, capture: '05_card_layout.png', img: { x: -1.958, y: -123.008, w: 853.28, h: 1855.113 },
            chip: { labelKey: 'rich_cards_label' } },
        ],
        // Chips size themselves to their label and centre on the visible part
        // of the card they annotate, so a long translation widens the pill
        // instead of being clipped. Figma hand-measured both widths (218.977 /
        // 203.051) and centred neither.
        chip: {
          y: 0, h: 55.639, padX: 37, bg: '#e8f6ef', border: '2px solid #c3e0d5',
          color: '#3d5d53', size: 22, weight: 400, radius: 999,
          shadow: 'none',
        },
      },
    },
  ],
};
