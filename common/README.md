## Common library

Common constants, data structures, utils, beans and services that can be shared as code between backend api applications.

How to include common module as dependency in your api application

    <dependency>
        <groupId>fi.hel</groupId>
        <artifactId>common</artifactId>
        <version>0.0.2-SNAPSHOT</version>
    </dependency>

If you need Spring beans/components from elastic or events, remember to add a ComponentScan definition to your api application.

### Sending Kafka events

Use predefined topics and message formats when sending event messages. For message formats, see "fi.hel.verkkokauppa.common.events.message" package. For known topic names and event types, see TopicName and EventType classes. When sending events autowire an instance of SendEventService.

add to Spring application class:

    @SpringBootApplication
    @EnableKafka
    @ComponentScan({"fi.hel.verkkokauppa.events", "fi.hel.verkkokauppa.common.events", "fi.hel.verkkokauppa.common.error"})
    public class EventsApplication

add to application.properties or environment

    spring.kafka.bootstrap-servers=localhost:9092
    kafka.user=
    kafka.password=

### Connecting to Elasticsearch

add to Spring application class:

    @SpringBootApplication
    @EnableElasticsearchRepositories
    @ComponentScan({"fi.hel.verkkokauppa.payment", "fi.hel.verkkokauppa.common.elastic", "fi.hel.verkkokauppa.common.error"})
    public class PaymentApplication

add to application.properties or environment

    elasticsearch.service.url=localhost:9200
    elasticsearch.service.user=
    elasticsearch.service.password=

### Error handling only

All backend apis should use common error handling, so excluding events and elastic and including only error, add:

    @ComponentScan({"fi.hel.verkkokauppa.mymodulehere", "fi.hel.verkkokauppa.common.error"})

### Local development environment

TODO setting up a development environment with Elasticsearch or Kafka docker instances
