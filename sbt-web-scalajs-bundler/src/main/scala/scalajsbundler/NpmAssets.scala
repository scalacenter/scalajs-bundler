package scalajsbundler

import com.typesafe.sbt.web.PathMapping
import org.scalajs.sbtplugin.ScalaJSPlugin.AutoImport.fastOptJS
import sbt.Keys.crossTarget
import sbt._

import scalajsbundler.ScalaJSBundlerPlugin.autoImport.npmUpdate

object NpmAssets {

  /**
    * {{{
    *   npmAssets ++= NpmAssets.ofProject(client) { _ / "some-package" / "some-relevant" ** "files" }.value
    * }}}
    *
    * @return A task producing a sequence of files paired with their path relative to the `node_modules` directory
    *         of `project`.
    * @param project The project that depends on the NPM package containing the assets to publish.
    * @param assets A function that finds files given the `node_modules` directory of `project`
    */
  def ofProject(project: ProjectReference)(assets: File => PathFinder): Def.Initialize[Task[Seq[PathMapping]]] =
    Def.task {
      val nodeModules = (crossTarget in project).value / "node_modules" // HACK should be (npmUpdate in project).value
      assets(nodeModules).pair(relativeTo(nodeModules))
    }.dependsOn(npmUpdate in (project, Compile, fastOptJS))

}
