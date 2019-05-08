package example

import org.scalatest.FreeSpec

import scala.scalajs.js
import scala.scalajs.js.Date

class MainTest extends FreeSpec {

  "getMillis" - {
    "should return UNIX timestamp" in {
      val millisBefore = Date.now().toLong
      val millis = Main.getNowInMillis
      val millisAfter = Date.now().toLong
      assert(millis >= millisBefore)
      assert(millis <= millisAfter)
    }
  }

}
