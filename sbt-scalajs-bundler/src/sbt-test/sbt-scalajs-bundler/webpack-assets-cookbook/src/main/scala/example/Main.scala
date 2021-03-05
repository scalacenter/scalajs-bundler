package example

import com.github.ahnfelt.react4s._

case class HelloComponent() extends Component[NoEmit] {

  override def render(get: Get): Node = E.div(
    E.h1(Text("Hello World !"))
  )
}

object Main {

  def main(args: Array[String]) : Unit = {
    val main = Component(HelloComponent)
    ReactBridge.renderToDomById(main, "content")
  }

}
