package dev.kaplan.expense.model

import spray.json._

case class ExpenseEvent(id: String, amount: BigDecimal, description: String, timestamp: Long) {
  def enrich(rates: UsdConversionRate): EnrichedExpense =
    EnrichedExpense(id, Amount(amount * rates.TRY, amount * rates.EUR, amount), description, timestamp)
}

object ExpenseEvent extends DefaultJsonProtocol {
  implicit val formatter: RootJsonFormat[ExpenseEvent] = jsonFormat4(ExpenseEvent.apply)
}
