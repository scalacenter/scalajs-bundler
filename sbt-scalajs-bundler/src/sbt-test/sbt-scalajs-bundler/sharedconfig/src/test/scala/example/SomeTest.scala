package example

import org.junit.Assert._
import org.junit.Test

import org.scalajs.dom.document

import scala.scalajs.js
import leaflet.modules._

class SomeTest {

  @Test def leafletZoom(): Unit = {
    val container = document.body.innerHTML = """<div id="container" />"""

    val map = Leaflet.map("container").setView(js.Array(51.505f, -0.09f), 13)
    val zoomlevel = map.getZoom()
    assertEquals(13, zoomlevel)
  }

}
