# Expense Stream
This is an example to demonstrate how Akka Stream coexists with Akka Typed. You can read the full article [here](https://kaplan.dev/articles/akka-stream-coexistence-with-akka-typed/).

### How to Run
Create a local kafka cluster and required topics
```bash
cd kafka-docker
docker-compose up -d
```

Start kafka console consumer to read the results
```bash
kafka-console-consumer.sh --bootstrap-server localhost:9092 --topic transactions-destination
```

Run the application
```bash
sbt run
```

Copy example payload and send it with producer
```bash
xclip -selection clipboard payload.json
kafka-console-producer.sh --broker-list localhost:9092 --topic transactions-source
> { "id": "1234", "amount": 13.37, "description": "Test", "timestamp": 1589133043 }
>
```

