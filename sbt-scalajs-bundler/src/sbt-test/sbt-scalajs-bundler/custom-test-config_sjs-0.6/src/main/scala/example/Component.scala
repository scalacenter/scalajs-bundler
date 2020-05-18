package example

import japgolly.scalajs.react._
import japgolly.scalajs.react.vdom.html_<^._

object Component {

  private val component =
    ScalaComponent
      .builder[Unit]
      .render_P ( _ =>
        <.div(
          ^.cls := "app"
        )
      ).build

  def apply() = component()
}
