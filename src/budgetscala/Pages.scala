package budgetscala

import java.time.LocalDate
import java.time.format.DateTimeFormatter

/** Renders the 4 pages of the app as full HTML documents (via [[Html.page]]).
  * [[BudgetHandler]] is the only caller; keeping rendering separate from
  * routing/HTTP concerns makes each page easy to reason about on its own.
  */
object Pages {

  private val dateFmt = DateTimeFormatter.ISO_LOCAL_DATE

  /** Formats a money amount with thousands separators, e.g. `45000` ->
    * `"45,000"`. Implemented as plain string manipulation rather than via
    * `f"...%,d"` so it doesn't depend on how the `f` string interpolator's
    * compile-time format checking treats `BigInt` (which is not one of the
    * numeric types it's guaranteed to accept for a `%d` conversion).
    */
  private def formatAmount(amount: BigDecimal): String = {
    val whole = amount.setScale(0, BigDecimal.RoundingMode.HALF_UP).toBigInt
    val sign = if (whole.signum < 0) "-" else ""
    val digits = whole.abs.toString
    val grouped = digits.reverse.grouped(3).mkString(",").reverse
    sign + grouped
  }

  private def errorBlock(errorMessage: Option[String]): String =
    errorMessage.map(m => s"""<div class="error">${Html.escape(m)}</div>""").getOrElse("")

  /** Page 1 of 4: totals and a simple bar breakdown of expenses by category. */
  def dashboard(ledger: Ledger): String = {
    val income = ledger.totalIncome
    val expense = ledger.totalExpense
    val balance = ledger.balance
    val breakdown = ledger.expenseByCategory
    val maxAmount = if (breakdown.isEmpty) BigDecimal(1) else breakdown.map(_._2).max

    val bars = breakdown
      .map { case (category, amount) =>
        val pct = if (maxAmount == 0) 0 else (amount / maxAmount * 100).toInt
        s"""<div class="bar-row">
           |  <div style="width:90px">${Html.escape(category)}</div>
           |  <div class="bar-track"><div class="bar-fill" style="width:${pct}%"></div></div>
           |  <div style="width:90px;text-align:right">¥${formatAmount(amount)}</div>
           |</div>""".stripMargin
      }
      .mkString("\n")

    val breakdownSection = if (breakdown.isEmpty) "<p>まだ支出の記録がありません。</p>" else bars

    val body =
      s"""<h1>ダッシュボード</h1>
         |<div class="summary-grid">
         |  <div class="summary-card"><div class="label">収入合計</div><div class="value income">¥${formatAmount(income)}</div></div>
         |  <div class="summary-card"><div class="label">支出合計</div><div class="value expense">¥${formatAmount(expense)}</div></div>
         |  <div class="summary-card"><div class="label">残高</div><div class="value">¥${formatAmount(balance)}</div></div>
         |</div>
         |<h2>カテゴリ別支出</h2>
         |$breakdownSection
         |""".stripMargin

    Html.page("ダッシュボード", "/", body)
  }

  /** Page 2 of 4: every transaction, newest first, with a delete action. */
  def transactions(ledger: Ledger, errorMessage: Option[String] = None): String = {
    val rows = ledger.allTransactions
      .map { t =>
        val (typeLabel, sign, cls) = t.transactionType match {
          case TransactionType.Income  => ("収入", "", "income")
          case TransactionType.Expense => ("支出", "-", "expense")
        }
        s"""<tr>
           |  <td>${t.date}</td>
           |  <td><span class="$cls">$typeLabel</span></td>
           |  <td>${Html.escape(t.category)}</td>
           |  <td class="$cls">$sign¥${formatAmount(t.amount)}</td>
           |  <td>${Html.escape(t.memo)}</td>
           |  <td>
           |    <form class="inline" method="post" action="/transactions/${t.id}/delete" onsubmit="return confirm('削除しますか？');">
           |      <button type="submit" class="danger">削除</button>
           |    </form>
           |  </td>
           |</tr>""".stripMargin
      }
      .mkString("\n")

    val tableOrEmpty =
      if (ledger.allTransactions.isEmpty) "<p>まだ取引がありません。</p>"
      else
        s"""<table>
           |<thead><tr><th>日付</th><th>種別</th><th>カテゴリ</th><th>金額</th><th>メモ</th><th></th></tr></thead>
           |<tbody>
           |$rows
           |</tbody>
           |</table>""".stripMargin

    val body =
      s"""<h1>取引一覧</h1>
         |${errorBlock(errorMessage)}
         |$tableOrEmpty
         |""".stripMargin

    Html.page("取引一覧", "/transactions", body)
  }

  /** Page 3 of 4: the "add transaction" form. */
  def newTransactionForm(ledger: Ledger, errorMessage: Option[String] = None): String = {
    val categoryOptions = ledger.allCategories
      .map(c => s"""<option value="${Html.escape(c)}">${Html.escape(c)}</option>""")
      .mkString("\n")
    val today = LocalDate.now().format(dateFmt)

    val body =
      s"""<h1>取引を追加</h1>
         |${errorBlock(errorMessage)}
         |<form class="card" method="post" action="/transactions/new">
         |  <label>日付<br><input type="date" name="date" value="$today" required></label>
         |  <label>種別<br>
         |    <select name="type">
         |      <option value="Expense">支出</option>
         |      <option value="Income">収入</option>
         |    </select>
         |  </label>
         |  <label>金額(円)<br><input type="number" name="amount" min="1" step="1" required></label>
         |  <label>カテゴリ<br>
         |    <select name="category">
         |$categoryOptions
         |    </select>
         |  </label>
         |  <label>メモ(任意)<br><input type="text" name="memo"></label>
         |  <button type="submit">追加</button>
         |</form>
         |""".stripMargin

    Html.page("取引を追加", "/transactions/new", body)
  }

  /** Page 4 of 4: the category list and "add category" form. */
  def categories(ledger: Ledger, errorMessage: Option[String] = None): String = {
    val items = ledger.allCategories.map(c => s"<li>${Html.escape(c)}</li>").mkString("\n")

    val body =
      s"""<h1>カテゴリ</h1>
         |${errorBlock(errorMessage)}
         |<ul>
         |$items
         |</ul>
         |<h2>カテゴリを追加</h2>
         |<form class="card" method="post" action="/categories">
         |  <label>カテゴリ名<br><input type="text" name="name" required></label>
         |  <button type="submit">追加</button>
         |</form>
         |""".stripMargin

    Html.page("カテゴリ", "/categories", body)
  }
}
