
name := "global-namespace-with-jsdom-unit-testing"

enablePlugins(ScalaJSBundlerPlugin, ScalaJSJUnitPlugin)

scalaVersion := "2.12.11"

scalaJSUseMainModuleInitializer := true

//#relevant-settings
npmDependencies in Compile ++= Seq(
  "moment" -> "2.18.1"
)

npmDevDependencies in Compile ++= Seq(
  "webpack-merge" -> "4.1.2",
  "imports-loader" -> "0.8.0",
  "expose-loader" -> "0.7.5"
)

webpackConfigFile in fastOptJS := Some(baseDirectory.value / "dev.webpack.config.js")

webpackConfigFile in Test := Some(baseDirectory.value / "test.webpack.config.js")

// Execute the tests in browser-like environment
requireJsDomEnv in Test := true
//#relevant-settings

version in installJsdom := "12.0.0"

useYarn := true

ivyLoggingLevel in ThisBuild := UpdateLogging.Quiet
