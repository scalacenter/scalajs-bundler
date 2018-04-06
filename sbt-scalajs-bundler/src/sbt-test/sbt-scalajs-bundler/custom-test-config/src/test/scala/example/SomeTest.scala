package example

import utest._
import japgolly.scalajs.react.test._

import scala.scalajs.js

object RenderTest extends TestSuite {
  val tests = Tests {
    'render - {
      val component = Component()
      ReactTestUtils.withRenderedIntoDocument(component) { m =>
        assert(m.outerHtmlScrubbed() == """<div class="app"></div>""")
      }
    }
  }
}
