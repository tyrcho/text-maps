package textmaps.generate

import textmaps.dsl.*

/** BSP dungeon generator — produces a DungeonMapSource.Manual with rooms and connections. */
object DungeonGenerator:

  private val MIN_ROOM = 3
  private val MAX_ROOM = 8

  case class Rect(x: Int, y: Int, w: Int, h: Int)

  def generate(roomCount: Int, seed: Option[Long]): DungeonMapSource.Manual =
    val rng   = Rng(seed.getOrElse(42L))
    val cells = bsp(Rect(0, 0, roomCount * 6, roomCount * 6), roomCount, rng)
    val rooms = cells.zipWithIndex.map { case (cell, i) => makeRoom(cell, i, rng) }
    val conns = connectSiblings(cells, rooms)
    DungeonMapSource.Manual(rooms, conns)

  // ── BSP ────────────────────────────────────────────────────────────────

  private def bsp(area: Rect, targetLeaves: Int, rng: Rng): List[Rect] =
    if targetLeaves <= 1 || !canSplit(area) then List(area)
    else
      split(area, rng) match
        case None => List(area)
        case Some((a, b)) =>
          val half = targetLeaves / 2
          bsp(a, half, rng) ++ bsp(b, targetLeaves - half, rng)

  private def canSplit(r: Rect): Boolean = r.w >= MIN_ROOM * 2 + 2 || r.h >= MIN_ROOM * 2 + 2

  private def split(r: Rect, rng: Rng): Option[(Rect, Rect)] =
    val horizontal = if r.w >= MIN_ROOM * 2 + 2 && r.h >= MIN_ROOM * 2 + 2
      then rng.nextInt(2) == 0
      else r.h >= MIN_ROOM * 2 + 2
    if horizontal then
      val lo = MIN_ROOM + 1
      val hi = r.h - MIN_ROOM - 1
      if hi <= lo then None
      else
        val split = lo + rng.nextInt(hi - lo)
        Some(Rect(r.x, r.y, r.w, split), Rect(r.x, r.y + split, r.w, r.h - split))
    else
      val lo = MIN_ROOM + 1
      val hi = r.w - MIN_ROOM - 1
      if hi <= lo then None
      else
        val split = lo + rng.nextInt(hi - lo)
        Some(Rect(r.x, r.y, split, r.h), Rect(r.x + split, r.y, r.w - split, r.h))

  // ── Room placement ─────────────────────────────────────────────────────

  private def makeRoom(cell: Rect, idx: Int, rng: Rng): Room =
    val pad  = 1
    val maxW = math.max(MIN_ROOM, math.min(cell.w - pad * 2, MAX_ROOM))
    val maxH = math.max(MIN_ROOM, math.min(cell.h - pad * 2, MAX_ROOM))
    val rangeW = maxW - MIN_ROOM + 1
    val rangeH = maxH - MIN_ROOM + 1
    val w = MIN_ROOM + (if rangeW > 1 then rng.nextInt(rangeW) else 0)
    val h = MIN_ROOM + (if rangeH > 1 then rng.nextInt(rangeH) else 0)
    val id = if idx == 0 then "entrance" else s"room_$idx"
    Room(id = id, size = RoomSize(w, h))

  // ── Connections ────────────────────────────────────────────────────────

  private def connectSiblings(cells: List[Rect], rooms: List[Room]): List[Connection] =
    rooms.zip(cells).sliding(2).collect { case List((r1, _), (r2, _)) =>
      Connection(from = r1.id, to = r2.id)
    }.toList
