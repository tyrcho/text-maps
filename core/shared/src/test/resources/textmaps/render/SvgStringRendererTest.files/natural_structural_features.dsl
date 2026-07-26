map dungeon

room cave 6x4
  label: "Cave"
  stalactite:
  stalagmite:
  crevasse:

room hall 5x4
  label: "Hall"
  pillar:
  statue:
  pool:

room stream_room 5x3
  label: "Stream"
  stream:

connect cave -> hall
  door: open

connect hall -> stream_room
  door: open
