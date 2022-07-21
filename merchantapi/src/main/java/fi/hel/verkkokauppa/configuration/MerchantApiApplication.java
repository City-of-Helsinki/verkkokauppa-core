package fi.hel.verkkokauppa.configuration;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.jms.JmsAutoConfiguration;
import org.springframework.boot.autoconfigure.jms.activemq.ActiveMQAutoConfiguration;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.data.elasticsearch.repository.config.EnableElasticsearchRepositories;

@SpringBootApplication(exclude = {
		JmsAutoConfiguration.class,
		ActiveMQAutoConfiguration.class,
})
@EnableElasticsearchRepositories
@ComponentScan({
		"fi.hel.verkkokauppa.configuration",
		"fi.hel.verkkokauppa.configuration.configuration",
		"fi.hel.verkkokauppa.common.elastic",
		"fi.hel.verkkokauppa.common.error",
})
public class MerchantApiApplication {

	public static void main(String[] args) {
		SpringApplication.run(MerchantApiApplication.class, args);
	}

}
