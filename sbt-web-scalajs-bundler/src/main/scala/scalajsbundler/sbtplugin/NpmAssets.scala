package scalajsbundler.sbtplugin

import com.typesafe.sbt.web.PathMapping
import sbt._

import scalajsbundler.sbtplugin.ScalaJSBundlerPlugin.autoImport.npmUpdate

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
      val nodeModules = (npmUpdate in (project, Compile)).value / "node_modules"
      assets(nodeModules).pair(Path.relativeTo(nodeModules))
    }

}
