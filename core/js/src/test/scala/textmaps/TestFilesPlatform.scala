package textmaps

import scala.scalajs.js
import scala.scalajs.js.annotation.*

@js.native @JSImport("fs", JSImport.Namespace)
private object NodeFs extends js.Object:
  def readFileSync(path: String, encoding: String): String = js.native
  def writeFileSync(path: String, data: String): Unit      = js.native
  def mkdirSync(path: String, options: js.Dynamic): Unit   = js.native

/** Wires Node.js fs into TestFiles for the coreJS test runner. */
object TestFilesPlatformInit:
  def init(): Unit =
    TestFiles.readFilePlatform  = path => NodeFs.readFileSync(path, "utf8")
    TestFiles.writeFilePlatform = (path, content) =>
      val dir = path.split('/').dropRight(1).mkString("/")
      if dir.nonEmpty then NodeFs.mkdirSync(dir, js.Dynamic.literal(recursive = true))
      NodeFs.writeFileSync(path, content)
    // Read UPDATE_SNAPSHOTS from Node.js process.env (sys.env may not propagate)
    val envVal = js.Dynamic.global.process.env.UPDATE_SNAPSHOTS
    TestFiles.platformShouldUpdate = !js.isUndefined(envVal) && envVal.toString == "1"
