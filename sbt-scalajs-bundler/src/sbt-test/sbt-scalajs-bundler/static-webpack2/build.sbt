name := "static"

enablePlugins(ScalaJSBundlerPlugin)

scalaVersion := "2.11.8"

version in webpack := "2.1.0-beta.27"

libraryDependencies += "org.scala-js" %%% "scalajs-dom" % "0.9.1"

npmDependencies in Compile += "snabbdom" -> "0.5.3"

// Use a different Webpack configuration file for production
webpackConfigFile in fullOptJS := Some(baseDirectory.value / "prod.webpack.config.js")

libraryDependencies += "org.scalatest" %%% "scalatest" % "3.0.0" % Test

// Execute the tests in browser-like environment
requiresDOM in Test := true

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
  assert(IO.readBytes((webpack in (Compile, fullOptJS)).value.head).length == 19848)
}
