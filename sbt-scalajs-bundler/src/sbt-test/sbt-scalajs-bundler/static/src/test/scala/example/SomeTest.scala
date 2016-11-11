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

      assert(document.body.firstChild.nodeName == "H1")

    }
  }

}
