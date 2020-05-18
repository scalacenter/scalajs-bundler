package scalajsbundler.util

import java.io.File
import scala.collection.immutable.ListSet

object CachedBundleFiles {
  def cached(file: File, assets: List[File]): ListSet[File] = {
    // Let's put the main file first
    val sortedAssets = file :: assets.filterNot(_ == file)
    // use list set to preserve the order
    ListSet(sortedAssets: _*)
  }
}
