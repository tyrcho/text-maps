package textmaps.dsl

enum MapType:
  case Dungeon   // carved in rock — thick walls, thin corridors OK
  case Building  // constructed — thin 1-char walls, multiple exterior exits

enum DoorType:
  case Open       // standard unlocked door
  case Locked     // locked door
  case Secret     // secret/concealed door
  case Barred     // barred door
  case Double     // double door (two panels)
  case Doorway    // open doorway (no door, just an opening)
  case Portcullis // iron gate / grating

enum RoomShape:
  case Rectangular, Circular

enum WallSide:
  case North, South, East, West

enum StairDir:
  case Up, Down

enum RoomFeature:
  // Vertical movement
  case Stairs(dir: StairDir)             // standard staircase
  case SpiralStairs(dir: StairDir)       // spiral staircase
  case Ladder(dir: StairDir)             // ladder up or down
  // Wall openings
  case Window(side: WallSide)            // window (light/sight, not traversable)
  case ArrowSlit(side: WallSide)         // narrow defensive slit
  case IllusoryWall(side: WallSide)      // wall that appears solid but isn't
  case Exit(side: WallSide)              // exterior exit (to street, surface, outdoors)
  // Furniture / fixtures
  case Fireplace(side: WallSide)         // fireplace against a wall
  case Bed(side: WallSide)               // bed against a wall
  case Curtain(side: WallSide)           // hanging curtain across a wall
  // Structural
  case Pillar                            // load-bearing column
  case Statue                            // decorative statue
  // Natural features
  case Stalactite                        // icicle-shaped ceiling formation
  case Stalagmite                        // spike-shaped floor formation
  case Crevasse                          // crack/fissure in the floor
  case Pool                              // pool of water
  case Stream                            // flowing water crossing the room

case class RoomSize(width: Int, height: Int)

case class Room(
  id:       String,
  size:     RoomSize,
  label:    Option[String]    = None,
  shape:    RoomShape         = RoomShape.Rectangular,
  features: List[RoomFeature] = Nil,
)

case class Connection(
  from:     String,
  to:       String,
  door:     DoorType         = DoorType.Open,
  corridor: Option[RoomSize] = None,
)

case class MapMeta(
  name:    Option[String] = None,
  seed:    Option[Long]   = None,
  style:   Option[String] = None,
  mapType: MapType        = MapType.Dungeon,
)

enum DungeonMapSource:
  case Manual(rooms: List[Room], connections: List[Connection])
  case Generated(roomCount: Int, style: Option[String] = None, seed: Option[Long] = None)

case class DungeonMap(meta: MapMeta, source: DungeonMapSource)

case class ParseError(message: String, offset: Int, line: Int, col: Int)

type ParseResult = Either[ParseError, DungeonMap]
