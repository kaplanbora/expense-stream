package dev.kaplan.expense.stream

import akka.actor.typed.scaladsl.AskPattern._
import akka.actor.typed.{ActorRef, ActorSystem, SpawnProtocol}
import akka.kafka.ConsumerMessage.{CommittableMessage, CommittableOffset}
import akka.kafka.ProducerMessage
import akka.kafka.scaladsl.Producer
import akka.stream.Materializer
import akka.stream.scaladsl.{Flow, Sink}
import akka.util.Timeout
import akka.{Done, NotUsed}
import dev.kaplan.expense.actor.{CurrencyActor, MasterActor}
import dev.kaplan.expense.io.KafkaIO
import dev.kaplan.expense.model._
import dev.kaplan.expense.util.Extension._
import com.typesafe.scalalogging.StrictLogging
import dev.kaplan.expense.actor.CurrencyActor.RetrieveRates
import dev.kaplan.expense.actor.MasterActor.{SpawnCurrencyActor, SpawnResponse}
import org.apache.kafka.clients.producer.ProducerRecord
import spray.json._

import scala.concurrent.{ExecutionContext, Future}
import scala.util.{Either, Left, Right, Try}
import org.apache.kafka.common.TopicPartition
import akka.stream.scaladsl.Source
import akka.kafka.ConsumerMessage

class ExpenseStream(kafkaIO: KafkaIO, masterActor: ActorRef[MasterActor.Command])
                   (implicit system: ActorSystem[SpawnProtocol.Command], ec: ExecutionContext, t: Timeout)
                   extends StrictLogging {
  
  type KafkaEvent    = (CommittableOffset, ExpenseEvent)
  type KafkaEnvelope = ProducerMessage.Envelope[String, String, CommittableOffset]
  type KafkaSource   = Source[ConsumerMessage.CommittableMessage[String, String], NotUsed] 

  private implicit val mat: Materializer = Materializer(system)
  
  private val partitionedKafkaSource = kafkaIO.makeSource
  private val committerSink = kafkaIO.makeSink
  
  def run: Future[Done] =
    partitionedKafkaSource
      .mapAsyncUnordered(1)(spawnActors)
      .map { case (currencyActor, kafkaSource) =>
        kafkaSource via parser via enricher(currencyActor) via producer runWith committerSink
      }
      .runWith(Sink.ignore)

  private val spawnActors: ((TopicPartition, KafkaSource)) => Future[(ActorRef[CurrencyActor.Command], KafkaSource)] = {
    case (kafkaPartition, kafkaSource) =>
      masterActor.ask(SpawnCurrencyActor(kafkaPartition.partition, _))
        .map { case SpawnResponse(currencyActor) =>
          logger.debug(s"Assigning ${currencyActor.path} to partition: ${kafkaPartition.partition}")
          currencyActor -> kafkaSource
        }
  }

  private val invalidEventsCommitter: Sink[Either[CommittableOffset, _], NotUsed] =
    Flow[Either[CommittableOffset, _]]
      .collect { case Left(offset) => offset }
      .to(committerSink)

  private val parser: Flow[CommittableMessage[String, String], KafkaEvent, NotUsed] =
    Flow[CommittableMessage[String, String]]
      .map { event =>
        Try(event.record.value.parseJson.convertTo[ExpenseEvent])
          .fold(_ => Left(event.committableOffset), parsedEvent => Right(event.committableOffset -> parsedEvent))
      }
      .divertLeft(invalidEventsCommitter)

  private def enricher(currencyActor: ActorRef[CurrencyActor.Command]): Flow[KafkaEvent, KafkaEnvelope, NotUsed] =
    Flow[KafkaEvent]
      .mapAsyncUnordered(1) { case (offset, event) =>
        logger.debug(s"Event received: $event")
        currencyActor.ask(RetrieveRates)
          .map { currencyResponse =>
            val enrichedEvent = event.enrich(currencyResponse.conversionRates.rates).toJson.compactPrint
            logger.debug(s"Sending message to kafka: $enrichedEvent")
            val record = new ProducerRecord(kafkaIO.config.destinationTopic, event.id, enrichedEvent)
            ProducerMessage.single(record, offset)
          }
      }

  private val producer: Flow[KafkaEnvelope, CommittableOffset, NotUsed] =
    Producer
      .flexiFlow[String, String, CommittableOffset](kafkaIO.makeProducerSettings)
      .map(_.passThrough)
}
