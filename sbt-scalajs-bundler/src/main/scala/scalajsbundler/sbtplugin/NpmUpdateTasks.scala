package scalajsbundler.sbtplugin

import org.scalajs.core.tools.io.{FileVirtualJSFile, RelativeVirtualFile, VirtualJSFile}
import scalajsbundler.ExternalCommand
import sbt._

object NpmUpdateTasks {
  /**
    * Runs the Npm or Yarn
    * @param targetDir npm Directory
    * @param packageJsonFile Json file containing NPM dependencies
    * @param useYarn Whether to use yarn or npm
    * @param jsResources A sequence of javascript resources
    * @param stream A sbt TaskStream
    * @param npmExtraArgs Additional arguments to pass to npm
    * @param yarnExtraArgs Additional arguments to pass to yarn
    * @return The written npm directory
    */
  def npmUpdate(baseDir: File,
                targetDir: File,
                packageJsonFile: File,
                useYarn: Boolean,
                jsResources: Seq[VirtualJSFile with RelativeVirtualFile],
                streams: Keys.TaskStreams,
                npmExtraArgs: Seq[String],
                yarnExtraArgs: Seq[String]
                ) = {
    val log = streams.log

    val cachedActionFunction =
      FileFunction.cached(
        streams.cacheDirectory / "scalajsbundler-npm-update",
        inStyle = FilesInfo.hash
      ) { _ =>
        log.info("Updating NPM dependencies")
        ExternalCommand.install(baseDir, targetDir, useYarn, npmExtraArgs, yarnExtraArgs, log)
        jsResources.foreach { resource =>
          IO.write(targetDir / resource.relativePath, resource.content)
        }
        Set.empty
      }

    cachedActionFunction(Set(packageJsonFile) ++
      jsResources.collect { case f: FileVirtualJSFile =>
        f.file
      }.to[Set])

    targetDir
  }
}
