name := "browserless"

enablePlugins(ScalaJSBundlerPlugin)

scalaVersion := "2.11.8"

// Adds a dependency on the node-uuid npm package
npmDependencies in Compile += "node-uuid" -> "1.4.7"

// Adds a dependency on scalatest
libraryDependencies += "org.scalatest" %%% "scalatest" % "3.0.0" % Test
