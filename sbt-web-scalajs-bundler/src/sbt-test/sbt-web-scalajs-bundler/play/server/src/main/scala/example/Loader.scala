package example

import controllers.AssetsComponents
import play.api.ApplicationLoader.Context
import play.api.{ApplicationLoader, BuiltInComponentsFromContext}
import play.filters.HttpFiltersComponents
import router.Routes

class Loader extends ApplicationLoader {
  def load(context: Context) = new ExampleComponents(context).application
}

class ExampleComponents(context: Context) extends BuiltInComponentsFromContext(context) with AssetsComponents with HttpFiltersComponents {
  val controller = new ExampleController
  val router = new Routes(httpErrorHandler, controller, assets)
}