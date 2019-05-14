enablePlugins(ScalaJSBundlerPlugin)

scalaVersion := "2.12.8"

sourceGenerators in Compile += Def.task {
  val _ = (npmInstallDependencies in Compile).value
  Seq.empty[File]
}
