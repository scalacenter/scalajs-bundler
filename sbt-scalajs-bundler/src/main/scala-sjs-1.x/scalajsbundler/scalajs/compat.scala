package scalajsbundler.scalajs

import java.io.File
import java.nio.file.{Files, Path}

import com.google.common.jimfs.Jimfs

private[scalajsbundler] object compat {

  object io {
    type FileVirtualBinaryFile = Path
    type VirtualBinaryFile = Path

    def memVirtualBinaryFile(name: String, content: Array[Byte], version: Option[String]): Path =
      Files.write(Jimfs.newFileSystem().getPath(name), content)

    def fileVirtualBinaryFile(file: File): Path =
      file.toPath()

    implicit class FileOps(private val __self: Path) extends AnyVal {
      def version: Option[String] = Some(Files.getLastModifiedTime(__self).toString())
    }
  }

}
