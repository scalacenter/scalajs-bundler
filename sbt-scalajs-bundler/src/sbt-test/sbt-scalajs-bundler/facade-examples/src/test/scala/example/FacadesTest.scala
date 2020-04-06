package example

import org.junit.Assert._
import org.junit.Test

class FacadesTest {

  @Test def testObj(): Unit = {
    assertEquals(42, Obj.bar(41))
  }

  @Test def testFunc(): Unit = {
    assertEquals("Konrad says 2 plus 5 is 7", Func(2, 5))
  }

  @Test def testMember(): Unit = {
    assertEquals(42, Member(41))
  }

  @Test def testUser(): Unit = {
    val user = new User("Julien", 30)
    assertEquals("Julien", user.name)
    assertEquals(30, user.age)
  }

}
