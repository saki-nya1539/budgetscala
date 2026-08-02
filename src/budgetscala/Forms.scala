package budgetscala

import com.sun.net.httpserver.HttpExchange
import java.io.ByteArrayOutputStream
import java.net.{URLDecoder, URI}
import java.nio.charset.StandardCharsets

/** Parsing for `application/x-www-form-urlencoded` request bodies and query
  * strings. No servlet/web framework is used in this project, so this is a
  * small hand-rolled equivalent of what such a framework would normally
  * provide for you.
  */
object Forms {

  private def decode(s: String): String = URLDecoder.decode(s, StandardCharsets.UTF_8)

  /** Parses a `key=value&key2=value2` style string (already read into memory)
    * into a `Map`. Keys with no `=` are ignored; later duplicate keys win.
    */
  def parseEncoded(raw: String): Map[String, String] = {
    if (raw == null || raw.isEmpty) Map.empty
    else
      raw
        .split("&")
        .toList
        .flatMap { pair =>
          pair.split("=", 2) match {
            case Array(key, value) => Some(decode(key) -> decode(value))
            case Array(key) if key.nonEmpty => Some(decode(key) -> "")
            case _ => None
          }
        }
        .toMap
  }

  /** Reads and decodes the full request body of a POST request as a form. */
  def parseFormBody(exchange: HttpExchange): Map[String, String] = {
    val buffer = new ByteArrayOutputStream()
    val input = exchange.getRequestBody
    val chunk = new Array[Byte](4096)
    var read = input.read(chunk)
    while (read != -1) {
      buffer.write(chunk, 0, read)
      read = input.read(chunk)
    }
    parseEncoded(new String(buffer.toByteArray, StandardCharsets.UTF_8))
  }

  /** Parses the query string (`?a=1&b=2`) of a request URI. */
  def parseQuery(uri: URI): Map[String, String] = parseEncoded(uri.getRawQuery)
}
