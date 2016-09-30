val client =
  project.in(file("client"))
    .enablePlugins(ScalaJSPlugin)
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
      // FIXME Integrate with sbt-web-scalajs instead?
      resourceGenerators in Assets += Def.task {
        val bundles = (webpack in (client, Compile, fastOptJS in (client, Compile))).value
        val resources =
          bundles.map(file => (file, (resourceManaged in Assets).value / file.name))
        IO.copy(resources)
        resources.map(_._2)
      }.taskValue,
      pipelineStages := Seq(digest, gzip)
    )

val play =
  project.in(file("."))
    .aggregate(client, server)
