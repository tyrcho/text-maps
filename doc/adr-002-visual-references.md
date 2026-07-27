# ADR 002 - Visual references for map rendering

## Context

`doc/map-references/` holds 23 reference images (dungeon, medieval, modern building/house, sci-fi facility
maps) gathered to ground future rendering-style decisions in real examples rather than working from memory
alone. See `doc/map-references/SOURCES.md` for full attribution, including licensing notes: the original
WotC dungeon/medieval images are copyrighted, reference-only; the modern images are from permissively-
licensed open-source repos; a later pass (once broader internet access was available) added freely-licensed
Dyson Logos and watabou One Page Dungeon material plus a CC0 sci-fi facility icon set.

**Update: three of the takeaways below are now implemented** (background differentiation, room label
styles, and the cave room shape — see "Implemented" notes inline below). This ADR started as a
reference-only note; it's now a mix of implemented decisions and remaining future work, kept in one place
since the "why" for both draws on the same reference images.

## Takeaways per category

### Dungeon

- The 5etools adventure maps (`dungeon-5e-*`) are the clearest match for this project's actual target
  aesthetic: irregular cave/room outlines with a soft drop-shadow edge, numbered rooms keyed to a text
  legend off to the side, sparse interior detail (a few dots/marks per room rather than dense fill).
  `SvgStringRenderer`'s current dense diagonal cross-hatch is a stylistic choice, not the only valid one —
  these references argue for also considering a lighter "shadow-edge" fill as an alternative dungeon style.
  **Implemented (partially):** `RoomShape.Cave` (DSL: `shape: cave`) draws a deterministic irregular blob
  outline instead of a rectangle/circle, matching Wave Echo Cave / Cragmaw Hideout's organic room shapes.
  The numbered-rooms-plus-legend convention is now the `Dungeon` default (`labels: legend`, see "Modern"
  below) — a proper legend box below the map, not per-room text. The soft-shadow *fill* style (as opposed
  to shape/labels) is still just the existing hatch — not attempted here.
- `dungeon-5e-redbrand-hideout.webp` (a dungeon literally built under a town building) is a concrete
  reference for how `Dungeon` and `Building` styles might connect if this project ever supports mixed/nested
  maps — not on the current roadmap, but worth remembering.
- `dungeon-5e-stone-tooth-fortress.webp` and `-sunless-citadel.webp` are classic module maps with more
  rectilinear structure (built fortress rather than natural cave) — useful for comparing against the BSP
  generator's rectangular-room output.
- **Implemented:** the stairs glyph (`RoomFeature.Stairs`, `SvgStringRenderer.stairHatch`) was redesigned
  from a diagonal cross-hatch box to a bordered box with tapering horizontal step bars (narrow near the
  top, wide near the bottom — "steps receding into the distance"), directly from a reference icon the user
  shared, keeping the existing direction-arrow overlay.
- `dungeon-dyson-willowstone-hall.png` (Dyson Logos, freely commercial-licensed — see SOURCES.md) is a
  concrete example of the "soft-shadow edge" alternative fill mentioned above: a light rock-hatch band right
  at the cave-wall boundary with a clean, mostly-white grid interior, rather than the dense diagonal
  cross-hatch filling the whole room. Still not attempted in `SvgStringRenderer` — this is a better-licensed
  reference for that same future option, not a new decision.
- `dungeon-opd-numbered-plan.png`, `dungeon-opd-callout-labels.png`, and `dungeon-opd-titled-callouts.png`
  (watabou's One Page Dungeon generator — the exact "One Page Dungeon" style already named in ADR-001, and
  specifically called out by the user as a liked style) show **two label conventions this project doesn't
  have yet**, distinct from the existing `legend`/`inline` `LabelStyle`s:
  1. Large in-room numerals with no visible external legend or in-room name text (`dungeon-opd-numbered-plan.png`).
  2. Callout boxes connected to specific map features by a leader line, used both for short tags
     (`callout-labels`) and full read-aloud room text under a map title + one-line hook
     (`titled-callouts`) — the latter is the complete "one-page dungeon" document convention (title, hook,
     numbered boxed descriptions, map), not just a room-labeling choice.
  Neither is implemented; recorded here as two additional `LabelStyle` candidates (`numbered-plain` and
  `callout`) if this project ever wants closer parity with the reference style the user pointed to.

### Medieval

- City/town-scale examples (`medieval-5e-phandalin-town.webp`, `-greenest-town.webp`) show dense, irregular
  building footprints along organic streets — the clearest available reference for the city/town map type
  listed as future work in ADR-001 and the README (Voronoi districts); not something to build now.
- Single-building examples (`medieval-5e-cragmaw-castle.webp`, `-castle-naerytar.webp`, `-hunting-lodge.webp`)
  show towers, courtyards, and thick fortification walls distinct from both the current `Dungeon` (rock
  hatching) and `Building` (thin walls) styles — a future "keep/manor" preset would need its own wall
  treatment, not a straight reuse of either. **Not implemented** — still future work; see Non-decisions.

### Modern buildings/houses

- All 6 modern examples are thin-wall, rectilinear, multi-room floor plans with doors/windows marked on
  walls — this matches the current `Building` `MapType`'s thin-wall/multiple-exterior-exit design in
  `Ast.scala`/`SvgStringRenderer.scala`. No structural mismatch found. **Implemented:** `SvgStringRenderer`
  now actually branches on `MapType` (it never did before) — `Building` maps render a plain background,
  `Dungeon` maps keep the hatch.
- **In-room label placement is the strongest recurring signal across this whole reference set**, not just
  the modern category: `modern-floorplan-scan.jpg`, `modern-floorplan-designer.png`, and especially
  `modern-cubicasa5k-labeled-plan.png` (real-estate-style plan, room name centered inside each room, no
  external legend at all) all place the label *inside* the room shape rather than below it as
  `SvgStringRenderer` currently does. **Implemented:** new `LabelStyle` (`legend` | `inline`), DSL property
  `labels:`. `Building` defaults to `inline` (label centred in the room, no number); `Dungeon` defaults to
  `legend` (numbered rooms + a legend box, see "Dungeon" above). Either can be overridden explicitly per map.
- A clean, modern **institutional/facility** floor plan style (icon-based legend, identical floor plan
  repeated across several labeled sectors/floors) was identified as a gap when only `github.com` was
  reachable. **Now partially filled:** `scifi-facility-deckplan.png` and `scifi-facility-symbols.png`
  (CC0, MarkGosbell — see SOURCES.md) show a grid-based sci-fi/modern facility with a genuine icon-based
  symbol set (helipad, airlock, machinery, hazard/rubble fill) rather than text labels — this is the
  "icon-based legend glyphs" future work item mentioned below made concrete, though implementing an
  `IconLegend`-style feature set is still not attempted. Still no equivalent found for a *non-sci-fi*
  (office/hospital/school) institutional style repeated across labeled floors — that half of the original
  gap remains open.
- Seven more real-estate/floor-plan-tool samples (`modern-realestate-*`, `modern-roomsketcher-*`, see
  SOURCES.md) confirm the room-fill-by-function pattern beyond CubiCasa's plain black-and-white: the German
  real-estate examples color-fill each room by type (kitchen/bath/bedroom get different tints) *and* still
  label with both a name and a size (m² or width×height) — a fuller variant of `inline` than currently
  implemented, which centers a name only. **Not implemented** — `inline` labels currently carry no
  dimension/area text.
- The RoomSketcher hotel-lobby example (`modern-roomsketcher-hotel-lobby.jpg`) shows a labeling case this
  project's room-based model doesn't cover at all: a single open floor with several *named zones* (Lobby,
  Bar, Concierge, Storage, three Elevators, Stairs, Luggage Storage, two Bathrooms) that have no dividing
  walls between most of them — closer to a floor-plan "zone" than a `Room`. Not something to build now, but
  worth remembering if this project ever wants open-plan/mixed-use buildings.
- The RoomSketcher hotel-suite example (`modern-roomsketcher-hotel-suite.jpg`) shows two mirror-symmetric
  rooms sharing a party wall, each with its own connected ensuite — a same-scale sibling-rooms layout
  distinct from the corridor-linked room chains the other `Building` references show. No structural gap
  found (this project's room graph already supports arbitrary adjacency), just noted as a plausible fixture
  shape for future test maps.

## Non-decisions

This ADR intentionally does not commit to implementing the city/town map type, a grid-square dungeon mode,
medieval fortification-wall styling (thick walls, towers, courtyards), icon-based legend glyphs (concrete
reference now available, see "Modern buildings/houses" above), or the callout/leader-line and plain-numeral
label styles surfaced by the One Page Dungeon references (see "Dungeon" above) — those remain future work
per the README and `doc/map-references/SOURCES.md`'s scope note. It only records what the gathered
references suggest, for whoever picks that work up next.
