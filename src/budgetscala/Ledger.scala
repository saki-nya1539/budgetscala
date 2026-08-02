package budgetscala

import java.time.LocalDate

class ValidationException(message: String) extends Exception(message)
class DuplicateCategoryException(name: String) extends Exception(s"Category already exists: $name")
class CategoryNotFoundException(name: String) extends Exception(s"Category not found: $name")
class TransactionNotFoundException(id: String) extends Exception(s"Transaction not found: $id")

/** In-memory personal budget ledger: categories, transactions, and the
  * aggregate figures shown on the dashboard.
  *
  * This is deliberately not thread-safe internally; [[BudgetHandler]]
  * serializes access to a single shared `Ledger` instance itself (see its
  * documentation) since `com.sun.net.httpserver.HttpServer` dispatches
  * requests from a small thread pool.
  */
class Ledger {
  private var transactions: List[Transaction] = List.empty
  private var categories: List[String] = List("食費", "交通費", "娯楽", "給与", "その他")
  private var nextId: Int = 1

  def allCategories: List[String] = categories

  def addCategory(name: String): String = {
    val trimmed = name.trim
    if (trimmed.isEmpty) throw new ValidationException("category name must not be empty")
    if (categories.contains(trimmed)) throw new DuplicateCategoryException(trimmed)
    categories = categories :+ trimmed
    trimmed
  }

  def allTransactions: List[Transaction] =
    // Sort on `toEpochDay` (a Long) rather than `LocalDate` directly: LocalDate
    // implements `Comparable[ChronoLocalDate]` rather than `Comparable[LocalDate]`,
    // so Scala cannot always derive an implicit `Ordering[LocalDate]` for it.
    transactions.sortBy(t => (t.date.toEpochDay, t.id))(Ordering[(Long, String)].reverse)

  def addTransaction(
      date: LocalDate,
      transactionType: TransactionType,
      amount: BigDecimal,
      category: String,
      memo: String = ""
  ): Transaction = {
    if (amount <= 0) throw new ValidationException("amount must be a positive number")
    val trimmedCategory = category.trim
    if (!categories.contains(trimmedCategory)) throw new CategoryNotFoundException(trimmedCategory)
    val tx = Transaction(s"tx-$nextId", date, transactionType, amount, trimmedCategory, memo.trim)
    nextId += 1
    transactions = transactions :+ tx
    tx
  }

  def deleteTransaction(id: String): Unit = {
    if (!transactions.exists(_.id == id)) throw new TransactionNotFoundException(id)
    transactions = transactions.filterNot(_.id == id)
  }

  def totalIncome: BigDecimal =
    transactions.filter(_.transactionType == TransactionType.Income).map(_.amount).sum

  def totalExpense: BigDecimal =
    transactions.filter(_.transactionType == TransactionType.Expense).map(_.amount).sum

  def balance: BigDecimal = totalIncome - totalExpense

  /** Total expenses per category, sorted from largest to smallest. Categories
    * with no expenses are omitted.
    */
  def expenseByCategory: List[(String, BigDecimal)] =
    transactions
      .filter(_.transactionType == TransactionType.Expense)
      .groupBy(_.category)
      .view
      .mapValues(_.map(_.amount).sum)
      .toList
      .sortBy(-_._2)
}
