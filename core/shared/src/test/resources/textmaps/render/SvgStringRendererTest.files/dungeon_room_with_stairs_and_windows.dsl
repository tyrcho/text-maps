map dungeon

room entrance 4x3
  label: "Entry"

room vault 3x3
  label: "Vault"
  stairs: up west
  window: north

connect entrance -> vault
  door: locked
