
package scalajsbundler

import play.api.libs.json._
import play.api.libs.functional.syntax._
import sbt.Logger
import scala.math.max
import java.io.File
import java.nio.file.Path

/**
 * Webpack stats model and json parsers
 */
object Stats {

  final case class Asset(name: String, size: Long, emitted: Option[Boolean], chunkNames: List[String]) {
    def formattedSize: String = {
      val oneKiB = 1024L
      val oneMiB = oneKiB * oneKiB

      if (size < oneKiB) s"$size bytes"
      else if (size < oneMiB) f"${size / oneKiB.toFloat}%1.2f KiB"
      else f"${size / oneMiB.toFloat}%1.2f MiB"
    }
  }

  object formatting {

    final case class Part(t: String, l: Int) {
      def maxL(p: Part): Part =
        copy(l = max(l, p.l))

      def leftPad: String =
        // String interpolation doesn't support dynamic padding
        t.reverse.padTo(l, ' ').reverse.mkString
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

  final case class WebpackStats(version: String, hash: String, time: Long, outputPath: Option[Path], errors: List[String], warnings: List[String], assets: List[Asset]) {

    /**
      * Prints to the log an output similar to what webpack pushes to stdout
      */
    def print(log: Logger): Unit = {
      import formatting._
      // Print base info
      List(s"Version: $version", s"Hash: $hash", s"Time: ${time}ms", s"Path: ${outputPath.getOrElse("<default>")}").foreach(x => log.info(x))
      log.info("")
      // Print the assets
      assets.map { a =>
        val emitted = a.emitted.fold("<unknown>")(a => if (a) "[emitted]" else "")
        AssetLine(Part(a.name), Part(a.formattedSize), Part(emitted), Part(a.chunkNames.mkString("[", ",", "]")))
      }.foldLeft(List(AssetLine.Zero)) {
        case (lines, curr) =>
          val adj = lines.map(_.adjustPadding(curr))
          val adjNew = adj.headOption.fold(curr)(curr.adjustPadding)
          (adjNew :: adj.reverse).reverse
      }.foreach { l =>
        log.info(l.show)
      }
      log.info("")
    }

    /**
      * Attempts to find the name of the asset for the project name
      * Note that we only search on files ending on .js skipping e.g. map files
      */
    def assetName(project: String): Option[String] =
      assets.find(a => a.chunkNames.contains(project) && a.name.endsWith(".js")).map(_.name)

    /**
     * Resolve the asset on the output path or the target dir if unavailable
     */
    def resolveAsset(altDir: Path, asset: String): Option[File] =
      assetName(asset).map(a => outputPath.getOrElse(altDir).resolve(a).toFile)

    /**
     * Resolve alles asset on the output path or the target dir if unavailable
     */
    def resolveAllAssets(altDir: Path): List[File] =
      assets.map(a => outputPath.getOrElse(altDir).resolve(a.name).toFile)
  }

  implicit val assetsReads: Reads[Asset] = (
    (JsPath \ "name").read[String] and
    (JsPath \ "size").read[Long] and
    (JsPath \ "emitted").readNullable[Boolean] and
    (JsPath \\ "chunkNames").read[List[String]]
  )(Asset.apply _)

  implicit val statsReads: Reads[WebpackStats] = (
    (JsPath \ "version").read[String] and
    (JsPath \ "hash").read[String] and
    (JsPath \ "time").read[Long] and
    (JsPath \ "outputPath").readNullable[String].map(x => x.map(new File(_).toPath)) and // It seems webpack 2 doesn't produce outputPath
    (JsPath \ "errors").read[List[String]] and
    (JsPath \ "warnings").read[List[String]] and
    (JsPath \ "assets").read[List[Asset]]
  )(WebpackStats.apply _)

}
