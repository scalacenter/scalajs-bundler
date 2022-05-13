scalaVersion := "2.12.8"

useYarn := true

yarnExtraArgs in Compile := Seq("--silent")

scalaJSUseMainModuleInitializer := true

webpackConfigFile := Some(baseDirectory.value / "webpack.config.js")

enablePlugins(ScalaJSBundlerPlugin)

ivyLoggingLevel in ThisBuild := UpdateLogging.Quiet
