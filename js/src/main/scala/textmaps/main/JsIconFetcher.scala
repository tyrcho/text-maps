package textmaps.main

import org.scalajs.dom
import textmaps.icons.{FetchedIcon, IconCache, IconFetcher, IconSvgParser}

/** Fetches icon SVGs from the Iconify API using a *synchronous* XHR call, so
 *  the render pipeline can stay synchronous end to end — `SvgStringRenderer
 *  .render` is a pure, synchronous function everywhere else in this project,
 *  and threading Promises through the whole DSL-edit → render → inject path
 *  would be a much bigger change than this feature warrants. Synchronous XHR
 *  blocks the main thread and is deprecated; worth revisiting (e.g. debounced
 *  async re-render) if it causes noticeable input lag once icons are in use.
 *
 *  Iconify's response is confirmed to send `Access-Control-Allow-Origin: *`,
 *  so the cross-origin XHR itself should be permitted by the browser; this
 *  specific code path (running inside an actual browser) has not been
 *  exercised in this environment, though — worth a manual spot-check. */
final class XhrIconFetcher(baseUrl: String = "https://api.iconify.design") extends IconFetcher:
  def fetch(iconSet: String, iconName: String): Option[FetchedIcon] =
    try
      val xhr = new dom.XMLHttpRequest()
      xhr.open("GET", s"$baseUrl/$iconSet/$iconName.svg", false)
      xhr.send()
      if xhr.status == 200 then IconSvgParser.parse(xhr.responseText) else None
    catch case _: Throwable => None

/** `localStorage`-backed cache, namespaced so it doesn't collide with
 *  anything else the app stores there. Falls back to no-op silently if
 *  storage is unavailable or full (e.g. private browsing). */
final class LocalStorageIconCache extends IconCache:
  private def key(iconSet: String, iconName: String): String =
    s"text-maps-icon-cache:$iconSet/$iconName"

  def get(iconSet: String, iconName: String): Option[FetchedIcon] =
    try
      Option(dom.window.localStorage.getItem(key(iconSet, iconName))).flatMap { stored =>
        stored.split("\n", 2) match
          case Array(viewBox, body) => Some(FetchedIcon(viewBox, body))
          case _                    => None
      }
    catch case _: Throwable => None

  def put(iconSet: String, iconName: String, icon: FetchedIcon): Unit =
    try dom.window.localStorage.setItem(key(iconSet, iconName), s"${icon.viewBox}\n${icon.body}")
    catch case _: Throwable => ()
