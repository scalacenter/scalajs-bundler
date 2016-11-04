package uuid

import scala.annotation.meta.field
import scala.scalajs.js
import scala.scalajs.js.annotation.JSImport.Namespace
import scala.scalajs.js.annotation.{JSExport, JSImport}
import scala.scalajs.js.|

@JSImport("node-uuid/uuid", Namespace)
@js.native
object uuid extends js.Object {

  def v1(options: js.UndefOr[UUIDOptions] = js.undefined): String = js.native

  def v1(
    options: UUIDOptions,
    buffer: js.Array[Double],
    offset: js.UndefOr[Double]
  ): js.Array[Double] = js.native

  def v4(options: js.UndefOr[UUIDOptions] = js.undefined): String = js.native

  def v4(
    options: UUIDOptions,
    buffer: js.Array[Double],
    offset: js.UndefOr[Double]
  ): js.Array[Double] = js.native

  def parse(
    id: String,
    buffer: js.UndefOr[js.Array[Double]] = js.undefined,
    offset: js.UndefOr[Double] = js.undefined
  ): js.Array[Double] = js.native

  def unparse(
    buffer: js.Array[Double],
    offset: js.UndefOr[Double] = js.undefined
  ): String = js.native

}

case class UUIDOptions(
  @(JSExport @field) node: js.UndefOr[js.Array[js.Any]] = js.undefined,
  @(JSExport @field) clockseq: js.UndefOr[Double] = js.undefined,
  @(JSExport @field) msecs: js.UndefOr[Double | js.Date] = js.undefined,
  @(JSExport @field) nsecs: js.UndefOr[Double] = js.undefined
)