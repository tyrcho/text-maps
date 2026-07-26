package textmaps.render

import textmaps.dsl.*
import textmaps.layout.{LayoutEngine, RenderedMap}

/** Canonical map definitions shared between AsciiRendererTest and SvgStringRendererTest. */
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
        features = List(RoomFeature.Stairs(StairDir.Up), RoomFeature.Window(WallSide.North))),
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

  /** Also exercises feature positioning: Crevasse/Pillar use an approximate `-at:`
   *  side bias, Statue uses a precise `-at:` grid-cell coordinate; Stalactite/
   *  Stalagmite/Pool are left at their Auto defaults for comparison. */
  val naturalStructuralFeatures: RenderedMap = LayoutEngine.layout(
    List(
      Room("cave", RoomSize(6, 4), Some("Cave"),
        features = List(
          RoomFeature.Stalactite(),
          RoomFeature.Stalagmite(),
          RoomFeature.Crevasse(position = FeaturePosition.Side(WallSide.West)),
        )),
      Room("hall", RoomSize(5, 4), Some("Hall"),
        features = List(
          RoomFeature.Pillar(position = FeaturePosition.Side(WallSide.North)),
          RoomFeature.Statue(position = FeaturePosition.At(2, 1)),
          RoomFeature.Pool(),
        )),
      Room("stream_room", RoomSize(5, 3), Some("Stream"),
        features = List(RoomFeature.Stream())),
    ),
    List(
      Connection("cave", "hall",        DoorType.Open),
      Connection("hall", "stream_room", DoorType.Open),
    ),
    None,
  )

  val wallFeatures: RenderedMap = LayoutEngine.layout(
    List(
      Room("barracks", RoomSize(5, 4), Some("Barracks"),
        // Order matches DslParser.parseRoomFeatures' fixed emission order
        // (arrow-slit before bed) so this fixture matches its DSL companion byte-for-byte.
        features = List(
          RoomFeature.ArrowSlit(WallSide.East),
          RoomFeature.Bed(WallSide.North),
          RoomFeature.Bed(WallSide.South),
        )),
      Room("hall", RoomSize(5, 4), Some("Hall"),
        // Order matches DslParser.parseRoomFeatures' fixed emission order
        // (illusory-wall, then fireplace, then curtain).
        features = List(
          RoomFeature.IllusoryWall(WallSide.South),
          RoomFeature.Fireplace(WallSide.North),
          RoomFeature.Curtain(WallSide.West),
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
