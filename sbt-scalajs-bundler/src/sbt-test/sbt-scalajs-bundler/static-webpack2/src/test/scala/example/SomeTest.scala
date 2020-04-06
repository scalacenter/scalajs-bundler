package example

import org.junit.Assert._
import org.junit.Test

import org.scalajs.dom.document

import scala.scalajs.js

class SomeTest {

  @Test def testSnabbdom(): Unit = {
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
    assertEquals("H1", patchedNode.nodeName)
    assertEquals("It works", patchedNode.textContent)
  }

}
