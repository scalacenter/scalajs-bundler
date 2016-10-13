package scalajsbundler

import java.io.{BufferedInputStream, FileInputStream}
import java.util.zip.ZipInputStream

import org.scalajs.core.tools.json._
import sbt._

import NpmDependencies.Dependencies

/**
  * NPM dependencies, for each configuration.
  * This information can not be included in the pom.xml, so we
  * serialize for each Scala.js artifact into an additional file
  * in the artifact .jar.
  */
case class NpmDependencies(
  compileDependencies: Dependencies,
  testDependencies: Dependencies,
  compileDevDependencies: Dependencies,
  testDevDependencies: Dependencies
) {
  /** Merge operator */
  def ++ (that: NpmDependencies): NpmDependencies =
    NpmDependencies(
      compileDependencies ++ that.compileDependencies,
      testDependencies ++ that.testDependencies,
      compileDevDependencies ++ that.compileDevDependencies,
      testDevDependencies ++ that.testDevDependencies
    )
}

object NpmDependencies {

  /** Name of the file containing the NPM dependencies */
  val manifestFileName = "NPM_DEPENDENCIES"

  type Dependencies = List[(String, String)]

  implicit val serializer: JSONSerializer[NpmDependencies] =
    new JSONSerializer[NpmDependencies] {
      def serialize(npmManifest: NpmDependencies): JSON =
        new JSONObjBuilder()
          .fld("compile-dependencies", npmManifest.compileDependencies)
          .fld("test-dependencies", npmManifest.testDependencies)
          .fld("compile-devDependencies", npmManifest.compileDevDependencies)
          .fld("test-devDependencies", npmManifest.testDevDependencies)
          .toJSON
    }

  implicit val deserializer: JSONDeserializer[NpmDependencies] =
    new JSONDeserializer[NpmDependencies] {
      def deserialize(json: JSON): NpmDependencies = {
        val obj = new JSONObjExtractor(json)
        NpmDependencies(
          obj.fld[Dependencies]("compile-dependencies"),
          obj.fld[Dependencies]("test-dependencies"),
          obj.fld[Dependencies]("compile-devDependencies"),
          obj.fld[Dependencies]("test-devDependencies")
        )
      }
    }

  implicit def tuple2Serializer[A](implicit aSerializer: JSONSerializer[A]): JSONSerializer[(String, A)] =
    new JSONSerializer[(String, A)] {
      def serialize(tuple: (String, A)): JSON =
        JSONSerializer.mapJSON[A].serialize(Map(tuple._1 -> tuple._2))
    }

  implicit def tuple2Deserializer[A](implicit aDeserializer: JSONDeserializer[A]): JSONDeserializer[(String, A)] =
    new JSONDeserializer[(String, A)] {
      def deserialize(json: JSON): (String, A) =
        JSONDeserializer.mapJSON[A].deserialize(json).head
    }

  /**
    * @param cp Classpath
    * @return All the NPM dependencies found in the given classpath
    */
  def collectFromClasspath(cp: Def.Classpath): NpmDependencies =
    (
      for {
        cpEntry <- Attributed.data(cp) if cpEntry.exists
        results <-
          if (cpEntry.isFile && cpEntry.name.endsWith(".jar")) {
            val stream = new ZipInputStream(new BufferedInputStream(new FileInputStream(cpEntry)))
            try {
              Iterator.continually(stream.getNextEntry())
                .takeWhile(_ != null)
                .filter(_.getName == NpmDependencies.manifestFileName)
                .map(_ => fromJSON[NpmDependencies](readJSON(IO.readStream(stream))))
                .to[Seq]
            } finally {
              stream.close()
            }
          } else if (cpEntry.isDirectory) {
            for {
              (file, _) <- Path.selectSubpaths(cpEntry, new ExactFilter(NpmDependencies.manifestFileName))
            } yield {
              fromJSON[NpmDependencies](readJSON(IO.read(file)))
            }
          } else sys.error(s"Illegal classpath entry: ${cpEntry.absolutePath}")
      } yield results
    ).fold(NpmDependencies(Nil, Nil, Nil, Nil))(_ ++ _)

  /**
    * Writes the given dependencies into a manifest file
    */
  def writeManifest(
    npmDependencies: NpmDependencies,
    classDirectory: File
  ): File = {
    val manifestFile = classDirectory / manifestFileName
    IO.write(manifestFile, jsonToString(npmDependencies.toJSON))
    manifestFile
  }

}