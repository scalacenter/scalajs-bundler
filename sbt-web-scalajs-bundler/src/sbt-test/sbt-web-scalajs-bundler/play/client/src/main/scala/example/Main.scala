package example

import snabbdom.{snabbdom, modules, h}

import scala.scalajs.js

import org.scalajs.dom.document

object Main {

  def main(args: Array[String]): Unit = {
    val patch = snabbdom.init(js.Array(modules.props, modules.eventlisteners))

    val view =
      h("p", "Hello, world!": js.Any)

    patch(document.body.querySelector("div"), view)
  }

}
