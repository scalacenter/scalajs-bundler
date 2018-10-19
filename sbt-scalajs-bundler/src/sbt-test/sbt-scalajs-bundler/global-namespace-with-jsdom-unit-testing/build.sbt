
name := "global-namespace-with-jsdom-unit-testing"

enablePlugins(ScalaJSBundlerPlugin)

scalaVersion := "2.11.12"

scalaJSUseMainModuleInitializer := true

//#relevant-settings
libraryDependencies += "ru.pavkin" %%% "scala-js-momentjs" % "0.7.0"

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
requiresDOM in Test := true
//#relevant-settings

version in installJsdom := "12.0.0"

libraryDependencies += "org.scalatest" %%% "scalatest" % "3.0.0" % Test

useYarn := true

ivyLoggingLevel in ThisBuild := UpdateLogging.Quiet
