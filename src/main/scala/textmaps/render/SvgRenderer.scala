package textmaps.render

import com.raquo.laminar.api.L.svg
import com.raquo.laminar.api.L.svg.*
import com.raquo.laminar.api.L.textToTextNode
import com.raquo.laminar.nodes.ReactiveSvgElement
import org.scalajs.dom
import textmaps.dsl.{DoorType, RoomShape}
import textmaps.layout.*

/** Renders a RenderedMap as dungeon-grid SVG.
 *
 *  Visual style: dark stone background, grid overlay, rooms as lit floor tiles,
 *  corridors as passages, door glyphs at connections.
 */
object SvgRenderer:

  private val GRID    = 30  // px per grid cell (matches UNIT_PX in LayoutEngine)
  private val WALL    = 3   // wall thickness in px
  private val FONT_SZ = 12  // room label font size

  def render(map: RenderedMap): List[ReactiveSvgElement[dom.svg.Element]] =
    svgDefs(map) :: background(map) ::
    map.corridors.map(corridor) :::
    map.rooms.map(room) :::
    map.doors.map(door)

  // ── <defs> ────────────────────────────────────────────────────────────

  private def svgDefs(map: RenderedMap): ReactiveSvgElement[dom.svg.Element] =
    defs(
      // Grid tile pattern
      pattern(
        idAttr      := "grid",
        width       := GRID.toString,
        height      := GRID.toString,
        patternUnits := "userSpaceOnUse",
        path(
          d    := s"M $GRID 0 L 0 0 0 $GRID",
          fill := "none",
          stroke        := "#2a2e3a",
          strokeWidth   := "0.5",
        ),
      ),
      // Door symbols
      symbol(
        idAttr  := "door-open",
        viewBox := "0 0 10 10",
        // Open archway: just two posts
        rect(x := "0", y := "3", width := "2", height := "4", fill := "#8b7355"),
        rect(x := "8", y := "3", width := "2", height := "4", fill := "#8b7355"),
      ),
      symbol(
        idAttr  := "door-locked",
        viewBox := "0 0 10 10",
        rect(x := "1", y := "2", width := "8", height := "6", rx := "1",
          fill := "#1a1a1a", stroke := "#c0392b", strokeWidth := "1.5"),
        // Keyhole
        circle(cx := "5", cy := "5", r := "1.5", fill := "#c0392b"),
      ),
      symbol(
        idAttr  := "door-secret",
        viewBox := "0 0 10 10",
        // Dashed outline — looks like hidden passage
        rect(x := "1", y := "3", width := "8", height := "4",
          fill := "none", stroke := "#7c3aed",
          strokeWidth := "1", style := "stroke-dasharray: 2,1"),
      ),
      symbol(
        idAttr  := "door-barred",
        viewBox := "0 0 10 10",
        rect(x := "1", y := "2", width := "8", height := "6",
          fill := "#1a1a1a", stroke := "#d97706", strokeWidth := "1.5"),
        // Bars
        line(x1 := "3", y1 := "2", x2 := "3", y2 := "8", stroke := "#d97706", strokeWidth := "1"),
        line(x1 := "5", y1 := "2", x2 := "5", y2 := "8", stroke := "#d97706", strokeWidth := "1"),
        line(x1 := "7", y1 := "2", x2 := "7", y2 := "8", stroke := "#d97706", strokeWidth := "1"),
      ),
    )

  // ── Background ────────────────────────────────────────────────────────

  private def background(map: RenderedMap): ReactiveSvgElement[dom.svg.Element] =
    val pad = 40.0
    g(
      // Stone fill
      rect(
        x      := (map.minX - pad).toInt.toString,
        y      := (map.minY - pad).toInt.toString,
        width  := (map.width + pad * 2).toInt.toString,
        height := (map.height + pad * 2).toInt.toString,
        fill   := "#0d0d0f",
      ),
      // Grid overlay
      rect(
        x      := (map.minX - pad).toInt.toString,
        y      := (map.minY - pad).toInt.toString,
        width  := (map.width + pad * 2).toInt.toString,
        height := (map.height + pad * 2).toInt.toString,
        fill   := "url(#grid)",
      ),
    )

  // ── Rooms ────────────────────────────────────────────────────────────

  private def room(rm: RenderedRoom): ReactiveSvgElement[dom.svg.Element] =
    g(
      cls := "room",
      rm.shape match
        case RoomShape.Rectangular =>
          g(
            // Floor
            rect(
              x      := rm.x.toInt.toString,
              y      := rm.y.toInt.toString,
              width  := rm.w.toInt.toString,
              height := rm.h.toInt.toString,
              fill   := "#3a3a4a",
            ),
            // Inner floor lighter center
            rect(
              x      := (rm.x + WALL).toInt.toString,
              y      := (rm.y + WALL).toInt.toString,
              width  := (rm.w - WALL * 2).toInt.toString,
              height := (rm.h - WALL * 2).toInt.toString,
              fill   := "#4a4a5e",
            ),
            // Walls (thick border)
            rect(
              x           := rm.x.toInt.toString,
              y           := rm.y.toInt.toString,
              width       := rm.w.toInt.toString,
              height      := rm.h.toInt.toString,
              fill        := "none",
              stroke      := "#7a7a8a",
              strokeWidth := WALL.toString,
            ),
          )
        case RoomShape.Circular =>
          val cxV = (rm.x + rm.w / 2).toInt
          val cyV = (rm.y + rm.h / 2).toInt
          val rad = (math.min(rm.w, rm.h) / 2).toInt
          g(
            circle(cx := cxV.toString, cy := cyV.toString, r := rad.toString, fill := "#3a3a4a"),
            circle(cx := cxV.toString, cy := cyV.toString, r := (rad - WALL).toString, fill := "#4a4a5e"),
            circle(cx := cxV.toString, cy := cyV.toString, r := rad.toString,
              fill := "none", stroke := "#7a7a8a", strokeWidth := WALL.toString),
          )
      ,
      // Room label
      text(
        x                := (rm.x + rm.w / 2).toInt.toString,
        y                := (rm.y + rm.h / 2).toInt.toString,
        textAnchor       := "middle",
        dominantBaseline := "middle",
        fill             := "#c8c8d8",
        fontSize         := FONT_SZ.toString,
        rm.label,
      ),
    )

  // ── Corridors ────────────────────────────────────────────────────────

  private def corridor(c: RenderedCorridor): ReactiveSvgElement[dom.svg.Element] =
    val pts = c.points.map(p => s"${p.x.toInt},${p.y.toInt}").mkString(" ")
    g(
      // Passage fill (floor)
      polyline(
        points      := pts,
        fill        := "none",
        stroke      := "#4a4a5e",
        strokeWidth := "20",
        style := "stroke-linecap: square",
      ),
      // Passage walls (outline)
      polyline(
        points      := pts,
        fill        := "none",
        stroke      := "#7a7a8a",
        strokeWidth := "22",
        style := "stroke-linecap: square",
        opacity     := "0.6",
      ),
    )

  // ── Doors ────────────────────────────────────────────────────────────

  private def door(d: RenderedDoor): ReactiveSvgElement[dom.svg.Element] =
    val symbolId = d.doorType match
      case DoorType.Open   => "door-open"
      case DoorType.Locked => "door-locked"
      case DoorType.Secret => "door-secret"
      case DoorType.Barred => "door-barred"
    use(
      href      := s"#$symbolId",
      x         := (d.position.x - 5).toInt.toString,
      y         := (d.position.y - 5).toInt.toString,
      width     := "10",
      height    := "10",
      transform := s"rotate(${d.angle.toInt},${d.position.x.toInt},${d.position.y.toInt})",
    )
