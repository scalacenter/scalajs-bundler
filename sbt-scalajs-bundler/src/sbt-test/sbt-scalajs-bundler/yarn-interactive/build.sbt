useYarn := true
npmDependencies in Compile += "neat" -> "1.8.0"

enablePlugins(ScalaJSBundlerPlugin)