package textmaps

/** Cross-platform file helpers for approval tests.
 *  Platform-specific implementations live in core/js/ and core/native/. */
object TestFiles:
  /** Set to true by the platform init when UPDATE_SNAPSHOTS=1. */
  var platformShouldUpdate: Boolean = false

  def shouldUpdate: Boolean = platformShouldUpdate

  def readApproved(name: String): String  = readFilePlatform(approvedPath(name))
  def writeApproved(name: String, content: String): Unit = writeFilePlatform(approvedPath(name), content)

  private def approvedPath(name: String) = s"core/testdata/ascii/$name.approved.txt"

  var readFilePlatform:  String => String          = _ => sys.error("readFile not wired — call TestFilesPlatformInit.init()")
  var writeFilePlatform: (String, String) => Unit  = (_, _) => sys.error("writeFile not wired")
