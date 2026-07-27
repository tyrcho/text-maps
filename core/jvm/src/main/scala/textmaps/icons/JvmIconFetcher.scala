package textmaps.icons

import java.net.URI
import java.net.http.{HttpClient, HttpRequest, HttpResponse}
import java.time.Duration
import scala.util.Try

/** Fetches icon SVGs from the Iconify API (https://api.iconify.design) over
 *  HTTPS. Best-effort: any network error, timeout, or non-200 response
 *  yields None rather than throwing, so an unreachable icon degrades to the
 *  renderer's placeholder instead of failing the whole map.
 *
 *  Verified against the live API during development (see
 *  `core/jvm/src/test/resources/textmaps/icons-cache` for real fetched
 *  responses used as the JVM test suite's offline cache). */
final class HttpIconFetcher(baseUrl: String = "https://api.iconify.design") extends IconFetcher:
  private val client =
    HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(5)).build()

  def fetch(iconSet: String, iconName: String): Option[FetchedIcon] =
    Try {
      val request = HttpRequest.newBuilder(URI.create(s"$baseUrl/$iconSet/$iconName.svg"))
        .timeout(Duration.ofSeconds(5))
        .GET()
        .build()
      val response = client.send(request, HttpResponse.BodyHandlers.ofString())
      if response.statusCode() == 200 then IconSvgParser.parse(response.body()) else None
    }.toOption.flatten

/** Disk-backed cache under `dir`, one file per icon (`<set>__<name>.svg`,
 *  containing the viewBox on the first line and the body markup after it).
 *  Lets the JVM test suite and any JVM-hosted use of core run fully offline
 *  once populated, and avoids re-fetching the same icon on every run. */
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
