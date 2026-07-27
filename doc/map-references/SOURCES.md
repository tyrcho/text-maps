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
| `dungeon-5e-redbrand-hideout.webp` | Dungeon | `adventure/LMoP/Redbrand Hideout.webp` | Dungeon built under a town building — bridges dungeon/building styles — *Lost Mine of Phandelver*. See `redbrand-hideout-reproduction.dsl`/`.svg` (original text-maps content, not sourced externally) for a best-effort DSL reproduction and `adr-002-visual-references.md`'s "Dungeon" section for the concrete DSL/renderer gaps it surfaced. |
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

## Dungeon maps — freely-licensed sources (added once general internet access was available)

The entries below were added in a follow-up pass, once the sandbox could reach sites beyond
`github.com`/`raw.githubusercontent.com`. Unlike the WotC set above, these carry licenses that actually
permit this kind of reuse, so they're better long-term references even though the WotC set remains for its
closer match to a "professional adventure module" look.

| File | Category | Source | Description |
|---|---|---|---|
| `dungeon-dyson-willowstone-hall.png` | Dungeon | [Dyson's Dodecahedron — Willowstone Hall (5-Room Dungeon)](https://dysonlogos.blog/2023/07/12/willowstone-hall-5-room-dungeon/) | Hand-drawn dwarven-hall dungeon; one of Dyson Logos' maps released under the [free commercial license](https://dysonlogos.blog/about/copyright/) (use/remix/modify, attribution "Cartography by Dyson Logos") once monthly Patreon funding clears a threshold — genuinely reusable, not just reference-only |
| `dungeon-opd-numbered-plan.png` | Dungeon | [One Page Dungeon by watabou](https://watabou.itch.io/one-page-dungeon) (itch.io preview image) | Clean multi-room complex with large in-room numerals (no external legend visible in this view) — a third room-label variant alongside this project's existing `legend` and `inline` `LabelStyle`s |
| `dungeon-opd-callout-labels.png` | Dungeon | [One Page Dungeon by watabou](https://watabou.itch.io/one-page-dungeon) (itch.io preview image) | Colored parchment-style map where callout boxes with leader lines point at specific in-room features/text, rather than a numbered legend or in-room label |
| `dungeon-opd-titled-callouts.png` | Dungeon | [One Page Dungeon by watabou](https://watabou.itch.io/one-page-dungeon) (itch.io preview image) | Same callout-with-leader-line convention, plus the genre convention this tool is named for: a map title, one-line italic hook, and numbered read-aloud room descriptions — the full "one-page dungeon" document layout, not just the map |
| `scifi-facility-deckplan.png` | Modern/Sci-fi | [Classic Science Fiction Symbols by MarkGosbell](https://markgosbell.itch.io/classic-science-fiction-symbols) (itch.io preview image) | Full example spaceship/facility deck plan built from the symbol set below — grid, numbered/labeled rooms (`computer`, `com`), airlock and helipad markings, rubble/cargo icon fill |
| `scifi-facility-symbols.png` | Modern/Sci-fi | [Classic Science Fiction Symbols by MarkGosbell](https://markgosbell.itch.io/classic-science-fiction-symbols) (itch.io preview image) | Icon/symbol sheet: helipad, airlock, machinery, rubble-fill, hazard patterns — **CC0**, "commissioned by Probabletrain for use in Dungeon Scrawl natively." Fills the "icon-based legend glyphs" gap previously flagged as unfound |

Licensing notes: the `dungeon-opd-*` files are preview screenshots of watabou's itch.io page, itself
copyrighted, but the page states generated maps may be "cop[ied], modif[ied], include[d] in your commercial
rpg adventures etc." with attribution "appreciated, but not required" — so the *style* and *generated output*
are free to draw on even though these specific screenshot images are kept reference-only like the WotC set.
`scifi-facility-*` are CC0 per the itch.io page and can be treated as freely reusable, not just reference.

## Modern buildings/houses — real-estate and floor-plan-tool samples (color-coded style)

Added on request for more "real houses, apartments, hotels" samples, to round out the plain-black-line
`modern-cubicasa5k-labeled-plan.png` example above with the color-coded, dimension-labeled style common to
real-estate listings and commercial floor-plan software.

| File | Category | Source | Description |
|---|---|---|---|
| `modern-realestate-house-floorplan.jpg` | Modern | [landhaus-solamonte.de](https://www.landhaus-solamonte.de/) | German house ground floor: rooms color-coded by function, name + m² area per room, green exterior-wall insulation hatch, north arrow, fixtures (shower, bath, stairs sensor) |
| `modern-realestate-apartment-floorplan.jpg` | Modern | [atlant-immo.de](https://www.atlant-immo.de/) | German apartment/attic floor plan: yellow/pink room fill by type, name + m² per room, furniture icons, scale bar, exterior massing shown in gray above the plan |
| `modern-realestate-hotel-floorplan.jpg` | Modern | [Booking.com](https://www.booking.com/) listing image (`cf.bstatic.com`) | Hotel room floor plan: tan/blue room-type fill, name + width×height dimensions per room, furniture and door-swing icons |
| `modern-roomsketcher-house-floorplan.jpg` | Modern | [RoomSketcher floor plan gallery — house plans](https://www.roomsketcher.com/floor-plan-gallery/house-plans/two-story-house-plans/) | Two-story house ground floor: gray/cream zone fill by function (no per-room text labels), detailed furniture icons, garage with cars, exterior massing |
| `modern-roomsketcher-studio-apartment.jpg` | Modern | [RoomSketcher floor plan gallery — studio apartments](https://www.roomsketcher.com/floor-plan-gallery/apartment/studio-apartment-plans/) | Small studio apartment, single gray fill, one visible in-room text label ("Kitchen") — a sparser labeling convention than the fully-labeled real-estate examples above |
| `modern-roomsketcher-apartment-floorplan.jpg` | Modern | [RoomSketcher floor plan gallery — 3-bedroom apartments](https://www.roomsketcher.com/floor-plan-gallery/apartment/3-bedroom-apartment-plans/) | Larger multi-bedroom apartment, blue furniture fill on a white plan, dark thick walls, hatched wardrobe/closet symbols, no room-name text at all — furniture shape alone conveys room use |
| `modern-roomsketcher-hotel-suite.jpg` | Modern | [RoomSketcher floor plan gallery — hotel plans](https://www.roomsketcher.com/floor-plan-gallery/hotel/hotel-room-layout/) | Mirror-symmetric adjoining hotel rooms sharing a party wall, each with its own ensuite bath — a connected-room layout convention distinct from the corridor-linked rooms elsewhere in this folder |
| `modern-roomsketcher-hotel-lobby.jpg` | Modern | [RoomSketcher floor plan gallery — hotel plans](https://www.roomsketcher.com/floor-plan-gallery/hotel/hotel-room-layout/) | Open-plan hotel lobby: named functional zones (Lobby, Bar, Concierge, Storage, Elevator ×3, Stairs, Luggage Storage, Bathroom ×2) with no walls between them, a wood-grain floor texture fill, and furniture placed directly on the open floor — a "zone label without a room boundary" convention this project doesn't have |

Licensing note: these are marketing/gallery images from real-estate listings, a hotel booking platform, and
a commercial floor-plan tool's own example gallery — all likely copyrighted by their respective owner (the
property lister, hotel, or RoomSketcher). Kept strictly as internal, unshipped reference material like the
WotC set, not redistributed or represented as project-original assets.

## Note on scope

Earlier in this project the sandbox's network egress policy only allowed `github.com`/
`raw.githubusercontent.com`, which is why the original medieval/dungeon set above relied on a
copyrighted-content mirror. General internet access later became available and was used to add the
freely-licensed dungeon and sci-fi-facility entries, and the color-coded real-estate/RoomSketcher modern
entries, above.

Still not covered: a **city/town-scale** map in a style distinct from the WotC `medieval-5e-phandalin-town`/
`-greenest-town` examples (still the only town-scale references), and a genuine **medieval fortification**
reference distinct from the WotC castle maps (thick walls, towers, courtyards — still future work per
ADR-002's non-decisions).
