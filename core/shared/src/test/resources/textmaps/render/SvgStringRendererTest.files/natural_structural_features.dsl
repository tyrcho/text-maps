map dungeon

import icon-sets.iconify.design/game-icons as gi

room cave 6x4
  label: "Cave"
  gi.stalactites:
  gi.spikes:
  gi.cave-entrance: west

room hall 5x4
  label: "Hall"
  gi.ionic-column: north
  gi.colombian-statue: 2,1
  gi.water-drop:

room stream_room 5x3
  label: "Stream"
  gi.splashy-stream:

connect cave -> hall
  door: open

connect hall -> stream_room
  door: open
