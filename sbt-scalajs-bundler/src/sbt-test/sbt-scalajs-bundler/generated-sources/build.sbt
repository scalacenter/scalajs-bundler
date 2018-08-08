enablePlugins(ScalaJSBundlerPlugin)

sourceGenerators in Compile += Def.task {
  val _ = (npmInstallDependencies in Compile).value
  Seq.empty[File]
}
