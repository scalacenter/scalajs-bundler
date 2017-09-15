package example

import org.scalatest.FreeSpec

class MainTest extends FreeSpec {

  "LeftPad" - {
    "leftPad" in {
      assert(LeftPad("123", 5) == "  123")
    }

    "leftPad with custom char" in {
      assert(LeftPad("123", 5, '0') == "00123")
    }

    "leftPad with smaller length" in {
      assert(LeftPad("123", 2) == "123")
    }
  }

  "Main" - {
    "format with default length" in {
      assert(Main.format("scala") == "     scala")
    }

    "format with length" in {
      assert(Main.format("scala", 6) == " scala")
    }
  }

}
