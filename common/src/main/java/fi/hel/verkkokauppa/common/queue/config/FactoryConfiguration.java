package fi.hel.verkkokauppa.common.queue.config;

import fi.hel.verkkokauppa.common.queue.error.DefaultErrorHandler;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jms.annotation.EnableJms;
import org.springframework.jms.annotation.JmsListenerConfigurer;
import org.springframework.jms.config.DefaultJmsListenerContainerFactory;
import org.springframework.jms.config.JmsListenerContainerFactory;
import org.springframework.jms.config.JmsListenerEndpointRegistrar;

import javax.jms.ConnectionFactory;

@EnableJms
@Configuration
public class FactoryConfiguration implements JmsListenerConfigurer {

    @Autowired
    private ConnectionFactory connectionFactory;

    @Override
    public void configureJmsListeners(JmsListenerEndpointRegistrar registrar) {
        registrar.setContainerFactory(containerFactory());
    }

    @Bean
    public JmsListenerContainerFactory<?> containerFactory() {
        DefaultJmsListenerContainerFactory factory = new DefaultJmsListenerContainerFactory();
        factory.setConnectionFactory(connectionFactory);
        factory.setErrorHandler(new DefaultErrorHandler());
        return factory;
    }

}