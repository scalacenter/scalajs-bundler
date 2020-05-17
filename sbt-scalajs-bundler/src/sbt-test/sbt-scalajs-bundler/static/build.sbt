name := "static"

enablePlugins(ScalaJSBundlerPlugin, ScalaJSJUnitPlugin)

scalaVersion := "2.11.12"

scalaJSUseMainModuleInitializer := true

libraryDependencies += "org.scala-js" %%% "scalajs-dom" % "1.0.0"

npmDependencies in Compile += "snabbdom" -> "0.5.3"

npmDevDependencies in Compile += "uglifyjs-webpack-plugin" -> "1.2.2"

// Use a different Webpack configuration file for production
webpackConfigFile in fullOptJS := Some(baseDirectory.value / "prod.webpack.config.js")

// Execute the tests in browser-like environment
requireJsDomEnv in Test := true

version in installJsdom := "16.2.0"

webpackBundlingMode := BundlingMode.LibraryAndApplication()

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
  //#filter-files
  val files = (webpack in (Compile, fullOptJS)).value
  val bundleFile = files
    .find(_.metadata.get(BundlerFileTypeAttr).exists(_ == BundlerFileType.ApplicationBundle))
    .get.data
  //#filter-files
  val artifactSize = IO.readBytes(bundleFile).length

  val sizeLow = 17000
  val sizeHigh = 22000

  // Account for minor variance in size due to transitive dependency updates
  assert(
    artifactSize >= sizeLow && artifactSize <= sizeHigh,
    s"expected: [$sizeLow, $sizeHigh], got: $artifactSize"
  )
}

ivyLoggingLevel in ThisBuild := UpdateLogging.Quiet
