
package scalajsbundler

import play.api.libs.json._
import sbt.Logger
import scala.math.max
import java.io.File
import java.nio.file.Path

/**
 * Webpack stats model and json parsers
 */
object Stats {

  final case class Asset(name: String, size: Long, emitted: Boolean, chunkNames: List[String]) {
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

  sealed trait Issue {
    val message: String
    val moduleName: Option[String]
    val loc: Option[String]
    val stack: Option[String]
    val details: Option[String]

    def format(): String = {
      List(
        moduleName.map("in "+_),
        Some("Message: "+message),
        loc.map("Loc: "+_),
        stack.map("Stack: "+_),
        details.map("Details: "+_)
      ).flatten.mkString("\n")
    }
  }

  final case class WebpackError(
    message: String,
    moduleName: Option[String],
    loc: Option[String],
    stack: Option[String],
    details: Option[String]
  ) extends Issue

  final case class WebpackWarning(
    message: String,
    moduleName: Option[String],
    loc: Option[String],
    stack: Option[String],
    details: Option[String]
  ) extends Issue

  final case class WebpackStats(
    version: String,
    hash: String,
    time: Option[Long],
    outputPath: Option[Path],
    errors: List[WebpackError] = Nil,
    warnings: List[WebpackWarning] = Nil,
    assets: List[Asset] = Nil
  ) {

    /**
      * Prints to the log an output similar to what webpack pushes to stdout
      */
    def print(log: Logger): Unit = {
      import formatting._
      // Print base info
      List(
        Some(s"Version: $version"),
        Some(s"Hash: $hash"),
        time.map(time => s"Time: ${time}ms"),
        outputPath.map(s"Path: "+_)
      ).flatten.foreach(x => log.info(x))
      log.info("")
      // Print the assets
      assets.map { a =>
        val emitted = if (a.emitted) "[emitted]" else ""
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

  implicit val assetsReads: Reads[Asset] = Json.reads[Asset]
  implicit val errorReads: Reads[WebpackError] = Json.reads[WebpackError]
  implicit val warningReads: Reads[WebpackWarning] = Json.reads[WebpackWarning]
  implicit val pathReads: Reads[Path] = Reads.StringReads.map(new File(_).toPath)
  implicit val statsReads: Reads[WebpackStats] = Json.using[Json.WithDefaultValues].reads[WebpackStats]

}
