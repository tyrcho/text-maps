package textmaps.generate

import textmaps.dsl.*

class DungeonGeneratorTest extends munit.FunSuite:

  test("generates correct number of rooms"):
    val source = DungeonGenerator.generate(8, Some(42L))
    source match
      case DungeonMapSource.Manual(rooms, _) => assertEquals(rooms.length, 8)
      case _ => fail("expected Manual source")

  test("generates connections between rooms"):
    val source = DungeonGenerator.generate(5, Some(1L))
    source match
      case DungeonMapSource.Manual(_, conns) => assert(conns.nonEmpty)
      case _ => fail("expected Manual source")

  test("first room is always named entrance"):
    val source = DungeonGenerator.generate(4, Some(7L))
    source match
      case DungeonMapSource.Manual(rooms, _) => assertEquals(rooms.head.id, "entrance")
      case _ => fail("expected Manual source")

  test("same seed produces same output"):
    val a = DungeonGenerator.generate(6, Some(99L))
    val b = DungeonGenerator.generate(6, Some(99L))
    assertEquals(a, b)

  test("different seeds produce different output"):
    val a = DungeonGenerator.generate(6, Some(1L))
    val b = DungeonGenerator.generate(6, Some(2L))
    assert(a != b)

  test("room sizes are within valid bounds"):
    val source = DungeonGenerator.generate(10, Some(5L))
    source match
      case DungeonMapSource.Manual(rooms, _) =>
        rooms.foreach { r =>
          assert(r.size.width >= 3, s"width too small: ${r.size.width}")
          assert(r.size.height >= 3, s"height too small: ${r.size.height}")
        }
      case _ => fail("expected Manual source")
