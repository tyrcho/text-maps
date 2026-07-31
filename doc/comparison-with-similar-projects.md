# Comparison with Similar Projects

## Context

text-maps belongs to the "text-to-diagram" family of tools: plain text in,
rendered image out, scriptable and composable into other documents. This doc
records where the project sits relative to its direct sources of inspiration
and to Chartdown, a more recently discovered project covering similar ground,
so future decisions about scope can be made with this context in view instead
of re-deriving it each time.

## Sources of inspiration

- **[Mermaid](https://mermaid.js.org/)** — diagrams-as-code embedded in fenced
  code blocks, rendered via a CLI or client-side script. The README already
  frames the text-maps native CLI as being "like mermaid CLI / plantuml jar".
- **[PlantUML](https://plantuml.com/)** — text DSL for diagrams, rendered via
  a standalone jar; also the source of the `hide` keyword idea referenced
  below.
- **[campaignwiki.org/text-mapper](https://campaignwiki.org/text-mapper)** —
  a cell-coordinate-based hex/dungeon mapper (`0101 forest`), cited in
  [ADR-001](adr-001-architecture.md) as the reference project text-maps
  deliberately diverged from in favor of group-based declarations
  (`room entrance 4x4`) with automatic layout.

These three are the direct ancestors of text-maps' approach: text in, image
out, no GUI required, embeddable in docs and version control.

## Chartdown (github.com/Nossimonov/Chartdown)

Chartdown is a plain-text, Markdown-inspired language for maps, explicitly
positioned as doing "for maps what Markdown did for documents and what
Mermaid did for diagrams" — the same lineage text-maps comes from. It covers
three map archetypes (gridded battlemaps, hex exploration charts, gridless
region maps), TypeScript/npm stack, spec v0.4.

Below is where text-maps' current approach stands relative to it, per area.

### Layout: computed vs. authored — deliberate choice

text-maps computes room placement automatically via BFS from a connectivity
graph (`core/shared/.../layout/LayoutEngine.scala`); Chartdown requires the
author to give explicit coordinates/addresses (`A9`, `K9..L10`, `(x,y)`).
This mirrors the same trade-off already made against text-mapper in ADR-001:
low-friction authoring (describe rooms and connections, not coordinates)
matters more for our use case than precise-geography control. Not a gap to
close — a deliberate, already-settled choice.

### Scala 3 stack — deliberate choice

Same reasoning as ADR-001: cross-compiled Scala (JVM/JS/Native), zero npm
dependency, ADTs for the AST. Chartdown's TypeScript/npm stack has larger
ecosystem reach, but that's not sufficient reason to switch — this is an
intentional, already-decided trade-off, not an open question.

### Procedural generation — deliberate differentiator

text-maps' seeded BSP dungeon generator
(`core/shared/.../generate/DungeonGenerator.scala`) has no equivalent in
Chartdown, which is purely author-driven. Worth keeping as a differentiator
rather than something to reconcile.

### Visibility (GM/player split) — idea for the future

Chartdown marks individual elements `hidden`/`gm=`/`[gm]` inline and renders
two outputs from one source. Rather than porting that inline-tagging model
directly, a more flexible approach worth exploring later is closer to
PlantUML's `hide` directive: a separate command that selects what to exclude
at render time, instead of scattering visibility flags across every room/
feature declaration. Not scheduled — noted as a future idea.

### New map archetypes (hexcrawl, region, city) — already on the roadmap

Chartdown has already implemented hexcrawl and region maps, both of which
text-maps' own README/ADRs already list as planned-but-unbuilt. If/when
text-maps gets there, Chartdown's coordinate/extent addressing
(`docs/spec/02-coordinates-and-grids.md` in their repo) is useful prior art,
since those archetypes are inherently more "authored geography" than
text-maps' existing BFS auto-layout model. Not a current focus.

### Styling, AI tooling, exports (UVTT) — not our focus for now

Chartdown's swappable theme documents, `llms.txt`/MCP server for AI agents,
and UVTT export for VTT platforms are all noted as interesting but explicitly
out of scope for now.
