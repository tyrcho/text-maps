# Best-effort reproduction of doc/map-references/dungeon-5e-redbrand-hideout.webp using the current
# text-maps DSL, written to surface concrete gaps (see the write-up in adr-002-visual-references.md).
# Remaining simplifications forced by still-open gaps are called out inline below. The original version
# of this file used icon-substitution workarounds (e.g. gi.coffin standing in for a second sarcophagus)
# because repeating the same feature key silently overwrote the previous line; that's now fixed
# (DslParser's consumeProps preserves repeated keys instead of collapsing to the last one), so this
# version uses the genuine repeated icon/feature the reference actually has.
map dungeon "Redbrand Hideout"
  background: shadow-edge

import icon-sets.iconify.design/game-icons as gi
import icon-sets.iconify.design/mdi as mdi

# 1. Entry hall — reference has a water pool and two separate staircases in a small side alcove.
#    Both staircases now exist as distinct features (repeating stairs: on separate lines no longer
#    collides in the parser) but still visually overlap: unlike Icon, RoomFeature.Stairs has no
#    position field, so stairHatch always centers the glyph in the room regardless of facing — a
#    newly-found render-side gap, not fixed here (see adr-002).
room entrance 8x6
  label: "Entry Hall"
  gi.pool-dive: 5,1
  stairs: up west
  stairs: down east

# 2. Bedroom/storage — bunk beds on opposite walls work fine (the wall-side comma-list is the
#    one supported multi-instance case); crates + barrels are two different icons so both
#    survive too.
room barracks_2 6x5
  label: "Barracks"
  gi.bed: north,south
  gi.wooden-crate: 1,1
  gi.cellar-barrels: 4,1

# 3a/3b. The long corridor room with a locked door partway across (the red "X" glyph in the
#    reference) — modeled as two rooms joined by a `door: locked` connection, since there's no
#    concept of a door embedded mid-room on a single room's own wall.
room hall_3a 6x4
  label: "Hall"
  gi.cellar-barrels: 4,1

room hall_3b 5x4
  label: "Hall (locked wing)"

# 4. Crypt — reference has two sarcophagi; both now render from two `gi.sarcophagus:` lines.
room crypt_4 6x6
  label: "Crypt"
  gi.sarcophagus: 1,1
  gi.sarcophagus: 4,1

# 5. Rack room — reference shows two identical bar-rack fixtures plus a shackled captive.
room racks_5 5x6
  label: "Racks"
  gi.manacles: 1,1
  gi.manacles: 4,4
  gi.skeleton: 2,3

# 6. Small room off the racks, barrel pile.
room small_6 4x4
  label: "Storage Nook"
  gi.cellar-barrels: 2,2

# 7. Storage + stairs up.
room storage_7 5x5
  label: "Storage"
  gi.wooden-crate: 1,1
  stairs: up east

# 8. The central chasm/underground stream. In the reference this is unnumbered open terrain that
#    built rooms border and cross via two wooden bridges — here it has to be a numbered `cave`
#    room like any other, and the two bridges are just two ordinary connections into it (one is
#    the tree edge that places it; the other, room7 -> small_6 below, is an "extra" connection
#    added *after* both its endpoints are already placed elsewhere in the tree — see how that
#    renders, that's the actual finding).
room chasm_8 10x14
  label: "Underground Stream"
  shape: cave

# 9. Bedroom.
room bedroom_9 5x4
  label: "Bedroom"
  gi.bed: north,south

# 10. Storage, barrels + crates.
room storage_10 6x6
  label: "Storage"
  gi.cellar-barrels: 1,1
  gi.wooden-crate: 4,4

# 11. Workshop — no good game-icons workbench, so this deliberately mixes icon sets (mdi) to show
#    that's fine DSL-wise, and also deliberately leaves one icon name unresolved-ish territory
#    open (both of these do resolve, but the mechanism is the point: any Iconify set works).
room workshop_11 5x5
  label: "Workshop"
  mdi.table: 1,1
  mdi.toolbox: 3,3

# 12. Bedroom with a stair nook.
room bedroom_12 6x5
  label: "Bedroom"
  gi.bed: north,south
  stairs: up west

# Most of the reference's connections are directly adjoining rooms sharing a wall with just a doorway
# cut into it, not a corridor stretch — `corridor: 1x0` requests exactly that (width 1, zero placement
# gap), now that the distance formula is axis-aware instead of always inflated by each room's diagonal.
# The two chasm crossings below keep a real gap since those represent actual bridges over open terrain.

connect entrance -> hall_3a
  door: open
  corridor: 1x0

connect hall_3a -> hall_3b
  door: locked
  corridor: 1x0

connect hall_3b -> crypt_4
  door: open
  corridor: 1x0

connect crypt_4 -> racks_5
  door: open
  corridor: 1x0

connect racks_5 -> small_6
  door: open
  corridor: 1x0

connect entrance -> barracks_2
  door: open
  corridor: 1x0

connect barracks_2 -> storage_10
  door: secret
  corridor: 1x0

connect storage_10 -> bedroom_9
  door: open
  corridor: 1x0

connect storage_10 -> chasm_8
  door: open
  corridor: 4x2

connect chasm_8 -> bedroom_12
  door: open
  corridor: 4x2

connect bedroom_12 -> storage_7
  door: secret
  corridor: 1x0

connect storage_7 -> workshop_11
  door: open
  corridor: 1x0

# The deliberate "extra" loop edge: both storage_7 and small_6 are already placed via the two
# tree paths above (entrance -> barracks_2 -> ... -> chasm_8 -> bedroom_12 -> storage_7, versus
# entrance -> hall_3a -> ... -> racks_5 -> small_6) by the time this connection is processed, so
# it can't influence layout — only its corridor/doors get drawn post-hoc between wherever those
# two rooms already ended up. This stands in for the reference's second bridge / interconnected
# wings across the chasm.
connect storage_7 -> small_6
  door: secret
