map dungeon

room entrance 4x4
  label: "Entrance"

room hall 6x4
  label: "Hall"

room vault 3x3
  label: "Vault"

connect entrance -> hall
  door: open

connect hall -> vault
  door: secret
