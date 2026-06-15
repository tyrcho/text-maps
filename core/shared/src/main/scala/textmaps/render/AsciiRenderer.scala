package textmaps.render

import textmaps.dsl.{DoorType, RoomShape}
import textmaps.layout.*

/** Renders a RenderedMap as an ASCII art dungeon map.
 *
 *  Used as an intermediate representation for approval tests: the output is
 *  human-readable, deterministic, and shows up clearly in PR diffs.
 *
 *  Visual conventions:
 *    #   rock / wall (background)
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

  private val PX_PER_CHAR = 10  // pixels per character cell

  private def toC(px: Double): Int = math.round(px / PX_PER_CHAR).toInt

  def render(map: RenderedMap): String =
    if map.rooms.isEmpty then return "(empty)"

    case class CRoom(id: String, label: String, cx: Int, cy: Int, cw: Int, ch: Int)

    val cRooms = map.rooms.map { rm =>
      CRoom(rm.id, rm.label,
        toC(rm.x), toC(rm.y),
        math.max(1, toC(rm.w)), math.max(1, toC(rm.h)))
    }

    // Bounding box with 1-cell padding outside room walls
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

    // 1. Corridor floors (behind room walls — corridor opens through them)
    for c <- map.corridors; r <- c.rects do
      val cx = toC(r.x); val cy = toC(r.y)
      val cw = math.max(1, toC(r.w)); val ch = math.max(1, toC(r.h))
      for y <- cy until cy + ch; x <- cx until cx + cw do set(x, y, ' ')

    // 2. Room floors (drawn over corridor ends — rooms are always solid)
    for r <- cRooms do
      for y <- r.cy until r.cy + r.ch; x <- r.cx until r.cx + r.cw do set(x, y, ' ')

    // 3. Room wall borders (outside the floor rect)
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

    // 5. Door glyphs (at the room exit wall, overwriting the border)
    for d <- map.doors do
      val glyph = d.doorType match
        case DoorType.Open   => '.'
        case DoorType.Locked => 'L'
        case DoorType.Secret => '?'
        case DoorType.Barred => '='
      set(toC(d.position.x), toC(d.position.y), glyph)

    // Render: strip trailing whitespace per line for clean diffs
    grid.map(row => row.mkString.stripTrailing()).mkString("\n")
