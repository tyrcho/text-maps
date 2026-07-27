# Builtin icons — sources and attribution

These 19 `.svg` files are the source of truth for `BuiltinIcons.scala`
(`core/shared/src/main/scala/textmaps/icons/BuiltinIcons.scala`), which embeds each icon's path data
directly into the renderer so it's usable as a room feature with **no `import` statement** (e.g. `torch:`,
`chest: north`) — unlike `RoomFeature.Icon`'s general case, these never hit the network at render time.

All 19 are sourced from [game-icons.net](https://game-icons.net), via the
[Iconify `game-icons` collection](https://icon-sets.iconify.design/game-icons/) (`https://api.iconify.design/game-icons/<name>.svg`),
unmodified except for re-saving under their bare Iconify name.

## License

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

## Icon list

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

Regenerating `BuiltinIcons.scala` from this folder: extract each file's single `<path d="...">` attribute
and its shared `viewBox="0 0 512 512"` — see the git history of this commit for the extraction script used
(a short Python snippet, not checked in, since it's a one-off codegen step rather than part of the build).

Any `import ...as <alias>` + `<alias>.<icon-name>:` icon from **any** other Iconify set continues to work
exactly as before (fetched live, see `IconFetcher`) — this curated builtin set only covers the common
furnishings above; everything else still needs an explicit `import`.
