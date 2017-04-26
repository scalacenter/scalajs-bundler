package example

import scala.scalajs.js
import scala.scalajs.js.JSApp
import leaflet.modules._

object Main extends JSApp {
  def main(): Unit = {
    LeafletAssets

    val map = Leaflet.map("container").setView(js.Array(51.505f, -0.09f), 13)
    val tileLayer = Leaflet.tileLayer("http://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png")
    map.addLayer(tileLayer)
  }
}
