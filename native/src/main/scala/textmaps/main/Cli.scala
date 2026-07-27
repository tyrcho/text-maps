package textmaps.main

import textmaps.dsl.DslParser
import textmaps.generate.DungeonGenerator
import textmaps.icons.{CachingIconFetcher, CurlIconFetcher, FileIconCache}
import textmaps.layout.LayoutEngine
import textmaps.render.SvgStringRenderer

import java.io.{File, FileWriter, PrintWriter}
import java.nio.file.{Files, Paths}

/** Cache dir: `~/.cache/text-maps/icons` — persists fetched icons across CLI
 *  runs so repeat renders of the same map don't re-fetch every icon. */
private val iconFetcher =
  val cacheDir = new File(sys.props.getOrElse("user.home", "."), ".cache/text-maps/icons")
  new CachingIconFetcher(new FileIconCache(cacheDir), new CurlIconFetcher())

/** CLI entry point — mirrors the mermaid CLI / plantuml jar pattern.
 *
 *  Usage:
 *    text-maps                        # reads stdin, writes SVG to stdout
 *    text-maps input.txt              # reads file,  writes SVG to stdout
 *    text-maps input.txt output.svg   # reads file,  writes SVG to file
 */
@main def cli(args: String*): Unit =
  val input  = readInput(args.lift(0))
  val output = args.lift(1)
  DslParser.parse(input) match
    case Left(err) =>
      System.err.println(s"Parse error at line ${err.line}: ${err.message}")
      sys.exit(1)
    case Right(dungeon) =>
      val expanded = dungeon.source match
        case s: textmaps.dsl.DungeonMapSource.Generated =>
          val gen = DungeonGenerator.generate(s.roomCount, s.seed)
          dungeon.copy(source = gen)
        case _ => dungeon
      val rendered = LayoutEngine.compute(expanded)
      val svg      = SvgStringRenderer.render(rendered, iconFetcher)
      writeOutput(svg, output)

private def readInput(pathOpt: Option[String]): String =
  pathOpt match
    case None       => scala.io.Source.stdin.mkString
    case Some(path) => new String(Files.readAllBytes(Paths.get(path)))

private def writeOutput(content: String, pathOpt: Option[String]): Unit =
  pathOpt match
    case None       => println(content)
    case Some(path) =>
      val pw = new PrintWriter(new FileWriter(path))
      try pw.print(content)
      finally pw.close()
      System.err.println(s"Written to $path")
