package scalajsbundler.scalajs

import java.io.File
import java.nio.file.{Files, Path}

import com.google.common.jimfs.Jimfs

private[scalajsbundler] object compat {

  object backend {
    val Printers = org.scalajs.linker.backend.javascript.Printers
    val Trees = org.scalajs.linker.backend.javascript.Trees
    def function(args: List[org.scalajs.linker.backend.javascript.Trees.ParamDef], body: org.scalajs.linker.backend.javascript.Trees.Tree)(implicit pos: org.scalajs.ir.Position): org.scalajs.linker.backend.javascript.Trees.Function =
      org.scalajs.linker.backend.javascript.Trees.Function(arrow = false, args, body)
  }

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

  object ir {
    type Position = org.scalajs.ir.Position
    val Position = org.scalajs.ir.Position
  }

  object linker {
    type Config = org.scalajs.linker.interface.StandardConfig
  }

  object testing {
    type TestAdapter = org.scalajs.testing.adapter.TestAdapter
    val TestAdapter = org.scalajs.testing.adapter.TestAdapter
  }
}
