package textmaps.dsl

/** Whether the map is carved from rock (dungeon) or constructed above ground (building). */
enum MapType:
  case Dungeon   // thick rock walls, thin corridors OK
  case Building  // thin 1-char walls, multiple exterior exits, open exterior

enum DoorType:
  case Open, Locked, Secret, Barred

enum RoomShape:
  case Rectangular, Circular

/** Cardinal wall side — used to place windows, exterior exits, and stairs exits. */
enum WallSide:
  case North, South, East, West

enum StairDir:
  case Up, Down

/** Per-room features that don't create connections between rooms. */
enum RoomFeature:
  case Stairs(dir: StairDir)        // staircase going up or down
  case Window(side: WallSide)       // window opening (light/sight, not traversable)
  case Exit(side: WallSide)         // exterior door (to street, surface, or outdoors)

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
