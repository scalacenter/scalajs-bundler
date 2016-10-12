package scalajsbundler

import org.scalajs.sbtplugin.ScalaJSPlugin
import sbt._

object ScalaJSBundler extends AutoPlugin {

  override lazy val requires = ScalaJSPlugin

  override lazy val trigger = allRequirements

  object autoImport {

    val npmDependencies = settingKey[Seq[(String, String)]]("NPM dependencies (libraries that your program uses)")

    val npmDevDependencies = settingKey[Seq[(String, String)]]("NPM dev dependencies (libraries that the build uses)")

    val npmUpdate = taskKey[Unit]("Fetch NPM dependencies")

    val webpackConfigFile = settingKey[Option[File]]("Configuration file to use with webpack")

    val webpackEntries = taskKey[Seq[(String, File)]]("Webpack entry bundles")

    val webpack = taskKey[Seq[File]]("Bundle the output of a Scala.js stage using webpack")

  }

  override lazy val projectSettings: Seq[Def.Setting[_]] =
    ScalaJSBundlerInternal.projectSettings

}
