# Builtin icons — sources and attribution

This folder holds the source-of-truth `.svg` files for every feature glyph that's built directly into the
renderer with **no `import` statement needed** (e.g. `torch:`, `chest: north`, `stairs: up west`) — unlike
`RoomFeature.Icon`'s general case, none of these hit the network at render time.

Two groups, by origin:

- **19 furnishing icons** (`anvil.svg` … `wooden-crate.svg` below) — externally sourced from
  [game-icons.net](https://game-icons.net) via Iconify, rendered through
  `RoomFeature.Icon("builtin", <name>, ...)`, the same code path as any imported Iconify icon.
- **5 movement glyphs** (`stairs-up.svg`, `stairs-down.svg`, `ladder.svg`, `spiral-stairs-up.svg`,
  `spiral-stairs-down.svg`) — original text-maps artwork, not sourced from anywhere (though the spiral
  pair takes visual inspiration from the pie-wedge-circle motif in Iconify's
  `memory:table-top-spiral-stairs-round-up`/`-down`, redrawn from scratch — see the design note below).
  These back `RoomFeature.Stairs`/`SpiralStairs`/`Ladder`, a separate case from `Icon` (they carry a
  direction and, for `Stairs`, a facing wall, not just a size/position).

These `.svg` files are the **only** place any of this artwork is defined — there is no hand-maintained
Scala source containing image data anywhere in the repo. `textmaps.icons.BuiltinIcons` (the `paths`/
`movementGlyphs` maps `SvgStringRenderer.defs()` reads to emit `<symbol id="builtin-...">`/
`<symbol id="feat-...">`s) is generated fresh from this folder on every `sbt compile`, by
`project/BuiltinIconsGen.scala` (an `sbt Compile / sourceGenerators` task wired up in `build.sbt`'s `core`
cross-project) — the generated file lands in each platform's own `target/.../src_managed/`, never
committed. To change a glyph: edit the `.svg` file here and recompile; don't look for a Scala file to edit,
there isn't one.

## Furnishing icons — sourced from game-icons.net

All 19 are sourced from [game-icons.net](https://game-icons.net), via the
[Iconify `game-icons` collection](https://icon-sets.iconify.design/game-icons/) (`https://api.iconify.design/game-icons/<name>.svg`),
unmodified except for re-saving under their bare Iconify name.

### License

**CC BY 3.0** ([full text](https://creativecommons.org/licenses/by/3.0/)), collection license confirmed via
`https://api.iconify.design/collections?prefix=game-icons`. Per
[game-icons/icons' own license.txt](https://github.com/game-icons/icons/blob/master/license.txt`), icons are
contributed by many individual artists (Lorc, Delapouite, John Colburn, and dozens more — see that file for
the full roster) who ask for a "Icons made by {author}" mention in derivative works. Iconify's per-icon API
response doesn't expose which of those contributors made which specific icon, so — consistent with how most
Iconify-sourced derivative works credit this collection — attribution here is at the collection level:

> Icons made by various [game-icons.net](https://game-icons.net) contributors (Lorc, Delapouite, and others —
> see [game-icons/icons license.txt](https://github.com/game-icons/icons/blob/master/license.txt)), licensed
> [CC BY 3.0](https://creativecommons.org/licenses/by/3.0/).

### Icon list

| File | Iconify name | Used as (DSL) |
|---|---|---|
| `anvil.svg` | `game-icons:anvil` | `anvil:` |
| `barrel.svg` | `game-icons:barrel` | `barrel:` |
| `battle-axe.svg` | `game-icons:battle-axe` | `battle-axe:` |
| `beer-stein.svg` | `game-icons:beer-stein` | `beer-stein:` |
| `bookshelf.svg` | `game-icons:bookshelf` | `bookshelf:` |
| `cage.svg` | `game-icons:cage` | `cage:` |
| `campfire.svg` | `game-icons:campfire` | `campfire:` |
| `cauldron.svg` | `game-icons:cauldron` | `cauldron:` |
| `chest.svg` | `game-icons:chest` | `chest:` |
| `ionic-column.svg` | `game-icons:ionic-column` | `ionic-column:` |
| `key.svg` | `game-icons:key` | `key:` |
| `round-shield.svg` | `game-icons:round-shield` | `round-shield:` |
| `sarcophagus.svg` | `game-icons:sarcophagus` | `sarcophagus:` |
| `skull-crossed-bones.svg` | `game-icons:skull-crossed-bones` | `skull-crossed-bones:` |
| `spider-web.svg` | `game-icons:spider-web` | `spider-web:` |
| `stone-block.svg` | `game-icons:stone-block` | `stone-block:` |
| `torch.svg` | `game-icons:torch` | `torch:` |
| `well.svg` | `game-icons:well` | `well:` |
| `wooden-crate.svg` | `game-icons:wooden-crate` | `wooden-crate:` |

`BuiltinIconsGen` recognizes these as furnishing icons by their shared `viewBox="0 0 512 512"` (not by
filename), extracting each file's single `<path d="...">` attribute into `BuiltinIcons.paths`.

Any `import ...as <alias>` + `<alias>.<icon-name>:` icon from **any** other Iconify set continues to work
exactly as before (fetched live, see `IconFetcher`) — this curated builtin set only covers the common
furnishings above; everything else still needs an explicit `import`.

## Movement glyphs — original artwork

`BuiltinIconsGen` recognizes these by their shared `viewBox="0 0 30 30"` (this project's own feature-grid
size, distinct from the furnishing icons' Iconify-native `512x512`), extracting each file's full inner
markup (everything between the outer `<svg>...</svg>`) into `BuiltinIcons.movementGlyphs`, keyed by
filename.

| File | Used as (DSL) | Notes |
|---|---|---|
| `stairs-up.svg` | `stairs: up [facing] [position]` | Tapering bars, bold near the wall the flight leads toward |
| `stairs-down.svg` | `stairs: down [facing] [position]` | Same bars, bold near the room-facing entry instead |
| `ladder.svg` | `ladder: up\|down [position]` | Rungs; direction shown by a small arrow drawn alongside it, not part of this file |
| `spiral-stairs-up.svg` | `spiral-stairs: up [position]` | Circle split into 4 quadrants around a center newel post; 3 form the flight (tapering bold at the far end from the entry), the 4th is left fully open as the entry/landing — undirected (no `facing`) |
| `spiral-stairs-down.svg` | `spiral-stairs: down [position]` | Same circle, taper reversed (bold right at the entry, thin at the far end) |

No license concerns — these are original text-maps artwork. Design brief for the straight-stairs pair:
"hand-drawn, in the spirit of Iconify's `memory:table-top-stairs-up`/`-down`, but original". The spiral pair
went through two follow-up redesigns:
1. "closer to [icon-sets.iconify.design/?query=spiral-st](https://icon-sets.iconify.design/?query=spiral-st),
   keeping the bold gradient to show direction" — replaced an earlier circular-arrow-in-box design with a
   full-circle, 8-spoke radial wheel.
2. "you lost the entry in the stairs and the orientation. 90 deg of the arc should be used to show it like
   in the model" — that 8-spoke wheel had no opening at all (unlike the reference, which leaves one 90°
   quadrant of its circle completely blank, representing where you actually step on/off), and used 45°
   spoke increments instead of a 90°-quadrant structure. The current version fixes both: one quadrant
   (90°) is left empty as the entry, and the other 3 quadrants each get one tapering arc+radius pair —
   still the same depth-fade language the straight stairs use (bold = closer to the viewer's own floor
   level), now expressed per-quadrant instead of per-spoke. See git history for all three requests.

