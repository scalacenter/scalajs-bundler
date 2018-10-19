useYarn := true

yarnExtraArgs in Compile := Seq("--silent")

npmDependencies in Compile += "neat" -> "1.1.2"

enablePlugins(ScalaJSBundlerPlugin)

ivyLoggingLevel in ThisBuild := UpdateLogging.Quiet
