package textmaps.dsl

import textmaps.icons.BuiltinIcons

/** Line-oriented DSL parser. Each top-level declaration starts at column 0;
 *  properties are indented with at least one space. `import <path> as <alias>`
 *  lines (anywhere in the document, before or after the `map` header) are
 *  pulled out in a pre-pass, since they're a document-wide table rather than
 *  a statement in the room/connect grammar.
 */
object DslParser:

  private val ImportRe = """^import\s+(\S+)\s+as\s+(\S+)$""".r
  private val NoteRe   = """^note\s+(\S+)\s+of\s+(\S+)\s*:\s*(.*)$""".r

  def parse(input: String): ParseResult =
    val allLines = input.linesIterator.toList
    val (importLines, lines) = allLines.partition(l => ImportRe.matches(l.strip()))
    val imports = importLines.flatMap { l =>
      ImportRe.findFirstMatchIn(l.strip()).map { m =>
        val path  = m.group(1)
        val alias = m.group(2)
        alias -> path.split("/").last
      }
    }.toMap
    parseLines(lines, imports)

  // ── Line classification ────────────────────────────────────────────────

  private enum Line:
    case Blank
    case Comment
    case MapHeader(name: Option[String], mapType: MapType)
    case RoomDecl(id: String, size: RoomSize)
    case ConnDecl(from: String, to: String)
    case GenerateDecl(roomCount: Int, style: Option[String], seed: Option[Long])
    case NoteDecl(side: WallSide, roomId: String, text: String)
    case Prop(key: String, value: String)

  private def classifyLine(raw: String): Either[String, Line] =
    val line = raw.stripTrailing()
    if line.isBlank || line.strip().startsWith("#") then
      Right(Line.Blank)
    else if line.startsWith(" ") || line.startsWith("\t") then
      val stripped = line.strip()
      stripped.split(":", 2) match
        case Array(k, v) => Right(Line.Prop(k.strip(), v.strip().stripQuotes))
        case _           => Right(Line.Blank)
    else
      line match
        case NoteRe(sideStr, roomId, text) =>
          parseDirection(sideStr) match
            case Some(side) => Right(Line.NoteDecl(side, roomId, text.strip()))
            case None       => Left(s"Unknown note side: $sideStr")
        case _ => classifyTokens(line)

  private def classifyTokens(line: String): Either[String, Line] =
    val tokens = line.split("\\s+").toList
    tokens match
        case "map" :: "dungeon" :: rest =>
          Right(Line.MapHeader(if rest.nonEmpty then Some(rest.mkString(" ").stripQuotes) else None, MapType.Dungeon))
        case "map" :: "building" :: rest =>
          Right(Line.MapHeader(if rest.nonEmpty then Some(rest.mkString(" ").stripQuotes) else None, MapType.Building))
        case "room" :: id :: sizeStr :: _ =>
          parseSize(sizeStr).map(Line.RoomDecl(id, _))
        case "connect" :: from :: "->" :: to :: _ =>
          Right(Line.ConnDecl(from, to))
        case "generate" :: "dungeon" :: kvs =>
          val props = parseKvs(kvs)
          Right(Line.GenerateDecl(
            roomCount = props.get("rooms").flatMap(_.toIntOption).getOrElse(8),
            style     = props.get("style"),
            seed      = props.get("seed").flatMap(_.toLongOption),
          ))
        case _ =>
          Left(s"Unexpected: ${line.take(40)}")

  // ── Statement assembly ─────────────────────────────────────────────────

  private sealed trait Stmt
  private case class RoomStmt(room: Room)          extends Stmt
  private case class ConnStmt(conn: Connection)    extends Stmt
  private case class GenStmt(gen: DungeonMapSource.Generated) extends Stmt
  private case class NoteStmt(note: Note)          extends Stmt

  private def parseLines(lines: List[String], imports: Map[String, String]): ParseResult =
    val nonBlank = lines.indexWhere(l => l.strip().nonEmpty && !l.strip().startsWith("#"))
    if nonBlank < 0 then
      return Left(ParseError("Empty document", 0, 1, 1))

    val headerLine = lines(nonBlank)
    val metaResult = classifyLine(headerLine) match
      case Right(Line.MapHeader(name, mapType)) => Right(MapMeta(name = name, mapType = mapType))
      case _ => Left(ParseError(s"Expected 'map dungeon' or 'map building' but got: ${headerLine.take(40)}", 0, nonBlank + 1, 1))

    metaResult.flatMap { baseMeta =>
      val afterHeader = lines.drop(nonBlank + 1)
      val (headerProps, rest) = afterHeader.span(l => l.startsWith(" ") || l.startsWith("\t") || l.isBlank)
      val metaProps = headerProps.flatMap(l => classifyLine(l).toOption.collect { case Line.Prop(k, v) => k -> v }).toMap
      val meta = baseMeta.copy(
        seed       = metaProps.get("seed").flatMap(_.toLongOption),
        style      = metaProps.get("style"),
        labelStyle = metaProps.get("labels").flatMap(parseLabelStyle),
        background = metaProps.get("background").flatMap(parseBackgroundStyle),
      )

      parseStatements(rest, imports).map { stmts =>
        val genOpt = stmts.collectFirst { case GenStmt(g) => g }
        val source = genOpt.getOrElse {
          DungeonMapSource.Manual(
            rooms = stmts.collect { case RoomStmt(r) => r },
            connections = stmts.collect { case ConnStmt(c) => c },
            notes = stmts.collect { case NoteStmt(n) => n },
          )
        }
        DungeonMap(meta, source)
      }
    }

  private def parseStatements(lines: List[String], imports: Map[String, String]): Either[ParseError, List[Stmt]] =
    val stmts = collection.mutable.ListBuffer[Stmt]()
    val it    = lines.iterator.buffered

    while it.hasNext do
      val raw = it.next()
      classifyLine(raw) match
        case Right(Line.Blank) | Right(Line.Comment) => ()
        case Right(Line.RoomDecl(id, size)) =>
          val props = consumeProps(it)
          stmts += RoomStmt(Room(
            id       = id,
            size     = size,
            label    = props.one("label"),
            shape    = props.one("shape").flatMap(parseShape).getOrElse(RoomShape.Rectangular),
            features = parseRoomFeatures(props, imports),
          ))
        case Right(Line.ConnDecl(from, to)) =>
          val props = consumeProps(it)
          stmts += ConnStmt(Connection(
            from      = from,
            to        = to,
            door      = props.one("door").flatMap(parseDoor).getOrElse(DoorType.Open),
            corridor  = props.one("corridor").flatMap(parseSize(_).toOption),
            doorTo    = props.one("door-to").flatMap(parseDoor),
            swing     = props.one("swing").flatMap(parseSwing).getOrElse(DoorSwing.Default),
            swingTo   = props.one("swing-to").flatMap(parseSwing),
            direction = props.one("direction").flatMap(parseDirection),
          ))
        case Right(Line.GenerateDecl(n, style, seed)) =>
          stmts += GenStmt(DungeonMapSource.Generated(n, style, seed))
        case Right(Line.NoteDecl(side, roomId, text)) =>
          stmts += NoteStmt(Note(roomId, side, text))
        case Right(Line.MapHeader(_, _)) => ()
        case Right(Line.Prop(_, _))      => ()
        case Left(msg) =>
          return Left(ParseError(msg, 0, 1, 1))

    Right(stmts.toList)

  /** Property lines collected per room/connection, in declaration order. Repeating the same key on
   *  separate lines accumulates rather than overwrites — most properties are single-valued and only ever
   *  care about one occurrence (see `.one`), but room features (`stairs:`, `gi.<icon>:`, ...) can
   *  legitimately repeat to place more than one instance (e.g. two `gi.sarcophagus:` lines at different
   *  positions), which a plain last-wins `Map[String, String]` silently collapsed to one. */
  private def consumeProps(it: collection.BufferedIterator[String]): Map[String, List[String]] =
    val props = collection.mutable.Map[String, List[String]]()
    while it.hasNext && (it.head.startsWith(" ") || it.head.startsWith("\t") || it.head.isBlank) do
      classifyLine(it.next()) match
        case Right(Line.Prop(k, v)) => props(k) = props.getOrElse(k, Nil) :+ v
        case _ => ()
    props.toMap

  /** Single-valued property lookup (label, shape, door, corridor, ...) — last occurrence wins, matching
   *  this project's previous last-wins behavior for properties that only ever make sense once. */
  extension (props: Map[String, List[String]])
    private def one(key: String): Option[String] = props.get(key).flatMap(_.lastOption)

  // ── Feature parsing ────────────────────────────────────────────────────

  private def parseRoomFeatures(props: Map[String, List[String]], imports: Map[String, String]): List[RoomFeature] =
    val buf = List.newBuilder[RoomFeature]

    // Vertical movement — repeatable: multiple `stairs:`/`spiral-stairs:`/`ladder:` lines in one room
    // (e.g. two separate staircases) each produce their own feature instance, positioned like any Icon.
    props.getOrElse("stairs", Nil).flatMap(parseStairs).foreach { case (d, f, p) => buf += RoomFeature.Stairs(d, f, p) }
    props.getOrElse("spiral-stairs", Nil).flatMap(parseStairDirAndPosition).foreach { case (d, p) => buf += RoomFeature.SpiralStairs(d, p) }
    props.getOrElse("ladder", Nil).flatMap(parseStairDirAndPosition).foreach { case (d, p) => buf += RoomFeature.Ladder(d, p) }

    // Window — the only wall opening still a hardcoded direct SVG symbol,
    // alongside doors; supports comma-separated sides on one line (e.g. `window: north,south`)
    // and repeated `window:` lines alike.
    props.getOrElse("window", Nil).foreach(_.split(",").flatMap(s => parseWallSide(s.strip())).foreach(s => buf += RoomFeature.Window(s)))

    // A curated set of common furnishing icons (torch, chest, barrel, ...) is bundled directly into the
    // renderer and usable with no `import` at all, keyed by their bare name (e.g. `torch:`). See
    // `BuiltinIcons` for the full list and `doc/icons/builtin/SOURCES.md` for attribution. Same value
    // vocabulary (size/position/comma-list-of-sides/repeated-key) as the imported-icon case below.
    for
      (key, values)     <- props.toList.sortBy(_._1)
      if BuiltinIcons.paths.contains(key)
      value             <- values
      (size, position)  <- parseIconValue(value)
    do
      buf += RoomFeature.Icon(BuiltinIcons.iconSetName, key, size, position)

    // Every other room feature — free-standing structural/natural or a wall
    // furnishing (fireplace, bed, curtain, arrow slit, illusory wall, ...) — is
    // an icon from an imported Iconify icon set, keyed `<alias>.<icon-name>`
    // (e.g. `gi.stalactites`, given a header `import .../game-icons as gi`).
    // The value is either a size (`2`, `2x3`), a single position (`north`,
    // `2,1`), or — matching the old wall features' comma-separated-sides
    // support — a comma list of wall-side words (`north,south`), which creates
    // one Icon per side instead of a single positioned Icon. Repeating the same
    // `alias.icon-name:` key on separate lines (e.g. two `gi.sarcophagus:` lines
    // at different positions) likewise creates one Icon per line, not just one.
    // Iterated in sorted key order — `props`/`imports` are plain Maps with no
    // reliable iteration order, and feature order must be deterministic (both for
    // stable rendering and for DslFixtureRenderTest, which compares against a
    // hand-written Scala fixture listing features in this same sorted order).
    for
      (alias, iconSet) <- imports.toList.sortBy(_._1)
      (key, values)     <- props.toList.sortBy(_._1)
      iconName          <- Some(key).filter(_.startsWith(s"$alias.")).map(_.stripPrefix(s"$alias."))
      value             <- values
      (size, position)  <- parseIconValue(value)
    do
      buf += RoomFeature.Icon(iconSet, iconName, size, position)

    buf.result()

  private def parseFeatureSize(v: String): FeatureSize =
    v.strip() match
      case "" => FeatureSize.default
      case s if s.contains("x") =>
        s.split("x", 2) match
          case Array(w, h) => FeatureSize(w.strip().toIntOption.getOrElse(1), h.strip().toIntOption.getOrElse(1))
          case _           => FeatureSize.default
      case n => FeatureSize.square(n.toIntOption.getOrElse(1))

  /** A sized/positionable feature's single value: `north`/`south`/`east`/`west` (approximate
   *  position bias) or `col,row` (precise grid-cell position, e.g. `2,3`) set position with the
   *  default size; anything else (`2`, `2x3`, or empty) sets size with the default (Auto)
   *  position — size and an explicit position can't be combined in this one slot. */
  private def parseSizeOrPosition(v: String): (FeatureSize, FeaturePosition) =
    val s = v.strip()
    parseWallSide(s) match
      case Some(side) => (FeatureSize.default, FeaturePosition.Side(side))
      case None =>
        s.split(",", 2) match
          case Array(c, r) if c.strip().toIntOption.isDefined && r.strip().toIntOption.isDefined =>
            (FeatureSize.default, FeaturePosition.At(c.strip().toInt, r.strip().toInt))
          case _ =>
            (parseFeatureSize(s), FeaturePosition.Auto)

  /** An icon feature's value, which may expand to more than one feature: a comma
   *  list where every part is a wall-side word (e.g. `north,south`) yields one
   *  `(default size, Side(that wall))` pair per side — the multi-instance
   *  wall-furnishing case (`gi.bed: north,south` → two beds). Anything else
   *  (a single value, or a `col,row` coordinate, which is also comma-separated
   *  but isn't all wall-side words) falls back to `parseSizeOrPosition`. */
  private def parseIconValue(v: String): List[(FeatureSize, FeaturePosition)] =
    val parts = v.strip().split(",").map(_.strip())
    if parts.length > 1 && parts.forall(parseWallSide(_).isDefined) then
      parts.toList.flatMap(p => parseWallSide(p).map(side => (FeatureSize.default, FeaturePosition.Side(side))))
    else
      List(parseSizeOrPosition(v))

  private def parseWallSide(s: String): Option[WallSide] = s.toLowerCase match
    case "north" | "n" => Some(WallSide.North)
    case "south" | "s" => Some(WallSide.South)
    case "east"  | "e" => Some(WallSide.East)
    case "west"  | "w" => Some(WallSide.West)
    case _             => None

  private def parseStairDir(s: String): Option[StairDir] = s.toLowerCase match
    case "up"   => Some(StairDir.Up)
    case "down" => Some(StairDir.Down)
    case _      => None

  /** A free-standing feature's position, same vocabulary as `parseSizeOrPosition` but position-only
   *  (stairs/spiral-stairs/ladder aren't resizable): a wall-side word biases toward that edge of the
   *  room, `col,row` places it at precise grid-cell coordinates. */
  private def parsePositionToken(s: String): Option[FeaturePosition] =
    parseWallSide(s) match
      case Some(side) => Some(FeaturePosition.Side(side))
      case None =>
        s.split(",", 2) match
          case Array(c, r) if c.strip().toIntOption.isDefined && r.strip().toIntOption.isDefined =>
            Some(FeaturePosition.At(c.strip().toInt, r.strip().toInt))
          case _ => None

  /** `up`/`down` alone (facing defaults to north, position to Auto/centred), `up west` (facing only),
   *  `up west 2,1` (facing + position), or `up 2,1` (position only, facing stays default) — the
   *  second token is treated as the facing wall if it parses as one, otherwise as a position, so
   *  facing can be skipped without an explicit placeholder. */
  private def parseStairs(s: String): Option[(StairDir, WallSide, FeaturePosition)] =
    s.strip().split("\\s+").toList match
      case dir :: Nil => parseStairDir(dir).map((_, WallSide.North, FeaturePosition.Auto))
      case dir :: second :: rest =>
        parseStairDir(dir).map { d =>
          parseWallSide(second) match
            case Some(f) =>
              val pos = rest.headOption.flatMap(parsePositionToken).getOrElse(FeaturePosition.Auto)
              (d, f, pos)
            case None =>
              (d, WallSide.North, parsePositionToken(second).getOrElse(FeaturePosition.Auto))
        }
      case _ => None

  /** `up`/`down` alone (position defaults to Auto/centred), or `up 2,1` — direction plus an optional
   *  position (used by `spiral-stairs:`/`ladder:`, which have no facing concept). */
  private def parseStairDirAndPosition(s: String): Option[(StairDir, FeaturePosition)] =
    s.strip().split("\\s+").toList match
      case dir :: Nil       => parseStairDir(dir).map((_, FeaturePosition.Auto))
      case dir :: pos :: _  => parseStairDir(dir).map(d => (d, parsePositionToken(pos).getOrElse(FeaturePosition.Auto)))
      case _                => None

  // ── Helpers ─────────────────────────────────────────────────────────────

  private def parseSize(s: String): Either[String, RoomSize] =
    s.split("x", 2) match
      case Array(w, h) =>
        (w.toIntOption, h.toIntOption) match
          case (Some(wv), Some(hv)) => Right(RoomSize(wv, hv))
          case _ => Left(s"Invalid size: $s")
      case _ => Left(s"Invalid size: $s")

  private def parseShape(s: String): Option[RoomShape] = s.toLowerCase match
    case "rectangular" => Some(RoomShape.Rectangular)
    case "circular"    => Some(RoomShape.Circular)
    case "cave"        => Some(RoomShape.Cave)
    case _             => None

  private def parseLabelStyle(s: String): Option[LabelStyle] = s.toLowerCase match
    case "legend" => Some(LabelStyle.Legend)
    case "inline" => Some(LabelStyle.Inline)
    case _        => None

  private def parseBackgroundStyle(s: String): Option[BackgroundStyle] = s.toLowerCase match
    case "plain"       => Some(BackgroundStyle.Plain)
    case "hatch"       => Some(BackgroundStyle.Hatch)
    case "shadow-edge" => Some(BackgroundStyle.ShadowEdge)
    case _             => None

  private def parseDoor(s: String): Option[DoorType] = s.toLowerCase match
    case "open"   => Some(DoorType.Open)
    case "closed" => Some(DoorType.Closed)
    case "locked" => Some(DoorType.Locked)
    case "secret" => Some(DoorType.Secret)
    case _        => None

  private def parseSwing(s: String): Option[DoorSwing] = s.toLowerCase match
    case "default"          => Some(DoorSwing.Default)
    case "inside"  | "in"   => Some(DoorSwing.Inside)
    case "outside" | "out"  => Some(DoorSwing.Outside)
    case _                  => None

  /** Accepts both compass (north/south/east/west, n/s/e/w) and screen-relative
   *  (up/down/left/right, u/d/l/r) spellings for the same four directions. */
  private def parseDirection(s: String): Option[WallSide] = s.toLowerCase match
    case "north" | "n" | "up"    | "u" => Some(WallSide.North)
    case "south" | "s" | "down"  | "d" => Some(WallSide.South)
    case "east"  | "e" | "right" | "r" => Some(WallSide.East)
    case "west"  | "w" | "left"  | "l" => Some(WallSide.West)
    case _                              => None

  private def parseKvs(tokens: List[String]): Map[String, String] =
    tokens.flatMap { t =>
      t.split(":", 2) match
        case Array(k, v) => Some(k -> v)
        case _           => None
    }.toMap

  extension (s: String)
    private def stripQuotes: String = s.stripPrefix("\"").stripSuffix("\"")
