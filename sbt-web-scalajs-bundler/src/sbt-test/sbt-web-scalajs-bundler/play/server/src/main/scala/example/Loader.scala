package example

import play.api.ApplicationLoader.Context
import play.api.{Application, ApplicationLoader, BuiltInComponentsFromContext}
import play.filters.HttpFiltersComponents
import router.Routes

class Loader extends ApplicationLoader {
  def load(context: Context): Application = new ExampleComponents(context).application
}

class ExampleComponents(context: Context)
    extends BuiltInComponentsFromContext(context)
    with HttpFiltersComponents
    with controllers.AssetsComponents {
  val controller = new ExampleController(controllerComponents)
  val router = new Routes(httpErrorHandler, controller, assets)
}
