package textmaps.render

import java.nio.file.{Files, Path}
import org.scalatest.funsuite.AnyFunSuite
import textmaps.dsl.*
import textmaps.layout.LayoutEngine

/** Approval tests for SvgStringRenderer.
 *
 *  Approved files live in src/test/resources/textmaps/render/SvgStringRendererTest.files/
 *  as *.approved.svg — proper extension so editors render and diff them correctly.
 *
 *  To regenerate all approved files: UPDATE_SNAPSHOTS=1 make test
 */
class SvgStringRendererTest extends AnyFunSuite:

  private val approvedDir  = Path.of("src/test/resources/textmaps/render/SvgStringRendererTest.files")
  private val shouldUpdate = sys.env.get("UPDATE_SNAPSHOTS").contains("1")

  private def svgApproval(name: String, map: textmaps.layout.RenderedMap): Unit =
    val actual = SvgStringRenderer.render(map)
    val file   = approvedDir.resolve(s"$name.approved.svg")
    if shouldUpdate then
      Files.createDirectories(approvedDir)
      Files.writeString(file, actual)
    else
      val expected = Files.readString(file)
      assert(actual == expected, s"SVG output changed for '$name' — run UPDATE_SNAPSHOTS=1 to re-approve")

  test("two connected rooms"):
    svgApproval("two_connected_rooms", LayoutEngine.layout(
      List(
        Room("entrance", RoomSize(4, 4), Some("Entry")),
        Room("hall",     RoomSize(8, 4), Some("Great Hall")),
      ),
      List(Connection("entrance", "hall", DoorType.Open)),
      None,
    ))

  test("locked door between rooms"):
    svgApproval("locked_door_between_rooms", LayoutEngine.layout(
      List(
        Room("guard",  RoomSize(3, 3), Some("Guard")),
        Room("prison", RoomSize(4, 3), Some("Prison")),
      ),
      List(Connection("guard", "prison", DoorType.Locked)),
      None,
    ))

  test("three rooms in a chain"):
    svgApproval("three_rooms_in_a_chain", LayoutEngine.layout(
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
    svgApproval("single_room", LayoutEngine.layout(
      List(Room("alone", RoomSize(5, 3), Some("Alone"))),
      Nil,
      None,
    ))

  test("empty map"):
    svgApproval("empty_map", LayoutEngine.layout(Nil, Nil, None))

  test("dungeon room with stairs and windows"):
    svgApproval("dungeon_room_with_stairs_and_windows", LayoutEngine.layout(
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
    svgApproval("building_with_exterior_exits", LayoutEngine.layout(
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
