# ADR 001 - Core Architecture

## Context

Build a browser-based TTRPG map generator where users write a text DSL and see SVG output live. Goal: multiple map types (dungeon, city, hexcrawl, continent), each with both manual authoring and procedural generation.

## Decisions

### ScalaJS + Laminar (not TypeScript/React)

ScalaJS was chosen over TypeScript for this project. Key reasons:

- Algebraic data types (sealed traits, enums) make the DSL AST explicit and exhaustive. Pattern matching on map types is safe — the compiler enforces handling all cases.
- A line-oriented DSL parser is simpler to write and test in Scala than in TypeScript, with less ceremony around null handling.
- The sibling project `dict-web` already uses ScalaJS + Laminar, giving a proven build setup (sbt + in-process Java HTTP dev server) with zero npm dependency.

Laminar is used only for SVG element construction (`svg.*` builders). The reactive data flow is intentionally bypassed in favor of direct DOM manipulation — this avoids Laminar ownership complexity for a simple single-page tool with no component hierarchy.

### Line-oriented parser (not fastparse)

The initial plan included fastparse for the DSL grammar. It was replaced with a hand-written line-oriented parser.

- The DSL is deliberately line-based: declarations start at column 0, properties are indented. A combinator parser adds complexity without benefit for this structure.
- Line-oriented parsing gives precise error messages (line + column) with no library overhead.
- fastparse was removed as a dependency after the rewrite.

Trade-off: if the DSL grows nested constructs (e.g. rooms within rooms), migrating to a proper parser combinator will be easier than maintaining the line parser.

### Group-based DSL (not cell-based like text-mapper)

The reference project `campaignwiki.org/text-mapper` uses cell coordinates (`0101 forest`). This project uses group declarations (`room entrance 4x4`).

- Cell-based DSL requires knowing the grid topology upfront. Group-based lets the layout engine decide placement automatically.
- Groups map naturally to game concepts: rooms, districts, countries. Cell maps are harder to author for connected structures.
- The layout engine handles placement automatically via BFS, so users don't manage coordinates.

### BFS tree layout (not force-directed)

BFS from the first room places connected rooms outward in compass directions.

- Deterministic: same DSL always produces the same layout.
- Readable: hub-and-spoke topology matches typical dungeon structure (central hall with branches).
- Fast: O(n) passes over the room graph.

Force-directed layouts are non-deterministic and expensive for n < 20. Constraint-based layouts require a solver. BFS is good enough for the typical 5–15 room dungeon.

### BSP procedural generator (not cellular automata)

BSP was chosen for the `generate dungeon` keyword.

- BSP guarantees non-overlapping rooms and full connectivity — both hard to achieve with cellular automata.
- Room count and canvas size are directly controllable via parameters.
- BSP output is rectangular, which matches the classic TTRPG dungeon aesthetic.

The BSP produces a `DungeonMapSource.Manual` AST — identical to what the parser produces. No special rendering path for generated maps.

### SVG rendering (not Canvas/WebGL)

All rendering uses SVG.

- SVG is declarative and inspectable. Users can export the map and open it in Inkscape/Illustrator.
- No pixel-level operations needed for dungeon maps: rooms are rectangles, corridors are polylines, doors are symbols.
- SVG scales to any screen size without blur.

### Direct DOM manipulation for app wiring

App.scala wires DOM events imperatively rather than using Laminar's reactive `Var`/`Signal` model.

- The UI is a single textarea + single SVG output — no component hierarchy or shared state that would benefit from reactivity.
- Direct event listeners (`input`, `click`, `hashchange`) are simpler to trace and debug.
- Laminar is still used for SVG element construction since its `svg.*` API is more ergonomic than raw `document.createElementNS`.
