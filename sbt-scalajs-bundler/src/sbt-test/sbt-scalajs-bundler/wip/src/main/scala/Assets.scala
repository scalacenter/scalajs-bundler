import scala.scalajs.js
import js.annotation._

@JSImport("foobar/dotty.svg", JSImport.Default)
@js.native
object DottySvg extends js.Any

object Assets {
  def dottyLogoUrl: String = DottySvg.asInstanceOf[String]
}

object Main extends js.JSApp {
  override def main(): Unit = {
    println(Assets.dottyLogoUrl)
  }
}