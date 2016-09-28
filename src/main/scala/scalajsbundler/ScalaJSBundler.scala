package scalajsbundler

import org.scalajs.sbtplugin.ScalaJSPlugin
import sbt._

object ScalaJSBundler extends AutoPlugin {

  override lazy val requires = ScalaJSPlugin

  override lazy val trigger = allRequirements

  object autoImport {

    val npmDependencies = settingKey[Map[String, String]]("NPM dependencies (libraries that your program uses)")

    val npmDevDependencies = settingKey[Map[String, String]]("NPM dev dependencies (libraries that the build uses)")

    val npmUpdate = taskKey[File]("Fetch NPM dependencies")

    val webpackConfigFile = settingKey[Option[File]]("Configuration file to use with webpack")

    val webpack = taskKey[File]("Bundle the output of a Scala.js stage using Webpack")

  }

  override lazy val projectSettings: Seq[Def.Setting[_]] =
    ScalaJSBundlerInternal.projectSettings

}
