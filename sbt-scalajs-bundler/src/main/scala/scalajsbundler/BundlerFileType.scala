package scalajsbundler

/**
  * The type of a given [[BundlerFile.Public]]. Used for tagging files when they are produced by
  * the ScalaJsBundler and handed off to other SBT tasks.
  */
sealed abstract class BundlerFileType

object BundlerFileType {

  /**
    * Scala.js application
    */
  case object Application extends BundlerFileType

  /**
    * Library dependencies provided by webpack
    */
  case object Library extends BundlerFileType

  /**
    * Dependency loader, provides [[Library]] dependencies to [[Application]]
    */
  case object Loader extends BundlerFileType

  /**
    * Fully linked application bundle, containing [[Application]] and all it's dependencies
    */
  case object ApplicationBundle extends BundlerFileType

  /**
    * An asset of the bundled application
    */
  case object Asset extends BundlerFileType
}
