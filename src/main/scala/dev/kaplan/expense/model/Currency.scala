package dev.kaplan.expense.model

import spray.json._

case class UsdConversionRate(TRY: BigDecimal, EUR: BigDecimal)

object UsdConversionRate extends DefaultJsonProtocol {
  implicit val formatter: RootJsonFormat[UsdConversionRate] = jsonFormat2(UsdConversionRate.apply)
}

case class ConversionRates(timestamp: Long, rates: UsdConversionRate)

object ConversionRates extends DefaultJsonProtocol {
  implicit val formatter: RootJsonFormat[ConversionRates] = jsonFormat2(ConversionRates.apply)
}

