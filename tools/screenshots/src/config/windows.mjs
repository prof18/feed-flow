// Geometry lifted verbatim from the Figma blueprint
// (file gzweOdjwQnGs6vlvfDt0hl, "Windows Board", nodes 10:21 / 10:75 /
// 10:129 / 10:189).
//
// Windows is deliberately different from the mobile platforms: the Microsoft
// Store shows captions as listing metadata beside the image, so nothing is
// burned into the screenshot itself. With no on-image copy the output is
// identical in every language, hence `localised: false` — it renders once.

export const platform = {
  id: 'windows',
  localised: false,

  canvas: {
    w: 3840,
    h: 2160,
    bg: '#f7f4ec',
    border: '2px solid #e6e0d6',
    borderWidth: 2,
  },

  // Figma's capture x drifted ~1px off-centre on screen 03; centre them all,
  // as the phone boards do.
  centreDevice: true,

  blobs: [
    { x: 2590, y: -98, size: 1344, color: '#CFEFF3', opacity: 0.65 },
    { x: -122, y: 1486, size: 432, color: '#D9F1EF', opacity: 0.8 },
  ],

  screens: [
    {
      id: '01_three_pane_layout', kind: 'plain',
      shot: {
        x: 144.4, y: 113.2, w: 3547.555, h: 1929.6,
        shadow: '0px 16px 28px 0px rgba(0,0,0,0.08)',
        capture: '01_three_pane_layout.png',
      },
    },
    {
      id: '02_reader_mode', kind: 'plain',
      shot: {
        x: 672.4, y: 113.2, w: 2491.596, h: 1929.6,
        shadow: '0px 16px 28px 0px rgba(0,0,0,0.08)',
        capture: '02_reader_mode.png',
      },
    },
    {
      id: '03_sync_storage', kind: 'plain',
      shot: {
        x: 665.2, y: 113.2, w: 2506.768, h: 1929.6,
        shadow: '0px 16px 28px 0px rgba(0,0,0,0.08)',
        capture: '03_sync_storage.png',
      },
    },
    {
      id: '04_customize_the_list', kind: 'plain',
      shot: {
        x: 670, y: 113.2, w: 2495.551, h: 1929.6,
        shadow: '0px 16px 28px 0px rgba(0,0,0,0.08)',
        capture: '04_customize_the_list.png',
      },
    },
  ],
};
