FROM registry.access.redhat.com/ubi8/openjdk-11:latest as builder
COPY . /tmp/src
USER 0
#RUN chown -R 1001:0 /tmp/src && chmod -R ug+rwx /tmp/src && \
#    rpm-file-permissions
#USER 1001
WORKDIR /tmp/src
RUN mvn package
ARG JAR_FILE=target/*.jar
COPY ${JAR_FILE} app.jar
ENTRYPOINT ["java","-jar","/app.jar"]

