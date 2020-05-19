package dev.kaplan.expense.io

import akka.{Done, NotUsed}
import akka.actor.ActorSystem
import akka.kafka._
import akka.kafka.scaladsl.{Committer, Consumer}
import akka.stream.scaladsl.{Sink, Source}
import dev.kaplan.expense.model.KafkaConfig
import org.apache.kafka.common.TopicPartition
import org.apache.kafka.common.serialization.{StringDeserializer, StringSerializer}

import scala.concurrent.Future

class KafkaIO(val config: KafkaConfig)(implicit system: ActorSystem) {
  type KafkaMessage = ConsumerMessage.CommittableMessage[String, String]

  def makeSource: Source[(TopicPartition, Source[KafkaMessage, NotUsed]), Consumer.Control] =
    Consumer.committablePartitionedSource(makeConsumerSettings, Subscriptions.topics(config.sourceTopic))

  def makeSink: Sink[ConsumerMessage.Committable, Future[Done]] =
    Committer.sink(CommitterSettings(system))

  def makeConsumerSettings: ConsumerSettings[String, String] =
    ConsumerSettings(system, new StringDeserializer, new StringDeserializer)
      .withBootstrapServers(config.hosts)
      .withClientId(config.clientId)
      .withGroupId(config.groupId)

  def makeProducerSettings: ProducerSettings[String, String] =
    ProducerSettings(system, new StringSerializer, new StringSerializer)
      .withBootstrapServers(config.hosts)
}
