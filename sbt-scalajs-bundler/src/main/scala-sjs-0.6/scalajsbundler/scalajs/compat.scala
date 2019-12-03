package scalajsbundler.scalajs

private[scalajsbundler] object compat {

  object backend {
    val Printers = org.scalajs.core.tools.javascript.Printers
    val Trees = org.scalajs.core.tools.javascript.Trees
    def function(args: List[org.scalajs.core.tools.javascript.Trees.ParamDef], body: org.scalajs.core.tools.javascript.Trees.Tree)(implicit pos: org.scalajs.core.ir.Position): org.scalajs.core.tools.javascript.Trees.Function =
      org.scalajs.core.tools.javascript.Trees.Function(args, body)
  }

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

  object ir {
    type Position = org.scalajs.core.ir.Position
    val Position = org.scalajs.core.ir.Position
  }

  object linker {
    type Config = org.scalajs.core.tools.linker.StandardLinker.Config
  }

  object testing {
    type TestAdapter = org.scalajs.testadapter.TestAdapter
    val TestAdapter = org.scalajs.testadapter.TestAdapter
  }

}
