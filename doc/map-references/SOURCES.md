# Map reference images — sources

Reference images gathered to inform text-maps' rendering style (see `doc/adr-002-visual-references.md`
for what to take from each). These are internal design references, not redistributed project assets — none
of them are bundled into the built app or represented as text-maps' own art.

## Medieval buildings/towns and dungeon maps — official D&D 5e adventure art

Sourced from [5etools-mirror-2/5etools-img](https://github.com/5etools-mirror-2/5etools-img), a community
mirror of official **Wizards of the Coast** published-adventure map art (used by the 5etools reference site
and VTT users). **This is copyrighted WotC content, not open-source or freely licensed.** It's kept here
strictly as an internal, unshipped visual reference — the same purpose as a mood board — because it's the
closest real-world match to the professional TTRPG battle-map style (grid, walls, doors, room shapes) that
`text-maps`' own SVG renderer already imitates ("Dyson Logos / One Page Dungeon" style, see ADR-001). Do
not redistribute these files outside this reference folder or represent them as project-original assets.

| File | Category | Source path | Description |
|---|---|---|---|
| `medieval-5e-cragmaw-castle.webp` | Medieval | `adventure/LMoP/Cragmaw Castle.webp` | Small goblin-held keep — *Lost Mine of Phandelver* |
| `medieval-5e-phandalin-town.webp` | Medieval | `adventure/LMoP/Phandalin.webp` | Frontier town, multiple building footprints — *Lost Mine of Phandelver* |
| `medieval-5e-castle-naerytar.webp` | Medieval | `adventure/HotDQ/029-map-6-1-castle-naerytar.webp` | Multi-level castle — *Hoard of the Dragon Queen* |
| `medieval-5e-hunting-lodge.webp` | Medieval | `adventure/HotDQ/039-map-7-1-hunting-lodge.webp` | Single medieval building, smaller scale — *Hoard of the Dragon Queen* |
| `medieval-5e-greenest-town.webp` | Medieval | `adventure/HotDQ/007-map-1-1-greenest.webp` | Walled town under siege, dense building layout — *Hoard of the Dragon Queen* |
| `dungeon-5e-cragmaw-hideout.webp` | Dungeon | `adventure/LMoP/Cragmaw Hideout.webp` | Cave/tunnel dungeon — *Lost Mine of Phandelver* |
| `dungeon-5e-wave-echo-cave.webp` | Dungeon | `adventure/LMoP/Wave Echo Cave.webp` | Large multi-chamber cave dungeon — *Lost Mine of Phandelver* |
| `dungeon-5e-redbrand-hideout.webp` | Dungeon | `adventure/LMoP/Redbrand Hideout.webp` | Dungeon built under a town building — bridges dungeon/building styles — *Lost Mine of Phandelver* |
| `dungeon-5e-stone-tooth-fortress.webp` | Dungeon | `adventure/TftYP-TFoF/002-map-2-1-the-stone-tooth.webp` | Dwarven fortress dungeon — *The Forge of Fury* |
| `dungeon-5e-sunless-citadel.webp` | Dungeon | `adventure/TftYP-TSC/002-tsc01.webp` | Classic ruined-citadel dungeon — *The Sunless Citadel* |

(Full source URL prefix: `https://github.com/5etools-mirror-2/5etools-img/tree/main/`)

## Modern buildings/houses — open-source floor-plan tools

Permissively-licensed (each repo's own license applies), sourced from open-source floor-plan/architecture
projects on GitHub:

| File | Category | Source | Description |
|---|---|---|---|
| `modern-floorplan-scan.jpg` | Modern | [cansik/architectural-floor-plan](https://github.com/cansik/architectural-floor-plan) | Real scanned architectural floor plan used for ML-based room recognition |
| `modern-floorplan-2d-example.png` | Modern | [grebtsew/FloorplanToBlender3d](https://github.com/grebtsew/FloorplanToBlender3d) | 2D floor plan example used as generator input |
| `modern-blueprintjs-2d.png` | Modern | [aalavandhaann/blueprint-js](https://github.com/aalavandhaann/blueprint-js) | 2D interior/furniture floor plan from a browser floor-planning tool |
| `modern-blueprintjs-3d.png` | Modern | [aalavandhaann/blueprint-js](https://github.com/aalavandhaann/blueprint-js) | 3D-rendered view of the same floor plan tool |
| `modern-floorplan-designer.png` | Modern | [bugfishtm/floor-plan-designer](https://github.com/bugfishtm/floor-plan-designer) | Browser-based 2D floor plan designer screenshot |
| `modern-cubicasa5k-labeled-plan.png` | Modern | [yunusskeete/floor-plan-datasets](https://github.com/yunusskeete/floor-plan-datasets) (sample from the [CubiCasa5k](https://github.com/CubiCasa/CubiCasa5k) dataset, real Finnish real-estate floor plans) | Plain black-line floor plan with room-name labels placed *inside* each room — closest match to real-estate/survey-style plans |

## Note on scope

This sandbox's network egress policy only allows `github.com`/`raw.githubusercontent.com`; sites better
suited to sourcing freely-licensed medieval/dungeon art (Dyson Logos' blog, Wikimedia Commons, itch.io) were
unreachable, which is why the medieval/dungeon set above relies on a copyrighted-content mirror instead —
flagged clearly above so it isn't mistaken for a freely-licensed asset.

Also researched but not added: a clean, modern, rectilinear **institutional/sci-fi facility floor plan**
style (icon-based legend for stairs/restrooms/hazard doors, identical footprint repeated across several
labeled floors) — genuinely not covered by anything in this folder, but no open-source GitHub repo with a
committed example image in that style was found reachable from this sandbox. Good candidates to revisit if
broader web access is available later: Tom Cartos and 2-Minute Tabletop's "Secret Research Facility" battle
maps (itch.io, currently unreachable).
