name := "expense-stream"

version := "1.0.0"

scalaVersion := "2.13.1"

val akkaVersion = "2.6.4"

libraryDependencies ++= Seq(
  "com.typesafe.akka"          %% "akka-stream-kafka"        % "2.0.2",
  "com.typesafe.akka"          %% "akka-http-spray-json"     % "10.1.11",
  "com.typesafe.akka"          %% "akka-actor-typed"         % akkaVersion,
  "com.typesafe.akka"          %% "akka-stream"              % akkaVersion,
  "com.github.pureconfig"      %% "pureconfig"               % "0.12.3",
  "com.typesafe.scala-logging" %% "scala-logging"            % "3.9.2",
  "io.spray"                   %% "spray-json"               % "1.3.5",
  "ch.qos.logback"             % "logback-classic"           % "1.2.3",
)

scalacOptions += "-deprecation"
scalacOptions += "-Wunused"

