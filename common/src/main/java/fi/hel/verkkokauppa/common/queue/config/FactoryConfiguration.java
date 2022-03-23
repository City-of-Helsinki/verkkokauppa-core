package fi.hel.verkkokauppa.common.queue.config;

import fi.hel.verkkokauppa.common.queue.error.DefaultActiveMQErrorHandler;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;
import org.springframework.jms.annotation.EnableJms;
import org.springframework.jms.annotation.JmsListenerConfigurer;
import org.springframework.jms.config.DefaultJmsListenerContainerFactory;
import org.springframework.jms.config.JmsListenerContainerFactory;
import org.springframework.jms.config.JmsListenerEndpointRegistrar;

import javax.jms.ConnectionFactory;
import javax.jms.QueueConnectionFactory;

@EnableJms
@Configuration
public class FactoryConfiguration implements JmsListenerConfigurer {

    @Autowired
    private ConnectionFactory connectionFactory;
    @Autowired
    private QueueConnectionFactory queueConnectionFactory;

    @Autowired
    private Environment environment;

    @Override
    public void configureJmsListeners(JmsListenerEndpointRegistrar registrar) {
        if (environment.getProperty("spring.activemq.broker-url") != null) {
            registrar.setContainerFactory(containerFactory());
        }
    }

    @Bean
    public JmsListenerContainerFactory<?> containerFactory() {
        DefaultJmsListenerContainerFactory factory = new DefaultJmsListenerContainerFactory();
        factory.setConnectionFactory(connectionFactory);
        factory.setSessionTransacted(true);
        factory.setErrorHandler(new DefaultActiveMQErrorHandler());
        return factory;
    }
}