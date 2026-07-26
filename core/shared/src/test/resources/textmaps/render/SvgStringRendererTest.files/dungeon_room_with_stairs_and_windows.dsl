map dungeon

room entrance 4x3
  label: "Entry"
  exit: west

room vault 3x3
  label: "Vault"
  stairs: up
  window: north

connect entrance -> vault
  door: locked
