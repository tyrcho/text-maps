map dungeon

room entrance 4x4
  label: "Entrance"

room obstacle 4x4
  label: "Obstacle"

room far 4x4
  label: "Far Room"

connect entrance -> obstacle
  direction: east
  corridor: 1x2

connect entrance -> far
  direction: east
  corridor: 1x20
