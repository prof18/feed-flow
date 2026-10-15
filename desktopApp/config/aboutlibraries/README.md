# Bundled desktop license texts

AboutLibraries reads the definitions in `licenses/` through the existing desktop `configPath`. The standard SPDX texts are local so network-disabled Flatpak builds retain the complete notice body. This preserves the library-to-license IDs; it does not replace dependency metadata or library-specific license selection.

The five standard texts come from the official [SPDX license data revision](https://github.com/spdx/license-list-data/tree/31ba1a50e5397e00a304dbadc76531740e89ee48/json/details). Each JSON definition copies the source `licenseText` exactly, with its SPDX ID used for `hash`, `internalHash` and `spdxId`. Their SHA-256 values are computed over the UTF-8 text:

| SPDX ID | Text SHA-256 |
| --- | --- |
| Apache-2.0 | `074e6e32c86a4c0ef8b3ed25b721ca23aca83df277cd88106ef7177c354615ff` |
| BSD-2-Clause | `f32fb3b417a194167cfad068223fc975ba96c5960513a10f66a3c28720aec1df` |
| BSD-3-Clause | `5a93d5831e1297ab10fe643e1a631e83be392896da14ee2951285a79012df69d` |
| LGPL-2.1-or-later | `5749785c8bdefafcb5d798270ed0a967036fe2ca63dcedade1627565dfef81d2` |
| MIT | `b05785f9f18e6716bab63424b11454513b9943a222595b70411009202fc592b5` |

The IBM Plex OFL definition retains the actual distributed font license, including its copyright and reserved font name. Its source/provenance is recorded with the bundled font.

`DesktopLicenseResourcesTest` checks the generated report has a nonempty body for each license definition. The offline regression invokes the pinned exporter with its public `offlineMode` enabled before running that test; a full Flatpak check also disables the build sandbox network. Keep raw export/artifact comparisons in the local desktop evidence directory.
