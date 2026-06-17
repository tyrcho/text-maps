package textmaps.render

import textmaps.dsl.{DoorType, FeatureSize, MapType, RoomFeature, RoomShape, StairDir, WallSide}
import textmaps.layout.*

/** Renders a RenderedMap as ASCII art.
 *
 *  Used as an intermediate representation for approval tests.
 *
 *  Visual conventions (shared):
 *    +   room corner
 *    -   horizontal room wall
 *    |   vertical room wall
 *        space — room or corridor floor
 *    .   open door  (also: exterior exit)
 *    L   locked door
 *    ?   secret door
 *    =   barred door
 *    w   window (on wall)
 *    <   stairs up
 *    >   stairs down
 *
 *  Background by map type:
 *    dungeon  →  # (rock)
 *    building →  . (street / exterior)
 */
object AsciiRenderer:

  private val PX_PER_CHAR = 10

  private def toC(px: Double): Int = math.round(px / PX_PER_CHAR).toInt

  def render(map: RenderedMap): String =
    if map.rooms.isEmpty then return "(empty)"

    case class CRoom(id: String, label: String, cx: Int, cy: Int, cw: Int, ch: Int, features: List[RoomFeature])

    val cRooms = map.rooms.map { rm =>
      CRoom(rm.id, rm.label,
        toC(rm.x), toC(rm.y),
        math.max(1, toC(rm.w)), math.max(1, toC(rm.h)),
        rm.features)
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

    val bgChar = if map.mapType == MapType.Building then '.' else '#'
    val grid   = Array.fill(H, W)(bgChar)

    def set(gx: Int, gy: Int, c: Char): Unit =
      val ax = gx + ox; val ay = gy + oy
      if ax >= 0 && ax < W && ay >= 0 && ay < H then grid(ay)(ax) = c

    // Wall-center position for a given side of a room
    def wallPos(r: CRoom, side: WallSide): (Int, Int) = side match
      case WallSide.North => (r.cx + r.cw / 2, r.cy - 1)
      case WallSide.South => (r.cx + r.cw / 2, r.cy + r.ch)
      case WallSide.East  => (r.cx + r.cw,     r.cy + r.ch / 2)
      case WallSide.West  => (r.cx - 1,         r.cy + r.ch / 2)

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

    // 4. Natural / structural features (drawn before labels so labels read on top)
    val charsPerGrid = 1  // 1 char per grid square
    for r <- cRooms do
      r.features.foreach {
        case RoomFeature.Stream(size) =>
          val rows = math.max(1, size.h * charsPerGrid)
          val startRow = r.cy + (r.ch - rows) / 2
          for row <- startRow until startRow + rows; x <- r.cx until r.cx + r.cw do set(x, row, '~')

        case RoomFeature.Stalactite(size) =>
          val cols = math.min(size.w * charsPerGrid, r.cw)
          val rows = math.min(size.h * charsPerGrid, r.ch)
          val startCol = r.cx + (r.cw - cols) / 2
          for row <- r.cy until r.cy + rows; x <- startCol until startCol + cols do set(x, row, 'v')

        case RoomFeature.Stalagmite(size) =>
          val cols = math.min(size.w * charsPerGrid, r.cw)
          val rows = math.min(size.h * charsPerGrid, r.ch)
          val startCol = r.cx + (r.cw - cols) / 2
          for row <- (r.cy + r.ch - rows) until r.cy + r.ch; x <- startCol until startCol + cols do set(x, row, '^')

        case RoomFeature.Pool(size) =>
          val cols = math.min(size.w * charsPerGrid, r.cw)
          val rows = math.min(size.h * charsPerGrid, r.ch)
          val startCol = r.cx + (r.cw - cols) / 2
          val startRow = r.cy + (r.ch - rows) / 2
          for row <- startRow until startRow + rows; x <- startCol until startCol + cols do set(x, row, '~')

        case RoomFeature.Crevasse(size) =>
          val rows = math.max(1, size.h * charsPerGrid)
          val startRow = r.cy + (r.ch - rows) / 2
          for row <- startRow until startRow + rows; x <- r.cx until r.cx + r.cw do
            set(x, row, if (row - startRow) % 2 == 0 then '/' else '\\')

        case RoomFeature.Pillar(size) =>
          val cols = math.min(size.w * charsPerGrid, r.cw)
          val rows = math.min(size.h * charsPerGrid, r.ch)
          val cx = r.cx + (r.cw - cols) / 2; val cy = r.cy + (r.ch - rows) / 2
          for row <- cy until cy + rows; x <- cx until cx + cols do set(x, row, 'O')

        case RoomFeature.Statue(size) =>
          val cols = math.min(size.w * charsPerGrid, r.cw)
          val rows = math.min(size.h * charsPerGrid, r.ch)
          val cx = r.cx + (r.cw - cols) / 2; val cy = r.cy + (r.ch - rows) / 2
          for row <- cy until cy + rows; x <- cx until cx + cols do set(x, row, '@')

        case _ =>
      }

    // 4b. Room labels — movement-feature prefix/suffix baked in; drawn after features so labels read on top
    for r <- cRooms do
      val prefix = r.features.collectFirst {
        case RoomFeature.Stairs(StairDir.Up)         => "< "
        case RoomFeature.SpiralStairs(StairDir.Up)   => "S< "
        case RoomFeature.SpiralStairs(StairDir.Down) => "S> "
        case RoomFeature.Ladder(StairDir.Up)         => "^ "
        case RoomFeature.Ladder(StairDir.Down)       => "v "
      }.getOrElse("")
      val suffix = r.features.collectFirst {
        case RoomFeature.Stairs(StairDir.Down) => " >"
      }.getOrElse("")
      val display = (prefix + r.label + suffix).take(r.cw)
      val lx      = r.cx + (r.cw - display.length) / 2
      val ly      = r.cy + r.ch / 2
      display.zipWithIndex.foreach { case (c, i) => set(lx + i, ly, c) }

    // 5. Corridor door glyphs at both connecting room walls
    val roomById = cRooms.map(r => r.id -> r).toMap
    for (corr, door) <- map.corridors.zip(map.doors) do
      val glyph = door.doorType match
        case DoorType.Open       => '.'
        case DoorType.Locked     => 'L'
        case DoorType.Secret     => '?'
        case DoorType.Barred     => '='
        case DoorType.Double     => 'D'
        case DoorType.Doorway    => ' '  // open passage — no glyph, wall stays open
        case DoorType.Portcullis => '#'
      val a = roomById(corr.fromRoom)
      val b = roomById(corr.toRoom)
      corr.rects match
        case Nil =>
          set(toC(door.position.x), toC(door.position.y), glyph)
        case rects =>
          val first = rects.head
          if first.isHorizontal then
            val row = toC(first.y + first.h / 2)
            set(if a.cx < b.cx then a.cx + a.cw else a.cx - 1, row, glyph)
          else
            val col = toC(first.x + first.w / 2)
            set(col, if a.cy < b.cy then a.cy + a.ch else a.cy - 1, glyph)
          val toRect = rects.find(_.isHorizontal).getOrElse(rects.last)
          if toRect.isHorizontal then
            val row = toC(toRect.y + toRect.h / 2)
            set(if a.cx < b.cx then b.cx - 1 else b.cx + b.cw, row, glyph)
          else
            val col = toC(toRect.x + toRect.w / 2)
            set(col, if a.cy <= b.cy then b.cy - 1 else b.cy + b.ch, glyph)

    // 6. Wall-placed features — glyphs on the appropriate wall side
    for r <- cRooms do
      r.features.foreach {
        case RoomFeature.Window(side)       => val (x,y) = wallPos(r,side); set(x,y,'w')
        case RoomFeature.ArrowSlit(side)    => val (x,y) = wallPos(r,side); set(x,y,'>')
        case RoomFeature.IllusoryWall(side) => val (x,y) = wallPos(r,side); set(x,y,'!')
        case RoomFeature.Exit(side)         => val (x,y) = wallPos(r,side); set(x,y,'.')
        case RoomFeature.Fireplace(side)    => val (x,y) = wallPos(r,side); set(x,y,'f')
        case RoomFeature.Curtain(side)      => val (x,y) = wallPos(r,side); set(x,y,'~')
        case RoomFeature.Bed(side)          =>
          val (bx,by) = wallPos(r,side)
          // Draw bed as a 2-char wide marker along the wall
          set(bx, by, '=')
        case _ =>
      }

    grid.map(row => row.mkString.stripTrailing()).mkString("\n")
