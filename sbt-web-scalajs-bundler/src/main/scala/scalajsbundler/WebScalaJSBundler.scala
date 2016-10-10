package scalajsbundler

import sbt._
import webscalajs.WebScalaJS

/**
  * If WebScalaJS is enabled, tweak the pipelineStage so that the bundle is produced
  * as an sbt-web asset.
  */
object WebScalaJSBundler extends AutoPlugin {

  override lazy val requires = WebScalaJS

  override lazy val trigger = allRequirements

  override lazy val projectSettings = WebScalaJSBundlerInternal.projectSettings

}
