scalaVersion := "2.12.8"

lazy val yarnVersion = "1.22.16"

jsPackageManager := scalajsbundler.PackageManager.Yarn().withVersion(Some(yarnVersion))

scalaJSUseMainModuleInitializer := true

npmDependencies in Compile += "neat" -> "2.1.0"

enablePlugins(ScalaJSBundlerPlugin)

ivyLoggingLevel in ThisBuild := UpdateLogging.Quiet

TaskKey[Unit]("check-yarn-version") := {
  import scala.sys.process.Process
  val cmd = sys.props("os.name").toLowerCase match {
    case os if os.contains("win") => "powershell yarn"
    case _                        => "yarn"
  }
  val process = Process(s"$cmd -v", new File("target/scala-2.12/scalajs-bundler/main"))
  val out = (process!!)
  if(out.trim != yarnVersion) sys.error(s"unexpected yarn version: ${out.trim}")
  ()
}
