package example

import org.junit.Assert._
import org.junit.Test

class NewerLinkerTest {

  @Test def newerLinker(): Unit = {
    assertEquals("1.1.0", System.getProperty("java.vm.version"))

    /* Test the fix to https://github.com/scala-js/scala-js/issues/3984, which
     * was shipped in Scala.js 1.0.1.
     */
    def minusZero: Any = -0.0f
    assertSame(classOf[java.lang.Float], minusZero.getClass())
  }

}
