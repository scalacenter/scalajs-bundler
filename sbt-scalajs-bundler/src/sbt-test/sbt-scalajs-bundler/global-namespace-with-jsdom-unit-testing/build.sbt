
name := "global-namespace-with-jsdom-unit-testing"

enablePlugins(ScalaJSBundlerPlugin, ScalaJSJUnitPlugin)

scalaVersion := "2.12.11"

scalaJSUseMainModuleInitializer := true

//#relevant-settings
npmDependencies in Compile ++= Seq(
  "moment" -> "2.29.1"
)

npmDevDependencies in Compile ++= Seq(
  "webpack-merge" -> "5.7.3",
  "imports-loader" -> "2.0.0",
  "expose-loader" -> "2.0.0"
)

webpackConfigFile in fastOptJS := Some(baseDirectory.value / "dev.webpack.config.js")

webpackConfigFile in Test := Some(baseDirectory.value / "test.webpack.config.js")

// Execute the tests in browser-like environment
requireJsDomEnv in Test := true
//#relevant-settings

version in installJsdom := "12.0.0"

useYarn := true

ivyLoggingLevel in ThisBuild := UpdateLogging.Quiet
