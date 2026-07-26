package textmaps.render

import java.nio.file.{Files, Path}
import org.scalatest.funsuite.AnyFunSuite

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

  test("two connected rooms")             { asciiApproval("two_connected_rooms",             TestMaps.twoConnectedRooms)             }
  test("locked door between rooms")       { asciiApproval("locked_door_between_rooms",       TestMaps.lockedDoorBetweenRooms)        }
  test("three rooms in a chain")          { asciiApproval("three_rooms_in_a_chain",          TestMaps.threeRoomsInAChain)            }
  test("single room")                     { asciiApproval("single_room",                     TestMaps.singleRoom)                    }
  test("empty map")                       { asciiApproval("empty_map",                       TestMaps.emptyMap)                      }
  test("dungeon room with stairs")        { asciiApproval("dungeon_room_with_stairs_and_windows", TestMaps.dungeonRoomWithStairsAndWindows) }
  test("building with directional doors") { asciiApproval("building_directional_doors",      TestMaps.buildingWithDirectionalDoors)  }
  test("door types and swing")            { asciiApproval("door_types_and_swing",            TestMaps.doorTypesAndSwing)             }
  test("movement features")               { asciiApproval("movement_features",               TestMaps.movementFeatures)              }
  test("natural and structural features") { asciiApproval("natural_structural_features",     TestMaps.naturalStructuralFeatures)     }
  test("wall features")                   { asciiApproval("wall_features",                   TestMaps.wallFeatures)                  }
  test("cave room")                       { asciiApproval("cave_room",                        TestMaps.caveRoom)                      }
  test("dungeon with inline labels")      { asciiApproval("dungeon_inline_labels",            TestMaps.dungeonInlineLabels)           }
