# Map reference images — sources

Reference images gathered to inform text-maps' rendering style (see `doc/adr-002-visual-references.md`
for what to take from each). All are pulled from public open-source GitHub repositories; each keeps the
license of its source repo. These are internal design references, not redistributed project assets.

| File | Category | Source | Description |
|---|---|---|---|
| `dungeon-bsp-tilemap.png` | Dungeon | [Adrian104/Dungeon-Generator](https://github.com/Adrian104/Dungeon-Generator) | BSP-generated tile-based dungeon map |
| `dungeon-bsp-geometry.png` | Dungeon | [Adrian104/Dungeon-Generator](https://github.com/Adrian104/Dungeon-Generator) | BSP partition tree / room graph geometry |
| `dungeon-gridmapper-map.png` | Dungeon | [kensanata/gridmapper](https://github.com/kensanata/gridmapper) | Grid-based dungeon mapping tool output |
| `dungeon-gridmapper-ui.png` | Dungeon | [kensanata/gridmapper](https://github.com/kensanata/gridmapper) | Gridmapper editor UI with named map |
| `medieval-citygen-scene.jpg` | Medieval | [jmespadero/cityGen](https://github.com/jmespadero/cityGen) | Rendered medieval city scene from an automatic city generator |
| `medieval-mapgen-city.png` | Medieval | [LAVS-TM/Map-Generation](https://github.com/LAVS-TM/Map-Generation) | Procedurally generated medieval city map |
| `modern-floorplan-scan.jpg` | Modern | [cansik/architectural-floor-plan](https://github.com/cansik/architectural-floor-plan) | Real scanned architectural floor plan used for ML-based room recognition |
| `modern-floorplan-2d-example.png` | Modern | [grebtsew/FloorplanToBlender3d](https://github.com/grebtsew/FloorplanToBlender3d) | 2D floor plan example used as generator input |
| `modern-blueprintjs-2d.png` | Modern | [aalavandhaann/blueprint-js](https://github.com/aalavandhaann/blueprint-js) | 2D interior/furniture floor plan from a browser floor-planning tool |
| `modern-blueprintjs-3d.png` | Modern | [aalavandhaann/blueprint-js](https://github.com/aalavandhaann/blueprint-js) | 3D-rendered view of the same floor plan tool |
| `modern-floorplan-designer.png` | Modern | [bugfishtm/floor-plan-designer](https://github.com/bugfishtm/floor-plan-designer) | Browser-based 2D floor plan designer screenshot |

## Note on scope

This sandbox's network egress policy only allows `github.com`/`raw.githubusercontent.com`; sites better
suited to this search (Wikimedia Commons, Dyson Logos' blog, itch.io asset pages) were unreachable. Medieval
*building interiors* specifically (as opposed to city-scale layouts) weren't available as committed image
files in any reachable repo, hence only 2 medieval examples vs. 4–5 in the other categories. If broader web
access becomes available later, good next stops are Dyson Logos' blog (`dysonlogos.blog/maps`, CC-licensed
dungeon/building maps in the exact style this project already imitates) and Wikimedia Commons'
`Category:Floor plans of castles` / `Category:Plans of castles in England`.
