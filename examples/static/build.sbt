name := "example-static"

enablePlugins(ScalaJSPlugin)

scalaVersion := "2.11.8"

libraryDependencies += "org.scala-js" %%% "scalajs-dom" % "0.9.1"

scalaJSModuleKind := ModuleKind.NodeJSModule

npmDependencies in Compile += "snabbdom" -> "0.5.3"

webpackConfigFile in fullOptJS := Some(baseDirectory.value / "prod.webpack.config.js")
