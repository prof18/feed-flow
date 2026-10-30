# Reader fonts

Bundled OFL fonts used when the user picks a non-system reader typeface. The files live in
`shared/src/commonMain/resources/reader-fonts/`.

| Family | Files | License |
|--------|-------|---------|
| Literata | Literata-VF.ttf, Literata-Italic-VF.ttf | [OFL-literata.txt](OFL-literata.txt) |
| Source Serif 4 | SourceSerif4-VF.ttf, SourceSerif4-Italic-VF.ttf | [OFL-sourceserif4.txt](OFL-sourceserif4.txt) |
| IBM Plex Sans | IBMPlexSans-VF.ttf, IBMPlexSans-Italic-VF.ttf | [OFL-ibmplexsans.txt](OFL-ibmplexsans.txt) |
| Atkinson Hyperlegible Next | AtkinsonHyperlegibleNext-VF.ttf, AtkinsonHyperlegibleNext-Italic-VF.ttf | [OFL-atkinsonhyperlegiblenext.txt](OFL-atkinsonhyperlegiblenext.txt) |

Sources: [google/fonts](https://github.com/google/fonts) OFL collection (variable upright and italic files).

The apps show these licenses in their open source licenses screens. Keep the copies in sync when a font
changes: `androidApp/config/aboutlibraries/` and `desktopApp/config/aboutlibraries/` (AboutLibraries
definitions), and the `manual` entries in `iosApp/license_plist.yml`, which `iosApp/.scripts/licenses-notice-generator.sh`
renders into `iosApp/Assets/licenses.html`.
