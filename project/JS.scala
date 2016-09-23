package scalajsbundler

import org.scalajs.core.ir.Position
import org.scalajs.core.tools.javascript.Printers
import org.scalajs.core.tools.javascript.Trees._

object JS {

  implicit val position: Position = Position.NoPosition

  /** String literal */
  def str(value: String): StringLiteral =
    StringLiteral(value)

  /** Object literal */
  def obj(fields: (String, Tree)*): ObjectConstr =
    ObjectConstr(fields.map { case (ident, value) => (str(ident), value) }.to[List])

  def ref(ident: String): VarRef =
    VarRef(Ident(ident))

  def toJson(obj: ObjectConstr): String = show(obj, isStat = false)

  def show(tree: Tree, isStat: Boolean = true): String = {
    val writer = new java.io.StringWriter
    val printer = new Printers.JSTreePrinter(writer)
    printer.printTree(tree, isStat)
    writer.toString()
  }

  object syntax {

    implicit class RefSyntax(ref: VarRef) {
      def `.` (ident: String): DotSelect = DotSelect(ref, Ident(ident))
    }

    implicit class TreeSyntax(tree: Tree) {
      def := (rhs: Tree): Assign = Assign(tree, rhs)
    }

  }

}