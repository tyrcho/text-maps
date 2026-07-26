map building

room hall 5x4
  label: "Hall"
  window: north

room kitchen 3x3
  label: "Kitchen"

connect hall -> kitchen
  direction: east
  corridor: 1x3
  door: open
  swing: outside
  door-to: locked
  swing-to: inside
