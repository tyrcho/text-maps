package textmaps.render

import com.raquo.laminar.api.L.svg
import com.raquo.laminar.api.L.svg.*
import com.raquo.laminar.api.L.textToTextNode
import com.raquo.laminar.nodes.ReactiveSvgElement
import org.scalajs.dom
import textmaps.dsl.{DoorType, RoomShape}
import textmaps.layout.*

/** Renders a RenderedMap as a Dyson Logos / One Page Dungeon style dungeon.
 *
 *  Visual approach:
 *  - Entire canvas filled with dense diagonal cross-hatching (= stone/rock)
 *  - Room and corridor floors punch through with white rectangles
 *  - Subtle grid pattern overlaid on the white floor areas
 *  - Dark ink strokes trace the room perimeters and corridor walls
 *  - Door glyphs drawn across the passage at the room entrance
 */
object SvgRenderer:

  private val GRID    = 30   // px per grid cell — matches UNIT_PX in LayoutEngine
  private val WALL    = 1.5  // wall stroke width in px
  private val FONT_SZ = 9    // room label font size

  def render(map: RenderedMap): List[ReactiveSvgElement[dom.svg.Element]] =
    svgDefs(map) ::
    background(map) ::
    map.corridors.flatMap(corridorFloors) :::   // white floor rects (punch through hatch)
    map.rooms.map(roomFloor) :::                // white floor rects (on top of corridor ends)
    map.rooms.map(roomGrid) :::                 // subtle grid on room floors
    map.corridors.flatMap(corridorGrids) :::    // subtle grid on corridor floors
    map.rooms.map(roomWalls) :::                // dark wall strokes around rooms
    map.corridors.flatMap(corridorWalls) :::    // dark wall lines along corridor sides
    map.doors.map(door) :::
    map.rooms.map(roomLabel)

  // ── <defs> ────────────────────────────────────────────────────────────

  private def svgDefs(map: RenderedMap): ReactiveSvgElement[dom.svg.Element] =
    defs(
      // Dense diagonal cross-hatch — fills the "stone" areas outside rooms
      pattern(
        idAttr          := "hatch",
        width           := "6",
        height          := "6",
        patternTransform := "rotate(45 0 0)",
        patternUnits    := "userSpaceOnUse",
        line(x1 := "0", y1 := "0", x2 := "0", y2 := "6",
          stroke := "#888", strokeWidth := "1.2"),
      ),
      // Subtle floor grid — drawn on top of white room/corridor floors
      pattern(
        idAttr       := "floorgrid",
        width        := GRID.toString,
        height       := GRID.toString,
        patternUnits := "userSpaceOnUse",
        path(
          d    := s"M $GRID 0 L 0 0 0 $GRID",
          fill := "none",
          stroke      := "#d8d8d8",
          strokeWidth := "0.5",
        ),
      ),
      // Door symbol: two jamb posts across the passage
      symbol(
        idAttr  := "door-open",
        viewBox := "0 0 30 30",
        rect(x := "0", y := "12", width := "30", height := "6",
          fill := "white"),
        rect(x := "0", y := "11", width := "4", height := "8",
          fill := "#333"),
        rect(x := "26", y := "11", width := "4", height := "8",
          fill := "#333"),
      ),
      symbol(
        idAttr  := "door-locked",
        viewBox := "0 0 30 30",
        rect(x := "0", y := "12", width := "30", height := "6",
          fill := "white"),
        rect(x := "2", y := "11", width := "26", height := "8",
          fill := "#333"),
        rect(x := "4", y := "13", width := "22", height := "4",
          fill := "white"),
      ),
      symbol(
        idAttr  := "door-secret",
        viewBox := "0 0 30 30",
        // Looks just like the wall — secret!
        line(x1 := "0", y1 := "15", x2 := "30", y2 := "15",
          stroke := "#888", strokeWidth := "1.2",
          style := "stroke-dasharray: 6,6"),
      ),
      symbol(
        idAttr  := "door-barred",
        viewBox := "0 0 30 30",
        rect(x := "0", y := "12", width := "30", height := "6",
          fill := "white"),
        rect(x := "2", y := "11", width := "26", height := "8",
          fill := "#555"),
        line(x1 := "8",  y1 := "11", x2 := "8",  y2 := "19", stroke := "white", strokeWidth := "2"),
        line(x1 := "15", y1 := "11", x2 := "15", y2 := "19", stroke := "white", strokeWidth := "2"),
        line(x1 := "22", y1 := "11", x2 := "22", y2 := "19", stroke := "white", strokeWidth := "2"),
      ),
    )

  // ── Background: white base + full-canvas cross-hatch ─────────────────

  private def background(map: RenderedMap): ReactiveSvgElement[dom.svg.Element] =
    val pad = 60.0
    val bx  = (map.minX - pad).toInt.toString
    val by  = (map.minY - pad).toInt.toString
    val bw  = (map.width  + pad * 2).toInt.toString
    val bh  = (map.height + pad * 2).toInt.toString
    g(
      rect(idAttr := "bg-white", x := bx, y := by, width := bw, height := bh, fill := "white"),
      rect(idAttr := "bg-hatch", x := bx, y := by, width := bw, height := bh, fill := "url(#hatch)"),
    )

  // ── Room floor (punches through hatch) ───────────────────────────────

  private def roomFloor(rm: RenderedRoom): ReactiveSvgElement[dom.svg.Element] =
    rm.shape match
      case RoomShape.Rectangular =>
        rect(x := rm.x.toInt.toString, y := rm.y.toInt.toString,
          width := rm.w.toInt.toString, height := rm.h.toInt.toString,
          fill := "white")
      case RoomShape.Circular =>
        val cxV = (rm.x + rm.w / 2).toInt
        val cyV = (rm.y + rm.h / 2).toInt
        val rad = (math.min(rm.w, rm.h) / 2).toInt
        circle(cx := cxV.toString, cy := cyV.toString, r := rad.toString, fill := "white")

  // ── Room grid overlay ─────────────────────────────────────────────────

  private def roomGrid(rm: RenderedRoom): ReactiveSvgElement[dom.svg.Element] =
    rm.shape match
      case RoomShape.Rectangular =>
        rect(x := rm.x.toInt.toString, y := rm.y.toInt.toString,
          width := rm.w.toInt.toString, height := rm.h.toInt.toString,
          fill := "url(#floorgrid)")
      case RoomShape.Circular =>
        val cxV = (rm.x + rm.w / 2).toInt
        val cyV = (rm.y + rm.h / 2).toInt
        val rad = (math.min(rm.w, rm.h) / 2).toInt
        circle(cx := cxV.toString, cy := cyV.toString, r := rad.toString,
          fill := "url(#floorgrid)")

  // ── Room wall border ──────────────────────────────────────────────────

  private def roomWalls(rm: RenderedRoom): ReactiveSvgElement[dom.svg.Element] =
    rm.shape match
      case RoomShape.Rectangular =>
        rect(
          x           := rm.x.toInt.toString,
          y           := rm.y.toInt.toString,
          width       := rm.w.toInt.toString,
          height      := rm.h.toInt.toString,
          fill        := "none",
          stroke      := "#333",
          strokeWidth := WALL.toString,
        )
      case RoomShape.Circular =>
        val cxV = (rm.x + rm.w / 2).toInt
        val cyV = (rm.y + rm.h / 2).toInt
        val rad = (math.min(rm.w, rm.h) / 2).toInt
        circle(cx := cxV.toString, cy := cyV.toString, r := rad.toString,
          fill := "none", stroke := "#333", strokeWidth := WALL.toString)

  // ── Room label ────────────────────────────────────────────────────────

  private def roomLabel(rm: RenderedRoom): ReactiveSvgElement[dom.svg.Element] =
    text(
      x                := (rm.x + rm.w / 2).toInt.toString,
      y                := (rm.y + rm.h / 2).toInt.toString,
      textAnchor       := "middle",
      dominantBaseline := "middle",
      fill             := "#333",
      fontSize         := FONT_SZ.toString,
      style            := "font-family: serif; font-style: italic;",
      rm.label,
    )

  // ── Corridor floors (punch through hatch; drawn before room walls) ────

  private def corridorFloors(c: RenderedCorridor): List[ReactiveSvgElement[dom.svg.Element]] =
    c.rects.map { r =>
      rect(x := r.x.toInt.toString, y := r.y.toInt.toString,
        width := r.w.toInt.toString, height := r.h.toInt.toString,
        fill := "white")
    }

  // ── Corridor grid overlay ─────────────────────────────────────────────

  private def corridorGrids(c: RenderedCorridor): List[ReactiveSvgElement[dom.svg.Element]] =
    c.rects.map { r =>
      rect(x := r.x.toInt.toString, y := r.y.toInt.toString,
        width := r.w.toInt.toString, height := r.h.toInt.toString,
        fill := "url(#floorgrid)")
    }

  // ── Corridor wall lines (long sides of each rect segment) ────────────

  private def corridorWalls(c: RenderedCorridor): List[ReactiveSvgElement[dom.svg.Element]] =
    c.rects.flatMap { r =>
      if r.isHorizontal then
        // Top and bottom wall lines along the full width
        List(
          corridorWallLine(r.x, r.y, r.x + r.w, r.y),
          corridorWallLine(r.x, r.y + r.h, r.x + r.w, r.y + r.h),
        )
      else
        // Left and right wall lines along the full height
        List(
          corridorWallLine(r.x, r.y, r.x, r.y + r.h),
          corridorWallLine(r.x + r.w, r.y, r.x + r.w, r.y + r.h),
        )
    }

  private def corridorWallLine(
    x1v: Double, y1v: Double, x2v: Double, y2v: Double
  ): ReactiveSvgElement[dom.svg.Element] =
    line(
      x1 := x1v.toInt.toString, y1 := y1v.toInt.toString,
      x2 := x2v.toInt.toString, y2 := y2v.toInt.toString,
      stroke      := "#333",
      strokeWidth := WALL.toString,
    )

  // ── Door glyphs ───────────────────────────────────────────────────────

  private def door(d: RenderedDoor): ReactiveSvgElement[dom.svg.Element] =
    val symbolId = d.doorType match
      case DoorType.Open   => "door-open"
      case DoorType.Locked => "door-locked"
      case DoorType.Secret => "door-secret"
      case DoorType.Barred => "door-barred"
    use(
      href      := s"#$symbolId",
      x         := (d.position.x - 15).toInt.toString,
      y         := (d.position.y - 15).toInt.toString,
      width     := "30",
      height    := "30",
      transform := s"rotate(${d.angle.toInt},${d.position.x.toInt},${d.position.y.toInt})",
    )
