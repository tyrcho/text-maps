package textmaps.render

import com.github.writethemfirst.Approbation
import org.scalatest.funsuite.FixtureAnyFunSuite
import textmaps.dsl.*
import textmaps.layout.LayoutEngine

class AsciiRendererTest extends FixtureAnyFunSuite with Approbation:

  test("two connected rooms") { approver =>
    val rooms = List(
      Room("entrance", RoomSize(4, 4), Some("Entry")),
      Room("hall",     RoomSize(8, 4), Some("Great Hall")),
    )
    val conns = List(Connection("entrance", "hall", DoorType.Open))
    approver.verify(AsciiRenderer.render(LayoutEngine.layout(rooms, conns, None)))
  }

  test("locked door between rooms") { approver =>
    val rooms = List(
      Room("guard",  RoomSize(3, 3), Some("Guard")),
      Room("prison", RoomSize(4, 3), Some("Prison")),
    )
    val conns = List(Connection("guard", "prison", DoorType.Locked))
    approver.verify(AsciiRenderer.render(LayoutEngine.layout(rooms, conns, None)))
  }

  test("three rooms in a chain") { approver =>
    val rooms = List(
      Room("entrance", RoomSize(4, 4), Some("Entrance")),
      Room("hall",     RoomSize(6, 4), Some("Hall")),
      Room("vault",    RoomSize(3, 3), Some("Vault")),
    )
    val conns = List(
      Connection("entrance", "hall",  DoorType.Open),
      Connection("hall",     "vault", DoorType.Secret),
    )
    approver.verify(AsciiRenderer.render(LayoutEngine.layout(rooms, conns, None)))
  }

  test("single room") { approver =>
    val rooms = List(Room("alone", RoomSize(5, 3), Some("Alone")))
    approver.verify(AsciiRenderer.render(LayoutEngine.layout(rooms, Nil, None)))
  }

  test("empty map") { approver =>
    approver.verify(AsciiRenderer.render(LayoutEngine.layout(Nil, Nil, None)))
  }
