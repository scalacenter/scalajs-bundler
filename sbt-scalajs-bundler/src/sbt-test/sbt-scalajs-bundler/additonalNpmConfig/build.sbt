import scalajsbundler.util.JSON._
import scalajsbundler.Npm

val checkPackageJson = taskKey[Unit]("Check that the package.json file does not contain duplicate entries for the 'react' dependency")

lazy val npmConfig =
  Project("npmConfig", file("npmConfig"))
    .enablePlugins(ScalaJSBundlerPlugin)
    .settings(
      scalaVersion := "2.13.1",
      npmDependencies in Compile += "react" -> "16.13.1",
      npmExtraArgs in Compile := Seq("-silent"),
      additionalNpmConfig in Compile := Map(
        "name" -> str("foo"),
        "version" -> str("1.0.0"),
        "zoo" -> obj(
          "bees" -> bool(true),
          "cows" -> bool(false),
          "sharks" -> bool(true)
        )
      )
    ).settings(
      checkPackageJson := {
        val json = IO.read((npmUpdate in Compile).value / "package.json")
        val jsonLines = json.split("\n")

        findPackageJsonLine(jsonLines, "react" -> "16.13.1")

        findPackageJsonLine(jsonLines, "name" -> "foo")
        findPackageJsonLine(jsonLines, "version" -> "1.0.0")
        findPackageJsonLine(jsonLines, "bees" -> "true")
        findPackageJsonLine(jsonLines, "cows" -> "false")
        findPackageJsonLine(jsonLines, "sharks" -> "true")
      }
    )

def findPackageJsonLine(lines: Array[String], keyValue: (String, String)) = {

  val (key, value) = keyValue

  assert(
    lines.count(l => l.containsSlice(key) && l.containsSlice(value)) == 1,
    s"'package.json' missing expected key/value: ($key, $value)"
  )
}

ivyLoggingLevel in ThisBuild := UpdateLogging.Quiet
