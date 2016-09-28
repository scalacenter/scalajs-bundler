package example

import scala.scalajs.js
import scala.scalajs.js.JSApp
import scala.scalajs.js.Dynamic.literal

import snabbdom.{snabbdom, h, modules}

import org.scalajs.dom.document

object Main extends JSApp {
  def main(): Unit = {

    val someFn = () => println("someFn")
    val anotherEventHandler = () => println("another event handler")

    // Based on https://github.com/paldepind/snabbdom#inline-example
    val patch =
      snabbdom.init(js.Array( // Init patch function with choosen modules
        modules.`class`, // makes it easy to toggle classes
        modules.props, // for setting properties on DOM elements
        modules.style, // handles styling on elements with support for animations
        modules.eventlisteners // attaches event listeners
      ))

    val vnode = h("div#container.two.classes", literal(on = literal(click = someFn)), js.Array(
      h("span", literal(style = literal(fontWeight = "bold")), "This is bold": js.Any),
      " and this is just normal text",
      h("a", literal(props = literal(href = "/foo")), "I'll take you places!": js.Any)
    ))
    val container = document.getElementById("container")
    // Patch into empty DOM element – this modifies the DOM as a side effect
    patch(container, vnode)
    val newVnode = h("div#container.two.classes", literal(on = literal(click = anotherEventHandler)), js.Array(
      h("span", literal(style = literal(fontWeight = "normal", fontStyle = "italic")), "This is now italic type": js.Any),
      " and this is still just normal text",
      h("a", literal(props = literal(href = "/bar")), "I'll take you places!": js.Any)
    ))
    // Second `patch` invocation
    patch(vnode, newVnode) // Snabbdom efficiently updates the old view to the new state
  }
}
