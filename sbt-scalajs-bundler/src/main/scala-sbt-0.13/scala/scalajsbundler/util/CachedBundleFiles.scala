package scalajsbundler.util

import java.io.File
import scala.collection.immutable.ListSet

object CachedBundleFiles {
  /**
   * Returns a "sorted" list of files containing first the main file
   * and then the rest of the assets. While weak this convention lets us
   * work around the design of sbt file caching
   */
  def cached(file: File, assets: List[File]): ListSet[File] = {
    // Let's put the main file first
    val sortedAssets = file :: assets.filterNot(_ == file)
    // use list set to preserve the order. In sbt 0.13 we need to reverse the order
    ListSet(sortedAssets.reverse: _*)
  }
}
