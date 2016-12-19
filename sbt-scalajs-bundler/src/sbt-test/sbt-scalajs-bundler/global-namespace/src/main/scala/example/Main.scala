package example

import japgolly.scalajs.react.{ReactComponentB, ReactDOM}
import japgolly.scalajs.react.vdom.prefix_<^._
import org.scalajs.dom.window.document

import scala.scalajs.js.JSApp

object Main extends JSApp {
  def main(): Unit = {
    val HelloMessage = ReactComponentB[String]("HelloMessage")
      .render($ => <.div("Hello ", $.props))
      .build

    ReactDOM.render(HelloMessage("John"), document.getElementById("container"))
  }
}
