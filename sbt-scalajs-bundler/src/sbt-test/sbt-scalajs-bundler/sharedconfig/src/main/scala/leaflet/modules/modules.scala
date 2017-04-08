package leaflet
package modules

import scala.scalajs.js
import scala.scalajs.js.annotation.JSImport
import scala.scalajs.js.annotation.JSImport.Namespace

@js.native
trait Map extends js.Object {
  def setView(center: js.Array[Float], zoom: Int): Map = js.native

  def getZoom(): Int = js.native

  def addLayer(layer: Layer): js.Dynamic = js.native
}

@js.native
trait Layer extends js.Object {
  def addTo(map: Map): js.Dynamic = js.native
}

@JSImport("leaflet", JSImport.Namespace)
@js.native
object Leaflet extends js.Object {

  def map(elem: String): Map = js.native

  def tileLayer(url: String): Layer = js.native
}

@JSImport("!style-loader!css-loader!leaflet/dist/leaflet.css", JSImport.Default )
@js.native
object LeafletAssets extends js.Object {}
