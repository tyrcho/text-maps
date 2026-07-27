# text-maps

Browser-based TTRPG map generator. Write a text DSL, see your map rendered live as SVG — shareable via URL. Also ships as a native CLI binary for batch SVG generation.

## Usage

**Dev server (browser app):**
```sh
make dev     # compile + watch + serve on http://localhost:8082
make test    # run unit tests
make build   # production JS build → public/
```

**Native CLI (like mermaid CLI / plantuml jar):**
```sh
make native                                 # build the binary
echo 'map dungeon
room entrance 4x4
room hall 6x4
connect entrance -> hall' | ./native/target/scala-3.3.3/text-maps-native > dungeon.svg

text-maps-native input.txt              # reads file,  writes SVG to stdout
text-maps-native input.txt output.svg   # reads file,  writes SVG to file
```

**DSL — dungeon maps:**
```
map dungeon "The Sunken Keep"
  seed: 42
  labels: legend

room entrance 4x4
  label: "Gatehouse"

room great_hall 8x6
  label: "Great Hall"

room vault 4x4
  shape: circular

room cavern 6x5
  shape: cave

connect entrance -> great_hall
  door: open

connect great_hall -> vault
  door: secret

connect vault -> cavern
  direction: south
  corridor: 1x3
  door: locked
  swing: inside
  door-to: closed
  swing-to: outside
```

**Connections:** `direction: north|south|east|west` (aliases `n/s/e/w`, or screen-relative `up/down/left/right`
/ `u/d/l/r`) explicitly places the `to`-room in that direction from the `from`-room, overriding the
automatic layout for that connection; omit it to let the layout engine place the room automatically (the
default). `direction:` is a strong hint rather than an absolute — the layout engine can still swing a room
off its requested angle if placing it there would make a room or corridor cross another, already-placed
room (best-effort: rooms and corridors are never meant to geometrically cross). `corridor: WxH` sets the
passage's width (W, replaces the 1-unit default) and the minimum straight-line distance to leave between
the two rooms (H) — `H: 0` on an axis-aligned connection (no `direction:`, or one of the four cardinal
directions) places the two rooms exactly flush, sharing a wall with just a doorway and no corridor stretch
between them, rather than the usual gap. `door:` sets the door at the `from`-room end,
`door-to:` optionally sets an independent door at the `to`-room end (defaults to matching `door:` if
omitted). `swing:`/`swing-to:` (independent per end, same pattern) control whether that door is drawn with
an architectural swing arc: `default` (a plain gap, no arc) | `inside` (swings into that door's own room) |
`outside` (swings away from it, into the passage). Swing has no effect on `secret` doors, which always keep
the flat blend-into-wall look.

**Door types:** `open` (default) | `closed` | `locked` | `secret` (dashed blend-into-wall line, marked
with a small "S" above it)

**Room shapes:** `rectangular` (default) | `circular` | `cave` (irregular, hand-drawn-looking outline)

**Stairs:** `stairs: up` or `stairs: down` alone faces north by default; add a wall-side word to set which
wall the flight leads toward, e.g. `stairs: up west`. The glyph's tapering step bars point toward that wall;
no text or arrow marks Up vs Down — instead, step-bar stroke weight fades with depth as if viewed from
above (bold = closer to the viewer's own floor level, thin = farther away), boldest near the wall for `up`
(the flight rises toward the viewer) and boldest near the room-facing entry for `down` (the flight drops
away below).

**Every room feature except doors and windows is an Iconify icon** — pillars, statues, stalactites, pools,
fireplaces, beds, curtains, arrow slits, illusory walls, anything else in an Iconify set. Import an icon set
with a header-level `import` statement, then reference any icon in it as `<alias>.<icon-name>`:
```
map dungeon
import icon-sets.iconify.design/game-icons as gi

room hall 5x4
  label: "Hall"
  gi.stalactites:
  gi.ionic-column: north
  gi.colombian-statue: 2,1
  window: south

room barracks 4x4
  gi.bed: north,south
  gi.watchtower: east
```
`import` lines can appear before or after the `map` header. The path's last `/`-segment is used as the
Iconify set prefix (`icon-sets.iconify.design/game-icons` → `game-icons`; any Iconify set works the same
way — browse icons at [icon-sets.iconify.design](https://icon-sets.iconify.design)). A property key that
doesn't match `<known-alias>.<name>` is ignored, same as any other unrecognized key.

**Positioning**: the property's value is either a size, a position, or (wall furnishings) a comma list of
wall sides:
- A bare number or `WxH` (e.g. `gi.ionic-column: 2`, `gi.colombian-statue: 2x3`) sets size with default
  (centred) placement.
- A wall-side word (e.g. `gi.colombian-statue: west`) biases placement approximately toward that side of
  the room — this is also how wall furnishings like `gi.bed`/`gi.watchtower` are placed.
- `col,row` grid-cell coordinates (e.g. `gi.colombian-statue: 2,1`, measured from the room's own top-left
  interior corner) place it precisely.
- A comma-separated list of wall-side words, *all* of which must be wall sides (e.g. `gi.bed: north,south`),
  creates one instance of that icon per side instead of a single positioned one — the multi-instance case for
  furnishing several walls with the same icon.
- Leave the value empty (`gi.stalactites:`) to keep the default size and centred placement.
- Repeating the same `<alias>.<icon-name>:` key on separate lines within a room places one instance per
  line (e.g. two `gi.sarcophagus:` lines at different `col,row` coordinates place two sarcophagi) — as does
  repeating `stairs:`/`spiral-stairs:`/`ladder:` for more than one of those in a room, though those three
  don't yet carry a position, so more than one in the same room will visually overlap at the room's centre.

**Window is the one exception** — still a hardcoded, direct SVG symbol (a plain gap with three panes, same
`WallSide` positioning), alongside doors, since those are structural openings rather than furnishings.

Icons are fetched from `api.iconify.design` at render time (cached to disk for the Native CLI, to
`localStorage` in the browser) — an unreachable or unknown icon renders as a small dashed placeholder box
labeled with the icon name instead of breaking the rest of the map.

**Label styles** (header property `labels:`): `legend` (numbered rooms + a "N - label" legend box below the
map — default for `map dungeon`) | `inline` (no numbers; label centred inside each room — default for
`map building`). Set explicitly to override the per-map-type default.

**Background styles** (header property `background:`): `plain` (flat white — the default for every map
type) | `hatch` (dense diagonal cross-hatching fills the whole rock/exterior area, the look of `Dungeon`
maps before this property existed) | `shadow-edge` (a light hatch band hugging just each room/corridor's
own boundary, fading to plain white beyond it — closer to hand-drawn dungeon references like Dyson Logos'
maps than a uniform full-canvas hatch). Independent of `map dungeon`/`map building`; any map can pick any
style, e.g.:
```
map dungeon
  background: shadow-edge
```

**Notes** — a callout box connected to a room by a leader line, independent of that room's own label,
in the One Page Dungeon convention of annotating specific room contents off to the side:
```
note left of vault: A battered chest holds a scarf and a few silver coins.
note north of vault: A tapestry on the wall depicting the land around the halls.
```
`<side>` is any `WallSide` word (`north`/`south`/`east`/`west`) or its screen-relative alias
(`up`/`down`/`left`/`right`), same vocabulary as `direction:`. Text is word-wrapped automatically; more than
one note on the same side of the same room stacks along that wall rather than overlapping.

**Procedural generation** (replaces manual room declarations):
```
map dungeon
generate dungeon rooms:10 seed:42
```

**Share:** click the Share button — the DSL is LZ-compressed into the URL fragment.

## Implementation details

**Stack:** ScalaJS 1.16 + Scala Native 0.5.5 (cross-project). No npm, no Vite, no Laminar — sbt with an in-process Java HTTP dev server.

**Project layout:**
- `core/` — DSL parser, layout engine, BSP generator, `SvgStringRenderer`. Cross-compiles to JS and Native.
- `js/` — browser app (ScalaJS + scalajs-dom). Injects SVG strings from core via `innerHTML`.
- `native/` — CLI binary (Scala Native). Reads DSL from stdin or file, writes SVG to stdout or file.

**DSL parsing** (`core/dsl/DslParser.scala`): line-oriented parser. Declarations at column 0, properties indented. No parser combinator library.

**Layout engine** (`core/layout/LayoutEngine.scala`): BFS tree placement starting from the room named `entrance`, cycling through preset angles for each room unless a connection specifies an explicit `direction:`, which biases that placement instead (still overridable by collision avoidance, see below). Siblings of a room are resolved nearest-first so a farther sibling's collision check can see a nearer one already placed in the same batch, not just previously-placed rooms from earlier in the walk. Placement distance along one of the four cardinal directions is the exact half-width/half-height of each room (not the larger corner-to-centre diagonal used for the default 45°-stepped fan-out angles), so `corridor: Wx0` on an axis-aligned connection places two rooms genuinely flush, sharing a wall with just a doorway. Corridors are computed as straight or L-shaped rectangle segments (H + V legs) starting at room edges, creating visual doorway openings through wall borders (a flush pair gets no corridor segment at all — the doorway sits directly in the shared wall); a candidate room position is rejected (swung to a different angle at the same distance, searched outward from the requested angle) if it would either overlap another room or make its corridor cross one — rooms and corridors are never meant to geometrically cross, best-effort. Each connection renders two independently-typed, independently-swung, correctly-angled doors — one where the corridor meets each room's wall.

**SVG rendering** (`core/render/SvgStringRenderer.scala`): Dyson Logos / One Page Dungeon style — background is plain white by default regardless of map type (`background: plain`), with `hatch` (dense diagonal cross-hatching across the whole rock/exterior area) and `shadow-edge` (a hatch band hugging just each room/corridor's own boundary, drawn by stroking each shape's own outline before its white floor fill covers the inward half) as opt-in alternatives — see "Background styles" above. White floor shapes punch through (rectangular, circular, or an irregular hand-drawn-looking "cave" outline); a light grid is overlaid on rectangular floors *and* corridors alike, anchored to each shape's own top-left corner. Dark ink wall strokes. Doors are a plain gap by default, or an architectural leaf-and-arc symbol when `swing:` is set; windows are the one other hardcoded direct-SVG wall symbol. Stairs are a bordered box with tapering horizontal step bars. Room labels are either numbered (bold number inside the room, keyed to a legend box below the map — `labels: legend`, the `Dungeon` default) or inline (label text centred in the room, no numbers — `labels: inline`, the `Building` default). Notes render as a word-wrapped text box with a leader line to the room they annotate, positioned outside the room on the given side and stacked when more than one shares a side, expanding the viewBox as needed so callouts are never clipped. All corridor rendering completes before any room rendering, so a room's own floor/walls always paint over a corridor that happens to cross its footprint. Pure string generation, works on both JS and Native — except `RoomFeature.Icon` (every other room feature), which looks up icon SVG content through an injected `IconFetcher` (default: always-miss, so `render(map)` alone stays pure/deterministic).

**Icon fetching** (`core/shared/.../icons/IconFetcher.scala`): a small platform-agnostic `IconFetcher`/`IconCache` abstraction with a `CachingIconFetcher` decorator (cache-first, writes back on a live hit) and a lenient `IconSvgParser` (extracts `viewBox` + inner markup from an Iconify API response, `None` on anything unexpected). Concrete implementations are per-platform: `core/jvm/.../icons/JvmIconFetcher.scala` (`java.net.http.HttpClient` + a disk-file cache — also backs the JVM test suite's offline, deterministic icon cache under `core/jvm/src/test/resources/textmaps/icons-cache`), `core/native/.../icons/NativeIconFetcher.scala` (shells out to `curl`, since Scala Native has no HTTP client, caching to `~/.cache/text-maps/icons`), and `js/.../main/JsIconFetcher.scala` (synchronous XHR + `localStorage`, so the render pipeline can stay synchronous end-to-end in the browser too).

**Procedural generator** (`core/generate/DungeonGenerator.scala`): BSP (Binary Space Partitioning). Recursively splits a canvas, places a room in each leaf, connects siblings. Produces `DungeonMapSource.Manual` — same rendering path as hand-authored maps.

**URL sharing** (`js/main/App.scala`): LZString (CDN) compresses DSL into hash fragment (`#map/<compressed>`).

**Planned map types:** city/town (Voronoi districts), hexcrawl (region-based hex grid), continent (irregular polygon countries).
