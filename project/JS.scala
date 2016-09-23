package scalajsbundler

import org.scalajs.core.tools.javascript.Printers
import org.scalajs.core.tools.javascript.Trees.{ObjectConstr, Tree}

object JS {




  def toJson(obj: ObjectConstr): String = show(obj, isStat = false)

  def show(tree: Tree, isStat: Boolean = true): String = {
    val writer = new java.io.StringWriter
    val printer = new Printers.JSTreePrinter(writer)
    printer.printTree(tree, isStat)
    writer.toString()
  }

}