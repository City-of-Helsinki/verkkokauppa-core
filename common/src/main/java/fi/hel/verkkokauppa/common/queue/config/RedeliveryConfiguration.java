package fi.hel.verkkokauppa.common.queue.config;

import org.apache.activemq.RedeliveryPolicy;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.jms.activemq.ActiveMQConnectionFactoryCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RedeliveryConfiguration {

    @Value("${activemq.use-exponential-backoff:#{true}}")
    private Boolean useExponentialBackoff;

    @Value("${activemq.redelivery-delay:#{300000}}")
    private Long redeliveryDelay;

    @Value("${activemq.back-off-multiplier:#{5}}")
    private Long backOffMultiplier;

    @Value("${activemq.maximum-redeliveries:#{5}}")
    private Integer maximumRedeliveries;

    @Bean
    public ActiveMQConnectionFactoryCustomizer configureRedeliveryPolicy() {
        return connectionFactory ->
        {
            RedeliveryPolicy redeliveryPolicy = new RedeliveryPolicy();
            redeliveryPolicy.setUseExponentialBackOff(useExponentialBackoff);
            redeliveryPolicy.setRedeliveryDelay(redeliveryDelay);
            // Works as redeliveryDelayMultiplier
            redeliveryPolicy.setBackOffMultiplier(backOffMultiplier);
            redeliveryPolicy.setMaximumRedeliveries(maximumRedeliveries);
            // configure redelivery policy
            connectionFactory.setRedeliveryPolicy(redeliveryPolicy);
        };
    }
}