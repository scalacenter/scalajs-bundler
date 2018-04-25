package example

import org.scalatest.FreeSpec

class FacadesTest extends FreeSpec {

  "Obj" - {

    "has a bar method" in {
      assert(Obj.bar(41) == 42)
    }

  }

  "Func" - {

    "acts as a function" in {
      assert(Func(2, 5) == "Konrad says 2 plus 5 is 7")
    }

  }

  "Member" - {

    "can be called as a function" in {
      assert(Member(41) == 42)
    }

  }

  "User" - {

    "can be constructed and fields can be accessed" in {
      val user = new User("Julien", 30)
      assert(user.name == "Julien" && user.age == 30)
    }

  }

}
