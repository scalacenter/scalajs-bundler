name := "static"

enablePlugins(ScalaJSBundlerPlugin, ScalaJSJUnitPlugin)

scalaVersion := "2.11.12"

scalaJSUseMainModuleInitializer := true

version in webpack := "2.2.1"

version in startWebpackDevServer := "2.11.1"

libraryDependencies += "org.scala-js" %%% "scalajs-dom" % "0.9.8"

npmDependencies in Compile += "snabbdom" -> "0.5.3"

npmDevDependencies in Compile += "uglifyjs-webpack-plugin" -> "0.4.3"

// Use a different Webpack configuration file for production
webpackConfigFile in fullOptJS := Some(baseDirectory.value / "prod.webpack.config.js")

// Execute the tests in browser-like environment
requireJsDomEnv in Test := true

useYarn := true

// HtmlUnit does not support ECMAScript 2015
scalaJSLinkerConfig ~= { _.withESFeatures(_.withUseECMAScript2015(false)) }

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
  val artifactSize = IO.readBytes((webpack in (Compile, fullOptJS)).value.head.data).length
  val sizeLow = 18000
  val sizeHigh = 21000
  assert(
    artifactSize >= sizeLow && artifactSize <= sizeHigh,
    s"expected: [$sizeLow, $sizeHigh], got: $artifactSize"
  )
}

ivyLoggingLevel in ThisBuild := UpdateLogging.Quiet
