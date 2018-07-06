useYarn := true

yarnExtraArgs in Compile := Seq("--verbose")

npmDependencies in Compile += "neat" -> "1.1.2"

enablePlugins(ScalaJSBundlerPlugin)
