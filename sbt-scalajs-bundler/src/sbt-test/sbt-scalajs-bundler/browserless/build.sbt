name := "browserless"

enablePlugins(ScalaJSBundlerPlugin, ScalaJSJUnitPlugin)

scalaVersion := "2.11.12"

scalaJSUseMainModuleInitializer := true

// Adds a dependency on the uuid npm package
npmDependencies in Compile += "uuid" -> "3.1.0"

ivyLoggingLevel in ThisBuild := UpdateLogging.Quiet
