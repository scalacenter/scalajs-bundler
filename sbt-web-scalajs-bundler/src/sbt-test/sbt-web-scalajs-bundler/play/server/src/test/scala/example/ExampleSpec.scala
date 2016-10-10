package example

import org.scalatestplus.play._
import play.api.{ApplicationLoader, Environment}

class ExampleSpec extends PlaySpec with OneServerPerSuite with OneBrowserPerSuite with HtmlUnitFactory {

  implicit override lazy val app =
    (new Loader).load(ApplicationLoader.createContext(Environment.simple()))

  "The application" must {
    "load the page without error" in {
      go to s"http://localhost:$port/"
      find(tagName("p")).value.text mustBe "Hello, world!"
    }
  }

}
