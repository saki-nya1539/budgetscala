package budgetscala

/** Small HTML-rendering helpers shared by every page in [[Pages]].
  *
  * There is no templating library or external dependency here on purpose;
  * pages are plain Scala string interpolation, escaped through [[escape]]
  * wherever user-supplied text (memo, category names) is embedded.
  */
object Html {

  /** Escapes text for safe embedding inside HTML markup. */
  def escape(text: String): String =
    text
      .replace("&", "&amp;")
      .replace("<", "&lt;")
      .replace(">", "&gt;")
      .replace("\"", "&quot;")
      .replace("'", "&#39;")

  private val navItems: List[(String, String)] = List(
    "/" -> "ダッシュボード",
    "/transactions" -> "取引一覧",
    "/transactions/new" -> "取引を追加",
    "/categories" -> "カテゴリ"
  )

  private def renderNav(activePath: String): String = {
    val links = navItems
      .map { case (path, label) =>
        val cls = if (path == activePath) " class=\"active\"" else ""
        s"""<a href="$path"$cls>${escape(label)}</a>"""
      }
      .mkString("\n")
    s"""<nav>$links</nav>"""
  }

  /** Wraps `bodyHtml` in the common page shell: doctype, `<head>` with
    * inline CSS, and the 4-page navigation bar with `activePath` highlighted.
    */
  def page(title: String, activePath: String, bodyHtml: String): String =
    s"""<!DOCTYPE html>
       |<html lang="ja">
       |<head>
       |<meta charset="utf-8">
       |<title>${escape(title)} — BudgetScala</title>
       |<style>
       |  body { font-family: -apple-system, "Hiragino Sans", "Yu Gothic", sans-serif; margin: 0; background: #f5f6fa; color: #222; }
       |  nav { background: #2c3e6b; padding: 0 16px; display: flex; gap: 4px; }
       |  nav a { color: #d7dcf5; text-decoration: none; padding: 14px 12px; display: inline-block; font-size: 14px; }
       |  nav a.active { color: #fff; font-weight: bold; border-bottom: 3px solid #6c8ef5; }
       |  main { max-width: 720px; margin: 24px auto; background: #fff; padding: 24px 32px; border-radius: 8px; box-shadow: 0 1px 4px rgba(0,0,0,0.08); }
       |  h1 { font-size: 20px; margin-top: 0; }
       |  table { width: 100%; border-collapse: collapse; margin-top: 12px; }
       |  th, td { text-align: left; padding: 8px 6px; border-bottom: 1px solid #eee; font-size: 14px; }
       |  .income { color: #1b7f3a; }
       |  .expense { color: #c0392b; }
       |  .summary-grid { display: flex; gap: 16px; margin-bottom: 20px; }
       |  .summary-card { flex: 1; background: #f0f2fa; border-radius: 6px; padding: 12px 16px; }
       |  .summary-card .label { font-size: 12px; color: #666; }
       |  .summary-card .value { font-size: 20px; font-weight: bold; }
       |  .bar-row { display: flex; align-items: center; gap: 8px; margin: 6px 0; font-size: 13px; }
       |  .bar-track { flex: 1; background: #eee; border-radius: 4px; height: 10px; overflow: hidden; }
       |  .bar-fill { background: #6c8ef5; height: 100%; }
       |  form.inline { display: inline; }
       |  form.card { display: flex; flex-direction: column; gap: 10px; max-width: 360px; }
       |  label { font-size: 13px; color: #444; }
       |  input, select { padding: 6px 8px; font-size: 14px; }
       |  button { padding: 8px 14px; font-size: 14px; background: #2c3e6b; color: #fff; border: none; border-radius: 4px; cursor: pointer; }
       |  button.danger { background: #c0392b; padding: 4px 10px; font-size: 12px; }
       |  .error { background: #fde8e8; color: #a12a2a; padding: 8px 12px; border-radius: 4px; margin-bottom: 12px; font-size: 13px; }
       |</style>
       |</head>
       |<body>
       |${renderNav(activePath)}
       |<main>
       |$bodyHtml
       |</main>
       |</body>
       |</html>
       |""".stripMargin
}
