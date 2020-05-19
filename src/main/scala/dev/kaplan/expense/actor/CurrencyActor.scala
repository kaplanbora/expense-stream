package dev.kaplan.expense.actor

import akka.actor.typed.scaladsl.Behaviors
import akka.actor.typed.{ActorRef, Behavior}
import dev.kaplan.expense.model.ConversionRates

object CurrencyActor {
  sealed trait Command
  case class RetrieveRates(replyTo: ActorRef[CurrencyResponse]) extends Command
  case class UpdateRates(conversionRates: ConversionRates)     extends Command

  case class CurrencyResponse(conversionRates: ConversionRates)

  def apply(conversionRates: ConversionRates): Behavior[Command] =
    Behaviors.receiveMessage {
      case RetrieveRates(replyTo) =>
        replyTo ! CurrencyResponse(conversionRates)
        Behaviors.same
      case UpdateRates(newRates) =>
        apply(newRates)
    }
}
