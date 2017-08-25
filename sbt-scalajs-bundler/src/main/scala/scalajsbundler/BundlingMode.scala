package scalajsbundler

/**
  * The BundlingMode dictates how webpack is configured for bundling. Please refer
  * to the members of the sealed family details.
  */
sealed trait BundlingMode

object BundlingMode {

  /**
    * Bundle the entire application with webpack. Using this mode, the webpack `entry` key will
    * contain the Scala.js output file. All dependencies will be resolved via webpack, and the entire
    * bundle will be processed by any plugins. The output will be a runnable bundle, not a library.
    *
    * To see the specific webpack configuration that will be generated, refer to [[Webpack.writeConfigFile]].
    */
  case object Application extends BundlingMode

  /**
    * Shared base class for [[LibraryOnly]] and [[LibraryAndApplication]]. Both
    * must provide an `exportedName` field indicating what javascript global
    * the libraries will be exported to. Both library modes will generate an `entrypoints` file based
    * on the Scala.js imports and use that as the `entrypoint` for the generated `webpack.config.js`. The webpack
    * output will be a library, which will assign itself to a global variable when loaded.
    *
    * The `entrypoints` file also contains a `require` implementation, which can be exposed globally by including
    * the `loader` file. Refer to [[util.JSBundler.loaderScript]] for an example of such a script.
    *
    * To see the specific webpack configuration that will be generated, refer to [[Webpack.writeConfigFile]].
    */
  sealed trait Library extends BundlingMode {

    /**
      * Name of the global variable containing the dependencies
      */
    def exportedName: String
  }

  /**
    * Bundle only the libraries used by the application. This mode will generate an `entrypoints` file based
    * on the Scala.js imports and use that as the entrypoint for the generated `webpack.config.js`. The webpack
    * output will be a library, which will assign itself to a global variable when loaded.
    *
    * The `library` file produce in this mode must be combined with the `loader` and the Scala.js output in order
    * to fully duplicate the usability of [[Application]] mode.
    *
    * Refer to [[Library]] for additional details.
    *
    */
  case class LibraryOnly(exportedName: String = defaultLibraryExportedName) extends Library

  /**
    * Builds on [[LibraryOnly]] by generating the `loader` and concatenating it with the Scala.js output file. This
    * output is designed to be a drop-in replacement for fully processing the file via webpack ([[Application]] mode). When
    * `webpackEmitSourceMaps := true`, this mode will attempt to merge all the files using the node.js
    * 'concat-with-sourcemaps' module.
    *
    * Refer to [[Library]] for additional details.
    */
  case class LibraryAndApplication(exportedName: String = defaultLibraryExportedName) extends Library

  /**
    * The default exported library name, used by [[LibraryOnly]] and [[LibraryAndApplication]]
    */
  val defaultLibraryExportedName = "ScalaJSBundlerLibrary"

  /**
    * The default BundlingMode used by the ScalaJSBundler
    */
  val Default: BundlingMode = Application

}
