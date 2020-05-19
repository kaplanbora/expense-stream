package dev.kaplan.expense.io

import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport._
import akka.actor.ActorSystem
import akka.http.scaladsl.Http
import akka.http.scaladsl.model.{ContentTypes, HttpRequest}
import akka.http.scaladsl.unmarshalling.Unmarshal
import dev.kaplan.expense.model.ConversionRates

import scala.concurrent.{ExecutionContext, Future}

object HttpIO {
  def getLatestRates(token: String)(implicit s: ActorSystem, ec: ExecutionContext): Future[ConversionRates] =
    Http().singleRequest(
      HttpRequest(uri = s"https://openexchangerates.org/api/latest.json?base=USD&app_id=$token")
    ).flatMap { response =>
      Unmarshal(response.entity.withContentType(ContentTypes.`application/json`)).to[ConversionRates]
    }
}
