package textmaps.icons

/** The bit of an Iconify icon `SvgStringRenderer` actually needs: the source
 *  icon's own `viewBox` (icons aren't all drawn on the same coordinate grid)
 *  and its inner markup (the `<path>`/`<g>` children, without the outer
 *  `<svg>` wrapper — we re-wrap it ourselves at the feature's position). */
case class FetchedIcon(viewBox: String, body: String)

/** Looks up one icon's SVG content by `(iconSet, iconName)`, e.g.
 *  `("game-icons", "stalactites")`. Implementations may hit the network, a
 *  local cache, both, or neither — callers must treat `None` (icon
 *  unavailable, for any reason) as a normal, expected outcome rather than an
 *  error, since a missing icon should degrade to a placeholder, not break
 *  rendering of the rest of the map. */
trait IconFetcher:
  def fetch(iconSet: String, iconName: String): Option[FetchedIcon]

object IconFetcher:
  /** Always misses. The default for `SvgStringRenderer.render` so every
   *  existing caller keeps compiling and rendering deterministically without
   *  wiring up a real fetcher — icon features fall back to the placeholder. */
  val none: IconFetcher = (_, _) => None

/** A place to persist fetched icons across runs/renders, keyed the same way
 *  as `IconFetcher`. Implementations are platform-specific (disk, browser
 *  storage, ...); `IconCache.none` is a safe no-op default. */
trait IconCache:
  def get(iconSet: String, iconName: String): Option[FetchedIcon]
  def put(iconSet: String, iconName: String, icon: FetchedIcon): Unit

object IconCache:
  val none: IconCache = new IconCache:
    def get(iconSet: String, iconName: String): Option[FetchedIcon] = None
    def put(iconSet: String, iconName: String, icon: FetchedIcon): Unit = ()

/** Cache-first decorator: a cache hit returns immediately; a miss falls
 *  through to `live`, and a successful live fetch is written back to the
 *  cache so the next lookup for the same icon doesn't hit the network. */
final class CachingIconFetcher(cache: IconCache, live: IconFetcher) extends IconFetcher:
  def fetch(iconSet: String, iconName: String): Option[FetchedIcon] =
    cache.get(iconSet, iconName).orElse {
      live.fetch(iconSet, iconName).map { icon =>
        cache.put(iconSet, iconName, icon)
        icon
      }
    }

/** Parses the raw SVG XML an Iconify `/<set>/<name>.svg` API endpoint returns
 *  (e.g. `https://api.iconify.design/game-icons/stalactites.svg`) into just
 *  the `viewBox` and inner markup we need. Best-effort and lenient by design:
 *  yields `None` rather than throwing on anything unexpected, since a
 *  malformed or surprising response should degrade to the renderer's
 *  placeholder, never break the rest of the map.
 *
 *  Verified against live `api.iconify.design` responses for several
 *  `game-icons` icons (see `core/jvm/src/test/resources/textmaps/icons-cache`,
 *  populated from real fetches). */
object IconSvgParser:
  private val ViewBoxRe = """viewBox="([^"]*)"""".r
  private val BodyRe    = """(?s)<svg[^>]*>(.*)</svg>""".r

  def parse(rawSvg: String): Option[FetchedIcon] =
    for
      viewBox <- ViewBoxRe.findFirstMatchIn(rawSvg).map(_.group(1)).filter(_.nonEmpty)
      body    <- BodyRe.findFirstMatchIn(rawSvg).map(_.group(1).trim).filter(_.nonEmpty)
    yield FetchedIcon(viewBox, body)
