name := "example-browserless"

enablePlugins(ScalaJSPlugin)

scalaVersion := "2.11.8"

// Tells Scala.js to produce a Node.js module
scalaJSModuleKind := ModuleKind.NodeJSModule

// Adds a dependency on the node-uuid npm package
npmDependencies in Compile += "node-uuid" -> "1.4.7"

// Adds a dependency on scalatest
libraryDependencies += "org.scalatest" %%% "scalatest" % "3.0.0" % Test

// TODO Remove when it becomes the default
scalaJSUseRhino := false