package budgetscala

import java.time.LocalDate

/** Whether a transaction is money coming in or going out. */
enum TransactionType {
  case Income, Expense
}

/** A single income or expense entry.
  *
  * `amount` is always positive; whether it increases or decreases the
  * balance is determined by [[transactionType]]. Using `BigDecimal` (rather
  * than `Double`) avoids floating-point rounding artifacts when summing
  * money values.
  */
case class Transaction(
    id: String,
    date: LocalDate,
    transactionType: TransactionType,
    amount: BigDecimal,
    category: String,
    memo: String
)
