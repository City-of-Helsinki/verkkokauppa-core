package fi.hel.verkkokauppa.events.configuration;

import fi.hel.verkkokauppa.common.events.TopicName;
import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.TopicBuilder;

@Configuration
class KafkaTopicConfig {

    @Bean
    public NewTopic topicPayments() {
        return TopicBuilder.name(TopicName.PAYMENTS).build();
    }

    @Bean
    public NewTopic topicSubscriptions() {
        return TopicBuilder.name(TopicName.SUBSCRIPTIONS).build();
    }

    @Bean
    public NewTopic topicOrders() {
        return TopicBuilder.name(TopicName.ORDERS).build();
    }

}