package example

import org.scalatest.FreeSpec
import org.scalajs.dom.document

import scala.scalajs.js

class SomeTest extends FreeSpec {

  "snabbdom" - {
    "should patch the DOM" in {

      import snabbdom.{snabbdom, h, modules}
      val patch =
        snabbdom.init(js.Array(
          modules.props
        ))

      val vnode = h("h1", "It works": js.Any)

      val container = document.createElement("div")
      document.body.appendChild(container)

      patch(container, vnode)

      val patchedNode = document.body.lastChild
      assert(patchedNode.nodeName == "H1")
      assert(patchedNode.textContent == "It works")

    }
  }

}
