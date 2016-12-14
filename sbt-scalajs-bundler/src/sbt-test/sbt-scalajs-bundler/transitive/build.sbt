val sub1 =
  project.in(file("sub1"))
    .enablePlugins(ScalaJSBundlerPlugin)
    .settings(
      scalaVersion := "2.11.8",
      npmDependencies in Compile += "react" -> "15.4.1"
    )

val sub2 =
  project.in(file("sub2"))
    .enablePlugins(ScalaJSBundlerPlugin)
    .settings(
      scalaVersion := "2.11.8",
      npmDependencies in Compile += "react" -> "15.4.1"
    )

val checkPackageJson = taskKey[Unit]("Check that the package.json file does not contain duplicate entries for the 'react' dependency")

val root =
  project.in(file("."))
    .enablePlugins(ScalaJSBundlerPlugin)
    .settings(
      scalaVersion := "2.11.8",
      checkPackageJson := {
        val json = IO.read((npmUpdate in Compile).value / "package.json")
        assert(json.split("\n").count(_.containsSlice("react")) == 1, json)
      }
    ).dependsOn(sub1, sub2)
