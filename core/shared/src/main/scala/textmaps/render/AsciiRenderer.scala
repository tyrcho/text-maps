package textmaps.render

import textmaps.dsl.{DoorType, RoomShape}
import textmaps.layout.*

/** Renders a RenderedMap as ASCII art.
 *
 *  Used as an intermediate representation for approval tests.
 *
 *  Visual conventions:
 *    #   rock / background
 *    +   room corner
 *    -   horizontal room wall
 *    |   vertical room wall
 *        space — room or corridor floor
 *    .   open door
 *    L   locked door
 *    ?   secret door
 *    =   barred door
 */
object AsciiRenderer:

  private val PX_PER_CHAR = 10

  private def toC(px: Double): Int = math.round(px / PX_PER_CHAR).toInt

  def render(map: RenderedMap): String =
    if map.rooms.isEmpty then return "(empty)"

    case class CRoom(id: String, label: String, cx: Int, cy: Int, cw: Int, ch: Int)

    val cRooms = map.rooms.map { rm =>
      CRoom(rm.id, rm.label,
        toC(rm.x), toC(rm.y),
        math.max(1, toC(rm.w)), math.max(1, toC(rm.h)))
    }

    val pad  = 1
    val minX = cRooms.map(r => r.cx - 1).min - pad
    val minY = cRooms.map(r => r.cy - 1).min - pad
    val maxX = cRooms.map(r => r.cx + r.cw + 1).max + pad
    val maxY = cRooms.map(r => r.cy + r.ch + 1).max + pad
    val W    = maxX - minX
    val H    = maxY - minY
    val ox   = -minX
    val oy   = -minY

    val grid = Array.fill(H, W)('#')

    def set(gx: Int, gy: Int, c: Char): Unit =
      val ax = gx + ox; val ay = gy + oy
      if ax >= 0 && ax < W && ay >= 0 && ay < H then grid(ay)(ax) = c

    // 1. Corridor centerlines — 1 char wide
    for corr <- map.corridors; rect <- corr.rects do
      val rx = toC(rect.x); val ry = toC(rect.y)
      val rw = math.max(1, toC(rect.w)); val rh = math.max(1, toC(rect.h))
      if rect.isHorizontal then
        val midRow = toC(rect.y + rect.h / 2)
        for x <- rx until rx + rw do set(x, midRow, ' ')
      else
        val midCol = toC(rect.x + rect.w / 2)
        for y <- ry until ry + rh do set(midCol, y, ' ')

    // 2. Room floors
    for r <- cRooms do
      for y <- r.cy until r.cy + r.ch; x <- r.cx until r.cx + r.cw do set(x, y, ' ')

    // 3. Room wall borders
    for r <- cRooms do
      for x <- r.cx - 1 to r.cx + r.cw do
        set(x, r.cy - 1, '-'); set(x, r.cy + r.ch, '-')
      for y <- r.cy until r.cy + r.ch do
        set(r.cx - 1, y, '|'); set(r.cx + r.cw, y, '|')
      set(r.cx - 1, r.cy - 1, '+'); set(r.cx + r.cw, r.cy - 1, '+')
      set(r.cx - 1, r.cy + r.ch, '+'); set(r.cx + r.cw, r.cy + r.ch, '+')

    // 4. Room labels (centered, truncated to floor width)
    for r <- cRooms do
      val label = r.label.take(r.cw)
      val lx    = r.cx + (r.cw - label.length) / 2
      val ly    = r.cy + r.ch / 2
      label.zipWithIndex.foreach { case (c, i) => set(lx + i, ly, c) }

    // 5. Door glyphs at both connecting room walls.
    //    map.corridors[i] and map.doors[i] are both produced by conns.map in
    //    the same order, so zip() pairs each corridor with its door.
    val roomById = cRooms.map(r => r.id -> r).toMap
    for (corr, door) <- map.corridors.zip(map.doors) do
      val glyph = door.doorType match
        case DoorType.Open   => '.'
        case DoorType.Locked => 'L'
        case DoorType.Secret => '?'
        case DoorType.Barred => '='
      val a = roomById(corr.fromRoom)
      val b = roomById(corr.toRoom)
      corr.rects match
        case Nil =>
          // Rooms directly adjacent — use layout-provided position
          set(toC(door.position.x), toC(door.position.y), glyph)
        case rects =>
          val first = rects.head
          // FROM door: a's wall on the first rect's centerline
          if first.isHorizontal then
            val row = toC(first.y + first.h / 2)
            set(if a.cx < b.cx then a.cx + a.cw else a.cx - 1, row, glyph)
          else
            val col = toC(first.x + first.w / 2)
            set(col, if a.cy < b.cy then a.cy + a.ch else a.cy - 1, glyph)
          // TO door: prefer the horizontal rect (first wall B's left/right is crossed);
          // fall back to vertical rect only when no horizontal leg exists.
          val toRect = rects.find(_.isHorizontal).getOrElse(rects.last)
          if toRect.isHorizontal then
            val row = toC(toRect.y + toRect.h / 2)
            set(if a.cx < b.cx then b.cx - 1 else b.cx + b.cw, row, glyph)
          else
            val col = toC(toRect.x + toRect.w / 2)
            set(col, if a.cy <= b.cy then b.cy - 1 else b.cy + b.ch, glyph)

    grid.map(row => row.mkString.stripTrailing()).mkString("\n")
