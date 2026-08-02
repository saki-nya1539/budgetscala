package budgetscala.test

import scala.reflect.ClassTag

/** A small hand-rolled test harness — no external test framework/dependency
  * (no ScalaTest, no MUnit) — consistent with the same self-written-harness
  * pattern used across every other language in this portfolio. Each named
  * test runs independently; a failing test prints its message and is
  * counted, but does not stop the rest of the run.
  */
class TestHarness {
  private var total = 0
  private var failed = 0

  def run(name: String)(block: => Unit): Unit = {
    total += 1
    try {
      block
      println(s"  OK   $name")
    } catch {
      case e: Throwable =>
        failed += 1
        println(s"  FAIL $name")
        println(s"       ${e.getMessage}")
    }
  }

  def assertTrue(condition: Boolean, message: String = "expected true but was false"): Unit =
    if (!condition) throw new AssertionError(message)

  def assertEquals[T](expected: T, actual: T): Unit =
    if (expected != actual) throw new AssertionError(s"expected $expected but was $actual")

  /** Asserts that `block` throws an instance of `E`, and nothing else. */
  def assertThrows[E <: Throwable](block: => Unit)(implicit tag: ClassTag[E]): Unit = {
    try {
      block
      throw new AssertionError(s"expected ${tag.runtimeClass.getSimpleName} to be thrown, but nothing was")
    } catch {
      case e: AssertionError => throw e
      case e: Throwable =>
        if (!tag.runtimeClass.isInstance(e))
          throw new AssertionError(
            s"expected ${tag.runtimeClass.getSimpleName} but got ${e.getClass.getSimpleName}: ${e.getMessage}"
          )
    }
  }

  /** Prints a summary line and returns whether every test passed. */
  def summary(): Boolean = {
    println(s"\n${total}件中 ${total - failed}件成功, ${failed}件失敗")
    failed == 0
  }
}
