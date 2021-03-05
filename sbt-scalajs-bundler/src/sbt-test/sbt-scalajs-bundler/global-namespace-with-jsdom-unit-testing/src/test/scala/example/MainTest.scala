package example

import org.junit.Assert._
import org.junit.Test

import scala.scalajs.js
import scala.scalajs.js.Date

class MainTest {

  @Test def getMillis(): Unit = {
    val millisBefore = Date.now().toLong
    val millis = Main.getNowInMillis
    val millisAfter = Date.now().toLong
    assertTrue(millis >= millisBefore)
    assertTrue(millis <= millisAfter)
  }

}
