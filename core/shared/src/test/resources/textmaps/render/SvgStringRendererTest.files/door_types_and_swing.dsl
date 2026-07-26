map dungeon

room a 3x3
  label: "A"

room b 3x3
  label: "B"

room c 3x3
  label: "C"

room d 3x3
  label: "D"

connect a -> b
  direction: east
  door: closed
  swing: outside

connect b -> c
  direction: south
  door: locked
  swing: inside

connect c -> d
  direction: west
  door: secret
  swing: inside
