package textmaps.render

import textmaps.dsl.{DoorType, RoomShape}
import textmaps.layout.*

/** Renders a RenderedMap as a self-contained SVG string.
 *
 *  Used by the native CLI (stdout) and the browser app (innerHTML injection).
 *  No Laminar dependency — pure string generation.
 *
 *  Visual style: Dyson Logos / One Page Dungeon
 *  - Dense diagonal cross-hatching fills all "stone" areas
 *  - White floor rectangles punch through the hatch for rooms and corridors
 *  - Subtle 30px grid overlaid on floor areas
 *  - Dark ink wall strokes around room perimeters and corridor sides
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
        |${map.rooms.map(roomGrid).mkString("\n")}
        |${map.corridors.flatMap(corridorGrids).mkString("\n")}
        |${map.rooms.map(roomWalls).mkString("\n")}
        |${map.corridors.flatMap(corridorWalls).mkString("\n")}
        |${map.doors.map(door).mkString("\n")}
        |${map.rooms.map(roomLabel).mkString("\n")}
        |</svg>""".stripMargin

  /** Returns the SVG inner content (without the <svg> wrapper).
   *  Use this for innerHTML injection in the browser app. */
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
        |${map.rooms.map(roomGrid).mkString("\n")}
        |${map.corridors.flatMap(corridorGrids).mkString("\n")}
        |${map.rooms.map(roomWalls).mkString("\n")}
        |${map.corridors.flatMap(corridorWalls).mkString("\n")}
        |${map.doors.map(door).mkString("\n")}
        |${map.rooms.map(roomLabel).mkString("\n")}""".stripMargin

  // ── <defs> ────────────────────────────────────────────────────────────

  private def defs(): String = s"""<defs>
  <pattern id="hatch" width="6" height="6"
           patternTransform="rotate(45 0 0)" patternUnits="userSpaceOnUse">
    <line x1="0" y1="0" x2="0" y2="6" stroke="#888" stroke-width="1.2"/>
  </pattern>
  <pattern id="floorgrid" width="$GRID" height="$GRID" patternUnits="userSpaceOnUse">
    <path d="M $GRID 0 L 0 0 0 $GRID" fill="none" stroke="#d8d8d8" stroke-width="0.5"/>
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
      val cx = (rm.x + rm.w / 2).toInt
      val cy = (rm.y + rm.h / 2).toInt
      val r  = (math.min(rm.w, rm.h) / 2).toInt
      s"""<circle cx="$cx" cy="$cy" r="$r" fill="white"/>"""

  private def roomGrid(rm: RenderedRoom): String = rm.shape match
    case RoomShape.Rectangular =>
      s"""<rect x="${rm.x.toInt}" y="${rm.y.toInt}" width="${rm.w.toInt}" height="${rm.h.toInt}" fill="url(#floorgrid)"/>"""
    case RoomShape.Circular =>
      val cx = (rm.x + rm.w / 2).toInt
      val cy = (rm.y + rm.h / 2).toInt
      val r  = (math.min(rm.w, rm.h) / 2).toInt
      s"""<circle cx="$cx" cy="$cy" r="$r" fill="url(#floorgrid)"/>"""

  private def roomWalls(rm: RenderedRoom): String = rm.shape match
    case RoomShape.Rectangular =>
      s"""<rect x="${rm.x.toInt}" y="${rm.y.toInt}" width="${rm.w.toInt}" height="${rm.h.toInt}" fill="none" stroke="#333" stroke-width="$WALL"/>"""
    case RoomShape.Circular =>
      val cx = (rm.x + rm.w / 2).toInt
      val cy = (rm.y + rm.h / 2).toInt
      val r  = (math.min(rm.w, rm.h) / 2).toInt
      s"""<circle cx="$cx" cy="$cy" r="$r" fill="none" stroke="#333" stroke-width="$WALL"/>"""

  private def roomLabel(rm: RenderedRoom): String =
    val lx = (rm.x + rm.w / 2).toInt
    val ly = (rm.y + rm.h / 2).toInt
    s"""<text x="$lx" y="$ly" text-anchor="middle" dominant-baseline="middle" fill="#333" font-size="$FONT_SZ" font-family="serif" font-style="italic">${escapeXml(rm.label)}</text>"""

  // ── Corridors ────────────────────────────────────────────────────────

  private def corridorFloors(c: RenderedCorridor): List[String] =
    c.rects.map { r =>
      s"""<rect x="${r.x.toInt}" y="${r.y.toInt}" width="${r.w.toInt}" height="${r.h.toInt}" fill="white"/>"""
    }

  private def corridorGrids(c: RenderedCorridor): List[String] =
    c.rects.map { r =>
      s"""<rect x="${r.x.toInt}" y="${r.y.toInt}" width="${r.w.toInt}" height="${r.h.toInt}" fill="url(#floorgrid)"/>"""
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
