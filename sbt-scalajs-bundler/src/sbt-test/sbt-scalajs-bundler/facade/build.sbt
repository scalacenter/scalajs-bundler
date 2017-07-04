val `facade-project` =
  project.in(file("."))
    .aggregate(usage)

lazy val usage =
  project.in(file("usage"))
    .enablePlugins(ScalaJSBundlerPlugin)
    .settings(commonSettings: _*)
    .settings(
      scalaJSUseMainModuleInitializer := true,
      libraryDependencies += "org.scalatest" %%% "scalatest" % "3.0.0" % Test
    )
    .dependsOn(facade)

lazy val facade =
  project.in(file("facade"))
    .enablePlugins(ScalaJSBundlerPlugin)
    .settings(commonSettings: _*)
    .settings(
      npmDependencies in Compile += "node-uuid" -> "1.4.7"
    )

lazy val commonSettings = Seq(
  scalaVersion := "2.11.8"
)

