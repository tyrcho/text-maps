package textmaps.icons

import java.nio.file.Path

/** Fetcher backed solely by the committed test icon cache
 *  (`core/jvm/src/test/resources/textmaps/icons-cache`, populated from real
 *  Iconify API responses) — no live network calls, so approval tests render
 *  real icon content deterministically and fully offline. A cache miss stays
 *  a miss (no live fallback here) rather than reaching the network mid-test. */
object TestIcons:
  private val cacheDir = Path.of("core/jvm/src/test/resources/textmaps/icons-cache").toFile
  val fetcher: IconFetcher = new CachingIconFetcher(new FileIconCache(cacheDir), IconFetcher.none)
