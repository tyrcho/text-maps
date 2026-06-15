import scala.scalanative.build.*

ThisBuild / scalaVersion := "3.3.3"

// ── Core: shared DSL, layout, generator, string renderer ───────────────────

lazy val core = crossProject(JSPlatform, NativePlatform)
  .crossType(CrossType.Full)
  .in(file("core"))
  .settings(
    name := "text-maps-core",
    libraryDependencies += "org.scalameta" %%% "munit" % "1.0.0" % Test,
    testFrameworks += new TestFramework("munit.Framework"),
  )
  .jsSettings(
    // Node.js fs/path modules used by TestFilesPlatform (requires CommonJS mode)
    libraryDependencies += "org.scala-js" %%% "scalajs-dom" % "2.8.0" % Test,
    Test / scalaJSLinkerConfig ~= { _.withModuleKind(ModuleKind.CommonJSModule) },
  )

// ── JS: browser app (scalajs-dom only; SVG rendered as strings by core) ──────

lazy val js = project
  .in(file("js"))
  .enablePlugins(ScalaJSPlugin)
  .dependsOn(core.js)
  .settings(
    name := "text-maps-js",
    scalaJSUseMainModuleInitializer := true,
    libraryDependencies += "org.scala-js" %%% "scalajs-dom" % "2.8.0",
  )

// ── Native: CLI binary (reads DSL from stdin/file, writes SVG) ─────────────

lazy val native = project
  .in(file("native"))
  .enablePlugins(ScalaNativePlugin)
  .dependsOn(core.native)
  .settings(
    name := "text-maps-native",
    nativeConfig ~= { c =>
      c.withLTO(LTO.none)
       .withMode(Mode.releaseFast)
       .withGC(GC.commix)
    },
  )

// ── Dev server task (attached to js project) ──────────────────────────────

lazy val devServer = taskKey[Unit]("Start live-reload dev server on :8082 (background)")
devServer := {
  val root   = (ThisBuild / baseDirectory).value.toPath
  val jsFile = (js / Compile / fastLinkJS / scalaJSLinkerOutputDirectory).value.toPath.resolve("main.js")
  DevServer.start(root, jsFile, port = 8082)
  streams.value.log.info("Dev server: http://localhost:8082")
}

addCommandAlias("dev", ";js/fastLinkJS; devServer; ~js/fastLinkJS")
