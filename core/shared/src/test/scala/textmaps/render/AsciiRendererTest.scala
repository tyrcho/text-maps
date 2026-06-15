package textmaps.render

import textmaps.*
import textmaps.dsl.*
import textmaps.layout.LayoutEngine

class AsciiRendererTest extends munit.FunSuite:

  override def beforeAll(): Unit = TestFilesPlatformInit.init()

  private def approvalTest(name: String, map: textmaps.layout.RenderedMap): Unit =
    val actual = AsciiRenderer.render(map)
    if TestFiles.shouldUpdate then
      TestFiles.writeApproved(name, actual)
      println(s"[approved] wrote core/testdata/ascii/$name.approved.txt")
    else
      val expected = TestFiles.readApproved(name)
      assertEquals(actual, expected)

  test("two connected rooms"):
    val rooms = List(
      Room("entrance", RoomSize(4, 4), Some("Entry")),
      Room("hall",     RoomSize(8, 4), Some("Great Hall")),
    )
    val conns = List(Connection("entrance", "hall", DoorType.Open))
    approvalTest("two_rooms",
      LayoutEngine.layout(rooms, conns, None))

  test("locked door between rooms"):
    val rooms = List(
      Room("guard",  RoomSize(3, 3), Some("Guard")),
      Room("prison", RoomSize(4, 3), Some("Prison")),
    )
    val conns = List(Connection("guard", "prison", DoorType.Locked))
    approvalTest("locked_door",
      LayoutEngine.layout(rooms, conns, None))

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
    approvalTest("three_rooms_chain",
      LayoutEngine.layout(rooms, conns, None))

  test("single room"):
    val rooms = List(Room("alone", RoomSize(5, 3), Some("Alone")))
    approvalTest("single_room",
      LayoutEngine.layout(rooms, Nil, None))

  test("empty map"):
    val actual = AsciiRenderer.render(LayoutEngine.layout(Nil, Nil, None))
    assertEquals(actual, "(empty)")
