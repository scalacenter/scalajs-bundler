package example

import scala.scalajs.js
import scala.scalajs.js.annotation.JSImport

@js.native
@JSImport("left-pad", JSImport.Default)
object LeftPad extends js.Function3[String, Int, js.UndefOr[Char], String] {
  def apply(str: String, len: Int, ch: js.UndefOr[Char] = js.undefined): String =
    js.native
}

object Main extends js.JSApp {
  def format(s: String, len: Int = 10) = LeftPad(s, len)

  def main(): Unit = {
    println(format("scala"))
  }
}
