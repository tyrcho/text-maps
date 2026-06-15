package textmaps.dsl

class DslParserTest extends munit.FunSuite:

  test("parses minimal dungeon header"):
    val result = DslParser.parse("map dungeon\n")
    assert(result.isRight, result)

  test("parses room declarations"):
    val input =
      """map dungeon
        |room entrance 4x4
        |room hall 8x6
        |""".stripMargin
    val result = DslParser.parse(input)
    result match
      case Right(DungeonMap(_, DungeonMapSource.Manual(rooms, _))) =>
        assertEquals(rooms.length, 2)
        assertEquals(rooms.head.id, "entrance")
        assertEquals(rooms.head.size, RoomSize(4, 4))
        assertEquals(rooms(1).size, RoomSize(8, 6))
      case other => fail(s"unexpected: $other")

  test("parses room with label and shape"):
    val input =
      """map dungeon
        |room vault 3x3
        |  label: "Treasury"
        |  shape: circular
        |""".stripMargin
    DslParser.parse(input) match
      case Right(DungeonMap(_, DungeonMapSource.Manual(rooms, _))) =>
        assertEquals(rooms.head.label, Some("Treasury"))
        assertEquals(rooms.head.shape, RoomShape.Circular)
      case other => fail(s"unexpected: $other")

  test("parses connections with door type"):
    val input =
      """map dungeon
        |room a 3x3
        |room b 3x3
        |connect a -> b
        |  door: locked
        |""".stripMargin
    DslParser.parse(input) match
      case Right(DungeonMap(_, DungeonMapSource.Manual(_, conns))) =>
        assertEquals(conns.length, 1)
        assertEquals(conns.head.from, "a")
        assertEquals(conns.head.to, "b")
        assertEquals(conns.head.door, DoorType.Locked)
      case other => fail(s"unexpected: $other")

  test("parses map metadata"):
    val input =
      """map dungeon "The Keep"
        |  seed: 42
        |  style: classic
        |""".stripMargin
    DslParser.parse(input) match
      case Right(DungeonMap(meta, _)) =>
        assertEquals(meta.seed, Some(42L))
        assertEquals(meta.style, Some("classic"))
      case other => fail(s"unexpected: $other")

  test("parses generate statement"):
    val input = "map dungeon\ngenerate dungeon rooms:8 style:classic seed:99\n"
    DslParser.parse(input) match
      case Right(DungeonMap(_, DungeonMapSource.Generated(n, style, seed))) =>
        assertEquals(n, 8)
        assertEquals(style, Some("classic"))
        assertEquals(seed, Some(99L))
      case other => fail(s"unexpected: $other")

  test("returns parse error on bad input"):
    val result = DslParser.parse("not valid dungeon dsl")
    assert(result.isLeft)

  test("ignores comments"):
    val input =
      """# This is a comment
        |map dungeon
        |# another comment
        |room hall 4x4
        |""".stripMargin
    val result = DslParser.parse(input)
    assert(result.isRight, result)
