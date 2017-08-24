name := "js-resources"

enablePlugins(ScalaJSBundlerPlugin)

scalaVersion := "2.11.8"

scalaJSUseMainModuleInitializer := true

npmDependencies in Compile += "uuid" -> "3.1.0"

libraryDependencies += "org.scalatest" %%% "scalatest" % "3.0.0" % Test
