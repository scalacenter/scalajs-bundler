name := "scalajs-npm-dependencies-example"

description := "Example project demonstrating how to use npm packages from Scala.js"

enablePlugins(ScalaJSPlugin, SbtWebpack)

scalaVersion := "2.11.8"

libraryDependencies += "org.scala-js" %%% "scalajs-dom" % "0.9.1"

scalaJSModuleKind := ModuleKind.NodeJSModule

npmDependencies in Compile += "snabbdom" -> "0.5.3"

webpackConfigFile in fullOptJS := Some(baseDirectory.value / "prod.webpack.config.js")
