package textmaps.render

import java.nio.file.{Files, Path}
import munit.FunSuite
import textmaps.dsl.*
import textmaps.layout.LayoutEngine

class AsciiRendererTest extends FunSuite:

  private val approvedDir = Path.of("core/testdata/ascii")
  private val shouldUpdate = sys.env.get("UPDATE_SNAPSHOTS").contains("1")

  private def approvalTest(name: String, map: textmaps.layout.RenderedMap): Unit =
    val actual  = AsciiRenderer.render(map)
    val file    = approvedDir.resolve(s"$name.approved.txt")
    if shouldUpdate then
      Files.createDirectories(approvedDir)
      Files.writeString(file, actual)
    else
      val expected = Files.readString(file)
      assertEquals(actual, expected)

  test("two connected rooms"):
    val rooms = List(
      Room("entrance", RoomSize(4, 4), Some("Entry")),
      Room("hall",     RoomSize(8, 4), Some("Great Hall")),
    )
    val conns = List(Connection("entrance", "hall", DoorType.Open))
    approvalTest("two_rooms", LayoutEngine.layout(rooms, conns, None))

  test("locked door between rooms"):
    val rooms = List(
      Room("guard",  RoomSize(3, 3), Some("Guard")),
      Room("prison", RoomSize(4, 3), Some("Prison")),
    )
    val conns = List(Connection("guard", "prison", DoorType.Locked))
    approvalTest("locked_door", LayoutEngine.layout(rooms, conns, None))

  test("three rooms in a chain"):
    val rooms = List(
      Room("entrance", RoomSize(4, 4), Some("Entrance")),
      Room("hall",     RoomSize(6, 4), Some("Hall")),
      Room("vault",    RoomSize(3, 3), Some("Vault")),
    )
    val conns = List(
      Connection("entrance", "hall",  DoorType.Open),
      Connection("hall",     "vault", DoorType.Secret),
    )
    approvalTest("three_rooms_chain", LayoutEngine.layout(rooms, conns, None))

  test("single room"):
    val rooms = List(Room("alone", RoomSize(5, 3), Some("Alone")))
    approvalTest("single_room", LayoutEngine.layout(rooms, Nil, None))

  test("empty map"):
    assertEquals(AsciiRenderer.render(LayoutEngine.layout(Nil, Nil, None)), "(empty)")
