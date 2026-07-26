map dungeon

room barracks 5x4
  label: "Barracks"
  bed: north,south
  arrow-slit: east

room hall 5x4
  label: "Hall"
  fireplace: north
  curtain: west
  illusory-wall: south

connect barracks -> hall
  door: open
