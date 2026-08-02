package budgetscala.test

import budgetscala._
import java.time.LocalDate

object LedgerTest {
  def run(h: TestHarness): Unit = {
    h.run("addTransaction rejects zero or negative amounts") {
      val ledger = new Ledger()
      h.assertThrows[ValidationException] {
        ledger.addTransaction(LocalDate.now(), TransactionType.Expense, BigDecimal(0), "食費")
      }
      h.assertThrows[ValidationException] {
        ledger.addTransaction(LocalDate.now(), TransactionType.Expense, BigDecimal(-500), "食費")
      }
    }

    h.run("addTransaction rejects an unknown category") {
      val ledger = new Ledger()
      h.assertThrows[CategoryNotFoundException] {
        ledger.addTransaction(LocalDate.now(), TransactionType.Expense, BigDecimal(1000), "存在しないカテゴリ")
      }
    }

    h.run("addCategory rejects blank and duplicate names") {
      val ledger = new Ledger()
      h.assertThrows[ValidationException] {
        ledger.addCategory("   ")
      }
      h.assertThrows[DuplicateCategoryException] {
        ledger.addCategory("食費")
      }
      ledger.addCategory("書籍")
      h.assertTrue(ledger.allCategories.contains("書籍"))
    }

    h.run("totalIncome, totalExpense, and balance are computed correctly") {
      val ledger = new Ledger()
      ledger.addTransaction(LocalDate.now(), TransactionType.Income, BigDecimal(300000), "給与")
      ledger.addTransaction(LocalDate.now(), TransactionType.Expense, BigDecimal(50000), "食費")
      ledger.addTransaction(LocalDate.now(), TransactionType.Expense, BigDecimal(20000), "交通費")

      h.assertEquals(BigDecimal(300000), ledger.totalIncome)
      h.assertEquals(BigDecimal(70000), ledger.totalExpense)
      h.assertEquals(BigDecimal(230000), ledger.balance)
    }

    h.run("expenseByCategory groups and sorts by amount descending") {
      val ledger = new Ledger()
      ledger.addTransaction(LocalDate.now(), TransactionType.Expense, BigDecimal(3000), "食費")
      ledger.addTransaction(LocalDate.now(), TransactionType.Expense, BigDecimal(7000), "食費")
      ledger.addTransaction(LocalDate.now(), TransactionType.Expense, BigDecimal(5000), "娯楽")
      ledger.addTransaction(LocalDate.now(), TransactionType.Income, BigDecimal(100000), "給与")

      val breakdown = ledger.expenseByCategory
      h.assertEquals(2, breakdown.length)
      h.assertEquals("食費", breakdown.head._1)
      h.assertEquals(BigDecimal(10000), breakdown.head._2)
      h.assertEquals("娯楽", breakdown(1)._1)
    }

    h.run("deleteTransaction removes it and rejects an unknown id") {
      val ledger = new Ledger()
      val tx = ledger.addTransaction(LocalDate.now(), TransactionType.Expense, BigDecimal(1000), "食費")
      h.assertTrue(ledger.allTransactions.exists(_.id == tx.id))

      ledger.deleteTransaction(tx.id)
      h.assertTrue(!ledger.allTransactions.exists(_.id == tx.id))

      h.assertThrows[TransactionNotFoundException] {
        ledger.deleteTransaction(tx.id)
      }
    }

    h.run("allTransactions is sorted newest first") {
      val ledger = new Ledger()
      val today = LocalDate.now()
      val older = ledger.addTransaction(today.minusDays(5), TransactionType.Expense, BigDecimal(1000), "食費")
      val newer = ledger.addTransaction(today, TransactionType.Expense, BigDecimal(2000), "食費")

      val all = ledger.allTransactions
      h.assertEquals(newer.id, all.head.id)
      h.assertEquals(older.id, all(1).id)
    }

    h.run("addTransaction trims memo and category whitespace") {
      val ledger = new Ledger()
      val tx = ledger.addTransaction(LocalDate.now(), TransactionType.Expense, BigDecimal(1000), "  食費  ", "  ラーメン  ")
      h.assertEquals("食費", tx.category)
      h.assertEquals("ラーメン", tx.memo)
    }
  }
}
