val client =
  project.in(file("client"))
    .enablePlugins(ScalaJSPlugin, ScalaJSWeb)
    .settings(
      scalaVersion := "2.11.8",
      scalaJSModuleKind := ModuleKind.NodeJSModule,
      libraryDependencies += "org.scala-js" %%% "scalajs-dom" % "0.9.1",
      npmDependencies in Compile += "snabbdom" -> "0.5.3",
      // TODO Remove when it becomes the default
      scalaJSUseRhino := false
    )

val server =
  project.in(file("server"))
    .enablePlugins(PlayScala)
    .disablePlugins(PlayLayoutPlugin)
    .settings(
      scalaVersion := "2.11.8",
      libraryDependencies += "com.typesafe.play" %% "twirl-api" % "1.2.0",
      libraryDependencies += "org.scalatestplus.play" %% "scalatestplus-play" % "1.5.1" % Test,
      scalaJSProjects := Seq(client),
      pipelineStages in Assets := Seq(scalaJSPipeline),
      pipelineStages := Seq(digest, gzip)
    )

val play =
  project.in(file("."))
    .aggregate(client, server)
