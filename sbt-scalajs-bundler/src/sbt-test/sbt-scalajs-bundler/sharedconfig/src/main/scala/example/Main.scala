package example

import scala.scalajs.js
import leaflet.modules._

object Main {
  def main(args: Array[String]): Unit = {
    LeafletAssets

    val map = Leaflet.map("container").setView(js.Array(51.505f, -0.09f), 13)
    val tileLayer = Leaflet.tileLayer("http://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png")
    map.addLayer(tileLayer)
  }
}
