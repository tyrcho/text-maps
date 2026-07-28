package textmaps.render

import textmaps.dsl.{BackgroundStyle, DoorSwing, DoorType, FeaturePosition, FeatureSize, LabelStyle, RoomFeature, RoomShape, RoomSize, StairDir, WallSide}
import textmaps.generate.Rng
import textmaps.icons.{BuiltinIcons, IconFetcher}
import textmaps.layout.*

/** Renders a RenderedMap as a self-contained SVG string.
 *
 *  Visual style: Dyson Logos / One Page Dungeon
 *  - Background is plain white by default (`BackgroundStyle.Plain`), regardless of MapType; opt into
 *    `BackgroundStyle.Hatch` for dense diagonal cross-hatching across the whole rock/exterior area, or
 *    `BackgroundStyle.ShadowEdge` for a hatch band hugging just each room/corridor's own boundary (a
 *    soft halo, fading to plain white beyond it) — see the `background:` DSL header property
 *  - White floor rectangles for rooms and corridors
 *  - All corridor rendering (floor/walls/grid) is emitted before any room rendering, so a room's own
 *    floor+walls always paint over a corridor that happens to cross its footprint — layout has no
 *    corridor/room collision avoidance, so a long or L-shaped corridor between distant rooms can
 *    geometrically pass through an unrelated room; this ordering keeps that room visually on top rather
 *    than showing a stray corridor wall stroke drawn over it
 *  - Floor grid anchored to each shape's own top-left (not global coordinates),
 *    for rooms and corridors alike
 *  - Dark ink wall strokes
 *  - Stairs: box with tapering step bars, rotated to face the wall the flight
 *    leads toward; bar stroke weight fades with depth to show Up vs Down
 *  - Windows: small opening gap on the wall — along with doors, the only
 *    hardcoded direct-SVG room feature
 *  - Doors: flat gap glyph by default; an architectural leaf + swing arc when
 *    `swing` is Inside/Outside; secret doors get a small "S" above their line
 *  - Every other room feature (`RoomFeature.Icon`) is an embedded Iconify icon,
 *    fetched via an injected `IconFetcher`; a missing icon falls back to a
 *    dashed placeholder box + its name rather than rendering nothing
 *  - Room number inside (bold, centred); room name below the room
 */
object SvgStringRenderer:

  private val GRID    = 30
  private val WALL    = 1.5
  private val FONT_SZ = 9

  def render(map: RenderedMap, iconFetcher: IconFetcher = IconFetcher.none): String =
    val pad = 60.0
    val baseVx = (map.minX - pad).toInt
    val baseVy = (map.minY - pad).toInt
    val baseVw = (map.width  + pad * 2).toInt
    val baseVh = (map.height + pad * 2 + legendHeight(map)).toInt
    val (vx, vy, vw, vh) = expandForNotes(map, baseVx, baseVy, baseVw, baseVh)
    s"""|<?xml version="1.0" encoding="UTF-8"?>
        |<svg xmlns="http://www.w3.org/2000/svg"
        |     viewBox="$vx $vy $vw $vh"
        |     width="$vw" height="$vh">
        |${defs()}
        |${background(vx, vy, vw, vh, map.backgroundStyle)}
        |${edgeHatchLayer(map)}
        |${map.corridors.flatMap(corridorFloors).mkString("\n")}
        |${map.corridors.flatMap(corridorWalls).mkString("\n")}
        |${map.corridors.flatMap(corridorGrid).mkString("\n")}
        |${map.rooms.map(roomFloor).mkString("\n")}
        |${map.rooms.map(roomWalls).mkString("\n")}
        |${map.rooms.map(roomGrid).mkString("\n")}
        |${map.rooms.flatMap(rm => roomFeatures(rm, iconFetcher)).mkString("\n")}
        |${map.doors.map(door).mkString("\n")}
        |${map.rooms.zipWithIndex.map { (rm, i) => roomLabel(rm, i + 1, map.labelStyle) }.mkString("\n")}
        |${legendBox(map)}
        |${noteCallouts(map)}
        |</svg>""".stripMargin

  def renderInner(map: RenderedMap, iconFetcher: IconFetcher = IconFetcher.none): String =
    val pad = 60.0
    val baseVx = (map.minX - pad).toInt
    val baseVy = (map.minY - pad).toInt
    val baseVw = (map.width  + pad * 2).toInt
    val baseVh = (map.height + pad * 2 + legendHeight(map)).toInt
    val (vx, vy, vw, vh) = expandForNotes(map, baseVx, baseVy, baseVw, baseVh)
    val viewBoxAttr = s"$vx $vy $vw $vh"
    s"""|__VIEWBOX__${viewBoxAttr}__END__
        |${defs()}
        |${background(vx, vy, vw, vh, map.backgroundStyle)}
        |${edgeHatchLayer(map)}
        |${map.corridors.flatMap(corridorFloors).mkString("\n")}
        |${map.corridors.flatMap(corridorWalls).mkString("\n")}
        |${map.corridors.flatMap(corridorGrid).mkString("\n")}
        |${map.rooms.map(roomFloor).mkString("\n")}
        |${map.rooms.map(roomWalls).mkString("\n")}
        |${map.rooms.map(roomGrid).mkString("\n")}
        |${map.rooms.flatMap(rm => roomFeatures(rm, iconFetcher)).mkString("\n")}
        |${map.doors.map(door).mkString("\n")}
        |${map.rooms.zipWithIndex.map { (rm, i) => roomLabel(rm, i + 1, map.labelStyle) }.mkString("\n")}
        |${legendBox(map)}
        |${noteCallouts(map)}""".stripMargin

  // ── <defs> ────────────────────────────────────────────────────────────

  private def defs(): String = s"""<defs>
  <pattern id="hatch" width="6" height="6"
           patternTransform="rotate(45 0 0)" patternUnits="userSpaceOnUse">
    <line x1="0" y1="0" x2="0" y2="6" stroke="#888" stroke-width="1.2"/>
  </pattern>

  <!-- ── Door symbols (viewBox 0 0 30 30, open gap fills corridor width) ── -->
  <symbol id="door-open" viewBox="0 0 30 30">
    <rect x="0"  y="12" width="30" height="6" fill="white"/>
    <rect x="0"  y="11" width="4"  height="8" fill="#333"/>
    <rect x="26" y="11" width="4"  height="8" fill="#333"/>
  </symbol>
  <symbol id="door-closed" viewBox="0 0 30 30">
    <rect x="0" y="12" width="30" height="6" fill="white"/>
    <rect x="2" y="13" width="26" height="4" fill="#333"/>
  </symbol>
  <symbol id="door-locked" viewBox="0 0 30 30">
    <rect x="0" y="12" width="30" height="6" fill="white"/>
    <rect x="2" y="11" width="26" height="8" fill="#333"/>
    <rect x="4" y="13" width="22" height="4" fill="white"/>
  </symbol>
  <symbol id="door-secret" viewBox="0 0 30 30">
    <line x1="0" y1="15" x2="30" y2="15"
          stroke="#888" stroke-width="1.2" stroke-dasharray="6,6"/>
  </symbol>

  <!-- ── Room feature symbols (viewBox 0 0 30 30, one grid cell), see BuiltinIcons.movementGlyphs for
       the markup itself and doc/icons/builtin/{stairs,spiral-stairs}-{up,down}.svg / ladder.svg for the
       design notes (tapering-bar / quadrant-with-an-open-entry / rung glyphs). ── -->
$movementSymbolDefs
  <!-- Wall-placed feature symbol (30×6, designed to sit on a wall) -->
  <symbol id="feat-window" viewBox="0 0 30 6">
    <rect x="0"  y="0" width="30" height="6" fill="white"/>
    <rect x="0"  y="1" width="8"  height="4" fill="#cce" stroke="#99c" stroke-width="0.5"/>
    <rect x="11" y="1" width="8"  height="4" fill="#cce" stroke="#99c" stroke-width="0.5"/>
    <rect x="22" y="1" width="8"  height="4" fill="#cce" stroke="#99c" stroke-width="0.5"/>
  </symbol>

  <!-- ── Builtin furnishing icons — no `import` needed, see BuiltinIcons.scala ── -->
$builtinSymbolDefs
</defs>"""

  /** One `<symbol>` per `BuiltinIcons.movementGlyphs` entry, id'd `feat-<name>`. */
  private def movementSymbolDefs: String =
    BuiltinIcons.movementGlyphs.toList.sortBy(_._1).map { (name, markup) =>
      s"""  <symbol id="feat-$name" viewBox="0 0 30 30">\n$markup\n  </symbol>"""
    }.mkString("\n")

  /** One `<symbol>` per `BuiltinIcons.paths` entry, id'd `builtin-<name>` so `<name>: <value>` works as a
   *  room feature with no `import` statement (see `DslParser.parseRoomFeatures`'s builtin-icon pass). */
  private def builtinSymbolDefs: String =
    BuiltinIcons.paths.toList.sortBy(_._1).map { (name, d) =>
      s"""  <symbol id="builtin-$name" viewBox="${BuiltinIcons.viewBox}"><path fill="#333" d="$d"/></symbol>"""
    }.mkString("\n")

  // ── Background ────────────────────────────────────────────────────────

  private def background(bx: Int, by: Int, bw: Int, bh: Int, backgroundStyle: BackgroundStyle): String =
    backgroundStyle match
      case BackgroundStyle.Hatch =>
        // Carved in rock: hatched stone fill behind the rooms.
        s"""|<rect x="$bx" y="$by" width="$bw" height="$bh" fill="white"/>
            |<rect x="$bx" y="$by" width="$bw" height="$bh" fill="url(#hatch)"/>""".stripMargin
      case BackgroundStyle.Plain | BackgroundStyle.ShadowEdge =>
        // Plain: flat background, matching real floor-plan references.
        // ShadowEdge: also flat here — its hatch band is drawn per-room/per-corridor, see edgeHatchLayer.
        s"""<rect x="$bx" y="$by" width="$bw" height="$bh" fill="white"/>"""

  /** ShadowEdge only: a hatch band straddling each room/corridor's own boundary, drawn *before* the
   *  white floor fill so only the outward-facing half remains visible — a soft halo hugging the wall,
   *  fading to plain white beyond it (rather than Hatch's full-canvas fill). No-op for other styles. */
  private def edgeHatchLayer(map: RenderedMap): String =
    if map.backgroundStyle != BackgroundStyle.ShadowEdge then ""
    else (map.rooms.map(roomEdgeHatch) ++ map.corridors.flatMap(corridorEdgeHatch)).mkString("\n")

  private def roomEdgeHatch(rm: RenderedRoom): String = rm.shape match
    case RoomShape.Rectangular =>
      s"""<rect x="${rm.x.toInt}" y="${rm.y.toInt}" width="${rm.w.toInt}" height="${rm.h.toInt}" fill="none" stroke="url(#hatch)" stroke-width="$GRID"/>"""
    case RoomShape.Circular =>
      val cx = (rm.x + rm.w / 2).toInt; val cy = (rm.y + rm.h / 2).toInt
      val r  = (math.min(rm.w, rm.h) / 2).toInt
      s"""<circle cx="$cx" cy="$cy" r="$r" fill="none" stroke="url(#hatch)" stroke-width="$GRID"/>"""
    case RoomShape.Cave =>
      s"""<path d="${caveOutline(rm)}" fill="none" stroke="url(#hatch)" stroke-width="$GRID"/>"""

  private def corridorEdgeHatch(c: RenderedCorridor): List[String] =
    c.rects.map { r =>
      s"""<rect x="${r.x.toInt}" y="${r.y.toInt}" width="${r.w.toInt}" height="${r.h.toInt}" fill="none" stroke="url(#hatch)" stroke-width="$GRID"/>"""
    }

  // ── Rooms ────────────────────────────────────────────────────────────

  private def roomFloor(rm: RenderedRoom): String = rm.shape match
    case RoomShape.Rectangular =>
      s"""<rect x="${rm.x.toInt}" y="${rm.y.toInt}" width="${rm.w.toInt}" height="${rm.h.toInt}" fill="white"/>"""
    case RoomShape.Circular =>
      val cx = (rm.x + rm.w / 2).toInt; val cy = (rm.y + rm.h / 2).toInt
      val r  = (math.min(rm.w, rm.h) / 2).toInt
      s"""<circle cx="$cx" cy="$cy" r="$r" fill="white"/>"""
    case RoomShape.Cave =>
      s"""<path d="${caveOutline(rm)}" fill="white"/>"""

  /** Grid lines anchored to this room's walls, not global space. */
  private def roomGrid(rm: RenderedRoom): String = rm.shape match
    case RoomShape.Circular => ""
    case RoomShape.Cave     => ""
    case RoomShape.Rectangular =>
      val lines = collection.mutable.ListBuffer[String]()
      var vx = rm.x + GRID
      while vx < rm.x + rm.w do
        lines += s"""<line x1="${vx.toInt}" y1="${rm.y.toInt}" x2="${vx.toInt}" y2="${(rm.y + rm.h).toInt}" stroke="#d8d8d8" stroke-width="0.5"/>"""
        vx += GRID
      var hy = rm.y + GRID
      while hy < rm.y + rm.h do
        lines += s"""<line x1="${rm.x.toInt}" y1="${hy.toInt}" x2="${(rm.x + rm.w).toInt}" y2="${hy.toInt}" stroke="#d8d8d8" stroke-width="0.5"/>"""
        hy += GRID
      lines.mkString("\n")

  private def roomWalls(rm: RenderedRoom): String = rm.shape match
    case RoomShape.Rectangular =>
      s"""<rect x="${rm.x.toInt}" y="${rm.y.toInt}" width="${rm.w.toInt}" height="${rm.h.toInt}" fill="none" stroke="#333" stroke-width="$WALL"/>"""
    case RoomShape.Circular =>
      val cx = (rm.x + rm.w / 2).toInt; val cy = (rm.y + rm.h / 2).toInt
      val r  = (math.min(rm.w, rm.h) / 2).toInt
      s"""<circle cx="$cx" cy="$cy" r="$r" fill="none" stroke="#333" stroke-width="$WALL"/>"""
    case RoomShape.Cave =>
      s"""<path d="${caveOutline(rm)}" fill="none" stroke="#333" stroke-width="$WALL"/>"""

  private val CAVE_POINTS = 10

  /** Deterministic irregular blob outline (soft, rounded — not a jagged polygon),
   *  seeded from the room id so the same room always renders the same shape.
   *  Both the radius and the angle of each point are jittered — radius jitter alone
   *  (at a subtle range) reads as a wobbly ellipse rather than a distinct cave shape. */
  private def caveOutline(rm: RenderedRoom): String =
    val center  = Vec2(rm.x + rm.w / 2, rm.y + rm.h / 2)
    val rx      = rm.w / 2 * 0.9
    val ry      = rm.h / 2 * 0.9
    val rng     = Rng(rm.id.hashCode.toLong)
    val angleJitter = (2 * math.Pi / CAVE_POINTS) * 0.4
    val pts = (0 until CAVE_POINTS).map { i =>
      val baseAngle = 2 * math.Pi * i / CAVE_POINTS
      val angle     = baseAngle + rng.nextDoubleIn(-angleJitter, angleJitter)
      val jitter    = rng.nextDoubleIn(0.55, 1.35)
      center + Vec2(rx * jitter * math.cos(angle), ry * jitter * math.sin(angle))
    }
    val mids  = pts.indices.map(i => (pts(i) + pts((i + 1) % pts.length)) * 0.5)
    val start = mids.last
    val segments = pts.indices.map { i =>
      val p = pts(i); val m = mids(i)
      f"Q ${p.x}%.1f,${p.y}%.1f ${m.x}%.1f,${m.y}%.1f"
    }.mkString(" ")
    f"M ${start.x}%.1f,${start.y}%.1f $segments Z"

  /** Legend style: bold room number inside the room, no text below (see legendBox).
   *  Inline style: no number, label centred in the room. */
  private def roomLabel(rm: RenderedRoom, num: Int, labelStyle: LabelStyle): String =
    val cx = (rm.x + rm.w / 2).toInt
    labelStyle match
      case LabelStyle.Legend =>
        val ny = (rm.y + FONT_SZ + 4).toInt
        s"""<text x="$cx" y="$ny" text-anchor="middle" dominant-baseline="auto" fill="#333" font-size="$FONT_SZ" font-family="sans-serif" font-weight="bold">$num</text>"""
      case LabelStyle.Inline =>
        val cy = (rm.y + rm.h / 2 + FONT_SZ * 0.35).toInt
        s"""<text x="$cx" y="$cy" text-anchor="middle" dominant-baseline="auto" fill="#333" font-size="$FONT_SZ" font-family="sans-serif">${escapeXml(rm.label)}</text>"""

  /** Extra viewBox height needed for the legend box, or 0 if not applicable. */
  private def legendHeight(map: RenderedMap): Double =
    if map.labelStyle != LabelStyle.Legend || map.rooms.isEmpty then 0.0
    else 20.0 + (FONT_SZ + 5) * (map.rooms.length + 1) + 10.0

  /** "N - label" list below the map, keyed to the in-room numbers. Dyson Logos /
   *  One Page Dungeon convention: a numbered legend instead of per-room text. */
  private def legendBox(map: RenderedMap): String =
    if map.labelStyle != LabelStyle.Legend || map.rooms.isEmpty then ""
    else
      val lineH   = FONT_SZ + 5
      val x       = map.minX.toInt
      val yTop    = (map.minY + map.height + 20).toInt
      val heading = s"""<text x="$x" y="$yTop" fill="#333" font-size="${FONT_SZ + 1}" font-family="sans-serif" font-weight="bold">Legend</text>"""
      val lines = map.rooms.zipWithIndex.map { (rm, i) =>
        val y = yTop + lineH * (i + 1)
        s"""<text x="$x" y="$y" fill="#555" font-size="$FONT_SZ" font-family="serif" font-style="italic">${i + 1} - ${escapeXml(rm.label)}</text>"""
      }
      (heading +: lines).mkString("\n")

  // ── Note callouts ───────────────────────────────────────────────────────
  // One Page Dungeon convention: a text box connected by a leader line to the room it annotates,
  // independent of that room's own label (DSL: `note <side> of <room-id>: <text>`).

  private val NOTE_BOX_W        = 160
  private val NOTE_MARGIN       = 30 // gap between a room's wall and its nearest note box
  private val NOTE_GAP          = 10 // gap between stacked note boxes on the same room+side
  private val NOTE_PAD          = 6
  private val NOTE_LINE_H       = FONT_SZ + 4
  private val NOTE_CHARS_PER_LINE = 22

  private case class NoteBox(x: Double, y: Double, w: Double, h: Double, lines: List[String], anchor: Vec2, leaderStart: Vec2)

  /** Greedy word-wrap — callout text is meant to be a longer read-aloud description, not a label. */
  private def wrapText(text: String, maxChars: Int): List[String] =
    val words = text.strip().split("\\s+").filter(_.nonEmpty)
    val lines = collection.mutable.ListBuffer[String]()
    var current = new StringBuilder
    words.foreach { w =>
      if current.isEmpty then current.append(w)
      else if current.length + 1 + w.length <= maxChars then current.append(' ').append(w)
      else
        lines += current.toString
        current = new StringBuilder(w)
    }
    if current.nonEmpty then lines += current.toString
    if lines.isEmpty then List("") else lines.toList

  /** One box per note, stacked along the room's own wall when more than one note shares the same
   *  room+side (grouped/sorted so output is deterministic regardless of map.notes' order). */
  private def noteBoxes(map: RenderedMap): List[NoteBox] =
    map.notes
      .groupBy(n => (n.room.id, n.side))
      .toList
      .sortBy { case ((id, side), _) => (id, side.toString) }
      .flatMap { case (_, notes) =>
        val rm = notes.head.room
        notes.foldLeft((0.0, List.empty[NoteBox])) { case ((offset, acc), n) =>
          val lines = wrapText(n.text, NOTE_CHARS_PER_LINE)
          val boxH  = (NOTE_PAD * 2 + lines.length * NOTE_LINE_H).toDouble
          val boxW  = NOTE_BOX_W.toDouble
          val (bx, by, anchor, leaderStart, advance) = n.side match
            case WallSide.West =>
              val a = Vec2(rm.x, rm.y + rm.h / 2 + offset)
              val x = rm.x - NOTE_MARGIN - boxW
              val y = rm.y + rm.h / 2 - boxH / 2 + offset
              (x, y, a, Vec2(x + boxW, y + boxH / 2), boxH + NOTE_GAP)
            case WallSide.East =>
              val a = Vec2(rm.x + rm.w, rm.y + rm.h / 2 + offset)
              val x = rm.x + rm.w + NOTE_MARGIN
              val y = rm.y + rm.h / 2 - boxH / 2 + offset
              (x, y, a, Vec2(x, y + boxH / 2), boxH + NOTE_GAP)
            case WallSide.North =>
              val a = Vec2(rm.x + rm.w / 2 + offset, rm.y)
              val x = rm.x + rm.w / 2 - boxW / 2 + offset
              val y = rm.y - NOTE_MARGIN - boxH
              (x, y, a, Vec2(x + boxW / 2, y + boxH), boxW + NOTE_GAP)
            case WallSide.South =>
              val a = Vec2(rm.x + rm.w / 2 + offset, rm.y + rm.h)
              val x = rm.x + rm.w / 2 - boxW / 2 + offset
              val y = rm.y + rm.h + NOTE_MARGIN
              (x, y, a, Vec2(x + boxW / 2, y), boxW + NOTE_GAP)
          (offset + advance, acc :+ NoteBox(bx, by, boxW, boxH, lines, anchor, leaderStart))
        }._2
      }

  private def noteCallout(nb: NoteBox): String =
    val leader = s"""<line x1="${nb.leaderStart.x.toInt}" y1="${nb.leaderStart.y.toInt}" x2="${nb.anchor.x.toInt}" y2="${nb.anchor.y.toInt}" stroke="#333" stroke-width="1"/>"""
    val box    = s"""<rect x="${nb.x.toInt}" y="${nb.y.toInt}" width="${nb.w.toInt}" height="${nb.h.toInt}" fill="white" stroke="#333" stroke-width="1"/>"""
    val text = nb.lines.zipWithIndex.map { case (l, i) =>
      val ty = (nb.y + NOTE_PAD + FONT_SZ + i * NOTE_LINE_H).toInt
      s"""<text x="${(nb.x + NOTE_PAD).toInt}" y="$ty" fill="#333" font-size="$FONT_SZ" font-family="serif">${escapeXml(l)}</text>"""
    }.mkString("\n")
    s"$leader\n$box\n$text"

  private def noteCallouts(map: RenderedMap): String =
    noteBoxes(map).map(noteCallout).mkString("\n")

  /** Extends a base viewBox rect to also contain every note box, since callouts can protrude on
   *  any side of the map, unlike the legend (which only ever grows the box downward). */
  private def expandForNotes(map: RenderedMap, bx: Int, by: Int, bw: Int, bh: Int): (Int, Int, Int, Int) =
    val boxes = noteBoxes(map)
    if boxes.isEmpty then (bx, by, bw, bh)
    else
      val minX = boxes.map(_.x).min min bx
      val minY = boxes.map(_.y).min min by
      val maxX = boxes.map(b => b.x + b.w).max max (bx + bw)
      val maxY = boxes.map(b => b.y + b.h).max max (by + bh)
      (minX.toInt, minY.toInt, (maxX - minX).toInt, (maxY - minY).toInt)

  // ── Room features ────────────────────────────────────────────────────

  private def roomFeatures(rm: RenderedRoom, iconFetcher: IconFetcher): List[String] =
    rm.features.flatMap {
      case RoomFeature.Stairs(dir, facing, pos) => List(stairSymbol(rm, dir, facing, pos))
      case RoomFeature.SpiralStairs(dir, pos)   => List(spiralStairs(rm, dir, pos))
      case RoomFeature.Ladder(dir, pos)         => List(centeredSymbol(rm, "feat-ladder", dir, pos))
      case RoomFeature.Icon(iconSet, iconName, size, pos) => List(iconFeature(rm, iconSet, iconName, size, pos, iconFetcher))
      // Window — the only wall opening still a `<use>`-placed direct SVG symbol.
      case RoomFeature.Window(side)       => List(wallUse(rm, side, "feat-window",      30, 6))
    }

  /** Straight stairs: the hand-drawn `feat-stairs-up`/`feat-stairs-down` symbol (see `defs()`),
   *  positioned exactly like any Icon feature (`FeaturePosition`, default centred) and rotated so its
   *  narrow/wall end points toward `facing` (the wall the flight leads toward). */
  private def stairSymbol(rm: RenderedRoom, dir: StairDir, facing: WallSide, position: FeaturePosition): String =
    val bw = GRID; val bh = GRID
    val autoX = rm.x + (rm.w - bw) / 2
    val autoY = rm.y + (rm.h - bh) / 2
    val (x, y) = resolvePosition(rm, bw, bh, position, autoX, autoY)
    val bx = x.toInt; val by = y.toInt
    val cx = bx + bw / 2; val cy = by + bh / 2
    val symbolId = dir match
      case StairDir.Up   => "feat-stairs-up"
      case StairDir.Down => "feat-stairs-down"
    val rotate = facing match
      case WallSide.North => 0
      case WallSide.East  => 90
      case WallSide.South => 180
      case WallSide.West  => 270
    val transform = if rotate != 0 then s""" transform="rotate($rotate,$cx,$cy)"""" else ""
    s"""<use href="#$symbolId" x="$bx" y="$by" width="$bw" height="$bh"$transform/>"""

  /** Spiral staircase: the hand-drawn `feat-spiral-stairs-up`/`-down` symbol (see `defs()`), positioned
   *  exactly like any Icon feature. No `facing` concept (unlike straight `Stairs`), so no rotation. */
  private def spiralStairs(rm: RenderedRoom, dir: StairDir, position: FeaturePosition): String =
    val bw = GRID; val bh = GRID
    val autoX = rm.x + (rm.w - bw) / 2
    val autoY = rm.y + (rm.h - bh) / 2
    val (x, y) = resolvePosition(rm, bw, bh, position, autoX, autoY)
    val bx = x.toInt; val by = y.toInt
    val symbolId = dir match
      case StairDir.Up   => "feat-spiral-stairs-up"
      case StairDir.Down => "feat-spiral-stairs-down"
    s"""<use href="#$symbolId" x="$bx" y="$by" width="$bw" height="$bh"/>"""

  // ── Feature sizing (explicit FeatureSize, min = 1 grid square) ──────────

  private val FEATURE_MARGIN = 4.0

  /** Resolve a free-standing feature's top-left x/y from its FeaturePosition,
   *  falling back to (autoX, autoY) for Auto. `Side` biases toward that edge of
   *  the room's interior; `At` places it at explicit GRID-unit cell coordinates. */
  private def resolvePosition(rm: RenderedRoom, sw: Double, sh: Double, position: FeaturePosition, autoX: Double, autoY: Double): (Double, Double) =
    position match
      case FeaturePosition.Auto => (autoX, autoY)
      case FeaturePosition.Side(side) =>
        side match
          case WallSide.North => (autoX, rm.y + FEATURE_MARGIN)
          case WallSide.South => (autoX, rm.y + rm.h - sh - FEATURE_MARGIN)
          case WallSide.East  => (rm.x + rm.w - sw - FEATURE_MARGIN, autoY)
          case WallSide.West  => (rm.x + FEATURE_MARGIN, autoY)
      case FeaturePosition.At(col, row) =>
        (rm.x + col * GRID, rm.y + row * GRID)

  /** A free-standing feature backed by an icon: centred by default, sized by
   *  FeatureSize, positioned via FeaturePosition exactly like the hardcoded
   *  structural/natural features this replaces. A `BuiltinIcons.iconSetName`
   *  icon uses the matching `<symbol>` already embedded in `defs()` (no
   *  network, no `import` needed); any other `iconSet` is looked up via the
   *  injected `IconFetcher` and embedded as a nested `<svg>` (its own
   *  viewBox, scaled to the feature's box) — falling back to a dashed
   *  placeholder box + the icon's name when it couldn't be fetched (offline,
   *  uncached, unknown name, ...) so a missing icon degrades gracefully
   *  instead of silently rendering nothing. */
  private def iconFeature(rm: RenderedRoom, iconSet: String, iconName: String, size: FeatureSize, position: FeaturePosition, iconFetcher: IconFetcher): String =
    val sw = size.w * GRID
    val sh = size.h * GRID
    val autoX = rm.x + (rm.w - sw) / 2
    val autoY = rm.y + (rm.h - sh) / 2
    val (x, y) = resolvePosition(rm, sw, sh, position, autoX, autoY)
    val bx = x.toInt; val by = y.toInt
    if iconSet == BuiltinIcons.iconSetName then
      s"""<use href="#builtin-$iconName" x="$bx" y="$by" width="$sw" height="$sh"/>"""
    else
      iconFetcher.fetch(iconSet, iconName) match
        case Some(icon) =>
          s"""<svg x="$bx" y="$by" width="$sw" height="$sh" viewBox="${icon.viewBox}">${icon.body}</svg>"""
        case None =>
          val cx = bx + sw / 2; val cy = by + sh / 2
          s"""<rect x="$bx" y="$by" width="$sw" height="$sh" fill="none" stroke="#999" stroke-width="0.8" stroke-dasharray="3,2"/>
<text x="$cx" y="$cy" text-anchor="middle" dominant-baseline="middle" fill="#999" font-size="6" font-family="sans-serif">${escapeXml(iconName)}</text>"""

  /** A GRID×GRID symbol positioned like any Icon feature (used by centeredSymbol for ladders). */
  private def positionedUse(rm: RenderedRoom, id: String, position: FeaturePosition): (String, Double, Double) =
    val autoX = rm.x + (rm.w - GRID) / 2
    val autoY = rm.y + (rm.h - GRID) / 2
    val (x, y) = resolvePosition(rm, GRID, GRID, position, autoX, autoY)
    val bx = x.toInt; val by = y.toInt
    (s"""<use href="#$id" x="$bx" y="$by" width="$GRID" height="$GRID"/>""", x, y)

  /** Positioned symbol with a direction arrow overlay (for ladders). */
  private def centeredSymbol(rm: RenderedRoom, id: String, dir: StairDir, position: FeaturePosition): String =
    val (base, x, y) = positionedUse(rm, id, position)
    val ax = (x + GRID / 2).toInt
    val ay = (y + GRID + 4).toInt
    val arrow = dir match
      case StairDir.Up   => s"""<polygon points="$ax,${ay-8} ${ax-4},$ay ${ax+4},$ay" fill="#333"/>"""
      case StairDir.Down => s"""<polygon points="$ax,${ay} ${ax-4},${ay-8} ${ax+4},${ay-8}" fill="#333"/>"""
    s"$base\n$arrow"

  /** Place a symbol on a wall, centred, with correct rotation. */
  private def wallUse(rm: RenderedRoom, side: WallSide, id: String, sw: Int, sh: Int): String =
    // Default orientation: symbol sits on top of south wall (opening faces into room = north)
    // Rotate to match other sides.
    val (x, y, rotate, rx, ry) = side match
      case WallSide.South =>
        val x = (rm.x + rm.w / 2 - sw / 2).toInt
        val y = (rm.y + rm.h - sh).toInt
        (x, y, 0, 0, 0)
      case WallSide.North =>
        val x = (rm.x + rm.w / 2 - sw / 2).toInt
        val y = rm.y.toInt
        (x, y, 180, (rm.x + rm.w / 2).toInt, (rm.y + sh / 2).toInt)
      case WallSide.East =>
        val x = (rm.x + rm.w - sh).toInt
        val y = (rm.y + rm.h / 2 - sw / 2).toInt
        (x, y, 90, (rm.x + rm.w - sh / 2).toInt, (rm.y + rm.h / 2).toInt)
      case WallSide.West =>
        val x = rm.x.toInt
        val y = (rm.y + rm.h / 2 - sw / 2).toInt
        (x, y, -90, (rm.x + sh / 2).toInt, (rm.y + rm.h / 2).toInt)
    val transform = if rotate != 0 then s""" transform="rotate($rotate,$rx,$ry)"""" else ""
    s"""<use href="#$id" x="$x" y="$y" width="$sw" height="$sh"$transform/>"""

  // ── Corridors ────────────────────────────────────────────────────────

  private def corridorFloors(c: RenderedCorridor): List[String] =
    c.rects.map { r =>
      s"""<rect x="${r.x.toInt}" y="${r.y.toInt}" width="${r.w.toInt}" height="${r.h.toInt}" fill="white"/>"""
    }

  /** Grid lines anchored to each corridor rect's own top-left — same treatment as roomGrid. */
  private def corridorGrid(c: RenderedCorridor): List[String] =
    c.rects.flatMap { r =>
      val lines = collection.mutable.ListBuffer[String]()
      var vx = r.x + GRID
      while vx < r.x + r.w do
        lines += s"""<line x1="${vx.toInt}" y1="${r.y.toInt}" x2="${vx.toInt}" y2="${(r.y + r.h).toInt}" stroke="#d8d8d8" stroke-width="0.5"/>"""
        vx += GRID
      var hy = r.y + GRID
      while hy < r.y + r.h do
        lines += s"""<line x1="${r.x.toInt}" y1="${hy.toInt}" x2="${(r.x + r.w).toInt}" y2="${hy.toInt}" stroke="#d8d8d8" stroke-width="0.5"/>"""
        hy += GRID
      lines.toList
    }

  private def corridorWalls(c: RenderedCorridor): List[String] =
    c.rects.flatMap { r =>
      if r.isHorizontal then
        List(
          s"""<line x1="${r.x.toInt}" y1="${r.y.toInt}" x2="${(r.x+r.w).toInt}" y2="${r.y.toInt}" stroke="#333" stroke-width="$WALL"/>""",
          s"""<line x1="${r.x.toInt}" y1="${(r.y+r.h).toInt}" x2="${(r.x+r.w).toInt}" y2="${(r.y+r.h).toInt}" stroke="#333" stroke-width="$WALL"/>""",
        )
      else
        List(
          s"""<line x1="${r.x.toInt}" y1="${r.y.toInt}" x2="${r.x.toInt}" y2="${(r.y+r.h).toInt}" stroke="#333" stroke-width="$WALL"/>""",
          s"""<line x1="${(r.x+r.w).toInt}" y1="${r.y.toInt}" x2="${(r.x+r.w).toInt}" y2="${(r.y+r.h).toInt}" stroke="#333" stroke-width="$WALL"/>""",
        )
    }

  // ── Doors ────────────────────────────────────────────────────────────

  private def door(d: RenderedDoor): String =
    val symbolId = d.doorType match
      case DoorType.Open   => "door-open"
      case DoorType.Closed => "door-closed"
      case DoorType.Locked => "door-locked"
      case DoorType.Secret => "door-secret"
    // Secret doors always keep the flat blend-into-wall look — showing a swing
    // arc would defeat the point of a hidden door.
    if d.doorType == DoorType.Secret || d.swing == DoorSwing.Default then
      val dw = d.width.toInt
      val ux = (d.position.x - dw / 2).toInt
      val uy = (d.position.y - 15).toInt
      val px = d.position.x.toInt
      val py = d.position.y.toInt
      // preserveAspectRatio="none": stretch the (square) symbol to the corridor's actual width
      // so the door gap fully spans the passage instead of being letterboxed at 30px.
      val use = s"""<use href="#$symbolId" x="$ux" y="$uy" width="$dw" height="30" preserveAspectRatio="none" transform="rotate(${d.angle.toInt},$px,$py)"/>"""
      if d.doorType == DoorType.Secret then s"$use\n${secretMarker(d)}" else use
    else
      doorSwingArc(d)

  /** Small "S" above the dashed line, marking a secret door — offset clear of the
   *  line regardless of whether the wall (and so the line) is horizontal or vertical. */
  private def secretMarker(d: RenderedDoor): String =
    val margin = 6.0
    val sy = if d.angle == 90.0 then d.position.y - d.width / 2 - margin else d.position.y - margin
    s"""<text x="${d.position.x.toInt}" y="${sy.toInt}" text-anchor="middle" dominant-baseline="middle" fill="#888" font-size="8" font-family="sans-serif" font-weight="bold">S</text>"""

  /** Architectural door symbol: a leaf line + swing arc, swinging into or away
   *  from this door's own room. The leaf is drawn at a 45° opening angle (not a
   *  full 90° quarter-turn), so the arc sweeps 45° too. Computed directly in
   *  world coordinates from the door's position/width/angle/intoRoomSign — no
   *  rotation-frame math needed, unlike the flat `<use>`-based symbols above. */
  private def doorSwingArc(d: RenderedDoor): String =
    val half     = d.width / 2
    val vertical = d.angle == 90.0 // wall is vertical (E/W wall); gap runs along Y, leaf swings along X
    val hinge = if vertical then Vec2(d.position.x, d.position.y - half) else Vec2(d.position.x - half, d.position.y)
    val jamb2 = if vertical then Vec2(d.position.x, d.position.y + half) else Vec2(d.position.x + half, d.position.y)
    val dirSign = if d.swing == DoorSwing.Inside then d.intoRoomSign else -d.intoRoomSign
    val along   = math.cos(math.Pi / 4) * d.width // toward jamb2 (closed-leaf direction)
    val perp    = math.sin(math.Pi / 4) * d.width // toward intoRoomSign (open-leaf direction)
    val leafEnd =
      if vertical then Vec2(hinge.x + dirSign * perp, hinge.y + along)
      else Vec2(hinge.x + along, hinge.y + dirSign * perp)
    val cross   = (leafEnd.x - hinge.x) * (jamb2.y - hinge.y) - (leafEnd.y - hinge.y) * (jamb2.x - hinge.x)
    val sweep   = if cross > 0 then 1 else 0

    val gap =
      if vertical then f"""<rect x="${d.position.x - 2}%.1f" y="${d.position.y - half}%.1f" width="4" height="${d.width}%.1f" fill="white"/>"""
      else f"""<rect x="${d.position.x - half}%.1f" y="${d.position.y - 2}%.1f" width="${d.width}%.1f" height="4" fill="white"/>"""
    val leaf = f"""<line x1="${hinge.x}%.1f" y1="${hinge.y}%.1f" x2="${leafEnd.x}%.1f" y2="${leafEnd.y}%.1f" stroke="#333" stroke-width="1.2"/>"""
    val arc  = f"""<path d="M ${leafEnd.x}%.1f,${leafEnd.y}%.1f A ${d.width}%.1f,${d.width}%.1f 0 0,$sweep ${jamb2.x}%.1f,${jamb2.y}%.1f" fill="none" stroke="#333" stroke-width="0.8"/>"""
    s"$gap\n$leaf\n$arc"

  private def escapeXml(s: String): String =
    s.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;").replace("\"", "&quot;")
