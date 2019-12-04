enablePlugins(ScalaJSBundlerPlugin, ScalaJSJUnitPlugin)
scalaVersion := "2.12.3"
scalaJSUseMainModuleInitializer := true
npmDependencies in Compile += "left-pad" -> "1.1.3"

ivyLoggingLevel in ThisBuild := UpdateLogging.Quiet
