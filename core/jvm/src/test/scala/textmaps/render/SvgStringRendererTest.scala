package textmaps.render

import java.nio.file.{Files, Path}
import org.scalatest.funsuite.AnyFunSuite

class SvgStringRendererTest extends AnyFunSuite:

  private val approvedDir  = Path.of("core/shared/src/test/resources/textmaps/render/SvgStringRendererTest.files")
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

  test("two connected rooms")             { svgApproval("two_connected_rooms",             TestMaps.twoConnectedRooms)             }
  test("locked door between rooms")       { svgApproval("locked_door_between_rooms",       TestMaps.lockedDoorBetweenRooms)        }
  test("three rooms in a chain")          { svgApproval("three_rooms_in_a_chain",          TestMaps.threeRoomsInAChain)            }
  test("single room")                     { svgApproval("single_room",                     TestMaps.singleRoom)                    }
  test("empty map")                       { svgApproval("empty_map",                       TestMaps.emptyMap)                      }
  test("dungeon room with stairs")        { svgApproval("dungeon_room_with_stairs_and_windows", TestMaps.dungeonRoomWithStairsAndWindows) }
  test("building with exterior exits")    { svgApproval("building_with_exterior_exits",    TestMaps.buildingWithExteriorExits)     }
  test("new door types")                  { svgApproval("new_door_types",                  TestMaps.newDoorTypes)                  }
  test("movement features")               { svgApproval("movement_features",               TestMaps.movementFeatures)              }
  test("natural and structural features") { svgApproval("natural_structural_features",     TestMaps.naturalStructuralFeatures)     }
  test("wall features")                   { svgApproval("wall_features",                   TestMaps.wallFeatures)                  }
  test("cave room")                       { svgApproval("cave_room",                        TestMaps.caveRoom)                      }
  test("dungeon with inline labels")      { svgApproval("dungeon_inline_labels",            TestMaps.dungeonInlineLabels)           }
