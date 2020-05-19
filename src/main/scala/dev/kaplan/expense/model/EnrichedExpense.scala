package dev.kaplan.expense.model

import spray.json._

case class Amount(TRY: BigDecimal, EUR: BigDecimal, USD: BigDecimal)

object Amount extends DefaultJsonProtocol {
  implicit val formatter: RootJsonFormat[Amount] = jsonFormat3(Amount.apply)
}

case class EnrichedExpense(id: String, amount: Amount, description: String, timestamp: Long)

object EnrichedExpense extends DefaultJsonProtocol {
  implicit val formatter: RootJsonFormat[EnrichedExpense] = jsonFormat4(EnrichedExpense.apply)
}

