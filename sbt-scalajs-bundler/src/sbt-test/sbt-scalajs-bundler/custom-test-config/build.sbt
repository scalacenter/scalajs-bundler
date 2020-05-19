name := "custom-test"

val reactJS = "16.13.1"
val scalaJsReact = "1.7.0"

enablePlugins(ScalaJSBundlerPlugin)

scalaVersion := "2.13.2"

scalaJSUseMainModuleInitializer := true

libraryDependencies ++= Seq(
  "com.github.japgolly.scalajs-react" %%% "core"  % scalaJsReact,
  "com.github.japgolly.scalajs-react" %%% "test"  % scalaJsReact % Test,
  "com.lihaoyi"                       %%% "utest" % "0.7.4" % Test
)

// Use a different Webpack configuration file for test
webpackConfigFile in Test := Some(baseDirectory.value / "test.webpack.config.js")

testFrameworks += new TestFramework("utest.runner.Framework")

// Execute the tests in browser-like environment
requireJsDomEnv in Test := true

webpackBundlingMode := BundlingMode.LibraryAndApplication()

useYarn := true

version in webpack := "4.32.2"

npmDependencies in Compile ++= Seq(
  "react"     -> reactJS,
  "react-dom" -> reactJS
)

ivyLoggingLevel in ThisBuild := UpdateLogging.Quiet
