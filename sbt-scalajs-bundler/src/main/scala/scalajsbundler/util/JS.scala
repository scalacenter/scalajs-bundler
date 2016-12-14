package scalajsbundler.util

import java.util.concurrent.atomic.AtomicInteger

import org.scalajs.core.ir.Position
import org.scalajs.core.tools.javascript.Printers
import org.scalajs.core.tools.javascript.Trees._

/** A convenient wrapper around JS trees */
final case class JS(tree: Tree) extends AnyVal {
  import JS.position
  def dot(ident: String): JS = JS(DotSelect(tree, Ident(ident)))
  def bracket(ident: String): JS = JS(BracketSelect(tree, StringLiteral(ident)))
  def bracket(ident: JS): JS = JS(BracketSelect(tree, ident.tree))
  def assign(rhs: JS): JS = JS(Assign(tree, rhs.tree))
  def apply(args: JS*): JS = JS(Apply(tree, args.map(_.tree).to[List]))

  def show: String = tree.show
}

object JS {

  implicit lazy val position: Position = Position.NoPosition

  /** String literal */
  def str(value: String): JS =
    JS(StringLiteral(value))

  /** Boolean literal */
  def bool(value: Boolean): JS = JS(BooleanLiteral(value))

  /** Object literal */
  def obj(fields: (String, JS)*): JS =
    JS(ObjectConstr(fields.map { case (ident, value) => (StringLiteral(ident), value.tree) }.to[List]))

  def objStr(fields: Seq[(String, String)]): JS =
    obj(fields.map { case (k, v) => k -> JS.str(v) }: _*)

  /** Array literal */
  def arr(elems: JS*): JS =
    JS(ArrayConstr(elems.map(_.tree).to[List]))

  /** Variable reference */
  def ref(ident: String): JS =
    JS(varRef(ident))

  private def varRef(ident: String): VarRef = VarRef(Ident(ident))

  def regex(value: String): JS =
    JS(New(varRef("RegExp"), List(StringLiteral(value))))

  /** Block of several statements */
  def block(stats: JS*): JS = JS(Block(stats.map(_.tree).to[List]))

  /** Anonymous function definition */
  def fun(body: JS => JS): JS = {
    val param = freshIdentifier()
    JS(Function(List(ParamDef(Ident(param), rest = false)), Return(body(ref(param)).tree)))
  }

  /** Name binding */
  def let(value: JS)(usage: JS => JS): JS = {
    val ident = freshIdentifier()
    JS(Block(VarDef(Ident(ident), value.tree), usage(ref(ident)).tree))
  }

  /** Name binding */
  def let(value1: JS, value2: JS)(usage: (JS, JS) => JS): JS = {
    val ident1 = freshIdentifier()
    val ident2 = freshIdentifier()
    JS(
      Block(
        VarDef(Ident(ident1), value1.tree),
        VarDef(Ident(ident2), value2.tree),
        usage(ref(ident1), ref(ident2)).tree
      )
    )
  }

  def `new`(ctor: JS, args: JS*): JS = JS(New(ctor.tree, args.map(_.tree).to[List]))

  def toJson(obj: JS): String = show(obj.tree, isStat = false)

  def show(tree: Tree, isStat: Boolean = true): String = {
    val writer = new java.io.StringWriter
    val printer = new Printers.JSTreePrinter(writer)
    printer.printTree(tree, isStat)
    writer.toString
  }

  private val identifierSeq = new AtomicInteger(0)
  private def freshIdentifier(): String =
    s"x${identifierSeq.getAndIncrement()}"

}
