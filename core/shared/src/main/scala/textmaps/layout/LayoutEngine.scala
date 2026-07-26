package textmaps.layout

import textmaps.dsl.*

val UNIT_PX     = 30.0  // pixels per abstract size unit (= 1 grid cell)
val CORRIDOR_PX = 60.0  // minimum gap between rooms during BFS placement
val MARGIN_PX   = 20.0  // padding around viewBox
val CORRIDOR_W  = 30.0  // passage width (1 grid unit)

case class RenderedRoom(
  id:       String,
  label:    String,
  x:        Double,  // top-left corner
  y:        Double,
  w:        Double,  // pixel width
  h:        Double,  // pixel height
  shape:    RoomShape,
  features: List[RoomFeature] = Nil,
)

// A floor rectangle segment of a corridor (H or V leg of the L-shape).
case class CorridorRect(x: Double, y: Double, w: Double, h: Double):
  def isHorizontal: Boolean = w >= h

case class RenderedCorridor(
  fromRoom: String,
  toRoom:   String,
  doorType: DoorType,
  rects:    List[CorridorRect],
)

case class RenderedDoor(position: Vec2, doorType: DoorType, angle: Double)

case class RenderedMap(
  rooms:      List[RenderedRoom],
  corridors:  List[RenderedCorridor],
  doors:      List[RenderedDoor],
  minX:       Double,
  minY:       Double,
  width:      Double,
  height:     Double,
  mapType:    MapType    = MapType.Dungeon,
  labelStyle: LabelStyle = LabelStyle.Legend,
)

object LayoutEngine:

  /** Dungeon maps default to a numbered legend; Building maps default to inline labels. */
  def defaultLabelStyle(mapType: MapType): LabelStyle = mapType match
    case MapType.Dungeon  => LabelStyle.Legend
    case MapType.Building => LabelStyle.Inline

  def compute(map: DungeonMap): RenderedMap =
    map.source match
      case DungeonMapSource.Manual(rooms, conns) =>
        layout(rooms, conns, map.meta.seed, map.meta.mapType, map.meta.labelStyle)
      case DungeonMapSource.Generated(_, _, _) =>
        layout(List.empty, List.empty, None, map.meta.mapType, map.meta.labelStyle)

  def layout(
    rooms:      List[Room],
    conns:      List[Connection],
    seed:       Option[Long],
    mapType:    MapType             = MapType.Dungeon,
    labelStyle: Option[LabelStyle]  = None,
  ): RenderedMap =
    val resolvedLabelStyle = labelStyle.getOrElse(defaultLabelStyle(mapType))
    if rooms.isEmpty then emptyMap.copy(mapType = mapType, labelStyle = resolvedLabelStyle)
    else
      val centers   = bfsLayout(rooms, conns)
      val rendered  = rooms.map(r => renderRoom(r, centers(r.id)))
      val byId      = rendered.map(r => r.id -> r).toMap
      val corridors = conns.map(c => renderCorridor(c, byId))
      val doors     = conns.map(c => renderDoor(c, byId))
      val allX = rendered.flatMap(r => List(r.x - MARGIN_PX, r.x + r.w + MARGIN_PX))
      val allY = rendered.flatMap(r => List(r.y - MARGIN_PX, r.y + r.h + MARGIN_PX))
      RenderedMap(
        rooms      = rendered,
        corridors  = corridors,
        doors      = doors,
        minX       = allX.min,
        minY       = allY.min,
        width      = allX.max - allX.min,
        height     = allY.max - allY.min,
        mapType    = mapType,
        labelStyle = resolvedLabelStyle,
      )

  // ── BFS tree placement ──────────────────────────────────────────────────

  private def bfsLayout(rooms: List[Room], conns: List[Connection]): Map[String, Vec2] =
    val adj    = buildAdj(rooms, conns)
    val start  = rooms.find(_.id == "entrance").orElse(rooms.headOption).get
    val placed = collection.mutable.Map[String, Vec2](start.id -> Vec2.zero)
    val queue  = collection.mutable.Queue[(String, Vec2, Int, Double)]()
    queue.enqueue((start.id, Vec2.zero, 0, 0.0))

    val roomsById = rooms.map(r => r.id -> r).toMap
    val angles    = Array(0.0, 90.0, 270.0, 180.0, 45.0, 135.0, 225.0, 315.0)

    while queue.nonEmpty do
      val (cur, pos, _, _) = queue.dequeue()
      val neighbors = adj.getOrElse(cur, Nil).filterNot(placed.contains)
      neighbors.zipWithIndex.foreach { case (nb, i) =>
        val angle     = angles(i % angles.length)
        val curRoom   = roomsById(cur)
        val nbRoom    = roomsById(nb)
        val dist      = roomHalfDiag(curRoom) + roomHalfDiag(nbRoom) + CORRIDOR_PX
        val candidate = pos + Vec2.polar(angle, dist)
        val final_    = resolveCollision(candidate, placed.toMap, roomsById)
        placed(nb) = final_
        queue.enqueue((nb, final_, 0, angle))
      }

    val maxX = if placed.isEmpty then 0.0 else placed.values.map(_.x).max
    rooms.filterNot(r => placed.contains(r.id)).zipWithIndex.foreach { case (r, i) =>
      placed(r.id) = Vec2(maxX + 200 * (i + 1), 0)
    }
    placed.toMap

  private def buildAdj(rooms: List[Room], conns: List[Connection]): Map[String, List[String]] =
    conns.foldLeft(Map.empty[String, List[String]]) { case (acc, c) =>
      acc
        .updated(c.from, c.to :: acc.getOrElse(c.from, Nil))
        .updated(c.to, c.from :: acc.getOrElse(c.to, Nil))
    }

  private def roomHalfDiag(r: Room): Double =
    math.sqrt(math.pow(r.size.width * UNIT_PX / 2, 2) + math.pow(r.size.height * UNIT_PX / 2, 2))

  private def resolveCollision(
    candidate: Vec2,
    placed:    Map[String, Vec2],
    roomsById: Map[String, Room],
  ): Vec2 =
    val jitter = Array(0.0, 45.0, 90.0, 135.0, 180.0, 225.0, 270.0, 315.0)
    jitter.foldLeft(candidate) { (best, angle) =>
      if !overlapsAny(best, placed, roomsById) then best
      else candidate + Vec2.polar(angle, CORRIDOR_PX * 0.8)
    }

  private def overlapsAny(pos: Vec2, placed: Map[String, Vec2], roomsById: Map[String, Room]): Boolean =
    placed.exists { case (id, center) =>
      val r  = roomsById(id)
      val dx = math.abs(pos.x - center.x)
      val dy = math.abs(pos.y - center.y)
      dx < (r.size.width  * UNIT_PX + CORRIDOR_PX * 0.5) &&
      dy < (r.size.height * UNIT_PX + CORRIDOR_PX * 0.5)
    }

  // ── Rendering helpers ───────────────────────────────────────────────────

  private def renderRoom(r: Room, center: Vec2): RenderedRoom =
    val w = r.size.width  * UNIT_PX
    val h = r.size.height * UNIT_PX
    RenderedRoom(r.id, r.label.getOrElse(r.id), center.x - w / 2, center.y - h / 2, w, h, r.shape, r.features)

  // How far each corridor rect extends into the connected room.
  // The corridor's white fill paints over the room's wall border at this
  // overlap, creating a visual doorway opening in the rendered map.
  private val WALL_OVERLAP = 3.0

  private def renderCorridor(c: Connection, byId: Map[String, RenderedRoom]): RenderedCorridor =
    val a    = byId(c.from)
    val b    = byId(c.to)
    val cxA  = a.x + a.w / 2
    val cyA  = a.y + a.h / 2
    val cxB  = b.x + b.w / 2
    val cyB  = b.y + b.h / 2
    val half = CORRIDOR_W / 2

    val rects = collection.mutable.ListBuffer[CorridorRect]()

    val heightDiff = math.abs(cyB - cyA)
    val widthDiff  = math.abs(cxB - cxA)

    if heightDiff <= CORRIDOR_W then
      // Same-ish height: straight horizontal corridor wall-to-wall
      val exitX  = if cxB >= cxA then a.x + a.w - WALL_OVERLAP else a.x + WALL_OVERLAP
      val entryX = if cxB >= cxA then b.x + WALL_OVERLAP else b.x + b.w - WALL_OVERLAP
      val hLen   = math.abs(entryX - exitX)
      if hLen > 1 then
        val midY = (cyA + cyB) / 2
        rects += CorridorRect(math.min(exitX, entryX), midY - half, hLen, CORRIDOR_W)

    else if widthDiff <= CORRIDOR_W then
      // Same-ish x: straight vertical corridor wall-to-wall
      val exitY  = if cyB >= cyA then a.y + a.h - WALL_OVERLAP else a.y + WALL_OVERLAP
      val entryY = if cyB >= cyA then b.y + WALL_OVERLAP else b.y + b.h - WALL_OVERLAP
      val vLen   = math.abs(entryY - exitY)
      if vLen > 1 then
        val midX = (cxA + cxB) / 2
        rects += CorridorRect(midX - half, math.min(exitY, entryY), CORRIDOR_W, vLen)

    else
      // L-shaped: horizontal to B's vertical axis, then vertical to B's entry wall
      val exitX  = if cxB >= cxA then a.x + a.w - WALL_OVERLAP else a.x + WALL_OVERLAP
      val entryY = if cyB >= cyA then b.y - WALL_OVERLAP else b.y + b.h + WALL_OVERLAP
      val hLen   = math.abs(cxB - exitX)
      if hLen > 1 then
        rects += CorridorRect(math.min(exitX, cxB), cyA - half, hLen, CORRIDOR_W)
      val vLen   = math.abs(entryY - cyA)
      if vLen > 1 then
        rects += CorridorRect(cxB - half, math.min(cyA, entryY), CORRIDOR_W, vLen)

    RenderedCorridor(c.from, c.to, c.door, rects.toList)

  private def renderDoor(c: Connection, byId: Map[String, RenderedRoom]): RenderedDoor =
    val a    = byId(c.from)
    val b    = byId(c.to)
    val cxA  = a.x + a.w / 2
    val cyA  = a.y + a.h / 2
    val cxB  = b.x + b.w / 2
    val exitX = if cxB >= cxA then a.x + a.w else a.x
    RenderedDoor(Vec2(exitX, cyA), c.door, 90.0)

  private val emptyMap = RenderedMap(Nil, Nil, Nil, 0, 0, 400, 300)
