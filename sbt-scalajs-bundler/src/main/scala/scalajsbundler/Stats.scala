
package scalajsbundler

import java.time.LocalDateTime

import play.api.libs.json._
import play.api.libs.functional.syntax._
import sbt.Logger
import scala.math.max

/**
 * Webpack stats model and json parsers
 */
object Stats {

  final case class Asset(name: String, size: Long, emmited: Boolean, chunkNames: List[String])

  object formatting {

    final case class Part(t: String, l: Int) {
      def maxL(p: Part): Part =
        copy(l = max(l, p.l))

      def leftPad: String =
        // String interpolation doesn't support dynamic padding
        t.reverse.padTo(l, " ").reverse.mkString
    }

    object Part {
      def apply(t: String) = new Part(t, t.length)
    }

    final case class AssetLine(asset: Part, size: Part, emitted: Part, chunks: Part) {
      def adjustPadding(p: AssetLine): AssetLine = copy(asset.maxL(p.asset), size.maxL(p.size), emitted.maxL(p.emitted), chunks.maxL(p.chunks))
      def show: String = List(asset, size, emitted, chunks).map(_.leftPad).mkString("   ")
    }

    object AssetLine {
      val Zero: AssetLine = AssetLine(Part("Asset"), Part("Size"), Part(""), Part("Chunks"))
    }

  }

  final case class WebpackStats(version: String, hash: String, time: Long, errors: List[String], warnings: List[String], assets: List[Asset]) {

    /**
      * Prints to the log an output similar to what webpack pushes to stdout
      */
    def print(log: Logger): Unit = {
      import formatting._
      // Print base info
      List(s"Version: $version", s"Hash: $hash", s"Time: ${time}ms", s"Built at ${LocalDateTime.now}").foreach(x => log.info(x))
      log.info("")
      // Print the assets
      assets.map { a =>
        val emitted = if (a.emmited) "[emitted]" else ""
        AssetLine(Part(a.name), Part(a.size.toString), Part(emitted), Part(a.chunkNames.mkString("[", ",", "]")))
      }.foldLeft(List(AssetLine.Zero)) {
        case (lines, curr) =>
          val adj = lines.map(_.adjustPadding(curr))
          val adjNew = adj.headOption.fold(curr)(curr.adjustPadding)
          (adjNew :: adj.reverse).reverse
      }.foreach { l =>
        log.info(l.show)
      }
      log.info("")
      // Filtering is a workaround for #111
      warnings.filterNot(_.contains("https://raw.githubusercontent.com")).foreach(x => log.warn(x))
      errors.foreach(x => log.error(x))
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
