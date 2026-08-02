package budgetscala

import com.sun.net.httpserver.{HttpExchange, HttpHandler}
import java.io.OutputStream
import java.nio.charset.StandardCharsets
import java.time.LocalDate
import java.time.format.DateTimeParseException
import scala.util.control.NonFatal

/** Routes incoming HTTP requests to the 4 pages and their form submissions,
  * and renders responses via [[Pages]].
  *
  * `com.sun.net.httpserver.HttpServer`'s default executor may dispatch
  * requests to more than one worker thread; since [[Ledger]] is a plain
  * mutable class (not internally synchronized), every request is handled
  * inside a `ledger.synchronized` block here rather than making `Ledger`
  * itself thread-safe, to keep the domain model simple and easy to unit
  * test in isolation from any concurrency concerns.
  */
class BudgetHandler(ledger: Ledger) extends HttpHandler {

  private val deletePattern = """^/transactions/([^/]+)/delete$""".r

  override def handle(exchange: HttpExchange): Unit = {
    try {
      ledger.synchronized {
        route(exchange)
      }
    } catch {
      case NonFatal(e) =>
        respond(exchange, 500, s"Internal error: ${e.getMessage}")
    } finally {
      exchange.close()
    }
  }

  private def route(exchange: HttpExchange): Unit = {
    val method = exchange.getRequestMethod
    val path = exchange.getRequestURI.getPath

    (method, path) match {
      case ("GET", "/") =>
        respondHtml(exchange, 200, Pages.dashboard(ledger))

      case ("GET", "/transactions") =>
        respondHtml(exchange, 200, Pages.transactions(ledger))

      case ("GET", "/transactions/new") =>
        respondHtml(exchange, 200, Pages.newTransactionForm(ledger))

      case ("POST", "/transactions/new") =>
        handleCreateTransaction(exchange)

      case ("POST", deletePattern(id)) =>
        handleDeleteTransaction(exchange, id)

      case ("GET", "/categories") =>
        respondHtml(exchange, 200, Pages.categories(ledger))

      case ("POST", "/categories") =>
        handleCreateCategory(exchange)

      case _ =>
        respond(exchange, 404, "Not found")
    }
  }

  private def handleCreateTransaction(exchange: HttpExchange): Unit = {
    val form = Forms.parseFormBody(exchange)
    try {
      val date = LocalDate.parse(form.getOrElse("date", ""))
      val txType = form.getOrElse("type", "Expense") match {
        case "Income" => TransactionType.Income
        case _        => TransactionType.Expense
      }
      val amount = BigDecimal(form.getOrElse("amount", ""))
      val category = form.getOrElse("category", "")
      val memo = form.getOrElse("memo", "")
      ledger.addTransaction(date, txType, amount, category, memo)
      redirect(exchange, "/transactions")
    } catch {
      case e: ValidationException =>
        respondHtml(exchange, 400, Pages.newTransactionForm(ledger, Some(e.getMessage)))
      case e: CategoryNotFoundException =>
        respondHtml(exchange, 400, Pages.newTransactionForm(ledger, Some(e.getMessage)))
      case _: NumberFormatException =>
        respondHtml(exchange, 400, Pages.newTransactionForm(ledger, Some("金額は数値で入力してください")))
      case _: DateTimeParseException =>
        respondHtml(exchange, 400, Pages.newTransactionForm(ledger, Some("日付を正しく入力してください")))
    }
  }

  private def handleDeleteTransaction(exchange: HttpExchange, id: String): Unit = {
    try {
      ledger.deleteTransaction(id)
    } catch {
      case _: TransactionNotFoundException => // already gone; treat delete as idempotent
    }
    redirect(exchange, "/transactions")
  }

  private def handleCreateCategory(exchange: HttpExchange): Unit = {
    val form = Forms.parseFormBody(exchange)
    try {
      ledger.addCategory(form.getOrElse("name", ""))
      redirect(exchange, "/categories")
    } catch {
      case e: ValidationException =>
        respondHtml(exchange, 400, Pages.categories(ledger, Some(e.getMessage)))
      case e: DuplicateCategoryException =>
        respondHtml(exchange, 400, Pages.categories(ledger, Some(e.getMessage)))
    }
  }

  private def respondHtml(exchange: HttpExchange, status: Int, html: String): Unit =
    respond(exchange, status, html, "text/html; charset=utf-8")

  private def respond(
      exchange: HttpExchange,
      status: Int,
      body: String,
      contentType: String = "text/plain; charset=utf-8"
  ): Unit = {
    val bytes = body.getBytes(StandardCharsets.UTF_8)
    exchange.getResponseHeaders.add("Content-Type", contentType)
    exchange.sendResponseHeaders(status, bytes.length.toLong)
    val out: OutputStream = exchange.getResponseBody
    try out.write(bytes)
    finally out.close()
  }

  /** Sends a 303 "See Other" redirect with no response body. The caller does
    * not close `exchange`; [[handle]]'s `finally` block does that once for
    * every branch, to avoid double-closing it here.
    */
  private def redirect(exchange: HttpExchange, location: String): Unit = {
    exchange.getResponseHeaders.add("Location", location)
    exchange.sendResponseHeaders(303, -1)
  }
}
