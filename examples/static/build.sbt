name := "example-static"

enablePlugins(ScalaJSPlugin)

scalaVersion := "2.11.8"

// Tells Scala.js to produce a Node.js module
scalaJSModuleKind := ModuleKind.NodeJSModule

// Adds a dependency on the Scala facade for the DOM API
libraryDependencies += "org.scala-js" %%% "scalajs-dom" % "0.9.1"

// Adds a dependency on the snabbdom npm package
npmDependencies in Compile += "snabbdom" -> "0.5.3"

// Uses a different Webpack configuration file for production
webpackConfigFile in fullOptJS := Some(baseDirectory.value / "prod.webpack.config.js")
