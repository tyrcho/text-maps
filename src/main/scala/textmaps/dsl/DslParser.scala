package textmaps.dsl

/** Line-oriented DSL parser. Each top-level declaration starts at column 0;
 *  properties are indented with at least one space.
 */
object DslParser:

  def parse(input: String): ParseResult =
    val lines = input.linesIterator.toList
    parseLines(lines)

  // ── Line classification ────────────────────────────────────────────────

  private enum Line:
    case Blank
    case Comment
    case MapHeader(name: Option[String])
    case RoomDecl(id: String, size: RoomSize)
    case ConnDecl(from: String, to: String)
    case GenerateDecl(roomCount: Int, style: Option[String], seed: Option[Long])
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
      val tokens = line.split("\\s+").toList
      tokens match
        case "map" :: "dungeon" :: rest =>
          val name = if rest.nonEmpty then Some(rest.mkString(" ").stripQuotes) else None
          Right(Line.MapHeader(name))
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

  private def parseLines(lines: List[String]): ParseResult =
    // Require map header as first non-blank line
    val nonBlank = lines.indexWhere(l => l.strip().nonEmpty && !l.strip().startsWith("#"))
    if nonBlank < 0 then
      return Left(ParseError("Empty document", 0, 1, 1))

    // Parse header line
    val headerLine = lines(nonBlank)
    val metaResult = classifyLine(headerLine) match
      case Right(Line.MapHeader(name)) => Right(MapMeta(name = name))
      case _ => Left(ParseError(s"Expected 'map dungeon' but got: ${headerLine.take(40)}", 0, nonBlank + 1, 1))

    metaResult.flatMap { baseMeta =>
      // Collect props after header (indented lines before first decl)
      val afterHeader = lines.drop(nonBlank + 1)
      val (headerProps, rest) = afterHeader.span(l => l.startsWith(" ") || l.startsWith("\t") || l.isBlank)
      val metaProps = headerProps.flatMap(l => classifyLine(l).toOption.collect { case Line.Prop(k, v) => k -> v }).toMap
      val meta = baseMeta.copy(
        seed  = metaProps.get("seed").flatMap(_.toLongOption),
        style = metaProps.get("style"),
      )

      // Parse remaining statements
      parseStatements(rest).map { stmts =>
        val genOpt = stmts.collectFirst { case GenStmt(g) => g }
        val source = genOpt.getOrElse {
          DungeonMapSource.Manual(
            rooms = stmts.collect { case RoomStmt(r) => r },
            connections = stmts.collect { case ConnStmt(c) => c },
          )
        }
        DungeonMap(meta, source)
      }
    }

  private def parseStatements(lines: List[String]): Either[ParseError, List[Stmt]] =
    val stmts = collection.mutable.ListBuffer[Stmt]()
    val it    = lines.iterator.buffered

    while it.hasNext do
      val (lineNo, raw) = it.next() match { case s => (0, s) }
      classifyLine(raw) match
        case Right(Line.Blank) | Right(Line.Comment) => ()
        case Right(Line.RoomDecl(id, size)) =>
          val props = consumeProps(it)
          stmts += RoomStmt(Room(
            id    = id,
            size  = size,
            label = props.get("label"),
            shape = props.get("shape").flatMap(parseShape).getOrElse(RoomShape.Rectangular),
          ))
        case Right(Line.ConnDecl(from, to)) =>
          val props = consumeProps(it)
          stmts += ConnStmt(Connection(
            from     = from,
            to       = to,
            door     = props.get("door").flatMap(parseDoor).getOrElse(DoorType.Open),
            corridor = props.get("corridor").flatMap(parseSize(_).toOption),
          ))
        case Right(Line.GenerateDecl(n, style, seed)) =>
          stmts += GenStmt(DungeonMapSource.Generated(n, style, seed))
        case Right(Line.MapHeader(_)) => () // ignore nested headers
        case Right(Line.Prop(_, _))   => () // top-level props — ignore
        case Left(msg) =>
          return Left(ParseError(msg, 0, 1, 1))

    Right(stmts.toList)

  private def consumeProps(it: collection.BufferedIterator[String]): Map[String, String] =
    val props = collection.mutable.Map[String, String]()
    while it.hasNext && (it.head.startsWith(" ") || it.head.startsWith("\t") || it.head.isBlank) do
      val line = it.next()
      classifyLine(line) match
        case Right(Line.Prop(k, v)) => props(k) = v
        case _ => ()
    props.toMap

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
    case _             => None

  private def parseDoor(s: String): Option[DoorType] = s.toLowerCase match
    case "open"   => Some(DoorType.Open)
    case "locked" => Some(DoorType.Locked)
    case "secret" => Some(DoorType.Secret)
    case "barred" => Some(DoorType.Barred)
    case _        => None

  private def parseKvs(tokens: List[String]): Map[String, String] =
    tokens.flatMap { t =>
      t.split(":", 2) match
        case Array(k, v) => Some(k -> v)
        case _           => None
    }.toMap

  extension (s: String)
    private def stripQuotes: String = s.stripPrefix("\"").stripSuffix("\"")
