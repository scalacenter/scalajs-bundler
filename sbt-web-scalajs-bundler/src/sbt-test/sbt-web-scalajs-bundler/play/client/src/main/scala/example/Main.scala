package example

import snabbdom.{snabbdom, modules, h}

import scala.scalajs.js
import scala.scalajs.js.JSApp

import org.scalajs.dom.document

object Main extends JSApp {

  def main(): Unit = {
    val patch = snabbdom.init(js.Array(modules.props, modules.eventlisteners))

    val view =
      h("p", "Hello, world!": js.Any)

    patch(document.body.querySelector("div"), view)
  }

}
