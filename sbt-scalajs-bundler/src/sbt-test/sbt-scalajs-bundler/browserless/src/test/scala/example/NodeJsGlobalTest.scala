package example

import org.scalatest.FreeSpec
import scala.scalajs.js
import js.Dynamic.{global => g}

class NodeJsGlobalTest extends FreeSpec {

  "Node.js global" - {

    "has a require function" in {
      assert(js.typeOf(g.global) == "object")
      assert(js.typeOf(g.global.require) == "function")

      assert(g.global.require("fs") != null)
    }
  }
}
