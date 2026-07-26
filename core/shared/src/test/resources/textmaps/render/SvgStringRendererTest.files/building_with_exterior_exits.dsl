map building

room hall 5x4
  label: "Hall"
  exit: west,east
  window: north

room kitchen 3x3
  label: "Kitchen"
  exit: south

connect hall -> kitchen
  corridor: 1x3
  door: open
  door-to: locked
