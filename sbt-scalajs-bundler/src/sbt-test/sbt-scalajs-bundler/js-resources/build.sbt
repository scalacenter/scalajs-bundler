name := "js-resources"

enablePlugins(ScalaJSBundlerPlugin)

scalaVersion := "2.11.8"

npmDependencies in Compile += "node-uuid" -> "1.4.7"

libraryDependencies += "org.scalatest" %%% "scalatest" % "3.0.0" % Test
