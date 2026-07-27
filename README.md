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
default). `corridor: WxH` sets the passage's width (W, replaces the 1-unit default) and the minimum
straight-line distance to leave between the two rooms (H). `door:` sets the door at the `from`-room end,
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

**Room features can be positioned** — for the free-standing features (`pillar`, `statue`, `stalactite`,
`stalagmite`, `crevasse`, `pool`, `stream`), the property's value is either a size or a position, not both:
a bare number or `WxH` (e.g. `pillar: 2`, `statue: 2x3`) sets size with default placement; a wall-side word
(e.g. `statue: west`) biases placement approximately toward that side of the room; `col,row` grid-cell
coordinates (e.g. `statue: 2,1`, measured from the room's own top-left interior corner) place it precisely.
Leave the value empty (`pool:`) to keep the feature's default size and placement (centred, or
ceiling/floor-anchored for stalactite/stalagmite).

**Label styles** (header property `labels:`): `legend` (numbered rooms + a "N - label" legend box below the
map — default for `map dungeon`) | `inline` (no numbers; label centred inside each room — default for
`map building`). Set explicitly to override the per-map-type default.

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

**Layout engine** (`core/layout/LayoutEngine.scala`): BFS tree placement starting from the room named `entrance`, cycling through preset angles for each room unless a connection specifies an explicit `direction:`, which forces that placement instead. Corridors are computed as L-shaped rectangle segments (H + V legs) starting at room edges, creating visual doorway openings through wall borders. Each connection renders two independently-typed, independently-swung, correctly-angled doors — one where the corridor meets each room's wall.

**SVG rendering** (`core/render/SvgStringRenderer.scala`): Dyson Logos / One Page Dungeon style — dense diagonal cross-hatching fills the stone areas of `Dungeon` maps; `Building` maps get a plain background instead, matching real floor-plan references. White floor shapes punch through (rectangular, circular, or an irregular hand-drawn-looking "cave" outline); a light grid is overlaid on rectangular floors *and* corridors alike, anchored to each shape's own top-left corner. Dark ink wall strokes. Doors are a plain gap by default, or an architectural leaf-and-arc symbol when `swing:` is set. Stairs are a bordered box with tapering horizontal step bars. Room labels are either numbered (bold number inside the room, keyed to a legend box below the map — `labels: legend`, the `Dungeon` default) or inline (label text centred in the room, no numbers — `labels: inline`, the `Building` default). Pure string generation, works on both JS and Native.

**Procedural generator** (`core/generate/DungeonGenerator.scala`): BSP (Binary Space Partitioning). Recursively splits a canvas, places a room in each leaf, connects siblings. Produces `DungeonMapSource.Manual` — same rendering path as hand-authored maps.

**URL sharing** (`js/main/App.scala`): LZString (CDN) compresses DSL into hash fragment (`#map/<compressed>`).

**Planned map types:** city/town (Voronoi districts), hexcrawl (region-based hex grid), continent (irregular polygon countries).
