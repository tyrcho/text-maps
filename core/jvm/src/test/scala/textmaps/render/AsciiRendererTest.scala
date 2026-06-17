package textmaps.render

import java.nio.file.{Files, Path}
import org.scalatest.funsuite.AnyFunSuite
import textmaps.dsl.*
import textmaps.layout.LayoutEngine

/** Approval tests for AsciiRenderer.
 *
 *  Approved files live in core/shared/src/test/resources/textmaps/render/AsciiRendererTest.files/
 *  as *.approved.txt — plain text so editors and diffs display them correctly.
 *
 *  To regenerate all approved files: UPDATE_SNAPSHOTS=1 make test
 */
class AsciiRendererTest extends AnyFunSuite:

  private val approvedDir  = Path.of("core/shared/src/test/resources/textmaps/render/AsciiRendererTest.files")
  private val shouldUpdate = sys.env.get("UPDATE_SNAPSHOTS").contains("1")

  private def asciiApproval(name: String, map: textmaps.layout.RenderedMap): Unit =
    val actual = AsciiRenderer.render(map)
    val file   = approvedDir.resolve(s"$name.approved.txt")
    if shouldUpdate then
      Files.createDirectories(approvedDir)
      Files.writeString(file, actual)
    else
      val expected = Files.readString(file)
      assert(actual == expected, s"ASCII output changed for '$name' — run UPDATE_SNAPSHOTS=1 to re-approve")

  test("two connected rooms"):
    asciiApproval("two_connected_rooms", LayoutEngine.layout(
      List(
        Room("entrance", RoomSize(4, 4), Some("Entry")),
        Room("hall",     RoomSize(8, 4), Some("Great Hall")),
      ),
      List(Connection("entrance", "hall", DoorType.Open)),
      None,
    ))

  test("locked door between rooms"):
    asciiApproval("locked_door_between_rooms", LayoutEngine.layout(
      List(
        Room("guard",  RoomSize(3, 3), Some("Guard")),
        Room("prison", RoomSize(4, 3), Some("Prison")),
      ),
      List(Connection("guard", "prison", DoorType.Locked)),
      None,
    ))

  test("three rooms in a chain"):
    asciiApproval("three_rooms_in_a_chain", LayoutEngine.layout(
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
    ))

  test("single room"):
    asciiApproval("single_room", LayoutEngine.layout(
      List(Room("alone", RoomSize(5, 3), Some("Alone"))),
      Nil,
      None,
    ))

  test("empty map"):
    asciiApproval("empty_map", LayoutEngine.layout(Nil, Nil, None))

  test("dungeon room with stairs and windows"):
    asciiApproval("dungeon_room_with_stairs_and_windows", LayoutEngine.layout(
      List(
        Room("entrance", RoomSize(4, 3), Some("Entry"),
          features = List(RoomFeature.Exit(WallSide.West))),
        Room("vault", RoomSize(3, 3), Some("Vault"),
          features = List(RoomFeature.Stairs(StairDir.Up), RoomFeature.Window(WallSide.North))),
      ),
      List(Connection("entrance", "vault", DoorType.Locked)),
      None,
    ))

  test("building with exterior exits"):
    asciiApproval("building_with_exterior_exits", LayoutEngine.layout(
      List(
        Room("hall", RoomSize(5, 4), Some("Hall"),
          features = List(
            RoomFeature.Exit(WallSide.West),
            RoomFeature.Exit(WallSide.East),
            RoomFeature.Window(WallSide.North),
          )),
        Room("kitchen", RoomSize(3, 3), Some("Kitchen"),
          features = List(RoomFeature.Exit(WallSide.South))),
      ),
      List(Connection("hall", "kitchen", DoorType.Open)),
      None,
      MapType.Building,
    ))

  test("new door types"):
    asciiApproval("new_door_types", LayoutEngine.layout(
      List(
        Room("a", RoomSize(3, 3), Some("A")),
        Room("b", RoomSize(3, 3), Some("B")),
        Room("c", RoomSize(3, 3), Some("C")),
        Room("d", RoomSize(3, 3), Some("D")),
      ),
      List(
        Connection("a", "b", DoorType.Double),
        Connection("b", "c", DoorType.Doorway),
        Connection("c", "d", DoorType.Portcullis),
      ),
      None,
    ))

  test("movement features"):
    asciiApproval("movement_features", LayoutEngine.layout(
      List(
        Room("top",    RoomSize(4, 3), Some("Top"),    features = List(RoomFeature.SpiralStairs(StairDir.Down))),
        Room("middle", RoomSize(4, 3), Some("Middle"), features = List(RoomFeature.Ladder(StairDir.Up))),
        Room("bottom", RoomSize(4, 3), Some("Bottom"), features = List(RoomFeature.Ladder(StairDir.Down))),
      ),
      List(
        Connection("top", "middle", DoorType.Open),
        Connection("middle", "bottom", DoorType.Open),
      ),
      None,
    ))

  test("natural and structural features"):
    asciiApproval("natural_structural_features", LayoutEngine.layout(
      List(
        Room("cave", RoomSize(6, 4), Some("Cave"),
          features = List(
            RoomFeature.Stalactite,
            RoomFeature.Stalagmite,
            RoomFeature.Crevasse,
          )),
        Room("hall", RoomSize(5, 4), Some("Hall"),
          features = List(
            RoomFeature.Pillar,
            RoomFeature.Statue,
            RoomFeature.Pool,
          )),
        Room("stream_room", RoomSize(5, 3), Some("Stream"),
          features = List(RoomFeature.Stream)),
      ),
      List(
        Connection("cave", "hall", DoorType.Open),
        Connection("hall", "stream_room", DoorType.Open),
      ),
      None,
    ))

  test("wall features"):
    asciiApproval("wall_features", LayoutEngine.layout(
      List(
        Room("barracks", RoomSize(5, 4), Some("Barracks"),
          features = List(
            RoomFeature.Bed(WallSide.North),
            RoomFeature.Bed(WallSide.South),
            RoomFeature.ArrowSlit(WallSide.East),
          )),
        Room("hall", RoomSize(5, 4), Some("Hall"),
          features = List(
            RoomFeature.Fireplace(WallSide.North),
            RoomFeature.Curtain(WallSide.West),
            RoomFeature.IllusoryWall(WallSide.South),
          )),
      ),
      List(Connection("barracks", "hall", DoorType.Open)),
      None,
    ))
