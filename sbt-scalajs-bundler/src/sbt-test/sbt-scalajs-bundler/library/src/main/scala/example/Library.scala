//#library-definition
package example

import scala.scalajs.js.annotation.{JSExportTopLevel, JSExportAll}

@JSExportTopLevel(name="sjs_example_Library") @JSExportAll
object Library {
  def foo(): String = SomeOtherCode.quux(true)
  def bar(): String = SomeOtherCode.quux(false)
}
//#library-definition

object SomeOtherCode {
  import uuid.UUID

  def quux(b: Boolean): String = if (b) UUID.v4() else UUID.v1()

}
