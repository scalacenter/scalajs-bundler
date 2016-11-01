package example

import scala.scalajs.js
import scala.scalajs.js.annotation.JSImport

@JSImport("./my-module", JSImport.Namespace)
@js.native
object MyModule extends js.Object {
  val someUuid: String = js.native
}
