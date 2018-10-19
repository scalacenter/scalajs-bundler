name := "custom-test"

val reactJS = "15.6.1"
val scalaJsReact = "1.1.1"

enablePlugins(ScalaJSBundlerPlugin)

scalaVersion := "2.11.12"

scalaJSUseMainModuleInitializer := true

libraryDependencies ++= Seq(
  "com.github.japgolly.scalajs-react" %%% "core"  % scalaJsReact,
  "com.github.japgolly.scalajs-react" %%% "test"  % scalaJsReact % Test,
  "com.lihaoyi"                       %%% "utest" % "0.6.3" % Test
)

// Use a different Webpack configuration file for test
webpackConfigFile in Test := Some(baseDirectory.value / "test.webpack.config.js")

testFrameworks += new TestFramework("utest.runner.Framework")

// Execute the tests in browser-like environment
requiresDOM in Test := true

webpackBundlingMode := BundlingMode.LibraryAndApplication()

useYarn := true

version in webpack := "4.1.1"

version in startWebpackDevServer := "3.1.1"

npmDependencies in Compile ++= Seq(
  "react"     -> reactJS,
  "react-dom" -> reactJS
)

ivyLoggingLevel in ThisBuild := UpdateLogging.Quiet
