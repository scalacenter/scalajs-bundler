package scalajsbundler.util

import java.io.{BufferedInputStream, ByteArrayOutputStream, FileInputStream, OutputStream}

import scalajsbundler.scalajs.compat.io.{FileVirtualBinaryFile, memVirtualBinaryFile, VirtualBinaryFile}
import sbt.{Attributed, Def, File, FileFilter, Path, globFilter}

import scala.annotation.tailrec

private[scalajsbundler] object ScalaJSNativeLibraries {

  // Copied from https://github.com/scala-js/jsdependencies
  def apply(fullClasspath: Seq[Attributed[File]]): Seq[(String, VirtualBinaryFile)] = {
    collectFromClasspath(fullClasspath,
      "*.js", collectJar = jsFilesInJar,
      collectFile = (f, relPath) => relPath -> new FileVirtualBinaryFile(f))
  }

  /** Collect certain file types from a classpath.
    *
    *  @param cp Classpath to collect from
    *  @param filter Filter for (real) files of interest (not in jars)
    *  @param collectJar Collect elements from a jar (called for all jars)
    *  @param collectFile Collect a single file. Params are the file and the
    *      relative path of the file (to its classpath entry root).
    *  @return Collected elements attributed with physical files they originated
    *      from (key: scalaJSSourceFiles).
    */
  private def collectFromClasspath[T](cp: Def.Classpath, filter: FileFilter,
    collectJar: File => Seq[T],
    collectFile: (File, String) => T): Seq[T] = {

    val results = Seq.newBuilder[T]

    for (cpEntry <- Attributed.data(cp) if cpEntry.exists) {
      if (cpEntry.isFile && cpEntry.getName.endsWith(".jar")) {
        results ++= collectJar(cpEntry)
      } else if (cpEntry.isDirectory) {
        for {
          (file, relPath0) <- Path.selectSubpaths(cpEntry, filter)
        } {
          val relPath = relPath0.replace(java.io.File.separatorChar, '/')
          results += collectFile(file, relPath)
        }
      } else {
        throw new IllegalArgumentException(
          "Illegal classpath entry: " + cpEntry.getPath)
      }
    }

    results.result()
  }

  private def jsFilesInJar(jar: File): List[(String, VirtualBinaryFile)] =
    jarListEntries(jar, _.endsWith(".js"))

  private def jarListEntries[T](jar: File,
    p: String => Boolean): List[(String, VirtualBinaryFile)] = {

    import java.util.zip._

    val jarPath = jar.getPath
    val jarVersion = new FileVirtualBinaryFile(jar).version

    val stream =
      new ZipInputStream(new BufferedInputStream(new FileInputStream(jar)))
    try {
      val buf = new Array[Byte](4096)

      @tailrec
      def readAll(out: OutputStream): Unit = {
        val read = stream.read(buf)
        if (read != -1) {
          out.write(buf, 0, read)
          readAll(out)
        }
      }

      def makeVF(e: ZipEntry): (String, VirtualBinaryFile) = {
        val size = e.getSize
        val out =
          if (0 <= size && size <= Int.MaxValue) new ByteArrayOutputStream(size.toInt)
          else new ByteArrayOutputStream()

        try {
          readAll(out)
          val relName = e.getName
          val vf = memVirtualBinaryFile(s"$jarPath:$relName", out.toByteArray(),
            jarVersion)
          relName -> vf
        } finally {
          out.close()
        }
      }

      Iterator.continually(stream.getNextEntry())
        .takeWhile(_ != null)
        .filter(e => p(e.getName))
        .map(makeVF)
        .toList
    } finally {
      stream.close()
    }
  }

}
