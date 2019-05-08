import java.util.zip.ZipFile

import com.typesafe.sbt.packager.SettingsHelper._

name := "webpack-assets"

scalaVersion := "2.12.6"

scalaJSUseMainModuleInitializer := true

resolvers += Resolver.sonatypeRepo("snapshots")

npmDependencies.in(Compile) := Seq(
  "react" -> "16.4.2",
  "react-dom" -> "16.4.2"
)

webpackBundlingMode := scalajsbundler.BundlingMode.LibraryAndApplication()

libraryDependencies ++= Seq(
  "com.github.ahnfelt" %%% "react4s" % "0.9.15-SNAPSHOT"
)

//#scalajs-files
// Use ScalaJs and the sbt native packager
enablePlugins(ScalaJSBundlerPlugin, UniversalPlugin, UniversalDeployPlugin)

// All files shall go directly into the archive rather than having a top level directory matching
// the module name
topLevelDirectory := None

// Map all assets produced by the ScalaJs Bundler to their location within the archive
mappings.in(Universal) ++= webpack.in(Compile, fullOptJS).value.map { f =>
  f.data -> s"assets/${f.data.getName()}"
}
//#scalajs-files

//#additional-files
// Add any other required files to the archive
mappings.in(Universal) ++= Seq(
  target.value / ("scala-" + scalaBinaryVersion.value) / "scalajs-bundler" / "main" / "node_modules" / "react" / "umd" / "react.production.min.js" -> "assets/react.production.min.js",
  target.value / ("scala-" + scalaBinaryVersion.value) / "scalajs-bundler" / "main" / "node_modules" / "react-dom" / "umd" / "react-dom.production.min.js" -> "assets/react-dom.production.min.js"
)
//#additional-files

makeDeploymentSettings(Universal, packageBin.in(Universal), "zip")

TaskKey[Unit]("checkArchive") := {

  val expected : List[String] = List(
    "index.html",
    "assets/webpack-assets-opt-bundle.js",
    "assets/webpack-assets-opt-loader.js",
    "assets/webpack-assets-opt-library.js",
    "assets/webpack-assets-opt-library.js.map",
    "assets/webpack-assets-opt.js",
    "assets/react.production.min.js",
    "assets/react-dom.production.min.js"
  )

  val archive = packageBin.in(Universal).value
  assert(archive.exists() && archive.isFile())

  val entries = new ZipHelper(archive).entries

  assert(expected.size == entries.size)
  assert(expected.forall(e => entries.contains(e)))
}

ivyLoggingLevel in ThisBuild := UpdateLogging.Quiet
