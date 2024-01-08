package fi.hel.verkkokauppa.mockproductmanagement;

import org.apache.activemq.artemis.jms.client.ActiveMQConnectionFactory;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.jms.JmsAutoConfiguration;
import org.springframework.boot.autoconfigure.jms.activemq.ActiveMQAutoConfiguration;
import org.springframework.context.annotation.ComponentScan;

@SpringBootApplication(exclude = {
		JmsAutoConfiguration.class,
		ActiveMQAutoConfiguration.class,
})
@ComponentScan({
		"fi.hel.verkkokauppa.mockproductmanagement",
		"fi.hel.verkkokauppa.backend",
		"fi.hel.verkkokauppa.common.sentry"
})
public class MockproductmanagementApplication {

	public static void main(String[] args) {
		SpringApplication.run(MockproductmanagementApplication.class, args);
	}

}
