package scalajsbundler.util

// This is copied from `org.scalajs.linker.backend.javascript.Trees` in Scala.js.
private[util] object JSTrees {
  /* The case classes for JS Trees are sealed instead of final because making
   * them final triggers bugs with Scala 2.11.x and 2.12.{1-4}, in combination
   * with their `implicit val pos`.
   */

  /** AST node of JavaScript. */
  abstract sealed class Tree {

    def show: String = {
      val writer = new java.io.StringWriter
      val printer = new JSPrinters.JSTreePrinter(writer)
      printer.printTree(this, isStat = true)
      writer.toString()
    }
  }

  // Comments

  sealed case class DocComment(text: String) extends Tree

  // Identifiers and properties

  sealed trait PropertyName

  sealed case class Ident(name: String) extends PropertyName {
    require(Ident.isValidJSIdentifierName(name), s"'$name' is not a valid JS identifier name")
  }

  object Ident {

    /** Tests whether the given string is a valid `IdentifierName` for the ECMAScript language specification.
      *
      * This does not exclude keywords, as they can be used as identifiers in some productions, notably as property
      * names.
      */
    def isValidJSIdentifierName(name: String): Boolean = {
      // scalastyle:off return
      // This method is called on every `Ident` creation; it should be fast.
      val len = name.length()
      if (len == 0)
        return false
      val c = name.charAt(0)
      if (c != '$' && c != '_' && !Character.isUnicodeIdentifierStart(c))
        return false
      var i = 1
      while (i != len) {
        val c = name.charAt(i)
        if (c != '$' && !Character.isUnicodeIdentifierPart(c))
          return false
        i += 1
      }
      true
      // scalastyle:on return
    }
  }

  sealed case class ComputedName(tree: Tree) extends PropertyName

  // Definitions

  sealed trait LocalDef extends Tree {
    def name: Ident
    def mutable: Boolean

    def ref: Tree = VarRef(name)
  }

  sealed case class VarDef(name: Ident, rhs: Option[Tree]) extends LocalDef {
    def mutable: Boolean = true
  }

  /** ES6 let or const (depending on the mutable flag). */
  sealed case class Let(name: Ident, mutable: Boolean, rhs: Option[Tree]) extends LocalDef

  sealed case class ParamDef(name: Ident, rest: Boolean) extends LocalDef {
    def mutable: Boolean = true
  }

  // Control flow constructs

  sealed case class Skip() extends Tree

  sealed class Block private (val stats: List[Tree]) extends Tree {

    override def toString(): String =
      stats.mkString("Block(", ",", ")")
  }

  object Block {

    def apply(stats: List[Tree]): Tree = {
      val flattenedStats = stats flatMap {
        case Skip()          => Nil
        case Block(subStats) => subStats
        case other           => other :: Nil
      }
      flattenedStats match {
        case Nil         => Skip()
        case only :: Nil => only
        case _           => new Block(flattenedStats)
      }
    }

    def apply(stats: Tree*): Tree =
      apply(stats.toList)

    def unapply(block: Block): Some[List[Tree]] = Some(block.stats)
  }

  sealed case class Labeled(label: Ident, body: Tree) extends Tree

  sealed case class Assign(lhs: Tree, rhs: Tree) extends Tree {
    require(
      lhs match {
        case _: VarRef | _: DotSelect | _: BracketSelect => true
        case _                                           => false
      },
      s"Invalid lhs for Assign: $lhs")
  }

  sealed case class Return(expr: Tree) extends Tree

  sealed case class If(cond: Tree, thenp: Tree, elsep: Tree) extends Tree

  sealed case class While(cond: Tree, body: Tree, label: Option[Ident] = None) extends Tree

  sealed case class DoWhile(body: Tree, cond: Tree, label: Option[Ident] = None) extends Tree

  sealed case class ForIn(lhs: Tree, obj: Tree, body: Tree) extends Tree

  sealed case class For(init: Tree, guard: Tree, update: Tree, body: Tree) extends Tree

  sealed case class TryCatch(block: Tree, errVar: Ident, handler: Tree) extends Tree

  sealed case class TryFinally(block: Tree, finalizer: Tree) extends Tree

  sealed case class Throw(expr: Tree) extends Tree

  sealed case class Break(label: Option[Ident] = None) extends Tree

  sealed case class Continue(label: Option[Ident] = None) extends Tree

  sealed case class Switch(selector: Tree, cases: List[(Tree, Tree)], default: Tree) extends Tree

  sealed case class Debugger() extends Tree

  // Expressions

  sealed case class New(ctor: Tree, args: List[Tree]) extends Tree

  sealed case class DotSelect(qualifier: Tree, item: Ident) extends Tree

  sealed case class BracketSelect(qualifier: Tree, item: Tree) extends Tree

  /** Syntactic apply. It is a method call if fun is a dot-select or bracket-select. It is a function call otherwise.
    */
  sealed case class Apply(fun: Tree, args: List[Tree]) extends Tree

  /** Dynamic `import(arg)`. */
  sealed case class ImportCall(arg: Tree) extends Tree

  /** `...items`, the "spread" operator of ECMAScript 6.
    *
    * It is only valid in ECMAScript 6, in the `args`/`items` of a [[New]], [[Apply]], or [[ArrayConstr]].
    *
    * @param items
    *   An iterable whose items will be spread
    */
  sealed case class Spread(items: Tree) extends Tree

  sealed case class Delete(prop: Tree) extends Tree {
    require(
      prop match {
        case _: DotSelect | _: BracketSelect => true
        case _                               => false
      },
      s"Invalid prop for Delete: $prop")
  }

  /** Unary operation (always preserves pureness).
    *
    * Operations which do not preserve pureness are not allowed in this tree. These are notably ++ and --
    */
  sealed case class UnaryOp(op: UnaryOp.Code, lhs: Tree) extends Tree

  object UnaryOp {

    /** Codes are raw Ints to be able to switch-match on them. */
    type Code = Int

    final val + = 1
    final val - = 2
    final val ~ = 3
    final val ! = 4

    final val typeof = 5
  }

  /** `++x`, `x++`, `--x` or `x--`. */
  sealed case class IncDec(prefix: Boolean, inc: Boolean, arg: Tree) extends Tree

  /** Binary operation (always preserves pureness).
    *
    * Operations which do not preserve pureness are not allowed in this tree. These are notably +=, -=, *=, /= and %=
    */
  sealed case class BinaryOp(op: BinaryOp.Code, lhs: Tree, rhs: Tree) extends Tree

  object BinaryOp {

    /** Codes are raw Ints to be able to switch-match on them. */
    type Code = Int

    final val === = 1
    final val !== = 2

    final val + = 3
    final val - = 4
    final val * = 5
    final val / = 6
    final val % = 7

    final val | = 8
    final val & = 9
    final val ^ = 10
    final val << = 11
    final val >> = 12
    final val >>> = 13

    final val < = 14
    final val <= = 15
    final val > = 16
    final val >= = 17

    final val && = 18
    final val || = 19

    final val in = 20
    final val instanceof = 21
  }

  sealed case class ArrayConstr(items: List[Tree]) extends Tree

  sealed case class ObjectConstr(fields: List[(PropertyName, Tree)]) extends Tree

  // Literals

  /** Marker for literals. Literals are always pure. */
  sealed trait Literal extends Tree

  sealed case class Undefined() extends Literal

  sealed case class Null() extends Literal

  sealed case class BooleanLiteral(value: Boolean) extends Literal

  sealed case class IntLiteral(value: Int) extends Literal

  sealed case class DoubleLiteral(value: Double) extends Literal

  sealed case class StringLiteral(value: String) extends Literal with PropertyName

  sealed case class BigIntLiteral(value: BigInt) extends Literal

  // Atomic expressions

  sealed case class VarRef(ident: Ident) extends Tree

  sealed case class This() extends Tree

  sealed case class Function(arrow: Boolean, args: List[ParamDef], body: Tree) extends Tree

  // Named function definition

  sealed case class FunctionDef(name: Ident, args: List[ParamDef], body: Tree) extends Tree

  // ECMAScript 6 classes

  sealed case class ClassDef(className: Option[Ident], parentClass: Option[Tree], members: List[Tree]) extends Tree

  sealed case class MethodDef(static: Boolean, name: PropertyName, args: List[ParamDef], body: Tree) extends Tree

  sealed case class GetterDef(static: Boolean, name: PropertyName, body: Tree) extends Tree

  sealed case class SetterDef(static: Boolean, name: PropertyName, param: ParamDef, body: Tree) extends Tree

  sealed case class Super() extends Tree

  // ECMAScript 6 modules

  /** The name of an ES module export.
    *
    * It must be a valid `IdentifierName`, as tested by [[ExportName.isValidExportName]].
    */
  sealed case class ExportName(name: String) {
    require(ExportName.isValidExportName(name), s"'$name' is not a valid export name")
  }

  object ExportName {

    /** Tests whether a string is a valid export name.
      *
      * A string is a valid export name if and only if it is a valid ECMAScript `IdentifierName`, which is defined in
      * [[http://www.ecma-international.org/ecma-262/6.0/#sec-names-and-keywords Section 11.6 of the ECMAScript 2015 specification]].
      *
      * Currently, this implementation is buggy in some corner cases, as it does not accept code points with the Unicode
      * properties `Other_ID_Start` and `Other_ID_Continue`. For example,
      * `isValidIdentifierName(0x2118.toChar.toString)` will return `false` instead of `true`.
      *
      * In theory, it does not really account for code points with the Unicode properties `Pattern_Syntax` and
      * `Pattern_White_Space`, which should be rejected. However, with the current version of Unicode (9.0.0), there
      * seems to be no such character that would be accepted by this method.
      */
    final def isValidExportName(name: String): Boolean = {
      // scalastyle:off return
      import java.lang.Character._

      def isJSIdentifierStart(cp: Int): Boolean =
        isUnicodeIdentifierStart(cp) || cp == '$' || cp == '_'

      def isJSIdentifierPart(cp: Int): Boolean = {
        val ZWNJ = 0x200C
        val ZWJ = 0x200D
        isUnicodeIdentifierPart(cp) || cp == '$' || cp == '_' || cp == ZWNJ || cp == ZWJ
      }

      if (name.isEmpty)
        return false

      val firstCP = name.codePointAt(0)
      if (!isJSIdentifierStart(firstCP))
        return false

      var i = charCount(firstCP)
      while (i < name.length) {
        val cp = name.codePointAt(i)
        if (!isJSIdentifierPart(cp))
          return false
        i += charCount(cp)
      }

      true
      // scalastyle:on return
    }
  }

  /** `import` statement, except namespace import.
    *
    * This corresponds to the following syntax:
    * {{{
    *  import { <binding1_1> as <binding1_2>, ..., <bindingN_1> as <bindingN_2> } from <from>
    * }}}
    * The `_1` parts of bindings are therefore the identifier names that are imported, as specified in `export` clauses
    * of the module. The `_2` parts are the names under which they are imported in the current module.
    *
    * Special cases:
    *   - When `_1.name == _2.name`, there is shorter syntax in ES, i.e., `import { binding } from 'from'`.
    *   - When `_1.name == "default"`, it is equivalent to a default import.
    */
  sealed case class Import(bindings: List[(ExportName, Ident)], from: StringLiteral) extends Tree

  /** Namespace `import` statement.
    *
    * This corresponds to the following syntax:
    * {{{
    *  import * as <binding> from <from>
    * }}}
    */
  sealed case class ImportNamespace(binding: Ident, from: StringLiteral) extends Tree

  /** `export` statement.
    *
    * This corresponds to the following syntax:
    * {{{
    *  export { <binding1_1> as <binding1_2>, ..., <bindingN_1> as <bindingN_2> }
    * }}}
    * The `_1` parts of bindings are therefore the identifiers from the current module that are exported. The `_2` parts
    * are the names under which they are exported to other modules.
    */
  sealed case class Export(bindings: List[(Ident, ExportName)]) extends Tree
}
