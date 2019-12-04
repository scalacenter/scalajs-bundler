package example

import org.junit.Assert._
import org.junit.Test

class MainTest {

  @Test def leftPad(): Unit = {
    assertEquals("  123", LeftPad("123", 5))
    assertEquals("00123", LeftPad("123", 5, '0'))
    assertEquals("123", LeftPad("123", 2))
  }

  @Test def main(): Unit = {
    assertEquals("     scala", Main.format("scala"))
    assertEquals(" scala", Main.format("scala", 6))
  }

}
