val sub1 =
  proj("sub1")
    .settings(
      npmDependencies in Compile += "react" -> "15.4.1"
    )

val sub2 =
  proj("sub2")
    .settings(
      npmDependencies in Compile += "react" -> "15.4.1"
    )

val sub3 =
  proj("sub3")
    .settings(
      npmDependencies in Compile += "react" -> "15.3.2"
    )

val sub4 =
  proj("sub4")
    .settings(
      npmDependencies in Compile += "react" -> ">=15.3.2"
    )

val checkPackageJson = taskKey[Unit]("Check that the package.json file does not contain duplicate entries for the 'react' dependency")

val noConflicts =
  proj("no-conflicts")
    .settings(
      checkPackageJson := {
        val json = IO.read((npmUpdate in Compile).value / "package.json")
        assert(json.split("\n").count(_.containsSlice("react")) == 1, json)
      }
    ).dependsOn(sub1, sub2)

val conflict =
  proj("conflict")
    .dependsOn(sub1, sub3)

val resolution =
  proj("resolution")
    .dependsOn(sub1, sub3)
    .settings(
      npmResolutions in Compile := Map("react" -> "15.4.1")
    )

val delegatedToPackageManager =
  proj("delegatedToPackageManager")
    .dependsOn(sub1, sub4)

def proj(id: String): Project =
  Project(id, file(id))
    .enablePlugins(ScalaJSBundlerPlugin)
    .settings(scalaVersion := "2.11.12")

ivyLoggingLevel in ThisBuild := UpdateLogging.Quiet
