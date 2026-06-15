package textmaps.layout

import textmaps.dsl.*

val UNIT_PX     = 30.0  // pixels per abstract size unit
val CORRIDOR_PX = 60.0  // minimum corridor gap between rooms
val MARGIN_PX   = 20.0  // padding around viewBox

case class RenderedRoom(
  id:    String,
  label: String,
  x:     Double,  // top-left corner
  y:     Double,
  w:     Double,  // pixel width
  h:     Double,  // pixel height
  shape: RoomShape,
)

case class RenderedCorridor(fromRoom: String, toRoom: String, points: List[Vec2])

case class RenderedDoor(position: Vec2, doorType: DoorType, angle: Double)

case class RenderedMap(
  rooms:     List[RenderedRoom],
  corridors: List[RenderedCorridor],
  doors:     List[RenderedDoor],
  minX:      Double,
  minY:      Double,
  width:     Double,
  height:    Double,
)

object LayoutEngine:

  def compute(map: DungeonMap): RenderedMap =
    map.source match
      case DungeonMapSource.Manual(rooms, conns) => layout(rooms, conns, map.meta.seed)
      case DungeonMapSource.Generated(_, _, _)   =>
        layout(List.empty, List.empty, None) // generator expands before layout

  def layout(rooms: List[Room], conns: List[Connection], seed: Option[Long]): RenderedMap =
    if rooms.isEmpty then emptyMap
    else
      val centers  = bfsLayout(rooms, conns)
      val rendered = rooms.map(r => renderRoom(r, centers(r.id)))
      val byId     = rendered.map(r => r.id -> r).toMap
      val corridors = conns.map(c => renderCorridor(c, byId))
      val doors     = conns.map(c => renderDoor(c, byId))
      val allX = rendered.flatMap(r => List(r.x - MARGIN_PX, r.x + r.w + MARGIN_PX))
      val allY = rendered.flatMap(r => List(r.y - MARGIN_PX, r.y + r.h + MARGIN_PX))
      RenderedMap(
        rooms     = rendered,
        corridors = corridors,
        doors     = doors,
        minX      = allX.min,
        minY      = allY.min,
        width     = allX.max - allX.min,
        height    = allY.max - allY.min,
      )

  // ── BFS tree placement ──────────────────────────────────────────────────

  private def bfsLayout(rooms: List[Room], conns: List[Connection]): Map[String, Vec2] =
    val adj   = buildAdj(rooms, conns)
    val start = rooms.find(_.id == "entrance").orElse(rooms.headOption).get
    val placed = collection.mutable.Map[String, Vec2](start.id -> Vec2.zero)
    val queue  = collection.mutable.Queue[(String, Vec2, Int, Double)]()
    // (id, center, depth, parent-angle)
    queue.enqueue((start.id, Vec2.zero, 0, 0.0))

    val roomsById = rooms.map(r => r.id -> r).toMap
    val angles    = Array(0.0, 90.0, 270.0, 180.0, 45.0, 135.0, 225.0, 315.0)

    while queue.nonEmpty do
      val (cur, pos, depth, parentAngle) = queue.dequeue()
      val neighbors = adj.getOrElse(cur, Nil).filterNot(placed.contains)
      val conn      = conns.find(c => (c.from == cur || c.to == cur))
      neighbors.zipWithIndex.foreach { case (nb, i) =>
        val angle    = angles(i % angles.length)
        val curRoom  = roomsById(cur)
        val nbRoom   = roomsById(nb)
        val dist     = roomHalfDiag(curRoom) + roomHalfDiag(nbRoom) + CORRIDOR_PX
        val candidate = pos + Vec2.polar(angle, dist)
        val final_   = resolveCollision(candidate, placed.toMap, roomsById)
        placed(nb) = final_
        queue.enqueue((nb, final_, depth + 1, angle))
      }

    // Place any disconnected rooms to the right
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
    placed: Map[String, Vec2],
    roomsById: Map[String, Room],
  ): Vec2 =
    val jitter = Array(0.0, 45.0, 90.0, 135.0, 180.0, 225.0, 270.0, 315.0)
    jitter.foldLeft(candidate) { (best, angle) =>
      if !overlapsAny(best, placed, roomsById) then best
      else candidate + Vec2.polar(angle, CORRIDOR_PX * 0.8)
    }

  private def overlapsAny(pos: Vec2, placed: Map[String, Vec2], roomsById: Map[String, Room]): Boolean =
    placed.exists { case (id, center) =>
      val r = roomsById(id)
      val dx = math.abs(pos.x - center.x)
      val dy = math.abs(pos.y - center.y)
      dx < (r.size.width * UNIT_PX + CORRIDOR_PX * 0.5) &&
      dy < (r.size.height * UNIT_PX + CORRIDOR_PX * 0.5)
    }

  // ── Rendering helpers ───────────────────────────────────────────────────

  private def renderRoom(r: Room, center: Vec2): RenderedRoom =
    val w = r.size.width * UNIT_PX
    val h = r.size.height * UNIT_PX
    RenderedRoom(r.id, r.label.getOrElse(r.id), center.x - w / 2, center.y - h / 2, w, h, r.shape)

  private def renderCorridor(c: Connection, byId: Map[String, RenderedRoom]): RenderedCorridor =
    val a = byId(c.from)
    val b = byId(c.to)
    val p1 = Vec2(a.x + a.w / 2, a.y + a.h / 2)
    val p2 = Vec2(b.x + b.w / 2, b.y + b.h / 2)
    // L-shaped path: horizontal then vertical
    val mid = Vec2(p2.x, p1.y)
    RenderedCorridor(c.from, c.to, List(p1, mid, p2))

  private def renderDoor(c: Connection, byId: Map[String, RenderedRoom]): RenderedDoor =
    val a   = byId(c.from)
    val p1  = Vec2(a.x + a.w / 2, a.y + a.h / 2)
    val b   = byId(c.to)
    val p2  = Vec2(b.x + b.w / 2, b.y + b.h / 2)
    val mid = (p1 + p2) * 0.5
    val dx  = p2.x - p1.x
    val dy  = p2.y - p1.y
    val ang = math.toDegrees(math.atan2(dy, dx))
    RenderedDoor(mid, c.door, ang)

  private val emptyMap = RenderedMap(Nil, Nil, Nil, 0, 0, 400, 300)
