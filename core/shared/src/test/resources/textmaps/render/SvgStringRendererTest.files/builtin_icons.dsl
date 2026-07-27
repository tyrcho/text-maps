map dungeon

room armory 6x5
  label: "Armory"
  torch: north
  chest: 1,1
  barrel: 4,3
  sarcophagus:

room storage 5x4
  label: "Storage"
  wooden-crate: 1,1
  cage: east

connect armory -> storage
  door: open
