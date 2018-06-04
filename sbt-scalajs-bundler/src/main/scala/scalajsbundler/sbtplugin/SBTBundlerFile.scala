package scalajsbundler.sbtplugin

import sbt.{AttributeKey, Attributed}

import scalajsbundler.{BundlerFile, BundlerFileType}

/**
  * SBT Specific Extensions to [[scalajsbundler.BundlerFile]]
  *
  * @param f The BundlerFile to enrich
  */
class SBTBundlerFile(f: BundlerFile.Public) {
    import SBTBundlerFile._

    def asAttributedFiles: Seq[sbt.Attributed[sbt.File]] = {
      val main = Attributed
        .blank(f.attributedFiles._1)
        .put(ProjectNameAttr, f.project)
        .put(BundlerFileTypeAttr, f.`type`)
      val assets = Attributed
        .blankSeq(f.attributedFiles._2).map {
          _
          .put(ProjectNameAttr, f.project)
          .put(BundlerFileTypeAttr, BundlerFileType.Asset)
        }
      main +: assets
    }
}

object SBTBundlerFile {

  /**
    * A string attribute describing the project and output stage (fastopt vs. opt)
    */
  val ProjectNameAttr: AttributeKey[String] = AttributeKey("project-name")

  /**
    * A [[scalajsbundler.BundlerFileType]], allowing unambigiuous selection of output files
    * downstream from the plugin.
    */
  val BundlerFileTypeAttr: AttributeKey[BundlerFileType] = AttributeKey(
    "bundler-file-type")
}
