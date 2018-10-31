package example

import controllers.{Assets, AssetsConfiguration, DefaultAssetsMetadata}
import play.api.ApplicationLoader.Context
import play.api.{ApplicationLoader, BuiltInComponentsFromContext}
import router.Routes

class Loader extends ApplicationLoader {
  def load(context: Context) = new ExampleComponents(context).application
}

class ExampleComponents(context: Context) extends BuiltInComponentsFromContext(context) {
  val httpFilters = Seq()
  val controller = new ExampleController
  val assetsConfiguration = new AssetsConfiguration()
  val assetsMetadata = new DefaultAssetsMetadata(assetsConfiguration, environment.resource _, fileMimeTypes)
  val assets = new Assets(httpErrorHandler, assetsMetadata)
  val router = new Routes(httpErrorHandler, controller, assets)
}