package scalajsbundler

import java.io.{BufferedInputStream, FileInputStream}
import java.util.zip.ZipInputStream

import play.api.libs.functional.syntax._
import play.api.libs.json.{Format, JsPath, Json, OFormat}
import sbt._
import scalajsbundler.NpmDependencies.Dependencies

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

  implicit val serializer: OFormat[NpmDependencies] = (
    (JsPath \ "compile-dependencies").format[Dependencies] and
    (JsPath \ "test-dependencies").format[Dependencies] and
    (JsPath \ "compile-devDependencies").format[Dependencies] and
    (JsPath \ "test-devDependencies").format[Dependencies]
  )(NpmDependencies.apply, Function.unlift(NpmDependencies.unapply))

  implicit def tuple2Serializer[A](implicit aSerializer: Format[A]): Format[(String, A)] =
    implicitly[Format[Map[String, A]]].inmap(_.head, Map(_))

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
                .map(_ => Json.parse(IO.readStream(stream)).as[NpmDependencies])
                .to[Seq]
            } finally {
              stream.close()
            }
          } else if (cpEntry.isDirectory) {
            for {
              (file, _) <- Path.selectSubpaths(cpEntry, new ExactFilter(NpmDependencies.manifestFileName))
            } yield {
              Json.parse(IO.read(file)).as[NpmDependencies]
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
    IO.write(manifestFile, Json.stringify(Json.toJson(npmDependencies)))
    manifestFile
  }

}