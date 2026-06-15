package textmaps.main

import org.scalajs.dom
import org.scalajs.dom.{document, window}
import textmaps.dsl.{DslParser, DungeonMapSource}
import textmaps.generate.DungeonGenerator
import textmaps.layout.{LayoutEngine, RenderedMap}
import textmaps.main.routing.{HashRouter, Route}
import textmaps.render.SvgRenderer

val defaultDsl =
  """map dungeon "The Sunken Keep"
  seed: 42

room entrance 4x4
  label: "Gatehouse"

room great_hall 8x6
  label: "Great Hall"

room prison 3x3
  label: "Prison"

room vault 4x4
  label: "Vault"
  shape: circular

room boss 6x5
  label: "Throne Room"

connect entrance -> great_hall
  door: open
  corridor: 2x4

connect great_hall -> prison
  door: locked

connect great_hall -> vault
  door: secret

connect great_hall -> boss
  corridor: 3x5
""".trim

@main def main(): Unit =
  // Module scripts are deferred — DOM is already ready when this runs
  if document.readyState == "loading" then
    document.addEventListener("DOMContentLoaded", (_: dom.Event) => setup())
  else
    setup()

def setup(): Unit =
  val ta       = document.getElementById("dsl-input").asInstanceOf[dom.html.TextArea]
  val errEl    = document.getElementById("error-banner").asInstanceOf[dom.html.Div]
  val mapSvg   = document.getElementById("map-svg").asInstanceOf[dom.svg.SVG]
  val shareBtn = document.getElementById("btn-share").asInstanceOf[dom.html.Button]

  ta.value = defaultDsl

  // Restore from hash
  HashRouter.current() match
    case Route.Map(compressed) => lzDecompress(compressed).foreach(ta.value = _)
    case _ => ()

  render(ta.value, errEl, mapSvg)

  ta.addEventListener("input", (_: dom.Event) => render(ta.value, errEl, mapSvg))

  shareBtn.addEventListener("click", (_: dom.Event) =>
    val compressed = LZString.compress(ta.value)
    HashRouter.push(Route.Map(compressed))
    window.navigator.clipboard.writeText(window.location.href)
  )

  HashRouter.onChange {
    case Route.Map(compressed) => lzDecompress(compressed).foreach { text =>
      ta.value = text
      render(text, errEl, mapSvg)
    }
    case _ => ()
  }

def render(dsl: String, errEl: dom.html.Div, mapSvg: dom.svg.SVG): Unit =
  DslParser.parse(dsl) match
    case Left(err) =>
      errEl.textContent = err.message
      errEl.classList.add("visible")
    case Right(ast) =>
      errEl.textContent = ""
      errEl.classList.remove("visible")
      val expanded = ast.source match
        case DungeonMapSource.Generated(n, _, seed) =>
          ast.copy(source = DungeonGenerator.generate(n, seed))
        case _ => ast
      val map = LayoutEngine.compute(expanded)
      renderToSvg(mapSvg, map)

def renderToSvg(svgEl: dom.svg.SVG, map: RenderedMap): Unit =
  val vb = s"${map.minX.toInt} ${map.minY.toInt} ${map.width.toInt} ${map.height.toInt}"
  svgEl.setAttribute("viewBox", vb)
  svgEl.setAttribute("preserveAspectRatio", "xMidYMid meet")
  while svgEl.firstChild != null do svgEl.removeChild(svgEl.firstChild)
  SvgRenderer.render(map).foreach(el => svgEl.appendChild(el.ref))

// LZString CDN facade
@scala.scalajs.js.native
@scala.scalajs.js.annotation.JSGlobal("LZString")
object LZString extends scala.scalajs.js.Object:
  def compress(input: String): String = scala.scalajs.js.native
  def decompress(input: String): String | Null = scala.scalajs.js.native

def lzDecompress(s: String): Option[String] =
  Option(LZString.decompress(s).asInstanceOf[String])
