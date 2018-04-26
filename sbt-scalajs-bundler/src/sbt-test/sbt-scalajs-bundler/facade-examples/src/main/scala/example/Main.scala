package example

import scala.scalajs.js.JSApp

object Main extends JSApp {

  def main(): Unit = {
    println(Obj.bar(42))
    println(Member(42))
    val user = new User("Julien", 30)
    println(user.name)
    println(Func(8, 12))
  }

}