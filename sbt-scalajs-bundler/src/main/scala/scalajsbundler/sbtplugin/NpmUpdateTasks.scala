package scalajsbundler.sbtplugin

import org.scalajs.core.tools.io.{FileVirtualJSFile, RelativeVirtualFile, VirtualJSFile}
import scalajsbundler.{Npm, Yarn}
import sbt._

object NpmUpdateTasks {
  /**
    * Runs the Npm or Yarn
    * @param targetDir npm Directory
    * @param packageJsonFile Json file containing NPM dependencies
    * @param useYarn Whether to use yarn or npm	
    * @param jsResources A sequence of javascript resources
    * @param stream A sbt TaskStream
    * @return The written npm directory
    */
  def npmUpdate(targetDir: File,
                packageJsonFile: File,
                useYarn: Boolean,
                jsResources: Seq[VirtualJSFile with RelativeVirtualFile],
                streams: Keys.TaskStreams
                ) = {
    val log = streams.log

    val cachedActionFunction =
      FileFunction.cached(
        streams.cacheDirectory / "scalajsbundler-npm-update",
        inStyle = FilesInfo.hash
      ) { _ =>
        log.info("Updating NPM dependencies")
        if (useYarn) {
          Yarn.run("install", "--non-interactive")(targetDir, log)
        } else {
          Npm.run("install")(targetDir, log)
        }
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
