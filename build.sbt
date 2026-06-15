ThisBuild / scalaVersion := "3.3.3"

lazy val root = project
  .in(file("."))
  .enablePlugins(ScalaJSPlugin)
  .settings(
    name := "text-maps",
    scalaJSUseMainModuleInitializer := true,
    libraryDependencies ++= Seq(
      "com.raquo"     %%% "laminar"     % "17.0.0",
      "org.scala-js"  %%% "scalajs-dom" % "2.8.0",
      "org.scalameta" %%% "munit"       % "1.0.0" % Test,
    ),
    testFrameworks += new TestFramework("munit.Framework"),
  )

lazy val devServer = taskKey[Unit]("Start live-reload dev server on :8080 (background)")
devServer := {
  val root   = baseDirectory.value.toPath
  val jsFile = (Compile / fastLinkJS / scalaJSLinkerOutputDirectory).value.toPath.resolve("main.js")
  DevServer.start(root, jsFile, port = 8082)
  streams.value.log.info("Dev server: http://localhost:8082")
}

addCommandAlias("dev", ";fastLinkJS; devServer; ~fastLinkJS")
