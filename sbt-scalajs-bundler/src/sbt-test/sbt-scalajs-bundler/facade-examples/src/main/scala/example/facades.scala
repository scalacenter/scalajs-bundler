package example

import scala.scalajs.js
import scala.scalajs.js.annotation.JSImport

// Note: the corresponding JS modules are defined in the src/main/resources/ directory

// --- Import a whole module as an object

@JSImport("./foo.js", JSImport.Namespace)
@js.native
object Obj extends js.Object {
  def bar(i: Int): Int = js.native
}


// --- Import just a module member, which is a function

@JSImport("./foo.js", "bar")
@js.native
object Member extends js.Function1[Int, Int] {
  def apply(i: Int): Int = js.native
}

// --- Import a class

@JSImport("./class.js", JSImport.Namespace)
@js.native
class User(val name: String, val age: Int) extends js.Object