# text-maps

Browser-based TTRPG map generator. Write a text DSL, see your map rendered live as SVG — shareable via URL.

## Usage

**Dev server:**
```sh
make dev     # compiles + watches + serves on http://localhost:8082
make test    # run unit tests
make build   # production build → public/
```

**DSL — dungeon maps:**
```
map dungeon "The Sunken Keep"
  seed: 42
  style: classic

room entrance 4x4
  label: "Gatehouse"

room great_hall 8x6
  label: "Great Hall"

room vault 4x4
  shape: circular

connect entrance -> great_hall
  door: open
  corridor: 2x4

connect great_hall -> vault
  door: secret
```

**Procedural generation** (replaces manual room declarations):
```
map dungeon
generate dungeon rooms:10 seed:42
```

**Door types:** `open` (default) | `locked` | `secret` | `barred`

**Share:** click the Share button — the DSL is LZ-compressed into the URL fragment.

## Implementation details

**Stack:** ScalaJS 1.16 + Laminar 17 (SVG rendering only) + munit tests. No npm, no Vite — just sbt with an in-process Java HTTP dev server.

**DSL parsing** (`dsl/DslParser.scala`): line-oriented parser. Lines starting at column 0 are declarations; indented lines are properties of the preceding declaration. No parser combinator library — plain Scala iterators.

**Layout engine** (`layout/LayoutEngine.scala`): BFS tree placement. Starts from the room named `entrance` (or the first declared room), places connected rooms outward in compass directions, resolves collisions with jitter. Scale: 30 px per abstract size unit.

**SVG rendering** (`render/SvgRenderer.scala`): dungeon-grid aesthetic — dark stone background with grid overlay, rooms as layered rectangles with wall borders, corridors as passages, door glyphs as SVG `<symbol>` instances. Uses Laminar's `svg.*` builders to construct DOM nodes.

**Procedural generator** (`generate/DungeonGenerator.scala`): BSP (Binary Space Partitioning) tree. Recursively splits a canvas rectangle, places a room in each leaf cell, connects siblings with corridors. Produces a `DungeonMapSource.Manual` AST — same rendering path as hand-written maps.

**URL sharing** (`main/App.scala`): LZString (CDN) compresses raw DSL text into the hash fragment (`#map/<compressed>`). Opening the URL decompresses and loads the DSL.

**Planned map types:** city/town (Voronoi districts), hexcrawl (region-based hex grid), continent (irregular polygon countries).
