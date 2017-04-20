
name := "sharedconfig"

enablePlugins(ScalaJSBundlerPlugin)

scalaVersion := "2.11.8"

libraryDependencies += "org.scala-js" %%% "scalajs-dom" % "0.9.1"

npmDependencies in Compile += "leaflet" -> "0.7.7"
npmDevDependencies in Compile ++= Seq(
  "webpack-merge" -> "4.1.0",
  "file-loader" -> "0.10.1",
  "image-webpack-loader" -> "3.3.0",
  "css-loader" -> "0.27.0",
  "style-loader" -> "0.16.0"
)

webpackConfigFile in fastOptJS := Some(baseDirectory.value / "dev.webpack.config.js")

// Use a different Webpack configuration file for production
webpackConfigFile in fullOptJS := Some(baseDirectory.value / "prod.webpack.config.js")

webpackConfigFile in test := Some(baseDirectory.value / "reload.webpack.config.js")

// Use a different Webpack configuration file for reload workflow
webpackConfigFile in webpackReload := Some(baseDirectory.value / "reload.webpack.config.js")

libraryDependencies += "org.scalatest" %%% "scalatest" % "3.0.0" % Test

// Execute the tests in browser-like environment
requiresDOM in Test := true

//enableReloadWorkflow := true

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
  assert(IO.readBytes((webpack in (Compile, fullOptJS)).value.head).length == 176387)
}