package fi.hel.verkkokauppa.common.queue.config;

import org.apache.activemq.command.ActiveMQQueue;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.jms.annotation.EnableJms;
import org.springframework.stereotype.Component;

import javax.jms.Queue;

/**
 * @EnableJms Used to declare support for JMS annotations
 *
 */
@Component
@EnableJms
public class BeanConfig {

    /**
     * Get the queue name in the configuration file
     */
    @Value("${queue.order.notifications:order-notifications}")
    private String orderNotifications;

    /**
     * Define the queue for messages
     */
    @Bean(name = "queueOrderNotifications")
    public Queue queueOrderNotifications() {
        return new ActiveMQQueue(orderNotifications);
    }

}
