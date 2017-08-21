package scalajsbundler.util

import java.util.concurrent.atomic.AtomicInteger

import org.scalajs.core.ir.Position
import org.scalajs.core.tools.javascript.Printers
import org.scalajs.core.tools.javascript.Trees._

private[util] sealed abstract class JSLike(val tree: Tree) {
  def show: String = tree.show
  def show(isStat: Boolean = true): String = JSLike.show(tree, isStat)
  def toJson: String = JSLike.show(tree, isStat = false)
}

object JSLike {
  def show(tree: Tree, isStat: Boolean = true): String = {
    val writer = new java.io.StringWriter
    val printer = new Printers.JSTreePrinter(writer)
    printer.printTree(tree, isStat)
    writer.toString
  }
}

/** A convenient wrapper around JS trees */
final class JS private(tree: Tree) extends JSLike(tree) {
  import JS.position
  def dot(ident: String): JS = JS(DotSelect(tree, Ident(ident)))
  def bracket(ident: String): JS = JS(BracketSelect(tree, StringLiteral(ident)))
  def bracket(ident: JSLike): JS = JS(BracketSelect(tree, ident.tree))
  def assign(rhs: JSLike): JS = JS(Assign(tree, rhs.tree))
  def apply(args: JSLike*): JS = JS(Apply(tree, args.map(_.tree).to[List]))
}

object JS {

  implicit lazy val position: Position = Position.NoPosition

  def apply(tree: Tree): JS = new JS(tree)

  /** Array literal. */
  def arr(elems: JSLike*): JS = JS(ArrayConstr(elems.map(_.tree).to[List]))

  /** Boolean literal */
  def bool(value: Boolean): JS = JS(BooleanLiteral(value))

  /** Object literal */
  def obj(fields: (String, JSLike)*): JS =
    JS(ObjectConstr(fields.map { case (ident, value) => (StringLiteral(ident), value.tree) }.to[List]))

  /** Object literal */
  def objStr(fields: Seq[(String, String)]): JS =
    obj(fields.map { case (k, v) => k -> JS.str(v) }: _*)

  /** String literal */
  def str(value: String): JS = JS(StringLiteral(value))

  /** Numeric literal */
  def int(value: Int): JS = JS(IntLiteral(value))

  /** Variable reference */
  def ref(ident: String): JS =
    JS(varRef(ident))

  private def varRef(ident: String): VarRef = VarRef(Ident(ident))

  /** Variable definition */
  def `var`(ident: String, rhs: Option[JSLike] = None): JS =
    JS(VarDef(Ident(ident), rhs.map(_.tree)))

  def regex(value: String): JS =
    JS(New(varRef("RegExp"), List(StringLiteral(value))))

  /** Block of several statements */
  def block(stats: JS*): JS = JS(Block(stats.map(_.tree).to[List]))

  /** Anonymous function definition */
  def fun(body: JS => JSLike): JS = {
    val param = freshIdentifier()
    JS(Function(List(ParamDef(Ident(param), rest = false)), Return(body(ref(param)).tree)))
  }

  /** Name binding */
  def let(value: JS)(usage: JS => JS): JS = {
    val ident = freshIdentifier()
    JS(Block(VarDef(Ident(ident), Some(value.tree)), usage(ref(ident)).tree))
  }

  /** Name binding */
  def let(value1: JS, value2: JS)(usage: (JS, JS) => JS): JS = {
    val ident1 = freshIdentifier()
    val ident2 = freshIdentifier()
    JS(
      Block(
        VarDef(Ident(ident1), Some(value1.tree)),
        VarDef(Ident(ident2), Some(value2.tree)),
        usage(ref(ident1), ref(ident2)).tree
      )
    )
  }

  def `new`(ctor: JS, args: JSLike*): JS = JS(New(ctor.tree, args.map(_.tree).to[List]))

  private val identifierSeq = new AtomicInteger(0)
  private def freshIdentifier(): String =
    s"x${identifierSeq.getAndIncrement()}"

}

final class JSON private(tree: Tree) extends JSLike(tree)

object JSON {

  implicit lazy val position: Position = Position.NoPosition

  def apply(tree: Tree): JSON = new JSON(tree)

  /** Array literal. */
  def arr(elems: JSON*): JSON = JSON(ArrayConstr(elems.map(_.tree).to[List]))

  /** Boolean literal */
  def bool(value: Boolean): JSON = JSON(BooleanLiteral(value))

  /** Object literal */
  def obj(fields: (String, JSON)*): JSON =
    JSON(ObjectConstr(fields.map { case (ident, value) => (StringLiteral(ident), value.tree) }.to[List]))

  /** Object literal */
  def objStr(fields: Seq[(String, String)]): JSON =
    obj(fields.map { case (k, v) => k -> JSON.str(v) }: _*)

  /** String literal */
  def str(value: String): JSON = JSON(StringLiteral(value))

}
