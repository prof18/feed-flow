// iPhone Duo, inner display (open). App Store Connect display type
// APP_IPHONE_DUO accepts 2007x2853 for the inner display. The board is shared
// with the outer display and lives in `../duo.mjs`.
import { duoPlatform } from '../duo.mjs';

export const platform = duoPlatform({ id: 'iphone_duo', canvas: { w: 2007, h: 2853 } });
