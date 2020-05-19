FROM openjdk:11

ARG JAR_PATH="transaction-consumer*.jar"

ENV LANG "en_US.UTF-8"
ENV LC_ALL "en_US.UTF-8"
ENV TZ "Europe/Istanbul"

COPY $JAR_PATH consumer.jar

ENTRYPOINT ["sh", "-c", "exec", "java", "-jar", "consumer.jar", "-XX:+ExitOnOutOfMemoryError", "-XX:MaxRAMPercentage=65"]

