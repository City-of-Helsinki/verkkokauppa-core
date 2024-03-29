#!/bin/bash
cd "$API_DIR" || exit
dos2unix mvnw
./mvnw spring-boot:run -Dspring-boot.run.jvmArguments="-agentlib:jdwp=transport=dt_socket,server=y,suspend=n,address=*:5005" -DskipTests &
while true; do
  inotifywait -e modify,create,delete,move -r ./src/ && ./mvnw compile
done