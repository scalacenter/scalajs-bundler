
// Problematic combination
webpackConfigFile in fastOptJS :=
  Some(baseDirectory.value / "foo-webpack.config.js")

enableReloadWorkflow := true


// Boilerplate
scalaJSUseMainModuleInitializer := true

version in webpack := "2.7.0"

npmDevDependencies in Compile ++= Seq(
  "webpack-merge" -> "4.1.0",
  "file-loader" -> "0.11.2"
)

enablePlugins(ScalaJSBundlerPlugin)

scalaVersion := "2.12.3"

lazy val check = taskKey[Unit]("yolo")

check := {
  val res = (webpack in (Compile, fastOptJS)).value.head
  val out = s"node $res".!!
  assert(out.split(System.lineSeparator).head == "dotty-loaded.svg")
}
