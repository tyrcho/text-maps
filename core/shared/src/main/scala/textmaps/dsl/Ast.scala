package textmaps.dsl

enum MapType:
  case Dungeon   // carved in rock — thick walls, thin corridors OK
  case Building  // constructed — thin 1-char walls, multiple exterior exits

enum DoorType:
  case Open   // unlocked, shown open (just a gap) unless swing is set
  case Closed // unlocked, shown shut (flush leaf across the gap) unless swing is set
  case Locked // locked door
  case Secret // secret/concealed door — always the blend-into-wall look, ignores swing

/** Orthogonal to DoorType: whether/which way the door swings, drawn as an
 *  architectural leaf + quarter-arc. Default = today's flat glyph, no arc.
 *  Ignored entirely for DoorType.Secret. */
enum DoorSwing:
  case Default, Inside, Outside

enum RoomShape:
  case Rectangular, Circular, Cave

enum LabelStyle:
  case Legend  // numbered rooms + a legend box listing "N - label" below the map
  case Inline  // no numbers; label text centred inside each room

/** Background fill behind rooms/corridors. Independent of MapType — any map can pick any style.
 *  Default (when unset) is Plain for every MapType. */
enum BackgroundStyle:
  case Plain      // flat white
  case Hatch      // dense diagonal cross-hatch filling the whole non-room area
  case ShadowEdge // light hatch band hugging just the room/corridor boundary, white beyond it

enum WallSide:
  case North, South, East, West

enum StairDir:
  case Up, Down

/** Size of a room feature in grid squares (1 square = GRID px = 30 px).
 *  Default is 1×1.  DSL syntax:  `gi.pool:` → 1×1, `gi.pool: 2` → 2×2, `gi.pool: 3x2` → 3×2. */
case class FeatureSize(w: Int = 1, h: Int = 1)
object FeatureSize:
  val default: FeatureSize = FeatureSize()
  def square(n: Int): FeatureSize = FeatureSize(n, n)

/** Where a free-standing (non-wall-mounted) feature sits within its room.
 *  `Auto` is today's behaviour (centred / ceiling- or floor-anchored / full-width,
 *  depending on the feature). DSL syntax: the feature's own value is either a size
 *  (`2`, `2x3`) or a position — `north`/`south`/`east`/`west` (approximate bias) or
 *  `2,3` (precise grid-cell coords, in GRID units from the room's own top-left
 *  interior corner) — never both at once. */
enum FeaturePosition:
  case Auto
  case Side(side: WallSide)
  case At(col: Int, row: Int)
object FeaturePosition:
  val auto: FeaturePosition = Auto

enum RoomFeature:
  // Vertical movement
  case Stairs(dir: StairDir, facing: WallSide = WallSide.North) // standard staircase; facing = wall it leads toward
  case SpiralStairs(dir: StairDir)       // spiral staircase
  case Ladder(dir: StairDir)             // ladder up or down
  // Wall opening — the only wall feature still a hardcoded direct SVG symbol
  // (alongside doors); every other wall furnishing is an Icon (see below).
  case Window(side: WallSide)
  // Any other room feature (free-standing structural/natural — pillar, statue,
  // stalactite, ... — or a wall furnishing — fireplace, bed, curtain, arrow
  // slit, illusory wall, ...): an icon from an imported Iconify icon set (DSL:
  // `import <path> as <alias>`, then `<alias>.<icon-name>: <value>`), default 1
  // square, resized/positioned the same way regardless of what it depicts —
  // `position: FeaturePosition.Side` is used for wall furnishings.
  case Icon(iconSet: String, iconName: String, size: FeatureSize = FeatureSize.default, position: FeaturePosition = FeaturePosition.Auto)

case class RoomSize(width: Int, height: Int)

case class Room(
  id:       String,
  size:     RoomSize,
  label:    Option[String]    = None,
  shape:    RoomShape         = RoomShape.Rectangular,
  features: List[RoomFeature] = Nil,
)

/** A callout annotation pointing at a room, independent of that room's own label — the
 *  leader-line-and-text-box convention seen in One Page Dungeon-style maps (DSL:
 *  `note <side> of <room-id>: <text>`, e.g. `note left of vault: A trunk of gold and rations.`). */
case class Note(roomId: String, side: WallSide, text: String)

case class Connection(
  from:      String,
  to:        String,
  door:      DoorType          = DoorType.Open,
  corridor:  Option[RoomSize]  = None,
  doorTo:    Option[DoorType]  = None,       // None = same as `door` (the door at the `to`-room end)
  swing:     DoorSwing         = DoorSwing.Default,
  swingTo:   Option[DoorSwing] = None,       // None = same as `swing` (the door at the `to`-room end)
  direction: Option[WallSide]  = None,       // explicit placement direction from `from` towards `to`; None = auto-placed
)

case class MapMeta(
  name:       Option[String]    = None,
  seed:       Option[Long]      = None,
  style:      Option[String]    = None,
  mapType:    MapType           = MapType.Dungeon,
  labelStyle: Option[LabelStyle] = None, // None = use the per-MapType default (Dungeon -> Legend, Building -> Inline)
  background: Option[BackgroundStyle] = None, // None = Plain, regardless of MapType
)

enum DungeonMapSource:
  case Manual(rooms: List[Room], connections: List[Connection], notes: List[Note] = Nil)
  case Generated(roomCount: Int, style: Option[String] = None, seed: Option[Long] = None)

case class DungeonMap(meta: MapMeta, source: DungeonMapSource)

case class ParseError(message: String, offset: Int, line: Int, col: Int)

type ParseResult = Either[ParseError, DungeonMap]
