package textmaps.icons

import scala.sys.process.*
import scala.util.Try

/** Fetches icon SVGs by shelling out to `curl` — Scala Native has no built-in
 *  HTTPS client. Requires `curl` on PATH; any failure (missing binary, no
 *  network, non-200 response, timeout) yields None rather than throwing.
 *
 *  Verified end-to-end (compiled, linked, and run against the live API via
 *  `./native/target/.../text-maps-native`) during development. */
final class CurlIconFetcher(baseUrl: String = "https://api.iconify.design") extends IconFetcher:
  def fetch(iconSet: String, iconName: String): Option[FetchedIcon] =
    Try {
      val url = s"$baseUrl/$iconSet/$iconName.svg"
      val out = Seq("curl", "-sS", "-f", "--max-time", "5", url).!!
      IconSvgParser.parse(out)
    }.toOption.flatten

/** Same on-disk cache format as the JVM `FileIconCache` — lets the Native CLI
 *  run fully offline once a cache directory has been populated (e.g. by the
 *  JVM fetcher, or copied from another machine with network access). */
final class FileIconCache(dir: java.io.File) extends IconCache:
  dir.mkdirs()

  private def file(iconSet: String, iconName: String): java.io.File =
    new java.io.File(dir, s"${iconSet}__$iconName.svg")

  def get(iconSet: String, iconName: String): Option[FetchedIcon] =
    val f = file(iconSet, iconName)
    if !f.isFile then None
    else
      val content = new String(java.nio.file.Files.readAllBytes(f.toPath), "UTF-8")
      content.split("\n", 2) match
        case Array(viewBox, body) => Some(FetchedIcon(viewBox, body))
        case _                    => None

  def put(iconSet: String, iconName: String, icon: FetchedIcon): Unit =
    val f = file(iconSet, iconName)
    java.nio.file.Files.write(f.toPath, s"${icon.viewBox}\n${icon.body}".getBytes("UTF-8"))
    ()
