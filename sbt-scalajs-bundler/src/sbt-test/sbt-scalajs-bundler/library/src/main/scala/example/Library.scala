//#library-definition
package example

import scala.scalajs.js.annotation.{JSExport, JSExportAll}

@JSExport @JSExportAll
object Library {
  def foo(): String = SomeOtherCode.quux(true)
  def bar(): String = SomeOtherCode.quux(false)
}
//#library-definition

object SomeOtherCode {
  import uuid.uuid

  def quux(b: Boolean): String = if (b) uuid.v4() else uuid.v1()

}