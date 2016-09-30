package example

import controllers.Assets
import play.api.ApplicationLoader.Context
import play.api.{ApplicationLoader, BuiltInComponentsFromContext}

class Loader extends ApplicationLoader {
  def load(context: Context) = new ExampleComponents(context).application
}

class ExampleComponents(context: Context) extends BuiltInComponentsFromContext(context) {
  val controller = new ExampleController
  val assets = new Assets(httpErrorHandler)
  val router = new Routes(httpErrorHandler, controller, assets)
}