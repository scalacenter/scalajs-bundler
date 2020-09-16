package example

import play.api.mvc.{AbstractController, ControllerComponents}
import play.twirl.api.StringInterpolation

class ExampleController(cc: ControllerComponents) extends AbstractController(cc) {

  val index = {
    val result =
      Ok(
        html"""<!doctype html>
        <html>
          <head></head>
          <body>
            <div>App is not loaded.</div>
            ${scalajs.html.scripts("client", controllers.routes.Assets.versioned(_).toString, name => getClass.getResource(s"/public/$name") != null)}
          </body>
        </html>
      """
      )
    Action(result)
  }

}
