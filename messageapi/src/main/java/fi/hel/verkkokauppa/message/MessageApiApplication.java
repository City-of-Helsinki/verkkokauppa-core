package fi.hel.verkkokauppa.message;

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
		"fi.hel.verkkokauppa.common.queue"
})
public class MessageApiApplication {

	public static void main(String[] args) {
		SpringApplication.run(MessageApiApplication.class, args);
	}

}
