package scalajsbundler.scalajs

private[scalajsbundler] object compat {

  object io {
    type FileVirtualBinaryFile = org.scalajs.core.tools.io.FileVirtualJSFile
    type VirtualBinaryFile = org.scalajs.core.tools.io.VirtualJSFile

    def memVirtualBinaryFile(name: String, content: Array[Byte], version: Option[String]): org.scalajs.core.tools.io.MemVirtualJSFile = {
      new org.scalajs.core.tools.io.MemVirtualJSFile(name).withContent(new String(content, "utf8"))
    }

    def fileVirtualBinaryFile(file: java.io.File): org.scalajs.core.tools.io.FileVirtualJSFile =
      new org.scalajs.core.tools.io.FileVirtualJSFile(file)

    implicit class FileOps(val __self: VirtualBinaryFile) extends AnyVal {
      def toFile(): java.io.File = __self.asInstanceOf[FileVirtualBinaryFile].file
    }
  }

}
