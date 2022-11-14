package example

import org.junit.Assert._
import org.junit.Test

class BasicTest {

  @Test def newerLinker(): Unit = {
    assertEquals("1", 1.0.toString())
  }

}
