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
  door: double

connect b -> c
  door: doorway

connect c -> d
  door: portcullis
