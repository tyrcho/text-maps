package textmaps.render

import textmaps.dsl.{DoorType, RoomFeature, RoomShape, StairDir, WallSide}
import textmaps.layout.*

/** Renders a RenderedMap as a self-contained SVG string.
 *
 *  Visual style: Dyson Logos / One Page Dungeon
 *  - Dense diagonal cross-hatching fills all rock/exterior areas
 *  - White floor rectangles for rooms and corridors
 *  - Floor grid anchored to each room's walls (not global coordinates)
 *  - Dark ink wall strokes; corridors have no grid
 *  - Stairs: hatched box with direction arrow
 *  - Windows: small opening gap on the wall
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
    val vh  = (map.height + pad * 2).toInt
    s"""|<?xml version="1.0" encoding="UTF-8"?>
        |<svg xmlns="http://www.w3.org/2000/svg"
        |     viewBox="$vx $vy $vw $vh"
        |     width="$vw" height="$vh">
        |${defs()}
        |${background(vx, vy, vw, vh)}
        |${map.corridors.flatMap(corridorFloors).mkString("\n")}
        |${map.rooms.map(roomFloor).mkString("\n")}
        |${map.corridors.flatMap(corridorWalls).mkString("\n")}
        |${map.rooms.map(roomWalls).mkString("\n")}
        |${map.rooms.map(roomGrid).mkString("\n")}
        |${map.rooms.flatMap(roomFeatures).mkString("\n")}
        |${map.doors.map(door).mkString("\n")}
        |${map.rooms.zipWithIndex.map { (rm, i) => roomLabel(rm, i + 1) }.mkString("\n")}
        |</svg>""".stripMargin

  def renderInner(map: RenderedMap): String =
    val pad = 60.0
    val vx  = (map.minX - pad).toInt
    val vy  = (map.minY - pad).toInt
    val vw  = (map.width  + pad * 2).toInt
    val vh  = (map.height + pad * 2).toInt
    val viewBoxAttr = s"$vx $vy $vw $vh"
    s"""|__VIEWBOX__${viewBoxAttr}__END__
        |${defs()}
        |${background(vx, vy, vw, vh)}
        |${map.corridors.flatMap(corridorFloors).mkString("\n")}
        |${map.rooms.map(roomFloor).mkString("\n")}
        |${map.corridors.flatMap(corridorWalls).mkString("\n")}
        |${map.rooms.map(roomWalls).mkString("\n")}
        |${map.rooms.map(roomGrid).mkString("\n")}
        |${map.rooms.flatMap(roomFeatures).mkString("\n")}
        |${map.doors.map(door).mkString("\n")}
        |${map.rooms.zipWithIndex.map { (rm, i) => roomLabel(rm, i + 1) }.mkString("\n")}""".stripMargin

  // ── <defs> ────────────────────────────────────────────────────────────

  private def defs(): String = s"""<defs>
  <pattern id="hatch" width="6" height="6"
           patternTransform="rotate(45 0 0)" patternUnits="userSpaceOnUse">
    <line x1="0" y1="0" x2="0" y2="6" stroke="#888" stroke-width="1.2"/>
  </pattern>
  <symbol id="door-open" viewBox="0 0 30 30">
    <rect x="0"  y="12" width="30" height="6" fill="white"/>
    <rect x="0"  y="11" width="4"  height="8" fill="#333"/>
    <rect x="26" y="11" width="4"  height="8" fill="#333"/>
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
  <symbol id="door-barred" viewBox="0 0 30 30">
    <rect x="0"  y="12" width="30" height="6" fill="white"/>
    <rect x="2"  y="11" width="26" height="8" fill="#555"/>
    <line x1="8"  y1="11" x2="8"  y2="19" stroke="white" stroke-width="2"/>
    <line x1="15" y1="11" x2="15" y2="19" stroke="white" stroke-width="2"/>
    <line x1="22" y1="11" x2="22" y2="19" stroke="white" stroke-width="2"/>
  </symbol>
</defs>"""

  // ── Background ────────────────────────────────────────────────────────

  private def background(bx: Int, by: Int, bw: Int, bh: Int): String =
    s"""|<rect x="$bx" y="$by" width="$bw" height="$bh" fill="white"/>
        |<rect x="$bx" y="$by" width="$bw" height="$bh" fill="url(#hatch)"/>""".stripMargin

  // ── Rooms ────────────────────────────────────────────────────────────

  private def roomFloor(rm: RenderedRoom): String = rm.shape match
    case RoomShape.Rectangular =>
      s"""<rect x="${rm.x.toInt}" y="${rm.y.toInt}" width="${rm.w.toInt}" height="${rm.h.toInt}" fill="white"/>"""
    case RoomShape.Circular =>
      val cx = (rm.x + rm.w / 2).toInt; val cy = (rm.y + rm.h / 2).toInt
      val r  = (math.min(rm.w, rm.h) / 2).toInt
      s"""<circle cx="$cx" cy="$cy" r="$r" fill="white"/>"""

  /** Grid lines anchored to this room's walls, not global space. */
  private def roomGrid(rm: RenderedRoom): String = rm.shape match
    case RoomShape.Circular => ""
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

  /** Room number (bold, inside) + label text (italic, below room). */
  private def roomLabel(rm: RenderedRoom, num: Int): String =
    val cx = (rm.x + rm.w / 2).toInt
    // Number at top-centre of room interior
    val ny = (rm.y + FONT_SZ + 4).toInt
    val numEl = s"""<text x="$cx" y="$ny" text-anchor="middle" dominant-baseline="auto" fill="#333" font-size="$FONT_SZ" font-family="sans-serif" font-weight="bold">$num</text>"""
    // Label just below the room's bottom wall
    val ly = (rm.y + rm.h + FONT_SZ + 3).toInt
    val labelEl = s"""<text x="$cx" y="$ly" text-anchor="middle" dominant-baseline="auto" fill="#555" font-size="${FONT_SZ - 1}" font-family="serif" font-style="italic">${escapeXml(rm.label)}</text>"""
    s"$numEl\n$labelEl"

  // ── Room features (stairs, windows) ──────────────────────────────────

  private def roomFeatures(rm: RenderedRoom): List[String] =
    rm.features.flatMap {
      case RoomFeature.Stairs(dir)   => List(stairSymbol(rm, dir))
      case RoomFeature.Window(side)  => List(windowSymbol(rm, side))
      case _                         => Nil
    }

  /** Hatched box with direction arrow — centred in the room. */
  private def stairSymbol(rm: RenderedRoom, dir: StairDir): String =
    val bw = GRID
    val bh = GRID
    val bx = (rm.x + (rm.w - bw) / 2).toInt
    val by = (rm.y + (rm.h - bh) / 2).toInt
    val clipId = s"sc${rm.id.filter(_.isLetterOrDigit)}"

    val clip = s"""<clipPath id="$clipId"><rect x="$bx" y="$by" width="$bw" height="$bh"/></clipPath>"""
    val box  = s"""<rect x="$bx" y="$by" width="$bw" height="$bh" fill="white" stroke="#333" stroke-width="0.8"/>"""

    // 45° diagonal hatch lines (bottom-left to top-right)
    val spacing = 5
    val hatchLines = (-bh to bw + bh by spacing).map { d =>
      s"""  <line x1="${bx + d}" y1="${by + bh}" x2="${bx + d + bh}" y2="$by" stroke="#333" stroke-width="0.7"/>"""
    }.mkString("\n")
    val hatch = s"""<g clip-path="url(#$clipId)">\n$hatchLines\n</g>"""

    // Arrow indicating direction (small filled triangle)
    val ax = bx + bw / 2
    val arrow = dir match
      case StairDir.Up =>
        val ty = by + 4; val by2 = by + bh - 4
        s"""<polygon points="$ax,$ty ${ax - 5},$by2 ${ax + 5},$by2" fill="#333"/>"""
      case StairDir.Down =>
        val ty = by + bh - 4; val by2 = by + 4
        s"""<polygon points="$ax,$ty ${ax - 5},$by2 ${ax + 5},$by2" fill="#333"/>"""

    s"$clip\n$box\n$hatch\n$arrow"

  /** Small opening gap painted over the wall at the window position. */
  private def windowSymbol(rm: RenderedRoom, side: WallSide): String =
    val (x, y, w, h) = side match
      case WallSide.North => ((rm.x + rm.w / 2 - 10).toInt, (rm.y - 1).toInt,       20, 3)
      case WallSide.South => ((rm.x + rm.w / 2 - 10).toInt, (rm.y + rm.h - 1).toInt, 20, 3)
      case WallSide.East  => ((rm.x + rm.w - 1).toInt,       (rm.y + rm.h / 2 - 10).toInt, 3, 20)
      case WallSide.West  => ((rm.x - 1).toInt,               (rm.y + rm.h / 2 - 10).toInt, 3, 20)
    s"""<rect x="$x" y="$y" width="$w" height="$h" fill="white" stroke="#99c" stroke-width="0.5"/>"""

  // ── Corridors ────────────────────────────────────────────────────────

  private def corridorFloors(c: RenderedCorridor): List[String] =
    c.rects.map { r =>
      s"""<rect x="${r.x.toInt}" y="${r.y.toInt}" width="${r.w.toInt}" height="${r.h.toInt}" fill="white"/>"""
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
      case DoorType.Locked => "door-locked"
      case DoorType.Secret => "door-secret"
      case DoorType.Barred => "door-barred"
    val ux = (d.position.x - 15).toInt
    val uy = (d.position.y - 15).toInt
    val px = d.position.x.toInt
    val py = d.position.y.toInt
    s"""<use href="#$symbolId" x="$ux" y="$uy" width="30" height="30" transform="rotate(${d.angle.toInt},$px,$py)"/>"""

  private def escapeXml(s: String): String =
    s.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;").replace("\"", "&quot;")
