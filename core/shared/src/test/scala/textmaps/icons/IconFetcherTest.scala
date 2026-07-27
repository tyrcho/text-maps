package textmaps.icons

class IconFetcherTest extends munit.FunSuite:

  test("IconSvgParser extracts viewBox and inner body from an Iconify-shaped SVG response"):
    val raw = """<svg xmlns="http://www.w3.org/2000/svg" width="1em" height="1em" viewBox="0 0 512 512"><path fill="currentColor" d="M1 2z"/></svg>"""
    assertEquals(IconSvgParser.parse(raw), Some(FetchedIcon("0 0 512 512", """<path fill="currentColor" d="M1 2z"/>""")))

  test("IconSvgParser handles multiple children and surrounding whitespace"):
    val raw = """<svg viewBox="0 0 24 24">
      <path d="a"/>
      <path d="b"/>
    </svg>"""
    val result = IconSvgParser.parse(raw)
    assert(result.isDefined)
    assertEquals(result.get.viewBox, "0 0 24 24")
    assert(result.get.body.contains("""<path d="a"/>"""))
    assert(result.get.body.contains("""<path d="b"/>"""))

  test("IconSvgParser yields None for input missing a viewBox"):
    assertEquals(IconSvgParser.parse("""<svg><path d="a"/></svg>"""), None)

  test("IconSvgParser yields None for unparseable input"):
    assertEquals(IconSvgParser.parse("not an svg at all"), None)

  test("CachingIconFetcher returns a cache hit without consulting the live fetcher"):
    val icon = FetchedIcon("0 0 24 24", "<path/>")
    val cache: IconCache = new IconCache:
      def get(iconSet: String, iconName: String) = Some(icon)
      def put(iconSet: String, iconName: String, i: FetchedIcon) = fail("should not write back on a hit")
    val live: IconFetcher = (_, _) => fail("should not call live fetcher on a cache hit")
    val fetcher = new CachingIconFetcher(cache, live)
    assertEquals(fetcher.fetch("game-icons", "stalactites"), Some(icon))

  test("CachingIconFetcher falls through to live on a cache miss and writes the result back"):
    val icon = FetchedIcon("0 0 24 24", "<path/>")
    var written: Option[(String, String, FetchedIcon)] = None
    val cache: IconCache = new IconCache:
      def get(iconSet: String, iconName: String) = None
      def put(iconSet: String, iconName: String, i: FetchedIcon) = written = Some((iconSet, iconName, i))
    val live: IconFetcher = (_, _) => Some(icon)
    val fetcher = new CachingIconFetcher(cache, live)
    assertEquals(fetcher.fetch("game-icons", "stalactites"), Some(icon))
    assertEquals(written, Some(("game-icons", "stalactites", icon)))

  test("CachingIconFetcher does not write back when live also misses"):
    var putCalled = false
    val cache: IconCache = new IconCache:
      def get(iconSet: String, iconName: String) = None
      def put(iconSet: String, iconName: String, i: FetchedIcon) = putCalled = true
    val live: IconFetcher = (_, _) => None
    val fetcher = new CachingIconFetcher(cache, live)
    assertEquals(fetcher.fetch("game-icons", "unknown-icon"), None)
    assert(!putCalled)

  test("IconFetcher.none always misses"):
    assertEquals(IconFetcher.none.fetch("game-icons", "stalactites"), None)
