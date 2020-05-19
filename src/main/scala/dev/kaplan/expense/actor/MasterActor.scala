package dev.kaplan.expense.actor

import akka.actor.typed._
import akka.actor.typed.scaladsl.Behaviors
import dev.kaplan.expense.model.ConversionRates

import scala.concurrent.duration._
import scala.concurrent.Future

object MasterActor {
  sealed trait Command
  case class UpdatedRates(conversionRates: ConversionRates)                          extends Command
  case class ChildTerminated(child: ActorRef[CurrencyActor.Command], partition: Int) extends Command
  case class SpawnCurrencyActor(partition: Int, replyTo: ActorRef[SpawnResponse])    extends Command
  case object TriggerUpdate                                                          extends Command

  case class SpawnResponse(currencyActor: ActorRef[CurrencyActor.Command])

  def apply(initialRates: ConversionRates,
            refreshInterval: FiniteDuration,
            stateUpdater: () => Future[ConversionRates]): Behavior[Command] =

    Behaviors.supervise[Command] {
      Behaviors.withTimers { timer =>
        timer.startTimerWithFixedDelay(TriggerUpdate, refreshInterval)

          def onMessage(currentRates: ConversionRates, children: Map[Int, ActorRef[CurrencyActor.Command]]): Behavior[Command] = 
            Behaviors.receive { case (context, message) => 
              message match {
                case SpawnCurrencyActor(partition, replyTo) =>
                  val newActor = context.spawn(CurrencyActor(currentRates), s"currency-actor-$partition")
                  replyTo ! SpawnResponse(newActor)
                  context.watchWith(newActor, ChildTerminated(newActor, partition))
                  onMessage(currentRates, children + (partition -> newActor))
                case TriggerUpdate =>
                  context.pipeToSelf(stateUpdater())(
                    _.fold(_ => UpdatedRates(currentRates), newRates => UpdatedRates(newRates))
                  )
                  Behaviors.same
                case UpdatedRates(newRates) =>
                  context.log.debug(s"Updating state with: $newRates")
                  children.values.foreach(_ ! CurrencyActor.UpdateRates(newRates))
                  onMessage(newRates, children)
                case ChildTerminated(_, partition) =>
                  onMessage(currentRates, children - partition)
              }
            }

          onMessage(initialRates, Map.empty)
      }
    }.onFailure(
      SupervisorStrategy.restartWithBackoff(minBackoff = 100.millis, maxBackoff = 30.second, randomFactor = 0.2)
    )
}
