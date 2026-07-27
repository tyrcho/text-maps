map dungeon

import icon-sets.iconify.design/game-icons as gi

room barracks 5x4
  label: "Barracks"
  gi.bed: north,south
  gi.watchtower: east

room hall 5x4
  label: "Hall"
  gi.fireplace: north
  gi.invisible: south
  gi.theater-curtains: west

connect barracks -> hall
  door: open
