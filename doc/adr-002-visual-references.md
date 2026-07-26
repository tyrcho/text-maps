# ADR 002 - Visual references for map rendering

## Context

`doc/map-references/` holds 11 reference images (dungeon, medieval, modern building/house maps) gathered
from open-source projects, to ground future rendering-style decisions in real examples rather than working
from memory alone. See `doc/map-references/SOURCES.md` for full attribution. This is a reference note, not
a redesign — no rendering code changes accompany it.

## Takeaways per category

### Dungeon

- `dungeon-gridmapper-map.png`/`-ui.png` use discrete grid squares rather than continuous cross-hatch fill.
  Useful contrast case against `SvgStringRenderer`'s current dense diagonal hatching — a grid-square mode
  could be a future rendering variant, not a replacement.
- `dungeon-bsp-tilemap.png`/`-geometry.png` validate the current `DungeonGenerator`'s choices: rectangular
  leaf rooms, sibling-only connections, no overlap. Nothing here suggests changing the BSP approach.

### Medieval

- Only city-scale examples were available (`medieval-citygen-scene.jpg`, `medieval-mapgen-city.png`) —
  walled perimeters, dense irregular building placement following streets, radial/organic street networks.
  This is the clearest available reference for the city/town map type listed as future work in ADR-001
  and the README (Voronoi districts) — not something to build now.
- No medieval single-building interior references were reachable (see SOURCES.md scope note). If the
  `Building` map type ever grows a "keep/manor" preset, revisit sourcing images for that specifically —
  the current 2 medieval images don't cover it.

### Modern buildings/houses

- All 5 modern examples are thin-wall, rectilinear, multi-room floor plans with doors/windows marked on
  walls — this matches the current `Building` `MapType`'s thin-wall/multiple-exterior-exit design in
  `Ast.scala`/`SvgStringRenderer.scala`. No structural mismatch found.
- Several examples (`modern-floorplan-scan.jpg`, `modern-floorplan-designer.png`) label rooms with names
  and sometimes dimensions directly inside the room, not just below it as `SvgStringRenderer` currently
  does. Worth considering as a future label-placement option, not required now.

## Non-decisions

This ADR intentionally does not commit to implementing the city/town map type, a grid-square dungeon mode,
or new label placement — those remain future work per the README. It only records what the gathered
references suggest, for whoever picks that work up next.
