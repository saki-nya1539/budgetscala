package budgetscala.test

object TestRunner {
  def main(args: Array[String]): Unit = {
    val h = new TestHarness()
    LedgerTest.run(h)
    val allPassed = h.summary()
    if (!allPassed) sys.exit(1)
  }
}
