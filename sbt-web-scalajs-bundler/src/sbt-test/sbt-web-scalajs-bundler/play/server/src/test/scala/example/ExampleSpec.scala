package example

import org.scalatestplus.play.guice.GuiceOneServerPerSuite
import org.scalatestplus.play.{HtmlUnitFactory, OneBrowserPerSuite, PlaySpec}
import play.api.ApplicationLoader.Context
import play.api.Environment

class ExampleSpec extends PlaySpec with GuiceOneServerPerSuite with OneBrowserPerSuite with HtmlUnitFactory {

  implicit override lazy val app =
    (new Loader).load(Context.create(Environment.simple()))

  "The application" must {
    "load assets imported from NPM modules" in {
      go to s"http://localhost:$port/assets/font-awesome/css/font-awesome.min.css"
      pageSource must include("Font Awesome")
    }
  }

}
