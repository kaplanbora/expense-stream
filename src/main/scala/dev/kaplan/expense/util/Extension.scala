package dev.kaplan.expense.util

import akka.stream.{Graph, SinkShape}
import akka.stream.scaladsl.Flow

object Extension {
  implicit class FlowOps[A, L, R, M](flow: Flow[A, Either[L, R], M]) {
    def divertLeft(diversionFlow: Graph[SinkShape[Either[L, R]], M]): Flow[A, R, M] =
      flow.via {
        Flow[Either[L, R]]
          .divertTo(diversionFlow, _.isLeft)
          .collect { case Right(right) => right }
      }
  }
}
