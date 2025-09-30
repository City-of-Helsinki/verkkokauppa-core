#FROM eclipse-temurin:11
FROM registry.access.redhat.com/ubi8/openjdk-11
RUN apt-get update && apt-get -y upgrade
RUN apt-get install -y inotify-tools dos2unix
ENV HOME=/app/orderapi
RUN mkdir -p $HOME
WORKDIR $HOME
