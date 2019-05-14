package scalajsbundler.scalajs

private[scalajsbundler] object compat {

  object backend {
    val Printers = org.scalajs.linker.backend.javascript.Printers
    val Trees = org.scalajs.linker.backend.javascript.Trees
    def function(args: List[org.scalajs.linker.backend.javascript.Trees.ParamDef], body: org.scalajs.linker.backend.javascript.Trees.Tree)(implicit pos: org.scalajs.ir.Position): org.scalajs.linker.backend.javascript.Trees.Function =
      org.scalajs.linker.backend.javascript.Trees.Function(arrow = false, args, body)
  }

  object io {
    type FileVirtualBinaryFile = org.scalajs.io.FileVirtualBinaryFile
    type VirtualBinaryFile = org.scalajs.io.VirtualBinaryFile
    def memVirtualBinaryFile(name: String, content: Array[Byte], version: Option[String]): org.scalajs.io.MemVirtualBinaryFile =
      org.scalajs.io.MemVirtualBinaryFile(name, content, version)
  }

  object ir {
    type Position = org.scalajs.ir.Position
    val Position = org.scalajs.ir.Position
  }

  object linker {
    type Config = org.scalajs.linker.StandardLinker.Config
  }

  object testing {
    type TestAdapter = org.scalajs.testing.adapter.TestAdapter
    val TestAdapter = org.scalajs.testing.adapter.TestAdapter
  }
}