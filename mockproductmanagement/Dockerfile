FROM maven:3-openjdk-11
WORKDIR /app
COPY . /app
RUN mvn install

FROM openjdk:11
WORKDIR /app
COPY --from=0 /app/target/mockproductmanagement-0.0.1-SNAPSHOT.jar /app/target/mockproductmanagement-0.0.1-SNAPSHOT.jar
EXPOSE 8082
CMD ["java -jar mockproductmanagement-0.0.1-SNAPSHOT.jar"] 
