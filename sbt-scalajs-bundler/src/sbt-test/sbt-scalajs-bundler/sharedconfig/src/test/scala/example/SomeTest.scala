package example

import org.scalatest.FreeSpec
import org.scalajs.dom.document

import scala.scalajs.js
import leaflet.modules._

class SomeTest extends FreeSpec {

  "leaflet" - {
    "should return a zoomlevel" in {
      val container = document.body.innerHTML = """<div id="container" />"""

      val map = Leaflet.map("container").setView(js.Array(51.505f, -0.09f), 13)
      val zoomlevel = map.getZoom()
      assert(zoomlevel == 13)
    }
  }

}
