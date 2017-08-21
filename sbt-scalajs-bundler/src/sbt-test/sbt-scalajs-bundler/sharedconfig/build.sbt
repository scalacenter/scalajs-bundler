
name := "sharedconfig"

enablePlugins(ScalaJSBundlerPlugin)

scalaVersion := "2.11.8"

scalaJSUseMainModuleInitializer := true

libraryDependencies += "org.scala-js" %%% "scalajs-dom" % "0.9.1"

npmDependencies in Compile += "leaflet" -> "0.7.7"
npmDevDependencies in Compile ++= Seq(
  "webpack-merge" -> "4.1.0",
  "file-loader" -> "0.11.2",
  "image-webpack-loader" -> "3.3.1",
  "css-loader" -> "0.28.5",
  "style-loader" -> "0.18.2",
  "url-loader" -> "0.5.9"
)

webpackConfigFile in fastOptJS := Some(baseDirectory.value / "dev.webpack.config.js")

// Use a different Webpack configuration file for production
webpackConfigFile in fullOptJS := Some(baseDirectory.value / "prod.webpack.config.js")

// Use the shared Webpack configuration file for reload workflow and for running the tests
webpackConfigFile in webpackReload := Some(baseDirectory.value / "common.webpack.config.js")

webpackConfigFile in Test := Some(baseDirectory.value / "common.webpack.config.js")

libraryDependencies += "org.scalatest" %%% "scalatest" % "3.0.0" % Test

// Execute the tests in browser-like environment
requiresDOM in Test := true

enableReloadWorkflow := true

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
  val size = IO.readBytes((webpack in (Compile, fullOptJS)).value.head).length
  // Account for minor variance in size due to transitive dependency updates
  assert(size > 150000 && size < 200000)
}
