package dev.kaplan.expense.model

import scala.concurrent.duration.FiniteDuration

case class Config(kafka: KafkaConfig, refreshInterval: FiniteDuration, apiToken: String)

case class KafkaConfig(clientId: String,
                       groupId: String,
                       hosts: String,
                       sourceTopic: String,
                       destinationTopic: String)
