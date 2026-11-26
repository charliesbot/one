---
name: ONE
colors:
  primary: "#B4653A"
typography:
  display-medium: { fontFamily: Fraunces }
  display-small: { fontFamily: Fraunces }
  headline-medium: { fontFamily: Fraunces }
material:
  scheme: { seed: "{colors.primary}", variant: tonal-spot, contrast: 0 }
---

# ONE design system

**Color source: dynamic.** The app generates its scheme from the wallpaper
(`dynamicLightColorScheme` / `dynamicDarkColorScheme`; minSdk 31 always has
it). The seed above only renders previews and design boards, so screens must
never depend on a particular hue: color strictly through role pairs
(`primary`/`onPrimary`, `primaryContainer`/`onPrimaryContainer`, …), no hex
literals and no theme-specific branching.

**Palette style:** tonal-spot.

**Brand font:** Fraunces for the Today hero (dial countdown, screen title on the
band). Everything else stays on Google Sans Flex.

**Approved direction:** Sunrise dial for Today
(`design/today.explore.kt`, direction "Sunrise dial"). Token map for the hero:

- Band: `primaryContainer` / `onPrimaryContainer`, 48dp bottom corners on
  compact and medium, fully rounded panel at 840dp+.
- Sun (`MaterialShapes.Sunny`): `primary` with `onPrimary` text.
- Progress wave: `onPrimaryContainer`; track: `surfaceBright`.
- Week: Sunny shapes; goal met `primary`, today `primaryContainer`, partial
  `secondaryContainer`, empty `surfaceContainerHigh`.

**Adaptation:** single column under the band with a navigation rail from 600dp,
two panes (band beside details and recent fasts) from 840dp.
