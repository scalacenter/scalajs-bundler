scalaVersion := "2.12.8"

useYarn := true

yarnExtraArgs in Compile := Seq("--silent")

scalaJSUseMainModuleInitializer := true

npmDependencies in Compile += "neat" -> "2.1.0"

enablePlugins(ScalaJSBundlerPlugin)

ivyLoggingLevel in ThisBuild := UpdateLogging.Quiet
