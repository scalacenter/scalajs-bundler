package example

import scala.scalajs.js
import scala.scalajs.js.JSApp
import moment._

object Main extends JSApp {
  def main(): Unit = {

  }

  val getNowInMillis: Double = Moment().value()
}
