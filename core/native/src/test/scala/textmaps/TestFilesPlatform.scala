package textmaps

import java.io.{File, FileWriter, PrintWriter}
import java.nio.file.{Files, Paths}

/** Wires JDK file I/O into TestFiles for the coreNative test runner. */
object TestFilesPlatformInit:
  def init(): Unit =
    TestFiles.readFilePlatform  = path =>
      new String(Files.readAllBytes(Paths.get(path)))
    TestFiles.writeFilePlatform = (path, content) =>
      val file = new File(path)
      file.getParentFile.mkdirs()
      val pw = new PrintWriter(new FileWriter(file))
      try pw.print(content) finally pw.close()
    TestFiles.platformShouldUpdate = sys.env.get("UPDATE_SNAPSHOTS").contains("1")
