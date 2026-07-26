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
  corridor: 1x3
  door: locked
  door-to: barred
```

**Connections:** `corridor: WxH` sets the passage's width (W, replaces the 1-unit default) and the minimum
straight-line distance to leave between the two rooms (H); `door:` sets the door at the `from`-room end,
`door-to:` optionally sets an independent door at the `to`-room end (defaults to matching `door:` if omitted).

**Room shapes:** `rectangular` (default) | `circular` | `cave` (irregular, hand-drawn-looking outline)

**Label styles** (header property `labels:`): `legend` (numbered rooms + a "N - label" legend box below the
map — default for `map dungeon`) | `inline` (no numbers; label centred inside each room — default for
`map building`). Set explicitly to override the per-map-type default.

**Procedural generation** (replaces manual room declarations):
```
map dungeon
generate dungeon rooms:10 seed:42
```

**Door types:** `open` (default) | `locked` | `secret` | `barred`

**Share:** click the Share button — the DSL is LZ-compressed into the URL fragment.

## Implementation details

**Stack:** ScalaJS 1.16 + Scala Native 0.5.5 (cross-project). No npm, no Vite, no Laminar — sbt with an in-process Java HTTP dev server.

**Project layout:**
- `core/` — DSL parser, layout engine, BSP generator, `SvgStringRenderer`. Cross-compiles to JS and Native.
- `js/` — browser app (ScalaJS + scalajs-dom). Injects SVG strings from core via `innerHTML`.
- `native/` — CLI binary (Scala Native). Reads DSL from stdin or file, writes SVG to stdout or file.

**DSL parsing** (`core/dsl/DslParser.scala`): line-oriented parser. Declarations at column 0, properties indented. No parser combinator library.

**Layout engine** (`core/layout/LayoutEngine.scala`): BFS tree placement starting from the room named `entrance`. Corridors are computed as L-shaped rectangle segments (H + V legs) starting at room edges, creating visual doorway openings through wall borders. Each connection renders two independently-typed, correctly-angled doors — one where the corridor meets each room's wall.

**SVG rendering** (`core/render/SvgStringRenderer.scala`): Dyson Logos / One Page Dungeon style — dense diagonal cross-hatching fills the stone areas of `Dungeon` maps; `Building` maps get a plain background instead, matching real floor-plan references. White floor shapes punch through (rectangular, circular, or an irregular hand-drawn-looking "cave" outline); subtle 30px grid overlaid on rectangular floors; dark ink wall strokes. Room labels are either numbered (bold number inside the room, keyed to a legend box below the map — `labels: legend`, the `Dungeon` default) or inline (label text centred in the room, no numbers — `labels: inline`, the `Building` default). Pure string generation, works on both JS and Native.

**Procedural generator** (`core/generate/DungeonGenerator.scala`): BSP (Binary Space Partitioning). Recursively splits a canvas, places a room in each leaf, connects siblings. Produces `DungeonMapSource.Manual` — same rendering path as hand-authored maps.

**URL sharing** (`js/main/App.scala`): LZString (CDN) compresses DSL into hash fragment (`#map/<compressed>`).

**Planned map types:** city/town (Voronoi districts), hexcrawl (region-based hex grid), continent (irregular polygon countries).
