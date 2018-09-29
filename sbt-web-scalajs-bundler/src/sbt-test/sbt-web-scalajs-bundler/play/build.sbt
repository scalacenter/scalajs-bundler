val client =
  project.in(file("client"))
    .enablePlugins(ScalaJSBundlerPlugin, ScalaJSWeb)
    .settings(
      scalaVersion := "2.11.12",
      scalaJSUseMainModuleInitializer := true,
      libraryDependencies += "org.scala-js" %%% "scalajs-dom" % "0.9.1",
      npmDependencies in Compile ++= Seq(
        "snabbdom" -> "0.5.3",
        "font-awesome" -> "4.7.0",
        "url-loader" -> "0.5.9"
      )
    )

val server =
  project.in(file("server"))
    .enablePlugins(PlayScala, WebScalaJSBundlerPlugin)
    .disablePlugins(PlayLayoutPlugin)
    .settings(
      scalaVersion := "2.11.12",
      libraryDependencies += "com.typesafe.play" %% "twirl-api" % "1.2.0",
      libraryDependencies += "org.scalatestplus.play" %% "scalatestplus-play" % "1.5.1" % Test,
      scalaJSProjects := Seq(client),
      pipelineStages in Assets := Seq(scalaJSPipeline),
      pipelineStages := Seq(digest, gzip),
      // Expose as sbt-web assets some files retrieved from the NPM packages of the `client` project
      npmAssets ++= NpmAssets.ofProject(client) { modules => (modules / "font-awesome").*** }.value
    )

val play =
  project.in(file("."))
    .aggregate(client, server)
