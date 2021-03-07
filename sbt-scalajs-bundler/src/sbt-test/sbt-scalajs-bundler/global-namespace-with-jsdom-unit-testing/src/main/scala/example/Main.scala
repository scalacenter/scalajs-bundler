package example

import scala.scalajs.js
import scala.scalajs.js.annotation._

// Emulate a Scala.js facade library that assumes everything to be in the global scope
object moment {
  @js.native
  @JSGlobal("moment")
  object Moment extends js.Object {
    def apply(): Date = js.native
  }

  @js.native
  trait Date extends js.Object {
    @JSName("valueOf")
    def value(): Double = js.native
  }
}

import moment._

object Main {
  def main(args: Array[String]): Unit = {

  }

  val getNowInMillis: Double = Moment().value()
}
