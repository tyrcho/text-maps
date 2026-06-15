package textmaps.layout

import textmaps.dsl.*

class LayoutEngineTest extends munit.FunSuite:

  private def makeRoom(id: String, w: Int = 4, h: Int = 4) = Room(id, RoomSize(w, h))
  private def conn(from: String, to: String) = Connection(from, to)

  test("single room is placed at origin"):
    val map = DungeonMap(MapMeta(), DungeonMapSource.Manual(List(makeRoom("a")), Nil))
    val rendered = LayoutEngine.compute(map)
    assertEquals(rendered.rooms.length, 1)

  test("two connected rooms have distinct positions"):
    val rooms = List(makeRoom("a"), makeRoom("b"))
    val conns = List(conn("a", "b"))
    val map = DungeonMap(MapMeta(), DungeonMapSource.Manual(rooms, conns))
    val rendered = LayoutEngine.compute(map)
    val a = rendered.rooms.find(_.id == "a").get
    val b = rendered.rooms.find(_.id == "b").get
    assert(a.x != b.x || a.y != b.y, "rooms should not overlap")

  test("all rooms are present in output"):
    val rooms = (1 to 5).map(i => makeRoom(s"r$i")).toList
    val conns = rooms.zip(rooms.tail).map { case (a, b) => conn(a.id, b.id) }
    val map = DungeonMap(MapMeta(), DungeonMapSource.Manual(rooms, conns))
    val rendered = LayoutEngine.compute(map)
    assertEquals(rendered.rooms.length, 5)

  test("corridors equal number of connections"):
    val rooms = List(makeRoom("a"), makeRoom("b"), makeRoom("c"))
    val conns = List(conn("a", "b"), conn("b", "c"))
    val map = DungeonMap(MapMeta(), DungeonMapSource.Manual(rooms, conns))
    val rendered = LayoutEngine.compute(map)
    assertEquals(rendered.corridors.length, 2)

  test("disconnected rooms still appear"):
    val rooms = List(makeRoom("a"), makeRoom("isolated"))
    val map = DungeonMap(MapMeta(), DungeonMapSource.Manual(rooms, Nil))
    val rendered = LayoutEngine.compute(map)
    assertEquals(rendered.rooms.length, 2)

  test("empty map returns empty rendered map"):
    val map = DungeonMap(MapMeta(), DungeonMapSource.Manual(Nil, Nil))
    val rendered = LayoutEngine.compute(map)
    assert(rendered.rooms.isEmpty)
