package textmaps.render

import textmaps.dsl.*
import textmaps.layout.{LayoutEngine, RenderedMap}

/** Canonical map definitions used by SvgStringRendererTest. */
object TestMaps:

  val twoConnectedRooms: RenderedMap = LayoutEngine.layout(
    List(
      Room("entrance", RoomSize(4, 4), Some("Entry")),
      Room("hall",     RoomSize(8, 4), Some("Great Hall")),
    ),
    List(Connection("entrance", "hall", DoorType.Open)),
    None,
  )

  val lockedDoorBetweenRooms: RenderedMap = LayoutEngine.layout(
    List(
      Room("guard",  RoomSize(3, 3), Some("Guard")),
      Room("prison", RoomSize(4, 3), Some("Prison")),
    ),
    List(Connection("guard", "prison", DoorType.Locked)),
    None,
  )

  val threeRoomsInAChain: RenderedMap = LayoutEngine.layout(
    List(
      Room("entrance", RoomSize(4, 4), Some("Entrance")),
      Room("hall",     RoomSize(6, 4), Some("Hall")),
      Room("vault",    RoomSize(3, 3), Some("Vault")),
    ),
    List(
      Connection("entrance", "hall",  DoorType.Open),
      Connection("hall",     "vault", DoorType.Secret),
    ),
    None,
  )

  val singleRoom: RenderedMap = LayoutEngine.layout(
    List(Room("alone", RoomSize(5, 3), Some("Alone"))),
    Nil,
    None,
  )

  val emptyMap: RenderedMap = LayoutEngine.layout(Nil, Nil, None)

  val dungeonRoomWithStairsAndWindows: RenderedMap = LayoutEngine.layout(
    List(
      Room("entrance", RoomSize(4, 3), Some("Entry")),
      Room("vault", RoomSize(3, 3), Some("Vault"),
        features = List(RoomFeature.Stairs(StairDir.Up, WallSide.West), RoomFeature.Window(WallSide.North))),
    ),
    List(Connection("entrance", "vault", DoorType.Locked)),
    None,
  )

  /** Explicit connection direction + independent per-end swing arcs, on a Building map. */
  val buildingWithDirectionalDoors: RenderedMap = LayoutEngine.layout(
    List(
      Room("hall", RoomSize(5, 4), Some("Hall"),
        features = List(RoomFeature.Window(WallSide.North))),
      Room("kitchen", RoomSize(3, 3), Some("Kitchen")),
    ),
    List(Connection(
      "hall", "kitchen", DoorType.Open,
      corridor = Some(RoomSize(1, 3)),
      doorTo   = Some(DoorType.Locked),
      swing    = DoorSwing.Outside,
      swingTo  = Some(DoorSwing.Inside),
      direction = Some(WallSide.East),
    )),
    None,
    MapType.Building,
  )

  /** The 4 door types plus swing arcs, chained through 3 explicit cardinal directions. */
  val doorTypesAndSwing: RenderedMap = LayoutEngine.layout(
    List(
      Room("a", RoomSize(3, 3), Some("A")),
      Room("b", RoomSize(3, 3), Some("B")),
      Room("c", RoomSize(3, 3), Some("C")),
      Room("d", RoomSize(3, 3), Some("D")),
    ),
    List(
      Connection("a", "b", DoorType.Closed, swing = DoorSwing.Outside, direction = Some(WallSide.East)),
      Connection("b", "c", DoorType.Locked, swing = DoorSwing.Inside,  direction = Some(WallSide.South)),
      Connection("c", "d", DoorType.Secret, swing = DoorSwing.Inside,  direction = Some(WallSide.West)),
    ),
    None,
  )

  val movementFeatures: RenderedMap = LayoutEngine.layout(
    List(
      Room("top",    RoomSize(4, 3), Some("Top"),    features = List(RoomFeature.SpiralStairs(StairDir.Down))),
      Room("middle", RoomSize(4, 3), Some("Middle"), features = List(RoomFeature.Ladder(StairDir.Up))),
      Room("bottom", RoomSize(4, 3), Some("Bottom"), features = List(RoomFeature.Ladder(StairDir.Down))),
    ),
    List(
      Connection("top",    "middle", DoorType.Open),
      Connection("middle", "bottom", DoorType.Open),
    ),
    None,
  )

  /** Also exercises feature positioning: cave-entrance/ionic-column use an approximate
   *  side-bias position, colombian-statue uses a precise grid-cell coordinate;
   *  stalactites/spikes/water-drop are left at their Auto defaults for comparison. All
   *  icons are from the "game-icons" Iconify set (DSL: `import ... as gi`).
   *
   *  Feature order within each room matches DslParser's sorted-by-property-key
   *  order (`gi.<name>` alphabetically) so this fixture stays byte-identical to
   *  what parsing the equivalent `.dsl` file produces. */
  val naturalStructuralFeatures: RenderedMap = LayoutEngine.layout(
    List(
      Room("cave", RoomSize(6, 4), Some("Cave"),
        features = List(
          RoomFeature.Icon("game-icons", "cave-entrance", position = FeaturePosition.Side(WallSide.West)),
          RoomFeature.Icon("game-icons", "spikes"),
          RoomFeature.Icon("game-icons", "stalactites"),
        )),
      Room("hall", RoomSize(5, 4), Some("Hall"),
        features = List(
          RoomFeature.Icon("game-icons", "colombian-statue", position = FeaturePosition.At(2, 1)),
          RoomFeature.Icon("game-icons", "ionic-column", position = FeaturePosition.Side(WallSide.North)),
          RoomFeature.Icon("game-icons", "water-drop"),
        )),
      Room("stream_room", RoomSize(5, 3), Some("Stream"),
        features = List(RoomFeature.Icon("game-icons", "splashy-stream"))),
    ),
    List(
      Connection("cave", "hall",        DoorType.Open),
      Connection("hall", "stream_room", DoorType.Open),
    ),
    None,
  )

  /** Wall furnishings (formerly hardcoded ArrowSlit/Bed/IllusoryWall/Fireplace/Curtain)
   *  are now icons too — only Window stays a hardcoded direct SVG symbol. Feature order
   *  matches DslParser's sorted-by-property-key order (`gi.<name>` alphabetically), and
   *  `gi.bed: north,south` expands to two Icon instances in that order, so this fixture
   *  stays byte-identical to what parsing the equivalent `.dsl` file produces. */
  val wallFeatures: RenderedMap = LayoutEngine.layout(
    List(
      Room("barracks", RoomSize(5, 4), Some("Barracks"),
        features = List(
          RoomFeature.Icon("game-icons", "bed", position = FeaturePosition.Side(WallSide.North)),
          RoomFeature.Icon("game-icons", "bed", position = FeaturePosition.Side(WallSide.South)),
          RoomFeature.Icon("game-icons", "watchtower", position = FeaturePosition.Side(WallSide.East)),
        )),
      Room("hall", RoomSize(5, 4), Some("Hall"),
        features = List(
          RoomFeature.Icon("game-icons", "fireplace", position = FeaturePosition.Side(WallSide.North)),
          RoomFeature.Icon("game-icons", "invisible", position = FeaturePosition.Side(WallSide.South)),
          RoomFeature.Icon("game-icons", "theater-curtains", position = FeaturePosition.Side(WallSide.West)),
        )),
    ),
    List(Connection("barracks", "hall", DoorType.Open)),
    None,
  )

  val caveRoom: RenderedMap = LayoutEngine.layout(
    List(
      Room("entrance", RoomSize(4, 3), Some("Entrance")),
      Room("cavern",   RoomSize(6, 5), Some("Cavern"), shape = RoomShape.Cave),
    ),
    List(Connection("entrance", "cavern", DoorType.Open)),
    None,
  )

  val dungeonInlineLabels: RenderedMap = LayoutEngine.layout(
    List(
      Room("entrance", RoomSize(4, 4), Some("Entry")),
      Room("hall",     RoomSize(6, 4), Some("Hall")),
    ),
    List(Connection("entrance", "hall", DoorType.Open)),
    None,
    MapType.Dungeon,
    Some(LabelStyle.Inline),
  )
