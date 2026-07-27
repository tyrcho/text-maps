package textmaps.render

import textmaps.dsl.{DoorSwing, DoorType, FeaturePosition, FeatureSize, LabelStyle, MapType, RoomFeature, RoomShape, RoomSize, StairDir, WallSide}
import textmaps.generate.Rng
import textmaps.layout.*

/** Renders a RenderedMap as a self-contained SVG string.
 *
 *  Visual style: Dyson Logos / One Page Dungeon
 *  - Dense diagonal cross-hatching fills all rock/exterior areas
 *  - White floor rectangles for rooms and corridors
 *  - Floor grid anchored to each shape's own top-left (not global coordinates),
 *    for rooms and corridors alike
 *  - Dark ink wall strokes
 *  - Stairs: box with tapering step bars, rotated to face the wall the flight
 *    leads toward, with a small "UP"/"DN" label for direction of travel
 *  - Windows: small opening gap on the wall
 *  - Doors: flat gap glyph by default; an architectural leaf + swing arc when
 *    `swing` is Inside/Outside
 *  - Room number inside (bold, centred); room name below the room
 */
object SvgStringRenderer:

  private val GRID    = 30
  private val WALL    = 1.5
  private val FONT_SZ = 9

  def render(map: RenderedMap): String =
    val pad = 60.0
    val vx  = (map.minX - pad).toInt
    val vy  = (map.minY - pad).toInt
    val vw  = (map.width  + pad * 2).toInt
    val vh  = (map.height + pad * 2 + legendHeight(map)).toInt
    s"""|<?xml version="1.0" encoding="UTF-8"?>
        |<svg xmlns="http://www.w3.org/2000/svg"
        |     viewBox="$vx $vy $vw $vh"
        |     width="$vw" height="$vh">
        |${defs()}
        |${background(vx, vy, vw, vh, map.mapType)}
        |${map.corridors.flatMap(corridorFloors).mkString("\n")}
        |${map.rooms.map(roomFloor).mkString("\n")}
        |${map.corridors.flatMap(corridorWalls).mkString("\n")}
        |${map.rooms.map(roomWalls).mkString("\n")}
        |${map.rooms.map(roomGrid).mkString("\n")}
        |${map.corridors.flatMap(corridorGrid).mkString("\n")}
        |${map.rooms.flatMap(roomFeatures).mkString("\n")}
        |${map.doors.map(door).mkString("\n")}
        |${map.rooms.zipWithIndex.map { (rm, i) => roomLabel(rm, i + 1, map.labelStyle) }.mkString("\n")}
        |${legendBox(map)}
        |</svg>""".stripMargin

  def renderInner(map: RenderedMap): String =
    val pad = 60.0
    val vx  = (map.minX - pad).toInt
    val vy  = (map.minY - pad).toInt
    val vw  = (map.width  + pad * 2).toInt
    val vh  = (map.height + pad * 2 + legendHeight(map)).toInt
    val viewBoxAttr = s"$vx $vy $vw $vh"
    s"""|__VIEWBOX__${viewBoxAttr}__END__
        |${defs()}
        |${background(vx, vy, vw, vh, map.mapType)}
        |${map.corridors.flatMap(corridorFloors).mkString("\n")}
        |${map.rooms.map(roomFloor).mkString("\n")}
        |${map.corridors.flatMap(corridorWalls).mkString("\n")}
        |${map.rooms.map(roomWalls).mkString("\n")}
        |${map.rooms.map(roomGrid).mkString("\n")}
        |${map.corridors.flatMap(corridorGrid).mkString("\n")}
        |${map.rooms.flatMap(roomFeatures).mkString("\n")}
        |${map.doors.map(door).mkString("\n")}
        |${map.rooms.zipWithIndex.map { (rm, i) => roomLabel(rm, i + 1, map.labelStyle) }.mkString("\n")}
        |${legendBox(map)}""".stripMargin

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

  <!-- ── Room feature symbols (viewBox 0 0 30 30, one grid cell) ── -->
  <symbol id="feat-ladder" viewBox="0 0 30 30">
    <line x1="8"  y1="3"  x2="8"  y2="27" stroke="#333" stroke-width="1.5"/>
    <line x1="22" y1="3"  x2="22" y2="27" stroke="#333" stroke-width="1.5"/>
    <line x1="8"  y1="7"  x2="22" y2="7"  stroke="#333" stroke-width="1"/>
    <line x1="8"  y1="13" x2="22" y2="13" stroke="#333" stroke-width="1"/>
    <line x1="8"  y1="19" x2="22" y2="19" stroke="#333" stroke-width="1"/>
    <line x1="8"  y1="25" x2="22" y2="25" stroke="#333" stroke-width="1"/>
  </symbol>
  <symbol id="feat-pillar" viewBox="0 0 30 30">
    <circle cx="15" cy="15" r="10" fill="#666" stroke="#333" stroke-width="1"/>
    <circle cx="15" cy="15" r="7"  fill="#888"/>
  </symbol>
  <symbol id="feat-statue" viewBox="0 0 30 30">
    <polygon points="15,3 27,15 15,27 3,15" fill="#ccc" stroke="#333" stroke-width="1.2"/>
    <line x1="15" y1="7"  x2="15" y2="23" stroke="#333" stroke-width="0.8"/>
    <line x1="7"  y1="15" x2="23" y2="15" stroke="#333" stroke-width="0.8"/>
  </symbol>
  <symbol id="feat-pool" viewBox="0 0 30 30">
    <rect x="2" y="2" width="26" height="26" rx="8" fill="white" stroke="#99c" stroke-width="1"/>
    <path d="M 6 10 Q 10 7 14 10 Q 18 13 22 10" fill="none" stroke="#99c" stroke-width="1.2"/>
    <path d="M 6 16 Q 10 13 14 16 Q 18 19 22 16" fill="none" stroke="#99c" stroke-width="1.2"/>
    <path d="M 6 22 Q 10 19 14 22 Q 18 25 22 22" fill="none" stroke="#99c" stroke-width="1.2"/>
  </symbol>
  <symbol id="feat-stream" viewBox="0 0 30 30">
    <path d="M 0 12 Q 5 8 10 14 Q 15 20 20 14 Q 25 8 30 12"
          fill="none" stroke="#99c" stroke-width="1.8"/>
    <path d="M 0 18 Q 5 14 10 20 Q 15 26 20 20 Q 25 14 30 18"
          fill="none" stroke="#99c" stroke-width="1.8"/>
  </symbol>
  <symbol id="feat-stalactite" viewBox="0 0 30 30">
    <polygon points="7,2  4,16  10,16"  fill="#666"/>
    <polygon points="15,2 12,13  18,13" fill="#666"/>
    <polygon points="23,2 20,18  26,18" fill="#666"/>
  </symbol>
  <symbol id="feat-stalagmite" viewBox="0 0 30 30">
    <polygon points="7,28  4,14  10,14"  fill="#666"/>
    <polygon points="15,28 12,17  18,17" fill="#666"/>
    <polygon points="23,28 20,12  26,12" fill="#666"/>
  </symbol>
  <symbol id="feat-crevasse" viewBox="0 0 30 30">
    <polygon points="0,15 5,10 9,16 13,8 17,18 21,9 25,16 30,15 30,20 25,20 21,14 17,23 13,13 9,21 5,15 0,20"
             fill="#555" stroke="#333" stroke-width="0.5"/>
  </symbol>

  <!-- Wall-placed feature symbols (30×30, designed to sit on a wall) -->
  <symbol id="feat-window" viewBox="0 0 30 6">
    <rect x="0"  y="0" width="30" height="6" fill="white"/>
    <rect x="0"  y="1" width="8"  height="4" fill="#cce" stroke="#99c" stroke-width="0.5"/>
    <rect x="11" y="1" width="8"  height="4" fill="#cce" stroke="#99c" stroke-width="0.5"/>
    <rect x="22" y="1" width="8"  height="4" fill="#cce" stroke="#99c" stroke-width="0.5"/>
  </symbol>
  <symbol id="feat-arrow-slit" viewBox="0 0 30 6">
    <rect x="0"  y="0" width="30" height="6" fill="white"/>
    <rect x="12" y="1" width="6"  height="4" fill="#555"/>
  </symbol>
  <symbol id="feat-illusory-wall" viewBox="0 0 30 4">
    <line x1="0" y1="2" x2="30" y2="2"
          stroke="#888" stroke-width="1.5" stroke-dasharray="4,3"/>
  </symbol>
  <symbol id="feat-fireplace" viewBox="0 0 30 20">
    <rect x="0"  y="0"  width="30" height="20" fill="#555" stroke="#333" stroke-width="1"/>
    <path d="M 7 19 L 7 8 A 8 8 0 0 1 23 8 L 23 19 Z" fill="white"/>
    <path d="M 11 17 Q 15 9 19 17" fill="none" stroke="#c63" stroke-width="1.5"/>
  </symbol>
  <symbol id="feat-bed" viewBox="0 0 30 24">
    <rect x="1"  y="1"  width="28" height="22" fill="white" stroke="#333" stroke-width="1"/>
    <rect x="1"  y="1"  width="28" height="7"  fill="#ddd" stroke="#333" stroke-width="0.5"/>
    <circle cx="8"  cy="5" r="3" fill="white" stroke="#333" stroke-width="0.5"/>
    <circle cx="22" cy="5" r="3" fill="white" stroke="#333" stroke-width="0.5"/>
  </symbol>
  <symbol id="feat-curtain" viewBox="0 0 30 16">
    <line x1="0" y1="2" x2="30" y2="2" stroke="#333" stroke-width="1.5"/>
    <path d="M 0 2 Q 3 14 6 8 Q 9 2 12 14 Q 15 2 18 14 Q 21 2 24 14 Q 27 2 30 8"
          fill="none" stroke="#333" stroke-width="1.2"/>
  </symbol>
</defs>"""

  // ── Background ────────────────────────────────────────────────────────

  private def background(bx: Int, by: Int, bw: Int, bh: Int, mapType: MapType): String =
    mapType match
      case MapType.Dungeon =>
        // Carved in rock: hatched stone fill behind the rooms.
        s"""|<rect x="$bx" y="$by" width="$bw" height="$bh" fill="white"/>
            |<rect x="$bx" y="$by" width="$bw" height="$bh" fill="url(#hatch)"/>""".stripMargin
      case MapType.Building =>
        // Constructed: plain background, matching real floor-plan references.
        s"""<rect x="$bx" y="$by" width="$bw" height="$bh" fill="white"/>"""

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

  // ── Room features ────────────────────────────────────────────────────

  private def roomFeatures(rm: RenderedRoom): List[String] =
    rm.features.flatMap {
      case RoomFeature.Stairs(dir, facing) => List(stairHatch(rm, dir, facing))
      case RoomFeature.SpiralStairs(dir)  => List(spiralStairs(rm, dir))
      case RoomFeature.Ladder(dir)        => List(centeredSymbol(rm, "feat-ladder", dir))
      case RoomFeature.Pillar(size, pos)     => List(sizedCentered(rm, "feat-pillar",     size, position = pos))
      case RoomFeature.Statue(size, pos)     => List(sizedCentered(rm, "feat-statue",     size, position = pos))
      case RoomFeature.Pool(size, pos)       => List(sizedCentered(rm, "feat-pool",       size, position = pos))
      case RoomFeature.Crevasse(size, pos)   => List(sizedCentered(rm, "feat-crevasse",   size, slimH = true, position = pos))
      case RoomFeature.Stream(size, pos)     => List(sizedStream(rm, size, pos))
      case RoomFeature.Stalactite(size, pos) => List(sizedCeiling(rm, "feat-stalactite", size, pos))
      case RoomFeature.Stalagmite(size, pos) => List(sizedFloor(rm,  "feat-stalagmite",  size, pos))
      // Wall features — use `<use>` placed on the wall
      case RoomFeature.Window(side)       => List(wallUse(rm, side, "feat-window",      30, 6))
      case RoomFeature.ArrowSlit(side)    => List(wallUse(rm, side, "feat-arrow-slit",  30, 6))
      case RoomFeature.IllusoryWall(side) => List(wallUse(rm, side, "feat-illusory-wall", 30, 4))
      case RoomFeature.Fireplace(side)    => List(wallUse(rm, side, "feat-fireplace",   30, 20))
      case RoomFeature.Bed(side)          => List(wallUse(rm, side, "feat-bed",         30, 24))
      case RoomFeature.Curtain(side)      => List(wallUse(rm, side, "feat-curtain",     30, 16))
    }

  /** Bordered box with tapering horizontal step bars (narrow near the top, wide
   *  near the bottom) — "steps receding into the distance". Rotated so the narrow
   *  end points toward `facing` (the wall the stairs lead toward); an "UP"/"DN"
   *  label (no arrow) shows direction of travel. */
  private def stairHatch(rm: RenderedRoom, dir: StairDir, facing: WallSide): String =
    val bw = GRID; val bh = GRID
    val bx = (rm.x + (rm.w - bw) / 2).toInt
    val by = (rm.y + (rm.h - bh) / 2).toInt
    val cx = bx + bw / 2.0
    val cy = by + bh / 2.0
    val box = s"""<rect x="$bx" y="$by" width="$bw" height="$bh" fill="white" stroke="#333" stroke-width="0.8"/>"""
    val steps  = 5
    val stepGap = bh.toDouble / (steps + 1)
    val bars = (1 to steps).map { i =>
      val y         = by + stepGap * i
      val widthFrac = 0.28 + 0.13 * i
      val halfW     = bw * widthFrac / 2
      f"""<line x1="${cx - halfW}%.1f" y1="$y%.1f" x2="${cx + halfW}%.1f" y2="$y%.1f" stroke="#333" stroke-width="1.4"/>"""
    }.mkString("\n")
    val rotate = facing match
      case WallSide.North => 0
      case WallSide.East  => 90
      case WallSide.South => 180
      case WallSide.West  => 270
    val group = s"""<g transform="rotate($rotate,${cx.toInt},${cy.toInt})">$box\n$bars</g>"""
    val label = dir match
      case StairDir.Up   => "UP"
      case StairDir.Down => "DN"
    val ly = (by + bh + 9).toInt
    val text = s"""<text x="${cx.toInt}" y="$ly" text-anchor="middle" fill="#333" font-size="7" font-family="sans-serif" font-weight="bold">$label</text>"""
    s"$group\n$text"

  /** Spiral staircase: circular arrow in a box. */
  private def spiralStairs(rm: RenderedRoom, dir: StairDir): String =
    val bw = GRID; val bh = GRID
    val bx = (rm.x + (rm.w - bw) / 2).toInt
    val by = (rm.y + (rm.h - bh) / 2).toInt
    val cx = bx + bw / 2; val cy = by + bh / 2
    val box = s"""<rect x="$bx" y="$by" width="$bw" height="$bh" fill="white" stroke="#333" stroke-width="0.8"/>"""
    // Outer arc (270°) + inner arc (180°) to suggest a spiral
    val (sweep1, sweep2, tipDx, tipDy) = dir match
      case StairDir.Up   => (1, 1,  0, -5)
      case StairDir.Down => (0, 0,  0,  5)
    val outer = s"""<path d="M $cx,${cy-10} A 10,10 0 1,$sweep1 ${cx-10},$cy" fill="none" stroke="#333" stroke-width="1.2"/>"""
    val inner = s"""<path d="M ${cx-10},$cy A 5,5 0 1,$sweep2 $cx,${cy-5}" fill="none" stroke="#333" stroke-width="0.8"/>"""
    val tip   = s"""<polygon points="${cx-10},$cy ${cx-10+tipDx-3},${cy+tipDy} ${cx-10+tipDx+3},${cy+tipDy}" fill="#333"/>"""
    s"$box\n$outer\n$inner\n$tip"

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

  /** Pool, pillar, statue, crevasse: centred by default, sized by FeatureSize.
   *  slimH=true fixes height at 1 GRID (crevasse is a horizontal band). */
  private def sizedCentered(rm: RenderedRoom, id: String, size: FeatureSize, slimH: Boolean = false, position: FeaturePosition = FeaturePosition.Auto): String =
    val sw = size.w * GRID
    val sh = if slimH then GRID else size.h * GRID
    val autoX = rm.x + (rm.w - sw) / 2
    val autoY = rm.y + (rm.h - sh) / 2
    val (x, y) = resolvePosition(rm, sw, sh, position, autoX, autoY)
    s"""<use href="#$id" x="${x.toInt}" y="${y.toInt}" width="$sw" height="$sh"/>"""

  /** Stalactite: centred horizontally, anchored to ceiling by default. */
  private def sizedCeiling(rm: RenderedRoom, id: String, size: FeatureSize, position: FeaturePosition = FeaturePosition.Auto): String =
    val sw = size.w * GRID
    val sh = size.h * GRID
    val autoX = rm.x + (rm.w - sw) / 2
    val autoY = rm.y
    val (x, y) = resolvePosition(rm, sw, sh, position, autoX, autoY)
    s"""<use href="#$id" x="${x.toInt}" y="${y.toInt}" width="$sw" height="$sh"/>"""

  /** Stalagmite: centred horizontally, anchored to floor by default. */
  private def sizedFloor(rm: RenderedRoom, id: String, size: FeatureSize, position: FeaturePosition = FeaturePosition.Auto): String =
    val sw = size.w * GRID
    val sh = size.h * GRID
    val autoX = rm.x + (rm.w - sw) / 2
    val autoY = rm.y + rm.h - sh
    val (x, y) = resolvePosition(rm, sw, sh, position, autoX, autoY)
    s"""<use href="#$id" x="${x.toInt}" y="${y.toInt}" width="$sw" height="$sh"/>"""

  /** Stream: always spans full room width; size.h controls band height.
   *  Position only affects the vertical placement of the band (north/south bias
   *  or an explicit row); east/west and column have no effect on a full-width feature. */
  private def sizedStream(rm: RenderedRoom, size: FeatureSize, position: FeaturePosition = FeaturePosition.Auto): String =
    val sw  = rm.w.toInt
    val sh  = size.h * GRID
    val x   = rm.x.toInt
    val autoY1 = rm.y + (rm.h - sh) / 2
    val y1 = (position match
      case FeaturePosition.Auto                         => autoY1
      case FeaturePosition.Side(WallSide.North)          => rm.y + FEATURE_MARGIN
      case FeaturePosition.Side(WallSide.South)          => rm.y + rm.h - sh - FEATURE_MARGIN
      case FeaturePosition.Side(_)                       => autoY1
      case FeaturePosition.At(_, row)                    => rm.y + row * GRID
    ).toInt
    val y2  = y1 + sh / 2
    val mx  = (rm.x + rm.w / 2).toInt
    val amp = math.min(sh / 4, 12)
    s"""<path d="M $x,$y1 Q $mx,${y1-amp} ${x+sw},$y1" fill="none" stroke="#99c" stroke-width="1.8"/>
<path d="M $x,$y2 Q $mx,${y2+amp} ${x+sw},$y2" fill="none" stroke="#99c" stroke-width="1.8"/>"""

  /** A centred <use> of a GRID×GRID symbol (used by centeredSymbol for ladders). */
  private def centeredUse(rm: RenderedRoom, id: String): String =
    val x = (rm.x + (rm.w - GRID) / 2).toInt
    val y = (rm.y + (rm.h - GRID) / 2).toInt
    s"""<use href="#$id" x="$x" y="$y" width="$GRID" height="$GRID"/>"""

  /** Centred symbol with a direction arrow overlay (for ladders). */
  private def centeredSymbol(rm: RenderedRoom, id: String, dir: StairDir): String =
    val base = centeredUse(rm, id)
    val ax = (rm.x + rm.w / 2).toInt
    val ay = (rm.y + rm.h / 2 + GRID / 2 + 4).toInt
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
      s"""<use href="#$symbolId" x="$ux" y="$uy" width="$dw" height="30" preserveAspectRatio="none" transform="rotate(${d.angle.toInt},$px,$py)"/>"""
    else
      doorSwingArc(d)

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
