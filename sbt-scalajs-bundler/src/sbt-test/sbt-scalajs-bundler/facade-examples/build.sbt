name := "facade-examples"

enablePlugins(ScalaJSBundlerPlugin)

scalaVersion := "2.11.8"

scalaJSUseMainModuleInitializer := true

npmDependencies in Compile += "node-uuid" -> "1.4.7"

libraryDependencies += "org.scalatest" %%% "scalatest" % "3.0.0" % Test
