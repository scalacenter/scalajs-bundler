package scalajsbundler

import play.api.libs.json.{Json, Reads}
import sbt._

import scala.util.Try

case class NpmPackage(version: String) {
  def major: Option[Int] = {
    val r = """^(\d+)(\..*|)$""".r
    version match {
      case r(v, _) => Try(v.toInt).toOption
      case _ => None
    }
  }
}

object NpmPackage {
  implicit val npmPackageDeserializer: Reads[NpmPackage] = Json.reads[NpmPackage]

  def getForModule(targetDir: File, module: String): Option[NpmPackage] = {
    val webpackPackageJsonFilePath = targetDir / "node_modules" / module / "package.json"

    Try(Json.parse(IO.read(webpackPackageJsonFilePath)).as[NpmPackage]).toOption
  }
}