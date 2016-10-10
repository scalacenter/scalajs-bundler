val client =
  project.in(file("client"))
    .enablePlugins(ScalaJSPlugin, ScalaJSWeb)
    .settings(
      scalaVersion := "2.11.8",
      scalaJSModuleKind := ModuleKind.NodeJSModule,
      libraryDependencies += "org.scala-js" %%% "scalajs-dom" % "0.9.1",
      npmDependencies in Compile += "snabbdom" -> "0.5.3"
    )

val server =
  project.in(file("server"))
    .enablePlugins(PlayScala)
    .disablePlugins(PlayLayoutPlugin)
    .settings(
      scalaVersion := "2.11.8",
      libraryDependencies += "com.typesafe.play" %% "twirl-api" % "1.2.0",
      scalaJSProjects := Seq(client),
      pipelineStages in Assets := Seq(scalaJSPipeline),
      pipelineStages := Seq(digest, gzip)
    )

val play =
  project.in(file("."))
    .aggregate(client, server)
