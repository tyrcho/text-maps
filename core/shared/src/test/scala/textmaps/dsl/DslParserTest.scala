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

  test("parses map building"):
    val input =
      """map building "The Inn"
        |room lobby 4x4
        |  label: "Lobby"
        |  window: north, east
        |""".stripMargin
    DslParser.parse(input) match
      case Right(DungeonMap(meta, DungeonMapSource.Manual(rooms, _))) =>
        assertEquals(meta.mapType, MapType.Building)
        assertEquals(rooms.head.features.count { case RoomFeature.Window(_) => true; case _ => false }, 2)
        assert(rooms.head.features.contains(RoomFeature.Window(WallSide.North)))
        assert(rooms.head.features.contains(RoomFeature.Window(WallSide.East)))
      case other => fail(s"unexpected: $other")

  test("parses door-to, swing, swing-to and direction on a connection"):
    val input =
      """map dungeon
        |room a 3x3
        |room b 3x3
        |connect a -> b
        |  direction: north
        |  door: locked
        |  door-to: open
        |  swing: inside
        |  swing-to: outside
        |""".stripMargin
    DslParser.parse(input) match
      case Right(DungeonMap(_, DungeonMapSource.Manual(_, conns))) =>
        val c = conns.head
        assertEquals(c.direction, Some(WallSide.North))
        assertEquals(c.door, DoorType.Locked)
        assertEquals(c.doorTo, Some(DoorType.Open))
        assertEquals(c.swing, DoorSwing.Inside)
        assertEquals(c.swingTo, Some(DoorSwing.Outside))
      case other => fail(s"unexpected: $other")

  test("parses direction aliases (udlr)"):
    val input =
      """map dungeon
        |room a 3x3
        |room b 3x3
        |connect a -> b
        |  direction: r
        |""".stripMargin
    DslParser.parse(input) match
      case Right(DungeonMap(_, DungeonMapSource.Manual(_, conns))) =>
        assertEquals(conns.head.direction, Some(WallSide.East))
      case other => fail(s"unexpected: $other")

  test("parses an import statement and resolves alias.icon-name features"):
    val input =
      """import icon-sets.iconify.design/game-icons as gi
        |map dungeon
        |room cave 6x5
        |  gi.stalactites:
        |""".stripMargin
    DslParser.parse(input) match
      case Right(DungeonMap(_, DungeonMapSource.Manual(rooms, _))) =>
        assert(rooms.head.features.contains(RoomFeature.Icon("game-icons", "stalactites")))
      case other => fail(s"unexpected: $other")

  test("import statement works after the map header too"):
    val input =
      """map dungeon
        |import icon-sets.iconify.design/game-icons as gi
        |room cave 6x5
        |  gi.stalactites:
        |""".stripMargin
    DslParser.parse(input) match
      case Right(DungeonMap(_, DungeonMapSource.Manual(rooms, _))) =>
        assert(rooms.head.features.contains(RoomFeature.Icon("game-icons", "stalactites")))
      case other => fail(s"unexpected: $other")

  test("icon feature position (approximate and precise)"):
    val input =
      """import icon-sets.iconify.design/game-icons as gi
        |map dungeon
        |room cave 6x5
        |  gi.ionic-column: north
        |  gi.colombian-statue: 2,3
        |""".stripMargin
    DslParser.parse(input) match
      case Right(DungeonMap(_, DungeonMapSource.Manual(rooms, _))) =>
        assert(rooms.head.features.contains(RoomFeature.Icon("game-icons", "ionic-column", FeatureSize.default, FeaturePosition.Side(WallSide.North))))
        assert(rooms.head.features.contains(RoomFeature.Icon("game-icons", "colombian-statue", FeatureSize.default, FeaturePosition.At(2, 3))))
      case other => fail(s"unexpected: $other")

  test("icon feature size (no position)"):
    val input =
      """import icon-sets.iconify.design/game-icons as gi
        |map dungeon
        |room cave 6x5
        |  gi.ionic-column: 2
        |  gi.colombian-statue: 2x3
        |""".stripMargin
    DslParser.parse(input) match
      case Right(DungeonMap(_, DungeonMapSource.Manual(rooms, _))) =>
        assert(rooms.head.features.contains(RoomFeature.Icon("game-icons", "ionic-column", FeatureSize.square(2), FeaturePosition.Auto)))
        assert(rooms.head.features.contains(RoomFeature.Icon("game-icons", "colombian-statue", FeatureSize(2, 3), FeaturePosition.Auto)))
      case other => fail(s"unexpected: $other")

  test("a comma list of wall sides creates one icon instance per side (wall-furnishing case)"):
    val input =
      """import icon-sets.iconify.design/game-icons as gi
        |map dungeon
        |room barracks 5x4
        |  gi.bed: north,south
        |""".stripMargin
    DslParser.parse(input) match
      case Right(DungeonMap(_, DungeonMapSource.Manual(rooms, _))) =>
        assertEquals(rooms.head.features, List(
          RoomFeature.Icon("game-icons", "bed", FeatureSize.default, FeaturePosition.Side(WallSide.North)),
          RoomFeature.Icon("game-icons", "bed", FeatureSize.default, FeaturePosition.Side(WallSide.South)),
        ))
      case other => fail(s"unexpected: $other")

  test("a col,row coordinate is not mistaken for a wall-side comma list"):
    val input =
      """import icon-sets.iconify.design/game-icons as gi
        |map dungeon
        |room cave 6x5
        |  gi.colombian-statue: 2,3
        |""".stripMargin
    DslParser.parse(input) match
      case Right(DungeonMap(_, DungeonMapSource.Manual(rooms, _))) =>
        assertEquals(rooms.head.features, List(RoomFeature.Icon("game-icons", "colombian-statue", FeatureSize.default, FeaturePosition.At(2, 3))))
      case other => fail(s"unexpected: $other")

  test("a property key with a dot but no matching import is ignored, not treated as an icon"):
    val input =
      """map dungeon
        |room cave 6x5
        |  gi.stalactites: north
        |""".stripMargin
    DslParser.parse(input) match
      case Right(DungeonMap(_, DungeonMapSource.Manual(rooms, _))) =>
        assertEquals(rooms.head.features, Nil)
      case other => fail(s"unexpected: $other")

  test("parses stairs feature (facing defaults to north)"):
    val input =
      """map dungeon
        |room vault 3x3
        |  stairs: up
        |""".stripMargin
    DslParser.parse(input) match
      case Right(DungeonMap(_, DungeonMapSource.Manual(rooms, _))) =>
        assert(rooms.head.features.contains(RoomFeature.Stairs(StairDir.Up, WallSide.North)))
      case other => fail(s"unexpected: $other")

  test("parses stairs feature with explicit facing"):
    val input =
      """map dungeon
        |room vault 3x3
        |  stairs: down west
        |""".stripMargin
    DslParser.parse(input) match
      case Right(DungeonMap(_, DungeonMapSource.Manual(rooms, _))) =>
        assert(rooms.head.features.contains(RoomFeature.Stairs(StairDir.Down, WallSide.West)))
      case other => fail(s"unexpected: $other")

  test("ignores comments"):
    val input =
      """# This is a comment
        |map dungeon
        |# another comment
        |room hall 4x4
        |""".stripMargin
    val result = DslParser.parse(input)
    assert(result.isRight, result)
