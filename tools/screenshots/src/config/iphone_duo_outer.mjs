// iPhone Duo, outer display (closed). App Store Connect display type
// APP_IPHONE_DUO accepts 1398x2034 for the outer display. Same board as the
// inner display, scaled; see `../duo.mjs`.
import { duoPlatform } from '../duo.mjs';

export const platform = duoPlatform({ id: 'iphone_duo_outer', canvas: { w: 1398, h: 2034 } });
