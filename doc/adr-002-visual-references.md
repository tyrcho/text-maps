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
  maps — not on the current roadmap, but worth remembering. **Reproduction attempt:** an actual best-effort
  DSL/SVG at `doc/map-references/redbrand-hideout-reproduction.{dsl,svg}` surfaced concrete, code-verified
  gaps rather than speculative ones, plus one actual rendering bug (**fixed twice — first cosmetically,
  then properly**, not just documented):
  1. Eyeballing the rendered reproduction first showed a corridor's dark wall strokes drawn on top of an
     unrelated room it geometrically crossed — `render`/`renderInner` (`SvgStringRenderer.scala`)
     interleaved room floors between corridor floors and corridor walls, so a corridor crossing a room it
     wasn't connected to (easy with no corridor/room collision avoidance) painted its wall strokes over
     that room's already-drawn floor. Fixed by grouping all corridor rendering before all room rendering,
     so a room's own floor+walls always render on top. This only fixed the *symptom* — the corridor still
     geometrically crossed the room, just hidden underneath it.
  2. Correctly called out as insufficient: two map elements should never cross at all, not just render in
     the right order when they do. **Properly fixed** in `LayoutEngine.bfsLayout`/`resolveCollision`
     (`LayoutEngine.scala`): neighbors of a room are now resolved in ascending placement-distance order
     (nearer siblings placed first, so a farther sibling's collision check can actually see them — they
     previously could be resolved in adjacency-list order, missing same-batch crossings entirely), and
     collision avoidance now swings a candidate room around its full placement radius at a widening set of
     angle deltas (0°, ±15°, ±30°, ... up to 180°) checking both room-room overlap *and* whether the
     resulting corridor (via a new shared `computeCorridorRects`, so this check and the actual render can
     never disagree) would cross any other already-placed room — not just a small fixed-size Cartesian
     nudge, which was too weak to route around a room sitting far along the same ray. An explicit
     `direction:` is only a hint here, same precedent as pre-existing room-overlap avoidance already set —
     collision avoidance can swing a room off its requested angle rather than let its corridor cross
     another room. Best-effort, not an absolute guarantee (a sufficiently pathological layout can still
     exhaust the search), but the z-order fix from step 1 remains as defense for whatever residual cases
     avoidance can't resolve — notably loop/extra edges (gap 5 below), whose endpoints are already fixed
     by the time they're processed and so can't be moved at all.
  Locked in by both a `corridor_avoids_room` SVG fixture and a direct geometric assertion in
  `LayoutEngineTest` (`"a corridor never crosses a room it isn't connected to"`) that no corridor rect
  overlaps any room it isn't one of the two endpoints of.

  Remaining gaps:
  1. **No irregular/polygonal "built" room shape.** `RoomShape` (`Ast.scala:19-20`) is only
     `Rectangular | Circular | Cave`. The reference's built interior (rooms 1, 3, the 4/5/6 cluster) has
     jogged, notched, L-shaped outlines that fit none of these.
  2. **No unnumbered open terrain.** Every `Cave`-shaped area is still a `Room` with a label and a legend
     row — the reference's central chasm/underground river is background terrain the built rooms border
     and cross via bridges, with no number of its own. Modeled here as room 9 ("Underground Stream"), a
     `cave` room like any other, forcing it into the numbered legend.
  3. **No "bridge" (or any linear-span) feature** crossing open terrain — nothing in `RoomFeature`
     (`Ast.scala:52-66`) models this; the two crossings are just ordinary connections into the cave room.
  4. **A room can only hold one instance of a given feature key.** ~~`consumeProps`
     (`DslParser.scala:144-150`) collects a room's properties into a `Map[String, String]` — a repeated key
     silently overwrites the previous line.~~ **Implemented:** `consumeProps` now returns
     `Map[String, List[String]]`, accumulating repeated keys instead of overwriting; `parseRoomFeatures`
     iterates every value for `stairs`/`spiral-stairs`/`ladder`/`window` and for each `<alias>.<icon-name>`
     match, so repeating e.g. `gi.sarcophagus:` on two lines now genuinely produces two `Icon` features (two
     `DslParserTest` cases lock this in). Single-valued properties (`label`, `shape`, `door`, `corridor`,
     ...) keep today's last-wins behavior via a small `.one(key)` extension. The reproduction's
     sarcophagus/coffin and manacles/skeleton icon-substitution workarounds have been removed — it now uses
     the same icon twice, as the reference does.
     ~~**Newly found while re-verifying this fix on the reproduction:** the *renderer* side isn't fully
     solved. Unlike `RoomFeature.Icon` (which carries a `position: FeaturePosition`), `RoomFeature.Stairs`/
     `SpiralStairs`/`Ladder` have no position field — `stairHatch` (`SvgStringRenderer.scala:285-310`)
     always centers the glyph in the room regardless of `facing`. So the reproduction's entry hall now has
     two genuinely distinct `Stairs` features (`up west` and `down east`), but they render on top of each
     other at the same center point — the second simply hides the first. Not fixed here; would need a
     `position` field on those three cases plus DSL syntax to set it (today's `stairs: <dir> <facing>` has
     no slot for one).~~ **Implemented** (user request: "stairs should work as any icon"): `Stairs`/
     `SpiralStairs`/`Ladder` all gained a `position: FeaturePosition = Auto` field, and `DslParser.parseStairs`/
     `parseStairDirAndPosition` accept an optional trailing `col,row` or wall-side token (`stairs: up west
     0,0`, `stairs: up 2,1` with facing left at its default, `spiral-stairs: up 1,1`, `ladder: down east`).
     The renderer side was also reworked to match `Icon`'s own mechanism rather than just accepting the new
     field: the old inline-computed `stairHatch` box was replaced with hand-drawn `feat-stairs-up`/
     `feat-stairs-down` `<symbol>`s in `defs()` (same tapering-bars design, inspired by — but not fetched
     from — Iconify's `memory:table-top-stairs-up`/`-down`), placed via the same `resolvePosition` helper
     `iconFeature` already used. The reproduction's entry hall now places its two staircases at distinct
     `col,row` positions and they render as two clearly separate glyphs (see `dungeon_room_with_stairs_and_windows`
     fixture for the minimal regression case).
  5. **No support for cyclic layouts.** `bfsLayout` (`LayoutEngine.scala:102-140`) places each room exactly
     once via a spanning tree from `entrance`. The reproduction's last connection (`storage_7 -> small_6`)
     deliberately closes a loop — both rooms are already placed via separate tree paths by the time it's
     processed — and it still renders a corridor/doors (`conns.map` runs over every connection, not just
     tree edges), just with whatever straight/L-shaped geometry falls out of wherever the tree happened to
     put those two rooms, not deliberate bridge placement.
  6. **No icon-based/glyph legend** — confirmed blocker for reproducing the reference's separate symbol key
     (Bars, Bridge, Bunk Bed, Locked Door, Rack, Secret Door, Sarcophagus, Table); already future work below.
  7. **Minimum room-to-room gap is diagonal-based, not wall-aligned.** ~~`roomHalfDiag` sums each room's
     full corner-to-center diagonal regardless of which axis the connection runs along, so the reference's
     flush, party-wall-adjacent room clusters can't be fully reproduced — there's always a visible corridor
     stub between rooms, even at the smallest `corridor:` size.~~ **Implemented:** new `roomHalfExtent`
     (`LayoutEngine.scala`) returns the exact half-width/half-height for the four cardinal placement angles
     instead of the always-larger diagonal, so `corridor: Wx0` on an axis-aligned connection now places two
     rooms genuinely flush, sharing a wall with just a doorway — no corridor floor rect gets drawn at all
     (the existing `hLen > 1`/`vLen > 1` guards in `computeCorridorRects` already skip near-zero-length
     segments). Off-axis (default 45°-stepped fan-out) placements keep the conservative diagonal margin,
     since there's no single exact edge-distance for an arbitrary rectangle at an arbitrary angle. This also
     required fixing `overlapsAny`, which previously approximated "does the candidate overlap an
     already-placed room" using only the *other* room's size plus a fixed margin — accurate enough when
     every distance was diagonal-inflated regardless of angle, but it flagged genuinely flush placements as
     false-positive collisions once `roomHalfExtent` started producing exact touching distances; it's now a
     proper AABB test using both rooms' actual sizes, and touching (not less than) no longer counts as
     overlap. The Redbrand reproduction now uses `corridor: 1x0` on its directly-adjoining connections
     (keeping a real gap only on the two chasm/bridge crossings) and the result is dramatically more
     compact — a wrapped-around footprint instead of a sprawling chain, much closer to the reference's
     actual shape. Locked in by a `LayoutEngineTest` case asserting exact wall-to-wall adjacency.
  Not attempted: compass rose, off-map distance/direction annotations ("100 feet to forest"), scale caption
  ("1 square = 5 feet") — decorative, not layout-affecting, lowest priority of what the exercise surfaced.
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
  cross-hatch filling the whole non-room area. **Implemented:** new `BackgroundStyle` enum (DSL header
  property `background:`) with three values — `plain` (flat white, and now the **default for every map
  type**, replacing `Dungeon`'s old implicit dense-hatch default), `hatch` (that old dense cross-hatch,
  kept as an explicit opt-in), and `shadow-edge` (this reference's halo: each room/corridor's own shape is
  stroked with the hatch pattern *before* its white floor fill is drawn, so only the outward-facing half of
  the stroke survives — a band hugging the wall that fades to plain white beyond it, with no new geometry
  needed since it reuses each shape's existing outline). See `dungeon_hatch_background`/
  `dungeon_shadow_edge_background` fixtures for both opt-in styles side by side.
- `dungeon-opd-numbered-plan.png`, `dungeon-opd-callout-labels.png`, and `dungeon-opd-titled-callouts.png`
  (watabou's One Page Dungeon generator — the exact "One Page Dungeon" style already named in ADR-001, and
  specifically called out by the user as a liked style) show **two label conventions this project doesn't
  have yet**, distinct from the existing `legend`/`inline` `LabelStyle`s:
  1. Large in-room numerals with no visible external legend or in-room name text (`dungeon-opd-numbered-plan.png`).
     Not implemented — still a `LabelStyle` candidate (`numbered-plain`) if this project ever wants closer
     parity with the reference style the user pointed to.
  2. Callout boxes connected to specific map features by a leader line, used both for short tags
     (`callout-labels`) and full read-aloud room text under a map title + one-line hook
     (`titled-callouts`) — the latter is the complete "one-page dungeon" document convention (title, hook,
     numbered boxed descriptions, map), not just a room-labeling choice. **Implemented (the leader-line
     callout half, not the title/hook document convention):** a new top-level DSL statement,
     `note <side> of <room-id>: <text>` (`Ast.Note`, `DungeonMapSource.Manual.notes`), independent of a
     room's own label rather than a `LabelStyle` variant — a room can have any number of notes plus its own
     label at once. Renders as a word-wrapped box with a leader line to the room, positioned on the given
     side and stacked when more than one note shares a room+side; extends the viewBox as needed
     (`SvgStringRenderer.noteBoxes`/`noteCallouts`/`expandForNotes`) since callouts can protrude on any side,
     unlike the legend which only ever grows the map downward. See the `room_notes` fixture.
- **Implemented (user request, not tied to a specific reference image):** a curated set of 19 common
  furnishing icons (torch, chest, barrel, wooden-crate, ionic-column, sarcophagus, cauldron, campfire, cage,
  anvil, bookshelf, key, well, stone-block, skull-crossed-bones, spider-web, beer-stein, battle-axe,
  round-shield) now works with **no `import` statement at all** — `BuiltinIcons.scala` embeds each icon's
  path data at compile time (source `.svg` files kept in `doc/icons/builtin/`, sourced from
  [game-icons.net](https://game-icons.net) via Iconify, CC BY 3.0, see that folder's `SOURCES.md` for
  attribution) and `SvgStringRenderer.defs()` emits one `<symbol id="builtin-<name>">` per entry;
  `DslParser` recognizes a bare (non-`alias.`-prefixed) key matching one of these names and emits
  `RoomFeature.Icon("builtin", <name>, ...)`, the same case class the general Iconify-import path uses, just
  with a sentinel `iconSet` the renderer special-cases to skip `IconFetcher` entirely. Every other Iconify
  icon still needs an explicit `import` — this only covers the common cases. See the `builtin_icons` fixture.
  The hand-drawn `Stairs`/`SpiralStairs`/`Ladder` glyphs from the item above are original artwork rather than
  Iconify-sourced, but got the same treatment for consistency: `doc/icons/builtin/stairs-up.svg`,
  `stairs-down.svg`, `ladder.svg`, `spiral-stairs-up.svg`, `spiral-stairs-down.svg` are now standalone files
  too (previously only inline `<symbol>` markup in `SvgStringRenderer.defs()`), so every builtin glyph — not
  just the ones sourced from game-icons.net — has an individually-inspectable `.svg` in the repo.
- **Implemented (follow-up user request):** the `SpiralStairs` glyph was redrawn a second time — "use sth
  closer to [icon-sets.iconify.design/?query=spiral-st](https://icon-sets.iconify.design/?query=spiral-st)
  for spiral stairs (keeping the bold gradiant to show direction)". The circular-arrow-in-a-box from the
  original implementation is gone; `feat-spiral-stairs-up`/`-down` (`SvgStringRenderer.defs()`) are now a
  circle of 8 radial "tread" spokes fanning from a center newel post — the genuine architectural top-down
  spiral-stair symbol, and visually closer to that search's `memory:table-top-spiral-stairs-round-up`/
  `-down` pie-wedge-circle icons (that icon set's own artwork wasn't reused verbatim — it's drawn as a
  solid "card" with a combined spiral+bar-ladder motif that doesn't fit this project's thin-line style at
  a 30×30 glyph size). Direction is still shown purely by stroke-weight taper, no arrow, extending the same
  depth-fade language `Stairs`' straight bars already use around the circle instead of along a row: bold at
  12 o'clock fading clockwise for `Up`, bold at 9 o'clock fading toward 12 o'clock for `Down`. See the
  `movement_features` fixture and `doc/icons/builtin/spiral-stairs-up.svg`/`-down.svg`.

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
  now actually branches on background style (it never branched on anything before) — see "Dungeon" above;
  this is no longer tied to `MapType` at all (background is now the independent `BackgroundStyle`
  property, defaulting to plain for both `Dungeon` and `Building`).
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
reference now available, see "Modern buildings/houses" above), or the plain-numeral label style surfaced by
the One Page Dungeon references (see "Dungeon" above, `numbered-plain`) — those remain future work per the
README and `doc/map-references/SOURCES.md`'s scope note. (The callout/leader-line half of that same
reference is now implemented as `note`, see "Dungeon" above — only the title/one-line-hook/numbered-
read-aloud-description document convention it's paired with in the reference remains undone.) This ADR only
records what the gathered references suggest, for whoever picks up whatever's left.

**Update:** the soft-shadow-edge background fill *is* now implemented (`BackgroundStyle.ShadowEdge`, see
"Dungeon" above) — no longer a non-decision. Its default-background change (Plain for every `MapType`,
replacing `Dungeon`'s old implicit dense-hatch default) was a related but separate call the user made
explicitly when scoping that work, not something this ADR's reference-gathering argued for on its own.
