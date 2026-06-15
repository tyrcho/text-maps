package textmaps.dsl

enum DoorType:
  case Open, Locked, Secret, Barred

enum RoomShape:
  case Rectangular, Circular

case class RoomSize(width: Int, height: Int)

case class Room(
  id:    String,
  size:  RoomSize,
  label: Option[String] = None,
  shape: RoomShape = RoomShape.Rectangular,
)

case class Connection(
  from:     String,
  to:       String,
  door:     DoorType = DoorType.Open,
  corridor: Option[RoomSize] = None,
)

case class MapMeta(
  name:  Option[String] = None,
  seed:  Option[Long]   = None,
  style: Option[String] = None,
)

enum DungeonMapSource:
  case Manual(rooms: List[Room], connections: List[Connection])
  case Generated(roomCount: Int, style: Option[String] = None, seed: Option[Long] = None)

case class DungeonMap(meta: MapMeta, source: DungeonMapSource)

case class ParseError(message: String, offset: Int, line: Int, col: Int)

type ParseResult = Either[ParseError, DungeonMap]
