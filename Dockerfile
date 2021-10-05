FROM registry.access.redhat.com/ubi8/openjdk-11:latest as builder
COPY . /tmp/src
USER 0
# Since we want to execute the mvn command with RUN (and not when the container gets started),
# we have to do here some manual setup which would be made by the maven's entrypoint script
RUN mkdir -p /root/.m2 \
    && mkdir /root/.m2/repository
# Copy maven settings, containing repository configurations
COPY settings.xml /root/.m2
EXPOSE 8080
VOLUME /tmp/
ARG API_DIR=NOTSET
WORKDIR ${API_DIR}
ENTRYPOINT ["mvn","spring-boot:run","-Dserver.id=talpa-artifacts -Dserver.username=City-of-Helsinki -Dserver.password="]