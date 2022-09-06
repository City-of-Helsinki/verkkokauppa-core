package fi.hel.verkkokauppa.events.configuration;

import fi.hel.verkkokauppa.common.events.message.OrderMessage;
import fi.hel.verkkokauppa.common.events.message.PaymentMessage;
import fi.hel.verkkokauppa.common.events.message.RefundMessage;
import fi.hel.verkkokauppa.common.events.message.SubscriptionMessage;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.common.config.SaslConfigs;
import org.apache.kafka.common.security.plain.PlainLoginModule;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory;
import org.springframework.kafka.config.KafkaListenerContainerFactory;
import org.springframework.kafka.core.ConsumerFactory;
import org.springframework.kafka.core.DefaultKafkaConsumerFactory;
import org.springframework.kafka.listener.ConcurrentMessageListenerContainer;
import org.springframework.kafka.support.serializer.JsonDeserializer;

import java.util.HashMap;
import java.util.Map;

@Configuration
public class KafkaConsumerConfig {

    @Value("${spring.kafka.bootstrap-servers:#{null}}")
    private String bootstrapServers;

    @Value("${kafka.client.authentication.enabled:#{true}}")
    private Boolean kafkaClientAuthenticationEnabled;

    @Value("${kafka.user:#{null}}")
    private String kafkaUser;

    @Value("${kafka.password:#{null}}")
    private String kafkaPassword;

    // default consumer string key, string value

    @Bean
    public Map<String, Object> consumerConfigs() {
        Map<String, Object> props = new HashMap<>();
        props.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        props.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
        props.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);

        if (kafkaClientAuthenticationEnabled) {
            props.put("security.protocol", "SASL_PLAINTEXT");
            //props.put(SaslConfigs.SASL_ENABLED_MECHANISMS, "SCRAM-SHA-512");
            props.put(SaslConfigs.SASL_MECHANISM, "SCRAM-SHA-512");
            props.put(SaslConfigs.SASL_JAAS_CONFIG, "org.apache.kafka.common.security.scram.ScramLoginModule required username=\"" + kafkaUser + "\" password=\"" + kafkaPassword + "\";");
        }

        return props;
    }

    @Bean
    public ConsumerFactory<String, String> consumerFactory() {
        return new DefaultKafkaConsumerFactory<>(consumerConfigs());
    }

    @Bean
    public KafkaListenerContainerFactory<ConcurrentMessageListenerContainer<String, String>> kafkaListenerContainerFactory() {
        ConcurrentKafkaListenerContainerFactory<String, String> factory =
                new ConcurrentKafkaListenerContainerFactory<>();
        factory.setConsumerFactory(consumerFactory());
        return factory;
    }

    // PaymentMessage consumer

    @Bean
    public ConsumerFactory<String, PaymentMessage> paymentsConsumerFactory() {
        return new DefaultKafkaConsumerFactory<>(
                consumerConfigs(),
                new StringDeserializer(),
                new JsonDeserializer<>(PaymentMessage.class));
    }

    @Bean
    public ConcurrentKafkaListenerContainerFactory<String, PaymentMessage> paymentsKafkaListenerContainerFactory() {
        ConcurrentKafkaListenerContainerFactory<String, PaymentMessage> factory =
                new ConcurrentKafkaListenerContainerFactory<>();
        factory.setConsumerFactory(paymentsConsumerFactory());
        return factory;
    }

    // SubscriptionMessage consumer

    @Bean
    public ConsumerFactory<String, SubscriptionMessage> subscriptionsConsumerFactory() {
        return new DefaultKafkaConsumerFactory<>(
                consumerConfigs(),
                new StringDeserializer(),
                new JsonDeserializer<>(SubscriptionMessage.class));
    }

    @Bean
    public ConcurrentKafkaListenerContainerFactory<String, SubscriptionMessage> subscriptionsKafkaListenerContainerFactory() {
        ConcurrentKafkaListenerContainerFactory<String, SubscriptionMessage> factory =
                new ConcurrentKafkaListenerContainerFactory<>();
        factory.setConsumerFactory(subscriptionsConsumerFactory());
        return factory;
    }

    // OrderMessage consumer

    @Bean
    public ConsumerFactory<String, OrderMessage> ordersConsumerFactory() {
        return new DefaultKafkaConsumerFactory<>(
                consumerConfigs(),
                new StringDeserializer(),
                new JsonDeserializer<>(OrderMessage.class));
    }

    @Bean
    public ConcurrentKafkaListenerContainerFactory<String, OrderMessage> ordersKafkaListenerContainerFactory() {
        ConcurrentKafkaListenerContainerFactory<String, OrderMessage> factory =
                new ConcurrentKafkaListenerContainerFactory<>();
        factory.setConsumerFactory(ordersConsumerFactory());
        return factory;
    }

    // RefundMessage consumer

    @Bean
    public ConsumerFactory<String, RefundMessage> refundsConsumerFactory() {
        return new DefaultKafkaConsumerFactory<>(
                consumerConfigs(),
                new StringDeserializer(),
                new JsonDeserializer<>(RefundMessage.class));
    }

    @Bean
    public ConcurrentKafkaListenerContainerFactory<String, RefundMessage> refundsKafkaListenerContainerFactory() {
        ConcurrentKafkaListenerContainerFactory<String, RefundMessage> factory =
                new ConcurrentKafkaListenerContainerFactory<>();
        factory.setConsumerFactory(refundsConsumerFactory());
        return factory;
    }

}