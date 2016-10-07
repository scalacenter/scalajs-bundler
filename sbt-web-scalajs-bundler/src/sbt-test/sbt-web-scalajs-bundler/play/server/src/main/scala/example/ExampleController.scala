package example

import controllers.Assets.Asset
import play.api.mvc.{Action, Controller}
import play.twirl.api.StringInterpolation

class ExampleController extends Controller {

  val index = Action {
    Ok(
      html"""<!doctype html>
        <html>
          <head>
            <script src="${controllers.routes.Assets.versioned("client-fastopt-bundle.js")}" defer></script>
          </head>
          <body>
            <h1>sbt-scalajs-bundler Play Example</h1>
          </body>
        </html>
      """
    )
  }

}
