package textmaps.render

import java.nio.file.{Files, Path}
import org.scalatest.funsuite.AnyFunSuite
import textmaps.dsl.DslParser
import textmaps.icons.TestIcons
import textmaps.layout.LayoutEngine

/** Parses each `.dsl` file next to the approved SVG/ASCII fixtures and asserts it
 *  reproduces the same approved output — keeps the DSL source from drifting out of
 *  sync with what the renderers actually produce. Coexists with the TestMaps-based
 *  tests, which exercise LayoutEngine/renderers directly without going through the
 *  DSL parser.
 */
class DslFixtureRenderTest extends AnyFunSuite:

  private val svgDir   = Path.of("core/shared/src/test/resources/textmaps/render/SvgStringRendererTest.files")
  private val asciiDir = Path.of("core/shared/src/test/resources/textmaps/render/AsciiRendererTest.files")

  private val names = List(
    "two_connected_rooms",
    "locked_door_between_rooms",
    "three_rooms_in_a_chain",
    "single_room",
    "empty_map",
    "dungeon_room_with_stairs_and_windows",
    "building_directional_doors",
    "door_types_and_swing",
    "movement_features",
    "natural_structural_features",
    "wall_features",
    "cave_room",
    "dungeon_inline_labels",
  )

  for name <- names do
    test(s"DSL fixture '$name' reproduces its approved SVG and ASCII output") {
      val dslText = Files.readString(svgDir.resolve(s"$name.dsl"))
      val parsed = DslParser.parse(dslText) match
        case Right(map) => map
        case Left(err)  => fail(s"DSL parse failed for '$name': $err")
      val rendered = LayoutEngine.compute(parsed)

      val expectedSvg = Files.readString(svgDir.resolve(s"$name.approved.svg"))
      assert(SvgStringRenderer.render(rendered, TestIcons.fetcher) == expectedSvg, s"SVG mismatch for '$name'")

      val expectedAscii = Files.readString(asciiDir.resolve(s"$name.approved.txt"))
      assert(AsciiRenderer.render(rendered) == expectedAscii, s"ASCII mismatch for '$name'")
    }
