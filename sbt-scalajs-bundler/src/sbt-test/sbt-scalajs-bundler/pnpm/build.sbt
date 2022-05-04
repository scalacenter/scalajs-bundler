import scalajsbundler.PackageManager
import scalajsbundler.util.JSON

scalaVersion := "2.12.8"

jsPackageManager := new PackageManager.ExternalProcess("pnpm", "install", Seq.empty)
  with PackageManager.LockFileSupport
  with PackageManager.AddPackagesSupport {
    val lockFileName: String = "pnpm-lock.yaml"
    val addPackagesCommand: String = "add"
    val addPackagesArgs: Seq[String] = Seq.empty
    val packageJsonContents: Map[String, JSON] = Map.empty
  }

scalaJSUseMainModuleInitializer := true

npmDependencies in Compile += "neat" -> "2.1.0"

enablePlugins(ScalaJSBundlerPlugin)

ivyLoggingLevel in ThisBuild := UpdateLogging.Quiet
