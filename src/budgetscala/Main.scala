package budgetscala

import com.sun.net.httpserver.HttpServer
import java.net.InetSocketAddress
import java.time.LocalDate

object Main {
  def main(args: Array[String]): Unit = {
    val port = args.headOption.flatMap(_.toIntOption).getOrElse(8080)

    val ledger = new Ledger()
    seedSampleData(ledger)

    val server = HttpServer.create(new InetSocketAddress(port), 0)
    server.createContext("/", new BudgetHandler(ledger))
    server.setExecutor(null) // use the JDK's default single-threaded executor
    server.start()

    println(s"BudgetScala is running at http://localhost:$port/")
    println("Ctrl+C to stop.")
  }

  /** A few sample transactions so the dashboard isn't empty on first run. */
  private def seedSampleData(ledger: Ledger): Unit = {
    val today = LocalDate.now()
    ledger.addTransaction(today.minusDays(20), TransactionType.Income, BigDecimal(200000), "給与", "8月分給与")
    ledger.addTransaction(today.minusDays(15), TransactionType.Expense, BigDecimal(45000), "食費", "スーパー・外食")
    ledger.addTransaction(today.minusDays(10), TransactionType.Expense, BigDecimal(8000), "交通費", "定期券")
    ledger.addTransaction(today.minusDays(3), TransactionType.Expense, BigDecimal(6000), "娯楽", "映画・カフェ")
  }
}
