
name := "sharedconfig"

enablePlugins(ScalaJSBundlerPlugin)

scalaVersion := "2.11.12"

scalaJSUseMainModuleInitializer := true

libraryDependencies += "org.scala-js" %%% "scalajs-dom" % "0.9.1"

npmDependencies in Compile += "leaflet" -> "0.7.7"

version in webpack                     := "4.1.1"

version in startWebpackDevServer       := "3.1.1"

npmDevDependencies in Compile ++= Seq(
  "webpack-merge" -> "4.1.2",
  "file-loader" -> "1.1.11",
  "image-webpack-loader" -> "4.1.0",
  "css-loader" -> "0.28.10",
  "style-loader" -> "0.20.2",
  "url-loader" -> "1.0.1"
)

webpackConfigFile in fastOptJS := Some(baseDirectory.value / "dev.webpack.config.js")

// Use a different Webpack configuration file for production
webpackConfigFile in fullOptJS := Some(baseDirectory.value / "prod.webpack.config.js")

// Use the shared Webpack configuration file for reload workflow and for running the tests
webpackConfigFile in Test := Some(baseDirectory.value / "common.webpack.config.js")

libraryDependencies += "org.scalatest" %%% "scalatest" % "3.0.0" % Test

// Execute the tests in browser-like environment
requiresDOM in Test := true

webpackBundlingMode := BundlingMode.LibraryAndApplication()

useYarn := true

// Check that a HTML can be loaded (and that its JavaScript can be executed) without errors
InputKey[Unit]("html") := {
  import complete.DefaultParsers._
  val page = (Space ~> StringBasic).parsed
  import com.gargoylesoftware.htmlunit.WebClient
  val client = new WebClient()
  try {
    client.getPage(s"file://${baseDirectory.value.absolutePath}/$page")
  } finally {
    client.close()
  }
}

TaskKey[Unit]("checkSize") := {
  val files = (webpack in (Compile, fullOptJS)).value
  val bundleFile = files
    .find(_.metadata.get(BundlerFileTypeAttr).exists(_ == BundlerFileType.ApplicationBundle))
    .get.data
  val artifactSize = IO.readBytes(bundleFile).length
  // Account for minor variance in size due to transitive dependency updates
  assert(artifactSize > 150000 && artifactSize < 200000)
}

ivyLoggingLevel in ThisBuild := UpdateLogging.Quiet
