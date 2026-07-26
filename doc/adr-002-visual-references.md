# ADR 002 - Visual references for map rendering

## Context

`doc/map-references/` holds 15 reference images (dungeon, medieval, modern building/house maps) gathered
to ground future rendering-style decisions in real examples rather than working from memory alone. See
`doc/map-references/SOURCES.md` for full attribution, including a licensing note: the dungeon/medieval
images are official D&D 5e adventure art (copyrighted, reference-only), the modern images are from
permissively-licensed open-source repos. This is a reference note, not a redesign — no rendering code
changes accompany it.

## Takeaways per category

### Dungeon

- The 5etools adventure maps (`dungeon-5e-*`) are the clearest match for this project's actual target
  aesthetic: irregular cave/room outlines with a soft drop-shadow edge, numbered rooms keyed to a text
  legend off to the side, sparse interior detail (a few dots/marks per room rather than dense fill).
  `SvgStringRenderer`'s current dense diagonal cross-hatch is a stylistic choice, not the only valid one —
  these references argue for also considering a lighter "shadow-edge" fill as an alternative dungeon style.
- `dungeon-5e-redbrand-hideout.webp` (a dungeon literally built under a town building) is a concrete
  reference for how `Dungeon` and `Building` styles might connect if this project ever supports mixed/nested
  maps — not on the current roadmap, but worth remembering.
- `dungeon-5e-stone-tooth-fortress.webp` and `-sunless-citadel.webp` are classic module maps with more
  rectilinear structure (built fortress rather than natural cave) — useful for comparing against the BSP
  generator's rectangular-room output.

### Medieval

- City/town-scale examples (`medieval-5e-phandalin-town.webp`, `-greenest-town.webp`) show dense, irregular
  building footprints along organic streets — the clearest available reference for the city/town map type
  listed as future work in ADR-001 and the README (Voronoi districts); not something to build now.
- Single-building examples (`medieval-5e-cragmaw-castle.webp`, `-castle-naerytar.webp`, `-hunting-lodge.webp`)
  show towers, courtyards, and thick fortification walls distinct from both the current `Dungeon` (rock
  hatching) and `Building` (thin walls) styles — a future "keep/manor" preset would need its own wall
  treatment, not a straight reuse of either.

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
