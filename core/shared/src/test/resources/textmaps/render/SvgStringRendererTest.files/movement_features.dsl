map dungeon

room top 4x3
  label: "Top"
  spiral-stairs: down

room middle 4x3
  label: "Middle"
  ladder: up

room bottom 4x3
  label: "Bottom"
  ladder: down

connect top -> middle
  door: open

connect middle -> bottom
  door: open
