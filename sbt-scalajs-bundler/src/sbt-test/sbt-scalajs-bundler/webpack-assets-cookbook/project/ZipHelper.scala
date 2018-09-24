import java.io.File
import java.util.zip.ZipFile

import scala.collection.JavaConverters._

class ZipHelper(f: File) {

  val zipFile = new ZipFile(f)

  lazy val entries : List[String] = zipFile.entries().asScala.toList.map { e => e.getName() }

}
