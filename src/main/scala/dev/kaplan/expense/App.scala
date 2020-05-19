package dev.kaplan.expense

import akka.actor
import akka.actor.typed.scaladsl.AskPattern._
import akka.actor.typed.scaladsl.adapter._
import akka.actor.typed.{ActorRef, ActorSystem, Props, SpawnProtocol}
import akka.util.Timeout
import com.typesafe.scalalogging.StrictLogging
import dev.kaplan.expense.actor.MasterActor
import dev.kaplan.expense.io.KafkaIO
import dev.kaplan.expense.model.{Config, ConversionRates}
import dev.kaplan.expense.stream.ExpenseStream
import pureconfig.ConfigSource
import pureconfig.generic.auto._
import dev.kaplan.expense.io.HttpIO.getLatestRates

import scala.concurrent.{ExecutionContext, Future}
import scala.concurrent.duration._
import scala.util._

object App extends StrictLogging {
  def main(args: Array[String]): Unit = {
    val config = ConfigSource.default.loadOrThrow[Config]

    implicit val system: ActorSystem[SpawnProtocol.Command] = ActorSystem(SpawnProtocol(), "expense-stream")
    implicit val classicSystem: actor.ActorSystem = system.toClassic
    implicit val ec: ExecutionContext = system.executionContext
    implicit val timeout: Timeout = Timeout(10.seconds)

    val kafkaIO = new KafkaIO(config.kafka)

    val stateUpdater: () => Future[ConversionRates] = () => getLatestRates(config.apiToken)

    val streamResult = for {
      initialState   <- getLatestRates(config.apiToken)
      masterActor    <- spawnMasterActor(initialState, config.refreshInterval, stateUpdater)
      expenseStream   = new ExpenseStream(kafkaIO, masterActor)
      streamResult   <- expenseStream.run
    } yield streamResult

    streamResult.onComplete {
      case Success(_) =>
        logger.info(s"Stream has been completed with success")
        system.terminate()
      case Failure(error) =>
        logger.error(s"Stream has been completed with failure", error)
        system.terminate()
    }
  }

  def spawnMasterActor(initialState: ConversionRates, refreshInterval: FiniteDuration, stateUpdater: () => Future[ConversionRates])
                      (implicit system: ActorSystem[SpawnProtocol.Command], timeout: Timeout): Future[ActorRef[MasterActor.Command]] =
    system.ask(
      SpawnProtocol.Spawn(
        MasterActor(initialState, refreshInterval, stateUpdater),
        "master-actor",
        Props.empty,
        _
      )
    )
}
