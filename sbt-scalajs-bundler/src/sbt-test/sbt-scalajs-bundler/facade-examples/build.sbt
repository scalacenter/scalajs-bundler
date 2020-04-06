name := "facade-examples"

enablePlugins(ScalaJSBundlerPlugin, ScalaJSJUnitPlugin)

scalaVersion := "2.11.12"

scalaJSUseMainModuleInitializer := true

npmDependencies in Compile += "uuid" -> "3.1.0"

ivyLoggingLevel in ThisBuild := UpdateLogging.Quiet
