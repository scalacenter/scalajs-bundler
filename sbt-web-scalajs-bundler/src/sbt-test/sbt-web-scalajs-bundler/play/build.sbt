val client =
  project.in(file("client"))
    .enablePlugins(ScalaJSBundlerPlugin)
    .settings(
      // Required to solve https://github.com/scala/bug/issues/11955
      scalaVersion := "2.13.4",
      scalaJSUseMainModuleInitializer := true,
      libraryDependencies += "org.scala-js" %%% "scalajs-dom" % "1.0.0",
      npmDependencies in Compile ++= Seq(
        "snabbdom" -> "0.5.3",
        "font-awesome" -> "4.7.0",
        "url-loader" -> "0.5.9"
      ),
      ivyLoggingLevel := UpdateLogging.Quiet
    )

val server =
  project.in(file("server"))
    .enablePlugins(PlayScala, WebScalaJSBundlerPlugin)
    .disablePlugins(PlayLayoutPlugin)
    .settings(
      // Required to solve https://github.com/scala/bug/issues/11955
      scalaVersion := "2.13.4",
      libraryDependencies ++= Seq(
        "com.vmunier" %% "scalajs-scripts" % "1.1.4",
        "org.scalatestplus.play" %% "scalatestplus-play" % "5.1.0" % Test
      ),
      scalaJSProjects := Seq(client),
      pipelineStages in Assets := Seq(scalaJSPipeline),
      pipelineStages := Seq(digest, gzip),
      // Expose as sbt-web assets some files retrieved from the NPM packages of the `client` project
      npmAssets ++= NpmAssets.ofProject(client) { modules => (modules / "font-awesome").allPaths }.value,
      ivyLoggingLevel := UpdateLogging.Quiet
    )

val play =
  project.in(file("."))
    .aggregate(client, server)
    .settings(
      ivyLoggingLevel := UpdateLogging.Quiet
    )
