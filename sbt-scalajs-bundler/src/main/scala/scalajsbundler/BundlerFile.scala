package scalajsbundler

import java.io.File
import java.nio.file.Path

import scalajsbundler.Stats.WebpackStats
import scalajsbundler.util.CachedBundleFiles
import scala.collection.immutable.ListSet

/**
  * Files used in the `ScalaJSBundler` pipeline.
  */
sealed trait BundlerFile extends Product with Serializable {
  def file: File
}

object BundlerFile {
  /**
    * Files that may be inputs to the webpack process
    */
  sealed trait WebpackInput extends BundlerFile {
    def project: String
  }

  /**
    * Internal-only files
    */
  sealed abstract class Internal extends BundlerFile

  /**
    * A library-mode entrypoint file
    * @param application The [[Application]] this entrypoint was generated from
    * @param file The file containing the entry point
    */
  case class EntryPoint(application: Application, file: java.io.File)
      extends Internal
      with WebpackInput {
    def project: String = application.project
  }

  object EntryPoint {
    /** Filename of the generated bundle, given its module entry name */
    def fileName(entry: String): String = s"$entry-entrypoint.js"
  }

  /**
    * The package.json file, used for populating the node_modules folder
    *
    * @param file The file reference for the package.json
    */
  case class PackageJson(file: java.io.File) extends Internal

  /**
    * A webpack configuration file.
    *
    * @param application The [[Application]] this file runs webpack for
    * @param file The webpack.config.js file reference
    */
  case class WebpackConfig(application: Application, file: java.io.File)
      extends Internal {
    def project: String = application.project

    def targetDir: Path = file.getParentFile.toPath

    /**
      * Returns the Library identifying the asset produced by scala.js through webpack stats
      */
    def asLibrary(stats: Option[WebpackStats]): Library =
      Library(project,
              stats.flatMap { s =>
                s.resolveAsset(targetDir, project)
              }.getOrElse(targetDir.resolve(Library.fileName(project)).toFile),
              stats.map { s =>
                s.resolveAllAssets(targetDir)
              }.getOrElse(Nil)
            )

    /**
      * Returns library from a set of cached files
      * By convention the first element is the `file` and the rest are the assets
      */
    def asLibraryFromCached(cached: Set[File]): Library = {
      assert(cached.size >= 1)
      val assets = if (cached.size == 1) Nil else cached.tail.toList
      Library(project, cached.head, assets)
    }

    /**
      * Returns the Application for this configuration identifying the asset produced by scala.js through webpack stats
      */
    def asApplicationBundle(stats: Option[WebpackStats]): ApplicationBundle =
      ApplicationBundle(project,
                        stats.flatMap { s =>
                          s.resolveAsset(targetDir, project)
                        }.getOrElse(targetDir.resolve(ApplicationBundle.fileName(project)).toFile),
                        stats.map { s =>
                          s.resolveAllAssets(targetDir)
                        }.getOrElse(Nil))

    /**
      * Returns an application bundle from a set of cached files
      * By convention the first element is the `file` and the rest are the assets
      */
    def asApplicationBundleFromCached(cached: Set[File]): ApplicationBundle = {
      assert(cached.size >= 1)
      val assets = if (cached.size == 1) Nil else cached.tail.toList
      ApplicationBundle(project, cached.head, assets)
    }
  }

  /**
    * Public webpack artifacts -- those that might be served to clients or packaged
    */
  sealed abstract class Public extends BundlerFile {
    def project: String
    // Attributed files, the first is the main file and the rest are assets
    def attributedFiles: (File, Seq[File]) = (file, Seq.empty)
    def `type`: BundlerFileType
  }

  /**
    * The Scala.js application itself, aka -fastopt.js or -opt.js
    *
    * @param project The application project name
    * @param file The file containing the application javascript
    * @param assets All the assets on the application
    */
  case class Application(project: String, file: File, assets: List[java.io.File])
      extends Public
      with WebpackInput {

    def targetDir: Path = file.getParentFile.toPath

    val `type`: BundlerFileType = BundlerFileType.Application
    def asLoader: Loader =
      Loader(this,
             targetDir
               .resolve(Loader.fileName(project))
               .toFile)

    def asEntryPoint: EntryPoint =
      EntryPoint(this,
                 targetDir
                   .resolve(EntryPoint.fileName(project))
                   .toFile)

    def asApplicationBundle: ApplicationBundle =
      ApplicationBundle(project,
                        targetDir
                          .resolve(ApplicationBundle.fileName(project))
                          .toFile,
                        assets)

    /**
      * Returns an application bundle from a set of cached files
      */
    def asApplicationBundleFromCached(cached: Set[File]): ApplicationBundle = {
      assert(cached.size >= 1)
      val assets = if (cached.size == 1) Nil else cached.tail.toList
      ApplicationBundle(project, cached.head, assets)
    }
  }

  /**
    * A webpack library bundle, containing only libraries
    * @param project The project the library bundle was generated for
    * @param file The file containing the application javascript
    * @param assets All the assets on the application
    */
  case class Library(project: String, file: File, assets: List[java.io.File]) extends Public {
    val `type`: BundlerFileType = BundlerFileType.Library
    val cached: ListSet[File] = CachedBundleFiles.cached(file, assets)

    override def attributedFiles: (File, Seq[File]) = (file, assets)
  }

  object Library {

    /** Suffix to apply to the libraries bundle */
    val suffix: String = "-library.js"

    /** Filename of the generated libraries bundle, given its module entry name */
    def fileName(entry: String): String = s"$entry$suffix"
  }

  /**
    * A webpack loader file. Allows an [[Application]] to access the dependencies bundled
    * into a [[Library]]
    * @param application Application to be loaded
    * @param file Loader file
    */
  case class Loader(application: Application, file: java.io.File)
      extends Public {
    val `type`: BundlerFileType = BundlerFileType.Loader
    def project: String = application.project
  }

  object Loader {

    /** Suffix to apply to the loaders file */
    val suffix: String = "-loader.js"

    /** Filename of the generated bundle, given its module entry name */
    def fileName(entry: String): String = s"$entry$suffix"
  }

  /**
    * A fully self-contained application bundle, including all dependencies.
    *
    * @param project The project name
    * @param file The file containing the application javascript
    * @param assets All the assets on the application
    */
  case class ApplicationBundle(project: String, file: File, assets: List[java.io.File])
      extends Public {
    val `type`: BundlerFileType = BundlerFileType.ApplicationBundle

    val cached: ListSet[File] = CachedBundleFiles.cached(file, assets)

    override def attributedFiles: (File, Seq[File]) = (file, assets)
  }

  object ApplicationBundle {

    /** Filename of the generated bundle, given its module entry name */
    def fileName(entry: String): String = s"$entry-bundle.js"
  }
}
