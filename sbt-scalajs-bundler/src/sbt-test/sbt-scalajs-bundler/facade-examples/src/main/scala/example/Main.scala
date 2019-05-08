package example

object Main {

  def main(args: Array[String]): Unit = {
    println(Obj.bar(42))
    println(Member(42))
    val user = new User("Julien", 30)
    println(user.name)
    println(Func(8, 12))
  }

}