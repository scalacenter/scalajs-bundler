
name := "global-namespace-with-jsdom-unit-testing"

enablePlugins(ScalaJSBundlerPlugin)

scalaVersion := "2.11.8"

scalaJSUseMainModuleInitializer := true

//#relevant-settings
libraryDependencies += "ru.pavkin" %%% "scala-js-momentjs" % "0.7.0"

npmDependencies in Compile ++= Seq(
  "moment" -> "2.18.1"
)

npmDevDependencies in Compile ++= Seq(
  "webpack-merge" -> "4.1.0",
  "imports-loader" -> "0.7.0",
  "expose-loader" -> "0.7.1"
)

webpackConfigFile in fastOptJS := Some(baseDirectory.value / "dev.webpack.config.js")

webpackConfigFile in Test := Some(baseDirectory.value / "test.webpack.config.js")

// Execute the tests in browser-like environment
jsEnv := new org.scalajs.jsenv.jsdomnodejs.JSDOMNodeJSEnv
//#relevant-settings

libraryDependencies += "org.scalatest" %%% "scalatest" % "3.0.0" % Test

useYarn := true
