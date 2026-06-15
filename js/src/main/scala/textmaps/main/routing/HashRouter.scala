package textmaps.main.routing

import org.scalajs.dom

enum Route:
  case Editor
  case Map(compressed: String)

object HashRouter:
  private var suppressNext = false

  def current(): Route = parse(dom.window.location.hash)

  def push(route: Route): Unit =
    suppressNext = true
    dom.window.location.hash = serialize(route)

  def onChange(handler: Route => Unit): Unit =
    dom.window.addEventListener("hashchange", (_: dom.Event) =>
      if suppressNext then suppressNext = false
      else handler(current())
    )

  private def serialize(route: Route): String = route match
    case Route.Editor       => "editor"
    case Route.Map(data)    => s"map/$data"

  private def parse(hash: String): Route =
    hash.stripPrefix("#").split("/", 2).toList match
      case "map" :: data :: Nil => Route.Map(data)
      case _                    => Route.Editor
