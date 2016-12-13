package snabbdom

import org.scalajs.dom.{Element, Text}

import scala.scalajs.js
import scala.scalajs.js.annotation.JSImport
import scala.scalajs.js.annotation.JSImport.Namespace
import scala.scalajs.js.|

@JSImport("snabbdom", Namespace)
@js.native
object snabbdom extends js.Object {
  def init(modules: js.Array[js.Object]): js.Function2[VNode | Element, VNode, VNode] = js.native
}

@JSImport("snabbdom/h", Namespace)
@js.native
object h extends js.Function3[String, js.UndefOr[js.Any], js.UndefOr[js.Any], VNode] {
  def apply(selector: String, b: js.UndefOr[js.Any] = js.undefined, c: js.UndefOr[js.Any] = js.undefined): VNode = js.native
}

@js.native
class VNode(
  selector: js.UndefOr[String],
  data: js.UndefOr[VNodeData],
  children: js.UndefOr[js.Array[VNode | String]],
  text: js.UndefOr[String],
  elm: js.UndefOr[Element | Text],
  key: js.UndefOr[String | Double]
) extends js.Object

@js.native
class VNodeData extends js.Object


