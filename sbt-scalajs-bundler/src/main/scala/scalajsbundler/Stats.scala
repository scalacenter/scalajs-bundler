
package scalajsbundler

import java.time.LocalDateTime

import play.api.libs.json._
import play.api.libs.functional.syntax._
import sbt.Logger

/**
 * Webpack stats model and json parsers
 */
object Stats {
  final case class Asset(name: String, size: Long, emmited: Boolean, chunkNames: List[String])
  final case class WebpackStats(version: String, hash: String, time: Long, errors: List[String], warnings: List[String], assets: List[Asset]) {
    /**
      * Prints to the log an output similar to what webpack pushes to stdout
      */
    def print(log: Logger): Unit = {
      List(s"Version: $version", s"Hash: $hash", s"Time: ${time}ms", s"Built at ${LocalDateTime.now}").foreach(x => log.info(x))
      errors.foreach(x => log.error(x))
      // Filtering is a workaround for #111
      warnings.filterNot(_.contains("https://raw.githubusercontent.com")).foreach(x => log.warn(x))
      log.warn(warnings.length.toString)
    }

    /**
      * Attempts to find the name of the asset for the project name
      * Note that we only search on files ending on .js skipping e.g. map files
      */
    def assetName(project: String): Option[String] =
      assets.find(a => a.chunkNames.contains(project) && a.name.endsWith(".js")).map(_.name)
  }

  implicit val assetsReads: Reads[Asset] = (
    (JsPath \ "name").read[String] and
    (JsPath \ "size").read[Long] and
    (JsPath \ "emitted").read[Boolean] and
    (JsPath \\ "chunkNames").read[List[String]]
  )(Asset.apply _)

  implicit val statsReads: Reads[WebpackStats] = (
    (JsPath \ "version").read[String] and
    (JsPath \ "hash").read[String] and
    (JsPath \ "time").read[Long] and
    (JsPath \ "errors").read[List[String]] and
    (JsPath \ "warnings").read[List[String]] and
    (JsPath \ "assets").read[List[Asset]]
    )(WebpackStats.apply _)

}
